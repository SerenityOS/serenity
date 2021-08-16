/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, Azul Systems, Inc. All rights reserved.
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
#include "classfile/vmSymbols.hpp"
#include "code/nmethod.hpp"
#include "compiler/compilationPolicy.hpp"
#include "compiler/compileBroker.hpp"
#include "interpreter/interpreter.hpp"
#include "interpreter/linkResolver.hpp"
#if INCLUDE_JVMCI
#include "jvmci/jvmciJavaClasses.hpp"
#endif
#include "memory/universe.hpp"
#include "oops/method.inline.hpp"
#include "oops/oop.inline.hpp"
#include "prims/jniCheck.hpp"
#include "prims/jvmtiExport.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/javaCalls.hpp"
#include "runtime/jniHandles.inline.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/os.inline.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/signature.hpp"
#include "runtime/stubRoutines.hpp"
#include "runtime/thread.inline.hpp"

// -----------------------------------------------------
// Implementation of JavaCallWrapper

JavaCallWrapper::JavaCallWrapper(const methodHandle& callee_method, Handle receiver, JavaValue* result, TRAPS) {
  JavaThread* thread = THREAD;
  bool clear_pending_exception = true;

  guarantee(thread->is_Java_thread(), "crucial check - the VM thread cannot and must not escape to Java code");
  assert(!thread->owns_locks(), "must release all locks when leaving VM");
  guarantee(thread->can_call_java(), "cannot make java calls from the native compiler");
  _result   = result;

  // Allocate handle block for Java code. This must be done before we change thread_state to _thread_in_Java_or_stub,
  // since it can potentially block.
  JNIHandleBlock* new_handles = JNIHandleBlock::allocate_block(thread);

  // After this, we are official in JavaCode. This needs to be done before we change any of the thread local
  // info, since we cannot find oops before the new information is set up completely.
  ThreadStateTransition::transition(thread, _thread_in_vm, _thread_in_Java);

  // Make sure that we handle asynchronous stops and suspends _before_ we clear all thread state
  // in JavaCallWrapper::JavaCallWrapper(). This way, we can decide if we need to do any pd actions
  // to prepare for stop/suspend (flush register windows on sparcs, cache sp, or other state).
  if (thread->has_special_runtime_exit_condition()) {
    thread->handle_special_runtime_exit_condition();
    if (HAS_PENDING_EXCEPTION) {
      clear_pending_exception = false;
    }
  }

  // Make sure to set the oop's after the thread transition - since we can block there. No one is GC'ing
  // the JavaCallWrapper before the entry frame is on the stack.
  _callee_method = callee_method();
  _receiver = receiver();

#ifdef CHECK_UNHANDLED_OOPS
  THREAD->allow_unhandled_oop(&_receiver);
#endif // CHECK_UNHANDLED_OOPS

  _thread       = thread;
  _handles      = _thread->active_handles();    // save previous handle block & Java frame linkage

  // For the profiler, the last_Java_frame information in thread must always be in
  // legal state. We have no last Java frame if last_Java_sp == NULL so
  // the valid transition is to clear _last_Java_sp and then reset the rest of
  // the (platform specific) state.

  _anchor.copy(_thread->frame_anchor());
  _thread->frame_anchor()->clear();

  debug_only(_thread->inc_java_call_counter());
  _thread->set_active_handles(new_handles);     // install new handle block and reset Java frame linkage

  assert (_thread->thread_state() != _thread_in_native, "cannot set native pc to NULL");

  // clear any pending exception in thread (native calls start with no exception pending)
  if(clear_pending_exception) {
    _thread->clear_pending_exception();
  }

  MACOS_AARCH64_ONLY(_thread->enable_wx(WXExec));
}


JavaCallWrapper::~JavaCallWrapper() {
  assert(_thread == JavaThread::current(), "must still be the same thread");

  MACOS_AARCH64_ONLY(_thread->enable_wx(WXWrite));

  // restore previous handle block & Java frame linkage
  JNIHandleBlock *_old_handles = _thread->active_handles();
  _thread->set_active_handles(_handles);

  _thread->frame_anchor()->zap();

  debug_only(_thread->dec_java_call_counter());

  // Old thread-local info. has been restored. We are not back in the VM.
  ThreadStateTransition::transition_from_java(_thread, _thread_in_vm);

  // State has been restored now make the anchor frame visible for the profiler.
  // Do this after the transition because this allows us to put an assert
  // the Java->vm transition which checks to see that stack is not walkable
  // on sparc/ia64 which will catch violations of the reseting of last_Java_frame
  // invariants (i.e. _flags always cleared on return to Java)

  _thread->frame_anchor()->copy(&_anchor);

  // Release handles after we are marked as being inside the VM again, since this
  // operation might block
  JNIHandleBlock::release_block(_old_handles, _thread);

  if (_thread->has_pending_exception() && _thread->has_last_Java_frame()) {
    // If we get here, the Java code threw an exception that unwound a frame.
    // It could be that the new frame anchor has not passed through the required
    // StackWatermark barriers. Therefore, we process any such deferred unwind
    // requests here.
    StackWatermarkSet::after_unwind(_thread);
  }
}


