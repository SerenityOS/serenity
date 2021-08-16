// Copyright (c) 2017 Instituto de Pesquisas Eldorado. All rights reserved.
// DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
//
// This code is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License version 2 only, as
// published by the Free Software Foundation.
//
// This code is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
// version 2 for more details (a copy is included in the LICENSE file that
// accompanied this code).
//
// You should have received a copy of the GNU General Public License version
// 2 along with this work; if not, write to the Free Software Foundation,
// Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
//
// Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
// or visit www.oracle.com if you need additional information or have any
// questions.

// Implemented according to "Descriptions of SHA-256, SHA-384, and SHA-512"
// (http://www.iwar.org.uk/comsec/resources/cipher/sha256-384-512.pdf).

#include "asm/macroAssembler.inline.hpp"
#include "runtime/stubRoutines.hpp"

/**********************************************************************
 * SHA 256
 *********************************************************************/

void MacroAssembler::sha256_deque(const VectorRegister src,
                                  const VectorRegister dst1,
                                  const VectorRegister dst2,
                                  const VectorRegister dst3) {
  vsldoi (dst1, src, src, 12);
  vsldoi (dst2, src, src, 8);
  vsldoi (dst3, src, src, 4);
}

void MacroAssembler::sha256_round(const VectorRegister* hs,
                                  const int total_hs,
                                  int& h_cnt,
                                  const VectorRegister kpw) {
  // convenience registers: cycle from 0-7 downwards
  const VectorRegister a = hs[(total_hs + 0 - (h_cnt % total_hs)) % total_hs];
  const VectorRegister b = hs[(total_hs + 1 - (h_cnt % total_hs)) % total_hs];
  const VectorRegister c = hs[(total_hs + 2 - (h_cnt % total_hs)) % total_hs];
  const VectorRegister d = hs[(total_hs + 3 - (h_cnt % total_hs)) % total_hs];
  const VectorRegister e = hs[(total_hs + 4 - (h_cnt % total_hs)) % total_hs];
  const VectorRegister f = hs[(total_hs + 5 - (h_cnt % total_hs)) % total_hs];
  const VectorRegister g = hs[(total_hs + 6 - (h_cnt % total_hs)) % total_hs];
  const VectorRegister h = hs[(total_hs + 7 - (h_cnt % total_hs)) % total_hs];
  // temporaries
  VectorRegister ch  = VR0;
  VectorRegister maj = VR1;
  VectorRegister bsa = VR2;
  VectorRegister bse = VR3;
  VectorRegister vt0 = VR4;
  VectorRegister vt1 = VR5;
  VectorRegister vt2 = VR6;
  VectorRegister vt3 = VR7;

  vsel       (ch,  g,   f, e);
  vxor       (maj, a,   b);
  vshasigmaw (bse, e,   1, 0xf);
  vadduwm    (vt2, ch,  kpw);
  vadduwm    (vt1, h,   bse);
  vsel       (maj, b,   c, maj);
  vadduwm    (vt3, vt1, vt2);
  vshasigmaw (bsa, a,   1, 0);
  vadduwm    (vt0, bsa, maj);

  vadduwm    (d,   d,   vt3);
  vadduwm    (h,   vt3, vt0);

  // advance vector pointer to the next iteration
  h_cnt++;
}

void MacroAssembler::sha256_load_h_vec(const VectorRegister a,
                                       const VectorRegister e,
                                       const Register hptr) {
  // temporaries
  Register tmp = R8;
  VectorRegister vt0 = VR0;
  VectorRegister vRb = VR6;
  // labels
  Label sha256_aligned;

  andi_  (tmp,  hptr, 0xf);
  lvx    (a,    hptr);
  addi   (tmp,  hptr, 16);
  lvx    (e,    tmp);
  beq    (CCR0, sha256_aligned);

  // handle unaligned accesses
  load_perm(vRb, hptr);
  addi   (tmp, hptr, 32);
  vec_perm(a,   e,    vRb);

  lvx    (vt0,  tmp);
  vec_perm(e,   vt0,  vRb);

  // aligned accesses
  bind(sha256_aligned);
}

void MacroAssembler::sha256_load_w_plus_k_vec(const Register buf_in,
                                              const VectorRegister* ws,
                                              const int total_ws,
                                              const Register k,
                                              const VectorRegister* kpws,
                                              const int total_kpws) {
  Label w_aligned, after_w_load;

  Register tmp       = R8;
  VectorRegister vt0 = VR0;
  VectorRegister vt1 = VR1;
  VectorRegister vRb = VR6;

  andi_ (tmp, buf_in, 0xF);
  beq   (CCR0, w_aligned); // address ends with 0x0, not 0x8

  // deal with unaligned addresses
  lvx    (ws[0], buf_in);
  load_perm(vRb, buf_in);

  for (int n = 1; n < total_ws; n++) {
    VectorRegister w_cur = ws[n];
    VectorRegister w_prev = ws[n-1];

    addi (tmp, buf_in, n * 16);
    lvx  (w_cur, tmp);
    vec_perm(w_prev, w_cur, vRb);
  }
  addi   (tmp, buf_in, total_ws * 16);
  lvx    (vt0, tmp);
  vec_perm(ws[total_ws-1], vt0, vRb);
  b      (after_w_load);

  bind(w_aligned);

  // deal with aligned addresses
  lvx(ws[0], buf_in);
  for (int n = 1; n < total_ws; n++) {
    VectorRegister w = ws[n];
    addi (tmp, buf_in, n * 16);
    lvx  (w, tmp);
  }

  bind(after_w_load);

#if defined(VM_LITTLE_ENDIAN)
  // Byte swapping within int values
  li       (tmp, 8);
  lvsl     (vt0, tmp);
  vspltisb (vt1, 0xb);
  vxor     (vt1, vt0, vt1);
  for (int n = 0; n < total_ws; n++) {
    VectorRegister w = ws[n];
    vec_perm(w, w, vt1);
  }
#endif

  // Loading k, which is always aligned to 16-bytes
  lvx    (kpws[0], k);
  for (int n = 1; n < total_kpws; n++) {
    VectorRegister kpw = kpws[n];
    addi (tmp, k, 16 * n);
    lvx  (kpw, tmp);
  }

  // Add w to K
  assert(total_ws == total_kpws, "Redesign the loop below");
  for (int n = 0; n < total_kpws; n++) {
    VectorRegister kpw = kpws[n];
    VectorRegister w   = ws[n];

    vadduwm  (kpw, kpw, w);
  }
}

