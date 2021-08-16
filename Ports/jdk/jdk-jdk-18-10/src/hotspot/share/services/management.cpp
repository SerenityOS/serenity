/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

#include "precompiled.hpp"
#include "jmm.h"
#include "classfile/classLoader.hpp"
#include "classfile/systemDictionary.hpp"
#include "classfile/vmClasses.hpp"
#include "compiler/compileBroker.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/iterator.hpp"
#include "memory/oopFactory.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/klass.hpp"
#include "oops/klass.inline.hpp"
#include "oops/objArrayKlass.hpp"
#include "oops/objArrayOop.inline.hpp"
#include "oops/oop.inline.hpp"
#include "oops/oopHandle.inline.hpp"
#include "oops/typeArrayOop.inline.hpp"
#include "runtime/flags/jvmFlag.hpp"
#include "runtime/globals.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/javaCalls.hpp"
#include "runtime/jniHandles.inline.hpp"
#include "runtime/notificationThread.hpp"
#include "runtime/os.hpp"
#include "runtime/thread.inline.hpp"
#include "runtime/threadSMR.hpp"
#include "runtime/vmOperations.hpp"
#include "services/classLoadingService.hpp"
#include "services/diagnosticCommand.hpp"
#include "services/diagnosticFramework.hpp"
#include "services/writeableFlags.hpp"
#include "services/heapDumper.hpp"
#include "services/lowMemoryDetector.hpp"
#include "services/gcNotifier.hpp"
#include "services/nmtDCmd.hpp"
#include "services/management.hpp"
#include "services/memoryManager.hpp"
#include "services/memoryPool.hpp"
#include "services/memoryService.hpp"
#include "services/runtimeService.hpp"
#include "services/threadService.hpp"
#include "utilities/debug.hpp"
#include "utilities/formatBuffer.hpp"
#include "utilities/macros.hpp"

PerfVariable* Management::_begin_vm_creation_time = NULL;
PerfVariable* Management::_end_vm_creation_time = NULL;
PerfVariable* Management::_vm_init_done_time = NULL;

InstanceKlass* Management::_diagnosticCommandImpl_klass = NULL;
InstanceKlass* Management::_garbageCollectorExtImpl_klass = NULL;
InstanceKlass* Management::_garbageCollectorMXBean_klass = NULL;
InstanceKlass* Management::_gcInfo_klass = NULL;
InstanceKlass* Management::_managementFactoryHelper_klass = NULL;
InstanceKlass* Management::_memoryManagerMXBean_klass = NULL;
InstanceKlass* Management::_memoryPoolMXBean_klass = NULL;
InstanceKlass* Management::_memoryUsage_klass = NULL;
InstanceKlass* Management::_sensor_klass = NULL;
InstanceKlass* Management::_threadInfo_klass = NULL;

jmmOptionalSupport Management::_optional_support = {0};
TimeStamp Management::_stamp;

void management_init() {
#if INCLUDE_MANAGEMENT
  Management::init();
  ThreadService::init();
  RuntimeService::init();
  ClassLoadingService::init();
#else
  ThreadService::init();
#endif // INCLUDE_MANAGEMENT
}

#if INCLUDE_MANAGEMENT

void Management::init() {
  EXCEPTION_MARK;

  // These counters are for java.lang.management API support.
  // They are created even if -XX:-UsePerfData is set and in
  // that case, they will be allocated on C heap.

  _begin_vm_creation_time =
            PerfDataManager::create_variable(SUN_RT, "createVmBeginTime",
                                             PerfData::U_None, CHECK);

  _end_vm_creation_time =
            PerfDataManager::create_variable(SUN_RT, "createVmEndTime",
                                             PerfData::U_None, CHECK);

  _vm_init_done_time =
            PerfDataManager::create_variable(SUN_RT, "vmInitDoneTime",
                                             PerfData::U_None, CHECK);

  // Initialize optional support
  _optional_support.isLowMemoryDetectionSupported = 1;
  _optional_support.isCompilationTimeMonitoringSupported = 1;
  _optional_support.isThreadContentionMonitoringSupported = 1;

  if (os::is_thread_cpu_time_supported()) {
    _optional_support.isCurrentThreadCpuTimeSupported = 1;
    _optional_support.isOtherThreadCpuTimeSupported = 1;
  } else {
    _optional_support.isCurrentThreadCpuTimeSupported = 0;
    _optional_support.isOtherThreadCpuTimeSupported = 0;
  }

  _optional_support.isObjectMonitorUsageSupported = 1;
#if INCLUDE_SERVICES
  // This depends on the heap inspector
  _optional_support.isSynchronizerUsageSupported = 1;
#endif // INCLUDE_SERVICES
  _optional_support.isThreadAllocatedMemorySupported = 1;
  _optional_support.isRemoteDiagnosticCommandsSupported = 1;

  // Registration of the diagnostic commands
  DCmdRegistrant::register_dcmds();
  DCmdRegistrant::register_dcmds_ext();
  uint32_t full_export = DCmd_Source_Internal | DCmd_Source_AttachAPI
                         | DCmd_Source_MBean;
  DCmdFactory::register_DCmdFactory(new DCmdFactoryImpl<NMTDCmd>(full_export, true, false));
}

void Management::initialize(TRAPS) {
  if (UseNotificationThread) {
    NotificationThread::initialize();
  }
  if (ManagementServer) {
    ResourceMark rm(THREAD);
    HandleMark hm(THREAD);

    // Load and initialize the jdk.internal.agent.Agent class
    // invoke startAgent method to start the management server
    Handle loader = Handle(THREAD, SystemDictionary::java_system_loader());
    Klass* k = SystemDictionary::resolve_or_null(vmSymbols::jdk_internal_agent_Agent(),
                                                   loader,
                                                   Handle(),
                                                   THREAD);
    if (k == NULL) {
      vm_exit_during_initialization("Management agent initialization failure: "
          "class jdk.internal.agent.Agent not found.");
    }

    JavaValue result(T_VOID);
    JavaCalls::call_static(&result,
                           k,
                           vmSymbols::startAgent_name(),
                           vmSymbols::void_method_signature(),
                           CHECK);
  }
}

void Management::get_optional_support(jmmOptionalSupport* support) {
  memcpy(support, &_optional_support, sizeof(jmmOptionalSupport));
}

InstanceKlass* Management::load_and_initialize_klass(Symbol* sh, TRAPS) {
  Klass* k = SystemDictionary::resolve_or_fail(sh, true, CHECK_NULL);
  return initialize_klass(k, THREAD);
}

InstanceKlass* Management::load_and_initialize_klass_or_null(Symbol* sh, TRAPS) {
  Klass* k = SystemDictionary::resolve_or_null(sh, CHECK_NULL);
  if (k == NULL) {
     return NULL;
  }
  return initialize_klass(k, THREAD);
}

InstanceKlass* Management::initialize_klass(Klass* k, TRAPS) {
  InstanceKlass* ik = InstanceKlass::cast(k);
  if (ik->should_be_initialized()) {
    ik->initialize(CHECK_NULL);
  }
  // If these classes change to not be owned by the boot loader, they need
  // to be walked to keep their class loader alive in oops_do.
  assert(ik->class_loader() == NULL, "need to follow in oops_do");
  return ik;
}

void Management::record_vm_startup_time(jlong begin, jlong duration) {
  // if the performance counter is not initialized,
  // then vm initialization failed; simply return.
  if (_begin_vm_creation_time == NULL) return;

  _begin_vm_creation_time->set_value(begin);
  _end_vm_creation_time->set_value(begin + duration);
  PerfMemory::set_accessible(true);
}

jlong Management::timestamp() {
  TimeStamp t;
  t.update();
  return t.ticks() - _stamp.ticks();
}

InstanceKlass* Management::java_lang_management_ThreadInfo_klass(TRAPS) {
  if (_threadInfo_klass == NULL) {
    _threadInfo_klass = load_and_initialize_klass(vmSymbols::java_lang_management_ThreadInfo(), CHECK_NULL);
  }
  return _threadInfo_klass;
}

InstanceKlass* Management::java_lang_management_MemoryUsage_klass(TRAPS) {
  if (_memoryUsage_klass == NULL) {
    _memoryUsage_klass = load_and_initialize_klass(vmSymbols::java_lang_management_MemoryUsage(), CHECK_NULL);
  }
  return _memoryUsage_klass;
}

InstanceKlass* Management::java_lang_management_MemoryPoolMXBean_klass(TRAPS) {
  if (_memoryPoolMXBean_klass == NULL) {
    _memoryPoolMXBean_klass = load_and_initialize_klass(vmSymbols::java_lang_management_MemoryPoolMXBean(), CHECK_NULL);
  }
  return _memoryPoolMXBean_klass;
}

InstanceKlass* Management::java_lang_management_MemoryManagerMXBean_klass(TRAPS) {
  if (_memoryManagerMXBean_klass == NULL) {
    _memoryManagerMXBean_klass = load_and_initialize_klass(vmSymbols::java_lang_management_MemoryManagerMXBean(), CHECK_NULL);
  }
  return _memoryManagerMXBean_klass;
}

InstanceKlass* Management::java_lang_management_GarbageCollectorMXBean_klass(TRAPS) {
  if (_garbageCollectorMXBean_klass == NULL) {
      _garbageCollectorMXBean_klass = load_and_initialize_klass(vmSymbols::java_lang_management_GarbageCollectorMXBean(), CHECK_NULL);
  }
  return _garbageCollectorMXBean_klass;
}

InstanceKlass* Management::sun_management_Sensor_klass(TRAPS) {
  if (_sensor_klass == NULL) {
    _sensor_klass = load_and_initialize_klass(vmSymbols::sun_management_Sensor(), CHECK_NULL);
  }
  return _sensor_klass;
}

InstanceKlass* Management::sun_management_ManagementFactoryHelper_klass(TRAPS) {
  if (_managementFactoryHelper_klass == NULL) {
    _managementFactoryHelper_klass = load_and_initialize_klass(vmSymbols::sun_management_ManagementFactoryHelper(), CHECK_NULL);
  }
  return _managementFactoryHelper_klass;
}

InstanceKlass* Management::com_sun_management_internal_GarbageCollectorExtImpl_klass(TRAPS) {
  if (_garbageCollectorExtImpl_klass == NULL) {
    _garbageCollectorExtImpl_klass =
                load_and_initialize_klass_or_null(vmSymbols::com_sun_management_internal_GarbageCollectorExtImpl(), CHECK_NULL);
  }
  return _garbageCollectorExtImpl_klass;
}

InstanceKlass* Management::com_sun_management_GcInfo_klass(TRAPS) {
  if (_gcInfo_klass == NULL) {
    _gcInfo_klass = load_and_initialize_klass(vmSymbols::com_sun_management_GcInfo(), CHECK_NULL);
  }
  return _gcInfo_klass;
}

InstanceKlass* Management::com_sun_management_internal_DiagnosticCommandImpl_klass(TRAPS) {
  if (_diagnosticCommandImpl_klass == NULL) {
    _diagnosticCommandImpl_klass = load_and_initialize_klass(vmSymbols::com_sun_management_internal_DiagnosticCommandImpl(), CHECK_NULL);
  }
  return _diagnosticCommandImpl_klass;
}

