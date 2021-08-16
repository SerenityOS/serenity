/*
 * Copyright (c) 2004, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include <stdlib.h>
#include <string.h>
#include <atomic>
#include "jni_tools.h"
#include "jvmti_tools.h"
#include "Injector.h"
#include "agent_common.h"

#define PASSED 0

extern "C" {

/* ========================================================================== */

#define DEFAULT_MAX_NUMBER_OF_CLASSES 100
#define DEFAULT_NUMBER_OF_SAMPLES 10
#define DEFAULT_SAMPLING_INTERVAL 100
#define DEFAULT_PACKAGE_NAME "nsk/jvmti/scenarios/hotswap"
#define PROFILE_CLASS_NAME "nsk/share/jvmti/ProfileCollector"

enum {
    VM_MODE_COMPILED    = 0,
    VM_MODE_INTERPRETED = 1,
    VM_MODE_MIXED       = 2
};

/* scaffold objects */
static jlong timeout = 0;

/* test options */
static int number_of_samples;
static jlong sampling_interval;
static const char* package_name;
static size_t package_name_length;
static int vm_mode = VM_MODE_COMPILED;
static int bci_mode = BCI_MODE_EMCP;
static int sync_freq = 0;

static jclass profile_klass = NULL;
static jfieldID count_field = NULL;

/* test objects */
static int max_classes;
static char** names = NULL;
static jvmtiClassDefinition* old_class_def = NULL;
static jvmtiClassDefinition* new_class_def = NULL;
static int classCount = 0;
/* lock to access classCount */
static jrawMonitorID classLoadLock = NULL;
static int newFlag = NSK_FALSE;

/* ========================================================================== */

