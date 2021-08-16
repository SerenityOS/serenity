/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include <string.h>
#include <stdlib.h>
#include <jni.h>
#include <jni_tools.h>
#include <nsk_tools.h>
#include <aod.h>

extern "C" {

static volatile int internalError = 0;

/*
 * This function can be used to inform AOD framework that some non critical for test logic
 * error happened inside shared function (e.g. JVMTI Deallocate failed).
 *
 * If this function was called status of all finishing AOD agents is changed to failed.
 */

void nsk_aod_internal_error() {
    NSK_COMPLAIN0("WARNING: some error happened inside common function, see log for details\n");
    internalError = 1;
}

void nsk_free_options(Options* options) {
  if (options != NULL) {
    int i;
    for (i = 0; i < NSK_AOD_MAX_OPTIONS; i++) {
      if (options->names[i] != NULL) {
        free(options->names[i]);
      }
      if (options->values[i] != NULL) {
        free(options->values[i]);
      }
    }
    free(options);
  }
}
/*
 * Work with agent options
 */

/*
 * Parse options and create structure Options
 */
static Options* nsk_aod_createOptionsObject(char* optionsString) {
    int i = 0;
    Options* options;
    char* name;
    char* value;
    char* sep;

    if (optionsString == NULL) {
      NSK_COMPLAIN0("options were not passed to the native agent\n");
      return NULL;
    }
    options = (Options*) malloc(sizeof(Options));
    memset(options, 0, sizeof(Options));
    options->size = 0;
    name = optionsString;
    while (name != NULL && i < NSK_AOD_MAX_OPTIONS) {
      sep = strchr(name, '=');
      if (sep == NULL) { // name not found
        NSK_COMPLAIN1("Invalid options format: '%s'\n", optionsString);
        nsk_free_options(options);
        return NULL;
      }
      *sep = '\0';
      options->names[i] =  strdup(name);
      value = sep + 1;
      if (*value == '\0') { // value not found
        NSK_COMPLAIN1("Option '%s' is empty\n", options->names[i]);
        nsk_free_options(options);
        return NULL;
      }
      sep = strchr(value, ' ');
      if (sep != NULL) {
        *sep = '\0';
        name = sep + 1;
      } else {
        name = strchr(value, '\0');
      }
      options->values[i] = strdup(value);
      i++;

      if (*name == '\0') {
        name = NULL;
      }
    }
    if (name != NULL) {
      NSK_COMPLAIN1("WARNING: not all options were parsed, only %d options can be specified\n",
                    NSK_AOD_MAX_OPTIONS);
    }
    options->size = i;
    return options;
}

Options* nsk_aod_createOptions(char* optionsString) {
    Options* options;

    if (!NSK_VERIFY((options = (Options*) nsk_aod_createOptionsObject(optionsString)) != NULL))
        return NULL;

    if (!NSK_VERIFY(nsk_aod_optionSpecified(options, NSK_AOD_AGENT_NAME_OPTION))) {
        NSK_COMPLAIN0("Agent name wasn't specified\n");
        return NULL;
    }

    /*
     * verbose mode is true by default
     */
    nsk_setVerboseMode(NSK_TRUE);

    if (nsk_aod_optionSpecified(options, NSK_AOD_VERBOSE_OPTION)) {
        if (strcmp(nsk_aod_getOptionValue(options, NSK_AOD_VERBOSE_OPTION), "false") == 0)
            nsk_setVerboseMode(NSK_FALSE);
    }

    return options;
}

const char* nsk_aod_getOptionValue(Options* options, const char* option) {
    int i;

    if (!NSK_VERIFY(options != NULL)) {
        NSK_COMPLAIN0("Options NULL\n");
        return NULL;
    }

    for (i = 0; i < options->size; i++) {
        if (strcmp(option, options->names[i]) == 0) {
            return options->values[i];
        }
    }

    NSK_COMPLAIN1("Option '%s' isn't defined\n", option);

    return NULL;
}

int nsk_aod_optionSpecified(Options* options, const char* option) {
    int i;

    if (!NSK_VERIFY(options != NULL)) {
        NSK_COMPLAIN0("Options NULL\n");
        return NSK_FALSE;
    }

    for (i = 0; i < options->size; i++) {
        if (strcmp(option, options->names[i]) == 0) {
            return NSK_TRUE;
        }
    }

    return NSK_FALSE;
}

/*
 * Agent synchronization with target application
 */

static const char* TARGET_APP_CLASS_NAME = "nsk/share/aod/TargetApplicationWaitingAgents";

static const char* AGENT_LOADED_METHOD_NAME = "agentLoaded";
static const char* AGENT_LOADED_METHOD_SIGNATURE = "(Ljava/lang/String;)V";

static const char* AGENT_FINISHED_METHOD_NAME = "agentFinished";
static const char* AGENT_FINISHED_METHOD_SIGNATURE = "(Ljava/lang/String;Z)V";

static jclass targetAppClass = NULL;
static jmethodID agentLoadedMethod = NULL;
static jmethodID agentFinishedMethod = NULL;

// this function is used to notify target application that native agent has been loaded
int nsk_aod_agentLoaded(JNIEnv* jni, const char* agentName) {
    jstring agentNameString;

    NSK_DISPLAY1("Agent %s is loaded\n", agentName);

    if (targetAppClass == NULL) {
        /*
         * FindClass returns local reference, to cache reference to target application class
         * global reference should be created
         */
        jclass localTargetAppClass;
        if (!NSK_JNI_VERIFY(jni, (localTargetAppClass =
            jni->FindClass(TARGET_APP_CLASS_NAME)) != NULL)) {
            return NSK_FALSE;
        }

        if (!NSK_JNI_VERIFY(jni, (targetAppClass = (jclass)
            jni->NewGlobalRef(localTargetAppClass)) != NULL)) {
            return NSK_FALSE;
        }
    }

    if (agentLoadedMethod == NULL) {
        if (!NSK_JNI_VERIFY(jni, (agentLoadedMethod =
            jni->GetStaticMethodID(targetAppClass, AGENT_LOADED_METHOD_NAME, AGENT_LOADED_METHOD_SIGNATURE)) != NULL))
            return NSK_FALSE;
    }

    if (!NSK_JNI_VERIFY(jni, (agentNameString =
        jni->NewStringUTF(agentName)) != NULL))
        return NSK_FALSE;

    jni->CallStaticVoidMethod(targetAppClass, agentLoadedMethod, agentNameString);

    return NSK_TRUE;
}

// this function is used to notify target application that native agent has been finished execution
int nsk_aod_agentFinished(JNIEnv* jni, const char* agentName, int success) {
    jstring agentNameString;

    if (!targetAppClass) {
        NSK_COMPLAIN1(
            "%s: TEST LOGIC ERROR: method 'agentFinished' was called before "
            "targetAppClass was initialized\n",
            agentName);
        return NSK_FALSE;
    }

    if (internalError && success) {
        success = 0;
        NSK_COMPLAIN1(
            "Status of agent '%s' is 'passed', but some error happened during test execution "
            "(see log for details), change agent status to 'failed'\n",
            agentName);
    }

    NSK_DISPLAY2("Agent %s finished (success: %d)\n", agentName, success);

    if (agentFinishedMethod == NULL) {
        if (!NSK_JNI_VERIFY(jni, (agentFinishedMethod =
            jni->GetStaticMethodID(targetAppClass, AGENT_FINISHED_METHOD_NAME, AGENT_FINISHED_METHOD_SIGNATURE)) != NULL))
            return NSK_FALSE;
    }

    if (!NSK_JNI_VERIFY(jni, (agentNameString = jni->NewStringUTF(agentName)) != NULL))
        return NSK_FALSE;

    jni->CallStaticVoidMethod(targetAppClass, agentFinishedMethod, agentNameString, success ? JNI_TRUE : JNI_FALSE);

    return NSK_TRUE;
}

/*
 * Auxiliary functions
 */

// JNI env creation

JNIEnv* nsk_aod_createJNIEnv(JavaVM* vm) {
    JNIEnv* jni;
    vm->GetEnv((void**)&jni, JNI_VERSION_1_2);

    NSK_VERIFY(jni != NULL);

    return jni;
}

}
