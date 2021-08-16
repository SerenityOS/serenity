/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "asm/macroAssembler.hpp"
#include "compiler/disassembler.hpp"
#include "interpreter/interp_masm.hpp"
#include "interpreter/interpreter.hpp"
#include "interpreter/interpreterRuntime.hpp"
#include "interpreter/templateInterpreterGenerator.hpp"
#include "runtime/arguments.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubRoutines.hpp"

#define __ Disassembler::hook<InterpreterMacroAssembler>(__FILE__, __LINE__, _masm)->


address TemplateInterpreterGenerator::generate_slow_signature_handler() {
  address entry = __ pc();
  // rbx,: method
  // rcx: temporary
  // rdi: pointer to locals
  // rsp: end of copied parameters area
  __ mov(rcx, rsp);
  __ call_VM(noreg, CAST_FROM_FN_PTR(address, InterpreterRuntime::slow_signature_handler), rbx, rdi, rcx);
  __ ret(0);
  return entry;
}

/**
 * Method entry for static native methods:
 *   int java.util.zip.CRC32.update(int crc, int b)
 */
address TemplateInterpreterGenerator::generate_CRC32_update_entry() {
  if (UseCRC32Intrinsics) {
    address entry = __ pc();

    // rbx: Method*
    // rsi: senderSP must preserved for slow path, set SP to it on fast path
    // rdx: scratch
    // rdi: scratch

    Label slow_path;
    // If we need a safepoint check, generate full interpreter entry.
    __ get_thread(rdi);
    __ safepoint_poll(slow_path, rdi, false /* at_return */, false /* in_nmethod */);

    // We don't generate local frame and don't align stack because
    // we call stub code and there is no safepoint on this path.

    // Load parameters
    const Register crc = rax;  // crc
    const Register val = rdx;  // source java byte value
    const Register tbl = rdi;  // scratch

    // Arguments are reversed on java expression stack
    __ movl(val, Address(rsp,   wordSize)); // byte value
    __ movl(crc, Address(rsp, 2*wordSize)); // Initial CRC

    __ lea(tbl, ExternalAddress(StubRoutines::crc_table_addr()));
    __ notl(crc); // ~crc
    __ update_byte_crc32(crc, val, tbl);
    __ notl(crc); // ~crc
    // result in rax

    // _areturn
    __ pop(rdi);                // get return address
    __ mov(rsp, rsi);           // set sp to sender sp
    __ jmp(rdi);

    // generate a vanilla native entry as the slow path
    __ bind(slow_path);
    __ jump_to_entry(Interpreter::entry_for_kind(Interpreter::native));
    return entry;
  }
  return NULL;
}

/**
 * Method entry for static native methods:
 *   int java.util.zip.CRC32.updateBytes(int crc, byte[] b, int off, int len)
 *   int java.util.zip.CRC32.updateByteBuffer(int crc, long buf, int off, int len)
 */
address TemplateInterpreterGenerator::generate_CRC32_updateBytes_entry(AbstractInterpreter::MethodKind kind) {
  if (UseCRC32Intrinsics) {
    address entry = __ pc();

    // rbx,: Method*
    // rsi: senderSP must preserved for slow path, set SP to it on fast path
    // rdx: scratch
    // rdi: scratch

    Label slow_path;
    // If we need a safepoint check, generate full interpreter entry.
    __ get_thread(rdi);
    __ safepoint_poll(slow_path, rdi, false /* at_return */, false /* in_nmethod */);

    // We don't generate local frame and don't align stack because
    // we call stub code and there is no safepoint on this path.

    // Load parameters
    const Register crc = rax;  // crc
    const Register buf = rdx;  // source java byte array address
    const Register len = rdi;  // length

    // value              x86_32
    // interp. arg ptr    ESP + 4
    // int java.util.zip.CRC32.updateBytes(int crc, byte[] b, int off, int len)
    //                                         3           2      1        0
    // int java.util.zip.CRC32.updateByteBuffer(int crc, long buf, int off, int len)
    //                                              4         2,3      1        0

    // Arguments are reversed on java expression stack
    __ movl(len,   Address(rsp,   4 + 0)); // Length
    // Calculate address of start element
    if (kind == Interpreter::java_util_zip_CRC32_updateByteBuffer) {
      __ movptr(buf, Address(rsp, 4 + 2 * wordSize)); // long buf
      __ addptr(buf, Address(rsp, 4 + 1 * wordSize)); // + offset
      __ movl(crc,   Address(rsp, 4 + 4 * wordSize)); // Initial CRC
    } else {
      __ movptr(buf, Address(rsp, 4 + 2 * wordSize)); // byte[] array
      __ addptr(buf, arrayOopDesc::base_offset_in_bytes(T_BYTE)); // + header size
      __ addptr(buf, Address(rsp, 4 + 1 * wordSize)); // + offset
      __ movl(crc,   Address(rsp, 4 + 3 * wordSize)); // Initial CRC
    }

    __ super_call_VM_leaf(CAST_FROM_FN_PTR(address, StubRoutines::updateBytesCRC32()), crc, buf, len);
    // result in rax

    // _areturn
    __ pop(rdi);                // get return address
    __ mov(rsp, rsi);           // set sp to sender sp
    __ jmp(rdi);

    // generate a vanilla native entry as the slow path
    __ bind(slow_path);
    __ jump_to_entry(Interpreter::entry_for_kind(Interpreter::native));
    return entry;
  }
  return NULL;
}