void JavaCallWrapper::oops_do(OopClosure* f) {
  f->do_oop((oop*)&_receiver);
  handles()->oops_do(f);
}


// Helper methods
static BasicType runtime_type_from(JavaValue* result) {
  switch (result->get_type()) {
    case T_BOOLEAN: // fall through
    case T_CHAR   : // fall through
    case T_SHORT  : // fall through
    case T_INT    : // fall through
#ifndef _LP64
    case T_OBJECT : // fall through
    case T_ARRAY  : // fall through
#endif
    case T_BYTE   : // fall through
    case T_VOID   : return T_INT;
    case T_LONG   : return T_LONG;
    case T_FLOAT  : return T_FLOAT;
    case T_DOUBLE : return T_DOUBLE;
#ifdef _LP64
    case T_ARRAY  : // fall through
    case T_OBJECT:  return T_OBJECT;
#endif
    default:
      ShouldNotReachHere();
      return T_ILLEGAL;
  }
}

// ============ Virtual calls ============

void JavaCalls::call_virtual(JavaValue* result, Klass* spec_klass, Symbol* name, Symbol* signature, JavaCallArguments* args, TRAPS) {
  CallInfo callinfo;
  Handle receiver = args->receiver();
  Klass* recvrKlass = receiver.is_null() ? (Klass*)NULL : receiver->klass();
  LinkInfo link_info(spec_klass, name, signature);
  LinkResolver::resolve_virtual_call(
          callinfo, receiver, recvrKlass, link_info, true, CHECK);
  methodHandle method(THREAD, callinfo.selected_method());
  assert(method.not_null(), "should have thrown exception");

  // Invoke the method
  JavaCalls::call(result, method, args, CHECK);
}


void JavaCalls::call_virtual(JavaValue* result, Handle receiver, Klass* spec_klass, Symbol* name, Symbol* signature, TRAPS) {
  JavaCallArguments args(receiver);
  call_virtual(result, spec_klass, name, signature, &args, CHECK);
}


void JavaCalls::call_virtual(JavaValue* result, Handle receiver, Klass* spec_klass, Symbol* name, Symbol* signature, Handle arg1, TRAPS) {
  JavaCallArguments args(receiver);
  args.push_oop(arg1);
  call_virtual(result, spec_klass, name, signature, &args, CHECK);
}



void JavaCalls::call_virtual(JavaValue* result, Handle receiver, Klass* spec_klass, Symbol* name, Symbol* signature, Handle arg1, Handle arg2, TRAPS) {
  JavaCallArguments args(receiver);
  args.push_oop(arg1);
  args.push_oop(arg2);
  call_virtual(result, spec_klass, name, signature, &args, CHECK);
}


// ============ Special calls ============

void JavaCalls::call_special(JavaValue* result, Klass* klass, Symbol* name, Symbol* signature, JavaCallArguments* args, TRAPS) {
  CallInfo callinfo;
  LinkInfo link_info(klass, name, signature);
  LinkResolver::resolve_special_call(callinfo, args->receiver(), link_info, CHECK);
  methodHandle method(THREAD, callinfo.selected_method());
  assert(method.not_null(), "should have thrown exception");

  // Invoke the method
  JavaCalls::call(result, method, args, CHECK);
}


void JavaCalls::call_special(JavaValue* result, Handle receiver, Klass* klass, Symbol* name, Symbol* signature, TRAPS) {
  JavaCallArguments args(receiver);
  call_special(result, klass, name, signature, &args, CHECK);
}


void JavaCalls::call_special(JavaValue* result, Handle receiver, Klass* klass, Symbol* name, Symbol* signature, Handle arg1, TRAPS) {
  JavaCallArguments args(receiver);
  args.push_oop(arg1);
  call_special(result, klass, name, signature, &args, CHECK);
}


