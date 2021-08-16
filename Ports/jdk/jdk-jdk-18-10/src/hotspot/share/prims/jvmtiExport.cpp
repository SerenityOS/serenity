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
#include "classfile/javaClasses.inline.hpp"
#include "classfile/moduleEntry.hpp"
#include "classfile/vmClasses.hpp"
#include "classfile/vmSymbols.hpp"
#include "code/nmethod.hpp"
#include "code/pcDesc.hpp"
#include "code/scopeDesc.hpp"
#include "gc/shared/oopStorageSet.hpp"
#include "interpreter/interpreter.hpp"
#include "jvmtifiles/jvmtiEnv.hpp"
#include "logging/log.hpp"
#include "logging/logStream.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/klass.inline.hpp"
#include "oops/objArrayKlass.hpp"
#include "oops/objArrayOop.hpp"
#include "oops/oop.inline.hpp"
#include "oops/oopHandle.inline.hpp"
#include "prims/jvmtiCodeBlobEvents.hpp"
#include "prims/jvmtiEventController.hpp"
#include "prims/jvmtiEventController.inline.hpp"
#include "prims/jvmtiExport.hpp"
#include "prims/jvmtiImpl.hpp"
#include "prims/jvmtiManageCapabilities.hpp"
#include "prims/jvmtiRawMonitor.hpp"
#include "prims/jvmtiRedefineClasses.hpp"
#include "prims/jvmtiTagMap.hpp"
#include "prims/jvmtiThreadState.inline.hpp"
#include "runtime/arguments.hpp"
#include "runtime/fieldDescriptor.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/javaCalls.hpp"
#include "runtime/jniHandles.inline.hpp"
#include "runtime/objectMonitor.hpp"
#include "runtime/objectMonitor.inline.hpp"
#include "runtime/os.hpp"
#include "runtime/osThread.hpp"
#include "runtime/safepointVerifiers.hpp"
#include "runtime/serviceThread.hpp"
#include "runtime/thread.inline.hpp"
#include "runtime/threadSMR.hpp"
#include "runtime/vframe.inline.hpp"
#include "runtime/vm_version.hpp"
#include "utilities/macros.hpp"

#ifdef JVMTI_TRACE
#define EVT_TRACE(evt,out) if ((JvmtiTrace::event_trace_flags(evt) & JvmtiTrace::SHOW_EVENT_SENT) != 0) { SafeResourceMark rm; log_trace(jvmti) out; }
#define EVT_TRIG_TRACE(evt,out) if ((JvmtiTrace::event_trace_flags(evt) & JvmtiTrace::SHOW_EVENT_TRIGGER) != 0) { SafeResourceMark rm; log_trace(jvmti) out; }
#else
#define EVT_TRIG_TRACE(evt,out)
#define EVT_TRACE(evt,out)
#endif

///////////////////////////////////////////////////////////////
//
// JvmtiEventTransition
//
// TO DO --
//  more handle purging

// Use this for JavaThreads and state is  _thread_in_vm.
class JvmtiJavaThreadEventTransition : StackObj {
private:
  ResourceMark _rm;
  ThreadToNativeFromVM _transition;
  HandleMark _hm;

public:
  JvmtiJavaThreadEventTransition(JavaThread *thread) :
    _rm(),
    _transition(thread),
    _hm(thread)  {};
};

// For JavaThreads which are not in _thread_in_vm state
// and other system threads use this.
class JvmtiThreadEventTransition : StackObj {
private:
  ResourceMark _rm;
  HandleMark _hm;
  JavaThreadState _saved_state;
  JavaThread *_jthread;

public:
  JvmtiThreadEventTransition(Thread *thread) : _rm(), _hm(thread) {
    if (thread->is_Java_thread()) {
       _jthread = JavaThread::cast(thread);
       _saved_state = _jthread->thread_state();
       if (_saved_state == _thread_in_Java) {
         ThreadStateTransition::transition_from_java(_jthread, _thread_in_native);
       } else {
         ThreadStateTransition::transition(_jthread, _saved_state, _thread_in_native);
       }
    } else {
      _jthread = NULL;
    }
  }

  ~JvmtiThreadEventTransition() {
    if (_jthread != NULL)
      ThreadStateTransition::transition_from_native(_jthread, _saved_state);
  }
};


///////////////////////////////////////////////////////////////
//
// JvmtiEventMark
//

class JvmtiEventMark : public StackObj {
private:
  JavaThread *_thread;
  JNIEnv* _jni_env;
  JvmtiThreadState::ExceptionState _saved_exception_state;
#if 0
  JNIHandleBlock* _hblock;
#endif

public:
  JvmtiEventMark(JavaThread *thread) :  _thread(thread),
                                        _jni_env(thread->jni_environment()),
                                        _saved_exception_state(JvmtiThreadState::ES_CLEARED) {
#if 0
    _hblock = thread->active_handles();
    _hblock->clear_thoroughly(); // so we can be safe
#else
    // we want to use the code above - but that needs the JNIHandle changes - later...
    // for now, steal JNI push local frame code
    JvmtiThreadState *state = thread->jvmti_thread_state();
    // we are before an event.
    // Save current jvmti thread exception state.
    if (state != NULL) {
      _saved_exception_state = state->get_exception_state();
    }

    JNIHandleBlock* old_handles = thread->active_handles();
    JNIHandleBlock* new_handles = JNIHandleBlock::allocate_block(thread);
    assert(new_handles != NULL, "should not be NULL");
    new_handles->set_pop_frame_link(old_handles);
    thread->set_active_handles(new_handles);
#endif
    assert(thread == JavaThread::current(), "thread must be current!");
    thread->frame_anchor()->make_walkable(thread);
  };

  ~JvmtiEventMark() {
#if 0
    _hblock->clear(); // for consistency with future correct behavior
#else
    // we want to use the code above - but that needs the JNIHandle changes - later...
    // for now, steal JNI pop local frame code
    JNIHandleBlock* old_handles = _thread->active_handles();
    JNIHandleBlock* new_handles = old_handles->pop_frame_link();
    assert(new_handles != NULL, "should not be NULL");
    _thread->set_active_handles(new_handles);
    // Note that we set the pop_frame_link to NULL explicitly, otherwise
    // the release_block call will release the blocks.
    old_handles->set_pop_frame_link(NULL);
    JNIHandleBlock::release_block(old_handles, _thread); // may block
#endif

    JvmtiThreadState* state = _thread->jvmti_thread_state();
    // we are continuing after an event.
    if (state != NULL) {
      // Restore the jvmti thread exception state.
      state->restore_exception_state(_saved_exception_state);
    }
  }

#if 0
  jobject to_jobject(oop obj) { return obj == NULL? NULL : _hblock->allocate_handle_fast(obj); }
#else
  // we want to use the code above - but that needs the JNIHandle changes - later...
  // for now, use regular make_local
  jobject to_jobject(oop obj) { return JNIHandles::make_local(_thread,obj); }
#endif

  jclass to_jclass(Klass* klass) { return (klass == NULL ? NULL : (jclass)to_jobject(klass->java_mirror())); }

  jmethodID to_jmethodID(const methodHandle& method) { return method->jmethod_id(); }

  JNIEnv* jni_env() { return _jni_env; }
};

class JvmtiThreadEventMark : public JvmtiEventMark {
private:
  jthread _jt;

public:
  JvmtiThreadEventMark(JavaThread *thread) :
    JvmtiEventMark(thread) {
    _jt = (jthread)(to_jobject(thread->threadObj()));
  };
 jthread jni_thread() { return _jt; }
};

class JvmtiClassEventMark : public JvmtiThreadEventMark {
private:
  jclass _jc;

public:
  JvmtiClassEventMark(JavaThread *thread, Klass* klass) :
    JvmtiThreadEventMark(thread) {
    _jc = to_jclass(klass);
  };
  jclass jni_class() { return _jc; }
};

class JvmtiMethodEventMark : public JvmtiThreadEventMark {
private:
  jmethodID _mid;

public:
  JvmtiMethodEventMark(JavaThread *thread, const methodHandle& method) :
    JvmtiThreadEventMark(thread),
    _mid(to_jmethodID(method)) {};
  jmethodID jni_methodID() { return _mid; }
};

class JvmtiLocationEventMark : public JvmtiMethodEventMark {
private:
  jlocation _loc;

public:
  JvmtiLocationEventMark(JavaThread *thread, const methodHandle& method, address location) :
    JvmtiMethodEventMark(thread, method),
    _loc(location - method->code_base()) {};
  jlocation location() { return _loc; }
};

class JvmtiExceptionEventMark : public JvmtiLocationEventMark {
private:
  jobject _exc;

public:
  JvmtiExceptionEventMark(JavaThread *thread, const methodHandle& method, address location, Handle exception) :
    JvmtiLocationEventMark(thread, method, location),
    _exc(to_jobject(exception())) {};
  jobject exception() { return _exc; }
};

class JvmtiClassFileLoadEventMark : public JvmtiThreadEventMark {
private:
  const char *_class_name;
  jobject _jloader;
  jobject _protection_domain;
  jclass  _class_being_redefined;

public:
  JvmtiClassFileLoadEventMark(JavaThread *thread, Symbol* name,
     Handle class_loader, Handle prot_domain, Klass* class_being_redefined) : JvmtiThreadEventMark(thread) {
      _class_name = name != NULL? name->as_utf8() : NULL;
      _jloader = (jobject)to_jobject(class_loader());
      _protection_domain = (jobject)to_jobject(prot_domain());
      if (class_being_redefined == NULL) {
        _class_being_redefined = NULL;
      } else {
        _class_being_redefined = (jclass)to_jclass(class_being_redefined);
      }
  };
  const char *class_name() {
    return _class_name;
  }
  jobject jloader() {
    return _jloader;
  }
  jobject protection_domain() {
    return _protection_domain;
  }
  jclass class_being_redefined() {
    return _class_being_redefined;
  }
};

//////////////////////////////////////////////////////////////////////////////

int               JvmtiExport::_field_access_count                        = 0;
int               JvmtiExport::_field_modification_count                  = 0;

bool              JvmtiExport::_can_access_local_variables                = false;
bool              JvmtiExport::_can_hotswap_or_post_breakpoint            = false;
bool              JvmtiExport::_can_modify_any_class                      = false;
bool              JvmtiExport::_can_walk_any_space                        = false;

uint64_t          JvmtiExport::_redefinition_count                        = 0;
bool              JvmtiExport::_all_dependencies_are_recorded             = false;

//
// field access management
//

// interpreter generator needs the address of the counter
address JvmtiExport::get_field_access_count_addr() {
  // We don't grab a lock because we don't want to
  // serialize field access between all threads. This means that a
  // thread on another processor can see the wrong count value and
  // may either miss making a needed call into post_field_access()
  // or will make an unneeded call into post_field_access(). We pay
  // this price to avoid slowing down the VM when we aren't watching
  // field accesses.
  // Other access/mutation safe by virtue of being in VM state.
  return (address)(&_field_access_count);
}

//
// field modification management
//

// interpreter generator needs the address of the counter
address JvmtiExport::get_field_modification_count_addr() {
  // We don't grab a lock because we don't
  // want to serialize field modification between all threads. This
  // means that a thread on another processor can see the wrong
  // count value and may either miss making a needed call into
  // post_field_modification() or will make an unneeded call into
  // post_field_modification(). We pay this price to avoid slowing
  // down the VM when we aren't watching field modifications.
  // Other access/mutation safe by virtue of being in VM state.
  return (address)(&_field_modification_count);
}


///////////////////////////////////////////////////////////////
// Functions needed by java.lang.instrument for starting up javaagent.
///////////////////////////////////////////////////////////////

jint
JvmtiExport::get_jvmti_interface(JavaVM *jvm, void **penv, jint version) {
  // The JVMTI_VERSION_INTERFACE_JVMTI part of the version number
  // has already been validated in JNI GetEnv().
  int major, minor, micro;

  // micro version doesn't matter here (yet?)
  decode_version_values(version, &major, &minor, &micro);
  switch (major) {
    case 1:
      switch (minor) {
        case 0:  // version 1.0.<micro> is recognized
        case 1:  // version 1.1.<micro> is recognized
        case 2:  // version 1.2.<micro> is recognized
          break;

        default:
          return JNI_EVERSION;  // unsupported minor version number
      }
      break;
    case 9:
      switch (minor) {
        case 0:  // version 9.0.<micro> is recognized
          break;
        default:
          return JNI_EVERSION;  // unsupported minor version number
      }
      break;
    case 11:
      switch (minor) {
        case 0:  // version 11.0.<micro> is recognized
          break;
        default:
          return JNI_EVERSION;  // unsupported minor version number
      }
      break;
    default:
      // Starting from 13 we do not care about minor version anymore
      if (major < 13 || major > Abstract_VM_Version::vm_major_version()) {
        return JNI_EVERSION;  // unsupported major version number
      }
  }

  if (JvmtiEnv::get_phase() == JVMTI_PHASE_LIVE) {
    JavaThread* current_thread = JavaThread::current();
    // transition code: native to VM
    ThreadInVMfromNative __tiv(current_thread);
    VM_ENTRY_BASE(jvmtiEnv*, JvmtiExport::get_jvmti_interface, current_thread)
    debug_only(VMNativeEntryWrapper __vew;)

    JvmtiEnv *jvmti_env = JvmtiEnv::create_a_jvmti(version);
    *penv = jvmti_env->jvmti_external();  // actual type is jvmtiEnv* -- not to be confused with JvmtiEnv*
    return JNI_OK;

  } else if (JvmtiEnv::get_phase() == JVMTI_PHASE_ONLOAD) {
    // not live, no thread to transition
    JvmtiEnv *jvmti_env = JvmtiEnv::create_a_jvmti(version);
    *penv = jvmti_env->jvmti_external();  // actual type is jvmtiEnv* -- not to be confused with JvmtiEnv*
    return JNI_OK;

  } else {
    // Called at the wrong time
    *penv = NULL;
    return JNI_EDETACHED;
  }
}

void
JvmtiExport::add_default_read_edges(Handle h_module, TRAPS) {
  if (!Universe::is_module_initialized()) {
    return; // extra safety
  }
  assert(!h_module.is_null(), "module should always be set");

  // Invoke the transformedByAgent method
  JavaValue result(T_VOID);
  JavaCalls::call_static(&result,
                         vmClasses::module_Modules_klass(),
                         vmSymbols::transformedByAgent_name(),
                         vmSymbols::transformedByAgent_signature(),
                         h_module,
                         THREAD);

  if (HAS_PENDING_EXCEPTION) {
    LogTarget(Trace, jvmti) log;
    LogStream log_stream(log);
    java_lang_Throwable::print(PENDING_EXCEPTION, &log_stream);
    log_stream.cr();
    CLEAR_PENDING_EXCEPTION;
    return;
  }
}

