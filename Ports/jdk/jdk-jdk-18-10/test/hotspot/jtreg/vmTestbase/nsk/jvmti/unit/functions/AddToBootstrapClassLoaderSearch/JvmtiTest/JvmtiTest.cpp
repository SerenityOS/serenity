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
#include <stdlib.h>
#include <string.h>
#include "jvmti.h"
#include "agent_common.h"

extern "C" {

#define JVMTI_ERROR_CHECK(str,res) if (res != JVMTI_ERROR_NONE) { printf(str); printf("%d\n",res); return res; }
#define JVMTI_ERROR_CHECK_EXPECTED_ERROR(str,res,err) if (res != err) { printf(str); printf("unexpected error %d\n",res); return res; }

#define JVMTI_ERROR_CHECK_VOID(str,res) if (res != JVMTI_ERROR_NONE) { printf(str); printf("%d\n",res); iGlobalStatus = 2; }

#define JVMTI_ERROR_CHECK_EXPECTED_ERROR_VOID(str,res,err) if (res != err) { printf(str); printf("unexpected error %d\n",res); iGlobalStatus = 2; }

static jvmtiEnv *jvmti;
static jint iGlobalStatus = 0;
static jvmtiCapabilities jvmti_caps;
static jvmtiEventCallbacks callbacks;
static int boot_class_count = 0;

static const char* BOOT_CLASS =
    "nsk/jvmti/unit/functions/AddToBootstrapClassLoaderSearch/Boot";

static const char* CLASSPATH = "java.class.path";

static char segment[3000] = ".";

int printdump = 0;


void debug_printf(const char *fmt, ...) {
    va_list args;

    va_start(args, fmt);
    if (printdump) {
        vprintf(fmt, args);
    }
    va_end(args);
}


/*
 * Check that it is not possible to add to the boot class path during the Start phase
 */
void JNICALL
vmStart(jvmtiEnv *jvmti, JNIEnv* jni) {
   jvmtiError res;

   debug_printf("VMStart event done\n");

   res = jvmti->AddToBootstrapClassLoaderSearch(segment);
   JVMTI_ERROR_CHECK_EXPECTED_ERROR_VOID("VMStart: AddToBootstrapClassLoaderSearch returned error ",
      res, JVMTI_ERROR_WRONG_PHASE);
}

/*
 * Check that it is possible to add to the boot class path before VMDeath event return.
 */
void JNICALL
vmDeath(jvmtiEnv *jvmti, JNIEnv* jni) {
   jvmtiError res;

   debug_printf("VMDeath event done\n");

   res = jvmti->AddToBootstrapClassLoaderSearch(segment);
   /* In the live phase, anything other than an existing JAR file is an invalid path.
      So, check that JVMTI_ERROR_ILLEGAL_ARGUMENT error is thrown.
   */
   JVMTI_ERROR_CHECK_EXPECTED_ERROR_VOID("VMDeath: AddToBootstrapClassLoaderSearch returned error ",
      res, JVMTI_ERROR_ILLEGAL_ARGUMENT);
}

/*
 * Check that it is possible to add to the boot class path during the Live phase
 */
void JNICALL vmInit(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thread) {
    jvmtiError res;

    debug_printf("VMInit event  done\n");

    res = jvmti->AddToBootstrapClassLoaderSearch(segment);
    /* In the live phase, anything other than an existing JAR file is an invalid path.
       So, check that JVMTI_ERROR_ILLEGAL_ARGUMENT error is thrown.
    */
    JVMTI_ERROR_CHECK_EXPECTED_ERROR_VOID("VMInit: AddToBootstrapClassLoaderSearch returned error ",
        res, JVMTI_ERROR_ILLEGAL_ARGUMENT);
}

/*
 * Check that it is not possible to add to the boot class path during the Primordial phase
 */
void JNICALL
NativeMethodBind(jvmtiEnv* jvmti, JNIEnv *jni,
                 jthread thread, jmethodID method,
                 void* address, void** new_address_ptr) {
   jvmtiPhase phase;
   jvmtiError res;

   res = jvmti->GetPhase(&phase);
   JVMTI_ERROR_CHECK_VOID("GetPhase returned error", res);

   if (phase == JVMTI_PHASE_PRIMORDIAL) {
      debug_printf("Primordial phase\n");

      res = jvmti->AddToBootstrapClassLoaderSearch(segment);
      JVMTI_ERROR_CHECK_EXPECTED_ERROR_VOID("Primordial: AddToBootstrapClassLoaderSearch returned error ",
         res, JVMTI_ERROR_WRONG_PHASE);
   }
}


void JNICALL
classFileLoadEvent(jvmtiEnv *jvmti_env, JNIEnv *env,
                        jclass redefined_class,
                        jobject loader,const char* name,
                        jobject protection_domain,
                        jint class_data_len,
                        const unsigned char* class_data,
                        jint* new_class_data_len,
                        unsigned char** new_class_data) {

    if (name != NULL && (strcmp(name, BOOT_CLASS) == 0)) {
        debug_printf("Received class file load hook event for class: \n\t%s\n",
            name);
        debug_printf("Received class loader: 0x%p \n", loader);
        /* Check to make sure Boot class got loaded from bootstrap class path.*/

      if (loader == NULL) {
         boot_class_count++;
      }
   }
}


void init_callbacks() {
    memset((void *)&callbacks, 0, sizeof(jvmtiEventCallbacks));

    callbacks.VMInit = vmInit;
    callbacks.VMStart = vmStart;
    callbacks.VMDeath = vmDeath;
    callbacks.NativeMethodBind = NativeMethodBind;
    callbacks.ClassFileLoadHook = classFileLoadEvent;
}


#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_JvmtiTest(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_JvmtiTest(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_JvmtiTest(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM * jvm, char *options, void *reserved) {
    jint res;
    char *idx;

    debug_printf("Agent_OnLoad event done\n");

    if (options && strlen(options) > 0) {
        if (strstr(options, "printdump")) {
            printdump = 1;
        }

        strncpy(segment, options, (size_t) sizeof(segment)/sizeof(char));
        segment[(size_t) sizeof(segment)/sizeof(char) - 1] = 0;
        idx = strchr(segment, ',');
        if (idx != NULL) *idx = 0;
    }

    res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
    if (res < 0) {
        printf("Wrong result of a valid call to GetEnv!\n");
        return JNI_ERR;
    }

    /* Add capabilities */
    res = jvmti->GetPotentialCapabilities(&jvmti_caps);
    JVMTI_ERROR_CHECK("GetPotentialCapabilities returned error", res);

    res = jvmti->AddCapabilities(&jvmti_caps);
    JVMTI_ERROR_CHECK("GetAddCapabilities returned error", res);


    /* Enable events */
    init_callbacks();
    res = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
    JVMTI_ERROR_CHECK("SetEventCallbacks returned error", res);

    res = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_START, NULL);
    JVMTI_ERROR_CHECK("SetEventNotificationMode for VM_START returned error", res);

    res = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, NULL);
    JVMTI_ERROR_CHECK("SetEventNotificationMode for VM_INIT returned error", res);

    res = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_NATIVE_METHOD_BIND, NULL);
    JVMTI_ERROR_CHECK("SetEventNotificationMode for NATIVE_METHOD_BIND returned error", res);

    res = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_DEATH, NULL);
    JVMTI_ERROR_CHECK("SetEventNotificationMode for VM_DEATH returned error", res);

    res = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, NULL);
    JVMTI_ERROR_CHECK("SetEventNotificationMode CLASS_FILE_LOAD_HOOK returned error", res);

    strcat(segment, "/newclass");
    debug_printf("segment=%s\n", segment);
    res = jvmti->AddToBootstrapClassLoaderSearch(segment);
    JVMTI_ERROR_CHECK("AddToBootStrapClassLoaderSearch returned error", res);

    return JNI_OK;
}

JNIEXPORT jint JNICALL
Java_nsk_jvmti_unit_functions_AddToBootstrapClassLoaderSearch_JvmtiTest_GetResult(JNIEnv * env, jclass cls) {

    if (boot_class_count != 1) {
        printf("Error: no ClassFileLoadHook event for Boot class loaded from bootstrap class path\n");
        iGlobalStatus = 2;
    }
    return iGlobalStatus;
}


}
