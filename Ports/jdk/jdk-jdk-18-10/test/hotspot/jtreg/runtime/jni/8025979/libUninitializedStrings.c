/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

//Method to verify expression and throw java/lang/Exception if it is FALSE
void Assert(JNIEnv *jni_env, jint expr, const char* message) {
    if(expr == 0){ //if expr is false
        (*jni_env)->FatalError(jni_env, message);
    }
}

//Method to allocate a java/lang/String object and return jstring as a result
jstring AllocateString(JNIEnv *jni_env) {
    jclass classString = NULL;
    jstring allocatedString = NULL;
    classString = (*jni_env)->FindClass(jni_env, "java/lang/String");
    //NULL check
    Assert(jni_env, (classString != NULL), "class String not found");
    //allocate object of type java/lang/String
    allocatedString = (jstring) (*jni_env)->AllocObject(jni_env, classString);
    //NULL check
    Assert(jni_env, (allocatedString != NULL), "allocated string is NULL");
    return allocatedString;
}

//GetStringLength test
JNIEXPORT void JNICALL Java_UninitializedStrings_lengthTest
(JNIEnv *jni_env, jclass cl) {
    jint stringLength = 0;
    jstring allocatedString = NULL;
    //allocate object of type java/lang/String
    allocatedString = AllocateString(jni_env);

    stringLength = (*jni_env)->GetStringLength(jni_env, allocatedString);
    Assert(jni_env, (stringLength == 0), "string length must be 0");
}

//GetStringChars test
JNIEXPORT void JNICALL Java_UninitializedStrings_charsTest
(JNIEnv *jni_env, jclass cl) {
    jint compareRes = 0;
    const jchar* stringChars = NULL;
    jstring allocatedString = NULL;
    //allocate object of type java/lang/String
    allocatedString = AllocateString(jni_env);

    stringChars = (*jni_env)->GetStringChars(jni_env, allocatedString, NULL);
    compareRes = (stringChars == NULL);
    //release stringChars pointer
    (*jni_env)->ReleaseStringChars(jni_env, allocatedString, stringChars);
    Assert(jni_env, compareRes, "string chars must be NULL");
}

//GetStringUTFLength test
JNIEXPORT void JNICALL Java_UninitializedStrings_utfLengthTest
(JNIEnv *jni_env, jclass cl) {
    jint stringLength = 0;
    jstring allocatedString = NULL;
    //allocate object of type java/lang/String
    allocatedString = AllocateString(jni_env);

    stringLength = (*jni_env)->GetStringUTFLength(jni_env, allocatedString);
    Assert(jni_env, (stringLength == 0), "string utf length must be 0");
}

//GetStringUTFChars test
JNIEXPORT void JNICALL Java_UninitializedStrings_utfCharsTest
(JNIEnv *jni_env, jclass cl) {
    jint compareRes = 0;
    const char* stringUtfChars = NULL;
    jstring allocatedString = NULL;
    //allocate object of type java/lang/String
    allocatedString = AllocateString(jni_env);

    stringUtfChars = (*jni_env)->GetStringUTFChars(jni_env, allocatedString, NULL);
    compareRes = (stringUtfChars == NULL);
    //release stringUtfChars pointer
    (*jni_env)->ReleaseStringUTFChars(jni_env, allocatedString, stringUtfChars);
    Assert(jni_env, compareRes, "string utf chars must be NULL");
}
