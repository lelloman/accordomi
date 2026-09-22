#include "internal.h"
#include <math.h>
#include <stdlib.h>
#include <string.h>
#define PI 3.14159265358979323846
size_t ac_fft_size(size_t n) {
    size_t s = 1;
    while (s < n)
        s *= 2;
    return s;
}
size_t ac_workspace_bytes(size_t n) {
    if (n < 2 || n > 1048576)
        return 0;
    return sizeof(ac_workspace) + (4 * ac_fft_size(4 * n) + 3 * (n + 1)) * sizeof(double) +
           (n + 1) * sizeof(int);
}
ac_workspace *ac_workspace_init(void *memory, size_t bytes, size_t n) {
    size_t required = ac_workspace_bytes(n);
    if (!memory || !required || bytes < required)
        return NULL;
    ac_workspace *w = memory;
    w->capacity = n;
    w->fft_capacity = ac_fft_size(4 * n);
    w->plan_size = 0;
    w->real = (double *)(w + 1);
    w->imag = w->real + w->fft_capacity;
    w->twiddle_real = w->imag + w->fft_capacity;
    w->twiddle_imag = w->twiddle_real + w->fft_capacity;
    w->energy = w->twiddle_imag + w->fft_capacity;
    w->values = w->energy + n + 1;
    w->difference = w->values + n + 1;
    w->peaks = (int *)(w->difference + n + 1);
    return w;
}
ac_workspace *ac_workspace_create(size_t n) {
    size_t bytes = ac_workspace_bytes(n);
    if (!bytes)
        return NULL;
    void *memory = malloc(bytes);
    if (!memory)
        return NULL;
    return ac_workspace_init(memory, bytes, n);
}
void ac_workspace_destroy(ac_workspace *w) {
    free(w);
}
/* Stage-contiguous twiddles remove the loop-carried complex rotation. The
 * butterfly inputs/outputs are disjoint, so the compiler can use ARM64 NEON
 * (or the host vector ISA) without architecture-specific accuracy shortcuts. */
