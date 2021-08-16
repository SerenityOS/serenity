/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

/*
 This test case to test the following:

           GetCurrentThreadCpuTime
           GetThreadCpuTime
           GetTime
 */

#define VARIANCE (0.10)
#define VARIANCE_PERCENT (VARIANCE * 100.0)

#include <stdio.h>
#include <string.h>
#include "jvmti.h"
#include "agent_common.h"

#include "jni_tools.h"

extern "C" {

#define JVMTI_ERROR_CHECK_DURING_ONLOAD(str,res) if (res != JVMTI_ERROR_NONE) { printf("Fatal error: %s - %d\n", str, res); return JNI_ERR; }

#define JVMTI_ERROR_CHECK_RETURN(str,res) if (res != JVMTI_ERROR_NONE) { printf("Error: %s - %d\n", str, res); return; }

#define JVMTI_ERROR_CHECK(str,res) if (res != JVMTI_ERROR_NONE) { printf("Error: %s - %d\n", str, res); }

#define THREADS_LIMIT 200


jvmtiEnv *jvmti;
jint iGlobalStatus = 0;
jthread susp_thrd[THREADS_LIMIT];
static jvmtiEventCallbacks callbacks;
static jvmtiCapabilities capabilities;
jrawMonitorID jraw_monitor[20];
  jlong initial_time;

  struct ThreadInfo {
    jint iterationCount;
    jlong currThreadTime;
    jlong threadTime;
    jweak ref;
  } thread_info[THREADS_LIMIT];

int printdump = 1;


void debug_printf(const char *fmt, ...) {
    va_list args;

    va_start(args, fmt);
    if (printdump) {
        vprintf(fmt, args);
    }
    va_end(args);
}

void JNICALL vmInit(jvmtiEnv *jvmti_env, JNIEnv *env, jthread thread) {
    jvmtiError err;
    char buffer[32];

    debug_printf("VMInit event\n");

    debug_printf("jvmti GetTime \n");
    err = jvmti_env->GetTime(&initial_time);
    JVMTI_ERROR_CHECK("GetTime", err);
    debug_printf("  Initial time: %s ns\n",
        jlong_to_string(initial_time, buffer));
}

void JNICALL vmExit(jvmtiEnv *jvmti_env, JNIEnv *env) {
    debug_printf("VMDeath event\n");
}


void init_callbacks() {
    memset((void *)&callbacks, 0, sizeof(jvmtiEventCallbacks));
    callbacks.VMInit = vmInit;
    callbacks.VMDeath = vmExit;
}


#ifdef STATIC_BUILD
JNIEXPORT jint JNICALL Agent_OnLoad_JvmtiTest(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNICALL Agent_OnAttach_JvmtiTest(JavaVM *jvm, char *options, void *reserved) {
    return Agent_Initialize(jvm, options, reserved);
}
JNIEXPORT jint JNI_OnLoad_JvmtiTest(JavaVM *jvm, char *options, void *reserved) {
    return JNI_VERSION_1_8;
}
#endif
jint Agent_Initialize(JavaVM * jvm, char *options, void *reserved) {
    jint res;
    jvmtiError err;

    if (options && strlen(options) > 0) {
        if (strstr(options, "printdump")) {
            printdump = 1;
        }
    }

    res = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
    if (res < 0) {
        printf("Wrong result of a valid call to GetEnv!\n");
        return JNI_ERR;
    }


    /* Add capabilities */
    memset(&capabilities, 0, sizeof(jvmtiCapabilities));
    capabilities.can_get_current_thread_cpu_time = 1;
    capabilities.can_get_thread_cpu_time = 1;
    err = jvmti->AddCapabilities(&capabilities);
    JVMTI_ERROR_CHECK_DURING_ONLOAD("(AddCapabilities)", err);

    /* Enable events */
    init_callbacks();
    res = jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
    JVMTI_ERROR_CHECK_DURING_ONLOAD("SetEventCallbacks returned error", res);

    res = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, NULL);
    JVMTI_ERROR_CHECK_DURING_ONLOAD("SetEventNotificationMode for VM_INIT returned error", res);

    res = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_DEATH, NULL);
    JVMTI_ERROR_CHECK_DURING_ONLOAD("SetEventNotificationMode for vm death event returned error", res);

    return JNI_OK;
}


JNIEXPORT jint JNICALL
Java_nsk_jvmti_unit_timers_JvmtiTest_GetResult(JNIEnv * env, jclass cls) {
    return iGlobalStatus;
}

#define milli(x) ((x)/(1000L * 1000L))

