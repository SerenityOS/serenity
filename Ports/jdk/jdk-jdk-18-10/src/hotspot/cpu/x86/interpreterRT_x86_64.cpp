/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "interpreter/interp_masm.hpp"
#include "interpreter/interpreter.hpp"
#include "interpreter/interpreterRuntime.hpp"
#include "memory/allocation.inline.hpp"
#include "oops/method.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/icache.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/signature.hpp"

#define __ _masm->

// Implementation of SignatureHandlerGenerator

InterpreterRuntime::SignatureHandlerGenerator::SignatureHandlerGenerator(const methodHandle& method, CodeBuffer* buffer) :
    NativeSignatureIterator(method) {
  _masm = new MacroAssembler(buffer);
#ifdef AMD64
#ifdef _WIN64
  _num_args = (method->is_static() ? 1 : 0);
  _stack_offset = (Argument::n_int_register_parameters_c+1)* wordSize; // don't overwrite return address
#else
  _num_int_args = (method->is_static() ? 1 : 0);
  _num_fp_args = 0;
  _stack_offset = wordSize; // don't overwrite return address
#endif // _WIN64
#endif // AMD64
}

Register InterpreterRuntime::SignatureHandlerGenerator::from() { return r14; }
Register InterpreterRuntime::SignatureHandlerGenerator::to()   { return rsp; }
Register InterpreterRuntime::SignatureHandlerGenerator::temp() { return rscratch1; }

void InterpreterRuntime::SignatureHandlerGenerator::pass_int() {
  const Address src(from(), Interpreter::local_offset_in_bytes(offset()));

#ifdef _WIN64
  switch (_num_args) {
  case 0:
    __ movl(c_rarg1, src);
    _num_args++;
    break;
  case 1:
    __ movl(c_rarg2, src);
    _num_args++;
    break;
  case 2:
    __ movl(c_rarg3, src);
    _num_args++;
    break;
  default:
    __ movl(rax, src);
    __ movl(Address(to(), _stack_offset), rax);
    _stack_offset += wordSize;
    break;
  }
#else
  switch (_num_int_args) {
  case 0:
    __ movl(c_rarg1, src);
    _num_int_args++;
    break;
  case 1:
    __ movl(c_rarg2, src);
    _num_int_args++;
    break;
  case 2:
    __ movl(c_rarg3, src);
    _num_int_args++;
    break;
  case 3:
    __ movl(c_rarg4, src);
    _num_int_args++;
    break;
  case 4:
    __ movl(c_rarg5, src);
    _num_int_args++;
    break;
  default:
    __ movl(rax, src);
    __ movl(Address(to(), _stack_offset), rax);
    _stack_offset += wordSize;
    break;
  }
#endif
}

void InterpreterRuntime::SignatureHandlerGenerator::pass_long() {
  const Address src(from(), Interpreter::local_offset_in_bytes(offset() + 1));

#ifdef _WIN64
  switch (_num_args) {
  case 0:
    __ movptr(c_rarg1, src);
    _num_args++;
    break;
  case 1:
    __ movptr(c_rarg2, src);
    _num_args++;
    break;
  case 2:
    __ movptr(c_rarg3, src);
    _num_args++;
    break;
  case 3:
  default:
    __ movptr(rax, src);
    __ movptr(Address(to(), _stack_offset), rax);
    _stack_offset += wordSize;
    break;
  }
#else
  switch (_num_int_args) {
  case 0:
    __ movptr(c_rarg1, src);
    _num_int_args++;
    break;
  case 1:
    __ movptr(c_rarg2, src);
    _num_int_args++;
    break;
  case 2:
    __ movptr(c_rarg3, src);
    _num_int_args++;
    break;
  case 3:
    __ movptr(c_rarg4, src);
    _num_int_args++;
    break;
  case 4:
    __ movptr(c_rarg5, src);
    _num_int_args++;
    break;
  default:
    __ movptr(rax, src);
    __ movptr(Address(to(), _stack_offset), rax);
    _stack_offset += wordSize;
    break;
  }
#endif
}

void InterpreterRuntime::SignatureHandlerGenerator::pass_float() {
  const Address src(from(), Interpreter::local_offset_in_bytes(offset()));

#ifdef _WIN64
  if (_num_args < Argument::n_float_register_parameters_c-1) {
    __ movflt(as_XMMRegister(++_num_args), src);
  } else {
    __ movl(rax, src);
    __ movl(Address(to(), _stack_offset), rax);
    _stack_offset += wordSize;
  }
#else
  if (_num_fp_args < Argument::n_float_register_parameters_c) {
    __ movflt(as_XMMRegister(_num_fp_args++), src);
  } else {
    __ movl(rax, src);
    __ movl(Address(to(), _stack_offset), rax);
    _stack_offset += wordSize;
  }
#endif
}

