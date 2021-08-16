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

#include <string.h>
#include "jvmti.h"
#include "agent_common.h"
#include "jni_tools.h"
#include "jvmti_tools.h"

extern "C" {

/* ============================================================================= */

static jlong timeout = 0;

#define PROPERTIES_COUNT  3

typedef struct PropertyDescStruct {
    const char* name;
    const char* value;
} PropertyDesc;

static PropertyDesc propDescList[PROPERTIES_COUNT] = {
    { "nsk.jvmti.test.property", "new value of nsk.jvmti.test.property" },
    { "nsk.jvmti.test.property.empty.old", "new value of nsk.jvmti.test.property.emply.old" },
    { "nsk.jvmti.test.property.empty.new", "" }
};

/* ============================================================================= */

static int setProperties(jvmtiEnv* jvmti) {
    int success = NSK_TRUE;
    int i;

    for (i = 0; i < PROPERTIES_COUNT; i++) {
        NSK_DISPLAY1("  property: %s\n", propDescList[i].name);
        NSK_DISPLAY1("     value: \"%s\"\n", propDescList[i].value);
        if (!NSK_JVMTI_VERIFY(
                jvmti->SetSystemProperty(propDescList[i].name, propDescList[i].value))) {
            success = NSK_FALSE;
        }
    }

    return success;
}

/* ============================================================================= */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_setsysprop003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_setsysprop003(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_setsysprop003(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiEnv* jvmti = NULL;

    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    timeout = nsk_jvmti_getWaitTime() * 60 * 1000;

    if (!NSK_VERIFY((jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    NSK_DISPLAY0(">>> Set new values for defined system properties in OnLoad phase\n");
    if (!setProperties(jvmti)) {
        nsk_jvmti_setFailStatus();
        return JNI_ERR;
    }

    return JNI_OK;
}

/* ============================================================================= */

}
