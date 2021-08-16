/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "cds/heapShared.hpp"
#include "classfile/resolutionErrors.hpp"
#include "classfile/systemDictionary.hpp"
#include "classfile/vmClasses.hpp"
#include "interpreter/bytecodeStream.hpp"
#include "interpreter/bytecodes.hpp"
#include "interpreter/interpreter.hpp"
#include "interpreter/linkResolver.hpp"
#include "interpreter/rewriter.hpp"
#include "logging/log.hpp"
#include "logging/logStream.hpp"
#include "memory/metadataFactory.hpp"
#include "memory/metaspaceClosure.hpp"
#include "memory/resourceArea.hpp"
#include "oops/access.inline.hpp"
#include "oops/compressedOops.hpp"
#include "oops/constantPool.inline.hpp"
#include "oops/cpCache.inline.hpp"
#include "oops/objArrayOop.inline.hpp"
#include "oops/oop.inline.hpp"
#include "prims/methodHandles.hpp"
#include "runtime/arguments.hpp"
#include "runtime/atomic.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/vm_version.hpp"
#include "utilities/macros.hpp"

// Implementation of ConstantPoolCacheEntry

void ConstantPoolCacheEntry::initialize_entry(int index) {
  assert(0 < index && index < 0x10000, "sanity check");
  _indices = index;
  _f1 = NULL;
  _f2 = _flags = 0;
  assert(constant_pool_index() == index, "");
}

void ConstantPoolCacheEntry::verify_just_initialized(bool f2_used) {
  assert((_indices & (~cp_index_mask)) == 0, "sanity");
  assert(_f1 == NULL, "sanity");
  assert(_flags == 0, "sanity");
  if (!f2_used) {
    assert(_f2 == 0, "sanity");
  }
}

void ConstantPoolCacheEntry::reinitialize(bool f2_used) {
  _indices &= cp_index_mask;
  _f1 = NULL;
  _flags = 0;
  if (!f2_used) {
    _f2 = 0;
  }
}

int ConstantPoolCacheEntry::make_flags(TosState state,
                                       int option_bits,
                                       int field_index_or_method_params) {
  assert(state < number_of_states, "Invalid state in make_flags");
  int f = ((int)state << tos_state_shift) | option_bits | field_index_or_method_params;
  // Preserve existing flag bit values
  // The low bits are a field offset, or else the method parameter size.
#ifdef ASSERT
  TosState old_state = flag_state();
  assert(old_state == (TosState)0 || old_state == state,
         "inconsistent cpCache flags state");
#endif
  return (_flags | f) ;
}

void ConstantPoolCacheEntry::set_bytecode_1(Bytecodes::Code code) {
#ifdef ASSERT
  // Read once.
  volatile Bytecodes::Code c = bytecode_1();
  assert(c == 0 || c == code || code == 0, "update must be consistent");
#endif
  // Need to flush pending stores here before bytecode is written.
  Atomic::release_store(&_indices, _indices | ((u_char)code << bytecode_1_shift));
}

void ConstantPoolCacheEntry::set_bytecode_2(Bytecodes::Code code) {
#ifdef ASSERT
  // Read once.
  volatile Bytecodes::Code c = bytecode_2();
  assert(c == 0 || c == code || code == 0, "update must be consistent");
#endif
  // Need to flush pending stores here before bytecode is written.
  Atomic::release_store(&_indices, _indices | ((u_char)code << bytecode_2_shift));
}

// Sets f1, ordering with previous writes.
void ConstantPoolCacheEntry::release_set_f1(Metadata* f1) {
  assert(f1 != NULL, "");
  Atomic::release_store(&_f1, f1);
}

void ConstantPoolCacheEntry::set_indy_resolution_failed() {
  Atomic::release_store(&_flags, _flags | (1 << indy_resolution_failed_shift));
}

