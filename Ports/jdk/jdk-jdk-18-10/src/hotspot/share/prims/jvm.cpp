/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "jvm.h"
#include "cds/classListParser.hpp"
#include "cds/classListWriter.hpp"
#include "cds/dynamicArchive.hpp"
#include "cds/heapShared.hpp"
#include "cds/lambdaFormInvokers.hpp"
#include "classfile/classFileStream.hpp"
#include "classfile/classLoader.hpp"
#include "classfile/classLoaderData.hpp"
#include "classfile/classLoaderData.inline.hpp"
#include "classfile/classLoadInfo.hpp"
#include "classfile/javaAssertions.hpp"
#include "classfile/javaClasses.inline.hpp"
#include "classfile/moduleEntry.hpp"
#include "classfile/modules.hpp"
#include "classfile/packageEntry.hpp"
#include "classfile/stringTable.hpp"
#include "classfile/symbolTable.hpp"
#include "classfile/systemDictionary.hpp"
#include "classfile/vmClasses.hpp"
#include "classfile/vmSymbols.hpp"
#include "gc/shared/collectedHeap.inline.hpp"
#include "interpreter/bytecode.hpp"
#include "interpreter/bytecodeUtils.hpp"
#include "jfr/jfrEvents.hpp"
#include "logging/log.hpp"
#include "memory/oopFactory.hpp"
#include "memory/referenceType.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/access.inline.hpp"
#include "oops/constantPool.hpp"
#include "oops/fieldStreams.inline.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/klass.inline.hpp"
#include "oops/method.hpp"
#include "oops/recordComponent.hpp"
#include "oops/objArrayKlass.hpp"
#include "oops/objArrayOop.inline.hpp"
#include "oops/oop.inline.hpp"
#include "prims/jvm_misc.hpp"
#include "prims/jvmtiExport.hpp"
#include "prims/jvmtiThreadState.hpp"
#include "prims/stackwalk.hpp"
#include "runtime/arguments.hpp"
#include "runtime/atomic.hpp"
#include "runtime/globals_extension.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/init.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/deoptimization.hpp"
#include "runtime/handshake.hpp"
#include "runtime/java.hpp"
#include "runtime/javaCalls.hpp"
#include "runtime/jfieldIDWorkaround.hpp"
#include "runtime/jniHandles.inline.hpp"
#include "runtime/os.inline.hpp"
#include "runtime/osThread.hpp"
#include "runtime/perfData.hpp"
#include "runtime/reflection.hpp"
#include "runtime/synchronizer.hpp"
#include "runtime/thread.inline.hpp"
#include "runtime/threadSMR.hpp"
#include "runtime/vframe.inline.hpp"
#include "runtime/vmOperations.hpp"
#include "runtime/vm_version.hpp"
#include "services/attachListener.hpp"
#include "services/management.hpp"
#include "services/threadService.hpp"
#include "utilities/copy.hpp"
#include "utilities/defaultStream.hpp"
#include "utilities/dtrace.hpp"
#include "utilities/events.hpp"
#include "utilities/macros.hpp"
#include "utilities/utf8.hpp"
#if INCLUDE_CDS
#include "classfile/systemDictionaryShared.hpp"
#endif
#if INCLUDE_JFR
#include "jfr/jfr.hpp"
#endif

#include <errno.h>

/*
  NOTE about use of any ctor or function call that can trigger a safepoint/GC:
  such ctors and calls MUST NOT come between an oop declaration/init and its
  usage because if objects are move this may cause various memory stomps, bus
  errors and segfaults. Here is a cookbook for causing so called "naked oop
  failures":

      JVM_ENTRY(jobjectArray, JVM_GetClassDeclaredFields<etc> {
          // Object address to be held directly in mirror & not visible to GC
          oop mirror = JNIHandles::resolve_non_null(ofClass);

          // If this ctor can hit a safepoint, moving objects around, then
          ComplexConstructor foo;

          // Boom! mirror may point to JUNK instead of the intended object
          (some dereference of mirror)

          // Here's another call that may block for GC, making mirror stale
          MutexLocker ml(some_lock);

          // And here's an initializer that can result in a stale oop
          // all in one step.
          oop o = call_that_can_throw_exception(TRAPS);


  The solution is to keep the oop declaration BELOW the ctor or function
  call that might cause a GC, do another resolve to reassign the oop, or
  consider use of a Handle instead of an oop so there is immunity from object
  motion. But note that the "QUICK" entries below do not have a handlemark
  and thus can only support use of handles passed in.
*/

static void trace_class_resolution_impl(Klass* to_class, TRAPS) {
  ResourceMark rm;
  int line_number = -1;
  const char * source_file = NULL;
  const char * trace = "explicit";
  InstanceKlass* caller = NULL;
  JavaThread* jthread = THREAD;
  if (jthread->has_last_Java_frame()) {
    vframeStream vfst(jthread);

    // scan up the stack skipping ClassLoader, AccessController and PrivilegedAction frames
    TempNewSymbol access_controller = SymbolTable::new_symbol("java/security/AccessController");
    Klass* access_controller_klass = SystemDictionary::resolve_or_fail(access_controller, false, CHECK);
    TempNewSymbol privileged_action = SymbolTable::new_symbol("java/security/PrivilegedAction");
    Klass* privileged_action_klass = SystemDictionary::resolve_or_fail(privileged_action, false, CHECK);

    Method* last_caller = NULL;

    while (!vfst.at_end()) {
      Method* m = vfst.method();
      if (!vfst.method()->method_holder()->is_subclass_of(vmClasses::ClassLoader_klass())&&
          !vfst.method()->method_holder()->is_subclass_of(access_controller_klass) &&
          !vfst.method()->method_holder()->is_subclass_of(privileged_action_klass)) {
        break;
      }
      last_caller = m;
      vfst.next();
    }
    // if this is called from Class.forName0 and that is called from Class.forName,
    // then print the caller of Class.forName.  If this is Class.loadClass, then print
    // that caller, otherwise keep quiet since this should be picked up elsewhere.
    bool found_it = false;
    if (!vfst.at_end() &&
        vfst.method()->method_holder()->name() == vmSymbols::java_lang_Class() &&
        vfst.method()->name() == vmSymbols::forName0_name()) {
      vfst.next();
      if (!vfst.at_end() &&
          vfst.method()->method_holder()->name() == vmSymbols::java_lang_Class() &&
          vfst.method()->name() == vmSymbols::forName_name()) {
        vfst.next();
        found_it = true;
      }
    } else if (last_caller != NULL &&
               last_caller->method_holder()->name() ==
                 vmSymbols::java_lang_ClassLoader() &&
               last_caller->name() == vmSymbols::loadClass_name()) {
      found_it = true;
    } else if (!vfst.at_end()) {
      if (vfst.method()->is_native()) {
        // JNI call
        found_it = true;
      }
    }
    if (found_it && !vfst.at_end()) {
      // found the caller
      caller = vfst.method()->method_holder();
      line_number = vfst.method()->line_number_from_bci(vfst.bci());
      if (line_number == -1) {
        // show method name if it's a native method
        trace = vfst.method()->name_and_sig_as_C_string();
      }
      Symbol* s = caller->source_file_name();
      if (s != NULL) {
        source_file = s->as_C_string();
      }
    }
  }
  if (caller != NULL) {
    if (to_class != caller) {
      const char * from = caller->external_name();
      const char * to = to_class->external_name();
      // print in a single call to reduce interleaving between threads
      if (source_file != NULL) {
        log_debug(class, resolve)("%s %s %s:%d (%s)", from, to, source_file, line_number, trace);
      } else {
        log_debug(class, resolve)("%s %s (%s)", from, to, trace);
      }
    }
  }
}

void trace_class_resolution(Klass* to_class) {
  EXCEPTION_MARK;
  trace_class_resolution_impl(to_class, THREAD);
  if (HAS_PENDING_EXCEPTION) {
    CLEAR_PENDING_EXCEPTION;
  }
}

// java.lang.System //////////////////////////////////////////////////////////////////////


JVM_LEAF(jlong, JVM_CurrentTimeMillis(JNIEnv *env, jclass ignored))
  return os::javaTimeMillis();
JVM_END

JVM_LEAF(jlong, JVM_NanoTime(JNIEnv *env, jclass ignored))
  return os::javaTimeNanos();
JVM_END

// The function below is actually exposed by jdk.internal.misc.VM and not
// java.lang.System, but we choose to keep it here so that it stays next
// to JVM_CurrentTimeMillis and JVM_NanoTime

const jlong MAX_DIFF_SECS = CONST64(0x0100000000); //  2^32
const jlong MIN_DIFF_SECS = -MAX_DIFF_SECS; // -2^32

JVM_LEAF(jlong, JVM_GetNanoTimeAdjustment(JNIEnv *env, jclass ignored, jlong offset_secs))
  jlong seconds;
  jlong nanos;

  os::javaTimeSystemUTC(seconds, nanos);

  // We're going to verify that the result can fit in a long.
  // For that we need the difference in seconds between 'seconds'
  // and 'offset_secs' to be such that:
  //     |seconds - offset_secs| < (2^63/10^9)
  // We're going to approximate 10^9 ~< 2^30 (1000^3 ~< 1024^3)
  // which makes |seconds - offset_secs| < 2^33
  // and we will prefer +/- 2^32 as the maximum acceptable diff
  // as 2^32 has a more natural feel than 2^33...
  //
  // So if |seconds - offset_secs| >= 2^32 - we return a special
  // sentinel value (-1) which the caller should take as an
  // exception value indicating that the offset given to us is
  // too far from range of the current time - leading to too big
  // a nano adjustment. The caller is expected to recover by
  // computing a more accurate offset and calling this method
  // again. (For the record 2^32 secs is ~136 years, so that
  // should rarely happen)
  //
  jlong diff = seconds - offset_secs;
  if (diff >= MAX_DIFF_SECS || diff <= MIN_DIFF_SECS) {
     return -1; // sentinel value: the offset is too far off the target
  }

  // return the adjustment. If you compute a time by adding
  // this number of nanoseconds along with the number of seconds
  // in the offset you should get the current UTC time.
  return (diff * (jlong)1000000000) + nanos;
JVM_END

JVM_ENTRY(void, JVM_ArrayCopy(JNIEnv *env, jclass ignored, jobject src, jint src_pos,
                               jobject dst, jint dst_pos, jint length))
  // Check if we have null pointers
  if (src == NULL || dst == NULL) {
    THROW(vmSymbols::java_lang_NullPointerException());
  }
  arrayOop s = arrayOop(JNIHandles::resolve_non_null(src));
  arrayOop d = arrayOop(JNIHandles::resolve_non_null(dst));
  assert(oopDesc::is_oop(s), "JVM_ArrayCopy: src not an oop");
  assert(oopDesc::is_oop(d), "JVM_ArrayCopy: dst not an oop");
  // Do copy
  s->klass()->copy_array(s, src_pos, d, dst_pos, length, thread);
JVM_END


static void set_property(Handle props, const char* key, const char* value, TRAPS) {
  JavaValue r(T_OBJECT);
  // public synchronized Object put(Object key, Object value);
  HandleMark hm(THREAD);
  Handle key_str    = java_lang_String::create_from_platform_dependent_str(key, CHECK);
  Handle value_str  = java_lang_String::create_from_platform_dependent_str((value != NULL ? value : ""), CHECK);
  JavaCalls::call_virtual(&r,
                          props,
                          vmClasses::Properties_klass(),
                          vmSymbols::put_name(),
                          vmSymbols::object_object_object_signature(),
                          key_str,
                          value_str,
                          THREAD);
}


#define PUTPROP(props, name, value) set_property((props), (name), (value), CHECK_(properties));

/*
 * Return all of the system properties in a Java String array with alternating
 * names and values from the jvm SystemProperty.
 * Which includes some internal and all commandline -D defined properties.
 */
JVM_ENTRY(jobjectArray, JVM_GetProperties(JNIEnv *env))
  ResourceMark rm(THREAD);
  HandleMark hm(THREAD);
  int ndx = 0;
  int fixedCount = 2;

  SystemProperty* p = Arguments::system_properties();
  int count = Arguments::PropertyList_count(p);

  // Allocate result String array
  InstanceKlass* ik = vmClasses::String_klass();
  objArrayOop r = oopFactory::new_objArray(ik, (count + fixedCount) * 2, CHECK_NULL);
  objArrayHandle result_h(THREAD, r);

  while (p != NULL) {
    const char * key = p->key();
    if (strcmp(key, "sun.nio.MaxDirectMemorySize") != 0) {
        const char * value = p->value();
        Handle key_str    = java_lang_String::create_from_platform_dependent_str(key, CHECK_NULL);
        Handle value_str  = java_lang_String::create_from_platform_dependent_str((value != NULL ? value : ""), CHECK_NULL);
        result_h->obj_at_put(ndx * 2,  key_str());
        result_h->obj_at_put(ndx * 2 + 1, value_str());
        ndx++;
    }
    p = p->next();
  }

  // Convert the -XX:MaxDirectMemorySize= command line flag
  // to the sun.nio.MaxDirectMemorySize property.
  // Do this after setting user properties to prevent people
  // from setting the value with a -D option, as requested.
  // Leave empty if not supplied
  if (!FLAG_IS_DEFAULT(MaxDirectMemorySize)) {
    char as_chars[256];
    jio_snprintf(as_chars, sizeof(as_chars), JULONG_FORMAT, MaxDirectMemorySize);
    Handle key_str = java_lang_String::create_from_platform_dependent_str("sun.nio.MaxDirectMemorySize", CHECK_NULL);
    Handle value_str  = java_lang_String::create_from_platform_dependent_str(as_chars, CHECK_NULL);
    result_h->obj_at_put(ndx * 2,  key_str());
    result_h->obj_at_put(ndx * 2 + 1, value_str());
    ndx++;
  }

  // JVM monitoring and management support
  // Add the sun.management.compiler property for the compiler's name
  {
#undef CSIZE
#if defined(_LP64) || defined(_WIN64)
  #define CSIZE "64-Bit "
#else
  #define CSIZE
#endif // 64bit

#if COMPILER1_AND_COMPILER2
    const char* compiler_name = "HotSpot " CSIZE "Tiered Compilers";
#else
#if defined(COMPILER1)
    const char* compiler_name = "HotSpot " CSIZE "Client Compiler";
#elif defined(COMPILER2)
    const char* compiler_name = "HotSpot " CSIZE "Server Compiler";
#elif INCLUDE_JVMCI
    #error "INCLUDE_JVMCI should imply COMPILER1_OR_COMPILER2"
#else
    const char* compiler_name = "";
#endif // compilers
#endif // COMPILER1_AND_COMPILER2

    if (*compiler_name != '\0' &&
        (Arguments::mode() != Arguments::_int)) {
      Handle key_str = java_lang_String::create_from_platform_dependent_str("sun.management.compiler", CHECK_NULL);
      Handle value_str  = java_lang_String::create_from_platform_dependent_str(compiler_name, CHECK_NULL);
      result_h->obj_at_put(ndx * 2,  key_str());
      result_h->obj_at_put(ndx * 2 + 1, value_str());
      ndx++;
    }
  }

  return (jobjectArray) JNIHandles::make_local(THREAD, result_h());
JVM_END


/*
 * Return the temporary directory that the VM uses for the attach
 * and perf data files.
 *
 * It is important that this directory is well-known and the
 * same for all VM instances. It cannot be affected by configuration
 * variables such as java.io.tmpdir.
 */
JVM_ENTRY(jstring, JVM_GetTemporaryDirectory(JNIEnv *env))
  HandleMark hm(THREAD);
  const char* temp_dir = os::get_temp_directory();
  Handle h = java_lang_String::create_from_platform_dependent_str(temp_dir, CHECK_NULL);
  return (jstring) JNIHandles::make_local(THREAD, h());
JVM_END


// java.lang.Runtime /////////////////////////////////////////////////////////////////////////

extern volatile jint vm_created;

JVM_ENTRY_NO_ENV(void, JVM_BeforeHalt())
#if INCLUDE_CDS
  // Link all classes for dynamic CDS dumping before vm exit.
  if (DynamicDumpSharedSpaces) {
    DynamicArchive::prepare_for_dynamic_dumping();
  }
#endif
  EventShutdown event;
  if (event.should_commit()) {
    event.set_reason("Shutdown requested from Java");
    event.commit();
  }
JVM_END


JVM_ENTRY_NO_ENV(void, JVM_Halt(jint code))
  before_exit(thread);
  vm_exit(code);
JVM_END


JVM_ENTRY_NO_ENV(void, JVM_GC(void))
  if (!DisableExplicitGC) {
    EventSystemGC event;
    event.set_invokedConcurrent(ExplicitGCInvokesConcurrent);
    Universe::heap()->collect(GCCause::_java_lang_system_gc);
    event.commit();
  }
JVM_END


JVM_LEAF(jlong, JVM_MaxObjectInspectionAge(void))
  return Universe::heap()->millis_since_last_whole_heap_examined();
JVM_END


static inline jlong convert_size_t_to_jlong(size_t val) {
  // In the 64-bit vm, a size_t can overflow a jlong (which is signed).
  NOT_LP64 (return (jlong)val;)
  LP64_ONLY(return (jlong)MIN2(val, (size_t)max_jlong);)
}

JVM_ENTRY_NO_ENV(jlong, JVM_TotalMemory(void))
  size_t n = Universe::heap()->capacity();
  return convert_size_t_to_jlong(n);
JVM_END


JVM_ENTRY_NO_ENV(jlong, JVM_FreeMemory(void))
  size_t n = Universe::heap()->unused();
  return convert_size_t_to_jlong(n);
JVM_END


JVM_ENTRY_NO_ENV(jlong, JVM_MaxMemory(void))
  size_t n = Universe::heap()->max_capacity();
  return convert_size_t_to_jlong(n);
JVM_END


JVM_ENTRY_NO_ENV(jint, JVM_ActiveProcessorCount(void))
  return os::active_processor_count();
JVM_END

JVM_ENTRY_NO_ENV(jboolean, JVM_IsUseContainerSupport(void))
#ifdef LINUX
  if (UseContainerSupport) {
    return JNI_TRUE;
  }
#endif
  return JNI_FALSE;
JVM_END

// java.lang.Throwable //////////////////////////////////////////////////////

JVM_ENTRY(void, JVM_FillInStackTrace(JNIEnv *env, jobject receiver))
  Handle exception(thread, JNIHandles::resolve_non_null(receiver));
  java_lang_Throwable::fill_in_stack_trace(exception);
JVM_END

// java.lang.NullPointerException ///////////////////////////////////////////

JVM_ENTRY(jstring, JVM_GetExtendedNPEMessage(JNIEnv *env, jthrowable throwable))
  if (!ShowCodeDetailsInExceptionMessages) return NULL;

  oop exc = JNIHandles::resolve_non_null(throwable);

  Method* method;
  int bci;
  if (!java_lang_Throwable::get_top_method_and_bci(exc, &method, &bci)) {
    return NULL;
  }
  if (method->is_native()) {
    return NULL;
  }

  stringStream ss;
  bool ok = BytecodeUtils::get_NPE_message_at(&ss, method, bci);
  if (ok) {
    oop result = java_lang_String::create_oop_from_str(ss.base(), CHECK_NULL);
    return (jstring) JNIHandles::make_local(THREAD, result);
  } else {
    return NULL;
  }
JVM_END

// java.lang.StackTraceElement //////////////////////////////////////////////


JVM_ENTRY(void, JVM_InitStackTraceElementArray(JNIEnv *env, jobjectArray elements, jobject throwable))
  Handle exception(THREAD, JNIHandles::resolve(throwable));
  objArrayOop st = objArrayOop(JNIHandles::resolve(elements));
  objArrayHandle stack_trace(THREAD, st);
  // Fill in the allocated stack trace
  java_lang_Throwable::get_stack_trace_elements(exception, stack_trace, CHECK);
JVM_END


JVM_ENTRY(void, JVM_InitStackTraceElement(JNIEnv* env, jobject element, jobject stackFrameInfo))
  Handle stack_frame_info(THREAD, JNIHandles::resolve_non_null(stackFrameInfo));
  Handle stack_trace_element(THREAD, JNIHandles::resolve_non_null(element));
  java_lang_StackFrameInfo::to_stack_trace_element(stack_frame_info, stack_trace_element, THREAD);
JVM_END


// java.lang.StackWalker //////////////////////////////////////////////////////


