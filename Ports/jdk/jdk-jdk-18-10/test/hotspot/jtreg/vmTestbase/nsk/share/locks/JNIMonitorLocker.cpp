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
#include "jni.h"
#include "nsk_tools.h"

extern "C" {


JNIEXPORT void JNICALL
Java_nsk_share_locks_JNIMonitorLocker_doLock(JNIEnv *env, jobject thisObject)
{
/*
This method executes JNI analog for following Java code:

        JNI_MonitorEnter(this);

        step1.unlockAll();
        step2.waitFor();
        readyWicket.unlock();
        inner.lock();

        JNI_MonitorExit(this);
*/
        jint success;
        jfieldID field;
        jclass thisObjectClass;

        // fields 'step1' and 'step2'
        jobject wicketObject;

        // class for fields 'step1', 'step2', 'readyWicket'
        jclass wicketClass;

        // field 'inner'
        jobject innerObject;

        // class for field 'inner'
        jclass deadlockLockerClass;

        success = env->MonitorEnter(thisObject);

        if (success != 0)
        {
                NSK_COMPLAIN1("MonitorEnter return non-zero: %d\n", success);

                env->ThrowNew(
                    env->FindClass("nsk/share/TestJNIError"),
                    "MonitorEnter return non-zero");
        }

        thisObjectClass = env->GetObjectClass(thisObject);

        // step1.unlockAll()
        field = env->GetFieldID(thisObjectClass, "step1", "Lnsk/share/Wicket;");

        wicketObject = env->GetObjectField(thisObject, field);
        wicketClass = env->GetObjectClass(wicketObject);

        env->CallVoidMethod(wicketObject,
                            env->GetMethodID(wicketClass, "unlockAll", "()V"));

        // step2.waitFor()
        field = env->GetFieldID(thisObjectClass, "step2", "Lnsk/share/Wicket;");
        wicketObject = env->GetObjectField(thisObject, field);

        env->CallVoidMethod(wicketObject,
                            env->GetMethodID(wicketClass, "waitFor", "()V"));

        // readyWicket.unlock()
        field = env->GetFieldID(thisObjectClass, "readyWicket", "Lnsk/share/Wicket;");
        wicketObject = env->GetObjectField(thisObject, field);

        env->CallVoidMethod(wicketObject,
                            env->GetMethodID(wicketClass, "unlock", "()V"));

        // inner.lock()
        field = env->GetFieldID(thisObjectClass, "inner", "Lnsk/share/locks/DeadlockLocker;");
        innerObject = env->GetObjectField(thisObject, field);
        deadlockLockerClass = env->GetObjectClass(innerObject);

        env->CallVoidMethod(innerObject,
                            env->GetMethodID(deadlockLockerClass, "lock", "()V"));

        success = env->MonitorExit(thisObject);

        if (success != 0)
        {
                NSK_COMPLAIN1("MonitorExit return non-zero: %d\n", success);

                env->ThrowNew(
                    env->FindClass("nsk/share/TestJNIError"),
                    "MonitorExit return non-zero");
        }
}

}