static void initialize_ThreadInfo_constructor_arguments(JavaCallArguments* args, ThreadSnapshot* snapshot, TRAPS) {
  Handle snapshot_thread(THREAD, snapshot->threadObj());

  jlong contended_time;
  jlong waited_time;
  if (ThreadService::is_thread_monitoring_contention()) {
    contended_time = Management::ticks_to_ms(snapshot->contended_enter_ticks());
    waited_time = Management::ticks_to_ms(snapshot->monitor_wait_ticks() + snapshot->sleep_ticks());
  } else {
    // set them to -1 if thread contention monitoring is disabled.
    contended_time = max_julong;
    waited_time = max_julong;
  }

  int thread_status = static_cast<int>(snapshot->thread_status());
  assert((thread_status & JMM_THREAD_STATE_FLAG_MASK) == 0, "Flags already set in thread_status in Thread object");
  if (snapshot->is_suspended()) {
    thread_status |= JMM_THREAD_STATE_FLAG_SUSPENDED;
  }
  if (snapshot->is_in_native()) {
    thread_status |= JMM_THREAD_STATE_FLAG_NATIVE;
  }

  ThreadStackTrace* st = snapshot->get_stack_trace();
  Handle stacktrace_h;
  if (st != NULL) {
    stacktrace_h = st->allocate_fill_stack_trace_element_array(CHECK);
  } else {
    stacktrace_h = Handle();
  }

  args->push_oop(snapshot_thread);
  args->push_int(thread_status);
  args->push_oop(Handle(THREAD, snapshot->blocker_object()));
  args->push_oop(Handle(THREAD, snapshot->blocker_object_owner()));
  args->push_long(snapshot->contended_enter_count());
  args->push_long(contended_time);
  args->push_long(snapshot->monitor_wait_count() + snapshot->sleep_count());
  args->push_long(waited_time);
  args->push_oop(stacktrace_h);
}

// Helper function to construct a ThreadInfo object
instanceOop Management::create_thread_info_instance(ThreadSnapshot* snapshot, TRAPS) {
  InstanceKlass* ik = Management::java_lang_management_ThreadInfo_klass(CHECK_NULL);
  JavaCallArguments args(14);

  // initialize the arguments for the ThreadInfo constructor
  initialize_ThreadInfo_constructor_arguments(&args, snapshot, CHECK_NULL);

  // Call ThreadInfo constructor with no locked monitors and synchronizers
  Handle element = JavaCalls::construct_new_instance(
                          ik,
                          vmSymbols::java_lang_management_ThreadInfo_constructor_signature(),
                          &args,
                          CHECK_NULL);
  return (instanceOop) element();
}

instanceOop Management::create_thread_info_instance(ThreadSnapshot* snapshot,
                                                    objArrayHandle monitors_array,
                                                    typeArrayHandle depths_array,
                                                    objArrayHandle synchronizers_array,
                                                    TRAPS) {
  InstanceKlass* ik = Management::java_lang_management_ThreadInfo_klass(CHECK_NULL);
  JavaCallArguments args(17);

  // initialize the arguments for the ThreadInfo constructor
  initialize_ThreadInfo_constructor_arguments(&args, snapshot, CHECK_NULL);

  // push the locked monitors and synchronizers in the arguments
  args.push_oop(monitors_array);
  args.push_oop(depths_array);
  args.push_oop(synchronizers_array);

  // Call ThreadInfo constructor with locked monitors and synchronizers
  Handle element = JavaCalls::construct_new_instance(
                          ik,
                          vmSymbols::java_lang_management_ThreadInfo_with_locks_constructor_signature(),
                          &args,
                          CHECK_NULL);
  return (instanceOop) element();
}


static GCMemoryManager* get_gc_memory_manager_from_jobject(jobject mgr, TRAPS) {
  if (mgr == NULL) {
    THROW_(vmSymbols::java_lang_NullPointerException(), NULL);
  }
  oop mgr_obj = JNIHandles::resolve(mgr);
  instanceHandle h(THREAD, (instanceOop) mgr_obj);

  InstanceKlass* k = Management::java_lang_management_GarbageCollectorMXBean_klass(CHECK_NULL);
  if (!h->is_a(k)) {
    THROW_MSG_(vmSymbols::java_lang_IllegalArgumentException(),
               "the object is not an instance of java.lang.management.GarbageCollectorMXBean class",
               NULL);
  }

  MemoryManager* gc = MemoryService::get_memory_manager(h);
  if (gc == NULL || !gc->is_gc_memory_manager()) {
    THROW_MSG_(vmSymbols::java_lang_IllegalArgumentException(),
               "Invalid GC memory manager",
               NULL);
  }
  return (GCMemoryManager*) gc;
}

static MemoryPool* get_memory_pool_from_jobject(jobject obj, TRAPS) {
  if (obj == NULL) {
    THROW_(vmSymbols::java_lang_NullPointerException(), NULL);
  }

  oop pool_obj = JNIHandles::resolve(obj);
  assert(pool_obj->is_instance(), "Should be an instanceOop");
  instanceHandle ph(THREAD, (instanceOop) pool_obj);

  return MemoryService::get_memory_pool(ph);
}

#endif // INCLUDE_MANAGEMENT

static void validate_thread_id_array(typeArrayHandle ids_ah, TRAPS) {
  int num_threads = ids_ah->length();

  // Validate input thread IDs
  int i = 0;
  for (i = 0; i < num_threads; i++) {
    jlong tid = ids_ah->long_at(i);
    if (tid <= 0) {
      // throw exception if invalid thread id.
      THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(),
                "Invalid thread ID entry");
    }
  }
}

#if INCLUDE_MANAGEMENT

static void validate_thread_info_array(objArrayHandle infoArray_h, TRAPS) {
  // check if the element of infoArray is of type ThreadInfo class
  Klass* threadinfo_klass = Management::java_lang_management_ThreadInfo_klass(CHECK);
  Klass* element_klass = ObjArrayKlass::cast(infoArray_h->klass())->element_klass();
  if (element_klass != threadinfo_klass) {
    THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(),
              "infoArray element type is not ThreadInfo class");
  }
}


static MemoryManager* get_memory_manager_from_jobject(jobject obj, TRAPS) {
  if (obj == NULL) {
    THROW_(vmSymbols::java_lang_NullPointerException(), NULL);
  }

  oop mgr_obj = JNIHandles::resolve(obj);
  assert(mgr_obj->is_instance(), "Should be an instanceOop");
  instanceHandle mh(THREAD, (instanceOop) mgr_obj);

  return MemoryService::get_memory_manager(mh);
}

// Returns a version string and sets major and minor version if
// the input parameters are non-null.
JVM_LEAF(jint, jmm_GetVersion(JNIEnv *env))
  return JMM_VERSION;
JVM_END

// Gets the list of VM monitoring and management optional supports
// Returns 0 if succeeded; otherwise returns non-zero.
JVM_LEAF(jint, jmm_GetOptionalSupport(JNIEnv *env, jmmOptionalSupport* support))
  if (support == NULL) {
    return -1;
  }
  Management::get_optional_support(support);
  return 0;
JVM_END

// Returns an array of java/lang/management/MemoryPoolMXBean object
// one for each memory pool if obj == null; otherwise returns
// an array of memory pools for a given memory manager if
// it is a valid memory manager.
JVM_ENTRY(jobjectArray, jmm_GetMemoryPools(JNIEnv* env, jobject obj))
  ResourceMark rm(THREAD);

  int num_memory_pools;
  MemoryManager* mgr = NULL;
  if (obj == NULL) {
    num_memory_pools = MemoryService::num_memory_pools();
  } else {
    mgr = get_memory_manager_from_jobject(obj, CHECK_NULL);
    if (mgr == NULL) {
      return NULL;
    }
    num_memory_pools = mgr->num_memory_pools();
  }

  // Allocate the resulting MemoryPoolMXBean[] object
  InstanceKlass* ik = Management::java_lang_management_MemoryPoolMXBean_klass(CHECK_NULL);
  objArrayOop r = oopFactory::new_objArray(ik, num_memory_pools, CHECK_NULL);
  objArrayHandle poolArray(THREAD, r);

  if (mgr == NULL) {
    // Get all memory pools
    for (int i = 0; i < num_memory_pools; i++) {
      MemoryPool* pool = MemoryService::get_memory_pool(i);
      instanceOop p = pool->get_memory_pool_instance(CHECK_NULL);
      instanceHandle ph(THREAD, p);
      poolArray->obj_at_put(i, ph());
    }
  } else {
    // Get memory pools managed by a given memory manager
    for (int i = 0; i < num_memory_pools; i++) {
      MemoryPool* pool = mgr->get_memory_pool(i);
      instanceOop p = pool->get_memory_pool_instance(CHECK_NULL);
      instanceHandle ph(THREAD, p);
      poolArray->obj_at_put(i, ph());
    }
  }
  return (jobjectArray) JNIHandles::make_local(THREAD, poolArray());
JVM_END

// Returns an array of java/lang/management/MemoryManagerMXBean object
// one for each memory manager if obj == null; otherwise returns
// an array of memory managers for a given memory pool if
// it is a valid memory pool.
JVM_ENTRY(jobjectArray, jmm_GetMemoryManagers(JNIEnv* env, jobject obj))
  ResourceMark rm(THREAD);

  int num_mgrs;
  MemoryPool* pool = NULL;
  if (obj == NULL) {
    num_mgrs = MemoryService::num_memory_managers();
  } else {
    pool = get_memory_pool_from_jobject(obj, CHECK_NULL);
    if (pool == NULL) {
      return NULL;
    }
    num_mgrs = pool->num_memory_managers();
  }

  // Allocate the resulting MemoryManagerMXBean[] object
  InstanceKlass* ik = Management::java_lang_management_MemoryManagerMXBean_klass(CHECK_NULL);
  objArrayOop r = oopFactory::new_objArray(ik, num_mgrs, CHECK_NULL);
  objArrayHandle mgrArray(THREAD, r);

  if (pool == NULL) {
    // Get all memory managers
    for (int i = 0; i < num_mgrs; i++) {
      MemoryManager* mgr = MemoryService::get_memory_manager(i);
      instanceOop p = mgr->get_memory_manager_instance(CHECK_NULL);
      instanceHandle ph(THREAD, p);
      mgrArray->obj_at_put(i, ph());
    }
  } else {
    // Get memory managers for a given memory pool
    for (int i = 0; i < num_mgrs; i++) {
      MemoryManager* mgr = pool->get_memory_manager(i);
      instanceOop p = mgr->get_memory_manager_instance(CHECK_NULL);
      instanceHandle ph(THREAD, p);
      mgrArray->obj_at_put(i, ph());
    }
  }
  return (jobjectArray) JNIHandles::make_local(THREAD, mgrArray());
JVM_END


