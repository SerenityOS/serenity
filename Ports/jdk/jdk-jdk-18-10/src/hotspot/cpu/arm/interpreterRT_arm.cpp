/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "asm/macroAssembler.inline.hpp"
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

InterpreterRuntime::SignatureHandlerGenerator::SignatureHandlerGenerator(
    const methodHandle& method, CodeBuffer* buffer) : NativeSignatureIterator(method) {
  _masm = new MacroAssembler(buffer);
  _abi_offset = 0;
  _ireg = is_static() ? 2 : 1;
#ifdef __ABI_HARD__
  _fp_slot = 0;
  _single_fpr_slot = 0;
#endif
}

#ifdef SHARING_FAST_NATIVE_FINGERPRINTS
// mapping from SignatureIterator param to (common) type of parsing
static const BasicType shared_type[] = {
  T_INT,    // bool
  T_INT,    // char
#ifndef __ABI_HARD__
  T_INT,    // float, passed as int
  T_LONG,   // double, passed as long
#else
  T_FLOAT,  // float
  T_DOUBLE, // double
#endif
  T_INT,    // byte
  T_INT,    // short
  T_INT,    // int
  T_LONG,   // long
  T_OBJECT, // obj
  T_OBJECT, // array
};

uint64_t InterpreterRuntime::normalize_fast_native_fingerprint(uint64_t fingerprint) {
  if (fingerprint == UCONST64(-1)) {
    // special signature used when the argument list cannot be encoded in a 64 bits value
    return fingerprint;
  }
  int shift = SignatureIterator::fp_static_feature_size;
  SignatureIterator::fingerprint_t result = fingerprint & ((1 << shift) - 1);

  BasicType ret_type = SignatureIterator::fp_return_type(fingerprint);
  // For ARM, the fast signature handler only needs to know whether
  // the return value must be unboxed. T_OBJECT and T_ARRAY need not
  // be distinguished from each other and all other return values
  // behave like integers with respect to the handler except T_BOOLEAN
  // which must be mapped to the range 0..1.
  if (is_reference_type(ret_type)) {
    ret_type = T_OBJECT;
  } else if (ret_type != T_BOOLEAN) {
    ret_type = T_INT;
  }
  result |= ((SignatureIterator::fingerprint_t) ret_type) << shift;
  shift += SignatureIterator::fp_result_feature_size;

  SignatureIterator::fingerprint_t unaccumulator = SignatureIterator::fp_start_parameters(fingerprint);
  while (true) {
    BasicType type = SignatureIterator::fp_next_parameter(unaccumulator);
    if (type == (BasicType)SignatureIterator::fp_parameters_done) {
      return result;
    }
    assert(SignatureIterator::fp_is_valid_type(type), "garbled fingerprint");
    BasicType shared = shared_type[type - T_BOOLEAN];
    result |= ((SignatureIterator::fingerprint_t) shared) << shift;
    shift += SignatureIterator::fp_parameter_feature_size;
  }
}
#endif // SHARING_FAST_NATIVE_FINGERPRINTS

// Implementation of SignatureHandlerGenerator
void InterpreterRuntime::SignatureHandlerGenerator::pass_int() {
  if (_ireg < GPR_PARAMS) {
    Register dst = as_Register(_ireg);
    __ ldr_s32(dst, Address(Rlocals, Interpreter::local_offset_in_bytes(offset())));
    _ireg++;
  } else {
    __ ldr_s32(Rtemp, Address(Rlocals, Interpreter::local_offset_in_bytes(offset())));
    __ str_32(Rtemp, Address(SP, _abi_offset * wordSize));
    _abi_offset++;
  }
}

void InterpreterRuntime::SignatureHandlerGenerator::pass_long() {
  if (_ireg <= 2) {
#if (ALIGN_WIDE_ARGUMENTS == 1)
    if ((_ireg & 1) != 0) {
      // 64-bit values should be 8-byte aligned
      _ireg++;
    }
#endif
    Register dst1 = as_Register(_ireg);
    Register dst2 = as_Register(_ireg+1);
    __ ldr(dst1, Address(Rlocals, Interpreter::local_offset_in_bytes(offset()+1)));
    __ ldr(dst2, Address(Rlocals, Interpreter::local_offset_in_bytes(offset())));
    _ireg += 2;
#if (ALIGN_WIDE_ARGUMENTS == 0)
  } else if (_ireg == 3) {
    // uses R3 + one stack slot
    Register dst1 = as_Register(_ireg);
    __ ldr(Rtemp, Address(Rlocals, Interpreter::local_offset_in_bytes(offset())));
    __ ldr(dst1, Address(Rlocals, Interpreter::local_offset_in_bytes(offset()+1)));
    __ str(Rtemp, Address(SP, _abi_offset * wordSize));
    _ireg += 1;
    _abi_offset += 1;
#endif
  } else {
#if (ALIGN_WIDE_ARGUMENTS == 1)
    if(_abi_offset & 1) _abi_offset++;
#endif
    __ ldr(Rtemp, Address(Rlocals, Interpreter::local_offset_in_bytes(offset()+1)));
    __ str(Rtemp, Address(SP, (_abi_offset) * wordSize));
    __ ldr(Rtemp, Address(Rlocals, Interpreter::local_offset_in_bytes(offset())));
    __ str(Rtemp, Address(SP, (_abi_offset+1) * wordSize));
    _abi_offset += 2;
    _ireg = 4;
  }
}

