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

#include <stdio.h>
#include <stdlib.h>

#include "jni.h"
#include "assert.h"

static jclass    classClass;
static jclass    iaeClass;
static jmethodID mid_Class_forName;
static jmethodID mid_Class_getField;
static jmethodID mid_Field_get;

int getField(JNIEnv *env, char* declaringClass_name, char* field_name);
int checkAndClearIllegalAccessExceptionThrown(JNIEnv *env);

int main(int argc, char** args) {
    JavaVM *jvm;
    JNIEnv *env;
    JavaVMInitArgs vm_args;
    JavaVMOption options[1];
    jint rc;

    vm_args.version = JNI_VERSION_1_2;
    vm_args.nOptions = 0;
    vm_args.options = options;

    if ((rc = JNI_CreateJavaVM(&jvm, (void**)&env, &vm_args)) != JNI_OK) {
        printf("ERROR: cannot create VM.\n");
        exit(-1);
    }

    classClass = (*env)->FindClass(env, "java/lang/Class");
    iaeClass = (*env)->FindClass(env, "java/lang/IllegalAccessException");
    mid_Class_forName = (*env)->GetStaticMethodID(env, classClass, "forName",
                                                  "(Ljava/lang/String;)Ljava/lang/Class;");
    assert(mid_Class_forName != NULL);

    mid_Class_getField = (*env)->GetMethodID(env, classClass, "getField",
                                             "(Ljava/lang/String;)Ljava/lang/reflect/Field;");
    assert(mid_Class_getField != NULL);

    jclass fieldClass = (*env)->FindClass(env, "java/lang/reflect/Field");
    mid_Field_get = (*env)->GetMethodID(env, fieldClass, "get", "(Ljava/lang/Object;)Ljava/lang/Object;");
    assert(mid_Class_getField != NULL);

    // can access to public member of an exported type
    if ((rc = getField(env, "java.lang.Integer", "TYPE")) != 0) {
        printf("ERROR: fail to access java.lang.Integer::TYPE\n");
        exit(-1);
    }

    // expect IAE to jdk.internal.misc.Unsafe class
    if ((rc = getField(env, "jdk.internal.misc.Unsafe", "INVALID_FIELD_OFFSET")) == 0) {
        printf("ERROR: IAE not thrown\n");
        exit(-1);
    }
    if (checkAndClearIllegalAccessExceptionThrown(env) != JNI_TRUE) {
        printf("ERROR: exception is not an instance of IAE\n");
        exit(-1);
    }

    // expect IAE to jdk.internal.misc.Unsafe class
    if ((rc = getField(env, "jdk.internal.misc.Unsafe", "INVALID_FIELD_OFFSET")) == 0) {
        printf("ERROR: IAE not thrown\n");
        exit(-1);
    }
    if (checkAndClearIllegalAccessExceptionThrown(env) != JNI_TRUE) {
        printf("ERROR: exception is not an instance of IAE\n");
        exit(-1);
    }

    (*jvm)->DestroyJavaVM(jvm);
    return 0;
}

int checkAndClearIllegalAccessExceptionThrown(JNIEnv *env) {
    jthrowable t = (*env)->ExceptionOccurred(env);
    if ((*env)->IsInstanceOf(env, t, iaeClass) == JNI_TRUE) {
        (*env)->ExceptionClear(env);
        return JNI_TRUE;
    }
    return JNI_FALSE;
}

int getField(JNIEnv *env, char* declaringClass_name, char* field_name) {
    jobject c = (*env)->CallStaticObjectMethod(env, classClass, mid_Class_forName,
                                               (*env)->NewStringUTF(env, declaringClass_name));
    if ((*env)->ExceptionOccurred(env) != NULL) {
        (*env)->ExceptionDescribe(env);
        return 1;
    }

    jobject f = (*env)->CallObjectMethod(env, c, mid_Class_getField, (*env)->NewStringUTF(env, field_name));
    if ((*env)->ExceptionOccurred(env) != NULL) {
        (*env)->ExceptionDescribe(env);
        return 2;
    }

    jobject v = (*env)->CallObjectMethod(env, f, mid_Field_get, c);
    if ((*env)->ExceptionOccurred(env) != NULL) {
        (*env)->ExceptionDescribe(env);
        return 3;
    }
    return 0;
}

