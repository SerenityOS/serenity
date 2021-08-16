/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "jvmti.h"
#include "agent_common.h"
#include "JVMTITools.h"

extern "C" {


#define PASSED 0
#define STATUS_FAILED 2

#define RETURN_FAILED errCode = STATUS_FAILED; fflush(0); return

static jvmtiEnv *jvmti = NULL;
static jvmtiCapabilities caps;
static jvmtiEventCallbacks callbacks;
static jint errCode = PASSED;
static jboolean printdump = JNI_TRUE;

static jmethodID midCheckPoint = NULL;
static jmethodID midRun        = NULL;

static jmethodID midCountDownInt     = NULL;
static jmethodID midCountDownShort   = NULL;
static jmethodID midCountDownChar    = NULL;
static jmethodID midCountDownByte    = NULL;
static jmethodID midCountDownBoolean = NULL;

static jint framesExpected = 0;
static jint framesCount = 0;
static jint methodExitEventCount = 0;

static const char *cls_exp = "Lnsk/jvmti/unit/ForceEarlyReturn/earlyretint$earlyretThread;";

#define METHCNT 5
static jint methidx = -1;
static const char *sign_exp[METHCNT] = { "(I)Z", "(I)B", "(I)C",
                                         "(I)S", "(I)I" };
static const char *name_exp[METHCNT] = { "countDownBoolean",
                                         "countDownByte",
                                         "countDownChar",
                                         "countDownShort",
                                         "countDownInt" };

static const int   val_exp [METHCNT] = { 1, 101, 202, 303, 404 };
static       int   val_ret [METHCNT] = { 0,   0,   0,   0,   0 };


static const char *argName = "nestingCount";

void check(jvmtiEnv *jvmti_env, jthread thr, jmethodID mid,
           jlocation loc, jint i) {
    jvmtiError err;
    jclass cls;
    jlocation loc_exp = (i == 0) ? 0x21 : 0xd;
    char *sigClass, *name, *sig, *generic;
    jvmtiLocalVariableEntry *table = NULL;
    jint entryCount = 0;
    jint argValue = -i;
    jint j;

    methidx = (methidx + 1) % METHCNT;

    err = jvmti_env->GetMethodDeclaringClass(mid, &cls);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetMethodDeclaringClass#%d) unexpected error: %s (%d)\n",
               i, TranslateError(err), err);
        RETURN_FAILED;
    }

    err = jvmti_env->GetClassSignature(cls, &sigClass, &generic);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetClassSignature#%d) unexpected error: %s (%d)\n",
               i, TranslateError(err), err);
        RETURN_FAILED;
    }

    err = jvmti_env->GetMethodName(mid, &name, &sig, &generic);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetMethodName#%d) unexpected error: %s (%d)\n",
               i, TranslateError(err), err);
        RETURN_FAILED;
    }

    if (sigClass == NULL || strcmp(sigClass, cls_exp) != 0) {
        printf("(step %d) Wrong class sig: \"%s\",\n", i, sigClass);
        printf(" expected: \"%s\"\n", cls_exp);
        RETURN_FAILED;
    }
    if (name == NULL || strcmp(name, name_exp[methidx]) != 0) {
        printf("(step %d) wrong method name: \"%s\",", i, name);
        printf(" expected: \"%s\"\n", name_exp[methidx]);
        RETURN_FAILED;
    }
    if (sig == NULL || strcmp(sig, sign_exp[methidx]) != 0) {
        printf("(step %d) wrong method sig: \"%s\",", i, sig);
        printf(" expected: \"%s\"\n", sign_exp[methidx]);
        RETURN_FAILED;
    }

    if (printdump == JNI_TRUE) {
        printf("\n>>> step %d: \"%s.%s%s\"\n", i, sigClass, name, sig);
        printf(">>>   location: 0x%x%08x\n", (jint)(loc >> 32), (jint)loc);
    }

    /* Get Local Variable Table to be able to get the argument value
     * from current method frame and compare it with the expected value
     */
    err = jvmti_env->GetLocalVariableTable(mid, &entryCount, &table);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetLocalVariableTable#%d) unexpected error: %s (%d)\n",
               i, TranslateError(err), err);
        RETURN_FAILED;
    }
    if (table != NULL) {
        for (j = 0; j < entryCount; j++) {
            if (strcmp(table[j].name, argName) == 0) {
                err = jvmti_env->GetLocalInt(thr, 0,
                    table[j].slot, &argValue);
                if (err != JVMTI_ERROR_NONE) {
                    printf("(GetLocalInt#%d) unexpected error: %s (%d)\n",
                           i, TranslateError(err), err);
                    RETURN_FAILED;
                }
            }
        }
    }

    if (loc != loc_exp) {
        printf("(step %d) wrong location: 0x%x%08x,",
               i, (jint)(loc >> 32), (jint)loc);
        printf(" expected: 0x%x\n", (jint)loc_exp);
        RETURN_FAILED;
    }
    if (argValue != i) {
        printf("(step %d) wrong argument value: %d,", i, argValue);
        printf(" expected: %d\n", i);
        RETURN_FAILED;
    }

    if (sigClass != NULL) {
        jvmti_env->Deallocate((unsigned char*)sigClass);
    }
    if (name != NULL) {
        jvmti_env->Deallocate((unsigned char*)name);
    }
    if (sig != NULL) {
        jvmti_env->Deallocate((unsigned char*)sig);
    }
    if (table != NULL) {
        for (j = 0; j < entryCount; j++) {
            jvmti_env->Deallocate((unsigned char*)(table[j].name));
            jvmti_env->Deallocate((unsigned char*)(table[j].signature));
        }
        jvmti_env->Deallocate((unsigned char*)table);
    }
    if (methodExitEventCount != (framesCount + 1)) {
        printf("(step %d) wrong methodExitEventCount: %d,",
               i, methodExitEventCount);
        printf(" expected: %d\n", framesCount + 1);
        RETURN_FAILED;
    }
    fflush(0);
}

