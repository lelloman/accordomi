#include "internal.h"
#include <math.h>
#include <stdlib.h>
#include <string.h>
#define PI 3.14159265358979323846
#define MAX_B .02
#define FIT_TOLERANCE_CENTS 2.5
static double cents(double a, double b) {
    return 1200 * log(a / b) / log(2);
}
double ac_partial_hz(double f, double b, int n) {
    return n * f * sqrt((1 + b * n * n) / (1 + b));
}
const char *ac_piano_status_name(ac_piano_status s) {
    switch (s) {
    case AC_PIANO_OK:
        return "usable";
    case AC_PIANO_INVALID:
        return "invalid_input";
    case AC_PIANO_QUIET:
        return "quiet";
    case AC_PIANO_INSUFFICIENT_PARTIALS:
        return "insufficient_partials";
    default:
        return "poor_fit";
    }
}
/* Regression is linear in squared frequency/partial number. RANSAC pairs
   identify a consensus before a relative-error-weighted least-squares refit. */
ac_piano_status ac_fit_partials(const ac_partial *p, size_t count, ac_piano_result *r) {
    if (!r)
        return AC_PIANO_INVALID;
    memset(r, 0, sizeof(*r));
    r->status = AC_PIANO_INVALID;
    if (!p || count > AC_MAX_PARTIALS)
        return r->status;
    for (size_t i = 0; i < count; ++i) {
        if (p[i].number < 1 || p[i].number > AC_MAX_PARTIALS || !isfinite(p[i].frequency_hz) ||
            p[i].frequency_hz <= 0 || !isfinite(p[i].amplitude) || p[i].amplitude < 0)
            return r->status;
        for (size_t j = 0; j < i; ++j)
            if (p[i].number == p[j].number)
                return r->status;
    }
    r->partial_count = (int)count;
    memcpy(r->partials, p, count * sizeof(*p));
    for (size_t i = 0; i < count; ++i) {
        r->partials[i].used = 0;
        r->partials[i].predicted_hz = 0;
        r->partials[i].residual_cents = 0;
    }
    if (count < 4)
        return r->status = AC_PIANO_INSUFFICIENT_PARTIALS;
    int best_count = 0;
    double best_error = HUGE_VAL, best_a = 0, best_b = 0;
    for (size_t i = 0; i < count; ++i)
        for (size_t j = i + 1; j < count; ++j) {
            double x1 = p[i].number * p[i].number, x2 = p[j].number * p[j].number;
            double y1 = pow(p[i].frequency_hz / p[i].number, 2),
                   y2 = pow(p[j].frequency_hz / p[j].number, 2);
            double b = (y2 - y1) / (x2 - x1), a = y1 - b * x1;
            if (a <= 0 || b / a < -1e-5 || b / a > MAX_B)
                continue;
            if (b < 0) {
                b = 0;
                a = (y1 + y2) / 2;
            }
            int inliers = 0;
            double error = 0;
            for (size_t k = 0; k < count; ++k) {
                double residual =
                    cents(p[k].frequency_hz, p[k].number * sqrt(a + b * p[k].number * p[k].number));
                if (fabs(residual) <= FIT_TOLERANCE_CENTS) {
                    ++inliers;
                    error += residual * residual;
                }
            }
            if (inliers > best_count || (inliers == best_count && error < best_error)) {
                best_count = inliers;
                best_error = error;
                best_a = a;
                best_b = b;
            }
        }
    if (best_count < 4)
        return r->status = AC_PIANO_POOR_FIT;
    for (int iteration = 0; iteration < 3; ++iteration) {
        double sw = 0, sx = 0, sy = 0, sxx = 0, sxy = 0;
        for (size_t i = 0; i < count; ++i) {
            double x = p[i].number * p[i].number, y = pow(p[i].frequency_hz / p[i].number, 2);
            if (fabs(cents(p[i].frequency_hz, p[i].number * sqrt(best_a + best_b * x))) >
                FIT_TOLERANCE_CENTS)
                continue;
            double weight = 1 / (y * y);
            sw += weight;
            sx += weight * x;
            sy += weight * y;
            sxx += weight * x * x;
            sxy += weight * x * y;
        }
        double divisor = sw * sxx - sx * sx;
        if (divisor <= 0)
            break;
        double b = (sw * sxy - sx * sy) / divisor, a = (sy - b * sx) / sw;
        if (b < 0) {
            b = 0;
            a = sy / sw;
        }
        if (a <= 0 || b / a > MAX_B)
            break;
        best_a = a;
        best_b = b;
    }
    r->first_partial_hz = sqrt(best_a + best_b);
    r->inharmonicity = best_b / best_a;
    double error = 0;
    for (size_t i = 0; i < count; ++i) {
        ac_partial *part = &r->partials[i];
        part->predicted_hz = ac_partial_hz(r->first_partial_hz, r->inharmonicity, part->number);
        part->residual_cents = cents(part->frequency_hz, part->predicted_hz);
        part->used = fabs(part->residual_cents) <= FIT_TOLERANCE_CENTS;
        if (part->used) {
            ++r->used_count;
            error += part->residual_cents * part->residual_cents;
        }
    }
    if (r->used_count < 4 || r->used_count < .6 * count)
        return r->status = AC_PIANO_POOR_FIT;
    r->rms_cents = sqrt(error / r->used_count);
    r->quality =
        fmin(1, r->used_count / 8.0) * (double)r->used_count / count * exp(-r->rms_cents / 2);
    return r->status = AC_PIANO_OK;
}
static int nearest_peak(const ac_workspace *w, int count, double bin) {
    int lo = 0, hi = count;
    while (lo < hi) {
        int mid = (lo + hi) / 2;
        if (w->peaks[mid] < bin)
            lo = mid + 1;
        else
            hi = mid;
    }
    if (lo == count)
        return w->peaks[count - 1];
    if (lo > 0 && fabs(w->peaks[lo - 1] - bin) < fabs(w->peaks[lo] - bin))
        --lo;
    return w->peaks[lo];
}
ac_piano_status ac_analyze_piano(ac_workspace *w, const float *s, size_t n, int rate,
                                 double expected, ac_piano_result *r) {
    if (!r)
        return AC_PIANO_INVALID;
    memset(r, 0, sizeof(*r));
    r->status = AC_PIANO_INVALID;
    if (!w || !s || n < 2048 || n > w->capacity || rate < 8000 || rate > 192000 ||
        !isfinite(expected) || expected < 20 || expected >= rate / 8.0)
        return r->status;
    size_t fft = ac_fft_size(4 * n);
    double mean = 0, energy = 0;
    for (size_t i = 0; i < n; ++i) {
        if (!isfinite(s[i]))
            return r->status;
        mean += s[i];
    }
    mean /= n;
    memset(w->real, 0, fft * sizeof(double));
    memset(w->imag, 0, fft * sizeof(double));
    double window_sum = 0;
    for (size_t i = 0; i < n; ++i) {
        double x = s[i] - mean, window = .5 - .5 * cos(2 * PI * i / (n - 1));
        energy += x * x;
        w->real[i] = x * window;
        window_sum += window;
    }
    if (sqrt(energy / n) < .0001)
        return r->status = AC_PIANO_QUIET;
    ac_fft(w->real, w->imag, fft, 0);
    double maximum = 0;
    for (size_t i = 0; i <= fft / 2; ++i) {
        w->real[i] = hypot(w->real[i], w->imag[i]);
        maximum = fmax(maximum, w->real[i]);
    }
    int peaks = 0;
    /* Discard peaks >46 dB below the strongest component and those without
       local prominence. This is a quality gate, not calibrated microphone SNR. */
    for (size_t i = 2; i + 2 < fft / 2; ++i) {
        double amplitude = w->real[i];
        if (amplitude < maximum * .005 || amplitude <= w->real[i - 1] || amplitude < w->real[i + 1])
            continue;
        double floor = 0;
        int floor_count = 0;
        for (int d = 16; d <= 32; d += 4) {
            if (i > (size_t)d) {
                floor += w->real[i - d];
                ++floor_count;
            }
            if (i + d < fft / 2) {
                floor += w->real[i + d];
                ++floor_count;
            }
        }
        if (floor_count && amplitude < 5 * floor / floor_count)
            continue;
        if ((size_t)peaks < w->capacity)
            w->peaks[peaks++] = (int)i;
    }
    if (peaks < 4)
        return r->status = AC_PIANO_INSUFFICIENT_PARTIALS;
    double bin_hz = (double)rate / fft, best_score = -1;
    ac_partial candidates[AC_MAX_PARTIALS], best[AC_MAX_PARTIALS];
    int best_count = 0;
    for (int bi = 0; bi <= 160; ++bi) {
        double b = bi == 0 ? 0 : 1e-6 * pow(MAX_B / 1e-6, (bi - 1) / 159.0);
        for (int shift = -80; shift <= 80; shift += 10) {
            double f = expected * pow(2, shift / 1200.0), score = 0;
            int count = 0, last_peak = -1;
            for (int number = 1; number <= AC_MAX_PARTIALS; ++number) {
                double predicted = ac_partial_hz(f, b, number);
                if (predicted >= rate * .48)
                    break;
                int peak = nearest_peak(w, peaks, predicted / bin_hz);
                double tolerance = fmax(predicted * (pow(2, 15.0 / 1200) - 1), 1.5 * rate / n);
                if (peak == last_peak || fabs(peak * bin_hz - predicted) > tolerance)
                    continue;
                double left = log(fmax(w->real[peak - 1], 1e-30)), center = log(w->real[peak]),
                       right = log(fmax(w->real[peak + 1], 1e-30));
                double divisor = left - 2 * center + right;
                double delta = divisor == 0 ? 0 : .5 * (left - right) / divisor;
                double frequency = (peak + delta) * bin_hz,
                       amplitude = 2 * exp(center - .25 * (left - right) * delta) / window_sum;
                candidates[count++] = (ac_partial){number, frequency, amplitude, 0, 0, 0};
                score += 1 - .1 * fabs(frequency - predicted) / tolerance;
                last_peak = peak;
            }
            if (count >= 4 && score > best_score) {
                best_score = score;
                best_count = count;
                memcpy(best, candidates, count * sizeof(*best));
            }
        }
    }
    if (!best_count)
        return r->status = AC_PIANO_INSUFFICIENT_PARTIALS;
    ac_fit_partials(best, (size_t)best_count, r);
    if (r->status == AC_PIANO_OK && fabs(cents(r->first_partial_hz, expected)) > 80)
        r->status = AC_PIANO_POOR_FIT;
    return r->status;
}
struct ac_piano_stream {
    int rate;
    size_t window, hop, filled;
    uint64_t start;
    double expected;
    float *samples;
    ac_workspace *workspace;
};
ac_piano_stream *ac_piano_stream_create(int rate, size_t window, size_t hop, double expected) {
    if (window < 2048 || window > 1048576 || hop < 1 || hop > window || rate < 8000 ||
        rate > 192000 || !isfinite(expected) || expected < 20 || expected >= rate / 8.0)
        return NULL;
    ac_piano_stream *s = calloc(1, sizeof(*s));
    if (!s)
        return NULL;
    s->rate = rate;
    s->window = window;
    s->hop = hop;
    s->expected = expected;
    s->samples = calloc(window, sizeof(float));
    s->workspace = ac_workspace_create(window);
    if (!s->samples || !s->workspace) {
        ac_piano_stream_destroy(s);
        return NULL;
    }
    return s;
}
void ac_piano_stream_destroy(ac_piano_stream *s) {
    if (s) {
        ac_workspace_destroy(s->workspace);
        free(s->samples);
        free(s);
    }
}
void ac_piano_stream_reset(ac_piano_stream *s) {
    if (s) {
        s->filled = 0;
        s->start = 0;
    }
}
int ac_piano_stream_push(ac_piano_stream *s, const float *samples, size_t count,
                         ac_piano_callback callback, void *user) {
    if (!s || (!samples && count) || !callback)
        return 0;
    while (count) {
        size_t take = s->window - s->filled;
        if (take > count)
            take = count;
        memcpy(s->samples + s->filled, samples, take * sizeof(float));
        s->filled += take;
        samples += take;
        count -= take;
        if (s->filled == s->window) {
            ac_piano_result result;
            ac_analyze_piano(s->workspace, s->samples, s->window, s->rate, s->expected, &result);
            callback(user, s->start, &result);
            memmove(s->samples, s->samples + s->hop, (s->window - s->hop) * sizeof(float));
            s->filled -= s->hop;
            s->start += s->hop;
        }
    }
    return 1;
}