void InterpreterRuntime::SignatureHandlerGenerator::pass_object() {
  if (_ireg < 4) {
    Register dst = as_Register(_ireg);
    __ ldr(dst, Address(Rlocals, Interpreter::local_offset_in_bytes(offset())));
    __ cmp(dst, 0);
    __ sub(dst, Rlocals, -Interpreter::local_offset_in_bytes(offset()), ne);
    _ireg++;
  } else {
    __ ldr(Rtemp, Address(Rlocals, Interpreter::local_offset_in_bytes(offset())));
    __ cmp(Rtemp, 0);
    __ sub(Rtemp, Rlocals, -Interpreter::local_offset_in_bytes(offset()), ne);
    __ str(Rtemp, Address(SP, _abi_offset * wordSize));
    _abi_offset++;
  }
}

#ifndef __ABI_HARD__
void InterpreterRuntime::SignatureHandlerGenerator::pass_float() {
  if (_ireg < 4) {
    Register dst = as_Register(_ireg);
    __ ldr(dst, Address(Rlocals, Interpreter::local_offset_in_bytes(offset())));
    _ireg++;
  } else {
    __ ldr(Rtemp, Address(Rlocals, Interpreter::local_offset_in_bytes(offset())));
    __ str(Rtemp, Address(SP, _abi_offset * wordSize));
    _abi_offset++;
  }
}

#else
#ifndef __SOFTFP__
void InterpreterRuntime::SignatureHandlerGenerator::pass_float() {
    if((_fp_slot < 16) || (_single_fpr_slot & 1)) {
      if ((_single_fpr_slot & 1) == 0) {
        _single_fpr_slot = _fp_slot;
        _fp_slot += 2;
      }
      __ flds(as_FloatRegister(_single_fpr_slot), Address(Rlocals, Interpreter::local_offset_in_bytes(offset())));
      _single_fpr_slot++;
    } else {
      __ ldr(Rtemp, Address(Rlocals, Interpreter::local_offset_in_bytes(offset())));
      __ str(Rtemp, Address(SP, _abi_offset * wordSize));
      _abi_offset++;
    }
}

void InterpreterRuntime::SignatureHandlerGenerator::pass_double() {
    if(_fp_slot <= 14) {
      __ fldd(as_FloatRegister(_fp_slot), Address(Rlocals, Interpreter::local_offset_in_bytes(offset()+1)));
      _fp_slot += 2;
    } else {
      __ ldr(Rtemp, Address(Rlocals, Interpreter::local_offset_in_bytes(offset()+1)));
      __ str(Rtemp, Address(SP, (_abi_offset) * wordSize));
      __ ldr(Rtemp, Address(Rlocals, Interpreter::local_offset_in_bytes(offset())));
      __ str(Rtemp, Address(SP, (_abi_offset+1) * wordSize));
      _abi_offset += 2;
      _single_fpr_slot = 16;
    }
}
#endif // __SOFTFP__
#endif // __ABI_HARD__

void InterpreterRuntime::SignatureHandlerGenerator::generate(uint64_t fingerprint) {
  iterate(fingerprint);

  BasicType result_type = SignatureIterator::fp_return_type(fingerprint);

  address result_handler = Interpreter::result_handler(result_type);

  __ mov_slow(R0, (intptr_t)result_handler);

  __ ret();
}


// Implementation of SignatureHandlerLibrary

void SignatureHandlerLibrary::pd_set_handler(address handler) {}

class SlowSignatureHandler: public NativeSignatureIterator {
 private:
  address   _from;
  intptr_t* _to;

#ifndef __ABI_HARD__
  virtual void pass_int() {
    *_to++ = *(jint *)(_from+Interpreter::local_offset_in_bytes(0));
    _from -= Interpreter::stackElementSize;
  }

  virtual void pass_float() {
    *_to++ = *(jint *)(_from+Interpreter::local_offset_in_bytes(0));
    _from -= Interpreter::stackElementSize;
  }

  virtual void pass_long() {
#if (ALIGN_WIDE_ARGUMENTS == 1)
    if (((intptr_t)_to & 7) != 0) {
      // 64-bit values should be 8-byte aligned
      _to++;
    }
#endif
    _to[0] = *(intptr_t*)(_from+Interpreter::local_offset_in_bytes(1));
    _to[1] = *(intptr_t*)(_from+Interpreter::local_offset_in_bytes(0));
    _to += 2;
    _from -= 2*Interpreter::stackElementSize;
  }