jvmtiError
JvmtiExport::add_module_reads(Handle module, Handle to_module, TRAPS) {
  if (!Universe::is_module_initialized()) {
    return JVMTI_ERROR_NONE; // extra safety
  }
  assert(!module.is_null(), "module should always be set");
  assert(!to_module.is_null(), "to_module should always be set");

  // Invoke the addReads method
  JavaValue result(T_VOID);
  JavaCalls::call_static(&result,
                         vmClasses::module_Modules_klass(),
                         vmSymbols::addReads_name(),
                         vmSymbols::addReads_signature(),
                         module,
                         to_module,
                         THREAD);

  if (HAS_PENDING_EXCEPTION) {
    LogTarget(Trace, jvmti) log;
    LogStream log_stream(log);
    java_lang_Throwable::print(PENDING_EXCEPTION, &log_stream);
    log_stream.cr();
    CLEAR_PENDING_EXCEPTION;
    return JVMTI_ERROR_INTERNAL;
  }
  return JVMTI_ERROR_NONE;
}

jvmtiError
JvmtiExport::add_module_exports(Handle module, Handle pkg_name, Handle to_module, TRAPS) {
  if (!Universe::is_module_initialized()) {
    return JVMTI_ERROR_NONE; // extra safety
  }
  assert(!module.is_null(), "module should always be set");
  assert(!to_module.is_null(), "to_module should always be set");
  assert(!pkg_name.is_null(), "pkg_name should always be set");

  // Invoke the addExports method
  JavaValue result(T_VOID);
  JavaCalls::call_static(&result,
                         vmClasses::module_Modules_klass(),
                         vmSymbols::addExports_name(),
                         vmSymbols::addExports_signature(),
                         module,
                         pkg_name,
                         to_module,
                         THREAD);

  if (HAS_PENDING_EXCEPTION) {
    Symbol* ex_name = PENDING_EXCEPTION->klass()->name();
    LogTarget(Trace, jvmti) log;
    LogStream log_stream(log);
    java_lang_Throwable::print(PENDING_EXCEPTION, &log_stream);
    log_stream.cr();
    CLEAR_PENDING_EXCEPTION;
    if (ex_name == vmSymbols::java_lang_IllegalArgumentException()) {
      return JVMTI_ERROR_ILLEGAL_ARGUMENT;
    }
    return JVMTI_ERROR_INTERNAL;
  }
  return JVMTI_ERROR_NONE;
}

jvmtiError
JvmtiExport::add_module_opens(Handle module, Handle pkg_name, Handle to_module, TRAPS) {
  if (!Universe::is_module_initialized()) {
    return JVMTI_ERROR_NONE; // extra safety
  }
  assert(!module.is_null(), "module should always be set");
  assert(!to_module.is_null(), "to_module should always be set");
  assert(!pkg_name.is_null(), "pkg_name should always be set");

  // Invoke the addOpens method
  JavaValue result(T_VOID);
  JavaCalls::call_static(&result,
                         vmClasses::module_Modules_klass(),
                         vmSymbols::addOpens_name(),
                         vmSymbols::addExports_signature(),
                         module,
                         pkg_name,
                         to_module,
                         THREAD);

  if (HAS_PENDING_EXCEPTION) {
    Symbol* ex_name = PENDING_EXCEPTION->klass()->name();
    LogTarget(Trace, jvmti) log;
    LogStream log_stream(log);
    java_lang_Throwable::print(PENDING_EXCEPTION, &log_stream);
    log_stream.cr();
    CLEAR_PENDING_EXCEPTION;
    if (ex_name == vmSymbols::java_lang_IllegalArgumentException()) {
      return JVMTI_ERROR_ILLEGAL_ARGUMENT;
    }
    return JVMTI_ERROR_INTERNAL;
  }
  return JVMTI_ERROR_NONE;
}

jvmtiError
JvmtiExport::add_module_uses(Handle module, Handle service, TRAPS) {
  if (!Universe::is_module_initialized()) {
    return JVMTI_ERROR_NONE; // extra safety
  }
  assert(!module.is_null(), "module should always be set");
  assert(!service.is_null(), "service should always be set");

  // Invoke the addUses method
  JavaValue result(T_VOID);
  JavaCalls::call_static(&result,
                         vmClasses::module_Modules_klass(),
                         vmSymbols::addUses_name(),
                         vmSymbols::addUses_signature(),
                         module,
                         service,
                         THREAD);

  if (HAS_PENDING_EXCEPTION) {
    LogTarget(Trace, jvmti) log;
    LogStream log_stream(log);
    java_lang_Throwable::print(PENDING_EXCEPTION, &log_stream);
    log_stream.cr();
    CLEAR_PENDING_EXCEPTION;
    return JVMTI_ERROR_INTERNAL;
  }
  return JVMTI_ERROR_NONE;
}

jvmtiError
JvmtiExport::add_module_provides(Handle module, Handle service, Handle impl_class, TRAPS) {
  if (!Universe::is_module_initialized()) {
    return JVMTI_ERROR_NONE; // extra safety
  }
  assert(!module.is_null(), "module should always be set");
  assert(!service.is_null(), "service should always be set");
  assert(!impl_class.is_null(), "impl_class should always be set");

  // Invoke the addProvides method
  JavaValue result(T_VOID);
  JavaCalls::call_static(&result,
                         vmClasses::module_Modules_klass(),
                         vmSymbols::addProvides_name(),
                         vmSymbols::addProvides_signature(),
                         module,
                         service,
                         impl_class,
                         THREAD);

  if (HAS_PENDING_EXCEPTION) {
    LogTarget(Trace, jvmti) log;
    LogStream log_stream(log);
    java_lang_Throwable::print(PENDING_EXCEPTION, &log_stream);
    log_stream.cr();
    CLEAR_PENDING_EXCEPTION;
    return JVMTI_ERROR_INTERNAL;
  }
  return JVMTI_ERROR_NONE;
}

void
JvmtiExport::decode_version_values(jint version, int * major, int * minor,
                                   int * micro) {
  *major = (version & JVMTI_VERSION_MASK_MAJOR) >> JVMTI_VERSION_SHIFT_MAJOR;
  *minor = (version & JVMTI_VERSION_MASK_MINOR) >> JVMTI_VERSION_SHIFT_MINOR;
  *micro = (version & JVMTI_VERSION_MASK_MICRO) >> JVMTI_VERSION_SHIFT_MICRO;
}

void JvmtiExport::enter_primordial_phase() {
  JvmtiEnvBase::set_phase(JVMTI_PHASE_PRIMORDIAL);
}

void JvmtiExport::enter_early_start_phase() {
  set_early_vmstart_recorded(true);
}

void JvmtiExport::enter_start_phase() {
  JvmtiEnvBase::set_phase(JVMTI_PHASE_START);
}

void JvmtiExport::enter_onload_phase() {
  JvmtiEnvBase::set_phase(JVMTI_PHASE_ONLOAD);
}

void JvmtiExport::enter_live_phase() {
  JvmtiEnvBase::set_phase(JVMTI_PHASE_LIVE);
}

//
// JVMTI events that the VM posts to the debugger and also startup agent
// and call the agent's premain() for java.lang.instrument.
//

void JvmtiExport::post_early_vm_start() {
  EVT_TRIG_TRACE(JVMTI_EVENT_VM_START, ("Trg Early VM start event triggered" ));

  // can now enable some events
  JvmtiEventController::vm_start();

  JvmtiEnvIterator it;
  for (JvmtiEnv* env = it.first(); env != NULL; env = it.next(env)) {
    // Only early vmstart envs post early VMStart event
    if (env->early_vmstart_env() && env->is_enabled(JVMTI_EVENT_VM_START)) {
      EVT_TRACE(JVMTI_EVENT_VM_START, ("Evt Early VM start event sent" ));
      JavaThread *thread  = JavaThread::current();
      JvmtiThreadEventMark jem(thread);
      JvmtiJavaThreadEventTransition jet(thread);
      jvmtiEventVMStart callback = env->callbacks()->VMStart;
      if (callback != NULL) {
        (*callback)(env->jvmti_external(), jem.jni_env());
      }
    }
  }
}

void JvmtiExport::post_vm_start() {
  EVT_TRIG_TRACE(JVMTI_EVENT_VM_START, ("Trg VM start event triggered" ));

  // can now enable some events
  JvmtiEventController::vm_start();

  JvmtiEnvIterator it;
  for (JvmtiEnv* env = it.first(); env != NULL; env = it.next(env)) {
    // Early vmstart envs do not post normal VMStart event
    if (!env->early_vmstart_env() && env->is_enabled(JVMTI_EVENT_VM_START)) {
      EVT_TRACE(JVMTI_EVENT_VM_START, ("Evt VM start event sent" ));

      JavaThread *thread  = JavaThread::current();
      JvmtiThreadEventMark jem(thread);
      JvmtiJavaThreadEventTransition jet(thread);
      jvmtiEventVMStart callback = env->callbacks()->VMStart;
      if (callback != NULL) {
        (*callback)(env->jvmti_external(), jem.jni_env());
      }
    }
  }
}

static OopStorage* _jvmti_oop_storage = NULL;
static OopStorage* _weak_tag_storage = NULL;

OopStorage* JvmtiExport::jvmti_oop_storage() {
  assert(_jvmti_oop_storage != NULL, "not yet initialized");
  return _jvmti_oop_storage;
}

OopStorage* JvmtiExport::weak_tag_storage() {
  assert(_weak_tag_storage != NULL, "not yet initialized");
  return _weak_tag_storage;
}

void JvmtiExport::initialize_oop_storage() {
  // OopStorage needs to be created early in startup and unconditionally
  // because of OopStorageSet static array indices.
  _jvmti_oop_storage = OopStorageSet::create_strong("JVMTI OopStorage", mtServiceability);
  _weak_tag_storage  = OopStorageSet::create_weak("JVMTI Tag Weak OopStorage", mtServiceability);
  _weak_tag_storage->register_num_dead_callback(&JvmtiTagMap::gc_notification);
}

void JvmtiExport::post_vm_initialized() {
  EVT_TRIG_TRACE(JVMTI_EVENT_VM_INIT, ("Trg VM init event triggered" ));

  // can now enable events
  JvmtiEventController::vm_init();

  JvmtiEnvIterator it;
  for (JvmtiEnv* env = it.first(); env != NULL; env = it.next(env)) {
    if (env->is_enabled(JVMTI_EVENT_VM_INIT)) {
      EVT_TRACE(JVMTI_EVENT_VM_INIT, ("Evt VM init event sent" ));

      JavaThread *thread  = JavaThread::current();
      JvmtiThreadEventMark jem(thread);
      JvmtiJavaThreadEventTransition jet(thread);
      jvmtiEventVMInit callback = env->callbacks()->VMInit;
      if (callback != NULL) {
        (*callback)(env->jvmti_external(), jem.jni_env(), jem.jni_thread());
      }
    }
  }
}


void JvmtiExport::post_vm_death() {
  EVT_TRIG_TRACE(JVMTI_EVENT_VM_DEATH, ("Trg VM death event triggered" ));

  JvmtiEnvIterator it;
  for (JvmtiEnv* env = it.first(); env != NULL; env = it.next(env)) {
    if (env->is_enabled(JVMTI_EVENT_VM_DEATH)) {
      EVT_TRACE(JVMTI_EVENT_VM_DEATH, ("Evt VM death event sent" ));

      JavaThread *thread  = JavaThread::current();
      JvmtiEventMark jem(thread);
      JvmtiJavaThreadEventTransition jet(thread);
      jvmtiEventVMDeath callback = env->callbacks()->VMDeath;
      if (callback != NULL) {
        (*callback)(env->jvmti_external(), jem.jni_env());
      }
    }
  }

  JvmtiEnvBase::set_phase(JVMTI_PHASE_DEAD);
  JvmtiEventController::vm_death();
}

char**
JvmtiExport::get_all_native_method_prefixes(int* count_ptr) {
  // Have to grab JVMTI thread state lock to be sure environment doesn't
  // go away while we iterate them.  No locks during VM bring-up.
  if (Threads::number_of_threads() == 0 || SafepointSynchronize::is_at_safepoint()) {
    return JvmtiEnvBase::get_all_native_method_prefixes(count_ptr);
  } else {
    MutexLocker mu(JvmtiThreadState_lock);
    return JvmtiEnvBase::get_all_native_method_prefixes(count_ptr);
  }
}

// Convert an external thread reference to a JavaThread found on the
// specified ThreadsList. The ThreadsListHandle in the caller "protects"
// the returned JavaThread *.
//
// If thread_oop_p is not NULL, then the caller wants to use the oop
// after this call so the oop is returned. On success, *jt_pp is set
// to the converted JavaThread * and JVMTI_ERROR_NONE is returned.
// On error, returns various JVMTI_ERROR_* values.
//
jvmtiError
JvmtiExport::cv_external_thread_to_JavaThread(ThreadsList * t_list,
                                              jthread thread,
                                              JavaThread ** jt_pp,
                                              oop * thread_oop_p) {
  assert(t_list != NULL, "must have a ThreadsList");
  assert(jt_pp != NULL, "must have a return JavaThread pointer");
  // thread_oop_p is optional so no assert()

  oop thread_oop = JNIHandles::resolve_external_guard(thread);
  if (thread_oop == NULL) {
    // NULL jthread, GC'ed jthread or a bad JNI handle.
    return JVMTI_ERROR_INVALID_THREAD;
  }
  // Looks like an oop at this point.

  if (!thread_oop->is_a(vmClasses::Thread_klass())) {
    // The oop is not a java.lang.Thread.
    return JVMTI_ERROR_INVALID_THREAD;
  }
  // Looks like a java.lang.Thread oop at this point.

  if (thread_oop_p != NULL) {
    // Return the oop to the caller; the caller may still want
    // the oop even if this function returns an error.
    *thread_oop_p = thread_oop;
  }

  JavaThread * java_thread = java_lang_Thread::thread(thread_oop);
  if (java_thread == NULL) {
    // The java.lang.Thread does not contain a JavaThread * so it has
    // not yet run or it has died.
    return JVMTI_ERROR_THREAD_NOT_ALIVE;
  }
  // Looks like a live JavaThread at this point.

  // We do not check the EnableThreadSMRExtraValidityChecks option
  // for this includes() call because JVM/TI's spec is tighter.
  if (!t_list->includes(java_thread)) {
    // Not on the JavaThreads list so it is not alive.
    return JVMTI_ERROR_THREAD_NOT_ALIVE;
  }

  // Return a live JavaThread that is "protected" by the
  // ThreadsListHandle in the caller.
  *jt_pp = java_thread;

  return JVMTI_ERROR_NONE;
}

// Convert an oop to a JavaThread found on the specified ThreadsList.
// The ThreadsListHandle in the caller "protects" the returned
// JavaThread *.
//
// On success, *jt_pp is set to the converted JavaThread * and
// JVMTI_ERROR_NONE is returned. On error, returns various
// JVMTI_ERROR_* values.
//
jvmtiError
JvmtiExport::cv_oop_to_JavaThread(ThreadsList * t_list, oop thread_oop,
                                  JavaThread ** jt_pp) {
  assert(t_list != NULL, "must have a ThreadsList");
  assert(thread_oop != NULL, "must have an oop");
  assert(jt_pp != NULL, "must have a return JavaThread pointer");

  if (!thread_oop->is_a(vmClasses::Thread_klass())) {
    // The oop is not a java.lang.Thread.
    return JVMTI_ERROR_INVALID_THREAD;
  }
  // Looks like a java.lang.Thread oop at this point.

  JavaThread * java_thread = java_lang_Thread::thread(thread_oop);
  if (java_thread == NULL) {
    // The java.lang.Thread does not contain a JavaThread * so it has
    // not yet run or it has died.
    return JVMTI_ERROR_THREAD_NOT_ALIVE;
  }
  // Looks like a live JavaThread at this point.

  // We do not check the EnableThreadSMRExtraValidityChecks option
  // for this includes() call because JVM/TI's spec is tighter.
  if (!t_list->includes(java_thread)) {
    // Not on the JavaThreads list so it is not alive.
    return JVMTI_ERROR_THREAD_NOT_ALIVE;
  }

  // Return a live JavaThread that is "protected" by the
  // ThreadsListHandle in the caller.
  *jt_pp = java_thread;

  return JVMTI_ERROR_NONE;
}