// Returns a java/lang/management/MemoryUsage object containing the memory usage
// of a given memory pool.
JVM_ENTRY(jobject, jmm_GetMemoryPoolUsage(JNIEnv* env, jobject obj))
  ResourceMark rm(THREAD);

  MemoryPool* pool = get_memory_pool_from_jobject(obj, CHECK_NULL);
  if (pool != NULL) {
    MemoryUsage usage = pool->get_memory_usage();
    Handle h = MemoryService::create_MemoryUsage_obj(usage, CHECK_NULL);
    return JNIHandles::make_local(THREAD, h());
  } else {
    return NULL;
  }
JVM_END

// Returns a java/lang/management/MemoryUsage object containing the memory usage
// of a given memory pool.
JVM_ENTRY(jobject, jmm_GetPeakMemoryPoolUsage(JNIEnv* env, jobject obj))
  ResourceMark rm(THREAD);

  MemoryPool* pool = get_memory_pool_from_jobject(obj, CHECK_NULL);
  if (pool != NULL) {
    MemoryUsage usage = pool->get_peak_memory_usage();
    Handle h = MemoryService::create_MemoryUsage_obj(usage, CHECK_NULL);
    return JNIHandles::make_local(THREAD, h());
  } else {
    return NULL;
  }
JVM_END

// Returns a java/lang/management/MemoryUsage object containing the memory usage
// of a given memory pool after most recent GC.
JVM_ENTRY(jobject, jmm_GetPoolCollectionUsage(JNIEnv* env, jobject obj))
  ResourceMark rm(THREAD);

  MemoryPool* pool = get_memory_pool_from_jobject(obj, CHECK_NULL);
  if (pool != NULL && pool->is_collected_pool()) {
    MemoryUsage usage = pool->get_last_collection_usage();
    Handle h = MemoryService::create_MemoryUsage_obj(usage, CHECK_NULL);
    return JNIHandles::make_local(THREAD, h());
  } else {
    return NULL;
  }
JVM_END

// Sets the memory pool sensor for a threshold type
JVM_ENTRY(void, jmm_SetPoolSensor(JNIEnv* env, jobject obj, jmmThresholdType type, jobject sensorObj))
  if (obj == NULL || sensorObj == NULL) {
    THROW(vmSymbols::java_lang_NullPointerException());
  }

  InstanceKlass* sensor_klass = Management::sun_management_Sensor_klass(CHECK);
  oop s = JNIHandles::resolve(sensorObj);
  assert(s->is_instance(), "Sensor should be an instanceOop");
  instanceHandle sensor_h(THREAD, (instanceOop) s);
  if (!sensor_h->is_a(sensor_klass)) {
    THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(),
              "Sensor is not an instance of sun.management.Sensor class");
  }

  MemoryPool* mpool = get_memory_pool_from_jobject(obj, CHECK);
  assert(mpool != NULL, "MemoryPool should exist");

  switch (type) {
    case JMM_USAGE_THRESHOLD_HIGH:
    case JMM_USAGE_THRESHOLD_LOW:
      // have only one sensor for threshold high and low
      mpool->set_usage_sensor_obj(sensor_h);
      break;
    case JMM_COLLECTION_USAGE_THRESHOLD_HIGH:
    case JMM_COLLECTION_USAGE_THRESHOLD_LOW:
      // have only one sensor for threshold high and low
      mpool->set_gc_usage_sensor_obj(sensor_h);
      break;
    default:
      assert(false, "Unrecognized type");
  }

JVM_END


// Sets the threshold of a given memory pool.
// Returns the previous threshold.
//
// Input parameters:
//   pool      - the MemoryPoolMXBean object
//   type      - threshold type
//   threshold - the new threshold (must not be negative)
//
JVM_ENTRY(jlong, jmm_SetPoolThreshold(JNIEnv* env, jobject obj, jmmThresholdType type, jlong threshold))
  if (threshold < 0) {
    THROW_MSG_(vmSymbols::java_lang_IllegalArgumentException(),
               "Invalid threshold value",
               -1);
  }

  if ((size_t)threshold > max_uintx) {
    stringStream st;
    st.print("Invalid valid threshold value. Threshold value (" JLONG_FORMAT ") > max value of size_t (" UINTX_FORMAT ")", threshold, max_uintx);
    THROW_MSG_(vmSymbols::java_lang_IllegalArgumentException(), st.as_string(), -1);
  }

  MemoryPool* pool = get_memory_pool_from_jobject(obj, CHECK_(0L));
  assert(pool != NULL, "MemoryPool should exist");

  jlong prev = 0;
  switch (type) {
    case JMM_USAGE_THRESHOLD_HIGH:
      if (!pool->usage_threshold()->is_high_threshold_supported()) {
        return -1;
      }
      prev = pool->usage_threshold()->set_high_threshold((size_t) threshold);
      break;

    case JMM_USAGE_THRESHOLD_LOW:
      if (!pool->usage_threshold()->is_low_threshold_supported()) {
        return -1;
      }
      prev = pool->usage_threshold()->set_low_threshold((size_t) threshold);
      break;

    case JMM_COLLECTION_USAGE_THRESHOLD_HIGH:
      if (!pool->gc_usage_threshold()->is_high_threshold_supported()) {
        return -1;
      }
      // return and the new threshold is effective for the next GC
      return pool->gc_usage_threshold()->set_high_threshold((size_t) threshold);

    case JMM_COLLECTION_USAGE_THRESHOLD_LOW:
      if (!pool->gc_usage_threshold()->is_low_threshold_supported()) {
        return -1;
      }
      // return and the new threshold is effective for the next GC
      return pool->gc_usage_threshold()->set_low_threshold((size_t) threshold);

    default:
      assert(false, "Unrecognized type");
      return -1;
  }

  // When the threshold is changed, reevaluate if the low memory
  // detection is enabled.
  if (prev != threshold) {
    LowMemoryDetector::recompute_enabled_for_collected_pools();
    LowMemoryDetector::detect_low_memory(pool);
  }
  return prev;
JVM_END

// Returns a java/lang/management/MemoryUsage object representing
// the memory usage for the heap or non-heap memory.
JVM_ENTRY(jobject, jmm_GetMemoryUsage(JNIEnv* env, jboolean heap))
  ResourceMark rm(THREAD);

  MemoryUsage usage;

  if (heap) {
    usage = Universe::heap()->memory_usage();
  } else {
    // Calculate the memory usage by summing up the pools.
    size_t total_init = 0;
    size_t total_used = 0;
    size_t total_committed = 0;
    size_t total_max = 0;
    bool   has_undefined_init_size = false;
    bool   has_undefined_max_size = false;

    for (int i = 0; i < MemoryService::num_memory_pools(); i++) {
      MemoryPool* pool = MemoryService::get_memory_pool(i);
      if (pool->is_non_heap()) {
        MemoryUsage u = pool->get_memory_usage();
        total_used += u.used();
        total_committed += u.committed();

        if (u.init_size() == MemoryUsage::undefined_size()) {
          has_undefined_init_size = true;
        }
        if (!has_undefined_init_size) {
          total_init += u.init_size();
        }

        if (u.max_size() == MemoryUsage::undefined_size()) {
          has_undefined_max_size = true;
        }
        if (!has_undefined_max_size) {
          total_max += u.max_size();
        }
      }
    }

    // if any one of the memory pool has undefined init_size or max_size,
    // set it to MemoryUsage::undefined_size()
    if (has_undefined_init_size) {
      total_init = MemoryUsage::undefined_size();
    }
    if (has_undefined_max_size) {
      total_max = MemoryUsage::undefined_size();
    }

    usage = MemoryUsage(total_init, total_used, total_committed, total_max);
  }

  Handle obj = MemoryService::create_MemoryUsage_obj(usage, CHECK_NULL);
  return JNIHandles::make_local(THREAD, obj());
JVM_END

// Returns the boolean value of a given attribute.
JVM_LEAF(jboolean, jmm_GetBoolAttribute(JNIEnv *env, jmmBoolAttribute att))
  switch (att) {
  case JMM_VERBOSE_GC:
    return MemoryService::get_verbose();
  case JMM_VERBOSE_CLASS:
    return ClassLoadingService::get_verbose();
  case JMM_THREAD_CONTENTION_MONITORING:
    return ThreadService::is_thread_monitoring_contention();
  case JMM_THREAD_CPU_TIME:
    return ThreadService::is_thread_cpu_time_enabled();
  case JMM_THREAD_ALLOCATED_MEMORY:
    return ThreadService::is_thread_allocated_memory_enabled();
  default:
    assert(0, "Unrecognized attribute");
    return false;
  }
JVM_END

// Sets the given boolean attribute and returns the previous value.
JVM_ENTRY(jboolean, jmm_SetBoolAttribute(JNIEnv *env, jmmBoolAttribute att, jboolean flag))
  switch (att) {
  case JMM_VERBOSE_GC:
    return MemoryService::set_verbose(flag != 0);
  case JMM_VERBOSE_CLASS:
    return ClassLoadingService::set_verbose(flag != 0);
  case JMM_THREAD_CONTENTION_MONITORING:
    return ThreadService::set_thread_monitoring_contention(flag != 0);
  case JMM_THREAD_CPU_TIME:
    return ThreadService::set_thread_cpu_time_enabled(flag != 0);
  case JMM_THREAD_ALLOCATED_MEMORY:
    return ThreadService::set_thread_allocated_memory_enabled(flag != 0);
  default:
    assert(0, "Unrecognized attribute");
    return false;
  }
JVM_END


static jlong get_gc_attribute(GCMemoryManager* mgr, jmmLongAttribute att) {
  switch (att) {
  case JMM_GC_TIME_MS:
    return mgr->gc_time_ms();

  case JMM_GC_COUNT:
    return mgr->gc_count();

  case JMM_GC_EXT_ATTRIBUTE_INFO_SIZE:
    // current implementation only has 1 ext attribute
    return 1;

  default:
    assert(0, "Unrecognized GC attribute");
    return -1;
  }
}

class VmThreadCountClosure: public ThreadClosure {
 private:
  int _count;
 public:
  VmThreadCountClosure() : _count(0) {};
  void do_thread(Thread* thread);
  int count() { return _count; }
};

void VmThreadCountClosure::do_thread(Thread* thread) {
  // exclude externally visible JavaThreads
  if (thread->is_Java_thread() && !thread->is_hidden_from_external_view()) {
    return;
  }

  _count++;
}

static jint get_vm_thread_count() {
  VmThreadCountClosure vmtcc;
  {
    MutexLocker ml(Threads_lock);
    Threads::threads_do(&vmtcc);
  }

  return vmtcc.count();
}

static jint get_num_flags() {
  // last flag entry is always NULL, so subtract 1
  int nFlags = (int) JVMFlag::numFlags - 1;
  int count = 0;
  for (int i = 0; i < nFlags; i++) {
    JVMFlag* flag = &JVMFlag::flags[i];
    // Exclude the locked (diagnostic, experimental) flags
    if (flag->is_unlocked() || flag->is_unlocker()) {
      count++;
    }
  }
  return count;
}

