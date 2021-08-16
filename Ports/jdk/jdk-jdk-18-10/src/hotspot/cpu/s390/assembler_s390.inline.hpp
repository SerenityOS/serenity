/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2016, 2021 SAP SE. All rights reserved.
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

#ifndef CPU_S390_ASSEMBLER_S390_INLINE_HPP
#define CPU_S390_ASSEMBLER_S390_INLINE_HPP

#include "asm/assembler.inline.hpp"
#include "asm/codeBuffer.hpp"
#include "code/codeCache.hpp"

// Convention: Use Z_R0 and Z_R1 instead of Z_scratch_* in all
// assembler_s390.* files.

// Local implementation of byte emitters to help inlining.
inline void Assembler::emit_16(int x) {
  CodeSection*       cs = code_section();
  address      code_pos = pc();
  *(unsigned short*)code_pos = (unsigned short)x;
  cs->set_end( code_pos + sizeof(unsigned short));
}

inline void Assembler::emit_32(int x) {
  CodeSection*       cs = code_section();
  address      code_pos = pc();
  *(jint*)code_pos = (jint)x;
  cs->set_end( code_pos + sizeof( jint));
}

inline void Assembler::emit_48(long x) {
  CodeSection*       cs = code_section();
  address      code_pos = pc();
  *(unsigned short*)code_pos = (unsigned short)(x>>32);
  *(jint*)(code_pos+sizeof(unsigned short)) = (jint)x;
  cs->set_end( code_pos + sizeof( jint) + sizeof( unsigned short));
}

// Support lightweight sync (from z196). Experimental as of now. For explanation see *.hpp file.
inline void Assembler::z_sync() {
  if (VM_Version::has_FastSync()) {
    z_bcr(bcondLightSync, Z_R0);
  } else {
    z_bcr(bcondFullSync, Z_R0);
  }
}
inline void Assembler::z_release() { }
inline void Assembler::z_acquire() { }
inline void Assembler::z_fence()   { z_sync(); }

inline void Assembler::z_illtrap() {
  emit_16(0);
}
inline void Assembler::z_illtrap(int id) {
  emit_16(id & 0x00ff);
}
inline void Assembler::z_illtrap_eyecatcher(unsigned short xpattern, unsigned short pattern) {
  z_llill(Z_R0, xpattern);
  z_iilh(Z_R0, pattern);
  z_illtrap((unsigned int)xpattern);
}

inline void Assembler::z_lhrl(Register r1, int64_t i2)  { emit_48( LHRL_ZOPC   | regt(r1, 8, 48) | simm32(i2, 16, 48)); }
inline void Assembler::z_lrl(Register r1, int64_t i2)   { emit_48( LRL_ZOPC    | regt(r1, 8, 48) | simm32(i2, 16, 48)); }
inline void Assembler::z_lghrl(Register r1, int64_t i2) { emit_48( LGHRL_ZOPC  | regt(r1, 8, 48) | simm32(i2, 16, 48)); }
inline void Assembler::z_lgfrl(Register r1, int64_t i2) { emit_48( LGFRL_ZOPC  | regt(r1, 8, 48) | simm32(i2, 16, 48)); }
inline void Assembler::z_lgrl(Register r1, int64_t i2)  { emit_48( LGRL_ZOPC   | regt(r1, 8, 48) | simm32(i2, 16, 48)); }
inline void Assembler::z_llhrl(Register r1, int64_t i2) { emit_48( LLHRL_ZOPC  | regt(r1, 8, 48) | simm32(i2, 16, 48)); }
inline void Assembler::z_llghrl(Register r1, int64_t i2){ emit_48( LLGHRL_ZOPC | regt(r1, 8, 48) | simm32(i2, 16, 48)); }
inline void Assembler::z_llgfrl(Register r1, int64_t i2){ emit_48( LLGFRL_ZOPC | regt(r1, 8, 48) | simm32(i2, 16, 48)); }

inline void Assembler::z_sthrl(Register r1, int64_t i2) { emit_48( STHRL_ZOPC  | regt(r1, 8, 48) | simm32(i2, 16, 48)); }
inline void Assembler::z_strl( Register r1, int64_t i2) { emit_48( STRL_ZOPC   | regt(r1, 8, 48) | simm32(i2, 16, 48)); }
inline void Assembler::z_stgrl(Register r1, int64_t i2) { emit_48( STGRL_ZOPC  | regt(r1, 8, 48) | simm32(i2, 16, 48)); }

inline void Assembler::z_cksm( Register r1, Register r2) { emit_32( CKSM_ZOPC   | regt(r1, 24, 32) | regt(r2, 28, 32)); }
inline void Assembler::z_km(   Register r1, Register r2) { emit_32( KM_ZOPC     | regt(r1, 24, 32) | regt(r2, 28, 32)); }
inline void Assembler::z_kmc(  Register r1, Register r2) { emit_32( KMC_ZOPC    | regt(r1, 24, 32) | regt(r2, 28, 32)); }
inline void Assembler::z_kma(  Register r1, Register r3, Register r2) { emit_32( KMA_ZOPC    | regt(r3, 16, 32) | regt(r1, 24, 32) | regt(r2, 28, 32)); }
inline void Assembler::z_kmf(  Register r1, Register r2) { emit_32( KMF_ZOPC    | regt(r1, 24, 32) | regt(r2, 28, 32)); }
inline void Assembler::z_kmctr(Register r1, Register r3, Register r2) { emit_32( KMCTR_ZOPC  | regt(r3, 16, 32) | regt(r1, 24, 32) | regt(r2, 28, 32)); }
inline void Assembler::z_kmo(  Register r1, Register r2) { emit_32( KMO_ZOPC    | regt(r1, 24, 32) | regt(r2, 28, 32)); }
inline void Assembler::z_kimd( Register r1, Register r2) { emit_32( KIMD_ZOPC   | regt(r1, 24, 32) | regt(r2, 28, 32)); }
inline void Assembler::z_klmd( Register r1, Register r2) { emit_32( KLMD_ZOPC   | regt(r1, 24, 32) | regt(r2, 28, 32)); }
inline void Assembler::z_kmac( Register r1, Register r2) { emit_32( KMAC_ZOPC   | regt(r1, 24, 32) | regt(r2, 28, 32)); }

inline void Assembler::z_exrl(Register r1, int64_t i2)  { emit_48( EXRL_ZOPC   | regt(r1, 8, 48) | simm32(i2, 16, 48)); }                             // z10
inline void Assembler::z_exrl(Register r1, address a2)  { emit_48( EXRL_ZOPC   | regt(r1, 8, 48) | simm32(RelAddr::pcrel_off32(a2, pc()), 16, 48)); } // z10

inline void Assembler::z_ectg(int64_t d1, Register b1, int64_t d2, Register b2, Register r3) { emit_48( ECTG_ZOPC | reg(r3, 8, 48) | uimm12(d1, 20, 48) | reg(b1, 16, 48) | uimm12(d2, 36, 48) | reg(b2, 32, 48)); }
inline void Assembler::z_ecag(Register r1, Register r3, int64_t d2, Register b2)             { emit_48( ECAG_ZOPC | reg(r1, 8, 48) | reg(r3, 12, 48) | simm20(d2) | reg(b2, 16, 48)); }


//------------------------------
// Interlocked-Update
//------------------------------
inline void Assembler::z_laa(  Register r1, Register r3, int64_t d2, Register b2) { emit_48( LAA_ZOPC   | regt(r1, 8, 48) | reg(r3, 12, 48) | simm20(d2) | regz(b2, 16, 48)); }
inline void Assembler::z_laag( Register r1, Register r3, int64_t d2, Register b2) { emit_48( LAAG_ZOPC  | regt(r1, 8, 48) | reg(r3, 12, 48) | simm20(d2) | regz(b2, 16, 48)); }
inline void Assembler::z_laal( Register r1, Register r3, int64_t d2, Register b2) { emit_48( LAAL_ZOPC  | regt(r1, 8, 48) | reg(r3, 12, 48) | simm20(d2) | regz(b2, 16, 48)); }
inline void Assembler::z_laalg(Register r1, Register r3, int64_t d2, Register b2) { emit_48( LAALG_ZOPC | regt(r1, 8, 48) | reg(r3, 12, 48) | simm20(d2) | regz(b2, 16, 48)); }
inline void Assembler::z_lan(  Register r1, Register r3, int64_t d2, Register b2) { emit_48( LAN_ZOPC   | regt(r1, 8, 48) | reg(r3, 12, 48) | simm20(d2) | regz(b2, 16, 48)); }
inline void Assembler::z_lang( Register r1, Register r3, int64_t d2, Register b2) { emit_48( LANG_ZOPC  | regt(r1, 8, 48) | reg(r3, 12, 48) | simm20(d2) | regz(b2, 16, 48)); }
inline void Assembler::z_lax(  Register r1, Register r3, int64_t d2, Register b2) { emit_48( LAX_ZOPC   | regt(r1, 8, 48) | reg(r3, 12, 48) | simm20(d2) | regz(b2, 16, 48)); }
inline void Assembler::z_laxg( Register r1, Register r3, int64_t d2, Register b2) { emit_48( LAXG_ZOPC  | regt(r1, 8, 48) | reg(r3, 12, 48) | simm20(d2) | regz(b2, 16, 48)); }
inline void Assembler::z_lao(  Register r1, Register r3, int64_t d2, Register b2) { emit_48( LAO_ZOPC   | regt(r1, 8, 48) | reg(r3, 12, 48) | simm20(d2) | regz(b2, 16, 48)); }
inline void Assembler::z_laog( Register r1, Register r3, int64_t d2, Register b2) { emit_48( LAOG_ZOPC  | regt(r1, 8, 48) | reg(r3, 12, 48) | simm20(d2) | regz(b2, 16, 48)); }

inline void Assembler::z_laa(  Register r1, Register r3, const Address& a) { assert(!a.has_index(), " no index reg allowed"); z_laa(  r1, r3, a.disp12(), a.base()); }
inline void Assembler::z_laag( Register r1, Register r3, const Address& a) { assert(!a.has_index(), " no index reg allowed"); z_laag( r1, r3, a.disp12(), a.base()); }
inline void Assembler::z_laal( Register r1, Register r3, const Address& a) { assert(!a.has_index(), " no index reg allowed"); z_laal( r1, r3, a.disp12(), a.base()); }
inline void Assembler::z_laalg(Register r1, Register r3, const Address& a) { assert(!a.has_index(), " no index reg allowed"); z_laalg(r1, r3, a.disp12(), a.base()); }
inline void Assembler::z_lan(  Register r1, Register r3, const Address& a) { assert(!a.has_index(), " no index reg allowed"); z_lan(  r1, r3, a.disp12(), a.base()); }
inline void Assembler::z_lang( Register r1, Register r3, const Address& a) { assert(!a.has_index(), " no index reg allowed"); z_lang( r1, r3, a.disp12(), a.base()); }
inline void Assembler::z_lax(  Register r1, Register r3, const Address& a) { assert(!a.has_index(), " no index reg allowed"); z_lax(  r1, r3, a.disp12(), a.base()); }
inline void Assembler::z_laxg( Register r1, Register r3, const Address& a) { assert(!a.has_index(), " no index reg allowed"); z_laxg( r1, r3, a.disp12(), a.base()); }
inline void Assembler::z_lao(  Register r1, Register r3, const Address& a) { assert(!a.has_index(), " no index reg allowed"); z_lao(  r1, r3, a.disp12(), a.base()); }
inline void Assembler::z_laog( Register r1, Register r3, const Address& a) { assert(!a.has_index(), " no index reg allowed"); z_laog( r1, r3, a.disp12(), a.base()); }