JVM_ENTRY(jobject, JVM_CallStackWalk(JNIEnv *env, jobject stackStream, jlong mode,
                                     jint skip_frames, jint frame_count, jint start_index,
                                     jobjectArray frames))
  if (!thread->has_last_Java_frame()) {
    THROW_MSG_(vmSymbols::java_lang_InternalError(), "doStackWalk: no stack trace", NULL);
  }

  Handle stackStream_h(THREAD, JNIHandles::resolve_non_null(stackStream));

  // frames array is a Class<?>[] array when only getting caller reference,
  // and a StackFrameInfo[] array (or derivative) otherwise. It should never
  // be null.
  objArrayOop fa = objArrayOop(JNIHandles::resolve_non_null(frames));
  objArrayHandle frames_array_h(THREAD, fa);

  int limit = start_index + frame_count;
  if (frames_array_h->length() < limit) {
    THROW_MSG_(vmSymbols::java_lang_IllegalArgumentException(), "not enough space in buffers", NULL);
  }

  oop result = StackWalk::walk(stackStream_h, mode, skip_frames, frame_count,
                               start_index, frames_array_h, CHECK_NULL);
  return JNIHandles::make_local(THREAD, result);
JVM_END


JVM_ENTRY(jint, JVM_MoreStackWalk(JNIEnv *env, jobject stackStream, jlong mode, jlong anchor,
                                  jint frame_count, jint start_index,
                                  jobjectArray frames))
  // frames array is a Class<?>[] array when only getting caller reference,
  // and a StackFrameInfo[] array (or derivative) otherwise. It should never
  // be null.
  objArrayOop fa = objArrayOop(JNIHandles::resolve_non_null(frames));
  objArrayHandle frames_array_h(THREAD, fa);

  int limit = start_index+frame_count;
  if (frames_array_h->length() < limit) {
    THROW_MSG_0(vmSymbols::java_lang_IllegalArgumentException(), "not enough space in buffers");
  }

  Handle stackStream_h(THREAD, JNIHandles::resolve_non_null(stackStream));
  return StackWalk::fetchNextBatch(stackStream_h, mode, anchor, frame_count,
                                   start_index, frames_array_h, THREAD);
JVM_END

// java.lang.Object ///////////////////////////////////////////////


JVM_ENTRY(jint, JVM_IHashCode(JNIEnv* env, jobject handle))
  // as implemented in the classic virtual machine; return 0 if object is NULL
  return handle == NULL ? 0 : ObjectSynchronizer::FastHashCode (THREAD, JNIHandles::resolve_non_null(handle)) ;
JVM_END


JVM_ENTRY(void, JVM_MonitorWait(JNIEnv* env, jobject handle, jlong ms))
  Handle obj(THREAD, JNIHandles::resolve_non_null(handle));
  JavaThreadInObjectWaitState jtiows(thread, ms != 0);
  if (JvmtiExport::should_post_monitor_wait()) {
    JvmtiExport::post_monitor_wait(thread, obj(), ms);

    // The current thread already owns the monitor and it has not yet
    // been added to the wait queue so the current thread cannot be
    // made the successor. This means that the JVMTI_EVENT_MONITOR_WAIT
    // event handler cannot accidentally consume an unpark() meant for
    // the ParkEvent associated with this ObjectMonitor.
  }
  ObjectSynchronizer::wait(obj, ms, CHECK);
JVM_END


JVM_ENTRY(void, JVM_MonitorNotify(JNIEnv* env, jobject handle))
  Handle obj(THREAD, JNIHandles::resolve_non_null(handle));
  ObjectSynchronizer::notify(obj, CHECK);
JVM_END


JVM_ENTRY(void, JVM_MonitorNotifyAll(JNIEnv* env, jobject handle))
  Handle obj(THREAD, JNIHandles::resolve_non_null(handle));
  ObjectSynchronizer::notifyall(obj, CHECK);
JVM_END


JVM_ENTRY(jobject, JVM_Clone(JNIEnv* env, jobject handle))
  Handle obj(THREAD, JNIHandles::resolve_non_null(handle));
  Klass* klass = obj->klass();
  JvmtiVMObjectAllocEventCollector oam;

#ifdef ASSERT
  // Just checking that the cloneable flag is set correct
  if (obj->is_array()) {
    guarantee(klass->is_cloneable(), "all arrays are cloneable");
  } else {
    guarantee(obj->is_instance(), "should be instanceOop");
    bool cloneable = klass->is_subtype_of(vmClasses::Cloneable_klass());
    guarantee(cloneable == klass->is_cloneable(), "incorrect cloneable flag");
  }
#endif

  // Check if class of obj supports the Cloneable interface.
  // All arrays are considered to be cloneable (See JLS 20.1.5).
  // All j.l.r.Reference classes are considered non-cloneable.
  if (!klass->is_cloneable() ||
      (klass->is_instance_klass() &&
       InstanceKlass::cast(klass)->reference_type() != REF_NONE)) {
    ResourceMark rm(THREAD);
    THROW_MSG_0(vmSymbols::java_lang_CloneNotSupportedException(), klass->external_name());
  }

  // Make shallow object copy
  const int size = obj->size();
  oop new_obj_oop = NULL;
  if (obj->is_array()) {
    const int length = ((arrayOop)obj())->length();
    new_obj_oop = Universe::heap()->array_allocate(klass, size, length,
                                                   /* do_zero */ true, CHECK_NULL);
  } else {
    new_obj_oop = Universe::heap()->obj_allocate(klass, size, CHECK_NULL);
  }

  HeapAccess<>::clone(obj(), new_obj_oop, size);

  Handle new_obj(THREAD, new_obj_oop);
  // Caution: this involves a java upcall, so the clone should be
  // "gc-robust" by this stage.
  if (klass->has_finalizer()) {
    assert(obj->is_instance(), "should be instanceOop");
    new_obj_oop = InstanceKlass::register_finalizer(instanceOop(new_obj()), CHECK_NULL);
    new_obj = Handle(THREAD, new_obj_oop);
  }

  return JNIHandles::make_local(THREAD, new_obj());
JVM_END

// java.io.File ///////////////////////////////////////////////////////////////

JVM_LEAF(char*, JVM_NativePath(char* path))
  return os::native_path(path);
JVM_END


// Misc. class handling ///////////////////////////////////////////////////////////


JVM_ENTRY(jclass, JVM_GetCallerClass(JNIEnv* env))
  // Getting the class of the caller frame.
  //
  // The call stack at this point looks something like this:
  //
  // [0] [ @CallerSensitive public sun.reflect.Reflection.getCallerClass ]
  // [1] [ @CallerSensitive API.method                                   ]
  // [.] [ (skipped intermediate frames)                                 ]
  // [n] [ caller                                                        ]
  vframeStream vfst(thread);
  // Cf. LibraryCallKit::inline_native_Reflection_getCallerClass
  for (int n = 0; !vfst.at_end(); vfst.security_next(), n++) {
    Method* m = vfst.method();
    assert(m != NULL, "sanity");
    switch (n) {
    case 0:
      // This must only be called from Reflection.getCallerClass
      if (m->intrinsic_id() != vmIntrinsics::_getCallerClass) {
        THROW_MSG_NULL(vmSymbols::java_lang_InternalError(), "JVM_GetCallerClass must only be called from Reflection.getCallerClass");
      }
      // fall-through
    case 1:
      // Frame 0 and 1 must be caller sensitive.
      if (!m->caller_sensitive()) {
        THROW_MSG_NULL(vmSymbols::java_lang_InternalError(), err_msg("CallerSensitive annotation expected at frame %d", n));
      }
      break;
    default:
      if (!m->is_ignored_by_security_stack_walk()) {
        // We have reached the desired frame; return the holder class.
        return (jclass) JNIHandles::make_local(THREAD, m->method_holder()->java_mirror());
      }
      break;
    }
  }
  return NULL;
JVM_END


JVM_ENTRY(jclass, JVM_FindPrimitiveClass(JNIEnv* env, const char* utf))
  oop mirror = NULL;
  BasicType t = name2type(utf);
  if (t != T_ILLEGAL && !is_reference_type(t)) {
    mirror = Universe::java_mirror(t);
  }
  if (mirror == NULL) {
    THROW_MSG_0(vmSymbols::java_lang_ClassNotFoundException(), (char*) utf);
  } else {
    return (jclass) JNIHandles::make_local(THREAD, mirror);
  }
JVM_END


// Returns a class loaded by the bootstrap class loader; or null
// if not found.  ClassNotFoundException is not thrown.
// FindClassFromBootLoader is exported to the launcher for windows.
JVM_ENTRY(jclass, JVM_FindClassFromBootLoader(JNIEnv* env,
                                              const char* name))
  // Java libraries should ensure that name is never null or illegal.
  if (name == NULL || (int)strlen(name) > Symbol::max_length()) {
    // It's impossible to create this class;  the name cannot fit
    // into the constant pool.
    return NULL;
  }
  assert(UTF8::is_legal_utf8((const unsigned char*)name, (int)strlen(name), false), "illegal UTF name");

  TempNewSymbol h_name = SymbolTable::new_symbol(name);
  Klass* k = SystemDictionary::resolve_or_null(h_name, CHECK_NULL);
  if (k == NULL) {
    return NULL;
  }

  if (log_is_enabled(Debug, class, resolve)) {
    trace_class_resolution(k);
  }
  return (jclass) JNIHandles::make_local(THREAD, k->java_mirror());
JVM_END

// Find a class with this name in this loader, using the caller's protection domain.
JVM_ENTRY(jclass, JVM_FindClassFromCaller(JNIEnv* env, const char* name,
                                          jboolean init, jobject loader,
                                          jclass caller))
  TempNewSymbol h_name =
       SystemDictionary::class_name_symbol(name, vmSymbols::java_lang_ClassNotFoundException(),
                                           CHECK_NULL);

  oop loader_oop = JNIHandles::resolve(loader);
  oop from_class = JNIHandles::resolve(caller);
  oop protection_domain = NULL;
  // If loader is null, shouldn't call ClassLoader.checkPackageAccess; otherwise get
  // NPE. Put it in another way, the bootstrap class loader has all permission and
  // thus no checkPackageAccess equivalence in the VM class loader.
  // The caller is also passed as NULL by the java code if there is no security
  // manager to avoid the performance cost of getting the calling class.
  if (from_class != NULL && loader_oop != NULL) {
    protection_domain = java_lang_Class::as_Klass(from_class)->protection_domain();
  }

  Handle h_loader(THREAD, loader_oop);
  Handle h_prot(THREAD, protection_domain);
  jclass result = find_class_from_class_loader(env, h_name, init, h_loader,
                                               h_prot, false, THREAD);

  if (log_is_enabled(Debug, class, resolve) && result != NULL) {
    trace_class_resolution(java_lang_Class::as_Klass(JNIHandles::resolve_non_null(result)));
  }
  return result;
JVM_END

// Currently only called from the old verifier.
JVM_ENTRY(jclass, JVM_FindClassFromClass(JNIEnv *env, const char *name,
                                         jboolean init, jclass from))
  TempNewSymbol h_name =
       SystemDictionary::class_name_symbol(name, vmSymbols::java_lang_ClassNotFoundException(),
                                           CHECK_NULL);
  oop from_class_oop = JNIHandles::resolve(from);
  Klass* from_class = (from_class_oop == NULL)
                           ? (Klass*)NULL
                           : java_lang_Class::as_Klass(from_class_oop);
  oop class_loader = NULL;
  oop protection_domain = NULL;
  if (from_class != NULL) {
    class_loader = from_class->class_loader();
    protection_domain = from_class->protection_domain();
  }
  Handle h_loader(THREAD, class_loader);
  Handle h_prot  (THREAD, protection_domain);
  jclass result = find_class_from_class_loader(env, h_name, init, h_loader,
                                               h_prot, true, thread);

  if (log_is_enabled(Debug, class, resolve) && result != NULL) {
    // this function is generally only used for class loading during verification.
    ResourceMark rm;
    oop from_mirror = JNIHandles::resolve_non_null(from);
    Klass* from_class = java_lang_Class::as_Klass(from_mirror);
    const char * from_name = from_class->external_name();

    oop mirror = JNIHandles::resolve_non_null(result);
    Klass* to_class = java_lang_Class::as_Klass(mirror);
    const char * to = to_class->external_name();
    log_debug(class, resolve)("%s %s (verification)", from_name, to);
  }

  return result;
JVM_END

// common code for JVM_DefineClass() and JVM_DefineClassWithSource()
static jclass jvm_define_class_common(const char *name,
                                      jobject loader, const jbyte *buf,
                                      jsize len, jobject pd, const char *source,
                                      TRAPS) {
  if (source == NULL)  source = "__JVM_DefineClass__";

  JavaThread* jt = THREAD;

  PerfClassTraceTime vmtimer(ClassLoader::perf_define_appclass_time(),
                             ClassLoader::perf_define_appclass_selftime(),
                             ClassLoader::perf_define_appclasses(),
                             jt->get_thread_stat()->perf_recursion_counts_addr(),
                             jt->get_thread_stat()->perf_timers_addr(),
                             PerfClassTraceTime::DEFINE_CLASS);

  if (UsePerfData) {
    ClassLoader::perf_app_classfile_bytes_read()->inc(len);
  }

  // Class resolution will get the class name from the .class stream if the name is null.
  TempNewSymbol class_name = name == NULL ? NULL :
       SystemDictionary::class_name_symbol(name, vmSymbols::java_lang_NoClassDefFoundError(),
                                           CHECK_NULL);

  ResourceMark rm(THREAD);
  ClassFileStream st((u1*)buf, len, source, ClassFileStream::verify);
  Handle class_loader (THREAD, JNIHandles::resolve(loader));
  Handle protection_domain (THREAD, JNIHandles::resolve(pd));
  ClassLoadInfo cl_info(protection_domain);
  Klass* k = SystemDictionary::resolve_from_stream(&st, class_name,
                                                   class_loader,
                                                   cl_info,
                                                   CHECK_NULL);

  if (log_is_enabled(Debug, class, resolve)) {
    trace_class_resolution(k);
  }

  return (jclass) JNIHandles::make_local(THREAD, k->java_mirror());
}

enum {
  NESTMATE              = java_lang_invoke_MemberName::MN_NESTMATE_CLASS,
  HIDDEN_CLASS          = java_lang_invoke_MemberName::MN_HIDDEN_CLASS,
  STRONG_LOADER_LINK    = java_lang_invoke_MemberName::MN_STRONG_LOADER_LINK,
  ACCESS_VM_ANNOTATIONS = java_lang_invoke_MemberName::MN_ACCESS_VM_ANNOTATIONS
};

/*
 * Define a class with the specified flags that indicates if it's a nestmate,
 * hidden, or strongly referenced from class loader.
 */
static jclass jvm_lookup_define_class(jclass lookup, const char *name,
                                      const jbyte *buf, jsize len, jobject pd,
                                      jboolean init, int flags, jobject classData, TRAPS) {
  ResourceMark rm(THREAD);

  Klass* lookup_k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(lookup));
  // Lookup class must be a non-null instance
  if (lookup_k == NULL) {
    THROW_MSG_0(vmSymbols::java_lang_IllegalArgumentException(), "Lookup class is null");
  }
  assert(lookup_k->is_instance_klass(), "Lookup class must be an instance klass");

  Handle class_loader (THREAD, lookup_k->class_loader());

  bool is_nestmate = (flags & NESTMATE) == NESTMATE;
  bool is_hidden = (flags & HIDDEN_CLASS) == HIDDEN_CLASS;
  bool is_strong = (flags & STRONG_LOADER_LINK) == STRONG_LOADER_LINK;
  bool vm_annotations = (flags & ACCESS_VM_ANNOTATIONS) == ACCESS_VM_ANNOTATIONS;

  InstanceKlass* host_class = NULL;
  if (is_nestmate) {
    host_class = InstanceKlass::cast(lookup_k)->nest_host(CHECK_NULL);
  }

  log_info(class, nestmates)("LookupDefineClass: %s - %s%s, %s, %s, %s",
                             name,
                             is_nestmate ? "with dynamic nest-host " : "non-nestmate",
                             is_nestmate ? host_class->external_name() : "",
                             is_hidden ? "hidden" : "not hidden",
                             is_strong ? "strong" : "weak",
                             vm_annotations ? "with vm annotations" : "without vm annotation");

  if (!is_hidden) {
    // classData is only applicable for hidden classes
    if (classData != NULL) {
      THROW_MSG_0(vmSymbols::java_lang_IllegalArgumentException(), "classData is only applicable for hidden classes");
    }
    if (is_nestmate) {
      THROW_MSG_0(vmSymbols::java_lang_IllegalArgumentException(), "dynamic nestmate is only applicable for hidden classes");
    }
    if (!is_strong) {
      THROW_MSG_0(vmSymbols::java_lang_IllegalArgumentException(), "an ordinary class must be strongly referenced by its defining loader");
    }
    if (vm_annotations) {
      THROW_MSG_0(vmSymbols::java_lang_IllegalArgumentException(), "vm annotations only allowed for hidden classes");
    }
    if (flags != STRONG_LOADER_LINK) {
      THROW_MSG_0(vmSymbols::java_lang_IllegalArgumentException(),
                  err_msg("invalid flag 0x%x", flags));
    }
  }

  // Class resolution will get the class name from the .class stream if the name is null.
  TempNewSymbol class_name = name == NULL ? NULL :
       SystemDictionary::class_name_symbol(name, vmSymbols::java_lang_NoClassDefFoundError(),
                                           CHECK_NULL);

  Handle protection_domain (THREAD, JNIHandles::resolve(pd));
  const char* source = is_nestmate ? host_class->external_name() : "__JVM_LookupDefineClass__";
  ClassFileStream st((u1*)buf, len, source, ClassFileStream::verify);

  InstanceKlass* ik = NULL;
  if (!is_hidden) {
    ClassLoadInfo cl_info(protection_domain);
    ik = SystemDictionary::resolve_from_stream(&st, class_name,
                                               class_loader,
                                               cl_info,
                                               CHECK_NULL);

    if (log_is_enabled(Debug, class, resolve)) {
      trace_class_resolution(ik);
    }
  } else { // hidden
    Handle classData_h(THREAD, JNIHandles::resolve(classData));
    ClassLoadInfo cl_info(protection_domain,
                          host_class,
                          classData_h,
                          is_hidden,
                          is_strong,
                          vm_annotations);
    ik = SystemDictionary::resolve_from_stream(&st, class_name,
                                               class_loader,
                                               cl_info,
                                               CHECK_NULL);

    // The hidden class loader data has been artificially been kept alive to
    // this point. The mirror and any instances of this class have to keep
    // it alive afterwards.
    ik->class_loader_data()->dec_keep_alive();

    if (is_nestmate && log_is_enabled(Debug, class, nestmates)) {
      ModuleEntry* module = ik->module();
      const char * module_name = module->is_named() ? module->name()->as_C_string() : UNNAMED_MODULE;
      log_debug(class, nestmates)("Dynamic nestmate: %s/%s, nest_host %s, %s",
                                  module_name,
                                  ik->external_name(),
                                  host_class->external_name(),
                                  ik->is_hidden() ? "is hidden" : "is not hidden");
    }
  }
  assert(Reflection::is_same_class_package(lookup_k, ik),
         "lookup class and defined class are in different packages");

  if (init) {
    ik->initialize(CHECK_NULL);
  } else {
    ik->link_class(CHECK_NULL);
  }

  return (jclass) JNIHandles::make_local(THREAD, ik->java_mirror());
}

JVM_ENTRY(jclass, JVM_DefineClass(JNIEnv *env, const char *name, jobject loader, const jbyte *buf, jsize len, jobject pd))
  return jvm_define_class_common(name, loader, buf, len, pd, NULL, THREAD);
JVM_END

/*
 * Define a class with the specified lookup class.
 *  lookup:  Lookup class
 *  name:    the name of the class
 *  buf:     class bytes
 *  len:     length of class bytes
 *  pd:      protection domain
 *  init:    initialize the class
 *  flags:   properties of the class
 *  classData: private static pre-initialized field
 */
JVM_ENTRY(jclass, JVM_LookupDefineClass(JNIEnv *env, jclass lookup, const char *name, const jbyte *buf,
          jsize len, jobject pd, jboolean initialize, int flags, jobject classData))

  if (lookup == NULL) {
    THROW_MSG_0(vmSymbols::java_lang_IllegalArgumentException(), "Lookup class is null");
  }

  assert(buf != NULL, "buf must not be NULL");

  return jvm_lookup_define_class(lookup, name, buf, len, pd, initialize, flags, classData, THREAD);
