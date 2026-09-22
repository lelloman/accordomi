#ifndef AC_WAV_H
#define AC_WAV_H
#include <stdint.h>
#include <stdio.h>
typedef struct {
    FILE *file;
    int rate, channels, bits, format, align;
    uint64_t remaining;
} ac_wav;
int ac_wav_open(ac_wav *, const char *path, char *error, size_t error_size);
/* Selected zero-based channel. Returns frames or -1 on invalid/truncated data. */
int ac_wav_read(ac_wav *, int channel, float *out, int capacity);
void ac_wav_close(ac_wav *);
#endif
