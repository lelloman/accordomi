#include "accordomi.h"
#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

typedef struct {
    int midi;
    double b;
} sample;
static int compare(const void *a, const void *b) {
    return ((const sample *)a)->midi - ((const sample *)b)->midi;
}
int main(int argc, char **argv) {
    double reference = 440;
    if (argc != 2 && argc != 4)
        goto usage;
    if (argc == 4) {
        char *end;
        if (strcmp(argv[2], "--reference"))
            goto usage;
        reference = strtod(argv[3], &end);
        if (end == argv[3] || *end || !isfinite(reference))
            goto usage;
    }
    FILE *file = fopen(argv[1], "r");
    if (!file) {
        fprintf(stderr, "Cannot open samples CSV\n");
        return 2;
    }
    sample samples[88];
    size_t count = 0;
    char line[256];
    while (fgets(line, sizeof(line), file)) {
        if (!strcmp(line, "midi,B\n") || !strcmp(line, "midi,B\r\n"))
            continue;
        int midi;
        double b;
        char extra;
        if (count == 88 || sscanf(line, " %d , %lf %c", &midi, &b, &extra) != 2 || midi < 21 ||
            midi > 108) {
            fprintf(stderr, "Expected one midi,B pair per line, at most 88 notes\n");
            fclose(file);
            return 2;
        }
        samples[count++] = (sample){midi, b};
    }
    int error = ferror(file);
    fclose(file);
    if (error)
        return 2;
    qsort(samples, count, sizeof(sample), compare);
    int midi[88];
    double measured[88], b[88], targets[88];
    for (size_t i = 0; i < count; ++i) {
        midi[i] = samples[i].midi;
        measured[i] = samples[i].b;
    }
    if (!ac_piano_targets(midi, measured, count, reference, b, targets)) {
        fprintf(stderr,
                "Invalid model: require at least four distinct measured notes spanning A1..E6, "
                "valid B and reference, and an ordered curve within 100 cents of ET\n");
        return 2;
    }
    for (int i = 0; i < 88; ++i)
        printf("{\"model_version\":1,\"midi\":%d,\"reference_hz\":%.12g,\"B\":%.12g,\"target_hz\":%"
               ".12g,\"stretch_cents\":%.9g}\n",
               i + 21, reference, b[i], targets[i],
               ac_cents_between(targets[i], ac_equal_tempered_hz(i + 21, reference)));
    return fflush(stdout) ? 2 : 0;
usage:
    fprintf(stderr, "Usage: accordomi-targets SAMPLES.csv [--reference 440]\nCSV: midi,B (sorted "
                    "automatically). Outputs 88 JSON Lines.\n");
    return 2;
}
