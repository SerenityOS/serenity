/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

void InterpreterRuntime::SignatureHandlerGenerator::pass_int() {
  move(offset(), jni_offset() + 1);
}

void InterpreterRuntime::SignatureHandlerGenerator::pass_float() {
  move(offset(), jni_offset() + 1);
}

void InterpreterRuntime::SignatureHandlerGenerator::pass_long() {
   move(offset(), jni_offset() + 2);
   move(offset() + 1, jni_offset() + 1);
}

void InterpreterRuntime::SignatureHandlerGenerator::pass_object() {
  box (offset(), jni_offset() + 1);
}

void InterpreterRuntime::SignatureHandlerGenerator::move(int from_offset, int to_offset) {
  __ movl(temp(), Address(from(), Interpreter::local_offset_in_bytes(from_offset)));
  __ movl(Address(to(), to_offset * wordSize), temp());
}


void InterpreterRuntime::SignatureHandlerGenerator::box(int from_offset, int to_offset) {
  __ lea(temp(), Address(from(), Interpreter::local_offset_in_bytes(from_offset)));
  __ cmpptr(Address(from(), Interpreter::local_offset_in_bytes(from_offset)), (int32_t)NULL_WORD); // do not use temp() to avoid AGI
  Label L;
  __ jcc(Assembler::notZero, L);
  __ movptr(temp(), NULL_WORD);
  __ bind(L);
  __ movptr(Address(to(), to_offset * wordSize), temp());
}


void InterpreterRuntime::SignatureHandlerGenerator::generate( uint64_t fingerprint) {
  // generate code to handle arguments
  iterate(fingerprint);
  // return result handler
  __ lea(rax,
         ExternalAddress((address)Interpreter::result_handler(method()->result_type())));
  // return
  __ ret(0);
  __ flush();
}


Register InterpreterRuntime::SignatureHandlerGenerator::from()       { return rdi; }
Register InterpreterRuntime::SignatureHandlerGenerator::to()         { return rsp; }
Register InterpreterRuntime::SignatureHandlerGenerator::temp()       { return rcx; }


// Implementation of SignatureHandlerLibrary

void SignatureHandlerLibrary::pd_set_handler(address handler) {}

class SlowSignatureHandler: public NativeSignatureIterator {
 private:
  address   _from;
  intptr_t* _to;

  virtual void pass_int() {
    *_to++ = *(jint *)(_from+Interpreter::local_offset_in_bytes(0));
    _from -= Interpreter::stackElementSize;
  }

  virtual void pass_float() {
    *_to++ = *(jint *)(_from+Interpreter::local_offset_in_bytes(0));
    _from -= Interpreter::stackElementSize;
  }

  virtual void pass_long() {
    _to[0] = *(intptr_t*)(_from+Interpreter::local_offset_in_bytes(1));
    _to[1] = *(intptr_t*)(_from+Interpreter::local_offset_in_bytes(0));
    _to += 2;
    _from -= 2*Interpreter::stackElementSize;
  }

  virtual void pass_object() {
    // pass address of from
    intptr_t from_addr = (intptr_t)(_from + Interpreter::local_offset_in_bytes(0));
    *_to++ = (*(intptr_t*)from_addr == 0) ? NULL_WORD : from_addr;
    _from -= Interpreter::stackElementSize;
   }

 public:
  SlowSignatureHandler(const methodHandle& method, address from, intptr_t* to) :
    NativeSignatureIterator(method) {
    _from = from;
    _to   = to + (is_static() ? 2 : 1);
  }
};

JRT_ENTRY(address, InterpreterRuntime::slow_signature_handler(JavaThread* current, Method* method, intptr_t* from, intptr_t* to))
  methodHandle m(current, (Method*)method);
  assert(m->is_native(), "sanity check");
  // handle arguments
  SlowSignatureHandler(m, (address)from, to + 1).iterate((uint64_t)CONST64(-1));
  // return result handler
  return Interpreter::result_handler(m->result_type());
JRT_END