// Note that concurrent update of both bytecodes can leave one of them
// reset to zero.  This is harmless; the interpreter will simply re-resolve
// the damaged entry.  More seriously, the memory synchronization is needed
// to flush other fields (f1, f2) completely to memory before the bytecodes
// are updated, lest other processors see a non-zero bytecode but zero f1/f2.
void ConstantPoolCacheEntry::set_field(Bytecodes::Code get_code,
                                       Bytecodes::Code put_code,
                                       Klass* field_holder,
                                       int field_index,
                                       int field_offset,
                                       TosState field_type,
                                       bool is_final,
                                       bool is_volatile) {
  set_f1(field_holder);
  set_f2(field_offset);
  assert((field_index & field_index_mask) == field_index,
         "field index does not fit in low flag bits");
  set_field_flags(field_type,
                  ((is_volatile ? 1 : 0) << is_volatile_shift) |
                  ((is_final    ? 1 : 0) << is_final_shift),
                  field_index);
  set_bytecode_1(get_code);
  set_bytecode_2(put_code);
  NOT_PRODUCT(verify(tty));
}

void ConstantPoolCacheEntry::set_parameter_size(int value) {
  // This routine is called only in corner cases where the CPCE is not yet initialized.
  // See AbstractInterpreter::deopt_continue_after_entry.
  assert(_flags == 0 || parameter_size() == 0 || parameter_size() == value,
         "size must not change: parameter_size=%d, value=%d", parameter_size(), value);
  // Setting the parameter size by itself is only safe if the
  // current value of _flags is 0, otherwise another thread may have
  // updated it and we don't want to overwrite that value.  Don't
  // bother trying to update it once it's nonzero but always make
  // sure that the final parameter size agrees with what was passed.
  if (_flags == 0) {
    intx newflags = (value & parameter_size_mask);
    Atomic::cmpxchg(&_flags, (intx)0, newflags);
  }
  guarantee(parameter_size() == value,
            "size must not change: parameter_size=%d, value=%d", parameter_size(), value);
}

