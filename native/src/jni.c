#include "internal.h"
#include <jni.h>
#include <math.h>
#include <stdlib.h>
#define JNI_NAME(name) Java_com_lelloman_accordomi_nativeaudio_NativeAudio_##name
static void fail(JNIEnv *env, const char *type, const char *message) {
    jclass cls = (*env)->FindClass(env, type);
    if (cls)
        (*env)->ThrowNew(env, cls, message);
}
static jdoubleArray doubles(JNIEnv *env, const double *values, int count) {
    jdoubleArray array = (*env)->NewDoubleArray(env, count);
    if (array)
        (*env)->SetDoubleArrayRegion(env, array, 0, count, values);
    return array;
}
static int length(JNIEnv *env, jarray array, int minimum) {
    if (!array || (*env)->GetArrayLength(env, array) < minimum) {
        fail(env, "java/lang/IllegalArgumentException", "Invalid native audio array size");
        return 0;
    }
    return 1;
}
static ac_workspace *workspace(JNIEnv *env, jobject storage, int count) {
    if (!storage) {
        fail(env, "java/lang/IllegalArgumentException", "Missing workspace");
        return NULL;
    }
    void *memory = (*env)->GetDirectBufferAddress(env, storage);
    jlong bytes = (*env)->GetDirectBufferCapacity(env, storage);
    ac_workspace *w = memory;
    size_t required = ac_workspace_bytes((size_t)count);
    if (!w || !required || bytes < (jlong)required || w->capacity < (size_t)count)
        w = NULL;
    if (!w)
        fail(env, "java/lang/IllegalArgumentException", "Invalid workspace or audio frame size");
    return w;
}
JNIEXPORT jint JNICALL JNI_NAME(workspaceBytes)(JNIEnv *env, jobject self, jint count) {
    (void)env;
    (void)self;
    return (jint)ac_workspace_bytes((size_t)count);
}
JNIEXPORT void JNICALL JNI_NAME(initializeWorkspace)(JNIEnv *env, jobject self, jobject storage,
                                                     jint count) {
    (void)self;
    if (!storage) {
        fail(env, "java/lang/IllegalArgumentException", "Missing workspace");
        return;
    }
    void *memory = (*env)->GetDirectBufferAddress(env, storage);
    jlong bytes = (*env)->GetDirectBufferCapacity(env, storage);
    if (bytes <= 0 || !ac_workspace_init(memory, (size_t)bytes, (size_t)count))
        fail(env, "java/lang/IllegalArgumentException", "Invalid workspace");
}
JNIEXPORT jdoubleArray JNICALL JNI_NAME(detectNative)(JNIEnv *env, jobject self,
                                                      jfloatArray samples, jint rate, jint method,
                                                      jobject storage) {
    (void)self;
    if (!length(env, samples, 0))
        return NULL;
    int n = (*env)->GetArrayLength(env, samples);
    if (n < 512 || rate <= 0)
        return NULL;
    ac_workspace *w = workspace(env, storage, n);
    if (!w)
        return NULL;
    jfloat *s = (*env)->GetFloatArrayElements(env, samples, NULL);
    ac_pitch pitch;
    int ok = s && ac_detect(w, s, (size_t)n, rate, (ac_method)method, &pitch);
    if (s)
        (*env)->ReleaseFloatArrayElements(env, samples, s, JNI_ABORT);

    if (!ok)
        return NULL;
    double result[] = {pitch.frequency_hz, pitch.clarity};
    return doubles(env, result, 2);
}
JNIEXPORT jdoubleArray JNICALL JNI_NAME(correlateNative)(JNIEnv *env, jobject self,
                                                         jfloatArray samples, jobject storage) {
    (void)self;
    if (!length(env, samples, 2))
        return NULL;
    int n = (*env)->GetArrayLength(env, samples);
    ac_workspace *w = workspace(env, storage, n);
    if (!w)
        return NULL;
    double *result = calloc((size_t)n * 3, sizeof(double));
    if (!result) {
        fail(env, "java/lang/OutOfMemoryError", "Cannot allocate correlations");
        return NULL;
    }
    jfloat *s = (*env)->GetFloatArrayElements(env, samples, NULL);
    int ok = s && ac_correlate(w, s, n, result, result + n, result + 2 * n);
    if (s)
        (*env)->ReleaseFloatArrayElements(env, samples, s, JNI_ABORT);
    jdoubleArray out = NULL;
    if (ok)
        out = doubles(env, result, n * 3);
    else if (!(*env)->ExceptionCheck(env))
        fail(env, "java/lang/IllegalArgumentException", "Invalid correlation input");
    free(result);
    return out;
}
JNIEXPORT jdoubleArray JNICALL JNI_NAME(tuning)(JNIEnv *env, jobject self, jdouble hz,
                                                jdouble reference) {
    (void)self;
    int midi;
    double target, cents;
    if (!ac_tuning_reading(hz, reference, &midi, &target, &cents)) {
        fail(env, "java/lang/IllegalArgumentException", "Frequencies must be finite and positive");
        return NULL;
    }
    double result[] = {midi, target, cents};
    return doubles(env, result, 3);
}
JNIEXPORT jdouble JNICALL JNI_NAME(equalTemperedHz)(JNIEnv *env, jobject self, jint midi,
                                                    jdouble reference) {
    (void)env;
    (void)self;
    return ac_equal_tempered_hz(midi, reference);
}
JNIEXPORT jdoubleArray JNICALL JNI_NAME(stabilize)(JNIEnv *env, jobject self, jdoubleArray state,
                                                   jdouble hz, jfloat clarity) {
    (void)self;
    if (!length(env, state, 7))
        return NULL;
    double v[7];
    (*env)->GetDoubleArrayRegion(env, state, 0, 7, v);
    for (int i = 0; i < 7; ++i) {
        if (!isfinite(v[i])) {
            fail(env, "java/lang/IllegalArgumentException", "Invalid stabilizer state");
            return NULL;
        }
    }
    if (v[6] < 0 || v[6] > 8) {
        fail(env, "java/lang/IllegalArgumentException", "Invalid dropout count");
        return NULL;
    }
    ac_stabilizer s = {{v[0], (float)v[1]}, {v[2], (float)v[3]}, v[4] != 0, v[5] != 0, (int)v[6]};
    ac_pitch input = {hz, clarity}, out;
    int ok = ac_stabilizer_update(&s, hz > 0 ? &input : NULL, &out);
    double next[] = {s.previous.frequency_hz,
                     s.previous.clarity,
                     s.pending.frequency_hz,
                     s.pending.clarity,
                     s.has_previous,
                     s.has_pending,
                     s.missing};
    (*env)->SetDoubleArrayRegion(env, state, 0, 7, next);
    if (!ok)
        return NULL;
    double result[] = {out.frequency_hz, out.clarity};
    return doubles(env, result, 2);
}
JNIEXPORT void JNICALL JNI_NAME(oscillator)(JNIEnv *env, jobject self, jdoubleArray state,
                                            jdouble hz, jint rate, jboolean releasing,
                                            jshortArray output) {
    (void)self;
    if (!length(env, state, 2) || !length(env, output, 0))
        return;
    double v[2];
    (*env)->GetDoubleArrayRegion(env, state, 0, 2, v);
    ac_oscillator s = {v[0], v[1]};
    jshort *out = (*env)->GetShortArrayElements(env, output, NULL);
    if (!out)
        return;
    int ok = ac_oscillator_fill(&s, hz, rate, releasing, out, (*env)->GetArrayLength(env, output));
    (*env)->ReleaseShortArrayElements(env, output, out, 0);
    if (!ok) {
        fail(env, "java/lang/IllegalArgumentException", "Invalid oscillator input");
        return;
    }
    double next[] = {s.phase, s.gain};
    (*env)->SetDoubleArrayRegion(env, state, 0, 2, next);
}
JNIEXPORT jdoubleArray JNICALL JNI_NAME(pianoNative)(JNIEnv *env, jobject self, jfloatArray samples,
                                                     jint rate, jdouble expected, jdouble known_b,
                                                     jobject storage) {
    (void)self;
    if (!length(env, samples, 2))
        return NULL;
    int n = (*env)->GetArrayLength(env, samples);
    ac_workspace *w = workspace(env, storage, n);
    if (!w)
        return NULL;
    jfloat *s = (*env)->GetFloatArrayElements(env, samples, NULL);
    ac_piano_result r;
    if (!s) {
        return NULL;
    }
    if (known_b < 0)
        ac_analyze_piano(w, s, n, rate, expected, &r);
    else
        ac_measure_piano(w, s, n, rate, expected, known_b, &r);
    (*env)->ReleaseFloatArrayElements(env, samples, s, JNI_ABORT);
    double result[7 + 6 * AC_MAX_PARTIALS] = {r.status,       r.first_partial_hz, r.inharmonicity,
                                              r.rms_cents,    r.quality,          r.used_count,
                                              r.partial_count};
    for (int i = 0; i < r.partial_count; ++i) {
        int j = 7 + 6 * i;
        ac_partial p = r.partials[i];
        result[j] = p.number;
        result[j + 1] = p.frequency_hz;
        result[j + 2] = p.amplitude;
        result[j + 3] = p.predicted_hz;
        result[j + 4] = p.residual_cents;
        result[j + 5] = p.used;
    }
    return doubles(env, result, 7 + 6 * r.partial_count);
}

