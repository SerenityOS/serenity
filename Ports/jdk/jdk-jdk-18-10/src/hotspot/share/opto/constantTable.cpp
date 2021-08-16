/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "asm/codeBuffer.hpp"
#include "asm/macroAssembler.hpp"
#include "opto/block.hpp"
#include "opto/constantTable.hpp"
#include "opto/machnode.hpp"
#include "opto/output.hpp"

//=============================================================================
// Two Constant's are equal when the type and the value are equal.
bool ConstantTable::Constant::operator==(const Constant& other) {
  if (type()          != other.type()         )  return false;
  if (can_be_reused() != other.can_be_reused())  return false;
  // For floating point values we compare the bit pattern.
  switch (type()) {
  case T_INT:
  case T_FLOAT:   return (_v._value.i == other._v._value.i);
  case T_LONG:
  case T_DOUBLE:  return (_v._value.j == other._v._value.j);
  case T_OBJECT:
  case T_ADDRESS: return (_v._value.l == other._v._value.l);
  case T_VOID:    return (_v._value.l == other._v._value.l);  // jump-table entries
  case T_METADATA: return (_v._metadata == other._v._metadata);
  default: ShouldNotReachHere(); return false;
  }
}

int ConstantTable::qsort_comparator(Constant* a, Constant* b) {
  // sort descending
  if (a->freq() > b->freq())  return -1;
  if (a->freq() < b->freq())  return  1;
  return 0;
}

static int type_to_size_in_bytes(BasicType t) {
  switch (t) {
  case T_INT:     return sizeof(jint   );
  case T_LONG:    return sizeof(jlong  );
  case T_FLOAT:   return sizeof(jfloat );
  case T_DOUBLE:  return sizeof(jdouble);
  case T_METADATA: return sizeof(Metadata*);
    // We use T_VOID as marker for jump-table entries (labels) which
    // need an internal word relocation.
  case T_VOID:
  case T_ADDRESS:
  case T_OBJECT:  return sizeof(jobject);
  default:
    ShouldNotReachHere();
    return -1;
  }
}

void ConstantTable::calculate_offsets_and_size() {
  // First, sort the array by frequencies.
  _constants.sort(qsort_comparator);

#ifdef ASSERT
  // Make sure all jump-table entries were sorted to the end of the
  // array (they have a negative frequency).
  bool found_void = false;
  for (int i = 0; i < _constants.length(); i++) {
    Constant con = _constants.at(i);
    if (con.type() == T_VOID)
      found_void = true;  // jump-tables
    else
      assert(!found_void, "wrong sorting");
  }
#endif

  int offset = 0;
  for (int i = 0; i < _constants.length(); i++) {
    Constant* con = _constants.adr_at(i);

    // Align offset for type.
    int typesize = type_to_size_in_bytes(con->type());
    offset = align_up(offset, typesize);
    con->set_offset(offset);   // set constant's offset

    if (con->type() == T_VOID) {
      MachConstantNode* n = (MachConstantNode*) con->get_jobject();
      offset = offset + typesize * n->outcnt();  // expand jump-table
    } else {
      offset = offset + typesize;
    }
  }

  // Align size up to the next section start (which is insts; see
  // CodeBuffer::align_at_start).
  assert(_size == -1, "already set?");
  _size = align_up(offset, (int)CodeEntryAlignment);
}

bool ConstantTable::emit(CodeBuffer& cb) const {
  MacroAssembler _masm(&cb);
  for (int i = 0; i < _constants.length(); i++) {
    Constant con = _constants.at(i);
    address constant_addr = NULL;
    switch (con.type()) {
    case T_INT:    constant_addr = _masm.int_constant(   con.get_jint()   ); break;
    case T_LONG:   constant_addr = _masm.long_constant(  con.get_jlong()  ); break;
    case T_FLOAT:  constant_addr = _masm.float_constant( con.get_jfloat() ); break;
    case T_DOUBLE: constant_addr = _masm.double_constant(con.get_jdouble()); break;
    case T_OBJECT: {
      jobject obj = con.get_jobject();
      int oop_index = _masm.oop_recorder()->find_index(obj);
      constant_addr = _masm.address_constant((address) obj, oop_Relocation::spec(oop_index));
      break;
    }
    case T_ADDRESS: {
      address addr = (address) con.get_jobject();
      constant_addr = _masm.address_constant(addr);
      break;
    }
    // We use T_VOID as marker for jump-table entries (labels) which
    // need an internal word relocation.
    case T_VOID: {
      MachConstantNode* n = (MachConstantNode*) con.get_jobject();
      // Fill the jump-table with a dummy word.  The real value is
      // filled in later in fill_jump_table.
      address dummy = (address) n;
      constant_addr = _masm.address_constant(dummy);
      if (constant_addr == NULL) {
        return false;
      }
      assert((constant_addr - _masm.code()->consts()->start()) == con.offset(),
             "must be: %d == %d", (int)(constant_addr - _masm.code()->consts()->start()), (int)(con.offset()));

      // Expand jump-table
      address last_addr = NULL;
      for (uint j = 1; j < n->outcnt(); j++) {
        last_addr = _masm.address_constant(dummy + j);
        if (last_addr == NULL) {
          return false;
        }
      }
#ifdef ASSERT
      address start = _masm.code()->consts()->start();
      address new_constant_addr = last_addr - ((n->outcnt() - 1) * sizeof(address));
      // Expanding the jump-table could result in an expansion of the const code section.
      // In that case, we need to check if the new constant address matches the offset.
      assert((constant_addr - start == con.offset()) || (new_constant_addr - start == con.offset()),
             "must be: %d == %d or %d == %d (after an expansion)", (int)(constant_addr - start), (int)(con.offset()),
             (int)(new_constant_addr - start), (int)(con.offset()));
#endif
      continue; // Loop
    }
    case T_METADATA: {
      Metadata* obj = con.get_metadata();
      int metadata_index = _masm.oop_recorder()->find_index(obj);
      constant_addr = _masm.address_constant((address) obj, metadata_Relocation::spec(metadata_index));
      break;
    }
    default: ShouldNotReachHere();
    }

    if (constant_addr == NULL) {
      return false;
    }
    assert((constant_addr - _masm.code()->consts()->start()) == con.offset(),
            "must be: %d == %d", (int)(constant_addr - _masm.code()->consts()->start()), (int)(con.offset()));
  }
  return true;
}

