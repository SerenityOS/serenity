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
#include "jvmti.h"
#include "agent_common.h"
#include "JVMTITools.h"

extern "C" {


#define PASSED 0
#define STATUS_FAILED 2

typedef struct {
    const char *sig;
} iface_info;

typedef struct {
    const char *name;
    jint icount;
    iface_info *ifaces;
} class_info;

static jvmtiEnv *jvmti = NULL;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;

static iface_info i2[] = {
    { "Lnsk/jvmti/GetImplementedInterfaces/getintrf007$InnerInterface1;" }
};

static iface_info i3[] = {
    { "Lnsk/jvmti/GetImplementedInterfaces/getintrf007$InnerInterface2;" }
};

static iface_info i7[] = {
    { "Lnsk/jvmti/GetImplementedInterfaces/OuterInterface1;" }
};

static iface_info i8[] = {
    { "Lnsk/jvmti/GetImplementedInterfaces/OuterInterface1;" }
};

static iface_info i9[] = {
    { "Lnsk/jvmti/GetImplementedInterfaces/OuterInterface2;" }
};

static class_info classes[] = {
    { "InnerClass1", 0, NULL },
    { "InnerInterface1", 0, NULL },
    { "InnerInterface2", 1, i2 },
    { "InnerClass2", 1, i3 },
    { "OuterClass1", 0, NULL },
    { "OuterClass2", 0, NULL },
    { "OuterInterface1", 0, NULL },
    { "OuterClass3", 1, i7 },
    { "OuterInterface2", 1, i8 },
    { "OuterClass4", 1, i9 },
    { "OuterClass5", 0, NULL }
};

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_getintrf007(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_getintrf007(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_getintrf007(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res;

    if (options != NULL && strcmp(options, "printdump") == 0) {
        printdump = JNI_TRUE;
    }

    res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
    if (res != JNI_OK || jvmti == NULL) {
        printf("Wrong result of a valid call to GetEnv!\n");
        return JNI_ERR;
    }

    return JNI_OK;
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_GetImplementedInterfaces_getintrf007_check(JNIEnv *env, jclass cls, jint i, jclass clazz) {
    jvmtiError err;
    jint icount;
    jclass *interfaces;
    char *sig, *generic;
    int j;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        result = STATUS_FAILED;
        return;
    }

    if (printdump == JNI_TRUE) {
        printf(">>> %s:\n", classes[i].name);
    }

    err = jvmti->GetImplementedInterfaces(clazz, &icount, &interfaces);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetImplementedInterfaces#%d) unexpected error: %s (%d)\n",
               i, TranslateError(err), err);
        result = STATUS_FAILED;
        return;
    }

    if (icount != classes[i].icount) {
        printf("(%d) wrong number of interfaces: %d, expected: %d\n",
               i, icount, classes[i].icount);
        result = STATUS_FAILED;
    }

    for (j = 0; j < icount; j++) {
        if (interfaces[j] == NULL) {
            printf("(%d:%d) null reference\n", i, j);
        } else {
            err = jvmti->GetClassSignature(interfaces[j],
                &sig, &generic);
            if (err != JVMTI_ERROR_NONE) {
                printf("(GetClassSignature#%d:%d) unexpected error: %s (%d)\n",
                       i, j, TranslateError(err), err);
            } else {
                if (printdump == JNI_TRUE) {
                    printf(">>>   [%d]: %s\n", j, sig);
                }
                if ((j < classes[i].icount) && (sig == NULL ||
                        strcmp(sig, classes[i].ifaces[j].sig) != 0)) {
                    printf("(%d:%d) wrong interface: \"%s\"", i, j, sig);
                    printf(", expected: \"%s\"\n", classes[i].ifaces[j].sig);
                    result = STATUS_FAILED;
                }
            }
        }
    }
}

JNIEXPORT int JNICALL Java_nsk_jvmti_GetImplementedInterfaces_getintrf007_getRes(JNIEnv *env, jclass cls) {
    return result;
}

}
