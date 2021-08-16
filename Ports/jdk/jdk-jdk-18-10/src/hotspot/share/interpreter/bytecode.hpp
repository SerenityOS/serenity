/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_INTERPRETER_BYTECODE_HPP
#define SHARE_INTERPRETER_BYTECODE_HPP

#include "interpreter/bytecodes.hpp"
#include "memory/allocation.hpp"
#include "oops/method.hpp"
#include "utilities/align.hpp"
#include "utilities/bytes.hpp"

class ciBytecodeStream;

// The base class for different kinds of bytecode abstractions.
// Provides the primitive operations to manipulate code relative
// to the bcp.

class Bytecode: public StackObj {
 protected:
  const address   _bcp;
  const Bytecodes::Code _code;

  // Address computation
  address addr_at            (int offset)        const     { return (address)_bcp + offset; }
  u_char byte_at(int offset) const               { return *addr_at(offset); }
  address aligned_addr_at    (int offset)        const     { return align_up(addr_at(offset), jintSize); }

  // Word access:
  int     get_Java_u2_at     (int offset)        const     { return Bytes::get_Java_u2(addr_at(offset)); }
  int     get_Java_u4_at     (int offset)        const     { return Bytes::get_Java_u4(addr_at(offset)); }
  int     get_aligned_Java_u4_at(int offset)     const     { return Bytes::get_Java_u4(aligned_addr_at(offset)); }
  int     get_native_u2_at   (int offset)        const     { return Bytes::get_native_u2(addr_at(offset)); }
  int     get_native_u4_at   (int offset)        const     { return Bytes::get_native_u4(addr_at(offset)); }

 public:
  Bytecode(Method* method, address bcp): _bcp(bcp), _code(Bytecodes::code_at(method, addr_at(0))) {
    assert(method != NULL, "this form requires a valid Method*");
  }
  // Defined in ciStreams.hpp
  inline Bytecode(const ciBytecodeStream* stream, address bcp = NULL);

  // Attributes
  address bcp() const                            { return _bcp; }
  int instruction_size() const                   { return Bytecodes::length_for_code_at(_code, bcp()); }

  Bytecodes::Code code() const                   { return _code; }
  Bytecodes::Code java_code() const              { return Bytecodes::java_code(code()); }
  Bytecodes::Code invoke_code() const            { return (code() == Bytecodes::_invokehandle) ? code() : java_code(); }

  // Static functions for parsing bytecodes in place.
  int get_index_u1(Bytecodes::Code bc) const {
    assert_same_format_as(bc); assert_index_size(1, bc);
    return *(jubyte*)addr_at(1);
  }
  int get_index_u2(Bytecodes::Code bc, bool is_wide = false) const {
    assert_same_format_as(bc, is_wide); assert_index_size(2, bc, is_wide);
    address p = addr_at(is_wide ? 2 : 1);
    if (can_use_native_byte_order(bc, is_wide))
      return Bytes::get_native_u2(p);
    else  return Bytes::get_Java_u2(p);
  }
  int get_index_u1_cpcache(Bytecodes::Code bc) const {
    assert_same_format_as(bc); assert_index_size(1, bc);
    return *(jubyte*)addr_at(1) + ConstantPool::CPCACHE_INDEX_TAG;
  }
  int get_index_u2_cpcache(Bytecodes::Code bc) const {
    assert_same_format_as(bc); assert_index_size(2, bc); assert_native_index(bc);
    return Bytes::get_native_u2(addr_at(1)) + ConstantPool::CPCACHE_INDEX_TAG;
  }
  int get_index_u4(Bytecodes::Code bc) const {
    assert_same_format_as(bc); assert_index_size(4, bc);
    assert(can_use_native_byte_order(bc), "");
    return Bytes::get_native_u4(addr_at(1));
  }
  bool has_index_u4(Bytecodes::Code bc) const {
    return bc == Bytecodes::_invokedynamic;
  }

  int get_offset_s2(Bytecodes::Code bc) const {
    assert_same_format_as(bc); assert_offset_size(2, bc);
    return (jshort) Bytes::get_Java_u2(addr_at(1));
  }
  int get_offset_s4(Bytecodes::Code bc) const {
    assert_same_format_as(bc); assert_offset_size(4, bc);
    return (jint) Bytes::get_Java_u4(addr_at(1));
  }

  int get_constant_u1(int offset, Bytecodes::Code bc) const {
    assert_same_format_as(bc); assert_constant_size(1, offset, bc);
    return *(jbyte*)addr_at(offset);
  }
  int get_constant_u2(int offset, Bytecodes::Code bc, bool is_wide = false) const {
    assert_same_format_as(bc, is_wide); assert_constant_size(2, offset, bc, is_wide);
    return (jshort) Bytes::get_Java_u2(addr_at(offset));
  }