/**
* Method entry for static native methods:
*   int java.util.zip.CRC32C.updateBytes(int crc, byte[] b, int off, int end)
*   int java.util.zip.CRC32C.updateByteBuffer(int crc, long address, int off, int end)
*/
address TemplateInterpreterGenerator::generate_CRC32C_updateBytes_entry(AbstractInterpreter::MethodKind kind) {
  if (UseCRC32CIntrinsics) {
    address entry = __ pc();
    // Load parameters
    const Register crc = rax;  // crc
    const Register buf = rcx;  // source java byte array address
    const Register len = rdx;  // length
    const Register end = len;

    // value              x86_32
    // interp. arg ptr    ESP + 4
    // int java.util.zip.CRC32.updateBytes(int crc, byte[] b, int off, int end)
    //                                         3           2      1        0
    // int java.util.zip.CRC32.updateByteBuffer(int crc, long address, int off, int end)
    //                                              4         2,3          1        0

    // Arguments are reversed on java expression stack
    __ movl(end, Address(rsp, 4 + 0)); // end
    __ subl(len, Address(rsp, 4 + 1 * wordSize));  // end - offset == length
    // Calculate address of start element
    if (kind == Interpreter::java_util_zip_CRC32C_updateDirectByteBuffer) {
      __ movptr(buf, Address(rsp, 4 + 2 * wordSize)); // long address
      __ addptr(buf, Address(rsp, 4 + 1 * wordSize)); // + offset
      __ movl(crc, Address(rsp, 4 + 4 * wordSize)); // Initial CRC
    } else {
      __ movptr(buf, Address(rsp, 4 + 2 * wordSize)); // byte[] array
      __ addptr(buf, arrayOopDesc::base_offset_in_bytes(T_BYTE)); // + header size
      __ addptr(buf, Address(rsp, 4 + 1 * wordSize)); // + offset
      __ movl(crc, Address(rsp, 4 + 3 * wordSize)); // Initial CRC
    }
    __ super_call_VM_leaf(CAST_FROM_FN_PTR(address, StubRoutines::updateBytesCRC32C()), crc, buf, len);
    // result in rax
    // _areturn
    __ pop(rdi);                // get return address
    __ mov(rsp, rsi);           // set sp to sender sp
    __ jmp(rdi);

    return entry;
  }
  return NULL;
}

/**
 * Method entry for static native method:
 *    java.lang.Float.intBitsToFloat(int bits)
 */
address TemplateInterpreterGenerator::generate_Float_intBitsToFloat_entry() {
  if (UseSSE >= 1) {
    address entry = __ pc();

    // rsi: the sender's SP

    // Skip safepoint check (compiler intrinsic versions of this method
    // do not perform safepoint checks either).

    // Load 'bits' into xmm0 (interpreter returns results in xmm0)
    __ movflt(xmm0, Address(rsp, wordSize));

    // Return
    __ pop(rdi); // get return address
    __ mov(rsp, rsi); // set rsp to the sender's SP
    __ jmp(rdi);
    return entry;
  }

  return NULL;
}

/**
 * Method entry for static native method:
 *    java.lang.Float.floatToRawIntBits(float value)
 */
