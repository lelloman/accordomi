#include "wav.h"
#include <math.h>
#include <string.h>
static uint32_t u32(const unsigned char *p) {
    return (uint32_t)p[0] | ((uint32_t)p[1] << 8) | ((uint32_t)p[2] << 16) | ((uint32_t)p[3] << 24);
}
static unsigned u16(const unsigned char *p) {
    return p[0] | ((unsigned)p[1] << 8);
}
void ac_wav_close(ac_wav *w) {
    if (w->file)
        fclose(w->file);
    w->file = NULL;
}
int ac_wav_open(ac_wav *w, const char *path, char *error, size_t error_size) {
    memset(w, 0, sizeof(*w));
    w->file = fopen(path, "rb");
    if (!w->file) {
        snprintf(error, error_size, "Cannot open WAV: %s", path);
        return 0;
    }
    unsigned char h[16];
    long data_offset = 0;
    uint32_t data_size = 0;
    if (fseek(w->file, 0, SEEK_END) != 0)
        goto invalid;
    long file_size = ftell(w->file);
    rewind(w->file);
    if (fread(h, 1, 12, w->file) != 12 || memcmp(h, "RIFF", 4) || memcmp(h + 8, "WAVE", 4))
        goto invalid;
    uint64_t end = (uint64_t)u32(h + 4) + 8;
    if (file_size < 0 || end > (uint64_t)file_size || end < 12)
        goto invalid;
    while ((uint64_t)ftell(w->file) + 8 <= end) {
        if (fread(h, 1, 8, w->file) != 8)
            goto invalid;
        uint32_t size = u32(h + 4);
        long offset = ftell(w->file);
        uint64_t next = (uint64_t)offset + size + (size & 1);
        if (next > end)
            goto invalid;
        if (!memcmp(h, "fmt ", 4)) {
            if (size < 16 || fread(h, 1, 16, w->file) != 16)
                goto invalid;
            w->format = (int)u16(h);
            w->channels = (int)u16(h + 2);
            w->rate = (int)u32(h + 4);
            w->align = (int)u16(h + 12);
            w->bits = (int)u16(h + 14);
        } else if (!memcmp(h, "data", 4) && !data_offset) {
            data_offset = offset;
            data_size = size;
        }
        if (fseek(w->file, (long)next, SEEK_SET) != 0)
            goto invalid;
    }
    if (!data_offset || !data_size || w->channels < 1 || w->channels > 32 || w->rate < 8000 ||
        w->rate > 192000 || w->align != w->channels * (w->bits / 8) || w->align < 1 ||
        data_size % (unsigned)w->align ||
        !((w->format == 1 && (w->bits == 16 || w->bits == 24 || w->bits == 32)) ||
          (w->format == 3 && w->bits == 32)))
        goto invalid;
    if (fseek(w->file, data_offset, SEEK_SET) != 0)
        goto invalid;
    w->remaining = data_size / (unsigned)w->align;
    return 1;
invalid:
    snprintf(error, error_size,
             "Invalid/truncated WAV or unsupported encoding (use RIFF PCM16/24/32 or float32, "
             "8–192 kHz)");
    ac_wav_close(w);
    return 0;
}
int ac_wav_read(ac_wav *w, int channel, float *out, int capacity) {
    if (!w || !w->file || channel < 0 || channel >= w->channels || !out || capacity < 0)
        return -1;
    int count = 0;
    unsigned char frame[128];
    while (count < capacity && w->remaining) {
        if (fread(frame, 1, (size_t)w->align, w->file) != (size_t)w->align)
            return -1;
        const unsigned char *p = frame + channel * (w->bits / 8);
        double value;
        if (w->format == 3) {
            uint32_t raw = u32(p);
            float f;
            memcpy(&f, &raw, 4);
            value = f;
        } else if (w->bits == 16) {
            unsigned raw = u16(p);
            value = (raw >= 32768 ? (double)raw - 65536 : raw) / 32768;
        } else if (w->bits == 24) {
            uint32_t raw = (uint32_t)p[0] | ((uint32_t)p[1] << 8) | ((uint32_t)p[2] << 16);
            value = (raw >= 8388608 ? (double)raw - 16777216 : raw) / 8388608;
        } else {
            uint32_t raw = u32(p);
            value = (raw >= 2147483648u ? (double)raw - 4294967296.0 : raw) / 2147483648.0;
        }
        if (!isfinite(value))
            return -1;
        out[count++] = (float)value;
        --w->remaining;
    }
    return count;
}