class JvmtiClassFileLoadHookPoster : public StackObj {
 private:
  Symbol*            _h_name;
  Handle               _class_loader;
  Handle               _h_protection_domain;
  unsigned char **     _data_ptr;
  unsigned char **     _end_ptr;
  JavaThread *         _thread;
  jint                 _curr_len;
  unsigned char *      _curr_data;
  JvmtiEnv *           _curr_env;
  JvmtiCachedClassFileData ** _cached_class_file_ptr;
  JvmtiThreadState *   _state;
  Klass*               _class_being_redefined;
  JvmtiClassLoadKind   _load_kind;
  bool                 _has_been_modified;

 public:
  inline JvmtiClassFileLoadHookPoster(Symbol* h_name, Handle class_loader,
                                      Handle h_protection_domain,
                                      unsigned char **data_ptr, unsigned char **end_ptr,
                                      JvmtiCachedClassFileData **cache_ptr) {
    _h_name = h_name;
    _class_loader = class_loader;
    _h_protection_domain = h_protection_domain;
    _data_ptr = data_ptr;
    _end_ptr = end_ptr;
    _thread = JavaThread::current();
    _curr_len = *end_ptr - *data_ptr;
    _curr_data = *data_ptr;
    _curr_env = NULL;
    _cached_class_file_ptr = cache_ptr;
    _has_been_modified = false;

    _state = _thread->jvmti_thread_state();
    if (_state != NULL) {
      _class_being_redefined = _state->get_class_being_redefined();
      _load_kind = _state->get_class_load_kind();
      Klass* klass = (_class_being_redefined == NULL) ? NULL : _class_being_redefined;
      if (_load_kind != jvmti_class_load_kind_load && klass != NULL) {
        ModuleEntry* module_entry = InstanceKlass::cast(klass)->module();
        assert(module_entry != NULL, "module_entry should always be set");
        if (module_entry->is_named() &&
            module_entry->module() != NULL &&
            !module_entry->has_default_read_edges()) {
          if (!module_entry->set_has_default_read_edges()) {
            // We won a potential race.
            // Add read edges to the unnamed modules of the bootstrap and app class loaders
            Handle class_module(_thread, module_entry->module()); // Obtain j.l.r.Module
            JvmtiExport::add_default_read_edges(class_module, _thread);
          }
        }
      }
      // Clear class_being_redefined flag here. The action
      // from agent handler could generate a new class file load
      // hook event and if it is not cleared the new event generated
      // from regular class file load could have this stale redefined
      // class handle info.
      _state->clear_class_being_redefined();
    } else {
      // redefine and retransform will always set the thread state
      _class_being_redefined = NULL;
      _load_kind = jvmti_class_load_kind_load;
    }
  }

  void post() {
    post_all_envs();
    copy_modified_data();
  }

  bool has_been_modified() { return _has_been_modified; }

 private:
  void post_all_envs() {
    if (_load_kind != jvmti_class_load_kind_retransform) {
      // for class load and redefine,
      // call the non-retransformable agents
      JvmtiEnvIterator it;
      for (JvmtiEnv* env = it.first(); env != NULL; env = it.next(env)) {
        if (!env->is_retransformable() && env->is_enabled(JVMTI_EVENT_CLASS_FILE_LOAD_HOOK)) {
          // non-retransformable agents cannot retransform back,
          // so no need to cache the original class file bytes
          post_to_env(env, false);
        }
      }
    }
    JvmtiEnvIterator it;
    for (JvmtiEnv* env = it.first(); env != NULL; env = it.next(env)) {
      // retransformable agents get all events
      if (env->is_retransformable() && env->is_enabled(JVMTI_EVENT_CLASS_FILE_LOAD_HOOK)) {
        // retransformable agents need to cache the original class file
        // bytes if changes are made via the ClassFileLoadHook
        post_to_env(env, true);
      }
    }
  }

  void post_to_env(JvmtiEnv* env, bool caching_needed) {
    if (env->phase() == JVMTI_PHASE_PRIMORDIAL && !env->early_class_hook_env()) {
      return;
    }
    unsigned char *new_data = NULL;
    jint new_len = 0;
    JvmtiClassFileLoadEventMark jem(_thread, _h_name, _class_loader,
                                    _h_protection_domain,
                                    _class_being_redefined);
    JvmtiJavaThreadEventTransition jet(_thread);
    jvmtiEventClassFileLoadHook callback = env->callbacks()->ClassFileLoadHook;
    if (callback != NULL) {
      (*callback)(env->jvmti_external(), jem.jni_env(),
                  jem.class_being_redefined(),
                  jem.jloader(), jem.class_name(),
                  jem.protection_domain(),
                  _curr_len, _curr_data,
                  &new_len, &new_data);
    }
    if (new_data != NULL) {
      // this agent has modified class data.
      _has_been_modified = true;
      if (caching_needed && *_cached_class_file_ptr == NULL) {
        // data has been changed by the new retransformable agent
        // and it hasn't already been cached, cache it
        JvmtiCachedClassFileData *p;
        p = (JvmtiCachedClassFileData *)os::malloc(
          offset_of(JvmtiCachedClassFileData, data) + _curr_len, mtInternal);
        if (p == NULL) {
          vm_exit_out_of_memory(offset_of(JvmtiCachedClassFileData, data) + _curr_len,
            OOM_MALLOC_ERROR,
            "unable to allocate cached copy of original class bytes");
        }
        p->length = _curr_len;
        memcpy(p->data, _curr_data, _curr_len);
        *_cached_class_file_ptr = p;
      }

      if (_curr_data != *_data_ptr) {
        // curr_data is previous agent modified class data.
        // And this has been changed by the new agent so
        // we can delete it now.
        _curr_env->Deallocate(_curr_data);
      }

      // Class file data has changed by the current agent.
      _curr_data = new_data;
      _curr_len = new_len;
      // Save the current agent env we need this to deallocate the
      // memory allocated by this agent.
      _curr_env = env;
    }
  }

  void copy_modified_data() {
    // if one of the agent has modified class file data.
    // Copy modified class data to new resources array.
    if (_curr_data != *_data_ptr) {
      *_data_ptr = NEW_RESOURCE_ARRAY(u1, _curr_len);
      memcpy(*_data_ptr, _curr_data, _curr_len);
      *_end_ptr = *_data_ptr + _curr_len;
      _curr_env->Deallocate(_curr_data);
    }
  }
};

bool JvmtiExport::is_early_phase() {
  return JvmtiEnvBase::get_phase() <= JVMTI_PHASE_PRIMORDIAL;
}

bool JvmtiExport::has_early_class_hook_env() {
  JvmtiEnvIterator it;
  for (JvmtiEnv* env = it.first(); env != NULL; env = it.next(env)) {
    if (env->early_class_hook_env()) {
      return true;
    }
  }
  return false;
}

bool JvmtiExport::_should_post_class_file_load_hook = false;

// this entry is for class file load hook on class load, redefine and retransform
bool JvmtiExport::post_class_file_load_hook(Symbol* h_name,
                                            Handle class_loader,
                                            Handle h_protection_domain,
                                            unsigned char **data_ptr,
                                            unsigned char **end_ptr,
                                            JvmtiCachedClassFileData **cache_ptr) {
  if (JvmtiEnv::get_phase() < JVMTI_PHASE_PRIMORDIAL) {
    return false;
  }

  JvmtiClassFileLoadHookPoster poster(h_name, class_loader,
                                      h_protection_domain,
                                      data_ptr, end_ptr,
                                      cache_ptr);
  poster.post();
  return poster.has_been_modified();
}

void JvmtiExport::report_unsupported(bool on) {
  // If any JVMTI service is turned on, we need to exit before native code
  // tries to access nonexistant services.
  if (on) {
    vm_exit_during_initialization("Java Kernel does not support JVMTI.");
  }
}


static inline Klass* oop_to_klass(oop obj) {
  Klass* k = obj->klass();

  // if the object is a java.lang.Class then return the java mirror
  if (k == vmClasses::Class_klass()) {
    if (!java_lang_Class::is_primitive(obj)) {
      k = java_lang_Class::as_Klass(obj);
      assert(k != NULL, "class for non-primitive mirror must exist");
    }
  }
  return k;
}

class JvmtiObjectAllocEventMark : public JvmtiClassEventMark  {
 private:
   jobject _jobj;
   jlong    _size;
 public:
   JvmtiObjectAllocEventMark(JavaThread *thread, oop obj) : JvmtiClassEventMark(thread, oop_to_klass(obj)) {
     _jobj = (jobject)to_jobject(obj);
     _size = obj->size() * wordSize;
   };
   jobject jni_jobject() { return _jobj; }
   jlong size() { return _size; }
};

class JvmtiCompiledMethodLoadEventMark : public JvmtiMethodEventMark {
 private:
  jint _code_size;
  const void *_code_data;
  jint _map_length;
  jvmtiAddrLocationMap *_map;
  const void *_compile_info;
 public:
  JvmtiCompiledMethodLoadEventMark(JavaThread *thread, nmethod *nm, void* compile_info_ptr = NULL)
          : JvmtiMethodEventMark(thread,methodHandle(thread, nm->method())) {
    _code_data = nm->code_begin();
    _code_size = nm->code_size();
    _compile_info = compile_info_ptr; // Set void pointer of compiledMethodLoad Event. Default value is NULL.
    JvmtiCodeBlobEvents::build_jvmti_addr_location_map(nm, &_map, &_map_length);
  }
  ~JvmtiCompiledMethodLoadEventMark() {
     FREE_C_HEAP_ARRAY(jvmtiAddrLocationMap, _map);
  }

  jint code_size() { return _code_size; }
  const void *code_data() { return _code_data; }
  jint map_length() { return _map_length; }
  const jvmtiAddrLocationMap* map() { return _map; }
  const void *compile_info() { return _compile_info; }
};



class JvmtiMonitorEventMark : public JvmtiThreadEventMark {
private:
  jobject _jobj;
public:
  JvmtiMonitorEventMark(JavaThread *thread, oop object)
          : JvmtiThreadEventMark(thread){
     _jobj = to_jobject(object);
  }
  jobject jni_object() { return _jobj; }
};

///////////////////////////////////////////////////////////////
//
// pending CompiledMethodUnload support
//

void JvmtiExport::post_compiled_method_unload(
       jmethodID method, const void *code_begin) {
  if (JvmtiEnv::get_phase() < JVMTI_PHASE_PRIMORDIAL) {
    return;
  }
  JavaThread* thread = JavaThread::current();
  EVT_TRIG_TRACE(JVMTI_EVENT_COMPILED_METHOD_UNLOAD,
                 ("[%s] method compile unload event triggered",
                  JvmtiTrace::safe_get_thread_name(thread)));

  // post the event for each environment that has this event enabled.
  JvmtiEnvIterator it;
  for (JvmtiEnv* env = it.first(); env != NULL; env = it.next(env)) {
    if (env->is_enabled(JVMTI_EVENT_COMPILED_METHOD_UNLOAD)) {
      if (env->phase() == JVMTI_PHASE_PRIMORDIAL) {
        continue;
      }
      EVT_TRACE(JVMTI_EVENT_COMPILED_METHOD_UNLOAD,
                ("[%s] class compile method unload event sent jmethodID " PTR_FORMAT,
                 JvmtiTrace::safe_get_thread_name(thread), p2i(method)));

      ResourceMark rm(thread);

      JvmtiEventMark jem(thread);
      JvmtiJavaThreadEventTransition jet(thread);
      jvmtiEventCompiledMethodUnload callback = env->callbacks()->CompiledMethodUnload;
      if (callback != NULL) {
        (*callback)(env->jvmti_external(), method, code_begin);
      }
    }
  }
}

///////////////////////////////////////////////////////////////
//
// JvmtiExport
//

void JvmtiExport::post_raw_breakpoint(JavaThread *thread, Method* method, address location) {
  HandleMark hm(thread);
  methodHandle mh(thread, method);

  JvmtiThreadState *state = thread->jvmti_thread_state();
  if (state == NULL) {
    return;
  }
  EVT_TRIG_TRACE(JVMTI_EVENT_BREAKPOINT, ("[%s] Trg Breakpoint triggered",
                      JvmtiTrace::safe_get_thread_name(thread)));
  JvmtiEnvThreadStateIterator it(state);
  for (JvmtiEnvThreadState* ets = it.first(); ets != NULL; ets = it.next(ets)) {
    ets->compare_and_set_current_location(mh(), location, JVMTI_EVENT_BREAKPOINT);
    if (!ets->breakpoint_posted() && ets->is_enabled(JVMTI_EVENT_BREAKPOINT)) {
      ThreadState old_os_state = thread->osthread()->get_state();
      thread->osthread()->set_state(BREAKPOINTED);
      EVT_TRACE(JVMTI_EVENT_BREAKPOINT, ("[%s] Evt Breakpoint sent %s.%s @ " INTX_FORMAT,
                     JvmtiTrace::safe_get_thread_name(thread),
                     (mh() == NULL) ? "NULL" : mh()->klass_name()->as_C_string(),
                     (mh() == NULL) ? "NULL" : mh()->name()->as_C_string(),
                     location - mh()->code_base() ));

      JvmtiEnv *env = ets->get_env();
      JvmtiLocationEventMark jem(thread, mh, location);
      JvmtiJavaThreadEventTransition jet(thread);
      jvmtiEventBreakpoint callback = env->callbacks()->Breakpoint;
      if (callback != NULL) {
        (*callback)(env->jvmti_external(), jem.jni_env(), jem.jni_thread(),
                    jem.jni_methodID(), jem.location());
      }

      ets->set_breakpoint_posted();
      thread->osthread()->set_state(old_os_state);
    }
  }
}

//////////////////////////////////////////////////////////////////////////////

bool              JvmtiExport::_can_get_source_debug_extension            = false;
bool              JvmtiExport::_can_maintain_original_method_order        = false;
bool              JvmtiExport::_can_post_interpreter_events               = false;
bool              JvmtiExport::_can_post_on_exceptions                    = false;
bool              JvmtiExport::_can_post_breakpoint                       = false;
bool              JvmtiExport::_can_post_field_access                     = false;
bool              JvmtiExport::_can_post_field_modification               = false;
bool              JvmtiExport::_can_post_method_entry                     = false;
bool              JvmtiExport::_can_post_method_exit                      = false;
bool              JvmtiExport::_can_pop_frame                             = false;
bool              JvmtiExport::_can_force_early_return                    = false;
bool              JvmtiExport::_can_get_owned_monitor_info                = false;

bool              JvmtiExport::_early_vmstart_recorded                    = false;

