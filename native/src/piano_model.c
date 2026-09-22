#include "accordomi.h"
#include <math.h>
#include <stdlib.h>
#include <string.h>
#define NOTES 88
#define ANCHOR (69 - 21)

double ac_cents_between(double actual, double target) {
    return 1200 * log2(actual / target);
}
static int compare_double(const void *a, const void *b) {
    double x = *(const double *)a, y = *(const double *)b;
    return (x > y) - (x < y);
}
int ac_calibration_summary(const double *hz, const double *b, const double *quality, size_t count,
                           double *output) {
    if (!hz || !b || !quality || !output || count < 2 || count > 32)
        return 0;
    double frequencies[32], stiffness[32], minimum_quality = 1;
    for (size_t i = 0; i < count; ++i) {
        if (!isfinite(hz[i]) || hz[i] <= 0 || !isfinite(b[i]) || b[i] < 0 || b[i] > .02 ||
            !isfinite(quality[i]) || quality[i] < .45 || quality[i] > 1)
            return 0;
        frequencies[i] = hz[i];
        stiffness[i] = b[i];
        minimum_quality = fmin(minimum_quality, quality[i]);
    }
    qsort(frequencies, count, sizeof(double), compare_double);
    qsort(stiffness, count, sizeof(double), compare_double);
    /* Compare stiffness in its audible effect at partial 6, not percentage B
       (which becomes ill-conditioned as B approaches zero). */
    double b_spread = ac_cents_between(ac_partial_hz(1, stiffness[count - 1], 6),
                                       ac_partial_hz(1, stiffness[0], 6));
    if (ac_cents_between(frequencies[count - 1], frequencies[0]) > 3 || b_spread > 2)
        return 0;
    output[0] = (frequencies[(count - 1) / 2] + frequencies[count / 2]) / 2;
    output[1] = (stiffness[(count - 1) / 2] + stiffness[count / 2]) / 2;
    output[2] = minimum_quality;
    return 1;
}
/* Weighted linear least squares in cents; no zero-beat fifth/fourth objective.
   A4 is removed from all equations to enforce its first partial exactly. */
static void equation(double matrix[NOTES][NOTES + 1], const int *index, const double *coefficient,
                     int count, double target, double weight) {
    for (int i = 0; i < count; ++i) {
        if (index[i] == ANCHOR)
            continue;
        matrix[index[i]][NOTES] += weight * coefficient[i] * target;
        for (int j = 0; j < count; ++j)
            if (index[j] != ANCHOR)
                matrix[index[i]][index[j]] += weight * coefficient[i] * coefficient[j];
    }
}
int ac_piano_targets(const int *midi, const double *measured_b, size_t count, double reference,
                     double *interpolated_b, double *targets) {
    if (!midi || !measured_b || !interpolated_b || !targets || count < 4 || count > NOTES ||
        !isfinite(reference) || reference < 400 || reference > 480)
        return 0;
    for (size_t i = 0; i < count; ++i)
        if (midi[i] < 21 || midi[i] > 108 || (i && midi[i] <= midi[i - 1]) ||
            !isfinite(measured_b[i]) || measured_b[i] < 0 || measured_b[i] > .02)
            return 0;
    if (midi[0] > 33 || midi[count - 1] < 88)
        return 0;
    /* Log(B+epsilon) interpolation; hold endpoints rather than inventing an
       extrapolated trend. UI explicitly labels unmeasured and end-region notes. */
    for (int note = 21; note <= 108; ++note) {
        size_t upper = 0;
        while (upper < count && midi[upper] < note)
            ++upper;
        double b;
        if (!upper)
            b = measured_b[0];
        else if (upper == count)
            b = measured_b[count - 1];
        else {
            double t = (double)(note - midi[upper - 1]) / (midi[upper] - midi[upper - 1]);
            b = exp((1 - t) * log(measured_b[upper - 1] + 1e-8) +
                    t * log(measured_b[upper] + 1e-8)) -
                1e-8;
        }
        interpolated_b[note - 21] = fmax(0, b);
    }
    double(*matrix)[NOTES + 1] = calloc(NOTES, sizeof(*matrix));
    if (!matrix)
        return 0;
    for (int i = 0; i < NOTES; ++i) {
        int index[] = {i};
        double coefficient[] = {1};
        /* Weak ET prior everywhere, stronger across the temperament register. */
        equation(matrix, index, coefficient, 1, 0, i + 21 >= 48 && i + 21 <= 72 ? .08 : .002);
        if (i && i < NOTES - 1) {
            int adjacent[] = {i - 1, i, i + 1};
            double smooth[] = {1, -2, 1};
            equation(matrix, adjacent, smooth, 3, 0, 2);
        }
        if (i + 12 < NOTES) {
            int octave[] = {i, i + 12};
            double difference[] = {-1, 1};
            for (int type = 1; type <= 2; ++type) {
                double ratio = ac_partial_hz(1, interpolated_b[i], 2 * type) /
                               ac_partial_hz(1, interpolated_b[i + 12], type);
                double stretch = ac_cents_between(ratio, 2);
                double weight = type == 1 ? 1 : (i + 21 < 60 ? 1 : .35);
                equation(matrix, octave, difference, 2, stretch, weight);
            }
        }
    }
    matrix[ANCHOR][ANCHOR] = 1;
    for (int pivot = 0; pivot < NOTES; ++pivot) {
        double divisor = matrix[pivot][pivot];
        if (!isfinite(divisor) || divisor <= 1e-12) {
            free(matrix);
            return 0;
        }
        for (int j = pivot; j <= NOTES; ++j)
            matrix[pivot][j] /= divisor;
        for (int i = pivot + 1; i < NOTES; ++i) {
            double factor = matrix[i][pivot];
            for (int j = pivot; j <= NOTES; ++j)
                matrix[i][j] -= factor * matrix[pivot][j];
        }
    }
    double offsets[NOTES];
    for (int i = NOTES - 1; i >= 0; --i) {
        offsets[i] = matrix[i][NOTES];
        for (int j = i + 1; j < NOTES; ++j)
            offsets[i] -= matrix[i][j] * offsets[j];
        if (!isfinite(offsets[i]) || fabs(offsets[i]) > 100) {
            free(matrix);
            return 0;
        }
    }
    free(matrix);
    for (int i = 0; i < NOTES; ++i) {
        targets[i] = ac_equal_tempered_hz(i + 21, reference) * exp2(offsets[i] / 1200);
        if (i && targets[i] <= targets[i - 1])
            return 0;
    }
    return 1;
}
int ac_pitch_stability(const double *hz, const double *seconds, size_t count) {
    if (!hz || !seconds || count < 5 || count > 32 || seconds[count - 1] - seconds[0] < 2)
        return 0;
    double sx = 0, sy = 0, sxx = 0, sxy = 0, minimum = 1e9, maximum = -1e9;
    for (size_t i = 0; i < count; ++i) {
        if (!isfinite(hz[i]) || hz[i] <= 0 || !isfinite(seconds[i]) ||
            (i && seconds[i] <= seconds[i - 1]))
            return 0;
        double x = seconds[i] - seconds[0], y = ac_cents_between(hz[i], hz[0]);
        sx += x;
        sy += y;
        sxx += x * x;
        sxy += x * y;
        minimum = fmin(minimum, y);
        maximum = fmax(maximum, y);
    }
    double slope = (count * sxy - sx * sy) / (count * sxx - sx * sx);
    if (fabs(slope) > .5 || maximum - minimum > 3)
        return 2;
    return maximum - minimum <= 1.5 ? 1 : 0;
}
