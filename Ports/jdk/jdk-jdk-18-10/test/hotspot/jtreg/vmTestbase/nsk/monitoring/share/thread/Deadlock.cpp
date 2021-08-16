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
             env->GetMethodID(_class, _methodName, _sig)) != NULL)) \
                return

#define CALL_VOID_NOPARAM(_obj, _class, _methodName)\
            GET_METHOD_ID(method, _class, _methodName, "()V");\
        if (!NSK_JNI_VERIFY_VOID(env, env->CallVoidMethod(_obj, method))) \
                return

/*
 * Class:     nsk_monitoring_share_thread_MonitorDeadlock_DeadlockThread
 * Method:    nativeLock2
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_nsk_monitoring_share_thread_Deadlock_00024NativeLocker_lock
(JNIEnv *env, jobject o) {
        jclass testBugClass, nativeLockerClass, lockerClass, wicketClass;
        jobject lock, inner, step1, step2, step3;
        jfieldID field;
        jmethodID method;

        GET_OBJECT_CLASS(nativeLockerClass, o);
        FIND_CLASS(lockerClass, "nsk/monitoring/share/thread/Deadlock$Locker");
        FIND_CLASS(wicketClass, "nsk/share/Wicket");
        FIND_CLASS(testBugClass, "nsk/share/TestBug");
        GET_OBJ_FIELD(lock, o, nativeLockerClass, "lock", "Ljava/lang/Object;");
        GET_OBJ_FIELD(step1, o, nativeLockerClass, "step1", "Lnsk/share/Wicket;");
        if (step1 == NULL) {
                env->ThrowNew(testBugClass, "step1 field is null");
                return;
        }
        GET_OBJ_FIELD(step2, o, nativeLockerClass, "step2", "Lnsk/share/Wicket;");
        if (step2 == NULL) {
                env->ThrowNew(testBugClass, "step2 field is null");
                return;
        }
        GET_OBJ_FIELD(step3, o, nativeLockerClass, "step3", "Lnsk/share/Wicket;");
        if (step3 == NULL) {
                env->ThrowNew(testBugClass, "step3 field is null");
                return;
        }
        GET_OBJ_FIELD(inner, o, lockerClass, "inner", "Lnsk/monitoring/share/thread/Deadlock$Locker;");
        if (env->MonitorEnter(lock) == JNI_OK) {
                if (inner == NULL) {
                        env->ThrowNew(testBugClass, "Should not reach here");
                } else {
                        CALL_VOID_NOPARAM(step1, wicketClass, "unlock");
                        CALL_VOID_NOPARAM(step2, wicketClass, "waitFor");
                        CALL_VOID_NOPARAM(step3, wicketClass, "unlock");
                        CALL_VOID_NOPARAM(inner, lockerClass, "lock");
                }
                env->MonitorExit(lock);
        } else {
                env->ThrowNew(testBugClass, "MonitorEnter(lock) call failed");
        }
}

}