void MacroAssembler::sha256_calc_4w(const VectorRegister w0,
                                    const VectorRegister w1,
                                    const VectorRegister w2,
                                    const VectorRegister w3,
                                    const VectorRegister kpw0,
                                    const VectorRegister kpw1,
                                    const VectorRegister kpw2,
                                    const VectorRegister kpw3,
                                    const Register j,
                                    const Register k) {
  // Temporaries
  const VectorRegister  vt0  = VR0;
  const VectorRegister  vt1  = VR1;
  const VectorSRegister vsrt1 = vt1->to_vsr();
  const VectorRegister  vt2  = VR2;
  const VectorRegister  vt3  = VR3;
  const VectorSRegister vst3 = vt3->to_vsr();
  const VectorRegister  vt4  = VR4;

  // load to k[j]
  lvx        (vt0, j,   k);

  // advance j
  addi       (j,   j,   16); // 16 bytes were read

#if defined(VM_LITTLE_ENDIAN)
  // b = w[j-15], w[j-14], w[j-13], w[j-12]
  vsldoi     (vt1, w1,  w0, 12);

  // c = w[j-7], w[j-6], w[j-5], w[j-4]
  vsldoi     (vt2, w3,  w2, 12);

#else
  // b = w[j-15], w[j-14], w[j-13], w[j-12]
  vsldoi     (vt1, w0,  w1, 4);

  // c = w[j-7], w[j-6], w[j-5], w[j-4]
  vsldoi     (vt2, w2,  w3, 4);
#endif

  // d = w[j-2], w[j-1], w[j-4], w[j-3]
  vsldoi     (vt3, w3,  w3, 8);

  // b = s0(w[j-15]) , s0(w[j-14]) , s0(w[j-13]) , s0(w[j-12])
  vshasigmaw (vt1, vt1, 0,  0);

  // d = s1(w[j-2]) , s1(w[j-1]) , s1(w[j-4]) , s1(w[j-3])
  vshasigmaw (vt3, vt3, 0,  0xf);

  // c = s0(w[j-15]) + w[j-7],
  //     s0(w[j-14]) + w[j-6],
  //     s0(w[j-13]) + w[j-5],
  //     s0(w[j-12]) + w[j-4]
  vadduwm    (vt2, vt1, vt2);

  // c = s0(w[j-15]) + w[j-7] + w[j-16],
  //     s0(w[j-14]) + w[j-6] + w[j-15],
  //     s0(w[j-13]) + w[j-5] + w[j-14],
  //     s0(w[j-12]) + w[j-4] + w[j-13]
  vadduwm    (vt2, vt2, w0);

  // e = s0(w[j-15]) + w[j-7] + w[j-16] + s1(w[j-2]), // w[j]
  //     s0(w[j-14]) + w[j-6] + w[j-15] + s1(w[j-1]), // w[j+1]
  //     s0(w[j-13]) + w[j-5] + w[j-14] + s1(w[j-4]), // UNDEFINED
  //     s0(w[j-12]) + w[j-4] + w[j-13] + s1(w[j-3])  // UNDEFINED
  vadduwm    (vt4, vt2, vt3);

  // At this point, e[0] and e[1] are the correct values to be stored at w[j]
  // and w[j+1].
  // e[2] and e[3] are not considered.
  // b = s1(w[j]) , s1(s(w[j+1]) , UNDEFINED , UNDEFINED
  vshasigmaw (vt1, vt4, 0,  0xf);

  // v5 = s1(w[j-2]) , s1(w[j-1]) , s1(w[j]) , s1(w[j+1])
#if defined(VM_LITTLE_ENDIAN)
  xxmrgld    (vst3, vsrt1, vst3);
#else
  xxmrghd    (vst3, vst3, vsrt1);
#endif

  // c = s0(w[j-15]) + w[j-7] + w[j-16] + s1(w[j-2]), // w[j]
  //     s0(w[j-14]) + w[j-6] + w[j-15] + s1(w[j-1]), // w[j+1]
  //     s0(w[j-13]) + w[j-5] + w[j-14] + s1(w[j]),   // w[j+2]
  //     s0(w[j-12]) + w[j-4] + w[j-13] + s1(w[j+1])  // w[j+4]
  vadduwm    (vt2, vt2, vt3);

  // Updating w0 to w3 to hold the new previous 16 values from w.
  vmr        (w0,  w1);
  vmr        (w1,  w2);
  vmr        (w2,  w3);
  vmr        (w3,  vt2);

  // store k + w to v9 (4 values at once)
#if defined(VM_LITTLE_ENDIAN)
  vadduwm    (kpw0, vt2, vt0);

  vsldoi     (kpw1, kpw0, kpw0, 12);
  vsldoi     (kpw2, kpw0, kpw0, 8);
  vsldoi     (kpw3, kpw0, kpw0, 4);
#else
  vadduwm    (kpw3, vt2, vt0);

  vsldoi     (kpw2, kpw3, kpw3, 12);
  vsldoi     (kpw1, kpw3, kpw3, 8);
  vsldoi     (kpw0, kpw3, kpw3, 4);
#endif
}

