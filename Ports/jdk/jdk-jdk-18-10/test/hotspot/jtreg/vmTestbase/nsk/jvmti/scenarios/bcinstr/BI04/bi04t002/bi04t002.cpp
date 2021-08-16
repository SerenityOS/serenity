/*
 * Copyright (c) 2004, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "ExceptionCheckingJniEnv.hpp"
#include "jni_tools.h"
#include "jvmti_tools.h"

extern "C" {

/* scaffold objects */
static jlong timeout = 0;

#define PATH_TO_NEW_BYTECODE "pathToNewByteCode"
#define TESTED_CLASS_NAME "java/lang/Object"

static jint newClassSize;
static unsigned char* newClassBytes;

static jvmtiClassDefinition classDef;

/* ============================================================================= */

int readNewBytecode(jvmtiEnv* jvmti) {

    char filename[256];
    FILE *bytecode;
    const char *pathToByteCode = nsk_jvmti_findOptionValue(PATH_TO_NEW_BYTECODE);
    jint read_bytes;

    if (pathToByteCode)
        sprintf(filename,"%s/%s/%s.class",
                    pathToByteCode, "newclass02", TESTED_CLASS_NAME);
    else
        sprintf(filename,"%s/%s.class",
                    "newclass02", TESTED_CLASS_NAME);

    NSK_DISPLAY1("Reading new bytecode for java.lang.Object\n\tfile name: %s\n",
                        filename);

    bytecode = fopen(filename, "rb");
    if (bytecode == NULL) {
        NSK_COMPLAIN0("error opening file\n");
        return NSK_FALSE;
    }

    fseek(bytecode, 0, SEEK_END);
    classDef.class_byte_count = ftell(bytecode);
    rewind(bytecode);

    if (!NSK_JVMTI_VERIFY(jvmti->Allocate(classDef.class_byte_count, &newClassBytes))) {
        NSK_COMPLAIN0("buffer couldn't be allocated\n");
        return NSK_FALSE;
    }
    classDef.class_bytes = newClassBytes;
    read_bytes = (jint) fread(newClassBytes, 1,
                            classDef.class_byte_count, bytecode);
    fclose(bytecode);
    if (read_bytes != classDef.class_byte_count) {
        NSK_COMPLAIN0("error reading file\n");
        return NSK_FALSE;
    }

    return NSK_TRUE;
}

/* ============================================================================= */

/** Agent algorithm. */
static void JNICALL
agentProc(jvmtiEnv* jvmti, JNIEnv* jni, void* arg) {

    ExceptionCheckingJniEnvPtr ec_jni(jni);
    /*Wait for debuggee to set classes to be redefined nsk_jvmti_waitForSync#4*/
    NSK_DISPLAY0("Wait for debuggee to set classes to be redefined nsk_jvmti_waitForSync#4\n");
    if (!nsk_jvmti_waitForSync(timeout))
        return;

    NSK_DISPLAY1("Find class: %s\n", TESTED_CLASS_NAME);
    classDef.klass = ec_jni->FindClass(TESTED_CLASS_NAME, TRACE_JNI_CALL);
    classDef.klass = (jclass) ec_jni->NewGlobalRef(classDef.klass, TRACE_JNI_CALL);

    NSK_DISPLAY0("Redfine class with new byte code\n");
    NSK_DISPLAY3("class definition:\n\t0x%p, 0x%p:%d\n",
                    classDef.klass,
                    classDef.class_bytes,
                    classDef.class_byte_count);
    if (nsk_getVerboseMode()) {
        nsk_printHexBytes("   ", 16, classDef.class_byte_count,
                                classDef.class_bytes);
    }
    if (!NSK_JVMTI_VERIFY(jvmti->RedefineClasses(1, &classDef))) {
        nsk_jvmti_setFailStatus();
        return;
    }

    ec_jni->DeleteGlobalRef(classDef.klass, TRACE_JNI_CALL);

    if (!nsk_jvmti_resumeSync())
        return;
}

/* ============================================================================= */

/** Agent library initialization. */
#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_bi04t002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_bi04t002(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_bi04t002(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    jvmtiEnv *jvmti = NULL;

    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    timeout = nsk_jvmti_getWaitTime() * 60 * 1000;

    jvmti = nsk_jvmti_createJVMTIEnv(jvm, reserved);
    if (!NSK_VERIFY(jvmti != NULL))
        return JNI_ERR;

    {
        jvmtiCapabilities caps;
        memset(&caps, 0, sizeof(caps));

        caps.can_redefine_classes = 1;
        caps.can_redefine_any_class = 1;
        if (!NSK_JVMTI_VERIFY(jvmti->AddCapabilities(&caps)))
            return JNI_ERR;
    }

    if (!NSK_VERIFY(readNewBytecode(jvmti)))
        return JNI_ERR;

    if (!NSK_VERIFY(nsk_jvmti_setAgentProc(agentProc, NULL)))
        return JNI_ERR;

    return JNI_OK;
}

/* ============================================================================= */


}