  // These are used locally and also from bytecode streams.
  void assert_same_format_as(Bytecodes::Code testbc, bool is_wide = false) const NOT_DEBUG_RETURN;
  static void assert_index_size(int required_size, Bytecodes::Code bc, bool is_wide = false) NOT_DEBUG_RETURN;
  static void assert_offset_size(int required_size, Bytecodes::Code bc, bool is_wide = false) NOT_DEBUG_RETURN;
  static void assert_constant_size(int required_size, int where, Bytecodes::Code bc, bool is_wide = false) NOT_DEBUG_RETURN;
  static void assert_native_index(Bytecodes::Code bc, bool is_wide = false) NOT_DEBUG_RETURN;
  static bool can_use_native_byte_order(Bytecodes::Code bc, bool is_wide = false) {
    return (!Endian::is_Java_byte_ordering_different() || Bytecodes::native_byte_order(bc /*, is_wide*/));
  }
};


// Abstractions for lookupswitch bytecode
class LookupswitchPair {
 private:
  const address _bcp;

  address addr_at            (int offset)        const     { return _bcp + offset; }
  int     get_Java_u4_at     (int offset)        const     { return Bytes::get_Java_u4(addr_at(offset)); }

 public:
  LookupswitchPair(address bcp): _bcp(bcp) {}
  int  match() const                             { return get_Java_u4_at(0 * jintSize); }
  int  offset() const                            { return get_Java_u4_at(1 * jintSize); }
};


class Bytecode_lookupswitch: public Bytecode {
 public:
  Bytecode_lookupswitch(Method* method, address bcp): Bytecode(method, bcp) { verify(); }
  // Defined in ciStreams.hpp
  inline Bytecode_lookupswitch(const ciBytecodeStream* stream);
  void verify() const PRODUCT_RETURN;

  // Attributes
  int  default_offset() const                    { return get_aligned_Java_u4_at(1 + 0*jintSize); }
  int  number_of_pairs() const                   { return get_aligned_Java_u4_at(1 + 1*jintSize); }
  LookupswitchPair pair_at(int i) const          {
    assert(0 <= i && i < number_of_pairs(), "pair index out of bounds");
    return LookupswitchPair(aligned_addr_at(1 + (1 + i)*2*jintSize));
  }
};

class Bytecode_tableswitch: public Bytecode {
 public:
  Bytecode_tableswitch(Method* method, address bcp): Bytecode(method, bcp) { verify(); }
  // Defined in ciStreams.hpp
  inline Bytecode_tableswitch(const ciBytecodeStream* stream);
  void verify() const PRODUCT_RETURN;

  // Attributes
  int  default_offset() const                    { return get_aligned_Java_u4_at(1 + 0*jintSize); }
  int  low_key() const                           { return get_aligned_Java_u4_at(1 + 1*jintSize); }
  int  high_key() const                          { return get_aligned_Java_u4_at(1 + 2*jintSize); }
  int  dest_offset_at(int i) const;
  int  length()                                  { return high_key()-low_key()+1; }
};

// Common code for decoding invokes and field references.

class Bytecode_member_ref: public Bytecode {
 protected:
  const Method* _method;                          // method containing the bytecode

  Bytecode_member_ref(const methodHandle& method, int bci)  : Bytecode(method(), method()->bcp_from(bci)), _method(method()) {}

  const Method* method() const                 { return _method; }
  ConstantPool* constants() const              { return _method->constants(); }
  ConstantPoolCache* cpcache() const           { return _method->constants()->cache(); }
  ConstantPoolCacheEntry* cpcache_entry() const;

 public:
  int          index() const;                    // cache index (loaded from instruction)
  int          pool_index() const;               // constant pool index
  Symbol*      klass() const;                    // returns the klass of the method or field
  Symbol*      name() const;                     // returns the name of the method or field
  Symbol*      signature() const;                // returns the signature of the method or field

  BasicType    result_type() const;              // returns the result type of the getfield or invoke
};

// Abstraction for invoke_{virtual, static, interface, special, dynamic, handle}

class Bytecode_invoke: public Bytecode_member_ref {
 protected:
  // Constructor that skips verification
  Bytecode_invoke(const methodHandle& method, int bci, bool unused)  : Bytecode_member_ref(method, bci) {}

 public:
  Bytecode_invoke(const methodHandle& method, int bci)  : Bytecode_member_ref(method, bci) { verify(); }
  void verify() const;

  // Attributes
  Method* static_target(TRAPS);                  // "specified" method   (from constant pool)

  // Testers
  bool is_invokeinterface() const                { return invoke_code() == Bytecodes::_invokeinterface; }
  bool is_invokevirtual() const                  { return invoke_code() == Bytecodes::_invokevirtual; }
  bool is_invokestatic() const                   { return invoke_code() == Bytecodes::_invokestatic; }
  bool is_invokespecial() const                  { return invoke_code() == Bytecodes::_invokespecial; }
  bool is_invokedynamic() const                  { return invoke_code() == Bytecodes::_invokedynamic; }
  bool is_invokehandle() const                   { return invoke_code() == Bytecodes::_invokehandle; }