void ConstantPoolCacheEntry::set_direct_or_vtable_call(Bytecodes::Code invoke_code,
                                                       const methodHandle& method,
                                                       int vtable_index,
                                                       bool sender_is_interface) {
  bool is_vtable_call = (vtable_index >= 0);  // FIXME: split this method on this boolean
  assert(method->interpreter_entry() != NULL, "should have been set at this point");
  assert(!method->is_obsolete(),  "attempt to write obsolete method to cpCache");

  int byte_no = -1;
  bool change_to_virtual = false;
  InstanceKlass* holder = NULL;  // have to declare this outside the switch
  switch (invoke_code) {
    case Bytecodes::_invokeinterface:
      holder = method->method_holder();
      // check for private interface method invocations
      if (vtable_index == Method::nonvirtual_vtable_index && holder->is_interface() ) {
        assert(method->is_private(), "unexpected non-private method");
        assert(method->can_be_statically_bound(), "unexpected non-statically-bound method");
        // set_f2_as_vfinal_method checks if is_vfinal flag is true.
        set_method_flags(as_TosState(method->result_type()),
                         (                             1      << is_vfinal_shift) |
                         ((method->is_final_method() ? 1 : 0) << is_final_shift),
                         method()->size_of_parameters());
        set_f2_as_vfinal_method(method());
        byte_no = 2;
        set_f1(holder); // interface klass*
        break;
      }
      else {
        // We get here from InterpreterRuntime::resolve_invoke when an invokeinterface
        // instruction links to a non-interface method (in Object). This can happen when
        // an interface redeclares an Object method (like CharSequence declaring toString())
        // or when invokeinterface is used explicitly.
        // In that case, the method has no itable index and must be invoked as a virtual.
        // Set a flag to keep track of this corner case.
        assert(holder->is_interface() || holder == vmClasses::Object_klass(), "unexpected holder class");
        assert(method->is_public(), "Calling non-public method in Object with invokeinterface");
        change_to_virtual = true;

        // ...and fall through as if we were handling invokevirtual:
      }
    case Bytecodes::_invokevirtual:
      {
        if (!is_vtable_call) {
          assert(method->can_be_statically_bound(), "");
          // set_f2_as_vfinal_method checks if is_vfinal flag is true.
          set_method_flags(as_TosState(method->result_type()),
                           (                             1      << is_vfinal_shift) |
                           ((method->is_final_method() ? 1 : 0) << is_final_shift)  |
                           ((change_to_virtual         ? 1 : 0) << is_forced_virtual_shift),
                           method()->size_of_parameters());
          set_f2_as_vfinal_method(method());
        } else {
          assert(!method->can_be_statically_bound(), "");
          assert(vtable_index >= 0, "valid index");
          assert(!method->is_final_method(), "sanity");
          set_method_flags(as_TosState(method->result_type()),
                           ((change_to_virtual ? 1 : 0) << is_forced_virtual_shift),
                           method()->size_of_parameters());
          set_f2(vtable_index);
        }
        byte_no = 2;
        break;
      }

    case Bytecodes::_invokespecial:
    case Bytecodes::_invokestatic:
      assert(!is_vtable_call, "");
      // Note:  Read and preserve the value of the is_vfinal flag on any
      // invokevirtual bytecode shared with this constant pool cache entry.
      // It is cheap and safe to consult is_vfinal() at all times.
      // Once is_vfinal is set, it must stay that way, lest we get a dangling oop.
      set_method_flags(as_TosState(method->result_type()),
                       ((is_vfinal()               ? 1 : 0) << is_vfinal_shift) |
                       ((method->is_final_method() ? 1 : 0) << is_final_shift),
                       method()->size_of_parameters());
      set_f1(method());
      byte_no = 1;
      break;
    default:
      ShouldNotReachHere();
      break;
  }

  // Note:  byte_no also appears in TemplateTable::resolve.
  if (byte_no == 1) {
    assert(invoke_code != Bytecodes::_invokevirtual &&
           invoke_code != Bytecodes::_invokeinterface, "");
    bool do_resolve = true;
    // Don't mark invokespecial to method as resolved if sender is an interface.  The receiver
    // has to be checked that it is a subclass of the current class every time this bytecode
    // is executed.
    if (invoke_code == Bytecodes::_invokespecial && sender_is_interface &&
        method->name() != vmSymbols::object_initializer_name()) {
      do_resolve = false;
    }
    if (invoke_code == Bytecodes::_invokestatic) {
      assert(method->method_holder()->is_initialized() ||
             method->method_holder()->is_reentrant_initialization(Thread::current()),
             "invalid class initialization state for invoke_static");

      if (!VM_Version::supports_fast_class_init_checks() && method->needs_clinit_barrier()) {
        // Don't mark invokestatic to method as resolved if the holder class has not yet completed
        // initialization. An invokestatic must only proceed if the class is initialized, but if
        // we resolve it before then that class initialization check is skipped.
        //
        // When fast class initialization checks are supported (VM_Version::supports_fast_class_init_checks() == true),
        // template interpreter supports fast class initialization check for
        // invokestatic which doesn't require call site re-resolution to
        // enforce class initialization barrier.
        do_resolve = false;
      }
    }
    if (do_resolve) {
      set_bytecode_1(invoke_code);
    }
  } else if (byte_no == 2)  {
    if (change_to_virtual) {
      assert(invoke_code == Bytecodes::_invokeinterface, "");
      // NOTE: THIS IS A HACK - BE VERY CAREFUL!!!
      //
      // Workaround for the case where we encounter an invokeinterface, but we
      // should really have an _invokevirtual since the resolved method is a
      // virtual method in java.lang.Object. This is a corner case in the spec
      // but is presumably legal. javac does not generate this code.
      //
      // We do not set bytecode_1() to _invokeinterface, because that is the
      // bytecode # used by the interpreter to see if it is resolved.  In this
      // case, the method gets reresolved with caller for each interface call
      // because the actual selected method may not be public.
      //
      // We set bytecode_2() to _invokevirtual.
      // See also interpreterRuntime.cpp. (8/25/2000)
    } else {
      assert(invoke_code == Bytecodes::_invokevirtual ||
             (invoke_code == Bytecodes::_invokeinterface &&
              ((method->is_private() ||
                (method->is_final() && method->method_holder() == vmClasses::Object_klass())))),
             "unexpected invocation mode");
      if (invoke_code == Bytecodes::_invokeinterface &&
          (method->is_private() || method->is_final())) {
        // We set bytecode_1() to _invokeinterface, because that is the
        // bytecode # used by the interpreter to see if it is resolved.
        // We set bytecode_2() to _invokevirtual.
        set_bytecode_1(invoke_code);
      }
    }
    // set up for invokevirtual, even if linking for invokeinterface also:
    set_bytecode_2(Bytecodes::_invokevirtual);
  } else {
    ShouldNotReachHere();
  }
  NOT_PRODUCT(verify(tty));
}

