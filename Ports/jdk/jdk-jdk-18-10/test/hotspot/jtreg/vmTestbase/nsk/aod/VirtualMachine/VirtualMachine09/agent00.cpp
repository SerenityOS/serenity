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

#include <stdio.h>
#include <string.h>
#include <jni.h>
#include <aod.h>

extern "C" {

/*
 * Test checks following spec clause: "Agent_OnAttach function is invoked even if the agent library was loaded
 * prior to invoking this method"
 *
 * This agent is loaded as static agent via 'agentlib:' VM option and also dynamically attached, so this agent
 * has both Agent_OnLoad and Agent_OnAttach.
 */

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *vm, char *optionsString, void *reserved) {
    // can't use NSK_DISPLAY since needed for nsk_ functions initialization isn't done here
    printf("Agent_OnLoad: agent is loaded\n");
    fflush(stdout);
    return JNI_OK;
}

JNIEXPORT jint JNICALL Agent_OnAttach(JavaVM *vm, char *optionsString, void *reserved) {
    JNIEnv* jni = NULL;
    Options* options = NULL;
    const char* agentName;

    options = (Options*) nsk_aod_createOptions(optionsString);
    if (!NSK_VERIFY(options != NULL))
        return JNI_ERR;

    agentName = nsk_aod_getOptionValue(options, NSK_AOD_AGENT_NAME_OPTION);

    jni = (JNIEnv*) nsk_aod_createJNIEnv(vm);
    if (jni == NULL)
        return JNI_ERR;

    NSK_DISPLAY1("%s: initialization was done\n", agentName);

    if (!NSK_VERIFY(nsk_aod_agentLoaded(jni, agentName)))
        return JNI_ERR;

    nsk_aod_agentFinished(jni, agentName, 1);

    return JNI_OK;
}

}
