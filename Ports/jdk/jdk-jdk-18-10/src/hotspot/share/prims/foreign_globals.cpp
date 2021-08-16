/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "foreign_globals.hpp"
#include "classfile/symbolTable.hpp"
#include "classfile/systemDictionary.hpp"
#include "classfile/vmSymbols.hpp"
#include "memory/resourceArea.hpp"
#include "runtime/fieldDescriptor.hpp"
#include "runtime/fieldDescriptor.inline.hpp"

#define FOREIGN_ABI "jdk/internal/foreign/abi/"

static int field_offset(InstanceKlass* cls, const char* fieldname, Symbol* sigsym) {
  TempNewSymbol fieldnamesym = SymbolTable::new_symbol(fieldname, (int)strlen(fieldname));
  fieldDescriptor fd;
  bool success = cls->find_field(fieldnamesym, sigsym, false, &fd);
  assert(success, "Field not found");
  return fd.offset();
}

static InstanceKlass* find_InstanceKlass(const char* name, TRAPS) {
  TempNewSymbol sym = SymbolTable::new_symbol(name, (int)strlen(name));
  Klass* k = SystemDictionary::resolve_or_null(sym, Handle(), Handle(), THREAD);
  assert(k != nullptr, "Can not find class: %s", name);
  return InstanceKlass::cast(k);
}

const ForeignGlobals& ForeignGlobals::instance() {
  static ForeignGlobals globals; // thread-safe lazy init-once (since C++11)
  return globals;
}

const ABIDescriptor ForeignGlobals::parse_abi_descriptor(jobject jabi) {
  return instance().parse_abi_descriptor_impl(jabi);
}
const BufferLayout ForeignGlobals::parse_buffer_layout(jobject jlayout) {
  return instance().parse_buffer_layout_impl(jlayout);
}

const CallRegs ForeignGlobals::parse_call_regs(jobject jconv) {
  return instance().parse_call_regs_impl(jconv);
}

ForeignGlobals::ForeignGlobals() {
  JavaThread* current_thread = JavaThread::current();
  ResourceMark rm(current_thread);

  // ABIDescriptor
  InstanceKlass* k_ABI = find_InstanceKlass(FOREIGN_ABI "ABIDescriptor", current_thread);
  const char* strVMSArrayArray = "[[L" FOREIGN_ABI "VMStorage;";
  Symbol* symVMSArrayArray = SymbolTable::new_symbol(strVMSArrayArray);
  ABI.inputStorage_offset = field_offset(k_ABI, "inputStorage", symVMSArrayArray);
  ABI.outputStorage_offset = field_offset(k_ABI, "outputStorage", symVMSArrayArray);
  ABI.volatileStorage_offset = field_offset(k_ABI, "volatileStorage", symVMSArrayArray);
  ABI.stackAlignment_offset = field_offset(k_ABI, "stackAlignment", vmSymbols::int_signature());
  ABI.shadowSpace_offset = field_offset(k_ABI, "shadowSpace", vmSymbols::int_signature());

  // VMStorage
  InstanceKlass* k_VMS = find_InstanceKlass(FOREIGN_ABI "VMStorage", current_thread);
  VMS.index_offset = field_offset(k_VMS, "index", vmSymbols::int_signature());
  VMS.type_offset = field_offset(k_VMS, "type", vmSymbols::int_signature());

  // BufferLayout
  InstanceKlass* k_BL = find_InstanceKlass(FOREIGN_ABI "BufferLayout", current_thread);
  BL.size_offset = field_offset(k_BL, "size", vmSymbols::long_signature());
  BL.arguments_next_pc_offset = field_offset(k_BL, "arguments_next_pc", vmSymbols::long_signature());
  BL.stack_args_bytes_offset = field_offset(k_BL, "stack_args_bytes", vmSymbols::long_signature());
  BL.stack_args_offset = field_offset(k_BL, "stack_args", vmSymbols::long_signature());
  BL.input_type_offsets_offset = field_offset(k_BL, "input_type_offsets", vmSymbols::long_array_signature());
  BL.output_type_offsets_offset = field_offset(k_BL, "output_type_offsets", vmSymbols::long_array_signature());

  // CallRegs
  const char* strVMSArray = "[L" FOREIGN_ABI "VMStorage;";
  Symbol* symVMSArray = SymbolTable::new_symbol(strVMSArray);
  InstanceKlass* k_CC = find_InstanceKlass(FOREIGN_ABI "ProgrammableUpcallHandler$CallRegs", current_thread);
  CallConvOffsets.arg_regs_offset = field_offset(k_CC, "argRegs", symVMSArray);
  CallConvOffsets.ret_regs_offset = field_offset(k_CC, "retRegs", symVMSArray);
}

void CallRegs::calling_convention(BasicType* sig_bt, VMRegPair *parm_regs, uint argcnt) const {
  int src_pos = 0;
  for (uint i = 0; i < argcnt; i++) {
    switch (sig_bt[i]) {
      case T_BOOLEAN:
      case T_CHAR:
      case T_BYTE:
      case T_SHORT:
      case T_INT:
      case T_FLOAT:
        assert(src_pos < _args_length, "oob");
        parm_regs[i].set1(_arg_regs[src_pos++]);
        break;
      case T_LONG:
      case T_DOUBLE:
        assert((i + 1) < argcnt && sig_bt[i + 1] == T_VOID, "expecting half");
        assert(src_pos < _args_length, "oob");
        parm_regs[i].set2(_arg_regs[src_pos++]);
        break;
      case T_VOID: // Halves of longs and doubles
        assert(i != 0 && (sig_bt[i - 1] == T_LONG || sig_bt[i - 1] == T_DOUBLE), "expecting half");
        parm_regs[i].set_bad();
        break;
      default:
        ShouldNotReachHere();
        break;
    }
  }
}