JNIEXPORT void JNICALL
Java_nsk_jvmti_unit_timers_JvmtiTest_RegisterCompletedThread(JNIEnv * env,
        jclass cls, jthread thread, jint threadNumber, jint iterationCount) {
    jvmtiError ret;
    jlong curr;

    debug_printf("jvmti GetCurrentThreadCpuTime \n");
    ret = jvmti->GetCurrentThreadCpuTime(&curr);
    JVMTI_ERROR_CHECK_RETURN("GetCurrentThreadCpuTime", ret);

    thread_info[threadNumber].iterationCount = iterationCount;
    thread_info[threadNumber].currThreadTime = curr;
    thread_info[threadNumber].ref = env->NewWeakGlobalRef(thread);
}

static void print_timerinfo(jvmtiTimerInfo* timerInfo) {
  char buffer[32];
  const char* timerKind;
  switch (timerInfo->kind) {
  case JVMTI_TIMER_USER_CPU:
    timerKind = "JVMTI_TIMER_USER_CPU";
    break;
  case JVMTI_TIMER_TOTAL_CPU:
    timerKind = "JVMTI_TIMER_TOTAL_CPU";
    break;
  case JVMTI_TIMER_ELAPSED:
    timerKind = "JVMTI_TIMER_ELAPSED_CPU";
    break;
  default:
    timerKind = "<unknown>";
    break;
  }
  debug_printf("  Max = %s [%s %s] kind = %s\n",
               jlong_to_string(timerInfo->max_value, buffer),
               timerInfo->may_skip_forward? "skip-forward" : "stable",
               timerInfo->may_skip_backward? "skip-backward" : "stable",
               timerKind);
}