JVM_END

JVM_ENTRY(jclass, JVM_DefineClassWithSource(JNIEnv *env, const char *name, jobject loader, const jbyte *buf, jsize len, jobject pd, const char *source))

  return jvm_define_class_common(name, loader, buf, len, pd, source, THREAD);
JVM_END

JVM_ENTRY(jclass, JVM_FindLoadedClass(JNIEnv *env, jobject loader, jstring name))
  ResourceMark rm(THREAD);

  Handle h_name (THREAD, JNIHandles::resolve_non_null(name));
  char* str = java_lang_String::as_utf8_string(h_name());

  // Sanity check, don't expect null
  if (str == NULL) return NULL;

  // Internalize the string, converting '.' to '/' in string.
  char* p = (char*)str;
  while (*p != '\0') {
    if (*p == '.') {
      *p = '/';
    }
    p++;
  }

  const int str_len = (int)(p - str);
  if (str_len > Symbol::max_length()) {
    // It's impossible to create this class;  the name cannot fit
    // into the constant pool.
    return NULL;
  }
  TempNewSymbol klass_name = SymbolTable::new_symbol(str, str_len);

  // Security Note:
  //   The Java level wrapper will perform the necessary security check allowing
  //   us to pass the NULL as the initiating class loader.
  Handle h_loader(THREAD, JNIHandles::resolve(loader));
  Klass* k = SystemDictionary::find_instance_or_array_klass(klass_name,
                                                              h_loader,
                                                              Handle());
#if INCLUDE_CDS
  if (k == NULL) {
    // If the class is not already loaded, try to see if it's in the shared
    // archive for the current classloader (h_loader).
    k = SystemDictionaryShared::find_or_load_shared_class(klass_name, h_loader, CHECK_NULL);
  }
#endif
  return (k == NULL) ? NULL :
            (jclass) JNIHandles::make_local(THREAD, k->java_mirror());
JVM_END

// Module support //////////////////////////////////////////////////////////////////////////////

JVM_ENTRY(void, JVM_DefineModule(JNIEnv *env, jobject module, jboolean is_open, jstring version,
                                 jstring location, jobjectArray packages))
  Handle h_module (THREAD, JNIHandles::resolve(module));
  Modules::define_module(h_module, is_open, version, location, packages, CHECK);
JVM_END

JVM_ENTRY(void, JVM_SetBootLoaderUnnamedModule(JNIEnv *env, jobject module))
  Handle h_module (THREAD, JNIHandles::resolve(module));
  Modules::set_bootloader_unnamed_module(h_module, CHECK);
JVM_END

JVM_ENTRY(void, JVM_AddModuleExports(JNIEnv *env, jobject from_module, jstring package, jobject to_module))
  Handle h_from_module (THREAD, JNIHandles::resolve(from_module));
  Handle h_to_module (THREAD, JNIHandles::resolve(to_module));
  Modules::add_module_exports_qualified(h_from_module, package, h_to_module, CHECK);
JVM_END

JVM_ENTRY(void, JVM_AddModuleExportsToAllUnnamed(JNIEnv *env, jobject from_module, jstring package))
  Handle h_from_module (THREAD, JNIHandles::resolve(from_module));
  Modules::add_module_exports_to_all_unnamed(h_from_module, package, CHECK);
JVM_END

JVM_ENTRY(void, JVM_AddModuleExportsToAll(JNIEnv *env, jobject from_module, jstring package))
  Handle h_from_module (THREAD, JNIHandles::resolve(from_module));
  Modules::add_module_exports(h_from_module, package, Handle(), CHECK);
JVM_END

JVM_ENTRY (void, JVM_AddReadsModule(JNIEnv *env, jobject from_module, jobject source_module))
  Handle h_from_module (THREAD, JNIHandles::resolve(from_module));
  Handle h_source_module (THREAD, JNIHandles::resolve(source_module));
  Modules::add_reads_module(h_from_module, h_source_module, CHECK);
JVM_END

JVM_ENTRY(void, JVM_DefineArchivedModules(JNIEnv *env, jobject platform_loader, jobject system_loader))
  Handle h_platform_loader (THREAD, JNIHandles::resolve(platform_loader));
  Handle h_system_loader (THREAD, JNIHandles::resolve(system_loader));
  Modules::define_archived_modules(h_platform_loader, h_system_loader, CHECK);
JVM_END

// Reflection support //////////////////////////////////////////////////////////////////////////////

JVM_ENTRY(jstring, JVM_InitClassName(JNIEnv *env, jclass cls))
  assert (cls != NULL, "illegal class");
  JvmtiVMObjectAllocEventCollector oam;
  ResourceMark rm(THREAD);
  HandleMark hm(THREAD);
  Handle java_class(THREAD, JNIHandles::resolve(cls));
  oop result = java_lang_Class::name(java_class, CHECK_NULL);
  return (jstring) JNIHandles::make_local(THREAD, result);
JVM_END


JVM_ENTRY(jobjectArray, JVM_GetClassInterfaces(JNIEnv *env, jclass cls))
  JvmtiVMObjectAllocEventCollector oam;
  oop mirror = JNIHandles::resolve_non_null(cls);

  // Special handling for primitive objects
  if (java_lang_Class::is_primitive(mirror)) {
    // Primitive objects does not have any interfaces
    objArrayOop r = oopFactory::new_objArray(vmClasses::Class_klass(), 0, CHECK_NULL);
    return (jobjectArray) JNIHandles::make_local(THREAD, r);
  }

  Klass* klass = java_lang_Class::as_Klass(mirror);
  // Figure size of result array
  int size;
  if (klass->is_instance_klass()) {
    size = InstanceKlass::cast(klass)->local_interfaces()->length();
  } else {
    assert(klass->is_objArray_klass() || klass->is_typeArray_klass(), "Illegal mirror klass");
    size = 2;
  }

  // Allocate result array
  objArrayOop r = oopFactory::new_objArray(vmClasses::Class_klass(), size, CHECK_NULL);
  objArrayHandle result (THREAD, r);
  // Fill in result
  if (klass->is_instance_klass()) {
    // Regular instance klass, fill in all local interfaces
    for (int index = 0; index < size; index++) {
      Klass* k = InstanceKlass::cast(klass)->local_interfaces()->at(index);
      result->obj_at_put(index, k->java_mirror());
    }
  } else {
    // All arrays implement java.lang.Cloneable and java.io.Serializable
    result->obj_at_put(0, vmClasses::Cloneable_klass()->java_mirror());
    result->obj_at_put(1, vmClasses::Serializable_klass()->java_mirror());
  }
  return (jobjectArray) JNIHandles::make_local(THREAD, result());
JVM_END


JVM_ENTRY(jboolean, JVM_IsInterface(JNIEnv *env, jclass cls))
  oop mirror = JNIHandles::resolve_non_null(cls);
  if (java_lang_Class::is_primitive(mirror)) {
    return JNI_FALSE;
  }
  Klass* k = java_lang_Class::as_Klass(mirror);
  jboolean result = k->is_interface();
  assert(!result || k->is_instance_klass(),
         "all interfaces are instance types");
  // The compiler intrinsic for isInterface tests the
  // Klass::_access_flags bits in the same way.
  return result;
JVM_END

JVM_ENTRY(jboolean, JVM_IsHiddenClass(JNIEnv *env, jclass cls))
  oop mirror = JNIHandles::resolve_non_null(cls);
  if (java_lang_Class::is_primitive(mirror)) {
    return JNI_FALSE;
  }
  Klass* k = java_lang_Class::as_Klass(mirror);
  return k->is_hidden();
JVM_END

JVM_ENTRY(jobjectArray, JVM_GetClassSigners(JNIEnv *env, jclass cls))
  JvmtiVMObjectAllocEventCollector oam;
  oop mirror = JNIHandles::resolve_non_null(cls);
  if (java_lang_Class::is_primitive(mirror)) {
    // There are no signers for primitive types
    return NULL;
  }

  objArrayHandle signers(THREAD, java_lang_Class::signers(mirror));

  // If there are no signers set in the class, or if the class
  // is an array, return NULL.
  if (signers == NULL) return NULL;

  // copy of the signers array
  Klass* element = ObjArrayKlass::cast(signers->klass())->element_klass();
  objArrayOop signers_copy = oopFactory::new_objArray(element, signers->length(), CHECK_NULL);
  for (int index = 0; index < signers->length(); index++) {
    signers_copy->obj_at_put(index, signers->obj_at(index));
  }

  // return the copy
  return (jobjectArray) JNIHandles::make_local(THREAD, signers_copy);
JVM_END


JVM_ENTRY(void, JVM_SetClassSigners(JNIEnv *env, jclass cls, jobjectArray signers))
  oop mirror = JNIHandles::resolve_non_null(cls);
  if (!java_lang_Class::is_primitive(mirror)) {
    // This call is ignored for primitive types and arrays.
    // Signers are only set once, ClassLoader.java, and thus shouldn't
    // be called with an array.  Only the bootstrap loader creates arrays.
    Klass* k = java_lang_Class::as_Klass(mirror);
    if (k->is_instance_klass()) {
      java_lang_Class::set_signers(k->java_mirror(), objArrayOop(JNIHandles::resolve(signers)));
    }
  }
JVM_END


JVM_ENTRY(jobject, JVM_GetProtectionDomain(JNIEnv *env, jclass cls))
  oop mirror = JNIHandles::resolve_non_null(cls);
  if (mirror == NULL) {
    THROW_(vmSymbols::java_lang_NullPointerException(), NULL);
  }

  if (java_lang_Class::is_primitive(mirror)) {
    // Primitive types does not have a protection domain.
    return NULL;
  }

  oop pd = java_lang_Class::protection_domain(mirror);
  return (jobject) JNIHandles::make_local(THREAD, pd);
JVM_END


// Returns the inherited_access_control_context field of the running thread.
JVM_ENTRY(jobject, JVM_GetInheritedAccessControlContext(JNIEnv *env, jclass cls))
  oop result = java_lang_Thread::inherited_access_control_context(thread->threadObj());
  return JNIHandles::make_local(THREAD, result);
JVM_END

JVM_ENTRY(jobject, JVM_GetStackAccessControlContext(JNIEnv *env, jclass cls))
  if (!UsePrivilegedStack) return NULL;

  ResourceMark rm(THREAD);
  GrowableArray<Handle>* local_array = new GrowableArray<Handle>(12);
  JvmtiVMObjectAllocEventCollector oam;

  // count the protection domains on the execution stack. We collapse
  // duplicate consecutive protection domains into a single one, as
  // well as stopping when we hit a privileged frame.

  oop previous_protection_domain = NULL;
  Handle privileged_context(thread, NULL);
  bool is_privileged = false;
  oop protection_domain = NULL;

  // Iterate through Java frames
  vframeStream vfst(thread);
  for(; !vfst.at_end(); vfst.next()) {
    // get method of frame
    Method* method = vfst.method();

    // stop at the first privileged frame
    if (method->method_holder() == vmClasses::AccessController_klass() &&
      method->name() == vmSymbols::executePrivileged_name())
    {
      // this frame is privileged
      is_privileged = true;

      javaVFrame *priv = vfst.asJavaVFrame();       // executePrivileged

      StackValueCollection* locals = priv->locals();
      StackValue* ctx_sv = locals->at(1); // AccessControlContext context
      StackValue* clr_sv = locals->at(2); // Class<?> caller
      assert(!ctx_sv->obj_is_scalar_replaced(), "found scalar-replaced object");
      assert(!clr_sv->obj_is_scalar_replaced(), "found scalar-replaced object");
      privileged_context    = ctx_sv->get_obj();
      Handle caller         = clr_sv->get_obj();

      Klass *caller_klass = java_lang_Class::as_Klass(caller());
      protection_domain  = caller_klass->protection_domain();
    } else {
      protection_domain = method->method_holder()->protection_domain();
    }

    if ((previous_protection_domain != protection_domain) && (protection_domain != NULL)) {
      local_array->push(Handle(thread, protection_domain));
      previous_protection_domain = protection_domain;
    }

    if (is_privileged) break;
  }


  // either all the domains on the stack were system domains, or
  // we had a privileged system domain
  if (local_array->is_empty()) {
    if (is_privileged && privileged_context.is_null()) return NULL;

    oop result = java_security_AccessControlContext::create(objArrayHandle(), is_privileged, privileged_context, CHECK_NULL);
    return JNIHandles::make_local(THREAD, result);
  }

  objArrayOop context = oopFactory::new_objArray(vmClasses::ProtectionDomain_klass(),
                                                 local_array->length(), CHECK_NULL);
  objArrayHandle h_context(thread, context);
  for (int index = 0; index < local_array->length(); index++) {
    h_context->obj_at_put(index, local_array->at(index)());
  }

  oop result = java_security_AccessControlContext::create(h_context, is_privileged, privileged_context, CHECK_NULL);

  return JNIHandles::make_local(THREAD, result);
JVM_END


JVM_ENTRY(jboolean, JVM_IsArrayClass(JNIEnv *env, jclass cls))
  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(cls));
  return (k != NULL) && k->is_array_klass() ? true : false;
JVM_END


JVM_ENTRY(jboolean, JVM_IsPrimitiveClass(JNIEnv *env, jclass cls))
  oop mirror = JNIHandles::resolve_non_null(cls);
  return (jboolean) java_lang_Class::is_primitive(mirror);
JVM_END


JVM_ENTRY(jint, JVM_GetClassModifiers(JNIEnv *env, jclass cls))
  oop mirror = JNIHandles::resolve_non_null(cls);
  if (java_lang_Class::is_primitive(mirror)) {
    // Primitive type
    return JVM_ACC_ABSTRACT | JVM_ACC_FINAL | JVM_ACC_PUBLIC;
  }

  Klass* k = java_lang_Class::as_Klass(mirror);
  debug_only(int computed_modifiers = k->compute_modifier_flags());
  assert(k->modifier_flags() == computed_modifiers, "modifiers cache is OK");
  return k->modifier_flags();
JVM_END


// Inner class reflection ///////////////////////////////////////////////////////////////////////////////

JVM_ENTRY(jobjectArray, JVM_GetDeclaredClasses(JNIEnv *env, jclass ofClass))
  JvmtiVMObjectAllocEventCollector oam;
  // ofClass is a reference to a java_lang_Class object. The mirror object
  // of an InstanceKlass
  oop ofMirror = JNIHandles::resolve_non_null(ofClass);
  if (java_lang_Class::is_primitive(ofMirror) ||
      ! java_lang_Class::as_Klass(ofMirror)->is_instance_klass()) {
    oop result = oopFactory::new_objArray(vmClasses::Class_klass(), 0, CHECK_NULL);
    return (jobjectArray)JNIHandles::make_local(THREAD, result);
  }

  InstanceKlass* k = InstanceKlass::cast(java_lang_Class::as_Klass(ofMirror));
  InnerClassesIterator iter(k);

  if (iter.length() == 0) {
    // Neither an inner nor outer class
    oop result = oopFactory::new_objArray(vmClasses::Class_klass(), 0, CHECK_NULL);
    return (jobjectArray)JNIHandles::make_local(THREAD, result);
  }

  // find inner class info
  constantPoolHandle cp(thread, k->constants());
  int length = iter.length();

  // Allocate temp. result array
  objArrayOop r = oopFactory::new_objArray(vmClasses::Class_klass(), length/4, CHECK_NULL);
  objArrayHandle result (THREAD, r);
  int members = 0;

  for (; !iter.done(); iter.next()) {
    int ioff = iter.inner_class_info_index();
    int ooff = iter.outer_class_info_index();

    if (ioff != 0 && ooff != 0) {
      // Check to see if the name matches the class we're looking for
      // before attempting to find the class.
      if (cp->klass_name_at_matches(k, ooff)) {
        Klass* outer_klass = cp->klass_at(ooff, CHECK_NULL);
        if (outer_klass == k) {
           Klass* ik = cp->klass_at(ioff, CHECK_NULL);
           InstanceKlass* inner_klass = InstanceKlass::cast(ik);

           // Throws an exception if outer klass has not declared k as
           // an inner klass
           Reflection::check_for_inner_class(k, inner_klass, true, CHECK_NULL);

           result->obj_at_put(members, inner_klass->java_mirror());
           members++;
        }
      }
    }
  }

  if (members != length) {
    // Return array of right length
    objArrayOop res = oopFactory::new_objArray(vmClasses::Class_klass(), members, CHECK_NULL);
    for(int i = 0; i < members; i++) {
      res->obj_at_put(i, result->obj_at(i));
    }
    return (jobjectArray)JNIHandles::make_local(THREAD, res);
  }

  return (jobjectArray)JNIHandles::make_local(THREAD, result());
JVM_END


JVM_ENTRY(jclass, JVM_GetDeclaringClass(JNIEnv *env, jclass ofClass))
{
  // ofClass is a reference to a java_lang_Class object.
  oop ofMirror = JNIHandles::resolve_non_null(ofClass);
  if (java_lang_Class::is_primitive(ofMirror)) {
    return NULL;
  }
  Klass* klass = java_lang_Class::as_Klass(ofMirror);
  if (!klass->is_instance_klass()) {
    return NULL;
  }

  bool inner_is_member = false;
  Klass* outer_klass
    = InstanceKlass::cast(klass)->compute_enclosing_class(&inner_is_member, CHECK_NULL);
  if (outer_klass == NULL)  return NULL;  // already a top-level class
  if (!inner_is_member)  return NULL;     // a hidden class (inside a method)
  return (jclass) JNIHandles::make_local(THREAD, outer_klass->java_mirror());
}
JVM_END

JVM_ENTRY(jstring, JVM_GetSimpleBinaryName(JNIEnv *env, jclass cls))
{
  oop mirror = JNIHandles::resolve_non_null(cls);
  if (java_lang_Class::is_primitive(mirror)) {
    return NULL;
  }
  Klass* klass = java_lang_Class::as_Klass(mirror);
  if (!klass->is_instance_klass()) {
    return NULL;
  }
  InstanceKlass* k = InstanceKlass::cast(klass);
  int ooff = 0, noff = 0;
  if (k->find_inner_classes_attr(&ooff, &noff, THREAD)) {
    if (noff != 0) {
      constantPoolHandle i_cp(thread, k->constants());
      Symbol* name = i_cp->symbol_at(noff);
      Handle str = java_lang_String::create_from_symbol(name, CHECK_NULL);
      return (jstring) JNIHandles::make_local(THREAD, str());
    }
  }
  return NULL;
}
JVM_END

JVM_ENTRY(jstring, JVM_GetClassSignature(JNIEnv *env, jclass cls))
  assert (cls != NULL, "illegal class");
  JvmtiVMObjectAllocEventCollector oam;
  ResourceMark rm(THREAD);
  oop mirror = JNIHandles::resolve_non_null(cls);
  // Return null for arrays and primatives
  if (!java_lang_Class::is_primitive(mirror)) {
    Klass* k = java_lang_Class::as_Klass(mirror);
    if (k->is_instance_klass()) {
      Symbol* sym = InstanceKlass::cast(k)->generic_signature();
      if (sym == NULL) return NULL;
      Handle str = java_lang_String::create_from_symbol(sym, CHECK_NULL);
      return (jstring) JNIHandles::make_local(THREAD, str());
    }
  }
  return NULL;
JVM_END


JVM_ENTRY(jbyteArray, JVM_GetClassAnnotations(JNIEnv *env, jclass cls))
  assert (cls != NULL, "illegal class");
  oop mirror = JNIHandles::resolve_non_null(cls);
  // Return null for arrays and primitives
  if (!java_lang_Class::is_primitive(mirror)) {
    Klass* k = java_lang_Class::as_Klass(mirror);
    if (k->is_instance_klass()) {
      typeArrayOop a = Annotations::make_java_array(InstanceKlass::cast(k)->class_annotations(), CHECK_NULL);
      return (jbyteArray) JNIHandles::make_local(THREAD, a);
    }
  }
  return NULL;
JVM_END


