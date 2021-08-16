/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CI_CISTREAMS_HPP
#define SHARE_CI_CISTREAMS_HPP

#include "ci/ciClassList.hpp"
#include "ci/ciExceptionHandler.hpp"
#include "ci/ciInstanceKlass.hpp"
#include "ci/ciMethod.hpp"
#include "interpreter/bytecode.hpp"

// ciBytecodeStream
//
// The class is used to iterate over the bytecodes of a method.
// It hides the details of constant pool structure/access by
// providing accessors for constant pool items.  It returns only pure
// Java bytecodes; VM-internal _fast bytecodes are translated back to
// their original form during iteration.
class ciBytecodeStream : StackObj {
private:
  // Handling for the weird bytecodes
  Bytecodes::Code next_wide_or_table(Bytecodes::Code); // Handle _wide & complicated inline table

  static Bytecodes::Code check_java(Bytecodes::Code c) {
    assert(Bytecodes::is_java_code(c), "should not return _fast bytecodes");
    return c;
  }

  static Bytecodes::Code check_defined(Bytecodes::Code c) {
    assert(Bytecodes::is_defined(c), "");
    return c;
  }

  ciMethod* _method;           // the method
  ciInstanceKlass* _holder;
  address _bc_start;            // Start of current bytecode for table
  address _was_wide;            // Address past last wide bytecode
  jint* _table_base;            // Aligned start of last table or switch

  address _start;                  // Start of bytecodes
  address _end;                    // Past end of bytecodes
  address _pc;                     // Current PC
  Bytecodes::Code _bc;             // Current bytecode
  Bytecodes::Code _raw_bc;         // Current bytecode, raw form

  void reset( address base, unsigned int size ) {
    _bc_start =_was_wide = 0;
    _start = _pc = base; _end = base + size;
  }

  Bytecode bytecode() const { return Bytecode(this, _bc_start); }
  Bytecode next_bytecode() const { return Bytecode(this, _pc); }

public:
  // End-Of-Bytecodes
  static Bytecodes::Code EOBC() {
    return Bytecodes::_illegal;
  }

  ciBytecodeStream(ciMethod* m) {
    reset_to_method(m);
  }

  ciBytecodeStream() {
    reset_to_method(NULL);
  }

  ciMethod* method() const { return _method; }

  void reset_to_method(ciMethod* m) {
    _method = m;
    if (m == NULL) {
      _holder = NULL;
      reset(NULL, 0);
    } else {
      _holder = m->holder();
      reset(m->code(), m->code_size());
    }
  }

  void reset_to_bci( int bci );

  // Force the iterator to report a certain bci.
  void force_bci(int bci);

  void set_max_bci( int max ) {
    _end = _start + max;
  }

  address cur_bcp() const       { return _bc_start; }  // Returns bcp to current instruction
  int next_bci() const          { return _pc - _start; }
  int cur_bci() const           { return _bc_start - _start; }
  int instruction_size() const  { return _pc - _bc_start; }

  Bytecodes::Code cur_bc() const{ return check_java(_bc); }
  Bytecodes::Code cur_bc_raw() const { return check_defined(_raw_bc); }
  Bytecodes::Code next_bc()     { return Bytecodes::java_code((Bytecodes::Code)* _pc); }

  // Return current ByteCode and increment PC to next bytecode, skipping all
  // intermediate constants.  Returns EOBC at end.
  // Expected usage:
  //     ciBytecodeStream iter(m);
  //     while (iter.next() != ciBytecodeStream::EOBC()) { ... }
  Bytecodes::Code next() {
    _bc_start = _pc;                        // Capture start of bc
    if( _pc >= _end ) return EOBC();        // End-Of-Bytecodes

    // Fetch Java bytecode
    // All rewritten bytecodes maintain the size of original bytecode.
    _bc = Bytecodes::java_code(_raw_bc = (Bytecodes::Code)*_pc);
    int csize = Bytecodes::length_for(_bc); // Expected size
    _pc += csize;                           // Bump PC past bytecode
    if (csize == 0) {
      _bc = next_wide_or_table(_bc);
    }
    return check_java(_bc);
  }

  bool is_wide() const { return ( _pc == _was_wide ); }

  // Does this instruction contain an index which refes into the CP cache?
  bool has_cache_index() const { return Bytecodes::uses_cp_cache(cur_bc_raw()); }

  int get_index_u1() const {
    return bytecode().get_index_u1(cur_bc_raw());
  }

