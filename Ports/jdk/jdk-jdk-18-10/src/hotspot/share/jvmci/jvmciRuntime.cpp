/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include "precompiled.hpp"
#include "classfile/javaClasses.inline.hpp"
#include "classfile/symbolTable.hpp"
#include "classfile/systemDictionary.hpp"
#include "classfile/vmClasses.hpp"
#include "compiler/compileBroker.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "gc/shared/oopStorage.inline.hpp"
#include "jvmci/jniAccessMark.inline.hpp"
#include "jvmci/jvmciCompilerToVM.hpp"
#include "jvmci/jvmciRuntime.hpp"
#include "jvmci/metadataHandles.hpp"
#include "logging/log.hpp"
#include "memory/oopFactory.hpp"
#include "memory/universe.hpp"
#include "oops/constantPool.inline.hpp"
#include "oops/klass.inline.hpp"
#include "oops/method.inline.hpp"
#include "oops/objArrayKlass.hpp"
#include "oops/oop.inline.hpp"
#include "oops/typeArrayOop.inline.hpp"
#include "prims/jvmtiExport.hpp"
#include "prims/methodHandles.hpp"
#include "runtime/atomic.hpp"
#include "runtime/deoptimization.hpp"
#include "runtime/fieldDescriptor.inline.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/java.hpp"
#include "runtime/jniHandles.inline.hpp"
#include "runtime/reflectionUtils.hpp"
#include "runtime/sharedRuntime.hpp"
#if INCLUDE_G1GC
#include "gc/g1/g1BarrierSetRuntime.hpp"
#endif // INCLUDE_G1GC

// Simple helper to see if the caller of a runtime stub which
// entered the VM has been deoptimized

static bool caller_is_deopted() {
  JavaThread* thread = JavaThread::current();
  RegisterMap reg_map(thread, false);
  frame runtime_frame = thread->last_frame();
  frame caller_frame = runtime_frame.sender(&reg_map);
  assert(caller_frame.is_compiled_frame(), "must be compiled");
  return caller_frame.is_deoptimized_frame();
}

// Stress deoptimization
static void deopt_caller() {
  if ( !caller_is_deopted()) {
    JavaThread* thread = JavaThread::current();
    RegisterMap reg_map(thread, false);
    frame runtime_frame = thread->last_frame();
    frame caller_frame = runtime_frame.sender(&reg_map);
    Deoptimization::deoptimize_frame(thread, caller_frame.id(), Deoptimization::Reason_constraint);
    assert(caller_is_deopted(), "Must be deoptimized");
  }
}

// Manages a scope for a JVMCI runtime call that attempts a heap allocation.
// If there is a pending nonasync exception upon closing the scope and the runtime
// call is of the variety where allocation failure returns NULL without an
// exception, the following action is taken:
//   1. The pending nonasync exception is cleared
//   2. NULL is written to JavaThread::_vm_result
//   3. Checks that an OutOfMemoryError is Universe::out_of_memory_error_retry().
class RetryableAllocationMark: public StackObj {
 private:
  JavaThread* _thread;
 public:
  RetryableAllocationMark(JavaThread* thread, bool activate) {
    if (activate) {
      assert(!thread->in_retryable_allocation(), "retryable allocation scope is non-reentrant");
      _thread = thread;
      _thread->set_in_retryable_allocation(true);
    } else {
      _thread = NULL;
    }
  }
  ~RetryableAllocationMark() {
    if (_thread != NULL) {
      _thread->set_in_retryable_allocation(false);
      JavaThread* THREAD = _thread; // For exception macros.
      if (HAS_PENDING_EXCEPTION) {
        oop ex = PENDING_EXCEPTION;
        // Do not clear probable async exceptions.
        CLEAR_PENDING_NONASYNC_EXCEPTION;
        oop retry_oome = Universe::out_of_memory_error_retry();
        if (ex->is_a(retry_oome->klass()) && retry_oome != ex) {
          ResourceMark rm;
          fatal("Unexpected exception in scope of retryable allocation: " INTPTR_FORMAT " of type %s", p2i(ex), ex->klass()->external_name());
        }
        _thread->set_vm_result(NULL);
      }
    }
  }
};

JRT_BLOCK_ENTRY(void, JVMCIRuntime::new_instance_common(JavaThread* current, Klass* klass, bool null_on_fail))
  JRT_BLOCK;
  assert(klass->is_klass(), "not a class");
  Handle holder(current, klass->klass_holder()); // keep the klass alive
  InstanceKlass* h = InstanceKlass::cast(klass);
  {
    RetryableAllocationMark ram(current, null_on_fail);
    h->check_valid_for_instantiation(true, CHECK);
    oop obj;
    if (null_on_fail) {
      if (!h->is_initialized()) {
        // Cannot re-execute class initialization without side effects
        // so return without attempting the initialization
        return;
      }
    } else {
      // make sure klass is initialized
      h->initialize(CHECK);
    }
    // allocate instance and return via TLS
    obj = h->allocate_instance(CHECK);
    current->set_vm_result(obj);
  }
  JRT_BLOCK_END;
  SharedRuntime::on_slowpath_allocation_exit(current);
JRT_END

JRT_BLOCK_ENTRY(void, JVMCIRuntime::new_array_common(JavaThread* current, Klass* array_klass, jint length, bool null_on_fail))
  JRT_BLOCK;
  // Note: no handle for klass needed since they are not used
  //       anymore after new_objArray() and no GC can happen before.
  //       (This may have to change if this code changes!)
  assert(array_klass->is_klass(), "not a class");
  oop obj;
  if (array_klass->is_typeArray_klass()) {
    BasicType elt_type = TypeArrayKlass::cast(array_klass)->element_type();
    RetryableAllocationMark ram(current, null_on_fail);
    obj = oopFactory::new_typeArray(elt_type, length, CHECK);
  } else {
    Handle holder(current, array_klass->klass_holder()); // keep the klass alive
    Klass* elem_klass = ObjArrayKlass::cast(array_klass)->element_klass();
    RetryableAllocationMark ram(current, null_on_fail);
    obj = oopFactory::new_objArray(elem_klass, length, CHECK);
  }
  current->set_vm_result(obj);
  // This is pretty rare but this runtime patch is stressful to deoptimization
  // if we deoptimize here so force a deopt to stress the path.
  if (DeoptimizeALot) {
    static int deopts = 0;
    // Alternate between deoptimizing and raising an error (which will also cause a deopt)
    if (deopts++ % 2 == 0) {
      if (null_on_fail) {
        return;
      } else {
        ResourceMark rm(current);
        THROW(vmSymbols::java_lang_OutOfMemoryError());
      }
    } else {
      deopt_caller();
    }
  }
  JRT_BLOCK_END;
  SharedRuntime::on_slowpath_allocation_exit(current);
JRT_END

JRT_ENTRY(void, JVMCIRuntime::new_multi_array_common(JavaThread* current, Klass* klass, int rank, jint* dims, bool null_on_fail))
  assert(klass->is_klass(), "not a class");
  assert(rank >= 1, "rank must be nonzero");
  Handle holder(current, klass->klass_holder()); // keep the klass alive
  RetryableAllocationMark ram(current, null_on_fail);
  oop obj = ArrayKlass::cast(klass)->multi_allocate(rank, dims, CHECK);
  current->set_vm_result(obj);
JRT_END

JRT_ENTRY(void, JVMCIRuntime::dynamic_new_array_common(JavaThread* current, oopDesc* element_mirror, jint length, bool null_on_fail))
  RetryableAllocationMark ram(current, null_on_fail);
  oop obj = Reflection::reflect_new_array(element_mirror, length, CHECK);
  current->set_vm_result(obj);
JRT_END

JRT_ENTRY(void, JVMCIRuntime::dynamic_new_instance_common(JavaThread* current, oopDesc* type_mirror, bool null_on_fail))
  InstanceKlass* klass = InstanceKlass::cast(java_lang_Class::as_Klass(type_mirror));

  if (klass == NULL) {
    ResourceMark rm(current);
    THROW(vmSymbols::java_lang_InstantiationException());
  }
  RetryableAllocationMark ram(current, null_on_fail);

  // Create new instance (the receiver)
  klass->check_valid_for_instantiation(false, CHECK);

  if (null_on_fail) {
    if (!klass->is_initialized()) {
      // Cannot re-execute class initialization without side effects
      // so return without attempting the initialization
      return;
    }
  } else {
    // Make sure klass gets initialized
    klass->initialize(CHECK);
  }

  oop obj = klass->allocate_instance(CHECK);
  current->set_vm_result(obj);
JRT_END

extern void vm_exit(int code);