static bool jvm_get_field_common(jobject field, fieldDescriptor& fd) {
  // some of this code was adapted from from jni_FromReflectedField

  oop reflected = JNIHandles::resolve_non_null(field);
  oop mirror    = java_lang_reflect_Field::clazz(reflected);
  Klass* k    = java_lang_Class::as_Klass(mirror);
  int slot      = java_lang_reflect_Field::slot(reflected);
  int modifiers = java_lang_reflect_Field::modifiers(reflected);

  InstanceKlass* ik = InstanceKlass::cast(k);
  intptr_t offset = ik->field_offset(slot);

  if (modifiers & JVM_ACC_STATIC) {
    // for static fields we only look in the current class
    if (!ik->find_local_field_from_offset(offset, true, &fd)) {
      assert(false, "cannot find static field");
      return false;
    }
  } else {
    // for instance fields we start with the current class and work
    // our way up through the superclass chain
    if (!ik->find_field_from_offset(offset, false, &fd)) {
      assert(false, "cannot find instance field");
      return false;
    }
  }
  return true;
}

static Method* jvm_get_method_common(jobject method) {
  // some of this code was adapted from from jni_FromReflectedMethod

  oop reflected = JNIHandles::resolve_non_null(method);
  oop mirror    = NULL;
  int slot      = 0;

  if (reflected->klass() == vmClasses::reflect_Constructor_klass()) {
    mirror = java_lang_reflect_Constructor::clazz(reflected);
    slot   = java_lang_reflect_Constructor::slot(reflected);
  } else {
    assert(reflected->klass() == vmClasses::reflect_Method_klass(),
           "wrong type");
    mirror = java_lang_reflect_Method::clazz(reflected);
    slot   = java_lang_reflect_Method::slot(reflected);
  }
  Klass* k = java_lang_Class::as_Klass(mirror);

  Method* m = InstanceKlass::cast(k)->method_with_idnum(slot);
  assert(m != NULL, "cannot find method");
  return m;  // caller has to deal with NULL in product mode
}

/* Type use annotations support (JDK 1.8) */

JVM_ENTRY(jbyteArray, JVM_GetClassTypeAnnotations(JNIEnv *env, jclass cls))
  assert (cls != NULL, "illegal class");
  ResourceMark rm(THREAD);
  // Return null for arrays and primitives
  if (!java_lang_Class::is_primitive(JNIHandles::resolve(cls))) {
    Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve(cls));
    if (k->is_instance_klass()) {
      AnnotationArray* type_annotations = InstanceKlass::cast(k)->class_type_annotations();
      if (type_annotations != NULL) {
        typeArrayOop a = Annotations::make_java_array(type_annotations, CHECK_NULL);
        return (jbyteArray) JNIHandles::make_local(THREAD, a);
      }
    }
  }
  return NULL;
JVM_END

JVM_ENTRY(jbyteArray, JVM_GetMethodTypeAnnotations(JNIEnv *env, jobject method))
  assert (method != NULL, "illegal method");
  // method is a handle to a java.lang.reflect.Method object
  Method* m = jvm_get_method_common(method);
  if (m == NULL) {
    return NULL;
  }

  AnnotationArray* type_annotations = m->type_annotations();
  if (type_annotations != NULL) {
    typeArrayOop a = Annotations::make_java_array(type_annotations, CHECK_NULL);
    return (jbyteArray) JNIHandles::make_local(THREAD, a);
  }

  return NULL;
JVM_END

JVM_ENTRY(jbyteArray, JVM_GetFieldTypeAnnotations(JNIEnv *env, jobject field))
  assert (field != NULL, "illegal field");
  fieldDescriptor fd;
  bool gotFd = jvm_get_field_common(field, fd);
  if (!gotFd) {
    return NULL;
  }

  return (jbyteArray) JNIHandles::make_local(THREAD, Annotations::make_java_array(fd.type_annotations(), THREAD));
JVM_END

static void bounds_check(const constantPoolHandle& cp, jint index, TRAPS) {
  if (!cp->is_within_bounds(index)) {
    THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(), "Constant pool index out of bounds");
  }
}

JVM_ENTRY(jobjectArray, JVM_GetMethodParameters(JNIEnv *env, jobject method))
{
  // method is a handle to a java.lang.reflect.Method object
  Method* method_ptr = jvm_get_method_common(method);
  methodHandle mh (THREAD, method_ptr);
  Handle reflected_method (THREAD, JNIHandles::resolve_non_null(method));
  const int num_params = mh->method_parameters_length();

  if (num_params < 0) {
    // A -1 return value from method_parameters_length means there is no
    // parameter data.  Return null to indicate this to the reflection
    // API.
    assert(num_params == -1, "num_params should be -1 if it is less than zero");
    return (jobjectArray)NULL;
  } else {
    // Otherwise, we return something up to reflection, even if it is
    // a zero-length array.  Why?  Because in some cases this can
    // trigger a MalformedParametersException.

    // make sure all the symbols are properly formatted
    for (int i = 0; i < num_params; i++) {
      MethodParametersElement* params = mh->method_parameters_start();
      int index = params[i].name_cp_index;
      constantPoolHandle cp(THREAD, mh->constants());
      bounds_check(cp, index, CHECK_NULL);

      if (0 != index && !mh->constants()->tag_at(index).is_utf8()) {
        THROW_MSG_0(vmSymbols::java_lang_IllegalArgumentException(),
                    "Wrong type at constant pool index");
      }

    }

    objArrayOop result_oop = oopFactory::new_objArray(vmClasses::reflect_Parameter_klass(), num_params, CHECK_NULL);
    objArrayHandle result (THREAD, result_oop);

    for (int i = 0; i < num_params; i++) {
      MethodParametersElement* params = mh->method_parameters_start();
      // For a 0 index, give a NULL symbol
      Symbol* sym = 0 != params[i].name_cp_index ?
        mh->constants()->symbol_at(params[i].name_cp_index) : NULL;
      int flags = params[i].flags;
      oop param = Reflection::new_parameter(reflected_method, i, sym,
                                            flags, CHECK_NULL);
      result->obj_at_put(i, param);
    }
    return (jobjectArray)JNIHandles::make_local(THREAD, result());
  }
}
JVM_END

// New (JDK 1.4) reflection implementation /////////////////////////////////////

JVM_ENTRY(jobjectArray, JVM_GetClassDeclaredFields(JNIEnv *env, jclass ofClass, jboolean publicOnly))
{
  JvmtiVMObjectAllocEventCollector oam;

  oop ofMirror = JNIHandles::resolve_non_null(ofClass);
  // Exclude primitive types and array types
  if (java_lang_Class::is_primitive(ofMirror) ||
      java_lang_Class::as_Klass(ofMirror)->is_array_klass()) {
    // Return empty array
    oop res = oopFactory::new_objArray(vmClasses::reflect_Field_klass(), 0, CHECK_NULL);
    return (jobjectArray) JNIHandles::make_local(THREAD, res);
  }

  InstanceKlass* k = InstanceKlass::cast(java_lang_Class::as_Klass(ofMirror));
  constantPoolHandle cp(THREAD, k->constants());

  // Ensure class is linked
  k->link_class(CHECK_NULL);

  // Allocate result
  int num_fields;

  if (publicOnly) {
    num_fields = 0;
    for (JavaFieldStream fs(k); !fs.done(); fs.next()) {
      if (fs.access_flags().is_public()) ++num_fields;
    }
  } else {
    num_fields = k->java_fields_count();
  }

  objArrayOop r = oopFactory::new_objArray(vmClasses::reflect_Field_klass(), num_fields, CHECK_NULL);
  objArrayHandle result (THREAD, r);

  int out_idx = 0;
  fieldDescriptor fd;
  for (JavaFieldStream fs(k); !fs.done(); fs.next()) {
    if (!publicOnly || fs.access_flags().is_public()) {
      fd.reinitialize(k, fs.index());
      oop field = Reflection::new_field(&fd, CHECK_NULL);
      result->obj_at_put(out_idx, field);
      ++out_idx;
    }
  }
  assert(out_idx == num_fields, "just checking");
  return (jobjectArray) JNIHandles::make_local(THREAD, result());
}
JVM_END

// A class is a record if and only if it is final and a direct subclass of
// java.lang.Record and has a Record attribute; otherwise, it is not a record.
JVM_ENTRY(jboolean, JVM_IsRecord(JNIEnv *env, jclass cls))
{
  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(cls));
  if (k != NULL && k->is_instance_klass()) {
    InstanceKlass* ik = InstanceKlass::cast(k);
    return ik->is_record();
  } else {
    return false;
  }
}
JVM_END

// Returns an array containing the components of the Record attribute,
// or NULL if the attribute is not present.
//
// Note that this function returns the components of the Record attribute
// even if the class is not a record.
JVM_ENTRY(jobjectArray, JVM_GetRecordComponents(JNIEnv* env, jclass ofClass))
{
  Klass* c = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(ofClass));
  assert(c->is_instance_klass(), "must be");
  InstanceKlass* ik = InstanceKlass::cast(c);

  Array<RecordComponent*>* components = ik->record_components();
  if (components != NULL) {
    JvmtiVMObjectAllocEventCollector oam;
    constantPoolHandle cp(THREAD, ik->constants());
    int length = components->length();
    assert(length >= 0, "unexpected record_components length");
    objArrayOop record_components =
      oopFactory::new_objArray(vmClasses::RecordComponent_klass(), length, CHECK_NULL);
    objArrayHandle components_h (THREAD, record_components);

    for (int x = 0; x < length; x++) {
      RecordComponent* component = components->at(x);
      assert(component != NULL, "unexpected NULL record component");
      oop component_oop = java_lang_reflect_RecordComponent::create(ik, component, CHECK_NULL);
      components_h->obj_at_put(x, component_oop);
    }
    return (jobjectArray)JNIHandles::make_local(THREAD, components_h());
  }

  return NULL;
}
JVM_END

static bool select_method(const methodHandle& method, bool want_constructor) {
  if (want_constructor) {
    return (method->is_initializer() && !method->is_static());
  } else {
    return  (!method->is_initializer() && !method->is_overpass());
  }
}

static jobjectArray get_class_declared_methods_helper(
                                  JNIEnv *env,
                                  jclass ofClass, jboolean publicOnly,
                                  bool want_constructor,
                                  Klass* klass, TRAPS) {

  JvmtiVMObjectAllocEventCollector oam;

  oop ofMirror = JNIHandles::resolve_non_null(ofClass);
  // Exclude primitive types and array types
  if (java_lang_Class::is_primitive(ofMirror)
      || java_lang_Class::as_Klass(ofMirror)->is_array_klass()) {
    // Return empty array
    oop res = oopFactory::new_objArray(klass, 0, CHECK_NULL);
    return (jobjectArray) JNIHandles::make_local(THREAD, res);
  }

  InstanceKlass* k = InstanceKlass::cast(java_lang_Class::as_Klass(ofMirror));

  // Ensure class is linked
  k->link_class(CHECK_NULL);

  Array<Method*>* methods = k->methods();
  int methods_length = methods->length();

  // Save original method_idnum in case of redefinition, which can change
  // the idnum of obsolete methods.  The new method will have the same idnum
  // but if we refresh the methods array, the counts will be wrong.
  ResourceMark rm(THREAD);
  GrowableArray<int>* idnums = new GrowableArray<int>(methods_length);
  int num_methods = 0;

  for (int i = 0; i < methods_length; i++) {
    methodHandle method(THREAD, methods->at(i));
    if (select_method(method, want_constructor)) {
      if (!publicOnly || method->is_public()) {
        idnums->push(method->method_idnum());
        ++num_methods;
      }
    }
  }

  // Allocate result
  objArrayOop r = oopFactory::new_objArray(klass, num_methods, CHECK_NULL);
  objArrayHandle result (THREAD, r);

  // Now just put the methods that we selected above, but go by their idnum
  // in case of redefinition.  The methods can be redefined at any safepoint,
  // so above when allocating the oop array and below when creating reflect
  // objects.
  for (int i = 0; i < num_methods; i++) {
    methodHandle method(THREAD, k->method_with_idnum(idnums->at(i)));
    if (method.is_null()) {
      // Method may have been deleted and seems this API can handle null
      // Otherwise should probably put a method that throws NSME
      result->obj_at_put(i, NULL);
    } else {
      oop m;
      if (want_constructor) {
        m = Reflection::new_constructor(method, CHECK_NULL);
      } else {
        m = Reflection::new_method(method, false, CHECK_NULL);
      }
      result->obj_at_put(i, m);
    }
  }

  return (jobjectArray) JNIHandles::make_local(THREAD, result());
}

JVM_ENTRY(jobjectArray, JVM_GetClassDeclaredMethods(JNIEnv *env, jclass ofClass, jboolean publicOnly))
{
  return get_class_declared_methods_helper(env, ofClass, publicOnly,
                                           /*want_constructor*/ false,
                                           vmClasses::reflect_Method_klass(), THREAD);
}
JVM_END

JVM_ENTRY(jobjectArray, JVM_GetClassDeclaredConstructors(JNIEnv *env, jclass ofClass, jboolean publicOnly))
{
  return get_class_declared_methods_helper(env, ofClass, publicOnly,
                                           /*want_constructor*/ true,
                                           vmClasses::reflect_Constructor_klass(), THREAD);
}
JVM_END

JVM_ENTRY(jint, JVM_GetClassAccessFlags(JNIEnv *env, jclass cls))
{
  oop mirror = JNIHandles::resolve_non_null(cls);
  if (java_lang_Class::is_primitive(mirror)) {
    // Primitive type
    return JVM_ACC_ABSTRACT | JVM_ACC_FINAL | JVM_ACC_PUBLIC;
  }

  Klass* k = java_lang_Class::as_Klass(mirror);
  return k->access_flags().as_int() & JVM_ACC_WRITTEN_FLAGS;
}
JVM_END

JVM_ENTRY(jboolean, JVM_AreNestMates(JNIEnv *env, jclass current, jclass member))
{
  Klass* c = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(current));
  assert(c->is_instance_klass(), "must be");
  InstanceKlass* ck = InstanceKlass::cast(c);
  Klass* m = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(member));
  assert(m->is_instance_klass(), "must be");
  InstanceKlass* mk = InstanceKlass::cast(m);
  return ck->has_nestmate_access_to(mk, THREAD);
}
JVM_END

JVM_ENTRY(jclass, JVM_GetNestHost(JNIEnv* env, jclass current))
{
  // current is not a primitive or array class
  Klass* c = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(current));
  assert(c->is_instance_klass(), "must be");
  InstanceKlass* ck = InstanceKlass::cast(c);
  InstanceKlass* host = ck->nest_host(THREAD);
  return (jclass) (host == NULL ? NULL :
                   JNIHandles::make_local(THREAD, host->java_mirror()));
}
JVM_END

JVM_ENTRY(jobjectArray, JVM_GetNestMembers(JNIEnv* env, jclass current))
{
  // current is not a primitive or array class
  ResourceMark rm(THREAD);
  Klass* c = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(current));
  assert(c->is_instance_klass(), "must be");
  InstanceKlass* ck = InstanceKlass::cast(c);
  InstanceKlass* host = ck->nest_host(THREAD);

  log_trace(class, nestmates)("Calling GetNestMembers for type %s with nest-host %s",
                              ck->external_name(), host->external_name());
  {
    JvmtiVMObjectAllocEventCollector oam;
    Array<u2>* members = host->nest_members();
    int length = members == NULL ? 0 : members->length();

    log_trace(class, nestmates)(" - host has %d listed nest members", length);

    // nest host is first in the array so make it one bigger
    objArrayOop r = oopFactory::new_objArray(vmClasses::Class_klass(),
                                             length + 1, CHECK_NULL);
    objArrayHandle result(THREAD, r);
    result->obj_at_put(0, host->java_mirror());
    if (length != 0) {
      int count = 0;
      for (int i = 0; i < length; i++) {
        int cp_index = members->at(i);
        Klass* k = host->constants()->klass_at(cp_index, THREAD);
        if (HAS_PENDING_EXCEPTION) {
          if (PENDING_EXCEPTION->is_a(vmClasses::VirtualMachineError_klass())) {
            return NULL; // propagate VMEs
          }
          if (log_is_enabled(Trace, class, nestmates)) {
            stringStream ss;
            char* target_member_class = host->constants()->klass_name_at(cp_index)->as_C_string();
            ss.print(" - resolution of nest member %s failed: ", target_member_class);
            java_lang_Throwable::print(PENDING_EXCEPTION, &ss);
            log_trace(class, nestmates)("%s", ss.as_string());
          }
          CLEAR_PENDING_EXCEPTION;
          continue;
        }
        if (k->is_instance_klass()) {
          InstanceKlass* ik = InstanceKlass::cast(k);
          InstanceKlass* nest_host_k = ik->nest_host(CHECK_NULL);
          if (nest_host_k == host) {
            result->obj_at_put(count+1, k->java_mirror());
            count++;
            log_trace(class, nestmates)(" - [%d] = %s", count, ik->external_name());
          } else {
            log_trace(class, nestmates)(" - skipping member %s with different host %s",
                                        ik->external_name(), nest_host_k->external_name());
          }
        } else {
          log_trace(class, nestmates)(" - skipping member %s that is not an instance class",
                                      k->external_name());
        }
      }
      if (count < length) {
        // we had invalid entries so we need to compact the array
        log_trace(class, nestmates)(" - compacting array from length %d to %d",
                                    length + 1, count + 1);

        objArrayOop r2 = oopFactory::new_objArray(vmClasses::Class_klass(),
                                                  count + 1, CHECK_NULL);
        objArrayHandle result2(THREAD, r2);
        for (int i = 0; i < count + 1; i++) {
          result2->obj_at_put(i, result->obj_at(i));
        }
        return (jobjectArray)JNIHandles::make_local(THREAD, result2());
      }
    }
    else {
      assert(host == ck || ck->is_hidden(), "must be singleton nest or dynamic nestmate");
    }
    return (jobjectArray)JNIHandles::make_local(THREAD, result());
  }
}
JVM_END

JVM_ENTRY(jobjectArray, JVM_GetPermittedSubclasses(JNIEnv* env, jclass current))
{
  oop mirror = JNIHandles::resolve_non_null(current);
  assert(!java_lang_Class::is_primitive(mirror), "should not be");
  Klass* c = java_lang_Class::as_Klass(mirror);
  assert(c->is_instance_klass(), "must be");
  InstanceKlass* ik = InstanceKlass::cast(c);
  ResourceMark rm(THREAD);
  log_trace(class, sealed)("Calling GetPermittedSubclasses for %s type %s",
                           ik->is_sealed() ? "sealed" : "non-sealed", ik->external_name());
  if (ik->is_sealed()) {
    JvmtiVMObjectAllocEventCollector oam;
    Array<u2>* subclasses = ik->permitted_subclasses();
    int length = subclasses->length();

    log_trace(class, sealed)(" - sealed class has %d permitted subclasses", length);

    objArrayOop r = oopFactory::new_objArray(vmClasses::Class_klass(),
                                             length, CHECK_NULL);
    objArrayHandle result(THREAD, r);
    int count = 0;
    for (int i = 0; i < length; i++) {
      int cp_index = subclasses->at(i);
      Klass* k = ik->constants()->klass_at(cp_index, THREAD);
      if (HAS_PENDING_EXCEPTION) {
        if (PENDING_EXCEPTION->is_a(vmClasses::VirtualMachineError_klass())) {
          return NULL; // propagate VMEs
        }
        if (log_is_enabled(Trace, class, sealed)) {
          stringStream ss;
          char* permitted_subclass = ik->constants()->klass_name_at(cp_index)->as_C_string();
          ss.print(" - resolution of permitted subclass %s failed: ", permitted_subclass);
          java_lang_Throwable::print(PENDING_EXCEPTION, &ss);
          log_trace(class, sealed)("%s", ss.as_string());
        }

        CLEAR_PENDING_EXCEPTION;
        continue;
      }
      if (k->is_instance_klass()) {
        result->obj_at_put(count++, k->java_mirror());
        log_trace(class, sealed)(" - [%d] = %s", count, k->external_name());
      }
    }
    if (count < length) {
      // we had invalid entries so we need to compact the array
      objArrayOop r2 = oopFactory::new_objArray(vmClasses::Class_klass(),
                                                count, CHECK_NULL);
      objArrayHandle result2(THREAD, r2);
      for (int i = 0; i < count; i++) {
        result2->obj_at_put(i, result->obj_at(i));
      }
      return (jobjectArray)JNIHandles::make_local(THREAD, result2());
    }
    return (jobjectArray)JNIHandles::make_local(THREAD, result());
  } else {
    return NULL;
  }
}
JVM_END

// Constant pool access //////////////////////////////////////////////////////////

