/*
 * Copyright (c) 2019, Red Hat, Inc
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

static jlong fib(jint num) {
    if (num == 0) {
        return 0;
    }
    if (num <= 2) {
        return 1;
    }
    return fib(num - 2) + fib(num -1);
}

static void callCallback(JNIEnv *env, jclass cls, jobject target, jlong result) {
    jmethodID mid = (*env)->GetMethodID(env, cls, "callback", "(J)V");
    if (mid == NULL) {
        jclass nsme = (jclass) (*env)->NewGlobalRef(env,
                                                    (*env)->FindClass(env,
                                                                      "java/lang/NoSuchMethodException"));
        if (nsme != NULL) {
            (*env)->ThrowNew(env, nsme, "Can't find method callback()");
        }
        return;
    }
    (*env)->CallVoidMethod(env, target, mid, result);
}

static void calculateAndCallCallback(JNIEnv *env, jclass cls, jobject target, jint num) {
    jlong result = -1;
    result = fib(num);
    callCallback(env, cls, target, result);
}

JNIEXPORT void JNICALL
Java_fib_FibJNI_callJNI(JNIEnv *env, jclass cls, jobject target, jint num) {
    calculateAndCallCallback(env, cls, target, num);
}
