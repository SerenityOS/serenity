/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef _JAVA_JMM_H_
#define _JAVA_JMM_H_

/*
 * This is a private interface used by JDK for JVM monitoring
 * and management.
 *
 * Bump the version number when either of the following happens:
 *
 * 1. There is a change in functions in JmmInterface.
 *
 * 2. There is a change in the contract between VM and Java classes.
 */

#include "jni.h"

#ifdef __cplusplus
extern "C" {
#endif

enum {
  JMM_VERSION_1   = 0x20010000,
  JMM_VERSION_1_0 = 0x20010000,
  JMM_VERSION_1_1 = 0x20010100, // JDK 6
  JMM_VERSION_1_2 = 0x20010200, // JDK 7
  JMM_VERSION_1_2_1 = 0x20010201, // JDK 7 GA
  JMM_VERSION_1_2_2 = 0x20010202,
  JMM_VERSION_2   = 0x20020000, // JDK 10
  JMM_VERSION_3   = 0x20030000, // JDK 14
  JMM_VERSION     = JMM_VERSION_3
};

typedef struct {
  unsigned int isLowMemoryDetectionSupported : 1;
  unsigned int isCompilationTimeMonitoringSupported : 1;
  unsigned int isThreadContentionMonitoringSupported : 1;
  unsigned int isCurrentThreadCpuTimeSupported : 1;
  unsigned int isOtherThreadCpuTimeSupported : 1;
  unsigned int isObjectMonitorUsageSupported : 1;
  unsigned int isSynchronizerUsageSupported : 1;
  unsigned int isThreadAllocatedMemorySupported : 1;
  unsigned int isRemoteDiagnosticCommandsSupported : 1;
  unsigned int : 22;
} jmmOptionalSupport;

typedef enum {
  JMM_CLASS_LOADED_COUNT             = 1,    /* Total number of loaded classes */
  JMM_CLASS_UNLOADED_COUNT           = 2,    /* Total number of unloaded classes */
  JMM_THREAD_TOTAL_COUNT             = 3,    /* Total number of threads that have been started */
  JMM_THREAD_LIVE_COUNT              = 4,    /* Current number of live threads */
  JMM_THREAD_PEAK_COUNT              = 5,    /* Peak number of live threads */
  JMM_THREAD_DAEMON_COUNT            = 6,    /* Current number of daemon threads */
  JMM_JVM_INIT_DONE_TIME_MS          = 7,    /* Time when the JVM finished initialization */
  JMM_COMPILE_TOTAL_TIME_MS          = 8,    /* Total accumulated time spent in compilation */
  JMM_GC_TIME_MS                     = 9,    /* Total accumulated time spent in collection */
  JMM_GC_COUNT                       = 10,   /* Total number of collections */
  JMM_JVM_UPTIME_MS                  = 11,   /* The JVM uptime in milliseconds */

  JMM_INTERNAL_ATTRIBUTE_INDEX       = 100,
  JMM_CLASS_LOADED_BYTES             = 101,  /* Number of bytes loaded instance classes */
  JMM_CLASS_UNLOADED_BYTES           = 102,  /* Number of bytes unloaded instance classes */
  JMM_TOTAL_CLASSLOAD_TIME_MS        = 103,  /* Accumulated VM class loader time */
  JMM_VM_GLOBAL_COUNT                = 104,  /* Number of VM internal flags */
  JMM_SAFEPOINT_COUNT                = 105,  /* Total number of safepoints */
  JMM_TOTAL_SAFEPOINTSYNC_TIME_MS    = 106,  /* Accumulated time spent getting to safepoints */
  JMM_TOTAL_STOPPED_TIME_MS          = 107,  /* Accumulated time spent at safepoints */
  JMM_TOTAL_APP_TIME_MS              = 108,  /* Accumulated time spent in Java application */
  JMM_VM_THREAD_COUNT                = 109,  /* Current number of VM internal threads */
  JMM_CLASS_INIT_TOTAL_COUNT         = 110,  /* Number of classes for which initializers were run */
  JMM_CLASS_INIT_TOTAL_TIME_MS       = 111,  /* Accumulated time spent in class initializers */
  JMM_METHOD_DATA_SIZE_BYTES         = 112,  /* Size of method data in memory */
  JMM_CLASS_VERIFY_TOTAL_TIME_MS     = 113,  /* Accumulated time spent in class verifier */
  JMM_SHARED_CLASS_LOADED_COUNT      = 114,  /* Number of shared classes loaded */
  JMM_SHARED_CLASS_UNLOADED_COUNT    = 115,  /* Number of shared classes unloaded */
  JMM_SHARED_CLASS_LOADED_BYTES      = 116,  /* Number of bytes loaded shared classes */
  JMM_SHARED_CLASS_UNLOADED_BYTES    = 117,  /* Number of bytes unloaded shared classes */

  JMM_OS_ATTRIBUTE_INDEX             = 200,
  JMM_OS_PROCESS_ID                  = 201,  /* Process id of the JVM */
  JMM_OS_MEM_TOTAL_PHYSICAL_BYTES    = 202,  /* Physical memory size */

  JMM_GC_EXT_ATTRIBUTE_INFO_SIZE     = 401   /* the size of the GC specific attributes for a given GC memory manager */
} jmmLongAttribute;

typedef enum {
  JMM_VERBOSE_GC                     = 21,
  JMM_VERBOSE_CLASS                  = 22,
  JMM_THREAD_CONTENTION_MONITORING   = 23,
  JMM_THREAD_CPU_TIME                = 24,
  JMM_THREAD_ALLOCATED_MEMORY        = 25
} jmmBoolAttribute;


enum {
  JMM_THREAD_STATE_FLAG_SUSPENDED = 0x00100000,
  JMM_THREAD_STATE_FLAG_NATIVE    = 0x00400000
};

#define JMM_THREAD_STATE_FLAG_MASK  0xFFF00000

typedef enum {
  JMM_STAT_PEAK_THREAD_COUNT         = 801,
  JMM_STAT_THREAD_CONTENTION_COUNT   = 802,
  JMM_STAT_THREAD_CONTENTION_TIME    = 803,
  JMM_STAT_THREAD_CONTENTION_STAT    = 804,
  JMM_STAT_PEAK_POOL_USAGE           = 805,
  JMM_STAT_GC_STAT                   = 806
} jmmStatisticType;

typedef enum {
  JMM_USAGE_THRESHOLD_HIGH            = 901,
  JMM_USAGE_THRESHOLD_LOW             = 902,
  JMM_COLLECTION_USAGE_THRESHOLD_HIGH = 903,
  JMM_COLLECTION_USAGE_THRESHOLD_LOW  = 904
} jmmThresholdType;

/* Should match what is allowed in globals.hpp */
typedef enum {
  JMM_VMGLOBAL_TYPE_UNKNOWN  = 0,
  JMM_VMGLOBAL_TYPE_JBOOLEAN = 1,
  JMM_VMGLOBAL_TYPE_JSTRING  = 2,
  JMM_VMGLOBAL_TYPE_JLONG    = 3,
  JMM_VMGLOBAL_TYPE_JDOUBLE  = 4
} jmmVMGlobalType;

typedef enum {
  JMM_VMGLOBAL_ORIGIN_DEFAULT      = 1,   /* Default value */
  JMM_VMGLOBAL_ORIGIN_COMMAND_LINE = 2,   /* Set at command line (or JNI invocation) */
  JMM_VMGLOBAL_ORIGIN_MANAGEMENT   = 3,   /* Set via management interface */
  JMM_VMGLOBAL_ORIGIN_ENVIRON_VAR  = 4,   /* Set via environment variables */
  JMM_VMGLOBAL_ORIGIN_CONFIG_FILE  = 5,   /* Set via config file (such as .hotspotrc) */
  JMM_VMGLOBAL_ORIGIN_ERGONOMIC    = 6,   /* Set via ergonomic */
  JMM_VMGLOBAL_ORIGIN_ATTACH_ON_DEMAND = 7,   /* Set via attach */
  JMM_VMGLOBAL_ORIGIN_OTHER        = 99   /* Set via some other mechanism */
} jmmVMGlobalOrigin;

typedef struct {
  jstring           name;
  jvalue            value;
  jmmVMGlobalType   type;           /* Data type */
  jmmVMGlobalOrigin origin;         /* Default or non-default value */
  unsigned int      writeable : 1;  /* dynamically writeable */
  unsigned int      external  : 1;  /* external supported interface */
  unsigned int      reserved  : 30;
  void *reserved1;
  void *reserved2;
} jmmVMGlobal;

typedef struct {
  const char*  name;
  char         type;
  const char*  description;
} jmmExtAttributeInfo;

/* Caller has to set the following fields before calling GetLastGCStat
 *   o usage_before_gc               - array of MemoryUsage objects
 *   o usage_after_gc                - array of MemoryUsage objects
 *   o gc_ext_attribute_values_size - size of gc_ext_atttribute_values array
 *   o gc_ext_attribtue_values      - array of jvalues
 */
typedef struct {
  jlong        gc_index;                       /* Index of the collections */
  jlong        start_time;                     /* Start time of the GC */
  jlong        end_time;                       /* End time of the GC */
  jobjectArray usage_before_gc;                /* Memory usage array before GC */
  jobjectArray usage_after_gc;                 /* Memory usage array after GC */
  jint         gc_ext_attribute_values_size;   /* set by the caller of GetGCStat */
  jvalue*      gc_ext_attribute_values;        /* Array of jvalue for GC extension attributes */
  jint         num_gc_ext_attributes;          /* number of GC extension attribute values s are filled */
                                               /* -1 indicates gc_ext_attribute_values is not big enough */
} jmmGCStat;

typedef struct {
  const char* name;                /* Name of the diagnostic command */
  const char* description;         /* Short description */
  const char* impact;              /* Impact on the JVM */
  const char* permission_class;    /* Class name of the required permission if any */
  const char* permission_name;     /* Permission name of the required permission if any */
  const char* permission_action;   /* Action name of the required permission if any*/
  int         num_arguments;       /* Number of supported options or arguments */
  jboolean    enabled;             /* True if the diagnostic command can be invoked, false otherwise*/
} dcmdInfo;

typedef struct {
  const char* name;                /* Option/Argument name*/
  const char* description;         /* Short description */
  const char* type;                /* Type: STRING, BOOLEAN, etc. */
  const char* default_string;      /* Default value in a parsable string */
  jboolean    mandatory;           /* True if the option/argument is mandatory */
  jboolean    option;              /* True if it is an option, false if it is an argument */
                                   /* (see diagnosticFramework.hpp for option/argument definitions) */
  jboolean    multiple;            /* True is the option can be specified several time */
  int         position;            /* Expected position for this argument (this field is */
                                   /* meaningless for options) */
} dcmdArgInfo;

typedef struct jmmInterface_1_ {
  void*        reserved1;
  void*        reserved2;

  jint         (JNICALL *GetVersion)             (JNIEnv *env);

  jint         (JNICALL *GetOptionalSupport)     (JNIEnv *env,
                                                  jmmOptionalSupport* support_ptr);

  jint         (JNICALL *GetThreadInfo)          (JNIEnv *env,
                                                  jlongArray ids,
                                                  jint maxDepth,
                                                  jobjectArray infoArray);

  jobjectArray (JNICALL *GetMemoryPools)         (JNIEnv* env, jobject mgr);

  jobjectArray (JNICALL *GetMemoryManagers)      (JNIEnv* env, jobject pool);

  jobject      (JNICALL *GetMemoryPoolUsage)     (JNIEnv* env, jobject pool);
  jobject      (JNICALL *GetPeakMemoryPoolUsage) (JNIEnv* env, jobject pool);

  jlong        (JNICALL *GetOneThreadAllocatedMemory)
                                                 (JNIEnv *env,
                                                  jlong thread_id);
  void         (JNICALL *GetThreadAllocatedMemory)
                                                 (JNIEnv *env,
                                                  jlongArray ids,
                                                  jlongArray sizeArray);

  jobject      (JNICALL *GetMemoryUsage)         (JNIEnv* env, jboolean heap);

  jlong        (JNICALL *GetLongAttribute)       (JNIEnv *env, jobject obj, jmmLongAttribute att);
  jboolean     (JNICALL *GetBoolAttribute)       (JNIEnv *env, jmmBoolAttribute att);
  jboolean     (JNICALL *SetBoolAttribute)       (JNIEnv *env, jmmBoolAttribute att, jboolean flag);

  jint         (JNICALL *GetLongAttributes)      (JNIEnv *env,
                                                  jobject obj,
                                                  jmmLongAttribute* atts,
                                                  jint count,
                                                  jlong* result);

  jobjectArray (JNICALL *FindCircularBlockedThreads) (JNIEnv *env);

  // Not used in JDK 6 or JDK 7
  jlong        (JNICALL *GetThreadCpuTime)       (JNIEnv *env, jlong thread_id);

  jobjectArray (JNICALL *GetVMGlobalNames)       (JNIEnv *env);
  jint         (JNICALL *GetVMGlobals)           (JNIEnv *env,
                                                  jobjectArray names,
                                                  jmmVMGlobal *globals,
                                                  jint count);

  jint         (JNICALL *GetInternalThreadTimes) (JNIEnv *env,
                                                  jobjectArray names,
                                                  jlongArray times);

  jboolean     (JNICALL *ResetStatistic)         (JNIEnv *env,
                                                  jvalue obj,
                                                  jmmStatisticType type);

  void         (JNICALL *SetPoolSensor)          (JNIEnv *env,
                                                  jobject pool,
                                                  jmmThresholdType type,
                                                  jobject sensor);

  jlong        (JNICALL *SetPoolThreshold)       (JNIEnv *env,
                                                  jobject pool,
                                                  jmmThresholdType type,
                                                  jlong threshold);
  jobject      (JNICALL *GetPoolCollectionUsage) (JNIEnv* env, jobject pool);

  jint         (JNICALL *GetGCExtAttributeInfo)  (JNIEnv *env,
                                                  jobject mgr,
                                                  jmmExtAttributeInfo *ext_info,
                                                  jint count);
  void         (JNICALL *GetLastGCStat)          (JNIEnv *env,
                                                  jobject mgr,
                                                  jmmGCStat *gc_stat);

  jlong        (JNICALL *GetThreadCpuTimeWithKind)
                                                 (JNIEnv *env,
                                                  jlong thread_id,
                                                  jboolean user_sys_cpu_time);
  void         (JNICALL *GetThreadCpuTimesWithKind)
                                                 (JNIEnv *env,
                                                  jlongArray ids,
                                                  jlongArray timeArray,
                                                  jboolean user_sys_cpu_time);

  jint         (JNICALL *DumpHeap0)              (JNIEnv *env,
                                                  jstring outputfile,
                                                  jboolean live);
  jobjectArray (JNICALL *FindDeadlocks)          (JNIEnv *env,
                                                  jboolean object_monitors_only);
  void         (JNICALL *SetVMGlobal)            (JNIEnv *env,
                                                  jstring flag_name,
                                                  jvalue  new_value);
  void*        reserved6;
  jobjectArray (JNICALL *DumpThreads)            (JNIEnv *env,
                                                  jlongArray ids,
                                                  jboolean lockedMonitors,
                                                  jboolean lockedSynchronizers,
                                                  jint maxDepth);
  void         (JNICALL *SetGCNotificationEnabled) (JNIEnv *env,
                                                    jobject mgr,
                                                    jboolean enabled);
  jobjectArray (JNICALL *GetDiagnosticCommands)  (JNIEnv *env);
  void         (JNICALL *GetDiagnosticCommandInfo)
                                                 (JNIEnv *env,
                                                  jobjectArray cmds,
                                                  dcmdInfo *infoArray);
  void         (JNICALL *GetDiagnosticCommandArgumentsInfo)
                                                 (JNIEnv *env,
                                                  jstring commandName,
                                                  dcmdArgInfo *infoArray);
  jstring      (JNICALL *ExecuteDiagnosticCommand)
                                                 (JNIEnv *env,
                                                  jstring command);
  void         (JNICALL *SetDiagnosticFrameworkNotificationEnabled)
                                                 (JNIEnv *env,
                                                  jboolean enabled);
} JmmInterface;

#ifdef __cplusplus
} /* extern "C" */
#endif /* __cplusplus */

#endif /* !_JAVA_JMM_H_ */
