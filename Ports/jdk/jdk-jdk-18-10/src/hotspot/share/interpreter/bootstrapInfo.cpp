/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/javaClasses.inline.hpp"
#include "classfile/resolutionErrors.hpp"
#include "classfile/systemDictionary.hpp"
#include "classfile/vmClasses.hpp"
#include "interpreter/bootstrapInfo.hpp"
#include "interpreter/linkResolver.hpp"
#include "logging/log.hpp"
#include "logging/logStream.hpp"
#include "memory/oopFactory.hpp"
#include "memory/resourceArea.hpp"
#include "oops/cpCache.inline.hpp"
#include "oops/objArrayOop.inline.hpp"
#include "oops/typeArrayOop.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/thread.inline.hpp"
#include "runtime/vmThread.hpp"

//------------------------------------------------------------------------------------------------------------------------
// Implementation of BootstrapInfo

BootstrapInfo::BootstrapInfo(const constantPoolHandle& pool, int bss_index, int indy_index)
  : _pool(pool),
    _bss_index(bss_index),
    _indy_index(indy_index),
    // derived and eagerly cached:
    _argc(      pool->bootstrap_argument_count_at(bss_index) ),
    _name(      pool->uncached_name_ref_at(bss_index) ),
    _signature( pool->uncached_signature_ref_at(bss_index) )
{
  _is_resolved = false;
  assert(pool->tag_at(bss_index).has_bootstrap(), "");
  assert(indy_index == -1 || pool->invokedynamic_bootstrap_ref_index_at(indy_index) == bss_index, "invalid bootstrap specifier index");
}

// If there is evidence this call site was already linked, set the
// existing linkage data into result, or throw previous exception.
// Return true if either action is taken, else false.
bool BootstrapInfo::resolve_previously_linked_invokedynamic(CallInfo& result, TRAPS) {
  assert(_indy_index != -1, "");
  ConstantPoolCacheEntry* cpce = invokedynamic_cp_cache_entry();
  if (!cpce->is_f1_null()) {
    methodHandle method(     THREAD, cpce->f1_as_method());
    Handle       appendix(   THREAD, cpce->appendix_if_resolved(_pool));
    result.set_handle(method, appendix, THREAD);
    Exceptions::wrap_dynamic_exception(/* is_indy */ true, CHECK_false);
    return true;
  } else if (cpce->indy_resolution_failed()) {
    int encoded_index = ResolutionErrorTable::encode_cpcache_index(_indy_index);
    ConstantPool::throw_resolution_error(_pool, encoded_index, CHECK_false);
    return true;
  } else {
    return false;
  }
}

// Resolve the bootstrap specifier in 3 steps:
// - unpack the BSM by resolving the MH constant
// - obtain the NameAndType description for the condy/indy
// - prepare the BSM's static arguments
Handle BootstrapInfo::resolve_bsm(TRAPS) {
  if (_bsm.not_null()) {
    return _bsm;
  }

  bool is_indy = is_method_call();
  // The tag at the bootstrap method index must be a valid method handle or a method handle in error.
  // If it is a MethodHandleInError, a resolution error will be thrown which will be wrapped if necessary
  // with a BootstrapMethodError.
  assert(_pool->tag_at(bsm_index()).is_method_handle() ||
         _pool->tag_at(bsm_index()).is_method_handle_in_error(), "MH not present, classfile structural constraint");
  oop bsm_oop = _pool->resolve_possibly_cached_constant_at(bsm_index(), THREAD);
  Exceptions::wrap_dynamic_exception(is_indy, CHECK_NH);
  guarantee(java_lang_invoke_MethodHandle::is_instance(bsm_oop), "classfile must supply a valid BSM");
  _bsm = Handle(THREAD, bsm_oop);

  // Obtain NameAndType information
  resolve_bss_name_and_type(THREAD);
  Exceptions::wrap_dynamic_exception(is_indy, CHECK_NH);

  // Prepare static arguments
  resolve_args(THREAD);
  Exceptions::wrap_dynamic_exception(is_indy, CHECK_NH);

  return _bsm;
}