void MacroAssembler::sha256_update_sha_state(const VectorRegister a,
                                             const VectorRegister b_,
                                             const VectorRegister c,
                                             const VectorRegister d,
                                             const VectorRegister e,
                                             const VectorRegister f,
                                             const VectorRegister g,
                                             const VectorRegister h,
                                             const Register hptr) {
  // temporaries
  VectorRegister vt0  = VR0;
  VectorRegister vt1  = VR1;
  VectorRegister vt2  = VR2;
  VectorRegister vt3  = VR3;
  VectorRegister vt4  = VR4;
  VectorRegister vt5  = VR5;
  VectorRegister vaux = VR6;
  VectorRegister vRb  = VR6;
  Register tmp        = R8;
  Register of16       = R8;
  Register of32       = R9;
  Label state_load_aligned;

  // Load hptr
  andi_   (tmp, hptr, 0xf);
  li      (of16, 16);
  lvx     (vt0, hptr);
  lvx     (vt5, of16, hptr);
  beq     (CCR0, state_load_aligned);

  // handle unaligned accesses
  li      (of32, 32);
  load_perm(vRb, hptr);

  vec_perm(vt0, vt5,  vRb);        // vt0 = hptr[0]..hptr[3]

  lvx     (vt1, hptr, of32);
  vec_perm(vt5, vt1,  vRb);        // vt5 = hptr[4]..hptr[7]

  // aligned accesses
  bind(state_load_aligned);

#if defined(VM_LITTLE_ENDIAN)
  vmrglw  (vt1, b_, a);            // vt1 = {a, b, ?, ?}
  vmrglw  (vt2, d, c);             // vt2 = {c, d, ?, ?}
  vmrglw  (vt3, f, e);             // vt3 = {e, f, ?, ?}
  vmrglw  (vt4, h, g);             // vt4 = {g, h, ?, ?}
  xxmrgld (vt1->to_vsr(), vt2->to_vsr(), vt1->to_vsr()); // vt1 = {a, b, c, d}
  xxmrgld (vt3->to_vsr(), vt4->to_vsr(), vt3->to_vsr()); // vt3 = {e, f, g, h}
  vadduwm (a,   vt0, vt1);         // a = {a+hptr[0], b+hptr[1], c+hptr[2], d+hptr[3]}
  vadduwm (e,   vt5, vt3);         // e = {e+hptr[4], f+hptr[5], g+hptr[6], h+hptr[7]}

  // Save hptr back, works for any alignment
  xxswapd (vt0->to_vsr(), a->to_vsr());
  stxvd2x (vt0->to_vsr(), hptr);
  xxswapd (vt5->to_vsr(), e->to_vsr());
  stxvd2x (vt5->to_vsr(), of16, hptr);
#else
  vmrglw  (vt1, a, b_);            // vt1 = {a, b, ?, ?}
  vmrglw  (vt2, c, d);             // vt2 = {c, d, ?, ?}
  vmrglw  (vt3, e, f);             // vt3 = {e, f, ?, ?}
  vmrglw  (vt4, g, h);             // vt4 = {g, h, ?, ?}
  xxmrgld (vt1->to_vsr(), vt1->to_vsr(), vt2->to_vsr()); // vt1 = {a, b, c, d}
  xxmrgld (vt3->to_vsr(), vt3->to_vsr(), vt4->to_vsr()); // vt3 = {e, f, g, h}
  vadduwm (d,   vt0, vt1);         // d = {a+hptr[0], b+hptr[1], c+hptr[2], d+hptr[3]}
  vadduwm (h,   vt5, vt3);         // h = {e+hptr[4], f+hptr[5], g+hptr[6], h+hptr[7]}

  // Save hptr back, works for any alignment
  stxvd2x (d->to_vsr(), hptr);
  stxvd2x (h->to_vsr(), of16, hptr);
#endif
}

static const uint32_t sha256_round_table[64] __attribute((aligned(16))) = {
  0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5,
  0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
  0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3,
  0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
  0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc,
  0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
  0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7,
  0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
  0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
  0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
  0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3,
  0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
  0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
  0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
  0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208,
  0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2,
};
static const uint32_t *sha256_round_consts = sha256_round_table;

