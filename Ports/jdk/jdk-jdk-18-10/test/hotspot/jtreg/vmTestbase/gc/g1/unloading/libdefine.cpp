/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include <stdio.h>

#include <string.h>
#include <jvmti.h>

#define STATUS_FAILED 2
#define STATUS_PASSED 0

#define REFERENCES_ARRAY_SIZE 10000000

#ifndef _Included_gc_g1_unloading_unloading_classloaders_JNIClassloader
#define _Included_gc_g1_unloading_unloading_classloaders_JNIClassloader

extern "C" {

/*
 * Class:     gc_g1_unloading_unloading_classloaders_JNIClassloader
 * Method:    loadThroughJNI0
 * Signature: (Ljava/lang/String;Ljava/lang/ClassLoader;[B)Ljava/lang/Class;
 */
JNIEXPORT jclass JNICALL Java_gc_g1_unloading_classloaders_JNIClassloader_loadThroughJNI0 (JNIEnv * env,
                                         jclass clazz, jstring className, jobject classLoader, jbyteArray bytecode) {

  const char * classNameChar = env->GetStringUTFChars(className, NULL);
  jbyte * arrayContent = env->GetByteArrayElements(bytecode, NULL);
  jsize bytecodeLength = env->GetArrayLength(bytecode);
  jclass returnValue = env->DefineClass(classNameChar, classLoader, arrayContent, bytecodeLength);
  env->ReleaseByteArrayElements(bytecode, arrayContent, JNI_ABORT);
  env->ReleaseStringUTFChars(className, classNameChar);
  if (!returnValue) {
    printf("ERROR: DefineClass call returned NULL by some reason. Classloading failed.\n");
  }

  return returnValue;
}

 /*
  * Class:     gc_g1_unloading_unloading_loading_ClassLoadingHelper
  * Method:    makeRedefinition0
  * Signature: (ILjava/lang/Class;[B)I
  */
JNIEXPORT jint JNICALL  Java_gc_g1_unloading_loading_ClassLoadingHelper_makeRedefinition0(JNIEnv *env,
                jclass clazz, jint fl, jclass redefCls, jbyteArray classBytes) {
    JavaVM * jvm;
    jvmtiEnv * jvmti;
    jvmtiError err;
    jvmtiCapabilities caps;
    jvmtiClassDefinition classDef;
    jint jint_err = env->GetJavaVM(&jvm);
    if (jint_err) {
        printf("GetJavaVM returned nonzero: %d", jint_err);
        return STATUS_FAILED;
    }

    jint_err = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_0);
    if (jint_err) {
        printf("GetEnv returned nonzero: %d", jint_err);
        return STATUS_FAILED;
    }

    err = jvmti->GetPotentialCapabilities(&caps);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetPotentialCapabilities) unexpected error: %d\n",err);
        return JNI_ERR;
    }

    err = jvmti->AddCapabilities(&caps);
    if (err != JVMTI_ERROR_NONE) {
        printf("(AddCapabilities) unexpected error: %d\n", err);
        return JNI_ERR;
    }

    if (!caps.can_redefine_classes) {
        printf("ERROR: Can't redefine classes. jvmtiCapabilities.can_redefine_classes isn't set up.");
        return STATUS_FAILED;
    }

    classDef.klass = redefCls;
    classDef.class_byte_count =
        env->GetArrayLength(classBytes);
    jbyte * class_bytes = env->GetByteArrayElements(classBytes, NULL);
    classDef.class_bytes = (unsigned char *)class_bytes;

    if (fl == 2) {
        printf(">>>>>>>> Invoke RedefineClasses():\n");
        printf("\tnew class byte count=%d\n", classDef.class_byte_count);
    }
    err = jvmti->RedefineClasses(1, &classDef);
    env->ReleaseByteArrayElements(classBytes, class_bytes, JNI_ABORT);
    if (err != JVMTI_ERROR_NONE) {
        printf("%s: Failed to call RedefineClasses():\n", __FILE__);
        printf("\tthe function returned error %d\n", err);
        printf("\tFor more info about this error see the JVMTI spec.\n");
        return STATUS_FAILED;
    }
    if (fl == 2)
        printf("<<<<<<<< RedefineClasses() is successfully done\n");

    return STATUS_PASSED;
}

jobject referencesArray[REFERENCES_ARRAY_SIZE];
int firstFreeIndex = 0;

/*
 * Class:     gc_g1_unloading_unloading_keepref_JNIGlobalRefHolder
 * Method:    keepGlobalJNIReference
 * Signature: (Ljava/lang/Object;)I
 */
JNIEXPORT jint JNICALL Java_gc_g1_unloading_keepref_JNIGlobalRefHolder_keepGlobalJNIReference
  (JNIEnv * env, jclass clazz, jobject obj) {
    int returnValue;
    referencesArray[firstFreeIndex] = env->NewGlobalRef(obj);
    printf("checkpoint1 %d \n", firstFreeIndex);
    returnValue = firstFreeIndex;
    firstFreeIndex++;
    return returnValue;
}

/*
 * Class:     gc_g1_unloading_unloading_keepref_JNIGlobalRefHolder
 * Method:    deleteGlobalJNIReference
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_gc_g1_unloading_keepref_JNIGlobalRefHolder_deleteGlobalJNIReference
  (JNIEnv * env, jclass clazz, jint index) {
    env->DeleteGlobalRef(referencesArray[index]);
}


/*
 * Class:     gc_g1_unloading_unloading_keepref_JNILocalRefHolder
 * Method:    holdWithJNILocalReference
 * Signature: (Ljava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_gc_g1_unloading_keepref_JNILocalRefHolder_holdWithJNILocalReference
  (JNIEnv * env, jobject thisObject, jobject syncObject) {
    jclass clazz, objectClazz;
    jfieldID objectToKeepField;
    jobject objectToKeep, localRef;
    jmethodID waitMethod;

    clazz = env->GetObjectClass(thisObject);
    objectToKeepField = env->GetFieldID(clazz, "objectToKeep", "Ljava/lang/Object;");
    objectToKeep = env->GetObjectField(thisObject, objectToKeepField);
    localRef = env->NewLocalRef(objectToKeep);
    env->SetObjectField(thisObject, objectToKeepField, NULL);

    objectClazz = env->FindClass("Ljava/lang/Object;");
    waitMethod = env->GetMethodID(objectClazz, "wait", "()V");
    env->CallVoidMethod(syncObject, waitMethod);
    printf("checkpoint2 \n");
}
}
#endif