// Enter this method from compiled code handler below. This is where we transition
// to VM mode. This is done as a helper routine so that the method called directly
// from compiled code does not have to transition to VM. This allows the entry
// method to see if the nmethod that we have just looked up a handler for has
// been deoptimized while we were in the vm. This simplifies the assembly code
// cpu directories.
//
// We are entering here from exception stub (via the entry method below)
// If there is a compiled exception handler in this method, we will continue there;
// otherwise we will unwind the stack and continue at the caller of top frame method
// Note: we enter in Java using a special JRT wrapper. This wrapper allows us to
// control the area where we can allow a safepoint. After we exit the safepoint area we can
// check to see if the handler we are going to return is now in a nmethod that has
// been deoptimized. If that is the case we return the deopt blob
// unpack_with_exception entry instead. This makes life for the exception blob easier
// because making that same check and diverting is painful from assembly language.
JRT_ENTRY_NO_ASYNC(static address, exception_handler_for_pc_helper(JavaThread* current, oopDesc* ex, address pc, CompiledMethod*& cm))
  // Reset method handle flag.
  current->set_is_method_handle_return(false);

  Handle exception(current, ex);
  cm = CodeCache::find_compiled(pc);
  assert(cm != NULL, "this is not a compiled method");
  // Adjust the pc as needed/
  if (cm->is_deopt_pc(pc)) {
    RegisterMap map(current, false);
    frame exception_frame = current->last_frame().sender(&map);
    // if the frame isn't deopted then pc must not correspond to the caller of last_frame
    assert(exception_frame.is_deoptimized_frame(), "must be deopted");
    pc = exception_frame.pc();
  }
  assert(exception.not_null(), "NULL exceptions should be handled by throw_exception");
  assert(oopDesc::is_oop(exception()), "just checking");
  // Check that exception is a subclass of Throwable
  assert(exception->is_a(vmClasses::Throwable_klass()),
         "Exception not subclass of Throwable");

  // debugging support
  // tracing
  if (log_is_enabled(Info, exceptions)) {
    ResourceMark rm;
    stringStream tempst;
    assert(cm->method() != NULL, "Unexpected null method()");
    tempst.print("JVMCI compiled method <%s>\n"
                 " at PC" INTPTR_FORMAT " for thread " INTPTR_FORMAT,
                 cm->method()->print_value_string(), p2i(pc), p2i(current));
    Exceptions::log_exception(exception, tempst.as_string());
  }
  // for AbortVMOnException flag
  Exceptions::debug_check_abort(exception);

  // Check the stack guard pages and reenable them if necessary and there is
  // enough space on the stack to do so.  Use fast exceptions only if the guard
  // pages are enabled.
  bool guard_pages_enabled = current->stack_overflow_state()->reguard_stack_if_needed();

  if (JvmtiExport::can_post_on_exceptions()) {
    // To ensure correct notification of exception catches and throws
    // we have to deoptimize here.  If we attempted to notify the
    // catches and throws during this exception lookup it's possible
    // we could deoptimize on the way out of the VM and end back in
    // the interpreter at the throw site.  This would result in double
    // notifications since the interpreter would also notify about
    // these same catches and throws as it unwound the frame.

    RegisterMap reg_map(current);
    frame stub_frame = current->last_frame();
    frame caller_frame = stub_frame.sender(&reg_map);

    // We don't really want to deoptimize the nmethod itself since we
    // can actually continue in the exception handler ourselves but I
    // don't see an easy way to have the desired effect.
    Deoptimization::deoptimize_frame(current, caller_frame.id(), Deoptimization::Reason_constraint);
    assert(caller_is_deopted(), "Must be deoptimized");

    return SharedRuntime::deopt_blob()->unpack_with_exception_in_tls();
  }

  // ExceptionCache is used only for exceptions at call sites and not for implicit exceptions
  if (guard_pages_enabled) {
    address fast_continuation = cm->handler_for_exception_and_pc(exception, pc);
    if (fast_continuation != NULL) {
      // Set flag if return address is a method handle call site.
      current->set_is_method_handle_return(cm->is_method_handle_return(pc));
      return fast_continuation;
    }
  }

  // If the stack guard pages are enabled, check whether there is a handler in
  // the current method.  Otherwise (guard pages disabled), force an unwind and
  // skip the exception cache update (i.e., just leave continuation==NULL).
  address continuation = NULL;
  if (guard_pages_enabled) {

    // New exception handling mechanism can support inlined methods
    // with exception handlers since the mappings are from PC to PC

    // Clear out the exception oop and pc since looking up an
    // exception handler can cause class loading, which might throw an
    // exception and those fields are expected to be clear during
    // normal bytecode execution.
    current->clear_exception_oop_and_pc();

    bool recursive_exception = false;
    continuation = SharedRuntime::compute_compiled_exc_handler(cm, pc, exception, false, false, recursive_exception);
    // If an exception was thrown during exception dispatch, the exception oop may have changed
    current->set_exception_oop(exception());
    current->set_exception_pc(pc);

    // The exception cache is used only for non-implicit exceptions
    // Update the exception cache only when another exception did
    // occur during the computation of the compiled exception handler
    // (e.g., when loading the class of the catch type).
    // Checking for exception oop equality is not
    // sufficient because some exceptions are pre-allocated and reused.
    if (continuation != NULL && !recursive_exception && !SharedRuntime::deopt_blob()->contains(continuation)) {
      cm->add_handler_for_exception_and_pc(exception, pc, continuation);
    }
  }

  // Set flag if return address is a method handle call site.
  current->set_is_method_handle_return(cm->is_method_handle_return(pc));

  if (log_is_enabled(Info, exceptions)) {
    ResourceMark rm;
    log_info(exceptions)("Thread " PTR_FORMAT " continuing at PC " PTR_FORMAT
                         " for exception thrown at PC " PTR_FORMAT,
                         p2i(current), p2i(continuation), p2i(pc));
  }

  return continuation;
JRT_END

// Enter this method from compiled code only if there is a Java exception handler
// in the method handling the exception.
// We are entering here from exception stub. We don't do a normal VM transition here.
// We do it in a helper. This is so we can check to see if the nmethod we have just
// searched for an exception handler has been deoptimized in the meantime.
address JVMCIRuntime::exception_handler_for_pc(JavaThread* current) {
  oop exception = current->exception_oop();
  address pc = current->exception_pc();
  // Still in Java mode
  DEBUG_ONLY(NoHandleMark nhm);
  CompiledMethod* cm = NULL;
  address continuation = NULL;
  {
    // Enter VM mode by calling the helper
    ResetNoHandleMark rnhm;
    continuation = exception_handler_for_pc_helper(current, exception, pc, cm);
  }
  // Back in JAVA, use no oops DON'T safepoint

  // Now check to see if the compiled method we were called from is now deoptimized.
  // If so we must return to the deopt blob and deoptimize the nmethod
  if (cm != NULL && caller_is_deopted()) {
    continuation = SharedRuntime::deopt_blob()->unpack_with_exception_in_tls();
  }

  assert(continuation != NULL, "no handler found");
  return continuation;
}

JRT_BLOCK_ENTRY(void, JVMCIRuntime::monitorenter(JavaThread* current, oopDesc* obj, BasicLock* lock))
  SharedRuntime::monitor_enter_helper(obj, lock, current);
JRT_END

JRT_LEAF(void, JVMCIRuntime::monitorexit(JavaThread* current, oopDesc* obj, BasicLock* lock))
  assert(current->last_Java_sp(), "last_Java_sp must be set");
  assert(oopDesc::is_oop(obj), "invalid lock object pointer dected");
  SharedRuntime::monitor_exit_helper(obj, lock, current);
JRT_END

// Object.notify() fast path, caller does slow path
JRT_LEAF(jboolean, JVMCIRuntime::object_notify(JavaThread* current, oopDesc* obj))

  // Very few notify/notifyAll operations find any threads on the waitset, so
  // the dominant fast-path is to simply return.
  // Relatedly, it's critical that notify/notifyAll be fast in order to
  // reduce lock hold times.
  if (!SafepointSynchronize::is_synchronizing()) {
    if (ObjectSynchronizer::quick_notify(obj, current, false)) {
      return true;
    }
  }
  return false; // caller must perform slow path

JRT_END

// Object.notifyAll() fast path, caller does slow path
JRT_LEAF(jboolean, JVMCIRuntime::object_notifyAll(JavaThread* current, oopDesc* obj))

  if (!SafepointSynchronize::is_synchronizing() ) {
    if (ObjectSynchronizer::quick_notify(obj, current, true)) {
      return true;
    }
  }
  return false; // caller must perform slow path

JRT_END

JRT_BLOCK_ENTRY(int, JVMCIRuntime::throw_and_post_jvmti_exception(JavaThread* current, const char* exception, const char* message))
  JRT_BLOCK;
  TempNewSymbol symbol = SymbolTable::new_symbol(exception);
  SharedRuntime::throw_and_post_jvmti_exception(current, symbol, message);
  JRT_BLOCK_END;
  return caller_is_deopted();
JRT_END

JRT_BLOCK_ENTRY(int, JVMCIRuntime::throw_klass_external_name_exception(JavaThread* current, const char* exception, Klass* klass))
  JRT_BLOCK;
  ResourceMark rm(current);
  TempNewSymbol symbol = SymbolTable::new_symbol(exception);
  SharedRuntime::throw_and_post_jvmti_exception(current, symbol, klass->external_name());
  JRT_BLOCK_END;
  return caller_is_deopted();
JRT_END

JRT_BLOCK_ENTRY(int, JVMCIRuntime::throw_class_cast_exception(JavaThread* current, const char* exception, Klass* caster_klass, Klass* target_klass))
  JRT_BLOCK;
  ResourceMark rm(current);
  const char* message = SharedRuntime::generate_class_cast_message(caster_klass, target_klass);
  TempNewSymbol symbol = SymbolTable::new_symbol(exception);
  SharedRuntime::throw_and_post_jvmti_exception(current, symbol, message);
  JRT_BLOCK_END;
  return caller_is_deopted();
JRT_END

class ArgumentPusher : public SignatureIterator {
 protected:
  JavaCallArguments*  _jca;
  jlong _argument;
  bool _pushed;

