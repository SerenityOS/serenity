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
#ifndef NSK_SHARE_AOD_H
#define NSK_SHARE_AOD_H

#include <jni.h>
#include <jni_tools.h>
#include <nsk_tools.h>

extern "C" {

/*
 * This function can be used to inform AOD framework that some non-critical for test logic
 * error happened inside shared function (e.g. JVMTI Deallocate failed).
 *
 * If this function was called status of all finishing AOD agents is changed to failed.
 */

void nsk_aod_internal_error();

/*
 * Work with agent options
 */

#define NSK_AOD_MAX_OPTIONS 10

#define NSK_AOD_AGENT_NAME_OPTION "-agentName"
#define NSK_AOD_VERBOSE_OPTION "-verbose"

typedef struct {
    char* names[NSK_AOD_MAX_OPTIONS];
    char* values[NSK_AOD_MAX_OPTIONS];
    int size;
} Options;

Options* nsk_aod_createOptions(char* optionsString);

const char* nsk_aod_getOptionValue(Options* options, const char* option);

int nsk_aod_optionSpecified(Options* options, const char* option);

/*
 * Agent synchronization with target application
 */

// this function is used to notify target application that native agent has been loaded
int nsk_aod_agentLoaded(JNIEnv* jni, const char* agentName);

// this function is used to notify target application that native agent has been finished execution
int nsk_aod_agentFinished(JNIEnv* jni, const char* agentName, int success);


// JNI env creation

JNIEnv* nsk_aod_createJNIEnv(JavaVM* vm);

}

#endif /* END OF NSK_SHARE_AOD_H */