static void butterflies(double *restrict ar, double *restrict ai, double *restrict br,
                        double *restrict bi, const double *restrict wr, const double *restrict wi,
                        size_t count, double sign) {
    for (size_t i = 0; i < count; ++i) {
        double imaginary = sign * wi[i];
        double real = wr[i] * br[i] - imaginary * bi[i];
        double imag = wr[i] * bi[i] + imaginary * br[i];
        br[i] = ar[i] - real;
        bi[i] = ai[i] - imag;
        ar[i] += real;
        ai[i] += imag;
    }
}
void ac_fft(ac_workspace *w, size_t n, int inverse) {
    double *r = w->real, *im = w->imag;
    if (w->plan_size != n) {
        for (size_t width = 2; width <= n; width *= 2) {
            size_t half = width / 2;
            for (size_t offset = 0; offset < half; ++offset) {
                double angle = -2 * PI * offset / width;
                w->twiddle_real[half + offset] = cos(angle);
                w->twiddle_imag[half + offset] = sin(angle);
            }
        }
        w->plan_size = n;
    }
    size_t reversed = 0;
    for (size_t i = 1; i < n; ++i) {
        size_t bit = n >> 1;
        while (reversed & bit) {
            reversed ^= bit;
            bit >>= 1;
        }
        reversed ^= bit;
        if (i < reversed) {
            double t = r[i];
            r[i] = r[reversed];
            r[reversed] = t;
            t = im[i];
            im[i] = im[reversed];
            im[reversed] = t;
        }
    }
    for (size_t width = 2; width <= n; width *= 2) {
        size_t half = width / 2;
        for (size_t start = 0; start < n; start += width)
            butterflies(r + start, im + start, r + start + half, im + start + half,
                        w->twiddle_real + half, w->twiddle_imag + half, half, inverse ? -1 : 1);
    }
    if (inverse)
        for (size_t i = 0; i < n; ++i) {
            r[i] /= n;
            im[i] /= n;
        }
}
int ac_prepare_correlation(ac_workspace *w, const float *samples, size_t n) {
    if (!w || !samples || n < 2 || n > w->capacity)
        return 0;
    size_t fft = ac_fft_size(2 * n);
    memset(w->real, 0, fft * sizeof(double));
    memset(w->imag, 0, fft * sizeof(double));
    double mean = 0;
    for (size_t i = 0; i < n; ++i) {
        if (!isfinite(samples[i]))
            return 0;
        mean += samples[i];
    }
    mean /= n;
    w->energy[0] = 0;
    for (size_t i = 0; i < n; ++i) {
        double s = samples[i] - mean;
        w->real[i] = s;
        w->energy[i + 1] = w->energy[i] + s * s;
    }
    ac_fft(w, fft, 0);
    for (size_t i = 0; i < fft; ++i) {
        w->real[i] = w->real[i] * w->real[i] + w->imag[i] * w->imag[i];
        w->imag[i] = 0;
    }
    ac_fft(w, fft, 1);
    return 1;
}
int ac_correlate(ac_workspace *w, const float *s, size_t n, double *c, double *a, double *b) {
    if (!c || !a || !b || !ac_prepare_correlation(w, s, n))
        return 0;
    for (size_t i = 0; i < n; ++i) {
        c[i] = w->real[i];
        a[i] = w->energy[n - i];
        b[i] = w->energy[n] - w->energy[i];
    }
    return 1;
}
static double interpolate(const double *v, int tau, int max) {
    if (tau <= 1 || tau >= max)
        return tau;
    double d = v[tau - 1] - 2 * v[tau] + v[tau + 1];
    return d == 0 ? tau : tau + (v[tau - 1] - v[tau + 1]) / (2 * d);
}
int ac_detect(ac_workspace *w, const float *s, size_t n, int rate, ac_method method,
              ac_pitch *out) {
    if (out)
        memset(out, 0, sizeof(*out));
    if (!out || !w || !s || n < 512 || n > w->capacity || rate <= 0 || method < AC_YIN ||
        method > AC_MCLEOD)
        return 0;
    double amplitude = 0;
    for (size_t i = 0; i < n; ++i) {
        if (!isfinite(s[i]))
            return 0;
        amplitude = fmax(amplitude, fabs(s[i]));
    }
    if (amplitude < (method == AC_AUTOCORRELATION ? .01f : .003f))
        return 0;
    int min = rate / 4800, max = rate / 24;
    if ((size_t)max > n / 2)
        max = (int)(n / 2);
    if (min >= max || !ac_prepare_correlation(w, s, n))
        return 0;
    double *v = w->values, *diff = w->difference;
    memset(v, 0, (max + 1) * sizeof(double));
    int peak = -1;
    if (method == AC_YIN) {
        double sum = 0;
        v[0] = 1;
        diff[0] = 0;
        for (int i = 1; i <= max; ++i) {
            diff[i] = fmax(0, w->energy[n - i] + w->energy[n] - w->energy[i] - 2 * w->real[i]);
            sum += diff[i];
            v[i] = sum == 0 ? 1 : diff[i] * i / sum;
        }
        for (int i = min; i <= max; ++i)
            if (v[i] < .2) {
                peak = i;
                while (peak < max && v[peak + 1] < v[peak])
                    ++peak;
                break;
            }
    } else {
        for (int i = (method == AC_MCLEOD ? 0 : min); i <= max; ++i) {
            double a = w->energy[n - i], b = w->energy[n] - w->energy[i];
            double divisor = method == AC_MCLEOD ? a + b : sqrt(a * b);
            v[i] = divisor <= 0 ? 0 : (method == AC_MCLEOD ? 2 : 1) * w->real[i] / divisor;
        }
        int count = 0;
        if (method == AC_AUTOCORRELATION) {
            for (int i = min + 1; i < max; ++i)
                if (v[i] >= .6 && v[i] > v[i - 1] && v[i] >= v[i + 1])
                    w->peaks[count++] = i;
        } else {
            int i = 1;
            while (i <= max && v[i] > 0)
                ++i;
            while (i <= max) {
                while (i <= max && v[i] <= 0)
                    ++i;
                if (i > max)
                    break;
                int best = i;
                while (i <= max && v[i] > 0) {
                    if (v[i] > v[best])
                        best = i;
                    ++i;
                }
                if (best >= min)
                    w->peaks[count++] = best;
            }
        }
        double highest = 0;
        for (int i = 0; i < count; ++i)
            highest = fmax(highest, v[w->peaks[i]]);
        double cutoff = highest * (method == AC_MCLEOD ? .85 : .9);
        for (int i = 0; i < count; ++i)
            if (v[w->peaks[i]] >= cutoff && v[w->peaks[i]] >= .6) {
                peak = w->peaks[i];
                break;
            }
    }
    if (peak < 0)
        return 0;
    double tau = interpolate(method == AC_YIN ? diff : v, peak, max);
    if (tau <= 0)
        return 0;
    out->frequency_hz = rate / tau;
    out->clarity = (float)fmin(1, fmax(0, method == AC_YIN ? 1 - v[peak] : v[peak]));
    return 1;
}
