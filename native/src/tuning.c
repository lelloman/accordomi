#include "accordomi.h"
#include <math.h>
#include <string.h>
#define PI 3.14159265358979323846
double ac_equal_tempered_hz(int midi, double reference) {
    return reference * pow(2, ((double)midi - 69) / 12);
}
int ac_tuning_reading(double hz, double reference, int *midi, double *target, double *cents) {
    if (!midi || !target || !cents || !isfinite(hz) || hz <= 0 || !isfinite(reference) ||
        reference <= 0)
        return 0;
    double semitones = floor(12 * (log(hz) - log(reference)) / log(2) + .5);
    *midi = 69 + (int)semitones;
    *target = ac_equal_tempered_hz(*midi, reference);
    *cents = 1200 * log(hz / *target) / log(2);
    return 1;
}
void ac_stabilizer_reset(ac_stabilizer *s) {
    if (s)
        memset(s, 0, sizeof(*s));
}
int ac_stabilizer_update(ac_stabilizer *s, const ac_pitch *input, ac_pitch *out) {
    if (!s || !out)
        return 0;
    if (!input) {
        s->has_pending = 0;
        if (!s->has_previous)
            return 0;
        if (++s->missing > 8) {
            ac_stabilizer_reset(s);
            return 0;
        }
        *out = s->previous;
        return 1;
    }
    s->missing = 0;
    if (!s->has_previous) {
        s->previous = *input;
        s->has_previous = 1;
    } else if (fabs(1200 * log(input->frequency_hz / s->previous.frequency_hz) / log(2)) >= 50) {
        int agrees = s->has_pending &&
                     fabs(1200 * log(input->frequency_hz / s->pending.frequency_hz) / log(2)) <= 35;
        s->has_pending = !agrees;
        if (agrees)
            s->previous = *input;
        else
            s->pending = *input;
    } else {
        s->has_pending = 0;
        s->previous.frequency_hz *= pow(input->frequency_hz / s->previous.frequency_hz, .35);
        s->previous.clarity += (input->clarity - s->previous.clarity) * .35f;
    }
    *out = s->previous;
    return 1;
}
int ac_oscillator_fill(ac_oscillator *s, double hz, int rate, int releasing, int16_t *out,
                       size_t count) {
    if (!s || !out || !isfinite(s->phase) || !isfinite(s->gain) || !isfinite(hz) || hz <= 0 ||
        rate <= 0 || hz >= rate / 2.0)
        return 0;
    double step = 2 * PI * hz / rate, gain_step = 1 / (rate * .01);
    for (size_t i = 0; i < count; ++i) {
        s->gain = fmin(1, fmax(0, s->gain + (releasing ? -gain_step : gain_step)));
        out[i] = (int16_t)(sin(s->phase) * s->gain * .2 * 32767);
        s->phase = fmod(s->phase + step, 2 * PI);
    }
    return 1;
}