void ConstantPoolCacheEntry::set_direct_call(Bytecodes::Code invoke_code, const methodHandle& method,
                                             bool sender_is_interface) {
  int index = Method::nonvirtual_vtable_index;
  // index < 0; FIXME: inline and customize set_direct_or_vtable_call
  set_direct_or_vtable_call(invoke_code, method, index, sender_is_interface);
}

void ConstantPoolCacheEntry::set_vtable_call(Bytecodes::Code invoke_code, const methodHandle& method, int index) {
  // either the method is a miranda or its holder should accept the given index
  assert(method->method_holder()->is_interface() || method->method_holder()->verify_vtable_index(index), "");
  // index >= 0; FIXME: inline and customize set_direct_or_vtable_call
  set_direct_or_vtable_call(invoke_code, method, index, false);
}

void ConstantPoolCacheEntry::set_itable_call(Bytecodes::Code invoke_code,
                                             Klass* referenced_klass,
                                             const methodHandle& method, int index) {
  assert(method->method_holder()->verify_itable_index(index), "");
  assert(invoke_code == Bytecodes::_invokeinterface, "");
  InstanceKlass* interf = method->method_holder();
  assert(interf->is_interface(), "must be an interface");
  assert(!method->is_final_method(), "interfaces do not have final methods; cannot link to one here");
  set_f1(referenced_klass);
  set_f2((intx)method());
  set_method_flags(as_TosState(method->result_type()),
                   0,  // no option bits
                   method()->size_of_parameters());
  set_bytecode_1(Bytecodes::_invokeinterface);
}


void ConstantPoolCacheEntry::set_method_handle(const constantPoolHandle& cpool, const CallInfo &call_info) {
  set_method_handle_common(cpool, Bytecodes::_invokehandle, call_info);
}

void ConstantPoolCacheEntry::set_dynamic_call(const constantPoolHandle& cpool, const CallInfo &call_info) {
  set_method_handle_common(cpool, Bytecodes::_invokedynamic, call_info);
}