JVM_ENTRY(jobject, JVM_GetClassConstantPool(JNIEnv *env, jclass cls))
{
  JvmtiVMObjectAllocEventCollector oam;
  oop mirror = JNIHandles::resolve_non_null(cls);
  // Return null for primitives and arrays
  if (!java_lang_Class::is_primitive(mirror)) {
    Klass* k = java_lang_Class::as_Klass(mirror);
    if (k->is_instance_klass()) {
      InstanceKlass* k_h = InstanceKlass::cast(k);
      Handle jcp = reflect_ConstantPool::create(CHECK_NULL);
      reflect_ConstantPool::set_cp(jcp(), k_h->constants());
      return JNIHandles::make_local(THREAD, jcp());
    }
  }
  return NULL;
}
JVM_END


JVM_ENTRY(jint, JVM_ConstantPoolGetSize(JNIEnv *env, jobject obj, jobject unused))
{
  constantPoolHandle cp = constantPoolHandle(THREAD, reflect_ConstantPool::get_cp(JNIHandles::resolve_non_null(obj)));
  return cp->length();
}
JVM_END


JVM_ENTRY(jclass, JVM_ConstantPoolGetClassAt(JNIEnv *env, jobject obj, jobject unused, jint index))
{
  constantPoolHandle cp = constantPoolHandle(THREAD, reflect_ConstantPool::get_cp(JNIHandles::resolve_non_null(obj)));
  bounds_check(cp, index, CHECK_NULL);
  constantTag tag = cp->tag_at(index);
  if (!tag.is_klass() && !tag.is_unresolved_klass()) {
    THROW_MSG_0(vmSymbols::java_lang_IllegalArgumentException(), "Wrong type at constant pool index");
  }
  Klass* k = cp->klass_at(index, CHECK_NULL);
  return (jclass) JNIHandles::make_local(THREAD, k->java_mirror());
}
JVM_END

JVM_ENTRY(jclass, JVM_ConstantPoolGetClassAtIfLoaded(JNIEnv *env, jobject obj, jobject unused, jint index))
{
  constantPoolHandle cp = constantPoolHandle(THREAD, reflect_ConstantPool::get_cp(JNIHandles::resolve_non_null(obj)));
  bounds_check(cp, index, CHECK_NULL);
  constantTag tag = cp->tag_at(index);
  if (!tag.is_klass() && !tag.is_unresolved_klass()) {
    THROW_MSG_0(vmSymbols::java_lang_IllegalArgumentException(), "Wrong type at constant pool index");
  }
  Klass* k = ConstantPool::klass_at_if_loaded(cp, index);
  if (k == NULL) return NULL;
  return (jclass) JNIHandles::make_local(THREAD, k->java_mirror());
}
JVM_END

static jobject get_method_at_helper(const constantPoolHandle& cp, jint index, bool force_resolution, TRAPS) {
  constantTag tag = cp->tag_at(index);
  if (!tag.is_method() && !tag.is_interface_method()) {
    THROW_MSG_0(vmSymbols::java_lang_IllegalArgumentException(), "Wrong type at constant pool index");
  }
  int klass_ref  = cp->uncached_klass_ref_index_at(index);
  Klass* k_o;
  if (force_resolution) {
    k_o = cp->klass_at(klass_ref, CHECK_NULL);
  } else {
    k_o = ConstantPool::klass_at_if_loaded(cp, klass_ref);
    if (k_o == NULL) return NULL;
  }
  InstanceKlass* k = InstanceKlass::cast(k_o);
  Symbol* name = cp->uncached_name_ref_at(index);
  Symbol* sig  = cp->uncached_signature_ref_at(index);
  methodHandle m (THREAD, k->find_method(name, sig));
  if (m.is_null()) {
    THROW_MSG_0(vmSymbols::java_lang_RuntimeException(), "Unable to look up method in target class");
  }
  oop method;
  if (!m->is_initializer() || m->is_static()) {
    method = Reflection::new_method(m, true, CHECK_NULL);
  } else {
    method = Reflection::new_constructor(m, CHECK_NULL);
  }
  return JNIHandles::make_local(THREAD, method);
}

JVM_ENTRY(jobject, JVM_ConstantPoolGetMethodAt(JNIEnv *env, jobject obj, jobject unused, jint index))
{
  JvmtiVMObjectAllocEventCollector oam;
  constantPoolHandle cp = constantPoolHandle(THREAD, reflect_ConstantPool::get_cp(JNIHandles::resolve_non_null(obj)));
  bounds_check(cp, index, CHECK_NULL);
  jobject res = get_method_at_helper(cp, index, true, CHECK_NULL);
  return res;
}
JVM_END

JVM_ENTRY(jobject, JVM_ConstantPoolGetMethodAtIfLoaded(JNIEnv *env, jobject obj, jobject unused, jint index))
{
  JvmtiVMObjectAllocEventCollector oam;
  constantPoolHandle cp = constantPoolHandle(THREAD, reflect_ConstantPool::get_cp(JNIHandles::resolve_non_null(obj)));
  bounds_check(cp, index, CHECK_NULL);
  jobject res = get_method_at_helper(cp, index, false, CHECK_NULL);
  return res;
}
JVM_END

static jobject get_field_at_helper(constantPoolHandle cp, jint index, bool force_resolution, TRAPS) {
  constantTag tag = cp->tag_at(index);
  if (!tag.is_field()) {
    THROW_MSG_0(vmSymbols::java_lang_IllegalArgumentException(), "Wrong type at constant pool index");
  }
  int klass_ref  = cp->uncached_klass_ref_index_at(index);
  Klass* k_o;
  if (force_resolution) {
    k_o = cp->klass_at(klass_ref, CHECK_NULL);
  } else {
    k_o = ConstantPool::klass_at_if_loaded(cp, klass_ref);
    if (k_o == NULL) return NULL;
  }
  InstanceKlass* k = InstanceKlass::cast(k_o);
  Symbol* name = cp->uncached_name_ref_at(index);
  Symbol* sig  = cp->uncached_signature_ref_at(index);
  fieldDescriptor fd;
  Klass* target_klass = k->find_field(name, sig, &fd);
  if (target_klass == NULL) {
    THROW_MSG_0(vmSymbols::java_lang_RuntimeException(), "Unable to look up field in target class");
  }
  oop field = Reflection::new_field(&fd, CHECK_NULL);
  return JNIHandles::make_local(THREAD, field);
}

JVM_ENTRY(jobject, JVM_ConstantPoolGetFieldAt(JNIEnv *env, jobject obj, jobject unusedl, jint index))
{
  JvmtiVMObjectAllocEventCollector oam;
  constantPoolHandle cp = constantPoolHandle(THREAD, reflect_ConstantPool::get_cp(JNIHandles::resolve_non_null(obj)));
  bounds_check(cp, index, CHECK_NULL);
  jobject res = get_field_at_helper(cp, index, true, CHECK_NULL);
  return res;
}
JVM_END

JVM_ENTRY(jobject, JVM_ConstantPoolGetFieldAtIfLoaded(JNIEnv *env, jobject obj, jobject unused, jint index))
{
  JvmtiVMObjectAllocEventCollector oam;
  constantPoolHandle cp = constantPoolHandle(THREAD, reflect_ConstantPool::get_cp(JNIHandles::resolve_non_null(obj)));
  bounds_check(cp, index, CHECK_NULL);
  jobject res = get_field_at_helper(cp, index, false, CHECK_NULL);
  return res;
}
JVM_END

JVM_ENTRY(jobjectArray, JVM_ConstantPoolGetMemberRefInfoAt(JNIEnv *env, jobject obj, jobject unused, jint index))
{
  JvmtiVMObjectAllocEventCollector oam;
  constantPoolHandle cp = constantPoolHandle(THREAD, reflect_ConstantPool::get_cp(JNIHandles::resolve_non_null(obj)));
  bounds_check(cp, index, CHECK_NULL);
  constantTag tag = cp->tag_at(index);
  if (!tag.is_field_or_method()) {
    THROW_MSG_0(vmSymbols::java_lang_IllegalArgumentException(), "Wrong type at constant pool index");
  }
  int klass_ref = cp->uncached_klass_ref_index_at(index);
  Symbol*  klass_name  = cp->klass_name_at(klass_ref);
  Symbol*  member_name = cp->uncached_name_ref_at(index);
  Symbol*  member_sig  = cp->uncached_signature_ref_at(index);
  objArrayOop  dest_o = oopFactory::new_objArray(vmClasses::String_klass(), 3, CHECK_NULL);
  objArrayHandle dest(THREAD, dest_o);
  Handle str = java_lang_String::create_from_symbol(klass_name, CHECK_NULL);
  dest->obj_at_put(0, str());
  str = java_lang_String::create_from_symbol(member_name, CHECK_NULL);
  dest->obj_at_put(1, str());
  str = java_lang_String::create_from_symbol(member_sig, CHECK_NULL);
  dest->obj_at_put(2, str());
  return (jobjectArray) JNIHandles::make_local(THREAD, dest());
}
JVM_END

JVM_ENTRY(jint, JVM_ConstantPoolGetClassRefIndexAt(JNIEnv *env, jobject obj, jobject unused, jint index))
{
  JvmtiVMObjectAllocEventCollector oam;
  constantPoolHandle cp(THREAD, reflect_ConstantPool::get_cp(JNIHandles::resolve_non_null(obj)));
  bounds_check(cp, index, CHECK_0);
  constantTag tag = cp->tag_at(index);
  if (!tag.is_field_or_method()) {
    THROW_MSG_0(vmSymbols::java_lang_IllegalArgumentException(), "Wrong type at constant pool index");
  }
  return (jint) cp->uncached_klass_ref_index_at(index);
}
JVM_END

JVM_ENTRY(jint, JVM_ConstantPoolGetNameAndTypeRefIndexAt(JNIEnv *env, jobject obj, jobject unused, jint index))
{
  JvmtiVMObjectAllocEventCollector oam;
  constantPoolHandle cp(THREAD, reflect_ConstantPool::get_cp(JNIHandles::resolve_non_null(obj)));
  bounds_check(cp, index, CHECK_0);
  constantTag tag = cp->tag_at(index);
  if (!tag.is_invoke_dynamic() && !tag.is_field_or_method()) {
    THROW_MSG_0(vmSymbols::java_lang_IllegalArgumentException(), "Wrong type at constant pool index");
  }
  return (jint) cp->uncached_name_and_type_ref_index_at(index);
}
JVM_END

JVM_ENTRY(jobjectArray, JVM_ConstantPoolGetNameAndTypeRefInfoAt(JNIEnv *env, jobject obj, jobject unused, jint index))
{
  JvmtiVMObjectAllocEventCollector oam;
  constantPoolHandle cp(THREAD, reflect_ConstantPool::get_cp(JNIHandles::resolve_non_null(obj)));
  bounds_check(cp, index, CHECK_NULL);
  constantTag tag = cp->tag_at(index);
  if (!tag.is_name_and_type()) {
    THROW_MSG_0(vmSymbols::java_lang_IllegalArgumentException(), "Wrong type at constant pool index");
  }
  Symbol* member_name = cp->symbol_at(cp->name_ref_index_at(index));
  Symbol* member_sig = cp->symbol_at(cp->signature_ref_index_at(index));
  objArrayOop dest_o = oopFactory::new_objArray(vmClasses::String_klass(), 2, CHECK_NULL);
  objArrayHandle dest(THREAD, dest_o);
  Handle str = java_lang_String::create_from_symbol(member_name, CHECK_NULL);
  dest->obj_at_put(0, str());
  str = java_lang_String::create_from_symbol(member_sig, CHECK_NULL);
  dest->obj_at_put(1, str());
  return (jobjectArray) JNIHandles::make_local(THREAD, dest());
}
JVM_END

JVM_ENTRY(jint, JVM_ConstantPoolGetIntAt(JNIEnv *env, jobject obj, jobject unused, jint index))
{
  constantPoolHandle cp = constantPoolHandle(THREAD, reflect_ConstantPool::get_cp(JNIHandles::resolve_non_null(obj)));
  bounds_check(cp, index, CHECK_0);
  constantTag tag = cp->tag_at(index);
  if (!tag.is_int()) {
    THROW_MSG_0(vmSymbols::java_lang_IllegalArgumentException(), "Wrong type at constant pool index");
  }
  return cp->int_at(index);
}
JVM_END

JVM_ENTRY(jlong, JVM_ConstantPoolGetLongAt(JNIEnv *env, jobject obj, jobject unused, jint index))
{
  constantPoolHandle cp = constantPoolHandle(THREAD, reflect_ConstantPool::get_cp(JNIHandles::resolve_non_null(obj)));
  bounds_check(cp, index, CHECK_(0L));
  constantTag tag = cp->tag_at(index);
  if (!tag.is_long()) {
    THROW_MSG_0(vmSymbols::java_lang_IllegalArgumentException(), "Wrong type at constant pool index");
  }
  return cp->long_at(index);
}
JVM_END

JVM_ENTRY(jfloat, JVM_ConstantPoolGetFloatAt(JNIEnv *env, jobject obj, jobject unused, jint index))
{
  constantPoolHandle cp = constantPoolHandle(THREAD, reflect_ConstantPool::get_cp(JNIHandles::resolve_non_null(obj)));
  bounds_check(cp, index, CHECK_(0.0f));
  constantTag tag = cp->tag_at(index);
  if (!tag.is_float()) {
    THROW_MSG_0(vmSymbols::java_lang_IllegalArgumentException(), "Wrong type at constant pool index");
  }
  return cp->float_at(index);
}
JVM_END

JVM_ENTRY(jdouble, JVM_ConstantPoolGetDoubleAt(JNIEnv *env, jobject obj, jobject unused, jint index))
{
  constantPoolHandle cp = constantPoolHandle(THREAD, reflect_ConstantPool::get_cp(JNIHandles::resolve_non_null(obj)));
  bounds_check(cp, index, CHECK_(0.0));
  constantTag tag = cp->tag_at(index);
  if (!tag.is_double()) {
    THROW_MSG_0(vmSymbols::java_lang_IllegalArgumentException(), "Wrong type at constant pool index");
  }
  return cp->double_at(index);
}
JVM_END

JVM_ENTRY(jstring, JVM_ConstantPoolGetStringAt(JNIEnv *env, jobject obj, jobject unused, jint index))
{
  constantPoolHandle cp = constantPoolHandle(THREAD, reflect_ConstantPool::get_cp(JNIHandles::resolve_non_null(obj)));
  bounds_check(cp, index, CHECK_NULL);
  constantTag tag = cp->tag_at(index);
  if (!tag.is_string()) {
    THROW_MSG_0(vmSymbols::java_lang_IllegalArgumentException(), "Wrong type at constant pool index");
  }
  oop str = cp->string_at(index, CHECK_NULL);
  return (jstring) JNIHandles::make_local(THREAD, str);
}
JVM_END

JVM_ENTRY(jstring, JVM_ConstantPoolGetUTF8At(JNIEnv *env, jobject obj, jobject unused, jint index))
{
  JvmtiVMObjectAllocEventCollector oam;
  constantPoolHandle cp = constantPoolHandle(THREAD, reflect_ConstantPool::get_cp(JNIHandles::resolve_non_null(obj)));
  bounds_check(cp, index, CHECK_NULL);
  constantTag tag = cp->tag_at(index);
  if (!tag.is_symbol()) {
    THROW_MSG_0(vmSymbols::java_lang_IllegalArgumentException(), "Wrong type at constant pool index");
  }
  Symbol* sym = cp->symbol_at(index);
  Handle str = java_lang_String::create_from_symbol(sym, CHECK_NULL);
  return (jstring) JNIHandles::make_local(THREAD, str());
}
JVM_END

JVM_ENTRY(jbyte, JVM_ConstantPoolGetTagAt(JNIEnv *env, jobject obj, jobject unused, jint index))
{
  constantPoolHandle cp = constantPoolHandle(THREAD, reflect_ConstantPool::get_cp(JNIHandles::resolve_non_null(obj)));
  bounds_check(cp, index, CHECK_0);
  constantTag tag = cp->tag_at(index);
  jbyte result = tag.value();
  // If returned tag values are not from the JVM spec, e.g. tags from 100 to 105,
  // they are changed to the corresponding tags from the JVM spec, so that java code in
  // sun.reflect.ConstantPool will return only tags from the JVM spec, not internal ones.
  if (tag.is_klass_or_reference()) {
      result = JVM_CONSTANT_Class;
  } else if (tag.is_string_index()) {
      result = JVM_CONSTANT_String;
  } else if (tag.is_method_type_in_error()) {
      result = JVM_CONSTANT_MethodType;
  } else if (tag.is_method_handle_in_error()) {
      result = JVM_CONSTANT_MethodHandle;
  } else if (tag.is_dynamic_constant_in_error()) {
      result = JVM_CONSTANT_Dynamic;
  }
  return result;
}
JVM_END

// Assertion support. //////////////////////////////////////////////////////////

JVM_ENTRY(jboolean, JVM_DesiredAssertionStatus(JNIEnv *env, jclass unused, jclass cls))
  assert(cls != NULL, "bad class");

  oop r = JNIHandles::resolve(cls);
  assert(! java_lang_Class::is_primitive(r), "primitive classes not allowed");
  if (java_lang_Class::is_primitive(r)) return false;

  Klass* k = java_lang_Class::as_Klass(r);
  assert(k->is_instance_klass(), "must be an instance klass");
  if (!k->is_instance_klass()) return false;

  ResourceMark rm(THREAD);
  const char* name = k->name()->as_C_string();
  bool system_class = k->class_loader() == NULL;
  return JavaAssertions::enabled(name, system_class);

JVM_END


// Return a new AssertionStatusDirectives object with the fields filled in with
// command-line assertion arguments (i.e., -ea, -da).
JVM_ENTRY(jobject, JVM_AssertionStatusDirectives(JNIEnv *env, jclass unused))
  JvmtiVMObjectAllocEventCollector oam;
  oop asd = JavaAssertions::createAssertionStatusDirectives(CHECK_NULL);
  return JNIHandles::make_local(THREAD, asd);
JVM_END

// Verification ////////////////////////////////////////////////////////////////////////////////

// Reflection for the verifier /////////////////////////////////////////////////////////////////

// RedefineClasses support: bug 6214132 caused verification to fail.
// All functions from this section should call the jvmtiThreadSate function:
//   Klass* class_to_verify_considering_redefinition(Klass* klass).
// The function returns a Klass* of the _scratch_class if the verifier
// was invoked in the middle of the class redefinition.
// Otherwise it returns its argument value which is the _the_class Klass*.
// Please, refer to the description in the jvmtiThreadSate.hpp.

JVM_ENTRY(const char*, JVM_GetClassNameUTF(JNIEnv *env, jclass cls))
  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(cls));
  k = JvmtiThreadState::class_to_verify_considering_redefinition(k, thread);
  return k->name()->as_utf8();
JVM_END


JVM_ENTRY(void, JVM_GetClassCPTypes(JNIEnv *env, jclass cls, unsigned char *types))
  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(cls));
  k = JvmtiThreadState::class_to_verify_considering_redefinition(k, thread);
  // types will have length zero if this is not an InstanceKlass
  // (length is determined by call to JVM_GetClassCPEntriesCount)
  if (k->is_instance_klass()) {
    ConstantPool* cp = InstanceKlass::cast(k)->constants();
    for (int index = cp->length() - 1; index >= 0; index--) {
      constantTag tag = cp->tag_at(index);
      types[index] = (tag.is_unresolved_klass()) ? (unsigned char) JVM_CONSTANT_Class : tag.value();
    }
  }
JVM_END


JVM_ENTRY(jint, JVM_GetClassCPEntriesCount(JNIEnv *env, jclass cls))
  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(cls));
  k = JvmtiThreadState::class_to_verify_considering_redefinition(k, thread);
  return (!k->is_instance_klass()) ? 0 : InstanceKlass::cast(k)->constants()->length();
JVM_END


JVM_ENTRY(jint, JVM_GetClassFieldsCount(JNIEnv *env, jclass cls))
  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(cls));
  k = JvmtiThreadState::class_to_verify_considering_redefinition(k, thread);
  return (!k->is_instance_klass()) ? 0 : InstanceKlass::cast(k)->java_fields_count();
JVM_END


JVM_ENTRY(jint, JVM_GetClassMethodsCount(JNIEnv *env, jclass cls))
  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(cls));
  k = JvmtiThreadState::class_to_verify_considering_redefinition(k, thread);
  return (!k->is_instance_klass()) ? 0 : InstanceKlass::cast(k)->methods()->length();
JVM_END