static jlong get_long_attribute(jmmLongAttribute att) {
  switch (att) {
  case JMM_CLASS_LOADED_COUNT:
    return ClassLoadingService::loaded_class_count();

  case JMM_CLASS_UNLOADED_COUNT:
    return ClassLoadingService::unloaded_class_count();

  case JMM_THREAD_TOTAL_COUNT:
    return ThreadService::get_total_thread_count();

  case JMM_THREAD_LIVE_COUNT:
    return ThreadService::get_live_thread_count();

  case JMM_THREAD_PEAK_COUNT:
    return ThreadService::get_peak_thread_count();

  case JMM_THREAD_DAEMON_COUNT:
    return ThreadService::get_daemon_thread_count();

  case JMM_JVM_INIT_DONE_TIME_MS:
    return Management::vm_init_done_time();

  case JMM_JVM_UPTIME_MS:
    return Management::ticks_to_ms(os::elapsed_counter());

  case JMM_COMPILE_TOTAL_TIME_MS:
    return Management::ticks_to_ms(CompileBroker::total_compilation_ticks());

  case JMM_OS_PROCESS_ID:
    return os::current_process_id();

  // Hotspot-specific counters
  case JMM_CLASS_LOADED_BYTES:
    return ClassLoadingService::loaded_class_bytes();

  case JMM_CLASS_UNLOADED_BYTES:
    return ClassLoadingService::unloaded_class_bytes();

  case JMM_SHARED_CLASS_LOADED_COUNT:
    return ClassLoadingService::loaded_shared_class_count();

  case JMM_SHARED_CLASS_UNLOADED_COUNT:
    return ClassLoadingService::unloaded_shared_class_count();


  case JMM_SHARED_CLASS_LOADED_BYTES:
    return ClassLoadingService::loaded_shared_class_bytes();

  case JMM_SHARED_CLASS_UNLOADED_BYTES:
    return ClassLoadingService::unloaded_shared_class_bytes();

  case JMM_TOTAL_CLASSLOAD_TIME_MS:
    return ClassLoader::classloader_time_ms();

  case JMM_VM_GLOBAL_COUNT:
    return get_num_flags();

  case JMM_SAFEPOINT_COUNT:
    return RuntimeService::safepoint_count();

  case JMM_TOTAL_SAFEPOINTSYNC_TIME_MS:
    return RuntimeService::safepoint_sync_time_ms();

  case JMM_TOTAL_STOPPED_TIME_MS:
    return RuntimeService::safepoint_time_ms();

  case JMM_TOTAL_APP_TIME_MS:
    return RuntimeService::application_time_ms();

  case JMM_VM_THREAD_COUNT:
    return get_vm_thread_count();

  case JMM_CLASS_INIT_TOTAL_COUNT:
    return ClassLoader::class_init_count();

  case JMM_CLASS_INIT_TOTAL_TIME_MS:
    return ClassLoader::class_init_time_ms();

  case JMM_CLASS_VERIFY_TOTAL_TIME_MS:
    return ClassLoader::class_verify_time_ms();

  case JMM_METHOD_DATA_SIZE_BYTES:
    return ClassLoadingService::class_method_data_size();

  case JMM_OS_MEM_TOTAL_PHYSICAL_BYTES:
    return os::physical_memory();

  default:
    return -1;
  }
}


// Returns the long value of a given attribute.
JVM_ENTRY(jlong, jmm_GetLongAttribute(JNIEnv *env, jobject obj, jmmLongAttribute att))
  if (obj == NULL) {
    return get_long_attribute(att);
  } else {
    GCMemoryManager* mgr = get_gc_memory_manager_from_jobject(obj, CHECK_(0L));
    if (mgr != NULL) {
      return get_gc_attribute(mgr, att);
    }
  }
  return -1;
JVM_END

// Gets the value of all attributes specified in the given array
// and sets the value in the result array.
// Returns the number of attributes found.
JVM_ENTRY(jint, jmm_GetLongAttributes(JNIEnv *env,
                                      jobject obj,
                                      jmmLongAttribute* atts,
                                      jint count,
                                      jlong* result))

  int num_atts = 0;
  if (obj == NULL) {
    for (int i = 0; i < count; i++) {
      result[i] = get_long_attribute(atts[i]);
      if (result[i] != -1) {
        num_atts++;
      }
    }
  } else {
    GCMemoryManager* mgr = get_gc_memory_manager_from_jobject(obj, CHECK_0);
    for (int i = 0; i < count; i++) {
      result[i] = get_gc_attribute(mgr, atts[i]);
      if (result[i] != -1) {
        num_atts++;
      }
    }
  }
  return num_atts;
JVM_END

// Helper function to do thread dump for a specific list of threads
static void do_thread_dump(ThreadDumpResult* dump_result,
                           typeArrayHandle ids_ah,  // array of thread ID (long[])
                           int num_threads,
                           int max_depth,
                           bool with_locked_monitors,
                           bool with_locked_synchronizers,
                           TRAPS) {
  // no need to actually perform thread dump if no TIDs are specified
  if (num_threads == 0) return;

  // First get an array of threadObj handles.
  // A JavaThread may terminate before we get the stack trace.
  GrowableArray<instanceHandle>* thread_handle_array = new GrowableArray<instanceHandle>(num_threads);

  {
    // Need this ThreadsListHandle for converting Java thread IDs into
    // threadObj handles; dump_result->set_t_list() is called in the
    // VM op below so we can't use it yet.
    ThreadsListHandle tlh;
    for (int i = 0; i < num_threads; i++) {
      jlong tid = ids_ah->long_at(i);
      JavaThread* jt = tlh.list()->find_JavaThread_from_java_tid(tid);
      oop thread_obj = (jt != NULL ? jt->threadObj() : (oop)NULL);
      instanceHandle threadObj_h(THREAD, (instanceOop) thread_obj);
      thread_handle_array->append(threadObj_h);
    }
  }

  // Obtain thread dumps and thread snapshot information
  VM_ThreadDump op(dump_result,
                   thread_handle_array,
                   num_threads,
                   max_depth, /* stack depth */
                   with_locked_monitors,
                   with_locked_synchronizers);
  VMThread::execute(&op);
}

// Gets an array of ThreadInfo objects. Each element is the ThreadInfo
// for the thread ID specified in the corresponding entry in
// the given array of thread IDs; or NULL if the thread does not exist
// or has terminated.
//
// Input parameters:
//   ids       - array of thread IDs
//   maxDepth  - the maximum depth of stack traces to be dumped:
//               maxDepth == -1 requests to dump entire stack trace.
//               maxDepth == 0  requests no stack trace.
//   infoArray - array of ThreadInfo objects
//
// QQQ - Why does this method return a value instead of void?
JVM_ENTRY(jint, jmm_GetThreadInfo(JNIEnv *env, jlongArray ids, jint maxDepth, jobjectArray infoArray))
  // Check if threads is null
  if (ids == NULL || infoArray == NULL) {
    THROW_(vmSymbols::java_lang_NullPointerException(), -1);
  }

  if (maxDepth < -1) {
    THROW_MSG_(vmSymbols::java_lang_IllegalArgumentException(),
               "Invalid maxDepth", -1);
  }

  ResourceMark rm(THREAD);
  typeArrayOop ta = typeArrayOop(JNIHandles::resolve_non_null(ids));
  typeArrayHandle ids_ah(THREAD, ta);

  oop infoArray_obj = JNIHandles::resolve_non_null(infoArray);
  objArrayOop oa = objArrayOop(infoArray_obj);
  objArrayHandle infoArray_h(THREAD, oa);

  // validate the thread id array
  validate_thread_id_array(ids_ah, CHECK_0);

  // validate the ThreadInfo[] parameters
  validate_thread_info_array(infoArray_h, CHECK_0);

  // infoArray must be of the same length as the given array of thread IDs
  int num_threads = ids_ah->length();
  if (num_threads != infoArray_h->length()) {
    THROW_MSG_(vmSymbols::java_lang_IllegalArgumentException(),
               "The length of the given ThreadInfo array does not match the length of the given array of thread IDs", -1);
  }

  // Must use ThreadDumpResult to store the ThreadSnapshot.
  // GC may occur after the thread snapshots are taken but before
  // this function returns. The threadObj and other oops kept
  // in the ThreadSnapshot are marked and adjusted during GC.
  ThreadDumpResult dump_result(num_threads);

  if (maxDepth == 0) {
    // No stack trace to dump so we do not need to stop the world.
    // Since we never do the VM op here we must set the threads list.
    dump_result.set_t_list();
    for (int i = 0; i < num_threads; i++) {
      jlong tid = ids_ah->long_at(i);
      JavaThread* jt = dump_result.t_list()->find_JavaThread_from_java_tid(tid);
      if (jt == NULL) {
        // if the thread does not exist or now it is terminated,
        // create dummy snapshot
        dump_result.add_thread_snapshot();
      } else {
        dump_result.add_thread_snapshot(jt);
      }
    }
  } else {
    // obtain thread dump with the specific list of threads with stack trace
    do_thread_dump(&dump_result,
                   ids_ah,
                   num_threads,
                   maxDepth,
                   false, /* no locked monitor */
                   false, /* no locked synchronizers */
                   CHECK_0);
  }

  int num_snapshots = dump_result.num_snapshots();
  assert(num_snapshots == num_threads, "Must match the number of thread snapshots");
  assert(num_snapshots == 0 || dump_result.t_list_has_been_set(), "ThreadsList must have been set if we have a snapshot");
  int index = 0;
  for (ThreadSnapshot* ts = dump_result.snapshots(); ts != NULL; index++, ts = ts->next()) {
    // For each thread, create an java/lang/management/ThreadInfo object
    // and fill with the thread information

    if (ts->threadObj() == NULL) {
     // if the thread does not exist or now it is terminated, set threadinfo to NULL
      infoArray_h->obj_at_put(index, NULL);
      continue;
    }

    // Create java.lang.management.ThreadInfo object
    instanceOop info_obj = Management::create_thread_info_instance(ts, CHECK_0);
    infoArray_h->obj_at_put(index, info_obj);
  }
  return 0;
JVM_END