//   R3_ARG1   - byte[]  Input string with padding but in Big Endian
//   R4_ARG2   - int[]   SHA.state (at first, the root of primes)
//   R5_ARG3   - int     offset
//   R6_ARG4   - int     limit
//
//   Internal Register usage:
//   R7        - k
//   R8        - tmp | j | of16
//   R9        - of32
//   VR0-VR8   - ch, maj, bsa, bse, vt0-vt3 | vt0-vt5, vaux/vRb
//   VR9-VR16  - a-h
//   VR17-VR20 - w0-w3
//   VR21-VR23 - vRb | vaux0-vaux2
//   VR24-VR27 - kpw0-kpw3
void MacroAssembler::sha256(bool multi_block) {
  static const ssize_t buf_size = 64;
  static const uint8_t w_size = sizeof(sha256_round_table)/sizeof(uint32_t);
#ifdef AIX
  // malloc provides 16 byte alignment
  if (((uintptr_t)sha256_round_consts & 0xF) != 0) {
    uint32_t *new_round_consts = (uint32_t*)malloc(sizeof(sha256_round_table));
    guarantee(new_round_consts, "oom");
    memcpy(new_round_consts, sha256_round_consts, sizeof(sha256_round_table));
    sha256_round_consts = (const uint32_t*)new_round_consts;
  }
#endif

  Register buf_in = R3_ARG1;
  Register state  = R4_ARG2;
  Register ofs    = R5_ARG3;
  Register limit  = R6_ARG4;

  Label sha_loop, core_loop;

  // Save non-volatile vector registers in the red zone
  static const VectorRegister nv[] = {
    VR20, VR21, VR22, VR23, VR24, VR25, VR26, VR27/*, VR28, VR29, VR30, VR31*/
  };
  static const uint8_t nv_size = sizeof(nv) / sizeof (VectorRegister);

  for (int c = 0; c < nv_size; c++) {
    Register tmp = R8;
    li  (tmp, (c - (nv_size)) * 16);
    stvx(nv[c], tmp, R1);
  }

  // Load hash state to registers
  VectorRegister a = VR9;
  VectorRegister b = VR10;
  VectorRegister c = VR11;
  VectorRegister d = VR12;
  VectorRegister e = VR13;
  VectorRegister f = VR14;
  VectorRegister g = VR15;
  VectorRegister h = VR16;
  static const VectorRegister hs[] = {a, b, c, d, e, f, g, h};
  static const int total_hs = sizeof(hs)/sizeof(VectorRegister);
  // counter for cycling through hs vector to avoid register moves between iterations
  int h_cnt = 0;

  // Load a-h registers from the memory pointed by state
#if defined(VM_LITTLE_ENDIAN)
  sha256_load_h_vec(a, e, state);
#else
  sha256_load_h_vec(d, h, state);
#endif

  // keep k loaded also during MultiBlock loops
  Register k = R7;
  assert(((uintptr_t)sha256_round_consts & 0xF) == 0, "k alignment");
  load_const_optimized(k, (address)sha256_round_consts, R0);

  // Avoiding redundant loads
  if (multi_block) {
    align(OptoLoopAlignment);
  }
  bind(sha_loop);
#if defined(VM_LITTLE_ENDIAN)
  sha256_deque(a, b, c, d);
  sha256_deque(e, f, g, h);
#else
  sha256_deque(d, c, b, a);
  sha256_deque(h, g, f, e);
#endif

  // Load 16 elements from w out of the loop.
  // Order of the int values is Endianess specific.
  VectorRegister w0 = VR17;
  VectorRegister w1 = VR18;
  VectorRegister w2 = VR19;
  VectorRegister w3 = VR20;
  static const VectorRegister ws[] = {w0, w1, w2, w3};
  static const int total_ws = sizeof(ws)/sizeof(VectorRegister);

  VectorRegister kpw0 = VR24;
  VectorRegister kpw1 = VR25;
  VectorRegister kpw2 = VR26;
  VectorRegister kpw3 = VR27;
  static const VectorRegister kpws[] = {kpw0, kpw1, kpw2, kpw3};
  static const int total_kpws = sizeof(kpws)/sizeof(VectorRegister);

  sha256_load_w_plus_k_vec(buf_in, ws, total_ws, k, kpws, total_kpws);

  // Cycle through the first 16 elements
  assert(total_ws == total_kpws, "Redesign the loop below");
  for (int n = 0; n < total_ws; n++) {
    VectorRegister vaux0 = VR21;
    VectorRegister vaux1 = VR22;
    VectorRegister vaux2 = VR23;

    sha256_deque(kpws[n], vaux0, vaux1, vaux2);

#if defined(VM_LITTLE_ENDIAN)
    sha256_round(hs, total_hs, h_cnt, kpws[n]);
    sha256_round(hs, total_hs, h_cnt, vaux0);
    sha256_round(hs, total_hs, h_cnt, vaux1);
    sha256_round(hs, total_hs, h_cnt, vaux2);
#else
    sha256_round(hs, total_hs, h_cnt, vaux2);
    sha256_round(hs, total_hs, h_cnt, vaux1);
    sha256_round(hs, total_hs, h_cnt, vaux0);
    sha256_round(hs, total_hs, h_cnt, kpws[n]);
#endif
  }

  Register tmp = R8;
  // loop the 16th to the 64th iteration by 8 steps
  li   (tmp, (w_size - 16) / total_hs);
  mtctr(tmp);

  // j will be aligned to 4 for loading words.
  // Whenever read, advance the pointer (e.g: when j is used in a function)
  Register j = R8;
  li   (j, 16*4);

  align(OptoLoopAlignment);
  bind(core_loop);

  // due to VectorRegister rotate, always iterate in multiples of total_hs
  for (int n = 0; n < total_hs/4; n++) {
    sha256_calc_4w(w0, w1, w2, w3, kpw0, kpw1, kpw2, kpw3, j, k);
    sha256_round(hs, total_hs, h_cnt, kpw0);
    sha256_round(hs, total_hs, h_cnt, kpw1);
    sha256_round(hs, total_hs, h_cnt, kpw2);
    sha256_round(hs, total_hs, h_cnt, kpw3);
  }

  bdnz   (core_loop);

  // Update hash state
  sha256_update_sha_state(a, b, c, d, e, f, g, h, state);

  if (multi_block) {
    addi(buf_in, buf_in, buf_size);
    addi(ofs, ofs, buf_size);
    cmplw(CCR0, ofs, limit);
    ble(CCR0, sha_loop);

    // return ofs
    mr(R3_RET, ofs);
  }

  // Restore non-volatile registers
  for (int c = 0; c < nv_size; c++) {
    Register tmp = R8;
    li  (tmp, (c - (nv_size)) * 16);
    lvx(nv[c], tmp, R1);
  }
}


/**********************************************************************
 * SHA 512
 *********************************************************************/