bool              JvmtiExport::_should_post_single_step                   = false;
bool              JvmtiExport::_should_post_field_access                  = false;
bool              JvmtiExport::_should_post_field_modification            = false;
bool              JvmtiExport::_should_post_class_load                    = false;
bool              JvmtiExport::_should_post_class_prepare                 = false;
bool              JvmtiExport::_should_post_class_unload                  = false;
bool              JvmtiExport::_should_post_thread_life                   = false;
bool              JvmtiExport::_should_clean_up_heap_objects              = false;
bool              JvmtiExport::_should_post_native_method_bind            = false;
bool              JvmtiExport::_should_post_dynamic_code_generated        = false;
bool              JvmtiExport::_should_post_data_dump                     = false;
bool              JvmtiExport::_should_post_compiled_method_load          = false;
bool              JvmtiExport::_should_post_compiled_method_unload        = false;
bool              JvmtiExport::_should_post_monitor_contended_enter       = false;
bool              JvmtiExport::_should_post_monitor_contended_entered     = false;
bool              JvmtiExport::_should_post_monitor_wait                  = false;
bool              JvmtiExport::_should_post_monitor_waited                = false;
bool              JvmtiExport::_should_post_garbage_collection_start      = false;
bool              JvmtiExport::_should_post_garbage_collection_finish     = false;
bool              JvmtiExport::_should_post_object_free                   = false;
bool              JvmtiExport::_should_post_resource_exhausted            = false;
bool              JvmtiExport::_should_post_vm_object_alloc               = false;
bool              JvmtiExport::_should_post_sampled_object_alloc          = false;
bool              JvmtiExport::_should_post_on_exceptions                 = false;

////////////////////////////////////////////////////////////////////////////////////////////////


//
// JVMTI single step management
//
void JvmtiExport::at_single_stepping_point(JavaThread *thread, Method* method, address location) {
  assert(JvmtiExport::should_post_single_step(), "must be single stepping");

  HandleMark hm(thread);
  methodHandle mh(thread, method);

  // update information about current location and post a step event
  JvmtiThreadState *state = thread->jvmti_thread_state();
  if (state == NULL) {
    return;
  }
  EVT_TRIG_TRACE(JVMTI_EVENT_SINGLE_STEP, ("[%s] Trg Single Step triggered",
                      JvmtiTrace::safe_get_thread_name(thread)));
  if (!state->hide_single_stepping()) {
    if (state->is_pending_step_for_popframe()) {
      state->process_pending_step_for_popframe();
    }
    if (state->is_pending_step_for_earlyret()) {
      state->process_pending_step_for_earlyret();
    }
    JvmtiExport::post_single_step(thread, mh(), location);
  }
}


void JvmtiExport::expose_single_stepping(JavaThread *thread) {
  JvmtiThreadState *state = thread->jvmti_thread_state();
  if (state != NULL) {
    state->clear_hide_single_stepping();
  }
}


bool JvmtiExport::hide_single_stepping(JavaThread *thread) {
  JvmtiThreadState *state = thread->jvmti_thread_state();
  if (state != NULL && state->is_enabled(JVMTI_EVENT_SINGLE_STEP)) {
    state->set_hide_single_stepping();
    return true;
  } else {
    return false;
  }
}

void JvmtiExport::post_class_load(JavaThread *thread, Klass* klass) {
  if (JvmtiEnv::get_phase() < JVMTI_PHASE_PRIMORDIAL) {
    return;
  }
  HandleMark hm(thread);

  EVT_TRIG_TRACE(JVMTI_EVENT_CLASS_LOAD, ("[%s] Trg Class Load triggered",
                      JvmtiTrace::safe_get_thread_name(thread)));
  JvmtiThreadState* state = thread->jvmti_thread_state();
  if (state == NULL) {
    return;
  }
  JvmtiEnvThreadStateIterator it(state);
  for (JvmtiEnvThreadState* ets = it.first(); ets != NULL; ets = it.next(ets)) {
    if (ets->is_enabled(JVMTI_EVENT_CLASS_LOAD)) {
      JvmtiEnv *env = ets->get_env();
      if (env->phase() == JVMTI_PHASE_PRIMORDIAL) {
        continue;
      }
      EVT_TRACE(JVMTI_EVENT_CLASS_LOAD, ("[%s] Evt Class Load sent %s",
                                         JvmtiTrace::safe_get_thread_name(thread),
                                         klass==NULL? "NULL" : klass->external_name() ));
      JvmtiClassEventMark jem(thread, klass);
      JvmtiJavaThreadEventTransition jet(thread);
      jvmtiEventClassLoad callback = env->callbacks()->ClassLoad;
      if (callback != NULL) {
        (*callback)(env->jvmti_external(), jem.jni_env(), jem.jni_thread(), jem.jni_class());
      }
    }
  }
}


void JvmtiExport::post_class_prepare(JavaThread *thread, Klass* klass) {
  if (JvmtiEnv::get_phase() < JVMTI_PHASE_PRIMORDIAL) {
    return;
  }
  HandleMark hm(thread);

  EVT_TRIG_TRACE(JVMTI_EVENT_CLASS_PREPARE, ("[%s] Trg Class Prepare triggered",
                      JvmtiTrace::safe_get_thread_name(thread)));
  JvmtiThreadState* state = thread->jvmti_thread_state();
  if (state == NULL) {
    return;
  }
  JvmtiEnvThreadStateIterator it(state);
  for (JvmtiEnvThreadState* ets = it.first(); ets != NULL; ets = it.next(ets)) {
    if (ets->is_enabled(JVMTI_EVENT_CLASS_PREPARE)) {
      JvmtiEnv *env = ets->get_env();
      if (env->phase() == JVMTI_PHASE_PRIMORDIAL) {
        continue;
      }
      EVT_TRACE(JVMTI_EVENT_CLASS_PREPARE, ("[%s] Evt Class Prepare sent %s",
                                            JvmtiTrace::safe_get_thread_name(thread),
                                            klass==NULL? "NULL" : klass->external_name() ));
      JvmtiClassEventMark jem(thread, klass);
      JvmtiJavaThreadEventTransition jet(thread);
      jvmtiEventClassPrepare callback = env->callbacks()->ClassPrepare;
      if (callback != NULL) {
        (*callback)(env->jvmti_external(), jem.jni_env(), jem.jni_thread(), jem.jni_class());
      }
    }
  }
}

void JvmtiExport::post_class_unload(Klass* klass) {
  if (JvmtiEnv::get_phase() < JVMTI_PHASE_PRIMORDIAL) {
    return;
  }

  // postings to the service thread so that it can perform them in a safe
  // context and in-order.
  ResourceMark rm;
  // JvmtiDeferredEvent copies the string.
  JvmtiDeferredEvent event = JvmtiDeferredEvent::class_unload_event(klass->name()->as_C_string());
  ServiceThread::enqueue_deferred_event(&event);
}


void JvmtiExport::post_class_unload_internal(const char* name) {
  if (JvmtiEnv::get_phase() < JVMTI_PHASE_PRIMORDIAL) {
    return;
  }
  assert(Thread::current()->is_service_thread(), "must be called from ServiceThread");
  JavaThread *thread = JavaThread::current();
  HandleMark hm(thread);

  EVT_TRIG_TRACE(EXT_EVENT_CLASS_UNLOAD, ("[?] Trg Class Unload triggered" ));
  if (JvmtiEventController::is_enabled((jvmtiEvent)EXT_EVENT_CLASS_UNLOAD)) {

    JvmtiEnvIterator it;
    for (JvmtiEnv* env = it.first(); env != NULL; env = it.next(env)) {
      if (env->phase() == JVMTI_PHASE_PRIMORDIAL) {
        continue;
      }
      if (env->is_enabled((jvmtiEvent)EXT_EVENT_CLASS_UNLOAD)) {
        EVT_TRACE(EXT_EVENT_CLASS_UNLOAD, ("[?] Evt Class Unload sent %s", name));

        JvmtiEventMark jem(thread);
        JvmtiJavaThreadEventTransition jet(thread);
        jvmtiExtensionEvent callback = env->ext_callbacks()->ClassUnload;
        if (callback != NULL) {
          (*callback)(env->jvmti_external(), jem.jni_env(), name);
        }
      }
    }
  }
}


void JvmtiExport::post_thread_start(JavaThread *thread) {
  if (JvmtiEnv::get_phase() < JVMTI_PHASE_PRIMORDIAL) {
    return;
  }
  assert(thread->thread_state() == _thread_in_vm, "must be in vm state");

  EVT_TRIG_TRACE(JVMTI_EVENT_THREAD_START, ("[%s] Trg Thread Start event triggered",
                      JvmtiTrace::safe_get_thread_name(thread)));

  // do JVMTI thread initialization (if needed)
  JvmtiEventController::thread_started(thread);

  // Do not post thread start event for hidden java thread.
  if (JvmtiEventController::is_enabled(JVMTI_EVENT_THREAD_START) &&
      !thread->is_hidden_from_external_view()) {
    JvmtiEnvIterator it;
    for (JvmtiEnv* env = it.first(); env != NULL; env = it.next(env)) {
      if (env->phase() == JVMTI_PHASE_PRIMORDIAL) {
        continue;
      }
      if (env->is_enabled(JVMTI_EVENT_THREAD_START)) {
        EVT_TRACE(JVMTI_EVENT_THREAD_START, ("[%s] Evt Thread Start event sent",
                     JvmtiTrace::safe_get_thread_name(thread) ));

        JvmtiThreadEventMark jem(thread);
        JvmtiJavaThreadEventTransition jet(thread);
        jvmtiEventThreadStart callback = env->callbacks()->ThreadStart;
        if (callback != NULL) {
          (*callback)(env->jvmti_external(), jem.jni_env(), jem.jni_thread());
        }
      }
    }
  }
}


void JvmtiExport::post_thread_end(JavaThread *thread) {
  if (JvmtiEnv::get_phase() < JVMTI_PHASE_PRIMORDIAL) {
    return;
  }
  EVT_TRIG_TRACE(JVMTI_EVENT_THREAD_END, ("[%s] Trg Thread End event triggered",
                      JvmtiTrace::safe_get_thread_name(thread)));

  JvmtiThreadState *state = thread->jvmti_thread_state();
  if (state == NULL) {
    return;
  }

  // Do not post thread end event for hidden java thread.
  if (state->is_enabled(JVMTI_EVENT_THREAD_END) &&
      !thread->is_hidden_from_external_view()) {

    JvmtiEnvThreadStateIterator it(state);
    for (JvmtiEnvThreadState* ets = it.first(); ets != NULL; ets = it.next(ets)) {
      if (ets->is_enabled(JVMTI_EVENT_THREAD_END)) {
        JvmtiEnv *env = ets->get_env();
        if (env->phase() == JVMTI_PHASE_PRIMORDIAL) {
          continue;
        }
        EVT_TRACE(JVMTI_EVENT_THREAD_END, ("[%s] Evt Thread End event sent",
                     JvmtiTrace::safe_get_thread_name(thread) ));

        JvmtiThreadEventMark jem(thread);
        JvmtiJavaThreadEventTransition jet(thread);
        jvmtiEventThreadEnd callback = env->callbacks()->ThreadEnd;
        if (callback != NULL) {
          (*callback)(env->jvmti_external(), jem.jni_env(), jem.jni_thread());
        }
      }
    }
  }
}

void JvmtiExport::post_object_free(JvmtiEnv* env, jlong tag) {
  assert(env->is_enabled(JVMTI_EVENT_OBJECT_FREE), "checking");

  EVT_TRIG_TRACE(JVMTI_EVENT_OBJECT_FREE, ("[?] Trg Object Free triggered" ));
  EVT_TRACE(JVMTI_EVENT_OBJECT_FREE, ("[?] Evt Object Free sent"));

  jvmtiEventObjectFree callback = env->callbacks()->ObjectFree;
  if (callback != NULL) {
    (*callback)(env->jvmti_external(), tag);
  }
}

void JvmtiExport::post_resource_exhausted(jint resource_exhausted_flags, const char* description) {

  JavaThread *thread  = JavaThread::current();

  log_error(jvmti)("Posting Resource Exhausted event: %s",
                   description != nullptr ? description : "unknown");

  // JDK-8213834: handlers of ResourceExhausted may attempt some analysis
  // which often requires running java.
  // This will cause problems on threads not able to run java, e.g. compiler
  // threads. To forestall these problems, we therefore suppress sending this
  // event from threads which are not able to run java.
  if (!thread->can_call_java()) {
    return;
  }

  EVT_TRIG_TRACE(JVMTI_EVENT_RESOURCE_EXHAUSTED, ("Trg resource exhausted event triggered" ));

  JvmtiEnvIterator it;
  for (JvmtiEnv* env = it.first(); env != NULL; env = it.next(env)) {
    if (env->is_enabled(JVMTI_EVENT_RESOURCE_EXHAUSTED)) {
      EVT_TRACE(JVMTI_EVENT_RESOURCE_EXHAUSTED, ("Evt resource exhausted event sent" ));

      JvmtiThreadEventMark jem(thread);
      JvmtiJavaThreadEventTransition jet(thread);
      jvmtiEventResourceExhausted callback = env->callbacks()->ResourceExhausted;
      if (callback != NULL) {
        (*callback)(env->jvmti_external(), jem.jni_env(),
                    resource_exhausted_flags, NULL, description);
      }
    }
  }
}

void JvmtiExport::post_method_entry(JavaThread *thread, Method* method, frame current_frame) {
  HandleMark hm(thread);
  methodHandle mh(thread, method);

  EVT_TRIG_TRACE(JVMTI_EVENT_METHOD_ENTRY, ("[%s] Trg Method Entry triggered %s.%s",
                     JvmtiTrace::safe_get_thread_name(thread),
                     (mh() == NULL) ? "NULL" : mh()->klass_name()->as_C_string(),
                     (mh() == NULL) ? "NULL" : mh()->name()->as_C_string() ));

  JvmtiThreadState* state = thread->jvmti_thread_state();
  if (state == NULL || !state->is_interp_only_mode()) {
    // for any thread that actually wants method entry, interp_only_mode is set
    return;
  }

  state->incr_cur_stack_depth();

  if (state->is_enabled(JVMTI_EVENT_METHOD_ENTRY)) {
    JvmtiEnvThreadStateIterator it(state);
    for (JvmtiEnvThreadState* ets = it.first(); ets != NULL; ets = it.next(ets)) {
      if (ets->is_enabled(JVMTI_EVENT_METHOD_ENTRY)) {
        EVT_TRACE(JVMTI_EVENT_METHOD_ENTRY, ("[%s] Evt Method Entry sent %s.%s",
                                             JvmtiTrace::safe_get_thread_name(thread),
                                             (mh() == NULL) ? "NULL" : mh()->klass_name()->as_C_string(),
                                             (mh() == NULL) ? "NULL" : mh()->name()->as_C_string() ));

        JvmtiEnv *env = ets->get_env();
        JvmtiMethodEventMark jem(thread, mh);
        JvmtiJavaThreadEventTransition jet(thread);
        jvmtiEventMethodEntry callback = env->callbacks()->MethodEntry;
        if (callback != NULL) {
          (*callback)(env->jvmti_external(), jem.jni_env(), jem.jni_thread(), jem.jni_methodID());
        }
      }
    }
  }
}