  jlong next_arg() {
    guarantee(!_pushed, "one argument");
    _pushed = true;
    return _argument;
  }

  float next_float() {
    guarantee(!_pushed, "one argument");
    _pushed = true;
    jvalue v;
    v.i = (jint) _argument;
    return v.f;
  }

  double next_double() {
    guarantee(!_pushed, "one argument");
    _pushed = true;
    jvalue v;
    v.j = _argument;
    return v.d;
  }

  Handle next_object() {
    guarantee(!_pushed, "one argument");
    _pushed = true;
    return Handle(Thread::current(), cast_to_oop(_argument));
  }

 public:
  ArgumentPusher(Symbol* signature, JavaCallArguments*  jca, jlong argument) : SignatureIterator(signature) {
    this->_return_type = T_ILLEGAL;
    _jca = jca;
    _argument = argument;
    _pushed = false;
    do_parameters_on(this);
  }

  void do_type(BasicType type) {
    switch (type) {
      case T_OBJECT:
      case T_ARRAY:   _jca->push_oop(next_object());         break;
      case T_BOOLEAN: _jca->push_int((jboolean) next_arg()); break;
      case T_CHAR:    _jca->push_int((jchar) next_arg());    break;
      case T_SHORT:   _jca->push_int((jint)  next_arg());    break;
      case T_BYTE:    _jca->push_int((jbyte) next_arg());    break;
      case T_INT:     _jca->push_int((jint)  next_arg());    break;
      case T_LONG:    _jca->push_long((jlong) next_arg());   break;
      case T_FLOAT:   _jca->push_float(next_float());        break;
      case T_DOUBLE:  _jca->push_double(next_double());      break;
      default:        fatal("Unexpected type %s", type2name(type));
    }
  }
};


JRT_ENTRY(jlong, JVMCIRuntime::invoke_static_method_one_arg(JavaThread* current, Method* method, jlong argument))
  ResourceMark rm;
  HandleMark hm(current);

  methodHandle mh(current, method);
  if (mh->size_of_parameters() > 1 && !mh->is_static()) {
    THROW_MSG_0(vmSymbols::java_lang_IllegalArgumentException(), "Invoked method must be static and take at most one argument");
  }

  Symbol* signature = mh->signature();
  JavaCallArguments jca(mh->size_of_parameters());
  ArgumentPusher jap(signature, &jca, argument);
  BasicType return_type = jap.return_type();
  JavaValue result(return_type);
  JavaCalls::call(&result, mh, &jca, CHECK_0);

  if (return_type == T_VOID) {
    return 0;
  } else if (return_type == T_OBJECT || return_type == T_ARRAY) {
    current->set_vm_result(result.get_oop());
    return 0;
  } else {
    jvalue *value = (jvalue *) result.get_value_addr();
    // Narrow the value down if required (Important on big endian machines)
    switch (return_type) {
      case T_BOOLEAN:
        return (jboolean) value->i;
      case T_BYTE:
        return (jbyte) value->i;
      case T_CHAR:
        return (jchar) value->i;
      case T_SHORT:
        return (jshort) value->i;
      case T_INT:
      case T_FLOAT:
        return value->i;
      case T_LONG:
      case T_DOUBLE:
        return value->j;
      default:
        fatal("Unexpected type %s", type2name(return_type));
        return 0;
    }
  }
JRT_END

JRT_LEAF(void, JVMCIRuntime::log_object(JavaThread* thread, oopDesc* obj, bool as_string, bool newline))
  ttyLocker ttyl;

  if (obj == NULL) {
    tty->print("NULL");
  } else if (oopDesc::is_oop_or_null(obj, true) && (!as_string || !java_lang_String::is_instance(obj))) {
    if (oopDesc::is_oop_or_null(obj, true)) {
      char buf[O_BUFLEN];
      tty->print("%s@" INTPTR_FORMAT, obj->klass()->name()->as_C_string(buf, O_BUFLEN), p2i(obj));
    } else {
      tty->print(INTPTR_FORMAT, p2i(obj));
    }
  } else {
    ResourceMark rm;
    assert(obj != NULL && java_lang_String::is_instance(obj), "must be");
    char *buf = java_lang_String::as_utf8_string(obj);
    tty->print_raw(buf);
  }
  if (newline) {
    tty->cr();
  }
JRT_END

#if INCLUDE_G1GC

void JVMCIRuntime::write_barrier_pre(JavaThread* thread, oopDesc* obj) {
  G1BarrierSetRuntime::write_ref_field_pre_entry(obj, thread);
}

void JVMCIRuntime::write_barrier_post(JavaThread* thread, volatile CardValue* card_addr) {
  G1BarrierSetRuntime::write_ref_field_post_entry(card_addr, thread);
}

#endif // INCLUDE_G1GC

JRT_LEAF(jboolean, JVMCIRuntime::validate_object(JavaThread* thread, oopDesc* parent, oopDesc* child))
  bool ret = true;
  if(!Universe::heap()->is_in(parent)) {
    tty->print_cr("Parent Object " INTPTR_FORMAT " not in heap", p2i(parent));
    parent->print();
    ret=false;
  }
  if(!Universe::heap()->is_in(child)) {
    tty->print_cr("Child Object " INTPTR_FORMAT " not in heap", p2i(child));
    child->print();
    ret=false;
  }
  return (jint)ret;
JRT_END

JRT_ENTRY(void, JVMCIRuntime::vm_error(JavaThread* current, jlong where, jlong format, jlong value))
  ResourceMark rm(current);
  const char *error_msg = where == 0L ? "<internal JVMCI error>" : (char*) (address) where;
  char *detail_msg = NULL;
  if (format != 0L) {
    const char* buf = (char*) (address) format;
    size_t detail_msg_length = strlen(buf) * 2;
    detail_msg = (char *) NEW_RESOURCE_ARRAY(u_char, detail_msg_length);
    jio_snprintf(detail_msg, detail_msg_length, buf, value);
  }
  report_vm_error(__FILE__, __LINE__, error_msg, "%s", detail_msg);
JRT_END

JRT_LEAF(oopDesc*, JVMCIRuntime::load_and_clear_exception(JavaThread* thread))
  oop exception = thread->exception_oop();
  assert(exception != NULL, "npe");
  thread->set_exception_oop(NULL);
  thread->set_exception_pc(0);
  return exception;
JRT_END

PRAGMA_DIAG_PUSH
PRAGMA_FORMAT_NONLITERAL_IGNORED
JRT_LEAF(void, JVMCIRuntime::log_printf(JavaThread* thread, const char* format, jlong v1, jlong v2, jlong v3))
  ResourceMark rm;
  tty->print(format, v1, v2, v3);
JRT_END
PRAGMA_DIAG_POP

static void decipher(jlong v, bool ignoreZero) {
  if (v != 0 || !ignoreZero) {
    void* p = (void *)(address) v;
    CodeBlob* cb = CodeCache::find_blob(p);
    if (cb) {
      if (cb->is_nmethod()) {
        char buf[O_BUFLEN];
        tty->print("%s [" INTPTR_FORMAT "+" JLONG_FORMAT "]", cb->as_nmethod_or_null()->method()->name_and_sig_as_C_string(buf, O_BUFLEN), p2i(cb->code_begin()), (jlong)((address)v - cb->code_begin()));
        return;
      }
      cb->print_value_on(tty);
      return;
    }
    if (Universe::heap()->is_in(p)) {
      oop obj = cast_to_oop(p);
      obj->print_value_on(tty);
      return;
    }
    tty->print(INTPTR_FORMAT " [long: " JLONG_FORMAT ", double %lf, char %c]",p2i((void *)v), (jlong)v, (jdouble)v, (char)v);
  }
}

PRAGMA_DIAG_PUSH
PRAGMA_FORMAT_NONLITERAL_IGNORED
JRT_LEAF(void, JVMCIRuntime::vm_message(jboolean vmError, jlong format, jlong v1, jlong v2, jlong v3))
  ResourceMark rm;
  const char *buf = (const char*) (address) format;
  if (vmError) {
    if (buf != NULL) {
      fatal(buf, v1, v2, v3);
    } else {
      fatal("<anonymous error>");
    }
  } else if (buf != NULL) {
    tty->print(buf, v1, v2, v3);
  } else {
    assert(v2 == 0, "v2 != 0");
    assert(v3 == 0, "v3 != 0");
    decipher(v1, false);
  }
JRT_END
PRAGMA_DIAG_POP

JRT_LEAF(void, JVMCIRuntime::log_primitive(JavaThread* thread, jchar typeChar, jlong value, jboolean newline))
  union {
      jlong l;
      jdouble d;
      jfloat f;
  } uu;
  uu.l = value;
  switch (typeChar) {
    case 'Z': tty->print(value == 0 ? "false" : "true"); break;
    case 'B': tty->print("%d", (jbyte) value); break;
    case 'C': tty->print("%c", (jchar) value); break;
    case 'S': tty->print("%d", (jshort) value); break;
    case 'I': tty->print("%d", (jint) value); break;
    case 'F': tty->print("%f", uu.f); break;
    case 'J': tty->print(JLONG_FORMAT, value); break;
    case 'D': tty->print("%lf", uu.d); break;
    default: assert(false, "unknown typeChar"); break;
  }
  if (newline) {
    tty->cr();
  }
JRT_END

JRT_ENTRY(jint, JVMCIRuntime::identity_hash_code(JavaThread* current, oopDesc* obj))
  return (jint) obj->identity_hash();
JRT_END