void ConstantPoolCacheEntry::set_method_handle_common(const constantPoolHandle& cpool,
                                                      Bytecodes::Code invoke_code,
                                                      const CallInfo &call_info) {
  // NOTE: This CPCE can be the subject of data races.
  // There are three words to update: flags, refs[f2], f1 (in that order).
  // Writers must store all other values before f1.
  // Readers must test f1 first for non-null before reading other fields.
  // Competing writers must acquire exclusive access via a lock.
  // A losing writer waits on the lock until the winner writes f1 and leaves
  // the lock, so that when the losing writer returns, he can use the linked
  // cache entry.

  JavaThread* current = JavaThread::current();
  objArrayHandle resolved_references(current, cpool->resolved_references());
  // Use the resolved_references() lock for this cpCache entry.
  // resolved_references are created for all classes with Invokedynamic, MethodHandle
  // or MethodType constant pool cache entries.
  assert(resolved_references() != NULL,
         "a resolved_references array should have been created for this class");
  ObjectLocker ol(resolved_references, current);
  if (!is_f1_null()) {
    return;
  }

  if (indy_resolution_failed()) {
    // Before we got here, another thread got a LinkageError exception during
    // resolution.  Ignore our success and throw their exception.
    ConstantPoolCache* cpCache = cpool->cache();
    int index = -1;
    for (int i = 0; i < cpCache->length(); i++) {
      if (cpCache->entry_at(i) == this) {
        index = i;
        break;
      }
    }
    guarantee(index >= 0, "Didn't find cpCache entry!");
    int encoded_index = ResolutionErrorTable::encode_cpcache_index(
                          ConstantPool::encode_invokedynamic_index(index));
    JavaThread* THREAD = JavaThread::current(); // For exception macros.
    ConstantPool::throw_resolution_error(cpool, encoded_index, THREAD);
    return;
  }

  Method* adapter            = call_info.resolved_method();
  const Handle appendix      = call_info.resolved_appendix();
  const bool has_appendix    = appendix.not_null();

  // Write the flags.
  // MHs and indy are always sig-poly and have a local signature.
  set_method_flags(as_TosState(adapter->result_type()),
                   ((has_appendix    ? 1 : 0) << has_appendix_shift        ) |
                   (                   1      << has_local_signature_shift ) |
                   (                   1      << is_final_shift            ),
                   adapter->size_of_parameters());

  LogStream* log_stream = NULL;
  LogStreamHandle(Debug, methodhandles, indy) lsh_indy;
  if (lsh_indy.is_enabled()) {
    ResourceMark rm;
    log_stream = &lsh_indy;
    log_stream->print_cr("set_method_handle bc=%d appendix=" PTR_FORMAT "%s method=" PTR_FORMAT " (local signature) ",
                         invoke_code,
                         p2i(appendix()),
                         (has_appendix ? "" : " (unused)"),
                         p2i(adapter));
    adapter->print_on(log_stream);
    if (has_appendix)  appendix()->print_on(log_stream);
  }

  // Method handle invokes and invokedynamic sites use both cp cache words.
  // refs[f2], if not null, contains a value passed as a trailing argument to the adapter.
  // In the general case, this could be the call site's MethodType,
  // for use with java.lang.Invokers.checkExactType, or else a CallSite object.
  // f1 contains the adapter method which manages the actual call.
  // In the general case, this is a compiled LambdaForm.
  // (The Java code is free to optimize these calls by binding other
  // sorts of methods and appendices to call sites.)
  // JVM-level linking is via f1, as if for invokespecial, and signatures are erased.
  // The appendix argument (if any) is added to the signature, and is counted in the parameter_size bits.
  // Even with the appendix, the method will never take more than 255 parameter slots.
  //
  // This means that given a call site like (List)mh.invoke("foo"),
  // the f1 method has signature '(Ljl/Object;Ljl/invoke/MethodType;)Ljl/Object;',
  // not '(Ljava/lang/String;)Ljava/util/List;'.
  // The fact that String and List are involved is encoded in the MethodType in refs[f2].
  // This allows us to create fewer Methods, while keeping type safety.
  //

  // Store appendix, if any.
  if (has_appendix) {
    const int appendix_index = f2_as_index();
    assert(appendix_index >= 0 && appendix_index < resolved_references->length(), "oob");
    assert(resolved_references->obj_at(appendix_index) == NULL, "init just once");
    resolved_references->obj_at_put(appendix_index, appendix());
  }

  release_set_f1(adapter);  // This must be the last one to set (see NOTE above)!

  // The interpreter assembly code does not check byte_2,
  // but it is used by is_resolved, method_if_resolved, etc.
  set_bytecode_1(invoke_code);
  NOT_PRODUCT(verify(tty));

  if (log_stream != NULL) {
    this->print(log_stream, 0);
  }

  assert(has_appendix == this->has_appendix(), "proper storage of appendix flag");
  assert(this->has_local_signature(), "proper storage of signature flag");
}

bool ConstantPoolCacheEntry::save_and_throw_indy_exc(
  const constantPoolHandle& cpool, int cpool_index, int index, constantTag tag, TRAPS) {

  assert(HAS_PENDING_EXCEPTION, "No exception got thrown!");
  assert(PENDING_EXCEPTION->is_a(vmClasses::LinkageError_klass()),
         "No LinkageError exception");

  // Use the resolved_references() lock for this cpCache entry.
  // resolved_references are created for all classes with Invokedynamic, MethodHandle
  // or MethodType constant pool cache entries.
  JavaThread* current = THREAD;
  objArrayHandle resolved_references(current, cpool->resolved_references());
  assert(resolved_references() != NULL,
         "a resolved_references array should have been created for this class");
  ObjectLocker ol(resolved_references, current);

  // if f1 is not null or the indy_resolution_failed flag is set then another
  // thread either succeeded in resolving the method or got a LinkageError
  // exception, before this thread was able to record its failure.  So, clear
  // this thread's exception and return false so caller can use the earlier
  // thread's result.
  if (!is_f1_null() || indy_resolution_failed()) {
    CLEAR_PENDING_EXCEPTION;
    return false;
  }

  Symbol* error = PENDING_EXCEPTION->klass()->name();
  Symbol* message = java_lang_Throwable::detail_message(PENDING_EXCEPTION);

  SystemDictionary::add_resolution_error(cpool, index, error, message);
  set_indy_resolution_failed();
  return true;
}

