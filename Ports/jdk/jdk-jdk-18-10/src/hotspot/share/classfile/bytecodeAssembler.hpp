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
 *
 */

#ifndef SHARE_CLASSFILE_BYTECODEASSEMBLER_HPP
#define SHARE_CLASSFILE_BYTECODEASSEMBLER_HPP

#include "memory/allocation.hpp"
#include "oops/method.hpp"
#include "oops/symbol.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/growableArray.hpp"
#include "utilities/resourceHash.hpp"


/**
 * Bytecode Assembler
 *
 * These classes are used to synthesize code for creating new methods from
 * within the VM.  This is only a partial implementation of an assembler;
 * only the bytecodes that are needed by clients are implemented at this time.
 * This is used during default method analysis to create overpass methods
 * and add them to a call during parsing.  Other uses (such as creating
 * bridges) may come later.  Any missing bytecodes can be implemented on an
 * as-need basis.
 */

class BytecodeBuffer : public GrowableArray<u1> {
 public:
  BytecodeBuffer() : GrowableArray<u1>(20) {}
};

// Entries in a yet-to-be-created constant pool.  Limited types for now.
class BytecodeCPEntry {
 public:
  enum tag {
    ERROR_TAG,
    UTF8,
    KLASS,
    STRING,
    NAME_AND_TYPE,
    METHODREF
  };

  u1 _tag;
  union {
    Symbol* utf8;
    u2 klass;
    u2 string;
    struct {
      u2 name_index;
      u2 type_index;
    } name_and_type;
    struct {
      u2 class_index;
      u2 name_and_type_index;
    } methodref;
    uintptr_t hash;
  } _u;

  BytecodeCPEntry() : _tag(ERROR_TAG) { _u.hash = 0; }
  BytecodeCPEntry(u1 tag) : _tag(tag) { _u.hash = 0; }

  static BytecodeCPEntry utf8(Symbol* symbol) {
    BytecodeCPEntry bcpe(UTF8);
    bcpe._u.utf8 = symbol;
    return bcpe;
  }

  static BytecodeCPEntry klass(u2 index) {
    BytecodeCPEntry bcpe(KLASS);
    bcpe._u.klass = index;
    return bcpe;
  }

  static BytecodeCPEntry string(u2 index) {
    BytecodeCPEntry bcpe(STRING);
    bcpe._u.string = index;
    return bcpe;
  }

  static BytecodeCPEntry name_and_type(u2 name, u2 type) {
    BytecodeCPEntry bcpe(NAME_AND_TYPE);
    bcpe._u.name_and_type.name_index = name;
    bcpe._u.name_and_type.type_index = type;
    return bcpe;
  }

  static BytecodeCPEntry methodref(u2 class_index, u2 nat) {
    BytecodeCPEntry bcpe(METHODREF);
    bcpe._u.methodref.class_index = class_index;
    bcpe._u.methodref.name_and_type_index = nat;
    return bcpe;
  }

  static bool equals(BytecodeCPEntry const& e0, BytecodeCPEntry const& e1) {
    return e0._tag == e1._tag && e0._u.hash == e1._u.hash;
  }

  static unsigned hash(BytecodeCPEntry const& e0) {
    return (unsigned)(e0._tag ^ e0._u.hash);
  }
};

class BytecodeConstantPool : ResourceObj {
 private:
  typedef ResourceHashtable<BytecodeCPEntry, u2,
      256, ResourceObj::RESOURCE_AREA, mtInternal,
      &BytecodeCPEntry::hash, &BytecodeCPEntry::equals> IndexHash;

  ConstantPool* _orig;
  GrowableArray<BytecodeCPEntry> _entries;
  IndexHash _indices;

  u2 find_or_add(BytecodeCPEntry const& bcpe);

 public:

  BytecodeConstantPool(ConstantPool* orig) : _orig(orig) {}

  BytecodeCPEntry const& at(u2 index) const { return _entries.at(index); }

  InstanceKlass* pool_holder() const {
    return _orig->pool_holder();
  }

  u2 utf8(Symbol* sym) {
    return find_or_add(BytecodeCPEntry::utf8(sym));
  }

  u2 klass(Symbol* class_name) {
    return find_or_add(BytecodeCPEntry::klass(utf8(class_name)));
  }

  u2 string(Symbol* str) {
    return find_or_add(BytecodeCPEntry::string(utf8(str)));
  }

  u2 name_and_type(Symbol* name, Symbol* sig) {
    return find_or_add(BytecodeCPEntry::name_and_type(utf8(name), utf8(sig)));
  }

  u2 methodref(Symbol* class_name, Symbol* name, Symbol* sig) {
    return find_or_add(BytecodeCPEntry::methodref(
        klass(class_name), name_and_type(name, sig)));
  }

  ConstantPool* create_constant_pool(TRAPS) const;
};

// Partial bytecode assembler - only what we need for creating
// overpass methods for default methods is implemented
class BytecodeAssembler : StackObj {
 private:
  BytecodeBuffer* _code;
  BytecodeConstantPool* _cp;

  void append(u1 imm_u1);
  void append(u2 imm_u2);
  void append(u4 imm_u4);

  void xload(u4 index, u1 quick, u1 twobyte);

 public:
  BytecodeAssembler(BytecodeBuffer* buffer, BytecodeConstantPool* cp)
    : _code(buffer), _cp(cp) {}

  void aload(u4 index);
  void areturn();
  void athrow();
  void checkcast(Symbol* sym);
  void dload(u4 index);
  void dreturn();
  void dup();
  void fload(u4 index);
  void freturn();
  void iload(u4 index);
  void invokespecial(Method* method);
  void invokespecial(Symbol* cls, Symbol* name, Symbol* sig);
  void invokevirtual(Method* method);
  void invokevirtual(Symbol* cls, Symbol* name, Symbol* sig);
  void ireturn();
  void ldc(u1 index);
  void ldc_w(u2 index);
  void lload(u4 index);
  void lreturn();
  void _new(Symbol* sym);
  void _return();

  void load_string(Symbol* sym);
  void load(BasicType bt, u4 index);
  void _return(BasicType bt);
};

#endif // SHARE_CLASSFILE_BYTECODEASSEMBLER_HPP
