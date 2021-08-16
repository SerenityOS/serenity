/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include <jvmti.h>

#include "jni_tools.h"
#include "nsk_tools.h"
#include "JVMTITools.h"
#include "jvmti_tools.h"

extern "C" {


static jvmtiEnv *test_jvmti = NULL;
static jvmtiCapabilities caps;

/*
 * Redefine a class with a new version (class file in byte array).
 *
 * native static public boolean redefineClassIntl(Class<?> clz, byte[] classFile);
 *
 * @param clz class for redefinition
 * @param classFile new version as a byte array
 * @return false if any errors occurred during class redefinition
 */
JNIEXPORT jboolean JNICALL
Java_vm_runtime_defmeth_shared_Util_redefineClassIntl(JNIEnv *env, jclass clazz, jclass clazzToRedefine, jbyteArray bytecodeArray) {
    jvmtiClassDefinition classDef;
    jboolean result = JNI_TRUE;

    if (!NSK_VERIFY(env != NULL) || !NSK_VERIFY(clazzToRedefine != NULL) || !NSK_VERIFY(bytecodeArray != NULL)) {
        return JNI_FALSE;
    }

    classDef.klass = clazzToRedefine;
    if (!NSK_JNI_VERIFY(env,
            (classDef.class_byte_count = /* jsize */ env->GetArrayLength(bytecodeArray)) > 0)) {
        return JNI_FALSE;
    }

    if (!NSK_JNI_VERIFY(env,
            (classDef.class_bytes = (const unsigned char *) /* jbyte* */ env->GetByteArrayElements(bytecodeArray, NULL)) != NULL)) {
        return JNI_FALSE;
    }

    if (!NSK_JVMTI_VERIFY(
            test_jvmti->RedefineClasses(1, &classDef))) {
        result = JNI_FALSE;
    }

    // Need to cleanup reference to byte[] whether RedefineClasses succeeded or not
    if (!NSK_JNI_VERIFY_VOID(env,
            env->ReleaseByteArrayElements(bytecodeArray, (jbyte*)classDef.class_bytes, JNI_ABORT))) {
        return JNI_FALSE;
    }

    return result;
}

jint Agent_Initialize(JavaVM *jvm, char *options, void *reserved) {
    /* init framework and parse options */
    if (!NSK_VERIFY(nsk_jvmti_parseOptions(options)))
        return JNI_ERR;

    /* create JVMTI environment */
    if (!NSK_VERIFY((test_jvmti =
            nsk_jvmti_createJVMTIEnv(jvm, reserved)) != NULL))
        return JNI_ERR;

    memset(&caps, 0, sizeof(jvmtiCapabilities));
    caps.can_redefine_classes = 1;
    if (!NSK_JVMTI_VERIFY(test_jvmti->AddCapabilities(&caps)))
        return JNI_ERR;

    if (!NSK_JVMTI_VERIFY(test_jvmti->GetCapabilities(&caps)))
        return JNI_ERR;

    if (!caps.can_redefine_classes)
        printf("Warning: RedefineClasses is not implemented\n");

    return JNI_OK;
}

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}

JNIEXPORT jint JNICALL Agent_OnAttach(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}

}
