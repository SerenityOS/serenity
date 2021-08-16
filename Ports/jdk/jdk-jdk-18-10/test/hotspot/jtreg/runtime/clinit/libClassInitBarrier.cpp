/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

static jmethodID methodId;

static jclass test_class_A;
static jclass test_class_B;

static jmethodID test_staticM_id;
static jmethodID test_staticS_id;
static jmethodID test_staticN_id;
static jmethodID test_A_m_id;

static jfieldID test_staticF_id;
static jfieldID test_A_f_id;

extern "C" {
    JNIEXPORT jboolean JNICALL Java_ClassInitBarrier_init(JNIEnv* env, jclass cls) {
        jclass runnable = env->FindClass("java/lang/Runnable");
        if (runnable == NULL)  return JNI_FALSE;

        methodId = env->GetMethodID(runnable, "run", "()V");
        if (methodId == NULL)  return JNI_FALSE;

        return JNI_TRUE;
    }

    JNIEXPORT jboolean JNICALL Java_ClassInitBarrier_00024Test_00024A_init(JNIEnv* env, jclass cls, jclass arg1) {
        test_class_A = (jclass)env->NewGlobalRef(cls);
        if (test_class_A == NULL)  return JNI_FALSE;

        test_class_B = (jclass)env->NewGlobalRef(arg1);
        if (test_class_B == NULL)  return JNI_FALSE;

        test_staticM_id = env->GetStaticMethodID(test_class_A, "staticM", "(Ljava/lang/Runnable;)V");
        if (test_staticM_id == NULL)  return JNI_FALSE;

        test_staticS_id = env->GetStaticMethodID(test_class_A, "staticS", "(Ljava/lang/Runnable;)V");
        if (test_staticS_id == NULL)  return JNI_FALSE;

        test_staticN_id = env->GetStaticMethodID(test_class_A, "staticN", "(Ljava/lang/Runnable;)V");
        if (test_staticN_id == NULL)  return JNI_FALSE;

        test_A_m_id = env->GetMethodID(test_class_A, "m", "()V");
        if (test_A_m_id == NULL)  return JNI_FALSE;

        test_staticF_id = env->GetStaticFieldID(test_class_A, "staticF", "I");
        if (test_staticF_id == NULL)  return JNI_FALSE;

        test_A_f_id = env->GetFieldID(test_class_A, "f", "I");
        if (test_A_f_id == NULL)  return JNI_FALSE;

        return JNI_TRUE;
    }

    JNIEXPORT void JNICALL Java_ClassInitBarrier_00024Test_00024A_staticN(JNIEnv* env, jclass cls, jobject action) {
        env->CallVoidMethod(action, methodId);
    }

    JNIEXPORT void JNICALL Java_ClassInitBarrier_00024Test_testInvokeStaticJNI(JNIEnv* env, jclass cls, jobject action) {
        env->CallStaticVoidMethod(test_class_A, test_staticM_id, action);
    }

    JNIEXPORT void JNICALL Java_ClassInitBarrier_00024Test_testInvokeStaticSyncJNI(JNIEnv* env, jclass cls, jobject action) {
        env->CallStaticVoidMethod(test_class_A, test_staticS_id, action);
    }

    JNIEXPORT void JNICALL Java_ClassInitBarrier_00024Test_testInvokeStaticNativeJNI(JNIEnv* env, jclass cls, jobject action) {
        env->CallStaticVoidMethod(test_class_A, test_staticN_id, action);
    }

    JNIEXPORT jint JNICALL Java_ClassInitBarrier_00024Test_testGetStaticJNI(JNIEnv* env, jclass cls, jobject action) {
        jint v = env->GetStaticIntField(test_class_A, test_staticF_id); // int v = A.staticF;
        env->CallVoidMethod(action, methodId);                          // action.run();
        return v;
    }

    JNIEXPORT void JNICALL Java_ClassInitBarrier_00024Test_testPutStaticJNI(JNIEnv* env, jclass cls, jobject action) {
        env->SetStaticIntField(test_class_A, test_staticF_id, 1); // A.staticF = 1;
        env->CallVoidMethod(action, methodId);                    // action.run();
    }

    JNIEXPORT jobject JNICALL Java_ClassInitBarrier_00024Test_testNewInstanceAJNI(JNIEnv* env, jclass cls, jobject action) {
        jobject obj = env->AllocObject(test_class_A); // A obj = new A();
        if (env->ExceptionOccurred()) {
          return NULL;
        } else if (obj == NULL) {
          jclass errorClass = env->FindClass("java/lang/AssertionError");
          int ret = env->ThrowNew(errorClass, "JNI: AllocObject: allocation failed, but no exception thrown");
          return NULL;
        }
        env->CallVoidMethod(action, methodId);        // action.run();
        return obj;
    }

    JNIEXPORT jobject JNICALL Java_ClassInitBarrier_00024Test_testNewInstanceBJNI(JNIEnv* env, jclass cls, jobject action) {
        jobject obj = env->AllocObject(test_class_B); // B obj = new B();
        if (env->ExceptionOccurred()) {
          return NULL;
        } else if (obj == NULL) {
          jclass errorClass = env->FindClass("java/lang/AssertionError");
          int ret = env->ThrowNew(errorClass, "JNI: AllocObject: allocation failed, but no exception thrown");
          return NULL;
        }
        env->CallVoidMethod(action, methodId);        // action.run();
        return obj;
    }

    JNIEXPORT jint JNICALL Java_ClassInitBarrier_00024Test_testGetFieldJNI(JNIEnv* env, jclass cls, jobject recv, jobject action) {
        jint v = env->GetIntField(recv, test_A_f_id); // int v = recv.f;
        env->CallVoidMethod(action, methodId);        // action.run();
        return v;
    }

    JNIEXPORT void JNICALL Java_ClassInitBarrier_00024Test_testPutFieldJNI(JNIEnv* env, jclass cls, jobject recv, jobject action) {
        env->SetIntField(recv, test_A_f_id, 1); // A.staticF = 1;
        env->CallVoidMethod(action, methodId);  // action.run();
    }

    JNIEXPORT void JNICALL Java_ClassInitBarrier_00024Test_testInvokeVirtualJNI(JNIEnv* env, jclass cls, jobject recv, jobject action) {
        env->CallVoidMethod(recv, test_A_m_id); // recv.m();
        if (env->ExceptionOccurred()) {
            return;
        }
        env->CallVoidMethod(action, methodId);  // action.run();
    }
}
