#include "accordomi.h"
#include "wav.h"
#include <errno.h>
#include <math.h>
#include <stdlib.h>
#include <string.h>
typedef struct {
    const char *path, *method;
    int midi, rate, csv, channel;
    size_t window, hop;
    double reference;
    int frames, usable;
} output_config;
static void json_string(const char *s) {
    putchar('"');
    for (; *s; ++s) {
        unsigned char c = (unsigned char)*s;
        if (c == '"' || c == '\\')
            printf("\\%c", c);
        else if (c < 32)
            printf("\\u%04x", c);
        else
            putchar(c);
    }
    putchar('"');
}
static void metadata(const output_config *o, uint64_t start) {
    printf("{\"engine_version\":\"%s\",\"source\":", AC_VERSION);
    json_string(o->path);
    printf(
        ",\"method\":\"%s\",\"midi\":%d,\"reference_hz\":%.12g,\"sample_rate\":%d,\"channel\":%d,"
        "\"window_samples\":%zu,\"hop_samples\":%zu,\"start_sample\":%llu,\"time_seconds\":%.9g",
        o->method, o->midi, o->reference, o->rate, o->channel, o->window, o->hop,
        (unsigned long long)start, (double)start / o->rate);
}
static void piano_output(void *user, uint64_t start, const ac_piano_result *r) {
    output_config *o = user;
    ++o->frames;
    o->usable += r->status == AC_PIANO_OK;
    if (o->csv) {
        for (int i = 0; i < (r->partial_count ? r->partial_count : 1); ++i) {
            const ac_partial empty = {0};
            const ac_partial *p = r->partial_count ? &r->partials[i] : &empty;
            printf("%llu,%.9g,%d,%d,%s,%.12g,%.12g,%.9g,%.9g,%d,%.12g,%.9g,%.12g,%.9g,%d\n",
                   (unsigned long long)start, (double)start / o->rate, o->rate, o->midi,
                   ac_piano_status_name(r->status), r->first_partial_hz, r->inharmonicity,
                   r->rms_cents, r->quality, p->number, p->frequency_hz, p->amplitude,
                   p->predicted_hz, p->residual_cents, p->used);
        }
        return;
    }
    metadata(o, start);
    printf(",\"status\":\"%s\",\"first_partial_hz\":%.12g,\"B\":%.12g,\"rms_cents\":%.9g,"
           "\"quality\":%.9g,\"used_count\":%d,\"partials\":[",
           ac_piano_status_name(r->status), r->first_partial_hz, r->inharmonicity, r->rms_cents,
           r->quality, r->used_count);
    for (int i = 0; i < r->partial_count; ++i) {
        ac_partial p = r->partials[i];
        printf("%s{\"number\":%d,\"measured_hz\":%.12g,\"amplitude\":%.9g,\"predicted_hz\":%.12g,"
               "\"residual_cents\":%.9g,\"used\":%s}",
               i ? "," : "", p.number, p.frequency_hz, p.amplitude, p.predicted_hz,
               p.residual_cents, p.used ? "true" : "false");
    }
    puts("]}");
}
static int numeric(const char *text, double *value) {
    char *end;
    errno = 0;
    *value = strtod(text, &end);
    return !errno && end != text && !*end && isfinite(*value);
}
static int usage(void) {
    fprintf(stderr, "Usage: accordomi-analyze FILE.wav --midi 21..108 [--method "
                    "piano|yin|autocorrelation|mcleod] [--reference 440] [--window SAMPLES] [--hop "
                    "SAMPLES] [--channel 1] [--format jsonl|csv]\nDefault: piano, 65536-sample "
                    "windows / 16384 hop. Detectors default to 4096 / 1024. CSV is piano-only. "
                    "Exit 0: usable frames; 2: input/usage error; 3: no usable frames.\n");
    return 2;
}
int main(int argc, char **argv) {
    if (argc < 2)
        return usage();
    if (!strcmp(argv[1], "--help")) {
        usage();
        return 0;
    }
    output_config o = {0};
    o.path = argv[1];
    o.method = "piano";
    o.reference = 440;
    o.midi = -1;
    o.channel = 1;
    for (int i = 2; i < argc; i += 2) {
        if (i + 1 >= argc)
            return usage();
        const char *key = argv[i], *value = argv[i + 1];
        double number;
        if (!strcmp(key, "--method"))
            o.method = value;
        else if (!strcmp(key, "--format")) {
            if (strcmp(value, "jsonl") && strcmp(value, "csv"))
                return usage();
            o.csv = !strcmp(value, "csv");
        } else {
            if (!numeric(value, &number))
                return usage();
            if (!strcmp(key, "--reference")) {
                if (number < 400 || number > 480)
                    return usage();
                o.reference = number;
            } else {
                if (number < 1 || number > 1048576 || floor(number) != number)
                    return usage();
                if (!strcmp(key, "--midi"))
                    o.midi = (int)number;
                else if (!strcmp(key, "--channel"))
                    o.channel = (int)number;
                else if (!strcmp(key, "--window"))
                    o.window = (size_t)number;
                else if (!strcmp(key, "--hop"))
                    o.hop = (size_t)number;
                else
                    return usage();
            }
        }
    }
    int piano = !strcmp(o.method, "piano");
    ac_method method;
    if (!strcmp(o.method, "yin"))
        method = AC_YIN;
    else if (!strcmp(o.method, "autocorrelation"))
        method = AC_AUTOCORRELATION;
    else if (!strcmp(o.method, "mcleod"))
        method = AC_MCLEOD;
    else if (piano)
        method = AC_YIN;
    else
        return usage();
    if (o.midi < 21 || o.midi > 108 || (o.csv && !piano))
        return usage();
    if (!o.window)
        o.window = piano ? 65536 : 4096;
    if (!o.hop)
        o.hop = o.window / 4;
    if (o.window < (piano ? 2048u : 512u) || o.hop > o.window)
        return usage();
    ac_wav wav;
    char error[256];
    if (!ac_wav_open(&wav, o.path, error, sizeof(error))) {
        fprintf(stderr, "%s\n", error);
        return 2;
    }
    o.rate = wav.rate;
    if (o.channel > wav.channels) {
        fprintf(stderr, "Channel is not present in WAV\n");
        ac_wav_close(&wav);
        return 2;
    }
    double expected = ac_equal_tempered_hz(o.midi, o.reference);
    ac_piano_stream *stream =
        piano ? ac_piano_stream_create(o.rate, o.window, o.hop, expected) : NULL;
    ac_workspace *work = piano ? NULL : ac_workspace_create(o.window);
    float *frame = piano ? NULL : calloc(o.window, sizeof(float));
    if ((piano && !stream) || (!piano && (!work || !frame))) {
        fprintf(stderr, "Unsupported analysis configuration or insufficient memory\n");
        ac_piano_stream_destroy(stream);
        ac_workspace_destroy(work);
        free(frame);
        ac_wav_close(&wav);
        return 2;
    }
    if (o.csv)
        puts("start_sample,time_seconds,sample_rate,midi,status,first_partial_hz,B,rms_cents,"
             "quality,partial,measured_hz,amplitude,predicted_hz,residual_cents,used");
    float block[4096];
    int count;
    size_t filled = 0;
    uint64_t start = 0;
    while ((count = ac_wav_read(&wav, o.channel - 1, block, 4096)) > 0) {
        if (piano)
            ac_piano_stream_push(stream, block, (size_t)count, piano_output, &o);
        else
            for (int i = 0; i < count; ++i) {
                frame[filled++] = block[i];
                if (filled == o.window) {
                    ac_pitch pitch;
                    int ok = ac_detect(work, frame, o.window, o.rate, method, &pitch);
                    ++o.frames;
                    o.usable += ok;
                    metadata(&o, start);
                    printf(",\"status\":\"%s\",\"frequency_hz\":%.12g,\"clarity\":%.9g}\n",
                           ok ? "usable" : "rejected", pitch.frequency_hz, pitch.clarity);
                    memmove(frame, frame + o.hop, (o.window - o.hop) * sizeof(float));
                    filled -= o.hop;
                    start += o.hop;
                }
            }
    }
    ac_piano_stream_destroy(stream);
    ac_workspace_destroy(work);
    free(frame);
    ac_wav_close(&wav);
    if (count < 0) {
        fprintf(stderr, "Truncated or non-finite WAV samples\n");
        return 2;
    }
    if (fflush(stdout) != 0 || ferror(stdout)) {
        fprintf(stderr, "Cannot write analysis output\n");
        return 2;
    }
    fprintf(stderr, "%d windows, %d usable; incomplete tail omitted\n", o.frames, o.usable);
    return o.usable ? 0 : 3;
}
