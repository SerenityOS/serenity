/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include "com_sun_management_internal_OperatingSystemImpl.h"

#include <sys/time.h>
#include <mach/mach.h>
#include <mach/task_info.h>

#include "jvm.h"

JNIEXPORT jdouble JNICALL
Java_com_sun_management_internal_OperatingSystemImpl_getCpuLoad0
(JNIEnv *env, jobject dummy)
{
    // This code is influenced by the darwin top source

    kern_return_t kr;
    mach_msg_type_number_t count;
    host_cpu_load_info_data_t load;

    static jlong last_used  = 0;
    static jlong last_total = 0;

    count = HOST_CPU_LOAD_INFO_COUNT;
    kr = host_statistics(mach_host_self(), HOST_CPU_LOAD_INFO, (host_info_t)&load, &count);
    if (kr != KERN_SUCCESS) {
        return -1;
    }

    jlong used  = load.cpu_ticks[CPU_STATE_USER] + load.cpu_ticks[CPU_STATE_NICE] + load.cpu_ticks[CPU_STATE_SYSTEM];
    jlong total = used + load.cpu_ticks[CPU_STATE_IDLE];

    if (last_used == 0 || last_total == 0) {
        // First call, just set the last values
        last_used  = used;
        last_total = total;
        // return 0 since we have no data, not -1 which indicates error
        return 0;
    }

    jlong used_delta  = used - last_used;
    jlong total_delta = total - last_total;

    jdouble cpu = (jdouble) used_delta / total_delta;

    last_used  = used;
    last_total = total;

    return cpu;
}


#define TIME_VALUE_TO_TIMEVAL(a, r) do {  \
     (r)->tv_sec = (a)->seconds;          \
     (r)->tv_usec = (a)->microseconds;    \
} while (0)


#define TIME_VALUE_TO_MICROSECONDS(TV) \
     ((TV).tv_sec * 1000 * 1000 + (TV).tv_usec)


JNIEXPORT jdouble JNICALL
Java_com_sun_management_internal_OperatingSystemImpl_getProcessCpuLoad0
(JNIEnv *env, jobject dummy)
{
    // This code is influenced by the darwin top source

    struct task_basic_info_64 task_info_data;
    struct task_thread_times_info thread_info_data;
    struct timeval user_timeval, system_timeval, task_timeval;
    struct timeval now;
    mach_port_t task = mach_task_self();
    kern_return_t kr;

    static jlong last_task_time = 0;
    static jlong last_time      = 0;

    mach_msg_type_number_t thread_info_count = TASK_THREAD_TIMES_INFO_COUNT;
    kr = task_info(task,
            TASK_THREAD_TIMES_INFO,
            (task_info_t)&thread_info_data,
            &thread_info_count);
    if (kr != KERN_SUCCESS) {
        // Most likely cause: |task| is a zombie.
        return -1;
    }

    mach_msg_type_number_t count = TASK_BASIC_INFO_64_COUNT;
    kr = task_info(task,
            TASK_BASIC_INFO_64,
            (task_info_t)&task_info_data,
            &count);
    if (kr != KERN_SUCCESS) {
        // Most likely cause: |task| is a zombie.
        return -1;
    }

    /* Set total_time. */
    // thread info contains live time...
    TIME_VALUE_TO_TIMEVAL(&thread_info_data.user_time, &user_timeval);
    TIME_VALUE_TO_TIMEVAL(&thread_info_data.system_time, &system_timeval);
    timeradd(&user_timeval, &system_timeval, &task_timeval);

    // ... task info contains terminated time.
    TIME_VALUE_TO_TIMEVAL(&task_info_data.user_time, &user_timeval);
    TIME_VALUE_TO_TIMEVAL(&task_info_data.system_time, &system_timeval);
    timeradd(&user_timeval, &task_timeval, &task_timeval);
    timeradd(&system_timeval, &task_timeval, &task_timeval);

    if (gettimeofday(&now, NULL) < 0) {
       return -1;
    }
    jint ncpus      = JVM_ActiveProcessorCount();
    jlong time      = TIME_VALUE_TO_MICROSECONDS(now) * ncpus;
    jlong task_time = TIME_VALUE_TO_MICROSECONDS(task_timeval);

    if ((last_task_time == 0) || (last_time == 0)) {
        // First call, just set the last values.
        last_task_time = task_time;
        last_time      = time;
        // return 0 since we have no data, not -1 which indicates error
        return 0;
    }

    jlong task_time_delta = task_time - last_task_time;
    jlong time_delta      = time - last_time;
    if (time_delta == 0) {
        return -1;
    }

    jdouble cpu = (jdouble) task_time_delta / time_delta;

    last_task_time = task_time;
    last_time      = time;

    return cpu;
 }

JNIEXPORT jdouble JNICALL
Java_com_sun_management_internal_OperatingSystemImpl_getSingleCpuLoad0
(JNIEnv *env, jobject dummy, jint cpu_number)
{
    return -1.0;
}

JNIEXPORT jlong JNICALL
Java_com_sun_management_internal_OperatingSystemImpl_getHostTotalCpuTicks0
(JNIEnv *env, jobject mbean)
{
    return -1;
}

JNIEXPORT jint JNICALL
Java_com_sun_management_internal_OperatingSystemImpl_getHostConfiguredCpuCount0
(JNIEnv *env, jobject mbean)
{
    return -1;
}

JNIEXPORT jint JNICALL
Java_com_sun_management_internal_OperatingSystemImpl_getHostOnlineCpuCount0
(JNIEnv *env, jobject mbean)
{
    return -1;
}