void MacroAssembler::sha512_load_w_vec(const Register buf_in,
                                       const VectorRegister* ws,
                                       const int total_ws) {
  Register tmp       = R8;
  VectorRegister vRb = VR8;
  VectorRegister aux = VR9;
  Label is_aligned, after_alignment;

  andi_  (tmp, buf_in, 0xF);
  beq    (CCR0, is_aligned); // address ends with 0x0, not 0x8

  // deal with unaligned addresses
  lvx    (ws[0], buf_in);
  load_perm(vRb, buf_in);

  for (int n = 1; n < total_ws; n++) {
    VectorRegister w_cur = ws[n];
    VectorRegister w_prev = ws[n-1];
    addi (tmp, buf_in, n * 16);
    lvx  (w_cur, tmp);
    vec_perm(w_prev, w_cur, vRb);
  }
  addi   (tmp, buf_in, total_ws * 16);
  lvx    (aux, tmp);
  vec_perm(ws[total_ws-1], aux, vRb);
  b      (after_alignment);

  bind(is_aligned);
  lvx  (ws[0], buf_in);
  for (int n = 1; n < total_ws; n++) {
    VectorRegister w = ws[n];
    addi (tmp, buf_in, n * 16);
    lvx  (w, tmp);
  }

  bind(after_alignment);
}

// Update hash state
void MacroAssembler::sha512_update_sha_state(const Register state,
                                             const VectorRegister* hs,
                                             const int total_hs) {

#if defined(VM_LITTLE_ENDIAN)
  int start_idx = 0;
#else
  int start_idx = 1;
#endif

  // load initial hash from the memory pointed by state
  VectorRegister ini_a = VR10;
  VectorRegister ini_c = VR12;
  VectorRegister ini_e = VR14;
  VectorRegister ini_g = VR16;
  static const VectorRegister inis[] = {ini_a, ini_c, ini_e, ini_g};
  static const int total_inis = sizeof(inis)/sizeof(VectorRegister);

  Label state_save_aligned, after_state_save_aligned;

  Register addr      = R7;
  Register tmp       = R8;
  VectorRegister vRb = VR8;
  VectorRegister aux = VR9;

  andi_(tmp, state, 0xf);
  beq(CCR0, state_save_aligned);
  // deal with unaligned addresses

  {
    VectorRegister a = hs[0];
    VectorRegister b_ = hs[1];
    VectorRegister c = hs[2];
    VectorRegister d = hs[3];
    VectorRegister e = hs[4];
    VectorRegister f = hs[5];
    VectorRegister g = hs[6];
    VectorRegister h = hs[7];
    load_perm(vRb, state);
    lvx    (ini_a, state);
    addi   (addr, state, 16);

    lvx    (ini_c, addr);
    addi   (addr, state, 32);
    vec_perm(ini_a, ini_c, vRb);

    lvx    (ini_e, addr);
    addi   (addr, state, 48);
    vec_perm(ini_c, ini_e, vRb);

    lvx    (ini_g, addr);
    addi   (addr, state, 64);
    vec_perm(ini_e, ini_g, vRb);

    lvx    (aux, addr);
    vec_perm(ini_g, aux, vRb);

#if defined(VM_LITTLE_ENDIAN)
    xxmrgld(a->to_vsr(), b_->to_vsr(), a->to_vsr());
    xxmrgld(c->to_vsr(), d->to_vsr(), c->to_vsr());
    xxmrgld(e->to_vsr(), f->to_vsr(), e->to_vsr());
    xxmrgld(g->to_vsr(), h->to_vsr(), g->to_vsr());
#else
    xxmrgld(b_->to_vsr(), a->to_vsr(), b_->to_vsr());
    xxmrgld(d->to_vsr(), c->to_vsr(), d->to_vsr());
    xxmrgld(f->to_vsr(), e->to_vsr(), f->to_vsr());
    xxmrgld(h->to_vsr(), g->to_vsr(), h->to_vsr());
#endif

    for (int n = start_idx; n < total_hs; n += 2) {
      VectorRegister h_cur = hs[n];
      VectorRegister ini_cur = inis[n/2];

      vaddudm(h_cur, ini_cur, h_cur);
    }

    for (int n = start_idx; n < total_hs; n += 2) {
      VectorRegister h_cur = hs[n];

      mfvrd  (tmp, h_cur);
#if defined(VM_LITTLE_ENDIAN)
      std    (tmp, 8*n + 8, state);
#else
      std    (tmp, 8*n - 8, state);
#endif
      vsldoi (aux, h_cur, h_cur, 8);
      mfvrd  (tmp, aux);
      std    (tmp, 8*n + 0, state);
    }

    b      (after_state_save_aligned);
  }

  bind(state_save_aligned);
  {
    for (int n = 0; n < total_hs; n += 2) {
#if defined(VM_LITTLE_ENDIAN)
      VectorRegister h_cur = hs[n];
      VectorRegister h_next = hs[n+1];
#else
      VectorRegister h_cur = hs[n+1];
      VectorRegister h_next = hs[n];
#endif
      VectorRegister ini_cur = inis[n/2];

      if (n/2 == 0) {
        lvx(ini_cur, state);
      } else {
        addi(addr, state, (n/2) * 16);
        lvx(ini_cur, addr);
      }
      xxmrgld(h_cur->to_vsr(), h_next->to_vsr(), h_cur->to_vsr());
    }

    for (int n = start_idx; n < total_hs; n += 2) {
      VectorRegister h_cur = hs[n];
      VectorRegister ini_cur = inis[n/2];

      vaddudm(h_cur, ini_cur, h_cur);
    }

    for (int n = start_idx; n < total_hs; n += 2) {
      VectorRegister h_cur = hs[n];

      if (n/2 == 0) {
        stvx(h_cur, state);
      } else {
        addi(addr, state, (n/2) * 16);
        stvx(h_cur, addr);
      }
    }
  }

  bind(after_state_save_aligned);
}