void JvmtiExport::post_method_exit(JavaThread* thread, Method* method, frame current_frame) {
  HandleMark hm(thread);
  methodHandle mh(thread, method);

  JvmtiThreadState *state = thread->jvmti_thread_state();

  if (state == NULL || !state->is_interp_only_mode()) {
    // for any thread that actually wants method exit, interp_only_mode is set
    return;
  }

  // return a flag when a method terminates by throwing an exception
  // i.e. if an exception is thrown and it's not caught by the current method
  bool exception_exit = state->is_exception_detected() && !state->is_exception_caught();
  Handle result;
  jvalue value;
  value.j = 0L;

  if (state->is_enabled(JVMTI_EVENT_METHOD_EXIT)) {
    // if the method hasn't been popped because of an exception then we populate
    // the return_value parameter for the callback. At this point we only have
    // the address of a "raw result" and we just call into the interpreter to
    // convert this into a jvalue.
    if (!exception_exit) {
      oop oop_result;
      BasicType type = current_frame.interpreter_frame_result(&oop_result, &value);
      if (is_reference_type(type)) {
        result = Handle(thread, oop_result);
        value.l = JNIHandles::make_local(thread, result());
      }
    }
  }

  // Deferred transition to VM, so we can stash away the return oop before GC
  // Note that this transition is not needed when throwing an exception, because
  // there is no oop to retain.
  JavaThread* current = thread; // for JRT_BLOCK
  JRT_BLOCK
    post_method_exit_inner(thread, mh, state, exception_exit, current_frame, value);
  JRT_BLOCK_END

  if (result.not_null() && !mh->is_native()) {
    // We have to restore the oop on the stack for interpreter frames
    *(oop*)current_frame.interpreter_frame_tos_address() = result();
  }
}

void JvmtiExport::post_method_exit_inner(JavaThread* thread,
                                         methodHandle& mh,
                                         JvmtiThreadState *state,
                                         bool exception_exit,
                                         frame current_frame,
                                         jvalue& value) {
  EVT_TRIG_TRACE(JVMTI_EVENT_METHOD_EXIT, ("[%s] Trg Method Exit triggered %s.%s",
                                           JvmtiTrace::safe_get_thread_name(thread),
                                           (mh() == NULL) ? "NULL" : mh()->klass_name()->as_C_string(),
                                           (mh() == NULL) ? "NULL" : mh()->name()->as_C_string() ));

  if (state->is_enabled(JVMTI_EVENT_METHOD_EXIT)) {
    JvmtiEnvThreadStateIterator it(state);
    for (JvmtiEnvThreadState* ets = it.first(); ets != NULL; ets = it.next(ets)) {
      if (ets->is_enabled(JVMTI_EVENT_METHOD_EXIT)) {
        EVT_TRACE(JVMTI_EVENT_METHOD_EXIT, ("[%s] Evt Method Exit sent %s.%s",
                                            JvmtiTrace::safe_get_thread_name(thread),
                                            (mh() == NULL) ? "NULL" : mh()->klass_name()->as_C_string(),
                                            (mh() == NULL) ? "NULL" : mh()->name()->as_C_string() ));

        JvmtiEnv *env = ets->get_env();
        JvmtiMethodEventMark jem(thread, mh);
        JvmtiJavaThreadEventTransition jet(thread);
        jvmtiEventMethodExit callback = env->callbacks()->MethodExit;
        if (callback != NULL) {
          (*callback)(env->jvmti_external(), jem.jni_env(), jem.jni_thread(),
                      jem.jni_methodID(), exception_exit,  value);
        }
      }
    }
  }

  JvmtiEnvThreadStateIterator it(state);
  for (JvmtiEnvThreadState* ets = it.first(); ets != NULL; ets = it.next(ets)) {
    if (ets->has_frame_pops()) {
      int cur_frame_number = state->cur_stack_depth();

      if (ets->is_frame_pop(cur_frame_number)) {
        // we have a NotifyFramePop entry for this frame.
        // now check that this env/thread wants this event
        if (ets->is_enabled(JVMTI_EVENT_FRAME_POP)) {
          EVT_TRACE(JVMTI_EVENT_FRAME_POP, ("[%s] Evt Frame Pop sent %s.%s",
                                            JvmtiTrace::safe_get_thread_name(thread),
                                            (mh() == NULL) ? "NULL" : mh()->klass_name()->as_C_string(),
                                            (mh() == NULL) ? "NULL" : mh()->name()->as_C_string() ));

          // we also need to issue a frame pop event for this frame
          JvmtiEnv *env = ets->get_env();
          JvmtiMethodEventMark jem(thread, mh);
          JvmtiJavaThreadEventTransition jet(thread);
          jvmtiEventFramePop callback = env->callbacks()->FramePop;
          if (callback != NULL) {
            (*callback)(env->jvmti_external(), jem.jni_env(), jem.jni_thread(),
                        jem.jni_methodID(), exception_exit);
          }
        }
        // remove the frame's entry
        {
          MutexLocker mu(JvmtiThreadState_lock);
          ets->clear_frame_pop(cur_frame_number);
        }
      }
    }
  }

  state->decr_cur_stack_depth();
}


// Todo: inline this for optimization
void JvmtiExport::post_single_step(JavaThread *thread, Method* method, address location) {
  HandleMark hm(thread);
  methodHandle mh(thread, method);

  JvmtiThreadState *state = thread->jvmti_thread_state();
  if (state == NULL) {
    return;
  }
  JvmtiEnvThreadStateIterator it(state);
  for (JvmtiEnvThreadState* ets = it.first(); ets != NULL; ets = it.next(ets)) {
    ets->compare_and_set_current_location(mh(), location, JVMTI_EVENT_SINGLE_STEP);
    if (!ets->single_stepping_posted() && ets->is_enabled(JVMTI_EVENT_SINGLE_STEP)) {
      EVT_TRACE(JVMTI_EVENT_SINGLE_STEP, ("[%s] Evt Single Step sent %s.%s @ " INTX_FORMAT,
                    JvmtiTrace::safe_get_thread_name(thread),
                    (mh() == NULL) ? "NULL" : mh()->klass_name()->as_C_string(),
                    (mh() == NULL) ? "NULL" : mh()->name()->as_C_string(),
                    location - mh()->code_base() ));

      JvmtiEnv *env = ets->get_env();
      JvmtiLocationEventMark jem(thread, mh, location);
      JvmtiJavaThreadEventTransition jet(thread);
      jvmtiEventSingleStep callback = env->callbacks()->SingleStep;
      if (callback != NULL) {
        (*callback)(env->jvmti_external(), jem.jni_env(), jem.jni_thread(),
                    jem.jni_methodID(), jem.location());
      }

      ets->set_single_stepping_posted();
    }
  }
}

void JvmtiExport::post_exception_throw(JavaThread *thread, Method* method, address location, oop exception) {
  HandleMark hm(thread);
  methodHandle mh(thread, method);
  Handle exception_handle(thread, exception);

  JvmtiThreadState *state = thread->jvmti_thread_state();
  if (state == NULL) {
    return;
  }

  EVT_TRIG_TRACE(JVMTI_EVENT_EXCEPTION, ("[%s] Trg Exception thrown triggered",
                      JvmtiTrace::safe_get_thread_name(thread)));
  if (!state->is_exception_detected()) {
    state->set_exception_detected();
    JvmtiEnvThreadStateIterator it(state);
    for (JvmtiEnvThreadState* ets = it.first(); ets != NULL; ets = it.next(ets)) {
      if (ets->is_enabled(JVMTI_EVENT_EXCEPTION) && (exception != NULL)) {

        EVT_TRACE(JVMTI_EVENT_EXCEPTION,
                     ("[%s] Evt Exception thrown sent %s.%s @ " INTX_FORMAT,
                      JvmtiTrace::safe_get_thread_name(thread),
                      (mh() == NULL) ? "NULL" : mh()->klass_name()->as_C_string(),
                      (mh() == NULL) ? "NULL" : mh()->name()->as_C_string(),
                      location - mh()->code_base() ));

        JvmtiEnv *env = ets->get_env();
        JvmtiExceptionEventMark jem(thread, mh, location, exception_handle);

        // It's okay to clear these exceptions here because we duplicate
        // this lookup in InterpreterRuntime::exception_handler_for_exception.
        EXCEPTION_MARK;

        bool should_repeat;
        vframeStream st(thread);
        assert(!st.at_end(), "cannot be at end");
        Method* current_method = NULL;
        // A GC may occur during the Method::fast_exception_handler_bci_for()
        // call below if it needs to load the constraint class. Using a
        // methodHandle to keep the 'current_method' from being deallocated
        // if GC happens.
        methodHandle current_mh = methodHandle(thread, current_method);
        int current_bci = -1;
        do {
          current_method = st.method();
          current_mh = methodHandle(thread, current_method);
          current_bci = st.bci();
          do {
            should_repeat = false;
            Klass* eh_klass = exception_handle()->klass();
            current_bci = Method::fast_exception_handler_bci_for(
              current_mh, eh_klass, current_bci, THREAD);
            if (HAS_PENDING_EXCEPTION) {
              exception_handle = Handle(thread, PENDING_EXCEPTION);
              CLEAR_PENDING_EXCEPTION;
              should_repeat = true;
            }
          } while (should_repeat && (current_bci != -1));
          st.next();
        } while ((current_bci < 0) && (!st.at_end()));

        jmethodID catch_jmethodID;
        if (current_bci < 0) {
          catch_jmethodID = 0;
          current_bci = 0;
        } else {
          catch_jmethodID = jem.to_jmethodID(current_mh);
        }

        JvmtiJavaThreadEventTransition jet(thread);
        jvmtiEventException callback = env->callbacks()->Exception;
        if (callback != NULL) {
          (*callback)(env->jvmti_external(), jem.jni_env(), jem.jni_thread(),
                      jem.jni_methodID(), jem.location(),
                      jem.exception(),
                      catch_jmethodID, current_bci);
        }
      }
    }
  }

  // frames may get popped because of this throw, be safe - invalidate cached depth
  state->invalidate_cur_stack_depth();
}


void JvmtiExport::notice_unwind_due_to_exception(JavaThread *thread, Method* method, address location, oop exception, bool in_handler_frame) {
  HandleMark hm(thread);
  methodHandle mh(thread, method);
  Handle exception_handle(thread, exception);

  JvmtiThreadState *state = thread->jvmti_thread_state();
  if (state == NULL) {
    return;
  }
  EVT_TRIG_TRACE(JVMTI_EVENT_EXCEPTION_CATCH,
                    ("[%s] Trg unwind_due_to_exception triggered %s.%s @ %s" INTX_FORMAT " - %s",
                     JvmtiTrace::safe_get_thread_name(thread),
                     (mh() == NULL) ? "NULL" : mh()->klass_name()->as_C_string(),
                     (mh() == NULL) ? "NULL" : mh()->name()->as_C_string(),
                     location==0? "no location:" : "",
                     location==0? 0 : location - mh()->code_base(),
                     in_handler_frame? "in handler frame" : "not handler frame" ));

  if (state->is_exception_detected()) {

    state->invalidate_cur_stack_depth();
    if (!in_handler_frame) {
      // Not in exception handler.
      if(state->is_interp_only_mode()) {
        // method exit and frame pop events are posted only in interp mode.
        // When these events are enabled code should be in running in interp mode.
        jvalue no_value;
        no_value.j = 0L;
        JvmtiExport::post_method_exit_inner(thread, mh, state, true, thread->last_frame(), no_value);
        // The cached cur_stack_depth might have changed from the
        // operations of frame pop or method exit. We are not 100% sure
        // the cached cur_stack_depth is still valid depth so invalidate
        // it.
        state->invalidate_cur_stack_depth();
      }
    } else {
      // In exception handler frame. Report exception catch.
      assert(location != NULL, "must be a known location");
      // Update cur_stack_depth - the frames above the current frame
      // have been unwound due to this exception:
      assert(!state->is_exception_caught(), "exception must not be caught yet.");
      state->set_exception_caught();

      JvmtiEnvThreadStateIterator it(state);
      for (JvmtiEnvThreadState* ets = it.first(); ets != NULL; ets = it.next(ets)) {
        if (ets->is_enabled(JVMTI_EVENT_EXCEPTION_CATCH) && (exception_handle() != NULL)) {
          EVT_TRACE(JVMTI_EVENT_EXCEPTION_CATCH,
                     ("[%s] Evt ExceptionCatch sent %s.%s @ " INTX_FORMAT,
                      JvmtiTrace::safe_get_thread_name(thread),
                      (mh() == NULL) ? "NULL" : mh()->klass_name()->as_C_string(),
                      (mh() == NULL) ? "NULL" : mh()->name()->as_C_string(),
                      location - mh()->code_base() ));

          JvmtiEnv *env = ets->get_env();
          JvmtiExceptionEventMark jem(thread, mh, location, exception_handle);
          JvmtiJavaThreadEventTransition jet(thread);
          jvmtiEventExceptionCatch callback = env->callbacks()->ExceptionCatch;
          if (callback != NULL) {
            (*callback)(env->jvmti_external(), jem.jni_env(), jem.jni_thread(),
                      jem.jni_methodID(), jem.location(),
                      jem.exception());
          }
        }
      }
    }
  }
}

oop JvmtiExport::jni_GetField_probe(JavaThread *thread, jobject jobj, oop obj,
                                    Klass* klass, jfieldID fieldID, bool is_static) {
  if (*((int *)get_field_access_count_addr()) > 0 && thread->has_last_Java_frame()) {
    // At least one field access watch is set so we have more work to do.
    post_field_access_by_jni(thread, obj, klass, fieldID, is_static);
    // event posting can block so refetch oop if we were passed a jobj
    if (jobj != NULL) return JNIHandles::resolve_non_null(jobj);
  }
  return obj;
}

void JvmtiExport::post_field_access_by_jni(JavaThread *thread, oop obj,
                                           Klass* klass, jfieldID fieldID, bool is_static) {
  // We must be called with a Java context in order to provide reasonable
  // values for the klazz, method, and location fields. The callers of this
  // function don't make the call unless there is a Java context.
  assert(thread->has_last_Java_frame(), "must be called with a Java context");

  ResourceMark rm;
  fieldDescriptor fd;
  // if get_field_descriptor finds fieldID to be invalid, then we just bail
  bool valid_fieldID = JvmtiEnv::get_field_descriptor(klass, fieldID, &fd);
  assert(valid_fieldID == true,"post_field_access_by_jni called with invalid fieldID");
  if (!valid_fieldID) return;
  // field accesses are not watched so bail
  if (!fd.is_field_access_watched()) return;

  HandleMark hm(thread);
  Handle h_obj;
  if (!is_static) {
    // non-static field accessors have an object, but we need a handle
    assert(obj != NULL, "non-static needs an object");
    h_obj = Handle(thread, obj);
  }
  post_field_access(thread,
                    thread->last_frame().interpreter_frame_method(),
                    thread->last_frame().interpreter_frame_bcp(),
                    klass, h_obj, fieldID);
}

