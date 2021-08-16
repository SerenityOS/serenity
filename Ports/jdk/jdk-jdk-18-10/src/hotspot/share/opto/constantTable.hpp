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

#ifndef SHARE_OPTO_CONSTANTTABLE_HPP
#define SHARE_OPTO_CONSTANTTABLE_HPP

#include "utilities/globalDefinitions.hpp"

class CodeBuffer;
class Metadata;
class MachConstantNode;
class MachOper;

class ConstantTable {
public:
  // Constant entry of the constant table.
  class Constant {
  private:
    BasicType _type;
    union {
      jvalue    _value;
      Metadata* _metadata;
    } _v;
    int       _offset;         // offset of this constant (in bytes) relative to the constant table base.
    float     _freq;
    bool      _can_be_reused;  // true (default) if the value can be shared with other users.

  public:
    Constant() : _type(T_ILLEGAL), _offset(-1), _freq(0.0f), _can_be_reused(true) { _v._value.l = 0; }
    Constant(BasicType type, jvalue value, float freq = 0.0f, bool can_be_reused = true) :
      _type(type),
      _offset(-1),
      _freq(freq),
      _can_be_reused(can_be_reused)
    {
      assert(type != T_METADATA, "wrong constructor");
      _v._value = value;
    }
    Constant(Metadata* metadata, bool can_be_reused = true) :
      _type(T_METADATA),
      _offset(-1),
      _freq(0.0f),
      _can_be_reused(can_be_reused)
    {
      _v._metadata = metadata;
    }

    bool operator==(const Constant& other);

    BasicType type()      const    { return _type; }

    jint    get_jint()    const    { return _v._value.i; }
    jlong   get_jlong()   const    { return _v._value.j; }
    jfloat  get_jfloat()  const    { return _v._value.f; }
    jdouble get_jdouble() const    { return _v._value.d; }
    jobject get_jobject() const    { return _v._value.l; }

    Metadata* get_metadata() const { return _v._metadata; }

    int         offset()  const    { return _offset; }
    void    set_offset(int offset) {        _offset = offset; }

    float       freq()    const    { return _freq;         }
    void    inc_freq(float freq)   {        _freq += freq; }

    bool    can_be_reused() const  { return _can_be_reused; }
  };

private:
  GrowableArray<Constant> _constants;          // Constants of this table.
  int                     _size;               // Size in bytes the emitted constant table takes (including padding).
  int                     _table_base_offset;  // Offset of the table base that gets added to the constant offsets.
  int                     _nof_jump_tables;    // Number of jump-tables in this constant table.

  static int qsort_comparator(Constant* a, Constant* b);

  // We use negative frequencies to keep the order of the
  // jump-tables in which they were added.  Otherwise we get into
  // trouble with relocation.
  float next_jump_table_freq() { return -1.0f * (++_nof_jump_tables); }

public:
  ConstantTable() :
    _size(-1),
    _table_base_offset(-1),  // We can use -1 here since the constant table is always bigger than 2 bytes (-(size / 2), see MachConstantBaseNode::emit).
    _nof_jump_tables(0)
  {}

  int size() const { assert(_size != -1, "not calculated yet"); return _size; }

  int calculate_table_base_offset() const;  // AD specific
  void set_table_base_offset(int x)  { assert(_table_base_offset == -1 || x == _table_base_offset, "can't change"); _table_base_offset = x; }
  int      table_base_offset() const { assert(_table_base_offset != -1, "not set yet");                      return _table_base_offset; }

  bool emit(CodeBuffer& cb) const;

  // Returns the offset of the last entry (the top) of the constant table.
  int  top_offset() const { assert(_constants.top().offset() != -1, "not bound yet"); return _constants.top().offset(); }

  void calculate_offsets_and_size();
  int  find_offset(Constant& con) const;

  void     add(Constant& con);
  Constant add(MachConstantNode* n, BasicType type, jvalue value);
  Constant add(Metadata* metadata);
  Constant add(MachConstantNode* n, MachOper* oper);
  Constant add(MachConstantNode* n, jint i) {
    jvalue value; value.i = i;
    return add(n, T_INT, value);
  }
  Constant add(MachConstantNode* n, jlong j) {
    jvalue value; value.j = j;
    return add(n, T_LONG, value);
  }
  Constant add(MachConstantNode* n, jfloat f) {
    jvalue value; value.f = f;
    return add(n, T_FLOAT, value);
  }
  Constant add(MachConstantNode* n, jdouble d) {
    jvalue value; value.d = d;
    return add(n, T_DOUBLE, value);
  }

  // Jump-table
  Constant  add_jump_table(MachConstantNode* n);
  void     fill_jump_table(CodeBuffer& cb, MachConstantNode* n, GrowableArray<Label*> labels) const;
};


#endif // SHARE_OPTO_CONSTANTTABLE_HPP