// Use h_cnt to cycle through hs elements but also increment it at the end
void MacroAssembler::sha512_round(const VectorRegister* hs,
                                  const int total_hs, int& h_cnt,
                                  const VectorRegister kpw) {

  // convenience registers: cycle from 0-7 downwards
  const VectorRegister a = hs[(total_hs + 0 - (h_cnt % total_hs)) % total_hs];
  const VectorRegister b = hs[(total_hs + 1 - (h_cnt % total_hs)) % total_hs];
  const VectorRegister c = hs[(total_hs + 2 - (h_cnt % total_hs)) % total_hs];
  const VectorRegister d = hs[(total_hs + 3 - (h_cnt % total_hs)) % total_hs];
  const VectorRegister e = hs[(total_hs + 4 - (h_cnt % total_hs)) % total_hs];
  const VectorRegister f = hs[(total_hs + 5 - (h_cnt % total_hs)) % total_hs];
  const VectorRegister g = hs[(total_hs + 6 - (h_cnt % total_hs)) % total_hs];
  const VectorRegister h = hs[(total_hs + 7 - (h_cnt % total_hs)) % total_hs];
  // temporaries
  const VectorRegister Ch   = VR20;
  const VectorRegister Maj  = VR21;
  const VectorRegister bsa  = VR22;
  const VectorRegister bse  = VR23;
  const VectorRegister tmp1 = VR24;
  const VectorRegister tmp2 = VR25;

  vsel      (Ch,   g,    f,   e);
  vxor      (Maj,  a,    b);
  vshasigmad(bse,  e,    1,   0xf);
  vaddudm   (tmp2, Ch,   kpw);
  vaddudm   (tmp1, h,    bse);
  vsel      (Maj,  b,    c,   Maj);
  vaddudm   (tmp1, tmp1, tmp2);
  vshasigmad(bsa,  a,    1,   0);
  vaddudm   (tmp2, bsa,  Maj);
  vaddudm   (d,    d,    tmp1);
  vaddudm   (h,    tmp1, tmp2);

  // advance vector pointer to the next iteration
  h_cnt++;
}

void MacroAssembler::sha512_calc_2w(const VectorRegister w0,
                                    const VectorRegister w1,
                                    const VectorRegister w2,
                                    const VectorRegister w3,
                                    const VectorRegister w4,
                                    const VectorRegister w5,
                                    const VectorRegister w6,
                                    const VectorRegister w7,
                                    const VectorRegister kpw0,
                                    const VectorRegister kpw1,
                                    const Register j,
                                    const VectorRegister vRb,
                                    const Register k) {
  // Temporaries
  const VectorRegister VR_a = VR20;
  const VectorRegister VR_b = VR21;
  const VectorRegister VR_c = VR22;
  const VectorRegister VR_d = VR23;

  // load to k[j]
  lvx        (VR_a, j,    k);
  // advance j
  addi       (j,    j,    16); // 16 bytes were read

#if defined(VM_LITTLE_ENDIAN)
  // v6 = w[j-15], w[j-14]
  vperm      (VR_b, w1,   w0,  vRb);
  // v12 = w[j-7], w[j-6]
  vperm      (VR_c, w5,   w4,  vRb);
#else
  // v6 = w[j-15], w[j-14]
  vperm      (VR_b, w0,   w1,  vRb);
  // v12 = w[j-7], w[j-6]
  vperm      (VR_c, w4,   w5,  vRb);
#endif

  // v6 = s0(w[j-15]) , s0(w[j-14])
  vshasigmad (VR_b, VR_b,    0,   0);
  // v5 = s1(w[j-2]) , s1(w[j-1])
  vshasigmad (VR_d, w7,      0,   0xf);
  // v6 = s0(w[j-15]) + w[j-7] , s0(w[j-14]) + w[j-6]
  vaddudm    (VR_b, VR_b, VR_c);
  // v8 = s1(w[j-2]) + w[j-16] , s1(w[j-1]) + w[j-15]
  vaddudm    (VR_d, VR_d, w0);
  // v9 = s0(w[j-15]) + w[j-7] + w[j-16] + s1(w[j-2]), // w[j]
  //      s0(w[j-14]) + w[j-6] + w[j-15] + s1(w[j-1]), // w[j+1]
  vaddudm    (VR_c, VR_d, VR_b);
  // Updating w0 to w7 to hold the new previous 16 values from w.
  vmr        (w0,   w1);
  vmr        (w1,   w2);
  vmr        (w2,   w3);
  vmr        (w3,   w4);
  vmr        (w4,   w5);
  vmr        (w5,   w6);
  vmr        (w6,   w7);
  vmr        (w7,   VR_c);

#if defined(VM_LITTLE_ENDIAN)
  // store k + w to kpw0 (2 values at once)
  vaddudm    (kpw0, VR_c, VR_a);
  // kpw1 holds (k + w)[1]
  vsldoi     (kpw1, kpw0, kpw0, 8);
#else
  // store k + w to kpw0 (2 values at once)
  vaddudm    (kpw1, VR_c, VR_a);
  // kpw1 holds (k + w)[1]
  vsldoi     (kpw0, kpw1, kpw1, 8);
#endif
}