void JvmtiExport::post_field_access(JavaThread *thread, Method* method,
  address location, Klass* field_klass, Handle object, jfieldID field) {

  HandleMark hm(thread);
  methodHandle mh(thread, method);

  JvmtiThreadState *state = thread->jvmti_thread_state();
  if (state == NULL) {
    return;
  }
  EVT_TRIG_TRACE(JVMTI_EVENT_FIELD_ACCESS, ("[%s] Trg Field Access event triggered",
                      JvmtiTrace::safe_get_thread_name(thread)));
  JvmtiEnvThreadStateIterator it(state);
  for (JvmtiEnvThreadState* ets = it.first(); ets != NULL; ets = it.next(ets)) {
    if (ets->is_enabled(JVMTI_EVENT_FIELD_ACCESS)) {
      EVT_TRACE(JVMTI_EVENT_FIELD_ACCESS, ("[%s] Evt Field Access event sent %s.%s @ " INTX_FORMAT,
                     JvmtiTrace::safe_get_thread_name(thread),
                     (mh() == NULL) ? "NULL" : mh()->klass_name()->as_C_string(),
                     (mh() == NULL) ? "NULL" : mh()->name()->as_C_string(),
                     location - mh()->code_base() ));

      JvmtiEnv *env = ets->get_env();
      JvmtiLocationEventMark jem(thread, mh, location);
      jclass field_jclass = jem.to_jclass(field_klass);
      jobject field_jobject = jem.to_jobject(object());
      JvmtiJavaThreadEventTransition jet(thread);
      jvmtiEventFieldAccess callback = env->callbacks()->FieldAccess;
      if (callback != NULL) {
        (*callback)(env->jvmti_external(), jem.jni_env(), jem.jni_thread(),
                    jem.jni_methodID(), jem.location(),
                    field_jclass, field_jobject, field);
      }
    }
  }
}

oop JvmtiExport::jni_SetField_probe(JavaThread *thread, jobject jobj, oop obj,
                                    Klass* klass, jfieldID fieldID, bool is_static,
                                    char sig_type, jvalue *value) {
  if (*((int *)get_field_modification_count_addr()) > 0 && thread->has_last_Java_frame()) {
    // At least one field modification watch is set so we have more work to do.
    post_field_modification_by_jni(thread, obj, klass, fieldID, is_static, sig_type, value);
    // event posting can block so refetch oop if we were passed a jobj
    if (jobj != NULL) return JNIHandles::resolve_non_null(jobj);
  }
  return obj;
}

void JvmtiExport::post_field_modification_by_jni(JavaThread *thread, oop obj,
                                                 Klass* klass, jfieldID fieldID, bool is_static,
                                                 char sig_type, jvalue *value) {
  // We must be called with a Java context in order to provide reasonable
  // values for the klazz, method, and location fields. The callers of this
  // function don't make the call unless there is a Java context.
  assert(thread->has_last_Java_frame(), "must be called with Java context");

  ResourceMark rm;
  fieldDescriptor fd;
  // if get_field_descriptor finds fieldID to be invalid, then we just bail
  bool valid_fieldID = JvmtiEnv::get_field_descriptor(klass, fieldID, &fd);
  assert(valid_fieldID == true,"post_field_modification_by_jni called with invalid fieldID");
  if (!valid_fieldID) return;
  // field modifications are not watched so bail
  if (!fd.is_field_modification_watched()) return;

  HandleMark hm(thread);

  Handle h_obj;
  if (!is_static) {
    // non-static field accessors have an object, but we need a handle
    assert(obj != NULL, "non-static needs an object");
    h_obj = Handle(thread, obj);
  }
  post_field_modification(thread,
                          thread->last_frame().interpreter_frame_method(),
                          thread->last_frame().interpreter_frame_bcp(),
                          klass, h_obj, fieldID, sig_type, value);
}

void JvmtiExport::post_raw_field_modification(JavaThread *thread, Method* method,
  address location, Klass* field_klass, Handle object, jfieldID field,
  char sig_type, jvalue *value) {

  if (sig_type == JVM_SIGNATURE_INT || sig_type == JVM_SIGNATURE_BOOLEAN ||
      sig_type == JVM_SIGNATURE_BYTE || sig_type == JVM_SIGNATURE_CHAR ||
      sig_type == JVM_SIGNATURE_SHORT) {
    // 'I' instructions are used for byte, char, short and int.
    // determine which it really is, and convert
    fieldDescriptor fd;
    bool found = JvmtiEnv::get_field_descriptor(field_klass, field, &fd);
    // should be found (if not, leave as is)
    if (found) {
      jint ival = value->i;
      // convert value from int to appropriate type
      switch (fd.field_type()) {
      case T_BOOLEAN:
        sig_type = JVM_SIGNATURE_BOOLEAN;
        value->i = 0; // clear it
        value->z = (jboolean)ival;
        break;
      case T_BYTE:
        sig_type = JVM_SIGNATURE_BYTE;
        value->i = 0; // clear it
        value->b = (jbyte)ival;
        break;
      case T_CHAR:
        sig_type = JVM_SIGNATURE_CHAR;
        value->i = 0; // clear it
        value->c = (jchar)ival;
        break;
      case T_SHORT:
        sig_type = JVM_SIGNATURE_SHORT;
        value->i = 0; // clear it
        value->s = (jshort)ival;
        break;
      case T_INT:
        // nothing to do
        break;
      default:
        // this is an integer instruction, should be one of above
        ShouldNotReachHere();
        break;
      }
    }
  }

  assert(sig_type != JVM_SIGNATURE_ARRAY, "array should have sig_type == 'L'");
  bool handle_created = false;

  // convert oop to JNI handle.
  if (sig_type == JVM_SIGNATURE_CLASS) {
    handle_created = true;
    value->l = (jobject)JNIHandles::make_local(thread, cast_to_oop(value->l));
  }

  post_field_modification(thread, method, location, field_klass, object, field, sig_type, value);

  // Destroy the JNI handle allocated above.
  if (handle_created) {
    JNIHandles::destroy_local(value->l);
  }
}

void JvmtiExport::post_field_modification(JavaThread *thread, Method* method,
  address location, Klass* field_klass, Handle object, jfieldID field,
  char sig_type, jvalue *value_ptr) {

  HandleMark hm(thread);
  methodHandle mh(thread, method);

  JvmtiThreadState *state = thread->jvmti_thread_state();
  if (state == NULL) {
    return;
  }
  EVT_TRIG_TRACE(JVMTI_EVENT_FIELD_MODIFICATION,
                     ("[%s] Trg Field Modification event triggered",
                      JvmtiTrace::safe_get_thread_name(thread)));

  JvmtiEnvThreadStateIterator it(state);
  for (JvmtiEnvThreadState* ets = it.first(); ets != NULL; ets = it.next(ets)) {
    if (ets->is_enabled(JVMTI_EVENT_FIELD_MODIFICATION)) {
      EVT_TRACE(JVMTI_EVENT_FIELD_MODIFICATION,
                   ("[%s] Evt Field Modification event sent %s.%s @ " INTX_FORMAT,
                    JvmtiTrace::safe_get_thread_name(thread),
                    (mh() == NULL) ? "NULL" : mh()->klass_name()->as_C_string(),
                    (mh() == NULL) ? "NULL" : mh()->name()->as_C_string(),
                    location - mh()->code_base() ));

      JvmtiEnv *env = ets->get_env();
      JvmtiLocationEventMark jem(thread, mh, location);
      jclass field_jclass = jem.to_jclass(field_klass);
      jobject field_jobject = jem.to_jobject(object());
      JvmtiJavaThreadEventTransition jet(thread);
      jvmtiEventFieldModification callback = env->callbacks()->FieldModification;
      if (callback != NULL) {
        (*callback)(env->jvmti_external(), jem.jni_env(), jem.jni_thread(),
                    jem.jni_methodID(), jem.location(),
                    field_jclass, field_jobject, field, sig_type, *value_ptr);
      }
    }
  }
}

void JvmtiExport::post_native_method_bind(Method* method, address* function_ptr) {
  JavaThread* thread = JavaThread::current();
  assert(thread->thread_state() == _thread_in_vm, "must be in vm state");

  HandleMark hm(thread);
  methodHandle mh(thread, method);

  EVT_TRIG_TRACE(JVMTI_EVENT_NATIVE_METHOD_BIND, ("[%s] Trg Native Method Bind event triggered",
                      JvmtiTrace::safe_get_thread_name(thread)));

  if (JvmtiEventController::is_enabled(JVMTI_EVENT_NATIVE_METHOD_BIND)) {
    JvmtiEnvIterator it;
    for (JvmtiEnv* env = it.first(); env != NULL; env = it.next(env)) {
      if (env->is_enabled(JVMTI_EVENT_NATIVE_METHOD_BIND)) {
        EVT_TRACE(JVMTI_EVENT_NATIVE_METHOD_BIND, ("[%s] Evt Native Method Bind event sent",
                     JvmtiTrace::safe_get_thread_name(thread) ));

        JvmtiMethodEventMark jem(thread, mh);
        JvmtiJavaThreadEventTransition jet(thread);
        JNIEnv* jni_env = (env->phase() == JVMTI_PHASE_PRIMORDIAL) ? NULL : jem.jni_env();
        jvmtiEventNativeMethodBind callback = env->callbacks()->NativeMethodBind;
        if (callback != NULL) {
          (*callback)(env->jvmti_external(), jni_env, jem.jni_thread(),
                      jem.jni_methodID(), (void*)(*function_ptr), (void**)function_ptr);
        }
      }
    }
  }
}

// Returns a record containing inlining information for the given nmethod
jvmtiCompiledMethodLoadInlineRecord* create_inline_record(nmethod* nm) {
  jint numstackframes = 0;
  jvmtiCompiledMethodLoadInlineRecord* record = (jvmtiCompiledMethodLoadInlineRecord*)NEW_RESOURCE_OBJ(jvmtiCompiledMethodLoadInlineRecord);
  record->header.kind = JVMTI_CMLR_INLINE_INFO;
  record->header.next = NULL;
  record->header.majorinfoversion = JVMTI_CMLR_MAJOR_VERSION_1;
  record->header.minorinfoversion = JVMTI_CMLR_MINOR_VERSION_0;
  record->numpcs = 0;
  for(PcDesc* p = nm->scopes_pcs_begin(); p < nm->scopes_pcs_end(); p++) {
   if(p->scope_decode_offset() == DebugInformationRecorder::serialized_null) continue;
   record->numpcs++;
  }
  record->pcinfo = (PCStackInfo*)(NEW_RESOURCE_ARRAY(PCStackInfo, record->numpcs));
  int scope = 0;
  for(PcDesc* p = nm->scopes_pcs_begin(); p < nm->scopes_pcs_end(); p++) {
    if(p->scope_decode_offset() == DebugInformationRecorder::serialized_null) continue;
    void* pc_address = (void*)p->real_pc(nm);
    assert(pc_address != NULL, "pc_address must be non-null");
    record->pcinfo[scope].pc = pc_address;
    numstackframes=0;
    for(ScopeDesc* sd = nm->scope_desc_at(p->real_pc(nm));sd != NULL;sd = sd->sender()) {
      numstackframes++;
    }
    assert(numstackframes != 0, "numstackframes must be nonzero.");
    record->pcinfo[scope].methods = (jmethodID *)NEW_RESOURCE_ARRAY(jmethodID, numstackframes);
    record->pcinfo[scope].bcis = (jint *)NEW_RESOURCE_ARRAY(jint, numstackframes);
    record->pcinfo[scope].numstackframes = numstackframes;
    int stackframe = 0;
    for(ScopeDesc* sd = nm->scope_desc_at(p->real_pc(nm));sd != NULL;sd = sd->sender()) {
      // sd->method() can be NULL for stubs but not for nmethods. To be completely robust, include an assert that we should never see a null sd->method()
      guarantee(sd->method() != NULL, "sd->method() cannot be null.");
      record->pcinfo[scope].methods[stackframe] = sd->method()->jmethod_id();
      record->pcinfo[scope].bcis[stackframe] = sd->bci();
      stackframe++;
    }
    scope++;
  }
  return record;
}

void JvmtiExport::post_compiled_method_load(nmethod *nm) {
  guarantee(!nm->is_unloading(), "nmethod isn't unloaded or unloading");
  if (JvmtiEnv::get_phase() < JVMTI_PHASE_PRIMORDIAL) {
    return;
  }
  JavaThread* thread = JavaThread::current();

  EVT_TRIG_TRACE(JVMTI_EVENT_COMPILED_METHOD_LOAD,
                 ("[%s] method compile load event triggered",
                 JvmtiTrace::safe_get_thread_name(thread)));

  JvmtiEnvIterator it;
  for (JvmtiEnv* env = it.first(); env != NULL; env = it.next(env)) {
    post_compiled_method_load(env, nm);
  }
}

// post a COMPILED_METHOD_LOAD event for a given environment
void JvmtiExport::post_compiled_method_load(JvmtiEnv* env, nmethod *nm) {
  if (env->phase() == JVMTI_PHASE_PRIMORDIAL || !env->is_enabled(JVMTI_EVENT_COMPILED_METHOD_LOAD)) {
    return;
  }
  jvmtiEventCompiledMethodLoad callback = env->callbacks()->CompiledMethodLoad;
  if (callback == NULL) {
    return;
  }
  JavaThread* thread = JavaThread::current();

  EVT_TRACE(JVMTI_EVENT_COMPILED_METHOD_LOAD,
           ("[%s] method compile load event sent %s.%s  ",
            JvmtiTrace::safe_get_thread_name(thread),
            (nm->method() == NULL) ? "NULL" : nm->method()->klass_name()->as_C_string(),
            (nm->method() == NULL) ? "NULL" : nm->method()->name()->as_C_string()));
  ResourceMark rm(thread);
  HandleMark hm(thread);

  assert(!nm->is_zombie(), "nmethod zombie in post_compiled_method_load");
  // Add inlining information
  jvmtiCompiledMethodLoadInlineRecord* inlinerecord = create_inline_record(nm);
  // Pass inlining information through the void pointer
  JvmtiCompiledMethodLoadEventMark jem(thread, nm, inlinerecord);
  JvmtiJavaThreadEventTransition jet(thread);
  (*callback)(env->jvmti_external(), jem.jni_methodID(),
              jem.code_size(), jem.code_data(), jem.map_length(),
              jem.map(), jem.compile_info());
}

void JvmtiExport::post_dynamic_code_generated_internal(const char *name, const void *code_begin, const void *code_end) {
  assert(name != NULL && name[0] != '\0', "sanity check");

  JavaThread* thread = JavaThread::current();
  // In theory everyone coming thru here is in_vm but we need to be certain
  // because a callee will do a vm->native transition
  ThreadInVMfromUnknown __tiv;

  EVT_TRIG_TRACE(JVMTI_EVENT_DYNAMIC_CODE_GENERATED,
                 ("[%s] method dynamic code generated event triggered",
                 JvmtiTrace::safe_get_thread_name(thread)));
  JvmtiEnvIterator it;
  for (JvmtiEnv* env = it.first(); env != NULL; env = it.next(env)) {
    if (env->is_enabled(JVMTI_EVENT_DYNAMIC_CODE_GENERATED)) {
      EVT_TRACE(JVMTI_EVENT_DYNAMIC_CODE_GENERATED,
                ("[%s] dynamic code generated event sent for %s",
                JvmtiTrace::safe_get_thread_name(thread), name));
      JvmtiEventMark jem(thread);
      JvmtiJavaThreadEventTransition jet(thread);
      jint length = (jint)pointer_delta(code_end, code_begin, sizeof(char));
      jvmtiEventDynamicCodeGenerated callback = env->callbacks()->DynamicCodeGenerated;
      if (callback != NULL) {
        (*callback)(env->jvmti_external(), name, (void*)code_begin, length);
      }
    }
  }
}