  virtual void pass_object() {
    intptr_t from_addr = (intptr_t)(_from + Interpreter::local_offset_in_bytes(0));
    *_to++ = (*(intptr_t*)from_addr == 0) ? (intptr_t)NULL : from_addr;
    _from -= Interpreter::stackElementSize;
   }

#else

  intptr_t* _toFP;
  intptr_t* _toGP;
  int       _last_gp;
  int       _last_fp;
  int       _last_single_fp;

  virtual void pass_int() {
    if(_last_gp < GPR_PARAMS) {
      _toGP[_last_gp++] = *(jint *)(_from+Interpreter::local_offset_in_bytes(0));
    } else {
      *_to++ = *(jint *)(_from+Interpreter::local_offset_in_bytes(0));
    }
    _from -= Interpreter::stackElementSize;
  }

  virtual void pass_long() {
    assert(ALIGN_WIDE_ARGUMENTS == 1, "ABI_HARD not supported with unaligned wide arguments");
    if (_last_gp <= 2) {
      if(_last_gp & 1) _last_gp++;
      _toGP[_last_gp++] = *(jint *)(_from+Interpreter::local_offset_in_bytes(1));
      _toGP[_last_gp++] = *(jint *)(_from+Interpreter::local_offset_in_bytes(0));
    } else {
      if (((intptr_t)_to & 7) != 0) {
        // 64-bit values should be 8-byte aligned
        _to++;
      }
      _to[0] = *(intptr_t*)(_from+Interpreter::local_offset_in_bytes(1));
      _to[1] = *(intptr_t*)(_from+Interpreter::local_offset_in_bytes(0));
      _to += 2;
      _last_gp = 4;
    }
    _from -= 2*Interpreter::stackElementSize;
  }

  virtual void pass_object() {
    intptr_t from_addr = (intptr_t)(_from + Interpreter::local_offset_in_bytes(0));
    if(_last_gp < GPR_PARAMS) {
      _toGP[_last_gp++] = (*(intptr_t*)from_addr == 0) ? NULL : from_addr;
    } else {
      *_to++ = (*(intptr_t*)from_addr == 0) ? NULL : from_addr;
    }
    _from -= Interpreter::stackElementSize;
  }

  virtual void pass_float() {
    if((_last_fp < 16) || (_last_single_fp & 1)) {
      if ((_last_single_fp & 1) == 0) {
        _last_single_fp = _last_fp;
        _last_fp += 2;
      }

      _toFP[_last_single_fp++] = *(jint *)(_from+Interpreter::local_offset_in_bytes(0));
    } else {
      *_to++ = *(jint *)(_from+Interpreter::local_offset_in_bytes(0));
    }
    _from -= Interpreter::stackElementSize;
  }

  virtual void pass_double() {
    assert(ALIGN_WIDE_ARGUMENTS == 1, "ABI_HARD not supported with unaligned wide arguments");
    if(_last_fp <= 14) {
      _toFP[_last_fp++] = *(intptr_t*)(_from+Interpreter::local_offset_in_bytes(1));
      _toFP[_last_fp++] = *(intptr_t*)(_from+Interpreter::local_offset_in_bytes(0));
    } else {
      if (((intptr_t)_to & 7) != 0) {      // 64-bit values should be 8-byte aligned
        _to++;
      }
      _to[0] = *(intptr_t*)(_from+Interpreter::local_offset_in_bytes(1));
      _to[1] = *(intptr_t*)(_from+Interpreter::local_offset_in_bytes(0));
      _to += 2;
      _last_single_fp = 16;
    }
    _from -= 2*Interpreter::stackElementSize;
  }

#endif // !__ABI_HARD__

 public:
  SlowSignatureHandler(const methodHandle& method, address from, intptr_t* to) :
    NativeSignatureIterator(method) {
    _from = from;

#ifdef __ABI_HARD__
    _toGP  = to;
    _toFP = _toGP + GPR_PARAMS;
    _to   = _toFP + (8*2);
    _last_gp = (is_static() ? 2 : 1);
    _last_fp = 0;
    _last_single_fp = 0;
#else
    _to   = to + (is_static() ? 2 : 1);
#endif // __ABI_HARD__
  }
};

JRT_ENTRY(address, InterpreterRuntime::slow_signature_handler(JavaThread* current, Method* method, intptr_t* from, intptr_t* to))
  methodHandle m(current, (Method*)method);
  assert(m->is_native(), "sanity check");
  SlowSignatureHandler(m, (address)from, to).iterate(UCONST64(-1));
  return Interpreter::result_handler(m->result_type());
JRT_END
