/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include <jni.h>
#include <stdio.h>
#include "jni_tools.h"

extern "C" {

#define FIND_CLASS(_class, _className)\
    if (!NSK_JNI_VERIFY(env, (_class = \
            env->FindClass(_className)) != NULL))\
        return

#define GET_OBJECT_CLASS(_class, _obj)\
    if (!NSK_JNI_VERIFY(env, (_class = \
            env->GetObjectClass(_obj)) != NULL))\
        return

#define GET_STATIC_FIELD_ID(_fieldID, _class, _fieldName, _fieldSig)\
    if (!NSK_JNI_VERIFY(env, (_fieldID = \
            env->GetStaticFieldID(_class, _fieldName, _fieldSig)) != NULL))\
        return

#define GET_STATIC_OBJ_FIELD(_value, _class, _fieldName, _fieldSig)\
    GET_STATIC_FIELD_ID(field, _class, _fieldName, _fieldSig);\
    _value = env->GetStaticObjectField(_class, field)

#define GET_STATIC_BOOL_FIELD(_value, _class, _fieldName)\
    GET_STATIC_FIELD_ID(field, _class, _fieldName, "Z");\
    _value = env->GetStaticBooleanField(_class, field)

#define GET_FIELD_ID(_fieldID, _class, _fieldName, _fieldSig)\
    if (!NSK_JNI_VERIFY(env, (_fieldID = \
            env->GetFieldID(_class, _fieldName, _fieldSig)) != NULL))\
        return

#define GET_INT_FIELD(_value, _obj, _class, _fieldName)\
    GET_FIELD_ID(field, _class, _fieldName, "I");\
    _value = env->GetIntField(_obj, field)

#define GET_LONG_FIELD(_value, _obj, _class, _fieldName)\
    GET_FIELD_ID(field, _class, _fieldName, "J");\
    _value = env->GetLongField(_obj, field)

#define GET_STATIC_INT_FIELD(_value, _class, _fieldName)\
    GET_STATIC_FIELD_ID(field, _class, _fieldName, "I");\
    _value = env->GetStaticIntField(_class, field)

#define SET_INT_FIELD(_obj, _class, _fieldName, _newValue)\
    GET_FIELD_ID(field, _class, _fieldName, "I");\
    env->SetIntField(_obj, field, _newValue)

#define GET_OBJ_FIELD(_value, _obj, _class, _fieldName, _fieldSig)\
    GET_FIELD_ID(field, _class, _fieldName, _fieldSig);\
    _value = env->GetObjectField(_obj, field)


#define GET_ARR_ELEMENT(_arr, _index)\
    env->GetObjectArrayElement(_arr, _index)

#define SET_ARR_ELEMENT(_arr, _index, _newValue)\
    env->SetObjectArrayElement(_arr, _index, _newValue)

#define GET_STATIC_METHOD_ID(_methodID, _class, _methodName, _sig)\
    if (!NSK_JNI_VERIFY(env, (_methodID = \
            env->GetStaticMethodID(_class, _methodName, _sig)) != NULL))\
        return

#define GET_METHOD_ID(_methodID, _class, _methodName, _sig)\
    if (!NSK_JNI_VERIFY(env, (_methodID = \
            env->GetMethodID(_class, _methodName, _sig)) != NULL))\
        return

#define CALL_STATIC_VOID_NOPARAM(_class, _methodName)\
    GET_STATIC_METHOD_ID(method, _class, _methodName, "()V");\
    if (!NSK_JNI_VERIFY_VOID(env, env->CallStaticVoidMethod(_class, method)))\
        return

#define CALL_STATIC_VOID(_class, _methodName, _sig, _param)\
    GET_STATIC_METHOD_ID(method, _class, _methodName, _sig);\
    if (!NSK_JNI_VERIFY_VOID(env, env->CallStaticVoidMethod(_class, method, _param)))\
        return

#define CALL_VOID_NOPARAM(_obj, _class, _methodName)\
    GET_METHOD_ID(method, _class, _methodName, "()V");\
    if (!NSK_JNI_VERIFY_VOID(env, env->CallVoidMethod(_obj, method)))\
        return

#define CALL_VOID(_obj, _class, _methodName, _sig, _param)\
    GET_METHOD_ID(method, _class, _methodName, _sig);\
    if (!NSK_JNI_VERIFY_VOID(env, env->CallVoidMethod(_obj, method, _param)))\
        return

#define CALL_VOID2(_obj, _class, _methodName, _sig, _param1, _param2)\
    GET_METHOD_ID(method, _class, _methodName, _sig);\
    if (!NSK_JNI_VERIFY_VOID(env, env->CallVoidMethod(_obj, method, _param1, _param2)))\
        return

#define CALL_INT_NOPARAM(_value, _obj, _class, _methodName)\
    GET_METHOD_ID(method, _class, _methodName, "()I");\
    _value = env->CallIntMethod(_obj, method)

#define NEW_OBJ(_obj, _class, _constructorName, _sig, _params)\
    GET_METHOD_ID(method, _class, _constructorName, _sig);\
    if (!NSK_JNI_VERIFY(env, (_obj = \
            env->NewObject(_class, method, _params)) != NULL))\
        return

#define MONITOR_ENTER(x) \
    NSK_JNI_VERIFY(env, env->MonitorEnter(x) == 0)

#define MONITOR_EXIT(x) \
    NSK_JNI_VERIFY(env, env->MonitorExit(x) == 0)

#define TRACE(msg)\
   GET_OBJ_FIELD(logger, obj, threadClass, "logger", "Lnsk/share/Log$Logger;");\
   jmsg = env->NewStringUTF(msg);\
   CALL_VOID2(logger, loggerClass, "trace",\
                           "(ILjava/lang/String;)V", 50, jmsg)


/*
 * Class:     nsk_monitoring_share_thread_RecursiveMonitoringThread
 * Method:    nativeRecursiveMethod
 * Signature: (IZ)V
 */
JNIEXPORT void JNICALL Java_nsk_monitoring_share_thread_RecursiveMonitoringThread_nativeRecursiveMethod
(JNIEnv *env, jobject o, jint currentDepth, jboolean pureNative) {
        jclass klass;
        jmethodID method;

        GET_OBJECT_CLASS(klass, o);
        if (currentDepth-- > 0) {
/*              printf("Current depth: %d\n", currentDepth); */
                CALL_STATIC_VOID_NOPARAM(klass, "yield");
                if (pureNative == JNI_TRUE) {
                        CALL_VOID2(o, klass, "nativeRecursiveMethod", "(IZ)V", currentDepth, pureNative);
                } else {
                        CALL_VOID(o, klass, "recursiveMethod", "(I)V", currentDepth);
                }
        } else {
                CALL_VOID_NOPARAM(o, klass, "runInside");
        }
}

}