  // Get a byte index following this bytecode.
  // If prefixed with a wide bytecode, get a wide index.
  int get_index() const {
    assert(!has_cache_index(), "else use cpcache variant");
    return (_pc == _was_wide)   // was widened?
      ? get_index_u2(true)      // yes, return wide index
      : get_index_u1();         // no, return narrow index
  }

  // Get 2-byte index (byte swapping depending on which bytecode)
  int get_index_u2(bool is_wide = false) const {
    return bytecode().get_index_u2(cur_bc_raw(), is_wide);
  }

  // Get 2-byte index in native byte order.  (Rewriter::rewrite makes these.)
  int get_index_u2_cpcache() const {
    return bytecode().get_index_u2_cpcache(cur_bc_raw());
  }

  // Get 4-byte index, for invokedynamic.
  int get_index_u4() const {
    return bytecode().get_index_u4(cur_bc_raw());
  }

  bool has_index_u4() const {
    return bytecode().has_index_u4(cur_bc_raw());
  }

  // Get dimensions byte (multinewarray)
  int get_dimensions() const { return *(unsigned char*)(_pc-1); }

  // Sign-extended index byte/short, no widening
  int get_constant_u1()                     const { return bytecode().get_constant_u1(instruction_size()-1, cur_bc_raw()); }
  int get_constant_u2(bool is_wide = false) const { return bytecode().get_constant_u2(instruction_size()-2, cur_bc_raw(), is_wide); }

  // Get a byte signed constant for "iinc".  Invalid for other bytecodes.
  // If prefixed with a wide bytecode, get a wide constant
  int get_iinc_con() const {return (_pc==_was_wide) ? (jshort) get_constant_u2(true) : (jbyte) get_constant_u1();}

  // 2-byte branch offset from current pc
  int get_dest() const {
    return cur_bci() + bytecode().get_offset_s2(cur_bc_raw());
  }

  // 2-byte branch offset from next pc
  int next_get_dest() const {
    assert(_pc < _end, "");
    return next_bci() + next_bytecode().get_offset_s2(Bytecodes::_ifeq);
  }

  // 4-byte branch offset from current pc
  int get_far_dest() const {
    return cur_bci() + bytecode().get_offset_s4(cur_bc_raw());
  }

  // For a lookup or switch table, return target destination
  int get_int_table( int index ) const {
    return Bytes::get_Java_u4((address)&_table_base[index]); }

  int get_dest_table( int index ) const {
    return cur_bci() + get_int_table(index); }

  // --- Constant pool access ---
  int get_constant_raw_index() const;
  int get_constant_pool_index() const;
  int get_field_index();
  int get_method_index();

  // If this bytecode is a new, newarray, multianewarray, instanceof,
  // or checkcast, get the referenced klass.
  ciKlass* get_klass(bool& will_link);
  int get_klass_index() const;

  // If this bytecode is one of the ldc variants, get the referenced
  // constant.  Do not attempt to resolve it, since that would require
  // execution of Java code.  If it is not resolved, return an unloaded
  // object (ciConstant.as_object()->is_loaded() == false).
  ciConstant get_constant();
  constantTag get_constant_pool_tag(int index) const;

  // True if the klass-using bytecode points to an unresolved klass
  bool is_unresolved_klass() const {
    constantTag tag = get_constant_pool_tag(get_klass_index());
    return tag.is_unresolved_klass();
  }

  bool is_unresolved_klass_in_error() const {
    constantTag tag = get_constant_pool_tag(get_klass_index());
    return tag.is_unresolved_klass_in_error();
  }

  // If this bytecode is one of get_field, get_static, put_field,
  // or put_static, get the referenced field.
  ciField* get_field(bool& will_link);

  ciInstanceKlass* get_declared_field_holder();
  int      get_field_holder_index();

  ciMethod*     get_method(bool& will_link, ciSignature* *declared_signature_result);
  bool          has_appendix();
  ciObject*     get_appendix();
  bool          has_local_signature();
  ciKlass*      get_declared_method_holder();
  int           get_method_holder_index();
  int           get_method_signature_index(const constantPoolHandle& cpool);

};


// ciSignatureStream
//
// The class is used to iterate over the elements of a method signature.
class ciSignatureStream : public StackObj {
private:
  ciSignature* _sig;
  int          _pos;
  // holder is a method's holder
  ciKlass*     _holder;
public:
  ciSignatureStream(ciSignature* signature, ciKlass* holder = NULL) {
    _sig = signature;
    _pos = 0;
    _holder = holder;
  }