void JNICALL Breakpoint(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thread, jmethodID method, jlocation location) {
    jvmtiError err;

    if (midCheckPoint != method) {
        printf("bp: don't know where we get called from");
        RETURN_FAILED;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> breakpoint in checkPoint\n");
    }

    err = jvmti_env->ClearBreakpoint(midCheckPoint, 0);
    if (err != JVMTI_ERROR_NONE) {
        printf("(ClearBreakpoint) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        RETURN_FAILED;
    }

    err = jvmti_env->SetEventNotificationMode(JVMTI_ENABLE,
        JVMTI_EVENT_SINGLE_STEP, thread);
    if (err != JVMTI_ERROR_NONE) {
        printf("Cannot enable single step events: %s (%d)\n",
               TranslateError(err), err);
        RETURN_FAILED;
    }

    err = jvmti_env->SetEventNotificationMode(JVMTI_ENABLE,
        JVMTI_EVENT_METHOD_EXIT, thread);
    if (err != JVMTI_ERROR_NONE) {
        printf("Cannot enable method exit events: %s (%d)\n",
               TranslateError(err), err);
        RETURN_FAILED;
    }

    err = jvmti_env->ForceEarlyReturnVoid(thread);
    if (err != JVMTI_ERROR_NONE) {
        printf("(ForceEarlyReturn) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        RETURN_FAILED;
    }
    fflush(0);
}

void JNICALL SingleStep(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thread, jmethodID method, jlocation location) {
    jvmtiError err;

    if (method == midRun) {
        if (printdump == JNI_TRUE) {
            printf(">>> returned early %d frames till method \"run()\"\n",
                   framesCount);
        }

        err = jvmti_env->SetEventNotificationMode(JVMTI_DISABLE,
            JVMTI_EVENT_SINGLE_STEP, thread);
        if (err != JVMTI_ERROR_NONE) {
            printf("Cannot disable single step events: %s (%d)\n",
                   TranslateError(err), err);
            RETURN_FAILED;
        }
        err = jvmti_env->SetEventNotificationMode(JVMTI_DISABLE,
            JVMTI_EVENT_METHOD_EXIT, thread);
        if (err != JVMTI_ERROR_NONE) {
            printf("Cannot disable method exit events: %s (%d)\n",
                   TranslateError(err), err);
            RETURN_FAILED;
        }
    } else if (method == midCountDownInt  || method == midCountDownShort
            || method == midCountDownChar || method == midCountDownByte
            || method == midCountDownBoolean) {
        check(jvmti_env, thread, method, location, framesCount);
        framesCount++;

        err = jvmti_env->ForceEarlyReturnInt(thread,
                                                val_exp[methidx]);
        if (err != JVMTI_ERROR_NONE) {
            printf("(ForceEarlyReturn) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            RETURN_FAILED;
        }
    }
    fflush(0);
}

void JNICALL MethodExit(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thread,
        jmethodID method, jboolean was_popped_by_exception, jvalue value) {
    val_ret[0] = value.z;
    val_ret[1] = value.b;
    val_ret[2] = value.c;
    val_ret[3] = value.s;
    val_ret[4] = value.i;
    methodExitEventCount++;
    if (method == midRun || method == midCheckPoint) {
        return;
    }
    printf(">>> ForceEarlyReturnInt value: %d, expected %d\n",
            val_ret[methidx], val_exp[methidx]);

    if (val_ret[methidx] != val_exp[methidx]) {
        printf("Wrong ForceEarlyReturnInt return value: %d\n", val_ret[methidx]);
        errCode = STATUS_FAILED;
    }
    if (was_popped_by_exception) {
        printf("Method was_popped_by_exception unexpectedly\n");
        errCode = STATUS_FAILED;
    }
    fflush(0);
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_earlyretint(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_earlyretint(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_earlyretint(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiError err;
    jint res;

    if (options != NULL && strcmp(options, "printdump") == 0) {
        printf("Printdump is turned on!\n");

        printdump = JNI_TRUE;
    }

    res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
    if (res != JNI_OK || jvmti == NULL) {
        printf("Wrong error code from a valid call to GetEnv!\n");
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

    if (!caps.can_force_early_return) {
        printf("Warning: ForceEarlyReturn is not implemented\n");
    }

    if (caps.can_generate_breakpoint_events &&
        caps.can_generate_method_exit_events &&
        caps.can_generate_single_step_events)
    {
        callbacks.Breakpoint = &Breakpoint;
        callbacks.SingleStep = &SingleStep;
        callbacks.MethodExit = &MethodExit;
        err = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
        if (err != JVMTI_ERROR_NONE) {
            printf("(SetEventCallbacks) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            return JNI_ERR;
        }
    } else {
        printf("Warning: Breakpoint or SingleStep event are not implemented\n");
    }

    return JNI_OK;
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_unit_ForceEarlyReturn_earlyretint_getReady(
    JNIEnv *env, jclass c, jclass cls, jint depth) {
    jvmtiError err;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        RETURN_FAILED;
    }

    if (!caps.can_force_early_return ||
        !caps.can_generate_breakpoint_events ||
        !caps.can_generate_method_exit_events ||
        !caps.can_generate_single_step_events) {
        return;
    }

    midRun = env->GetMethodID(cls, "run", "()V");
    if (midRun == NULL) {
        printf("Cannot find Method ID for method run\n");
        RETURN_FAILED;
    }

    midCheckPoint = env->GetMethodID(cls, "checkPoint", "()V");
    if (midCheckPoint == NULL) {
        printf("Cannot find Method ID for method checkPoint\n");
        RETURN_FAILED;
    }

    midCountDownInt = env->GetMethodID(cls, "countDownInt", "(I)I");
    if (midCheckPoint == NULL) {
        printf("Cannot find Method ID for method countDownInt\n");
        RETURN_FAILED;
    }

    midCountDownShort = env->GetMethodID(cls, "countDownShort", "(I)S");
    if (midCountDownShort == NULL) {
        printf("Cannot find Method ID for method countDownShort\n");
        RETURN_FAILED;
    }

    midCountDownChar = env->GetMethodID(cls, "countDownChar", "(I)C");
    if (midCountDownChar == NULL) {
        printf("Cannot find Method ID for method countDownChar\n");
        RETURN_FAILED;
    }

    midCountDownByte = env->GetMethodID(cls, "countDownByte", "(I)B");
    if (midCountDownByte == NULL) {
        printf("Cannot find Method ID for method countDownByte\n");
        RETURN_FAILED;
    }

    midCountDownBoolean = env->GetMethodID(cls, "countDownBoolean", "(I)Z");
    if (midCountDownBoolean == NULL) {
        printf("Cannot find Method ID for method countDownBoolean\n");
        RETURN_FAILED;
    }

    err = jvmti->SetBreakpoint(midCheckPoint, 0);
    if (err != JVMTI_ERROR_NONE) {
        printf("(SetBreakpoint) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        RETURN_FAILED;
    }

    err = jvmti->SetEventNotificationMode(JVMTI_ENABLE,
        JVMTI_EVENT_BREAKPOINT, NULL);
    if (err != JVMTI_ERROR_NONE) {
        printf("Failed to enable BREAKPOINT event: %s (%d)\n",
               TranslateError(err), err);
        RETURN_FAILED;
    } else {
        framesExpected = depth;
    }
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_unit_ForceEarlyReturn_earlyretint_printInt(
      JNIEnv *env, jclass cls, jint val) {

    printf("\n>>> Returned value: dec %d, hex: %#x\n", val, val);
    fflush(0);
    return;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_unit_ForceEarlyReturn_earlyretint_check(JNIEnv *env, jclass cls) {
    if (framesCount != framesExpected) {
        printf("Wrong number of returned early frames: %d, expected: %d\n",
            framesCount, framesExpected);
        errCode = STATUS_FAILED;
    }
    fflush(0);
    return errCode;
}

}
