/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

static jvmtiEnv *jvmti = NULL;
static jint result = PASSED;
static jboolean printdump = JNI_FALSE;

#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_clsldrclss00x(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_clsldrclss00x(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_clsldrclss00x(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jint res;

    if (options != NULL && strcmp(options, "printdump") == 0) {
        printdump = JNI_TRUE;
    }

    res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
    if (res != JNI_OK || jvmti == NULL) {
        printf("Wrong result of a valid call to GetEnv!\n");
        return JNI_ERR;
    }

    return JNI_OK;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_unit_clsldrclss00x_check(JNIEnv *env, jclass appCls, jclass objCls) {
    jvmtiError err;
    jobject appClassloader;
    jobject objClassloader;
    jclass *classes;
    jint classCount;
    jboolean found;
    jint i;

    if (jvmti == NULL) {
        printf("JVMTI client was not properly loaded!\n");
        return STATUS_FAILED;
    }

    err = jvmti->GetClassLoader(appCls, &appClassloader);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetClassLoader app) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return result;
    }
    if (appClassloader == NULL) {
        printf("(GetClassLoader app) unexpected loader - NULL\n");
        result = STATUS_FAILED;
        return result;
    }

    err = jvmti->GetClassLoader(objCls, &objClassloader);
    if (err != JVMTI_ERROR_NONE) {
        printf("(GetClassLoader obj) unexpected error: %s (%d)\n",
               TranslateError(err), err);
        result = STATUS_FAILED;
        return result;
    }
    if (objClassloader != NULL) {
        printf("(GetClassLoader obj) unexpected loader - !NULL\n");
        result = STATUS_FAILED;
        return result;
    }

    err = jvmti->GetClassLoaderClasses(appClassloader, &classCount, &classes);
    if (err != JVMTI_ERROR_NONE) {
        printf("Error (GetClassLoaderClasses app): %s (%d)\n", TranslateError(err), err);
        result = STATUS_FAILED;
        return result;
    }
    if (printdump) {
      printf(">>> number of classes in app class loader: %d\n", classCount);
      if (JNI_FALSE) {
        for (i = 0; i < classCount; ++i) {
          char *classSig;
          jclass k = classes[i];
          err = jvmti->GetClassSignature(k, &classSig, NULL);
          if (err != JVMTI_ERROR_NONE) {
            printf("Error (getClassSignature): %s (%d)\n", TranslateError(err), err);
            result = STATUS_FAILED;
          } else {
            printf("    %s\n", classSig);
            err = jvmti->Deallocate((unsigned char*)classSig);
            if (err != JVMTI_ERROR_NONE) {
              printf("Error (Deallocate): %s (%d)\n", TranslateError(err), err);
              result = STATUS_FAILED;
            }
          }
        }
      }
    }
    found = JNI_FALSE;
    for (i = 0; i < classCount; ++i) {
      jclass k = classes[i];
      if (env->IsSameObject(k, appCls)) {
        if (printdump) {
          printf(">>> found app class in app class loader\n");
        }
        found = JNI_TRUE;
        break;
      }
    }
    if (!found) {
        printf("Error: didn't find app class in app class loader\n");
        result = STATUS_FAILED;
    }

    err = jvmti->GetClassLoaderClasses(objClassloader, &classCount, &classes);
    if (err != JVMTI_ERROR_NONE) {
        printf("Error (GetClassLoaderClasses obj): %s (%d)\n", TranslateError(err), err);
        result = STATUS_FAILED;
    }
    if (printdump) {
      printf(">>> number of classes in bootstrap class loader: %d\n", classCount);
    }
    found = JNI_FALSE;
    for (i = 0; i < classCount; ++i) {
      jclass k = classes[i];
      if (env->IsSameObject(k, objCls)) {
        if (printdump) {
          printf(">>> found Object class in bootstrap class loader\n");
        }
        found = JNI_TRUE;
        break;
      }
    }
    if (!found) {
        printf("Error: didn't find Object class in bootstrap class loader\n");
        result = STATUS_FAILED;
    }

    if (printdump) {
        printf(">>> ... done\n");
    }

    return result;
}

}