JNIEXPORT jdoubleArray JNICALL JNI_NAME(pianoTargets)(JNIEnv *env, jobject self, jintArray notes,
                                                      jdoubleArray stiffness, jdouble reference) {
    (void)self;
    if (!length(env, notes, 4) || !length(env, stiffness, 4))
        return NULL;
    int count = (*env)->GetArrayLength(env, notes);
    if (count > 88 || (*env)->GetArrayLength(env, stiffness) != count) {
        fail(env, "java/lang/IllegalArgumentException", "Invalid piano samples");
        return NULL;
    }
    jint midi[88];
    double b[88], result[176];
    (*env)->GetIntArrayRegion(env, notes, 0, count, midi);
    (*env)->GetDoubleArrayRegion(env, stiffness, 0, count, b);
    if (!ac_piano_targets(midi, b, count, reference, result, result + 88))
        return NULL;
    return doubles(env, result, 176);
}
JNIEXPORT jdoubleArray JNICALL JNI_NAME(calibrationSummary)(JNIEnv *env, jobject self,
                                                            jdoubleArray frequencies,
                                                            jdoubleArray stiffness,
                                                            jdoubleArray qualities) {
    (void)self;
    if (!length(env, frequencies, 2) || !length(env, stiffness, 2) || !length(env, qualities, 2))
        return NULL;
    int count = (*env)->GetArrayLength(env, frequencies);
    if (count > 32 || (*env)->GetArrayLength(env, stiffness) != count ||
        (*env)->GetArrayLength(env, qualities) != count)
        return NULL;
    double hz[32], b[32], quality[32], result[3];
    (*env)->GetDoubleArrayRegion(env, frequencies, 0, count, hz);
    (*env)->GetDoubleArrayRegion(env, stiffness, 0, count, b);
    (*env)->GetDoubleArrayRegion(env, qualities, 0, count, quality);
    if (!ac_calibration_summary(hz, b, quality, count, result))
        return NULL;
    return doubles(env, result, 3);
}
JNIEXPORT jdouble JNICALL JNI_NAME(cents)(JNIEnv *env, jobject self, jdouble hz, jdouble target) {
    (void)env;
    (void)self;
    return ac_cents_between(hz, target);
}
JNIEXPORT jint JNICALL JNI_NAME(stability)(JNIEnv *env, jobject self, jdoubleArray frequencies,
                                           jdoubleArray times) {
    (void)self;
    if (!length(env, frequencies, 0) || !length(env, times, 0))
        return 0;
    int count = (*env)->GetArrayLength(env, frequencies);
    if (count > 32 || (*env)->GetArrayLength(env, times) != count)
        return 0;
    double hz[32], seconds[32];
    (*env)->GetDoubleArrayRegion(env, frequencies, 0, count, hz);
    (*env)->GetDoubleArrayRegion(env, times, 0, count, seconds);
    return ac_pitch_stability(hz, seconds, count);
}