  bool at_return_type() { return _pos == _sig->count(); }

  bool is_done() { return _pos > _sig->count(); }

  void next() {
    if (_pos <= _sig->count()) {
      _pos++;
    }
  }

  ciType* type() {
    if (at_return_type()) {
      return _sig->return_type();
    } else {
      return _sig->type_at(_pos);
    }
  }

  // next klass in the signature
  ciKlass* next_klass() {
    ciKlass* sig_k;
    if (_holder != NULL) {
      sig_k = _holder;
      _holder = NULL;
    } else {
      while (!type()->is_klass()) {
        next();
      }
      assert(!at_return_type(), "passed end of signature");
      sig_k = type()->as_klass();
      next();
    }
    return sig_k;
  }
};


// ciExceptionHandlerStream
//
// The class is used to iterate over the exception handlers of
// a method.
class ciExceptionHandlerStream : public StackObj {
private:
  // The method whose handlers we are traversing
  ciMethod* _method;

  // Our current position in the list of handlers
  int        _pos;
  int        _end;

  ciInstanceKlass*  _exception_klass;
  int        _bci;
  bool       _is_exact;

public:
  ciExceptionHandlerStream(ciMethod* method) {
    _method = method;

    // Force loading of method code and handlers.
    _method->code();

    _pos = 0;
    _end = _method->_handler_count;
    _exception_klass = NULL;
    _bci    = -1;
    _is_exact = false;
  }

  ciExceptionHandlerStream(ciMethod* method, int bci,
                           ciInstanceKlass* exception_klass = NULL,
                           bool is_exact = false) {
    _method = method;

    // Force loading of method code and handlers.
    _method->code();

    _pos = -1;
    _end = _method->_handler_count + 1; // include the rethrow handler
    _exception_klass = (exception_klass != NULL && exception_klass->is_loaded()
                          ? exception_klass
                          : NULL);
    _bci = bci;
    assert(_bci >= 0, "bci out of range");
    _is_exact = is_exact;
    next();
  }

  // These methods are currently implemented in an odd way.
  // Count the number of handlers the iterator has ever produced
  // or will ever produce.  Do not include the final rethrow handler.
  // That is, a trivial exception handler stream will have a count
  // of zero and produce just the rethrow handler.
  int count();

  // Count the number of handlers this stream will produce from now on.
  // Include the current handler, and the final rethrow handler.
  // The remaining count will be zero iff is_done() is true,
  int count_remaining();

  bool is_done() {
    return (_pos >= _end);
  }

  void next() {
    _pos++;
    if (_bci != -1) {
      // We are not iterating over all handlers...
      while (!is_done()) {
        ciExceptionHandler* handler = _method->_exception_handlers[_pos];
        if (handler->is_in_range(_bci)) {
          if (handler->is_catch_all()) {
            // Found final active catch block.
            _end = _pos+1;
            return;
          } else if (_exception_klass == NULL || !handler->catch_klass()->is_loaded()) {
            // We cannot do any type analysis here.  Must conservatively assume
            // catch block is reachable.
            return;
          } else if (_exception_klass->is_subtype_of(handler->catch_klass())) {
            // This catch clause will definitely catch the exception.
            // Final candidate.
            _end = _pos+1;
            return;
          } else if (!_is_exact &&
                     handler->catch_klass()->is_subtype_of(_exception_klass)) {
            // This catch block may be reachable.
            return;
          }
        }

        // The catch block was not pertinent.  Go on.
        _pos++;
      }
    } else {
      // This is an iteration over all handlers.
      return;
    }
  }

  ciExceptionHandler* handler() {
    return _method->_exception_handlers[_pos];
  }
};



// Implementation for declarations in bytecode.hpp
Bytecode::Bytecode(const ciBytecodeStream* stream, address bcp): _bcp(bcp != NULL ? bcp : stream->cur_bcp()), _code(Bytecodes::code_at(NULL, addr_at(0))) {}
Bytecode_lookupswitch::Bytecode_lookupswitch(const ciBytecodeStream* stream): Bytecode(stream) { verify(); }
Bytecode_tableswitch::Bytecode_tableswitch(const ciBytecodeStream* stream): Bytecode(stream) { verify(); }

#endif // SHARE_CI_CISTREAMS_HPP
