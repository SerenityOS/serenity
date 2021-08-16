/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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

#include <jni.h>
#include <stdlib.h>
#include "jvm.h"
#include "management.h"
#include "sun_management_VMManagementImpl.h"

#define MAX_VERSION_LEN   20

JNIEXPORT jstring JNICALL
Java_sun_management_VMManagementImpl_getVersion0
  (JNIEnv *env, jclass dummy)
{
    char buf[MAX_VERSION_LEN];
    jstring version_string = NULL;

    unsigned int major = ((unsigned int) jmm_version & 0x0FFF0000) >> 16;
    unsigned int minor = ((unsigned int) jmm_version & 0xFF00) >> 8;

    // for internal use
    unsigned int micro = (unsigned int) jmm_version & 0xFF;

    sprintf(buf, "%d.%d", major, minor);
    version_string = (*env)->NewStringUTF(env, buf);
    return version_string;
}

static void setStaticBooleanField
   (JNIEnv* env, jclass cls, const char* name, jboolean value)
{
    jfieldID fid;
    fid = (*env)->GetStaticFieldID(env, cls, name, "Z");
    if (fid != 0) {
        (*env)->SetStaticBooleanField(env, cls, fid, value);
    }
}

JNIEXPORT void JNICALL
Java_sun_management_VMManagementImpl_initOptionalSupportFields
  (JNIEnv *env, jclass cls)
{
    jmmOptionalSupport mos;
    jint ret = jmm_interface->GetOptionalSupport(env, &mos);

    jboolean value;

    value = mos.isCompilationTimeMonitoringSupported;
    setStaticBooleanField(env, cls, "compTimeMonitoringSupport", value);

    value = mos.isThreadContentionMonitoringSupported;
    setStaticBooleanField(env, cls, "threadContentionMonitoringSupport", value);

    value = mos.isCurrentThreadCpuTimeSupported;
    setStaticBooleanField(env, cls, "currentThreadCpuTimeSupport", value);

    value = mos.isOtherThreadCpuTimeSupported;
    setStaticBooleanField(env, cls, "otherThreadCpuTimeSupport", value);

    if (jmm_version >= JMM_VERSION_1_1) {
        value = mos.isObjectMonitorUsageSupported;
        setStaticBooleanField(env, cls, "objectMonitorUsageSupport", value);

        value = mos.isSynchronizerUsageSupported;
        setStaticBooleanField(env, cls, "synchronizerUsageSupport", value);
    } else {
        setStaticBooleanField(env, cls, "objectMonitorUsageSupport", JNI_FALSE);
        setStaticBooleanField(env, cls, "synchronizerUsageSupport", JNI_FALSE);
    }

    value = mos.isThreadAllocatedMemorySupported;
    setStaticBooleanField(env, cls, "threadAllocatedMemorySupport", value);

    value = mos.isRemoteDiagnosticCommandsSupported;
    setStaticBooleanField(env, cls, "remoteDiagnosticCommandsSupport", value);
}

JNIEXPORT jobjectArray JNICALL
Java_sun_management_VMManagementImpl_getVmArguments0
  (JNIEnv *env, jobject dummy)
{
    return JVM_GetVmArguments(env);
}

JNIEXPORT jlong JNICALL
Java_sun_management_VMManagementImpl_getTotalClassCount
  (JNIEnv *env, jobject dummy)
{
    /* JMM_CLASS_LOADED_COUNT is the total number of classes loaded */
    jlong count = jmm_interface->GetLongAttribute(env, NULL,
                                                  JMM_CLASS_LOADED_COUNT);
    return count;
}

JNIEXPORT jlong JNICALL
Java_sun_management_VMManagementImpl_getUnloadedClassCount
  (JNIEnv *env, jobject dummy)
{
    /* JMM_CLASS_UNLOADED_COUNT is the total number of classes unloaded */
    jlong count = jmm_interface->GetLongAttribute(env, NULL,
                                                  JMM_CLASS_UNLOADED_COUNT);
    return count;
}

JNIEXPORT jboolean JNICALL
Java_sun_management_VMManagementImpl_getVerboseGC
  (JNIEnv *env, jobject dummy)
{
    return jmm_interface->GetBoolAttribute(env, JMM_VERBOSE_GC);
}

JNIEXPORT jboolean JNICALL
Java_sun_management_VMManagementImpl_getVerboseClass
  (JNIEnv *env, jobject dummy)
{
    return jmm_interface->GetBoolAttribute(env, JMM_VERBOSE_CLASS);
}

JNIEXPORT jlong JNICALL
Java_sun_management_VMManagementImpl_getTotalThreadCount
  (JNIEnv *env, jobject dummy)
{
    return jmm_interface->GetLongAttribute(env, NULL,
                                           JMM_THREAD_TOTAL_COUNT);
}

JNIEXPORT jint JNICALL
Java_sun_management_VMManagementImpl_getLiveThreadCount
  (JNIEnv *env, jobject dummy)
{
    jlong count = jmm_interface->GetLongAttribute(env, NULL,
                                                  JMM_THREAD_LIVE_COUNT);
    return (jint) count;
}

JNIEXPORT jint JNICALL
Java_sun_management_VMManagementImpl_getPeakThreadCount
  (JNIEnv *env, jobject dummy)
{
    jlong count = jmm_interface->GetLongAttribute(env, NULL,
                                                  JMM_THREAD_PEAK_COUNT);
    return (jint) count;
}

JNIEXPORT jint JNICALL
Java_sun_management_VMManagementImpl_getDaemonThreadCount
  (JNIEnv *env, jobject dummy)
{
    jlong count = jmm_interface->GetLongAttribute(env, NULL,
                                                  JMM_THREAD_DAEMON_COUNT);
    return (jint) count;
}