void JavaCalls::call_special(JavaValue* result, Handle receiver, Klass* klass, Symbol* name, Symbol* signature, Handle arg1, Handle arg2, TRAPS) {
  JavaCallArguments args(receiver);
  args.push_oop(arg1);
  args.push_oop(arg2);
  call_special(result, klass, name, signature, &args, CHECK);
}


// ============ Static calls ============

void JavaCalls::call_static(JavaValue* result, Klass* klass, Symbol* name, Symbol* signature, JavaCallArguments* args, TRAPS) {
  CallInfo callinfo;
  LinkInfo link_info(klass, name, signature);
  LinkResolver::resolve_static_call(callinfo, link_info, true, CHECK);
  methodHandle method(THREAD, callinfo.selected_method());
  assert(method.not_null(), "should have thrown exception");

  // Invoke the method
  JavaCalls::call(result, method, args, CHECK);
}


void JavaCalls::call_static(JavaValue* result, Klass* klass, Symbol* name, Symbol* signature, TRAPS) {
  JavaCallArguments args;
  call_static(result, klass, name, signature, &args, CHECK);
}


void JavaCalls::call_static(JavaValue* result, Klass* klass, Symbol* name, Symbol* signature, Handle arg1, TRAPS) {
  JavaCallArguments args(arg1);
  call_static(result, klass, name, signature, &args, CHECK);
}


void JavaCalls::call_static(JavaValue* result, Klass* klass, Symbol* name, Symbol* signature, Handle arg1, Handle arg2, TRAPS) {
  JavaCallArguments args;
  args.push_oop(arg1);
  args.push_oop(arg2);
  call_static(result, klass, name, signature, &args, CHECK);
}


void JavaCalls::call_static(JavaValue* result, Klass* klass, Symbol* name, Symbol* signature, Handle arg1, Handle arg2, Handle arg3, TRAPS) {
  JavaCallArguments args;
  args.push_oop(arg1);
  args.push_oop(arg2);
  args.push_oop(arg3);
  call_static(result, klass, name, signature, &args, CHECK);
}

// ============ allocate and initialize new object instance ============

Handle JavaCalls::construct_new_instance(InstanceKlass* klass, Symbol* constructor_signature, JavaCallArguments* args, TRAPS) {
  klass->initialize(CHECK_NH); // Quick no-op if already initialized.
  Handle obj = klass->allocate_instance_handle(CHECK_NH);
  JavaValue void_result(T_VOID);
  args->set_receiver(obj); // inserts <obj> as the first argument.
  JavaCalls::call_special(&void_result, klass,
                          vmSymbols::object_initializer_name(),
                          constructor_signature, args, CHECK_NH);
  // Already returned a Null Handle if any exception is pending.
  return obj;
}

Handle JavaCalls::construct_new_instance(InstanceKlass* klass, Symbol* constructor_signature, TRAPS) {
  JavaCallArguments args;
  return JavaCalls::construct_new_instance(klass, constructor_signature, &args, THREAD);
}

Handle JavaCalls::construct_new_instance(InstanceKlass* klass, Symbol* constructor_signature, Handle arg1, TRAPS) {
  JavaCallArguments args;
  args.push_oop(arg1);
  return JavaCalls::construct_new_instance(klass, constructor_signature, &args, THREAD);
}

Handle JavaCalls::construct_new_instance(InstanceKlass* klass, Symbol* constructor_signature, Handle arg1, Handle arg2, TRAPS) {
  JavaCallArguments args;
  args.push_oop(arg1);
  args.push_oop(arg2);
  return JavaCalls::construct_new_instance(klass, constructor_signature, &args, THREAD);
}

// -------------------------------------------------
// Implementation of JavaCalls (low level)


void JavaCalls::call(JavaValue* result, const methodHandle& method, JavaCallArguments* args, TRAPS) {
  // Check if we need to wrap a potential OS exception handler around thread.
  // This is used for e.g. Win32 structured exception handlers.
  // Need to wrap each and every time, since there might be native code down the
  // stack that has installed its own exception handlers.
  os::os_exception_wrapper(call_helper, result, method, args, THREAD);
}

