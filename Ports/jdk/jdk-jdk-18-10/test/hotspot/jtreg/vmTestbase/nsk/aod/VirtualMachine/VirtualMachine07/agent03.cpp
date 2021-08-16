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

#define AGENT_NAME "VMNativeAgent03"

#define ON_ATTACH_EXIT_CODE 10

/*
 */

JNIEXPORT jint JNICALL Agent_OnAttach(JavaVM *vm, char *optionsString, void *reserved) {
    JNIEnv* jni;

    jni = (JNIEnv*) nsk_aod_createJNIEnv(vm);
    if (jni == NULL)
        return JNI_ERR;

    // can't use NSK_DISPLAY since needed for nsk_ functions initialization isn't done here

    printf("%s: initialization was done\n", AGENT_NAME);
    fflush(stdout);

    if (!NSK_VERIFY(nsk_aod_agentLoaded(jni, AGENT_NAME)))
        return JNI_ERR;

    nsk_aod_agentFinished(jni, AGENT_NAME, 1);

    printf("%s: warning: agent is intentionally exiting from Agent_OnAttach with error code %d\n", AGENT_NAME, ON_ATTACH_EXIT_CODE);
    fflush(stdout);

    return ON_ATTACH_EXIT_CODE;
}

}
