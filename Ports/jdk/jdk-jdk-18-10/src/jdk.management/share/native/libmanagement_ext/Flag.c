/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
#include <stdio.h>
#include <string.h>
#include <jni.h>
#include "management_ext.h"
#include "com_sun_management_internal_Flag.h"

static jobject default_origin = NULL;
static jobject vm_creation_origin = NULL;
static jobject mgmt_origin = NULL;
static jobject envvar_origin = NULL;
static jobject config_file_origin = NULL;
static jobject ergo_origin = NULL;
static jobject attach_origin = NULL;
static jobject other_origin = NULL;

JNIEXPORT jint JNICALL
Java_com_sun_management_internal_Flag_getInternalFlagCount
  (JNIEnv *env, jclass cls)
{
    jlong count = jmm_interface->GetLongAttribute(env, NULL,
                                                  JMM_VM_GLOBAL_COUNT);
    return (jint) count;
}

JNIEXPORT jobjectArray JNICALL
  Java_com_sun_management_internal_Flag_getAllFlagNames
(JNIEnv *env, jclass cls)
{
  return jmm_interface->GetVMGlobalNames(env);
}

static jobject find_origin_constant(JNIEnv* env, const char* enum_name) {
    jvalue field;
    field = JNU_GetStaticFieldByName(env,
                                     NULL,
                                     "com/sun/management/VMOption$Origin",
                                     enum_name,
                                     "Lcom/sun/management/VMOption$Origin;");
    return (*env)->NewGlobalRef(env, field.l);
}

JNIEXPORT void JNICALL
Java_com_sun_management_internal_Flag_initialize
  (JNIEnv *env, jclass cls)
{
    default_origin = find_origin_constant(env, "DEFAULT");
    vm_creation_origin = find_origin_constant(env, "VM_CREATION");
    mgmt_origin = find_origin_constant(env, "MANAGEMENT");
    envvar_origin = find_origin_constant(env, "ENVIRON_VAR");
    config_file_origin = find_origin_constant(env, "CONFIG_FILE");
    ergo_origin = find_origin_constant(env, "ERGONOMIC");
    attach_origin = find_origin_constant(env, "ATTACH_ON_DEMAND");
    other_origin = find_origin_constant(env, "OTHER");
}

JNIEXPORT jint JNICALL
Java_com_sun_management_internal_Flag_getFlags
  (JNIEnv *env, jclass cls, jobjectArray names, jobjectArray flags, jint count)
{
    jint num_flags, i, index;
    jmmVMGlobal* globals;
    size_t gsize;
    const char* class_name = "com/sun/management/internal/Flag";
    const char* signature = "(Ljava/lang/String;Ljava/lang/Object;ZZLcom/sun/management/VMOption$Origin;)V";
    jobject origin;
    jobject valueObj;
    jobject flag;

    if (flags == NULL) {
        JNU_ThrowNullPointerException(env, 0);
        return 0;
    }

    if (count <= 0) {
        JNU_ThrowIllegalArgumentException(env, 0);
        return 0;
    }

    gsize = (size_t)count * sizeof(jmmVMGlobal);
    globals = (jmmVMGlobal*) malloc(gsize);
    if (globals == NULL) {
        JNU_ThrowOutOfMemoryError(env, 0);
        return 0;
    }

    memset(globals, 0, gsize);
    num_flags = jmm_interface->GetVMGlobals(env, names, globals, count);
    if (num_flags == 0) {
        free(globals);
        return 0;
    }

    index = 0;
    for (i = 0; i < count; i++) {
        if (globals[i].name == NULL) {
            continue;
        }
        switch (globals[i].type) {
        case JMM_VMGLOBAL_TYPE_JBOOLEAN:
            valueObj = JNU_NewObjectByName(env, "java/lang/Boolean", "(Z)V",
                                           globals[i].value.z);
            break;
        case JMM_VMGLOBAL_TYPE_JSTRING:
            valueObj = globals[i].value.l;
            break;
        case JMM_VMGLOBAL_TYPE_JLONG:
            valueObj = JNU_NewObjectByName(env, "java/lang/Long", "(J)V",
                                           globals[i].value.j);
            break;
        case JMM_VMGLOBAL_TYPE_JDOUBLE:
            valueObj = JNU_NewObjectByName(env, "java/lang/Double", "(D)V",
                                           globals[i].value.d);
            break;
        default:
            // ignore unsupported type
            continue;
        }

        if (valueObj == NULL && globals[i].type != JMM_VMGLOBAL_TYPE_JSTRING) {
            free(globals);
            JNU_ThrowOutOfMemoryError(env, 0);
            return 0;
        }

        switch (globals[i].origin) {
        case JMM_VMGLOBAL_ORIGIN_DEFAULT:
            origin = default_origin;
            break;
        case JMM_VMGLOBAL_ORIGIN_COMMAND_LINE:
            origin = vm_creation_origin;
            break;
        case JMM_VMGLOBAL_ORIGIN_MANAGEMENT:
            origin = mgmt_origin;
            break;
        case JMM_VMGLOBAL_ORIGIN_ENVIRON_VAR:
            origin = envvar_origin;
            break;
        case JMM_VMGLOBAL_ORIGIN_CONFIG_FILE:
            origin = config_file_origin;
            break;
        case JMM_VMGLOBAL_ORIGIN_ERGONOMIC:
            origin = ergo_origin;
            break;
        case JMM_VMGLOBAL_ORIGIN_ATTACH_ON_DEMAND:
            origin = attach_origin;
            break;
        case JMM_VMGLOBAL_ORIGIN_OTHER:
            origin = other_origin;
            break;
        default:
            // unknown origin
            origin = other_origin;
            break;
        }
        flag = JNU_NewObjectByName(env, class_name, signature, globals[i].name,
                                   valueObj, globals[i].writeable,
                                   globals[i].external, origin);
        if (flag == NULL) {
            free(globals);
            JNU_ThrowOutOfMemoryError(env, 0);
            return 0;
        }
        (*env)->SetObjectArrayElement(env, flags, index, flag);
        index++;
    }

    if (index != num_flags) {
        JNU_ThrowInternalError(env, "Number of Flag objects created unmatched");
        free(globals);
        return 0;
    }

    free(globals);

    /* return the number of Flag objects created */
    return num_flags;
}

JNIEXPORT void JNICALL
Java_com_sun_management_internal_Flag_setLongValue
  (JNIEnv *env, jclass cls, jstring name, jlong value)
{
   jvalue v;
   v.j = value;

   jmm_interface->SetVMGlobal(env, name, v);
}

JNIEXPORT void JNICALL
Java_com_sun_management_internal_Flag_setDoubleValue
  (JNIEnv *env, jclass cls, jstring name, jdouble value)
{
   jvalue v;
   v.d = value;

   jmm_interface->SetVMGlobal(env, name, v);
}

JNIEXPORT void JNICALL
Java_com_sun_management_internal_Flag_setBooleanValue
  (JNIEnv *env, jclass cls, jstring name, jboolean value)
{
   jvalue v;
   v.z = value;

   jmm_interface->SetVMGlobal(env, name, v);
}

JNIEXPORT void JNICALL
Java_com_sun_management_internal_Flag_setStringValue
  (JNIEnv *env, jclass cls, jstring name, jstring value)
{
   jvalue v;
   v.l = value;

   jmm_interface->SetVMGlobal(env, name, v);
}