void JavaCalls::call_helper(JavaValue* result, const methodHandle& method, JavaCallArguments* args, TRAPS) {

  JavaThread* thread = THREAD;
  assert(method.not_null(), "must have a method to call");
  assert(!SafepointSynchronize::is_at_safepoint(), "call to Java code during VM operation");
  assert(!thread->handle_area()->no_handle_mark_active(), "cannot call out to Java here");

  // Verify the arguments
  if (JVMCI_ONLY(args->alternative_target().is_null() &&) (DEBUG_ONLY(true ||) CheckJNICalls)) {
    args->verify(method, result->get_type());
  }
  // Ignore call if method is empty
  if (JVMCI_ONLY(args->alternative_target().is_null() &&) method->is_empty_method()) {
    assert(result->get_type() == T_VOID, "an empty method must return a void value");
    return;
  }

#ifdef ASSERT
  { InstanceKlass* holder = method->method_holder();
    // A klass might not be initialized since JavaCall's might be used during the executing of
    // the <clinit>. For example, a Thread.start might start executing on an object that is
    // not fully initialized! (bad Java programming style)
    assert(holder->is_linked(), "rewriting must have taken place");
  }
#endif

  CompilationPolicy::compile_if_required(method, CHECK);

  // Since the call stub sets up like the interpreter we call the from_interpreted_entry
  // so we can go compiled via a i2c. Otherwise initial entry method will always
  // run interpreted.
  address entry_point = method->from_interpreted_entry();
  if (JvmtiExport::can_post_interpreter_events() && thread->is_interp_only_mode()) {
    entry_point = method->interpreter_entry();
  }

  // Figure out if the result value is an oop or not (Note: This is a different value
  // than result_type. result_type will be T_INT of oops. (it is about size)
  BasicType result_type = runtime_type_from(result);
  bool oop_result_flag = is_reference_type(result->get_type());

  // Find receiver
  Handle receiver = (!method->is_static()) ? args->receiver() : Handle();

  // When we reenter Java, we need to reenable the reserved/yellow zone which
  // might already be disabled when we are in VM.
  thread->stack_overflow_state()->reguard_stack_if_needed();

  // Check that there are shadow pages available before changing thread state
  // to Java. Calculate current_stack_pointer here to make sure
  // stack_shadow_pages_available() and bang_stack_shadow_pages() use the same sp.
  address sp = os::current_stack_pointer();
  if (!os::stack_shadow_pages_available(THREAD, method, sp)) {
    // Throw stack overflow exception with preinitialized exception.
    Exceptions::throw_stack_overflow_exception(THREAD, __FILE__, __LINE__, method);
    return;
  } else {
    // Touch pages checked if the OS needs them to be touched to be mapped.
    os::map_stack_shadow_pages(sp);
  }

  // do call
  { JavaCallWrapper link(method, receiver, result, CHECK);
    { HandleMark hm(thread);  // HandleMark used by HandleMarkCleaner

      // NOTE: if we move the computation of the result_val_address inside
      // the call to call_stub, the optimizer produces wrong code.
      intptr_t* result_val_address = (intptr_t*)(result->get_value_addr());
      intptr_t* parameter_address = args->parameters();
#if INCLUDE_JVMCI
      // Gets the alternative target (if any) that should be called
      Handle alternative_target = args->alternative_target();
      if (!alternative_target.is_null()) {
        // Must extract verified entry point from HotSpotNmethod after VM to Java
        // transition in JavaCallWrapper constructor so that it is safe with
        // respect to nmethod sweeping.
        address verified_entry_point = (address) HotSpotJVMCI::InstalledCode::entryPoint(NULL, alternative_target());
        if (verified_entry_point != NULL) {
          thread->set_jvmci_alternate_call_target(verified_entry_point);
          entry_point = method->adapter()->get_i2c_entry();
        }
      }
#endif
      StubRoutines::call_stub()(
        (address)&link,
        // (intptr_t*)&(result->_value), // see NOTE above (compiler problem)
        result_val_address,          // see NOTE above (compiler problem)
        result_type,
        method(),
        entry_point,
        parameter_address,
        args->size_of_parameters(),
        CHECK
      );

      result = link.result();  // circumvent MS C++ 5.0 compiler bug (result is clobbered across call)
      // Preserve oop return value across possible gc points
      if (oop_result_flag) {
        thread->set_vm_result(result->get_oop());
      }
    }
  } // Exit JavaCallWrapper (can block - potential return oop must be preserved)

  // Check if a thread stop or suspend should be executed
  // The following assert was not realistic.  Thread.stop can set that bit at any moment.
  //assert(!thread->has_special_runtime_exit_condition(), "no async. exceptions should be installed");

  // Restore possible oop return
  if (oop_result_flag) {
    result->set_oop(thread->vm_result());
    thread->set_vm_result(NULL);
  }
}