// The following methods, used for the verifier, are never called with
// array klasses, so a direct cast to InstanceKlass is safe.
// Typically, these methods are called in a loop with bounds determined
// by the results of JVM_GetClass{Fields,Methods}Count, which return
// zero for arrays.
JVM_ENTRY(void, JVM_GetMethodIxExceptionIndexes(JNIEnv *env, jclass cls, jint method_index, unsigned short *exceptions))
  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(cls));
  k = JvmtiThreadState::class_to_verify_considering_redefinition(k, thread);
  Method* method = InstanceKlass::cast(k)->methods()->at(method_index);
  int length = method->checked_exceptions_length();
  if (length > 0) {
    CheckedExceptionElement* table= method->checked_exceptions_start();
    for (int i = 0; i < length; i++) {
      exceptions[i] = table[i].class_cp_index;
    }
  }
JVM_END


JVM_ENTRY(jint, JVM_GetMethodIxExceptionsCount(JNIEnv *env, jclass cls, jint method_index))
  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(cls));
  k = JvmtiThreadState::class_to_verify_considering_redefinition(k, thread);
  Method* method = InstanceKlass::cast(k)->methods()->at(method_index);
  return method->checked_exceptions_length();
JVM_END


JVM_ENTRY(void, JVM_GetMethodIxByteCode(JNIEnv *env, jclass cls, jint method_index, unsigned char *code))
  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(cls));
  k = JvmtiThreadState::class_to_verify_considering_redefinition(k, thread);
  Method* method = InstanceKlass::cast(k)->methods()->at(method_index);
  memcpy(code, method->code_base(), method->code_size());
JVM_END


JVM_ENTRY(jint, JVM_GetMethodIxByteCodeLength(JNIEnv *env, jclass cls, jint method_index))
  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(cls));
  k = JvmtiThreadState::class_to_verify_considering_redefinition(k, thread);
  Method* method = InstanceKlass::cast(k)->methods()->at(method_index);
  return method->code_size();
JVM_END


JVM_ENTRY(void, JVM_GetMethodIxExceptionTableEntry(JNIEnv *env, jclass cls, jint method_index, jint entry_index, JVM_ExceptionTableEntryType *entry))
  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(cls));
  k = JvmtiThreadState::class_to_verify_considering_redefinition(k, thread);
  Method* method = InstanceKlass::cast(k)->methods()->at(method_index);
  ExceptionTable extable(method);
  entry->start_pc   = extable.start_pc(entry_index);
  entry->end_pc     = extable.end_pc(entry_index);
  entry->handler_pc = extable.handler_pc(entry_index);
  entry->catchType  = extable.catch_type_index(entry_index);
JVM_END


JVM_ENTRY(jint, JVM_GetMethodIxExceptionTableLength(JNIEnv *env, jclass cls, int method_index))
  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(cls));
  k = JvmtiThreadState::class_to_verify_considering_redefinition(k, thread);
  Method* method = InstanceKlass::cast(k)->methods()->at(method_index);
  return method->exception_table_length();
JVM_END


JVM_ENTRY(jint, JVM_GetMethodIxModifiers(JNIEnv *env, jclass cls, int method_index))
  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(cls));
  k = JvmtiThreadState::class_to_verify_considering_redefinition(k, thread);
  Method* method = InstanceKlass::cast(k)->methods()->at(method_index);
  return method->access_flags().as_int() & JVM_RECOGNIZED_METHOD_MODIFIERS;
JVM_END


JVM_ENTRY(jint, JVM_GetFieldIxModifiers(JNIEnv *env, jclass cls, int field_index))
  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(cls));
  k = JvmtiThreadState::class_to_verify_considering_redefinition(k, thread);
  return InstanceKlass::cast(k)->field_access_flags(field_index) & JVM_RECOGNIZED_FIELD_MODIFIERS;
JVM_END


JVM_ENTRY(jint, JVM_GetMethodIxLocalsCount(JNIEnv *env, jclass cls, int method_index))
  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(cls));
  k = JvmtiThreadState::class_to_verify_considering_redefinition(k, thread);
  Method* method = InstanceKlass::cast(k)->methods()->at(method_index);
  return method->max_locals();
JVM_END


JVM_ENTRY(jint, JVM_GetMethodIxArgsSize(JNIEnv *env, jclass cls, int method_index))
  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(cls));
  k = JvmtiThreadState::class_to_verify_considering_redefinition(k, thread);
  Method* method = InstanceKlass::cast(k)->methods()->at(method_index);
  return method->size_of_parameters();
JVM_END


JVM_ENTRY(jint, JVM_GetMethodIxMaxStack(JNIEnv *env, jclass cls, int method_index))
  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(cls));
  k = JvmtiThreadState::class_to_verify_considering_redefinition(k, thread);
  Method* method = InstanceKlass::cast(k)->methods()->at(method_index);
  return method->verifier_max_stack();
JVM_END


JVM_ENTRY(jboolean, JVM_IsConstructorIx(JNIEnv *env, jclass cls, int method_index))
  ResourceMark rm(THREAD);
  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(cls));
  k = JvmtiThreadState::class_to_verify_considering_redefinition(k, thread);
  Method* method = InstanceKlass::cast(k)->methods()->at(method_index);
  return method->name() == vmSymbols::object_initializer_name();
JVM_END


JVM_ENTRY(jboolean, JVM_IsVMGeneratedMethodIx(JNIEnv *env, jclass cls, int method_index))
  ResourceMark rm(THREAD);
  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(cls));
  k = JvmtiThreadState::class_to_verify_considering_redefinition(k, thread);
  Method* method = InstanceKlass::cast(k)->methods()->at(method_index);
  return method->is_overpass();
JVM_END

JVM_ENTRY(const char*, JVM_GetMethodIxNameUTF(JNIEnv *env, jclass cls, jint method_index))
  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(cls));
  k = JvmtiThreadState::class_to_verify_considering_redefinition(k, thread);
  Method* method = InstanceKlass::cast(k)->methods()->at(method_index);
  return method->name()->as_utf8();
JVM_END


JVM_ENTRY(const char*, JVM_GetMethodIxSignatureUTF(JNIEnv *env, jclass cls, jint method_index))
  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(cls));
  k = JvmtiThreadState::class_to_verify_considering_redefinition(k, thread);
  Method* method = InstanceKlass::cast(k)->methods()->at(method_index);
  return method->signature()->as_utf8();
JVM_END

/**
 * All of these JVM_GetCP-xxx methods are used by the old verifier to
 * read entries in the constant pool.  Since the old verifier always
 * works on a copy of the code, it will not see any rewriting that
 * may possibly occur in the middle of verification.  So it is important
 * that nothing it calls tries to use the cpCache instead of the raw
 * constant pool, so we must use cp->uncached_x methods when appropriate.
 */
JVM_ENTRY(const char*, JVM_GetCPFieldNameUTF(JNIEnv *env, jclass cls, jint cp_index))
  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(cls));
  k = JvmtiThreadState::class_to_verify_considering_redefinition(k, thread);
  ConstantPool* cp = InstanceKlass::cast(k)->constants();
  switch (cp->tag_at(cp_index).value()) {
    case JVM_CONSTANT_Fieldref:
      return cp->uncached_name_ref_at(cp_index)->as_utf8();
    default:
      fatal("JVM_GetCPFieldNameUTF: illegal constant");
  }
  ShouldNotReachHere();
  return NULL;
JVM_END


JVM_ENTRY(const char*, JVM_GetCPMethodNameUTF(JNIEnv *env, jclass cls, jint cp_index))
  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(cls));
  k = JvmtiThreadState::class_to_verify_considering_redefinition(k, thread);
  ConstantPool* cp = InstanceKlass::cast(k)->constants();
  switch (cp->tag_at(cp_index).value()) {
    case JVM_CONSTANT_InterfaceMethodref:
    case JVM_CONSTANT_Methodref:
      return cp->uncached_name_ref_at(cp_index)->as_utf8();
    default:
      fatal("JVM_GetCPMethodNameUTF: illegal constant");
  }
  ShouldNotReachHere();
  return NULL;
JVM_END


JVM_ENTRY(const char*, JVM_GetCPMethodSignatureUTF(JNIEnv *env, jclass cls, jint cp_index))
  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(cls));
  k = JvmtiThreadState::class_to_verify_considering_redefinition(k, thread);
  ConstantPool* cp = InstanceKlass::cast(k)->constants();
  switch (cp->tag_at(cp_index).value()) {
    case JVM_CONSTANT_InterfaceMethodref:
    case JVM_CONSTANT_Methodref:
      return cp->uncached_signature_ref_at(cp_index)->as_utf8();
    default:
      fatal("JVM_GetCPMethodSignatureUTF: illegal constant");
  }
  ShouldNotReachHere();
  return NULL;
JVM_END


JVM_ENTRY(const char*, JVM_GetCPFieldSignatureUTF(JNIEnv *env, jclass cls, jint cp_index))
  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(cls));
  k = JvmtiThreadState::class_to_verify_considering_redefinition(k, thread);
  ConstantPool* cp = InstanceKlass::cast(k)->constants();
  switch (cp->tag_at(cp_index).value()) {
    case JVM_CONSTANT_Fieldref:
      return cp->uncached_signature_ref_at(cp_index)->as_utf8();
    default:
      fatal("JVM_GetCPFieldSignatureUTF: illegal constant");
  }
  ShouldNotReachHere();
  return NULL;
JVM_END


JVM_ENTRY(const char*, JVM_GetCPClassNameUTF(JNIEnv *env, jclass cls, jint cp_index))
  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(cls));
  k = JvmtiThreadState::class_to_verify_considering_redefinition(k, thread);
  ConstantPool* cp = InstanceKlass::cast(k)->constants();
  Symbol* classname = cp->klass_name_at(cp_index);
  return classname->as_utf8();
JVM_END


JVM_ENTRY(const char*, JVM_GetCPFieldClassNameUTF(JNIEnv *env, jclass cls, jint cp_index))
  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(cls));
  k = JvmtiThreadState::class_to_verify_considering_redefinition(k, thread);
  ConstantPool* cp = InstanceKlass::cast(k)->constants();
  switch (cp->tag_at(cp_index).value()) {
    case JVM_CONSTANT_Fieldref: {
      int class_index = cp->uncached_klass_ref_index_at(cp_index);
      Symbol* classname = cp->klass_name_at(class_index);
      return classname->as_utf8();
    }
    default:
      fatal("JVM_GetCPFieldClassNameUTF: illegal constant");
  }
  ShouldNotReachHere();
  return NULL;
JVM_END


JVM_ENTRY(const char*, JVM_GetCPMethodClassNameUTF(JNIEnv *env, jclass cls, jint cp_index))
  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(cls));
  k = JvmtiThreadState::class_to_verify_considering_redefinition(k, thread);
  ConstantPool* cp = InstanceKlass::cast(k)->constants();
  switch (cp->tag_at(cp_index).value()) {
    case JVM_CONSTANT_Methodref:
    case JVM_CONSTANT_InterfaceMethodref: {
      int class_index = cp->uncached_klass_ref_index_at(cp_index);
      Symbol* classname = cp->klass_name_at(class_index);
      return classname->as_utf8();
    }
    default:
      fatal("JVM_GetCPMethodClassNameUTF: illegal constant");
  }
  ShouldNotReachHere();
  return NULL;
JVM_END


JVM_ENTRY(jint, JVM_GetCPFieldModifiers(JNIEnv *env, jclass cls, int cp_index, jclass called_cls))
  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(cls));
  Klass* k_called = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(called_cls));
  k        = JvmtiThreadState::class_to_verify_considering_redefinition(k, thread);
  k_called = JvmtiThreadState::class_to_verify_considering_redefinition(k_called, thread);
  ConstantPool* cp = InstanceKlass::cast(k)->constants();
  ConstantPool* cp_called = InstanceKlass::cast(k_called)->constants();
  switch (cp->tag_at(cp_index).value()) {
    case JVM_CONSTANT_Fieldref: {
      Symbol* name      = cp->uncached_name_ref_at(cp_index);
      Symbol* signature = cp->uncached_signature_ref_at(cp_index);
      InstanceKlass* ik = InstanceKlass::cast(k_called);
      for (JavaFieldStream fs(ik); !fs.done(); fs.next()) {
        if (fs.name() == name && fs.signature() == signature) {
          return fs.access_flags().as_short() & JVM_RECOGNIZED_FIELD_MODIFIERS;
        }
      }
      return -1;
    }
    default:
      fatal("JVM_GetCPFieldModifiers: illegal constant");
  }
  ShouldNotReachHere();
  return 0;
JVM_END


JVM_ENTRY(jint, JVM_GetCPMethodModifiers(JNIEnv *env, jclass cls, int cp_index, jclass called_cls))
  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(cls));
  Klass* k_called = java_lang_Class::as_Klass(JNIHandles::resolve_non_null(called_cls));
  k        = JvmtiThreadState::class_to_verify_considering_redefinition(k, thread);
  k_called = JvmtiThreadState::class_to_verify_considering_redefinition(k_called, thread);
  ConstantPool* cp = InstanceKlass::cast(k)->constants();
  switch (cp->tag_at(cp_index).value()) {
    case JVM_CONSTANT_Methodref:
    case JVM_CONSTANT_InterfaceMethodref: {
      Symbol* name      = cp->uncached_name_ref_at(cp_index);
      Symbol* signature = cp->uncached_signature_ref_at(cp_index);
      Array<Method*>* methods = InstanceKlass::cast(k_called)->methods();
      int methods_count = methods->length();
      for (int i = 0; i < methods_count; i++) {
        Method* method = methods->at(i);
        if (method->name() == name && method->signature() == signature) {
            return method->access_flags().as_int() & JVM_RECOGNIZED_METHOD_MODIFIERS;
        }
      }
      return -1;
    }
    default:
      fatal("JVM_GetCPMethodModifiers: illegal constant");
  }
  ShouldNotReachHere();
  return 0;
JVM_END


// Misc //////////////////////////////////////////////////////////////////////////////////////////////

JVM_LEAF(void, JVM_ReleaseUTF(const char *utf))
  // So long as UTF8::convert_to_utf8 returns resource strings, we don't have to do anything
JVM_END


JVM_ENTRY(jboolean, JVM_IsSameClassPackage(JNIEnv *env, jclass class1, jclass class2))
  oop class1_mirror = JNIHandles::resolve_non_null(class1);
  oop class2_mirror = JNIHandles::resolve_non_null(class2);
  Klass* klass1 = java_lang_Class::as_Klass(class1_mirror);
  Klass* klass2 = java_lang_Class::as_Klass(class2_mirror);
  return (jboolean) Reflection::is_same_class_package(klass1, klass2);
JVM_END

// Printing support //////////////////////////////////////////////////
extern "C" {

ATTRIBUTE_PRINTF(3, 0)
int jio_vsnprintf(char *str, size_t count, const char *fmt, va_list args) {
  // Reject count values that are negative signed values converted to
  // unsigned; see bug 4399518, 4417214
  if ((intptr_t)count <= 0) return -1;

  int result = os::vsnprintf(str, count, fmt, args);
  if (result > 0 && (size_t)result >= count) {
    result = -1;
  }

  return result;
}

ATTRIBUTE_PRINTF(3, 4)
int jio_snprintf(char *str, size_t count, const char *fmt, ...) {
  va_list args;
  int len;
  va_start(args, fmt);
  len = jio_vsnprintf(str, count, fmt, args);
  va_end(args);
  return len;
}

ATTRIBUTE_PRINTF(2, 3)
int jio_fprintf(FILE* f, const char *fmt, ...) {
  int len;
  va_list args;
  va_start(args, fmt);
  len = jio_vfprintf(f, fmt, args);
  va_end(args);
  return len;
}

ATTRIBUTE_PRINTF(2, 0)
int jio_vfprintf(FILE* f, const char *fmt, va_list args) {
  if (Arguments::vfprintf_hook() != NULL) {
     return Arguments::vfprintf_hook()(f, fmt, args);
  } else {
    return vfprintf(f, fmt, args);
  }
}

ATTRIBUTE_PRINTF(1, 2)
JNIEXPORT int jio_printf(const char *fmt, ...) {
  int len;
  va_list args;
  va_start(args, fmt);
  len = jio_vfprintf(defaultStream::output_stream(), fmt, args);
  va_end(args);
  return len;
}

// HotSpot specific jio method
void jio_print(const char* s, size_t len) {
  // Try to make this function as atomic as possible.
  if (Arguments::vfprintf_hook() != NULL) {
    jio_fprintf(defaultStream::output_stream(), "%.*s", (int)len, s);
  } else {
    // Make an unused local variable to avoid warning from gcc compiler.
    size_t count = ::write(defaultStream::output_fd(), s, (int)len);
  }
}

} // Extern C

// java.lang.Thread //////////////////////////////////////////////////////////////////////////////

// In most of the JVM thread support functions we need to access the
// thread through a ThreadsListHandle to prevent it from exiting and
// being reclaimed while we try to operate on it. The exceptions to this
// rule are when operating on the current thread, or if the monitor of
// the target java.lang.Thread is locked at the Java level - in both
// cases the target cannot exit.

static void thread_entry(JavaThread* thread, TRAPS) {
  HandleMark hm(THREAD);
  Handle obj(THREAD, thread->threadObj());
  JavaValue result(T_VOID);
  JavaCalls::call_virtual(&result,
                          obj,
                          vmClasses::Thread_klass(),
                          vmSymbols::run_method_name(),
                          vmSymbols::void_method_signature(),
                          THREAD);
}


JVM_ENTRY(void, JVM_StartThread(JNIEnv* env, jobject jthread))
  JavaThread *native_thread = NULL;

  // We cannot hold the Threads_lock when we throw an exception,
  // due to rank ordering issues. Example:  we might need to grab the
  // Heap_lock while we construct the exception.
  bool throw_illegal_thread_state = false;

  // We must release the Threads_lock before we can post a jvmti event
  // in Thread::start.
  {
    // Ensure that the C++ Thread and OSThread structures aren't freed before
    // we operate.
    MutexLocker mu(Threads_lock);

    // Since JDK 5 the java.lang.Thread threadStatus is used to prevent
    // re-starting an already started thread, so we should usually find
    // that the JavaThread is null. However for a JNI attached thread
    // there is a small window between the Thread object being created
    // (with its JavaThread set) and the update to its threadStatus, so we
    // have to check for this
    if (java_lang_Thread::thread(JNIHandles::resolve_non_null(jthread)) != NULL) {
      throw_illegal_thread_state = true;
    } else {
      // We could also check the stillborn flag to see if this thread was already stopped, but
      // for historical reasons we let the thread detect that itself when it starts running

      jlong size =
             java_lang_Thread::stackSize(JNIHandles::resolve_non_null(jthread));
      // Allocate the C++ Thread structure and create the native thread.  The
      // stack size retrieved from java is 64-bit signed, but the constructor takes
      // size_t (an unsigned type), which may be 32 or 64-bit depending on the platform.
      //  - Avoid truncating on 32-bit platforms if size is greater than UINT_MAX.
      //  - Avoid passing negative values which would result in really large stacks.
      NOT_LP64(if (size > SIZE_MAX) size = SIZE_MAX;)
      size_t sz = size > 0 ? (size_t) size : 0;
      native_thread = new JavaThread(&thread_entry, sz);

      // At this point it may be possible that no osthread was created for the
      // JavaThread due to lack of memory. Check for this situation and throw
      // an exception if necessary. Eventually we may want to change this so
      // that we only grab the lock if the thread was created successfully -
      // then we can also do this check and throw the exception in the
      // JavaThread constructor.
      if (native_thread->osthread() != NULL) {
        // Note: the current thread is not being used within "prepare".
        native_thread->prepare(jthread);
      }
    }
  }

  if (throw_illegal_thread_state) {
    THROW(vmSymbols::java_lang_IllegalThreadStateException());
  }

  assert(native_thread != NULL, "Starting null thread?");

  if (native_thread->osthread() == NULL) {
    ResourceMark rm(thread);
    log_warning(os, thread)("Failed to start the native thread for java.lang.Thread \"%s\"",
                            JavaThread::name_for(JNIHandles::resolve_non_null(jthread)));
    // No one should hold a reference to the 'native_thread'.
    native_thread->smr_delete();
    if (JvmtiExport::should_post_resource_exhausted()) {
      JvmtiExport::post_resource_exhausted(
        JVMTI_RESOURCE_EXHAUSTED_OOM_ERROR | JVMTI_RESOURCE_EXHAUSTED_THREADS,
        os::native_thread_creation_failed_msg());
    }
    THROW_MSG(vmSymbols::java_lang_OutOfMemoryError(),
              os::native_thread_creation_failed_msg());
  }