Method* ConstantPoolCacheEntry::method_if_resolved(const constantPoolHandle& cpool) {
  // Decode the action of set_method and set_interface_call
  Bytecodes::Code invoke_code = bytecode_1();
  if (invoke_code != (Bytecodes::Code)0) {
    Metadata* f1 = f1_ord();
    if (f1 != NULL) {
      switch (invoke_code) {
      case Bytecodes::_invokeinterface:
        assert(f1->is_klass(), "");
        return f2_as_interface_method();
      case Bytecodes::_invokestatic:
      case Bytecodes::_invokespecial:
        assert(!has_appendix(), "");
      case Bytecodes::_invokehandle:
      case Bytecodes::_invokedynamic:
        assert(f1->is_method(), "");
        return (Method*)f1;
      default:
        break;
      }
    }
  }
  invoke_code = bytecode_2();
  if (invoke_code != (Bytecodes::Code)0) {
    switch (invoke_code) {
    case Bytecodes::_invokevirtual:
      if (is_vfinal()) {
        // invokevirtual
        Method* m = f2_as_vfinal_method();
        assert(m->is_method(), "");
        return m;
      } else {
        int holder_index = cpool->uncached_klass_ref_index_at(constant_pool_index());
        if (cpool->tag_at(holder_index).is_klass()) {
          Klass* klass = cpool->resolved_klass_at(holder_index);
          return klass->method_at_vtable(f2_as_index());
        }
      }
      break;
    default:
      break;
    }
  }
  return NULL;
}


oop ConstantPoolCacheEntry::appendix_if_resolved(const constantPoolHandle& cpool) {
  if (!has_appendix())
    return NULL;
  const int ref_index = f2_as_index();
  objArrayOop resolved_references = cpool->resolved_references();
  return resolved_references->obj_at(ref_index);
}


#if INCLUDE_JVMTI

void log_adjust(const char* entry_type, Method* old_method, Method* new_method, bool* trace_name_printed) {
  ResourceMark rm;

  if (!(*trace_name_printed)) {
    log_info(redefine, class, update)("adjust: name=%s", old_method->method_holder()->external_name());
    *trace_name_printed = true;
  }
  log_trace(redefine, class, update, constantpool)
    ("cpc %s entry update: %s", entry_type, new_method->external_name());
}

// RedefineClasses() API support:
// If this ConstantPoolCacheEntry refers to old_method then update it
// to refer to new_method.
void ConstantPoolCacheEntry::adjust_method_entry(Method* old_method,
       Method* new_method, bool * trace_name_printed) {

  if (is_vfinal()) {
    // virtual and final so _f2 contains method ptr instead of vtable index
    if (f2_as_vfinal_method() == old_method) {
      // match old_method so need an update
      // NOTE: can't use set_f2_as_vfinal_method as it asserts on different values
      _f2 = (intptr_t)new_method;
      log_adjust("vfinal", old_method, new_method, trace_name_printed);
    }
    return;
  }

  assert (_f1 != NULL, "should not call with uninteresting entry");

  if (!(_f1->is_method())) {
    // _f1 is a Klass* for an interface, _f2 is the method
    if (f2_as_interface_method() == old_method) {
      _f2 = (intptr_t)new_method;
      log_adjust("interface", old_method, new_method, trace_name_printed);
    }
  } else if (_f1 == old_method) {
    _f1 = new_method;
    log_adjust("special, static or dynamic", old_method, new_method, trace_name_printed);
  }
}

// a constant pool cache entry should never contain old or obsolete methods
bool ConstantPoolCacheEntry::check_no_old_or_obsolete_entries() {
  Method* m = get_interesting_method_entry();
  // return false if m refers to a non-deleted old or obsolete method
  if (m != NULL) {
    assert(m->is_valid() && m->is_method(), "m is a valid method");
    return !m->is_old() && !m->is_obsolete(); // old is always set for old and obsolete
  } else {
    return true;
  }
}

Method* ConstantPoolCacheEntry::get_interesting_method_entry() {
  if (!is_method_entry()) {
    // not a method entry so not interesting by default
    return NULL;
  }
  Method* m = NULL;
  if (is_vfinal()) {
    // virtual and final so _f2 contains method ptr instead of vtable index
    m = f2_as_vfinal_method();
  } else if (is_f1_null()) {
    // NULL _f1 means this is a virtual entry so also not interesting
    return NULL;
  } else {
    if (!(_f1->is_method())) {
      // _f1 is a Klass* for an interface
      m = f2_as_interface_method();
    } else {
      m = f1_as_method();
    }
  }
  assert(m != NULL && m->is_method(), "sanity check");
  if (m == NULL || !m->is_method()) {
    return NULL;
  }
  return m;
}
#endif // INCLUDE_JVMTI