//--------------------------------------------------------------------------------------
// Implementation of JavaCallArguments

inline bool is_value_state_indirect_oop(uint state) {
  assert(state != JavaCallArguments::value_state_oop,
         "Checking for handles after removal");
  assert(state < JavaCallArguments::value_state_limit,
         "Invalid value state %u", state);
  return state != JavaCallArguments::value_state_primitive;
}

inline oop resolve_indirect_oop(intptr_t value, uint state) {
  switch (state) {
  case JavaCallArguments::value_state_handle:
  {
    oop* ptr = reinterpret_cast<oop*>(value);
    return Handle::raw_resolve(ptr);
  }

  case JavaCallArguments::value_state_jobject:
  {
    jobject obj = reinterpret_cast<jobject>(value);
    return JNIHandles::resolve(obj);
  }

  default:
    ShouldNotReachHere();
    return NULL;
  }
}

intptr_t* JavaCallArguments::parameters() {
  // First convert all handles to oops
  for(int i = 0; i < _size; i++) {
    uint state = _value_state[i];
    assert(state != value_state_oop, "Multiple handle conversions");
    if (is_value_state_indirect_oop(state)) {
      oop obj = resolve_indirect_oop(_value[i], state);
      _value[i] = cast_from_oop<intptr_t>(obj);
      _value_state[i] = value_state_oop;
    }
  }
  // Return argument vector
  return _value;
}


class SignatureChekker : public SignatureIterator {
 private:
   int _pos;
   BasicType _return_type;
   u_char* _value_state;
   intptr_t* _value;

 public:
  SignatureChekker(Symbol* signature,
                   BasicType return_type,
                   bool is_static,
                   u_char* value_state,
                   intptr_t* value) :
    SignatureIterator(signature),
    _pos(0),
    _return_type(return_type),
    _value_state(value_state),
    _value(value)
  {
    if (!is_static) {
      check_value(true); // Receiver must be an oop
    }
    do_parameters_on(this);
    check_return_type(return_type);
  }

 private:
  void check_value(bool is_reference) {
    uint state = _value_state[_pos++];
    if (is_reference) {
      guarantee(is_value_state_indirect_oop(state),
                "signature does not match pushed arguments: %u at %d",
                state, _pos - 1);
    } else {
      guarantee(state == JavaCallArguments::value_state_primitive,
                "signature does not match pushed arguments: %u at %d",
                state, _pos - 1);
    }
  }

  void check_return_type(BasicType t) {
    guarantee(t == _return_type, "return type does not match");
  }

  void check_single_word() {
    check_value(false);
  }

  void check_double_word() {
    check_value(false);
    check_value(false);
  }

  void check_reference() {
    intptr_t v = _value[_pos];
    if (v != 0) {
      // v is a "handle" referring to an oop, cast to integral type.
      // There shouldn't be any handles in very low memory.
      guarantee((size_t)v >= (size_t)os::vm_page_size(),
                "Bad JNI oop argument %d: " PTR_FORMAT, _pos, v);
      // Verify the pointee.
      oop vv = resolve_indirect_oop(v, _value_state[_pos]);
      guarantee(oopDesc::is_oop_or_null(vv, true),
                "Bad JNI oop argument %d: " PTR_FORMAT " -> " PTR_FORMAT,
                _pos, v, p2i(vv));
    }

    check_value(true);          // Verify value state.
  }

  friend class SignatureIterator;  // so do_parameters_on can call do_type
  void do_type(BasicType type) {
    switch (type) {
    case T_BYTE:
    case T_BOOLEAN:
    case T_CHAR:
    case T_SHORT:
    case T_INT:
    case T_FLOAT:  // this one also
      check_single_word(); break;
    case T_LONG:
    case T_DOUBLE:
      check_double_word(); break;
    case T_ARRAY:
    case T_OBJECT:
      check_reference(); break;
    default:
      ShouldNotReachHere();
    }
  }
};


void JavaCallArguments::verify(const methodHandle& method, BasicType return_type) {
  guarantee(method->size_of_parameters() == size_of_parameters(), "wrong no. of arguments pushed");

  // Treat T_OBJECT and T_ARRAY as the same
  if (is_reference_type(return_type)) return_type = T_OBJECT;

  // Check that oop information is correct
  Symbol* signature = method->signature();

  SignatureChekker sc(signature,
                      return_type,
                      method->is_static(),
                      _value_state,
                      _value);
}