void JvmtiExport::post_dynamic_code_generated(const char *name, const void *code_begin, const void *code_end) {
  jvmtiPhase phase = JvmtiEnv::get_phase();
  if (phase == JVMTI_PHASE_PRIMORDIAL || phase == JVMTI_PHASE_START) {
    post_dynamic_code_generated_internal(name, code_begin, code_end);
  } else {
    // It may not be safe to post the event from this thread.  Defer all
    // postings to the service thread so that it can perform them in a safe
    // context and in-order.
    JvmtiDeferredEvent event = JvmtiDeferredEvent::dynamic_code_generated_event(
        name, code_begin, code_end);
    ServiceThread::enqueue_deferred_event(&event);
  }
}


// post a DYNAMIC_CODE_GENERATED event for a given environment
// used by GenerateEvents
void JvmtiExport::post_dynamic_code_generated(JvmtiEnv* env, const char *name,
                                              const void *code_begin, const void *code_end)
{
  JavaThread* thread = JavaThread::current();
  EVT_TRIG_TRACE(JVMTI_EVENT_DYNAMIC_CODE_GENERATED,
                 ("[%s] dynamic code generated event triggered (by GenerateEvents)",
                  JvmtiTrace::safe_get_thread_name(thread)));
  if (env->is_enabled(JVMTI_EVENT_DYNAMIC_CODE_GENERATED)) {
    EVT_TRACE(JVMTI_EVENT_DYNAMIC_CODE_GENERATED,
              ("[%s] dynamic code generated event sent for %s",
               JvmtiTrace::safe_get_thread_name(thread), name));
    JvmtiEventMark jem(thread);
    JvmtiJavaThreadEventTransition jet(thread);
    jint length = (jint)pointer_delta(code_end, code_begin, sizeof(char));
    jvmtiEventDynamicCodeGenerated callback = env->callbacks()->DynamicCodeGenerated;
    if (callback != NULL) {
      (*callback)(env->jvmti_external(), name, (void*)code_begin, length);
    }
  }
}

// post a DynamicCodeGenerated event while holding locks in the VM.
void JvmtiExport::post_dynamic_code_generated_while_holding_locks(const char* name,
                                                                  address code_begin, address code_end)
{
  // register the stub with the current dynamic code event collector
  // Cannot take safepoint here so do not use state_for to get
  // jvmti thread state.
  // The collector and/or state might be NULL if JvmtiDynamicCodeEventCollector
  // has been initialized while JVMTI_EVENT_DYNAMIC_CODE_GENERATED was disabled.
  JvmtiThreadState* state = JavaThread::current()->jvmti_thread_state();
  if (state != NULL) {
    JvmtiDynamicCodeEventCollector *collector = state->get_dynamic_code_event_collector();
    if (collector != NULL) {
      collector->register_stub(name, code_begin, code_end);
    }
  }
}

// Collect all the vm internally allocated objects which are visible to java world
void JvmtiExport::record_vm_internal_object_allocation(oop obj) {
  Thread* thread = Thread::current_or_null();
  if (thread != NULL && thread->is_Java_thread())  {
    // Can not take safepoint here.
    NoSafepointVerifier no_sfpt;
    // Cannot take safepoint here so do not use state_for to get
    // jvmti thread state.
    JvmtiThreadState *state = JavaThread::cast(thread)->jvmti_thread_state();
    if (state != NULL) {
      // state is non NULL when VMObjectAllocEventCollector is enabled.
      JvmtiVMObjectAllocEventCollector *collector;
      collector = state->get_vm_object_alloc_event_collector();
      if (collector != NULL && collector->is_enabled()) {
        // Don't record classes as these will be notified via the ClassLoad
        // event.
        if (obj->klass() != vmClasses::Class_klass()) {
          collector->record_allocation(obj);
        }
      }
    }
  }
}

// Collect all the sampled allocated objects.
void JvmtiExport::record_sampled_internal_object_allocation(oop obj) {
  Thread* thread = Thread::current_or_null();
  if (thread != NULL && thread->is_Java_thread())  {
    // Can not take safepoint here.
    NoSafepointVerifier no_sfpt;
    // Cannot take safepoint here so do not use state_for to get
    // jvmti thread state.
    JvmtiThreadState *state = JavaThread::cast(thread)->jvmti_thread_state();
    if (state != NULL) {
      // state is non NULL when SampledObjectAllocEventCollector is enabled.
      JvmtiSampledObjectAllocEventCollector *collector;
      collector = state->get_sampled_object_alloc_event_collector();

      if (collector != NULL && collector->is_enabled()) {
        collector->record_allocation(obj);
      }
    }
  }
}

void JvmtiExport::post_garbage_collection_finish() {
  Thread *thread = Thread::current(); // this event is posted from VM-Thread.
  EVT_TRIG_TRACE(JVMTI_EVENT_GARBAGE_COLLECTION_FINISH,
                 ("[%s] garbage collection finish event triggered",
                  JvmtiTrace::safe_get_thread_name(thread)));
  JvmtiEnvIterator it;
  for (JvmtiEnv* env = it.first(); env != NULL; env = it.next(env)) {
    if (env->is_enabled(JVMTI_EVENT_GARBAGE_COLLECTION_FINISH)) {
      EVT_TRACE(JVMTI_EVENT_GARBAGE_COLLECTION_FINISH,
                ("[%s] garbage collection finish event sent",
                 JvmtiTrace::safe_get_thread_name(thread)));
      JvmtiThreadEventTransition jet(thread);
      // JNIEnv is NULL here because this event is posted from VM Thread
      jvmtiEventGarbageCollectionFinish callback = env->callbacks()->GarbageCollectionFinish;
      if (callback != NULL) {
        (*callback)(env->jvmti_external());
      }
    }
  }
}

void JvmtiExport::post_garbage_collection_start() {
  Thread* thread = Thread::current(); // this event is posted from vm-thread.
  EVT_TRIG_TRACE(JVMTI_EVENT_GARBAGE_COLLECTION_START,
                 ("[%s] garbage collection start event triggered",
                  JvmtiTrace::safe_get_thread_name(thread)));
  JvmtiEnvIterator it;
  for (JvmtiEnv* env = it.first(); env != NULL; env = it.next(env)) {
    if (env->is_enabled(JVMTI_EVENT_GARBAGE_COLLECTION_START)) {
      EVT_TRACE(JVMTI_EVENT_GARBAGE_COLLECTION_START,
                ("[%s] garbage collection start event sent",
                 JvmtiTrace::safe_get_thread_name(thread)));
      JvmtiThreadEventTransition jet(thread);
      // JNIEnv is NULL here because this event is posted from VM Thread
      jvmtiEventGarbageCollectionStart callback = env->callbacks()->GarbageCollectionStart;
      if (callback != NULL) {
        (*callback)(env->jvmti_external());
      }
    }
  }
}

void JvmtiExport::post_data_dump() {
  Thread *thread = Thread::current();
  EVT_TRIG_TRACE(JVMTI_EVENT_DATA_DUMP_REQUEST,
                 ("[%s] data dump request event triggered",
                  JvmtiTrace::safe_get_thread_name(thread)));
  JvmtiEnvIterator it;
  for (JvmtiEnv* env = it.first(); env != NULL; env = it.next(env)) {
    if (env->is_enabled(JVMTI_EVENT_DATA_DUMP_REQUEST)) {
      EVT_TRACE(JVMTI_EVENT_DATA_DUMP_REQUEST,
                ("[%s] data dump request event sent",
                 JvmtiTrace::safe_get_thread_name(thread)));
     JvmtiThreadEventTransition jet(thread);
     // JNIEnv is NULL here because this event is posted from VM Thread
     jvmtiEventDataDumpRequest callback = env->callbacks()->DataDumpRequest;
     if (callback != NULL) {
       (*callback)(env->jvmti_external());
     }
    }
  }
}

void JvmtiExport::post_monitor_contended_enter(JavaThread *thread, ObjectMonitor *obj_mntr) {
  oop object = obj_mntr->object();
  JvmtiThreadState *state = thread->jvmti_thread_state();
  if (state == NULL) {
    return;
  }

  HandleMark hm(thread);
  Handle h(thread, object);

  EVT_TRIG_TRACE(JVMTI_EVENT_MONITOR_CONTENDED_ENTER,
                     ("[%s] monitor contended enter event triggered",
                      JvmtiTrace::safe_get_thread_name(thread)));

  JvmtiEnvThreadStateIterator it(state);
  for (JvmtiEnvThreadState* ets = it.first(); ets != NULL; ets = it.next(ets)) {
    if (ets->is_enabled(JVMTI_EVENT_MONITOR_CONTENDED_ENTER)) {
      EVT_TRACE(JVMTI_EVENT_MONITOR_CONTENDED_ENTER,
                   ("[%s] monitor contended enter event sent",
                    JvmtiTrace::safe_get_thread_name(thread)));
      JvmtiMonitorEventMark  jem(thread, h());
      JvmtiEnv *env = ets->get_env();
      JvmtiThreadEventTransition jet(thread);
      jvmtiEventMonitorContendedEnter callback = env->callbacks()->MonitorContendedEnter;
      if (callback != NULL) {
        (*callback)(env->jvmti_external(), jem.jni_env(), jem.jni_thread(), jem.jni_object());
      }
    }
  }
}

void JvmtiExport::post_monitor_contended_entered(JavaThread *thread, ObjectMonitor *obj_mntr) {
  oop object = obj_mntr->object();
  JvmtiThreadState *state = thread->jvmti_thread_state();
  if (state == NULL) {
    return;
  }

  HandleMark hm(thread);
  Handle h(thread, object);

  EVT_TRIG_TRACE(JVMTI_EVENT_MONITOR_CONTENDED_ENTERED,
                     ("[%s] monitor contended entered event triggered",
                      JvmtiTrace::safe_get_thread_name(thread)));

  JvmtiEnvThreadStateIterator it(state);
  for (JvmtiEnvThreadState* ets = it.first(); ets != NULL; ets = it.next(ets)) {
    if (ets->is_enabled(JVMTI_EVENT_MONITOR_CONTENDED_ENTERED)) {
      EVT_TRACE(JVMTI_EVENT_MONITOR_CONTENDED_ENTERED,
                   ("[%s] monitor contended enter event sent",
                    JvmtiTrace::safe_get_thread_name(thread)));
      JvmtiMonitorEventMark  jem(thread, h());
      JvmtiEnv *env = ets->get_env();
      JvmtiThreadEventTransition jet(thread);
      jvmtiEventMonitorContendedEntered callback = env->callbacks()->MonitorContendedEntered;
      if (callback != NULL) {
        (*callback)(env->jvmti_external(), jem.jni_env(), jem.jni_thread(), jem.jni_object());
      }
    }
  }
}

void JvmtiExport::post_monitor_wait(JavaThread *thread, oop object,
                                          jlong timeout) {
  JvmtiThreadState *state = thread->jvmti_thread_state();
  if (state == NULL) {
    return;
  }

  HandleMark hm(thread);
  Handle h(thread, object);

  EVT_TRIG_TRACE(JVMTI_EVENT_MONITOR_WAIT,
                     ("[%s] monitor wait event triggered",
                      JvmtiTrace::safe_get_thread_name(thread)));

  JvmtiEnvThreadStateIterator it(state);
  for (JvmtiEnvThreadState* ets = it.first(); ets != NULL; ets = it.next(ets)) {
    if (ets->is_enabled(JVMTI_EVENT_MONITOR_WAIT)) {
      EVT_TRACE(JVMTI_EVENT_MONITOR_WAIT,
                   ("[%s] monitor wait event sent",
                    JvmtiTrace::safe_get_thread_name(thread)));
      JvmtiMonitorEventMark  jem(thread, h());
      JvmtiEnv *env = ets->get_env();
      JvmtiThreadEventTransition jet(thread);
      jvmtiEventMonitorWait callback = env->callbacks()->MonitorWait;
      if (callback != NULL) {
        (*callback)(env->jvmti_external(), jem.jni_env(), jem.jni_thread(),
                    jem.jni_object(), timeout);
      }
    }
  }
}

void JvmtiExport::post_monitor_waited(JavaThread *thread, ObjectMonitor *obj_mntr, jboolean timed_out) {
  oop object = obj_mntr->object();
  JvmtiThreadState *state = thread->jvmti_thread_state();
  if (state == NULL) {
    return;
  }

  HandleMark hm(thread);
  Handle h(thread, object);

  EVT_TRIG_TRACE(JVMTI_EVENT_MONITOR_WAITED,
                     ("[%s] monitor waited event triggered",
                      JvmtiTrace::safe_get_thread_name(thread)));

  JvmtiEnvThreadStateIterator it(state);
  for (JvmtiEnvThreadState* ets = it.first(); ets != NULL; ets = it.next(ets)) {
    if (ets->is_enabled(JVMTI_EVENT_MONITOR_WAITED)) {
      EVT_TRACE(JVMTI_EVENT_MONITOR_WAITED,
                   ("[%s] monitor waited event sent",
                    JvmtiTrace::safe_get_thread_name(thread)));
      JvmtiMonitorEventMark  jem(thread, h());
      JvmtiEnv *env = ets->get_env();
      JvmtiThreadEventTransition jet(thread);
      jvmtiEventMonitorWaited callback = env->callbacks()->MonitorWaited;
      if (callback != NULL) {
        (*callback)(env->jvmti_external(), jem.jni_env(), jem.jni_thread(),
                    jem.jni_object(), timed_out);
      }
    }
  }
}

void JvmtiExport::post_vm_object_alloc(JavaThread *thread, oop object) {
  EVT_TRIG_TRACE(JVMTI_EVENT_VM_OBJECT_ALLOC, ("[%s] Trg vm object alloc triggered",
                      JvmtiTrace::safe_get_thread_name(thread)));
  if (object == NULL) {
    return;
  }
  HandleMark hm(thread);
  Handle h(thread, object);
  JvmtiEnvIterator it;
  for (JvmtiEnv* env = it.first(); env != NULL; env = it.next(env)) {
    if (env->is_enabled(JVMTI_EVENT_VM_OBJECT_ALLOC)) {
      EVT_TRACE(JVMTI_EVENT_VM_OBJECT_ALLOC, ("[%s] Evt vmobject alloc sent %s",
                                         JvmtiTrace::safe_get_thread_name(thread),
                                         object==NULL? "NULL" : object->klass()->external_name()));

      JvmtiObjectAllocEventMark jem(thread, h());
      JvmtiJavaThreadEventTransition jet(thread);
      jvmtiEventVMObjectAlloc callback = env->callbacks()->VMObjectAlloc;
      if (callback != NULL) {
        (*callback)(env->jvmti_external(), jem.jni_env(), jem.jni_thread(),
                    jem.jni_jobject(), jem.jni_class(), jem.size());
      }
    }
  }
}