void InterpreterRuntime::SignatureHandlerGenerator::pass_double() {
  const Address src(from(), Interpreter::local_offset_in_bytes(offset() + 1));

#ifdef _WIN64
  if (_num_args < Argument::n_float_register_parameters_c-1) {
    __ movdbl(as_XMMRegister(++_num_args), src);
  } else {
    __ movptr(rax, src);
    __ movptr(Address(to(), _stack_offset), rax);
    _stack_offset += wordSize;
  }
#else
  if (_num_fp_args < Argument::n_float_register_parameters_c) {
    __ movdbl(as_XMMRegister(_num_fp_args++), src);
  } else {
    __ movptr(rax, src);
    __ movptr(Address(to(), _stack_offset), rax);
    _stack_offset += wordSize;
  }
#endif
}

void InterpreterRuntime::SignatureHandlerGenerator::pass_object() {
  const Address src(from(), Interpreter::local_offset_in_bytes(offset()));

#ifdef _WIN64
  switch (_num_args) {
  case 0:
    assert(offset() == 0, "argument register 1 can only be (non-null) receiver");
    __ lea(c_rarg1, src);
    _num_args++;
    break;
  case 1:
    __ lea(rax, src);
    __ xorl(c_rarg2, c_rarg2);
    __ cmpptr(src, 0);
    __ cmov(Assembler::notEqual, c_rarg2, rax);
    _num_args++;
    break;
  case 2:
    __ lea(rax, src);
    __ xorl(c_rarg3, c_rarg3);
    __ cmpptr(src, 0);
    __ cmov(Assembler::notEqual, c_rarg3, rax);
    _num_args++;
    break;
  default:
    __ lea(rax, src);
    __ xorl(temp(), temp());
    __ cmpptr(src, 0);
    __ cmov(Assembler::notEqual, temp(), rax);
    __ movptr(Address(to(), _stack_offset), temp());
    _stack_offset += wordSize;
    break;
  }
#else
  switch (_num_int_args) {
  case 0:
    assert(offset() == 0, "argument register 1 can only be (non-null) receiver");
    __ lea(c_rarg1, src);
    _num_int_args++;
    break;
  case 1:
    __ lea(rax, src);
    __ xorl(c_rarg2, c_rarg2);
    __ cmpptr(src, 0);
    __ cmov(Assembler::notEqual, c_rarg2, rax);
    _num_int_args++;
    break;
  case 2:
    __ lea(rax, src);
    __ xorl(c_rarg3, c_rarg3);
    __ cmpptr(src, 0);
    __ cmov(Assembler::notEqual, c_rarg3, rax);
    _num_int_args++;
    break;
  case 3:
    __ lea(rax, src);
    __ xorl(c_rarg4, c_rarg4);
    __ cmpptr(src, 0);
    __ cmov(Assembler::notEqual, c_rarg4, rax);
    _num_int_args++;
    break;
  case 4:
    __ lea(rax, src);
    __ xorl(c_rarg5, c_rarg5);
    __ cmpptr(src, 0);
    __ cmov(Assembler::notEqual, c_rarg5, rax);
    _num_int_args++;
    break;
  default:
    __ lea(rax, src);
    __ xorl(temp(), temp());
    __ cmpptr(src, 0);
    __ cmov(Assembler::notEqual, temp(), rax);
    __ movptr(Address(to(), _stack_offset), temp());
    _stack_offset += wordSize;
    break;
  }
#endif
}

void InterpreterRuntime::SignatureHandlerGenerator::generate(uint64_t fingerprint) {
  // generate code to handle arguments
  iterate(fingerprint);

  // return result handler
  __ lea(rax, ExternalAddress(Interpreter::result_handler(method()->result_type())));
  __ ret(0);

  __ flush();
}


// Implementation of SignatureHandlerLibrary

void SignatureHandlerLibrary::pd_set_handler(address handler) {}