JRT_ENTRY(jint, JVMCIRuntime::test_deoptimize_call_int(JavaThread* current, int value))
  deopt_caller();
  return (jint) value;
JRT_END


// private static JVMCIRuntime JVMCI.initializeRuntime()
JVM_ENTRY_NO_ENV(jobject, JVM_GetJVMCIRuntime(JNIEnv *env, jclass c))
  JNI_JVMCIENV(thread, env);
  if (!EnableJVMCI) {
    JVMCI_THROW_MSG_NULL(InternalError, "JVMCI is not enabled");
  }
  JVMCIENV->runtime()->initialize_HotSpotJVMCIRuntime(JVMCI_CHECK_NULL);
  JVMCIObject runtime = JVMCIENV->runtime()->get_HotSpotJVMCIRuntime(JVMCI_CHECK_NULL);
  return JVMCIENV->get_jobject(runtime);
JVM_END

void JVMCIRuntime::call_getCompiler(TRAPS) {
  THREAD_JVMCIENV(JavaThread::current());
  JVMCIObject jvmciRuntime = JVMCIRuntime::get_HotSpotJVMCIRuntime(JVMCI_CHECK);
  initialize(JVMCIENV);
  JVMCIENV->call_HotSpotJVMCIRuntime_getCompiler(jvmciRuntime, JVMCI_CHECK);
}

void JVMCINMethodData::initialize(
  int nmethod_mirror_index,
  const char* name,
  FailedSpeculation** failed_speculations)
{
  _failed_speculations = failed_speculations;
  _nmethod_mirror_index = nmethod_mirror_index;
  if (name != NULL) {
    _has_name = true;
    char* dest = (char*) this->name();
    strcpy(dest, name);
  } else {
    _has_name = false;
  }
}

void JVMCINMethodData::add_failed_speculation(nmethod* nm, jlong speculation) {
  jlong index = speculation >> JVMCINMethodData::SPECULATION_LENGTH_BITS;
  guarantee(index >= 0 && index <= max_jint, "Encoded JVMCI speculation index is not a positive Java int: " INTPTR_FORMAT, index);
  int length = speculation & JVMCINMethodData::SPECULATION_LENGTH_MASK;
  if (index + length > (uint) nm->speculations_size()) {
    fatal(INTPTR_FORMAT "[index: " JLONG_FORMAT ", length: %d out of bounds wrt encoded speculations of length %u", speculation, index, length, nm->speculations_size());
  }
  address data = nm->speculations_begin() + index;
  FailedSpeculation::add_failed_speculation(nm, _failed_speculations, data, length);
}

oop JVMCINMethodData::get_nmethod_mirror(nmethod* nm, bool phantom_ref) {
  if (_nmethod_mirror_index == -1) {
    return NULL;
  }
  if (phantom_ref) {
    return nm->oop_at_phantom(_nmethod_mirror_index);
  } else {
    return nm->oop_at(_nmethod_mirror_index);
  }
}

void JVMCINMethodData::set_nmethod_mirror(nmethod* nm, oop new_mirror) {
  assert(_nmethod_mirror_index != -1, "cannot set JVMCI mirror for nmethod");
  oop* addr = nm->oop_addr_at(_nmethod_mirror_index);
  assert(new_mirror != NULL, "use clear_nmethod_mirror to clear the mirror");
  assert(*addr == NULL, "cannot overwrite non-null mirror");

  *addr = new_mirror;

  // Since we've patched some oops in the nmethod,
  // (re)register it with the heap.
  MutexLocker ml(CodeCache_lock, Mutex::_no_safepoint_check_flag);
  Universe::heap()->register_nmethod(nm);
}

void JVMCINMethodData::clear_nmethod_mirror(nmethod* nm) {
  if (_nmethod_mirror_index != -1) {
    oop* addr = nm->oop_addr_at(_nmethod_mirror_index);
    *addr = NULL;
  }
}

void JVMCINMethodData::invalidate_nmethod_mirror(nmethod* nm) {
  oop nmethod_mirror = get_nmethod_mirror(nm, /* phantom_ref */ false);
  if (nmethod_mirror == NULL) {
    return;
  }

  // Update the values in the mirror if it still refers to nm.
  // We cannot use JVMCIObject to wrap the mirror as this is called
  // during GC, forbidding the creation of JNIHandles.
  JVMCIEnv* jvmciEnv = NULL;
  nmethod* current = (nmethod*) HotSpotJVMCI::InstalledCode::address(jvmciEnv, nmethod_mirror);
  if (nm == current) {
    if (!nm->is_alive()) {
      // Break the link from the mirror to nm such that
      // future invocations via the mirror will result in
      // an InvalidInstalledCodeException.
      HotSpotJVMCI::InstalledCode::set_address(jvmciEnv, nmethod_mirror, 0);
      HotSpotJVMCI::InstalledCode::set_entryPoint(jvmciEnv, nmethod_mirror, 0);
    } else if (nm->is_not_entrant()) {
      // Zero the entry point so any new invocation will fail but keep
      // the address link around that so that existing activations can
      // be deoptimized via the mirror (i.e. JVMCIEnv::invalidate_installed_code).
      HotSpotJVMCI::InstalledCode::set_entryPoint(jvmciEnv, nmethod_mirror, 0);
    }
  }

  if (_nmethod_mirror_index != -1 && nm->is_unloaded()) {
    // Drop the reference to the nmethod mirror object but don't clear the actual oop reference.  Otherwise
    // it would appear that the nmethod didn't need to be unloaded in the first place.
    _nmethod_mirror_index = -1;
  }
}

JVMCIRuntime::JVMCIRuntime(int id) {
  _init_state = uninitialized;
  _shared_library_javavm = NULL;
  _id = id;
  _metadata_handles = new MetadataHandles();
  JVMCI_event_1("created new JVMCI runtime %d (" PTR_FORMAT ")", id, p2i(this));
}

// Handles to objects in the Hotspot heap.
static OopStorage* object_handles() {
  return Universe::vm_global();
}

jobject JVMCIRuntime::make_global(const Handle& obj) {
  assert(!Universe::heap()->is_gc_active(), "can't extend the root set during GC");
  assert(oopDesc::is_oop(obj()), "not an oop");
  oop* ptr = object_handles()->allocate();
  jobject res = NULL;
  if (ptr != NULL) {
    assert(*ptr == NULL, "invariant");
    NativeAccess<>::oop_store(ptr, obj());
    res = reinterpret_cast<jobject>(ptr);
  } else {
    vm_exit_out_of_memory(sizeof(oop), OOM_MALLOC_ERROR,
                          "Cannot create JVMCI oop handle");
  }
  MutexLocker ml(JVMCI_lock);
  return res;
}

void JVMCIRuntime::destroy_global(jobject handle) {
  // Assert before nulling out, for better debugging.
  assert(is_global_handle(handle), "precondition");
  oop* oop_ptr = reinterpret_cast<oop*>(handle);
  NativeAccess<>::oop_store(oop_ptr, (oop)NULL);
  object_handles()->release(oop_ptr);
  MutexLocker ml(JVMCI_lock);
}

bool JVMCIRuntime::is_global_handle(jobject handle) {
  const oop* ptr = reinterpret_cast<oop*>(handle);
  return object_handles()->allocation_status(ptr) == OopStorage::ALLOCATED_ENTRY;
}

jmetadata JVMCIRuntime::allocate_handle(const methodHandle& handle) {
  MutexLocker ml(JVMCI_lock);
  return _metadata_handles->allocate_handle(handle);
}

jmetadata JVMCIRuntime::allocate_handle(const constantPoolHandle& handle) {
  MutexLocker ml(JVMCI_lock);
  return _metadata_handles->allocate_handle(handle);
}

void JVMCIRuntime::release_handle(jmetadata handle) {
  MutexLocker ml(JVMCI_lock);
  _metadata_handles->chain_free_list(handle);
}

// Function for redirecting shared library JavaVM output to tty
static void _log(const char* buf, size_t count) {
  tty->write((char*) buf, count);
}

// Function for redirecting shared library JavaVM fatal error data to a log file.
// The log file is opened on first call to this function.
static void _fatal_log(const char* buf, size_t count) {
  JVMCI::fatal_log(buf, count);
}

// Function for shared library JavaVM to flush tty
static void _flush_log() {
  tty->flush();
}

// Function for shared library JavaVM to exit HotSpot on a fatal error
static void _fatal() {
  intx current_thread_id = os::current_thread_id();
  fatal("thread " INTX_FORMAT ": Fatal error in JVMCI shared library", current_thread_id);
}