// Dump thread info for the specified threads.
// It returns an array of ThreadInfo objects. Each element is the ThreadInfo
// for the thread ID specified in the corresponding entry in
// the given array of thread IDs; or NULL if the thread does not exist
// or has terminated.
//
// Input parameter:
//    ids - array of thread IDs; NULL indicates all live threads
//    locked_monitors - if true, dump locked object monitors
//    locked_synchronizers - if true, dump locked JSR-166 synchronizers
//
JVM_ENTRY(jobjectArray, jmm_DumpThreads(JNIEnv *env, jlongArray thread_ids, jboolean locked_monitors,
                                        jboolean locked_synchronizers, jint maxDepth))
  ResourceMark rm(THREAD);

  typeArrayOop ta = typeArrayOop(JNIHandles::resolve(thread_ids));
  int num_threads = (ta != NULL ? ta->length() : 0);
  typeArrayHandle ids_ah(THREAD, ta);

  ThreadDumpResult dump_result(num_threads);  // can safepoint

  if (ids_ah() != NULL) {

    // validate the thread id array
    validate_thread_id_array(ids_ah, CHECK_NULL);

    // obtain thread dump of a specific list of threads
    do_thread_dump(&dump_result,
                   ids_ah,
                   num_threads,
                   maxDepth, /* stack depth */
                   (locked_monitors ? true : false),      /* with locked monitors */
                   (locked_synchronizers ? true : false), /* with locked synchronizers */
                   CHECK_NULL);
  } else {
    // obtain thread dump of all threads
    VM_ThreadDump op(&dump_result,
                     maxDepth, /* stack depth */
                     (locked_monitors ? true : false),     /* with locked monitors */
                     (locked_synchronizers ? true : false) /* with locked synchronizers */);
    VMThread::execute(&op);
  }

  int num_snapshots = dump_result.num_snapshots();
  assert(num_snapshots == 0 || dump_result.t_list_has_been_set(), "ThreadsList must have been set if we have a snapshot");

  // create the result ThreadInfo[] object
  InstanceKlass* ik = Management::java_lang_management_ThreadInfo_klass(CHECK_NULL);
  objArrayOop r = oopFactory::new_objArray(ik, num_snapshots, CHECK_NULL);
  objArrayHandle result_h(THREAD, r);

  int index = 0;
  for (ThreadSnapshot* ts = dump_result.snapshots(); ts != NULL; ts = ts->next(), index++) {
    if (ts->threadObj() == NULL) {
     // if the thread does not exist or now it is terminated, set threadinfo to NULL
      result_h->obj_at_put(index, NULL);
      continue;
    }

    ThreadStackTrace* stacktrace = ts->get_stack_trace();
    assert(stacktrace != NULL, "Must have a stack trace dumped");

    // Create Object[] filled with locked monitors
    // Create int[] filled with the stack depth where a monitor was locked
    int num_frames = stacktrace->get_stack_depth();
    int num_locked_monitors = stacktrace->num_jni_locked_monitors();

    // Count the total number of locked monitors
    for (int i = 0; i < num_frames; i++) {
      StackFrameInfo* frame = stacktrace->stack_frame_at(i);
      num_locked_monitors += frame->num_locked_monitors();
    }

    objArrayHandle monitors_array;
    typeArrayHandle depths_array;
    objArrayHandle synchronizers_array;

    if (locked_monitors) {
      // Constructs Object[] and int[] to contain the object monitor and the stack depth
      // where the thread locked it
      objArrayOop array = oopFactory::new_objArray(vmClasses::Object_klass(), num_locked_monitors, CHECK_NULL);
      objArrayHandle mh(THREAD, array);
      monitors_array = mh;

      typeArrayOop tarray = oopFactory::new_typeArray(T_INT, num_locked_monitors, CHECK_NULL);
      typeArrayHandle dh(THREAD, tarray);
      depths_array = dh;

      int count = 0;
      int j = 0;
      for (int depth = 0; depth < num_frames; depth++) {
        StackFrameInfo* frame = stacktrace->stack_frame_at(depth);
        int len = frame->num_locked_monitors();
        GrowableArray<OopHandle>* locked_monitors = frame->locked_monitors();
        for (j = 0; j < len; j++) {
          oop monitor = locked_monitors->at(j).resolve();
          assert(monitor != NULL, "must be a Java object");
          monitors_array->obj_at_put(count, monitor);
          depths_array->int_at_put(count, depth);
          count++;
        }
      }

      GrowableArray<OopHandle>* jni_locked_monitors = stacktrace->jni_locked_monitors();
      for (j = 0; j < jni_locked_monitors->length(); j++) {
        oop object = jni_locked_monitors->at(j).resolve();
        assert(object != NULL, "must be a Java object");
        monitors_array->obj_at_put(count, object);
        // Monitor locked via JNI MonitorEnter call doesn't have stack depth info
        depths_array->int_at_put(count, -1);
        count++;
      }
      assert(count == num_locked_monitors, "number of locked monitors doesn't match");
    }

    if (locked_synchronizers) {
      // Create Object[] filled with locked JSR-166 synchronizers
      assert(ts->threadObj() != NULL, "Must be a valid JavaThread");
      ThreadConcurrentLocks* tcl = ts->get_concurrent_locks();
      GrowableArray<OopHandle>* locks = (tcl != NULL ? tcl->owned_locks() : NULL);
      int num_locked_synchronizers = (locks != NULL ? locks->length() : 0);

      objArrayOop array = oopFactory::new_objArray(vmClasses::Object_klass(), num_locked_synchronizers, CHECK_NULL);
      objArrayHandle sh(THREAD, array);
      synchronizers_array = sh;

      for (int k = 0; k < num_locked_synchronizers; k++) {
        synchronizers_array->obj_at_put(k, locks->at(k).resolve());
      }
    }

    // Create java.lang.management.ThreadInfo object
    instanceOop info_obj = Management::create_thread_info_instance(ts,
                                                                   monitors_array,
                                                                   depths_array,
                                                                   synchronizers_array,
                                                                   CHECK_NULL);
    result_h->obj_at_put(index, info_obj);
  }

  return (jobjectArray) JNIHandles::make_local(THREAD, result_h());
JVM_END

// Reset statistic.  Return true if the requested statistic is reset.
// Otherwise, return false.
//
// Input parameters:
//  obj  - specify which instance the statistic associated with to be reset
//         For PEAK_POOL_USAGE stat, obj is required to be a memory pool object.
//         For THREAD_CONTENTION_COUNT and TIME stat, obj is required to be a thread ID.
//  type - the type of statistic to be reset
//
JVM_ENTRY(jboolean, jmm_ResetStatistic(JNIEnv *env, jvalue obj, jmmStatisticType type))
  ResourceMark rm(THREAD);

  switch (type) {
    case JMM_STAT_PEAK_THREAD_COUNT:
      ThreadService::reset_peak_thread_count();
      return true;

    case JMM_STAT_THREAD_CONTENTION_COUNT:
    case JMM_STAT_THREAD_CONTENTION_TIME: {
      jlong tid = obj.j;
      if (tid < 0) {
        THROW_(vmSymbols::java_lang_IllegalArgumentException(), JNI_FALSE);
      }

      // Look for the JavaThread of this given tid
      JavaThreadIteratorWithHandle jtiwh;
      if (tid == 0) {
        // reset contention statistics for all threads if tid == 0
        for (; JavaThread *java_thread = jtiwh.next(); ) {
          if (type == JMM_STAT_THREAD_CONTENTION_COUNT) {
            ThreadService::reset_contention_count_stat(java_thread);
          } else {
            ThreadService::reset_contention_time_stat(java_thread);
          }
        }
      } else {
        // reset contention statistics for a given thread
        JavaThread* java_thread = jtiwh.list()->find_JavaThread_from_java_tid(tid);
        if (java_thread == NULL) {
          return false;
        }

        if (type == JMM_STAT_THREAD_CONTENTION_COUNT) {
          ThreadService::reset_contention_count_stat(java_thread);
        } else {
          ThreadService::reset_contention_time_stat(java_thread);
        }
      }
      return true;
      break;
    }
    case JMM_STAT_PEAK_POOL_USAGE: {
      jobject o = obj.l;
      if (o == NULL) {
        THROW_(vmSymbols::java_lang_NullPointerException(), JNI_FALSE);
      }

      oop pool_obj = JNIHandles::resolve(o);
      assert(pool_obj->is_instance(), "Should be an instanceOop");
      instanceHandle ph(THREAD, (instanceOop) pool_obj);

      MemoryPool* pool = MemoryService::get_memory_pool(ph);
      if (pool != NULL) {
        pool->reset_peak_memory_usage();
        return true;
      }
      break;
    }
    case JMM_STAT_GC_STAT: {
      jobject o = obj.l;
      if (o == NULL) {
        THROW_(vmSymbols::java_lang_NullPointerException(), JNI_FALSE);
      }

      GCMemoryManager* mgr = get_gc_memory_manager_from_jobject(o, CHECK_false);
      if (mgr != NULL) {
        mgr->reset_gc_stat();
        return true;
      }
      break;
    }
    default:
      assert(0, "Unknown Statistic Type");
  }
  return false;
JVM_END

// Returns the fast estimate of CPU time consumed by
// a given thread (in nanoseconds).
// If thread_id == 0, return CPU time for the current thread.
JVM_ENTRY(jlong, jmm_GetThreadCpuTime(JNIEnv *env, jlong thread_id))
  if (!os::is_thread_cpu_time_supported()) {
    return -1;
  }

  if (thread_id < 0) {
    THROW_MSG_(vmSymbols::java_lang_IllegalArgumentException(),
               "Invalid thread ID", -1);
  }

  JavaThread* java_thread = NULL;
  if (thread_id == 0) {
    // current thread
    return os::current_thread_cpu_time();
  } else {
    ThreadsListHandle tlh;
    java_thread = tlh.list()->find_JavaThread_from_java_tid(thread_id);
    if (java_thread != NULL) {
      return os::thread_cpu_time((Thread*) java_thread);
    }
  }
  return -1;
JVM_END

// Returns a String array of all VM global flag names
JVM_ENTRY(jobjectArray, jmm_GetVMGlobalNames(JNIEnv *env))
  // last flag entry is always NULL, so subtract 1
  int nFlags = (int) JVMFlag::numFlags - 1;
  // allocate a temp array
  objArrayOop r = oopFactory::new_objArray(vmClasses::String_klass(),
                                           nFlags, CHECK_NULL);
  objArrayHandle flags_ah(THREAD, r);
  int num_entries = 0;
  for (int i = 0; i < nFlags; i++) {
    JVMFlag* flag = &JVMFlag::flags[i];
    // Exclude notproduct and develop flags in product builds.
    if (flag->is_constant_in_binary()) {
      continue;
    }
    // Exclude the locked (experimental, diagnostic) flags
    if (flag->is_unlocked() || flag->is_unlocker()) {
      Handle s = java_lang_String::create_from_str(flag->name(), CHECK_NULL);
      flags_ah->obj_at_put(num_entries, s());
      num_entries++;
    }
  }

  if (num_entries < nFlags) {
    // Return array of right length
    objArrayOop res = oopFactory::new_objArray(vmClasses::String_klass(), num_entries, CHECK_NULL);
    for(int i = 0; i < num_entries; i++) {
      res->obj_at_put(i, flags_ah->obj_at(i));
    }
    return (jobjectArray)JNIHandles::make_local(THREAD, res);
  }

  return (jobjectArray)JNIHandles::make_local(THREAD, flags_ah());
JVM_END