#ifdef _WIN64
class SlowSignatureHandler
  : public NativeSignatureIterator {
 private:
  address   _from;
  intptr_t* _to;
  intptr_t* _reg_args;
  intptr_t* _fp_identifiers;
  unsigned int _num_args;

  virtual void pass_int()
  {
    jint from_obj = *(jint *)(_from+Interpreter::local_offset_in_bytes(0));
    _from -= Interpreter::stackElementSize;

    if (_num_args < Argument::n_int_register_parameters_c-1) {
      *_reg_args++ = from_obj;
      _num_args++;
    } else {
      *_to++ = from_obj;
    }
  }

  virtual void pass_long()
  {
    intptr_t from_obj = *(intptr_t*)(_from+Interpreter::local_offset_in_bytes(1));
    _from -= 2*Interpreter::stackElementSize;

    if (_num_args < Argument::n_int_register_parameters_c-1) {
      *_reg_args++ = from_obj;
      _num_args++;
    } else {
      *_to++ = from_obj;
    }
  }

  virtual void pass_object()
  {
    intptr_t *from_addr = (intptr_t*)(_from + Interpreter::local_offset_in_bytes(0));
    _from -= Interpreter::stackElementSize;
    if (_num_args < Argument::n_int_register_parameters_c-1) {
      *_reg_args++ = (*from_addr == 0) ? NULL : (intptr_t) from_addr;
      _num_args++;
    } else {
      *_to++ = (*from_addr == 0) ? NULL : (intptr_t) from_addr;
    }
  }

  virtual void pass_float()
  {
    jint from_obj = *(jint *)(_from+Interpreter::local_offset_in_bytes(0));
    _from -= Interpreter::stackElementSize;

    if (_num_args < Argument::n_float_register_parameters_c-1) {
      assert((_num_args*2) < BitsPerWord, "_num_args*2 is out of range");
      *_reg_args++ = from_obj;
      *_fp_identifiers |= ((intptr_t)0x01 << (_num_args*2)); // mark as float
      _num_args++;
    } else {
      *_to++ = from_obj;
    }
  }

  virtual void pass_double()
  {
    intptr_t from_obj = *(intptr_t*)(_from+Interpreter::local_offset_in_bytes(1));
    _from -= 2*Interpreter::stackElementSize;

    if (_num_args < Argument::n_float_register_parameters_c-1) {
      assert((_num_args*2) < BitsPerWord, "_num_args*2 is out of range");
      *_reg_args++ = from_obj;
      *_fp_identifiers |= ((intptr_t)0x3 << (_num_args*2)); // mark as double
      _num_args++;
    } else {
      *_to++ = from_obj;
    }
  }

 public:
  SlowSignatureHandler(const methodHandle& method, address from, intptr_t* to)
    : NativeSignatureIterator(method)
  {
    _from = from;
    _to   = to;

    _reg_args = to - (method->is_static() ? 4 : 5);
    _fp_identifiers = to - 2;
    _to = _to + 4;  // Windows reserves stack space for register arguments
    *(int*) _fp_identifiers = 0;
    _num_args = (method->is_static() ? 1 : 0);
  }
};
#else
class SlowSignatureHandler
  : public NativeSignatureIterator {
 private:
  address   _from;
  intptr_t* _to;
  intptr_t* _int_args;
  intptr_t* _fp_args;
  intptr_t* _fp_identifiers;
  unsigned int _num_int_args;
  unsigned int _num_fp_args;

  virtual void pass_int()
  {
    jint from_obj = *(jint *)(_from+Interpreter::local_offset_in_bytes(0));
    _from -= Interpreter::stackElementSize;

    if (_num_int_args < Argument::n_int_register_parameters_c-1) {
      *_int_args++ = from_obj;
      _num_int_args++;
    } else {
      *_to++ = from_obj;
    }
  }

  virtual void pass_long()
  {
    intptr_t from_obj = *(intptr_t*)(_from+Interpreter::local_offset_in_bytes(1));
    _from -= 2*Interpreter::stackElementSize;

    if (_num_int_args < Argument::n_int_register_parameters_c-1) {
      *_int_args++ = from_obj;
      _num_int_args++;
    } else {
      *_to++ = from_obj;
    }
  }

  virtual void pass_object()
  {
    intptr_t *from_addr = (intptr_t*)(_from + Interpreter::local_offset_in_bytes(0));
    _from -= Interpreter::stackElementSize;

    if (_num_int_args < Argument::n_int_register_parameters_c-1) {
      *_int_args++ = (*from_addr == 0) ? NULL : (intptr_t)from_addr;
      _num_int_args++;
    } else {
      *_to++ = (*from_addr == 0) ? NULL : (intptr_t) from_addr;
    }
  }

  virtual void pass_float()
  {
    jint from_obj = *(jint*)(_from+Interpreter::local_offset_in_bytes(0));
    _from -= Interpreter::stackElementSize;

    if (_num_fp_args < Argument::n_float_register_parameters_c) {
      *_fp_args++ = from_obj;
      _num_fp_args++;
    } else {
      *_to++ = from_obj;
    }
  }

  virtual void pass_double()
  {
    intptr_t from_obj = *(intptr_t*)(_from+Interpreter::local_offset_in_bytes(1));
    _from -= 2*Interpreter::stackElementSize;

    if (_num_fp_args < Argument::n_float_register_parameters_c) {
      *_fp_args++ = from_obj;
      *_fp_identifiers |= (1 << _num_fp_args); // mark as double
      _num_fp_args++;
    } else {
      *_to++ = from_obj;
    }
  }

 public:
  SlowSignatureHandler(const methodHandle& method, address from, intptr_t* to)
    : NativeSignatureIterator(method)
  {
    _from = from;
    _to   = to;

    _int_args = to - (method->is_static() ? 14 : 15);
    _fp_args =  to - 9;
    _fp_identifiers = to - 10;
    *(int*) _fp_identifiers = 0;
    _num_int_args = (method->is_static() ? 1 : 0);
    _num_fp_args = 0;
  }
};
#endif


JRT_ENTRY(address,
          InterpreterRuntime::slow_signature_handler(JavaThread* current,
                                                     Method* method,
                                                     intptr_t* from,
                                                     intptr_t* to))
  methodHandle m(current, (Method*)method);
  assert(m->is_native(), "sanity check");

  // handle arguments
  SlowSignatureHandler(m, (address)from, to + 1).iterate((uint64_t)CONST64(-1));

  // return result handler
  return Interpreter::result_handler(m->result_type());
JRT_END