JNIEXPORT jlong JNICALL
Java_sun_management_VMManagementImpl_getTotalCompileTime
  (JNIEnv *env, jobject dummy)
{
    return jmm_interface->GetLongAttribute(env, NULL,
                                           JMM_COMPILE_TOTAL_TIME_MS);
}

JNIEXPORT jlong JNICALL
Java_sun_management_VMManagementImpl_getStartupTime
  (JNIEnv *env, jobject dummy)
{
    return jmm_interface->GetLongAttribute(env, NULL,
                                           JMM_JVM_INIT_DONE_TIME_MS);
}

JNIEXPORT jlong JNICALL
Java_sun_management_VMManagementImpl_getUptime0
  (JNIEnv *env, jobject dummy)
{
    return jmm_interface->GetLongAttribute(env, NULL, JMM_JVM_UPTIME_MS);
}

JNIEXPORT jboolean JNICALL
Java_sun_management_VMManagementImpl_isThreadContentionMonitoringEnabled
  (JNIEnv *env, jobject dummy)
{
    return jmm_interface->GetBoolAttribute(env,
                                           JMM_THREAD_CONTENTION_MONITORING);
}

JNIEXPORT jboolean JNICALL
Java_sun_management_VMManagementImpl_isThreadCpuTimeEnabled
  (JNIEnv *env, jobject dummy)
{
    return jmm_interface->GetBoolAttribute(env, JMM_THREAD_CPU_TIME);
}

JNIEXPORT jboolean JNICALL
Java_sun_management_VMManagementImpl_isThreadAllocatedMemoryEnabled
  (JNIEnv *env, jobject dummy)
{
    return jmm_interface->GetBoolAttribute(env, JMM_THREAD_ALLOCATED_MEMORY);
}

JNIEXPORT jint JNICALL
Java_sun_management_VMManagementImpl_getProcessId
  (JNIEnv *env, jobject dummy)
{
    jlong pid = jmm_interface->GetLongAttribute(env, NULL,
                                                JMM_OS_PROCESS_ID);
    return (jint) pid;
}

JNIEXPORT jint JNICALL
Java_sun_management_VMManagementImpl_getAvailableProcessors
  (JNIEnv *env, jobject dummy)
{
    return JVM_ActiveProcessorCount();
}

JNIEXPORT jlong JNICALL
Java_sun_management_VMManagementImpl_getSafepointCount
  (JNIEnv *env, jobject dummy)
{
    return jmm_interface->GetLongAttribute(env, NULL,
                                           JMM_SAFEPOINT_COUNT);
}

JNIEXPORT jlong JNICALL
Java_sun_management_VMManagementImpl_getTotalSafepointTime
  (JNIEnv *env, jobject dummy)
{
    return jmm_interface->GetLongAttribute(env, NULL,
                                           JMM_TOTAL_STOPPED_TIME_MS);
}

JNIEXPORT jlong JNICALL
Java_sun_management_VMManagementImpl_getSafepointSyncTime
  (JNIEnv *env, jobject dummy)
{
    return jmm_interface->GetLongAttribute(env, NULL,
                                           JMM_TOTAL_SAFEPOINTSYNC_TIME_MS);
}

JNIEXPORT jlong JNICALL
Java_sun_management_VMManagementImpl_getTotalApplicationNonStoppedTime
  (JNIEnv *env, jobject dummy)
{
    return jmm_interface->GetLongAttribute(env, NULL,
                                           JMM_TOTAL_APP_TIME_MS);
}

JNIEXPORT jlong JNICALL
Java_sun_management_VMManagementImpl_getLoadedClassSize
  (JNIEnv *env, jobject dummy)
{
    return jmm_interface->GetLongAttribute(env, NULL,
                                           JMM_CLASS_LOADED_BYTES);
}

JNIEXPORT jlong JNICALL
Java_sun_management_VMManagementImpl_getUnloadedClassSize
  (JNIEnv *env, jobject dummy)
{
    return jmm_interface->GetLongAttribute(env, NULL,
                                           JMM_CLASS_UNLOADED_BYTES);
}
JNIEXPORT jlong JNICALL
Java_sun_management_VMManagementImpl_getClassLoadingTime
  (JNIEnv *env, jobject dummy)
{
    return jmm_interface->GetLongAttribute(env, NULL,
                                           JMM_TOTAL_CLASSLOAD_TIME_MS);
}


JNIEXPORT jlong JNICALL
Java_sun_management_VMManagementImpl_getMethodDataSize
  (JNIEnv *env, jobject dummy)
{
    return jmm_interface->GetLongAttribute(env, NULL,
                                           JMM_METHOD_DATA_SIZE_BYTES);
}

JNIEXPORT jlong JNICALL
Java_sun_management_VMManagementImpl_getInitializedClassCount
  (JNIEnv *env, jobject dummy)
{
    return jmm_interface->GetLongAttribute(env, NULL,
                                           JMM_CLASS_INIT_TOTAL_COUNT);
}

JNIEXPORT jlong JNICALL
Java_sun_management_VMManagementImpl_getClassInitializationTime
  (JNIEnv *env, jobject dummy)
{
    return jmm_interface->GetLongAttribute(env, NULL,
                                           JMM_CLASS_INIT_TOTAL_TIME_MS);
}

JNIEXPORT jlong JNICALL
Java_sun_management_VMManagementImpl_getClassVerificationTime
  (JNIEnv *env, jobject dummy)
{
    return jmm_interface->GetLongAttribute(env, NULL,
                                           JMM_CLASS_VERIFY_TOTAL_TIME_MS);
}