void ConstantPoolCacheEntry::print(outputStream* st, int index) const {
  // print separator
  if (index == 0) st->print_cr("                 -------------");
  // print entry
  st->print("%3d  (" PTR_FORMAT ")  ", index, (intptr_t)this);
  st->print_cr("[%02x|%02x|%5d]", bytecode_2(), bytecode_1(),
               constant_pool_index());
  st->print_cr("                 [   " PTR_FORMAT "]", (intptr_t)_f1);
  st->print_cr("                 [   " PTR_FORMAT "]", (intptr_t)_f2);
  st->print_cr("                 [   " PTR_FORMAT "]", (intptr_t)_flags);
  st->print_cr("                 -------------");
}

void ConstantPoolCacheEntry::verify(outputStream* st) const {
  // not implemented yet
}

// Implementation of ConstantPoolCache

ConstantPoolCache* ConstantPoolCache::allocate(ClassLoaderData* loader_data,
                                     const intStack& index_map,
                                     const intStack& invokedynamic_index_map,
                                     const intStack& invokedynamic_map, TRAPS) {

  const int length = index_map.length() + invokedynamic_index_map.length();
  int size = ConstantPoolCache::size(length);

  return new (loader_data, size, MetaspaceObj::ConstantPoolCacheType, THREAD)
    ConstantPoolCache(length, index_map, invokedynamic_index_map, invokedynamic_map);
}

void ConstantPoolCache::initialize(const intArray& inverse_index_map,
                                   const intArray& invokedynamic_inverse_index_map,
                                   const intArray& invokedynamic_references_map) {
  for (int i = 0; i < inverse_index_map.length(); i++) {
    ConstantPoolCacheEntry* e = entry_at(i);
    int original_index = inverse_index_map.at(i);
    e->initialize_entry(original_index);
    assert(entry_at(i) == e, "sanity");
  }

  // Append invokedynamic entries at the end
  int invokedynamic_offset = inverse_index_map.length();
  for (int i = 0; i < invokedynamic_inverse_index_map.length(); i++) {
    int offset = i + invokedynamic_offset;
    ConstantPoolCacheEntry* e = entry_at(offset);
    int original_index = invokedynamic_inverse_index_map.at(i);
    e->initialize_entry(original_index);
    assert(entry_at(offset) == e, "sanity");
  }

  for (int ref = 0; ref < invokedynamic_references_map.length(); ref++) {
    const int cpci = invokedynamic_references_map.at(ref);
    if (cpci >= 0) {
      entry_at(cpci)->initialize_resolved_reference_index(ref);
    }
  }
}

void ConstantPoolCache::verify_just_initialized() {
  DEBUG_ONLY(walk_entries_for_initialization(/*check_only = */ true));
}

void ConstantPoolCache::remove_unshareable_info() {
  walk_entries_for_initialization(/*check_only = */ false);
}

void ConstantPoolCache::walk_entries_for_initialization(bool check_only) {
  Arguments::assert_is_dumping_archive();
  // When dumping the archive, we want to clean up the ConstantPoolCache
  // to remove any effect of linking due to the execution of Java code --
  // each ConstantPoolCacheEntry will have the same contents as if
  // ConstantPoolCache::initialize has just returned:
  //
  // - We keep the ConstantPoolCache::constant_pool_index() bits for all entries.
  // - We keep the "f2" field for entries used by invokedynamic and invokehandle
  // - All other bits in the entries are cleared to zero.
  ResourceMark rm;

  InstanceKlass* ik = constant_pool()->pool_holder();
  bool* f2_used = NEW_RESOURCE_ARRAY(bool, length());
  memset(f2_used, 0, sizeof(bool) * length());

  Thread* current = Thread::current();

  // Find all the slots that we need to preserve f2
  for (int i = 0; i < ik->methods()->length(); i++) {
    Method* m = ik->methods()->at(i);
    RawBytecodeStream bcs(methodHandle(current, m));
    while (!bcs.is_last_bytecode()) {
      Bytecodes::Code opcode = bcs.raw_next();
      switch (opcode) {
      case Bytecodes::_invokedynamic: {
          int index = Bytes::get_native_u4(bcs.bcp() + 1);
          int cp_cache_index = constant_pool()->invokedynamic_cp_cache_index(index);
          f2_used[cp_cache_index] = 1;
        }
        break;
      case Bytecodes::_invokehandle: {
          int cp_cache_index = Bytes::get_native_u2(bcs.bcp() + 1);
          f2_used[cp_cache_index] = 1;
        }
        break;
      default:
        break;
      }
    }
  }

  if (check_only) {
    DEBUG_ONLY(
      for (int i=0; i<length(); i++) {
        entry_at(i)->verify_just_initialized(f2_used[i]);
      })
  } else {
    for (int i=0; i<length(); i++) {
      entry_at(i)->reinitialize(f2_used[i]);
    }
  }
}