//--------------------------------
// Execution Prediction
//--------------------------------
inline void Assembler::z_pfd(  int64_t m1, int64_t d2, Register x2, Register b2) { emit_48( PFD_ZOPC   | uimm4(m1, 8, 48) | simm20(d2) | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_pfd(  int64_t m1, Address a)                            { z_pfd(m1, a.disp(), a.indexOrR0(), a.base()); }
inline void Assembler::z_pfdrl(int64_t m1, int64_t i2)                           { emit_48( PFDRL_ZOPC | uimm4(m1, 8, 48) | simm32(i2, 16, 48)); }
inline void Assembler::z_bpp(  int64_t m1, int64_t i2, int64_t d3, Register b3)  { emit_48( BPP_ZOPC   | uimm4(m1, 8, 48) | uimm12(d3, 20, 48) | reg(b3, 16, 48) | simm16(i2, 32, 48)); }
inline void Assembler::z_bprp( int64_t m1, int64_t i2, int64_t i3)               { emit_48( BPRP_ZOPC  | uimm4(m1, 8, 48) | simm12(i2, 12, 48) | simm24(i3, 24, 48)); }

//-------------------------------
// Transaction Control
//-------------------------------
inline void Assembler::z_tbegin( int64_t d1, Register b1, int64_t i2) { emit_48( TBEGIN_ZOPC  | uimm12(d1, 20, 48) | reg(b1, 16, 48) | uimm16(i2, 32, 48)); }
inline void Assembler::z_tbeginc(int64_t d1, Register b1, int64_t i2) { emit_48( TBEGINC_ZOPC | uimm12(d1, 20, 48) | reg(b1, 16, 48) | uimm16(i2, 32, 48)); }
inline void Assembler::z_tend()                                       { emit_32( TEND_ZOPC); }
inline void Assembler::z_tabort( int64_t d2, Register b2)             { emit_32( TABORT_ZOPC  | uimm12(d2, 20, 32) | reg(b2, 16, 32)); }
inline void Assembler::z_etnd(Register r1)                            { emit_32( ETND_ZOPC    | regt(r1, 24, 32)); }
inline void Assembler::z_ppa(Register r1, Register r2, int64_t m3)    { emit_32( PPA_ZOPC     | reg(r1, 24, 32) | reg(r2, 28, 32) | uimm4(m3, 16, 32)); }

//---------------------------------
// Conditional Execution
//---------------------------------
inline void Assembler::z_locr(  Register r1, Register r2, branch_condition cc)             { emit_32( LOCR_ZOPC  | regt(r1, 24, 32) | reg(r2, 28, 32) | uimm4(cc, 16, 32)); }               // z196
inline void Assembler::z_locgr( Register r1, Register r2, branch_condition cc)             { emit_32( LOCGR_ZOPC | regt(r1, 24, 32) | reg(r2, 28, 32) | uimm4(cc, 16, 32)); }               // z196
inline void Assembler::z_loc(   Register r1, int64_t d2, Register b2, branch_condition cc) { emit_48( LOC_ZOPC   | regt(r1,  8, 48) | simm20(d2) | regz(b2, 16, 48) | uimm4(cc, 12, 48)); } // z196
inline void Assembler::z_locg(  Register r1, int64_t d2, Register b2, branch_condition cc) { emit_48( LOCG_ZOPC  | regt(r1,  8, 48) | simm20(d2) | regz(b2, 16, 48) | uimm4(cc, 12, 48)); } // z196
inline void Assembler::z_loc(   Register r1, const Address &a, branch_condition cc)        { z_loc(r1, a.disp(), a.base(), cc); }
inline void Assembler::z_locg(  Register r1, const Address &a, branch_condition cc)        { z_locg(r1, a.disp(), a.base(), cc); }
inline void Assembler::z_stoc(  Register r1, int64_t d2, Register b2, branch_condition cc) { emit_48( STOC_ZOPC  | regt(r1,  8, 48) | simm20(d2) | regz(b2, 16, 48) | uimm4(cc, 12, 48)); } // z196
inline void Assembler::z_stocg( Register r1, int64_t d2, Register b2, branch_condition cc) { emit_48( STOCG_ZOPC | regt(r1,  8, 48) | simm20(d2) | regz(b2, 16, 48) | uimm4(cc, 12, 48)); } // z196

inline void Assembler::z_srst( Register r1, Register r2) { emit_32( SRST_ZOPC  | regt(r1, 24, 32) | reg(r2, 28, 32)); }
inline void Assembler::z_srstu(Register r1, Register r2) { emit_32( SRSTU_ZOPC | regt(r1, 24, 32) | reg(r2, 28, 32)); }

//---------------------------------
// Address calculation
//---------------------------------
inline void Assembler::z_layz(Register r1, int64_t d2, Register x2, Register b2) { emit_48( LAY_ZOPC | regt(r1, 8, 48) | simm20(d2) | reg(x2, 12, 48) | reg(b2, 16, 48)); }
inline void Assembler::z_lay( Register r1, const Address &a)                     { z_layz(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_lay( Register r1, int64_t d2, Register x2, Register b2) { emit_48( LAY_ZOPC | regt(r1, 8, 48) | simm20(d2) | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_laz( Register r1, int64_t d2, Register x2, Register b2) { emit_32( LA_ZOPC  | regt(r1, 8, 32) | uimm12(d2, 20, 32) | reg(x2, 12, 32) | reg(b2, 16, 32)); }
inline void Assembler::z_la(  Register r1, const Address &a)                     { z_laz(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_la(  Register r1, int64_t d2, Register x2, Register b2) { emit_32( LA_ZOPC  | regt(r1, 8, 32) | uimm12(d2, 20, 32) | reg(x2, 12, 32) | regz(b2, 16, 32));}
inline void Assembler::z_larl(Register r1, int64_t i2)    { emit_48( LARL_ZOPC | regt(r1, 8, 48) | simm32(i2, 16, 48)); }
inline void Assembler::z_larl(Register r1, address a)     { emit_48( LARL_ZOPC | regt(r1, 8, 48) | simm32(RelAddr::pcrel_off32(a, pc()), 16, 48)); }

inline void Assembler::z_lr(Register r1, Register r2)                          { emit_16( LR_ZOPC | regt(r1,8,16) | reg(r2,12,16)); }
inline void Assembler::z_lgr(Register r1, Register r2)                         { emit_32( LGR_ZOPC | regt(r1, 24, 32) | reg(r2, 28, 32)); }
inline void Assembler::z_lh(Register r1, int64_t d2, Register x2, Register b2) { emit_32( LH_ZOPC | 0 << 16 | regt(r1, 8, 32) | uimm12(d2, 20, 32) | reg(x2, 12, 32) | regz(b2, 16, 32)); }
inline void Assembler::z_lh(Register r1, const Address &a)                     { z_lh(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_l(Register r1, int64_t d2, Register x2, Register b2)  { emit_32( L_ZOPC | 0 << 16 | regt(r1, 8, 32) | uimm12(d2, 20, 32) | reg(x2, 12, 32) | regz(b2, 16, 32)); }
inline void Assembler::z_l(Register r1, const Address &a)                      { z_l(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_lg(Register r1, int64_t d2, Register x2, Register b2) { emit_48( LG_ZOPC | regt(r1, 8, 48) | simm20(d2) | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_lg(Register r1, const Address &a)                     { z_lg(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }

inline void Assembler::z_lbr(  Register r1, Register r2) { emit_32( LBR_ZOPC   | regt(r1, 24, 32) | reg( r2, 28, 32)); }
inline void Assembler::z_lhr(  Register r1, Register r2) { emit_32( LHR_ZOPC   | regt(r1, 24, 32) | reg( r2, 28, 32)); }
inline void Assembler::z_lgbr( Register r1, Register r2) { emit_32( LGBR_ZOPC  | regt(r1, 24, 32) | reg(r2, 28, 32)); }
inline void Assembler::z_lghr( Register r1, Register r2) { emit_32( LGHR_ZOPC  | regt(r1, 24, 32) | reg(r2, 28, 32)); }
inline void Assembler::z_lgfr( Register r1, Register r2) { emit_32( LGFR_ZOPC  | regt(r1, 24, 32) | reg(r2, 28, 32)); }
inline void Assembler::z_llhr( Register r1, Register r2) { emit_32( LLHR_ZOPC  | regt(r1, 24, 32) | reg(r2, 28, 32)); }
inline void Assembler::z_llgcr(Register r1, Register r2) { emit_32( LLGCR_ZOPC | regt(r1, 24, 32) | reg(r2, 28, 32)); }
inline void Assembler::z_llghr(Register r1, Register r2) { emit_32( LLGHR_ZOPC | regt(r1, 24, 32) | reg(r2, 28, 32)); }
inline void Assembler::z_llgfr(Register r1, Register r2) { emit_32( LLGFR_ZOPC | regt(r1, 24, 32) | reg(r2, 28, 32)); }

inline void Assembler::z_sth(Register r1, const Address &a)                     { z_sth(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_sth(Register r1, int64_t d2, Register x2, Register b2) { emit_32( STH_ZOPC | reg(r1, 8, 32) | uimm12(d2, 20, 32) | reg(x2, 12, 32) | regz(b2, 16, 32)); }
inline void Assembler::z_st( Register r1, const Address& d)                     { z_st(r1, d.disp(), d.indexOrR0(), d.base()); }
inline void Assembler::z_st( Register r1, int64_t d2, Register x2, Register b2) { emit_32( ST_ZOPC  | reg(r1, 8, 32) | uimm12(d2, 20, 32) | reg(x2, 12, 32) | regz(b2, 16, 32)); }
inline void Assembler::z_stg(Register r1, const Address& d)                     { z_stg(r1, d.disp(), d.indexOrR0(), d.base()); }
inline void Assembler::z_stg(Register r1, int64_t d2, Register x2, Register b2) { emit_48( STG_ZOPC | reg(r1, 8, 48) | simm20(d2)         | reg(x2, 12, 48) | regz(b2, 16, 48)); }

inline void Assembler::z_stcm (Register r1, int64_t m3, int64_t d2, Register b2) { emit_32( STCM_ZOPC  | regt(r1, 8, 32) | uimm4(m3, 12, 32) | uimm12(d2, 20, 32) | regz(b2, 16, 32)); }
inline void Assembler::z_stcmy(Register r1, int64_t m3, int64_t d2, Register b2) { emit_48( STCMY_ZOPC | regt(r1, 8, 48) | uimm4(m3, 12, 48) | simm20(d2)         | regz(b2, 16, 48)); }
inline void Assembler::z_stcmh(Register r1, int64_t m3, int64_t d2, Register b2) { emit_48( STCMH_ZOPC | regt(r1, 8, 48) | uimm4(m3, 12, 48) | simm20(d2)         | regz(b2, 16, 48)); }

// memory-immediate instructions (8-bit immediate)
inline void Assembler::z_cli( int64_t d1, Register b1, int64_t i2) { emit_32( CLI_ZOPC  | uimm12(d1, 20, 32) | regz(b1, 16, 32) | uimm8(i2, 8, 32)); }
inline void Assembler::z_mvi( int64_t d1, Register b1, int64_t i2) { emit_32( MVI_ZOPC  | uimm12(d1, 20, 32) | regz(b1, 16, 32) | imm8(i2, 8, 32)); }
inline void Assembler::z_tm(  int64_t d1, Register b1, int64_t i2) { emit_32( TM_ZOPC   | uimm12(d1, 20, 32) | regz(b1, 16, 32) | imm8(i2, 8, 32)); }
inline void Assembler::z_ni(  int64_t d1, Register b1, int64_t i2) { emit_32( NI_ZOPC   | uimm12(d1, 20, 32) | regz(b1, 16, 32) | imm8(i2, 8, 32)); }
inline void Assembler::z_oi(  int64_t d1, Register b1, int64_t i2) { emit_32( OI_ZOPC   | uimm12(d1, 20, 32) | regz(b1, 16, 32) | imm8(i2, 8, 32)); }
inline void Assembler::z_xi(  int64_t d1, Register b1, int64_t i2) { emit_32( XI_ZOPC   | uimm12(d1, 20, 32) | regz(b1, 16, 32) | imm8(i2, 8, 32)); }
inline void Assembler::z_cliy(int64_t d1, Register b1, int64_t i2) { emit_48( CLIY_ZOPC | simm20(d1)         | regz(b1, 16, 48) | uimm8(i2, 8, 48)); }
inline void Assembler::z_mviy(int64_t d1, Register b1, int64_t i2) { emit_48( MVIY_ZOPC | simm20(d1)         | regz(b1, 16, 48) | imm8(i2, 8, 48)); }
inline void Assembler::z_tmy( int64_t d1, Register b1, int64_t i2) { emit_48( TMY_ZOPC  | simm20(d1)         | regz(b1, 16, 48) | imm8(i2, 8, 48)); }
inline void Assembler::z_niy( int64_t d1, Register b1, int64_t i2) { emit_48( NIY_ZOPC  | simm20(d1)         | regz(b1, 16, 48) | imm8(i2, 8, 48)); }
inline void Assembler::z_oiy( int64_t d1, Register b1, int64_t i2) { emit_48( OIY_ZOPC  | simm20(d1)         | regz(b1, 16, 48) | imm8(i2, 8, 48)); }
inline void Assembler::z_xiy( int64_t d1, Register b1, int64_t i2) { emit_48( XIY_ZOPC  | simm20(d1)         | regz(b1, 16, 48) | imm8(i2, 8, 48)); }

inline void Assembler::z_cli( const Address& a, int64_t imm) { assert(!a.has_index(), " no index reg allowed in CLI");  z_cli( a.disp12(), a.base(), imm); }
inline void Assembler::z_mvi( const Address& a, int64_t imm) { assert(!a.has_index(), " no index reg allowed in CLI");  z_mvi( a.disp12(), a.base(), imm); }
inline void Assembler::z_tm(  const Address& a, int64_t imm) { assert(!a.has_index(), " no index reg allowed in CLI");  z_tm(  a.disp12(), a.base(), imm); }
inline void Assembler::z_ni(  const Address& a, int64_t imm) { assert(!a.has_index(), " no index reg allowed in CLI");  z_ni(  a.disp12(), a.base(), imm); }
inline void Assembler::z_oi(  const Address& a, int64_t imm) { assert(!a.has_index(), " no index reg allowed in CLI");  z_oi(  a.disp12(), a.base(), imm); }
inline void Assembler::z_xi(  const Address& a, int64_t imm) { assert(!a.has_index(), " no index reg allowed in CLI");  z_xi(  a.disp12(), a.base(), imm); }
inline void Assembler::z_cliy(const Address& a, int64_t imm) { assert(!a.has_index(), " no index reg allowed in CLIY"); z_cliy(a.disp20(), a.base(), imm); }
inline void Assembler::z_mviy(const Address& a, int64_t imm) { assert(!a.has_index(), " no index reg allowed in MVIY"); z_mviy(a.disp20(), a.base(), imm); }
inline void Assembler::z_tmy( const Address& a, int64_t imm) { assert(!a.has_index(), " no index reg allowed in TMY");  z_tmy( a.disp20(), a.base(), imm); }
inline void Assembler::z_niy( const Address& a, int64_t imm) { assert(!a.has_index(), " no index reg allowed in NIY");  z_niy( a.disp20(), a.base(), imm); }
inline void Assembler::z_oiy( const Address& a, int64_t imm) { assert(!a.has_index(), " no index reg allowed in OIY");  z_oiy( a.disp20(), a.base(), imm); }
inline void Assembler::z_xiy( const Address& a, int64_t imm) { assert(!a.has_index(), " no index reg allowed in XIY");  z_xiy( a.disp20(), a.base(), imm); }


inline void Assembler::z_mvc(const Address& d, const Address& s, int64_t l) {
  assert(!d.has_index() && !s.has_index(), "Address operand can not be encoded.");
  z_mvc(d.disp(), l-1, d.base(), s.disp(), s.base());
}
inline void Assembler::z_mvc(int64_t d1, int64_t l, Register b1, int64_t d2, Register b2) { emit_48( MVC_ZOPC | uimm12(d1, 20, 48) | uimm8(l, 8, 48) | regz(b1, 16, 48) | uimm12(d2, 36, 48) | regz(b2, 32, 48)); }
inline void Assembler::z_mvcle(Register r1, Register r3, int64_t d2, Register b2) { emit_32( MVCLE_ZOPC | reg(r1, 8, 32) | reg(r3, 12, 32) | uimm12(d2, 20, 32) | reg(b2, 16, 32)); }

inline void Assembler::z_mvhhi( int64_t d1, Register b1, int64_t i2) { emit_48( MVHHI_ZOPC | uimm12( d1, 20, 48) | regz(b1, 16, 48) | simm16(i2, 32, 48)); }
inline void Assembler::z_mvhi ( int64_t d1, Register b1, int64_t i2) { emit_48( MVHI_ZOPC  | uimm12( d1, 20, 48) | regz(b1, 16, 48) | simm16(i2, 32, 48)); }
inline void Assembler::z_mvghi( int64_t d1, Register b1, int64_t i2) { emit_48( MVGHI_ZOPC | uimm12( d1, 20, 48) | regz(b1, 16, 48) | simm16(i2, 32, 48)); }
inline void Assembler::z_mvhhi( const Address &d, int64_t i2) { assert(!d.has_index(), " no index reg allowed in MVHHI"); z_mvhhi( d.disp(), d.baseOrR0(), i2); }
inline void Assembler::z_mvhi ( const Address &d, int64_t i2) { assert(!d.has_index(), " no index reg allowed in MVHI");  z_mvhi(  d.disp(), d.baseOrR0(), i2); }
inline void Assembler::z_mvghi( const Address &d, int64_t i2) { assert(!d.has_index(), " no index reg allowed in MVGHI"); z_mvghi( d.disp(), d.baseOrR0(), i2); }

inline void Assembler::z_ex(Register r1, int64_t d2, Register x2, Register b2) { emit_32( EX_ZOPC | regz(r1, 8, 32) | uimm12(d2, 20, 32) | reg(x2, 12, 32) | regz(b2, 16, 32)); }

inline void Assembler::z_ic  (Register r1, int64_t d2, Register x2, Register b2) { emit_32( IC_ZOPC  | regt(r1, 8, 32) | uimm12(d2, 20, 32) | reg(x2, 12, 32) | regz(b2, 16, 32)); }
inline void Assembler::z_icy (Register r1, int64_t d2, Register x2, Register b2) { emit_48( ICY_ZOPC | regt(r1, 8, 48) | simm20(d2)         | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_icm (Register r1, int64_t m3, int64_t d2, Register b2) { emit_32( ICM_ZOPC  | regt(r1, 8, 32) | uimm4(m3, 12, 32) | uimm12(d2, 20, 32) | regz(b2, 16, 32)); }
inline void Assembler::z_icmy(Register r1, int64_t m3, int64_t d2, Register b2) { emit_48( ICMY_ZOPC | regt(r1, 8, 48) | uimm4(m3, 12, 48) | simm20(d2)         | regz(b2, 16, 48)); }
inline void Assembler::z_icmh(Register r1, int64_t m3, int64_t d2, Register b2) { emit_48( ICMH_ZOPC | regt(r1, 8, 48) | uimm4(m3, 12, 48) | simm20(d2)         | regz(b2, 16, 48)); }
inline void Assembler::z_iihh(Register r1, int64_t i2) { emit_32( IIHH_ZOPC | regt(r1, 8, 32) | imm16(i2, 16, 32)); }
inline void Assembler::z_iihl(Register r1, int64_t i2) { emit_32( IIHL_ZOPC | regt(r1, 8, 32) | imm16(i2, 16, 32)); }
inline void Assembler::z_iilh(Register r1, int64_t i2) { emit_32( IILH_ZOPC | regt(r1, 8, 32) | imm16(i2, 16, 32)); }
inline void Assembler::z_iill(Register r1, int64_t i2) { emit_32( IILL_ZOPC | regt(r1, 8, 32) | imm16(i2, 16, 32)); }
inline void Assembler::z_iihf(Register r1, int64_t i2) { emit_48( IIHF_ZOPC | regt(r1, 8, 48) | imm32(i2, 16, 48)); }
inline void Assembler::z_iilf(Register r1, int64_t i2) { emit_48( IILF_ZOPC | regt(r1, 8, 48) | imm32(i2, 16, 48)); }
inline void Assembler::z_lgf(Register r1, const Address& a) { z_lgf(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_lgf(Register r1, int64_t d2, Register x2, Register b2)  { emit_48( LGF_ZOPC | regt(r1, 8, 48) | simm20(d2) | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_lhy(Register r1, const Address &a) { z_lhy(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_lhy(Register r1, int64_t d2, Register x2, Register b2)  { emit_48( LHY_ZOPC | regt(r1, 8, 48) | simm20(d2) | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_lgh(Register r1, const Address &a) { z_lgh(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_lgh(Register r1, int64_t d2, Register x2, Register b2)  { emit_48( LGH_ZOPC | regt(r1, 8, 48) | simm20(d2) | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_lt(Register r1, const Address &a) { z_lt(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_lt (Register r1, int64_t d2, Register x2, Register b2)  { emit_48( LT_ZOPC  | regt(r1, 8, 48) | simm20(d2) | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_ltg(Register r1, const Address &a) { z_ltg(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_ltg(Register r1, int64_t d2, Register x2, Register b2)  { emit_48( LTG_ZOPC | regt(r1, 8, 48) | simm20(d2) | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_ltgf(Register r1, const Address &a) { z_ltgf(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_ltgf(Register r1, int64_t d2, Register x2, Register b2) { emit_48( LTGF_ZOPC| regt(r1, 8, 48) | simm20(d2) | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_lb(Register r1, const Address &a) { z_lb(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_lb (Register r1, int64_t d2, Register x2, Register b2)  { emit_48( LB_ZOPC  | regt(r1, 8, 48) | simm20(d2) | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_lgb(Register r1, const Address &a) { z_lgb(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_lgb(Register r1, int64_t d2, Register x2, Register b2)  { emit_48( LGB_ZOPC | regt(r1, 8, 48) | simm20(d2) | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_ly(Register r1, const Address &a) { z_ly(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_ly(Register r1, int64_t d2, Register x2, Register b2)   { emit_48( LY_ZOPC | regt(r1, 8, 48) | simm20(d2) | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_llc(Register r1, const Address& a) { z_llc(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_llc(Register r1, int64_t d2, Register x2, Register b2)  { emit_48( LLC_ZOPC  | regt(r1, 8, 48) | simm20(d2) | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_llh(Register r1, const Address &a) { z_llh(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_llh(Register r1, int64_t d2, Register x2, Register b2)  { emit_48( LLH_ZOPC  | regt(r1, 8, 48) | simm20(d2) | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_llgf(Register r1, const Address &a) { z_llgf(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_llgf(Register r1, int64_t d2, Register x2, Register b2) { emit_48( LLGF_ZOPC | regt(r1, 8, 48) | simm20(d2) | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_llgh(Register r1, const Address &a) { z_llgh(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_llgh(Register r1, int64_t d2, Register x2, Register b2) { emit_48( LLGH_ZOPC | regt(r1, 8, 48) | simm20(d2) | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_llgc(Register r1, const Address &a) { z_llgc(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_llgc(Register r1, int64_t d2, Register x2, Register b2) { emit_48( LLGC_ZOPC | regt(r1, 8, 48) | simm20(d2) | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_llgc(Register r1, int64_t d2, Register b2)              { z_llgc( r1, d2, Z_R0, b2); }
inline void Assembler::z_lhi(Register r1, int64_t i2) { emit_32( LHI_ZOPC | regt(r1, 8, 32) | simm16(i2, 16, 32)); }
inline void Assembler::z_lghi(Register r1, int64_t i2) { emit_32( LGHI_ZOPC | regt(r1, 8, 32) | simm16(i2, 16, 32)); }
inline void Assembler::z_lgfi(Register r1, int64_t i2) { emit_48( LGFI_ZOPC | regt(r1, 8, 48) | simm32(i2, 16, 48)); }
inline void Assembler::z_llihf(Register r1, int64_t i2) { emit_48( LLIHF_ZOPC | regt(r1, 8, 48) | imm32(i2, 16, 48)); }
inline void Assembler::z_llilf(Register r1, int64_t i2) { emit_48( LLILF_ZOPC | regt(r1, 8, 48) | imm32(i2, 16, 48)); }
inline void Assembler::z_llihh(Register r1, int64_t i2) { emit_32( LLIHH_ZOPC | regt(r1, 8, 32) | imm16(i2, 16, 32)); }
inline void Assembler::z_llihl(Register r1, int64_t i2) { emit_32( LLIHL_ZOPC | regt(r1, 8, 32) | imm16(i2, 16, 32)); }
inline void Assembler::z_llilh(Register r1, int64_t i2) { emit_32( LLILH_ZOPC | regt(r1, 8, 32) | imm16(i2, 16, 32)); }
inline void Assembler::z_llill(Register r1, int64_t i2) { emit_32( LLILL_ZOPC | regt(r1, 8, 32) | imm16(i2, 16, 32)); }

// allow "monadic" use
inline void Assembler::z_lcr(  Register r1, Register r2) { emit_16( LCR_ZOPC   | regt( r1,  8, 16) | reg((r2 == noreg) ? r1:r2, 12, 16)); }
inline void Assembler::z_lcgr( Register r1, Register r2) { emit_32( LCGR_ZOPC  | regt( r1, 24, 32) | reg((r2 == noreg) ? r1:r2, 28, 32)); }
inline void Assembler::z_lcgfr(Register r1, Register r2) { emit_32( LCGFR_ZOPC | regt( r1, 24, 32) | reg((r2 == noreg) ? r1:r2, 28, 32)); }
inline void Assembler::z_lnr(  Register r1, Register r2) { emit_16( LNR_ZOPC   | regt( r1,  8, 16) | reg((r2 == noreg) ? r1:r2, 12, 16)); }
inline void Assembler::z_lngr( Register r1, Register r2) { emit_32( LNGR_ZOPC  | regt( r1, 24, 32) | reg((r2 == noreg) ? r1:r2, 28, 32)); }
inline void Assembler::z_lngfr(Register r1, Register r2) { emit_32( LNGFR_ZOPC | regt( r1, 24, 32) | reg((r2 == noreg) ? r1:r2, 28, 32)); }
inline void Assembler::z_lpr(  Register r1, Register r2) { emit_16( LPR_ZOPC   | regt( r1,  8, 16) | reg((r2 == noreg) ? r1:r2, 12, 16)); }
inline void Assembler::z_lpgr( Register r1, Register r2) { emit_32( LPGR_ZOPC  | regt( r1, 24, 32) | reg((r2 == noreg) ? r1:r2, 28, 32)); }
inline void Assembler::z_lpgfr(Register r1, Register r2) { emit_32( LPGFR_ZOPC | regt( r1, 24, 32) | reg((r2 == noreg) ? r1:r2, 28, 32)); }

inline void Assembler::z_lrvr( Register r1, Register r2) { emit_32( LRVR_ZOPC  | regt(r1, 24, 32) | reg(r2, 28, 32)); }
inline void Assembler::z_lrvgr(Register r1, Register r2) { emit_32( LRVGR_ZOPC | regt(r1, 24, 32) | reg(r2, 28, 32)); }

inline void Assembler::z_ltr(  Register r1, Register r2) { emit_16( LTR_ZOPC   | regt(r1,  8, 16) | reg(r2, 12, 16)); }
inline void Assembler::z_ltgr( Register r1, Register r2) { emit_32( LTGR_ZOPC  | regt(r1, 24, 32) | reg(r2, 28, 32)); }
inline void Assembler::z_ltgfr(Register r1, Register r2) { emit_32( LTGFR_ZOPC | regt(r1, 24, 32) | reg(r2, 28, 32)); }
inline void Assembler::z_stc(  Register r1, const Address &a) { z_stc(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_stc(  Register r1, int64_t d2, Register x2, Register b2) { emit_32( STC_ZOPC | regt(r1, 8, 32) | uimm12(d2, 20, 32) | reg(x2, 12, 32) | regz(b2, 16, 32)); }
inline void Assembler::z_stcy( Register r1, const Address &a) { z_stcy(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_stcy( Register r1, int64_t d2, Register x2, Register b2) { emit_48( STCY_ZOPC | regt(r1, 8, 48) | simm20(d2) | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_sthy( Register r1, const Address &a) { z_sthy(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_sthy( Register r1, int64_t d2, Register x2, Register b2) { emit_48( STHY_ZOPC | regt(r1, 8, 48) | simm20(d2) | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_sty(  Register r1, const Address &a) { z_sty(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_sty(  Register r1, int64_t d2, Register x2, Register b2) { emit_48( STY_ZOPC | regt(r1, 8, 48) | simm20(d2) | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_stfle(int64_t d2, Register b2) { emit_32(STFLE_ZOPC | uimm12(d2,20,32) | regz(b2,16,32)); }


//-----------------------------------
// SHIFT/RORATE OPERATIONS
//-----------------------------------
inline void Assembler::z_sla( Register r1,              int64_t d2, Register b2) { emit_32( SLA_ZOPC  | regt(r1, 8, 32) | uimm12(d2, 20, 32) | reg(b2, 16, 32)); }
inline void Assembler::z_slak(Register r1, Register r3, int64_t d2, Register b2) { emit_48( SLAK_ZOPC | regt(r1, 8, 48) | simm20(d2)         | reg(b2, 16, 48) | reg(r3, 12, 48)); }
inline void Assembler::z_slag(Register r1, Register r3, int64_t d2, Register b2) { emit_48( SLAG_ZOPC | regt(r1, 8, 48) | simm20(d2)         | reg(b2, 16, 48) | reg(r3, 12, 48)); }
inline void Assembler::z_sra( Register r1,              int64_t d2, Register b2) { emit_32( SRA_ZOPC  | regt(r1, 8, 32) | uimm12(d2, 20, 32) | reg(b2, 16, 32)); }
inline void Assembler::z_srak(Register r1, Register r3, int64_t d2, Register b2) { emit_48( SRAK_ZOPC | regt(r1, 8, 48) | simm20(d2)         | reg(b2, 16, 48) | reg(r3, 12, 48)); }
inline void Assembler::z_srag(Register r1, Register r3, int64_t d2, Register b2) { emit_48( SRAG_ZOPC | regt(r1, 8, 48) | simm20(d2)         | reg(b2, 16, 48) | reg(r3, 12, 48)); }
inline void Assembler::z_sll( Register r1,              int64_t d2, Register b2) { emit_32( SLL_ZOPC  | regt(r1, 8, 32) | uimm12(d2, 20, 32) | reg(b2, 16, 32)); }
inline void Assembler::z_sllk(Register r1, Register r3, int64_t d2, Register b2) { emit_48( SLLK_ZOPC | regt(r1, 8, 48) | simm20(d2)         | reg(b2, 16, 48) | reg(r3, 12, 48)); }
inline void Assembler::z_sllg(Register r1, Register r3, int64_t d2, Register b2) { emit_48( SLLG_ZOPC | regt(r1, 8, 48) | simm20(d2)         | reg(b2, 16, 48) | reg(r3, 12, 48)); }
inline void Assembler::z_srl( Register r1,              int64_t d2, Register b2) { emit_32( SRL_ZOPC  | regt(r1, 8, 32) | uimm12(d2, 20, 32) | reg(b2, 16, 32)); }
inline void Assembler::z_srlk(Register r1, Register r3, int64_t d2, Register b2) { emit_48( SRLK_ZOPC | regt(r1, 8, 48) | simm20(d2)         | reg(b2, 16, 48) | reg(r3, 12, 48)); }
inline void Assembler::z_srlg(Register r1, Register r3, int64_t d2, Register b2) { emit_48( SRLG_ZOPC | regt(r1, 8, 48) | simm20(d2)         | reg(b2, 16, 48) | reg(r3, 12, 48)); }

// rotate left
inline void Assembler::z_rll( Register r1, Register r3, int64_t d2, Register b2) { emit_48( RLL_ZOPC  | regt(r1, 8, 48) | reg(r3, 12, 48) | simm20(d2) | reg(b2, 16, 48)); }
inline void Assembler::z_rllg(Register r1, Register r3, int64_t d2, Register b2) { emit_48( RLLG_ZOPC | regt(r1, 8, 48) | reg(r3, 12, 48) | simm20(d2) | reg(b2, 16, 48)); }

// Rotate the AND/XOR/OR/insert
inline void Assembler::z_rnsbg( Register r1, Register r2, int64_t spos3, int64_t epos4, int64_t nrot5, bool test_only) { // Rotate then AND selected bits.  -- z196
  const int64_t len = 48;
  assert(Immediate::is_uimm(spos3, 6), "range start out of range");   // Could just trim to 6bits wide w/o assertion.
  assert(Immediate::is_uimm(epos4, 6), "range end   out of range");   // Could just trim to 6bits wide w/o assertion.
  assert(Immediate::is_uimm(nrot5, 6), "rotate amount out of range"); // Could just leave it as is. leftmost 2 bits are ignored by instruction.
  emit_48( RNSBG_ZOPC | regt(r1, 8, len) | regt(r2, 12, len) | uimm6(spos3, 16+2, len) | uimm6(epos4, 24+2, len) | uimm6(nrot5, 32+2, len) | u_field(test_only ? 1 : 0, len-16-1, len-16-1));
}
inline void Assembler::z_rxsbg( Register r1, Register r2, int64_t spos3, int64_t epos4, int64_t nrot5, bool test_only) { // Rotate then XOR selected bits.  -- z196
  const int64_t len = 48;
  assert(Immediate::is_uimm(spos3, 6), "range start out of range");   // Could just trim to 6bits wide w/o assertion.
  assert(Immediate::is_uimm(epos4, 6), "range end   out of range");   // Could just trim to 6bits wide w/o assertion.
  assert(Immediate::is_uimm(nrot5, 6), "rotate amount out of range"); // Could just leave it as is. leftmost 2 bits are ignored by instruction.
  emit_48( RXSBG_ZOPC | regt(r1, 8, len) | regt(r2, 12, len) | uimm6(spos3, 16+2, len) | uimm6(epos4, 24+2, len) | uimm6(nrot5, 32+2, len) | u_field(test_only ? 1 : 0, len-16-1, len-16-1));
}
inline void Assembler::z_rosbg( Register r1, Register r2, int64_t spos3, int64_t epos4, int64_t nrot5, bool test_only) { // Rotate then OR selected bits.  -- z196
  const int64_t len = 48;
  assert(Immediate::is_uimm(spos3, 6), "range start out of range");   // Could just trim to 6bits wide w/o assertion.
  assert(Immediate::is_uimm(epos4, 6), "range end   out of range");   // Could just trim to 6bits wide w/o assertion.
  assert(Immediate::is_uimm(nrot5, 6), "rotate amount out of range"); // Could just leave it as is. leftmost 2 bits are ignored by instruction.
  emit_48( ROSBG_ZOPC | regt(r1, 8, len) | regt(r2, 12, len) | uimm6(spos3, 16+2, len) | uimm6(epos4, 24+2, len) | uimm6(nrot5, 32+2, len) | u_field(test_only ? 1 : 0, len-16-1, len-16-1));
}
inline void Assembler::z_risbg( Register r1, Register r2, int64_t spos3, int64_t epos4, int64_t nrot5, bool zero_rest) { // Rotate then INS selected bits.  -- z196
  const int64_t len = 48;
  assert(Immediate::is_uimm(spos3, 6), "range start out of range");   // Could just trim to 6bits wide w/o assertion.
  assert(Immediate::is_uimm(epos4, 6), "range end   out of range");   // Could just trim to 6bits wide w/o assertion.
  assert(Immediate::is_uimm(nrot5, 6), "rotate amount out of range"); // Could just leave it as is. leftmost 2 bits are ignored by instruction.
  emit_48( RISBG_ZOPC | regt(r1, 8, len) | regt(r2, 12, len) | uimm6(spos3, 16+2, len) | uimm6(epos4, 24+2, len) | uimm6(nrot5, 32+2, len) | u_field(zero_rest ? 1 : 0, len-24-1, len-24-1));
}


//------------------------------
// LOGICAL OPERATIONS
//------------------------------
inline void Assembler::z_n(   Register r1, int64_t d2, Register x2, Register b2) { emit_32( N_ZOPC  | regt(r1, 8, 32) | uimm12(d2, 20, 32) | reg(x2, 12, 32) | regz(b2, 16, 32)); }
inline void Assembler::z_ny(  Register r1, int64_t d2, Register x2, Register b2) { emit_48( NY_ZOPC | regt(r1, 8, 48) | simm20(d2)         | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_ng(  Register r1, int64_t d2, Register x2, Register b2) { emit_48( NG_ZOPC | regt(r1, 8, 48) | simm20(d2)         | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_n(   Register r1, const Address& a) { z_n( r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_ny(  Register r1, const Address& a) { z_ny(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_ng(  Register r1, const Address& a) { z_ng(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }

inline void Assembler::z_nr(  Register r1, Register r2)              { emit_16( NR_ZOPC   | regt(r1,  8, 16) | reg(r2, 12, 16)); }
inline void Assembler::z_ngr( Register r1, Register r2)              { emit_32( NGR_ZOPC  | regt(r1, 24, 32) | reg(r2, 28, 32)); }
inline void Assembler::z_nrk( Register r1, Register r2, Register r3) { emit_32( NRK_ZOPC  | regt(r1, 24, 32) | reg(r2, 28, 32) | reg(r3, 16, 32)); }
inline void Assembler::z_ngrk(Register r1, Register r2, Register r3) { emit_32( NGRK_ZOPC | regt(r1, 24, 32) | reg(r2, 28, 32) | reg(r3, 16, 32)); }

inline void Assembler::z_nihh(Register r1, int64_t i2) { emit_32( NIHH_ZOPC | regt(r1, 8, 32) | imm16(i2, 16, 32)); }
inline void Assembler::z_nihl(Register r1, int64_t i2) { emit_32( NIHL_ZOPC | regt(r1, 8, 32) | imm16(i2, 16, 32)); }
inline void Assembler::z_nilh(Register r1, int64_t i2) { emit_32( NILH_ZOPC | regt(r1, 8, 32) | imm16(i2, 16, 32)); }
inline void Assembler::z_nill(Register r1, int64_t i2) { emit_32( NILL_ZOPC | regt(r1, 8, 32) | imm16(i2, 16, 32)); }
inline void Assembler::z_nihf(Register r1, int64_t i2) { emit_48( NIHF_ZOPC | regt(r1, 8, 48) | imm32(i2, 16, 48)); }
inline void Assembler::z_nilf(Register r1, int64_t i2) { emit_48( NILF_ZOPC | regt(r1, 8, 48) | imm32(i2, 16, 48)); }

inline void Assembler::z_o(   Register r1, int64_t d2, Register x2, Register b2) { emit_32( O_ZOPC  | regt(r1, 8, 32) | uimm12(d2, 20, 32) | reg(x2, 12, 32) | regz(b2, 16, 32)); }
inline void Assembler::z_oy(  Register r1, int64_t d2, Register x2, Register b2) { emit_48( OY_ZOPC | regt(r1, 8, 48) | simm20(d2)         | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_og(  Register r1, int64_t d2, Register x2, Register b2) { emit_48( OG_ZOPC | regt(r1, 8, 48) | simm20(d2)         | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_o(   Register r1, const Address& a) { z_o( r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_oy(  Register r1, const Address& a) { z_oy(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_og(  Register r1, const Address& a) { z_og(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }

inline void Assembler::z_or(  Register r1, Register r2)              { emit_16( OR_ZOPC   | regt(r1,  8, 16) | reg(r2, 12, 16)); }
inline void Assembler::z_ogr( Register r1, Register r2)              { emit_32( OGR_ZOPC  | regt(r1, 24, 32) | reg(r2, 28, 32)); }
inline void Assembler::z_ork( Register r1, Register r2, Register r3) { emit_32( ORK_ZOPC  | regt(r1, 24, 32) | reg(r2, 28, 32) | reg(r3, 16, 32)); }
inline void Assembler::z_ogrk(Register r1, Register r2, Register r3) { emit_32( OGRK_ZOPC | regt(r1, 24, 32) | reg(r2, 28, 32) | reg(r3, 16, 32)); }

inline void Assembler::z_oihh(Register r1, int64_t i2) { emit_32( OIHH_ZOPC | regt(r1, 8, 32) | imm16(i2, 16, 32)); }
inline void Assembler::z_oihl(Register r1, int64_t i2) { emit_32( OIHL_ZOPC | regt(r1, 8, 32) | imm16(i2, 16, 32)); }
inline void Assembler::z_oilh(Register r1, int64_t i2) { emit_32( OILH_ZOPC | regt(r1, 8, 32) | imm16(i2, 16, 32)); }
inline void Assembler::z_oill(Register r1, int64_t i2) { emit_32( OILL_ZOPC | regt(r1, 8, 32) | imm16(i2, 16, 32)); }
inline void Assembler::z_oihf(Register r1, int64_t i2) { emit_48( OIHF_ZOPC | regt(r1, 8, 48) | imm32(i2, 16, 48)); }
inline void Assembler::z_oilf(Register r1, int64_t i2) { emit_48( OILF_ZOPC | regt(r1, 8, 48) | imm32(i2, 16, 48)); }

inline void Assembler::z_x(   Register r1, int64_t d2, Register x2, Register b2) { emit_32( X_ZOPC  | regt(r1, 8, 32) | uimm12(d2, 20, 32) | reg(x2, 12, 32) | regz(b2, 16, 32)); }
inline void Assembler::z_xy(  Register r1, int64_t d2, Register x2, Register b2) { emit_48( XY_ZOPC | regt(r1, 8, 48) | simm20(d2)         | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_xg(  Register r1, int64_t d2, Register x2, Register b2) { emit_48( XG_ZOPC | regt(r1, 8, 48) | simm20(d2)         | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_x(   Register r1, const Address& a) { z_x( r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_xy(  Register r1, const Address& a) { z_xy(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_xg(  Register r1, const Address& a) { z_xg(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }

inline void Assembler::z_xr(  Register r1, Register r2)              { emit_16( XR_ZOPC   | regt(r1,  8, 16) | reg(r2, 12, 16)); }
inline void Assembler::z_xgr( Register r1, Register r2)              { emit_32( XGR_ZOPC  | regt(r1, 24, 32) | reg(r2, 28, 32)); }
inline void Assembler::z_xrk( Register r1, Register r2, Register r3) { emit_32( XRK_ZOPC  | regt(r1, 24, 32) | reg(r2, 28, 32) | reg(r3, 16, 32)); }
inline void Assembler::z_xgrk(Register r1, Register r2, Register r3) { emit_32( XGRK_ZOPC | regt(r1, 24, 32) | reg(r2, 28, 32) | reg(r3, 16, 32)); }

inline void Assembler::z_xihf(Register r1, int64_t i2) { emit_48( XIHF_ZOPC | regt(r1, 8, 48) | imm32(i2, 16, 48)); }
inline void Assembler::z_xilf(Register r1, int64_t i2) { emit_48( XILF_ZOPC | regt(r1, 8, 48) | imm32(i2, 16, 48)); }

inline void Assembler::z_nc(int64_t d1, int64_t l, Register b1, int64_t d2, Register b2) { emit_48( NC_ZOPC | uimm12(d1, 20, 48) | uimm8(l, 8, 48) | regz(b1, 16, 48) | uimm12(d2, 36, 48) | regz(b2, 32, 48)); }
inline void Assembler::z_oc(int64_t d1, int64_t l, Register b1, int64_t d2, Register b2) { emit_48( OC_ZOPC | uimm12(d1, 20, 48) | uimm8(l, 8, 48) | regz(b1, 16, 48) | uimm12(d2, 36, 48) | regz(b2, 32, 48)); }
inline void Assembler::z_xc(int64_t d1, int64_t l, Register b1, int64_t d2, Register b2) { emit_48( XC_ZOPC | uimm12(d1, 20, 48) | uimm8(l, 8, 48) | regz(b1, 16, 48) | uimm12(d2, 36, 48) | regz(b2, 32, 48)); }
inline void Assembler::z_nc(Address dst, int64_t len, Address src2) { assert(!dst.has_index() && !src2.has_index(), "Cannot encode index"); z_nc(dst.disp12(), len-1, dst.base(), src2.disp12(), src2.base()); }
inline void Assembler::z_oc(Address dst, int64_t len, Address src2) { assert(!dst.has_index() && !src2.has_index(), "Cannot encode index"); z_oc(dst.disp12(), len-1, dst.base(), src2.disp12(), src2.base()); }
inline void Assembler::z_xc(Address dst, int64_t len, Address src2) { assert(!dst.has_index() && !src2.has_index(), "Cannot encode index"); z_xc(dst.disp12(), len-1, dst.base(), src2.disp12(), src2.base()); }


//---------------
// ADD
//---------------
inline void Assembler::z_a(   Register r1, int64_t d2, Register x2, Register b2) { emit_32( A_ZOPC    | regt(r1, 8, 32) | uimm12(d2, 20, 32) | reg(x2, 12, 32) | regz(b2, 16, 32)); }
inline void Assembler::z_ay(  Register r1, int64_t d2, Register x2, Register b2) { emit_48( AY_ZOPC   | regt(r1, 8, 48) | simm20(d2)         | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_al(  Register r1, int64_t d2, Register x2, Register b2) { emit_32( AL_ZOPC   | regt(r1, 8, 32) | uimm12(d2, 20, 32) | reg(x2, 12, 32) | regz(b2, 16, 32)); }
inline void Assembler::z_aly( Register r1, int64_t d2, Register x2, Register b2) { emit_48( ALY_ZOPC  | regt(r1, 8, 48) | simm20(d2)         | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_ag(  Register r1, int64_t d2, Register x2, Register b2) { emit_48( AG_ZOPC   | regt(r1, 8, 48) | simm20(d2)         | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_agf( Register r1, int64_t d2, Register x2, Register b2) { emit_48( AGF_ZOPC  | regt(r1, 8, 48) | simm20(d2)         | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_alg( Register r1, int64_t d2, Register x2, Register b2) { emit_48( ALG_ZOPC  | regt(r1, 8, 48) | simm20(d2)         | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_algf(Register r1, int64_t d2, Register x2, Register b2) { emit_48( ALGF_ZOPC | regt(r1, 8, 48) | simm20(d2)         | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_a(   Register r1, const Address& a) { z_a(   r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_ay(  Register r1, const Address& a) { z_ay(  r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_al(  Register r1, const Address& a) { z_al(  r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_aly( Register r1, const Address& a) { z_aly( r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_ag(  Register r1, const Address& a) { z_ag(  r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_agf( Register r1, const Address& a) { z_agf( r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_alg( Register r1, const Address& a) { z_alg( r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_algf(Register r1, const Address& a) { z_algf(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }

inline void Assembler::z_ar(  Register r1, Register r2) { emit_16( AR_ZOPC   | regt(r1,  8, 16) | reg(r2, 12, 16)); }
inline void Assembler::z_agr( Register r1, Register r2) { emit_32( AGR_ZOPC  | regt(r1, 24, 32) | reg(r2, 28, 32)); }
inline void Assembler::z_agfr(Register r1, Register r2) { emit_32( AGFR_ZOPC | regt(r1, 24, 32) | reg(r2, 28, 32)); }
inline void Assembler::z_ark( Register r1, Register r2, Register r3) { emit_32( ARK_ZOPC  | regt(r1, 24, 32) | reg(r2, 28, 32) | reg(r3, 16, 32)); }
inline void Assembler::z_agrk(Register r1, Register r2, Register r3) { emit_32( AGRK_ZOPC | regt(r1, 24, 32) | reg(r2, 28, 32) | reg(r3, 16, 32)); }

inline void Assembler::z_ahi(  Register r1, int64_t i2) { emit_32( AHI_ZOPC  | regt(r1, 8, 32) | simm16(i2, 16, 32)); }
inline void Assembler::z_afi(  Register r1, int64_t i2) { emit_48( AFI_ZOPC  | regt(r1, 8, 48) | simm32(i2, 16, 48)); }
inline void Assembler::z_aghi( Register r1, int64_t i2) { emit_32( AGHI_ZOPC | regt(r1, 8, 32) | simm16(i2, 16, 32)); }
inline void Assembler::z_agfi( Register r1, int64_t i2) { emit_48( AGFI_ZOPC | regt(r1, 8, 48) | simm32(i2, 16, 48)); }
inline void Assembler::z_aih(  Register r1, int64_t i2) { emit_48( AIH_ZOPC  | regt(r1, 8, 48) | simm32(i2, 16, 48)); }
inline void Assembler::z_ahik( Register r1, Register r3, int64_t i2) { emit_48( AHIK_ZOPC  | regt(r1, 8, 48) | reg(r3, 12, 48) | simm16(i2, 16, 48)); }
inline void Assembler::z_aghik(Register r1, Register r3, int64_t i2) { emit_48( AGHIK_ZOPC | regt(r1, 8, 48) | reg(r3, 12, 48) | simm16(i2, 16, 48)); }


//-----------------------
// ADD LOGICAL
//-----------------------
inline void Assembler::z_alr(  Register r1, Register r2) { emit_16( ALR_ZOPC   | regt(r1,  8, 16) | reg(r2, 12, 16)); }
inline void Assembler::z_algr( Register r1, Register r2) { emit_32( ALGR_ZOPC  | regt(r1, 24, 32) | reg(r2, 28, 32)); }
inline void Assembler::z_algfr(Register r1, Register r2) { emit_32( ALGFR_ZOPC | regt(r1, 24, 32) | reg(r2, 28, 32)); }
inline void Assembler::z_alrk( Register r1, Register r2, Register r3) { emit_32( ALRK_ZOPC  | regt(r1, 24, 32) | reg(r2, 28, 32) | reg(r3, 16, 32)); }
inline void Assembler::z_algrk(Register r1, Register r2, Register r3) { emit_32( ALGRK_ZOPC | regt(r1, 24, 32) | reg(r2, 28, 32) | reg(r3, 16, 32)); }
inline void Assembler::z_alcgr(Register r1, Register r2) { emit_32( ALCGR_ZOPC | regt(r1, 24, 32) | reg(r2, 28, 32)); }

inline void Assembler::z_alfi( Register r1, int64_t i2) { emit_48( ALFI_ZOPC |  regt(r1, 8, 48) | uimm32(i2, 16, 48)); }
inline void Assembler::z_algfi(Register r1, int64_t i2) { emit_48( ALGFI_ZOPC | regt(r1, 8, 48) | uimm32(i2, 16, 48)); }

inline void Assembler::z_alhsik( Register r1, Register r3, int64_t i2) { emit_48( ALHSIK_ZOPC  | regt(r1, 8, 48) | reg(r3, 12, 48) | simm16(i2, 16, 48)); }
inline void Assembler::z_alghsik(Register r1, Register r3, int64_t i2) { emit_48( ALGHSIK_ZOPC | regt(r1, 8, 48) | reg(r3, 12, 48) | simm16(i2, 16, 48)); }

// In-memory arithmetic (add signed, add logical with signed immediate)
inline void Assembler::z_asi(  int64_t d1, Register b1, int64_t i2) { emit_48( ASI_ZOPC   | simm8(i2, 8, 48) | simm20(d1) | regz(b1, 16, 48)); }
inline void Assembler::z_agsi( int64_t d1, Register b1, int64_t i2) { emit_48( AGSI_ZOPC  | simm8(i2, 8, 48) | simm20(d1) | regz(b1, 16, 48)); }
inline void Assembler::z_alsi( int64_t d1, Register b1, int64_t i2) { emit_48( ALSI_ZOPC  | simm8(i2, 8, 48) | simm20(d1) | regz(b1, 16, 48)); }
inline void Assembler::z_algsi(int64_t d1, Register b1, int64_t i2) { emit_48( ALGSI_ZOPC | simm8(i2, 8, 48) | simm20(d1) | regz(b1, 16, 48)); }
inline void Assembler::z_asi(  const Address& d, int64_t i2) { assert(!d.has_index(), "No index in ASI");   z_asi(  d.disp(), d.base(), i2); }
inline void Assembler::z_agsi( const Address& d, int64_t i2) { assert(!d.has_index(), "No index in AGSI");  z_agsi( d.disp(), d.base(), i2); }
inline void Assembler::z_alsi( const Address& d, int64_t i2) { assert(!d.has_index(), "No index in ALSI");  z_alsi( d.disp(), d.base(), i2); }
inline void Assembler::z_algsi(const Address& d, int64_t i2) { assert(!d.has_index(), "No index in ALGSI"); z_algsi(d.disp(), d.base(), i2); }


//--------------------
// SUBTRACT
//--------------------
inline void Assembler::z_s(   Register r1, int64_t d2, Register x2, Register b2) { emit_32( S_ZOPC    | regt(r1, 8, 32) | uimm12(d2, 20, 32) | reg(x2, 12, 32) | regz(b2, 16, 32)); }
inline void Assembler::z_sy(  Register r1, int64_t d2, Register x2, Register b2) { emit_48( SY_ZOPC   | regt(r1, 8, 48) | simm20(d2)         | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_sg(  Register r1, int64_t d2, Register x2, Register b2) { emit_48( SG_ZOPC   | regt(r1, 8, 48) | simm20(d2)         | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_sgf( Register r1, int64_t d2, Register x2, Register b2) { emit_48( SGF_ZOPC  | regt(r1, 8, 48) | simm20(d2)         | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_slg( Register r1, int64_t d2, Register x2, Register b2) { emit_48( SLG_ZOPC  | regt(r1, 8, 48) | simm20(d2)         | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_slgf(Register r1, int64_t d2, Register x2, Register b2) { emit_48( SLGF_ZOPC | regt(r1, 8, 48) | simm20(d2)         | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_s(   Register r1, const Address& a) { z_s(   r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_sy(  Register r1, const Address& a) { z_sy(  r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_sg(  Register r1, const Address& a) { z_sg(  r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_sgf( Register r1, const Address& a) { z_sgf( r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_slg( Register r1, const Address& a) { z_slg( r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_slgf(Register r1, const Address& a) { z_slgf(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }

inline void Assembler::z_sr(  Register r1, Register r2) { emit_16( SR_ZOPC   | regt(r1,  8, 16) | reg(r2, 12, 16)); }
inline void Assembler::z_sgr( Register r1, Register r2) { emit_32( SGR_ZOPC  | regt(r1, 24, 32) | reg(r2, 28, 32)); }
inline void Assembler::z_sgfr(Register r1, Register r2) { emit_32( SGFR_ZOPC | regt(r1, 24, 32) | reg(r2, 28, 32)); }
inline void Assembler::z_srk( Register r1, Register r2, Register r3) { emit_32( SRK_ZOPC  | regt(r1, 24, 32) | reg(r2, 28, 32) | reg(r3, 16, 32)); }
inline void Assembler::z_sgrk(Register r1, Register r2, Register r3) { emit_32( SGRK_ZOPC | regt(r1, 24, 32) | reg(r2, 28, 32) | reg(r3, 16, 32)); }

inline void Assembler::z_sh(  Register r1, int64_t d2, Register x2, Register b2) { emit_32( SH_ZOPC  | regt(r1, 8, 32) | uimm12(d2, 20, 32) | reg(x2, 12, 32) | regz(b2, 16, 32)); }
inline void Assembler::z_shy( Register r1, int64_t d2, Register x2, Register b2) { emit_48( SHY_ZOPC | regt(r1, 8, 48) | simm20(d2)         | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_sh(  Register r1, const Address &a) { z_sh( r1, a.disp(), a.indexOrR0(), a.base()); }
inline void Assembler::z_shy( Register r1, const Address &a) { z_shy(r1, a.disp(), a.indexOrR0(), a.base()); }


//----------------------------
// SUBTRACT LOGICAL
//----------------------------
inline void Assembler::z_slr(  Register r1, Register r2) { emit_16( SLR_ZOPC   | regt(r1,  8, 16) | reg(r2, 12, 16)); }
inline void Assembler::z_slgr( Register r1, Register r2) { emit_32( SLGR_ZOPC  | regt(r1, 24, 32) | reg(r2, 28, 32)); }
inline void Assembler::z_slgfr(Register r1, Register r2) { emit_32( SLGFR_ZOPC | regt(r1, 24, 32) | reg(r2, 28, 32)); }
inline void Assembler::z_slrk( Register r1, Register r2, Register r3) { emit_32(SLRK_ZOPC  | regt(r1, 24, 32) | reg(r2, 28, 32) | reg(r3, 16, 32)); }
inline void Assembler::z_slgrk(Register r1, Register r2, Register r3) { emit_32(SLGRK_ZOPC | regt(r1, 24, 32) | reg(r2, 28, 32) | reg(r3, 16, 32)); }
inline void Assembler::z_slfi( Register r1, int64_t i2) { emit_48( SLFI_ZOPC  | regt(r1, 8, 48) | uimm32(i2, 16, 48)); }
inline void Assembler::z_slgfi(Register r1, int64_t i2) { emit_48( SLGFI_ZOPC | regt(r1, 8, 48) | uimm32(i2, 16, 48)); }


//--------------------
// MULTIPLY
//--------------------
inline void Assembler::z_msr(  Register r1, Register r2) { emit_32( MSR_ZOPC   | regt(r1, 24, 32) | reg(r2, 28, 32)); }
inline void Assembler::z_msgr( Register r1, Register r2) { emit_32( MSGR_ZOPC  | regt(r1, 24, 32) | reg(r2, 28, 32)); }
inline void Assembler::z_msgfr(Register r1, Register r2) { emit_32( MSGFR_ZOPC | regt(r1, 24, 32) | reg(r2, 28, 32)); }
inline void Assembler::z_mlr(  Register r1, Register r2) { emit_32( MLR_ZOPC   | regt(r1, 24, 32) | reg(r2, 28, 32)); }
inline void Assembler::z_mlgr( Register r1, Register r2) { emit_32( MLGR_ZOPC  | regt(r1, 24, 32) | reg(r2, 28, 32)); }

inline void Assembler::z_mhy( Register r1, int64_t d2, Register x2, Register b2) { emit_48( MHY_ZOPC  | regt(r1, 8, 48) | simm20(d2) | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_msy( Register r1, int64_t d2, Register x2, Register b2) { emit_48( MSY_ZOPC  | regt(r1, 8, 48) | simm20(d2) | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_msg( Register r1, int64_t d2, Register x2, Register b2) { emit_48( MSG_ZOPC  | regt(r1, 8, 48) | simm20(d2) | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_msgf(Register r1, int64_t d2, Register x2, Register b2) { emit_48( MSGF_ZOPC | regt(r1, 8, 48) | simm20(d2) | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_ml(  Register r1, int64_t d2, Register x2, Register b2) { emit_48( ML_ZOPC   | regt(r1, 8, 48) | simm20(d2) | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_mlg( Register r1, int64_t d2, Register x2, Register b2) { emit_48( MLG_ZOPC  | regt(r1, 8, 48) | simm20(d2) | reg(x2, 12, 48) | regz(b2, 16, 48)); }

inline void Assembler::z_mhy( Register r1, const Address& a) { z_mhy( r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_msy( Register r1, const Address& a) { z_msy( r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_msg( Register r1, const Address& a) { z_msg( r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_msgf(Register r1, const Address& a) { z_msgf(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_ml(  Register r1, const Address& a) { z_ml(  r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_mlg( Register r1, const Address& a) { z_mlg( r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }

inline void Assembler::z_msfi( Register r1, int64_t i2) { emit_48( MSFI_ZOPC  | regt(r1, 8, 48) | simm32(i2, 16, 48)); }
inline void Assembler::z_msgfi(Register r1, int64_t i2) { emit_48( MSGFI_ZOPC | regt(r1, 8, 48) | simm32(i2, 16, 48)); }
inline void Assembler::z_mhi(  Register r1, int64_t i2) { emit_32( MHI_ZOPC   | regt(r1, 8, 32) | simm16(i2, 16, 32)); }
inline void Assembler::z_mghi( Register r1, int64_t i2) { emit_32( MGHI_ZOPC  | regt(r1, 8, 32) | simm16(i2, 16, 32)); }


//------------------
// DIVIDE
//------------------
inline void Assembler::z_dsgr( Register r1, Register r2) { emit_32( DSGR_ZOPC | regt(r1, 24, 32) | reg(r2, 28, 32)); }
inline void Assembler::z_dsgfr(Register r1, Register r2) { emit_32( DSGFR_ZOPC | regt(r1, 24, 32) | reg(r2, 28, 32)); }


//-------------------
// COMPARE
//-------------------
inline void Assembler::z_cr(  Register r1, Register r2) { emit_16( CR_ZOPC   | reg(r1,  8, 16) | reg(r2,12,16)); }
inline void Assembler::z_cgr( Register r1, Register r2) { emit_32( CGR_ZOPC  | reg(r1, 24, 32) | reg(r2, 28, 32)); }
inline void Assembler::z_cgfr(Register r1, Register r2) { emit_32( CGFR_ZOPC | reg(r1, 24, 32) | reg(r2, 28, 32)); }
inline void Assembler::z_chi( Register r1, int64_t i2)  { emit_32( CHI_ZOPC  | reg(r1,  8, 32) | simm16(i2, 16, 32)); }
inline void Assembler::z_cghi(Register r1, int64_t i2)  { emit_32( CGHI_ZOPC | reg(r1,  8, 32) | simm16(i2, 16, 32)); }
inline void Assembler::z_cfi( Register r1, int64_t i2)  { emit_48( CFI_ZOPC  | regt(r1, 8, 48) | uimm32(i2, 16, 48)); }
inline void Assembler::z_cgfi(Register r1, int64_t i2)  { emit_48( CGFI_ZOPC | regt(r1, 8, 48) | uimm32(i2, 16, 48)); }
inline void Assembler::z_ch(Register r1, const Address &a) { z_ch(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_ch(Register r1, int64_t d2, Register x2, Register b2) { emit_32( CH_ZOPC | reg(r1, 8, 32) | uimm12(d2, 20, 32) | reg(x2, 12, 32) | regz(b2, 16, 32)); }
inline void Assembler::z_c(Register r1, const Address &a) { z_c(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_c(Register r1,  int64_t d2, Register x2, Register b2) { emit_32( C_ZOPC  | reg(r1, 8, 32) | uimm12(d2, 20, 32) | reg(x2, 12, 32) | regz(b2, 16, 32)); }
inline void Assembler::z_cy(Register r1, const Address &a) { z_cy(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_cy(Register r1, int64_t d2, Register x2, Register b2) { emit_48( CY_ZOPC | regt(r1, 8, 48) | simm20(d2) | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_cy(Register r1, int64_t d2, Register b2) { z_cy(r1, d2, Z_R0, b2); }
inline void Assembler::z_cg(Register r1, const Address &a) { z_cg(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_cg(Register r1, int64_t d2, Register x2, Register b2) { emit_48( CG_ZOPC | reg(r1, 8, 48) | simm20(d2) | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_clr(Register r1, Register r2) { emit_16( CLR_ZOPC | reg(r1,8,16) | reg(r2,12,16)); }
inline void Assembler::z_clgr(Register r1, Register r2) { emit_32( CLGR_ZOPC | regt(r1, 24, 32) | reg(r2, 28, 32)); }


inline void Assembler::z_clfi(Register r1, int64_t i2)  { emit_48( CLFI_ZOPC  | regt(r1, 8, 48) | uimm32(i2, 16, 48)); }
inline void Assembler::z_clgfi(Register r1, int64_t i2) { emit_48( CLGFI_ZOPC | regt(r1, 8, 48) | uimm32(i2, 16, 48)); }
inline void Assembler::z_cl(Register r1, const Address &a) { z_cl(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_cl(Register r1, int64_t d2, Register x2, Register b2) { emit_32( CL_ZOPC | regt(r1, 8, 32) | uimm12(d2,20,32) | reg(x2, 12, 32) | regz(b2, 16, 32)); }
inline void Assembler::z_cly(Register r1, const Address &a) { z_cly(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_cly(Register r1, int64_t d2, Register x2, Register b2) { emit_48( CLY_ZOPC | regt(r1, 8, 48) | simm20(d2) | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_cly(Register r1, int64_t d2, Register b2) { z_cly(r1, d2, Z_R0, b2); }
inline void Assembler::z_clg(Register r1, const Address &a) { z_clg(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_clg(Register r1, int64_t d2, Register x2, Register b2) { emit_48( CLG_ZOPC | reg(r1, 8, 48) | simm20(d2) | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_clc(int64_t d1, int64_t l, Register b1, int64_t d2, Register b2) { emit_48( CLC_ZOPC | uimm12(d1, 20, 48) | uimm8(l, 8, 48) | regz(b1, 16, 48) | uimm12(d2, 36, 48) | regz(b2, 32, 48)); }
inline void Assembler::z_clcle(Register r1, Register r3, int64_t d2, Register b2) { emit_32( CLCLE_ZOPC | reg(r1, 8, 32) | reg(r3, 12, 32) | uimm12(d2, 20, 32) | reg(b2, 16, 32)); }
inline void Assembler::z_clclu(Register r1, Register r3, int64_t d2, Register b2) { emit_48( CLCLU_ZOPC | reg(r1, 8, 48) | reg(r3, 12, 48) | uimm12(d2, 20, 48) | reg(b2, 16, 48)); }

inline void Assembler::z_tmll(Register r1, int64_t i2) { emit_32( TMLL_ZOPC | regt(r1, 8, 32) | imm16(i2, 16, 32)); }
inline void Assembler::z_tmlh(Register r1, int64_t i2) { emit_32( TMLH_ZOPC | regt(r1, 8, 32) | imm16(i2, 16, 32)); }
inline void Assembler::z_tmhl(Register r1, int64_t i2) { emit_32( TMHL_ZOPC | regt(r1, 8, 32) | imm16(i2, 16, 32)); }
inline void Assembler::z_tmhh(Register r1, int64_t i2) { emit_32( TMHH_ZOPC | regt(r1, 8, 32) | imm16(i2, 16, 32)); }

// translate characters
inline void Assembler::z_troo(Register r1, Register r2, int64_t m3) { emit_32( TROO_ZOPC | reg(r1, 24, 32) | reg(r2, 28, 32) | uimm4(m3, 16, 32)); }
inline void Assembler::z_trot(Register r1, Register r2, int64_t m3) { emit_32( TROT_ZOPC | reg(r1, 24, 32) | reg(r2, 28, 32) | uimm4(m3, 16, 32)); }
inline void Assembler::z_trto(Register r1, Register r2, int64_t m3) { emit_32( TRTO_ZOPC | reg(r1, 24, 32) | reg(r2, 28, 32) | uimm4(m3, 16, 32)); }
inline void Assembler::z_trtt(Register r1, Register r2, int64_t m3) { emit_32( TRTT_ZOPC | reg(r1, 24, 32) | reg(r2, 28, 32) | uimm4(m3, 16, 32)); }

// signed comparison
inline void Assembler::z_crb(Register r1,  Register r2, branch_condition m3, int64_t d4, Register b4) { emit_48( CRB_ZOPC  | reg(r1, 8, 48) | reg(r2, 12, 48) | uimm12(d4, 20, 48) | reg(b4, 16, 48) | uimm4(m3, 32, 48)); }
inline void Assembler::z_cgrb(Register r1, Register r2, branch_condition m3, int64_t d4, Register b4) { emit_48( CGRB_ZOPC | reg(r1, 8, 48) | reg(r2, 12, 48) | uimm12(d4, 20, 48) | reg(b4, 16, 48) | uimm4(m3, 32, 48)); }
inline void Assembler::z_crj(Register r1,  Register r2, branch_condition m3, address a4)              { emit_48( CRJ_ZOPC  | reg(r1, 8, 48) | reg(r2, 12, 48) | simm16(RelAddr::pcrel_off16(a4, pc()), 16, 48) | uimm4(m3, 32, 48)); }
inline void Assembler::z_cgrj(Register r1, Register r2, branch_condition m3, address a4)              { emit_48( CGRJ_ZOPC | reg(r1, 8, 48) | reg(r2, 12, 48) | simm16(RelAddr::pcrel_off16(a4, pc()), 16, 48) | uimm4(m3, 32, 48)); }
inline void Assembler::z_cib(Register r1,  int64_t i2, branch_condition m3, int64_t d4, Register b4)  { emit_48( CIB_ZOPC  | reg(r1, 8, 48) | uimm4(m3, 12, 48) | uimm12(d4, 20, 48) | reg(b4, 16, 48) | simm8(i2, 32, 48)); }
inline void Assembler::z_cgib(Register r1, int64_t i2, branch_condition m3, int64_t d4, Register b4)  { emit_48( CGIB_ZOPC | reg(r1, 8, 48) | uimm4(m3, 12, 48) | uimm12(d4, 20, 48) | reg(b4, 16, 48) | simm8(i2, 32, 48)); }
inline void Assembler::z_cij(Register r1,  int64_t i2, branch_condition m3, address a4)               { emit_48( CIJ_ZOPC  | reg(r1, 8, 48) | uimm4(m3, 12, 48) | simm16(RelAddr::pcrel_off16(a4, pc()), 16, 48) | simm8(i2, 32, 48)); }
inline void Assembler::z_cgij(Register r1, int64_t i2, branch_condition m3, address a4)               { emit_48( CGIJ_ZOPC | reg(r1, 8, 48) | uimm4(m3, 12, 48) | simm16(RelAddr::pcrel_off16(a4, pc()), 16, 48) | simm8(i2, 32, 48)); }
// unsigned comparison
inline void Assembler::z_clrb(Register r1,  Register r2, branch_condition m3, int64_t d4, Register b4) { emit_48( CLRB_ZOPC  | reg(r1, 8, 48) | reg(r2, 12, 48) | uimm12(d4, 20, 48) | reg(b4, 16, 48) | uimm4(m3, 32, 48)); }
inline void Assembler::z_clgrb(Register r1, Register r2, branch_condition m3, int64_t d4, Register b4) { emit_48( CLGRB_ZOPC | reg(r1, 8, 48) | reg(r2, 12, 48) | uimm12(d4, 20, 48) | reg(b4, 16, 48) | uimm4(m3, 32, 48)); }
inline void Assembler::z_clrj(Register r1,  Register r2, branch_condition m3, address a4)              { emit_48( CLRJ_ZOPC  | reg(r1, 8, 48) | reg(r2, 12, 48) | simm16(RelAddr::pcrel_off16(a4, pc()), 16, 48) | uimm4(m3, 32, 48)); }
inline void Assembler::z_clgrj(Register r1, Register r2, branch_condition m3, address a4)              { emit_48( CLGRJ_ZOPC | reg(r1, 8, 48) | reg(r2, 12, 48) | simm16(RelAddr::pcrel_off16(a4, pc()), 16, 48) | uimm4(m3, 32, 48)); }
inline void Assembler::z_clib(Register r1,  int64_t i2, branch_condition m3, int64_t d4, Register b4)  { emit_48( CLIB_ZOPC  | reg(r1, 8, 48) | uimm4(m3, 12, 48) | uimm12(d4, 20, 48) | reg(b4, 16, 48) | uimm8(i2, 32, 48)); }
inline void Assembler::z_clgib(Register r1, int64_t i2, branch_condition m3, int64_t d4, Register b4)  { emit_48( CLGIB_ZOPC | reg(r1, 8, 48) | uimm4(m3, 12, 48) | uimm12(d4, 20, 48) | reg(b4, 16, 48) | uimm8(i2, 32, 48)); }
inline void Assembler::z_clij(Register r1,  int64_t i2, branch_condition m3, address a4)               { emit_48( CLIJ_ZOPC  | reg(r1, 8, 48) | uimm4(m3, 12, 48) | simm16(RelAddr::pcrel_off16(a4, pc()), 16, 48) | uimm8(i2, 32, 48)); }
inline void Assembler::z_clgij(Register r1, int64_t i2, branch_condition m3, address a4)               { emit_48( CLGIJ_ZOPC | reg(r1, 8, 48) | uimm4(m3, 12, 48) | simm16(RelAddr::pcrel_off16(a4, pc()), 16, 48) | uimm8(i2, 32, 48)); }

// Compare and trap instructions (signed).
inline void Assembler::z_crt(Register  r1, Register r2, int64_t m3)  { emit_32( CRT_ZOPC   | reg(r1, 24, 32) | reg(r2, 28, 32) | uimm4(m3, 16, 32)); }
inline void Assembler::z_cgrt(Register r1, Register r2, int64_t m3)  { emit_32( CGRT_ZOPC  | reg(r1, 24, 32) | reg(r2, 28, 32) | uimm4(m3, 16, 32)); }
inline void Assembler::z_cit(Register  r1, int64_t i2, int64_t m3)   { emit_48( CIT_ZOPC   | reg(r1,  8, 48) | simm16(i2, 16, 48) | uimm4(m3, 32, 48)); }
inline void Assembler::z_cgit(Register r1, int64_t i2, int64_t m3)   { emit_48( CGIT_ZOPC  | reg(r1,  8, 48) | simm16(i2, 16, 48) | uimm4(m3, 32, 48)); }

// Compare and trap instructions (unsigned).
inline void Assembler::z_clrt(Register  r1, Register r2, int64_t m3) { emit_32( CLRT_ZOPC  | reg(r1, 24, 32) | reg(r2, 28, 32) | uimm4(m3, 16, 32)); }
inline void Assembler::z_clgrt(Register r1, Register r2, int64_t m3) { emit_32( CLGRT_ZOPC | reg(r1, 24, 32) | reg(r2, 28, 32) | uimm4(m3, 16, 32)); }
inline void Assembler::z_clfit(Register r1, int64_t i2, int64_t m3)  { emit_48( CLFIT_ZOPC | reg(r1,  8, 48) | uimm16(i2, 16, 48) | uimm4(m3, 32, 48)); }
inline void Assembler::z_clgit(Register r1, int64_t i2, int64_t m3)  { emit_48( CLGIT_ZOPC | reg(r1,  8, 48) | uimm16(i2, 16, 48) | uimm4(m3, 32, 48)); }

inline void Assembler::z_bc(  branch_condition m1, int64_t d2, Register x2, Register b2) { emit_32( BC_ZOPC | 0 << 16 | uimm4(m1, 8, 32) | uimm12(d2, 20, 32) | reg(x2, 12, 32) | regz(b2, 16, 32)); }
inline void Assembler::z_bcr( branch_condition m1, Register r2) { emit_16( BCR_ZOPC | uimm4(m1,8,16) | reg(r2,12,16)); }
inline void Assembler::z_brc( branch_condition i1, int64_t i2)  { emit_32( BRC_ZOPC | uimm4(i1, 8, 32) | simm16(i2, 16, 32)); }
inline void Assembler::z_brc( branch_condition i1, address a)   { emit_32( BRC_ZOPC | uimm4(i1, 8, 32) | simm16(RelAddr::pcrel_off16(a, pc()), 16, 32)); }
inline void Assembler::z_brcl(branch_condition i1, address a)   { emit_48( BRCL_ZOPC | uimm4(i1, 8, 48)| simm32(RelAddr::pcrel_off32(a, pc()), 16, 48)); }
inline void Assembler::z_bctgr(Register r1, Register r2)        { emit_32( BCTGR_ZOPC | reg( r1, 24, 32) | reg( r2, 28, 32)); };

inline void Assembler::z_basr(Register r1, Register r2) { emit_16( BASR_ZOPC | regt(r1,8,16) | reg(r2,12,16)); }

inline void Assembler::z_brasl(Register r1, address a) { emit_48( BRASL_ZOPC | regt(r1, 8, 48) | simm32(RelAddr::pcrel_off32(a, pc()), 16, 48)); }

inline void Assembler::z_brct(Register r1, address a) { emit_32( BRCT_ZOPC | regt(r1, 8, 32) | simm16(RelAddr::pcrel_off16(a, pc()), 16, 32)); }
inline void Assembler::z_brct(Register r1, Label& L) {z_brct(r1, target(L)); }

inline void Assembler::z_brxh(Register r1, Register r3, address a) {emit_32( BRXH_ZOPC | reg(r1, 8, 32) | reg(r3, 12, 32)  | simm16(RelAddr::pcrel_off16(a, pc()), 16, 32));}
inline void Assembler::z_brxh(Register r1, Register r3, Label& L) {z_brxh(r1, r3, target(L)); }

inline void Assembler::z_brxle(Register r1, Register r3, address a) {emit_32( BRXLE_ZOPC | reg(r1, 8, 32) | reg(r3, 12, 32) | simm16(RelAddr::pcrel_off16(a, pc()), 16, 32));}
inline void Assembler::z_brxle(Register r1, Register r3, Label& L) {z_brxle(r1, r3, target(L)); }

inline void Assembler::z_brxhg(Register r1, Register r3, address a) {emit_48( BRXHG_ZOPC | reg(r1, 8, 48) | reg(r3, 12, 48) | simm16(RelAddr::pcrel_off16(a, pc()), 16, 48));}
inline void Assembler::z_brxhg(Register r1, Register r3, Label& L) {z_brxhg(r1, r3, target(L)); }

inline void Assembler::z_brxlg(Register r1, Register r3, address a) {emit_48( BRXLG_ZOPC | reg(r1, 8, 48) | reg(r3, 12, 48) | simm16(RelAddr::pcrel_off16(a, pc()), 16, 48));}
inline void Assembler::z_brxlg(Register r1, Register r3, Label& L) {z_brxlg(r1, r3, target(L)); }

inline void Assembler::z_flogr(Register r1, Register r2) { emit_32( FLOGR_ZOPC  | reg(r1, 24, 32) | reg(r2, 28, 32)); }
inline void Assembler::z_popcnt(Register r1, Register r2) { emit_32( POPCNT_ZOPC  | reg(r1, 24, 32) | reg(r2, 28, 32)); }
inline void Assembler::z_ahhhr(Register r1, Register r2, Register r3) { emit_32( AHHHR_ZOPC  | reg(r3, 16, 32) | reg(r1, 24, 32) | reg(r2, 28, 32)); }
inline void Assembler::z_ahhlr(Register r1, Register r2, Register r3) { emit_32( AHHLR_ZOPC  | reg(r3, 16, 32) | reg(r1, 24, 32) | reg(r2, 28, 32)); }

inline void Assembler::z_tam() { emit_16( TAM_ZOPC); }
inline void Assembler::z_stckf(int64_t d2, Register b2) { emit_32( STCKF_ZOPC | uimm12(d2, 20, 32) | regz(b2, 16, 32)); }
inline void Assembler::z_stm( Register r1, Register r3, int64_t d2, Register b2) { emit_32( STM_ZOPC  | reg(r1, 8, 32) | reg(r3,12,32)| reg(b2,16,32) | uimm12(d2, 20,32)); }
inline void Assembler::z_stmy(Register r1, Register r3, int64_t d2, Register b2) { emit_48( STMY_ZOPC | reg(r1, 8, 48) | reg(r3,12,48)| reg(b2,16,48) | simm20(d2) ); }
inline void Assembler::z_stmg(Register r1, Register r3, int64_t d2, Register b2) { emit_48( STMG_ZOPC | reg(r1, 8, 48) | reg(r3,12,48)| reg(b2,16,48) | simm20(d2) ); }
inline void Assembler::z_lm(  Register r1, Register r3, int64_t d2, Register b2) { emit_32( LM_ZOPC   | reg(r1, 8, 32) | reg(r3,12,32)| reg(b2,16,32) | uimm12(d2, 20,32)); }
inline void Assembler::z_lmy( Register r1, Register r3, int64_t d2, Register b2) { emit_48( LMY_ZOPC  | reg(r1, 8, 48) | reg(r3,12,48)| reg(b2,16,48) | simm20(d2) ); }
inline void Assembler::z_lmg( Register r1, Register r3, int64_t d2, Register b2) { emit_48( LMG_ZOPC  | reg(r1, 8, 48) | reg(r3,12,48)| reg(b2,16,48) | simm20(d2) ); }

inline void Assembler::z_cs( Register r1, Register r3, int64_t d2, Register b2) { emit_32( CS_ZOPC  | regt(r1, 8, 32) | reg(r3, 12, 32) | reg(b2, 16, 32) | uimm12(d2, 20, 32)); }
inline void Assembler::z_csy(Register r1, Register r3, int64_t d2, Register b2) { emit_48( CSY_ZOPC | regt(r1, 8, 48) | reg(r3, 12, 48) | reg(b2, 16, 48) | simm20(d2)); }
inline void Assembler::z_csg(Register r1, Register r3, int64_t d2, Register b2) { emit_48( CSG_ZOPC | regt(r1, 8, 48) | reg(r3, 12, 48) | reg(b2, 16, 48) | simm20(d2)); }
inline void Assembler::z_cs( Register r1, Register r3, const Address& a) { assert(!a.has_index(), "Cannot encode index"); z_cs( r1, r3, a.disp(), a.baseOrR0()); }
inline void Assembler::z_csy(Register r1, Register r3, const Address& a) { assert(!a.has_index(), "Cannot encode index"); z_csy(r1, r3, a.disp(), a.baseOrR0()); }
inline void Assembler::z_csg(Register r1, Register r3, const Address& a) { assert(!a.has_index(), "Cannot encode index"); z_csg(r1, r3, a.disp(), a.baseOrR0()); }

inline void Assembler::z_cvd(Register r1, int64_t d2, Register x2, Register b2)  { emit_32( CVD_ZOPC  | regt(r1, 8, 32) | reg(x2, 12, 32) | reg(b2, 16, 32) | uimm12(d2, 20, 32)); }
inline void Assembler::z_cvdg(Register r1, int64_t d2, Register x2, Register b2) { emit_48( CVDG_ZOPC | regt(r1, 8, 48) | reg(x2, 12, 48) | reg(b2, 16, 48) | simm20(d2)); }


//---------------------------
//--  Vector Instructions  --
//---------------------------

//---<  Vector Support Instructions  >---

// Load (transfer from memory)
inline void Assembler::z_vlm(    VectorRegister v1, VectorRegister v3, int64_t d2, Register b2)       {emit_48(VLM_ZOPC   | vreg(v1,  8)     | vreg(v3, 12)     | rsmask_48(d2,     b2)); }
inline void Assembler::z_vl(     VectorRegister v1, int64_t d2, Register x2, Register b2)             {emit_48(VL_ZOPC    | vreg(v1,  8)                        | rxmask_48(d2, x2, b2)); }
inline void Assembler::z_vleb(   VectorRegister v1, int64_t d2, Register x2, Register b2, int64_t m3) {emit_48(VLEB_ZOPC  | vreg(v1,  8)                        | rxmask_48(d2, x2, b2) | veix_mask(m3, VRET_BYTE, 32)); }
inline void Assembler::z_vleh(   VectorRegister v1, int64_t d2, Register x2, Register b2, int64_t m3) {emit_48(VLEH_ZOPC  | vreg(v1,  8)                        | rxmask_48(d2, x2, b2) | veix_mask(m3, VRET_HW,   32)); }
inline void Assembler::z_vlef(   VectorRegister v1, int64_t d2, Register x2, Register b2, int64_t m3) {emit_48(VLEF_ZOPC  | vreg(v1,  8)                        | rxmask_48(d2, x2, b2) | veix_mask(m3, VRET_FW,   32)); }
inline void Assembler::z_vleg(   VectorRegister v1, int64_t d2, Register x2, Register b2, int64_t m3) {emit_48(VLEG_ZOPC  | vreg(v1,  8)                        | rxmask_48(d2, x2, b2) | veix_mask(m3, VRET_DW,   32)); }

// Gather/Scatter
inline void Assembler::z_vgef(   VectorRegister v1, int64_t d2, VectorRegister vx2, Register b2, int64_t m3) {emit_48(VGEF_ZOPC  | vreg(v1,  8)                 | rvmask_48(d2, vx2, b2) | veix_mask(m3, VRET_FW,   32)); }
inline void Assembler::z_vgeg(   VectorRegister v1, int64_t d2, VectorRegister vx2, Register b2, int64_t m3) {emit_48(VGEG_ZOPC  | vreg(v1,  8)                 | rvmask_48(d2, vx2, b2) | veix_mask(m3, VRET_DW,   32)); }

inline void Assembler::z_vscef(  VectorRegister v1, int64_t d2, VectorRegister vx2, Register b2, int64_t m3) {emit_48(VSCEF_ZOPC | vreg(v1,  8)                 | rvmask_48(d2, vx2, b2) | veix_mask(m3, VRET_FW,   32)); }
inline void Assembler::z_vsceg(  VectorRegister v1, int64_t d2, VectorRegister vx2, Register b2, int64_t m3) {emit_48(VSCEG_ZOPC | vreg(v1,  8)                 | rvmask_48(d2, vx2, b2) | veix_mask(m3, VRET_DW,   32)); }

// load and replicate
inline void Assembler::z_vlrep(  VectorRegister v1, int64_t d2, Register x2, Register b2, int64_t m3) {emit_48(VLREP_ZOPC | vreg(v1,  8)                        | rxmask_48(d2, x2, b2) | vesc_mask(m3, VRET_BYTE, VRET_DW, 32)); }
inline void Assembler::z_vlrepb( VectorRegister v1, int64_t d2, Register x2, Register b2)             {z_vlrep(v1, d2, x2, b2, VRET_BYTE); }// load byte and replicate to all vector elements of type 'B'
inline void Assembler::z_vlreph( VectorRegister v1, int64_t d2, Register x2, Register b2)             {z_vlrep(v1, d2, x2, b2, VRET_HW); }  // load HW   and replicate to all vector elements of type 'H'
inline void Assembler::z_vlrepf( VectorRegister v1, int64_t d2, Register x2, Register b2)             {z_vlrep(v1, d2, x2, b2, VRET_FW); }  // load FW   and replicate to all vector elements of type 'F'
inline void Assembler::z_vlrepg( VectorRegister v1, int64_t d2, Register x2, Register b2)             {z_vlrep(v1, d2, x2, b2, VRET_DW); }  // load DW   and replicate to all vector elements of type 'G'

inline void Assembler::z_vllez(  VectorRegister v1, int64_t d2, Register x2, Register b2, int64_t m3) {emit_48(VLLEZ_ZOPC | vreg(v1,  8)                        | rxmask_48(d2, x2, b2) | vesc_mask(m3, VRET_BYTE, VRET_DW, 32)); }
inline void Assembler::z_vllezb( VectorRegister v1, int64_t d2, Register x2, Register b2)             {z_vllez(v1, d2, x2, b2, VRET_BYTE); }// load logical byte into left DW of VR, zero all other bit positions.
inline void Assembler::z_vllezh( VectorRegister v1, int64_t d2, Register x2, Register b2)             {z_vllez(v1, d2, x2, b2, VRET_HW); }  // load logical HW   into left DW of VR, zero all other bit positions.
inline void Assembler::z_vllezf( VectorRegister v1, int64_t d2, Register x2, Register b2)             {z_vllez(v1, d2, x2, b2, VRET_FW); }  // load logical FW   into left DW of VR, zero all other bit positions.
inline void Assembler::z_vllezg( VectorRegister v1, int64_t d2, Register x2, Register b2)             {z_vllez(v1, d2, x2, b2, VRET_DW); }  // load logical DW   into left DW of VR, zero all other bit positions.

inline void Assembler::z_vlbb(   VectorRegister v1, int64_t d2, Register x2, Register b2, int64_t m3) {emit_48(VLBB_ZOPC  | vreg(v1,  8)                        | rxmask_48(d2, x2, b2) | uimm4(m3, 32, 48)); }
inline void Assembler::z_vll(    VectorRegister v1, Register r3, int64_t d2, Register b2)             {emit_48(VLL_ZOPC   | vreg(v1,  8)     |  reg(r3, 12, 48) | rsmask_48(d2,     b2)); }

// Load (register to register)
inline void Assembler::z_vlr (   VectorRegister v1, VectorRegister v2)                                {emit_48(VLR_ZOPC   | vreg(v1,  8)     | vreg(v2, 12)); }

inline void Assembler::z_vlgv(   Register r1, VectorRegister v3, int64_t d2, Register b2, int64_t m4) {emit_48(VLGV_ZOPC  |  reg(r1,  8, 48) | vreg(v3, 12)     | rsmask_48(d2,     b2) | vesc_mask(m4, VRET_BYTE, VRET_DW, 32)); }
inline void Assembler::z_vlgvb(  Register r1, VectorRegister v3, int64_t d2, Register b2)             {z_vlgv(r1, v3, d2, b2, VRET_BYTE); } // load byte from VR element (index d2(b2)) into GR (logical)
inline void Assembler::z_vlgvh(  Register r1, VectorRegister v3, int64_t d2, Register b2)             {z_vlgv(r1, v3, d2, b2, VRET_HW); }   // load HW   from VR element (index d2(b2)) into GR (logical)
inline void Assembler::z_vlgvf(  Register r1, VectorRegister v3, int64_t d2, Register b2)             {z_vlgv(r1, v3, d2, b2, VRET_FW); }   // load FW   from VR element (index d2(b2)) into GR (logical)
inline void Assembler::z_vlgvg(  Register r1, VectorRegister v3, int64_t d2, Register b2)             {z_vlgv(r1, v3, d2, b2, VRET_DW); }   // load DW   from VR element (index d2(b2)) into GR.

inline void Assembler::z_vlvg(   VectorRegister v1, Register r3, int64_t d2, Register b2, int64_t m4) {emit_48(VLVG_ZOPC  | vreg(v1,  8)     |  reg(r3, 12, 48) | rsmask_48(d2,     b2) | vesc_mask(m4, VRET_BYTE, VRET_DW, 32)); }
inline void Assembler::z_vlvgb(  VectorRegister v1, Register r3, int64_t d2, Register b2)             {z_vlvg(v1, r3, d2, b2, VRET_BYTE); }
inline void Assembler::z_vlvgh(  VectorRegister v1, Register r3, int64_t d2, Register b2)             {z_vlvg(v1, r3, d2, b2, VRET_HW); }
inline void Assembler::z_vlvgf(  VectorRegister v1, Register r3, int64_t d2, Register b2)             {z_vlvg(v1, r3, d2, b2, VRET_FW); }
inline void Assembler::z_vlvgg(  VectorRegister v1, Register r3, int64_t d2, Register b2)             {z_vlvg(v1, r3, d2, b2, VRET_DW); }

inline void Assembler::z_vlvgp(  VectorRegister v1, Register r2, Register r3)                         {emit_48(VLVGP_ZOPC | vreg(v1,  8)     |  reg(r2, 12, 48) |  reg(r3, 16, 48)); }

// vector register pack
inline void Assembler::z_vpk(    VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4) {emit_48(VPK_ZOPC   | vreg(v1,  8)     | vreg(v2, 12)     | vreg(v3, 16)     | vesc_mask(m4, VRET_HW, VRET_DW, 32)); }
inline void Assembler::z_vpkh(   VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vpk(v1, v2, v3, VRET_HW); }       // vector element type 'H'
inline void Assembler::z_vpkf(   VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vpk(v1, v2, v3, VRET_FW); }       // vector element type 'F'
inline void Assembler::z_vpkg(   VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vpk(v1, v2, v3, VRET_DW); }       // vector element type 'G'

inline void Assembler::z_vpks(   VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4, int64_t cc5) {emit_48(VPKS_ZOPC  | vreg(v1,  8) |  vreg(v2, 12) | vreg(v3, 16) | vesc_mask(m4, VRET_HW, VRET_DW, 32) | voprc_ccmask(cc5, 24)); }
inline void Assembler::z_vpksh(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vpks(v1, v2, v3, VRET_HW, VOPRC_CCIGN); }   // vector element type 'H', don't set CC
inline void Assembler::z_vpksf(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vpks(v1, v2, v3, VRET_FW, VOPRC_CCIGN); }   // vector element type 'F', don't set CC
inline void Assembler::z_vpksg(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vpks(v1, v2, v3, VRET_DW, VOPRC_CCIGN); }   // vector element type 'G', don't set CC
inline void Assembler::z_vpkshs( VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vpks(v1, v2, v3, VRET_HW, VOPRC_CCSET); }   // vector element type 'H', set CC
inline void Assembler::z_vpksfs( VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vpks(v1, v2, v3, VRET_FW, VOPRC_CCSET); }   // vector element type 'F', set CC
inline void Assembler::z_vpksgs( VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vpks(v1, v2, v3, VRET_DW, VOPRC_CCSET); }   // vector element type 'G', set CC

inline void Assembler::z_vpkls(  VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4, int64_t cc5) {emit_48(VPKLS_ZOPC | vreg(v1,  8) |  vreg(v2, 12) | vreg(v3, 16) | vesc_mask(m4, VRET_HW, VRET_DW, 32) | voprc_ccmask(cc5, 24)); }
inline void Assembler::z_vpklsh( VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vpkls(v1, v2, v3, VRET_HW, VOPRC_CCIGN); }  // vector element type 'H', don't set CC
inline void Assembler::z_vpklsf( VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vpkls(v1, v2, v3, VRET_FW, VOPRC_CCIGN); }  // vector element type 'F', don't set CC
inline void Assembler::z_vpklsg( VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vpkls(v1, v2, v3, VRET_DW, VOPRC_CCIGN); }  // vector element type 'G', don't set CC
inline void Assembler::z_vpklshs(VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vpkls(v1, v2, v3, VRET_HW, VOPRC_CCSET); }  // vector element type 'H', set CC
inline void Assembler::z_vpklsfs(VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vpkls(v1, v2, v3, VRET_FW, VOPRC_CCSET); }  // vector element type 'F', set CC
inline void Assembler::z_vpklsgs(VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vpkls(v1, v2, v3, VRET_DW, VOPRC_CCSET); }  // vector element type 'G', set CC

// vector register unpack (sign-extended)
inline void Assembler::z_vuph(   VectorRegister v1, VectorRegister v2, int64_t m3)                    {emit_48(VUPH_ZOPC  | vreg(v1,  8)     | vreg(v2, 12)     | vesc_mask(m3, VRET_BYTE, VRET_FW, 32)); }
inline void Assembler::z_vuphb(  VectorRegister v1, VectorRegister v2)                                {z_vuph(v1, v2, VRET_BYTE); }        // vector element type 'B'
inline void Assembler::z_vuphh(  VectorRegister v1, VectorRegister v2)                                {z_vuph(v1, v2, VRET_HW); }          // vector element type 'H'
inline void Assembler::z_vuphf(  VectorRegister v1, VectorRegister v2)                                {z_vuph(v1, v2, VRET_FW); }          // vector element type 'F'
inline void Assembler::z_vupl(   VectorRegister v1, VectorRegister v2, int64_t m3)                    {emit_48(VUPL_ZOPC  | vreg(v1,  8)     | vreg(v2, 12)     | vesc_mask(m3, VRET_BYTE, VRET_FW, 32)); }
inline void Assembler::z_vuplb(  VectorRegister v1, VectorRegister v2)                                {z_vupl(v1, v2, VRET_BYTE); }        // vector element type 'B'
inline void Assembler::z_vuplh(  VectorRegister v1, VectorRegister v2)                                {z_vupl(v1, v2, VRET_HW); }          // vector element type 'H'
inline void Assembler::z_vuplf(  VectorRegister v1, VectorRegister v2)                                {z_vupl(v1, v2, VRET_FW); }          // vector element type 'F'

// vector register unpack (zero-extended)
inline void Assembler::z_vuplh(  VectorRegister v1, VectorRegister v2, int64_t m3)                    {emit_48(VUPLH_ZOPC | vreg(v1,  8)     | vreg(v2, 12)     | vesc_mask(m3, VRET_BYTE, VRET_FW, 32)); }
inline void Assembler::z_vuplhb( VectorRegister v1, VectorRegister v2)                                {z_vuplh(v1, v2, VRET_BYTE); }       // vector element type 'B'
inline void Assembler::z_vuplhh( VectorRegister v1, VectorRegister v2)                                {z_vuplh(v1, v2, VRET_HW); }         // vector element type 'H'
inline void Assembler::z_vuplhf( VectorRegister v1, VectorRegister v2)                                {z_vuplh(v1, v2, VRET_FW); }         // vector element type 'F'
inline void Assembler::z_vupll(  VectorRegister v1, VectorRegister v2, int64_t m3)                    {emit_48(VUPLL_ZOPC | vreg(v1,  8)     | vreg(v2, 12)     | vesc_mask(m3, VRET_BYTE, VRET_FW, 32)); }
inline void Assembler::z_vupllb( VectorRegister v1, VectorRegister v2)                                {z_vupll(v1, v2, VRET_BYTE); }       // vector element type 'B'
inline void Assembler::z_vupllh( VectorRegister v1, VectorRegister v2)                                {z_vupll(v1, v2, VRET_HW); }         // vector element type 'H'
inline void Assembler::z_vupllf( VectorRegister v1, VectorRegister v2)                                {z_vupll(v1, v2, VRET_FW); }         // vector element type 'F'

// vector register merge high/low
inline void Assembler::z_vmrh(   VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4) {emit_48(VMRH_ZOPC  | vreg(v1,  8)     | vreg(v2, 12)     | vreg(v3, 16)     | vesc_mask(m4, VRET_BYTE, VRET_DW, 32)); }
inline void Assembler::z_vmrhb(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vmrh(v1, v2, v3, VRET_BYTE); }    // vector element type 'B'
inline void Assembler::z_vmrhh(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vmrh(v1, v2, v3, VRET_HW); }      // vector element type 'H'
inline void Assembler::z_vmrhf(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vmrh(v1, v2, v3, VRET_FW); }      // vector element type 'F'
inline void Assembler::z_vmrhg(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vmrh(v1, v2, v3, VRET_DW); }      // vector element type 'G'

inline void Assembler::z_vmrl(   VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4) {emit_48(VMRL_ZOPC  | vreg(v1,  8)     | vreg(v2, 12)     | vreg(v3, 16)     | vesc_mask(m4, VRET_BYTE, VRET_DW, 32)); }
inline void Assembler::z_vmrlb(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vmrh(v1, v2, v3, VRET_BYTE); }    // vector element type 'B'
inline void Assembler::z_vmrlh(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vmrh(v1, v2, v3, VRET_HW); }      // vector element type 'H'
inline void Assembler::z_vmrlf(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vmrh(v1, v2, v3, VRET_FW); }      // vector element type 'F'
inline void Assembler::z_vmrlg(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vmrh(v1, v2, v3, VRET_DW); }      // vector element type 'G'

// vector register permute
inline void Assembler::z_vperm(  VectorRegister v1, VectorRegister v2, VectorRegister v3, VectorRegister v4) {emit_48(VPERM_ZOPC | vreg(v1,  8) |  vreg(v2, 12) | vreg(v3, 16) | vreg(v4, 32)); }
inline void Assembler::z_vpdi(   VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t        m4) {emit_48(VPDI_ZOPC  | vreg(v1,  8) |  vreg(v2, 12) | vreg(v3, 16) | uimm4(m4, 32, 48)); }

// vector register replicate
inline void Assembler::z_vrep(   VectorRegister v1, VectorRegister v3, int64_t imm2, int64_t m4)      {emit_48(VREP_ZOPC  | vreg(v1,  8)     | vreg(v3, 12)     | simm16(imm2, 16, 48) | vesc_mask(m4, VRET_BYTE, VRET_DW, 32)); }
inline void Assembler::z_vrepb(  VectorRegister v1, VectorRegister v3, int64_t imm2)                  {z_vrep(v1, v3, imm2, VRET_BYTE); }  // vector element type 'B'
inline void Assembler::z_vreph(  VectorRegister v1, VectorRegister v3, int64_t imm2)                  {z_vrep(v1, v3, imm2, VRET_HW); }    // vector element type 'H'
inline void Assembler::z_vrepf(  VectorRegister v1, VectorRegister v3, int64_t imm2)                  {z_vrep(v1, v3, imm2, VRET_FW); }    // vector element type 'F'
inline void Assembler::z_vrepg(  VectorRegister v1, VectorRegister v3, int64_t imm2)                  {z_vrep(v1, v3, imm2, VRET_DW); }    // vector element type 'G'
inline void Assembler::z_vrepi(  VectorRegister v1, int64_t imm2,      int64_t m3)                    {emit_48(VREPI_ZOPC | vreg(v1,  8)                        | simm16(imm2, 16, 48) | vesc_mask(m3, VRET_BYTE, VRET_DW, 32)); }
inline void Assembler::z_vrepib( VectorRegister v1, int64_t imm2)                                     {z_vrepi(v1, imm2, VRET_BYTE); }     // vector element type 'B'
inline void Assembler::z_vrepih( VectorRegister v1, int64_t imm2)                                     {z_vrepi(v1, imm2, VRET_HW); }       // vector element type 'B'
inline void Assembler::z_vrepif( VectorRegister v1, int64_t imm2)                                     {z_vrepi(v1, imm2, VRET_FW); }       // vector element type 'B'
inline void Assembler::z_vrepig( VectorRegister v1, int64_t imm2)                                     {z_vrepi(v1, imm2, VRET_DW); }       // vector element type 'B'

inline void Assembler::z_vsel(   VectorRegister v1, VectorRegister v2, VectorRegister v3, VectorRegister v4) {emit_48(VSEL_ZOPC  | vreg(v1,  8) |  vreg(v2, 12) |  vreg(v3, 16) |  vreg(v4, 32)); }
inline void Assembler::z_vseg(   VectorRegister v1, VectorRegister v2, int64_t m3)                    {emit_48(VSEG_ZOPC  | vreg(v1,  8)     | vreg(v2, 12)     | uimm4(m3, 32, 48)); }

// Load (immediate)
inline void Assembler::z_vleib(  VectorRegister v1, int64_t imm2, int64_t m3)                         {emit_48(VLEIB_ZOPC | vreg(v1,  8)                        | simm16(imm2, 32, 48)  | veix_mask(m3, VRET_BYTE, 32)); }
inline void Assembler::z_vleih(  VectorRegister v1, int64_t imm2, int64_t m3)                         {emit_48(VLEIH_ZOPC | vreg(v1,  8)                        | simm16(imm2, 32, 48)  | veix_mask(m3, VRET_HW,   32)); }
inline void Assembler::z_vleif(  VectorRegister v1, int64_t imm2, int64_t m3)                         {emit_48(VLEIF_ZOPC | vreg(v1,  8)                        | simm16(imm2, 32, 48)  | veix_mask(m3, VRET_FW,   32)); }
inline void Assembler::z_vleig(  VectorRegister v1, int64_t imm2, int64_t m3)                         {emit_48(VLEIG_ZOPC | vreg(v1,  8)                        | simm16(imm2, 32, 48)  | veix_mask(m3, VRET_DW,   32)); }

// Store
inline void Assembler::z_vstm(   VectorRegister v1, VectorRegister v3, int64_t d2, Register b2)       {emit_48(VSTM_ZOPC  | vreg(v1,  8)     | vreg(v3, 12)     | rsmask_48(d2,     b2)); }
inline void Assembler::z_vst(    VectorRegister v1, int64_t d2, Register x2, Register b2)             {emit_48(VST_ZOPC   | vreg(v1,  8)                        | rxmask_48(d2, x2, b2)); }
inline void Assembler::z_vsteb(  VectorRegister v1, int64_t d2, Register x2, Register b2, int64_t m3) {emit_48(VSTEB_ZOPC | vreg(v1,  8)                        | rxmask_48(d2, x2, b2) | veix_mask(m3, VRET_BYTE, 32)); }
inline void Assembler::z_vsteh(  VectorRegister v1, int64_t d2, Register x2, Register b2, int64_t m3) {emit_48(VSTEH_ZOPC | vreg(v1,  8)                        | rxmask_48(d2, x2, b2) | veix_mask(m3, VRET_HW,   32)); }
inline void Assembler::z_vstef(  VectorRegister v1, int64_t d2, Register x2, Register b2, int64_t m3) {emit_48(VSTEF_ZOPC | vreg(v1,  8)                        | rxmask_48(d2, x2, b2) | veix_mask(m3, VRET_FW,   32)); }
inline void Assembler::z_vsteg(  VectorRegister v1, int64_t d2, Register x2, Register b2, int64_t m3) {emit_48(VSTEG_ZOPC | vreg(v1,  8)                        | rxmask_48(d2, x2, b2) | veix_mask(m3, VRET_DW,   32)); }
inline void Assembler::z_vstl(   VectorRegister v1, Register r3, int64_t d2, Register b2)             {emit_48(VSTL_ZOPC  | vreg(v1,  8)     |  reg(r3, 12, 48) | rsmask_48(d2,     b2)); }

// Misc
inline void Assembler::z_vgm(    VectorRegister v1, int64_t imm2, int64_t imm3, int64_t m4)           {emit_48(VGM_ZOPC   | vreg(v1,  8)     | uimm8( imm2, 16, 48) | uimm8(imm3, 24, 48) | vesc_mask(m4, VRET_BYTE, VRET_DW, 32)); }
inline void Assembler::z_vgmb(   VectorRegister v1, int64_t imm2, int64_t imm3)                       {z_vgm(v1, imm2, imm3, VRET_BYTE); } // vector element type 'B'
inline void Assembler::z_vgmh(   VectorRegister v1, int64_t imm2, int64_t imm3)                       {z_vgm(v1, imm2, imm3, VRET_HW); }   // vector element type 'H'
inline void Assembler::z_vgmf(   VectorRegister v1, int64_t imm2, int64_t imm3)                       {z_vgm(v1, imm2, imm3, VRET_FW); }   // vector element type 'F'
inline void Assembler::z_vgmg(   VectorRegister v1, int64_t imm2, int64_t imm3)                       {z_vgm(v1, imm2, imm3, VRET_DW); }   // vector element type 'G'

inline void Assembler::z_vgbm(   VectorRegister v1, int64_t imm2)                                     {emit_48(VGBM_ZOPC  | vreg(v1,  8)     | uimm16(imm2, 16, 48)); }
inline void Assembler::z_vzero(  VectorRegister v1)                                                   {z_vgbm(v1, 0); }      // preferred method to set vreg to all zeroes
inline void Assembler::z_vone(   VectorRegister v1)                                                   {z_vgbm(v1, 0xffff); } // preferred method to set vreg to all ones

//---<  Vector Arithmetic Instructions  >---

// Load
inline void Assembler::z_vlc(    VectorRegister v1, VectorRegister v2, int64_t m3)                    {emit_48(VLC_ZOPC   | vreg(v1,  8) | vreg(v2, 12) | vesc_mask(m3, VRET_BYTE, VRET_DW, 32)); }
inline void Assembler::z_vlcb(   VectorRegister v1, VectorRegister v2)                                {z_vlc(v1, v2, VRET_BYTE); }         // vector element type 'B'
inline void Assembler::z_vlch(   VectorRegister v1, VectorRegister v2)                                {z_vlc(v1, v2, VRET_HW); }           // vector element type 'H'
inline void Assembler::z_vlcf(   VectorRegister v1, VectorRegister v2)                                {z_vlc(v1, v2, VRET_FW); }           // vector element type 'F'
inline void Assembler::z_vlcg(   VectorRegister v1, VectorRegister v2)                                {z_vlc(v1, v2, VRET_DW); }           // vector element type 'G'
inline void Assembler::z_vlp(    VectorRegister v1, VectorRegister v2, int64_t m3)                    {emit_48(VLP_ZOPC   | vreg(v1,  8) | vreg(v2, 12) | vesc_mask(m3, VRET_BYTE, VRET_DW, 32)); }
inline void Assembler::z_vlpb(   VectorRegister v1, VectorRegister v2)                                {z_vlp(v1, v2, VRET_BYTE); }         // vector element type 'B'
inline void Assembler::z_vlph(   VectorRegister v1, VectorRegister v2)                                {z_vlp(v1, v2, VRET_HW); }           // vector element type 'H'
inline void Assembler::z_vlpf(   VectorRegister v1, VectorRegister v2)                                {z_vlp(v1, v2, VRET_FW); }           // vector element type 'F'
inline void Assembler::z_vlpg(   VectorRegister v1, VectorRegister v2)                                {z_vlp(v1, v2, VRET_DW); }           // vector element type 'G'

// ADD
inline void Assembler::z_va(     VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4) {emit_48(VA_ZOPC    | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16) | vesc_mask(m4, VRET_BYTE, VRET_QW, 32)); }
inline void Assembler::z_vab(    VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_va(v1, v2, v3, VRET_BYTE); }      // vector element type 'B'
inline void Assembler::z_vah(    VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_va(v1, v2, v3, VRET_HW); }        // vector element type 'H'
inline void Assembler::z_vaf(    VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_va(v1, v2, v3, VRET_FW); }        // vector element type 'F'
inline void Assembler::z_vag(    VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_va(v1, v2, v3, VRET_DW); }        // vector element type 'G'
inline void Assembler::z_vaq(    VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_va(v1, v2, v3, VRET_QW); }        // vector element type 'Q'
inline void Assembler::z_vacc(   VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4) {emit_48(VACC_ZOPC  | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16) | vesc_mask(m4, VRET_BYTE, VRET_QW, 32)); }
inline void Assembler::z_vaccb(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vacc(v1, v2, v3, VRET_BYTE); }    // vector element type 'B'
inline void Assembler::z_vacch(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vacc(v1, v2, v3, VRET_HW); }      // vector element type 'H'
inline void Assembler::z_vaccf(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vacc(v1, v2, v3, VRET_FW); }      // vector element type 'F'
inline void Assembler::z_vaccg(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vacc(v1, v2, v3, VRET_DW); }      // vector element type 'G'
inline void Assembler::z_vaccq(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vacc(v1, v2, v3, VRET_QW); }      // vector element type 'Q'

// SUB
inline void Assembler::z_vs(     VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4) {emit_48(VS_ZOPC    | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16) | vesc_mask(m4, VRET_BYTE, VRET_QW, 32)); }
inline void Assembler::z_vsb(    VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vs(v1, v2, v3, VRET_BYTE); }      // vector element type 'B'
inline void Assembler::z_vsh(    VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vs(v1, v2, v3, VRET_HW); }        // vector element type 'H'
inline void Assembler::z_vsf(    VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vs(v1, v2, v3, VRET_FW); }        // vector element type 'F'
inline void Assembler::z_vsg(    VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vs(v1, v2, v3, VRET_DW); }        // vector element type 'G'
inline void Assembler::z_vsq(    VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vs(v1, v2, v3, VRET_QW); }        // vector element type 'Q'
inline void Assembler::z_vscbi(  VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4) {emit_48(VSCBI_ZOPC | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16) | vesc_mask(m4, VRET_BYTE, VRET_QW, 32)); }
inline void Assembler::z_vscbib( VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vscbi(v1, v2, v3, VRET_BYTE); }   // vector element type 'B'
inline void Assembler::z_vscbih( VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vscbi(v1, v2, v3, VRET_HW); }     // vector element type 'H'
inline void Assembler::z_vscbif( VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vscbi(v1, v2, v3, VRET_FW); }     // vector element type 'F'
inline void Assembler::z_vscbig( VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vscbi(v1, v2, v3, VRET_DW); }     // vector element type 'G'
inline void Assembler::z_vscbiq( VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vscbi(v1, v2, v3, VRET_QW); }     // vector element type 'Q'

// MULTIPLY
inline void Assembler::z_vml(    VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4) {emit_48(VML_ZOPC   | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16) | vesc_mask(m4, VRET_BYTE, VRET_FW, 32)); }
inline void Assembler::z_vmh(    VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4) {emit_48(VMH_ZOPC   | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16) | vesc_mask(m4, VRET_BYTE, VRET_FW, 32)); }
inline void Assembler::z_vmlh(   VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4) {emit_48(VMLH_ZOPC  | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16) | vesc_mask(m4, VRET_BYTE, VRET_FW, 32)); }
inline void Assembler::z_vme(    VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4) {emit_48(VME_ZOPC   | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16) | vesc_mask(m4, VRET_BYTE, VRET_FW, 32)); }
inline void Assembler::z_vmle(   VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4) {emit_48(VMLE_ZOPC  | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16) | vesc_mask(m4, VRET_BYTE, VRET_FW, 32)); }
inline void Assembler::z_vmo(    VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4) {emit_48(VMO_ZOPC   | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16) | vesc_mask(m4, VRET_BYTE, VRET_FW, 32)); }
inline void Assembler::z_vmlo(   VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4) {emit_48(VMLO_ZOPC  | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16) | vesc_mask(m4, VRET_BYTE, VRET_FW, 32)); }

// MULTIPLY & ADD
inline void Assembler::z_vmal(   VectorRegister v1, VectorRegister v2, VectorRegister v3, VectorRegister v4, int64_t m5) {emit_48(VMAL_ZOPC  | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16) | vreg(v4, 32) | vesc_mask(m5, VRET_BYTE, VRET_FW, 20)); }
inline void Assembler::z_vmah(   VectorRegister v1, VectorRegister v2, VectorRegister v3, VectorRegister v4, int64_t m5) {emit_48(VMAH_ZOPC  | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16) | vreg(v4, 32) | vesc_mask(m5, VRET_BYTE, VRET_FW, 20)); }
inline void Assembler::z_vmalh(  VectorRegister v1, VectorRegister v2, VectorRegister v3, VectorRegister v4, int64_t m5) {emit_48(VMALH_ZOPC | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16) | vreg(v4, 32) | vesc_mask(m5, VRET_BYTE, VRET_FW, 20)); }
inline void Assembler::z_vmae(   VectorRegister v1, VectorRegister v2, VectorRegister v3, VectorRegister v4, int64_t m5) {emit_48(VMAE_ZOPC  | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16) | vreg(v4, 32) | vesc_mask(m5, VRET_BYTE, VRET_FW, 20)); }
inline void Assembler::z_vmale(  VectorRegister v1, VectorRegister v2, VectorRegister v3, VectorRegister v4, int64_t m5) {emit_48(VMALE_ZOPC | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16) | vreg(v4, 32) | vesc_mask(m5, VRET_BYTE, VRET_FW, 20)); }
inline void Assembler::z_vmao(   VectorRegister v1, VectorRegister v2, VectorRegister v3, VectorRegister v4, int64_t m5) {emit_48(VMAO_ZOPC  | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16) | vreg(v4, 32) | vesc_mask(m5, VRET_BYTE, VRET_FW, 20)); }
inline void Assembler::z_vmalo(  VectorRegister v1, VectorRegister v2, VectorRegister v3, VectorRegister v4, int64_t m5) {emit_48(VMALO_ZOPC | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16) | vreg(v4, 32) | vesc_mask(m5, VRET_BYTE, VRET_FW, 20)); }

// VECTOR SUM
inline void Assembler::z_vsum(   VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4) {emit_48(VSUM_ZOPC  | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16) | vesc_mask(m4, VRET_BYTE, VRET_HW, 32)); }
inline void Assembler::z_vsumb(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vsum(v1, v2, v3, VRET_BYTE); }    // vector element type 'B'
inline void Assembler::z_vsumh(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vsum(v1, v2, v3, VRET_HW); }      // vector element type 'H'
inline void Assembler::z_vsumg(  VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4) {emit_48(VSUMG_ZOPC | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16) | vesc_mask(m4, VRET_HW,   VRET_FW, 32)); }
inline void Assembler::z_vsumgh( VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vsumg(v1, v2, v3, VRET_HW); }     // vector element type 'B'
inline void Assembler::z_vsumgf( VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vsumg(v1, v2, v3, VRET_FW); }     // vector element type 'H'
inline void Assembler::z_vsumq(  VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4) {emit_48(VSUMQ_ZOPC | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16) | vesc_mask(m4, VRET_FW,   VRET_DW, 32)); }
inline void Assembler::z_vsumqf( VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vsumq(v1, v2, v3, VRET_FW); }     // vector element type 'B'
inline void Assembler::z_vsumqg( VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vsumq(v1, v2, v3, VRET_DW); }     // vector element type 'H'

// Average
inline void Assembler::z_vavg(   VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4) {emit_48(VAVG_ZOPC  | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16) | vesc_mask(m4, VRET_BYTE, VRET_DW, 32)); }
inline void Assembler::z_vavgb(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vavg(v1, v2, v3, VRET_BYTE); }    // vector element type 'B'
inline void Assembler::z_vavgh(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vavg(v1, v2, v3, VRET_HW); }      // vector element type 'H'
inline void Assembler::z_vavgf(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vavg(v1, v2, v3, VRET_FW); }      // vector element type 'F'
inline void Assembler::z_vavgg(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vavg(v1, v2, v3, VRET_DW); }      // vector element type 'G'
inline void Assembler::z_vavgl(  VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4) {emit_48(VAVGL_ZOPC | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16) | vesc_mask(m4, VRET_BYTE, VRET_DW, 32)); }
inline void Assembler::z_vavglb( VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vavgl(v1, v2, v3, VRET_BYTE); }   // vector element type 'B'
inline void Assembler::z_vavglh( VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vavgl(v1, v2, v3, VRET_HW); }     // vector element type 'H'
inline void Assembler::z_vavglf( VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vavgl(v1, v2, v3, VRET_FW); }     // vector element type 'F'
inline void Assembler::z_vavglg( VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vavgl(v1, v2, v3, VRET_DW); }     // vector element type 'G'

// VECTOR Galois Field Multiply Sum
inline void Assembler::z_vgfm(   VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4) {emit_48(VGFM_ZOPC  | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16) | vesc_mask(m4, VRET_BYTE, VRET_DW, 32)); }
inline void Assembler::z_vgfmb(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vgfm(v1, v2, v3, VRET_BYTE); }    // vector element type 'B'
inline void Assembler::z_vgfmh(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vgfm(v1, v2, v3, VRET_HW); }      // vector element type 'H'
inline void Assembler::z_vgfmf(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vgfm(v1, v2, v3, VRET_FW); }      // vector element type 'F'
inline void Assembler::z_vgfmg(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vgfm(v1, v2, v3, VRET_DW); }      // vector element type 'G'
inline void Assembler::z_vgfma(  VectorRegister v1, VectorRegister v2, VectorRegister v3, VectorRegister v4, int64_t m5) {emit_48(VGFMA_ZOPC | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16) | vreg(v3, 16) | vesc_mask(m5, VRET_BYTE, VRET_DW, 20)); }
inline void Assembler::z_vgfmab( VectorRegister v1, VectorRegister v2, VectorRegister v3, VectorRegister v4) {z_vgfma(v1, v2, v3, v4, VRET_BYTE); } // vector element type 'B'
inline void Assembler::z_vgfmah( VectorRegister v1, VectorRegister v2, VectorRegister v3, VectorRegister v4) {z_vgfma(v1, v2, v3, v4, VRET_HW); }   // vector element type 'H'
inline void Assembler::z_vgfmaf( VectorRegister v1, VectorRegister v2, VectorRegister v3, VectorRegister v4) {z_vgfma(v1, v2, v3, v4, VRET_FW); }   // vector element type 'F'
inline void Assembler::z_vgfmag( VectorRegister v1, VectorRegister v2, VectorRegister v3, VectorRegister v4) {z_vgfma(v1, v2, v3, v4, VRET_DW); }   // vector element type 'G'

//---<  Vector Logical Instructions  >---

// AND
inline void Assembler::z_vn(     VectorRegister v1, VectorRegister v2, VectorRegister v3)             {emit_48(VN_ZOPC    | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16)); }
inline void Assembler::z_vnc(    VectorRegister v1, VectorRegister v2, VectorRegister v3)             {emit_48(VNC_ZOPC   | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16)); }

// XOR
inline void Assembler::z_vx(     VectorRegister v1, VectorRegister v2, VectorRegister v3)             {emit_48(VX_ZOPC    | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16)); }

// NOR
inline void Assembler::z_vno(    VectorRegister v1, VectorRegister v2, VectorRegister v3)             {emit_48(VNO_ZOPC   | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16)); }

// OR
inline void Assembler::z_vo(     VectorRegister v1, VectorRegister v2, VectorRegister v3)             {emit_48(VO_ZOPC    | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16)); }

// Comparison (element-wise)
inline void Assembler::z_vceq(   VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4, int64_t cc5) {emit_48(VCEQ_ZOPC  | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16) | vesc_mask(m4, VRET_BYTE, VRET_DW, 32) | voprc_ccmask(cc5, 24)); }
inline void Assembler::z_vceqb(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vceq(v1, v2, v3, VRET_BYTE, VOPRC_CCIGN); } // vector element type 'B', don't set CC
inline void Assembler::z_vceqh(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vceq(v1, v2, v3, VRET_HW,   VOPRC_CCIGN); } // vector element type 'H', don't set CC
inline void Assembler::z_vceqf(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vceq(v1, v2, v3, VRET_FW,   VOPRC_CCIGN); } // vector element type 'F', don't set CC
inline void Assembler::z_vceqg(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vceq(v1, v2, v3, VRET_DW,   VOPRC_CCIGN); } // vector element type 'G', don't set CC
inline void Assembler::z_vceqbs( VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vceq(v1, v2, v3, VRET_BYTE, VOPRC_CCSET); } // vector element type 'B', don't set CC
inline void Assembler::z_vceqhs( VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vceq(v1, v2, v3, VRET_HW,   VOPRC_CCSET); } // vector element type 'H', don't set CC
inline void Assembler::z_vceqfs( VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vceq(v1, v2, v3, VRET_FW,   VOPRC_CCSET); } // vector element type 'F', don't set CC
inline void Assembler::z_vceqgs( VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vceq(v1, v2, v3, VRET_DW,   VOPRC_CCSET); } // vector element type 'G', don't set CC
inline void Assembler::z_vch(    VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4, int64_t cc5) {emit_48(VCH_ZOPC   | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16) | vesc_mask(m4, VRET_BYTE, VRET_DW, 32) | voprc_ccmask(cc5, 24)); }
inline void Assembler::z_vchb(   VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vch(v1, v2, v3, VRET_BYTE,  VOPRC_CCIGN); }  // vector element type 'B', don't set CC
inline void Assembler::z_vchh(   VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vch(v1, v2, v3, VRET_HW,    VOPRC_CCIGN); }  // vector element type 'H', don't set CC
inline void Assembler::z_vchf(   VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vch(v1, v2, v3, VRET_FW,    VOPRC_CCIGN); }  // vector element type 'F', don't set CC
inline void Assembler::z_vchg(   VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vch(v1, v2, v3, VRET_DW,    VOPRC_CCIGN); }  // vector element type 'G', don't set CC
inline void Assembler::z_vchbs(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vch(v1, v2, v3, VRET_BYTE,  VOPRC_CCSET); }  // vector element type 'B', don't set CC
inline void Assembler::z_vchhs(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vch(v1, v2, v3, VRET_HW,    VOPRC_CCSET); }  // vector element type 'H', don't set CC
inline void Assembler::z_vchfs(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vch(v1, v2, v3, VRET_FW,    VOPRC_CCSET); }  // vector element type 'F', don't set CC
inline void Assembler::z_vchgs(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vch(v1, v2, v3, VRET_DW,    VOPRC_CCSET); }  // vector element type 'G', don't set CC
inline void Assembler::z_vchl(   VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4, int64_t cc5) {emit_48(VCHL_ZOPC  | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16) | vesc_mask(m4, VRET_BYTE, VRET_DW, 32) | voprc_ccmask(cc5, 24)); }
inline void Assembler::z_vchlb(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vchl(v1, v2, v3, VRET_BYTE, VOPRC_CCIGN); }  // vector element type 'B', don't set CC
inline void Assembler::z_vchlh(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vchl(v1, v2, v3, VRET_HW,   VOPRC_CCIGN); }  // vector element type 'H', don't set CC
inline void Assembler::z_vchlf(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vchl(v1, v2, v3, VRET_FW,   VOPRC_CCIGN); }  // vector element type 'F', don't set CC
inline void Assembler::z_vchlg(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vchl(v1, v2, v3, VRET_DW,   VOPRC_CCIGN); }  // vector element type 'G', don't set CC
inline void Assembler::z_vchlbs( VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vchl(v1, v2, v3, VRET_BYTE, VOPRC_CCSET); }  // vector element type 'B', don't set CC
inline void Assembler::z_vchlhs( VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vchl(v1, v2, v3, VRET_HW,   VOPRC_CCSET); }  // vector element type 'H', don't set CC
inline void Assembler::z_vchlfs( VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vchl(v1, v2, v3, VRET_FW,   VOPRC_CCSET); }  // vector element type 'F', don't set CC
inline void Assembler::z_vchlgs( VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vchl(v1, v2, v3, VRET_DW,   VOPRC_CCSET); }  // vector element type 'G', don't set CC

// Max/Min (element-wise)
inline void Assembler::z_vmx(    VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4) {emit_48(VMX_ZOPC   | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16) | vesc_mask(m4, VRET_BYTE, VRET_DW, 32)); }
inline void Assembler::z_vmxb(   VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vmx(v1, v2, v3, VRET_BYTE); }     // vector element type 'B'
inline void Assembler::z_vmxh(   VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vmx(v1, v2, v3, VRET_HW); }       // vector element type 'H'
inline void Assembler::z_vmxf(   VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vmx(v1, v2, v3, VRET_FW); }       // vector element type 'F'
inline void Assembler::z_vmxg(   VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vmx(v1, v2, v3, VRET_DW); }       // vector element type 'G'
inline void Assembler::z_vmxl(   VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4) {emit_48(VMXL_ZOPC  | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16) | vesc_mask(m4, VRET_BYTE, VRET_DW, 32)); }
inline void Assembler::z_vmxlb(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vmxl(v1, v2, v3, VRET_BYTE); }    // vector element type 'B'
inline void Assembler::z_vmxlh(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vmxl(v1, v2, v3, VRET_HW); }      // vector element type 'H'
inline void Assembler::z_vmxlf(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vmxl(v1, v2, v3, VRET_FW); }      // vector element type 'F'
inline void Assembler::z_vmxlg(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vmxl(v1, v2, v3, VRET_DW); }      // vector element type 'G'
inline void Assembler::z_vmn(    VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4) {emit_48(VMN_ZOPC   | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16) | vesc_mask(m4, VRET_BYTE, VRET_DW, 32)); }
inline void Assembler::z_vmnb(   VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vmn(v1, v2, v3, VRET_BYTE); }     // vector element type 'B'
inline void Assembler::z_vmnh(   VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vmn(v1, v2, v3, VRET_HW); }       // vector element type 'H'
inline void Assembler::z_vmnf(   VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vmn(v1, v2, v3, VRET_FW); }       // vector element type 'F'
inline void Assembler::z_vmng(   VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vmn(v1, v2, v3, VRET_DW); }       // vector element type 'G'
inline void Assembler::z_vmnl(   VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t m4) {emit_48(VMNL_ZOPC  | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16) | vesc_mask(m4, VRET_BYTE, VRET_DW, 32)); }
inline void Assembler::z_vmnlb(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vmnl(v1, v2, v3, VRET_BYTE); }    // vector element type 'B'
inline void Assembler::z_vmnlh(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vmnl(v1, v2, v3, VRET_HW); }      // vector element type 'H'
inline void Assembler::z_vmnlf(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vmnl(v1, v2, v3, VRET_FW); }      // vector element type 'F'
inline void Assembler::z_vmnlg(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vmnl(v1, v2, v3, VRET_DW); }      // vector element type 'G'

// Leading/Trailing Zeros, population count
inline void Assembler::z_vclz(   VectorRegister v1, VectorRegister v2, int64_t m3)                    {emit_48(VCLZ_ZOPC  | vreg(v1,  8) | vreg(v2, 12) | vesc_mask(m3, VRET_BYTE, VRET_DW, 32)); }
inline void Assembler::z_vclzb(  VectorRegister v1, VectorRegister v2)                                {z_vclz(v1, v2, VRET_BYTE); }        // vector element type 'B'
inline void Assembler::z_vclzh(  VectorRegister v1, VectorRegister v2)                                {z_vclz(v1, v2, VRET_HW); }          // vector element type 'H'
inline void Assembler::z_vclzf(  VectorRegister v1, VectorRegister v2)                                {z_vclz(v1, v2, VRET_FW); }          // vector element type 'F'
inline void Assembler::z_vclzg(  VectorRegister v1, VectorRegister v2)                                {z_vclz(v1, v2, VRET_DW); }          // vector element type 'G'
inline void Assembler::z_vctz(   VectorRegister v1, VectorRegister v2, int64_t m3)                    {emit_48(VCTZ_ZOPC  | vreg(v1,  8) | vreg(v2, 12) | vesc_mask(m3, VRET_BYTE, VRET_DW, 32)); }
inline void Assembler::z_vctzb(  VectorRegister v1, VectorRegister v2)                                {z_vctz(v1, v2, VRET_BYTE); }        // vector element type 'B'
inline void Assembler::z_vctzh(  VectorRegister v1, VectorRegister v2)                                {z_vctz(v1, v2, VRET_HW); }          // vector element type 'H'
inline void Assembler::z_vctzf(  VectorRegister v1, VectorRegister v2)                                {z_vctz(v1, v2, VRET_FW); }          // vector element type 'F'
inline void Assembler::z_vctzg(  VectorRegister v1, VectorRegister v2)                                {z_vctz(v1, v2, VRET_DW); }          // vector element type 'G'
inline void Assembler::z_vpopct( VectorRegister v1, VectorRegister v2, int64_t m3)                    {emit_48(VPOPCT_ZOPC| vreg(v1,  8) | vreg(v2, 12) | vesc_mask(m3, VRET_BYTE, VRET_DW, 32)); }

// Rotate/Shift
inline void Assembler::z_verllv( VectorRegister v1, VectorRegister v2, VectorRegister v3,               int64_t m4) {emit_48(VERLLV_ZOPC| vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16)      | vesc_mask(m4, VRET_BYTE, VRET_DW, 32)); }
inline void Assembler::z_verllvb(VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_verllv(v1, v2, v3, VRET_BYTE); }  // vector element type 'B'
inline void Assembler::z_verllvh(VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_verllv(v1, v2, v3, VRET_HW); }    // vector element type 'H'
inline void Assembler::z_verllvf(VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_verllv(v1, v2, v3, VRET_FW); }    // vector element type 'F'
inline void Assembler::z_verllvg(VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_verllv(v1, v2, v3, VRET_DW); }    // vector element type 'G'
inline void Assembler::z_verll(  VectorRegister v1, VectorRegister v3, int64_t d2, Register b2,         int64_t m4) {emit_48(VERLL_ZOPC | vreg(v1,  8) | vreg(v3, 12) | rsmask_48(d2, b2) | vesc_mask(m4, VRET_BYTE, VRET_DW, 32)); }
inline void Assembler::z_verllb( VectorRegister v1, VectorRegister v3, int64_t d2, Register b2)       {z_verll(v1, v3, d2, b2, VRET_BYTE);}// vector element type 'B'
inline void Assembler::z_verllh( VectorRegister v1, VectorRegister v3, int64_t d2, Register b2)       {z_verll(v1, v3, d2, b2, VRET_HW);}  // vector element type 'H'
inline void Assembler::z_verllf( VectorRegister v1, VectorRegister v3, int64_t d2, Register b2)       {z_verll(v1, v3, d2, b2, VRET_FW);}  // vector element type 'F'
inline void Assembler::z_verllg( VectorRegister v1, VectorRegister v3, int64_t d2, Register b2)       {z_verll(v1, v3, d2, b2, VRET_DW);}  // vector element type 'G'
inline void Assembler::z_verim(  VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t imm4, int64_t m5) {emit_48(VERLL_ZOPC | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16)      | uimm8(imm4, 24, 48) | vesc_mask(m5, VRET_BYTE, VRET_DW, 32)); }
inline void Assembler::z_verimb( VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t imm4) {z_verim(v1, v2, v3, imm4, VRET_BYTE); }   // vector element type 'B'
inline void Assembler::z_verimh( VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t imm4) {z_verim(v1, v2, v3, imm4, VRET_HW); }     // vector element type 'H'
inline void Assembler::z_verimf( VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t imm4) {z_verim(v1, v2, v3, imm4, VRET_FW); }     // vector element type 'F'
inline void Assembler::z_verimg( VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t imm4) {z_verim(v1, v2, v3, imm4, VRET_DW); }     // vector element type 'G'

inline void Assembler::z_veslv(  VectorRegister v1, VectorRegister v2, VectorRegister v3,               int64_t m4) {emit_48(VESLV_ZOPC | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16)      | vesc_mask(m4, VRET_BYTE, VRET_DW, 32)); }
inline void Assembler::z_veslvb( VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_veslv(v1, v2, v3, VRET_BYTE); }   // vector element type 'B'
inline void Assembler::z_veslvh( VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_veslv(v1, v2, v3, VRET_HW); }     // vector element type 'H'
inline void Assembler::z_veslvf( VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_veslv(v1, v2, v3, VRET_FW); }     // vector element type 'F'
inline void Assembler::z_veslvg( VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_veslv(v1, v2, v3, VRET_DW); }     // vector element type 'G'
inline void Assembler::z_vesl(   VectorRegister v1, VectorRegister v3, int64_t d2, Register b2,         int64_t m4) {emit_48(VESL_ZOPC  | vreg(v1,  8) | vreg(v3, 12) | rsmask_48(d2, b2) | vesc_mask(m4, VRET_BYTE, VRET_DW, 32)); }
inline void Assembler::z_veslb(  VectorRegister v1, VectorRegister v3, int64_t d2, Register b2)       {z_vesl(v1, v3, d2, b2, VRET_BYTE);} // vector element type 'B'
inline void Assembler::z_veslh(  VectorRegister v1, VectorRegister v3, int64_t d2, Register b2)       {z_vesl(v1, v3, d2, b2, VRET_HW);}   // vector element type 'H'
inline void Assembler::z_veslf(  VectorRegister v1, VectorRegister v3, int64_t d2, Register b2)       {z_vesl(v1, v3, d2, b2, VRET_FW);}   // vector element type 'F'
inline void Assembler::z_veslg(  VectorRegister v1, VectorRegister v3, int64_t d2, Register b2)       {z_vesl(v1, v3, d2, b2, VRET_DW);}   // vector element type 'G'

inline void Assembler::z_vesrav( VectorRegister v1, VectorRegister v2, VectorRegister v3,               int64_t m4) {emit_48(VESRAV_ZOPC| vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16)      | vesc_mask(m4, VRET_BYTE, VRET_DW, 32)); }
inline void Assembler::z_vesravb(VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vesrav(v1, v2, v3, VRET_BYTE); }  // vector element type 'B'
inline void Assembler::z_vesravh(VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vesrav(v1, v2, v3, VRET_HW); }    // vector element type 'H'
inline void Assembler::z_vesravf(VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vesrav(v1, v2, v3, VRET_FW); }    // vector element type 'F'
inline void Assembler::z_vesravg(VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vesrav(v1, v2, v3, VRET_DW); }    // vector element type 'G'
inline void Assembler::z_vesra(  VectorRegister v1, VectorRegister v3, int64_t d2, Register b2,         int64_t m4) {emit_48(VESRA_ZOPC | vreg(v1,  8) | vreg(v3, 12) | rsmask_48(d2, b2) | vesc_mask(m4, VRET_BYTE, VRET_DW, 32)); }
inline void Assembler::z_vesrab( VectorRegister v1, VectorRegister v3, int64_t d2, Register b2)       {z_vesra(v1, v3, d2, b2, VRET_BYTE);}// vector element type 'B'
inline void Assembler::z_vesrah( VectorRegister v1, VectorRegister v3, int64_t d2, Register b2)       {z_vesra(v1, v3, d2, b2, VRET_HW);}  // vector element type 'H'
inline void Assembler::z_vesraf( VectorRegister v1, VectorRegister v3, int64_t d2, Register b2)       {z_vesra(v1, v3, d2, b2, VRET_FW);}  // vector element type 'F'
inline void Assembler::z_vesrag( VectorRegister v1, VectorRegister v3, int64_t d2, Register b2)       {z_vesra(v1, v3, d2, b2, VRET_DW);}  // vector element type 'G'
inline void Assembler::z_vesrlv( VectorRegister v1, VectorRegister v2, VectorRegister v3,               int64_t m4) {emit_48(VESRLV_ZOPC| vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16)      | vesc_mask(m4, VRET_BYTE, VRET_DW, 32)); }
inline void Assembler::z_vesrlvb(VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vesrlv(v1, v2, v3, VRET_BYTE); }  // vector element type 'B'
inline void Assembler::z_vesrlvh(VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vesrlv(v1, v2, v3, VRET_HW); }    // vector element type 'H'
inline void Assembler::z_vesrlvf(VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vesrlv(v1, v2, v3, VRET_FW); }    // vector element type 'F'
inline void Assembler::z_vesrlvg(VectorRegister v1, VectorRegister v2, VectorRegister v3)             {z_vesrlv(v1, v2, v3, VRET_DW); }    // vector element type 'G'
inline void Assembler::z_vesrl(  VectorRegister v1, VectorRegister v3, int64_t d2, Register b2,         int64_t m4) {emit_48(VESRL_ZOPC | vreg(v1,  8) | vreg(v3, 12) | rsmask_48(d2, b2) | vesc_mask(m4, VRET_BYTE, VRET_DW, 32)); }
inline void Assembler::z_vesrlb( VectorRegister v1, VectorRegister v3, int64_t d2, Register b2)       {z_vesrl(v1, v3, d2, b2, VRET_BYTE);}// vector element type 'B'
inline void Assembler::z_vesrlh( VectorRegister v1, VectorRegister v3, int64_t d2, Register b2)       {z_vesrl(v1, v3, d2, b2, VRET_HW);}  // vector element type 'H'
inline void Assembler::z_vesrlf( VectorRegister v1, VectorRegister v3, int64_t d2, Register b2)       {z_vesrl(v1, v3, d2, b2, VRET_FW);}  // vector element type 'F'
inline void Assembler::z_vesrlg( VectorRegister v1, VectorRegister v3, int64_t d2, Register b2)       {z_vesrl(v1, v3, d2, b2, VRET_DW);}  // vector element type 'G'

inline void Assembler::z_vsl(    VectorRegister v1, VectorRegister v2, VectorRegister v3)               {emit_48(VSL_ZOPC   | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16)); }
inline void Assembler::z_vslb(   VectorRegister v1, VectorRegister v2, VectorRegister v3)               {emit_48(VSLB_ZOPC  | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16)); }
inline void Assembler::z_vsldb(  VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t imm4) {emit_48(VSLDB_ZOPC | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16) | uimm8(imm4, 24, 48)); }

inline void Assembler::z_vsra(   VectorRegister v1, VectorRegister v2, VectorRegister v3)             {emit_48(VSRA_ZOPC  | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16)); }
inline void Assembler::z_vsrab(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {emit_48(VSRAB_ZOPC | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16)); }
inline void Assembler::z_vsrl(   VectorRegister v1, VectorRegister v2, VectorRegister v3)             {emit_48(VSRL_ZOPC  | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16)); }
inline void Assembler::z_vsrlb(  VectorRegister v1, VectorRegister v2, VectorRegister v3)             {emit_48(VSRLB_ZOPC | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16)); }

// Test under Mask
inline void Assembler::z_vtm(    VectorRegister v1, VectorRegister v2)                                {emit_48(VTM_ZOPC   | vreg(v1,  8) | vreg(v2, 12)); }

//---<  Vector String Instructions  >---
inline void Assembler::z_vfae(   VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t imm4, int64_t cc5) {emit_48(VFAE_ZOPC  | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16)      | vesc_mask(imm4, VRET_BYTE, VRET_FW, 32) | voprc_any(cc5, 24) ); }  // Find any element
inline void Assembler::z_vfaeb(  VectorRegister v1, VectorRegister v2, VectorRegister v3,               int64_t cc5) {z_vfae(v1, v2, v3, VRET_BYTE, cc5); }
inline void Assembler::z_vfaeh(  VectorRegister v1, VectorRegister v2, VectorRegister v3,               int64_t cc5) {z_vfae(v1, v2, v3, VRET_HW,   cc5); }
inline void Assembler::z_vfaef(  VectorRegister v1, VectorRegister v2, VectorRegister v3,               int64_t cc5) {z_vfae(v1, v2, v3, VRET_FW,   cc5); }
inline void Assembler::z_vfee(   VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t imm4, int64_t cc5) {emit_48(VFEE_ZOPC  | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16)      | vesc_mask(imm4, VRET_BYTE, VRET_FW, 32) | voprc_any(cc5, 24) ); }  // Find element equal
inline void Assembler::z_vfeeb(  VectorRegister v1, VectorRegister v2, VectorRegister v3,               int64_t cc5) {z_vfee(v1, v2, v3, VRET_BYTE, cc5); }
inline void Assembler::z_vfeeh(  VectorRegister v1, VectorRegister v2, VectorRegister v3,               int64_t cc5) {z_vfee(v1, v2, v3, VRET_HW,   cc5); }
inline void Assembler::z_vfeef(  VectorRegister v1, VectorRegister v2, VectorRegister v3,               int64_t cc5) {z_vfee(v1, v2, v3, VRET_FW,   cc5); }
inline void Assembler::z_vfene(  VectorRegister v1, VectorRegister v2, VectorRegister v3, int64_t imm4, int64_t cc5) {emit_48(VFENE_ZOPC | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16)      | vesc_mask(imm4, VRET_BYTE, VRET_FW, 32) | voprc_any(cc5, 24) ); }  // Find element not equal
inline void Assembler::z_vfeneb( VectorRegister v1, VectorRegister v2, VectorRegister v3,               int64_t cc5) {z_vfene(v1, v2, v3, VRET_BYTE, cc5); }
inline void Assembler::z_vfeneh( VectorRegister v1, VectorRegister v2, VectorRegister v3,               int64_t cc5) {z_vfene(v1, v2, v3, VRET_HW,   cc5); }
inline void Assembler::z_vfenef( VectorRegister v1, VectorRegister v2, VectorRegister v3,               int64_t cc5) {z_vfene(v1, v2, v3, VRET_FW,   cc5); }
inline void Assembler::z_vstrc(  VectorRegister v1, VectorRegister v2, VectorRegister v3, VectorRegister v4, int64_t imm5, int64_t cc6) {emit_48(VSTRC_ZOPC | vreg(v1,  8) | vreg(v2, 12) | vreg(v3, 16) | vreg(v4, 32)     | vesc_mask(imm5, VRET_BYTE, VRET_FW, 20) | voprc_any(cc6, 24) ); }  // String range compare
inline void Assembler::z_vstrcb( VectorRegister v1, VectorRegister v2, VectorRegister v3, VectorRegister v4,               int64_t cc6) {z_vstrc(v1, v2, v3, v4, VRET_BYTE, cc6); }
inline void Assembler::z_vstrch( VectorRegister v1, VectorRegister v2, VectorRegister v3, VectorRegister v4,               int64_t cc6) {z_vstrc(v1, v2, v3, v4, VRET_HW,   cc6); }
inline void Assembler::z_vstrcf( VectorRegister v1, VectorRegister v2, VectorRegister v3, VectorRegister v4,               int64_t cc6) {z_vstrc(v1, v2, v3, v4, VRET_FW,   cc6); }
inline void Assembler::z_vistr(  VectorRegister v1, VectorRegister v2, int64_t imm3, int64_t cc5) {emit_48(VISTR_ZOPC  | vreg(v1,  8) | vreg(v2, 12) | vesc_mask(imm3, VRET_BYTE, VRET_FW, 32) | voprc_any(cc5, 24) ); }  // isolate string
inline void Assembler::z_vistrb( VectorRegister v1, VectorRegister v2,               int64_t cc5) {z_vistr(v1, v2, VRET_BYTE, cc5); }
inline void Assembler::z_vistrh( VectorRegister v1, VectorRegister v2,               int64_t cc5) {z_vistr(v1, v2, VRET_HW,   cc5); }
inline void Assembler::z_vistrf( VectorRegister v1, VectorRegister v2,               int64_t cc5) {z_vistr(v1, v2, VRET_FW,   cc5); }
inline void Assembler::z_vistrbs(VectorRegister v1, VectorRegister v2)                            {z_vistr(v1, v2, VRET_BYTE, VOPRC_CCSET); }
inline void Assembler::z_vistrhs(VectorRegister v1, VectorRegister v2)                            {z_vistr(v1, v2, VRET_HW,   VOPRC_CCSET); }
inline void Assembler::z_vistrfs(VectorRegister v1, VectorRegister v2)                            {z_vistr(v1, v2, VRET_FW,   VOPRC_CCSET); }


//-------------------------------
// FLOAT INSTRUCTIONS
//-------------------------------

//----------------
// LOAD
//----------------
inline void Assembler::z_ler(  FloatRegister r1, FloatRegister r2) { emit_16( LER_ZOPC   | fregt(r1,8,16)    | freg(r2,12,16));   }
inline void Assembler::z_ldr(  FloatRegister r1, FloatRegister r2) { emit_16( LDR_ZOPC   | fregt(r1,8,16)    | freg(r2,12,16));   }
inline void Assembler::z_ldebr(FloatRegister r1, FloatRegister r2) { emit_32( LDEBR_ZOPC | fregt(r1, 24, 32) | freg(r2, 28, 32)); }
inline void Assembler::z_ledbr(FloatRegister r1, FloatRegister r2) { emit_32( LEDBR_ZOPC | fregt(r1, 24, 32) | freg(r2, 28, 32)); }
inline void Assembler::z_le( FloatRegister r1, int64_t d2, Register x2, Register b2) { emit_32( LE_ZOPC  | fregt(r1, 8, 32) | uimm12(d2, 20, 32) | reg(x2, 12, 32) | regz(b2, 16, 32)); }
inline void Assembler::z_ley(FloatRegister r1, int64_t d2, Register x2, Register b2) { emit_48( LEY_ZOPC | fregt(r1, 8, 48) | simm20(d2)         | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_ld( FloatRegister r1, int64_t d2, Register x2, Register b2) { emit_32( LD_ZOPC  | fregt(r1, 8, 32) | uimm12(d2, 20, 32) | reg(x2, 12, 32) | regz(b2, 16, 32)); }
inline void Assembler::z_ldy(FloatRegister r1, int64_t d2, Register x2, Register b2) { emit_48( LDY_ZOPC | fregt(r1, 8, 48) | simm20(d2)         | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_le( FloatRegister r1, const Address &a) { z_le( r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_ley(FloatRegister r1, const Address &a) { z_ley(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_ld( FloatRegister r1, const Address &a) { z_ld( r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_ldy(FloatRegister r1, const Address &a) { z_ldy(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }

inline void Assembler::z_lzdr(FloatRegister r1) { emit_32( LZDR_ZOPC | fregt(r1, 24, 32)); }
inline void Assembler::z_lzer(FloatRegister f1) { emit_32( LZER_ZOPC | fregt(f1, 24, 32)); }


//-----------------
// STORE
//-----------------
inline void Assembler::z_ste( FloatRegister r1, int64_t d2, Register x2, Register b2) { emit_32( STE_ZOPC  | freg(r1, 8, 32) | uimm12(d2, 20, 32) | reg(x2, 12, 32) | regz(b2, 16, 32)); }
inline void Assembler::z_stey(FloatRegister r1, int64_t d2, Register x2, Register b2) { emit_48( STEY_ZOPC | freg(r1, 8, 48) | simm20(d2)         | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_std( FloatRegister r1, int64_t d2, Register x2, Register b2) { emit_32( STD_ZOPC  | freg(r1, 8, 32) | uimm12(d2, 20, 32) | reg(x2, 12, 32) | regz(b2, 16, 32)); }
inline void Assembler::z_stdy(FloatRegister r1, int64_t d2, Register x2, Register b2) { emit_48( STDY_ZOPC | freg(r1, 8, 48) | simm20(d2)         | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_ste( FloatRegister r1, const Address &a) { z_ste( r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_stey(FloatRegister r1, const Address &a) { z_stey(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_std( FloatRegister r1, const Address &a) { z_std( r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_stdy(FloatRegister r1, const Address &a) { z_stdy(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }


//---------------
// ADD
//---------------
inline void Assembler::z_aebr( FloatRegister f1, FloatRegister f2)                  { emit_32( AEBR_ZOPC | fregt( f1, 24, 32) | freg( f2, 28, 32));}
inline void Assembler::z_adbr( FloatRegister f1, FloatRegister f2)                  { emit_32( ADBR_ZOPC | fregt( f1, 24, 32) | freg( f2, 28, 32));}
inline void Assembler::z_aeb(  FloatRegister f1, int64_t d2, Register x2, Register b2 ) { emit_48( AEB_ZOPC | fregt( f1, 8, 48) | uimm12( d2, 20, 48) | reg( x2, 12, 48) | regz( b2, 16, 48));}
inline void Assembler::z_adb(  FloatRegister f1, int64_t d2, Register x2, Register b2 ) { emit_48( ADB_ZOPC | fregt( f1, 8, 48) | uimm12( d2, 20, 48) | reg( x2, 12, 48) | regz( b2, 16, 48));}
inline void Assembler::z_aeb(  FloatRegister r1, const Address& a)                   { z_aeb(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_adb(  FloatRegister r1, const Address& a)                   { z_adb(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }


//---------------
// SUB
//---------------
inline void Assembler::z_sebr( FloatRegister f1, FloatRegister f2)                  { emit_32( SEBR_ZOPC | fregt( f1, 24, 32) | freg( f2, 28, 32));}
inline void Assembler::z_sdbr( FloatRegister f1, FloatRegister f2)                  { emit_32( SDBR_ZOPC | fregt( f1, 24, 32) | freg( f2, 28, 32));}
inline void Assembler::z_seb(  FloatRegister f1, int64_t d2, Register x2, Register b2 ) { emit_48( SEB_ZOPC | fregt( f1, 8, 48) | uimm12( d2, 20, 48) | reg( x2, 12, 48) | regz( b2, 16, 48));}
inline void Assembler::z_sdb(  FloatRegister f1, int64_t d2, Register x2, Register b2 ) { emit_48( SDB_ZOPC | fregt( f1, 8, 48) | uimm12( d2, 20, 48) | reg( x2, 12, 48) | regz( b2, 16, 48));}
inline void Assembler::z_seb(  FloatRegister r1, const Address& a)                   { z_seb(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_sdb(  FloatRegister r1, const Address& a)                   { z_sdb(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }

inline void Assembler::z_lcebr(FloatRegister r1, FloatRegister r2)                   { emit_32( LCEBR_ZOPC | fregt(r1, 24, 32) | freg(r2, 28, 32)); }
inline void Assembler::z_lcdbr(FloatRegister r1, FloatRegister r2)                   { emit_32( LCDBR_ZOPC | fregt(r1, 24, 32) | freg(r2, 28, 32)); }

inline void Assembler::z_lpdbr( FloatRegister fr1, FloatRegister fr2) { emit_32( LPDBR_ZOPC | fregt( fr1, 24,32) | freg((fr2 == fnoreg) ? fr1:fr2, 28, 32)); }


//---------------
// MUL
//---------------
inline void Assembler::z_meebr(FloatRegister f1, FloatRegister f2)                  { emit_32( MEEBR_ZOPC | fregt( f1, 24, 32) | freg( f2, 28, 32));}
inline void Assembler::z_mdbr( FloatRegister f1, FloatRegister f2)                  { emit_32( MDBR_ZOPC  | fregt( f1, 24, 32) | freg( f2, 28, 32));}
inline void Assembler::z_meeb( FloatRegister f1, int64_t d2, Register x2, Register b2 ) { emit_48( MEEB_ZOPC | fregt( f1, 8, 48) | uimm12( d2, 20, 48) | reg( x2, 12, 48) | regz( b2, 16, 48));}
inline void Assembler::z_mdb(  FloatRegister f1, int64_t d2, Register x2, Register b2 ) { emit_48( MDB_ZOPC  | fregt( f1, 8, 48) | uimm12( d2, 20, 48) | reg( x2, 12, 48) | regz( b2, 16, 48));}
inline void Assembler::z_meeb( FloatRegister r1, const Address& a)                   { z_meeb( r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_mdb(  FloatRegister r1, const Address& a)                   { z_mdb(  r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }


//---------------
// MUL-ADD
//---------------
inline void Assembler::z_maebr(FloatRegister f1, FloatRegister f3, FloatRegister f2) { emit_32( MAEBR_ZOPC | fregt(f1, 16, 32) | freg(f3, 24, 32) | freg(f2, 28, 32) );}
inline void Assembler::z_madbr(FloatRegister f1, FloatRegister f3, FloatRegister f2) { emit_32( MADBR_ZOPC | fregt(f1, 16, 32) | freg(f3, 24, 32) | freg(f2, 28, 32) );}
inline void Assembler::z_msebr(FloatRegister f1, FloatRegister f3, FloatRegister f2) { emit_32( MSEBR_ZOPC | fregt(f1, 16, 32) | freg(f3, 24, 32) | freg(f2, 28, 32) );}
inline void Assembler::z_msdbr(FloatRegister f1, FloatRegister f3, FloatRegister f2) { emit_32( MSDBR_ZOPC | fregt(f1, 16, 32) | freg(f3, 24, 32) | freg(f2, 28, 32) );}
inline void Assembler::z_maeb(FloatRegister f1, FloatRegister f3, int64_t d2, Register x2, Register b2) { emit_48( MAEB_ZOPC | fregt(f1, 32, 48) | freg(f3, 8, 48) | uimm12(d2, 20, 48) | reg(x2, 12, 48) | regz(b2, 16, 48) );}
inline void Assembler::z_madb(FloatRegister f1, FloatRegister f3, int64_t d2, Register x2, Register b2) { emit_48( MADB_ZOPC | fregt(f1, 32, 48) | freg(f3, 8, 48) | uimm12(d2, 20, 48) | reg(x2, 12, 48) | regz(b2, 16, 48) );}
inline void Assembler::z_mseb(FloatRegister f1, FloatRegister f3, int64_t d2, Register x2, Register b2) { emit_48( MSEB_ZOPC | fregt(f1, 32, 48) | freg(f3, 8, 48) | uimm12(d2, 20, 48) | reg(x2, 12, 48) | regz(b2, 16, 48) );}
inline void Assembler::z_msdb(FloatRegister f1, FloatRegister f3, int64_t d2, Register x2, Register b2) { emit_48( MSDB_ZOPC | fregt(f1, 32, 48) | freg(f3, 8, 48) | uimm12(d2, 20, 48) | reg(x2, 12, 48) | regz(b2, 16, 48) );}
inline void Assembler::z_maeb(FloatRegister f1, FloatRegister f3, const Address& a) { z_maeb(f1, f3, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_madb(FloatRegister f1, FloatRegister f3, const Address& a) { z_madb(f1, f3, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_mseb(FloatRegister f1, FloatRegister f3, const Address& a) { z_mseb(f1, f3, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_msdb(FloatRegister f1, FloatRegister f3, const Address& a) { z_msdb(f1, f3, a.disp(), a.indexOrR0(), a.baseOrR0()); }


//---------------
// DIV
//---------------
inline void Assembler::z_debr( FloatRegister f1, FloatRegister f2)                      { emit_32( DEBR_ZOPC | fregt( f1, 24, 32) | freg( f2, 28, 32));}
inline void Assembler::z_ddbr( FloatRegister f1, FloatRegister f2)                      { emit_32( DDBR_ZOPC | fregt( f1, 24, 32) | freg( f2, 28, 32));}
inline void Assembler::z_deb(  FloatRegister f1, int64_t d2, Register x2, Register b2 ) { emit_48( DEB_ZOPC  | fregt( f1, 8, 48)  | uimm12( d2, 20, 48) | reg( x2, 12, 48) | regz( b2, 16, 48));}
inline void Assembler::z_ddb(  FloatRegister f1, int64_t d2, Register x2, Register b2 ) { emit_48( DDB_ZOPC  | fregt( f1, 8, 48)  | uimm12( d2, 20, 48) | reg( x2, 12, 48) | regz( b2, 16, 48));}
inline void Assembler::z_deb(  FloatRegister r1, const Address& a)                      { z_deb(  r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_ddb(  FloatRegister r1, const Address& a)                      { z_ddb(  r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }


//---------------
// square root
//---------------
inline void Assembler::z_sqdbr(FloatRegister f1, FloatRegister f2)                       { emit_32(SQDBR_ZOPC | fregt(f1, 24, 32)  | freg(f2, 28, 32)); }
inline void Assembler::z_sqdb( FloatRegister fr1, int64_t d2, Register x2, Register b2 ) { emit_48( SQDB_ZOPC | fregt( fr1, 8, 48) | uimm12( d2, 20, 48) | reg( x2, 12, 48) | regz( b2, 16, 48));}
inline void Assembler::z_sqdb( FloatRegister fr1, int64_t d2, Register b2)               { z_sqdb( fr1, d2, Z_R0, b2);}


//---------------
// CMP
//---------------
inline void Assembler::z_cebr(FloatRegister r1, FloatRegister r2)                    { emit_32( CEBR_ZOPC | fregt(r1, 24, 32) | freg(r2, 28, 32)); }
inline void Assembler::z_ceb(FloatRegister r1, int64_t d2, Register x2, Register b2) { emit_48( CEB_ZOPC  | fregt(r1, 8, 48) | uimm12(d2, 20, 48) | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_ceb(FloatRegister r1, const Address &a)                     { z_ceb(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }
inline void Assembler::z_cdbr(FloatRegister r1, FloatRegister r2)                    { emit_32( CDBR_ZOPC | fregt(r1, 24, 32) | freg(r2, 28, 32)); }
inline void Assembler::z_cdb(FloatRegister r1, int64_t d2, Register x2, Register b2) { emit_48( CDB_ZOPC  | fregt(r1, 8, 48) | uimm12(d2, 20, 48) | reg(x2, 12, 48) | regz(b2, 16, 48)); }
inline void Assembler::z_cdb(FloatRegister r1, const Address &a)                     { z_cdb(r1, a.disp(), a.indexOrR0(), a.baseOrR0()); }


//------------------------------------
// FLOAT <-> INT conversion
//------------------------------------
inline void Assembler::z_ldgr(FloatRegister r1, Register r2)                  { emit_32( LDGR_ZOPC  | fregt(r1, 24, 32)  | reg(r2, 28, 32));  }
inline void Assembler::z_lgdr(Register r1, FloatRegister r2)                  { emit_32( LGDR_ZOPC  | regt( r1, 24, 32)  | freg(r2, 28, 32)); }

inline void Assembler::z_cefbr( FloatRegister r1, Register r2)                { emit_32( CEFBR_ZOPC | fregt( r1, 24, 32) | reg( r2, 28, 32)); }
inline void Assembler::z_cdfbr( FloatRegister r1, Register r2)                { emit_32( CDFBR_ZOPC | fregt( r1, 24, 32) | reg( r2, 28, 32)); }
inline void Assembler::z_cegbr( FloatRegister r1, Register r2)                { emit_32( CEGBR_ZOPC | fregt( r1, 24, 32) | reg( r2, 28, 32)); }
inline void Assembler::z_cdgbr( FloatRegister r1, Register r2)                { emit_32( CDGBR_ZOPC | fregt( r1, 24, 32) | reg( r2, 28, 32)); }

inline void Assembler::z_cfebr(Register r1, FloatRegister r2, RoundingMode m) { emit_32( CFEBR_ZOPC | regt(r1, 24, 32) | rounding_mode(m, 16, 32) | freg(r2, 28, 32)); }
inline void Assembler::z_cfdbr(Register r1, FloatRegister r2, RoundingMode m) { emit_32( CFDBR_ZOPC | regt(r1, 24, 32) | rounding_mode(m, 16, 32) | freg(r2, 28, 32)); }
inline void Assembler::z_cgebr(Register r1, FloatRegister r2, RoundingMode m) { emit_32( CGEBR_ZOPC | regt(r1, 24, 32) | rounding_mode(m, 16, 32) | freg(r2, 28, 32)); }
inline void Assembler::z_cgdbr(Register r1, FloatRegister r2, RoundingMode m) { emit_32( CGDBR_ZOPC | regt(r1, 24, 32) | rounding_mode(m, 16, 32) | freg(r2, 28, 32)); }


  inline void Assembler::z_layz(Register r1, int64_t d2, Register b2)      { z_layz(r1, d2, Z_R0, b2); }
  inline void Assembler::z_lay(Register r1, int64_t d2, Register b2)       { z_lay( r1, d2, Z_R0, b2); }
  inline void Assembler::z_laz(Register r1, int64_t d2, Register b2)       { z_laz( r1, d2, Z_R0, b2); }
  inline void Assembler::z_la(Register r1, int64_t d2, Register b2)        { z_la(  r1, d2, Z_R0, b2); }
  inline void Assembler::z_l(Register r1, int64_t d2, Register b2)         { z_l(   r1, d2, Z_R0, b2); }
  inline void Assembler::z_ly(Register r1, int64_t d2, Register b2)        { z_ly(  r1, d2, Z_R0, b2); }
  inline void Assembler::z_lg(Register r1, int64_t d2, Register b2)        { z_lg(  r1, d2, Z_R0, b2); }
  inline void Assembler::z_st(Register r1, int64_t d2, Register b2)        { z_st(  r1, d2, Z_R0, b2); }
  inline void Assembler::z_sty(Register r1, int64_t d2, Register b2)       { z_sty( r1, d2, Z_R0, b2); }
  inline void Assembler::z_stg(Register r1, int64_t d2, Register b2)       { z_stg( r1, d2, Z_R0, b2); }
  inline void Assembler::z_lgf(Register r1, int64_t d2, Register b2)       { z_lgf( r1, d2, Z_R0, b2); }
  inline void Assembler::z_lgh(Register r1, int64_t d2, Register b2)       { z_lgh( r1, d2, Z_R0, b2); }
  inline void Assembler::z_llgh(Register r1, int64_t d2, Register b2)      { z_llgh(r1, d2, Z_R0, b2); }
  inline void Assembler::z_llgf(Register r1, int64_t d2, Register b2)      { z_llgf(r1, d2, Z_R0, b2); }
  inline void Assembler::z_lgb(Register r1, int64_t d2, Register b2)       { z_lgb( r1, d2, Z_R0, b2); }
  inline void Assembler::z_cl( Register r1, int64_t d2, Register b2)       { z_cl(  r1, d2, Z_R0, b2); }
  inline void Assembler::z_c(Register r1, int64_t d2, Register b2)         { z_c(   r1, d2, Z_R0, b2); }
  inline void Assembler::z_cg(Register r1, int64_t d2, Register b2)        { z_cg(  r1, d2, Z_R0, b2); }
  inline void Assembler::z_sh(Register r1, int64_t d2, Register b2)        { z_sh(  r1, d2, Z_R0, b2); }
  inline void Assembler::z_shy(Register r1, int64_t d2, Register b2)       { z_shy( r1, d2, Z_R0, b2); }
  inline void Assembler::z_ste(FloatRegister r1, int64_t d2, Register b2)  { z_ste( r1, d2, Z_R0, b2); }
  inline void Assembler::z_std(FloatRegister r1, int64_t d2, Register b2)  { z_std( r1, d2, Z_R0, b2); }
  inline void Assembler::z_stdy(FloatRegister r1, int64_t d2, Register b2) { z_stdy(r1, d2, Z_R0, b2); }
  inline void Assembler::z_stey(FloatRegister r1, int64_t d2, Register b2) { z_stey(r1, d2, Z_R0, b2); }
  inline void Assembler::z_ld(FloatRegister r1, int64_t d2, Register b2)   { z_ld(  r1, d2, Z_R0, b2); }
  inline void Assembler::z_ldy(FloatRegister r1, int64_t d2, Register b2)  { z_ldy( r1, d2, Z_R0, b2); }
  inline void Assembler::z_le(FloatRegister r1, int64_t d2, Register b2)   { z_le(  r1, d2, Z_R0, b2); }
  inline void Assembler::z_ley(FloatRegister r1, int64_t d2, Register b2)  { z_ley( r1, d2, Z_R0, b2); }
  inline void Assembler::z_agf(Register r1, int64_t d2, Register b2)       { z_agf( r1, d2, Z_R0, b2); }
  inline void Assembler::z_cvd(Register r1, int64_t d2, Register b2)       { z_cvd( r1, d2, Z_R0, b2); }
  inline void Assembler::z_cvdg(Register r1, int64_t d2, Register b2)      { z_cvdg(r1, d2, Z_R0, b2); }

// signed comparison
inline void Assembler::z_crj(Register r1, Register r2, branch_condition m3, Label& L)   { z_crj(  r1, r2, m3, target(L)); }
inline void Assembler::z_cgrj(Register r1, Register r2, branch_condition m3, Label& L)  { z_cgrj( r1, r2, m3, target(L)); }
inline void Assembler::z_cij(Register r1, int64_t i2, branch_condition m3, Label& L)    { z_cij(  r1, i2, m3, target(L)); }
inline void Assembler::z_cgij(Register r1, int64_t i2, branch_condition m3, Label& L)   { z_cgij( r1, i2, m3, target(L)); }
// unsigned comparison
inline void Assembler::z_clrj(Register r1, Register r2, branch_condition m3, Label& L)  { z_clrj( r1, r2, m3, target(L)); }
inline void Assembler::z_clgrj(Register r1, Register r2, branch_condition m3, Label& L) { z_clgrj(r1, r2, m3, target(L)); }
inline void Assembler::z_clij(Register r1, int64_t i2, branch_condition m3, Label& L)   { z_clij( r1, i2, m3, target(L)); }
inline void Assembler::z_clgij(Register r1, int64_t i2, branch_condition m3, Label& L)  { z_clgij(r1, i2, m3, target(L)); }

// branch never (nop), branch always
inline void Assembler::z_nop() { z_bcr(bcondNop, Z_R0); }
inline void Assembler::nop() { z_nop(); }
inline void Assembler::z_br(Register r2) { assert(r2 != Z_R0, "nop if target is Z_R0, use z_nop() instead"); z_bcr(bcondAlways, r2 ); }

inline void Assembler::z_exrl(Register r1, Label& L) { z_exrl(r1, target(L)); }  // z10
inline void Assembler::z_larl(Register r1, Label& L) { z_larl(r1, target(L)); }
inline void Assembler::z_bru(   Label& L) { z_brc(bcondAlways, target(L)); }
inline void Assembler::z_brul(  Label& L) { z_brcl(bcondAlways, target(L)); }
inline void Assembler::z_brul( address a) { z_brcl(bcondAlways,a ); }
inline void Assembler::z_brh(   Label& L) { z_brc(bcondHigh, target(L)); }
inline void Assembler::z_brl(   Label& L) { z_brc(bcondLow, target(L)); }
inline void Assembler::z_bre(   Label& L) { z_brc(bcondEqual, target(L)); }
inline void Assembler::z_brnh(  Label& L) { z_brc(bcondNotHigh, target(L)); }
inline void Assembler::z_brnl(  Label& L) { z_brc(bcondNotLow, target(L)); }
inline void Assembler::z_brne(  Label& L) { z_brc(bcondNotEqual, target(L)); }
inline void Assembler::z_brz(   Label& L) { z_brc(bcondZero, target(L)); }
inline void Assembler::z_brnz(  Label& L) { z_brc(bcondNotZero, target(L)); }
inline void Assembler::z_braz(  Label& L) { z_brc(bcondAllZero, target(L)); }
inline void Assembler::z_brnaz( Label& L) { z_brc(bcondNotAllZero, target(L)); }
inline void Assembler::z_brnp(  Label& L) { z_brc( bcondNotPositive, target( L)); }
inline void Assembler::z_btrue( Label& L) { z_brc(bcondAllOne, target(L)); }
inline void Assembler::z_bfalse(Label& L) { z_brc(bcondAllZero, target(L)); }
inline void Assembler::z_bvat(  Label& L) { z_brc(bcondVAlltrue, target(L)); }
inline void Assembler::z_bvnt(  Label& L) { z_brc((Assembler::branch_condition)(bcondVMixed | bcondVAllfalse), target(L)); }
inline void Assembler::z_bvmix( Label& L) { z_brc(bcondVMixed, target(L)); }
inline void Assembler::z_bvaf(  Label& L) { z_brc(bcondVAllfalse, target(L)); }
inline void Assembler::z_bvnf(  Label& L) { z_brc((Assembler::branch_condition)(bcondVMixed | bcondVAlltrue), target(L)); }
inline void Assembler::z_brno(  Label& L) { z_brc(bcondNotOrdered, target(L)); }
inline void Assembler::z_brc( branch_condition m, Label& L) { z_brc(m, target(L)); }
inline void Assembler::z_brcl(branch_condition m, Label& L) { z_brcl(m, target(L)); }


// Instruction must start at passed address.
// Extra check for illtraps with ID.
inline unsigned int Assembler::instr_len(unsigned char *instr) {
  switch ((*instr) >> 6) {
    case 0: return 2;
    case 1: // fallthru
    case 2: return 4;
    case 3: return 6;
    default:
      // Control can't reach here.
      // The switch expression examines just the leftmost two bytes
      // of the main opcode. So the range of values is just [0..3].
      // Having a default clause makes the compiler happy.
      ShouldNotReachHere();
      return 0;
  }
}

// Move instr at pc right-justified into passed long int.
// Return instr len in bytes as function result.
// Note: 2-byte instr don't really need to be accessed unsigned
// because leftmost two bits are always zero. We use
// unsigned here for reasons of uniformity.
inline unsigned int Assembler::get_instruction(unsigned char *pc, unsigned long *instr) {
  unsigned int len = instr_len(pc);
  switch (len) {
    case 2:
      *instr = *(unsigned short*) pc; break;
    case 4:
      *instr = *(unsigned int*) pc; break;
    case 6:
      // Must compose this case. Can't read 8 bytes and then cut off
      // the rightmost two bytes. Could potentially access
      // unallocated storage.
      *instr = ((unsigned long)(*(unsigned int*)   pc)) << 16 |
               ((unsigned long)*(unsigned short*) (pc + 4)); break;
    default:
      // Control can't reach here.
      // The length as returned from instr_len() can only be 2, 4, or 6 bytes.
      // Having a default clause makes the compiler happy.
      ShouldNotReachHere();
      *instr = 0L; // This assignment is there to make gcc8 happy.
      break;
  }
  return len;
}

// Check if instruction is the expected one.
// Instruction is passed right-justified in inst.
inline bool Assembler::is_equal(unsigned long inst, unsigned long idef) {
  unsigned long imask;

  if ((idef >> 32) != 0) { // 6byte instructions
    switch (idef >> 40) {  // select mask by main opcode
      case 0xc0:
      case 0xc2:
      case 0xc4:
      case 0xc6: imask = RIL_MASK; break;
      case 0xec:
        if ((idef & 0x00ffL) < 0x0080L) {
          imask = RIE_MASK;
          break;
        }
        // Fallthru for other sub opcodes.
      default:
#ifdef ASSERT
        tty->print_cr("inst = %16.16lx, idef = %16.16lx, imask unspecified\n", inst, idef);
        tty->flush();
#endif
        ShouldNotReachHere();
        return 0;
    }
  } else {                 // 4-byte instructions
    switch (idef >> 24) {  // Select mask by main opcode.
      case 0x84:
      case 0x85: imask = RSI_MASK; break;
      case 0xa5:
      case 0xa7: imask =  RI_MASK; break;
      case 0xb9: imask = RRE_MASK; break; // RRE_MASK or RRF_MASK. Opcode fields are at same bit positions.
      default: {
#ifdef ASSERT
        tty->print_cr("inst = %16.16lx, idef = %16.16lx, imask unspecified\n", inst, idef);
        tty->flush();
#endif
        ShouldNotReachHere();
        return 0;
      }
    }
  }
  return (inst & imask) == idef;
}

inline bool Assembler::is_equal(unsigned long inst, unsigned long idef, unsigned long imask) {
  assert(imask != 0, "valid instruction mask required");
  return (inst & imask) == idef;
}

// Check if instruction is the expected one.
// Instruction is passed left-justified at inst.
inline bool Assembler::is_equal(address iloc, unsigned long idef) {
  unsigned long inst;
  get_instruction(iloc, &inst);
  return is_equal(inst, idef);
}

inline bool Assembler::is_equal(address iloc, unsigned long idef, unsigned long imask) {
  unsigned long inst;
  get_instruction(iloc, &inst);
  return is_equal(inst, idef, imask);
}

inline bool Assembler::is_sigtrap_range_check(address pc) {
  return (is_equal(pc, CLFIT_ZOPC, RIE_MASK) || is_equal(pc, CLRT_ZOPC, RRE_MASK));
}

inline bool Assembler::is_sigtrap_zero_check(address pc) {
  return (is_equal(pc, CGIT_ZOPC, RIE_MASK) || is_equal(pc, CIT_ZOPC, RIE_MASK));
}

#endif // CPU_S390_ASSEMBLER_S390_INLINE_HPP