address TemplateInterpreterGenerator::generate_Float_floatToRawIntBits_entry() {
  if (UseSSE >= 1) {
    address entry = __ pc();

    // rsi: the sender's SP

    // Skip safepoint check (compiler intrinsic versions of this method
    // do not perform safepoint checks either).

    // Load the parameter (a floating-point value) into rax.
    __ movl(rax, Address(rsp, wordSize));

    // Return
    __ pop(rdi); // get return address
    __ mov(rsp, rsi); // set rsp to the sender's SP
    __ jmp(rdi);
    return entry;
  }

  return NULL;
}


/**
 * Method entry for static native method:
 *    java.lang.Double.longBitsToDouble(long bits)
 */
address TemplateInterpreterGenerator::generate_Double_longBitsToDouble_entry() {
   if (UseSSE >= 2) {
     address entry = __ pc();

     // rsi: the sender's SP

     // Skip safepoint check (compiler intrinsic versions of this method
     // do not perform safepoint checks either).

     // Load 'bits' into xmm0 (interpreter returns results in xmm0)
     __ movdbl(xmm0, Address(rsp, wordSize));

     // Return
     __ pop(rdi); // get return address
     __ mov(rsp, rsi); // set rsp to the sender's SP
     __ jmp(rdi);
     return entry;
   }

   return NULL;
}

/**
 * Method entry for static native method:
 *    java.lang.Double.doubleToRawLongBits(double value)
 */
address TemplateInterpreterGenerator::generate_Double_doubleToRawLongBits_entry() {
  if (UseSSE >= 2) {
    address entry = __ pc();

    // rsi: the sender's SP

    // Skip safepoint check (compiler intrinsic versions of this method
    // do not perform safepoint checks either).

    // Load the parameter (a floating-point value) into rax.
    __ movl(rdx, Address(rsp, 2*wordSize));
    __ movl(rax, Address(rsp, wordSize));

    // Return
    __ pop(rdi); // get return address
    __ mov(rsp, rsi); // set rsp to the sender's SP
    __ jmp(rdi);
    return entry;
  }

  return NULL;
}