JNIEnv* JVMCIRuntime::init_shared_library_javavm() {
  JavaVM* javaVM = (JavaVM*) _shared_library_javavm;
  if (javaVM == NULL) {
    MutexLocker locker(JVMCI_lock);
    // Check again under JVMCI_lock
    javaVM = (JavaVM*) _shared_library_javavm;
    if (javaVM != NULL) {
      return NULL;
    }
    char* sl_path;
    void* sl_handle = JVMCI::get_shared_library(sl_path, true);

    jint (*JNI_CreateJavaVM)(JavaVM **pvm, void **penv, void *args);
    typedef jint (*JNI_CreateJavaVM_t)(JavaVM **pvm, void **penv, void *args);

    JNI_CreateJavaVM = CAST_TO_FN_PTR(JNI_CreateJavaVM_t, os::dll_lookup(sl_handle, "JNI_CreateJavaVM"));
    if (JNI_CreateJavaVM == NULL) {
      fatal("Unable to find JNI_CreateJavaVM in %s", sl_path);
    }

    ResourceMark rm;
    JavaVMInitArgs vm_args;
    vm_args.version = JNI_VERSION_1_2;
    vm_args.ignoreUnrecognized = JNI_TRUE;
    JavaVMOption options[5];
    jlong javaVM_id = 0;

    // Protocol: JVMCI shared library JavaVM should support a non-standard "_javavm_id"
    // option whose extraInfo info field is a pointer to which a unique id for the
    // JavaVM should be written.
    options[0].optionString = (char*) "_javavm_id";
    options[0].extraInfo = &javaVM_id;

    options[1].optionString = (char*) "_log";
    options[1].extraInfo = (void*) _log;
    options[2].optionString = (char*) "_flush_log";
    options[2].extraInfo = (void*) _flush_log;
    options[3].optionString = (char*) "_fatal";
    options[3].extraInfo = (void*) _fatal;
    options[4].optionString = (char*) "_fatal_log";
    options[4].extraInfo = (void*) _fatal_log;

    vm_args.version = JNI_VERSION_1_2;
    vm_args.options = options;
    vm_args.nOptions = sizeof(options) / sizeof(JavaVMOption);

    JNIEnv* env = NULL;
    int result = (*JNI_CreateJavaVM)(&javaVM, (void**) &env, &vm_args);
    if (result == JNI_OK) {
      guarantee(env != NULL, "missing env");
      _shared_library_javavm = javaVM;
      JVMCI_event_1("created JavaVM[%ld]@" PTR_FORMAT " for JVMCI runtime %d", javaVM_id, p2i(javaVM), _id);
      return env;
    } else {
      fatal("JNI_CreateJavaVM failed with return value %d", result);
    }
  }
  return NULL;
}

void JVMCIRuntime::init_JavaVM_info(jlongArray info, JVMCI_TRAPS) {
  if (info != NULL) {
    typeArrayOop info_oop = (typeArrayOop) JNIHandles::resolve(info);
    if (info_oop->length() < 4) {
      JVMCI_THROW_MSG(ArrayIndexOutOfBoundsException, err_msg("%d < 4", info_oop->length()));
    }
    JavaVM* javaVM = (JavaVM*) _shared_library_javavm;
    info_oop->long_at_put(0, (jlong) (address) javaVM);
    info_oop->long_at_put(1, (jlong) (address) javaVM->functions->reserved0);
    info_oop->long_at_put(2, (jlong) (address) javaVM->functions->reserved1);
    info_oop->long_at_put(3, (jlong) (address) javaVM->functions->reserved2);
  }
}

#define JAVAVM_CALL_BLOCK                                             \
  guarantee(thread != NULL && _shared_library_javavm != NULL, "npe"); \
  ThreadToNativeFromVM ttnfv(thread);                                 \
  JavaVM* javavm = (JavaVM*) _shared_library_javavm;

jint JVMCIRuntime::AttachCurrentThread(JavaThread* thread, void **penv, void *args) {
  JAVAVM_CALL_BLOCK
  return javavm->AttachCurrentThread(penv, args);
}

jint JVMCIRuntime::AttachCurrentThreadAsDaemon(JavaThread* thread, void **penv, void *args) {
  JAVAVM_CALL_BLOCK
  return javavm->AttachCurrentThreadAsDaemon(penv, args);
}

jint JVMCIRuntime::DetachCurrentThread(JavaThread* thread) {
  JAVAVM_CALL_BLOCK
  return javavm->DetachCurrentThread();
}

jint JVMCIRuntime::GetEnv(JavaThread* thread, void **penv, jint version) {
  JAVAVM_CALL_BLOCK
  return javavm->GetEnv(penv, version);
}
#undef JAVAVM_CALL_BLOCK                                             \

void JVMCIRuntime::initialize_HotSpotJVMCIRuntime(JVMCI_TRAPS) {
  if (is_HotSpotJVMCIRuntime_initialized()) {
    if (JVMCIENV->is_hotspot() && UseJVMCINativeLibrary) {
      JVMCI_THROW_MSG(InternalError, "JVMCI has already been enabled in the JVMCI shared library");
    }
  }

  initialize(JVMCIENV);

  // This should only be called in the context of the JVMCI class being initialized
  JVMCIObject result = JVMCIENV->call_HotSpotJVMCIRuntime_runtime(JVMCI_CHECK);
  result = JVMCIENV->make_global(result);

  OrderAccess::storestore();  // Ensure handle is fully constructed before publishing
  _HotSpotJVMCIRuntime_instance = result;

  JVMCI::_is_initialized = true;
}

void JVMCIRuntime::initialize(JVMCIEnv* JVMCIENV) {
  // Check first without JVMCI_lock
  if (_init_state == fully_initialized) {
    return;
  }

  MutexLocker locker(JVMCI_lock);
  // Check again under JVMCI_lock
  if (_init_state == fully_initialized) {
    return;
  }

  while (_init_state == being_initialized) {
    JVMCI_event_1("waiting for initialization of JVMCI runtime %d", _id);
    JVMCI_lock->wait();
    if (_init_state == fully_initialized) {
      JVMCI_event_1("done waiting for initialization of JVMCI runtime %d", _id);
      return;
    }
  }

  JVMCI_event_1("initializing JVMCI runtime %d", _id);
  _init_state = being_initialized;

  {
    MutexUnlocker unlock(JVMCI_lock);

    JavaThread* THREAD = JavaThread::current(); // For exception macros.
    HandleMark hm(THREAD);
    ResourceMark rm(THREAD);
    if (JVMCIENV->is_hotspot()) {
      HotSpotJVMCI::compute_offsets(CHECK_EXIT);
    } else {
      JNIAccessMark jni(JVMCIENV);

      JNIJVMCI::initialize_ids(jni.env());
      if (jni()->ExceptionCheck()) {
        jni()->ExceptionDescribe();
        fatal("JNI exception during init");
      }
    }

    if (!JVMCIENV->is_hotspot()) {
      JNIAccessMark jni(JVMCIENV, THREAD);
      JNIJVMCI::register_natives(jni.env());
    }
    create_jvmci_primitive_type(T_BOOLEAN, JVMCI_CHECK_EXIT_((void)0));
    create_jvmci_primitive_type(T_BYTE, JVMCI_CHECK_EXIT_((void)0));
    create_jvmci_primitive_type(T_CHAR, JVMCI_CHECK_EXIT_((void)0));
    create_jvmci_primitive_type(T_SHORT, JVMCI_CHECK_EXIT_((void)0));
    create_jvmci_primitive_type(T_INT, JVMCI_CHECK_EXIT_((void)0));
    create_jvmci_primitive_type(T_LONG, JVMCI_CHECK_EXIT_((void)0));
    create_jvmci_primitive_type(T_FLOAT, JVMCI_CHECK_EXIT_((void)0));
    create_jvmci_primitive_type(T_DOUBLE, JVMCI_CHECK_EXIT_((void)0));
    create_jvmci_primitive_type(T_VOID, JVMCI_CHECK_EXIT_((void)0));

    if (!JVMCIENV->is_hotspot()) {
      JVMCIENV->copy_saved_properties();
    }
  }

  _init_state = fully_initialized;
  JVMCI_event_1("initialized JVMCI runtime %d", _id);
  JVMCI_lock->notify_all();
}

JVMCIObject JVMCIRuntime::create_jvmci_primitive_type(BasicType type, JVMCI_TRAPS) {
  JavaThread* THREAD = JavaThread::current(); // For exception macros.
  // These primitive types are long lived and are created before the runtime is fully set up
  // so skip registering them for scanning.
  JVMCIObject mirror = JVMCIENV->get_object_constant(java_lang_Class::primitive_mirror(type), false, true);
  if (JVMCIENV->is_hotspot()) {
    JavaValue result(T_OBJECT);
    JavaCallArguments args;
    args.push_oop(Handle(THREAD, HotSpotJVMCI::resolve(mirror)));
    args.push_int(type2char(type));
    JavaCalls::call_static(&result, HotSpotJVMCI::HotSpotResolvedPrimitiveType::klass(), vmSymbols::fromMetaspace_name(), vmSymbols::primitive_fromMetaspace_signature(), &args, CHECK_(JVMCIObject()));

    return JVMCIENV->wrap(JNIHandles::make_local(result.get_oop()));
  } else {
    JNIAccessMark jni(JVMCIENV);
    jobject result = jni()->CallStaticObjectMethod(JNIJVMCI::HotSpotResolvedPrimitiveType::clazz(),
                                           JNIJVMCI::HotSpotResolvedPrimitiveType_fromMetaspace_method(),
                                           mirror.as_jobject(), type2char(type));
    if (jni()->ExceptionCheck()) {
      return JVMCIObject();
    }
    return JVMCIENV->wrap(result);
  }
}

void JVMCIRuntime::initialize_JVMCI(JVMCI_TRAPS) {
  if (!is_HotSpotJVMCIRuntime_initialized()) {
    initialize(JVMCI_CHECK);
    JVMCIENV->call_JVMCI_getRuntime(JVMCI_CHECK);
  }
}

JVMCIObject JVMCIRuntime::get_HotSpotJVMCIRuntime(JVMCI_TRAPS) {
  initialize(JVMCIENV);
  initialize_JVMCI(JVMCI_CHECK_(JVMCIObject()));
  return _HotSpotJVMCIRuntime_instance;
}

