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
#include <jni.h>
#include <jvmti.h>
#include <time.h>
#include <stdlib.h>
#include "jni_tools.h"

extern "C" {

static jvmtiEnv *jvmti = NULL;
static jvmtiCapabilities caps;

JNIEXPORT jint JNICALL
Agent_OnLoad(JavaVM *jvm, char *options, void *reserved) {
            jint res;

            res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_0);
            if (res != JNI_OK || jvmti == NULL) {
                    printf("Wrong result of a valid call to GetEnv!\n");
                    return JNI_ERR;
            }
            return JNI_OK;
}

/*
 * Class:     nsk_share_gc_lock_jvmti_JVMTIAllocLocker
 * Method:    jVMTIAllocSection
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_nsk_share_gc_lock_jvmti_JVMTIAllocLocker_jVMTIAllocSection
(JNIEnv *env, jobject o, jlong enterTime, jlong sleepTime) {
        unsigned char *ptr;
        time_t current_time, old_time;
        jvmtiError err;
        old_time = time(NULL);
        enterTime /= 1000;
        current_time = 0;
        while (current_time - old_time < enterTime) {
                err = jvmti->Allocate(1, &ptr);
                mssleep((long) sleepTime);
                err = jvmti->Deallocate(ptr);
                mssleep((long) sleepTime);
                current_time = time(NULL);
        }
}

}
