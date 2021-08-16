/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
#include <stdlib.h>
#include "jvmti.h"
#include "jni.h"

#ifdef __cplusplus
extern "C" {
#endif


static jvmtiEnv *jvmti = NULL;

// valid while a test is executed
static jobject testResultObject = NULL;
static jclass testResultClass = NULL;


static void reportError(const char *msg, int err) {
    printf("%s, error: %d\n", msg, err);
}


// logs the notification and updates currentTestResult
static void handleNotification(JNIEnv *jni_env,
    jmethodID method,
    jfieldID field,
    jclass field_klass,
    int modified,
    jlocation location)
{
    jvmtiError err;
    char *name = NULL;
    char *mname = NULL;
    char *mgensig = NULL;
    jclass methodClass = NULL;
    char *csig = NULL;

    if (testResultObject == NULL) {
        // we are out of test
        return;
    }

    err = (*jvmti)->GetFieldName(jvmti, field_klass, field, &name, NULL, NULL);
    if (err != JVMTI_ERROR_NONE) {
        reportError("GetFieldName failed", err);
        return;
    }

    err = (*jvmti)->GetMethodName(jvmti, method, &mname, NULL, &mgensig);
    if (err != JVMTI_ERROR_NONE) {
        reportError("GetMethodName failed", err);
        return;
    }

    err = (*jvmti)->GetMethodDeclaringClass(jvmti, method, &methodClass);
    if (err != JVMTI_ERROR_NONE) {
        reportError("GetMethodDeclaringClass failed", err);
        return;
    }

    err = (*jvmti)->GetClassSignature(jvmti, methodClass, &csig, NULL);
    if (err != JVMTI_ERROR_NONE) {
        reportError("GetClassSignature failed", err);
        return;
    }

    printf("\"class: %s method: %s%s\" %s field: \"%s\", location: %d\n",
        csig, mname, mgensig, modified ? "modified" : "accessed", name, (int)location);

    // set TestResult
    if (testResultObject != NULL && testResultClass != NULL) {
        jfieldID fieldID;
        // field names in TestResult are "<field_name>_access"/"<field_name>_modify"
        char *fieldName = (char *)malloc(strlen(name) + 16);
        strcpy(fieldName, name);
        strcat(fieldName, modified ? "_modify" : "_access");

        fieldID = (*jni_env)->GetFieldID(jni_env, testResultClass, fieldName, "Z");
        if (fieldID != NULL) {
            (*jni_env)->SetBooleanField(jni_env, testResultObject, fieldID, JNI_TRUE);
        } else {
            // the field is not interesting for the test
        }
        // clear any possible exception
        (*jni_env)->ExceptionClear(jni_env);

        free(fieldName);
    }

    (*jvmti)->Deallocate(jvmti, (unsigned char*)csig);
    (*jvmti)->Deallocate(jvmti, (unsigned char*)mname);
    (*jvmti)->Deallocate(jvmti, (unsigned char*)mgensig);
    (*jvmti)->Deallocate(jvmti, (unsigned char*)name);
}

// recursively sets access and modification watchers for all
// fields of the object specified.
void setWatchers(JNIEnv *jni_env, const jobject obj)
{
    jclass klass;

    if (obj == NULL) {
        return;
    }

    klass = (*jni_env)->GetObjectClass(jni_env, obj);
    do {
        jfieldID* klassFields = NULL;
        jint fieldCount = 0;
        int i;
        jvmtiError err = (*jvmti)->GetClassFields(jvmti, klass, &fieldCount, &klassFields);
        if (err != JVMTI_ERROR_NONE) {
            reportError("Failed to get class fields", err);
            return;
        }

        for (i = 0; i < fieldCount; ++i) {
            char *sig = NULL;
            err = (*jvmti)->SetFieldModificationWatch(jvmti, klass, klassFields[i]);
            if (err != JVMTI_ERROR_NONE && err != JVMTI_ERROR_DUPLICATE) {
                reportError("Failed to set field modification", err);
                return;
            }

            err = (*jvmti)->SetFieldAccessWatch(jvmti, klass, klassFields[i]);
            if (err != JVMTI_ERROR_NONE && err != JVMTI_ERROR_DUPLICATE) {
                reportError("Failed to set field access", err);
                return;
            }

            err = (*jvmti)->GetFieldName(jvmti, klass, klassFields[i], NULL, &sig, NULL);
            if (sig) {
                if (sig[0] == 'L') {
                    jobject fieldVal = (*jni_env)->GetObjectField(jni_env, obj, klassFields[i]);
                    setWatchers(jni_env, fieldVal);
                }
                (*jvmti)->Deallocate(jvmti, (unsigned char*)sig);
            }
        }

        (*jvmti)->Deallocate(jvmti, (unsigned char*)klassFields);

        klass = (*jni_env)->GetSuperclass(jni_env, klass);
    } while (klass != NULL);
}


static void JNICALL
onFieldAccess(jvmtiEnv *jvmti_env,
            JNIEnv* jni_env,
            jthread thread,
            jmethodID method,
            jlocation location,
            jclass field_klass,
            jobject object,
            jfieldID field)
{
    handleNotification(jni_env, method, field, field_klass, 0, location);
}


static void JNICALL
onFieldModification(jvmtiEnv *jvmti_env,
            JNIEnv* jni_env,
            jthread thread,
            jmethodID method,
            jlocation location,
            jclass field_klass,
            jobject object,
            jfieldID field,
            char signature_type,
            jvalue new_value)
{
    handleNotification(jni_env, method, field, field_klass, 1, location);

    if (signature_type == 'L') {
        jobject newObject = new_value.l;
        setWatchers(jni_env, newObject);
    }
}


JNIEXPORT jint JNICALL
Agent_OnLoad(JavaVM *jvm, char *options, void *reserved)
{
    jvmtiError err;
    jvmtiCapabilities caps = {0};
    jvmtiEventCallbacks callbacks = {0};
    jint res = (*jvm)->GetEnv(jvm, (void **) &jvmti, JVMTI_VERSION_1_1);
    if (res != JNI_OK || jvmti == NULL) {
        reportError("GetEnv failed", res);
        return JNI_ERR;
    }

    caps.can_generate_field_modification_events = 1;
    caps.can_generate_field_access_events = 1;
    caps.can_tag_objects = 1;
    err = (*jvmti)->AddCapabilities(jvmti, &caps);
    if (err != JVMTI_ERROR_NONE) {
        reportError("Failed to set capabilities", err);
        return JNI_ERR;
    }

    callbacks.FieldModification = &onFieldModification;
    callbacks.FieldAccess = &onFieldAccess;

    err = (*jvmti)->SetEventCallbacks(jvmti, &callbacks, sizeof(callbacks));
    if (err != JVMTI_ERROR_NONE) {
        reportError("Failed to set event callbacks", err);
        return JNI_ERR;
    }

    err = (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE, JVMTI_EVENT_FIELD_ACCESS, NULL);
    if (err != JVMTI_ERROR_NONE) {
        reportError("Failed to set access notifications", err);
        return JNI_ERR;
    }

    err = (*jvmti)->SetEventNotificationMode(jvmti, JVMTI_ENABLE, JVMTI_EVENT_FIELD_MODIFICATION, NULL);
    if (err != JVMTI_ERROR_NONE) {
        reportError("Failed to set modification notifications", err);
        return JNI_ERR;
    }
    setbuf(stdout, NULL);
    return JNI_OK;
}


JNIEXPORT jboolean JNICALL
Java_FieldAccessWatch_initWatchers(JNIEnv *env, jclass thisClass, jclass cls, jobject field)
{
    jfieldID fieldId;
    jvmtiError err;

    if (jvmti == NULL) {
        reportError("jvmti is NULL", 0);
        return JNI_FALSE;
    }

    fieldId = (*env)->FromReflectedField(env, field);

    err = (*jvmti)->SetFieldModificationWatch(jvmti, cls, fieldId);
    if (err != JVMTI_ERROR_NONE) {
        reportError("SetFieldModificationWatch failed", err);
        return JNI_FALSE;
    }

    err = (*jvmti)->SetFieldAccessWatch(jvmti, cls, fieldId);
    if (err != JVMTI_ERROR_NONE) {
        reportError("SetFieldAccessWatch failed", err);
        return JNI_FALSE;
    }

    return JNI_TRUE;
}


JNIEXPORT jboolean JNICALL
Java_FieldAccessWatch_startTest(JNIEnv *env, jclass thisClass, jobject testResults)
{
    testResultObject = (*env)->NewGlobalRef(env, testResults);
    testResultClass = (jclass)(*env)->NewGlobalRef(env, (*env)->GetObjectClass(env, testResultObject));

    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_FieldAccessWatch_stopTest(JNIEnv *env, jclass thisClass)
{
    if (testResultObject != NULL) {
        (*env)->DeleteGlobalRef(env, testResultObject);
        testResultObject = NULL;
    }
    if (testResultClass != NULL) {
        (*env)->DeleteGlobalRef(env, testResultClass);
        testResultClass = NULL;
    }
}


#ifdef __cplusplus
}
#endif