// Resolve metadata from the JVM_Dynamic_info or JVM_InvokeDynamic_info's name and type information.
void BootstrapInfo::resolve_bss_name_and_type(TRAPS) {
  assert(_bsm.not_null(), "resolve_bsm first");
  Symbol* name = this->name();
  Symbol* type = this->signature();
  _name_arg = java_lang_String::create_from_symbol(name, CHECK);
  if (type->char_at(0) == '(') {
    _type_arg = SystemDictionary::find_method_handle_type(type, caller(), CHECK);
  } else {
    _type_arg = SystemDictionary::find_java_mirror_for_type(type, caller(), SignatureStream::NCDFError, CHECK);
  }
}

// Resolve the bootstrap method's static arguments and store the result in _arg_values.
void BootstrapInfo::resolve_args(TRAPS) {
  assert(_bsm.not_null(), "resolve_bsm first");

  // if there are no static arguments, return leaving _arg_values as null
  if (_argc == 0 && UseBootstrapCallInfo < 2) return;

  bool use_BSCI;
  switch (UseBootstrapCallInfo) {
  default: use_BSCI = true;  break;  // stress mode
  case 0:  use_BSCI = false; break;  // stress mode
  case 1:                            // normal mode
    // If we were to support an alternative mode of BSM invocation,
    // we'd convert to pull mode here if the BSM could be a candidate
    // for that alternative mode.  We can't easily test for things
    // like varargs here, but we can get away with approximate testing,
    // since the JDK runtime will make up the difference either way.
    // For now, exercise the pull-mode path if the BSM is of arity 2,
    // or if there is a potential condy loop (see below).
    oop mt_oop = java_lang_invoke_MethodHandle::type(_bsm());
    use_BSCI = (java_lang_invoke_MethodType::ptype_count(mt_oop) == 2);
    break;
  }

  // Here's a reason to use BSCI even if it wasn't requested:
  // If a condy uses a condy argument, we want to avoid infinite
  // recursion (condy loops) in the C code.  It's OK in Java,
  // because Java has stack overflow checking, so we punt
  // potentially cyclic cases from C to Java.
  if (!use_BSCI && _pool->tag_at(_bss_index).is_dynamic_constant()) {
    bool found_unresolved_condy = false;
    for (int i = 0; i < _argc; i++) {
      int arg_index = _pool->bootstrap_argument_index_at(_bss_index, i);
      if (_pool->tag_at(arg_index).is_dynamic_constant()) {
        // potential recursion point condy -> condy
        bool found_it = false;
        _pool->find_cached_constant_at(arg_index, found_it, CHECK);
        if (!found_it) { found_unresolved_condy = true; break; }
      }
    }
    if (found_unresolved_condy)
      use_BSCI = true;
  }

  const int SMALL_ARITY = 5;
  if (use_BSCI && _argc <= SMALL_ARITY && UseBootstrapCallInfo <= 2) {
    // If there are only a few arguments, and none of them need linking,
    // push them, instead of asking the JDK runtime to turn around and
    // pull them, saving a JVM/JDK transition in some simple cases.
    bool all_resolved = true;
    for (int i = 0; i < _argc; i++) {
      bool found_it = false;
      int arg_index = _pool->bootstrap_argument_index_at(_bss_index, i);
      _pool->find_cached_constant_at(arg_index, found_it, CHECK);
      if (!found_it) { all_resolved = false; break; }
    }
    if (all_resolved)
      use_BSCI = false;
  }

  if (!use_BSCI) {
    // return {arg...}; resolution of arguments is done immediately, before JDK code is called
    objArrayOop args_oop = oopFactory::new_objArray(vmClasses::Object_klass(), _argc, CHECK);
    objArrayHandle args(THREAD, args_oop);
    _pool->copy_bootstrap_arguments_at(_bss_index, 0, _argc, args, 0, true, Handle(), CHECK);
    oop arg_oop = ((_argc == 1) ? args->obj_at(0) : (oop)NULL);
    // try to discard the singleton array
    if (arg_oop != NULL && !arg_oop->is_array()) {
      // JVM treats arrays and nulls specially in this position,
      // but other things are just single arguments
      _arg_values = Handle(THREAD, arg_oop);
    } else {
      _arg_values = args;
    }
  } else {
    // return {arg_count, pool_index}; JDK code must pull the arguments as needed
    typeArrayOop ints_oop = oopFactory::new_typeArray(T_INT, 2, CHECK);
    ints_oop->int_at_put(0, _argc);
    ints_oop->int_at_put(1, _bss_index);
    _arg_values = Handle(THREAD, ints_oop);
  }
}