void ConstantPoolCache::deallocate_contents(ClassLoaderData* data) {
  assert(!is_shared(), "shared caches are not deallocated");
  data->remove_handle(_resolved_references);
  set_resolved_references(OopHandle());
  MetadataFactory::free_array<u2>(data, _reference_map);
  set_reference_map(NULL);
}

#if INCLUDE_CDS_JAVA_HEAP
oop ConstantPoolCache::archived_references() {
  if (_archived_references_index < 0) {
    return NULL;
  }
  return HeapShared::get_root(_archived_references_index);
}

void ConstantPoolCache::clear_archived_references() {
  if (_archived_references_index >= 0) {
    HeapShared::clear_root(_archived_references_index);
    _archived_references_index = -1;
  }
}

void ConstantPoolCache::set_archived_references(oop o) {
  assert(DumpSharedSpaces, "called only during runtime");
  _archived_references_index = HeapShared::append_root(o);
}
#endif

#if INCLUDE_JVMTI
// RedefineClasses() API support:
// If any entry of this ConstantPoolCache points to any of
// old_methods, replace it with the corresponding new_method.
void ConstantPoolCache::adjust_method_entries(bool * trace_name_printed) {
  for (int i = 0; i < length(); i++) {
    ConstantPoolCacheEntry* entry = entry_at(i);
    Method* old_method = entry->get_interesting_method_entry();
    if (old_method == NULL || !old_method->is_old()) {
      continue; // skip uninteresting entries
    }
    if (old_method->is_deleted()) {
      // clean up entries with deleted methods
      entry->initialize_entry(entry->constant_pool_index());
      continue;
    }
    Method* new_method = old_method->get_new_method();
    entry_at(i)->adjust_method_entry(old_method, new_method, trace_name_printed);
  }
}

// the constant pool cache should never contain old or obsolete methods
bool ConstantPoolCache::check_no_old_or_obsolete_entries() {
  ResourceMark rm;
  for (int i = 1; i < length(); i++) {
    Method* m = entry_at(i)->get_interesting_method_entry();
    if (m != NULL && !entry_at(i)->check_no_old_or_obsolete_entries()) {
      log_trace(redefine, class, update, constantpool)
        ("cpcache check found old method entry: class: %s, old: %d, obsolete: %d, method: %s",
         constant_pool()->pool_holder()->external_name(), m->is_old(), m->is_obsolete(), m->external_name());
      return false;
    }
  }
  return true;
}

void ConstantPoolCache::dump_cache() {
  for (int i = 1; i < length(); i++) {
    if (entry_at(i)->get_interesting_method_entry() != NULL) {
      entry_at(i)->print(tty, i);
    }
  }
}
#endif // INCLUDE_JVMTI

void ConstantPoolCache::metaspace_pointers_do(MetaspaceClosure* it) {
  log_trace(cds)("Iter(ConstantPoolCache): %p", this);
  it->push(&_constant_pool);
  it->push(&_reference_map);
}

// Printing

void ConstantPoolCache::print_on(outputStream* st) const {
  st->print_cr("%s", internal_name());
  // print constant pool cache entries
  for (int i = 0; i < length(); i++) entry_at(i)->print(st, i);
}

void ConstantPoolCache::print_value_on(outputStream* st) const {
  st->print("cache [%d]", length());
  print_address_on(st);
  st->print(" for ");
  constant_pool()->print_value_on(st);
}


// Verification

void ConstantPoolCache::verify_on(outputStream* st) {
  // print constant pool cache entries
  for (int i = 0; i < length(); i++) entry_at(i)->verify(st);
}