// Utility function used by jmm_GetVMGlobals.  Returns false if flag type
// can't be determined, true otherwise.  If false is returned, then *global
// will be incomplete and invalid.
bool add_global_entry(Handle name, jmmVMGlobal *global, JVMFlag *flag, TRAPS) {
  Handle flag_name;
  if (name() == NULL) {
    flag_name = java_lang_String::create_from_str(flag->name(), CHECK_false);
  } else {
    flag_name = name;
  }
  global->name = (jstring)JNIHandles::make_local(THREAD, flag_name());

  if (flag->is_bool()) {
    global->value.z = flag->get_bool() ? JNI_TRUE : JNI_FALSE;
    global->type = JMM_VMGLOBAL_TYPE_JBOOLEAN;
  } else if (flag->is_int()) {
    global->value.j = (jlong)flag->get_int();
    global->type = JMM_VMGLOBAL_TYPE_JLONG;
  } else if (flag->is_uint()) {
    global->value.j = (jlong)flag->get_uint();
    global->type = JMM_VMGLOBAL_TYPE_JLONG;
  } else if (flag->is_intx()) {
    global->value.j = (jlong)flag->get_intx();
    global->type = JMM_VMGLOBAL_TYPE_JLONG;
  } else if (flag->is_uintx()) {
    global->value.j = (jlong)flag->get_uintx();
    global->type = JMM_VMGLOBAL_TYPE_JLONG;
  } else if (flag->is_uint64_t()) {
    global->value.j = (jlong)flag->get_uint64_t();
    global->type = JMM_VMGLOBAL_TYPE_JLONG;
  } else if (flag->is_double()) {
    global->value.d = (jdouble)flag->get_double();
    global->type = JMM_VMGLOBAL_TYPE_JDOUBLE;
  } else if (flag->is_size_t()) {
    global->value.j = (jlong)flag->get_size_t();
    global->type = JMM_VMGLOBAL_TYPE_JLONG;
  } else if (flag->is_ccstr()) {
    Handle str = java_lang_String::create_from_str(flag->get_ccstr(), CHECK_false);
    global->value.l = (jobject)JNIHandles::make_local(THREAD, str());
    global->type = JMM_VMGLOBAL_TYPE_JSTRING;
  } else {
    global->type = JMM_VMGLOBAL_TYPE_UNKNOWN;
    return false;
  }

  global->writeable = flag->is_writeable();
  global->external = flag->is_external();
  switch (flag->get_origin()) {
    case JVMFlagOrigin::DEFAULT:
      global->origin = JMM_VMGLOBAL_ORIGIN_DEFAULT;
      break;
    case JVMFlagOrigin::COMMAND_LINE:
      global->origin = JMM_VMGLOBAL_ORIGIN_COMMAND_LINE;
      break;
    case JVMFlagOrigin::ENVIRON_VAR:
      global->origin = JMM_VMGLOBAL_ORIGIN_ENVIRON_VAR;
      break;
    case JVMFlagOrigin::CONFIG_FILE:
      global->origin = JMM_VMGLOBAL_ORIGIN_CONFIG_FILE;
      break;
    case JVMFlagOrigin::MANAGEMENT:
      global->origin = JMM_VMGLOBAL_ORIGIN_MANAGEMENT;
      break;
    case JVMFlagOrigin::ERGONOMIC:
      global->origin = JMM_VMGLOBAL_ORIGIN_ERGONOMIC;
      break;
    case JVMFlagOrigin::ATTACH_ON_DEMAND:
      global->origin = JMM_VMGLOBAL_ORIGIN_ATTACH_ON_DEMAND;
      break;
    default:
      global->origin = JMM_VMGLOBAL_ORIGIN_OTHER;
  }

  return true;
}

// Fill globals array of count length with jmmVMGlobal entries
// specified by names. If names == NULL, fill globals array
// with all Flags. Return value is number of entries
// created in globals.
// If a JVMFlag with a given name in an array element does not
// exist, globals[i].name will be set to NULL.
JVM_ENTRY(jint, jmm_GetVMGlobals(JNIEnv *env,
                                 jobjectArray names,
                                 jmmVMGlobal *globals,
                                 jint count))


  if (globals == NULL) {
    THROW_(vmSymbols::java_lang_NullPointerException(), 0);
  }

  ResourceMark rm(THREAD);

  if (names != NULL) {
    // return the requested globals
    objArrayOop ta = objArrayOop(JNIHandles::resolve_non_null(names));
    objArrayHandle names_ah(THREAD, ta);
    // Make sure we have a String array
    Klass* element_klass = ObjArrayKlass::cast(names_ah->klass())->element_klass();
    if (element_klass != vmClasses::String_klass()) {
      THROW_MSG_(vmSymbols::java_lang_IllegalArgumentException(),
                 "Array element type is not String class", 0);
    }

    int names_length = names_ah->length();
    int num_entries = 0;
    for (int i = 0; i < names_length && i < count; i++) {
      oop s = names_ah->obj_at(i);
      if (s == NULL) {
        THROW_(vmSymbols::java_lang_NullPointerException(), 0);
      }

      Handle sh(THREAD, s);
      char* str = java_lang_String::as_utf8_string(s);
      JVMFlag* flag = JVMFlag::find_flag(str);
      if (flag != NULL &&
          add_global_entry(sh, &globals[i], flag, THREAD)) {
        num_entries++;
      } else {
        globals[i].name = NULL;
      }
    }
    return num_entries;
  } else {
    // return all globals if names == NULL

    // last flag entry is always NULL, so subtract 1
    int nFlags = (int) JVMFlag::numFlags - 1;
    Handle null_h;
    int num_entries = 0;
    for (int i = 0; i < nFlags && num_entries < count;  i++) {
      JVMFlag* flag = &JVMFlag::flags[i];
      // Exclude notproduct and develop flags in product builds.
      if (flag->is_constant_in_binary()) {
        continue;
      }
      // Exclude the locked (diagnostic, experimental) flags
      if ((flag->is_unlocked() || flag->is_unlocker()) &&
          add_global_entry(null_h, &globals[num_entries], flag, THREAD)) {
        num_entries++;
      }
    }
    return num_entries;
  }
JVM_END

JVM_ENTRY(void, jmm_SetVMGlobal(JNIEnv *env, jstring flag_name, jvalue new_value))
  ResourceMark rm(THREAD);

  oop fn = JNIHandles::resolve_external_guard(flag_name);
  if (fn == NULL) {
    THROW_MSG(vmSymbols::java_lang_NullPointerException(),
              "The flag name cannot be null.");
  }
  char* name = java_lang_String::as_utf8_string(fn);

  FormatBuffer<80> error_msg("%s", "");
  int succeed = WriteableFlags::set_flag(name, new_value, JVMFlagOrigin::MANAGEMENT, error_msg);

  if (succeed != JVMFlag::SUCCESS) {
    if (succeed == JVMFlag::MISSING_VALUE) {
      // missing value causes NPE to be thrown
      THROW(vmSymbols::java_lang_NullPointerException());
    } else {
      // all the other errors are reported as IAE with the appropriate error message
      THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(),
                error_msg.buffer());
    }
  }
  assert(succeed == JVMFlag::SUCCESS, "Setting flag should succeed");
JVM_END

class ThreadTimesClosure: public ThreadClosure {
 private:
  objArrayHandle _names_strings;
  char **_names_chars;
  typeArrayHandle _times;
  int _names_len;
  int _times_len;
  int _count;

 public:
  ThreadTimesClosure(objArrayHandle names, typeArrayHandle times);
  ~ThreadTimesClosure();
  virtual void do_thread(Thread* thread);
  void do_unlocked(TRAPS);
  int count() { return _count; }
};

ThreadTimesClosure::ThreadTimesClosure(objArrayHandle names,
                                       typeArrayHandle times) {
  assert(names() != NULL, "names was NULL");
  assert(times() != NULL, "times was NULL");
  _names_strings = names;
  _names_len = names->length();
  _names_chars = NEW_C_HEAP_ARRAY(char*, _names_len, mtInternal);
  _times = times;
  _times_len = times->length();
  _count = 0;
}

//
// Called with Threads_lock held
//
void ThreadTimesClosure::do_thread(Thread* thread) {
  assert(Threads_lock->owned_by_self(), "Must hold Threads_lock");
  assert(thread != NULL, "thread was NULL");

  // exclude externally visible JavaThreads
  if (thread->is_Java_thread() && !thread->is_hidden_from_external_view()) {
    return;
  }

  if (_count >= _names_len || _count >= _times_len) {
    // skip if the result array is not big enough
    return;
  }

  ResourceMark rm; // thread->name() uses ResourceArea

  assert(thread->name() != NULL, "All threads should have a name");
  _names_chars[_count] = os::strdup_check_oom(thread->name());
  _times->long_at_put(_count, os::is_thread_cpu_time_supported() ?
                        os::thread_cpu_time(thread) : -1);
  _count++;
}

// Called without Threads_lock, we can allocate String objects.
void ThreadTimesClosure::do_unlocked(TRAPS) {

  for (int i = 0; i < _count; i++) {
    Handle s = java_lang_String::create_from_str(_names_chars[i],  CHECK);
    _names_strings->obj_at_put(i, s());
  }
}

ThreadTimesClosure::~ThreadTimesClosure() {
  for (int i = 0; i < _count; i++) {
    os::free(_names_chars[i]);
  }
  FREE_C_HEAP_ARRAY(char *, _names_chars);
}

// Fills names with VM internal thread names and times with the corresponding
// CPU times.  If names or times is NULL, a NullPointerException is thrown.
// If the element type of names is not String, an IllegalArgumentException is
// thrown.
// If an array is not large enough to hold all the entries, only the entries
// that fit will be returned.  Return value is the number of VM internal
// threads entries.
JVM_ENTRY(jint, jmm_GetInternalThreadTimes(JNIEnv *env,
                                           jobjectArray names,
                                           jlongArray times))
  if (names == NULL || times == NULL) {
     THROW_(vmSymbols::java_lang_NullPointerException(), 0);
  }
  objArrayOop na = objArrayOop(JNIHandles::resolve_non_null(names));
  objArrayHandle names_ah(THREAD, na);

  // Make sure we have a String array
  Klass* element_klass = ObjArrayKlass::cast(names_ah->klass())->element_klass();
  if (element_klass != vmClasses::String_klass()) {
    THROW_MSG_(vmSymbols::java_lang_IllegalArgumentException(),
               "Array element type is not String class", 0);
  }

  typeArrayOop ta = typeArrayOop(JNIHandles::resolve_non_null(times));
  typeArrayHandle times_ah(THREAD, ta);

  ThreadTimesClosure ttc(names_ah, times_ah);
  {
    MutexLocker ml(THREAD, Threads_lock);
    Threads::threads_do(&ttc);
  }
  ttc.do_unlocked(THREAD);
  return ttc.count();
JVM_END

static Handle find_deadlocks(bool object_monitors_only, TRAPS) {
  ResourceMark rm(THREAD);

  VM_FindDeadlocks op(!object_monitors_only /* also check concurrent locks? */);
  VMThread::execute(&op);

  DeadlockCycle* deadlocks = op.result();
  if (deadlocks == NULL) {
    // no deadlock found and return
    return Handle();
  }

  int num_threads = 0;
  DeadlockCycle* cycle;
  for (cycle = deadlocks; cycle != NULL; cycle = cycle->next()) {
    num_threads += cycle->num_threads();
  }

  objArrayOop r = oopFactory::new_objArray(vmClasses::Thread_klass(), num_threads, CHECK_NH);
  objArrayHandle threads_ah(THREAD, r);

  int index = 0;
  for (cycle = deadlocks; cycle != NULL; cycle = cycle->next()) {
    GrowableArray<JavaThread*>* deadlock_threads = cycle->threads();
    int len = deadlock_threads->length();
    for (int i = 0; i < len; i++) {
      threads_ah->obj_at_put(index, deadlock_threads->at(i)->threadObj());
      index++;
    }
  }
  return threads_ah;
}