void JvmtiExport::post_sampled_object_alloc(JavaThread *thread, oop object) {
  JvmtiThreadState *state = thread->jvmti_thread_state();
  if (state == NULL) {
    return;
  }

  EVT_TRIG_TRACE(JVMTI_EVENT_SAMPLED_OBJECT_ALLOC,
                 ("[%s] Trg sampled object alloc triggered",
                  JvmtiTrace::safe_get_thread_name(thread)));
  if (object == NULL) {
    return;
  }
  HandleMark hm(thread);
  Handle h(thread, object);

  JvmtiEnvThreadStateIterator it(state);
  for (JvmtiEnvThreadState* ets = it.first(); ets != NULL; ets = it.next(ets)) {
    if (ets->is_enabled(JVMTI_EVENT_SAMPLED_OBJECT_ALLOC)) {
      EVT_TRACE(JVMTI_EVENT_SAMPLED_OBJECT_ALLOC,
                ("[%s] Evt sampled object alloc sent %s",
                 JvmtiTrace::safe_get_thread_name(thread),
                 object == NULL ? "NULL" : object->klass()->external_name()));

      JvmtiEnv *env = ets->get_env();
      JvmtiObjectAllocEventMark jem(thread, h());
      JvmtiJavaThreadEventTransition jet(thread);
      jvmtiEventSampledObjectAlloc callback = env->callbacks()->SampledObjectAlloc;
      if (callback != NULL) {
        (*callback)(env->jvmti_external(), jem.jni_env(), jem.jni_thread(),
                    jem.jni_jobject(), jem.jni_class(), jem.size());
      }
    }
  }
}

////////////////////////////////////////////////////////////////////////////////////////////////

void JvmtiExport::cleanup_thread(JavaThread* thread) {
  assert(JavaThread::current() == thread, "thread is not current");
  MutexLocker mu(thread, JvmtiThreadState_lock);

  if (thread->jvmti_thread_state() != NULL) {
    // This has to happen after the thread state is removed, which is
    // why it is not in post_thread_end_event like its complement
    // Maybe both these functions should be rolled into the posts?
    JvmtiEventController::thread_ended(thread);
  }
}

void JvmtiExport::clear_detected_exception(JavaThread* thread) {
  assert(JavaThread::current() == thread, "thread is not current");

  JvmtiThreadState* state = thread->jvmti_thread_state();
  if (state != NULL) {
    state->clear_exception_state();
  }
}

// Onload raw monitor transition.
void JvmtiExport::transition_pending_onload_raw_monitors() {
  JvmtiPendingMonitors::transition_raw_monitors();
}

////////////////////////////////////////////////////////////////////////////////////////////////
#if INCLUDE_SERVICES
// Attach is disabled if SERVICES is not included

// type for the Agent_OnAttach entry point
extern "C" {
  typedef jint (JNICALL *OnAttachEntry_t)(JavaVM*, char *, void *);
}

jint JvmtiExport::load_agent_library(const char *agent, const char *absParam,
                                     const char *options, outputStream* st) {
  char ebuf[1024] = {0};
  char buffer[JVM_MAXPATHLEN];
  void* library = NULL;
  jint result = JNI_ERR;
  const char *on_attach_symbols[] = AGENT_ONATTACH_SYMBOLS;
  size_t num_symbol_entries = ARRAY_SIZE(on_attach_symbols);

  // The abs paramter should be "true" or "false"
  bool is_absolute_path = (absParam != NULL) && (strcmp(absParam,"true")==0);

  // Initially marked as invalid. It will be set to valid if we can find the agent
  AgentLibrary *agent_lib = new AgentLibrary(agent, options, is_absolute_path, NULL);

  // Check for statically linked in agent. If not found then if the path is
  // absolute we attempt to load the library. Otherwise we try to load it
  // from the standard dll directory.

  if (!os::find_builtin_agent(agent_lib, on_attach_symbols, num_symbol_entries)) {
    if (is_absolute_path) {
      library = os::dll_load(agent, ebuf, sizeof ebuf);
    } else {
      // Try to load the agent from the standard dll directory
      if (os::dll_locate_lib(buffer, sizeof(buffer), Arguments::get_dll_dir(),
                             agent)) {
        library = os::dll_load(buffer, ebuf, sizeof ebuf);
      }
      if (library == NULL) {
        // not found - try OS default library path
        if (os::dll_build_name(buffer, sizeof(buffer), agent)) {
          library = os::dll_load(buffer, ebuf, sizeof ebuf);
        }
      }
    }
    if (library != NULL) {
      agent_lib->set_os_lib(library);
      agent_lib->set_valid();
    }
  }
  // If the library was loaded then we attempt to invoke the Agent_OnAttach
  // function
  if (agent_lib->valid()) {
    // Lookup the Agent_OnAttach function
    OnAttachEntry_t on_attach_entry = NULL;
    on_attach_entry = CAST_TO_FN_PTR(OnAttachEntry_t,
       os::find_agent_function(agent_lib, false, on_attach_symbols, num_symbol_entries));
    if (on_attach_entry == NULL) {
      // Agent_OnAttach missing - unload library
      if (!agent_lib->is_static_lib()) {
        os::dll_unload(library);
      }
      st->print_cr("%s is not available in %s",
                   on_attach_symbols[0], agent_lib->name());
      delete agent_lib;
    } else {
      // Invoke the Agent_OnAttach function
      JavaThread* THREAD = JavaThread::current(); // For exception macros.
      {
        extern struct JavaVM_ main_vm;
        JvmtiThreadEventMark jem(THREAD);
        JvmtiJavaThreadEventTransition jet(THREAD);

        result = (*on_attach_entry)(&main_vm, (char*)options, NULL);
      }

      // Agent_OnAttach may have used JNI
      if (HAS_PENDING_EXCEPTION) {
        CLEAR_PENDING_EXCEPTION;
      }

      // If OnAttach returns JNI_OK then we add it to the list of
      // agent libraries so that we can call Agent_OnUnload later.
      if (result == JNI_OK) {
        Arguments::add_loaded_agent(agent_lib);
      } else {
        if (!agent_lib->is_static_lib()) {
          os::dll_unload(library);
        }
        delete agent_lib;
      }

      // Agent_OnAttach executed so completion status is JNI_OK
      st->print_cr("return code: %d", result);
      result = JNI_OK;
    }
  } else {
    st->print_cr("%s was not loaded.", agent);
    if (*ebuf != '\0') {
      st->print_cr("%s", ebuf);
    }
  }
  return result;
}

#endif // INCLUDE_SERVICES
////////////////////////////////////////////////////////////////////////////////////////////////

// Setup current current thread for event collection.
void JvmtiEventCollector::setup_jvmti_thread_state() {
  // set this event collector to be the current one.
  JvmtiThreadState* state = JvmtiThreadState::state_for(JavaThread::current());
  // state can only be NULL if the current thread is exiting which
  // should not happen since we're trying to configure for event collection
  guarantee(state != NULL, "exiting thread called setup_jvmti_thread_state");
  if (is_vm_object_alloc_event()) {
    JvmtiVMObjectAllocEventCollector *prev = state->get_vm_object_alloc_event_collector();

    // If we have a previous collector and it is disabled, it means this allocation came from a
    // callback induced VM Object allocation, do not register this collector then.
    if (prev && !prev->is_enabled()) {
      return;
    }
    _prev = prev;
    state->set_vm_object_alloc_event_collector((JvmtiVMObjectAllocEventCollector *)this);
  } else if (is_dynamic_code_event()) {
    _prev = state->get_dynamic_code_event_collector();
    state->set_dynamic_code_event_collector((JvmtiDynamicCodeEventCollector *)this);
  } else if (is_sampled_object_alloc_event()) {
    JvmtiSampledObjectAllocEventCollector *prev = state->get_sampled_object_alloc_event_collector();

    if (prev) {
      // JvmtiSampledObjectAllocEventCollector wants only one active collector
      // enabled. This allows to have a collector detect a user code requiring
      // a sample in the callback.
      return;
    }
    state->set_sampled_object_alloc_event_collector((JvmtiSampledObjectAllocEventCollector*) this);
  }

  _unset_jvmti_thread_state = true;
}

// Unset current event collection in this thread and reset it with previous
// collector.
void JvmtiEventCollector::unset_jvmti_thread_state() {
  if (!_unset_jvmti_thread_state) {
    return;
  }

  JvmtiThreadState* state = JavaThread::current()->jvmti_thread_state();
  if (state != NULL) {
    // restore the previous event collector (if any)
    if (is_vm_object_alloc_event()) {
      if (state->get_vm_object_alloc_event_collector() == this) {
        state->set_vm_object_alloc_event_collector((JvmtiVMObjectAllocEventCollector *)_prev);
      } else {
        // this thread's jvmti state was created during the scope of
        // the event collector.
      }
    } else if (is_dynamic_code_event()) {
      if (state->get_dynamic_code_event_collector() == this) {
        state->set_dynamic_code_event_collector((JvmtiDynamicCodeEventCollector *)_prev);
      } else {
        // this thread's jvmti state was created during the scope of
        // the event collector.
      }
    } else if (is_sampled_object_alloc_event()) {
      if (state->get_sampled_object_alloc_event_collector() == this) {
        state->set_sampled_object_alloc_event_collector((JvmtiSampledObjectAllocEventCollector*)_prev);
      } else {
        // this thread's jvmti state was created during the scope of
        // the event collector.
      }
    }
  }
}

// create the dynamic code event collector
JvmtiDynamicCodeEventCollector::JvmtiDynamicCodeEventCollector() : _code_blobs(NULL) {
  if (JvmtiExport::should_post_dynamic_code_generated()) {
    setup_jvmti_thread_state();
  }
}

// iterate over any code blob descriptors collected and post a
// DYNAMIC_CODE_GENERATED event to the profiler.
JvmtiDynamicCodeEventCollector::~JvmtiDynamicCodeEventCollector() {
  assert(!JavaThread::current()->owns_locks(), "all locks must be released to post deferred events");
 // iterate over any code blob descriptors that we collected
 if (_code_blobs != NULL) {
   for (int i=0; i<_code_blobs->length(); i++) {
     JvmtiCodeBlobDesc* blob = _code_blobs->at(i);
     JvmtiExport::post_dynamic_code_generated(blob->name(), blob->code_begin(), blob->code_end());
     FreeHeap(blob);
   }
   delete _code_blobs;
 }
 unset_jvmti_thread_state();
}

// register a stub
void JvmtiDynamicCodeEventCollector::register_stub(const char* name, address start, address end) {
 if (_code_blobs == NULL) {
   _code_blobs = new (ResourceObj::C_HEAP, mtServiceability) GrowableArray<JvmtiCodeBlobDesc*>(1, mtServiceability);
 }
 _code_blobs->append(new JvmtiCodeBlobDesc(name, start, end));
}

// Setup current thread to record vm allocated objects.
JvmtiObjectAllocEventCollector::JvmtiObjectAllocEventCollector() :
    _allocated(NULL), _enable(false), _post_callback(NULL) {
}

// Post vm_object_alloc event for vm allocated objects visible to java
// world.
void JvmtiObjectAllocEventCollector::generate_call_for_allocated() {
  if (_allocated) {
    set_enabled(false);
    for (int i = 0; i < _allocated->length(); i++) {
      oop obj = _allocated->at(i).resolve();
      _post_callback(JavaThread::current(), obj);
      // Release OopHandle
      _allocated->at(i).release(JvmtiExport::jvmti_oop_storage());

    }
    delete _allocated, _allocated = NULL;
  }
}

void JvmtiObjectAllocEventCollector::record_allocation(oop obj) {
  assert(is_enabled(), "Object alloc event collector is not enabled");
  if (_allocated == NULL) {
    _allocated = new (ResourceObj::C_HEAP, mtServiceability) GrowableArray<OopHandle>(1, mtServiceability);
  }
  _allocated->push(OopHandle(JvmtiExport::jvmti_oop_storage(), obj));
}

// Disable collection of VMObjectAlloc events
NoJvmtiVMObjectAllocMark::NoJvmtiVMObjectAllocMark() : _collector(NULL) {
  // a no-op if VMObjectAlloc event is not enabled
  if (!JvmtiExport::should_post_vm_object_alloc()) {
    return;
  }
  Thread* thread = Thread::current_or_null();
  if (thread != NULL && thread->is_Java_thread())  {
    JavaThread* current_thread = JavaThread::cast(thread);
    JvmtiThreadState *state = current_thread->jvmti_thread_state();
    if (state != NULL) {
      JvmtiVMObjectAllocEventCollector *collector;
      collector = state->get_vm_object_alloc_event_collector();
      if (collector != NULL && collector->is_enabled()) {
        _collector = collector;
        _collector->set_enabled(false);
      }
    }
  }
}

// Re-Enable collection of VMObjectAlloc events (if previously enabled)
NoJvmtiVMObjectAllocMark::~NoJvmtiVMObjectAllocMark() {
  if (was_enabled()) {
    _collector->set_enabled(true);
  }
};

// Setup current thread to record vm allocated objects.
JvmtiVMObjectAllocEventCollector::JvmtiVMObjectAllocEventCollector() {
  if (JvmtiExport::should_post_vm_object_alloc()) {
    _enable = true;
    setup_jvmti_thread_state();
    _post_callback = JvmtiExport::post_vm_object_alloc;
  }
}

JvmtiVMObjectAllocEventCollector::~JvmtiVMObjectAllocEventCollector() {
  if (_enable) {
    generate_call_for_allocated();
  }
  unset_jvmti_thread_state();
}

bool JvmtiSampledObjectAllocEventCollector::object_alloc_is_safe_to_sample() {
  Thread* thread = Thread::current();
  // Really only sample allocations if this is a JavaThread and not the compiler
  // thread.
  if (!thread->is_Java_thread() || thread->is_Compiler_thread()) {
    return false;
  }

  if (MultiArray_lock->owner() == thread) {
    return false;
  }
  return true;
}

// Setup current thread to record sampled allocated objects.
JvmtiSampledObjectAllocEventCollector::JvmtiSampledObjectAllocEventCollector() {
  if (JvmtiExport::should_post_sampled_object_alloc()) {
    if (!object_alloc_is_safe_to_sample()) {
      return;
    }

    _enable = true;
    setup_jvmti_thread_state();
    _post_callback = JvmtiExport::post_sampled_object_alloc;
  }
}

JvmtiSampledObjectAllocEventCollector::~JvmtiSampledObjectAllocEventCollector() {
  if (!_enable) {
    return;
  }

  generate_call_for_allocated();
  unset_jvmti_thread_state();

  // Unset the sampling collector as present in assertion mode only.
  assert(Thread::current()->is_Java_thread(),
         "Should always be in a Java thread");
}

JvmtiGCMarker::JvmtiGCMarker() {
  // if there aren't any JVMTI environments then nothing to do
  if (!JvmtiEnv::environments_might_exist()) {
    return;
  }

  if (JvmtiExport::should_post_garbage_collection_start()) {
    JvmtiExport::post_garbage_collection_start();
  }

  if (SafepointSynchronize::is_at_safepoint()) {
    // Do clean up tasks that need to be done at a safepoint
    JvmtiEnvBase::check_for_periodic_clean_up();
  }
}

JvmtiGCMarker::~JvmtiGCMarker() {
  // if there aren't any JVMTI environments then nothing to do
  if (!JvmtiEnv::environments_might_exist()) {
    return;
  }

  // JVMTI notify gc finish
  if (JvmtiExport::should_post_garbage_collection_finish()) {
    JvmtiExport::post_garbage_collection_finish();
  }
}