#if INCLUDE_JFR
  if (Jfr::is_recording() && EventThreadStart::is_enabled() &&
      EventThreadStart::is_stacktrace_enabled()) {
    JfrThreadLocal* tl = native_thread->jfr_thread_local();
    // skip Thread.start() and Thread.start0()
    tl->set_cached_stack_trace_id(JfrStackTraceRepository::record(thread, 2));
  }
#endif

  Thread::start(native_thread);

JVM_END


// JVM_Stop is implemented using a VM_Operation, so threads are forced to safepoints
// before the quasi-asynchronous exception is delivered.  This is a little obtrusive,
// but is thought to be reliable and simple. In the case, where the receiver is the
// same thread as the sender, no VM_Operation is needed.
JVM_ENTRY(void, JVM_StopThread(JNIEnv* env, jobject jthread, jobject throwable))
  ThreadsListHandle tlh(thread);
  oop java_throwable = JNIHandles::resolve(throwable);
  if (java_throwable == NULL) {
    THROW(vmSymbols::java_lang_NullPointerException());
  }
  oop java_thread = NULL;
  JavaThread* receiver = NULL;
  bool is_alive = tlh.cv_internal_thread_to_JavaThread(jthread, &receiver, &java_thread);
  Events::log_exception(thread,
                        "JVM_StopThread thread JavaThread " INTPTR_FORMAT " as oop " INTPTR_FORMAT " [exception " INTPTR_FORMAT "]",
                        p2i(receiver), p2i(java_thread), p2i(throwable));

  if (is_alive) {
    // jthread refers to a live JavaThread.
    if (thread == receiver) {
      // Exception is getting thrown at self so no VM_Operation needed.
      THROW_OOP(java_throwable);
    } else {
      // Use a VM_Operation to throw the exception.
      JavaThread::send_async_exception(java_thread, java_throwable);
    }
  } else {
    // Either:
    // - target thread has not been started before being stopped, or
    // - target thread already terminated
    // We could read the threadStatus to determine which case it is
    // but that is overkill as it doesn't matter. We must set the
    // stillborn flag for the first case, and if the thread has already
    // exited setting this flag has no effect.
    java_lang_Thread::set_stillborn(java_thread);
  }
JVM_END


JVM_ENTRY(jboolean, JVM_IsThreadAlive(JNIEnv* env, jobject jthread))
  oop thread_oop = JNIHandles::resolve_non_null(jthread);
  return java_lang_Thread::is_alive(thread_oop);
JVM_END


JVM_ENTRY(void, JVM_SuspendThread(JNIEnv* env, jobject jthread))
  ThreadsListHandle tlh(thread);
  JavaThread* receiver = NULL;
  bool is_alive = tlh.cv_internal_thread_to_JavaThread(jthread, &receiver, NULL);
  if (is_alive) {
    // jthread refers to a live JavaThread, but java_suspend() will
    // detect a thread that has started to exit and will ignore it.
    receiver->java_suspend();
  }
JVM_END


JVM_ENTRY(void, JVM_ResumeThread(JNIEnv* env, jobject jthread))
  ThreadsListHandle tlh(thread);
  JavaThread* receiver = NULL;
  bool is_alive = tlh.cv_internal_thread_to_JavaThread(jthread, &receiver, NULL);
  if (is_alive) {
    // jthread refers to a live JavaThread.
    receiver->java_resume();
  }
JVM_END


JVM_ENTRY(void, JVM_SetThreadPriority(JNIEnv* env, jobject jthread, jint prio))
  ThreadsListHandle tlh(thread);
  oop java_thread = NULL;
  JavaThread* receiver = NULL;
  bool is_alive = tlh.cv_internal_thread_to_JavaThread(jthread, &receiver, &java_thread);
  java_lang_Thread::set_priority(java_thread, (ThreadPriority)prio);

  if (is_alive) {
    // jthread refers to a live JavaThread.
    Thread::set_priority(receiver, (ThreadPriority)prio);
  }
  // Implied else: If the JavaThread hasn't started yet, then the
  // priority set in the java.lang.Thread object above will be pushed
  // down when it does start.
JVM_END


JVM_ENTRY(void, JVM_Yield(JNIEnv *env, jclass threadClass))
  if (os::dont_yield()) return;
  HOTSPOT_THREAD_YIELD();
  os::naked_yield();
JVM_END

static void post_thread_sleep_event(EventThreadSleep* event, jlong millis) {
  assert(event != NULL, "invariant");
  assert(event->should_commit(), "invariant");
  event->set_time(millis);
  event->commit();
}

JVM_ENTRY(void, JVM_Sleep(JNIEnv* env, jclass threadClass, jlong millis))
  if (millis < 0) {
    THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(), "timeout value is negative");
  }

  if (thread->is_interrupted(true) && !HAS_PENDING_EXCEPTION) {
    THROW_MSG(vmSymbols::java_lang_InterruptedException(), "sleep interrupted");
  }

  // Save current thread state and restore it at the end of this block.
  // And set new thread state to SLEEPING.
  JavaThreadSleepState jtss(thread);

  HOTSPOT_THREAD_SLEEP_BEGIN(millis);
  EventThreadSleep event;

  if (millis == 0) {
    os::naked_yield();
  } else {
    ThreadState old_state = thread->osthread()->get_state();
    thread->osthread()->set_state(SLEEPING);
    if (!thread->sleep(millis)) { // interrupted
      // An asynchronous exception (e.g., ThreadDeathException) could have been thrown on
      // us while we were sleeping. We do not overwrite those.
      if (!HAS_PENDING_EXCEPTION) {
        if (event.should_commit()) {
          post_thread_sleep_event(&event, millis);
        }
        HOTSPOT_THREAD_SLEEP_END(1);

        // TODO-FIXME: THROW_MSG returns which means we will not call set_state()
        // to properly restore the thread state.  That's likely wrong.
        THROW_MSG(vmSymbols::java_lang_InterruptedException(), "sleep interrupted");
      }
    }
    thread->osthread()->set_state(old_state);
  }
  if (event.should_commit()) {
    post_thread_sleep_event(&event, millis);
  }
  HOTSPOT_THREAD_SLEEP_END(0);
JVM_END

JVM_ENTRY(jobject, JVM_CurrentThread(JNIEnv* env, jclass threadClass))
  oop jthread = thread->threadObj();
  assert(jthread != NULL, "no current thread!");
  return JNIHandles::make_local(THREAD, jthread);
JVM_END

JVM_ENTRY(void, JVM_Interrupt(JNIEnv* env, jobject jthread))
  ThreadsListHandle tlh(thread);
  JavaThread* receiver = NULL;
  bool is_alive = tlh.cv_internal_thread_to_JavaThread(jthread, &receiver, NULL);
  if (is_alive) {
    // jthread refers to a live JavaThread.
    receiver->interrupt();
  }
JVM_END


// Return true iff the current thread has locked the object passed in

JVM_ENTRY(jboolean, JVM_HoldsLock(JNIEnv* env, jclass threadClass, jobject obj))
  if (obj == NULL) {
    THROW_(vmSymbols::java_lang_NullPointerException(), JNI_FALSE);
  }
  Handle h_obj(THREAD, JNIHandles::resolve(obj));
  return ObjectSynchronizer::current_thread_holds_lock(thread, h_obj);
JVM_END


JVM_ENTRY(void, JVM_DumpAllStacks(JNIEnv* env, jclass))
  VM_PrintThreads op;
  VMThread::execute(&op);
  if (JvmtiExport::should_post_data_dump()) {
    JvmtiExport::post_data_dump();
  }
JVM_END

JVM_ENTRY(void, JVM_SetNativeThreadName(JNIEnv* env, jobject jthread, jstring name))
  // We don't use a ThreadsListHandle here because the current thread
  // must be alive.
  oop java_thread = JNIHandles::resolve_non_null(jthread);
  JavaThread* thr = java_lang_Thread::thread(java_thread);
  if (thread == thr && !thr->has_attached_via_jni()) {
    // Thread naming is only supported for the current thread and
    // we don't set the name of an attached thread to avoid stepping
    // on other programs.
    ResourceMark rm(thread);
    const char *thread_name = java_lang_String::as_utf8_string(JNIHandles::resolve_non_null(name));
    os::set_native_thread_name(thread_name);
  }
JVM_END

// java.lang.SecurityManager ///////////////////////////////////////////////////////////////////////

JVM_ENTRY(jobjectArray, JVM_GetClassContext(JNIEnv *env))
  ResourceMark rm(THREAD);
  JvmtiVMObjectAllocEventCollector oam;
  vframeStream vfst(thread);

  if (vmClasses::reflect_CallerSensitive_klass() != NULL) {
    // This must only be called from SecurityManager.getClassContext
    Method* m = vfst.method();
    if (!(m->method_holder() == vmClasses::SecurityManager_klass() &&
          m->name()          == vmSymbols::getClassContext_name() &&
          m->signature()     == vmSymbols::void_class_array_signature())) {
      THROW_MSG_NULL(vmSymbols::java_lang_InternalError(), "JVM_GetClassContext must only be called from SecurityManager.getClassContext");
    }
  }

  // Collect method holders
  GrowableArray<Klass*>* klass_array = new GrowableArray<Klass*>();
  for (; !vfst.at_end(); vfst.security_next()) {
    Method* m = vfst.method();
    // Native frames are not returned
    if (!m->is_ignored_by_security_stack_walk() && !m->is_native()) {
      Klass* holder = m->method_holder();
      assert(holder->is_klass(), "just checking");
      klass_array->append(holder);
    }
  }

  // Create result array of type [Ljava/lang/Class;
  objArrayOop result = oopFactory::new_objArray(vmClasses::Class_klass(), klass_array->length(), CHECK_NULL);
  // Fill in mirrors corresponding to method holders
  for (int i = 0; i < klass_array->length(); i++) {
    result->obj_at_put(i, klass_array->at(i)->java_mirror());
  }

  return (jobjectArray) JNIHandles::make_local(THREAD, result);
JVM_END


// java.lang.Package ////////////////////////////////////////////////////////////////


JVM_ENTRY(jstring, JVM_GetSystemPackage(JNIEnv *env, jstring name))
  ResourceMark rm(THREAD);
  JvmtiVMObjectAllocEventCollector oam;
  char* str = java_lang_String::as_utf8_string(JNIHandles::resolve_non_null(name));
  oop result = ClassLoader::get_system_package(str, CHECK_NULL);
return (jstring) JNIHandles::make_local(THREAD, result);
JVM_END


JVM_ENTRY(jobjectArray, JVM_GetSystemPackages(JNIEnv *env))
  JvmtiVMObjectAllocEventCollector oam;
  objArrayOop result = ClassLoader::get_system_packages(CHECK_NULL);
  return (jobjectArray) JNIHandles::make_local(THREAD, result);
JVM_END


// java.lang.ref.Reference ///////////////////////////////////////////////////////////////


JVM_ENTRY(jobject, JVM_GetAndClearReferencePendingList(JNIEnv* env))
  MonitorLocker ml(Heap_lock);
  oop ref = Universe::reference_pending_list();
  if (ref != NULL) {
    Universe::clear_reference_pending_list();
  }
  return JNIHandles::make_local(THREAD, ref);
JVM_END

JVM_ENTRY(jboolean, JVM_HasReferencePendingList(JNIEnv* env))
  MonitorLocker ml(Heap_lock);
  return Universe::has_reference_pending_list();
JVM_END

JVM_ENTRY(void, JVM_WaitForReferencePendingList(JNIEnv* env))
  MonitorLocker ml(Heap_lock);
  while (!Universe::has_reference_pending_list()) {
    ml.wait();
  }
JVM_END

JVM_ENTRY(jboolean, JVM_ReferenceRefersTo(JNIEnv* env, jobject ref, jobject o))
  oop ref_oop = JNIHandles::resolve_non_null(ref);
  oop referent = java_lang_ref_Reference::weak_referent_no_keepalive(ref_oop);
  return referent == JNIHandles::resolve(o);
JVM_END

JVM_ENTRY(void, JVM_ReferenceClear(JNIEnv* env, jobject ref))
  oop ref_oop = JNIHandles::resolve_non_null(ref);
  // FinalReference has it's own implementation of clear().
  assert(!java_lang_ref_Reference::is_final(ref_oop), "precondition");
  if (java_lang_ref_Reference::unknown_referent_no_keepalive(ref_oop) == NULL) {
    // If the referent has already been cleared then done.
    // However, if the referent is dead but has not yet been cleared by
    // concurrent reference processing, it should NOT be cleared here.
    // Instead, clearing should be left to the GC.  Clearing it here could
    // detectably lose an expected notification, which is impossible with
    // STW reference processing.  The clearing in enqueue() doesn't have
    // this problem, since the enqueue covers the notification, but it's not
    // worth the effort to handle that case specially.
    return;
  }
  java_lang_ref_Reference::clear_referent(ref_oop);
JVM_END


// java.lang.ref.PhantomReference //////////////////////////////////////////////////


JVM_ENTRY(jboolean, JVM_PhantomReferenceRefersTo(JNIEnv* env, jobject ref, jobject o))
  oop ref_oop = JNIHandles::resolve_non_null(ref);
  oop referent = java_lang_ref_Reference::phantom_referent_no_keepalive(ref_oop);
  return referent == JNIHandles::resolve(o);
JVM_END


// ObjectInputStream ///////////////////////////////////////////////////////////////

// Return the first user-defined class loader up the execution stack, or null
// if only code from the bootstrap or platform class loader is on the stack.

JVM_ENTRY(jobject, JVM_LatestUserDefinedLoader(JNIEnv *env))
  for (vframeStream vfst(thread); !vfst.at_end(); vfst.next()) {
    InstanceKlass* ik = vfst.method()->method_holder();
    oop loader = ik->class_loader();
    if (loader != NULL && !SystemDictionary::is_platform_class_loader(loader)) {
      // Skip reflection related frames
      if (!ik->is_subclass_of(vmClasses::reflect_MethodAccessorImpl_klass()) &&
          !ik->is_subclass_of(vmClasses::reflect_ConstructorAccessorImpl_klass())) {
        return JNIHandles::make_local(THREAD, loader);
      }
    }
  }
  return NULL;
JVM_END


// Array ///////////////////////////////////////////////////////////////////////////////////////////


// resolve array handle and check arguments
static inline arrayOop check_array(JNIEnv *env, jobject arr, bool type_array_only, TRAPS) {
  if (arr == NULL) {
    THROW_0(vmSymbols::java_lang_NullPointerException());
  }
  oop a = JNIHandles::resolve_non_null(arr);
  if (!a->is_array()) {
    THROW_MSG_0(vmSymbols::java_lang_IllegalArgumentException(), "Argument is not an array");
  } else if (type_array_only && !a->is_typeArray()) {
    THROW_MSG_0(vmSymbols::java_lang_IllegalArgumentException(), "Argument is not an array of primitive type");
  }
  return arrayOop(a);
}


JVM_ENTRY(jint, JVM_GetArrayLength(JNIEnv *env, jobject arr))
  arrayOop a = check_array(env, arr, false, CHECK_0);
  return a->length();
JVM_END


JVM_ENTRY(jobject, JVM_GetArrayElement(JNIEnv *env, jobject arr, jint index))
  JvmtiVMObjectAllocEventCollector oam;
  arrayOop a = check_array(env, arr, false, CHECK_NULL);
  jvalue value;
  BasicType type = Reflection::array_get(&value, a, index, CHECK_NULL);
  oop box = Reflection::box(&value, type, CHECK_NULL);
  return JNIHandles::make_local(THREAD, box);
JVM_END


JVM_ENTRY(jvalue, JVM_GetPrimitiveArrayElement(JNIEnv *env, jobject arr, jint index, jint wCode))
  jvalue value;
  value.i = 0; // to initialize value before getting used in CHECK
  arrayOop a = check_array(env, arr, true, CHECK_(value));
  assert(a->is_typeArray(), "just checking");
  BasicType type = Reflection::array_get(&value, a, index, CHECK_(value));
  BasicType wide_type = (BasicType) wCode;
  if (type != wide_type) {
    Reflection::widen(&value, type, wide_type, CHECK_(value));
  }
  return value;
JVM_END


JVM_ENTRY(void, JVM_SetArrayElement(JNIEnv *env, jobject arr, jint index, jobject val))
  arrayOop a = check_array(env, arr, false, CHECK);
  oop box = JNIHandles::resolve(val);
  jvalue value;
  value.i = 0; // to initialize value before getting used in CHECK
  BasicType value_type;
  if (a->is_objArray()) {
    // Make sure we do no unbox e.g. java/lang/Integer instances when storing into an object array
    value_type = Reflection::unbox_for_regular_object(box, &value);
  } else {
    value_type = Reflection::unbox_for_primitive(box, &value, CHECK);
  }
  Reflection::array_set(&value, a, index, value_type, CHECK);
JVM_END


JVM_ENTRY(void, JVM_SetPrimitiveArrayElement(JNIEnv *env, jobject arr, jint index, jvalue v, unsigned char vCode))
  arrayOop a = check_array(env, arr, true, CHECK);
  assert(a->is_typeArray(), "just checking");
  BasicType value_type = (BasicType) vCode;
  Reflection::array_set(&v, a, index, value_type, CHECK);
JVM_END


JVM_ENTRY(jobject, JVM_NewArray(JNIEnv *env, jclass eltClass, jint length))
  JvmtiVMObjectAllocEventCollector oam;
  oop element_mirror = JNIHandles::resolve(eltClass);
  oop result = Reflection::reflect_new_array(element_mirror, length, CHECK_NULL);
  return JNIHandles::make_local(THREAD, result);
JVM_END


JVM_ENTRY(jobject, JVM_NewMultiArray(JNIEnv *env, jclass eltClass, jintArray dim))
  JvmtiVMObjectAllocEventCollector oam;
  arrayOop dim_array = check_array(env, dim, true, CHECK_NULL);
  oop element_mirror = JNIHandles::resolve(eltClass);
  assert(dim_array->is_typeArray(), "just checking");
  oop result = Reflection::reflect_new_multi_array(element_mirror, typeArrayOop(dim_array), CHECK_NULL);
  return JNIHandles::make_local(THREAD, result);
JVM_END


// Library support ///////////////////////////////////////////////////////////////////////////

JVM_ENTRY_NO_ENV(void*, JVM_LoadLibrary(const char* name))
  //%note jvm_ct
  char ebuf[1024];
  void *load_result;
  {
    ThreadToNativeFromVM ttnfvm(thread);
    load_result = os::dll_load(name, ebuf, sizeof ebuf);
  }
  if (load_result == NULL) {
    char msg[1024];
    jio_snprintf(msg, sizeof msg, "%s: %s", name, ebuf);
    // Since 'ebuf' may contain a string encoded using
    // platform encoding scheme, we need to pass
    // Exceptions::unsafe_to_utf8 to the new_exception method
    // as the last argument. See bug 6367357.
    Handle h_exception =
      Exceptions::new_exception(thread,
                                vmSymbols::java_lang_UnsatisfiedLinkError(),
                                msg, Exceptions::unsafe_to_utf8);

    THROW_HANDLE_0(h_exception);
  }
  log_info(library)("Loaded library %s, handle " INTPTR_FORMAT, name, p2i(load_result));
  return load_result;
JVM_END


JVM_LEAF(void, JVM_UnloadLibrary(void* handle))
  os::dll_unload(handle);
  log_info(library)("Unloaded library with handle " INTPTR_FORMAT, p2i(handle));
JVM_END


JVM_LEAF(void*, JVM_FindLibraryEntry(void* handle, const char* name))
  void* find_result = os::dll_lookup(handle, name);
  log_info(library)("%s %s in library with handle " INTPTR_FORMAT,
                    find_result != NULL ? "Found" : "Failed to find",
                    name, p2i(handle));
  return find_result;
JVM_END


// JNI version ///////////////////////////////////////////////////////////////////////////////

JVM_LEAF(jboolean, JVM_IsSupportedJNIVersion(jint version))
  return Threads::is_supported_jni_version_including_1_1(version);
JVM_END


// String support ///////////////////////////////////////////////////////////////////////////

