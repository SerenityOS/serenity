/*
 * Copyright (c) 2020 Microsoft Corporation. All rights reserved.
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

/*
 * Copyright (c) 2017 Project Nayuki. (MIT License)
 * https://www.nayuki.io/page/fast-md5-hash-implementation-in-x86-assembly
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * - The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 * - The Software is provided "as is", without warranty of any kind, express or
 *   implied, including but not limited to the warranties of merchantability,
 *   fitness for a particular purpose and noninfringement. In no event shall the
 *   authors or copyright holders be liable for any claim, damages or other
 *   liability, whether in an action of contract, tort or otherwise, arising from,
 *   out of or in connection with the Software or the use or other dealings in the
 *   Software.
 */

#include "precompiled.hpp"
#include "asm/assembler.hpp"
#include "asm/assembler.inline.hpp"
#include "runtime/stubRoutines.hpp"
#include "macroAssembler_x86.hpp"

// int com.sun.security.provider.MD5.implCompress0(byte[] b, int ofs)
void MacroAssembler::fast_md5(Register buf, Address state, Address ofs, Address limit, bool multi_block) {

  Label start, done_hash, loop0;

  bind(start);

  bind(loop0);

  // Save hash values for addition after rounds
  movptr(rdi, state);
  movl(rax, Address(rdi,  0));
  movl(rbx, Address(rdi,  4));
  movl(rcx, Address(rdi,  8));
  movl(rdx, Address(rdi, 12));

#define FF(r1, r2, r3, r4, k, s, t)              \
  movl(rsi, r3);                                 \
  addl(r1, Address(buf, k*4));                   \
  xorl(rsi, r4);                                 \
  andl(rsi, r2);                                 \
  xorl(rsi, r4);                                 \
  leal(r1, Address(r1, rsi, Address::times_1, t)); \
  roll(r1, s);                                   \
  addl(r1, r2);

#define GG(r1, r2, r3, r4, k, s, t)              \
  movl(rsi, r4);                                 \
  movl(rdi, r4);                                 \
  addl(r1, Address(buf, k*4));                   \
  notl(rsi);                                     \
  andl(rdi, r2);                                 \
  andl(rsi, r3);                                 \
  orl(rsi, rdi);                                 \
  leal(r1, Address(r1, rsi, Address::times_1, t)); \
  roll(r1, s);                                   \
  addl(r1, r2);

#define HH(r1, r2, r3, r4, k, s, t)              \
  movl(rsi, r3);                                 \
  addl(r1, Address(buf, k*4));                   \
  xorl(rsi, r4);                                 \
  xorl(rsi, r2);                                 \
  leal(r1, Address(r1, rsi, Address::times_1, t)); \
  roll(r1, s);                                   \
  addl(r1, r2);

#define II(r1, r2, r3, r4, k, s, t)              \
  movl(rsi, r4);                                 \
  notl(rsi);                                     \
  addl(r1, Address(buf, k*4));                   \
  orl(rsi, r2);                                  \
  xorl(rsi, r3);                                 \
  leal(r1, Address(r1, rsi, Address::times_1, t)); \
  roll(r1, s);                                   \
  addl(r1, r2);

  // Round 1
  FF(rax, rbx, rcx, rdx,  0,  7, 0xd76aa478)
  FF(rdx, rax, rbx, rcx,  1, 12, 0xe8c7b756)
  FF(rcx, rdx, rax, rbx,  2, 17, 0x242070db)
  FF(rbx, rcx, rdx, rax,  3, 22, 0xc1bdceee)
  FF(rax, rbx, rcx, rdx,  4,  7, 0xf57c0faf)
  FF(rdx, rax, rbx, rcx,  5, 12, 0x4787c62a)
  FF(rcx, rdx, rax, rbx,  6, 17, 0xa8304613)
  FF(rbx, rcx, rdx, rax,  7, 22, 0xfd469501)
  FF(rax, rbx, rcx, rdx,  8,  7, 0x698098d8)
  FF(rdx, rax, rbx, rcx,  9, 12, 0x8b44f7af)
  FF(rcx, rdx, rax, rbx, 10, 17, 0xffff5bb1)
  FF(rbx, rcx, rdx, rax, 11, 22, 0x895cd7be)
  FF(rax, rbx, rcx, rdx, 12,  7, 0x6b901122)
  FF(rdx, rax, rbx, rcx, 13, 12, 0xfd987193)
  FF(rcx, rdx, rax, rbx, 14, 17, 0xa679438e)
  FF(rbx, rcx, rdx, rax, 15, 22, 0x49b40821)

  // Round 2
  GG(rax, rbx, rcx, rdx,  1,  5, 0xf61e2562)
  GG(rdx, rax, rbx, rcx,  6,  9, 0xc040b340)
  GG(rcx, rdx, rax, rbx, 11, 14, 0x265e5a51)
  GG(rbx, rcx, rdx, rax,  0, 20, 0xe9b6c7aa)
  GG(rax, rbx, rcx, rdx,  5,  5, 0xd62f105d)
  GG(rdx, rax, rbx, rcx, 10,  9, 0x02441453)
  GG(rcx, rdx, rax, rbx, 15, 14, 0xd8a1e681)
  GG(rbx, rcx, rdx, rax,  4, 20, 0xe7d3fbc8)
  GG(rax, rbx, rcx, rdx,  9,  5, 0x21e1cde6)
  GG(rdx, rax, rbx, rcx, 14,  9, 0xc33707d6)
  GG(rcx, rdx, rax, rbx,  3, 14, 0xf4d50d87)
  GG(rbx, rcx, rdx, rax,  8, 20, 0x455a14ed)
  GG(rax, rbx, rcx, rdx, 13,  5, 0xa9e3e905)
  GG(rdx, rax, rbx, rcx,  2,  9, 0xfcefa3f8)
  GG(rcx, rdx, rax, rbx,  7, 14, 0x676f02d9)
  GG(rbx, rcx, rdx, rax, 12, 20, 0x8d2a4c8a)

  // Round 3
  HH(rax, rbx, rcx, rdx,  5,  4, 0xfffa3942)
  HH(rdx, rax, rbx, rcx,  8, 11, 0x8771f681)
  HH(rcx, rdx, rax, rbx, 11, 16, 0x6d9d6122)
  HH(rbx, rcx, rdx, rax, 14, 23, 0xfde5380c)
  HH(rax, rbx, rcx, rdx,  1,  4, 0xa4beea44)
  HH(rdx, rax, rbx, rcx,  4, 11, 0x4bdecfa9)
  HH(rcx, rdx, rax, rbx,  7, 16, 0xf6bb4b60)
  HH(rbx, rcx, rdx, rax, 10, 23, 0xbebfbc70)
  HH(rax, rbx, rcx, rdx, 13,  4, 0x289b7ec6)
  HH(rdx, rax, rbx, rcx,  0, 11, 0xeaa127fa)
  HH(rcx, rdx, rax, rbx,  3, 16, 0xd4ef3085)
  HH(rbx, rcx, rdx, rax,  6, 23, 0x04881d05)
  HH(rax, rbx, rcx, rdx,  9,  4, 0xd9d4d039)
  HH(rdx, rax, rbx, rcx, 12, 11, 0xe6db99e5)
  HH(rcx, rdx, rax, rbx, 15, 16, 0x1fa27cf8)
  HH(rbx, rcx, rdx, rax,  2, 23, 0xc4ac5665)

  // Round 4
  II(rax, rbx, rcx, rdx,  0,  6, 0xf4292244)
  II(rdx, rax, rbx, rcx,  7, 10, 0x432aff97)
  II(rcx, rdx, rax, rbx, 14, 15, 0xab9423a7)
  II(rbx, rcx, rdx, rax,  5, 21, 0xfc93a039)
  II(rax, rbx, rcx, rdx, 12,  6, 0x655b59c3)
  II(rdx, rax, rbx, rcx,  3, 10, 0x8f0ccc92)
  II(rcx, rdx, rax, rbx, 10, 15, 0xffeff47d)
  II(rbx, rcx, rdx, rax,  1, 21, 0x85845dd1)
  II(rax, rbx, rcx, rdx,  8,  6, 0x6fa87e4f)
  II(rdx, rax, rbx, rcx, 15, 10, 0xfe2ce6e0)
  II(rcx, rdx, rax, rbx,  6, 15, 0xa3014314)
  II(rbx, rcx, rdx, rax, 13, 21, 0x4e0811a1)
  II(rax, rbx, rcx, rdx,  4,  6, 0xf7537e82)
  II(rdx, rax, rbx, rcx, 11, 10, 0xbd3af235)
  II(rcx, rdx, rax, rbx,  2, 15, 0x2ad7d2bb)
  II(rbx, rcx, rdx, rax,  9, 21, 0xeb86d391)

#undef FF
#undef GG
#undef HH
#undef II

  // write hash values back in the correct order
  movptr(rdi, state);
  addl(Address(rdi,  0), rax);
  addl(Address(rdi,  4), rbx);
  addl(Address(rdi,  8), rcx);
  addl(Address(rdi, 12), rdx);

  if (multi_block) {
    // increment data pointer and loop if more to process
    addptr(buf, 64);
    addl(ofs, 64);
    movl(rsi, ofs);
    cmpl(rsi, limit);
    jcc(Assembler::belowEqual, loop0);
    movptr(rax, rsi); //return ofs
  }

  bind(done_hash);
}
