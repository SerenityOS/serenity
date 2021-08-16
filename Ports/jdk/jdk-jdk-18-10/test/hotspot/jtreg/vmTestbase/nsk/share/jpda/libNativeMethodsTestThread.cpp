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
#include "jni.h"
#include <stdlib.h>

extern "C" {


static void logMessage(JNIEnv *env, jobject thisObject, jstring message)
{
        jclass klass;
        klass = env->GetObjectClass(thisObject);
        env->CallVoidMethod(thisObject,
                            env->GetMethodID(klass, "log", "(Ljava/lang/String;)V"),
                            message);
}

JNIEXPORT void JNICALL
Java_nsk_share_jpda_NativeMethodsTestThread_VoidMethod(JNIEnv *env,
        jobject thisObject, jstring message)
{
        logMessage(env, thisObject, message);
}

JNIEXPORT jboolean JNICALL
Java_nsk_share_jpda_NativeMethodsTestThread_BooleanMethod(JNIEnv *env,
        jobject thisObject, jstring message)
{
        jclass klass;
        jfieldID valueField;

        logMessage(env, thisObject, message);

        klass = env->GetObjectClass(thisObject);

        valueField = env->GetStaticFieldID(klass, "expectedBooleanValue", "Z");

        return env->GetStaticBooleanField(klass, valueField);
}

JNIEXPORT jbyte JNICALL
Java_nsk_share_jpda_NativeMethodsTestThread_ByteMethod(JNIEnv *env,
        jobject thisObject, jstring message)
{
        jclass klass;
        jfieldID valueField;

        logMessage(env, thisObject, message);

        klass = env->GetObjectClass(thisObject);

        valueField = env->GetStaticFieldID(klass, "expectedByteValue", "B");

        return env->GetStaticByteField(klass, valueField);
}

JNIEXPORT jshort JNICALL
Java_nsk_share_jpda_NativeMethodsTestThread_ShortMethod(JNIEnv *env,
        jobject thisObject, jstring message)
{
        jclass klass;
        jfieldID valueField;

        logMessage(env, thisObject, message);

        klass = env->GetObjectClass(thisObject);

        valueField = env->GetStaticFieldID(klass, "expectedShortValue", "S");

        return env->GetStaticShortField(klass, valueField);
}

JNIEXPORT jchar JNICALL
Java_nsk_share_jpda_NativeMethodsTestThread_CharMethod(JNIEnv *env,
        jobject thisObject, jstring message)
{
        jclass klass;
        jfieldID valueField;

        logMessage(env, thisObject, message);

        klass = env->GetObjectClass(thisObject);

        valueField = env->GetStaticFieldID(klass, "expectedCharValue", "C");

        return env->GetStaticCharField(klass, valueField);
}

JNIEXPORT jint JNICALL
Java_nsk_share_jpda_NativeMethodsTestThread_IntMethod(JNIEnv *env,
        jobject thisObject, jstring message)
{
        jclass klass;
        jfieldID valueField;

        logMessage(env, thisObject, message);

        klass = env->GetObjectClass(thisObject);

        valueField = env->GetStaticFieldID(klass, "expectedIntValue", "I");

        return env->GetStaticIntField(klass, valueField);
}

JNIEXPORT jlong JNICALL
Java_nsk_share_jpda_NativeMethodsTestThread_LongMethod(JNIEnv *env,
        jobject thisObject, jstring message)
{
        jclass klass;
        jfieldID valueField;

        logMessage(env, thisObject, message);

        klass = env->GetObjectClass(thisObject);

        valueField = env->GetStaticFieldID(klass, "expectedLongValue", "J");

        return env->GetStaticLongField(klass, valueField);
}

JNIEXPORT jfloat JNICALL
Java_nsk_share_jpda_NativeMethodsTestThread_FloatMethod(JNIEnv *env,
        jobject thisObject, jstring message)
{
        jclass klass;
        jfieldID valueField;

        logMessage(env, thisObject, message);

        klass = env->GetObjectClass(thisObject);

        valueField = env->GetStaticFieldID(klass, "expectedFloatValue", "F");

        return env->GetStaticFloatField(klass, valueField);
}

JNIEXPORT jdouble JNICALL
Java_nsk_share_jpda_NativeMethodsTestThread_DoubleMethod(JNIEnv *env,
        jobject thisObject, jstring message)
{
        jclass klass;
        jfieldID valueField;

        logMessage(env, thisObject, message);

        klass = env->GetObjectClass(thisObject);

        valueField = env->GetStaticFieldID(klass, "expectedDoubleValue", "D");

        return env->GetStaticDoubleField(klass, valueField);
}

JNIEXPORT jobject JNICALL
Java_nsk_share_jpda_NativeMethodsTestThread_ObjectArrayMethod(JNIEnv *env,
        jobject thisObject, jstring message)
{
        jclass klass;
        jfieldID valueField;

        logMessage(env, thisObject, message);

        klass = env->GetObjectClass(thisObject);

        valueField = env->GetStaticFieldID(klass, "expectedObjectArrayValue",
                                           "[Ljava/lang/Object;");

        return env->GetStaticObjectField(klass, valueField);
}

JNIEXPORT jobject JNICALL
Java_nsk_share_jpda_NativeMethodsTestThread_StringMethod(JNIEnv *env,
        jobject thisObject, jstring message)
{
        jclass klass;
        jfieldID valueField;

        logMessage(env, thisObject, message);

        klass = env->GetObjectClass(thisObject);

        valueField = env->GetStaticFieldID(klass, "expectedStringValue",
                                           "Ljava/lang/String;");

        return env->GetStaticObjectField(klass, valueField);
}

JNIEXPORT jobject JNICALL
Java_nsk_share_jpda_NativeMethodsTestThread_ThreadMethod(JNIEnv *env,
        jobject thisObject, jstring message)
{
        jclass klass;
        jfieldID valueField;

        logMessage(env, thisObject, message);

        klass = env->GetObjectClass(thisObject);

        valueField = env->GetStaticFieldID(klass, "expectedThreadValue",
                                           "Ljava/lang/Thread;");

        return env->GetStaticObjectField(klass, valueField);
}

JNIEXPORT jobject JNICALL
Java_nsk_share_jpda_NativeMethodsTestThread_ThreadGroupMethod(JNIEnv *env,
        jobject thisObject, jstring message)
{
        jclass klass;
        jfieldID valueField;

        logMessage(env, thisObject, message);

        klass = env->GetObjectClass(thisObject);

        valueField = env->GetStaticFieldID(klass, "expectedThreadGroupValue",
                                           "Ljava/lang/ThreadGroup;");

        return env->GetStaticObjectField(klass, valueField);
}

JNIEXPORT jobject JNICALL
Java_nsk_share_jpda_NativeMethodsTestThread_ClassObjectMethod(JNIEnv *env,
        jobject thisObject, jstring message)
{
        jclass klass;
        jfieldID valueField;

        logMessage(env, thisObject, message);

        klass = env->GetObjectClass(thisObject);

        valueField = env->GetStaticFieldID(klass, "expectedClassObjectValue",
                                           "Ljava/lang/Class;");

        return env->GetStaticObjectField(klass, valueField);
}

JNIEXPORT jobject JNICALL
Java_nsk_share_jpda_NativeMethodsTestThread_ClassLoaderMethod(JNIEnv *env,
        jobject thisObject, jstring message)
{
        jclass klass;
        jfieldID valueField;

        logMessage(env, thisObject, message);

        klass = env->GetObjectClass(thisObject);

        valueField = env->GetStaticFieldID(klass, "expectedClassLoaderValue",
                                           "Ljava/lang/ClassLoader;");

        return env->GetStaticObjectField(klass, valueField);
}

JNIEXPORT jobject JNICALL
Java_nsk_share_jpda_NativeMethodsTestThread_ObjectMethod(JNIEnv *env,
        jobject thisObject, jstring message)
{
        jclass klass;
        jfieldID valueField;

        logMessage(env, thisObject, message);

        klass = env->GetObjectClass(thisObject);

        valueField = env->GetStaticFieldID(klass, "expectedObjectValue",
                                           "Ljava/lang/Object;");

        return env->GetStaticObjectField(klass, valueField);
}

JNIEXPORT jobject JNICALL
Java_nsk_share_jpda_NativeMethodsTestThread_BooleanWrapperMethod(JNIEnv *env,
        jobject thisObject, jstring message)
{
        jclass klass;
        jfieldID valueField;

        logMessage(env, thisObject, message);

        klass = env->GetObjectClass(thisObject);

        valueField = env->GetStaticFieldID(klass, "expectedBooleanWrapperValue",
                                           "Ljava/lang/Boolean;");

        return env->GetStaticObjectField(klass, valueField);
}

JNIEXPORT jobject JNICALL
Java_nsk_share_jpda_NativeMethodsTestThread_ByteWrapperMethod(JNIEnv *env,
        jobject thisObject, jstring message)
{
        jclass klass;
        jfieldID valueField;

        logMessage(env, thisObject, message);

        klass = env->GetObjectClass(thisObject);

        valueField = env->GetStaticFieldID(klass, "expectedByteWrapperValue",
                                           "Ljava/lang/Byte;");

        return env->GetStaticObjectField(klass, valueField);
}

JNIEXPORT jobject JNICALL
Java_nsk_share_jpda_NativeMethodsTestThread_ShortWrapperMethod(JNIEnv *env,
        jobject thisObject, jstring message)
{
        jclass klass;
        jfieldID valueField;

        logMessage(env, thisObject, message);

        klass = env->GetObjectClass(thisObject);

        valueField = env->GetStaticFieldID(klass, "expectedShortWrapperValue",
                                           "Ljava/lang/Short;");

        return env->GetStaticObjectField(klass, valueField);
}

JNIEXPORT jobject JNICALL
Java_nsk_share_jpda_NativeMethodsTestThread_CharWrapperMethod(JNIEnv *env,
        jobject thisObject, jstring message)
{
        jclass klass;
        jfieldID valueField;

        logMessage(env, thisObject, message);

        klass = env->GetObjectClass(thisObject);

        valueField = env->GetStaticFieldID(klass, "expectedCharWrapperValue",
                                           "Ljava/lang/Character;");

        return env->GetStaticObjectField(klass, valueField);
}

JNIEXPORT jobject JNICALL
Java_nsk_share_jpda_NativeMethodsTestThread_IntWrapperMethod(JNIEnv *env,
        jobject thisObject, jstring message)
{
        jclass klass;
        jfieldID valueField;

        logMessage(env, thisObject, message);

        klass = env->GetObjectClass(thisObject);

        valueField = env->GetStaticFieldID(klass, "expectedIntWrapperValue",
                                           "Ljava/lang/Integer;");

        return env->GetStaticObjectField(klass, valueField);
}

JNIEXPORT jobject JNICALL
Java_nsk_share_jpda_NativeMethodsTestThread_LongWrapperMethod(JNIEnv *env,
        jobject thisObject, jstring message)
{
        jclass klass;
        jfieldID valueField;

        logMessage(env, thisObject, message);

        klass = env->GetObjectClass(thisObject);

        valueField = env->GetStaticFieldID(klass, "expectedLongWrapperValue",
                                           "Ljava/lang/Long;");

        return env->GetStaticObjectField(klass, valueField);
}

JNIEXPORT jobject JNICALL
Java_nsk_share_jpda_NativeMethodsTestThread_FloatWrapperMethod(JNIEnv *env,
        jobject thisObject, jstring message)
{
        jclass klass;
        jfieldID valueField;

        logMessage(env, thisObject, message);

        klass = env->GetObjectClass(thisObject);

        valueField = env->GetStaticFieldID(klass, "expectedFloatWrapperValue",
                                           "Ljava/lang/Float;");

        return env->GetStaticObjectField(klass, valueField);
}

JNIEXPORT jobject JNICALL
Java_nsk_share_jpda_NativeMethodsTestThread_DoubleWrapperMethod(JNIEnv *env,
        jobject thisObject, jstring message)
{
        jclass klass;
        jfieldID valueField;

        logMessage(env, thisObject, message);

        klass = env->GetObjectClass(thisObject);

        valueField = env->GetStaticFieldID(klass, "expectedDoubleWrapperValue",
                                           "Ljava/lang/Double;");

        return env->GetStaticObjectField(klass, valueField);
}

}