// private static void CompilerToVM.registerNatives()
JVM_ENTRY_NO_ENV(void, JVM_RegisterJVMCINatives(JNIEnv *env, jclass c2vmClass))
  JNI_JVMCIENV(thread, env);

  if (!EnableJVMCI) {
    JVMCI_THROW_MSG(InternalError, "JVMCI is not enabled");
  }

  JVMCIENV->runtime()->initialize(JVMCIENV);

  {
    ResourceMark rm(thread);
    HandleMark hm(thread);
    ThreadToNativeFromVM trans(thread);

    // Ensure _non_oop_bits is initialized
    Universe::non_oop_word();

    if (JNI_OK != env->RegisterNatives(c2vmClass, CompilerToVM::methods, CompilerToVM::methods_count())) {
      if (!env->ExceptionCheck()) {
        for (int i = 0; i < CompilerToVM::methods_count(); i++) {
          if (JNI_OK != env->RegisterNatives(c2vmClass, CompilerToVM::methods + i, 1)) {
            guarantee(false, "Error registering JNI method %s%s", CompilerToVM::methods[i].name, CompilerToVM::methods[i].signature);
            break;
          }
        }
      } else {
        env->ExceptionDescribe();
      }
      guarantee(false, "Failed registering CompilerToVM native methods");
    }
  }
JVM_END


void JVMCIRuntime::shutdown() {
  if (_HotSpotJVMCIRuntime_instance.is_non_null()) {
    JVMCI_event_1("shutting down HotSpotJVMCIRuntime for JVMCI runtime %d", _id);
    JVMCIEnv __stack_jvmci_env__(JavaThread::current(), _HotSpotJVMCIRuntime_instance.is_hotspot(), __FILE__, __LINE__);
    JVMCIEnv* JVMCIENV = &__stack_jvmci_env__;
    JVMCIENV->call_HotSpotJVMCIRuntime_shutdown(_HotSpotJVMCIRuntime_instance);
    JVMCI_event_1("shut down HotSpotJVMCIRuntime for JVMCI runtime %d", _id);
  }
}

void JVMCIRuntime::bootstrap_finished(TRAPS) {
  if (_HotSpotJVMCIRuntime_instance.is_non_null()) {
    THREAD_JVMCIENV(JavaThread::current());
    JVMCIENV->call_HotSpotJVMCIRuntime_bootstrapFinished(_HotSpotJVMCIRuntime_instance, JVMCIENV);
  }
}

void JVMCIRuntime::describe_pending_hotspot_exception(JavaThread* THREAD, bool clear) {
  if (HAS_PENDING_EXCEPTION) {
    Handle exception(THREAD, PENDING_EXCEPTION);
    const char* exception_file = THREAD->exception_file();
    int exception_line = THREAD->exception_line();
    CLEAR_PENDING_EXCEPTION;
    if (exception->is_a(vmClasses::ThreadDeath_klass())) {
      // Don't print anything if we are being killed.
    } else {
      java_lang_Throwable::print_stack_trace(exception, tty);

      // Clear and ignore any exceptions raised during printing
      CLEAR_PENDING_EXCEPTION;
    }
    if (!clear) {
      THREAD->set_pending_exception(exception(), exception_file, exception_line);
    }
  }
}


void JVMCIRuntime::fatal_exception(JVMCIEnv* JVMCIENV, const char* message) {
  JavaThread* THREAD = JavaThread::current(); // For exception macros.

  static volatile int report_error = 0;
  if (!report_error && Atomic::cmpxchg(&report_error, 0, 1) == 0) {
    // Only report an error once
    tty->print_raw_cr(message);
    if (JVMCIENV != NULL) {
      JVMCIENV->describe_pending_exception(true);
    } else {
      describe_pending_hotspot_exception(THREAD, true);
    }
  } else {
    // Allow error reporting thread to print the stack trace.
    THREAD->sleep(200);
  }
  fatal("Fatal exception in JVMCI: %s", message);
}

// ------------------------------------------------------------------
// Note: the logic of this method should mirror the logic of
// constantPoolOopDesc::verify_constant_pool_resolve.
bool JVMCIRuntime::check_klass_accessibility(Klass* accessing_klass, Klass* resolved_klass) {
  if (accessing_klass->is_objArray_klass()) {
    accessing_klass = ObjArrayKlass::cast(accessing_klass)->bottom_klass();
  }
  if (!accessing_klass->is_instance_klass()) {
    return true;
  }

  if (resolved_klass->is_objArray_klass()) {
    // Find the element klass, if this is an array.
    resolved_klass = ObjArrayKlass::cast(resolved_klass)->bottom_klass();
  }
  if (resolved_klass->is_instance_klass()) {
    Reflection::VerifyClassAccessResults result =
      Reflection::verify_class_access(accessing_klass, InstanceKlass::cast(resolved_klass), true);
    return result == Reflection::ACCESS_OK;
  }
  return true;
}

// ------------------------------------------------------------------
Klass* JVMCIRuntime::get_klass_by_name_impl(Klass*& accessing_klass,
                                          const constantPoolHandle& cpool,
                                          Symbol* sym,
                                          bool require_local) {
  JVMCI_EXCEPTION_CONTEXT;

  // Now we need to check the SystemDictionary
  if (sym->char_at(0) == JVM_SIGNATURE_CLASS &&
      sym->char_at(sym->utf8_length()-1) == JVM_SIGNATURE_ENDCLASS) {
    // This is a name from a signature.  Strip off the trimmings.
    // Call recursive to keep scope of strippedsym.
    TempNewSymbol strippedsym = SymbolTable::new_symbol(sym->as_utf8()+1,
                                                        sym->utf8_length()-2);
    return get_klass_by_name_impl(accessing_klass, cpool, strippedsym, require_local);
  }

  Handle loader;
  Handle domain;
  if (accessing_klass != NULL) {
    loader = Handle(THREAD, accessing_klass->class_loader());
    domain = Handle(THREAD, accessing_klass->protection_domain());
  }

  Klass* found_klass;
  {
    ttyUnlocker ttyul;  // release tty lock to avoid ordering problems
    MutexLocker ml(THREAD, Compile_lock);
    if (!require_local) {
      found_klass = SystemDictionary::find_constrained_instance_or_array_klass(THREAD, sym, loader);
    } else {
      found_klass = SystemDictionary::find_instance_or_array_klass(sym, loader, domain);
    }
  }

  // If we fail to find an array klass, look again for its element type.
  // The element type may be available either locally or via constraints.
  // In either case, if we can find the element type in the system dictionary,
  // we must build an array type around it.  The CI requires array klasses
  // to be loaded if their element klasses are loaded, except when memory
  // is exhausted.
  if (sym->char_at(0) == JVM_SIGNATURE_ARRAY &&
      (sym->char_at(1) == JVM_SIGNATURE_ARRAY || sym->char_at(1) == JVM_SIGNATURE_CLASS)) {
    // We have an unloaded array.
    // Build it on the fly if the element class exists.
    TempNewSymbol elem_sym = SymbolTable::new_symbol(sym->as_utf8()+1,
                                                     sym->utf8_length()-1);

    // Get element Klass recursively.
    Klass* elem_klass =
      get_klass_by_name_impl(accessing_klass,
                             cpool,
                             elem_sym,
                             require_local);
    if (elem_klass != NULL) {
      // Now make an array for it
      return elem_klass->array_klass(THREAD);
    }
  }

  if (found_klass == NULL && !cpool.is_null() && cpool->has_preresolution()) {
    // Look inside the constant pool for pre-resolved class entries.
    for (int i = cpool->length() - 1; i >= 1; i--) {
      if (cpool->tag_at(i).is_klass()) {
        Klass*  kls = cpool->resolved_klass_at(i);
        if (kls->name() == sym) {
          return kls;
        }
      }
    }
  }

  return found_klass;
}

// ------------------------------------------------------------------
Klass* JVMCIRuntime::get_klass_by_name(Klass* accessing_klass,
                                  Symbol* klass_name,
                                  bool require_local) {
  ResourceMark rm;
  constantPoolHandle cpool;
  return get_klass_by_name_impl(accessing_klass,
                                                 cpool,
                                                 klass_name,
                                                 require_local);
}

// ------------------------------------------------------------------
// Implementation of get_klass_by_index.
Klass* JVMCIRuntime::get_klass_by_index_impl(const constantPoolHandle& cpool,
                                        int index,
                                        bool& is_accessible,
                                        Klass* accessor) {
  JVMCI_EXCEPTION_CONTEXT;
  Klass* klass = ConstantPool::klass_at_if_loaded(cpool, index);
  Symbol* klass_name = NULL;
  if (klass == NULL) {
    klass_name = cpool->klass_name_at(index);
  }

  if (klass == NULL) {
    // Not found in constant pool.  Use the name to do the lookup.
    Klass* k = get_klass_by_name_impl(accessor,
                                        cpool,
                                        klass_name,
                                        false);
    // Calculate accessibility the hard way.
    if (k == NULL) {
      is_accessible = false;
    } else if (k->class_loader() != accessor->class_loader() &&
               get_klass_by_name_impl(accessor, cpool, k->name(), true) == NULL) {
      // Loaded only remotely.  Not linked yet.
      is_accessible = false;
    } else {
      // Linked locally, and we must also check public/private, etc.
      is_accessible = check_klass_accessibility(accessor, k);
    }
    if (!is_accessible) {
      return NULL;
    }
    return k;
  }

  // It is known to be accessible, since it was found in the constant pool.
  is_accessible = true;
  return klass;
}

