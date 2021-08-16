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

#define GET_OBJ_FIELD(_value, _obj, _class, _fieldName, _fieldSig)\
        GET_FIELD_ID(field, _class, _fieldName, _fieldSig);\
        _value = env->GetObjectField(_obj, field)

#define GET_FIELD_ID(_fieldID, _class, _fieldName, _fieldSig)\
        if (!NSK_JNI_VERIFY(env, (_fieldID = \
             env->GetFieldID(_class, _fieldName, _fieldSig)) != NULL))\
                return

#define GET_METHOD_ID(_methodID, _class, _methodName, _sig)\
        if (!NSK_JNI_VERIFY(env, (_methodID = \
             env->GetMethodID(_class, _methodName, _sig)) != NULL))\
                return

#define CALL_VOID_NOPARAM(_obj, _class, _methodName)\
            GET_METHOD_ID(method, _class, _methodName, "()V");\
        if (!NSK_JNI_VERIFY_VOID(env, env->CallVoidMethod(_obj, method)))\
                return
/*
 * Class:     nsk_monitoring_share_thread_LockingThreads_Thread1
 * Method:    B
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_nsk_monitoring_share_thread_LockingThreads_00024Thread1_B
  (JNIEnv *env, jobject o) {
        jclass testBugClass, threadClass;
        jobject lock4, lock5;
        jfieldID field;
        jmethodID method;

        GET_OBJECT_CLASS(threadClass, o);
        FIND_CLASS(testBugClass, "nsk/share/TestBug");
        GET_OBJ_FIELD(lock4, o, threadClass, "lock4", "Lnsk/monitoring/share/thread/LockingThreads$CustomLock;");
        GET_OBJ_FIELD(lock5, o, threadClass, "lock5", "Lnsk/monitoring/share/thread/LockingThreads$CustomLock;");
        if (env->MonitorEnter(lock4) == JNI_OK) {
                if (env->MonitorEnter(lock5) == JNI_OK) {
                        CALL_VOID_NOPARAM(o, threadClass, "C");
                        env->MonitorExit(lock5);
                } else
                        env->ThrowNew(testBugClass, "MonitorEnter(lock5) call failed");
                env->MonitorExit(lock4);
        } else
                env->ThrowNew(testBugClass, "MonitorEnter(lock4) call failed");
}

}