void MacroAssembler::sha512_load_h_vec(const Register state,
                                       const VectorRegister* hs,
                                       const int total_hs) {
#if defined(VM_LITTLE_ENDIAN)
  VectorRegister a   = hs[0];
  VectorRegister g   = hs[6];
  int start_idx = 0;
#else
  VectorRegister a   = hs[1];
  VectorRegister g   = hs[7];
  int start_idx = 1;
#endif

  Register addr      = R7;
  VectorRegister vRb = VR8;
  Register tmp       = R8;
  Label state_aligned, after_state_aligned;

  andi_(tmp, state, 0xf);
  beq(CCR0, state_aligned);

  // deal with unaligned addresses
  VectorRegister aux = VR9;

  lvx(hs[start_idx], state);
  load_perm(vRb, state);

  for (int n = start_idx + 2; n < total_hs; n += 2) {
    VectorRegister h_cur   = hs[n];
    VectorRegister h_prev2 = hs[n - 2];
    addi(addr, state, (n/2) * 16);
    lvx(h_cur, addr);
    vec_perm(h_prev2, h_cur, vRb);
  }
  addi(addr, state, (total_hs/2) * 16);
  lvx    (aux, addr);
  vec_perm(hs[total_hs - 2 + start_idx], aux, vRb);
  b      (after_state_aligned);

  bind(state_aligned);

  // deal with aligned addresses
  lvx(hs[start_idx], state);

  for (int n = start_idx + 2; n < total_hs; n += 2) {
    VectorRegister h_cur = hs[n];
    addi(addr, state, (n/2) * 16);
    lvx(h_cur, addr);
  }

  bind(after_state_aligned);
}

static const uint64_t sha512_round_table[80] __attribute((aligned(16))) = {
  0x428a2f98d728ae22, 0x7137449123ef65cd,
  0xb5c0fbcfec4d3b2f, 0xe9b5dba58189dbbc,
  0x3956c25bf348b538, 0x59f111f1b605d019,
  0x923f82a4af194f9b, 0xab1c5ed5da6d8118,
  0xd807aa98a3030242, 0x12835b0145706fbe,
  0x243185be4ee4b28c, 0x550c7dc3d5ffb4e2,
  0x72be5d74f27b896f, 0x80deb1fe3b1696b1,
  0x9bdc06a725c71235, 0xc19bf174cf692694,
  0xe49b69c19ef14ad2, 0xefbe4786384f25e3,
  0x0fc19dc68b8cd5b5, 0x240ca1cc77ac9c65,
  0x2de92c6f592b0275, 0x4a7484aa6ea6e483,
  0x5cb0a9dcbd41fbd4, 0x76f988da831153b5,
  0x983e5152ee66dfab, 0xa831c66d2db43210,
  0xb00327c898fb213f, 0xbf597fc7beef0ee4,
  0xc6e00bf33da88fc2, 0xd5a79147930aa725,
  0x06ca6351e003826f, 0x142929670a0e6e70,
  0x27b70a8546d22ffc, 0x2e1b21385c26c926,
  0x4d2c6dfc5ac42aed, 0x53380d139d95b3df,
  0x650a73548baf63de, 0x766a0abb3c77b2a8,
  0x81c2c92e47edaee6, 0x92722c851482353b,
  0xa2bfe8a14cf10364, 0xa81a664bbc423001,
  0xc24b8b70d0f89791, 0xc76c51a30654be30,
  0xd192e819d6ef5218, 0xd69906245565a910,
  0xf40e35855771202a, 0x106aa07032bbd1b8,
  0x19a4c116b8d2d0c8, 0x1e376c085141ab53,
  0x2748774cdf8eeb99, 0x34b0bcb5e19b48a8,
  0x391c0cb3c5c95a63, 0x4ed8aa4ae3418acb,
  0x5b9cca4f7763e373, 0x682e6ff3d6b2b8a3,
  0x748f82ee5defb2fc, 0x78a5636f43172f60,
  0x84c87814a1f0ab72, 0x8cc702081a6439ec,
  0x90befffa23631e28, 0xa4506cebde82bde9,
  0xbef9a3f7b2c67915, 0xc67178f2e372532b,
  0xca273eceea26619c, 0xd186b8c721c0c207,
  0xeada7dd6cde0eb1e, 0xf57d4f7fee6ed178,
  0x06f067aa72176fba, 0x0a637dc5a2c898a6,
  0x113f9804bef90dae, 0x1b710b35131c471b,
  0x28db77f523047d84, 0x32caab7b40c72493,
  0x3c9ebe0a15c9bebc, 0x431d67c49c100d4c,
  0x4cc5d4becb3e42b6, 0x597f299cfc657e2a,
  0x5fcb6fab3ad6faec, 0x6c44198c4a475817,
};
static const uint64_t *sha512_round_consts = sha512_round_table;

