#include "accordomi.h"
#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#define PI 3.14159265358979323846
#define CHECK(x)                                                                                   \
    do {                                                                                           \
        if (!(x)) {                                                                                \
            fprintf(stderr, "FAIL %s:%d: %s\n", __FILE__, __LINE__, #x);                           \
            exit(1);                                                                               \
        }                                                                                          \
    } while (0)
static void detector_tests(void) {
    float samples[4096];
    ac_workspace *w = ac_workspace_create(4096);
    CHECK(w);
    for (int method = 0; method < 3; ++method)
        for (int reference = 400; reference <= 480; reference += 40)
            for (int midi = 21; midi <= 108; ++midi) {
                double f = ac_equal_tempered_hz(midi, reference);
                for (int i = 0; i < 4096; ++i)
                    samples[i] = (float)(.5 * sin(2 * PI * f * i / 44100));
                ac_pitch p;
                CHECK(ac_detect(w, samples, 4096, 44100, (ac_method)method, &p));
                CHECK(fabs(1200 * log2(p.frequency_hz / f)) < (reference == 440 ? 1 : 2));
            }
    memset(samples, 0, sizeof(samples));
    ac_pitch p;
    for (int method = 0; method < 3; ++method) {
        CHECK(!ac_detect(w, samples, 4096, 44100, (ac_method)method, &p));
    }
    samples[0] = NAN;
    CHECK(!ac_detect(w, samples, 4096, 44100, AC_YIN, &p));
    ac_workspace_destroy(w);
}
static void correlation_plan_tests(void) {
    const size_t sizes[] = {513, 4096, 1025, 8192, 513, 4096};
    ac_workspace *workspace = ac_workspace_create(8192);
    float *samples = malloc(8192 * sizeof(float));
    double *results = malloc(3 * 8192 * sizeof(double));
    CHECK(workspace && samples && results);
    for (size_t test = 0; test < sizeof(sizes) / sizeof(sizes[0]); ++test) {
        size_t n = sizes[test];
        double mean = 0;
        for (size_t i = 0; i < n; ++i) {
            samples[i] = (float)(.4 + sin(i * .17) + .2 * cos(i * .51));
            mean += samples[i];
        }
        mean /= n;
        const size_t lags[] = {0, 1, 10, n / 2, n - 1};
        for (int pass = 0; pass < 2; ++pass) {
            CHECK(ac_correlate(workspace, samples, n, results, results + n, results + 2 * n));
            for (size_t test_lag = 0; test_lag < 5; ++test_lag) {
                size_t lag = lags[test_lag];
                double correlation = 0, energy_a = 0, energy_b = 0;
                for (size_t i = 0; i < n - lag; ++i) {
                    double a = samples[i] - mean, b = samples[i + lag] - mean;
                    correlation += a * b;
                    energy_a += a * a;
                    energy_b += b * b;
                }
                CHECK(fabs(results[lag] - correlation) < 1e-8);
                CHECK(fabs(results[n + lag] - energy_a) < 1e-8);
                CHECK(fabs(results[2 * n + lag] - energy_b) < 1e-8);
            }
        }
    }
    free(results);
    free(samples);
    ac_workspace_destroy(workspace);
}
static void fit_tests(void) {
    ac_partial p[12] = {0};
    ac_piano_result r;
    for (int i = 0; i < 12; ++i) {
        p[i].number = i + 2;
        p[i].frequency_hz = ac_partial_hz(110, .0004, i + 2);
        p[i].amplitude = 1.0 / (i + 2);
    }
    p[4].frequency_hz *= 1.02;
    CHECK(ac_fit_partials(p, 12, &r) == AC_PIANO_OK);
    CHECK(fabs(r.first_partial_hz - 110) < 1e-7);
    CHECK(fabs(r.inharmonicity - .0004) < 1e-10);
    CHECK(!r.partials[4].used && r.used_count == 11);
    CHECK(ac_fit_partials(p, 3, &r) == AC_PIANO_INSUFFICIENT_PARTIALS);
    p[3].number = p[2].number;
    CHECK(ac_fit_partials(p, 12, &r) == AC_PIANO_INVALID);
}
static unsigned rng = 37;
static double noise(void) {
    rng = 1664525 * rng + 1013904223;
    return (double)rng / 4294967296.0 - .5;
}
static void signal(float *s, size_t n, int rate, double f, double b, int missing) {
    for (size_t i = 0; i < n; ++i) {
        double value = .0002 * noise(), time = (double)i / rate;
        for (int k = missing ? 2 : 1; k <= 12; ++k) {
            double partial = ac_partial_hz(f, b, k);
            if (partial > rate * .48)
                break;
            value += .18 / k * exp(-time * (.5 + k * .04)) * sin(2 * PI * partial * time + k * .7);
        }
        s[i] = (float)value;
    }
}
static int callbacks;
static ac_piano_result streamed;
static void receive(void *user, uint64_t start, const ac_piano_result *r) {
    (void)user;
    CHECK(start == (uint64_t)callbacks * 16384);
    ++callbacks;
    streamed = *r;
}
static void piano_tests(void) {
    size_t n = 65536;
    float *s = calloc(n, sizeof(float));
    ac_workspace *w = ac_workspace_create(n);
    CHECK(s && w);
    const double frequencies[] = {27.5, 110, 261.625565, 440, 1760, 4186.009};
    for (int rate = 44100; rate <= 48000; rate += 3900)
        for (int j = 0; j < 6; ++j)
            for (int missing = 0; missing < 2; ++missing) {
                double b = j == 5 ? .001 : .0002;
                double f = frequencies[j] * pow(2, 17.0 / 1200);
                signal(s, n, rate, f, b, missing);
                ac_piano_result r;
                ac_analyze_piano(w, s, n, rate, frequencies[j], &r);
                // At 44.1 kHz this C8 fixture has only three observable partials
                // when its fundamental is missing: reject instead of inventing B.
                if (j == 5 && missing && rate == 44100) {
                    CHECK(r.status == AC_PIANO_INSUFFICIENT_PARTIALS);
                    continue;
                }
                if (r.status != AC_PIANO_OK)
                    fprintf(stderr, "piano f=%g rate=%d missing=%d status=%s\n", f, rate, missing,
                            ac_piano_status_name(r.status));
                CHECK(r.status == AC_PIANO_OK);
                CHECK(fabs(1200 * log2(r.first_partial_hz / f)) < .3);
                CHECK(fabs(r.inharmonicity - b) < .00001);
            }
    const double stiffnesses[] = {0, 1e-5, .001, .005, .015};
    for (int i = 0; i < 5; ++i) {
        signal(s, n, 44100, 110, stiffnesses[i], 0);
        ac_piano_result r;
        CHECK(ac_analyze_piano(w, s, n, 44100, 110, &r) == AC_PIANO_OK);
        CHECK(fabs(r.inharmonicity - stiffnesses[i]) < 1e-5);
    }
    signal(s, n, 44100, 110, .0004, 1);
    ac_piano_result direct;
    ac_analyze_piano(w, s, n, 44100, 110, &direct);
    ac_piano_stream *stream = ac_piano_stream_create(44100, n, 16384, 110);
    CHECK(stream);
    for (size_t i = 0; i < n;) {
        size_t count = n - i < 317 ? n - i : 317;
        CHECK(ac_piano_stream_push(stream, s + i, count, receive, NULL));
        i += count;
    }
    CHECK(callbacks == 1);
    CHECK(streamed.first_partial_hz == direct.first_partial_hz);
    CHECK(streamed.inharmonicity == direct.inharmonicity);
    ac_piano_stream_reset(stream);
    callbacks = 0;
    CHECK(ac_piano_stream_push(stream, s, n - 1, receive, NULL));
    CHECK(callbacks == 0);
    ac_piano_stream_destroy(stream);
    memset(s, 0, n * sizeof(float));
    CHECK(ac_analyze_piano(w, s, n, 44100, 110, &direct) == AC_PIANO_QUIET);
    for (size_t i = 0; i < n; ++i)
        s[i] = (float)(.5 * sin(2 * PI * 440 * i / 44100));
    CHECK(ac_analyze_piano(w, s, n, 44100, 440, &direct) != AC_PIANO_OK);
    for (size_t i = 0; i < n; ++i)
        s[i] = (float)noise();
    CHECK(ac_analyze_piano(w, s, n, 44100, 110, &direct) != AC_PIANO_OK);
    ac_workspace_destroy(w);
    free(s);
}
static void piano_model_tests(void) {
    int midi[] = {21, 33, 45, 57, 60, 69, 81, 88, 93};
    double b[9] = {0}, interpolated[88], targets[88];
    CHECK(ac_piano_targets(midi, b, 9, 440, interpolated, targets));
    for (int i = 0; i < 88; ++i)
        CHECK(fabs(targets[i] - ac_equal_tempered_hz(i + 21, 440)) < 1e-8);
    for (int i = 0; i < 9; ++i)
        b[i] = .0003;
    CHECK(ac_piano_targets(midi, b, 9, 442, interpolated, targets));
    CHECK(fabs(targets[48] - 442) < 1e-10);
    CHECK(targets[0] < ac_equal_tempered_hz(21, 442));
    CHECK(targets[87] > ac_equal_tempered_hz(108, 442));
    double before[88];
    memcpy(before, targets, sizeof(before));
    for (int i = 0; i < 9; ++i)
        b[i] = .0006;
    CHECK(ac_piano_targets(midi, b, 9, 442, interpolated, targets));
    CHECK(targets[87] > before[87] && targets[0] < before[0]);
    for (int i = 1; i < 88; ++i)
        CHECK(targets[i] > targets[i - 1]);
    CHECK(!ac_piano_targets(midi, b, 3, 440, interpolated, targets));
    b[0] = NAN;
    CHECK(!ac_piano_targets(midi, b, 9, 440, interpolated, targets));
    double hz[] = {110, 110.01, 110.005}, stiffness[] = {.0003, .0003001, .0003002},
           quality[] = {.8, .9, .8}, summary[3];
    CHECK(ac_calibration_summary(hz, stiffness, quality, 3, summary));
    stiffness[2] = .003;
    CHECK(!ac_calibration_summary(hz, stiffness, quality, 3, summary));
    double frequencies[9], seconds[9];
    for (int i = 0; i < 9; ++i) {
        seconds[i] = i * .4;
        frequencies[i] = 440;
    }
    CHECK(ac_pitch_stability(frequencies, seconds, 9) == 1);
    for (int i = 0; i < 9; ++i)
        frequencies[i] = 440 * exp2(i / 1200.0);
    CHECK(ac_pitch_stability(frequencies, seconds, 9) == 2);
    CHECK(ac_pitch_stability(frequencies, seconds, 3) == 0);
    ac_workspace *w = ac_workspace_create(65536);
    float *samples = malloc(65536 * sizeof(float));
    CHECK(w && samples);
    signal(samples, 65536, 44100, 110 * exp2(23.0 / 1200), .0003, 1);
    ac_piano_result result;
    CHECK(ac_measure_piano(w, samples, 65536, 44100, 110, .0003, &result) == AC_PIANO_OK);
    CHECK(fabs(ac_cents_between(result.first_partial_hz, 110) - 23) < .3);
    /* With B already calibrated, a single treble partial is measurable, but low quality. */
    for (int i = 0; i < 65536; ++i)
        samples[i] = (float)(.4 * sin(2 * PI * 4186.009 * i / 44100));
    CHECK(ac_measure_piano(w, samples, 65536, 44100, 4186.009, .001, &result) == AC_PIANO_OK);
    CHECK(result.quality <= .25 && result.used_count == 1);
    CHECK(fabs(result.first_partial_hz - 4186.009) < .1);
    ac_workspace_destroy(w);
    free(samples);
}
int main(void) {
    detector_tests();
    correlation_plan_tests();
    fit_tests();
    piano_tests();
    piano_model_tests();
    puts("Native engine tests passed");
    return 0;
}