// ------------------------------------------------------------------
// Get a klass from the constant pool.
Klass* JVMCIRuntime::get_klass_by_index(const constantPoolHandle& cpool,
                                   int index,
                                   bool& is_accessible,
                                   Klass* accessor) {
  ResourceMark rm;
  Klass* result = get_klass_by_index_impl(cpool, index, is_accessible, accessor);
  return result;
}

// ------------------------------------------------------------------
// Implementation of get_field_by_index.
//
// Implementation note: the results of field lookups are cached
// in the accessor klass.
void JVMCIRuntime::get_field_by_index_impl(InstanceKlass* klass, fieldDescriptor& field_desc,
                                        int index) {
  JVMCI_EXCEPTION_CONTEXT;

  assert(klass->is_linked(), "must be linked before using its constant-pool");

  constantPoolHandle cpool(thread, klass->constants());

  // Get the field's name, signature, and type.
  Symbol* name  = cpool->name_ref_at(index);

  int nt_index = cpool->name_and_type_ref_index_at(index);
  int sig_index = cpool->signature_ref_index_at(nt_index);
  Symbol* signature = cpool->symbol_at(sig_index);

  // Get the field's declared holder.
  int holder_index = cpool->klass_ref_index_at(index);
  bool holder_is_accessible;
  Klass* declared_holder = get_klass_by_index(cpool, holder_index,
                                               holder_is_accessible,
                                               klass);

  // The declared holder of this field may not have been loaded.
  // Bail out with partial field information.
  if (!holder_is_accessible) {
    return;
  }


  // Perform the field lookup.
  Klass*  canonical_holder =
    InstanceKlass::cast(declared_holder)->find_field(name, signature, &field_desc);
  if (canonical_holder == NULL) {
    return;
  }

  assert(canonical_holder == field_desc.field_holder(), "just checking");
}

// ------------------------------------------------------------------
// Get a field by index from a klass's constant pool.
void JVMCIRuntime::get_field_by_index(InstanceKlass* accessor, fieldDescriptor& fd, int index) {
  ResourceMark rm;
  return get_field_by_index_impl(accessor, fd, index);
}

// ------------------------------------------------------------------
// Perform an appropriate method lookup based on accessor, holder,
// name, signature, and bytecode.
Method* JVMCIRuntime::lookup_method(InstanceKlass* accessor,
                                    Klass*        holder,
                                    Symbol*       name,
                                    Symbol*       sig,
                                    Bytecodes::Code bc,
                                    constantTag   tag) {
  // Accessibility checks are performed in JVMCIEnv::get_method_by_index_impl().
  assert(check_klass_accessibility(accessor, holder), "holder not accessible");

  LinkInfo link_info(holder, name, sig, accessor,
                     LinkInfo::AccessCheck::required,
                     LinkInfo::LoaderConstraintCheck::required,
                     tag);
  switch (bc) {
    case Bytecodes::_invokestatic:
      return LinkResolver::resolve_static_call_or_null(link_info);
    case Bytecodes::_invokespecial:
      return LinkResolver::resolve_special_call_or_null(link_info);
    case Bytecodes::_invokeinterface:
      return LinkResolver::linktime_resolve_interface_method_or_null(link_info);
    case Bytecodes::_invokevirtual:
      return LinkResolver::linktime_resolve_virtual_method_or_null(link_info);
    default:
      fatal("Unhandled bytecode: %s", Bytecodes::name(bc));
      return NULL; // silence compiler warnings
  }
}


// ------------------------------------------------------------------
Method* JVMCIRuntime::get_method_by_index_impl(const constantPoolHandle& cpool,
                                               int index, Bytecodes::Code bc,
                                               InstanceKlass* accessor) {
  if (bc == Bytecodes::_invokedynamic) {
    ConstantPoolCacheEntry* cpce = cpool->invokedynamic_cp_cache_entry_at(index);
    bool is_resolved = !cpce->is_f1_null();
    if (is_resolved) {
      // Get the invoker Method* from the constant pool.
      // (The appendix argument, if any, will be noted in the method's signature.)
      Method* adapter = cpce->f1_as_method();
      return adapter;
    }

    return NULL;
  }

  int holder_index = cpool->klass_ref_index_at(index);
  bool holder_is_accessible;
  Klass* holder = get_klass_by_index_impl(cpool, holder_index, holder_is_accessible, accessor);

  // Get the method's name and signature.
  Symbol* name_sym = cpool->name_ref_at(index);
  Symbol* sig_sym  = cpool->signature_ref_at(index);

  if (cpool->has_preresolution()
      || ((holder == vmClasses::MethodHandle_klass() || holder == vmClasses::VarHandle_klass()) &&
          MethodHandles::is_signature_polymorphic_name(holder, name_sym))) {
    // Short-circuit lookups for JSR 292-related call sites.
    // That is, do not rely only on name-based lookups, because they may fail
    // if the names are not resolvable in the boot class loader (7056328).
    switch (bc) {
    case Bytecodes::_invokevirtual:
    case Bytecodes::_invokeinterface:
    case Bytecodes::_invokespecial:
    case Bytecodes::_invokestatic:
      {
        Method* m = ConstantPool::method_at_if_loaded(cpool, index);
        if (m != NULL) {
          return m;
        }
      }
      break;
    default:
      break;
    }
  }

  if (holder_is_accessible) { // Our declared holder is loaded.
    constantTag tag = cpool->tag_ref_at(index);
    Method* m = lookup_method(accessor, holder, name_sym, sig_sym, bc, tag);
    if (m != NULL) {
      // We found the method.
      return m;
    }
  }

  // Either the declared holder was not loaded, or the method could
  // not be found.

  return NULL;
}

// ------------------------------------------------------------------
InstanceKlass* JVMCIRuntime::get_instance_klass_for_declared_method_holder(Klass* method_holder) {
  // For the case of <array>.clone(), the method holder can be an ArrayKlass*
  // instead of an InstanceKlass*.  For that case simply pretend that the
  // declared holder is Object.clone since that's where the call will bottom out.
  if (method_holder->is_instance_klass()) {
    return InstanceKlass::cast(method_holder);
  } else if (method_holder->is_array_klass()) {
    return vmClasses::Object_klass();
  } else {
    ShouldNotReachHere();
  }
  return NULL;
}


// ------------------------------------------------------------------
Method* JVMCIRuntime::get_method_by_index(const constantPoolHandle& cpool,
                                     int index, Bytecodes::Code bc,
                                     InstanceKlass* accessor) {
  ResourceMark rm;
  return get_method_by_index_impl(cpool, index, bc, accessor);
}

// ------------------------------------------------------------------
// Check for changes to the system dictionary during compilation
// class loads, evolution, breakpoints
JVMCI::CodeInstallResult JVMCIRuntime::validate_compile_task_dependencies(Dependencies* dependencies, JVMCICompileState* compile_state, char** failure_detail) {
  // If JVMTI capabilities were enabled during compile, the compilation is invalidated.
  if (compile_state != NULL && compile_state->jvmti_state_changed()) {
    *failure_detail = (char*) "Jvmti state change during compilation invalidated dependencies";
    return JVMCI::dependencies_failed;
  }

  CompileTask* task = compile_state == NULL ? NULL : compile_state->task();
  Dependencies::DepType result = dependencies->validate_dependencies(task, failure_detail);
  if (result == Dependencies::end_marker) {
    return JVMCI::ok;
  }

  return JVMCI::dependencies_failed;
}

void JVMCIRuntime::compile_method(JVMCIEnv* JVMCIENV, JVMCICompiler* compiler, const methodHandle& method, int entry_bci) {
  JVMCI_EXCEPTION_CONTEXT

  JVMCICompileState* compile_state = JVMCIENV->compile_state();

  bool is_osr = entry_bci != InvocationEntryBci;
  if (compiler->is_bootstrapping() && is_osr) {
    // no OSR compilations during bootstrap - the compiler is just too slow at this point,
    // and we know that there are no endless loops
    compile_state->set_failure(true, "No OSR during bootstrap");
    return;
  }
  if (JVMCI::in_shutdown()) {
    compile_state->set_failure(false, "Avoiding compilation during shutdown");
    return;
  }

  HandleMark hm(thread);
  JVMCIObject receiver = get_HotSpotJVMCIRuntime(JVMCIENV);
  if (JVMCIENV->has_pending_exception()) {
    fatal_exception(JVMCIENV, "Exception during HotSpotJVMCIRuntime initialization");
  }
  JVMCIObject jvmci_method = JVMCIENV->get_jvmci_method(method, JVMCIENV);
  if (JVMCIENV->has_pending_exception()) {
    JVMCIENV->describe_pending_exception(true);
    compile_state->set_failure(false, "exception getting JVMCI wrapper method");
    return;
  }

  JVMCIObject result_object = JVMCIENV->call_HotSpotJVMCIRuntime_compileMethod(receiver, jvmci_method, entry_bci,
                                                                     (jlong) compile_state, compile_state->task()->compile_id());
  if (!JVMCIENV->has_pending_exception()) {
    if (result_object.is_non_null()) {
      JVMCIObject failure_message = JVMCIENV->get_HotSpotCompilationRequestResult_failureMessage(result_object);
      if (failure_message.is_non_null()) {
        // Copy failure reason into resource memory first ...
        const char* failure_reason = JVMCIENV->as_utf8_string(failure_message);
        // ... and then into the C heap.
        failure_reason = os::strdup(failure_reason, mtJVMCI);
        bool retryable = JVMCIENV->get_HotSpotCompilationRequestResult_retry(result_object) != 0;
        compile_state->set_failure(retryable, failure_reason, true);
      } else {
        if (compile_state->task()->code() == NULL) {
          compile_state->set_failure(true, "no nmethod produced");
        } else {
          compile_state->task()->set_num_inlined_bytecodes(JVMCIENV->get_HotSpotCompilationRequestResult_inlinedBytecodes(result_object));
          compiler->inc_methods_compiled();
        }
      }
    } else {
      assert(false, "JVMCICompiler.compileMethod should always return non-null");
    }
  } else {
    // An uncaught exception here implies failure during compiler initialization.
    // The only sensible thing to do here is to exit the VM.
    fatal_exception(JVMCIENV, "Exception during JVMCI compiler initialization");
  }
  if (compiler->is_bootstrapping()) {
    compiler->set_bootstrap_compilation_request_handled();
  }
}