//   R3_ARG1   - byte[]  Input string with padding but in Big Endian
//   R4_ARG2   - int[]   SHA.state (at first, the root of primes)
//   R5_ARG3   - int     offset
//   R6_ARG4   - int     limit
//
//   Internal Register usage:
//   R7 R8 R9  - volatile temporaries
//   VR0-VR7   - a-h
//   VR8       - vRb
//   VR9       - aux (highly volatile, use with care)
//   VR10-VR17 - w0-w7 | ini_a-ini_h
//   VR18      - vsp16 | kplusw0
//   VR19      - vsp32 | kplusw1
//   VR20-VR25 - sha512_calc_2w and sha512_round temporaries
void MacroAssembler::sha512(bool multi_block) {
  static const ssize_t buf_size = 128;
  static const uint8_t w_size = sizeof(sha512_round_table)/sizeof(uint64_t);
#ifdef AIX
  // malloc provides 16 byte alignment
  if (((uintptr_t)sha512_round_consts & 0xF) != 0) {
    uint64_t *new_round_consts = (uint64_t*)malloc(sizeof(sha512_round_table));
    guarantee(new_round_consts, "oom");
    memcpy(new_round_consts, sha512_round_consts, sizeof(sha512_round_table));
    sha512_round_consts = (const uint64_t*)new_round_consts;
  }
#endif

  Register buf_in = R3_ARG1;
  Register state  = R4_ARG2;
  Register ofs    = R5_ARG3;
  Register limit  = R6_ARG4;

  Label sha_loop, core_loop;

  // Save non-volatile vector registers in the red zone
  static const VectorRegister nv[] = {
    VR20, VR21, VR22, VR23, VR24, VR25/*, VR26, VR27, VR28, VR29, VR30, VR31*/
  };
  static const uint8_t nv_size = sizeof(nv) / sizeof (VectorRegister);

  for (int c = 0; c < nv_size; c++) {
    Register idx = R7;
    li  (idx, (c - (nv_size)) * 16);
    stvx(nv[c], idx, R1);
  }

  // Load hash state to registers
  VectorRegister a = VR0;
  VectorRegister b = VR1;
  VectorRegister c = VR2;
  VectorRegister d = VR3;
  VectorRegister e = VR4;
  VectorRegister f = VR5;
  VectorRegister g = VR6;
  VectorRegister h = VR7;
  static const VectorRegister hs[] = {a, b, c, d, e, f, g, h};
  static const int total_hs = sizeof(hs)/sizeof(VectorRegister);
  // counter for cycling through hs vector to avoid register moves between iterations
  int h_cnt = 0;

  // Load a-h registers from the memory pointed by state
  sha512_load_h_vec(state, hs, total_hs);

  Register k = R9;
  assert(((uintptr_t)sha512_round_consts & 0xF) == 0, "k alignment");
  load_const_optimized(k, (address)sha512_round_consts, R0);

  if (multi_block) {
    align(OptoLoopAlignment);
  }
  bind(sha_loop);

  for (int n = 0; n < total_hs; n += 2) {
#if defined(VM_LITTLE_ENDIAN)
    VectorRegister h_cur = hs[n];
    VectorRegister h_next = hs[n + 1];
#else
    VectorRegister h_cur = hs[n + 1];
    VectorRegister h_next = hs[n];
#endif
    vsldoi (h_next, h_cur, h_cur, 8);
  }

  // Load 16 elements from w out of the loop.
  // Order of the long values is Endianess specific.
  VectorRegister w0 = VR10;
  VectorRegister w1 = VR11;
  VectorRegister w2 = VR12;
  VectorRegister w3 = VR13;
  VectorRegister w4 = VR14;
  VectorRegister w5 = VR15;
  VectorRegister w6 = VR16;
  VectorRegister w7 = VR17;
  static const VectorRegister ws[] = {w0, w1, w2, w3, w4, w5, w6, w7};
  static const int total_ws = sizeof(ws)/sizeof(VectorRegister);

  // Load 16 w into vectors and setup vsl for vperm
  sha512_load_w_vec(buf_in, ws, total_ws);

#if defined(VM_LITTLE_ENDIAN)
  VectorRegister vsp16 = VR18;
  VectorRegister vsp32 = VR19;
  VectorRegister shiftarg = VR9;

  vspltisw(vsp16,    8);
  vspltisw(shiftarg, 1);
  vsl     (vsp16,    vsp16, shiftarg);
  vsl     (vsp32,    vsp16, shiftarg);

  VectorRegister vsp8 = VR9;
  vspltish(vsp8,     8);

  // Convert input from Big Endian to Little Endian
  for (int c = 0; c < total_ws; c++) {
    VectorRegister w = ws[c];
    vrlh  (w, w, vsp8);
  }
  for (int c = 0; c < total_ws; c++) {
    VectorRegister w = ws[c];
    vrlw  (w, w, vsp16);
  }
  for (int c = 0; c < total_ws; c++) {
    VectorRegister w = ws[c];
    vrld  (w, w, vsp32);
  }
#endif

  Register Rb        = R10;
  VectorRegister vRb = VR8;
  li      (Rb, 8);
  load_perm(vRb, Rb);

  VectorRegister kplusw0 = VR18;
  VectorRegister kplusw1 = VR19;

  Register addr      = R7;

  for (int n = 0; n < total_ws; n++) {
    VectorRegister w = ws[n];

    if (n == 0) {
      lvx  (kplusw0, k);
    } else {
      addi (addr, k, n * 16);
      lvx  (kplusw0, addr);
    }
#if defined(VM_LITTLE_ENDIAN)
    vaddudm(kplusw0, kplusw0, w);
    vsldoi (kplusw1, kplusw0, kplusw0, 8);
#else
    vaddudm(kplusw1, kplusw0, w);
    vsldoi (kplusw0, kplusw1, kplusw1, 8);
#endif

    sha512_round(hs, total_hs, h_cnt, kplusw0);
    sha512_round(hs, total_hs, h_cnt, kplusw1);
  }

  Register tmp       = R8;
  li    (tmp, (w_size-16)/total_hs);
  mtctr (tmp);
  // j will be aligned to 4 for loading words.
  // Whenever read, advance the pointer (e.g: when j is used in a function)
  Register j = tmp;
  li     (j, 8*16);

  align(OptoLoopAlignment);
  bind(core_loop);

  // due to VectorRegister rotate, always iterate in multiples of total_hs
  for (int n = 0; n < total_hs/2; n++) {
    sha512_calc_2w(w0, w1, w2, w3, w4, w5, w6, w7, kplusw0, kplusw1, j, vRb, k);
    sha512_round(hs, total_hs, h_cnt, kplusw0);
    sha512_round(hs, total_hs, h_cnt, kplusw1);
  }

  bdnz   (core_loop);

  sha512_update_sha_state(state, hs, total_hs);

  if (multi_block) {
    addi(buf_in, buf_in, buf_size);
    addi(ofs, ofs, buf_size);
    cmplw(CCR0, ofs, limit);
    ble(CCR0, sha_loop);

    // return ofs
    mr(R3_RET, ofs);
  }

  // Restore non-volatile registers
  for (int c = 0; c < nv_size; c++) {
    Register idx = R7;
    li  (idx, (c - (nv_size)) * 16);
    lvx(nv[c], idx, R1);
  }
}