// Finds cycles of threads that are deadlocked involved in object monitors
// and JSR-166 synchronizers.
// Returns an array of Thread objects which are in deadlock, if any.
// Otherwise, returns NULL.
//
// Input parameter:
//    object_monitors_only - if true, only check object monitors
//
JVM_ENTRY(jobjectArray, jmm_FindDeadlockedThreads(JNIEnv *env, jboolean object_monitors_only))
  Handle result = find_deadlocks(object_monitors_only != 0, CHECK_NULL);
  return (jobjectArray) JNIHandles::make_local(THREAD, result());
JVM_END

// Finds cycles of threads that are deadlocked on monitor locks
// Returns an array of Thread objects which are in deadlock, if any.
// Otherwise, returns NULL.
JVM_ENTRY(jobjectArray, jmm_FindMonitorDeadlockedThreads(JNIEnv *env))
  Handle result = find_deadlocks(true, CHECK_NULL);
  return (jobjectArray) JNIHandles::make_local(THREAD, result());
JVM_END

// Gets the information about GC extension attributes including
// the name of the attribute, its type, and a short description.
//
// Input parameters:
//   mgr   - GC memory manager
//   info  - caller allocated array of jmmExtAttributeInfo
//   count - number of elements of the info array
//
// Returns the number of GC extension attributes filled in the info array; or
// -1 if info is not big enough
//
JVM_ENTRY(jint, jmm_GetGCExtAttributeInfo(JNIEnv *env, jobject mgr, jmmExtAttributeInfo* info, jint count))
  // All GC memory managers have 1 attribute (number of GC threads)
  if (count == 0) {
    return 0;
  }

  if (info == NULL) {
   THROW_(vmSymbols::java_lang_NullPointerException(), 0);
  }

  info[0].name = "GcThreadCount";
  info[0].type = 'I';
  info[0].description = "Number of GC threads";
  return 1;
JVM_END

// verify the given array is an array of java/lang/management/MemoryUsage objects
// of a given length and return the objArrayOop
static objArrayOop get_memory_usage_objArray(jobjectArray array, int length, TRAPS) {
  if (array == NULL) {
    THROW_(vmSymbols::java_lang_NullPointerException(), 0);
  }

  objArrayOop oa = objArrayOop(JNIHandles::resolve_non_null(array));
  objArrayHandle array_h(THREAD, oa);

  // array must be of the given length
  if (length != array_h->length()) {
    THROW_MSG_(vmSymbols::java_lang_IllegalArgumentException(),
               "The length of the given MemoryUsage array does not match the number of memory pools.", 0);
  }

  // check if the element of array is of type MemoryUsage class
  Klass* usage_klass = Management::java_lang_management_MemoryUsage_klass(CHECK_NULL);
  Klass* element_klass = ObjArrayKlass::cast(array_h->klass())->element_klass();
  if (element_klass != usage_klass) {
    THROW_MSG_(vmSymbols::java_lang_IllegalArgumentException(),
               "The element type is not MemoryUsage class", 0);
  }

  return array_h();
}

// Gets the statistics of the last GC of a given GC memory manager.
// Input parameters:
//   obj     - GarbageCollectorMXBean object
//   gc_stat - caller allocated jmmGCStat where:
//     a. before_gc_usage - array of MemoryUsage objects
//     b. after_gc_usage  - array of MemoryUsage objects
//     c. gc_ext_attributes_values_size is set to the
//        gc_ext_attribute_values array allocated
//     d. gc_ext_attribute_values is a caller allocated array of jvalue.
//
// On return,
//   gc_index == 0 indicates no GC statistics available
//
//   before_gc_usage and after_gc_usage - filled with per memory pool
//      before and after GC usage in the same order as the memory pools
//      returned by GetMemoryPools for a given GC memory manager.
//   num_gc_ext_attributes indicates the number of elements in
//      the gc_ext_attribute_values array is filled; or
//      -1 if the gc_ext_attributes_values array is not big enough
//
JVM_ENTRY(void, jmm_GetLastGCStat(JNIEnv *env, jobject obj, jmmGCStat *gc_stat))
  ResourceMark rm(THREAD);

  if (gc_stat->gc_ext_attribute_values_size > 0 && gc_stat->gc_ext_attribute_values == NULL) {
    THROW(vmSymbols::java_lang_NullPointerException());
  }

  // Get the GCMemoryManager
  GCMemoryManager* mgr = get_gc_memory_manager_from_jobject(obj, CHECK);

  // Make a copy of the last GC statistics
  // GC may occur while constructing the last GC information
  int num_pools = MemoryService::num_memory_pools();
  GCStatInfo stat(num_pools);
  if (mgr->get_last_gc_stat(&stat) == 0) {
    gc_stat->gc_index = 0;
    return;
  }

  gc_stat->gc_index = stat.gc_index();
  gc_stat->start_time = Management::ticks_to_ms(stat.start_time());
  gc_stat->end_time = Management::ticks_to_ms(stat.end_time());

  // Current implementation does not have GC extension attributes
  gc_stat->num_gc_ext_attributes = 0;

  // Fill the arrays of MemoryUsage objects with before and after GC
  // per pool memory usage
  objArrayOop bu = get_memory_usage_objArray(gc_stat->usage_before_gc,
                                             num_pools,
                                             CHECK);
  objArrayHandle usage_before_gc_ah(THREAD, bu);

  objArrayOop au = get_memory_usage_objArray(gc_stat->usage_after_gc,
                                             num_pools,
                                             CHECK);
  objArrayHandle usage_after_gc_ah(THREAD, au);

  for (int i = 0; i < num_pools; i++) {
    Handle before_usage = MemoryService::create_MemoryUsage_obj(stat.before_gc_usage_for_pool(i), CHECK);
    Handle after_usage;

    MemoryUsage u = stat.after_gc_usage_for_pool(i);
    if (u.max_size() == 0 && u.used() > 0) {
      // If max size == 0, this pool is a survivor space.
      // Set max size = -1 since the pools will be swapped after GC.
      MemoryUsage usage(u.init_size(), u.used(), u.committed(), (size_t)-1);
      after_usage = MemoryService::create_MemoryUsage_obj(usage, CHECK);
    } else {
      after_usage = MemoryService::create_MemoryUsage_obj(stat.after_gc_usage_for_pool(i), CHECK);
    }
    usage_before_gc_ah->obj_at_put(i, before_usage());
    usage_after_gc_ah->obj_at_put(i, after_usage());
  }

  if (gc_stat->gc_ext_attribute_values_size > 0) {
    // Current implementation only has 1 attribute (number of GC threads)
    // The type is 'I'
    gc_stat->gc_ext_attribute_values[0].i = mgr->num_gc_threads();
  }
JVM_END

JVM_ENTRY(void, jmm_SetGCNotificationEnabled(JNIEnv *env, jobject obj, jboolean enabled))
  ResourceMark rm(THREAD);
  // Get the GCMemoryManager
  GCMemoryManager* mgr = get_gc_memory_manager_from_jobject(obj, CHECK);
  mgr->set_notification_enabled(enabled?true:false);
JVM_END

// Dump heap - Returns 0 if succeeds.
JVM_ENTRY(jint, jmm_DumpHeap0(JNIEnv *env, jstring outputfile, jboolean live))
#if INCLUDE_SERVICES
  ResourceMark rm(THREAD);
  oop on = JNIHandles::resolve_external_guard(outputfile);
  if (on == NULL) {
    THROW_MSG_(vmSymbols::java_lang_NullPointerException(),
               "Output file name cannot be null.", -1);
  }
  Handle onhandle(THREAD, on);
  char* name = java_lang_String::as_platform_dependent_str(onhandle, CHECK_(-1));
  if (name == NULL) {
    THROW_MSG_(vmSymbols::java_lang_NullPointerException(),
               "Output file name cannot be null.", -1);
  }
  HeapDumper dumper(live ? true : false);
  if (dumper.dump(name) != 0) {
    const char* errmsg = dumper.error_as_C_string();
    THROW_MSG_(vmSymbols::java_io_IOException(), errmsg, -1);
  }
  return 0;
#else  // INCLUDE_SERVICES
  return -1;
#endif // INCLUDE_SERVICES
JVM_END

JVM_ENTRY(jobjectArray, jmm_GetDiagnosticCommands(JNIEnv *env))
  ResourceMark rm(THREAD);
  GrowableArray<const char *>* dcmd_list = DCmdFactory::DCmd_list(DCmd_Source_MBean);
  objArrayOop cmd_array_oop = oopFactory::new_objArray(vmClasses::String_klass(),
          dcmd_list->length(), CHECK_NULL);
  objArrayHandle cmd_array(THREAD, cmd_array_oop);
  for (int i = 0; i < dcmd_list->length(); i++) {
    oop cmd_name = java_lang_String::create_oop_from_str(dcmd_list->at(i), CHECK_NULL);
    cmd_array->obj_at_put(i, cmd_name);
  }
  return (jobjectArray) JNIHandles::make_local(THREAD, cmd_array());
JVM_END

JVM_ENTRY(void, jmm_GetDiagnosticCommandInfo(JNIEnv *env, jobjectArray cmds,
          dcmdInfo* infoArray))
  if (cmds == NULL || infoArray == NULL) {
    THROW(vmSymbols::java_lang_NullPointerException());
  }

  ResourceMark rm(THREAD);

  objArrayOop ca = objArrayOop(JNIHandles::resolve_non_null(cmds));
  objArrayHandle cmds_ah(THREAD, ca);

  // Make sure we have a String array
  Klass* element_klass = ObjArrayKlass::cast(cmds_ah->klass())->element_klass();
  if (element_klass != vmClasses::String_klass()) {
    THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(),
               "Array element type is not String class");
  }

  GrowableArray<DCmdInfo *>* info_list = DCmdFactory::DCmdInfo_list(DCmd_Source_MBean);

  int num_cmds = cmds_ah->length();
  for (int i = 0; i < num_cmds; i++) {
    oop cmd = cmds_ah->obj_at(i);
    if (cmd == NULL) {
        THROW_MSG(vmSymbols::java_lang_NullPointerException(),
                "Command name cannot be null.");
    }
    char* cmd_name = java_lang_String::as_utf8_string(cmd);
    if (cmd_name == NULL) {
        THROW_MSG(vmSymbols::java_lang_NullPointerException(),
                "Command name cannot be null.");
    }
    int pos = info_list->find((void*)cmd_name,DCmdInfo::by_name);
    if (pos == -1) {
        THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(),
             "Unknown diagnostic command");
    }
    DCmdInfo* info = info_list->at(pos);
    infoArray[i].name = info->name();
    infoArray[i].description = info->description();
    infoArray[i].impact = info->impact();
    JavaPermission p = info->permission();
    infoArray[i].permission_class = p._class;
    infoArray[i].permission_name = p._name;
    infoArray[i].permission_action = p._action;
    infoArray[i].num_arguments = info->num_arguments();
    infoArray[i].enabled = info->is_enabled();
  }
