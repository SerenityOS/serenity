/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

#include <assert.h>
#include <jni.h>
#include <jvmti.h>
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

#define GET_BOOL_FIELD(_value, _obj, _class, _fieldName)\
    GET_FIELD_ID(field, _class, _fieldName, "Z");\
    _value = env->GetBooleanField(_obj, field)

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

#define CALL_STATIC_OBJ(_value, _class, _methodName, _sig, _param)\
    GET_STATIC_METHOD_ID(method, _class, _methodName, _sig);\
    _value = env->CallStaticObjectMethod(_class, method, _param)

#define CALL_VOID_NOPARAM(_obj, _class, _methodName)\
    GET_METHOD_ID(method, _class, _methodName, "()V");\
    if (!NSK_JNI_VERIFY_VOID(env, env->CallVoidMethod(_obj, method)))\
        return

#define CALL_VOID(_obj, _class, _methodName, _sig, _param)\
    GET_METHOD_ID(method, _class, _methodName, "()V");\
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

    static const char *SctrlClassName="nsk/monitoring/share/ThreadController";
    static const char *SthreadControllerSig
            = "Lnsk/monitoring/share/ThreadController;";

    static const char *SThreadsGroupLocksSig
    ="Lnsk/monitoring/share/ThreadsGroupLocks;";
    static const char *SThreadsGroupLocksClassName
    ="nsk/monitoring/share/ThreadsGroupLocks";


    static const char *SbringState_mn="bringState";
    static const char *SnativeBringState_mn="nativeBringState";
    static const char *SrecursiveMethod_mn="recursiveMethod";
    static const char *SnativeRecursiveMethod_mn="nativeRecursiveMethod";
    static const char *SloggerClassName = "nsk/share/Log$Logger";

    static const char *Snoparams="()V";
    static const char *Slongparam="(J)V";

    /*
     * Class:     nsk_monitoring_share_BaseThread
     * Method:    nativeRecursiveMethod
     * Signature: ()V
     */
    JNIEXPORT void JNICALL
    Java_nsk_monitoring_share_BaseThread_nativeRecursiveMethod(JNIEnv *env,
            jobject obj) {
        jint currDepth, maxDepth;
        jobject logger;
        jstring jmsg;
        jfieldID field;
        jmethodID method;

        jobject controller;
        jclass threadClass, ctrlClass, loggerClass;

        int invocationType;

        GET_OBJECT_CLASS(threadClass, obj);
        FIND_CLASS(ctrlClass, SctrlClassName);
        FIND_CLASS(loggerClass, SloggerClassName);


        /* currDepth++ */
        GET_INT_FIELD(currDepth, obj, threadClass, "currentDepth");
        currDepth++;
        SET_INT_FIELD(obj, threadClass, "currentDepth", currDepth);

        GET_OBJ_FIELD(controller, obj, threadClass, "controller",
                      SthreadControllerSig);
        GET_INT_FIELD(maxDepth, controller, ctrlClass, "maxDepth");

        GET_STATIC_INT_FIELD(invocationType, ctrlClass, "invocationType");

        if (maxDepth - currDepth > 0)
        {
            CALL_STATIC_VOID_NOPARAM(threadClass, "yield");

            if (invocationType == 2/*MIXED_TYPE*/)
            {
                CALL_VOID_NOPARAM(obj, threadClass, SrecursiveMethod_mn);
            }
            else
            {
                CALL_VOID_NOPARAM(obj, threadClass, SnativeRecursiveMethod_mn);
            }
        }
        else
        {
            TRACE("state has been reached");
            if (invocationType == 2/*MIXED_TYPE*/)
            {
                CALL_VOID_NOPARAM(obj, threadClass, SbringState_mn);
            }
            else
            {
                CALL_VOID_NOPARAM(obj, threadClass, SnativeBringState_mn);
            }
        }

        currDepth--;
        GET_OBJECT_CLASS(threadClass, obj);
        SET_INT_FIELD(obj, threadClass, "currentDepth", currDepth);
    }

    /*
     * Class:     nsk_monitoring_share_BlockedThread
     * Method:    nativeBringState
     * Signature: ()V
     */
    JNIEXPORT void JNICALL
    Java_nsk_monitoring_share_BlockedThread_nativeBringState(JNIEnv *env,
            jobject obj) {
        jobject logger;
        jstring jmsg;
        jfieldID field;
        jmethodID method;

        jclass threadClass, loggerClass;

        jobject STATE;

        //ThreadsGroupLocks:
        jclass ThreadsGroupLocks;
        jobject  threadsGroupLocks;
        jmethodID getBarrier;


        //CountDownLatch
        jobject barrier;
        jclass CountDownLatch;

        //Blocker
        jobject blocker;
        jclass Blocker;

        GET_OBJECT_CLASS(threadClass, obj);

        FIND_CLASS(loggerClass, SloggerClassName);
        FIND_CLASS(ThreadsGroupLocks, SThreadsGroupLocksClassName);
        FIND_CLASS(Blocker, "Lnsk/monitoring/share/ThreadsGroupLocks$Blocker;");
        FIND_CLASS(CountDownLatch, "nsk/monitoring/share/ThreadsGroupLocks$PlainCountDownLatch");

        GET_OBJ_FIELD(threadsGroupLocks, obj, threadClass, "threadsGroupLocks", SThreadsGroupLocksSig);
        GET_STATIC_OBJ_FIELD(STATE, threadClass, "STATE", "Ljava/lang/Thread$State;");
        GET_OBJ_FIELD(blocker, threadsGroupLocks, ThreadsGroupLocks, "blocker", "Lnsk/monitoring/share/ThreadsGroupLocks$Blocker;");

        getBarrier = env->GetMethodID(ThreadsGroupLocks, "getBarrier",
            "(Ljava/lang/Thread$State;)Lnsk/monitoring/share/ThreadsGroupLocks$PlainCountDownLatch;");
        barrier = env->CallObjectMethod(threadsGroupLocks, getBarrier, STATE);


        TRACE("entering to monitor");

        CALL_VOID_NOPARAM(barrier, CountDownLatch, "countDown");
        CALL_VOID_NOPARAM(blocker, Blocker, "block");
        TRACE("exiting from monitor");

    }

    /*
     * Class:     nsk_monitoring_share_WaitingThread
     * Method:    nativeBringState
     * Signature: ()V
     */
    JNIEXPORT void JNICALL
    Java_nsk_monitoring_share_WaitingThread_nativeBringState(JNIEnv *env,
            jobject obj) {
        jobject logger;
        jstring jmsg;
        jfieldID field;
        jmethodID method;

        jclass threadClass, loggerClass;

        //STATE
        jobject STATE;

        //ThreadsGroupLocks:
        jclass ThreadsGroupLocks;
        jobject  threadsGroupLocks;
        jmethodID getBarrier;

        //CountDownLatch
        jobject barrier;
        jclass CountDownLatch;

        GET_OBJECT_CLASS(threadClass, obj);

        FIND_CLASS(loggerClass, SloggerClassName);
        FIND_CLASS(ThreadsGroupLocks, "nsk/monitoring/share/ThreadsGroupLocks");
        FIND_CLASS(CountDownLatch, "nsk/monitoring/share/ThreadsGroupLocks$PlainCountDownLatch");

        GET_STATIC_OBJ_FIELD(STATE, threadClass, "STATE", "Ljava/lang/Thread$State;");
        GET_OBJ_FIELD(threadsGroupLocks, obj, threadClass, "threadsGroupLocks", "Lnsk/monitoring/share/ThreadsGroupLocks;");

        getBarrier = env->GetMethodID(ThreadsGroupLocks, "getBarrier",
            "(Ljava/lang/Thread$State;)Lnsk/monitoring/share/ThreadsGroupLocks$PlainCountDownLatch;");
        barrier = env->CallObjectMethod(threadsGroupLocks, getBarrier, STATE);
        CALL_VOID_NOPARAM(barrier, CountDownLatch, "countDown");

        TRACE("waiting on a monitor");
        CALL_VOID_NOPARAM(barrier, CountDownLatch, "await");
    }

    /*
     * Class:     nsk_monitoring_share_SleepingThread
     * Method:    nativeBringState
     * Signature: ()V
     */
    JNIEXPORT void JNICALL
    Java_nsk_monitoring_share_SleepingThread_nativeBringState(JNIEnv *env,
            jobject obj) {
        jfieldID field;
        jmethodID method;

        jclass threadClass, loggerClass;

        //STATE
        jobject STATE;

        //ThreadsGroupLocks:
        jclass ThreadsGroupLocks;
        jobject  threadsGroupLocks;
        jmethodID getBarrier;

        //CountDownLatch
        jobject barrier;
        jclass CountDownLatch;

        //Thread
        jclass Thread;

        jlong sleepTime = 20 * 60 * 1000;


        GET_OBJECT_CLASS(threadClass, obj);

        FIND_CLASS(loggerClass, SloggerClassName);
        FIND_CLASS(ThreadsGroupLocks, "nsk/monitoring/share/ThreadsGroupLocks");
        FIND_CLASS(CountDownLatch, "nsk/monitoring/share/ThreadsGroupLocks$PlainCountDownLatch");

        GET_STATIC_OBJ_FIELD(STATE, threadClass, "STATE", "Ljava/lang/Thread$State;");
        GET_OBJ_FIELD(threadsGroupLocks, obj, threadClass, "threadsGroupLocks", "Lnsk/monitoring/share/ThreadsGroupLocks;");

        // Thread.sleep(3600 * 1000);
        FIND_CLASS(Thread, "java/lang/Thread");

        getBarrier = env->GetMethodID(ThreadsGroupLocks, "getBarrier",
            "(Ljava/lang/Thread$State;)Lnsk/monitoring/share/ThreadsGroupLocks$PlainCountDownLatch;");
        barrier = env->CallObjectMethod(threadsGroupLocks, getBarrier, STATE);
        CALL_VOID_NOPARAM(barrier, CountDownLatch, "countDown");

        CALL_STATIC_VOID(Thread, "sleep", "(J)V", sleepTime);
    }

    /*
     * Class:     nsk_monitoring_share_RunningThread
     * Method:    nativeBringState
     * Signature: ()V
     */
    JNIEXPORT void JNICALL
    Java_nsk_monitoring_share_RunningThread_nativeBringState(JNIEnv *env,
            jobject obj) {
        jobject logger;
        jstring jmsg;
        jfieldID field;
        jmethodID method;

        jclass threadClass, loggerClass;

        //STATE
        jobject STATE;

        //ThreadsGroupLocks:
        jclass ThreadsGroupLocks;
        jobject  threadsGroupLocks;
        jmethodID getBarrier;

        //CountDownLatch
        jobject barrier;
        jclass CountDownLatch;

        //Thread
        jclass Thread;

        //runnableCanExit
        jboolean flag = JNI_FALSE;

        GET_OBJECT_CLASS(threadClass, obj);

        FIND_CLASS(loggerClass, SloggerClassName);
        FIND_CLASS(ThreadsGroupLocks, "nsk/monitoring/share/ThreadsGroupLocks");
        FIND_CLASS(CountDownLatch, "nsk/monitoring/share/ThreadsGroupLocks$PlainCountDownLatch");

        GET_STATIC_OBJ_FIELD(STATE, threadClass, "STATE", "Ljava/lang/Thread$State;");
        GET_OBJ_FIELD(threadsGroupLocks, obj, threadClass, "threadsGroupLocks", "Lnsk/monitoring/share/ThreadsGroupLocks;");

        // Thread.sleep(3600 * 1000);
        FIND_CLASS(Thread, "java/lang/Thread");

        getBarrier = env->GetMethodID(ThreadsGroupLocks, "getBarrier",
            "(Ljava/lang/Thread$State;)Lnsk/monitoring/share/ThreadsGroupLocks$PlainCountDownLatch;");

        TRACE("running loop");

        barrier = env->CallObjectMethod(threadsGroupLocks, getBarrier, STATE);
        CALL_VOID_NOPARAM(barrier, CountDownLatch, "countDown");

        // while (!threadsGroupLocks.runnableCanExit.get()) {
        //        Thread.yield();
        //    }
        while (flag == JNI_FALSE)
        {
            GET_BOOL_FIELD(flag, threadsGroupLocks, ThreadsGroupLocks, "runnableCanExit");
            CALL_STATIC_VOID_NOPARAM(Thread, "yield");
        }

    }

    jstring getStateName(JNIEnv *env, jint state) {
        switch (state & JVMTI_JAVA_LANG_THREAD_STATE_MASK) {
            case JVMTI_JAVA_LANG_THREAD_STATE_NEW:
                return env->NewStringUTF("NEW");
            case JVMTI_JAVA_LANG_THREAD_STATE_TERMINATED:
                return env->NewStringUTF("TERMINATED");
            case JVMTI_JAVA_LANG_THREAD_STATE_RUNNABLE:
                return env->NewStringUTF("RUNNABLE");
            case JVMTI_JAVA_LANG_THREAD_STATE_BLOCKED:
                return env->NewStringUTF("BLOCKED");
            case JVMTI_JAVA_LANG_THREAD_STATE_WAITING:
                return env->NewStringUTF("WAITING");
            case JVMTI_JAVA_LANG_THREAD_STATE_TIMED_WAITING:
                return env->NewStringUTF("TIMED_WAITING");
            }
        // should never reach
        assert(0);
        return 0;
    }

    /*
     * Class:     nsk_monitoring_share_ThreadController
     * Method:    getThreadState
     * Signature: (Ljava/lang/Thread;)Ljava/lang/Thread/State;
     */
    JNIEXPORT jobject JNICALL
    Java_nsk_monitoring_share_ThreadController_getThreadState(JNIEnv *env,
            jobject obj, jobject thread) {

        JavaVM *vm;
        jvmtiEnv *jvmti;
        jclass ThreadState;
        jmethodID method;
        jobject threadState;
        jstring stateName;
        jint state;

        if (!NSK_VERIFY(env->GetJavaVM(&vm) == 0)) {
            return NULL;
        }

        if (!NSK_VERIFY(vm->GetEnv((void **)&jvmti, JVMTI_VERSION_1) == JNI_OK)) {
            return NULL;
        }

        if (!NSK_VERIFY(jvmti->GetThreadState((jthread)thread, &state) == JVMTI_ERROR_NONE)) {
            return NULL;
        }

        stateName = getStateName(env, state);
        if (!NSK_JNI_VERIFY(env, (ThreadState = env->FindClass("java/lang/Thread$State")) != NULL))
            return NULL;

        if (!NSK_JNI_VERIFY(env, (method = env->GetStaticMethodID(ThreadState, "valueOf", "(Ljava/lang/String;)Ljava/lang/Thread$State;")) != NULL))
            return NULL;
        threadState = env->CallStaticObjectMethod(ThreadState, method, stateName);

        return threadState;
    }

}
