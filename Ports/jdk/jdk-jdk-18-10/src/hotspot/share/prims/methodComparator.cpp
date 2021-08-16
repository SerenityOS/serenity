/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "logging/log.hpp"
#include "memory/resourceArea.hpp"
#include "oops/constantPool.inline.hpp"
#include "oops/oop.inline.hpp"
#include "oops/symbol.hpp"
#include "prims/methodComparator.hpp"
#include "runtime/handles.inline.hpp"
#include "utilities/globalDefinitions.hpp"

bool MethodComparator::methods_EMCP(Method* old_method, Method* new_method) {
  if (old_method->code_size() != new_method->code_size())
    return false;
  if (check_stack_and_locals_size(old_method, new_method) != 0) {
    if (log_is_enabled(Debug, redefine, class, methodcomparator)) {
      ResourceMark rm;
      log_debug(redefine, class, methodcomparator)
        ("Methods %s non-comparable with diagnosis %d",
         old_method->name()->as_C_string(), check_stack_and_locals_size(old_method, new_method));
    }
    return false;
  }

  ConstantPool* old_cp = old_method->constants();
  ConstantPool* new_cp = new_method->constants();
  Thread* current = Thread::current();
  BytecodeStream s_old(methodHandle(current, old_method));
  BytecodeStream s_new(methodHandle(current, new_method));
  Bytecodes::Code c_old, c_new;

  while ((c_old = s_old.next()) >= 0) {
    if ((c_new = s_new.next()) < 0 || c_old != c_new)
      return false;

    if (!args_same(c_old, c_new, &s_old, &s_new, old_cp, new_cp))
      return false;
  }
  return true;
}