int ConstantTable::find_offset(Constant& con) const {
  int idx = _constants.find(con);
  guarantee(idx != -1, "constant must be in constant table");
  int offset = _constants.at(idx).offset();
  guarantee(offset != -1, "constant table not emitted yet?");
  return offset;
}

void ConstantTable::add(Constant& con) {
  if (con.can_be_reused()) {
    int idx = _constants.find(con);
    if (idx != -1 && _constants.at(idx).can_be_reused()) {
      _constants.adr_at(idx)->inc_freq(con.freq());  // increase the frequency by the current value
      return;
    }
  }
  (void) _constants.append(con);
}

ConstantTable::Constant ConstantTable::add(MachConstantNode* n, BasicType type, jvalue value) {
  Block* b = Compile::current()->cfg()->get_block_for_node(n);
  Constant con(type, value, b->_freq);
  add(con);
  return con;
}

ConstantTable::Constant ConstantTable::add(Metadata* metadata) {
  Constant con(metadata);
  add(con);
  return con;
}

ConstantTable::Constant ConstantTable::add(MachConstantNode* n, MachOper* oper) {
  jvalue value;
  BasicType type = oper->type()->basic_type();
  switch (type) {
  case T_LONG:    value.j = oper->constantL(); break;
  case T_FLOAT:   value.f = oper->constantF(); break;
  case T_DOUBLE:  value.d = oper->constantD(); break;
  case T_OBJECT:
  case T_ADDRESS: value.l = (jobject) oper->constant(); break;
  case T_METADATA: return add((Metadata*)oper->constant()); break;
  default: guarantee(false, "unhandled type: %s", type2name(type));
  }
  return add(n, type, value);
}

ConstantTable::Constant ConstantTable::add_jump_table(MachConstantNode* n) {
  jvalue value;
  // We can use the node pointer here to identify the right jump-table
  // as this method is called from Compile::Fill_buffer right before
  // the MachNodes are emitted and the jump-table is filled (means the
  // MachNode pointers do not change anymore).
  value.l = (jobject) n;
  Constant con(T_VOID, value, next_jump_table_freq(), false);  // Labels of a jump-table cannot be reused.
  add(con);
  return con;
}

void ConstantTable::fill_jump_table(CodeBuffer& cb, MachConstantNode* n, GrowableArray<Label*> labels) const {
  // If called from Compile::scratch_emit_size do nothing.
  if (Compile::current()->output()->in_scratch_emit_size())  return;

  assert(labels.is_nonempty(), "must be");
  assert((uint) labels.length() == n->outcnt(), "must be equal: %d == %d", labels.length(), n->outcnt());

  // Since MachConstantNode::constant_offset() also contains
  // table_base_offset() we need to subtract the table_base_offset()
  // to get the plain offset into the constant table.
  int offset = n->constant_offset() - table_base_offset();

  MacroAssembler _masm(&cb);
  address* jump_table_base = (address*) (_masm.code()->consts()->start() + offset);

  for (uint i = 0; i < n->outcnt(); i++) {
    address* constant_addr = &jump_table_base[i];
    assert(*constant_addr == (((address) n) + i), "all jump-table entries must contain adjusted node pointer: " INTPTR_FORMAT " == " INTPTR_FORMAT, p2i(*constant_addr), p2i(((address) n) + i));
    *constant_addr = cb.consts()->target(*labels.at(i), (address) constant_addr);
    cb.consts()->relocate((address) constant_addr, relocInfo::internal_word_type);
  }
}