// there must be a LinkageError pending; try to save it and then throw
bool BootstrapInfo::save_and_throw_indy_exc(TRAPS) {
  assert(HAS_PENDING_EXCEPTION, "");
  assert(_indy_index != -1, "");
  ConstantPoolCacheEntry* cpce = invokedynamic_cp_cache_entry();
  int encoded_index = ResolutionErrorTable::encode_cpcache_index(_indy_index);
  bool recorded_res_status = cpce->save_and_throw_indy_exc(_pool, _bss_index,
                                                           encoded_index,
                                                           pool()->tag_at(_bss_index),
                                                           CHECK_false);
  return recorded_res_status;
}

void BootstrapInfo::resolve_newly_linked_invokedynamic(CallInfo& result, TRAPS) {
  assert(is_resolved(), "");
  result.set_handle(resolved_method(), resolved_appendix(), CHECK);
}

void BootstrapInfo::print_msg_on(outputStream* st, const char* msg) {
  ResourceMark rm;
  char what[20];
  st = st ? st : tty;

  if (_indy_index != -1)
    sprintf(what, "indy#%d", decode_indy_index());
  else
    sprintf(what, "condy");
  bool have_msg = (msg != NULL && strlen(msg) > 0);
  st->print_cr("%s%sBootstrap in %s %s@CP[%d] %s:%s%s BSMS[%d] BSM@CP[%d]%s argc=%d%s",
                (have_msg ? msg : ""), (have_msg ? " " : ""),
                caller()->name()->as_C_string(),
                what,  // "indy#42" or "condy"
                _bss_index,
                _name->as_C_string(),
                _signature->as_C_string(),
                (_type_arg.is_null() ? "" : "(resolved)"),
                bsms_attr_index(),
                bsm_index(), (_bsm.is_null() ? "" : "(resolved)"),
                _argc, (_arg_values.is_null() ? "" : "(resolved)"));
  if (_argc > 0) {
    char argbuf[80];
    argbuf[0] = 0;
    for (int i = 0; i < _argc; i++) {
      int pos = (int) strlen(argbuf);
      if (pos + 20 > (int)sizeof(argbuf)) {
        sprintf(argbuf + pos, "...");
        break;
      }
      if (i > 0)  argbuf[pos++] = ',';
      sprintf(argbuf+pos, "%d", arg_index(i));
    }
    st->print_cr("  argument indexes: {%s}", argbuf);
  }
  if (_bsm.not_null()) {
    st->print("  resolved BSM: "); _bsm->print_on(st);
  }

  // How the array of resolved arguments is printed depends highly
  // on how BootstrapInfo::resolve_args structures the array based on
  // the use_BSCI setting.
  if (_arg_values.not_null()) {
    // Find the static arguments within the first element of _arg_values.
    objArrayOop static_args = (objArrayOop)_arg_values();
    if (!static_args->is_array()) {
      assert(_argc == 1, "Invalid BSM _arg_values for non-array");
      st->print("  resolved arg[0]: "); static_args->print_on(st);
    } else if (static_args->is_objArray()) {
      int lines = 0;
      for (int i = 0; i < _argc; i++) {
        oop x = static_args->obj_at(i);
        if (x != NULL) {
          if (++lines > 6) {
            st->print_cr("  resolved arg[%d]: ...", i);
            break;
          }
          st->print("  resolved arg[%d]: ", i); x->print_on(st);
        }
      }
    } else if (static_args->is_typeArray()) {
      typeArrayOop tmp_array = (typeArrayOop) static_args;
      assert(tmp_array->length() == 2, "Invalid BSM _arg_values type array");
      st->print_cr("  resolved arg[0]: %d", tmp_array->int_at(0));
      st->print_cr("  resolved arg[1]: %d", tmp_array->int_at(1));
    }
  }
}