bool JVMCIRuntime::is_gc_supported(JVMCIEnv* JVMCIENV, CollectedHeap::Name name) {
  JVMCI_EXCEPTION_CONTEXT

  JVMCIObject receiver = get_HotSpotJVMCIRuntime(JVMCIENV);
  if (JVMCIENV->has_pending_exception()) {
    fatal_exception(JVMCIENV, "Exception during HotSpotJVMCIRuntime initialization");
  }
  return JVMCIENV->call_HotSpotJVMCIRuntime_isGCSupported(receiver, (int) name);
}

// ------------------------------------------------------------------
JVMCI::CodeInstallResult JVMCIRuntime::register_method(JVMCIEnv* JVMCIENV,
                                const methodHandle& method,
                                nmethodLocker& code_handle,
                                int entry_bci,
                                CodeOffsets* offsets,
                                int orig_pc_offset,
                                CodeBuffer* code_buffer,
                                int frame_words,
                                OopMapSet* oop_map_set,
                                ExceptionHandlerTable* handler_table,
                                ImplicitExceptionTable* implicit_exception_table,
                                AbstractCompiler* compiler,
                                DebugInformationRecorder* debug_info,
                                Dependencies* dependencies,
                                int compile_id,
                                bool has_unsafe_access,
                                bool has_wide_vector,
                                JVMCIObject compiled_code,
                                JVMCIObject nmethod_mirror,
                                FailedSpeculation** failed_speculations,
                                char* speculations,
                                int speculations_len) {
  JVMCI_EXCEPTION_CONTEXT;
  nmethod* nm = NULL;
  int comp_level = CompLevel_full_optimization;
  char* failure_detail = NULL;

  bool install_default = JVMCIENV->get_HotSpotNmethod_isDefault(nmethod_mirror) != 0;
  assert(JVMCIENV->isa_HotSpotNmethod(nmethod_mirror), "must be");
  JVMCIObject name = JVMCIENV->get_InstalledCode_name(nmethod_mirror);
  const char* nmethod_mirror_name = name.is_null() ? NULL : JVMCIENV->as_utf8_string(name);
  int nmethod_mirror_index;
  if (!install_default) {
    // Reserve or initialize mirror slot in the oops table.
    OopRecorder* oop_recorder = debug_info->oop_recorder();
    nmethod_mirror_index = oop_recorder->allocate_oop_index(nmethod_mirror.is_hotspot() ? nmethod_mirror.as_jobject() : NULL);
  } else {
    // A default HotSpotNmethod mirror is never tracked by the nmethod
    nmethod_mirror_index = -1;
  }

  JVMCI::CodeInstallResult result(JVMCI::ok);

  // We require method counters to store some method state (max compilation levels) required by the compilation policy.
  if (method->get_method_counters(THREAD) == NULL) {
    result = JVMCI::cache_full;
    failure_detail = (char*) "can't create method counters";
  }

  if (result == JVMCI::ok) {
    // To prevent compile queue updates.
    MutexLocker locker(THREAD, MethodCompileQueue_lock);

    // Prevent SystemDictionary::add_to_hierarchy from running
    // and invalidating our dependencies until we install this method.
    MutexLocker ml(Compile_lock);

    // Encode the dependencies now, so we can check them right away.
    dependencies->encode_content_bytes();

    // Record the dependencies for the current compile in the log
    if (LogCompilation) {
      for (Dependencies::DepStream deps(dependencies); deps.next(); ) {
        deps.log_dependency();
      }
    }

    // Check for {class loads, evolution, breakpoints} during compilation
    result = validate_compile_task_dependencies(dependencies, JVMCIENV->compile_state(), &failure_detail);
    if (result != JVMCI::ok) {
      // While not a true deoptimization, it is a preemptive decompile.
      MethodData* mdp = method()->method_data();
      if (mdp != NULL) {
        mdp->inc_decompile_count();
#ifdef ASSERT
        if (mdp->decompile_count() > (uint)PerMethodRecompilationCutoff) {
          ResourceMark m;
          tty->print_cr("WARN: endless recompilation of %s. Method was set to not compilable.", method()->name_and_sig_as_C_string());
        }
#endif
      }

      // All buffers in the CodeBuffer are allocated in the CodeCache.
      // If the code buffer is created on each compile attempt
      // as in C2, then it must be freed.
      //code_buffer->free_blob();
    } else {
      nm =  nmethod::new_nmethod(method,
                                 compile_id,
                                 entry_bci,
                                 offsets,
                                 orig_pc_offset,
                                 debug_info, dependencies, code_buffer,
                                 frame_words, oop_map_set,
                                 handler_table, implicit_exception_table,
                                 compiler, comp_level, GrowableArrayView<RuntimeStub*>::EMPTY,
                                 speculations, speculations_len,
                                 nmethod_mirror_index, nmethod_mirror_name, failed_speculations);


      // Free codeBlobs
      if (nm == NULL) {
        // The CodeCache is full.  Print out warning and disable compilation.
        {
          MutexUnlocker ml(Compile_lock);
          MutexUnlocker locker(MethodCompileQueue_lock);
          CompileBroker::handle_full_code_cache(CodeCache::get_code_blob_type(comp_level));
        }
        result = JVMCI::cache_full;
      } else {
        nm->set_has_unsafe_access(has_unsafe_access);
        nm->set_has_wide_vectors(has_wide_vector);

        // Record successful registration.
        // (Put nm into the task handle *before* publishing to the Java heap.)
        if (JVMCIENV->compile_state() != NULL) {
          JVMCIENV->compile_state()->task()->set_code(nm);
        }

        JVMCINMethodData* data = nm->jvmci_nmethod_data();
        assert(data != NULL, "must be");
        if (install_default) {
          assert(!nmethod_mirror.is_hotspot() || data->get_nmethod_mirror(nm, /* phantom_ref */ false) == NULL, "must be");
          if (entry_bci == InvocationEntryBci) {
            // If there is an old version we're done with it
            CompiledMethod* old = method->code();
            if (TraceMethodReplacement && old != NULL) {
              ResourceMark rm;
              char *method_name = method->name_and_sig_as_C_string();
              tty->print_cr("Replacing method %s", method_name);
            }
            if (old != NULL ) {
              old->make_not_entrant();
            }

            LogTarget(Info, nmethod, install) lt;
            if (lt.is_enabled()) {
              ResourceMark rm;
              char *method_name = method->name_and_sig_as_C_string();
              lt.print("Installing method (%d) %s [entry point: %p]",
                        comp_level, method_name, nm->entry_point());
            }
            // Allow the code to be executed
            MutexLocker ml(CompiledMethod_lock, Mutex::_no_safepoint_check_flag);
            if (nm->make_in_use()) {
              method->set_code(method, nm);
            } else {
              result = JVMCI::nmethod_reclaimed;
            }
          } else {
            LogTarget(Info, nmethod, install) lt;
            if (lt.is_enabled()) {
              ResourceMark rm;
              char *method_name = method->name_and_sig_as_C_string();
              lt.print("Installing osr method (%d) %s @ %d",
                        comp_level, method_name, entry_bci);
            }
            MutexLocker ml(CompiledMethod_lock, Mutex::_no_safepoint_check_flag);
            if (nm->make_in_use()) {
              InstanceKlass::cast(method->method_holder())->add_osr_nmethod(nm);
            } else {
              result = JVMCI::nmethod_reclaimed;
            }
          }
        } else {
          assert(!nmethod_mirror.is_hotspot() || data->get_nmethod_mirror(nm, /* phantom_ref */ false) == HotSpotJVMCI::resolve(nmethod_mirror), "must be");
          MutexLocker ml(CompiledMethod_lock, Mutex::_no_safepoint_check_flag);
          if (!nm->make_in_use()) {
            result = JVMCI::nmethod_reclaimed;
          }
        }
      }
    }
    if (result == JVMCI::ok) {
      code_handle.set_code(nm);
    }
  }

  // String creation must be done outside lock
  if (failure_detail != NULL) {
    // A failure to allocate the string is silently ignored.
    JVMCIObject message = JVMCIENV->create_string(failure_detail, JVMCIENV);
    JVMCIENV->set_HotSpotCompiledNmethod_installationFailureMessage(compiled_code, message);
  }

  if (result == JVMCI::ok) {
    // JVMTI -- compiled method notification (must be done outside lock)
    nm->post_compiled_method_load_event();
  }

  return result;
}