address TemplateInterpreterGenerator::generate_math_entry(AbstractInterpreter::MethodKind kind) {

  // rbx,: Method*
  // rcx: scratrch
  // rsi: sender sp

  if (!InlineIntrinsics) return NULL; // Generate a vanilla entry

  address entry_point = __ pc();

  // These don't need a safepoint check because they aren't virtually
  // callable. We won't enter these intrinsics from compiled code.
  // If in the future we added an intrinsic which was virtually callable
  // we'd have to worry about how to safepoint so that this code is used.

  // mathematical functions inlined by compiler
  // (interpreter must provide identical implementation
  // in order to avoid monotonicity bugs when switching
  // from interpreter to compiler in the middle of some
  // computation)
  //
  // stack: [ ret adr ] <-- rsp
  //        [ lo(arg) ]
  //        [ hi(arg) ]
  //
  if (kind == Interpreter::java_lang_math_fmaD) {
    if (!UseFMA) {
      return NULL; // Generate a vanilla entry
    }
    __ movdbl(xmm2, Address(rsp, 5 * wordSize));
    __ movdbl(xmm1, Address(rsp, 3 * wordSize));
    __ movdbl(xmm0, Address(rsp, 1 * wordSize));
    __ fmad(xmm0, xmm1, xmm2, xmm0);
    __ pop(rdi);                               // get return address
    __ mov(rsp, rsi);                          // set sp to sender sp
    __ jmp(rdi);

    return entry_point;
  } else if (kind == Interpreter::java_lang_math_fmaF) {
    if (!UseFMA) {
      return NULL; // Generate a vanilla entry
    }
    __ movflt(xmm2, Address(rsp, 3 * wordSize));
    __ movflt(xmm1, Address(rsp, 2 * wordSize));
    __ movflt(xmm0, Address(rsp, 1 * wordSize));
    __ fmaf(xmm0, xmm1, xmm2, xmm0);
    __ pop(rdi);                               // get return address
    __ mov(rsp, rsi);                          // set sp to sender sp
    __ jmp(rdi);

    return entry_point;
 }

  __ fld_d(Address(rsp, 1*wordSize));
  switch (kind) {
    case Interpreter::java_lang_math_sin :
        __ subptr(rsp, 2 * wordSize);
        __ fstp_d(Address(rsp, 0));
        if (VM_Version::supports_sse2() && StubRoutines::dsin() != NULL) {
          __ call(RuntimeAddress(CAST_FROM_FN_PTR(address, StubRoutines::dsin())));
        } else {
          __ call_VM_leaf0(CAST_FROM_FN_PTR(address, SharedRuntime::dsin));
        }
        __ addptr(rsp, 2 * wordSize);
        break;
    case Interpreter::java_lang_math_cos :
        __ subptr(rsp, 2 * wordSize);
        __ fstp_d(Address(rsp, 0));
        if (VM_Version::supports_sse2() && StubRoutines::dcos() != NULL) {
          __ call(RuntimeAddress(CAST_FROM_FN_PTR(address, StubRoutines::dcos())));
        } else {
          __ call_VM_leaf0(CAST_FROM_FN_PTR(address, SharedRuntime::dcos));
        }
        __ addptr(rsp, 2 * wordSize);
        break;
    case Interpreter::java_lang_math_tan :
        __ subptr(rsp, 2 * wordSize);
        __ fstp_d(Address(rsp, 0));
        if (StubRoutines::dtan() != NULL) {
          __ call(RuntimeAddress(CAST_FROM_FN_PTR(address, StubRoutines::dtan())));
        } else {
          __ call_VM_leaf0(CAST_FROM_FN_PTR(address, SharedRuntime::dtan));
        }
        __ addptr(rsp, 2 * wordSize);
        break;
    case Interpreter::java_lang_math_sqrt:
        __ fsqrt();
        break;
    case Interpreter::java_lang_math_abs:
        __ fabs();
        break;
    case Interpreter::java_lang_math_log:
        __ subptr(rsp, 2 * wordSize);
        __ fstp_d(Address(rsp, 0));
        if (StubRoutines::dlog() != NULL) {
          __ call(RuntimeAddress(CAST_FROM_FN_PTR(address, StubRoutines::dlog())));
        } else {
          __ call_VM_leaf0(CAST_FROM_FN_PTR(address, SharedRuntime::dlog));
        }
        __ addptr(rsp, 2 * wordSize);
        break;
    case Interpreter::java_lang_math_log10:
        __ subptr(rsp, 2 * wordSize);
        __ fstp_d(Address(rsp, 0));
        if (StubRoutines::dlog10() != NULL) {
          __ call(RuntimeAddress(CAST_FROM_FN_PTR(address, StubRoutines::dlog10())));
        } else {
          __ call_VM_leaf0(CAST_FROM_FN_PTR(address, SharedRuntime::dlog10));
        }
        __ addptr(rsp, 2 * wordSize);
        break;
    case Interpreter::java_lang_math_pow:
      __ fld_d(Address(rsp, 3*wordSize)); // second argument
      __ subptr(rsp, 4 * wordSize);
      __ fstp_d(Address(rsp, 0));
      __ fstp_d(Address(rsp, 2 * wordSize));
      if (StubRoutines::dpow() != NULL) {
        __ call(RuntimeAddress(CAST_FROM_FN_PTR(address, StubRoutines::dpow())));
      } else {
        __ call_VM_leaf0(CAST_FROM_FN_PTR(address, SharedRuntime::dpow));
      }
      __ addptr(rsp, 4 * wordSize);
      break;
    case Interpreter::java_lang_math_exp:
      __ subptr(rsp, 2*wordSize);
      __ fstp_d(Address(rsp, 0));
      if (StubRoutines::dexp() != NULL) {
        __ call(RuntimeAddress(CAST_FROM_FN_PTR(address, StubRoutines::dexp())));
      } else {
        __ call_VM_leaf0(CAST_FROM_FN_PTR(address, SharedRuntime::dexp));
      }
      __ addptr(rsp, 2*wordSize);
    break;
    default                              :
        ShouldNotReachHere();
  }

  // return double result in xmm0 for interpreter and compilers.
  if (UseSSE >= 2) {
    __ subptr(rsp, 2*wordSize);
    __ fstp_d(Address(rsp, 0));
    __ movdbl(xmm0, Address(rsp, 0));
    __ addptr(rsp, 2*wordSize);
  }

  // done, result in FPU ST(0) or XMM0
  __ pop(rdi);                               // get return address
  __ mov(rsp, rsi);                          // set sp to sender sp
  __ jmp(rdi);

  return entry_point;
}