JVM_ENTRY(jstring, JVM_InternString(JNIEnv *env, jstring str))
  JvmtiVMObjectAllocEventCollector oam;
  if (str == NULL) return NULL;
  oop string = JNIHandles::resolve_non_null(str);
  oop result = StringTable::intern(string, CHECK_NULL);
  return (jstring) JNIHandles::make_local(THREAD, result);
JVM_END


// VM Raw monitor support //////////////////////////////////////////////////////////////////////

// VM Raw monitors (not to be confused with JvmtiRawMonitors) are a simple mutual exclusion
// lock (not actually monitors: no wait/notify) that is exported by the VM for use by JDK
// library code. They may be used by JavaThreads and non-JavaThreads and do not participate
// in the safepoint protocol, thread suspension, thread interruption, or anything of that
// nature. JavaThreads will be "in native" when using this API from JDK code.


JNIEXPORT void* JNICALL JVM_RawMonitorCreate(void) {
  VM_Exit::block_if_vm_exited();
  return new os::PlatformMutex();
}


JNIEXPORT void JNICALL  JVM_RawMonitorDestroy(void *mon) {
  VM_Exit::block_if_vm_exited();
  delete ((os::PlatformMutex*) mon);
}


JNIEXPORT jint JNICALL JVM_RawMonitorEnter(void *mon) {
  VM_Exit::block_if_vm_exited();
  ((os::PlatformMutex*) mon)->lock();
  return 0;
}


JNIEXPORT void JNICALL JVM_RawMonitorExit(void *mon) {
  VM_Exit::block_if_vm_exited();
  ((os::PlatformMutex*) mon)->unlock();
}


// Shared JNI/JVM entry points //////////////////////////////////////////////////////////////

jclass find_class_from_class_loader(JNIEnv* env, Symbol* name, jboolean init,
                                    Handle loader, Handle protection_domain,
                                    jboolean throwError, TRAPS) {
  // Security Note:
  //   The Java level wrapper will perform the necessary security check allowing
  //   us to pass the NULL as the initiating class loader.  The VM is responsible for
  //   the checkPackageAccess relative to the initiating class loader via the
  //   protection_domain. The protection_domain is passed as NULL by the java code
  //   if there is no security manager in 3-arg Class.forName().
  Klass* klass = SystemDictionary::resolve_or_fail(name, loader, protection_domain, throwError != 0, CHECK_NULL);

  // Check if we should initialize the class
  if (init && klass->is_instance_klass()) {
    klass->initialize(CHECK_NULL);
  }
  return (jclass) JNIHandles::make_local(THREAD, klass->java_mirror());
}


// Method ///////////////////////////////////////////////////////////////////////////////////////////

JVM_ENTRY(jobject, JVM_InvokeMethod(JNIEnv *env, jobject method, jobject obj, jobjectArray args0))
  Handle method_handle;
  if (thread->stack_overflow_state()->stack_available((address) &method_handle) >= JVMInvokeMethodSlack) {
    method_handle = Handle(THREAD, JNIHandles::resolve(method));
    Handle receiver(THREAD, JNIHandles::resolve(obj));
    objArrayHandle args(THREAD, objArrayOop(JNIHandles::resolve(args0)));
    oop result = Reflection::invoke_method(method_handle(), receiver, args, CHECK_NULL);
    jobject res = JNIHandles::make_local(THREAD, result);
    if (JvmtiExport::should_post_vm_object_alloc()) {
      oop ret_type = java_lang_reflect_Method::return_type(method_handle());
      assert(ret_type != NULL, "sanity check: ret_type oop must not be NULL!");
      if (java_lang_Class::is_primitive(ret_type)) {
        // Only for primitive type vm allocates memory for java object.
        // See box() method.
        JvmtiExport::post_vm_object_alloc(thread, result);
      }
    }
    return res;
  } else {
    THROW_0(vmSymbols::java_lang_StackOverflowError());
  }
JVM_END


JVM_ENTRY(jobject, JVM_NewInstanceFromConstructor(JNIEnv *env, jobject c, jobjectArray args0))
  oop constructor_mirror = JNIHandles::resolve(c);
  objArrayHandle args(THREAD, objArrayOop(JNIHandles::resolve(args0)));
  oop result = Reflection::invoke_constructor(constructor_mirror, args, CHECK_NULL);
  jobject res = JNIHandles::make_local(THREAD, result);
  if (JvmtiExport::should_post_vm_object_alloc()) {
    JvmtiExport::post_vm_object_alloc(thread, result);
  }
  return res;
JVM_END

// Atomic ///////////////////////////////////////////////////////////////////////////////////////////

JVM_LEAF(jboolean, JVM_SupportsCX8())
  return VM_Version::supports_cx8();
JVM_END

JVM_ENTRY(void, JVM_InitializeFromArchive(JNIEnv* env, jclass cls))
  Klass* k = java_lang_Class::as_Klass(JNIHandles::resolve(cls));
  assert(k->is_klass(), "just checking");
  HeapShared::initialize_from_archived_subgraph(k, THREAD);
JVM_END

JVM_ENTRY(void, JVM_RegisterLambdaProxyClassForArchiving(JNIEnv* env,
                                              jclass caller,
                                              jstring interfaceMethodName,
                                              jobject factoryType,
                                              jobject interfaceMethodType,
                                              jobject implementationMember,
                                              jobject dynamicMethodType,
                                              jclass lambdaProxyClass))
#if INCLUDE_CDS
  if (!Arguments::is_dumping_archive()) {
    return;
  }

  Klass* caller_k = java_lang_Class::as_Klass(JNIHandles::resolve(caller));
  InstanceKlass* caller_ik = InstanceKlass::cast(caller_k);
  if (caller_ik->is_hidden()) {
    // Hidden classes not of type lambda proxy classes are currently not being archived.
    // If the caller_ik is of one of the above types, the corresponding lambda proxy class won't be
    // registered for archiving.
    return;
  }
  Klass* lambda_k = java_lang_Class::as_Klass(JNIHandles::resolve(lambdaProxyClass));
  InstanceKlass* lambda_ik = InstanceKlass::cast(lambda_k);
  assert(lambda_ik->is_hidden(), "must be a hidden class");
  assert(!lambda_ik->is_non_strong_hidden(), "expected a strong hidden class");

  Symbol* interface_method_name = NULL;
  if (interfaceMethodName != NULL) {
    interface_method_name = java_lang_String::as_symbol(JNIHandles::resolve_non_null(interfaceMethodName));
  }
  Handle factory_type_oop(THREAD, JNIHandles::resolve_non_null(factoryType));
  Symbol* factory_type = java_lang_invoke_MethodType::as_signature(factory_type_oop(), true);

  Handle interface_method_type_oop(THREAD, JNIHandles::resolve_non_null(interfaceMethodType));
  Symbol* interface_method_type = java_lang_invoke_MethodType::as_signature(interface_method_type_oop(), true);

  Handle implementation_member_oop(THREAD, JNIHandles::resolve_non_null(implementationMember));
  assert(java_lang_invoke_MemberName::is_method(implementation_member_oop()), "must be");
  Method* m = java_lang_invoke_MemberName::vmtarget(implementation_member_oop());

  Handle dynamic_method_type_oop(THREAD, JNIHandles::resolve_non_null(dynamicMethodType));
  Symbol* dynamic_method_type = java_lang_invoke_MethodType::as_signature(dynamic_method_type_oop(), true);

  SystemDictionaryShared::add_lambda_proxy_class(caller_ik, lambda_ik, interface_method_name, factory_type,
                                                 interface_method_type, m, dynamic_method_type, THREAD);
#endif // INCLUDE_CDS
JVM_END

JVM_ENTRY(jclass, JVM_LookupLambdaProxyClassFromArchive(JNIEnv* env,
                                                        jclass caller,
                                                        jstring interfaceMethodName,
                                                        jobject factoryType,
                                                        jobject interfaceMethodType,
                                                        jobject implementationMember,
                                                        jobject dynamicMethodType))
#if INCLUDE_CDS

  if (interfaceMethodName == NULL || factoryType == NULL || interfaceMethodType == NULL ||
      implementationMember == NULL || dynamicMethodType == NULL) {
    THROW_(vmSymbols::java_lang_NullPointerException(), NULL);
  }

  Klass* caller_k = java_lang_Class::as_Klass(JNIHandles::resolve(caller));
  InstanceKlass* caller_ik = InstanceKlass::cast(caller_k);
  if (!caller_ik->is_shared()) {
    // there won't be a shared lambda class if the caller_ik is not in the shared archive.
    return NULL;
  }

  Symbol* interface_method_name = java_lang_String::as_symbol(JNIHandles::resolve_non_null(interfaceMethodName));
  Handle factory_type_oop(THREAD, JNIHandles::resolve_non_null(factoryType));
  Symbol* factory_type = java_lang_invoke_MethodType::as_signature(factory_type_oop(), true);

  Handle interface_method_type_oop(THREAD, JNIHandles::resolve_non_null(interfaceMethodType));
  Symbol* interface_method_type = java_lang_invoke_MethodType::as_signature(interface_method_type_oop(), true);

  Handle implementation_member_oop(THREAD, JNIHandles::resolve_non_null(implementationMember));
  assert(java_lang_invoke_MemberName::is_method(implementation_member_oop()), "must be");
  Method* m = java_lang_invoke_MemberName::vmtarget(implementation_member_oop());

  Handle dynamic_method_type_oop(THREAD, JNIHandles::resolve_non_null(dynamicMethodType));
  Symbol* dynamic_method_type = java_lang_invoke_MethodType::as_signature(dynamic_method_type_oop(), true);

  InstanceKlass* lambda_ik = SystemDictionaryShared::get_shared_lambda_proxy_class(caller_ik, interface_method_name, factory_type,
                                                                                   interface_method_type, m, dynamic_method_type);
  jclass jcls = NULL;
  if (lambda_ik != NULL) {
    InstanceKlass* loaded_lambda = SystemDictionaryShared::prepare_shared_lambda_proxy_class(lambda_ik, caller_ik, THREAD);
    jcls = loaded_lambda == NULL ? NULL : (jclass) JNIHandles::make_local(THREAD, loaded_lambda->java_mirror());
  }
  return jcls;
#else
  return NULL;
#endif // INCLUDE_CDS
JVM_END

JVM_ENTRY(jboolean, JVM_IsCDSDumpingEnabled(JNIEnv* env))
  return Arguments::is_dumping_archive();
JVM_END

JVM_ENTRY(jboolean, JVM_IsSharingEnabled(JNIEnv* env))
  return UseSharedSpaces;
JVM_END

JVM_ENTRY_NO_ENV(jlong, JVM_GetRandomSeedForDumping())
  if (DumpSharedSpaces) {
    const char* release = Abstract_VM_Version::vm_release();
    const char* dbg_level = Abstract_VM_Version::jdk_debug_level();
    const char* version = VM_Version::internal_vm_info_string();
    jlong seed = (jlong)(java_lang_String::hash_code((const jbyte*)release, (int)strlen(release)) ^
                         java_lang_String::hash_code((const jbyte*)dbg_level, (int)strlen(dbg_level)) ^
                         java_lang_String::hash_code((const jbyte*)version, (int)strlen(version)));
    seed += (jlong)Abstract_VM_Version::vm_major_version();
    seed += (jlong)Abstract_VM_Version::vm_minor_version();
    seed += (jlong)Abstract_VM_Version::vm_security_version();
    seed += (jlong)Abstract_VM_Version::vm_patch_version();
    if (seed == 0) { // don't let this ever be zero.
      seed = 0x87654321;
    }
    log_debug(cds)("JVM_GetRandomSeedForDumping() = " JLONG_FORMAT, seed);
    return seed;
  } else {
    return 0;
  }
JVM_END

JVM_ENTRY(jboolean, JVM_IsDumpingClassList(JNIEnv *env))
#if INCLUDE_CDS
  return ClassListWriter::is_enabled() || DynamicDumpSharedSpaces;
#else
  return false;
#endif // INCLUDE_CDS
JVM_END

JVM_ENTRY(void, JVM_LogLambdaFormInvoker(JNIEnv *env, jstring line))
#if INCLUDE_CDS
  assert(ClassListWriter::is_enabled() || DynamicDumpSharedSpaces,  "Should be set and open or do dynamic dump");
  if (line != NULL) {
    ResourceMark rm(THREAD);
    Handle h_line (THREAD, JNIHandles::resolve_non_null(line));
    char* c_line = java_lang_String::as_utf8_string(h_line());
    if (DynamicDumpSharedSpaces) {
      // Note: LambdaFormInvokers::append_filtered and LambdaFormInvokers::append take same format which is not
      // same as below the print format. The line does not include LAMBDA_FORM_TAG.
      LambdaFormInvokers::append_filtered(os::strdup((const char*)c_line, mtInternal));
    }
    if (ClassListWriter::is_enabled()) {
      ClassListWriter w;
      w.stream()->print_cr("%s %s", LAMBDA_FORM_TAG, c_line);
    }
  }
#endif // INCLUDE_CDS
JVM_END

JVM_ENTRY(void, JVM_DumpClassListToFile(JNIEnv *env, jstring listFileName))
#if INCLUDE_CDS
  ResourceMark rm(THREAD);
  Handle file_handle(THREAD, JNIHandles::resolve_non_null(listFileName));
  char* file_name  = java_lang_String::as_utf8_string(file_handle());
  MetaspaceShared::dump_loaded_classes(file_name, THREAD);
#endif // INCLUDE_CDS
JVM_END

JVM_ENTRY(void, JVM_DumpDynamicArchive(JNIEnv *env, jstring archiveName))
#if INCLUDE_CDS
  ResourceMark rm(THREAD);
  Handle file_handle(THREAD, JNIHandles::resolve_non_null(archiveName));
  char* archive_name  = java_lang_String::as_utf8_string(file_handle());
  DynamicArchive::dump(archive_name, CHECK);
#endif // INCLUDE_CDS
JVM_END

// Returns an array of all live Thread objects (VM internal JavaThreads,
// jvmti agent threads, and JNI attaching threads  are skipped)
// See CR 6404306 regarding JNI attaching threads
JVM_ENTRY(jobjectArray, JVM_GetAllThreads(JNIEnv *env, jclass dummy))
  ResourceMark rm(THREAD);
  ThreadsListEnumerator tle(THREAD, false, false);
  JvmtiVMObjectAllocEventCollector oam;

  int num_threads = tle.num_threads();
  objArrayOop r = oopFactory::new_objArray(vmClasses::Thread_klass(), num_threads, CHECK_NULL);
  objArrayHandle threads_ah(THREAD, r);

  for (int i = 0; i < num_threads; i++) {
    Handle h = tle.get_threadObj(i);
    threads_ah->obj_at_put(i, h());
  }

  return (jobjectArray) JNIHandles::make_local(THREAD, threads_ah());
JVM_END


// Support for java.lang.Thread.getStackTrace() and getAllStackTraces() methods
// Return StackTraceElement[][], each element is the stack trace of a thread in
// the corresponding entry in the given threads array
JVM_ENTRY(jobjectArray, JVM_DumpThreads(JNIEnv *env, jclass threadClass, jobjectArray threads))
  JvmtiVMObjectAllocEventCollector oam;

  // Check if threads is null
  if (threads == NULL) {
    THROW_(vmSymbols::java_lang_NullPointerException(), 0);
  }

  objArrayOop a = objArrayOop(JNIHandles::resolve_non_null(threads));
  objArrayHandle ah(THREAD, a);
  int num_threads = ah->length();
  // check if threads is non-empty array
  if (num_threads == 0) {
    THROW_(vmSymbols::java_lang_IllegalArgumentException(), 0);
  }

  // check if threads is not an array of objects of Thread class
  Klass* k = ObjArrayKlass::cast(ah->klass())->element_klass();
  if (k != vmClasses::Thread_klass()) {
    THROW_(vmSymbols::java_lang_IllegalArgumentException(), 0);
  }

  ResourceMark rm(THREAD);

  GrowableArray<instanceHandle>* thread_handle_array = new GrowableArray<instanceHandle>(num_threads);
  for (int i = 0; i < num_threads; i++) {
    oop thread_obj = ah->obj_at(i);
    instanceHandle h(THREAD, (instanceOop) thread_obj);
    thread_handle_array->append(h);
  }

  // The JavaThread references in thread_handle_array are validated
  // in VM_ThreadDump::doit().
  Handle stacktraces = ThreadService::dump_stack_traces(thread_handle_array, num_threads, CHECK_NULL);
  return (jobjectArray)JNIHandles::make_local(THREAD, stacktraces());

JVM_END

// JVM monitoring and management support
JVM_ENTRY_NO_ENV(void*, JVM_GetManagement(jint version))
  return Management::get_jmm_interface(version);
JVM_END

// com.sun.tools.attach.VirtualMachine agent properties support
//
// Initialize the agent properties with the properties maintained in the VM
JVM_ENTRY(jobject, JVM_InitAgentProperties(JNIEnv *env, jobject properties))
  ResourceMark rm;

  Handle props(THREAD, JNIHandles::resolve_non_null(properties));

  PUTPROP(props, "sun.java.command", Arguments::java_command());
  PUTPROP(props, "sun.jvm.flags", Arguments::jvm_flags());
  PUTPROP(props, "sun.jvm.args", Arguments::jvm_args());
  return properties;
JVM_END

JVM_ENTRY(jobjectArray, JVM_GetEnclosingMethodInfo(JNIEnv *env, jclass ofClass))
{
  JvmtiVMObjectAllocEventCollector oam;

  if (ofClass == NULL) {
    return NULL;
  }
  Handle mirror(THREAD, JNIHandles::resolve_non_null(ofClass));
  // Special handling for primitive objects
  if (java_lang_Class::is_primitive(mirror())) {
    return NULL;
  }
  Klass* k = java_lang_Class::as_Klass(mirror());
  if (!k->is_instance_klass()) {
    return NULL;
  }
  InstanceKlass* ik = InstanceKlass::cast(k);
  int encl_method_class_idx = ik->enclosing_method_class_index();
  if (encl_method_class_idx == 0) {
    return NULL;
  }
  objArrayOop dest_o = oopFactory::new_objArray(vmClasses::Object_klass(), 3, CHECK_NULL);
  objArrayHandle dest(THREAD, dest_o);
  Klass* enc_k = ik->constants()->klass_at(encl_method_class_idx, CHECK_NULL);
  dest->obj_at_put(0, enc_k->java_mirror());
  int encl_method_method_idx = ik->enclosing_method_method_index();
  if (encl_method_method_idx != 0) {
    Symbol* sym = ik->constants()->symbol_at(
                        extract_low_short_from_int(
                          ik->constants()->name_and_type_at(encl_method_method_idx)));
    Handle str = java_lang_String::create_from_symbol(sym, CHECK_NULL);
    dest->obj_at_put(1, str());
    sym = ik->constants()->symbol_at(
              extract_high_short_from_int(
                ik->constants()->name_and_type_at(encl_method_method_idx)));
    str = java_lang_String::create_from_symbol(sym, CHECK_NULL);
    dest->obj_at_put(2, str());
  }
  return (jobjectArray) JNIHandles::make_local(THREAD, dest());
}
JVM_END

// Returns an array of java.lang.String objects containing the input arguments to the VM.
JVM_ENTRY(jobjectArray, JVM_GetVmArguments(JNIEnv *env))
  ResourceMark rm(THREAD);

  if (Arguments::num_jvm_args() == 0 && Arguments::num_jvm_flags() == 0) {
    return NULL;
  }

  char** vm_flags = Arguments::jvm_flags_array();
  char** vm_args = Arguments::jvm_args_array();
  int num_flags = Arguments::num_jvm_flags();
  int num_args = Arguments::num_jvm_args();

  InstanceKlass* ik = vmClasses::String_klass();
  objArrayOop r = oopFactory::new_objArray(ik, num_args + num_flags, CHECK_NULL);
  objArrayHandle result_h(THREAD, r);

  int index = 0;
  for (int j = 0; j < num_flags; j++, index++) {
    Handle h = java_lang_String::create_from_platform_dependent_str(vm_flags[j], CHECK_NULL);
    result_h->obj_at_put(index, h());
  }
  for (int i = 0; i < num_args; i++, index++) {
    Handle h = java_lang_String::create_from_platform_dependent_str(vm_args[i], CHECK_NULL);
    result_h->obj_at_put(index, h());
  }
  return (jobjectArray) JNIHandles::make_local(THREAD, result_h());
JVM_END

JVM_ENTRY_NO_ENV(jint, JVM_FindSignal(const char *name))
  return os::get_signal_number(name);
JVM_END