static int redefine(jvmtiEnv* jvmti, jvmtiClassDefinition* class_def) {

    if (!NSK_VERIFY(classCount != 0))
        return NSK_FALSE;

    NSK_DISPLAY1("Redefining %d classes...\n", classCount);

    if (!NSK_JVMTI_VERIFY(jvmti->RedefineClasses(classCount, class_def)))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* ========================================================================== */

/** callback functions **/

static void JNICALL
ClassFileLoadHook(jvmtiEnv *jvmti_env, JNIEnv *jni_env,
        jclass class_being_redefined, jobject loader,
        const char* name, jobject protection_domain,
        jint class_data_len, const unsigned char* class_data,
        jint *new_class_data_len, unsigned char** new_class_data) {
    jint name_len;

    if (name != NULL &&
            class_being_redefined == NULL &&
            (strcmp(name, PROFILE_CLASS_NAME) != 0) &&
            (strncmp(name, package_name, package_name_length) == 0)) {
        if (!NSK_JVMTI_VERIFY(jvmti_env->RawMonitorEnter(classLoadLock))) {
            nsk_jvmti_setFailStatus();
            return;
        }
        // use while instead of if to exit the block on error
        while (classCount < max_classes) {
            NSK_DISPLAY1("ClassFileLoadHook: %s\n", name);
            name_len = (jint)strlen(name) + 1;
            if (!NSK_JVMTI_VERIFY(jvmti_env->Allocate(name_len, (unsigned char**)& names[classCount]))) {
                nsk_jvmti_setFailStatus();
                break;
            }
            memcpy(names[classCount], name, name_len);
            if (!NSK_JVMTI_VERIFY(jvmti_env->Allocate(class_data_len, (unsigned char**)
                    & old_class_def[classCount].class_bytes))) {
                nsk_jvmti_setFailStatus();
                break;
            }
            memcpy((unsigned char*)old_class_def[classCount].class_bytes,
                class_data, class_data_len);
            old_class_def[classCount].class_byte_count = class_data_len;
            classCount++;
            break;
        }
        if (!NSK_JVMTI_VERIFY(jvmti_env->RawMonitorExit(classLoadLock))) {
            nsk_jvmti_setFailStatus();
        }
    }
}

static std::atomic<int> CompiledMethodLoadEventsCount(0);

static void JNICALL
CompiledMethodLoad(jvmtiEnv *jvmti_env, jmethodID method,
        jint code_size, const void* code_addr, jint map_length,
        const jvmtiAddrLocationMap* map, const void* compile_info) {
    char *name = NULL;
    char *signature = NULL;

    CompiledMethodLoadEventsCount++;

    if (!NSK_JVMTI_VERIFY(jvmti_env->GetMethodName(method, &name, &signature, NULL))) {
        nsk_jvmti_setFailStatus();
        return;
    }
    NSK_DISPLAY3("CompiledMethodLoad event: %s%s (0x%p)\n",
        name, signature, code_addr);
    if (name != NULL)
        jvmti_env->Deallocate((unsigned char*)name);
    if (signature != NULL)
        jvmti_env->Deallocate((unsigned char*)signature);
}

static std::atomic<int> SingleStepEventsCount(0);

static void JNICALL
SingleStep(jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread,
        jmethodID method, jlocation location) {

    SingleStepEventsCount++;
}

static std::atomic<int> ExceptionEventsCount(0);

static void JNICALL
Exception(jvmtiEnv *jvmti_env, JNIEnv *jni_env, jthread thread,
        jmethodID method, jlocation location, jobject exception,
        jmethodID catch_method, jlocation catch_location) {

    if (sync_freq && ((ExceptionEventsCount % sync_freq) == 0)) {

        if (nsk_getVerboseMode()) {
            jclass klass = NULL;
            char *signature = NULL;

            if (!NSK_JNI_VERIFY(jni_env, (klass = jni_env->GetObjectClass(exception)) != NULL)) {
                nsk_jvmti_setFailStatus();
                return;
            }
            if (!NSK_JVMTI_VERIFY(jvmti_env->GetClassSignature(klass, &signature, NULL))) {
                nsk_jvmti_setFailStatus();
                return;
            }
            NSK_DISPLAY2("Exception event %d: %s\n",
                ExceptionEventsCount.load(), signature);
            if (signature != NULL)
                jvmti_env->Deallocate((unsigned char*)signature);
        }

        if (!redefine(jvmti_env, (bci_mode != BCI_MODE_EMCP && newFlag) ?
                new_class_def : old_class_def))
            nsk_jvmti_setFailStatus();

        NSK_DISPLAY1("SingleStepEventsCount: %d\n", SingleStepEventsCount.load());
        if (vm_mode == VM_MODE_MIXED) {
            if (!NSK_JVMTI_VERIFY(jvmti_env->SetEventNotificationMode(
                    ((newFlag) ? JVMTI_DISABLE : JVMTI_ENABLE),
                    JVMTI_EVENT_SINGLE_STEP, NULL)))
                nsk_jvmti_setFailStatus();
        }

        if (nsk_getVerboseMode() && bci_mode != BCI_MODE_EMCP) {
            jint profileCount = jni_env->GetStaticIntField(profile_klass, count_field);
            NSK_DISPLAY1("profileCount: %d\n", profileCount);
        }

        newFlag = (newFlag) ? NSK_FALSE : NSK_TRUE;
    }

    ExceptionEventsCount++;
}

/* ========================================================================== */

static jrawMonitorID waitLock = NULL;

static int prepare(jvmtiEnv* jvmti, JNIEnv* jni) {
    int i;

    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, NULL)))
        return NSK_FALSE;

    if (vm_mode != VM_MODE_COMPILED) {
        if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_SINGLE_STEP, NULL)))
            return NSK_FALSE;
    }

    if (!NSK_JVMTI_VERIFY(jvmti->CreateRawMonitor("waitLock", &waitLock)))
        return NSK_FALSE;

    for (i = 0; i < classCount; i++) {
        NSK_DISPLAY1("Find class: %s\n", names[i]);
        if (!NSK_JNI_VERIFY(jni, (old_class_def[i].klass = jni->FindClass(names[i])) != NULL))
            return NSK_FALSE;

        if (!NSK_JNI_VERIFY(jni, (old_class_def[i].klass = (jclass)
                jni->NewGlobalRef(old_class_def[i].klass)) != NULL))
            return NSK_FALSE;
    }

    if (bci_mode != BCI_MODE_EMCP) {
        NSK_DISPLAY1("Find class: %s\n", PROFILE_CLASS_NAME);
        if (!NSK_JNI_VERIFY(jni, (profile_klass = jni->FindClass(PROFILE_CLASS_NAME)) != NULL))
            return NSK_FALSE;

        if (!NSK_JNI_VERIFY(jni, (profile_klass = (jclass)
                jni->NewGlobalRef(profile_klass)) != NULL))
            return NSK_FALSE;

        if (!NSK_JNI_VERIFY(jni, (count_field =
                jni->GetStaticFieldID(profile_klass,
                                      (bci_mode == BCI_MODE_CALL) ? "callCount" : "allocCount",
                                      "I")) != NULL))
            return NSK_FALSE;

        if (!NSK_JVMTI_VERIFY(jvmti->Allocate(classCount * sizeof(jvmtiClassDefinition),
                (unsigned char**) &new_class_def)))
            return NSK_FALSE;

        for (i = 0; i < classCount; i++) {
            new_class_def[i].klass = old_class_def[i].klass;
            if (!Inject(old_class_def[i].class_bytes,
                    old_class_def[i].class_byte_count,
                    (unsigned char**) &new_class_def[i].class_bytes,
                    &new_class_def[i].class_byte_count, bci_mode))
                return NSK_FALSE;
        }
    }

    if (sync_freq) {
        if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_EXCEPTION, NULL)))
            return NSK_FALSE;
    }

    return NSK_TRUE;
}