JNIEXPORT void JNICALL
Java_nsk_jvmti_unit_timers_JvmtiTest_Analyze(JNIEnv * env, jclass cls) {
    jvmtiError ret;
    jlong now;
    jlong etime;
    jint thrCnt;
    jvmtiTimerInfo timerInfoCurr;
    jvmtiTimerInfo timerInfoOther;
    jvmtiTimerInfo timerInfoTime;
    jint processor_count;
    jint totalIter = 0;
    jlong totalTimeCurr = 0;
    jlong totalTime = 0;
    jlong possibleTime;
    double one_iter_cost;
    jthread *thrArray;
    int k;
    int i;
    char buffer[32];

    debug_printf("jvmti GetTime \n");
    ret = jvmti->GetTime(&now);
    JVMTI_ERROR_CHECK_RETURN("GetTime", ret);
    etime = now - initial_time;
    debug_printf("  Elapsed time: %s ms\n",
        jlong_to_string(milli(etime), buffer));

    debug_printf("jvmti GetCurrentThreadCpuTimerInfo \n");
    ret = jvmti->GetCurrentThreadCpuTimerInfo(&timerInfoCurr);
    JVMTI_ERROR_CHECK_RETURN("GetCurrentThreadCpuTimerInfo", ret);
    print_timerinfo(&timerInfoCurr);

    debug_printf("jvmti GetThreadCpuTimerInfo \n");
    ret = jvmti->GetThreadCpuTimerInfo(&timerInfoOther);
    JVMTI_ERROR_CHECK_RETURN("GetThreadCpuTimerInfo", ret);
    print_timerinfo(&timerInfoOther);

    debug_printf("jvmti GetTimerInfo \n");
    ret = jvmti->GetTimerInfo(&timerInfoTime);
    JVMTI_ERROR_CHECK_RETURN("GetTimerInfo", ret);
    print_timerinfo(&timerInfoTime);

    debug_printf("jvmti GetAvailableProcessors \n");
    ret = jvmti->GetAvailableProcessors(&processor_count);
    JVMTI_ERROR_CHECK_RETURN("GetAvailableProcessors", ret);
    debug_printf("  processor_count = %d\n", processor_count);

    debug_printf("jvmti GetAllThreads \n");
    ret = jvmti->GetAllThreads(&thrCnt, &thrArray);
    JVMTI_ERROR_CHECK_RETURN("GetAllThreads", ret);

    for (k = 0; k < thrCnt; ++k) {
      jlong oth;
      jthread thread;

      thread = thrArray[k];
      ret = jvmti->GetThreadCpuTime(thread, &oth);
      JVMTI_ERROR_CHECK_RETURN("GetThreadCpuTime", ret);

      for (i = 1; i < THREADS_LIMIT; ++i) {
        jweak tref = thread_info[i].ref;
        if (tref != 0) {
          if (env->IsSameObject(thread, tref)) {
            thread_info[i].threadTime = oth;
            break;
          }
        }
      }
      if (i == THREADS_LIMIT) {
        jvmtiThreadInfo info;
        info.name = (char*) "*retrieval error*";
        ret = jvmti->GetThreadInfo(thread, &info);
        JVMTI_ERROR_CHECK("GetThreadInfo %d \n", ret);

        debug_printf("non-test thread: %s - %s ms\n", info.name,
            jlong_to_string(milli(oth), buffer));
      }
    }
    for (i = 1; i < THREADS_LIMIT; ++i) {
      jweak tref = thread_info[i].ref;
      if (tref != 0) {
        totalIter += thread_info[i].iterationCount;
        totalTimeCurr += thread_info[i].currThreadTime;
        totalTime += thread_info[i].threadTime;
      }
    }
    possibleTime = etime * processor_count;
    debug_printf("Totals -- \n");
    debug_printf("  Iter = %d\n", totalIter);
    debug_printf("  Total GetThreadCpuTime =              %s ns", jlong_to_string(totalTime, buffer));
    debug_printf("    %s ms\n", jlong_to_string(milli(totalTime), buffer));
    debug_printf("  Total GetCurrentThreadCpuTimerInfo =  %s ns", jlong_to_string(totalTimeCurr, buffer));
    debug_printf("    %s ms\n", jlong_to_string(milli(totalTimeCurr), buffer));
    debug_printf("  GetTime =                             %s ns", jlong_to_string(etime, buffer));
    debug_printf("    %s ms\n", jlong_to_string(milli(etime), buffer));
    debug_printf("  GetTime * processor_count =           %s ns", jlong_to_string(possibleTime, buffer));
    debug_printf("    %s ms\n", jlong_to_string(milli(possibleTime), buffer));
    if (totalTime <= possibleTime) {
      debug_printf("Pass: ttime <= possible_time\n");
    } else {
      printf("FAIL: ttime > possible_time\n");
      iGlobalStatus = 2;
    }
    if (totalTimeCurr <= totalTime) {
      debug_printf("Pass: ttime_curr <= ttime\n");
    } else {
      printf("FAIL: ttime_curr > ttime\n");
      iGlobalStatus = 2;
    }
    if (totalTimeCurr >= totalTime*(1-VARIANCE)) {
      debug_printf("Pass: ttime_curr >= %2.0f%% of ttime\n", 100.0 - VARIANCE_PERCENT);
    } else {
      printf("FAIL: ttime_curr < %2.0f%% of ttime\n", 100.0 - VARIANCE_PERCENT);
      iGlobalStatus = 2;
    }
    one_iter_cost = (double)totalTime / totalIter;
    debug_printf("CURRENT: total time returned by \"GetCurrentThreadCpuTime\".\n");
    debug_printf("OTHER: total time returned by \"GetThreadCpuTime\".\n");
    debug_printf("EXPECT: the expected time if TestThread.run() had a proportional cost across all threads.\n");
    debug_printf("%% DIFF: how much \"Expect\" is off by.\n");
    debug_printf("THREAD ITERATIONS  CURRENT    OTHER    EXPECT   % DIFF\n");
    for (i = 1; i < THREADS_LIMIT; ++i) {
      jweak tref = thread_info[i].ref;
      if (tref != 0) {
        jint ic = thread_info[i].iterationCount;
        jlong ctt = thread_info[i].currThreadTime;
        jlong tt = thread_info[i].threadTime;
        double expt = ic * one_iter_cost;
        double var = 100.0 * ((double)tt - expt) / expt;
        debug_printf("%6d %10d %5s ms", i, ic,
                     jlong_to_string(milli(ctt), buffer));
        debug_printf(" %5s ms %5.0f ms %7.1f%%\n",
                     jlong_to_string(milli(tt), buffer), milli(expt), var);
        if (ctt <= tt) {
          debug_printf("Pass: currThreadTime <= threadTime\n");
        } else {
          printf("FAIL: currThreadTime > threadTime\n");
          iGlobalStatus = 2;
        }
        {
          int passed = ctt >= tt*(1-VARIANCE);
#ifdef _WIN32
          // On Windows the timer is only accurate to within 15ms. This sometimes triggers
          // failures if the expected max variance is close to or below 15ms. So we don't
          // fail in this case.
          if (!passed && milli(tt - ctt) <= 15) {
            printf("Passing due to special consideration on Windows for 15ms timer accuracy\n");
            passed = 1;
          }
#endif
          if (passed) {
            debug_printf("Pass: currThreadTime(" JLONG_FORMAT ") >= %2.0f%% of threadTime(" JLONG_FORMAT ")\n",
                         ctt, 100.0 - VARIANCE_PERCENT, tt);
          } else {
            printf("FAIL: currThreadTime(" JLONG_FORMAT ") < %2.0f%% of threadTime(" JLONG_FORMAT ")\n",
                   ctt, 100.0 - VARIANCE_PERCENT, tt);
            iGlobalStatus = 2;
          }
        }
      }
    }
}


}
