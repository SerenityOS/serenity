/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#include <stdio.h>
#include <string.h>
#include <inttypes.h>
#include "jvmti.h"
#include "agent_common.h"
#include "JVMTITools.h"

extern "C" {


#define PASSED  0
#define STATUS_FAILED  2

typedef struct {
    const char *name;
    const char *sig;
    const jboolean stat;
    const char *descr;
    jfieldID fid;
    jvalue val;
} field;

static jvmtiEnv *jvmti;
static jvmtiEventCallbacks callbacks;
static jvmtiCapabilities caps;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;
static jfieldID actual_fid = NULL;
static char actual_sig = '\0';
static jvalue actual_val = {};
static field flds[] = {
    { "fld0", "J", JNI_TRUE, "static long", NULL, {} },
    { "fld1", "J", JNI_FALSE, "long", NULL, {} },
    { "fld2", "F", JNI_TRUE, "static float", NULL, {} },
    { "fld3", "F", JNI_FALSE, "float", NULL, {} },
    { "fld4", "D", JNI_TRUE, "static double", NULL, {} },
    { "fld5", "D", JNI_FALSE, "double", NULL, {} },
    { "fld6", "Ljava/lang/Object;", JNI_TRUE, "static Object", NULL, {} },
    { "fld7", "Ljava/lang/Object;", JNI_FALSE, "Object", NULL, {} },
    { "fld8", "Z", JNI_TRUE, "static boolean", NULL, {} },
    { "fld9", "Z", JNI_FALSE, "boolean", NULL, {} },
    { "fld10", "B", JNI_TRUE, "static byte", NULL, {} },
    { "fld11", "B", JNI_FALSE, "byte", NULL, {} },
    { "fld12", "S", JNI_TRUE, "static short", NULL, {} },
    { "fld13", "S", JNI_FALSE, "short", NULL, {} },
    { "fld14", "C", JNI_TRUE, "static char", NULL, {} },
    { "fld15", "C", JNI_FALSE, "char", NULL, {} }
};

void JNICALL FieldModification(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thr, jmethodID method, jlocation location, jclass field_klass, jobject obj,
        jfieldID field, char sig, jvalue new_value) {
    actual_fid = field;
    actual_sig = sig;
    actual_val = new_value;
    if (actual_sig == 'L') {
        actual_val.l = env->NewGlobalRef(actual_val.l);
    }
    if (printdump == JNI_TRUE) {
        printf(">>> FieldModification, field: 0x%p", actual_fid);
        switch (actual_sig) {
        case 'J':
            printf(", sig: 'J', val: 0x%x%08x\n",
                   (jint)(actual_val.j >> 32), (jint)actual_val.j);
            break;
        case 'F':
            printf(", sig: 'F', val: %.3f\n", (double)actual_val.f);
            break;
        case 'D':
            printf(", sig: 'D', val: %f\n", (double)actual_val.d);
            break;
        case 'L':
            printf(", sig: 'L', val: 0x%p\n", actual_val.l);
            break;
        case 'Z':
            printf(", sig: 'Z', val: 0x%x\n", actual_val.z);
            break;
        case 'B':
            printf(", sig: 'B', val: %d\n", actual_val.b);
            break;
        case 'S':
            printf(", sig: 'S', val: %d\n", actual_val.s);
            break;
        case 'C':
            printf(", sig: 'C', val: 0x%x\n", actual_val.c);
            break;
        case 'I':
            printf(", sig: 'I', val: %d\n", actual_val.i);
            break;
        default:
            printf(", sig: <unknown>, val: 0x%x%08x\n",
                   (jint)(actual_val.j >> 32), (jint)actual_val.j);
            break;
        }
    }
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_setfmodw005(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_setfmodw005(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_setfmodw005(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint  Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res;
    jvmtiError err;

    if (options != NULL && strcmp(options, "printdump") == 0) {
        printdump = JNI_TRUE;
    }

    res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
    if (res != JNI_OK || jvmti == NULL) {
        printf("Wrong result of a valid call to GetEnv !\n");
        return JNI_ERR;
    }

    err = jvmti->GetPotentialCapabilities(&caps);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetPotentialCapabilities) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return JNI_ERR;
    }

    err = jvmti->AddCapabilities(&caps);
    if (err != JVMTI_ERROR_NONE) {
        printf("(AddCapabilities) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return JNI_ERR;
    }

    err = jvmti->GetCapabilities(&caps);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetCapabilities) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return JNI_ERR;
    }

    if (caps.can_generate_field_modification_events) {
        callbacks.FieldModification = &FieldModification;
        err = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
        if (err != JVMTI_ERROR_NONE) {
            printf("(SetEventCallbacks) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            return JNI_ERR;
        }
    } else {
        printf("Warning: FieldModification watch is not implemented\n");
    }

    return JNI_OK;
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_SetFieldModificationWatch_setfmodw005_getReady(JNIEnv *env,
        jclass cls, jobject obj1, jobject obj2) {
    jvmtiError err;
    size_t i;

    if (!caps.can_generate_field_modification_events) {
        return;
    }

#ifdef _MSC_VER
    flds[0].val.j = 0x1234567890abcdefi64;
    flds[1].val.j = 0xfedcba0987654321i64;
#else
    flds[0].val.j = 0x1234567890abcdefULL;
    flds[1].val.j = 0xfedcba0987654321ULL;
#endif
    flds[2].val.f = 123.456F;
    flds[3].val.f = 654.321F;
    flds[4].val.d = 123456.654321;
    flds[5].val.d = 654321.123456;
    flds[6].val.l = env->NewGlobalRef(obj1);
    flds[7].val.l = env->NewGlobalRef(obj2);
    flds[8].val.z = JNI_TRUE;
    flds[9].val.z = JNI_FALSE;
    flds[10].val.b = 123;
    flds[11].val.b = -123;
    flds[12].val.s = 12345;
    flds[13].val.s = -12345;
    flds[14].val.c = 0xabcd;
    flds[15].val.c = 0xdcba;
    for (i = 0; i < sizeof(flds) / sizeof(field); i++) {
        if (flds[i].stat == JNI_TRUE) {
            flds[i].fid = env->GetStaticFieldID(cls, flds[i].name, flds[i].sig);
        } else {
            flds[i].fid = env->GetFieldID(cls, flds[i].name, flds[i].sig);
        }
        if (flds[i].fid == NULL) {
            printf("Unable to set modification watch on %s fld%" PRIuPTR ", fieldID=0",
                   flds[i].descr, i);
        } else {
            if (printdump == JNI_TRUE) {
                printf(">>> setting modification watch on %s", flds[i].descr);
                printf(" fld%" PRIuPTR ", fieldID=0x%p\n", i, flds[i].fid);
            }
            err = jvmti->SetFieldModificationWatch(cls, flds[i].fid);
            if (err != JVMTI_ERROR_NONE) {
                printf("(SetFieldModificationWatch#%" PRIuPTR ") unexpected error:", i);
                printf(" %s (%d)\n", TranslateError(err), err);
                result = STATUS_FAILED;
            }
        }
    }
    err = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
        JVMTI_EVENT_FIELD_MODIFICATION, NULL);
    if (err != JVMTI_ERROR_NONE) {
        printf("(SetEventNotificationMode) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
    }
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_SetFieldModificationWatch_setfmodw005_check(JNIEnv *env,
        jclass cls, jint ind) {
    int flag_err = 0;

    if (!caps.can_generate_field_modification_events) {
        return;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> checking on %s fld%d\n", flds[ind].descr, ind);
        printf(">>> new value expected=0x%08x%08x, actual=0x%08x%08x\n",
               (jint)(flds[ind].val.j >> 32), (jint)flds[ind].val.j,
               (jint)(actual_val.j >> 32), (jint)actual_val.j);
    }
    if (actual_fid != flds[ind].fid) {
        result = STATUS_FAILED;
        if (!flag_err) {
            printf("Field %s fld%d thrown error:\n", flds[ind].descr, ind);
            flag_err = 1;
        }
        printf("    field ID expected=0x%p, actual=0x%p\n",
               flds[ind].fid, actual_fid);
    }
    if (actual_sig != flds[ind].sig[0]) {
        result = STATUS_FAILED;
        if (!flag_err) {
            printf("Field %s fld%d thrown error:\n", flds[ind].descr, ind);
            flag_err = 1;
        }
        printf("    signature expected=%c, actual=%c\n",
               flds[ind].sig[0], actual_sig);
    }
    switch (flds[ind].sig[0]) {
    case 'J':
        if (actual_val.j != flds[ind].val.j) {
            result = STATUS_FAILED;
            if (!flag_err) {
                printf("Field %s fld%d thrown error:\n", flds[ind].descr, ind);
                flag_err = 1;
            }
            printf("    new value expected=0x%x%08x, actual=0x%x%08x\n",
                   (jint)(flds[ind].val.j >> 32), (jint)flds[ind].val.j,
                   (jint)(actual_val.j >> 32), (jint)actual_val.j);
        }
        break;
    case 'F':
        if (actual_val.f != flds[ind].val.f) {
            result = STATUS_FAILED;
            if (!flag_err) {
                printf("Field %s fld%d thrown error:\n", flds[ind].descr, ind);
                flag_err = 1;
            }
            printf("    new value expected=%f, actual=%f\n",
                   (double)flds[ind].val.f, (double)actual_val.f);
        }
        break;
    case 'D':
        if (actual_val.d != flds[ind].val.d) {
            result = STATUS_FAILED;
            if (!flag_err) {
                printf("Field %s fld%d thrown error:\n", flds[ind].descr, ind);
                flag_err = 1;
            }
            printf("    new value expected=%f, actual=%f\n",
                   (double)flds[ind].val.d, (double)actual_val.d);
        }
        break;
    case 'L':
        if (!env->IsSameObject(actual_val.l, flds[ind].val.l)) {
            result = STATUS_FAILED;
            if (!flag_err) {
                printf("Field %s fld%d thrown error:\n", flds[ind].descr, ind);
                flag_err = 1;
            }
            printf("    new value is not the same as expected\n");
        }
        break;
    case 'Z':
        if (actual_val.z != flds[ind].val.z) {
            result = STATUS_FAILED;
            if (!flag_err) {
                printf("Field %s fld%d thrown error:\n", flds[ind].descr, ind);
                flag_err = 1;
            }
            printf("    new value expected=0x%x, actual=0x%x\n",
                   flds[ind].val.z, actual_val.z);
        }
        break;
    case 'B':
        if (actual_val.b != flds[ind].val.b) {
            result = STATUS_FAILED;
            if (!flag_err) {
                printf("Field %s fld%d thrown error:\n", flds[ind].descr, ind);
                flag_err = 1;
            }
            printf("    new value expected=%d, actual=%d\n",
                   flds[ind].val.b, actual_val.b);
        }
        break;
    case 'S':
        if (actual_val.s != flds[ind].val.s) {
            result = STATUS_FAILED;
            if (!flag_err) {
                printf("Field %s fld%d thrown error:\n", flds[ind].descr, ind);
                flag_err = 1;
            }
            printf("    new value expected=%d, actual=%d\n",
                   flds[ind].val.s, actual_val.s);
        }
        break;
    case 'C':
        if (actual_val.c != flds[ind].val.c) {
            result = STATUS_FAILED;
            if (!flag_err) {
                printf("Field %s fld%d thrown error:\n", flds[ind].descr, ind);
                flag_err = 1;
            }
            printf("    new value expected=0x%x, actual=0x%x\n",
                   flds[ind].val.c, actual_val.c);
        }
        break;
    default:
        break;
    }
    actual_fid = NULL;
    actual_sig = '\0';
    actual_val.j = 0L;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_SetFieldModificationWatch_setfmodw005_getRes(JNIEnv *env,
        jclass cls) {
    return result;
}

}