/* ========================================================================== */

static int wait_for(jvmtiEnv* jvmti, jlong millis) {

    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorEnter(waitLock)))
        return NSK_FALSE;

    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorWait(waitLock, millis)))
        nsk_jvmti_setFailStatus();

    if (!NSK_JVMTI_VERIFY(jvmti->RawMonitorExit(waitLock)))
        return NSK_FALSE;

    return NSK_TRUE;
}

/* ========================================================================== */

/** Agent algorithm. */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {
    int i;

    if (!nsk_jvmti_waitForSync(timeout))
        return;

    if (!prepare(jvmti, jni)) {
        nsk_jvmti_setFailStatus();
        return;
    }

    /* resume debugee and wait for sync */
    if (!nsk_jvmti_resumeSync())
        return;
    if (!nsk_jvmti_waitForSync(timeout))
        return;

    if (sync_freq) {
        if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_EXCEPTION, NULL)))
            nsk_jvmti_setFailStatus();
    } else {

        for (i = 0; i < number_of_samples && !nsk_jvmti_isFailStatus(); i++) {
            wait_for(jvmti, sampling_interval);

            if (!redefine(jvmti, (bci_mode != BCI_MODE_EMCP && newFlag) ?
                    new_class_def : old_class_def))
                nsk_jvmti_setFailStatus();

            NSK_DISPLAY1("SingleStepEventsCount: %d\n", SingleStepEventsCount.load());
            if (vm_mode == VM_MODE_MIXED) {
                if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(
                        (((i % 2) == 0) ? JVMTI_DISABLE : JVMTI_ENABLE),
                        JVMTI_EVENT_SINGLE_STEP, NULL)))
                    nsk_jvmti_setFailStatus();
            }

            if (nsk_getVerboseMode() && bci_mode != BCI_MODE_EMCP) {
                jint profileCount = jni->GetStaticIntField(profile_klass, count_field);
                NSK_DISPLAY1("profileCount: %d\n", profileCount);
            }

            newFlag = (newFlag) ? NSK_FALSE : NSK_TRUE;
        }

    }

    if (vm_mode != VM_MODE_COMPILED) {
        if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_DISABLE, JVMTI_EVENT_SINGLE_STEP, NULL)))
            nsk_jvmti_setFailStatus();
    }

    if (!nsk_jvmti_resumeSync())
        return;
}

/* ========================================================================== */

