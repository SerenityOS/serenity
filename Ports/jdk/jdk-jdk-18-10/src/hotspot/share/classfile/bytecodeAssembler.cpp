/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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

#include "classfile/bytecodeAssembler.hpp"
#include "interpreter/bytecodes.hpp"
#include "memory/oopFactory.hpp"
#include "oops/constantPool.hpp"
#include "runtime/handles.inline.hpp"
#include "utilities/bytes.hpp"

u2 BytecodeConstantPool::find_or_add(BytecodeCPEntry const& bcpe) {

  u2 index = _entries.length();
  bool created = false;
  u2* probe = _indices.put_if_absent(bcpe, index, &created);
  if (created) {
    _entries.append(bcpe);
  } else {
    index = *probe;
  }
  return index + _orig->length();
}

ConstantPool* BytecodeConstantPool::create_constant_pool(TRAPS) const {
  if (_entries.length() == 0) {
    return _orig;
  }

  ConstantPool* cp = ConstantPool::allocate(
      _orig->pool_holder()->class_loader_data(),
      _orig->length() + _entries.length(), CHECK_NULL);

  cp->set_pool_holder(_orig->pool_holder());
  constantPoolHandle cp_h(THREAD, cp);
  _orig->copy_cp_to(1, _orig->length() - 1, cp_h, 1, CHECK_NULL);

  // Preserve dynamic constant information from the original pool
  cp->copy_fields(_orig);

  for (int i = 0; i < _entries.length(); ++i) {
    BytecodeCPEntry entry = _entries.at(i);
    int idx = i + _orig->length();
    switch (entry._tag) {
      case BytecodeCPEntry::UTF8:
        entry._u.utf8->increment_refcount();
        cp->symbol_at_put(idx, entry._u.utf8);
        break;
      case BytecodeCPEntry::KLASS:
        cp->klass_index_at_put(
            idx, entry._u.klass);
        break;
      case BytecodeCPEntry::STRING:
        cp->unresolved_string_at_put(
            idx, cp->symbol_at(entry._u.string));
        break;
      case BytecodeCPEntry::NAME_AND_TYPE:
        cp->name_and_type_at_put(idx,
            entry._u.name_and_type.name_index,
            entry._u.name_and_type.type_index);
        break;
      case BytecodeCPEntry::METHODREF:
        cp->method_at_put(idx,
            entry._u.methodref.class_index,
            entry._u.methodref.name_and_type_index);
        break;
      default:
        ShouldNotReachHere();
    }
  }

  cp->initialize_unresolved_klasses(_orig->pool_holder()->class_loader_data(),
                                    CHECK_NULL);
  return cp;
}

void BytecodeAssembler::append(u1 imm_u1) {
  _code->append(imm_u1);
}

void BytecodeAssembler::append(u2 imm_u2) {
  _code->append(0);
  _code->append(0);
  Bytes::put_Java_u2(_code->adr_at(_code->length() - 2), imm_u2);
}

void BytecodeAssembler::append(u4 imm_u4) {
  _code->append(0);
  _code->append(0);
  _code->append(0);
  _code->append(0);
  Bytes::put_Java_u4(_code->adr_at(_code->length() - 4), imm_u4);
}

void BytecodeAssembler::xload(u4 index, u1 onebyteop, u1 twobyteop) {
  if (index < 4) {
    _code->append(onebyteop + index);
  } else {
    _code->append(twobyteop);
    _code->append((u2)index);
  }
}

void BytecodeAssembler::dup() {
  _code->append(Bytecodes::_dup);
}

void BytecodeAssembler::_new(Symbol* sym) {
  u2 cpool_index = _cp->klass(sym);
  _code->append(Bytecodes::_new);
  append(cpool_index);
}

void BytecodeAssembler::load_string(Symbol* sym) {
  u2 cpool_index = _cp->string(sym);
  if (cpool_index < 0x100) {
    ldc(cpool_index);
  } else {
    ldc_w(cpool_index);
  }
}

void BytecodeAssembler::ldc(u1 index) {
  _code->append(Bytecodes::_ldc);
  append(index);
}

void BytecodeAssembler::ldc_w(u2 index) {
  _code->append(Bytecodes::_ldc_w);
  append(index);
}

void BytecodeAssembler::athrow() {
  _code->append(Bytecodes::_athrow);
}

void BytecodeAssembler::iload(u4 index) {
  xload(index, Bytecodes::_iload_0, Bytecodes::_iload);
}

void BytecodeAssembler::lload(u4 index) {
  xload(index, Bytecodes::_lload_0, Bytecodes::_lload);
}

void BytecodeAssembler::fload(u4 index) {
  xload(index, Bytecodes::_fload_0, Bytecodes::_fload);
}

void BytecodeAssembler::dload(u4 index) {
  xload(index, Bytecodes::_dload_0, Bytecodes::_dload);
}

void BytecodeAssembler::aload(u4 index) {
  xload(index, Bytecodes::_aload_0, Bytecodes::_aload);
}

void BytecodeAssembler::load(BasicType bt, u4 index) {
  switch (bt) {
    case T_BOOLEAN:
    case T_CHAR:
    case T_BYTE:
    case T_SHORT:
    case T_INT:     iload(index); break;
    case T_FLOAT:   fload(index); break;
    case T_DOUBLE:  dload(index); break;
    case T_LONG:    lload(index); break;
    default:
      if (is_reference_type(bt)) {
                    aload(index);
                    break;
      }
      ShouldNotReachHere();
  }
}

void BytecodeAssembler::checkcast(Symbol* sym) {
  u2 cpool_index = _cp->klass(sym);
  _code->append(Bytecodes::_checkcast);
  append(cpool_index);
}

void BytecodeAssembler::invokespecial(Method* method) {
  invokespecial(method->klass_name(), method->name(), method->signature());
}

void BytecodeAssembler::invokespecial(Symbol* klss, Symbol* name, Symbol* sig) {
  u2 methodref_index = _cp->methodref(klss, name, sig);
  _code->append(Bytecodes::_invokespecial);
  append(methodref_index);
}

void BytecodeAssembler::invokevirtual(Method* method) {
  invokevirtual(method->klass_name(), method->name(), method->signature());
}

void BytecodeAssembler::invokevirtual(Symbol* klss, Symbol* name, Symbol* sig) {
  u2 methodref_index = _cp->methodref(klss, name, sig);
  _code->append(Bytecodes::_invokevirtual);
  append(methodref_index);
}

void BytecodeAssembler::ireturn() {
  _code->append(Bytecodes::_ireturn);
}

void BytecodeAssembler::lreturn() {
  _code->append(Bytecodes::_lreturn);
}

void BytecodeAssembler::freturn() {
  _code->append(Bytecodes::_freturn);
}

void BytecodeAssembler::dreturn() {
  _code->append(Bytecodes::_dreturn);
}

void BytecodeAssembler::areturn() {
  _code->append(Bytecodes::_areturn);
}

void BytecodeAssembler::_return() {
  _code->append(Bytecodes::_return);
}

void BytecodeAssembler::_return(BasicType bt) {
  switch (bt) {
    case T_BOOLEAN:
    case T_CHAR:
    case T_BYTE:
    case T_SHORT:
    case T_INT:     ireturn(); break;
    case T_FLOAT:   freturn(); break;
    case T_DOUBLE:  dreturn(); break;
    case T_LONG:    lreturn(); break;
    case T_VOID:    _return(); break;
    default:
      if (is_reference_type(bt)) {
                    areturn();
                    break;
      }
      ShouldNotReachHere();
  }
}
