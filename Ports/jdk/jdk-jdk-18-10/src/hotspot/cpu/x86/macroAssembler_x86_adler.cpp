/*
* Copyright (c) 2021, Intel Corporation.
*
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
#include "asm/assembler.hpp"
#include "asm/assembler.inline.hpp"
#include "runtime/stubRoutines.hpp"
#include "macroAssembler_x86.hpp"

#ifdef _LP64
void MacroAssembler::updateBytesAdler32(Register init_d, Register data, Register size, XMMRegister yshuf0, XMMRegister yshuf1, ExternalAddress ascaletab)
{
      const int LIMIT = 5552;
      const int BASE = 65521;
      const int CHUNKSIZE =  16;
      const int CHUNKSIZE_M1 = CHUNKSIZE - 1;

      const Register s = r11;
      const Register a_d = r12; //r12d
      const Register b_d = r8; //r8d
      const Register end = r13;

      const XMMRegister ya = xmm0;
      const XMMRegister yb = xmm1;
      const XMMRegister ydata0 = xmm2;
      const XMMRegister ydata1 = xmm3;
      const XMMRegister ysa = xmm4;
      const XMMRegister ydata = ysa;
      const XMMRegister ytmp0 = ydata0;
      const XMMRegister ytmp1 = ydata1;
      const XMMRegister ytmp2 = xmm5;
      const XMMRegister xa = xmm0;
      const XMMRegister xb = xmm1;
      const XMMRegister xtmp0 = xmm2;
      const XMMRegister xtmp1 = xmm3;
      const XMMRegister xsa = xmm4;
      const XMMRegister xtmp2 = xmm5;
      assert_different_registers(init_d, data, size, s, a_d, b_d, end, rax);

      Label SLOOP1, SLOOP1A, SKIP_LOOP_1A, FINISH, LT64, DO_FINAL, FINAL_LOOP, ZERO_SIZE, END;

      push(r12);
      push(r13);
      push(r14);
      movl(b_d, init_d); //adler
      shrl(b_d, 16);
      andl(init_d, 0xFFFF);
      cmpl(size, 32);
      jcc(Assembler::below, LT64);
      movdl(xa, init_d); //vmovd - 32bit
      vpxor(yb, yb, yb, Assembler::AVX_256bit);

      bind(SLOOP1);
      movl(s, LIMIT);
      cmpl(s, size);
      cmovl(Assembler::above, s, size); // s = min(size, LIMIT)
      lea(end, Address(s, data, Address::times_1, -CHUNKSIZE_M1));
      cmpptr(data, end);
      jcc(Assembler::aboveEqual, SKIP_LOOP_1A);

      align(32);
      bind(SLOOP1A);
      vbroadcastf128(ydata, Address(data, 0), Assembler::AVX_256bit);
      addptr(data, CHUNKSIZE);
      vpshufb(ydata0, ydata, yshuf0, Assembler::AVX_256bit);
      vpaddd(ya, ya, ydata0, Assembler::AVX_256bit);
      vpaddd(yb, yb, ya, Assembler::AVX_256bit);
      vpshufb(ydata1, ydata, yshuf1, Assembler::AVX_256bit);
      vpaddd(ya, ya, ydata1, Assembler::AVX_256bit);
      vpaddd(yb, yb, ya, Assembler::AVX_256bit);
      cmpptr(data, end);
      jcc(Assembler::below, SLOOP1A);

      bind(SKIP_LOOP_1A);
      addptr(end, CHUNKSIZE_M1);
      testl(s, CHUNKSIZE_M1);
      jcc(Assembler::notEqual, DO_FINAL);

      // either we're done, or we just did LIMIT
      subl(size, s);

      // reduce
      vpslld(yb, yb, 3, Assembler::AVX_256bit); //b is scaled by 8
      vpmulld(ysa, ya, ascaletab, Assembler::AVX_256bit, r14);

      // compute horizontal sums of ya, yb, ysa
      vextracti128(xtmp0, ya, 1);
      vextracti128(xtmp1, yb, 1);
      vextracti128(xtmp2, ysa, 1);
      vpaddd(xa, xa, xtmp0, Assembler::AVX_128bit);
      vpaddd(xb, xb, xtmp1, Assembler::AVX_128bit);
      vpaddd(xsa, xsa, xtmp2, Assembler::AVX_128bit);
      vphaddd(xa, xa, xa, Assembler::AVX_128bit);
      vphaddd(xb, xb, xb, Assembler::AVX_128bit);
      vphaddd(xsa, xsa, xsa, Assembler::AVX_128bit);
      vphaddd(xa, xa, xa, Assembler::AVX_128bit);
      vphaddd(xb, xb, xb, Assembler::AVX_128bit);
      vphaddd(xsa, xsa, xsa, Assembler::AVX_128bit);

      movdl(rax, xa);
      xorl(rdx, rdx);
      movl(rcx, BASE);
      divl(rcx); // divide edx:eax by ecx, quot->eax, rem->edx
      movl(a_d, rdx);

      vpsubd(xb, xb, xsa, Assembler::AVX_128bit);
      movdl(rax, xb);
      addl(rax, b_d);
      xorl(rdx, rdx);
      movl(rcx, BASE);
      divl(rcx); // divide edx:eax by ecx, quot->eax, rem->edx
      movl(b_d, rdx);

      testl(size, size);
      jcc(Assembler::zero, FINISH);

      // continue loop
      movdl(xa, a_d);
      vpxor(yb, yb, yb, Assembler::AVX_256bit);
      jmp(SLOOP1);

      bind(FINISH);
      movl(rax, b_d);
      shll(rax, 16);
      orl(rax, a_d);
      jmp(END);

      bind(LT64);
      movl(a_d, init_d);
      lea(end, Address(data, size, Address::times_1));
      testl(size, size);
      jcc(Assembler::notZero, FINAL_LOOP);
      jmp(ZERO_SIZE);

      // handle remaining 1...15 bytes
      bind(DO_FINAL);
      // reduce
      vpslld(yb, yb, 3, Assembler::AVX_256bit); //b is scaled by 8
      vpmulld(ysa, ya, ascaletab, Assembler::AVX_256bit, r14); //scaled a

      vextracti128(xtmp0, ya, 1);
      vextracti128(xtmp1, yb, 1);
      vextracti128(xtmp2, ysa, 1);
      vpaddd(xa, xa, xtmp0, Assembler::AVX_128bit);
      vpaddd(xb, xb, xtmp1, Assembler::AVX_128bit);
      vpaddd(xsa, xsa, xtmp2, Assembler::AVX_128bit);
      vphaddd(xa, xa, xa, Assembler::AVX_128bit);
      vphaddd(xb, xb, xb, Assembler::AVX_128bit);
      vphaddd(xsa, xsa, xsa, Assembler::AVX_128bit);
      vphaddd(xa, xa, xa, Assembler::AVX_128bit);
      vphaddd(xb, xb, xb, Assembler::AVX_128bit);
      vphaddd(xsa, xsa, xsa, Assembler::AVX_128bit);
      vpsubd(xb, xb, xsa, Assembler::AVX_128bit);

      movdl(a_d, xa);
      movdl(rax, xb);
      addl(b_d, rax);

      align(32);
      bind(FINAL_LOOP);
      movzbl(rax, Address(data, 0)); //movzx   eax, byte[data]
      addl(a_d, rax);
      addptr(data, 1);
      addl(b_d, a_d);
      cmpptr(data, end);
      jcc(Assembler::below, FINAL_LOOP);

      bind(ZERO_SIZE);

      movl(rax, a_d);
      xorl(rdx, rdx);
      movl(rcx, BASE);
      divl(rcx); // div ecx -- divide edx:eax by ecx, quot->eax, rem->edx
      movl(a_d, rdx);

      movl(rax, b_d);
      xorl(rdx, rdx);
      movl(rcx, BASE);
      divl(rcx); // divide edx:eax by ecx, quot->eax, rem->edx
      shll(rdx, 16);
      orl(rdx, a_d);
      movl(rax, rdx);

      bind(END);
      pop(r14);
      pop(r13);
      pop(r12);
  }
#endif
