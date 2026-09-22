#define _POSIX_C_SOURCE 200809L
#include "accordomi.h"
#include "internal.h"
#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <time.h>

static double now(void) {
    struct timespec time;
    if (clock_gettime(CLOCK_MONOTONIC, &time))
        exit(2);
    return time.tv_sec + time.tv_nsec * 1e-9;
}
static int compare(const void *a, const void *b) {
    double x = *(const double *)a, y = *(const double *)b;
    return (x > y) - (x < y);
}
int main(void) {
    const size_t count = 4096;
    float samples[4096];
    const double amplitudes[] = {.45, .3, .15, .1};
    for (size_t i = 0; i < count; ++i) {
        double value = 0;
        for (int harmonic = 1; harmonic <= 4; ++harmonic)
            value += amplitudes[harmonic - 1] *
                     sin(2 * 3.14159265358979323846 * 110 * harmonic * i / 44100);
        samples[i] = (float)value;
    }
    ac_workspace *workspace = ac_workspace_create(count);
    if (!workspace)
        return 2;
    const char *names[] = {"correlation", "yin", "autocorrelation", "mcleod"};
    for (int test = 0; test < 4; ++test) {
        double timings[101], checksum = 0;
        for (int batch = -10; batch < 101; ++batch) {
            double start = now();
            for (int iteration = 0; iteration < 10; ++iteration) {
                if (!test) {
                    if (!ac_prepare_correlation(workspace, samples, count))
                        return 3;
                    checksum += workspace->real[1];
                } else {
                    ac_pitch result;
                    if (!ac_detect(workspace, samples, count, 44100, (ac_method)(test - 1),
                                   &result))
                        return 3;
                    checksum += result.frequency_hz;
                }
            }
            if (batch >= 0)
                timings[batch] = (now() - start) * 1e6 / 10;
        }
        qsort(timings, 101, sizeof(double), compare);
        printf("%s median_us=%.3f p95_us=%.3f checksum=%.6f\n", names[test], timings[50],
               timings[95], checksum);
    }
    ac_workspace_destroy(workspace);
    return 0;
}