bool MethodComparator::args_same(Bytecodes::Code const c_old,  Bytecodes::Code const c_new,
                                 BytecodeStream* const s_old,  BytecodeStream* const s_new,
                                 ConstantPool*   const old_cp, ConstantPool*   const new_cp) {
  // BytecodeStream returns the correct standard Java bytecodes for various "fast"
  // bytecode versions, so we don't have to bother about them here..
  switch (c_old) {
  case Bytecodes::_new            : // fall through
  case Bytecodes::_anewarray      : // fall through
  case Bytecodes::_multianewarray : // fall through
  case Bytecodes::_checkcast      : // fall through
  case Bytecodes::_instanceof     : {
    u2 cpi_old = s_old->get_index_u2();
    u2 cpi_new = s_new->get_index_u2();
    if (old_cp->klass_at_noresolve(cpi_old) != new_cp->klass_at_noresolve(cpi_new))
        return false;
    if (c_old == Bytecodes::_multianewarray &&
        *(jbyte*)(s_old->bcp() + 3) != *(jbyte*)(s_new->bcp() + 3))
      return false;
    break;
  }

  case Bytecodes::_getstatic       : // fall through
  case Bytecodes::_putstatic       : // fall through
  case Bytecodes::_getfield        : // fall through
  case Bytecodes::_putfield        : // fall through
  case Bytecodes::_invokevirtual   : // fall through
  case Bytecodes::_invokespecial   : // fall through
  case Bytecodes::_invokestatic    : // fall through
  case Bytecodes::_invokeinterface : {
    int cpci_old = s_old->get_index_u2_cpcache();
    int cpci_new = s_new->get_index_u2_cpcache();
    // Check if the names of classes, field/method names and signatures at these indexes
    // are the same. Indices which are really into constantpool cache (rather than constant
    // pool itself) are accepted by the constantpool query routines below.
    if ((old_cp->klass_ref_at_noresolve(cpci_old) != new_cp->klass_ref_at_noresolve(cpci_new)) ||
        (old_cp->name_ref_at(cpci_old) != new_cp->name_ref_at(cpci_new)) ||
        (old_cp->signature_ref_at(cpci_old) != new_cp->signature_ref_at(cpci_new)))
      return false;
    break;
  }
  case Bytecodes::_invokedynamic: {
    int cpci_old = s_old->get_index_u4();
    int cpci_new = s_new->get_index_u4();

    // Check if the names of classes, field/method names and signatures at these indexes
    // are the same. Indices which are really into constantpool cache (rather than constant
    // pool itself) are accepted by the constantpool query routines below.
    if ((old_cp->name_ref_at(cpci_old) != new_cp->name_ref_at(cpci_new)) ||
        (old_cp->signature_ref_at(cpci_old) != new_cp->signature_ref_at(cpci_new)))
      return false;

    // Translate object indexes to constant pool cache indexes.
    cpci_old = old_cp->invokedynamic_cp_cache_index(cpci_old);
    cpci_new = new_cp->invokedynamic_cp_cache_index(cpci_new);

    int cpi_old = old_cp->cache()->entry_at(cpci_old)->constant_pool_index();
    int cpi_new = new_cp->cache()->entry_at(cpci_new)->constant_pool_index();
    int bsm_old = old_cp->bootstrap_method_ref_index_at(cpi_old);
    int bsm_new = new_cp->bootstrap_method_ref_index_at(cpi_new);
    if (!pool_constants_same(bsm_old, bsm_new, old_cp, new_cp))
      return false;
    int cnt_old = old_cp->bootstrap_argument_count_at(cpi_old);
    int cnt_new = new_cp->bootstrap_argument_count_at(cpi_new);
    if (cnt_old != cnt_new)
      return false;
    for (int arg_i = 0; arg_i < cnt_old; arg_i++) {
      int idx_old = old_cp->bootstrap_argument_index_at(cpi_old, arg_i);
      int idx_new = new_cp->bootstrap_argument_index_at(cpi_new, arg_i);
      if (!pool_constants_same(idx_old, idx_new, old_cp, new_cp))
        return false;
    }
    break;
  }

  case Bytecodes::_ldc   : // fall through
  case Bytecodes::_ldc_w : {
    Bytecode_loadconstant ldc_old(s_old->method(), s_old->bci());
    Bytecode_loadconstant ldc_new(s_new->method(), s_new->bci());
    int cpi_old = ldc_old.pool_index();
    int cpi_new = ldc_new.pool_index();
    if (!pool_constants_same(cpi_old, cpi_new, old_cp, new_cp))
      return false;
    break;
  }

  case Bytecodes::_ldc2_w : {
    u2 cpi_old = s_old->get_index_u2();
    u2 cpi_new = s_new->get_index_u2();
    constantTag tag_old = old_cp->tag_at(cpi_old);
    constantTag tag_new = new_cp->tag_at(cpi_new);
    if (tag_old.value() != tag_new.value())
      return false;
    if (tag_old.is_long()) {
      if (old_cp->long_at(cpi_old) != new_cp->long_at(cpi_new))
        return false;
    } else {
      // Use jlong_cast to compare the bits rather than numerical values.
      // This makes a difference for NaN constants.
      if (jlong_cast(old_cp->double_at(cpi_old)) != jlong_cast(new_cp->double_at(cpi_new)))
        return false;
    }
    break;
  }

  case Bytecodes::_bipush :
    if (s_old->bcp()[1] != s_new->bcp()[1])
      return false;
    break;

  case Bytecodes::_sipush    :
    if (s_old->get_index_u2() != s_new->get_index_u2())
      return false;
    break;

  case Bytecodes::_aload  : // fall through
  case Bytecodes::_astore : // fall through
  case Bytecodes::_dload  : // fall through
  case Bytecodes::_dstore : // fall through
  case Bytecodes::_fload  : // fall through
  case Bytecodes::_fstore : // fall through
  case Bytecodes::_iload  : // fall through
  case Bytecodes::_istore : // fall through
  case Bytecodes::_lload  : // fall through
  case Bytecodes::_lstore : // fall through
  case Bytecodes::_ret    :
    if (s_old->is_wide() != s_new->is_wide())
      return false;
    if (s_old->get_index() != s_new->get_index())
      return false;
    break;

  case Bytecodes::_goto      : // fall through
  case Bytecodes::_if_acmpeq : // fall through
  case Bytecodes::_if_acmpne : // fall through
  case Bytecodes::_if_icmpeq : // fall through
  case Bytecodes::_if_icmpne : // fall through
  case Bytecodes::_if_icmplt : // fall through
  case Bytecodes::_if_icmpge : // fall through
  case Bytecodes::_if_icmpgt : // fall through
  case Bytecodes::_if_icmple : // fall through
  case Bytecodes::_ifeq      : // fall through
  case Bytecodes::_ifne      : // fall through
  case Bytecodes::_iflt      : // fall through
  case Bytecodes::_ifge      : // fall through
  case Bytecodes::_ifgt      : // fall through
  case Bytecodes::_ifle      : // fall through
  case Bytecodes::_ifnonnull : // fall through
  case Bytecodes::_ifnull    : // fall through
  case Bytecodes::_jsr       : {
    int old_ofs = s_old->bytecode().get_offset_s2(c_old);
    int new_ofs = s_new->bytecode().get_offset_s2(c_new);
    if (old_ofs != new_ofs)
      return false;
    break;
  }

  case Bytecodes::_iinc :
    if (s_old->is_wide() != s_new->is_wide())
      return false;
    if (!s_old->is_wide()) {
      // We could use get_index_u1 and get_constant_u1, but it's simpler to grab both bytes at once:
      if (Bytes::get_Java_u2(s_old->bcp() + 1) != Bytes::get_Java_u2(s_new->bcp() + 1))
        return false;
    } else {
      // We could use get_index_u2 and get_constant_u2, but it's simpler to grab all four bytes at once:
      if (Bytes::get_Java_u4(s_old->bcp() + 1) != Bytes::get_Java_u4(s_new->bcp() + 1))
        return false;
    }
    break;

  case Bytecodes::_goto_w : // fall through
  case Bytecodes::_jsr_w  : {
    int old_ofs = s_old->bytecode().get_offset_s4(c_old);
    int new_ofs = s_new->bytecode().get_offset_s4(c_new);
    if (old_ofs != new_ofs)
      return false;
    break;
  }

  case Bytecodes::_lookupswitch : // fall through
  case Bytecodes::_tableswitch  : {
    int len_old = s_old->instruction_size();
    int len_new = s_new->instruction_size();
    if (len_old != len_new)
      return false;
    if (memcmp(s_old->bcp(), s_new->bcp(), len_old) != 0)
      return false;
    break;
  }

  default:
    break;
  }

  return true;
}

