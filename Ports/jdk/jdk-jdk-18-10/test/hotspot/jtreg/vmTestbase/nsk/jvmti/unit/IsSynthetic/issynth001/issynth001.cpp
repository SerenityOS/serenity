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
#define JVM_ACC_SYNTHETIC     0x1000

static jvmtiEnv *jvmti = NULL;
static jvmtiCapabilities caps;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_issynth001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_issynth001(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_issynth001(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res;
    jvmtiError err;

    if (options != NULL && strcmp(options, "printdump") == 0) {
        printdump = JNI_TRUE;
    }

    res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
    if (res != JNI_OK || jvmti == NULL) {
        printf("Wrong result of a valid call to GetEnv!\n");
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

    if (!caps.can_get_synthetic_attribute) {
        printf("Warning: IsMethodSynthetic is not implemented\n");
    }

    return JNI_OK;
}

char const *jbooleanToString(jboolean flag) {
    return ((flag == JNI_TRUE) ? "true" : "false");
}

JNIEXPORT jint JNICALL
checkClassMethods(jclass klass) {
    jvmtiError err;
    jmethodID mid;
    jboolean isSynthetic;
    jint meth_cnt = 0;
    jmethodID* methods_ptr = NULL;
    char* name_ptr = NULL;
    char* sign_ptr = NULL;
    int i;

    err = jvmti->GetClassMethods(klass, &meth_cnt, &methods_ptr);
    if (err != JVMTI_ERROR_NONE) {
        printf("GetClassMethods unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return STATUS_FAILED;
    }

    for (i = 0; i < meth_cnt; i++) {
        jint mods;
        mid = methods_ptr[i];
        err = jvmti->GetMethodName(mid, &name_ptr,
                                    &sign_ptr, (char **) NULL);
        if (err != JVMTI_ERROR_NONE) {
            printf("(GetMethodName#%d) unexpected error: %s (%d)\n",
                   i, TranslateError(err), err);
            return STATUS_FAILED;
        }

        printf("Method # %d; name: %s, signature: %s\n", i, name_ptr, sign_ptr);
        err = jvmti->IsMethodSynthetic(mid, &isSynthetic);
        if (err != JVMTI_ERROR_NONE) {
            printf("(IsMethodSynthetic#%d) unexpected error: %s (%d)\n",
                   i, TranslateError(err), err);
            result = STATUS_FAILED;
            continue;
        }

        err = jvmti->GetMethodModifiers(mid, &mods);
        if (err != JVMTI_ERROR_NONE) {
            printf("(GetMethodModifiers#%d) unexpected error: %s (%d)\n",
                   i, TranslateError(err), err);
            result = STATUS_FAILED;
            continue;
        }
        printf("\tACC_SYNTHETIC bit: %d\n", ((mods & JVM_ACC_SYNTHETIC) != 0));
        if ((mods & JVM_ACC_SYNTHETIC) && isSynthetic) {
            printf("\tIsMethodSynthetic result and ACC_SYNTHETIC bit are matched\n");
        } else if ((mods & JVM_ACC_SYNTHETIC) || isSynthetic) {
            printf("Failure: IsMethodSynthetic result and ACC_SYNTHETIC bit don't match\n");
            result = STATUS_FAILED;
            continue;
        }
    }
    printf("\n");
    return result;
}

JNIEXPORT jint JNICALL
checkClassFields(jclass klass) {
    jvmtiError err;
    jfieldID fid;
    jboolean isSynthetic;
    jint fld_cnt = 0;
    jfieldID* fields_ptr = NULL;
    char* name_ptr = NULL;
    char* sign_ptr = NULL;
    int i;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        return STATUS_FAILED;
    }

    err = jvmti->GetClassFields(klass, &fld_cnt, &fields_ptr);
    if (err != JVMTI_ERROR_NONE) {
        printf("GetClassFields unexpected error: %s (%d)\n",
               TranslateError(err), err);
        return STATUS_FAILED;
    }

    for (i = 0; i < fld_cnt; i++) {
        jint mods;
        fid = fields_ptr[i];
        err = jvmti->GetFieldName(klass, fid, &name_ptr,
                                    &sign_ptr, (char **) NULL);
        if (err != JVMTI_ERROR_NONE) {
            printf("(GetFieldName#%d) unexpected error: %s (%d)\n",
                   i, TranslateError(err), err);
            return STATUS_FAILED;
        }

        printf("Field # %d; name: %s, signature: %s\n", i, name_ptr, sign_ptr);
        err = jvmti->IsFieldSynthetic(klass, fid, &isSynthetic);
        if (err != JVMTI_ERROR_NONE) {
            printf("(IsFieldSynthetic#%d) unexpected error: %s (%d)\n",
                   i, TranslateError(err), err);
            result = STATUS_FAILED;
            continue;
        }

        err = jvmti->GetFieldModifiers(klass, fid, &mods);
        if (err != JVMTI_ERROR_NONE) {
            printf("(GetFieldModifiers#%d) unexpected error: %s (%d)\n",
                   i, TranslateError(err), err);
            result = STATUS_FAILED;
            continue;
        }
        printf("\tACC_SYNTHETIC bit: %d\n", ((mods & JVM_ACC_SYNTHETIC) != 0));
        if ((mods & JVM_ACC_SYNTHETIC) && isSynthetic) {
            printf("\tIsFieldSynthetic result and ACC_SYNTHETIC bit are matched\n");
        } else if ((mods & JVM_ACC_SYNTHETIC) || isSynthetic) {
            printf("Failure: IsFieldSynthetic result and ACC_SYNTHETIC bit don't match\n");
            result = STATUS_FAILED;
            continue;
        }
    }

    return result;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_unit_IsSynthetic_issynth001_check(JNIEnv *env,
        jclass cls, jclass klass) {

    jvmtiError err;
    char* class_sign = NULL;

    if (!caps.can_get_synthetic_attribute) {
        return result;
    }

    err = jvmti->GetClassSignature(cls, &class_sign, (char **) NULL);
    if (err != JVMTI_ERROR_NONE) {
        printf("GetSourceFileName unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return result;
    } else {
        printf("Class signature: %s\n", class_sign);
    }

   /*
    * The following synthetic methods are expected:
    *   Name: class$,     Signature: (Ljava/lang/String;)Ljava/lang/Class;
    *   Name: access$000, Signature: (Lnsk/jvmti/unit/IsMethodSynthetic/issynth001;)I
    */

    result = checkClassMethods(cls);
    if (result != PASSED) {
        return result;
    }

    err = jvmti->GetClassSignature(klass, &class_sign, (char **) NULL);
    if (err != JVMTI_ERROR_NONE) {
        printf("GetSourceFileName unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return result;
    } else {
        printf("Class signature: %s\n", class_sign);
    }

   /* Just a sanity check of methods of Inner class.
    * No synthetic methods are expected here.
    */
    result = checkClassMethods(klass);
    if (result != PASSED) {
        return result;
    }

   /* Check of fields of Inner class.
    * Just one synthetic field is expected here:
    *   Name: this$0, Signature: Lnsk/jvmti/unit/IsSynthetic/issynth001;
    */
    result = checkClassFields(klass);

    return result;
}

JNIEXPORT jint JNICALL Java_issynth001_getRes(JNIEnv *env, jclass cls) {
    return result;
}

}
