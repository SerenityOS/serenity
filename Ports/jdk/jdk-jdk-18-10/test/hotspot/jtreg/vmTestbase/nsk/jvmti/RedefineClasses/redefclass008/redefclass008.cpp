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
#include <jvmti.h>
#include "agent_common.h"
#include "JVMTITools.h"

extern "C" {


#define BP_NUM 5 /* overall number of breakpoints */

#define STATUS_FAILED 2
#define PASSED 0

typedef struct {
    int inst;      /* type of a method: 0- static; 1- instance */
    char *m_name;  /* method name */
    char *m_sign;  /* JVM signature of a method */
    int loc;       /* breakpoint location in a method's body */
    jmethodID mid; /* JNI's method ID */
} breakpoint;

/* list of breakpoints */
static breakpoint breakpoints[] = {
    { 1, (char*) "checkIt", (char*) "(Ljava/io/PrintStream;Z)I", 0, NULL },
    { 1, (char*) "finMethod", (char*) "(JIJ)V", 5, NULL },
    { 1, (char*) "finMethod", (char*) "(JIJ)V", 4, NULL },
    { 1, (char*) "checkIt", (char*) "(Ljava/io/PrintStream;Z)I", 1, NULL },
    { 0, (char*) "statMethod", (char*) "(III)I", 1, NULL }
};

static jclass redefCls; /* JNI's Java class object */

static jvmtiEnv *jvmti = NULL;
static jvmtiCapabilities caps;
static jvmtiEventCallbacks callbacks;

void JNICALL Breakpoint(jvmtiEnv *jvmti_env, JNIEnv *env,
        jthread thread, jmethodID method, jlocation location) {
}

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_redefclass008(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_redefclass008(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_redefclass008(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint  Agent_Initialize(JavaVM *vm, char *options, void *reserved) {
    jint res;
    jvmtiError err;

    res = vm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
    if (res != JNI_OK) {
        printf("%s: Failed to call GetEnv: error=%d\n", __FILE__, res);
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

    if (!caps.can_redefine_classes) {
        printf("Warning: RedefineClasses is not implemented\n");
    }

    if (caps.can_generate_breakpoint_events) {
        callbacks.Breakpoint = &Breakpoint;
        err = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
        if (err != JVMTI_ERROR_NONE) {
            printf("(SetEventCallbacks) unexpected error: %s (%d)\n",
                   TranslateError(err), err);
            return JNI_ERR;
        }
    } else {
        printf("Warning: Breakpoint event is not implemented\n");
    }

    return JNI_OK;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_RedefineClasses_redefclass008_setBreakpoints(JNIEnv *env,
        jclass cls, jint vrb, jobject redefObj) {
    jvmtiError err;
    int i;

    if (!caps.can_redefine_classes || !caps.can_generate_breakpoint_events) {
        return PASSED;
    }

    redefCls = env->GetObjectClass(redefObj);

    for (i=0; i<BP_NUM; i++) {
/* get the JNI method ID for a method with name m_name and
   signature m_sign */
        if (breakpoints[i].inst) { /* an instance method */
            breakpoints[i].mid = env->GetMethodID(
                redefCls, breakpoints[i].m_name, breakpoints[i].m_sign);
            if (breakpoints[i].mid == NULL) {
                printf(
                    "%s: Failed to get the method ID for the instance method \"%s\" with signature \"%s\"\n",
                    __FILE__, breakpoints[i].m_name, breakpoints[i].m_sign);
                return STATUS_FAILED;
            }
        } else {                   /* a static method */
            breakpoints[i].mid = env->GetStaticMethodID(
                redefCls, breakpoints[i].m_name, breakpoints[i].m_sign);
            if (breakpoints[i].mid == NULL) {
                printf(
                    "%s: Failed to get the method ID for the static method \"%s\" with signature \"%s\"\n",
                    __FILE__, breakpoints[i].m_name, breakpoints[i].m_sign);
                return STATUS_FAILED;
            }
        }

        if (vrb == 1) {
            printf(
                ">>>>>>>> #%d Invoke SetBreakpoint():\n"
                "\tbreakpoint in the %s method: name=\"%s\"; "
                "signature=\"%s\"; location=%d\n",
                i, breakpoints[i].inst ? "instance" : "static",
                breakpoints[i].m_name, breakpoints[i].m_sign, breakpoints[i].loc);
        }

        err = jvmti->SetBreakpoint(breakpoints[i].mid, breakpoints[i].loc);
        if (err != JVMTI_ERROR_NONE) {
            printf("%s: Failed to call SetBreakpoint(): error=%d: %s\n",
                    __FILE__, err, TranslateError(err));
            return STATUS_FAILED;
        }

        err = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_BREAKPOINT, NULL);
        if (err != JVMTI_ERROR_NONE) {
            printf("Failed to enable BREAKPOINT event: %s (%d)\n",
                   TranslateError(err), err);
            return STATUS_FAILED;
        }

        if (vrb == 1)
            printf("<<<<<<<< #%d SetBreakpoint() is successfully done\n\n", i);
    }
    return PASSED;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_RedefineClasses_redefclass008_makeRedefinition(JNIEnv *env,
        jclass cls, jint vrb, jclass redefCls, jbyteArray classBytes) {
    jvmtiError err;
    jvmtiClassDefinition classDef;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        return STATUS_FAILED;
    }

    if (!caps.can_redefine_classes || !caps.can_generate_breakpoint_events) {
        return PASSED;
    }

/* fill the structure jvmtiClassDefinition */
    classDef.klass = redefCls;
    classDef.class_byte_count = env->GetArrayLength(classBytes);
    classDef.class_bytes = (unsigned char *) env->GetByteArrayElements(classBytes, NULL);

    if (vrb == 1)
        printf(">>>>>>>> Invoke RedefineClasses():\n\tnew class byte count=%d\n",
            classDef.class_byte_count);
    err = jvmti->RedefineClasses(1, &classDef);
    if (err != JVMTI_ERROR_NONE) {
        printf("%s: Failed to call RedefineClasses(): error=%d: %s\n",
            __FILE__, err, TranslateError(err));
        printf("\tFor more info about this error see the JVMTI spec.\n");
        return STATUS_FAILED;
    }
    if (vrb == 1)
        printf("<<<<<<<< RedefineClasses() is successfully done\n\n");

    return PASSED;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_RedefineClasses_redefclass008_getResult(JNIEnv *env,
        jclass cls, jint vrb, jobject redefObj) {
    jvmtiError err;
    int i;
    int totRes = PASSED;

    if (!caps.can_redefine_classes || !caps.can_generate_breakpoint_events) {
        return PASSED;
    }

    redefCls = env->GetObjectClass(redefObj);

/* all breakpoints should be cleared after the redefinition */
    for (i=0; i<BP_NUM; i++) {
/* get again the JNI method ID for a method with name m_name and
   signature m_sign */
        if (breakpoints[i].inst) { /* an instance method */
            breakpoints[i].mid = env->GetMethodID(
                redefCls, breakpoints[i].m_name, breakpoints[i].m_sign);
            if (breakpoints[i].mid == NULL) {
                printf(
                    "%s: getResult: Failed to get the method ID for the instance method"
                    "\"%s\" with signature \"%s\"\n",
                    __FILE__, breakpoints[i].m_name, breakpoints[i].m_sign);
                return STATUS_FAILED;
            }
        } else {                   /* a static method */
            breakpoints[i].mid = env->GetStaticMethodID(
                redefCls, breakpoints[i].m_name, breakpoints[i].m_sign);
            if (breakpoints[i].mid == NULL) {
                printf(
                    "%s: getResult: Failed to get the method ID for the static method"
                    "\"%s\" with signature \"%s\"\n",
                    __FILE__, breakpoints[i].m_name, breakpoints[i].m_sign);
                return STATUS_FAILED;
            }
        }

        err = jvmti->ClearBreakpoint(breakpoints[i].mid, breakpoints[i].loc);
        if (err != JVMTI_ERROR_NOT_FOUND) {
            printf(
                "TEST FAILED: Breakpoint #%d in the %s method:\n"
                "\tname=\"%s\"; signature=\"%s\"; location=%d was not cleared:\n"
                "\tClearBreakpoint() returned the error %d: %s\n\n",
                i, breakpoints[i].inst ? "instance" : "static",
                breakpoints[i].m_name, breakpoints[i].m_sign,
                breakpoints[i].loc, err, TranslateError(err));
            totRes = STATUS_FAILED;
        } else {
            if (vrb == 1) {
                printf(
                    "Check #%d PASSED: Breakpoint in the %s method:\n"
                    "\tname=\"%s\"; signature=\"%s\"; location=%d was cleared:\n"
                    "\tClearBreakpoint() returned the error %d: %s\n\n",
                    i, breakpoints[i].inst ? "instance" : "static",
                    breakpoints[i].m_name, breakpoints[i].m_sign,
                    breakpoints[i].loc, err, TranslateError(err));
            }

            err = jvmti->SetBreakpoint(breakpoints[i].mid, breakpoints[i].loc);
            if (err == JVMTI_ERROR_DUPLICATE) {
                printf(
                    "TEST FAILED: the function SetBreakpoint() returned the error %d: %s\n"
                    "\ti.e. the breakpoint #%d has not been really cleared.\n\n",
                    err, TranslateError(err), i);
                totRes = STATUS_FAILED;
            }
        }
    }

    return totRes;
}

}