  bool has_receiver() const                      { return !is_invokestatic() && !is_invokedynamic(); }

  bool is_valid() const                          { return is_invokeinterface() ||
                                                          is_invokevirtual()   ||
                                                          is_invokestatic()    ||
                                                          is_invokespecial()   ||
                                                          is_invokedynamic()   ||
                                                          is_invokehandle(); }

  bool has_appendix();

  int size_of_parameters() const;

 private:
  // Helper to skip verification.   Used is_valid() to check if the result is really an invoke
  inline friend Bytecode_invoke Bytecode_invoke_check(const methodHandle& method, int bci);
};

inline Bytecode_invoke Bytecode_invoke_check(const methodHandle& method, int bci) {
  return Bytecode_invoke(method, bci, false);
}


// Abstraction for all field accesses (put/get field/static)
class Bytecode_field: public Bytecode_member_ref {
 public:
  Bytecode_field(const methodHandle& method, int bci)  : Bytecode_member_ref(method, bci) { verify(); }

  // Testers
  bool is_getfield() const                       { return java_code() == Bytecodes::_getfield; }
  bool is_putfield() const                       { return java_code() == Bytecodes::_putfield; }
  bool is_getstatic() const                      { return java_code() == Bytecodes::_getstatic; }
  bool is_putstatic() const                      { return java_code() == Bytecodes::_putstatic; }

  bool is_getter() const                         { return is_getfield()  || is_getstatic(); }
  bool is_static() const                         { return is_getstatic() || is_putstatic(); }

  bool is_valid() const                          { return is_getfield()   ||
                                                          is_putfield()   ||
                                                          is_getstatic()  ||
                                                          is_putstatic(); }
  void verify() const;
};

// Abstraction for checkcast
class Bytecode_checkcast: public Bytecode {
 public:
  Bytecode_checkcast(Method* method, address bcp): Bytecode(method, bcp) { verify(); }
  void verify() const { assert(Bytecodes::java_code(code()) == Bytecodes::_checkcast, "check checkcast"); }

  // Returns index
  long index() const   { return get_index_u2(Bytecodes::_checkcast); };
};

// Abstraction for instanceof
class Bytecode_instanceof: public Bytecode {
 public:
  Bytecode_instanceof(Method* method, address bcp): Bytecode(method, bcp) { verify(); }
  void verify() const { assert(code() == Bytecodes::_instanceof, "check instanceof"); }

  // Returns index
  long index() const   { return get_index_u2(Bytecodes::_instanceof); };
};

class Bytecode_new: public Bytecode {
 public:
  Bytecode_new(Method* method, address bcp): Bytecode(method, bcp) { verify(); }
  void verify() const { assert(java_code() == Bytecodes::_new, "check new"); }

  // Returns index
  long index() const   { return get_index_u2(Bytecodes::_new); };
};

class Bytecode_multianewarray: public Bytecode {
 public:
  Bytecode_multianewarray(Method* method, address bcp): Bytecode(method, bcp) { verify(); }
  void verify() const { assert(java_code() == Bytecodes::_multianewarray, "check new"); }

  // Returns index
  long index() const   { return get_index_u2(Bytecodes::_multianewarray); };
};

class Bytecode_anewarray: public Bytecode {
 public:
  Bytecode_anewarray(Method* method, address bcp): Bytecode(method, bcp) { verify(); }
  void verify() const { assert(java_code() == Bytecodes::_anewarray, "check anewarray"); }

  // Returns index
  long index() const   { return get_index_u2(Bytecodes::_anewarray); };
};

// Abstraction for ldc, ldc_w and ldc2_w
class Bytecode_loadconstant: public Bytecode {
 private:
  const Method* _method;

  int raw_index() const;

 public:
  Bytecode_loadconstant(const methodHandle& method, int bci): Bytecode(method(), method->bcp_from(bci)), _method(method()) { verify(); }

  void verify() const {
    assert(_method != NULL, "must supply method");
    Bytecodes::Code stdc = Bytecodes::java_code(code());
    assert(stdc == Bytecodes::_ldc ||
           stdc == Bytecodes::_ldc_w ||
           stdc == Bytecodes::_ldc2_w, "load constant");
  }

  // Only non-standard bytecodes (fast_aldc) have reference cache indexes.
  bool has_cache_index() const { return code() >= Bytecodes::number_of_java_codes; }

  int pool_index() const;               // index into constant pool
  int cache_index() const {             // index into reference cache (or -1 if none)
    return has_cache_index() ? raw_index() : -1;
  }

  BasicType result_type() const;        // returns the result type of the ldc

  oop resolve_constant(TRAPS) const;
};

#endif // SHARE_INTERPRETER_BYTECODE_HPP
