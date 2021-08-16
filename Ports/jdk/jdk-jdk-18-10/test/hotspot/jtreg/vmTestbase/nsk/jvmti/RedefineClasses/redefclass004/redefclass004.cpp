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
#include "jni_tools.h"
#include "agent_common.h"
#include "JVMTITools.h"

extern "C" {


#define STATUS_FAILED 2
#define PASSED 0
#define NO_RESULTS 3

/* expected values of new instance fields */
static jint INTFLD = 333;
static jlong LONGFLD = 444;
#define STRFLD "completely new String field"

static jvmtiEnv *jvmti = NULL;
static jvmtiCapabilities caps;

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_redefclass004(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_redefclass004(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_redefclass004(JavaVM *jvm, char *options, void *reserved) {
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

    return JNI_OK;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_RedefineClasses_redefclass004_makeRedefinition(JNIEnv *env,
        jclass cls, jint vrb, jclass redefCls, jbyteArray classBytes) {
    jvmtiError err;
    jvmtiClassDefinition classDef;
    int no_results = 0;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        return STATUS_FAILED;
    }

    if (!caps.can_redefine_classes) {
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
        if (err == JVMTI_ERROR_UNSUPPORTED_REDEFINITION_SCHEMA_CHANGED) {
            printf(
                "Warning: unrestrictedly redefinition of classes is not implemented,\n"
                "\tso the test has no results.\n");
            no_results = 1;
        }
        else {
            printf("%s: Failed to call RedefineClasses():\n\tthe function returned error %d: %s\n",
                __FILE__, err, TranslateError(err));
            printf("\tFor more info about this error see the JVMTI spec.\n");
        }
        if (no_results == 1) return NO_RESULTS;
        else return JNI_ERR;
    }
    if (vrb == 1)
        printf("<<<<<<<< RedefineClasses() is successfully done\n");

    return PASSED;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_RedefineClasses_redefclass004_checkNewFields(JNIEnv *env,
        jobject obj, jint vrb, jobject redefObj) {
    jclass redefCls = env->GetObjectClass(redefObj);
    jfieldID fid;
    jint intFld;
    jlong longFld;
    jstring stringObj;
    const char *strFld = NULL;

/* get value of new instance field "intComplNewFld" */
    fid = env->GetFieldID(redefCls, "intComplNewFld", "I");
    if (fid == NULL) {
        printf("%s: Failed to get the field ID for the field \"intComplNewFld\"\n",
            __FILE__);
        return STATUS_FAILED;
    }
    intFld = env->GetIntField(redefObj, fid);

/* get value of new instance field "longComplNewFld" */
    fid = env->GetFieldID(redefCls, "longComplNewFld", "J");
    if (fid == NULL) {
        printf("%s: Failed to get the field ID for the field \"longComplNewFld\"\n",
            __FILE__);
        return STATUS_FAILED;
    }
    longFld = env->GetLongField(redefObj, fid);

/* get value of new instance field "stringComplNewFld" */
    fid = env->GetFieldID(redefCls, "stringComplNewFld", "Ljava/lang/String;");
    if (fid == NULL) {
        printf("%s: Failed to get the field ID for the field \"stringComplNewFld\"\n",
            __FILE__);
        return STATUS_FAILED;
    }
    stringObj = (jstring) env->GetObjectField(redefObj, fid);
    strFld = env->GetStringUTFChars(stringObj, 0);

    if (intFld != INTFLD || longFld != LONGFLD ||
        strcmp(strFld, STRFLD) != 0) {
        printf("Completely new fields have not assigned their default values:\n");
        printf("\tintComplNewFld = %d,\texpected %d\n",
            intFld, INTFLD);
        printf("\tlongComplNewFld = %" LL "d", longFld);
        printf(",\texpected %" LL "d\n", LONGFLD);
        printf("\tstringComplNewFld = \"%s\",\texpected \"%s\"\n",
            strFld, STRFLD);
        env->ReleaseStringUTFChars(stringObj, strFld);
        return STATUS_FAILED;
    } else {
        if (vrb == 1) {
            printf(
                "Completely new fields:\n"
                "\tstringComplNewFld = \"%s\"\n"
                "\tintComplNewFld = %d\n"
                "\tlongComplNewFld = %" LL "d\n",
                strFld, intFld, longFld);
        }
        env->ReleaseStringUTFChars(stringObj, strFld);
        return PASSED;
    }
}

}
