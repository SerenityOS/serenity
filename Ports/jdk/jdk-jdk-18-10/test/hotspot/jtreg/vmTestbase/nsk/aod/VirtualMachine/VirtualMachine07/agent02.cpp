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

#define AGENT_NAME "VMNativeAgent02"

#define EXPECTED_OPTIONS "VirtualMachine_TestOptions"

/*
 */

JNIEXPORT jint JNICALL Agent_OnAttach(JavaVM *vm, char *optionsString, void *reserved) {
    JNIEnv* jni;
    int success = 1;

    // can't use NSK_DISPLAY since needed for nsk_ functions initialization isn't done here

    jni = (JNIEnv*) nsk_aod_createJNIEnv(vm);
    if (jni == NULL)
        return JNI_ERR;

    printf("%s: initialization was done\n", AGENT_NAME);
    fflush(stdout);

    if (!NSK_VERIFY(nsk_aod_agentLoaded(jni, AGENT_NAME)))
        return JNI_ERR;

    if (optionsString == NULL) {
        success = 0;
        printf("%s: ERROR: unexpected null options\n", AGENT_NAME);
        fflush(stdout);
    } else {
        if (strcmp(optionsString, EXPECTED_OPTIONS)) {
            success = 0;
            printf("%s: ERROR: unexpected options string: '%s', expected is '%s'\n",
                    AGENT_NAME, optionsString, EXPECTED_OPTIONS);
            fflush(stdout);
        }
    }

    nsk_aod_agentFinished(jni, AGENT_NAME, success);

    return JNI_OK;
}

}