/** Agent library initialization. */
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiEnv* jvmti = NULL;
    jvmtiCapabilities caps;
    jvmtiEventCallbacks callbacks;
    const char* optValue;

    NSK_DISPLAY0("Agent_OnLoad\n");

    /* init framework and parse options */
    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    timeout = nsk_jvmti_getWaitTime() * 60 * 1000;

    /* get options */
    number_of_samples = nsk_jvmti_findOptionIntValue("samples",
        DEFAULT_NUMBER_OF_SAMPLES);
    if (!NSK_VERIFY(number_of_samples > 0))
        return JNI_ERR;
    NSK_DISPLAY1("samples: %d\n", number_of_samples);

    sampling_interval = nsk_jvmti_findOptionIntValue("interval",
        DEFAULT_SAMPLING_INTERVAL);
    if (!NSK_VERIFY(sampling_interval > 0))
        return JNI_ERR;
    NSK_DISPLAY1("interval: %d\n", sampling_interval);

    package_name = nsk_jvmti_findOptionStringValue("package",
        DEFAULT_PACKAGE_NAME);
    if (!NSK_VERIFY(package_name != NULL))
        return JNI_ERR;
    NSK_DISPLAY1("package: %s\n", package_name);

    package_name_length = strlen(package_name);
    if (!NSK_VERIFY(package_name_length > 0))
        return JNI_ERR;

    max_classes = nsk_jvmti_findOptionIntValue("classes",
        DEFAULT_MAX_NUMBER_OF_CLASSES);
    if (!NSK_VERIFY(max_classes > 0))
        return JNI_ERR;
    NSK_DISPLAY1("classes: %d\n", max_classes);

    optValue = nsk_jvmti_findOptionValue("mode");
    if (optValue != NULL) {
        if (strcmp(optValue, "compiled") == 0)
            vm_mode = VM_MODE_COMPILED;
        else if (strcmp(optValue, "interpreted") == 0)
            vm_mode = VM_MODE_INTERPRETED;
        else if (strcmp(optValue, "mixed") == 0)
            vm_mode = VM_MODE_MIXED;
        else {
            NSK_COMPLAIN1("Unknown option value: mode=%s\n", optValue);
            return JNI_ERR;
        }
    }

    optValue = nsk_jvmti_findOptionValue("bci");
    if (optValue != NULL) {
        if (strcmp(optValue, "emcp") == 0)
            bci_mode = BCI_MODE_EMCP;
        else if (strcmp(optValue, "call") == 0)
            bci_mode = BCI_MODE_CALL;
        else if (strcmp(optValue, "alloc") == 0)
            bci_mode = BCI_MODE_ALLOC;
        else {
            NSK_COMPLAIN1("Unknown option value: bci=%s\n", optValue);
            return JNI_ERR;
        }
    }

    sync_freq = nsk_jvmti_findOptionIntValue("sync", 0);
    if (!NSK_VERIFY(sync_freq >= 0))
        return JNI_ERR;
    NSK_DISPLAY1("sync: %d\n", sync_freq);

    /* create JVMTI environment */
    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    /* allocate tables for classes */
    if (!NSK_JVMTI_VERIFY(jvmti->Allocate(max_classes * sizeof(char*), (unsigned char**) &names)))
        return JNI_ERR;

    if (!NSK_JVMTI_VERIFY(jvmti->Allocate(max_classes * sizeof(jvmtiClassDefinition),
            (unsigned char**) &old_class_def)))
        return JNI_ERR;

    if (!NSK_JVMTI_VERIFY(jvmti->CreateRawMonitor("classLoadLock", &classLoadLock)))
        return JNI_ERR;

    /* add capabilities */
    memset(&caps, 0, sizeof(caps));
    caps.can_redefine_classes = 1;
    caps.can_generate_compiled_method_load_events = 1;
    if (vm_mode != VM_MODE_COMPILED) {
        caps.can_generate_single_step_events = 1;
    }
    if (sync_freq) {
        caps.can_generate_exception_events = 1;
    }
    if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps)))
        return JNI_ERR;

    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    /* set event callbacks */
    memset(&callbacks, 0, sizeof(callbacks));
    callbacks.ClassFileLoadHook = &ClassFileLoadHook;
    callbacks.CompiledMethodLoad = &CompiledMethodLoad;
    if (vm_mode != VM_MODE_COMPILED) {
        callbacks.SingleStep = &SingleStep;
    }
    if (sync_freq) {
        callbacks.Exception = &Exception;
    }
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks))))
        return JNI_ERR;

    /* enable events */
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, NULL)))
        return JNI_ERR;
    if (!NSK_JVMTI_VERIFY(jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_COMPILED_METHOD_LOAD, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ========================================================================== */

}
