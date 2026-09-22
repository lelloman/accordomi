#ifndef ACCORDOMI_H
#define ACCORDOMI_H
#include <stddef.h>
#include <stdint.h>
#ifdef __cplusplus
extern "C" {
#endif
#define AC_VERSION "0.1.0"
#define AC_MAX_PARTIALS 24
/* Opaque, reusable scratch storage. One owner/thread per workspace. */
typedef struct ac_workspace ac_workspace;
ac_workspace *ac_workspace_create(size_t capacity);
void ac_workspace_destroy(ac_workspace *workspace);
/* For managed callers with aligned, nonmoving memory (e.g. JNI direct buffers).
   Never pass an initialized caller-owned workspace to ac_workspace_destroy. */
size_t ac_workspace_bytes(size_t capacity);
ac_workspace *ac_workspace_init(void *memory, size_t bytes, size_t capacity);
typedef enum { AC_YIN = 0, AC_AUTOCORRELATION = 1, AC_MCLEOD = 2 } ac_method;
typedef struct {
    double frequency_hz;
    float clarity;
} ac_pitch;
/* Returns 1 for a reading, 0 for rejected/invalid input. No allocation. */
int ac_detect(ac_workspace *, const float *, size_t count, int sample_rate, ac_method, ac_pitch *);
/* Linear autocorrelation, centered input. Arrays have count entries. */
int ac_correlate(ac_workspace *, const float *, size_t count, double *correlation, double *energy_a,
                 double *energy_b);
double ac_equal_tempered_hz(int midi, double reference_hz);
int ac_tuning_reading(double hz, double reference_hz, int *midi, double *target, double *cents);
typedef struct {
    ac_pitch previous, pending;
    int has_previous, has_pending, missing;
} ac_stabilizer;
void ac_stabilizer_reset(ac_stabilizer *);
int ac_stabilizer_update(ac_stabilizer *, const ac_pitch *input, ac_pitch *output);
typedef struct {
    double phase, gain;
} ac_oscillator;
int ac_oscillator_fill(ac_oscillator *, double hz, int sample_rate, int releasing, int16_t *,
                       size_t count);

typedef enum {
    AC_PIANO_OK = 0,
    AC_PIANO_INVALID,
    AC_PIANO_QUIET,
    AC_PIANO_INSUFFICIENT_PARTIALS,
    AC_PIANO_POOR_FIT
} ac_piano_status;
typedef struct {
    int number;
    double frequency_hz, amplitude, predicted_hz, residual_cents;
    int used;
} ac_partial;
typedef struct {
    ac_piano_status status;
    double first_partial_hz, inharmonicity, rms_cents, quality;
    int partial_count, used_count;
    ac_partial partials[AC_MAX_PARTIALS];
} ac_piano_result;
const char *ac_piano_status_name(ac_piano_status);
/* F1 is the actual first partial: P(n)=n*F1*sqrt((1+B*n*n)/(1+B)). */
double ac_partial_hz(double first_partial_hz, double b, int number);
/* Robust fit of numbered observations; >=4 inliers required. Quality is a
   heuristic, NOT a probability or a calibrated uncertainty interval. */
ac_piano_status ac_fit_partials(const ac_partial *, size_t count, ac_piano_result *);
/* Known-note analysis. Search F1 within +/-80 cents of expected_hz and B
   within [0,.02]. Hann window, padded/interpolated spectral peaks. */
ac_piano_status ac_analyze_piano(ac_workspace *, const float *, size_t count, int sample_rate,
                                 double expected_hz, ac_piano_result *);
/* Fixed window/hop streaming adapter; callback runs synchronously. No padding
   of incomplete windows. Reset between notes/recordings. No processing allocation. */
typedef struct ac_piano_stream ac_piano_stream;
typedef void (*ac_piano_callback)(void *user, uint64_t start_sample, const ac_piano_result *);
ac_piano_stream *ac_piano_stream_create(int sample_rate, size_t window, size_t hop,
                                        double expected_hz);
void ac_piano_stream_reset(ac_piano_stream *);
void ac_piano_stream_destroy(ac_piano_stream *);
int ac_piano_stream_push(ac_piano_stream *, const float *, size_t count, ac_piano_callback,
                         void *user);
#ifdef __cplusplus
}
#endif
#endif