JVM_END

JVM_ENTRY(void, jmm_GetDiagnosticCommandArgumentsInfo(JNIEnv *env,
          jstring command, dcmdArgInfo* infoArray))
  ResourceMark rm(THREAD);
  oop cmd = JNIHandles::resolve_external_guard(command);
  if (cmd == NULL) {
    THROW_MSG(vmSymbols::java_lang_NullPointerException(),
              "Command line cannot be null.");
  }
  char* cmd_name = java_lang_String::as_utf8_string(cmd);
  if (cmd_name == NULL) {
    THROW_MSG(vmSymbols::java_lang_NullPointerException(),
              "Command line content cannot be null.");
  }
  DCmd* dcmd = NULL;
  DCmdFactory*factory = DCmdFactory::factory(DCmd_Source_MBean, cmd_name,
                                             strlen(cmd_name));
  if (factory != NULL) {
    dcmd = factory->create_resource_instance(NULL);
  }
  if (dcmd == NULL) {
    THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(),
              "Unknown diagnostic command");
  }
  DCmdMark mark(dcmd);
  GrowableArray<DCmdArgumentInfo*>* array = dcmd->argument_info_array();
  if (array->length() == 0) {
    return;
  }
  for (int i = 0; i < array->length(); i++) {
    infoArray[i].name = array->at(i)->name();
    infoArray[i].description = array->at(i)->description();
    infoArray[i].type = array->at(i)->type();
    infoArray[i].default_string = array->at(i)->default_string();
    infoArray[i].mandatory = array->at(i)->is_mandatory();
    infoArray[i].option = array->at(i)->is_option();
    infoArray[i].multiple = array->at(i)->is_multiple();
    infoArray[i].position = array->at(i)->position();
  }
  return;
JVM_END

JVM_ENTRY(jstring, jmm_ExecuteDiagnosticCommand(JNIEnv *env, jstring commandline))
  ResourceMark rm(THREAD);
  oop cmd = JNIHandles::resolve_external_guard(commandline);
  if (cmd == NULL) {
    THROW_MSG_NULL(vmSymbols::java_lang_NullPointerException(),
                   "Command line cannot be null.");
  }
  char* cmdline = java_lang_String::as_utf8_string(cmd);
  if (cmdline == NULL) {
    THROW_MSG_NULL(vmSymbols::java_lang_NullPointerException(),
                   "Command line content cannot be null.");
  }
  bufferedStream output;
  DCmd::parse_and_execute(DCmd_Source_MBean, &output, cmdline, ' ', CHECK_NULL);
  oop result = java_lang_String::create_oop_from_str(output.as_string(), CHECK_NULL);
  return (jstring) JNIHandles::make_local(THREAD, result);
JVM_END

JVM_ENTRY(void, jmm_SetDiagnosticFrameworkNotificationEnabled(JNIEnv *env, jboolean enabled))
  DCmdFactory::set_jmx_notification_enabled(enabled?true:false);
JVM_END

jlong Management::ticks_to_ms(jlong ticks) {
  assert(os::elapsed_frequency() > 0, "Must be non-zero");
  return (jlong)(((double)ticks / (double)os::elapsed_frequency())
                 * (double)1000.0);
}
#endif // INCLUDE_MANAGEMENT

// Gets the amount of memory allocated on the Java heap for a single thread.
// Returns -1 if the thread does not exist or has terminated.
JVM_ENTRY(jlong, jmm_GetOneThreadAllocatedMemory(JNIEnv *env, jlong thread_id))
  if (thread_id < 0) {
    THROW_MSG_(vmSymbols::java_lang_IllegalArgumentException(),
               "Invalid thread ID", -1);
  }

  if (thread_id == 0) { // current thread
    return thread->cooked_allocated_bytes();
  }

  ThreadsListHandle tlh;
  JavaThread* java_thread = tlh.list()->find_JavaThread_from_java_tid(thread_id);

  if (java_thread != NULL) {
    return java_thread->cooked_allocated_bytes();
  }
  return -1;
JVM_END

// Gets an array containing the amount of memory allocated on the Java
// heap for a set of threads (in bytes).  Each element of the array is
// the amount of memory allocated for the thread ID specified in the
// corresponding entry in the given array of thread IDs; or -1 if the
// thread does not exist or has terminated.
JVM_ENTRY(void, jmm_GetThreadAllocatedMemory(JNIEnv *env, jlongArray ids,
                                             jlongArray sizeArray))
  // Check if threads is null
  if (ids == NULL || sizeArray == NULL) {
    THROW(vmSymbols::java_lang_NullPointerException());
  }

  ResourceMark rm(THREAD);
  typeArrayOop ta = typeArrayOop(JNIHandles::resolve_non_null(ids));
  typeArrayHandle ids_ah(THREAD, ta);

  typeArrayOop sa = typeArrayOop(JNIHandles::resolve_non_null(sizeArray));
  typeArrayHandle sizeArray_h(THREAD, sa);

  // validate the thread id array
  validate_thread_id_array(ids_ah, CHECK);

  // sizeArray must be of the same length as the given array of thread IDs
  int num_threads = ids_ah->length();
  if (num_threads != sizeArray_h->length()) {
    THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(),
              "The length of the given long array does not match the length of "
              "the given array of thread IDs");
  }

  ThreadsListHandle tlh;
  for (int i = 0; i < num_threads; i++) {
    JavaThread* java_thread = tlh.list()->find_JavaThread_from_java_tid(ids_ah->long_at(i));
    if (java_thread != NULL) {
      sizeArray_h->long_at_put(i, java_thread->cooked_allocated_bytes());
    }
  }
JVM_END

// Returns the CPU time consumed by a given thread (in nanoseconds).
// If thread_id == 0, CPU time for the current thread is returned.
// If user_sys_cpu_time = true, user level and system CPU time of
// a given thread is returned; otherwise, only user level CPU time
// is returned.
JVM_ENTRY(jlong, jmm_GetThreadCpuTimeWithKind(JNIEnv *env, jlong thread_id, jboolean user_sys_cpu_time))
  if (!os::is_thread_cpu_time_supported()) {
    return -1;
  }

  if (thread_id < 0) {
    THROW_MSG_(vmSymbols::java_lang_IllegalArgumentException(),
               "Invalid thread ID", -1);
  }

  JavaThread* java_thread = NULL;
  if (thread_id == 0) {
    // current thread
    return os::current_thread_cpu_time(user_sys_cpu_time != 0);
  } else {
    ThreadsListHandle tlh;
    java_thread = tlh.list()->find_JavaThread_from_java_tid(thread_id);
    if (java_thread != NULL) {
      return os::thread_cpu_time((Thread*) java_thread, user_sys_cpu_time != 0);
    }
  }
  return -1;
JVM_END

// Gets an array containing the CPU times consumed by a set of threads
// (in nanoseconds).  Each element of the array is the CPU time for the
// thread ID specified in the corresponding entry in the given array
// of thread IDs; or -1 if the thread does not exist or has terminated.
// If user_sys_cpu_time = true, the sum of user level and system CPU time
// for the given thread is returned; otherwise, only user level CPU time
// is returned.
JVM_ENTRY(void, jmm_GetThreadCpuTimesWithKind(JNIEnv *env, jlongArray ids,
                                              jlongArray timeArray,
                                              jboolean user_sys_cpu_time))
  // Check if threads is null
  if (ids == NULL || timeArray == NULL) {
    THROW(vmSymbols::java_lang_NullPointerException());
  }

  ResourceMark rm(THREAD);
  typeArrayOop ta = typeArrayOop(JNIHandles::resolve_non_null(ids));
  typeArrayHandle ids_ah(THREAD, ta);

  typeArrayOop tia = typeArrayOop(JNIHandles::resolve_non_null(timeArray));
  typeArrayHandle timeArray_h(THREAD, tia);

  // validate the thread id array
  validate_thread_id_array(ids_ah, CHECK);

  // timeArray must be of the same length as the given array of thread IDs
  int num_threads = ids_ah->length();
  if (num_threads != timeArray_h->length()) {
    THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(),
              "The length of the given long array does not match the length of "
              "the given array of thread IDs");
  }

  ThreadsListHandle tlh;
  for (int i = 0; i < num_threads; i++) {
    JavaThread* java_thread = tlh.list()->find_JavaThread_from_java_tid(ids_ah->long_at(i));
    if (java_thread != NULL) {
      timeArray_h->long_at_put(i, os::thread_cpu_time((Thread*)java_thread,
                                                      user_sys_cpu_time != 0));
    }
  }
JVM_END



#if INCLUDE_MANAGEMENT
const struct jmmInterface_1_ jmm_interface = {
  NULL,
  NULL,
  jmm_GetVersion,
  jmm_GetOptionalSupport,
  jmm_GetThreadInfo,
  jmm_GetMemoryPools,
  jmm_GetMemoryManagers,
  jmm_GetMemoryPoolUsage,
  jmm_GetPeakMemoryPoolUsage,
  jmm_GetOneThreadAllocatedMemory,
  jmm_GetThreadAllocatedMemory,
  jmm_GetMemoryUsage,
  jmm_GetLongAttribute,
  jmm_GetBoolAttribute,
  jmm_SetBoolAttribute,
  jmm_GetLongAttributes,
  jmm_FindMonitorDeadlockedThreads,
  jmm_GetThreadCpuTime,
  jmm_GetVMGlobalNames,
  jmm_GetVMGlobals,
  jmm_GetInternalThreadTimes,
  jmm_ResetStatistic,
  jmm_SetPoolSensor,
  jmm_SetPoolThreshold,
  jmm_GetPoolCollectionUsage,
  jmm_GetGCExtAttributeInfo,
  jmm_GetLastGCStat,
  jmm_GetThreadCpuTimeWithKind,
  jmm_GetThreadCpuTimesWithKind,
  jmm_DumpHeap0,
  jmm_FindDeadlockedThreads,
  jmm_SetVMGlobal,
  NULL,
  jmm_DumpThreads,
  jmm_SetGCNotificationEnabled,
  jmm_GetDiagnosticCommands,
  jmm_GetDiagnosticCommandInfo,
  jmm_GetDiagnosticCommandArgumentsInfo,
  jmm_ExecuteDiagnosticCommand,
  jmm_SetDiagnosticFrameworkNotificationEnabled
};
#endif // INCLUDE_MANAGEMENT

void* Management::get_jmm_interface(int version) {
#if INCLUDE_MANAGEMENT
  if (version == JMM_VERSION) {
    return (void*) &jmm_interface;
  }
#endif // INCLUDE_MANAGEMENT
  return NULL;
}
