#ifndef AC_INTERNAL_H
#define AC_INTERNAL_H
#include "accordomi.h"
struct ac_workspace {
    size_t capacity, fft_capacity, plan_size;
    double *real, *imag, *energy, *values, *difference;
    double *twiddle_real, *twiddle_imag;
    int *peaks;
};
void ac_fft(ac_workspace *workspace, size_t count, int inverse);
size_t ac_fft_size(size_t count);
int ac_prepare_correlation(ac_workspace *, const float *, size_t);
#endif