bool MethodComparator::pool_constants_same(const int cpi_old, const int cpi_new,
                                           ConstantPool* const old_cp, ConstantPool* const new_cp) {
  constantTag tag_old = old_cp->tag_at(cpi_old);
  constantTag tag_new = new_cp->tag_at(cpi_new);
  if (tag_old.is_int() || tag_old.is_float()) {
    if (tag_old.value() != tag_new.value())
      return false;
    if (tag_old.is_int()) {
      if (old_cp->int_at(cpi_old) != new_cp->int_at(cpi_new))
        return false;
    } else {
      // Use jint_cast to compare the bits rather than numerical values.
      // This makes a difference for NaN constants.
      if (jint_cast(old_cp->float_at(cpi_old)) != jint_cast(new_cp->float_at(cpi_new)))
        return false;
    }
  } else if (tag_old.is_string() && tag_new.is_string()) {
    if (strcmp(old_cp->string_at_noresolve(cpi_old),
               new_cp->string_at_noresolve(cpi_new)) != 0)
      return false;
  } else if (tag_old.is_klass() || tag_old.is_unresolved_klass()) {
    // tag_old should be klass - 4881222
    if (! (tag_new.is_unresolved_klass() || tag_new.is_klass()))
      return false;
    if (old_cp->klass_at_noresolve(cpi_old) !=
        new_cp->klass_at_noresolve(cpi_new))
      return false;
  } else if (tag_old.is_method_type() && tag_new.is_method_type()) {
    int mti_old = old_cp->method_type_index_at(cpi_old);
    int mti_new = new_cp->method_type_index_at(cpi_new);
    if ((old_cp->symbol_at(mti_old) != new_cp->symbol_at(mti_new)))
      return false;
  } else if (tag_old.is_method_handle() && tag_new.is_method_handle()) {
    if (old_cp->method_handle_ref_kind_at(cpi_old) !=
        new_cp->method_handle_ref_kind_at(cpi_new))
      return false;
    int mhi_old = old_cp->method_handle_index_at(cpi_old);
    int mhi_new = new_cp->method_handle_index_at(cpi_new);
    if ((old_cp->uncached_klass_ref_at_noresolve(mhi_old) != new_cp->uncached_klass_ref_at_noresolve(mhi_new)) ||
        (old_cp->uncached_name_ref_at(mhi_old) != new_cp->uncached_name_ref_at(mhi_new)) ||
        (old_cp->uncached_signature_ref_at(mhi_old) != new_cp->uncached_signature_ref_at(mhi_new)))
      return false;
  } else {
    return false;  // unknown tag
  }
  return true;
}


int MethodComparator::check_stack_and_locals_size(Method* old_method, Method* new_method) {
  if (old_method->max_stack() != new_method->max_stack()) {
    return 1;
  } else if (old_method->max_locals() != new_method->max_locals()) {
    return 2;
  } else if (old_method->size_of_parameters() != new_method->size_of_parameters()) {
    return 3;
  } else return 0;
}
