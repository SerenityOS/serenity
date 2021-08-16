/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2012, 2020 SAP SE. All rights reserved.
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

#ifndef CPU_PPC_ASSEMBLER_PPC_INLINE_HPP
#define CPU_PPC_ASSEMBLER_PPC_INLINE_HPP

#include "asm/assembler.inline.hpp"
#include "asm/codeBuffer.hpp"
#include "code/codeCache.hpp"
#include "runtime/vm_version.hpp"

inline void Assembler::emit_int32(int x) {
  AbstractAssembler::emit_int32(x);
}

inline void Assembler::emit_data(int x) {
  emit_int32(x);
}

inline void Assembler::emit_data(int x, relocInfo::relocType rtype) {
  relocate(rtype);
  emit_int32(x);
}

inline void Assembler::emit_data(int x, RelocationHolder const& rspec) {
  relocate(rspec);
  emit_int32(x);
}

// Emit an address
inline address Assembler::emit_addr(const address addr) {
  address start = pc();
  emit_address(addr);
  return start;
}

#if !defined(ABI_ELFv2)
// Emit a function descriptor with the specified entry point, TOC, and
// ENV. If the entry point is NULL, the descriptor will point just
// past the descriptor.
inline address Assembler::emit_fd(address entry, address toc, address env) {
  FunctionDescriptor* fd = (FunctionDescriptor*)pc();

  assert(sizeof(FunctionDescriptor) == 3*sizeof(address), "function descriptor size");

  (void)emit_addr();
  (void)emit_addr();
  (void)emit_addr();

  fd->set_entry(entry == NULL ? pc() : entry);
  fd->set_toc(toc);
  fd->set_env(env);

  return (address)fd;
}
#endif

// Issue an illegal instruction. 0 is guaranteed to be an illegal instruction.
inline void Assembler::illtrap() { Assembler::emit_int32(0); }
inline bool Assembler::is_illtrap(int x) { return x == 0; }

// PPC 1, section 3.3.8, Fixed-Point Arithmetic Instructions
inline void Assembler::addi(   Register d, Register a, int si16)   { assert(a != R0, "r0 not allowed"); addi_r0ok( d, a, si16); }
inline void Assembler::addis(  Register d, Register a, int si16)   { assert(a != R0, "r0 not allowed"); addis_r0ok(d, a, si16); }
inline void Assembler::addi_r0ok(Register d,Register a,int si16)   { emit_int32(ADDI_OPCODE   | rt(d) | ra(a) | simm(si16, 16)); }
inline void Assembler::addis_r0ok(Register d,Register a,int si16)  { emit_int32(ADDIS_OPCODE  | rt(d) | ra(a) | simm(si16, 16)); }
inline void Assembler::addic_( Register d, Register a, int si16)   { emit_int32(ADDIC__OPCODE | rt(d) | ra(a) | simm(si16, 16)); }
inline void Assembler::subfic( Register d, Register a, int si16)   { emit_int32(SUBFIC_OPCODE | rt(d) | ra(a) | simm(si16, 16)); }
inline void Assembler::add(    Register d, Register a, Register b) { emit_int32(ADD_OPCODE    | rt(d) | ra(a) | rb(b) | oe(0) | rc(0)); }
inline void Assembler::add_(   Register d, Register a, Register b) { emit_int32(ADD_OPCODE    | rt(d) | ra(a) | rb(b) | oe(0) | rc(1)); }
inline void Assembler::subf(   Register d, Register a, Register b) { emit_int32(SUBF_OPCODE   | rt(d) | ra(a) | rb(b) | oe(0) | rc(0)); }
inline void Assembler::sub(    Register d, Register a, Register b) { subf(d, b, a); }
inline void Assembler::subf_(  Register d, Register a, Register b) { emit_int32(SUBF_OPCODE   | rt(d) | ra(a) | rb(b) | oe(0) | rc(1)); }
inline void Assembler::addc(   Register d, Register a, Register b) { emit_int32(ADDC_OPCODE   | rt(d) | ra(a) | rb(b) | oe(0) | rc(0)); }
inline void Assembler::addc_(  Register d, Register a, Register b) { emit_int32(ADDC_OPCODE   | rt(d) | ra(a) | rb(b) | oe(0) | rc(1)); }
inline void Assembler::subfc(  Register d, Register a, Register b) { emit_int32(SUBFC_OPCODE  | rt(d) | ra(a) | rb(b) | oe(0) | rc(0)); }
inline void Assembler::subfc_( Register d, Register a, Register b) { emit_int32(SUBFC_OPCODE  | rt(d) | ra(a) | rb(b) | oe(0) | rc(1)); }
inline void Assembler::adde(   Register d, Register a, Register b) { emit_int32(ADDE_OPCODE   | rt(d) | ra(a) | rb(b) | oe(0) | rc(0)); }
inline void Assembler::adde_(  Register d, Register a, Register b) { emit_int32(ADDE_OPCODE   | rt(d) | ra(a) | rb(b) | oe(0) | rc(1)); }
inline void Assembler::subfe(  Register d, Register a, Register b) { emit_int32(SUBFE_OPCODE  | rt(d) | ra(a) | rb(b) | oe(0) | rc(0)); }
inline void Assembler::subfe_( Register d, Register a, Register b) { emit_int32(SUBFE_OPCODE  | rt(d) | ra(a) | rb(b) | oe(0) | rc(1)); }
inline void Assembler::addme(  Register d, Register a)             { emit_int32(ADDME_OPCODE  | rt(d) | ra(a) |         oe(0) | rc(0)); }
inline void Assembler::addme_( Register d, Register a)             { emit_int32(ADDME_OPCODE  | rt(d) | ra(a) |         oe(0) | rc(1)); }
inline void Assembler::subfme( Register d, Register a)             { emit_int32(SUBFME_OPCODE | rt(d) | ra(a) |         oe(0) | rc(0)); }
inline void Assembler::subfme_(Register d, Register a)             { emit_int32(SUBFME_OPCODE | rt(d) | ra(a) |         oe(0) | rc(1)); }
inline void Assembler::addze(  Register d, Register a)             { emit_int32(ADDZE_OPCODE  | rt(d) | ra(a) |         oe(0) | rc(0)); }
inline void Assembler::addze_( Register d, Register a)             { emit_int32(ADDZE_OPCODE  | rt(d) | ra(a) |         oe(0) | rc(1)); }
inline void Assembler::subfze( Register d, Register a)             { emit_int32(SUBFZE_OPCODE | rt(d) | ra(a) |         oe(0) | rc(0)); }
inline void Assembler::subfze_(Register d, Register a)             { emit_int32(SUBFZE_OPCODE | rt(d) | ra(a) |         oe(0) | rc(1)); }
inline void Assembler::neg(    Register d, Register a)             { emit_int32(NEG_OPCODE    | rt(d) | ra(a) | oe(0) | rc(0)); }
inline void Assembler::neg_(   Register d, Register a)             { emit_int32(NEG_OPCODE    | rt(d) | ra(a) | oe(0) | rc(1)); }
inline void Assembler::mulli(  Register d, Register a, int si16)   { emit_int32(MULLI_OPCODE  | rt(d) | ra(a) | simm(si16, 16)); }
inline void Assembler::mulld(  Register d, Register a, Register b) { emit_int32(MULLD_OPCODE  | rt(d) | ra(a) | rb(b) | oe(0) | rc(0)); }
inline void Assembler::mulld_( Register d, Register a, Register b) { emit_int32(MULLD_OPCODE  | rt(d) | ra(a) | rb(b) | oe(0) | rc(1)); }
inline void Assembler::mullw(  Register d, Register a, Register b) { emit_int32(MULLW_OPCODE  | rt(d) | ra(a) | rb(b) | oe(0) | rc(0)); }
inline void Assembler::mullw_( Register d, Register a, Register b) { emit_int32(MULLW_OPCODE  | rt(d) | ra(a) | rb(b) | oe(0) | rc(1)); }
inline void Assembler::mulhw(  Register d, Register a, Register b) { emit_int32(MULHW_OPCODE  | rt(d) | ra(a) | rb(b) | rc(0)); }
inline void Assembler::mulhw_( Register d, Register a, Register b) { emit_int32(MULHW_OPCODE  | rt(d) | ra(a) | rb(b) | rc(1)); }
inline void Assembler::mulhwu( Register d, Register a, Register b) { emit_int32(MULHWU_OPCODE | rt(d) | ra(a) | rb(b) | rc(0)); }
inline void Assembler::mulhwu_(Register d, Register a, Register b) { emit_int32(MULHWU_OPCODE | rt(d) | ra(a) | rb(b) | rc(1)); }
inline void Assembler::mulhd(  Register d, Register a, Register b) { emit_int32(MULHD_OPCODE  | rt(d) | ra(a) | rb(b) | rc(0)); }
inline void Assembler::mulhd_( Register d, Register a, Register b) { emit_int32(MULHD_OPCODE  | rt(d) | ra(a) | rb(b) | rc(1)); }
inline void Assembler::mulhdu( Register d, Register a, Register b) { emit_int32(MULHDU_OPCODE | rt(d) | ra(a) | rb(b) | rc(0)); }
inline void Assembler::mulhdu_(Register d, Register a, Register b) { emit_int32(MULHDU_OPCODE | rt(d) | ra(a) | rb(b) | rc(1)); }
inline void Assembler::divd(   Register d, Register a, Register b) { emit_int32(DIVD_OPCODE   | rt(d) | ra(a) | rb(b) | oe(0) | rc(0)); }
inline void Assembler::divd_(  Register d, Register a, Register b) { emit_int32(DIVD_OPCODE   | rt(d) | ra(a) | rb(b) | oe(0) | rc(1)); }
inline void Assembler::divw(   Register d, Register a, Register b) { emit_int32(DIVW_OPCODE   | rt(d) | ra(a) | rb(b) | oe(0) | rc(0)); }
inline void Assembler::divw_(  Register d, Register a, Register b) { emit_int32(DIVW_OPCODE   | rt(d) | ra(a) | rb(b) | oe(0) | rc(1)); }
inline void Assembler::divwu(  Register d, Register a, Register b) { emit_int32(DIVWU_OPCODE  | rt(d) | ra(a) | rb(b) | oe(0) | rc(0)); }
inline void Assembler::divwu_( Register d, Register a, Register b) { emit_int32(DIVWU_OPCODE  | rt(d) | ra(a) | rb(b) | oe(0) | rc(1)); }

// Prefixed instructions, introduced by POWER10
inline void Assembler::paddi(Register d, Register a, long si34, bool r = false) {
  assert(a != R0 || r, "r0 not allowed, unless R is set (CIA relative)");
  paddi_r0ok( d, a, si34, r);
}

inline void Assembler::paddi_r0ok(Register d, Register a, long si34, bool r = false) {
  emit_int32(PADDI_PREFIX_OPCODE | r_eo(r) | d0_eo(si34));
  emit_int32(PADDI_SUFFIX_OPCODE | rt(d)   | ra(a)   | d1_eo(si34));
}

inline void Assembler::xxpermx( VectorSRegister d, VectorSRegister a, VectorSRegister b, VectorSRegister c, int ui3) {
  emit_int32(XXPERMX_PREFIX_OPCODE | uimm(ui3, 3));
  emit_int32(XXPERMX_SUFFIX_OPCODE | vsrt(d) | vsra(a) | vsrb(b) | vsrc(c));
}

// Fixed-Point Arithmetic Instructions with Overflow detection
inline void Assembler::addo(    Register d, Register a, Register b) { emit_int32(ADD_OPCODE    | rt(d) | ra(a) | rb(b) | oe(1) | rc(0)); }
inline void Assembler::addo_(   Register d, Register a, Register b) { emit_int32(ADD_OPCODE    | rt(d) | ra(a) | rb(b) | oe(1) | rc(1)); }
inline void Assembler::subfo(   Register d, Register a, Register b) { emit_int32(SUBF_OPCODE   | rt(d) | ra(a) | rb(b) | oe(1) | rc(0)); }
inline void Assembler::subfo_(  Register d, Register a, Register b) { emit_int32(SUBF_OPCODE   | rt(d) | ra(a) | rb(b) | oe(1) | rc(1)); }
inline void Assembler::addco(   Register d, Register a, Register b) { emit_int32(ADDC_OPCODE   | rt(d) | ra(a) | rb(b) | oe(1) | rc(0)); }
inline void Assembler::addco_(  Register d, Register a, Register b) { emit_int32(ADDC_OPCODE   | rt(d) | ra(a) | rb(b) | oe(1) | rc(1)); }
inline void Assembler::subfco(  Register d, Register a, Register b) { emit_int32(SUBFC_OPCODE  | rt(d) | ra(a) | rb(b) | oe(1) | rc(0)); }
inline void Assembler::subfco_( Register d, Register a, Register b) { emit_int32(SUBFC_OPCODE  | rt(d) | ra(a) | rb(b) | oe(1) | rc(1)); }
inline void Assembler::addeo(   Register d, Register a, Register b) { emit_int32(ADDE_OPCODE   | rt(d) | ra(a) | rb(b) | oe(1) | rc(0)); }
inline void Assembler::addeo_(  Register d, Register a, Register b) { emit_int32(ADDE_OPCODE   | rt(d) | ra(a) | rb(b) | oe(1) | rc(1)); }
inline void Assembler::subfeo(  Register d, Register a, Register b) { emit_int32(SUBFE_OPCODE  | rt(d) | ra(a) | rb(b) | oe(1) | rc(0)); }
inline void Assembler::subfeo_( Register d, Register a, Register b) { emit_int32(SUBFE_OPCODE  | rt(d) | ra(a) | rb(b) | oe(1) | rc(1)); }
inline void Assembler::addmeo(  Register d, Register a)             { emit_int32(ADDME_OPCODE  | rt(d) | ra(a) |         oe(1) | rc(0)); }
inline void Assembler::addmeo_( Register d, Register a)             { emit_int32(ADDME_OPCODE  | rt(d) | ra(a) |         oe(1) | rc(1)); }
inline void Assembler::subfmeo( Register d, Register a)             { emit_int32(SUBFME_OPCODE | rt(d) | ra(a) |         oe(1) | rc(0)); }
inline void Assembler::subfmeo_(Register d, Register a)             { emit_int32(SUBFME_OPCODE | rt(d) | ra(a) |         oe(1) | rc(1)); }
inline void Assembler::addzeo(  Register d, Register a)             { emit_int32(ADDZE_OPCODE  | rt(d) | ra(a) |         oe(1) | rc(0)); }
inline void Assembler::addzeo_( Register d, Register a)             { emit_int32(ADDZE_OPCODE  | rt(d) | ra(a) |         oe(1) | rc(1)); }
inline void Assembler::subfzeo( Register d, Register a)             { emit_int32(SUBFZE_OPCODE | rt(d) | ra(a) |         oe(1) | rc(0)); }
inline void Assembler::subfzeo_(Register d, Register a)             { emit_int32(SUBFZE_OPCODE | rt(d) | ra(a) |         oe(1) | rc(1)); }
inline void Assembler::nego(    Register d, Register a)             { emit_int32(NEG_OPCODE    | rt(d) | ra(a) | oe(1) | rc(0)); }
inline void Assembler::nego_(   Register d, Register a)             { emit_int32(NEG_OPCODE    | rt(d) | ra(a) | oe(1) | rc(1)); }
inline void Assembler::mulldo(  Register d, Register a, Register b) { emit_int32(MULLD_OPCODE  | rt(d) | ra(a) | rb(b) | oe(1) | rc(0)); }
inline void Assembler::mulldo_( Register d, Register a, Register b) { emit_int32(MULLD_OPCODE  | rt(d) | ra(a) | rb(b) | oe(1) | rc(1)); }
inline void Assembler::mullwo(  Register d, Register a, Register b) { emit_int32(MULLW_OPCODE  | rt(d) | ra(a) | rb(b) | oe(1) | rc(0)); }
inline void Assembler::mullwo_( Register d, Register a, Register b) { emit_int32(MULLW_OPCODE  | rt(d) | ra(a) | rb(b) | oe(1) | rc(1)); }
inline void Assembler::divdo(   Register d, Register a, Register b) { emit_int32(DIVD_OPCODE   | rt(d) | ra(a) | rb(b) | oe(1) | rc(0)); }
inline void Assembler::divdo_(  Register d, Register a, Register b) { emit_int32(DIVD_OPCODE   | rt(d) | ra(a) | rb(b) | oe(1) | rc(1)); }
inline void Assembler::divwo(   Register d, Register a, Register b) { emit_int32(DIVW_OPCODE   | rt(d) | ra(a) | rb(b) | oe(1) | rc(0)); }
inline void Assembler::divwo_(  Register d, Register a, Register b) { emit_int32(DIVW_OPCODE   | rt(d) | ra(a) | rb(b) | oe(1) | rc(1)); }

// extended mnemonics
inline void Assembler::li(   Register d, int si16)             { Assembler::addi_r0ok( d, R0, si16); }
inline void Assembler::lis(  Register d, int si16)             { Assembler::addis_r0ok(d, R0, si16); }
inline void Assembler::addir(Register d, int si16, Register a) { Assembler::addi(d, a, si16); }
inline void Assembler::subi( Register d, Register a, int si16) { Assembler::addi(d, a, -si16); }

// Prefixed instructions, introduced by POWER10
inline void Assembler::pli(Register d, long si34) { Assembler::paddi_r0ok( d, R0, si34, false); }

// PPC 1, section 3.3.9, Fixed-Point Compare Instructions
inline void Assembler::cmpi(  ConditionRegister f, int l, Register a, int si16)   { emit_int32( CMPI_OPCODE  | bf(f) | l10(l) | ra(a) | simm(si16,16)); }
inline void Assembler::cmp(   ConditionRegister f, int l, Register a, Register b) { emit_int32( CMP_OPCODE   | bf(f) | l10(l) | ra(a) | rb(b)); }
inline void Assembler::cmpli( ConditionRegister f, int l, Register a, int ui16)   { emit_int32( CMPLI_OPCODE | bf(f) | l10(l) | ra(a) | uimm(ui16,16)); }
inline void Assembler::cmpl(  ConditionRegister f, int l, Register a, Register b) { emit_int32( CMPL_OPCODE  | bf(f) | l10(l) | ra(a) | rb(b)); }
inline void Assembler::cmprb( ConditionRegister f, int l, Register a, Register b) { emit_int32( CMPRB_OPCODE | bf(f) | l10(l) | ra(a) | rb(b)); }
inline void Assembler::cmpeqb(ConditionRegister f, Register a, Register b)        { emit_int32( CMPEQB_OPCODE| bf(f) | ra(a)  | rb(b)); }

// extended mnemonics of Compare Instructions
inline void Assembler::cmpwi( ConditionRegister crx, Register a, int si16)   { Assembler::cmpi( crx, 0, a, si16); }
inline void Assembler::cmpdi( ConditionRegister crx, Register a, int si16)   { Assembler::cmpi( crx, 1, a, si16); }
inline void Assembler::cmpw(  ConditionRegister crx, Register a, Register b) { Assembler::cmp(  crx, 0, a, b); }
inline void Assembler::cmpd(  ConditionRegister crx, Register a, Register b) { Assembler::cmp(  crx, 1, a, b); }
inline void Assembler::cmplwi(ConditionRegister crx, Register a, int ui16)   { Assembler::cmpli(crx, 0, a, ui16); }
inline void Assembler::cmpldi(ConditionRegister crx, Register a, int ui16)   { Assembler::cmpli(crx, 1, a, ui16); }
inline void Assembler::cmplw( ConditionRegister crx, Register a, Register b) { Assembler::cmpl( crx, 0, a, b); }
inline void Assembler::cmpld( ConditionRegister crx, Register a, Register b) { Assembler::cmpl( crx, 1, a, b); }

inline void Assembler::isel(Register d, Register a, Register b, int c) { guarantee(VM_Version::has_isel(), "opcode not supported on this hardware");
                                                                         emit_int32(ISEL_OPCODE    | rt(d)  | ra(a) | rb(b) | bc(c)); }

// PPC 1, section 3.3.11, Fixed-Point Logical Instructions
inline void Assembler::andi_(   Register a, Register s, int ui16)      { emit_int32(ANDI_OPCODE    | rta(a) | rs(s) | uimm(ui16, 16)); }
inline void Assembler::andis_(  Register a, Register s, int ui16)      { emit_int32(ANDIS_OPCODE   | rta(a) | rs(s) | uimm(ui16, 16)); }
inline void Assembler::ori(     Register a, Register s, int ui16)      { emit_int32(ORI_OPCODE     | rta(a) | rs(s) | uimm(ui16, 16)); }
inline void Assembler::oris(    Register a, Register s, int ui16)      { emit_int32(ORIS_OPCODE    | rta(a) | rs(s) | uimm(ui16, 16)); }
inline void Assembler::xori(    Register a, Register s, int ui16)      { emit_int32(XORI_OPCODE    | rta(a) | rs(s) | uimm(ui16, 16)); }
inline void Assembler::xoris(   Register a, Register s, int ui16)      { emit_int32(XORIS_OPCODE   | rta(a) | rs(s) | uimm(ui16, 16)); }
inline void Assembler::andr(    Register a, Register s, Register b)    { emit_int32(AND_OPCODE     | rta(a) | rs(s) | rb(b) | rc(0)); }
inline void Assembler::and_(    Register a, Register s, Register b)    { emit_int32(AND_OPCODE     | rta(a) | rs(s) | rb(b) | rc(1)); }

inline void Assembler::or_unchecked(Register a, Register s, Register b){ emit_int32(OR_OPCODE      | rta(a) | rs(s) | rb(b) | rc(0)); }
inline void Assembler::orr(     Register a, Register s, Register b)    { if (a==s && s==b) { Assembler::nop(); } else { Assembler::or_unchecked(a,s,b); } }
inline void Assembler::or_(     Register a, Register s, Register b)    { emit_int32(OR_OPCODE      | rta(a) | rs(s) | rb(b) | rc(1)); }
inline void Assembler::xorr(    Register a, Register s, Register b)    { emit_int32(XOR_OPCODE     | rta(a) | rs(s) | rb(b) | rc(0)); }
inline void Assembler::xor_(    Register a, Register s, Register b)    { emit_int32(XOR_OPCODE     | rta(a) | rs(s) | rb(b) | rc(1)); }
inline void Assembler::nand(    Register a, Register s, Register b)    { emit_int32(NAND_OPCODE    | rta(a) | rs(s) | rb(b) | rc(0)); }
inline void Assembler::nand_(   Register a, Register s, Register b)    { emit_int32(NAND_OPCODE    | rta(a) | rs(s) | rb(b) | rc(1)); }
inline void Assembler::nor(     Register a, Register s, Register b)    { emit_int32(NOR_OPCODE     | rta(a) | rs(s) | rb(b) | rc(0)); }
inline void Assembler::nor_(    Register a, Register s, Register b)    { emit_int32(NOR_OPCODE     | rta(a) | rs(s) | rb(b) | rc(1)); }
inline void Assembler::andc(    Register a, Register s, Register b)    { emit_int32(ANDC_OPCODE    | rta(a) | rs(s) | rb(b) | rc(0)); }
inline void Assembler::andc_(   Register a, Register s, Register b)    { emit_int32(ANDC_OPCODE    | rta(a) | rs(s) | rb(b) | rc(1)); }
inline void Assembler::orc(     Register a, Register s, Register b)    { emit_int32(ORC_OPCODE     | rta(a) | rs(s) | rb(b) | rc(0)); }
inline void Assembler::orc_(    Register a, Register s, Register b)    { emit_int32(ORC_OPCODE     | rta(a) | rs(s) | rb(b) | rc(1)); }
inline void Assembler::extsb(   Register a, Register s)                { emit_int32(EXTSB_OPCODE   | rta(a) | rs(s) | rc(0)); }
inline void Assembler::extsb_(  Register a, Register s)                { emit_int32(EXTSB_OPCODE   | rta(a) | rs(s) | rc(1)); }
inline void Assembler::extsh(   Register a, Register s)                { emit_int32(EXTSH_OPCODE   | rta(a) | rs(s) | rc(0)); }
inline void Assembler::extsh_(  Register a, Register s)                { emit_int32(EXTSH_OPCODE   | rta(a) | rs(s) | rc(1)); }
inline void Assembler::extsw(   Register a, Register s)                { emit_int32(EXTSW_OPCODE   | rta(a) | rs(s) | rc(0)); }
inline void Assembler::extsw_(  Register a, Register s)                { emit_int32(EXTSW_OPCODE   | rta(a) | rs(s) | rc(1)); }

// extended mnemonics
inline void Assembler::nop()                              { Assembler::ori(R0, R0, 0); }
// NOP for FP and BR units (different versions to allow them to be in one group)
inline void Assembler::fpnop0()                           { Assembler::fmr(F30, F30); }
inline void Assembler::fpnop1()                           { Assembler::fmr(F31, F31); }
inline void Assembler::brnop0()                           { Assembler::mcrf(CCR2, CCR2); }
inline void Assembler::brnop1()                           { Assembler::mcrf(CCR3, CCR3); }
inline void Assembler::brnop2()                           { Assembler::mcrf(CCR4,  CCR4); }

inline void Assembler::mr(      Register d, Register s)   { Assembler::orr(d, s, s); }
inline void Assembler::ori_opt( Register d, int ui16)     { if (ui16!=0) Assembler::ori( d, d, ui16); }
inline void Assembler::oris_opt(Register d, int ui16)     { if (ui16!=0) Assembler::oris(d, d, ui16); }

inline void Assembler::endgroup()                         { Assembler::ori(R1, R1, 0); }

// count instructions
inline void Assembler::cntlzw(  Register a, Register s)              { emit_int32(CNTLZW_OPCODE | rta(a) | rs(s) | rc(0)); }
inline void Assembler::cntlzw_( Register a, Register s)              { emit_int32(CNTLZW_OPCODE | rta(a) | rs(s) | rc(1)); }
inline void Assembler::cntlzd(  Register a, Register s)              { emit_int32(CNTLZD_OPCODE | rta(a) | rs(s) | rc(0)); }
inline void Assembler::cntlzd_( Register a, Register s)              { emit_int32(CNTLZD_OPCODE | rta(a) | rs(s) | rc(1)); }
inline void Assembler::cnttzw(  Register a, Register s)              { emit_int32(CNTTZW_OPCODE | rta(a) | rs(s) | rc(0)); }
inline void Assembler::cnttzw_( Register a, Register s)              { emit_int32(CNTTZW_OPCODE | rta(a) | rs(s) | rc(1)); }
inline void Assembler::cnttzd(  Register a, Register s)              { emit_int32(CNTTZD_OPCODE | rta(a) | rs(s) | rc(0)); }
inline void Assembler::cnttzd_( Register a, Register s)              { emit_int32(CNTTZD_OPCODE | rta(a) | rs(s) | rc(1)); }

// PPC 1, section 3.3.12, Fixed-Point Rotate and Shift Instructions
inline void Assembler::sld(     Register a, Register s, Register b)  { emit_int32(SLD_OPCODE    | rta(a) | rs(s) | rb(b) | rc(0)); }
inline void Assembler::sld_(    Register a, Register s, Register b)  { emit_int32(SLD_OPCODE    | rta(a) | rs(s) | rb(b) | rc(1)); }
inline void Assembler::slw(     Register a, Register s, Register b)  { emit_int32(SLW_OPCODE    | rta(a) | rs(s) | rb(b) | rc(0)); }
inline void Assembler::slw_(    Register a, Register s, Register b)  { emit_int32(SLW_OPCODE    | rta(a) | rs(s) | rb(b) | rc(1)); }
inline void Assembler::srd(     Register a, Register s, Register b)  { emit_int32(SRD_OPCODE    | rta(a) | rs(s) | rb(b) | rc(0)); }
inline void Assembler::srd_(    Register a, Register s, Register b)  { emit_int32(SRD_OPCODE    | rta(a) | rs(s) | rb(b) | rc(1)); }
inline void Assembler::srw(     Register a, Register s, Register b)  { emit_int32(SRW_OPCODE    | rta(a) | rs(s) | rb(b) | rc(0)); }
inline void Assembler::srw_(    Register a, Register s, Register b)  { emit_int32(SRW_OPCODE    | rta(a) | rs(s) | rb(b) | rc(1)); }
inline void Assembler::srad(    Register a, Register s, Register b)  { emit_int32(SRAD_OPCODE   | rta(a) | rs(s) | rb(b) | rc(0)); }
inline void Assembler::srad_(   Register a, Register s, Register b)  { emit_int32(SRAD_OPCODE   | rta(a) | rs(s) | rb(b) | rc(1)); }
inline void Assembler::sraw(    Register a, Register s, Register b)  { emit_int32(SRAW_OPCODE   | rta(a) | rs(s) | rb(b) | rc(0)); }
inline void Assembler::sraw_(   Register a, Register s, Register b)  { emit_int32(SRAW_OPCODE   | rta(a) | rs(s) | rb(b) | rc(1)); }
inline void Assembler::sradi(   Register a, Register s, int sh6)     { emit_int32(SRADI_OPCODE  | rta(a) | rs(s) | sh162030(sh6) | rc(0)); }
inline void Assembler::sradi_(  Register a, Register s, int sh6)     { emit_int32(SRADI_OPCODE  | rta(a) | rs(s) | sh162030(sh6) | rc(1)); }
inline void Assembler::srawi(   Register a, Register s, int sh5)     { emit_int32(SRAWI_OPCODE  | rta(a) | rs(s) | sh1620(sh5) | rc(0)); }
inline void Assembler::srawi_(  Register a, Register s, int sh5)     { emit_int32(SRAWI_OPCODE  | rta(a) | rs(s) | sh1620(sh5) | rc(1)); }

// extended mnemonics for Shift Instructions
inline void Assembler::sldi(    Register a, Register s, int sh6)     { Assembler::rldicr(a, s, sh6, 63-sh6); }
inline void Assembler::sldi_(   Register a, Register s, int sh6)     { Assembler::rldicr_(a, s, sh6, 63-sh6); }
inline void Assembler::slwi(    Register a, Register s, int sh5)     { Assembler::rlwinm(a, s, sh5, 0, 31-sh5); }
inline void Assembler::slwi_(   Register a, Register s, int sh5)     { Assembler::rlwinm_(a, s, sh5, 0, 31-sh5); }
inline void Assembler::srdi(    Register a, Register s, int sh6)     { Assembler::rldicl(a, s, 64-sh6, sh6); }
inline void Assembler::srdi_(   Register a, Register s, int sh6)     { Assembler::rldicl_(a, s, 64-sh6, sh6); }
inline void Assembler::srwi(    Register a, Register s, int sh5)     { Assembler::rlwinm(a, s, 32-sh5, sh5, 31); }
inline void Assembler::srwi_(   Register a, Register s, int sh5)     { Assembler::rlwinm_(a, s, 32-sh5, sh5, 31); }

inline void Assembler::clrrdi(  Register a, Register s, int ui6)     { Assembler::rldicr(a, s, 0, 63-ui6); }
inline void Assembler::clrrdi_( Register a, Register s, int ui6)     { Assembler::rldicr_(a, s, 0, 63-ui6); }
inline void Assembler::clrldi(  Register a, Register s, int ui6)     { Assembler::rldicl(a, s, 0, ui6); }
inline void Assembler::clrldi_( Register a, Register s, int ui6)     { Assembler::rldicl_(a, s, 0, ui6); }
inline void Assembler::clrlsldi( Register a, Register s, int clrl6, int shl6) { Assembler::rldic( a, s, shl6, clrl6-shl6); }
inline void Assembler::clrlsldi_(Register a, Register s, int clrl6, int shl6) { Assembler::rldic_(a, s, shl6, clrl6-shl6); }
inline void Assembler::extrdi(  Register a, Register s, int n, int b){ Assembler::rldicl(a, s, b+n, 64-n); }
// testbit with condition register.
inline void Assembler::testbitdi(ConditionRegister cr, Register a, Register s, int ui6) {
  if (cr == CCR0) {
    Assembler::rldicr_(a, s, 63-ui6, 0);
  } else {
    Assembler::rldicr(a, s, 63-ui6, 0);
    Assembler::cmpdi(cr, a, 0);
  }
}

// Byte reverse instructions (introduced with Power10)
inline void Assembler::brh(Register a, Register s) { emit_int32(BRH_OPCODE | rta(a) | rs(s)); }
inline void Assembler::brw(Register a, Register s) { emit_int32(BRW_OPCODE | rta(a) | rs(s)); }
inline void Assembler::brd(Register a, Register s) { emit_int32(BRD_OPCODE | rta(a) | rs(s)); }

// rotate instructions
inline void Assembler::rotldi( Register a, Register s, int n) { Assembler::rldicl(a, s, n, 0); }
inline void Assembler::rotrdi( Register a, Register s, int n) { Assembler::rldicl(a, s, 64-n, 0); }
inline void Assembler::rotlwi( Register a, Register s, int n) { Assembler::rlwinm(a, s, n, 0, 31); }
inline void Assembler::rotrwi( Register a, Register s, int n) { Assembler::rlwinm(a, s, 32-n, 0, 31); }

inline void Assembler::rldic(   Register a, Register s, int sh6, int mb6)         { emit_int32(RLDIC_OPCODE  | rta(a) | rs(s) | sh162030(sh6) | mb2126(mb6) | rc(0)); }
inline void Assembler::rldic_(  Register a, Register s, int sh6, int mb6)         { emit_int32(RLDIC_OPCODE  | rta(a) | rs(s) | sh162030(sh6) | mb2126(mb6) | rc(1)); }
inline void Assembler::rldicr(  Register a, Register s, int sh6, int mb6)         { emit_int32(RLDICR_OPCODE | rta(a) | rs(s) | sh162030(sh6) | mb2126(mb6) | rc(0)); }
inline void Assembler::rldicr_( Register a, Register s, int sh6, int mb6)         { emit_int32(RLDICR_OPCODE | rta(a) | rs(s) | sh162030(sh6) | mb2126(mb6) | rc(1)); }
inline void Assembler::rldicl(  Register a, Register s, int sh6, int me6)         { emit_int32(RLDICL_OPCODE | rta(a) | rs(s) | sh162030(sh6) | me2126(me6) | rc(0)); }
inline void Assembler::rldicl_( Register a, Register s, int sh6, int me6)         { emit_int32(RLDICL_OPCODE | rta(a) | rs(s) | sh162030(sh6) | me2126(me6) | rc(1)); }
inline void Assembler::rlwinm(  Register a, Register s, int sh5, int mb5, int me5){ emit_int32(RLWINM_OPCODE | rta(a) | rs(s) | sh1620(sh5) | mb2125(mb5) | me2630(me5) | rc(0)); }
inline void Assembler::rlwinm_( Register a, Register s, int sh5, int mb5, int me5){ emit_int32(RLWINM_OPCODE | rta(a) | rs(s) | sh1620(sh5) | mb2125(mb5) | me2630(me5) | rc(1)); }
inline void Assembler::rldimi(  Register a, Register s, int sh6, int mb6)         { emit_int32(RLDIMI_OPCODE | rta(a) | rs(s) | sh162030(sh6) | mb2126(mb6) | rc(0)); }
inline void Assembler::rlwimi(  Register a, Register s, int sh5, int mb5, int me5){ emit_int32(RLWIMI_OPCODE | rta(a) | rs(s) | sh1620(sh5) | mb2125(mb5) | me2630(me5) | rc(0)); }
inline void Assembler::rldimi_( Register a, Register s, int sh6, int mb6)         { emit_int32(RLDIMI_OPCODE | rta(a) | rs(s) | sh162030(sh6) | mb2126(mb6) | rc(1)); }
inline void Assembler::insrdi(  Register a, Register s, int n,   int b)           { Assembler::rldimi(a, s, 64-(b+n), b); }
inline void Assembler::insrwi(  Register a, Register s, int n,   int b)           { Assembler::rlwimi(a, s, 32-(b+n), b, b+n-1); }

// PPC 1, section 3.3.2 Fixed-Point Load Instructions
inline void Assembler::lwzx( Register d, Register s1, Register s2) { emit_int32(LWZX_OPCODE | rt(d) | ra0mem(s1) | rb(s2));}
inline void Assembler::lwz(  Register d, int si16,    Register s1) { emit_int32(LWZ_OPCODE  | rt(d) | d1(si16)   | ra0mem(s1));}
inline void Assembler::lwzu( Register d, int si16,    Register s1) { assert(d != s1, "according to ibm manual"); emit_int32(LWZU_OPCODE | rt(d) | d1(si16) | rta0mem(s1));}

inline void Assembler::lwax( Register d, Register s1, Register s2) { emit_int32(LWAX_OPCODE | rt(d) | ra0mem(s1) | rb(s2));}
inline void Assembler::lwa(  Register d, int si16,    Register s1) { emit_int32(LWA_OPCODE  | rt(d) | ds(si16)   | ra0mem(s1));}

inline void Assembler::lwbrx( Register d, Register s1, Register s2) { emit_int32(LWBRX_OPCODE | rt(d) | ra0mem(s1) | rb(s2));}

inline void Assembler::lhzx( Register d, Register s1, Register s2) { emit_int32(LHZX_OPCODE | rt(d) | ra0mem(s1) | rb(s2));}
inline void Assembler::lhz(  Register d, int si16,    Register s1) { emit_int32(LHZ_OPCODE  | rt(d) | d1(si16)   | ra0mem(s1));}
inline void Assembler::lhzu( Register d, int si16,    Register s1) { assert(d != s1, "according to ibm manual"); emit_int32(LHZU_OPCODE | rt(d) | d1(si16) | rta0mem(s1));}

inline void Assembler::lhbrx( Register d, Register s1, Register s2) { emit_int32(LHBRX_OPCODE | rt(d) | ra0mem(s1) | rb(s2));}

inline void Assembler::lhax( Register d, Register s1, Register s2) { emit_int32(LHAX_OPCODE | rt(d) | ra0mem(s1) | rb(s2));}
inline void Assembler::lha(  Register d, int si16,    Register s1) { emit_int32(LHA_OPCODE  | rt(d) | d1(si16)   | ra0mem(s1));}
inline void Assembler::lhau( Register d, int si16,    Register s1) { assert(d != s1, "according to ibm manual"); emit_int32(LHAU_OPCODE | rt(d) | d1(si16) | rta0mem(s1));}

inline void Assembler::lbzx( Register d, Register s1, Register s2) { emit_int32(LBZX_OPCODE | rt(d) | ra0mem(s1) | rb(s2));}
inline void Assembler::lbz(  Register d, int si16,    Register s1) { emit_int32(LBZ_OPCODE  | rt(d) | d1(si16)   | ra0mem(s1));}
inline void Assembler::lbzu( Register d, int si16,    Register s1) { assert(d != s1, "according to ibm manual"); emit_int32(LBZU_OPCODE | rt(d) | d1(si16) | rta0mem(s1));}

inline void Assembler::ld(   Register d, int si16,    Register s1) { emit_int32(LD_OPCODE  | rt(d) | ds(si16)   | ra0mem(s1));}
inline void Assembler::ldx(  Register d, Register s1, Register s2) { emit_int32(LDX_OPCODE | rt(d) | ra0mem(s1) | rb(s2));}
inline void Assembler::ldu(  Register d, int si16,    Register s1) { assert(d != s1, "according to ibm manual"); emit_int32(LDU_OPCODE | rt(d) | ds(si16) | rta0mem(s1));}
inline void Assembler::ldbrx( Register d, Register s1, Register s2) { emit_int32(LDBRX_OPCODE | rt(d) | ra0mem(s1) | rb(s2));}

inline void Assembler::ld_ptr(Register d, int b, Register s1) { ld(d, b, s1); }
inline void Assembler::ld_ptr(Register d, ByteSize b, Register s1) { ld(d, in_bytes(b), s1); }

//  PPC 1, section 3.3.3 Fixed-Point Store Instructions
inline void Assembler::stwx( Register d, Register s1, Register s2) { emit_int32(STWX_OPCODE | rs(d) | ra0mem(s1) | rb(s2));}
inline void Assembler::stw(  Register d, int si16,    Register s1) { emit_int32(STW_OPCODE  | rs(d) | d1(si16)   | ra0mem(s1));}
inline void Assembler::stwu( Register d, int si16,    Register s1) { emit_int32(STWU_OPCODE | rs(d) | d1(si16)   | rta0mem(s1));}
inline void Assembler::stwbrx( Register d, Register s1, Register s2) { emit_int32(STWBRX_OPCODE | rs(d) | ra0mem(s1) | rb(s2));}

inline void Assembler::sthx( Register d, Register s1, Register s2) { emit_int32(STHX_OPCODE | rs(d) | ra0mem(s1) | rb(s2));}
inline void Assembler::sth(  Register d, int si16,    Register s1) { emit_int32(STH_OPCODE  | rs(d) | d1(si16)   | ra0mem(s1));}
inline void Assembler::sthu( Register d, int si16,    Register s1) { emit_int32(STHU_OPCODE | rs(d) | d1(si16)   | rta0mem(s1));}
inline void Assembler::sthbrx( Register d, Register s1, Register s2) { emit_int32(STHBRX_OPCODE | rs(d) | ra0mem(s1) | rb(s2));}

inline void Assembler::stbx( Register d, Register s1, Register s2) { emit_int32(STBX_OPCODE | rs(d) | ra0mem(s1) | rb(s2));}
inline void Assembler::stb(  Register d, int si16,    Register s1) { emit_int32(STB_OPCODE  | rs(d) | d1(si16)   | ra0mem(s1));}
inline void Assembler::stbu( Register d, int si16,    Register s1) { emit_int32(STBU_OPCODE | rs(d) | d1(si16)   | rta0mem(s1));}

inline void Assembler::std(  Register d, int si16,    Register s1) { emit_int32(STD_OPCODE  | rs(d) | ds(si16)   | ra0mem(s1));}
inline void Assembler::stdx( Register d, Register s1, Register s2) { emit_int32(STDX_OPCODE | rs(d) | ra0mem(s1) | rb(s2));}
inline void Assembler::stdu( Register d, int si16,    Register s1) { emit_int32(STDU_OPCODE | rs(d) | ds(si16)   | rta0mem(s1));}
inline void Assembler::stdux(Register s, Register a,  Register b)  { emit_int32(STDUX_OPCODE| rs(s) | rta0mem(a) | rb(b));}
inline void Assembler::stdbrx( Register d, Register s1, Register s2) { emit_int32(STDBRX_OPCODE | rs(d) | ra0mem(s1) | rb(s2));}

inline void Assembler::st_ptr(Register d, int b, Register s1) { std(d, b, s1); }
inline void Assembler::st_ptr(Register d, ByteSize b, Register s1) { std(d, in_bytes(b), s1); }

// PPC 1, section 3.3.13 Move To/From System Register Instructions
inline void Assembler::mtlr( Register s1)         { emit_int32(MTLR_OPCODE  | rs(s1)); }
inline void Assembler::mflr( Register d )         { emit_int32(MFLR_OPCODE  | rt(d)); }
inline void Assembler::mtctr(Register s1)         { emit_int32(MTCTR_OPCODE | rs(s1)); }
inline void Assembler::mfctr(Register d )         { emit_int32(MFCTR_OPCODE | rt(d)); }
inline void Assembler::mtcrf(int afxm, Register s){ emit_int32(MTCRF_OPCODE | fxm(afxm) | rs(s)); }
inline void Assembler::mfcr( Register d )         { emit_int32(MFCR_OPCODE  | rt(d)); }
inline void Assembler::mcrf( ConditionRegister crd, ConditionRegister cra)
                                                      { emit_int32(MCRF_OPCODE | bf(crd) | bfa(cra)); }
inline void Assembler::mtcr( Register s)          { Assembler::mtcrf(0xff, s); }
// Introduced in Power 9:
inline void Assembler::mcrxrx(ConditionRegister cra)
                                                  { emit_int32(MCRXRX_OPCODE | bf(cra)); }
inline void Assembler::setb(Register d, ConditionRegister cra)
                                                  { emit_int32(SETB_OPCODE | rt(d) | bfa(cra)); }

inline void Assembler::setbc(Register d, int biint)
                                                  { emit_int32(SETBC_OPCODE | rt(d) | bi(biint)); }
inline void Assembler::setbc(Register d, ConditionRegister cr, Condition cc) {
  setbc(d, bi0(cr, cc));
}
inline void Assembler::setnbc(Register d, int biint)
                                                  { emit_int32(SETNBC_OPCODE | rt(d) | bi(biint)); }
inline void Assembler::setnbc(Register d, ConditionRegister cr, Condition cc) {
  setnbc(d, bi0(cr, cc));
}

// Special purpose registers
// Exception Register
inline void Assembler::mtxer(Register s1)         { emit_int32(MTXER_OPCODE | rs(s1)); }
inline void Assembler::mfxer(Register d )         { emit_int32(MFXER_OPCODE | rt(d)); }
// Vector Register Save Register
inline void Assembler::mtvrsave(Register s1)      { emit_int32(MTVRSAVE_OPCODE | rs(s1)); }
inline void Assembler::mfvrsave(Register d )      { emit_int32(MFVRSAVE_OPCODE | rt(d)); }
// Timebase
inline void Assembler::mftb(Register d )          { emit_int32(MFTB_OPCODE  | rt(d)); }
// Introduced with Power 8:
// Data Stream Control Register
inline void Assembler::mtdscr(Register s1)        { emit_int32(MTDSCR_OPCODE | rs(s1)); }
inline void Assembler::mfdscr(Register d )        { emit_int32(MFDSCR_OPCODE | rt(d)); }
// Transactional Memory Registers
inline void Assembler::mftfhar(Register d )       { emit_int32(MFTFHAR_OPCODE   | rt(d)); }
inline void Assembler::mftfiar(Register d )       { emit_int32(MFTFIAR_OPCODE   | rt(d)); }
inline void Assembler::mftexasr(Register d )      { emit_int32(MFTEXASR_OPCODE  | rt(d)); }
inline void Assembler::mftexasru(Register d )     { emit_int32(MFTEXASRU_OPCODE | rt(d)); }

// SAP JVM 2006-02-13 PPC branch instruction.
// PPC 1, section 2.4.1 Branch Instructions
inline void Assembler::b( address a, relocInfo::relocType rt) { emit_data(BXX_OPCODE| li(disp( intptr_t(a), intptr_t(pc()))) |aa(0)|lk(0), rt); }
inline void Assembler::b( Label& L)                           { b( target(L)); }
inline void Assembler::bl(address a, relocInfo::relocType rt) { emit_data(BXX_OPCODE| li(disp( intptr_t(a), intptr_t(pc()))) |aa(0)|lk(1), rt); }
inline void Assembler::bl(Label& L)                           { bl(target(L)); }
inline void Assembler::bc( int boint, int biint, address a, relocInfo::relocType rt) { emit_data(BCXX_OPCODE| bo(boint) | bi(biint) | bd(disp( intptr_t(a), intptr_t(pc()))) | aa(0) | lk(0), rt); }
inline void Assembler::bc( int boint, int biint, Label& L)                           { bc(boint, biint, target(L)); }
inline void Assembler::bcl(int boint, int biint, address a, relocInfo::relocType rt) { emit_data(BCXX_OPCODE| bo(boint) | bi(biint) | bd(disp( intptr_t(a), intptr_t(pc()))) | aa(0)|lk(1)); }
inline void Assembler::bcl(int boint, int biint, Label& L)                           { bcl(boint, biint, target(L)); }

inline void Assembler::bclr(  int boint, int biint, int bhint, relocInfo::relocType rt) { emit_data(BCLR_OPCODE | bo(boint) | bi(biint) | bh(bhint) | aa(0) | lk(0), rt); }
inline void Assembler::bclrl( int boint, int biint, int bhint, relocInfo::relocType rt) { emit_data(BCLR_OPCODE | bo(boint) | bi(biint) | bh(bhint) | aa(0) | lk(1), rt); }
inline void Assembler::bcctr( int boint, int biint, int bhint, relocInfo::relocType rt) { emit_data(BCCTR_OPCODE| bo(boint) | bi(biint) | bh(bhint) | aa(0) | lk(0), rt); }
inline void Assembler::bcctrl(int boint, int biint, int bhint, relocInfo::relocType rt) { emit_data(BCCTR_OPCODE| bo(boint) | bi(biint) | bh(bhint) | aa(0) | lk(1), rt); }

// helper function for b
inline bool Assembler::is_within_range_of_b(address a, address pc) {
  // Guard against illegal branch targets, e.g. -1 (see CompiledStaticCall and ad-file).
  if ((((uint64_t)a) & 0x3) != 0) return false;

  const int range = 1 << (29-6); // li field is from bit 6 to bit 29.
  int value = disp(intptr_t(a), intptr_t(pc));
  bool result = -range <= value && value < range-1;
#ifdef ASSERT
  if (result) li(value); // Assert that value is in correct range.
#endif
  return result;
}

// helper functions for bcxx.
inline bool Assembler::is_within_range_of_bcxx(address a, address pc) {
  // Guard against illegal branch targets, e.g. -1 (see CompiledStaticCall and ad-file).
  if ((((uint64_t)a) & 0x3) != 0) return false;

  const int range = 1 << (29-16); // bd field is from bit 16 to bit 29.
  int value = disp(intptr_t(a), intptr_t(pc));
  bool result = -range <= value && value < range-1;
#ifdef ASSERT
  if (result) bd(value); // Assert that value is in correct range.
#endif
  return result;
}

// Get the destination of a bxx branch (b, bl, ba, bla).
address  Assembler::bxx_destination(address baddr) { return bxx_destination(*(int*)baddr, baddr); }
address  Assembler::bxx_destination(int instr, address pc) { return (address)bxx_destination_offset(instr, (intptr_t)pc); }
intptr_t Assembler::bxx_destination_offset(int instr, intptr_t bxx_pos) {
  intptr_t displ = inv_li_field(instr);
  return bxx_pos + displ;
}

// Extended mnemonics for Branch Instructions
inline void Assembler::blt(ConditionRegister crx, Label& L) { Assembler::bc(bcondCRbiIs1, bi0(crx, less), L); }
inline void Assembler::bgt(ConditionRegister crx, Label& L) { Assembler::bc(bcondCRbiIs1, bi0(crx, greater), L); }
inline void Assembler::beq(ConditionRegister crx, Label& L) { Assembler::bc(bcondCRbiIs1, bi0(crx, equal), L); }
inline void Assembler::bso(ConditionRegister crx, Label& L) { Assembler::bc(bcondCRbiIs1, bi0(crx, summary_overflow), L); }
inline void Assembler::bge(ConditionRegister crx, Label& L) { Assembler::bc(bcondCRbiIs0, bi0(crx, less), L); }
inline void Assembler::ble(ConditionRegister crx, Label& L) { Assembler::bc(bcondCRbiIs0, bi0(crx, greater), L); }
inline void Assembler::bne(ConditionRegister crx, Label& L) { Assembler::bc(bcondCRbiIs0, bi0(crx, equal), L); }
inline void Assembler::bns(ConditionRegister crx, Label& L) { Assembler::bc(bcondCRbiIs0, bi0(crx, summary_overflow), L); }

// Branch instructions with static prediction hints.
inline void Assembler::blt_predict_taken    (ConditionRegister crx, Label& L) { bc(bcondCRbiIs1_bhintIsTaken,    bi0(crx, less), L); }
inline void Assembler::bgt_predict_taken    (ConditionRegister crx, Label& L) { bc(bcondCRbiIs1_bhintIsTaken,    bi0(crx, greater), L); }
inline void Assembler::beq_predict_taken    (ConditionRegister crx, Label& L) { bc(bcondCRbiIs1_bhintIsTaken,    bi0(crx, equal), L); }
inline void Assembler::bso_predict_taken    (ConditionRegister crx, Label& L) { bc(bcondCRbiIs1_bhintIsTaken,    bi0(crx, summary_overflow), L); }
inline void Assembler::bge_predict_taken    (ConditionRegister crx, Label& L) { bc(bcondCRbiIs0_bhintIsTaken,    bi0(crx, less), L); }
inline void Assembler::ble_predict_taken    (ConditionRegister crx, Label& L) { bc(bcondCRbiIs0_bhintIsTaken,    bi0(crx, greater), L); }
inline void Assembler::bne_predict_taken    (ConditionRegister crx, Label& L) { bc(bcondCRbiIs0_bhintIsTaken,    bi0(crx, equal), L); }
inline void Assembler::bns_predict_taken    (ConditionRegister crx, Label& L) { bc(bcondCRbiIs0_bhintIsTaken,    bi0(crx, summary_overflow), L); }
inline void Assembler::blt_predict_not_taken(ConditionRegister crx, Label& L) { bc(bcondCRbiIs1_bhintIsNotTaken, bi0(crx, less), L); }
inline void Assembler::bgt_predict_not_taken(ConditionRegister crx, Label& L) { bc(bcondCRbiIs1_bhintIsNotTaken, bi0(crx, greater), L); }
inline void Assembler::beq_predict_not_taken(ConditionRegister crx, Label& L) { bc(bcondCRbiIs1_bhintIsNotTaken, bi0(crx, equal), L); }
inline void Assembler::bso_predict_not_taken(ConditionRegister crx, Label& L) { bc(bcondCRbiIs1_bhintIsNotTaken, bi0(crx, summary_overflow), L); }
inline void Assembler::bge_predict_not_taken(ConditionRegister crx, Label& L) { bc(bcondCRbiIs0_bhintIsNotTaken, bi0(crx, less), L); }
inline void Assembler::ble_predict_not_taken(ConditionRegister crx, Label& L) { bc(bcondCRbiIs0_bhintIsNotTaken, bi0(crx, greater), L); }
inline void Assembler::bne_predict_not_taken(ConditionRegister crx, Label& L) { bc(bcondCRbiIs0_bhintIsNotTaken, bi0(crx, equal), L); }
inline void Assembler::bns_predict_not_taken(ConditionRegister crx, Label& L) { bc(bcondCRbiIs0_bhintIsNotTaken, bi0(crx, summary_overflow), L); }

// For use in conjunction with testbitdi:
inline void Assembler::btrue( ConditionRegister crx, Label& L) { Assembler::bne(crx, L); }
inline void Assembler::bfalse(ConditionRegister crx, Label& L) { Assembler::beq(crx, L); }

inline void Assembler::bltl(ConditionRegister crx, Label& L) { Assembler::bcl(bcondCRbiIs1, bi0(crx, less), L); }
inline void Assembler::bgtl(ConditionRegister crx, Label& L) { Assembler::bcl(bcondCRbiIs1, bi0(crx, greater), L); }
inline void Assembler::beql(ConditionRegister crx, Label& L) { Assembler::bcl(bcondCRbiIs1, bi0(crx, equal), L); }
inline void Assembler::bsol(ConditionRegister crx, Label& L) { Assembler::bcl(bcondCRbiIs1, bi0(crx, summary_overflow), L); }
inline void Assembler::bgel(ConditionRegister crx, Label& L) { Assembler::bcl(bcondCRbiIs0, bi0(crx, less), L); }
inline void Assembler::blel(ConditionRegister crx, Label& L) { Assembler::bcl(bcondCRbiIs0, bi0(crx, greater), L); }
inline void Assembler::bnel(ConditionRegister crx, Label& L) { Assembler::bcl(bcondCRbiIs0, bi0(crx, equal), L); }
inline void Assembler::bnsl(ConditionRegister crx, Label& L) { Assembler::bcl(bcondCRbiIs0, bi0(crx, summary_overflow), L); }

// Extended mnemonics for Branch Instructions via LR.
// We use `blr' for returns.
inline void Assembler::blr(relocInfo::relocType rt) { Assembler::bclr(bcondAlways, 0, bhintbhBCLRisReturn, rt); }

// Extended mnemonics for Branch Instructions with CTR.
// Bdnz means `decrement CTR and jump to L if CTR is not zero'.
inline void Assembler::bdnz(Label& L) { Assembler::bc(16, 0, L); }
// Decrement and branch if result is zero.
inline void Assembler::bdz(Label& L)  { Assembler::bc(18, 0, L); }
// We use `bctr[l]' for jumps/calls in function descriptor glue
// code, e.g. for calls to runtime functions.
inline void Assembler::bctr( relocInfo::relocType rt) { Assembler::bcctr(bcondAlways, 0, bhintbhBCCTRisNotReturnButSame, rt); }
inline void Assembler::bctrl(relocInfo::relocType rt) { Assembler::bcctrl(bcondAlways, 0, bhintbhBCCTRisNotReturnButSame, rt); }
// Conditional jumps/branches via CTR.
inline void Assembler::beqctr( ConditionRegister crx, relocInfo::relocType rt) { Assembler::bcctr( bcondCRbiIs1, bi0(crx, equal), bhintbhBCCTRisNotReturnButSame, rt); }
inline void Assembler::beqctrl(ConditionRegister crx, relocInfo::relocType rt) { Assembler::bcctrl(bcondCRbiIs1, bi0(crx, equal), bhintbhBCCTRisNotReturnButSame, rt); }
inline void Assembler::bnectr( ConditionRegister crx, relocInfo::relocType rt) { Assembler::bcctr( bcondCRbiIs0, bi0(crx, equal), bhintbhBCCTRisNotReturnButSame, rt); }
inline void Assembler::bnectrl(ConditionRegister crx, relocInfo::relocType rt) { Assembler::bcctrl(bcondCRbiIs0, bi0(crx, equal), bhintbhBCCTRisNotReturnButSame, rt); }

// condition register logic instructions
inline void Assembler::crand( int d, int s1, int s2) { emit_int32(CRAND_OPCODE  | bt(d) | ba(s1) | bb(s2)); }
inline void Assembler::crnand(int d, int s1, int s2) { emit_int32(CRNAND_OPCODE | bt(d) | ba(s1) | bb(s2)); }
inline void Assembler::cror(  int d, int s1, int s2) { emit_int32(CROR_OPCODE   | bt(d) | ba(s1) | bb(s2)); }
inline void Assembler::crxor( int d, int s1, int s2) { emit_int32(CRXOR_OPCODE  | bt(d) | ba(s1) | bb(s2)); }
inline void Assembler::crnor( int d, int s1, int s2) { emit_int32(CRNOR_OPCODE  | bt(d) | ba(s1) | bb(s2)); }
inline void Assembler::creqv( int d, int s1, int s2) { emit_int32(CREQV_OPCODE  | bt(d) | ba(s1) | bb(s2)); }
inline void Assembler::crandc(int d, int s1, int s2) { emit_int32(CRANDC_OPCODE | bt(d) | ba(s1) | bb(s2)); }
inline void Assembler::crorc( int d, int s1, int s2) { emit_int32(CRORC_OPCODE  | bt(d) | ba(s1) | bb(s2)); }

// More convenient version.
inline void Assembler::crand( ConditionRegister crdst, Condition cdst, ConditionRegister crsrc, Condition csrc) {
  int dst_bit = condition_register_bit(crdst, cdst),
      src_bit = condition_register_bit(crsrc, csrc);
  crand(dst_bit, src_bit, dst_bit);
}
inline void Assembler::crnand(ConditionRegister crdst, Condition cdst, ConditionRegister crsrc, Condition csrc) {
  int dst_bit = condition_register_bit(crdst, cdst),
      src_bit = condition_register_bit(crsrc, csrc);
  crnand(dst_bit, src_bit, dst_bit);
}
inline void Assembler::cror(  ConditionRegister crdst, Condition cdst, ConditionRegister crsrc, Condition csrc) {
  int dst_bit = condition_register_bit(crdst, cdst),
      src_bit = condition_register_bit(crsrc, csrc);
  cror(dst_bit, src_bit, dst_bit);
}
inline void Assembler::crxor( ConditionRegister crdst, Condition cdst, ConditionRegister crsrc, Condition csrc) {
  int dst_bit = condition_register_bit(crdst, cdst),
      src_bit = condition_register_bit(crsrc, csrc);
  crxor(dst_bit, src_bit, dst_bit);
}
inline void Assembler::crnor( ConditionRegister crdst, Condition cdst, ConditionRegister crsrc, Condition csrc) {
  int dst_bit = condition_register_bit(crdst, cdst),
      src_bit = condition_register_bit(crsrc, csrc);
  crnor(dst_bit, src_bit, dst_bit);
}
inline void Assembler::creqv( ConditionRegister crdst, Condition cdst, ConditionRegister crsrc, Condition csrc) {
  int dst_bit = condition_register_bit(crdst, cdst),
      src_bit = condition_register_bit(crsrc, csrc);
  creqv(dst_bit, src_bit, dst_bit);
}
inline void Assembler::crandc(ConditionRegister crdst, Condition cdst, ConditionRegister crsrc, Condition csrc) {
  int dst_bit = condition_register_bit(crdst, cdst),
      src_bit = condition_register_bit(crsrc, csrc);
  crandc(dst_bit, src_bit, dst_bit);
}
inline void Assembler::crorc( ConditionRegister crdst, Condition cdst, ConditionRegister crsrc, Condition csrc) {
  int dst_bit = condition_register_bit(crdst, cdst),
      src_bit = condition_register_bit(crsrc, csrc);
  crorc(dst_bit, src_bit, dst_bit);
}

// Conditional move (>= Power7)
inline void Assembler::isel(Register d, ConditionRegister cr, Condition cc, bool inv, Register a, Register b) {
  if (b == noreg) {
    b = d; // Can be omitted if old value should be kept in "else" case.
  }
  Register first = a;
  Register second = b;
  if (inv) {
    first = b;
    second = a; // exchange
  }
  assert(first != R0, "r0 not allowed");
  isel(d, first, second, bi0(cr, cc));
}
inline void Assembler::isel_0(Register d, ConditionRegister cr, Condition cc, Register b) {
  if (b == noreg) {
    b = d; // Can be omitted if old value should be kept in "else" case.
  }
  isel(d, R0, b, bi0(cr, cc));
}

// PPC 2, section 3.2.1 Instruction Cache Instructions
inline void Assembler::icbi(    Register s1, Register s2)         { emit_int32( ICBI_OPCODE   | ra0mem(s1) | rb(s2)           ); }
// PPC 2, section 3.2.2 Data Cache Instructions
//inline void Assembler::dcba(  Register s1, Register s2)         { emit_int32( DCBA_OPCODE   | ra0mem(s1) | rb(s2)           ); }
inline void Assembler::dcbz(    Register s1, Register s2)         { emit_int32( DCBZ_OPCODE   | ra0mem(s1) | rb(s2)           ); }
inline void Assembler::dcbst(   Register s1, Register s2)         { emit_int32( DCBST_OPCODE  | ra0mem(s1) | rb(s2)           ); }
inline void Assembler::dcbf(    Register s1, Register s2)         { emit_int32( DCBF_OPCODE   | ra0mem(s1) | rb(s2)           ); }
// dcache read hint
inline void Assembler::dcbt(    Register s1, Register s2)         { emit_int32( DCBT_OPCODE   | ra0mem(s1) | rb(s2)           ); }
inline void Assembler::dcbtct(  Register s1, Register s2, int ct) { emit_int32( DCBT_OPCODE   | ra0mem(s1) | rb(s2) | thct(ct)); }
inline void Assembler::dcbtds(  Register s1, Register s2, int ds) { emit_int32( DCBT_OPCODE   | ra0mem(s1) | rb(s2) | thds(ds)); }
// dcache write hint
inline void Assembler::dcbtst(  Register s1, Register s2)         { emit_int32( DCBTST_OPCODE | ra0mem(s1) | rb(s2)           ); }
inline void Assembler::dcbtstct(Register s1, Register s2, int ct) { emit_int32( DCBTST_OPCODE | ra0mem(s1) | rb(s2) | thct(ct)); }

// machine barrier instructions:
inline void Assembler::sync(int a) { emit_int32( SYNC_OPCODE | l910(a)); }
inline void Assembler::sync()      { Assembler::sync(0); }
inline void Assembler::lwsync()    { Assembler::sync(1); }
inline void Assembler::ptesync()   { Assembler::sync(2); }
inline void Assembler::eieio()     { emit_int32( EIEIO_OPCODE); }
inline void Assembler::isync()     { emit_int32( ISYNC_OPCODE); }
inline void Assembler::elemental_membar(int e) { assert(0 < e && e < 16, "invalid encoding"); emit_int32( SYNC_OPCODE | e1215(e)); }

// Wait instructions for polling.
inline void Assembler::wait()    { emit_int32( WAIT_OPCODE); }
inline void Assembler::waitrsv() { emit_int32( WAIT_OPCODE | 1<<(31-10)); } // WC=0b01 >=Power7

// atomics
// Use ra0mem to disallow R0 as base.
inline void Assembler::lbarx_unchecked(Register d, Register a, Register b, int eh1)           { emit_int32( LBARX_OPCODE | rt(d) | ra0mem(a) | rb(b) | eh(eh1)); }
inline void Assembler::lharx_unchecked(Register d, Register a, Register b, int eh1)           { emit_int32( LHARX_OPCODE | rt(d) | ra0mem(a) | rb(b) | eh(eh1)); }
inline void Assembler::lwarx_unchecked(Register d, Register a, Register b, int eh1)           { emit_int32( LWARX_OPCODE | rt(d) | ra0mem(a) | rb(b) | eh(eh1)); }
inline void Assembler::ldarx_unchecked(Register d, Register a, Register b, int eh1)           { emit_int32( LDARX_OPCODE | rt(d) | ra0mem(a) | rb(b) | eh(eh1)); }
inline void Assembler::lqarx_unchecked(Register d, Register a, Register b, int eh1)           { emit_int32( LQARX_OPCODE | rt(d) | ra0mem(a) | rb(b) | eh(eh1)); }
inline bool Assembler::lxarx_hint_exclusive_access()                                          { return VM_Version::has_lxarxeh(); }
inline void Assembler::lbarx( Register d, Register a, Register b, bool hint_exclusive_access) { lbarx_unchecked(d, a, b, (hint_exclusive_access && lxarx_hint_exclusive_access() && UseExtendedLoadAndReserveInstructionsPPC64) ? 1 : 0); }
inline void Assembler::lharx( Register d, Register a, Register b, bool hint_exclusive_access) { lharx_unchecked(d, a, b, (hint_exclusive_access && lxarx_hint_exclusive_access() && UseExtendedLoadAndReserveInstructionsPPC64) ? 1 : 0); }
inline void Assembler::lwarx( Register d, Register a, Register b, bool hint_exclusive_access) { lwarx_unchecked(d, a, b, (hint_exclusive_access && lxarx_hint_exclusive_access() && UseExtendedLoadAndReserveInstructionsPPC64) ? 1 : 0); }
inline void Assembler::ldarx( Register d, Register a, Register b, bool hint_exclusive_access) { ldarx_unchecked(d, a, b, (hint_exclusive_access && lxarx_hint_exclusive_access() && UseExtendedLoadAndReserveInstructionsPPC64) ? 1 : 0); }
inline void Assembler::lqarx( Register d, Register a, Register b, bool hint_exclusive_access) { lqarx_unchecked(d, a, b, (hint_exclusive_access && lxarx_hint_exclusive_access() && UseExtendedLoadAndReserveInstructionsPPC64) ? 1 : 0); }
inline void Assembler::stbcx_(Register s, Register a, Register b)                             { emit_int32( STBCX_OPCODE | rs(s) | ra0mem(a) | rb(b) | rc(1)); }
inline void Assembler::sthcx_(Register s, Register a, Register b)                             { emit_int32( STHCX_OPCODE | rs(s) | ra0mem(a) | rb(b) | rc(1)); }
inline void Assembler::stwcx_(Register s, Register a, Register b)                             { emit_int32( STWCX_OPCODE | rs(s) | ra0mem(a) | rb(b) | rc(1)); }
inline void Assembler::stdcx_(Register s, Register a, Register b)                             { emit_int32( STDCX_OPCODE | rs(s) | ra0mem(a) | rb(b) | rc(1)); }
inline void Assembler::stqcx_(Register s, Register a, Register b)                             { emit_int32( STQCX_OPCODE | rs(s) | ra0mem(a) | rb(b) | rc(1)); }

// Instructions for adjusting thread priority
// for simultaneous multithreading (SMT) on >= POWER5.
inline void Assembler::smt_prio_very_low()    { Assembler::or_unchecked(R31, R31, R31); }
inline void Assembler::smt_prio_low()         { Assembler::or_unchecked(R1,  R1,  R1); }
inline void Assembler::smt_prio_medium_low()  { Assembler::or_unchecked(R6,  R6,  R6); }
inline void Assembler::smt_prio_medium()      { Assembler::or_unchecked(R2,  R2,  R2); }
inline void Assembler::smt_prio_medium_high() { Assembler::or_unchecked(R5,  R5,  R5); }
inline void Assembler::smt_prio_high()        { Assembler::or_unchecked(R3,  R3,  R3); }
// >= Power7
inline void Assembler::smt_yield()            { Assembler::or_unchecked(R27, R27, R27); } // never actually implemented
inline void Assembler::smt_mdoio()            { Assembler::or_unchecked(R29, R29, R29); } // never actually implemetned
inline void Assembler::smt_mdoom()            { Assembler::or_unchecked(R30, R30, R30); } // never actually implemented
// Power8
inline void Assembler::smt_miso()             { Assembler::or_unchecked(R26, R26, R26); } // never actually implemented

inline void Assembler::twi_0(Register a)      { twi_unchecked(0, a, 0);}

// trap instructions
inline void Assembler::tdi_unchecked(int tobits, Register a, int si16){                                     emit_int32( TDI_OPCODE | to(tobits) | ra(a) | si(si16)); }
inline void Assembler::twi_unchecked(int tobits, Register a, int si16){                                     emit_int32( TWI_OPCODE | to(tobits) | ra(a) | si(si16)); }
inline void Assembler::tdi(int tobits, Register a, int si16)          { assert(UseSIGTRAP, "precondition"); tdi_unchecked(tobits, a, si16);                      }
inline void Assembler::twi(int tobits, Register a, int si16)          { assert(UseSIGTRAP, "precondition"); twi_unchecked(tobits, a, si16);                      }
inline void Assembler::td( int tobits, Register a, Register b)        { assert(UseSIGTRAP, "precondition"); emit_int32( TD_OPCODE  | to(tobits) | ra(a) | rb(b)); }
inline void Assembler::tw( int tobits, Register a, Register b)        { assert(UseSIGTRAP, "precondition"); emit_int32( TW_OPCODE  | to(tobits) | ra(a) | rb(b)); }

// FLOATING POINT instructions ppc.
// PPC 1, section 4.6.2 Floating-Point Load Instructions
// Use ra0mem instead of ra in some instructions below.
inline void Assembler::lfs( FloatRegister d, int si16, Register a)   { emit_int32( LFS_OPCODE  | frt(d) | ra0mem(a) | simm(si16,16)); }
inline void Assembler::lfsu(FloatRegister d, int si16, Register a)   { emit_int32( LFSU_OPCODE | frt(d) | ra(a)     | simm(si16,16)); }
inline void Assembler::lfsx(FloatRegister d, Register a, Register b) { emit_int32( LFSX_OPCODE | frt(d) | ra0mem(a) | rb(b)); }
inline void Assembler::lfd( FloatRegister d, int si16, Register a)   { emit_int32( LFD_OPCODE  | frt(d) | ra0mem(a) | simm(si16,16)); }
inline void Assembler::lfdu(FloatRegister d, int si16, Register a)   { emit_int32( LFDU_OPCODE | frt(d) | ra(a)     | simm(si16,16)); }
inline void Assembler::lfdx(FloatRegister d, Register a, Register b) { emit_int32( LFDX_OPCODE | frt(d) | ra0mem(a) | rb(b)); }

// PPC 1, section 4.6.3 Floating-Point Store Instructions
// Use ra0mem instead of ra in some instructions below.
inline void Assembler::stfs( FloatRegister s, int si16, Register a)  { emit_int32( STFS_OPCODE  | frs(s) | ra0mem(a) | simm(si16,16)); }
inline void Assembler::stfsu(FloatRegister s, int si16, Register a)  { emit_int32( STFSU_OPCODE | frs(s) | ra(a)     | simm(si16,16)); }
inline void Assembler::stfsx(FloatRegister s, Register a, Register b){ emit_int32( STFSX_OPCODE | frs(s) | ra0mem(a) | rb(b)); }
inline void Assembler::stfd( FloatRegister s, int si16, Register a)  { emit_int32( STFD_OPCODE  | frs(s) | ra0mem(a) | simm(si16,16)); }
inline void Assembler::stfdu(FloatRegister s, int si16, Register a)  { emit_int32( STFDU_OPCODE | frs(s) | ra(a)     | simm(si16,16)); }
inline void Assembler::stfdx(FloatRegister s, Register a, Register b){ emit_int32( STFDX_OPCODE | frs(s) | ra0mem(a) | rb(b)); }

// PPC 1, section 4.6.4 Floating-Point Move Instructions
inline void Assembler::fmr( FloatRegister d, FloatRegister b) { emit_int32( FMR_OPCODE | frt(d) | frb(b) | rc(0)); }
inline void Assembler::fmr_(FloatRegister d, FloatRegister b) { emit_int32( FMR_OPCODE | frt(d) | frb(b) | rc(1)); }

inline void Assembler::frin( FloatRegister d, FloatRegister b) { emit_int32( FRIN_OPCODE | frt(d) | frb(b) | rc(0)); }
inline void Assembler::frip( FloatRegister d, FloatRegister b) { emit_int32( FRIP_OPCODE | frt(d) | frb(b) | rc(0)); }
inline void Assembler::frim( FloatRegister d, FloatRegister b) { emit_int32( FRIM_OPCODE | frt(d) | frb(b) | rc(0)); }

// These are special Power6 opcodes, reused for "lfdepx" and "stfdepx"
// on Power7.  Do not use.
//inline void Assembler::mffgpr( FloatRegister d, Register b)   { emit_int32( MFFGPR_OPCODE | frt(d) | rb(b) | rc(0)); }
//inline void Assembler::mftgpr( Register d, FloatRegister b)   { emit_int32( MFTGPR_OPCODE | rt(d) | frb(b) | rc(0)); }
// add cmpb and popcntb to detect ppc power version.
inline void Assembler::cmpb(   Register a, Register s, Register b) { guarantee(VM_Version::has_cmpb(), "opcode not supported on this hardware");
                                                                     emit_int32( CMPB_OPCODE    | rta(a) | rs(s) | rb(b) | rc(0)); }
inline void Assembler::popcntb(Register a, Register s)             { guarantee(VM_Version::has_popcntb(), "opcode not supported on this hardware");
                                                                     emit_int32( POPCNTB_OPCODE | rta(a) | rs(s)); };
inline void Assembler::popcntw(Register a, Register s)             { guarantee(VM_Version::has_popcntw(), "opcode not supported on this hardware");
                                                                     emit_int32( POPCNTW_OPCODE | rta(a) | rs(s)); };
inline void Assembler::popcntd(Register a, Register s)             { emit_int32( POPCNTD_OPCODE | rta(a) | rs(s)); };

inline void Assembler::fneg(  FloatRegister d, FloatRegister b) { emit_int32( FNEG_OPCODE  | frt(d) | frb(b) | rc(0)); }
inline void Assembler::fneg_( FloatRegister d, FloatRegister b) { emit_int32( FNEG_OPCODE  | frt(d) | frb(b) | rc(1)); }
inline void Assembler::fabs(  FloatRegister d, FloatRegister b) { emit_int32( FABS_OPCODE  | frt(d) | frb(b) | rc(0)); }
inline void Assembler::fabs_( FloatRegister d, FloatRegister b) { emit_int32( FABS_OPCODE  | frt(d) | frb(b) | rc(1)); }
inline void Assembler::fnabs( FloatRegister d, FloatRegister b) { emit_int32( FNABS_OPCODE | frt(d) | frb(b) | rc(0)); }
inline void Assembler::fnabs_(FloatRegister d, FloatRegister b) { emit_int32( FNABS_OPCODE | frt(d) | frb(b) | rc(1)); }

// PPC 1, section 4.6.5.1 Floating-Point Elementary Arithmetic Instructions
inline void Assembler::fadd(  FloatRegister d, FloatRegister a, FloatRegister b) { emit_int32( FADD_OPCODE  | frt(d) | fra(a) | frb(b) | rc(0)); }
inline void Assembler::fadd_( FloatRegister d, FloatRegister a, FloatRegister b) { emit_int32( FADD_OPCODE  | frt(d) | fra(a) | frb(b) | rc(1)); }
inline void Assembler::fadds( FloatRegister d, FloatRegister a, FloatRegister b) { emit_int32( FADDS_OPCODE | frt(d) | fra(a) | frb(b) | rc(0)); }
inline void Assembler::fadds_(FloatRegister d, FloatRegister a, FloatRegister b) { emit_int32( FADDS_OPCODE | frt(d) | fra(a) | frb(b) | rc(1)); }
inline void Assembler::fsub(  FloatRegister d, FloatRegister a, FloatRegister b) { emit_int32( FSUB_OPCODE  | frt(d) | fra(a) | frb(b) | rc(0)); }
inline void Assembler::fsub_( FloatRegister d, FloatRegister a, FloatRegister b) { emit_int32( FSUB_OPCODE  | frt(d) | fra(a) | frb(b) | rc(1)); }
inline void Assembler::fsubs( FloatRegister d, FloatRegister a, FloatRegister b) { emit_int32( FSUBS_OPCODE | frt(d) | fra(a) | frb(b) | rc(0)); }
inline void Assembler::fsubs_(FloatRegister d, FloatRegister a, FloatRegister b) { emit_int32( FSUBS_OPCODE | frt(d) | fra(a) | frb(b) | rc(1)); }
inline void Assembler::fmul(  FloatRegister d, FloatRegister a, FloatRegister c) { emit_int32( FMUL_OPCODE  | frt(d) | fra(a) | frc(c) | rc(0)); }
inline void Assembler::fmul_( FloatRegister d, FloatRegister a, FloatRegister c) { emit_int32( FMUL_OPCODE  | frt(d) | fra(a) | frc(c) | rc(1)); }
inline void Assembler::fmuls( FloatRegister d, FloatRegister a, FloatRegister c) { emit_int32( FMULS_OPCODE | frt(d) | fra(a) | frc(c) | rc(0)); }
inline void Assembler::fmuls_(FloatRegister d, FloatRegister a, FloatRegister c) { emit_int32( FMULS_OPCODE | frt(d) | fra(a) | frc(c) | rc(1)); }
inline void Assembler::fdiv(  FloatRegister d, FloatRegister a, FloatRegister b) { emit_int32( FDIV_OPCODE  | frt(d) | fra(a) | frb(b) | rc(0)); }
inline void Assembler::fdiv_( FloatRegister d, FloatRegister a, FloatRegister b) { emit_int32( FDIV_OPCODE  | frt(d) | fra(a) | frb(b) | rc(1)); }
inline void Assembler::fdivs( FloatRegister d, FloatRegister a, FloatRegister b) { emit_int32( FDIVS_OPCODE | frt(d) | fra(a) | frb(b) | rc(0)); }
inline void Assembler::fdivs_(FloatRegister d, FloatRegister a, FloatRegister b) { emit_int32( FDIVS_OPCODE | frt(d) | fra(a) | frb(b) | rc(1)); }

// Fused multiply-accumulate instructions.
// WARNING: Use only when rounding between the 2 parts is not desired.
// Some floating point tck tests will fail if used incorrectly.
inline void Assembler::fmadd(   FloatRegister d, FloatRegister a, FloatRegister c, FloatRegister b) { emit_int32( FMADD_OPCODE   | frt(d) | fra(a) | frb(b) | frc(c) | rc(0)); }
inline void Assembler::fmadd_(  FloatRegister d, FloatRegister a, FloatRegister c, FloatRegister b) { emit_int32( FMADD_OPCODE   | frt(d) | fra(a) | frb(b) | frc(c) | rc(1)); }
inline void Assembler::fmadds(  FloatRegister d, FloatRegister a, FloatRegister c, FloatRegister b) { emit_int32( FMADDS_OPCODE  | frt(d) | fra(a) | frb(b) | frc(c) | rc(0)); }
inline void Assembler::fmadds_( FloatRegister d, FloatRegister a, FloatRegister c, FloatRegister b) { emit_int32( FMADDS_OPCODE  | frt(d) | fra(a) | frb(b) | frc(c) | rc(1)); }
inline void Assembler::fmsub(   FloatRegister d, FloatRegister a, FloatRegister c, FloatRegister b) { emit_int32( FMSUB_OPCODE   | frt(d) | fra(a) | frb(b) | frc(c) | rc(0)); }
inline void Assembler::fmsub_(  FloatRegister d, FloatRegister a, FloatRegister c, FloatRegister b) { emit_int32( FMSUB_OPCODE   | frt(d) | fra(a) | frb(b) | frc(c) | rc(1)); }
inline void Assembler::fmsubs(  FloatRegister d, FloatRegister a, FloatRegister c, FloatRegister b) { emit_int32( FMSUBS_OPCODE  | frt(d) | fra(a) | frb(b) | frc(c) | rc(0)); }
inline void Assembler::fmsubs_( FloatRegister d, FloatRegister a, FloatRegister c, FloatRegister b) { emit_int32( FMSUBS_OPCODE  | frt(d) | fra(a) | frb(b) | frc(c) | rc(1)); }
inline void Assembler::fnmadd(  FloatRegister d, FloatRegister a, FloatRegister c, FloatRegister b) { emit_int32( FNMADD_OPCODE  | frt(d) | fra(a) | frb(b) | frc(c) | rc(0)); }
inline void Assembler::fnmadd_( FloatRegister d, FloatRegister a, FloatRegister c, FloatRegister b) { emit_int32( FNMADD_OPCODE  | frt(d) | fra(a) | frb(b) | frc(c) | rc(1)); }
inline void Assembler::fnmadds( FloatRegister d, FloatRegister a, FloatRegister c, FloatRegister b) { emit_int32( FNMADDS_OPCODE | frt(d) | fra(a) | frb(b) | frc(c) | rc(0)); }
inline void Assembler::fnmadds_(FloatRegister d, FloatRegister a, FloatRegister c, FloatRegister b) { emit_int32( FNMADDS_OPCODE | frt(d) | fra(a) | frb(b) | frc(c) | rc(1)); }
inline void Assembler::fnmsub(  FloatRegister d, FloatRegister a, FloatRegister c, FloatRegister b) { emit_int32( FNMSUB_OPCODE  | frt(d) | fra(a) | frb(b) | frc(c) | rc(0)); }
inline void Assembler::fnmsub_( FloatRegister d, FloatRegister a, FloatRegister c, FloatRegister b) { emit_int32( FNMSUB_OPCODE  | frt(d) | fra(a) | frb(b) | frc(c) | rc(1)); }
inline void Assembler::fnmsubs( FloatRegister d, FloatRegister a, FloatRegister c, FloatRegister b) { emit_int32( FNMSUBS_OPCODE | frt(d) | fra(a) | frb(b) | frc(c) | rc(0)); }
inline void Assembler::fnmsubs_(FloatRegister d, FloatRegister a, FloatRegister c, FloatRegister b) { emit_int32( FNMSUBS_OPCODE | frt(d) | fra(a) | frb(b) | frc(c) | rc(1)); }

// PPC 1, section 4.6.6 Floating-Point Rounding and Conversion Instructions
inline void Assembler::frsp(  FloatRegister d, FloatRegister b) { emit_int32( FRSP_OPCODE   | frt(d) | frb(b) | rc(0)); }
inline void Assembler::fctid( FloatRegister d, FloatRegister b) { emit_int32( FCTID_OPCODE  | frt(d) | frb(b) | rc(0)); }
inline void Assembler::fctidz(FloatRegister d, FloatRegister b) { emit_int32( FCTIDZ_OPCODE | frt(d) | frb(b) | rc(0)); }
inline void Assembler::fctiw( FloatRegister d, FloatRegister b) { emit_int32( FCTIW_OPCODE  | frt(d) | frb(b) | rc(0)); }
inline void Assembler::fctiwz(FloatRegister d, FloatRegister b) { emit_int32( FCTIWZ_OPCODE | frt(d) | frb(b) | rc(0)); }
inline void Assembler::fcfid( FloatRegister d, FloatRegister b) { emit_int32( FCFID_OPCODE  | frt(d) | frb(b) | rc(0)); }
inline void Assembler::fcfids(FloatRegister d, FloatRegister b) { guarantee(VM_Version::has_fcfids(), "opcode not supported on this hardware");
                                                                  emit_int32( FCFIDS_OPCODE | frt(d) | frb(b) | rc(0)); }

// PPC 1, section 4.6.7 Floating-Point Compare Instructions
inline void Assembler::fcmpu( ConditionRegister crx, FloatRegister a, FloatRegister b) { emit_int32( FCMPU_OPCODE | bf(crx) | fra(a) | frb(b)); }

// PPC 1, section 5.2.1 Floating-Point Arithmetic Instructions
inline void Assembler::fsqrt( FloatRegister d, FloatRegister b) { guarantee(VM_Version::has_fsqrt(), "opcode not supported on this hardware");
                                                                  emit_int32( FSQRT_OPCODE  | frt(d) | frb(b) | rc(0)); }
inline void Assembler::fsqrts(FloatRegister d, FloatRegister b) { guarantee(VM_Version::has_fsqrts(), "opcode not supported on this hardware");
                                                                  emit_int32( FSQRTS_OPCODE | frt(d) | frb(b) | rc(0)); }

// Vector instructions for >= Power6.
inline void Assembler::lvebx( VectorRegister d, Register s1, Register s2) { emit_int32( LVEBX_OPCODE  | vrt(d) | ra0mem(s1) | rb(s2)); }
inline void Assembler::lvehx( VectorRegister d, Register s1, Register s2) { emit_int32( LVEHX_OPCODE  | vrt(d) | ra0mem(s1) | rb(s2)); }
inline void Assembler::lvewx( VectorRegister d, Register s1, Register s2) { emit_int32( LVEWX_OPCODE  | vrt(d) | ra0mem(s1) | rb(s2)); }
inline void Assembler::lvx(   VectorRegister d, Register s1, Register s2) { emit_int32( LVX_OPCODE    | vrt(d) | ra0mem(s1) | rb(s2)); }
inline void Assembler::lvxl(  VectorRegister d, Register s1, Register s2) { emit_int32( LVXL_OPCODE   | vrt(d) | ra0mem(s1) | rb(s2)); }
inline void Assembler::stvebx(VectorRegister d, Register s1, Register s2) { emit_int32( STVEBX_OPCODE | vrt(d) | ra0mem(s1) | rb(s2)); }
inline void Assembler::stvehx(VectorRegister d, Register s1, Register s2) { emit_int32( STVEHX_OPCODE | vrt(d) | ra0mem(s1) | rb(s2)); }
inline void Assembler::stvewx(VectorRegister d, Register s1, Register s2) { emit_int32( STVEWX_OPCODE | vrt(d) | ra0mem(s1) | rb(s2)); }
inline void Assembler::stvx(  VectorRegister d, Register s1, Register s2) { emit_int32( STVX_OPCODE   | vrt(d) | ra0mem(s1) | rb(s2)); }
inline void Assembler::stvxl( VectorRegister d, Register s1, Register s2) { emit_int32( STVXL_OPCODE  | vrt(d) | ra0mem(s1) | rb(s2)); }
inline void Assembler::lvsl(  VectorRegister d, Register s1, Register s2) { emit_int32( LVSL_OPCODE   | vrt(d) | ra0mem(s1) | rb(s2)); }
inline void Assembler::lvsr(  VectorRegister d, Register s1, Register s2) { emit_int32( LVSR_OPCODE   | vrt(d) | ra0mem(s1) | rb(s2)); }

// Vector-Scalar (VSX) instructions.
inline void Assembler::lxv(     VectorSRegister d, int ui16, Register a)     { assert(is_aligned(ui16, 16), "displacement must be a multiple of 16"); emit_int32( LXV_OPCODE  | vsrt_dq(d) | ra0mem(a) | uimm(ui16, 16)); }
inline void Assembler::stxv(    VectorSRegister d, int ui16, Register a)     { assert(is_aligned(ui16, 16), "displacement must be a multiple of 16"); emit_int32( STXV_OPCODE  | vsrs_dq(d) | ra0mem(a) | uimm(ui16, 16)); }
inline void Assembler::lxvl(    VectorSRegister d, Register s1, Register b)  { emit_int32( LXVL_OPCODE    | vsrt(d) | ra0mem(s1) | rb(b)); }
inline void Assembler::stxvl(   VectorSRegister d, Register s1, Register b)  { emit_int32( STXVL_OPCODE   | vsrt(d) | ra0mem(s1) | rb(b)); }
inline void Assembler::lxvd2x(  VectorSRegister d, Register s1)              { emit_int32( LXVD2X_OPCODE  | vsrt(d) | ra(0) | rb(s1)); }
inline void Assembler::lxvd2x(  VectorSRegister d, Register s1, Register s2) { emit_int32( LXVD2X_OPCODE  | vsrt(d) | ra0mem(s1) | rb(s2)); }
inline void Assembler::stxvd2x( VectorSRegister d, Register s1)              { emit_int32( STXVD2X_OPCODE | vsrs(d) | ra(0) | rb(s1)); }
inline void Assembler::stxvd2x( VectorSRegister d, Register s1, Register s2) { emit_int32( STXVD2X_OPCODE | vsrs(d) | ra0mem(s1) | rb(s2)); }
inline void Assembler::mtvsrd(  VectorSRegister d, Register a)               { emit_int32( MTVSRD_OPCODE  | vsrt(d)  | ra(a)); }
inline void Assembler::mtvsrdd( VectorSRegister d, Register a, Register b)   { emit_int32( MTVSRDD_OPCODE | vsrt(d)  | ra(a) | rb(b)); }
inline void Assembler::mfvsrd(  Register d, VectorSRegister a)               { emit_int32( MFVSRD_OPCODE  | vsrs(a)  | ra(d)); }
inline void Assembler::mtvsrwz( VectorSRegister d, Register a)               { emit_int32( MTVSRWZ_OPCODE | vsrt(d) | ra(a)); }
inline void Assembler::mfvsrwz( Register d, VectorSRegister a)               { emit_int32( MFVSRWZ_OPCODE | vsrs(a) | ra(d)); }
inline void Assembler::xxspltib(VectorSRegister d, int ui8)                  { emit_int32( XXSPLTIB_OPCODE | vsrt(d) | imm8(ui8)); }
inline void Assembler::xxspltw( VectorSRegister d, VectorSRegister b, int ui2)           { emit_int32( XXSPLTW_OPCODE | vsrt(d) | vsrb(b) | xxsplt_uim(uimm(ui2,2))); }
inline void Assembler::xxland(  VectorSRegister d, VectorSRegister a, VectorSRegister b) { emit_int32( XXLAND_OPCODE | vsrt(d) | vsra(a) | vsrb(b)); }
inline void Assembler::xxlor(   VectorSRegister d, VectorSRegister a, VectorSRegister b) { emit_int32( XXLOR_OPCODE  | vsrt(d) | vsra(a) | vsrb(b)); }
inline void Assembler::xxlxor(  VectorSRegister d, VectorSRegister a, VectorSRegister b) { emit_int32( XXLXOR_OPCODE | vsrt(d) | vsra(a) | vsrb(b)); }
inline void Assembler::xxleqv(  VectorSRegister d, VectorSRegister a, VectorSRegister b) { emit_int32( XXLEQV_OPCODE | vsrt(d) | vsra(a) | vsrb(b)); }
inline void Assembler::xxbrd(   VectorSRegister d, VectorSRegister b)                    { emit_int32( XXBRD_OPCODE | vsrt(d) | vsrb(b) ); }
inline void Assembler::xxbrw(   VectorSRegister d, VectorSRegister b)                    { emit_int32( XXBRW_OPCODE | vsrt(d) | vsrb(b) ); }
inline void Assembler::xvdivsp( VectorSRegister d, VectorSRegister a, VectorSRegister b) { emit_int32( XVDIVSP_OPCODE | vsrt(d) | vsra(a) | vsrb(b)); }
inline void Assembler::xvdivdp( VectorSRegister d, VectorSRegister a, VectorSRegister b) { emit_int32( XVDIVDP_OPCODE | vsrt(d) | vsra(a) | vsrb(b)); }
inline void Assembler::xvabssp( VectorSRegister d, VectorSRegister b)                    { emit_int32( XVABSSP_OPCODE | vsrt(d) | vsrb(b)); }
inline void Assembler::xvabsdp( VectorSRegister d, VectorSRegister b)                    { emit_int32( XVABSDP_OPCODE | vsrt(d) | vsrb(b)); }
inline void Assembler::xvnegsp( VectorSRegister d, VectorSRegister b)                    { emit_int32( XVNEGSP_OPCODE | vsrt(d) | vsrb(b)); }
inline void Assembler::xvnegdp( VectorSRegister d, VectorSRegister b)                    { emit_int32( XVNEGDP_OPCODE | vsrt(d) | vsrb(b)); }
inline void Assembler::xvsqrtsp(VectorSRegister d, VectorSRegister b)                    { emit_int32( XVSQRTSP_OPCODE| vsrt(d) | vsrb(b)); }
inline void Assembler::xvsqrtdp(VectorSRegister d, VectorSRegister b)                    { emit_int32( XVSQRTDP_OPCODE| vsrt(d) | vsrb(b)); }
inline void Assembler::xscvdpspn(VectorSRegister d, VectorSRegister b)                   { emit_int32( XSCVDPSPN_OPCODE | vsrt(d) | vsrb(b)); }
inline void Assembler::xvadddp( VectorSRegister d, VectorSRegister a, VectorSRegister b) { emit_int32( XVADDDP_OPCODE | vsrt(d) | vsra(a) | vsrb(b)); }
inline void Assembler::xvsubdp( VectorSRegister d, VectorSRegister a, VectorSRegister b) { emit_int32( XVSUBDP_OPCODE | vsrt(d) | vsra(a) | vsrb(b)); }
inline void Assembler::xvmulsp( VectorSRegister d, VectorSRegister a, VectorSRegister b) { emit_int32( XVMULSP_OPCODE | vsrt(d) | vsra(a) | vsrb(b)); }
inline void Assembler::xvmuldp( VectorSRegister d, VectorSRegister a, VectorSRegister b) { emit_int32( XVMULDP_OPCODE | vsrt(d) | vsra(a) | vsrb(b)); }
inline void Assembler::xvmaddasp( VectorSRegister d, VectorSRegister a, VectorSRegister b) { emit_int32( XVMADDASP_OPCODE  | vsrt(d) | vsra(a) | vsrb(b)); }
inline void Assembler::xvmaddadp( VectorSRegister d, VectorSRegister a, VectorSRegister b) { emit_int32( XVMADDADP_OPCODE  | vsrt(d) | vsra(a) | vsrb(b)); }
inline void Assembler::xvmsubasp( VectorSRegister d, VectorSRegister a, VectorSRegister b) { emit_int32( XVMSUBASP_OPCODE  | vsrt(d) | vsra(a) | vsrb(b)); }
inline void Assembler::xvmsubadp( VectorSRegister d, VectorSRegister a, VectorSRegister b) { emit_int32( XVMSUBADP_OPCODE  | vsrt(d) | vsra(a) | vsrb(b)); }
inline void Assembler::xvnmsubasp(VectorSRegister d, VectorSRegister a, VectorSRegister b) { emit_int32( XVNMSUBASP_OPCODE | vsrt(d) | vsra(a) | vsrb(b)); }
inline void Assembler::xvnmsubadp(VectorSRegister d, VectorSRegister a, VectorSRegister b) { emit_int32( XVNMSUBADP_OPCODE | vsrt(d) | vsra(a) | vsrb(b)); }
inline void Assembler::xvrdpi(    VectorSRegister d, VectorSRegister b)                  { emit_int32( XVRDPI_OPCODE  | vsrt(d) | vsrb(b)); }
inline void Assembler::xvrdpic(   VectorSRegister d, VectorSRegister b)                  { emit_int32( XVRDPIC_OPCODE | vsrt(d) | vsrb(b)); }
inline void Assembler::xvrdpim(   VectorSRegister d, VectorSRegister b)                  { emit_int32( XVRDPIM_OPCODE | vsrt(d) | vsrb(b)); }
inline void Assembler::xvrdpip(   VectorSRegister d, VectorSRegister b)                  { emit_int32( XVRDPIP_OPCODE | vsrt(d) | vsrb(b)); }

inline void Assembler::mtvrd(   VectorRegister d, Register a)               { emit_int32( MTVSRD_OPCODE  | vsrt(d->to_vsr()) | ra(a)); }
inline void Assembler::mfvrd(   Register        a, VectorRegister d)         { emit_int32( MFVSRD_OPCODE  | vsrt(d->to_vsr()) | ra(a)); }
inline void Assembler::mtvrwz(  VectorRegister  d, Register a)               { emit_int32( MTVSRWZ_OPCODE | vsrt(d->to_vsr()) | ra(a)); }
inline void Assembler::mfvrwz(  Register        a, VectorRegister d)         { emit_int32( MFVSRWZ_OPCODE | vsrt(d->to_vsr()) | ra(a)); }
inline void Assembler::xxperm(  VectorSRegister d, VectorSRegister a, VectorSRegister b) { emit_int32( XXPERM_OPCODE  | vsrt(d) | vsra(a) | vsrb(b)); }
inline void Assembler::xxpermdi(VectorSRegister d, VectorSRegister a, VectorSRegister b, int dm) { emit_int32( XXPERMDI_OPCODE | vsrt(d) | vsra(a) | vsrb(b) | vsdm(dm)); }
inline void Assembler::xxmrghw( VectorSRegister d, VectorSRegister a, VectorSRegister b) { emit_int32( XXMRGHW_OPCODE | vsrt(d) | vsra(a) | vsrb(b)); }
inline void Assembler::xxmrglw( VectorSRegister d, VectorSRegister a, VectorSRegister b) { emit_int32( XXMRGHW_OPCODE | vsrt(d) | vsra(a) | vsrb(b)); }
inline void Assembler::xxsel(   VectorSRegister d, VectorSRegister a, VectorSRegister b, VectorSRegister c) { emit_int32( XXSEL_OPCODE | vsrt(d) | vsra(a) | vsrb(b) | vsrc(c)); }

// VSX Extended Mnemonics
inline void Assembler::xxspltd( VectorSRegister d, VectorSRegister a, int x)             { xxpermdi(d, a, a, x ? 3 : 0); }
inline void Assembler::xxmrghd( VectorSRegister d, VectorSRegister a, VectorSRegister b) { xxpermdi(d, a, b, 0); }
inline void Assembler::xxmrgld( VectorSRegister d, VectorSRegister a, VectorSRegister b) { xxpermdi(d, a, b, 3); }
inline void Assembler::xxswapd( VectorSRegister d, VectorSRegister a)                    { xxpermdi(d, a, a, 2); }

// Vector-Scalar (VSX) instructions.
inline void Assembler::mtfprd(  FloatRegister   d, Register a)      { emit_int32( MTVSRD_OPCODE  | frt(d)  | ra(a)); }
inline void Assembler::mtfprwa( FloatRegister   d, Register a)      { emit_int32( MTVSRWA_OPCODE | frt(d)  | ra(a)); }
inline void Assembler::mffprd(  Register        a, FloatRegister d) { emit_int32( MFVSRD_OPCODE  | frt(d)  | ra(a)); }

inline void Assembler::vpkpx(   VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VPKPX_OPCODE   | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vpkshss( VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VPKSHSS_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vpkswss( VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VPKSWSS_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vpkshus( VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VPKSHUS_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vpkswus( VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VPKSWUS_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vpkuhum( VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VPKUHUM_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vpkuwum( VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VPKUWUM_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vpkuhus( VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VPKUHUS_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vpkuwus( VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VPKUWUS_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vupkhpx( VectorRegister d, VectorRegister b)                   { emit_int32( VUPKHPX_OPCODE | vrt(d) | vrb(b)); }
inline void Assembler::vupkhsb( VectorRegister d, VectorRegister b)                   { emit_int32( VUPKHSB_OPCODE | vrt(d) | vrb(b)); }
inline void Assembler::vupkhsh( VectorRegister d, VectorRegister b)                   { emit_int32( VUPKHSH_OPCODE | vrt(d) | vrb(b)); }
inline void Assembler::vupklpx( VectorRegister d, VectorRegister b)                   { emit_int32( VUPKLPX_OPCODE | vrt(d) | vrb(b)); }
inline void Assembler::vupklsb( VectorRegister d, VectorRegister b)                   { emit_int32( VUPKLSB_OPCODE | vrt(d) | vrb(b)); }
inline void Assembler::vupklsh( VectorRegister d, VectorRegister b)                   { emit_int32( VUPKLSH_OPCODE | vrt(d) | vrb(b)); }
inline void Assembler::vmrghb(  VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VMRGHB_OPCODE  | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vmrghw(  VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VMRGHW_OPCODE  | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vmrghh(  VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VMRGHH_OPCODE  | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vmrglb(  VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VMRGLB_OPCODE  | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vmrglw(  VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VMRGLW_OPCODE  | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vmrglh(  VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VMRGLH_OPCODE  | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vsplt(   VectorRegister d, int ui4,          VectorRegister b) { emit_int32( VSPLT_OPCODE   | vrt(d) | vsplt_uim(uimm(ui4,4)) | vrb(b)); }
inline void Assembler::vsplth(  VectorRegister d, int ui3,          VectorRegister b) { emit_int32( VSPLTH_OPCODE  | vrt(d) | vsplt_uim(uimm(ui3,3)) | vrb(b)); }
inline void Assembler::vspltw(  VectorRegister d, int ui2,          VectorRegister b) { emit_int32( VSPLTW_OPCODE  | vrt(d) | vsplt_uim(uimm(ui2,2)) | vrb(b)); }
inline void Assembler::vspltisb(VectorRegister d, int si5)                            { emit_int32( VSPLTISB_OPCODE| vrt(d) | vsplti_sim(simm(si5,5))); }
inline void Assembler::vspltish(VectorRegister d, int si5)                            { emit_int32( VSPLTISH_OPCODE| vrt(d) | vsplti_sim(simm(si5,5))); }
inline void Assembler::vspltisw(VectorRegister d, int si5)                            { emit_int32( VSPLTISW_OPCODE| vrt(d) | vsplti_sim(simm(si5,5))); }
inline void Assembler::vperm(   VectorRegister d, VectorRegister a, VectorRegister b, VectorRegister c){ emit_int32( VPERM_OPCODE | vrt(d) | vra(a) | vrb(b) | vrc(c)); }
inline void Assembler::vpextd(  VectorRegister d, VectorRegister a, VectorRegister b)                  { emit_int32( VPEXTD_OPCODE| vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vsel(    VectorRegister d, VectorRegister a, VectorRegister b, VectorRegister c){ emit_int32( VSEL_OPCODE  | vrt(d) | vra(a) | vrb(b) | vrc(c)); }
inline void Assembler::vsl(     VectorRegister d, VectorRegister a, VectorRegister b)                  { emit_int32( VSL_OPCODE   | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vsldoi(  VectorRegister d, VectorRegister a, VectorRegister b, int ui4)         { emit_int32( VSLDOI_OPCODE| vrt(d) | vra(a) | vrb(b) | vsldoi_shb(uimm(ui4,4))); }
inline void Assembler::vslo(    VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VSLO_OPCODE    | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vsr(     VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VSR_OPCODE     | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vsro(    VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VSRO_OPCODE    | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vaddcuw( VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VADDCUW_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vaddshs( VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VADDSHS_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vaddsbs( VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VADDSBS_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vaddsws( VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VADDSWS_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vaddubm( VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VADDUBM_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vadduwm( VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VADDUWM_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vadduhm( VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VADDUHM_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vaddudm( VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VADDUDM_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vaddubs( VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VADDUBS_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vadduws( VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VADDUWS_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vadduhs( VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VADDUHS_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vaddfp(  VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VADDFP_OPCODE  | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vsubcuw( VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VSUBCUW_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vsubshs( VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VSUBSHS_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vsubsbs( VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VSUBSBS_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vsubsws( VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VSUBSWS_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vsububm( VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VSUBUBM_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vsubuwm( VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VSUBUWM_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vsubuhm( VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VSUBUHM_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vsubudm( VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VSUBUDM_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vsububs( VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VSUBUBS_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vsubuws( VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VSUBUWS_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vsubuhs( VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VSUBUHS_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vsubfp(  VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VSUBFP_OPCODE  | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vmulesb( VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VMULESB_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vmuleub( VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VMULEUB_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vmulesh( VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VMULESH_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vmuleuh( VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VMULEUH_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vmulosb( VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VMULOSB_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vmuloub( VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VMULOUB_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vmulosh( VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VMULOSH_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vmulosw( VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VMULOSW_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vmulouh( VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VMULOUH_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vmuluwm( VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VMULUWM_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vmhaddshs(VectorRegister d,VectorRegister a, VectorRegister b, VectorRegister c) { emit_int32( VMHADDSHS_OPCODE | vrt(d) | vra(a) | vrb(b)| vrc(c)); }
inline void Assembler::vmhraddshs(VectorRegister d,VectorRegister a,VectorRegister b, VectorRegister c) { emit_int32( VMHRADDSHS_OPCODE| vrt(d) | vra(a) | vrb(b)| vrc(c)); }
inline void Assembler::vmladduhm(VectorRegister d,VectorRegister a, VectorRegister b, VectorRegister c) { emit_int32( VMLADDUHM_OPCODE | vrt(d) | vra(a) | vrb(b)| vrc(c)); }
inline void Assembler::vmsubuhm(VectorRegister d, VectorRegister a, VectorRegister b, VectorRegister c) { emit_int32( VMSUBUHM_OPCODE  | vrt(d) | vra(a) | vrb(b)| vrc(c)); }
inline void Assembler::vmsummbm(VectorRegister d, VectorRegister a, VectorRegister b, VectorRegister c) { emit_int32( VMSUMMBM_OPCODE  | vrt(d) | vra(a) | vrb(b)| vrc(c)); }
inline void Assembler::vmsumshm(VectorRegister d, VectorRegister a, VectorRegister b, VectorRegister c) { emit_int32( VMSUMSHM_OPCODE  | vrt(d) | vra(a) | vrb(b)| vrc(c)); }
inline void Assembler::vmsumshs(VectorRegister d, VectorRegister a, VectorRegister b, VectorRegister c) { emit_int32( VMSUMSHS_OPCODE  | vrt(d) | vra(a) | vrb(b)| vrc(c)); }
inline void Assembler::vmsumuhm(VectorRegister d, VectorRegister a, VectorRegister b, VectorRegister c) { emit_int32( VMSUMUHM_OPCODE  | vrt(d) | vra(a) | vrb(b)| vrc(c)); }
inline void Assembler::vmsumuhs(VectorRegister d, VectorRegister a, VectorRegister b, VectorRegister c) { emit_int32( VMSUMUHS_OPCODE  | vrt(d) | vra(a) | vrb(b)| vrc(c)); }
inline void Assembler::vmaddfp( VectorRegister d, VectorRegister a, VectorRegister b, VectorRegister c) { emit_int32( VMADDFP_OPCODE   | vrt(d) | vra(a) | vrb(b)| vrc(c)); }
inline void Assembler::vsumsws( VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VSUMSWS_OPCODE  | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vsum2sws(VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VSUM2SWS_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vsum4sbs(VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VSUM4SBS_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vsum4ubs(VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VSUM4UBS_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vsum4shs(VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VSUM4SHS_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vavgsb(  VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VAVGSB_OPCODE   | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vavgsw(  VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VAVGSW_OPCODE   | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vavgsh(  VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VAVGSH_OPCODE   | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vavgub(  VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VAVGUB_OPCODE   | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vavguw(  VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VAVGUW_OPCODE   | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vavguh(  VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VAVGUH_OPCODE   | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vmaxsb(  VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VMAXSB_OPCODE   | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vmaxsw(  VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VMAXSW_OPCODE   | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vmaxsh(  VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VMAXSH_OPCODE   | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vmaxub(  VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VMAXUB_OPCODE   | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vmaxuw(  VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VMAXUW_OPCODE   | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vmaxuh(  VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VMAXUH_OPCODE   | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vminsb(  VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VMINSB_OPCODE   | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vminsw(  VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VMINSW_OPCODE   | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vminsh(  VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VMINSH_OPCODE   | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vminub(  VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VMINUB_OPCODE   | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vminuw(  VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VMINUW_OPCODE   | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vminuh(  VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VMINUH_OPCODE   | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vcmpequb(VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VCMPEQUB_OPCODE | vrt(d) | vra(a) | vrb(b) | vcmp_rc(0)); }
inline void Assembler::vcmpequh(VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VCMPEQUH_OPCODE | vrt(d) | vra(a) | vrb(b) | vcmp_rc(0)); }
inline void Assembler::vcmpequw(VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VCMPEQUW_OPCODE | vrt(d) | vra(a) | vrb(b) | vcmp_rc(0)); }
inline void Assembler::vcmpgtsh(VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VCMPGTSH_OPCODE | vrt(d) | vra(a) | vrb(b) | vcmp_rc(0)); }
inline void Assembler::vcmpgtsb(VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VCMPGTSB_OPCODE | vrt(d) | vra(a) | vrb(b) | vcmp_rc(0)); }
inline void Assembler::vcmpgtsw(VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VCMPGTSW_OPCODE | vrt(d) | vra(a) | vrb(b) | vcmp_rc(0)); }
inline void Assembler::vcmpgtub(VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VCMPGTUB_OPCODE | vrt(d) | vra(a) | vrb(b) | vcmp_rc(0)); }
inline void Assembler::vcmpgtuh(VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VCMPGTUH_OPCODE | vrt(d) | vra(a) | vrb(b) | vcmp_rc(0)); }
inline void Assembler::vcmpgtuw(VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VCMPGTUW_OPCODE | vrt(d) | vra(a) | vrb(b) | vcmp_rc(0)); }
inline void Assembler::vcmpequb_(VectorRegister d,VectorRegister a, VectorRegister b) { emit_int32( VCMPEQUB_OPCODE | vrt(d) | vra(a) | vrb(b) | vcmp_rc(1)); }
inline void Assembler::vcmpequh_(VectorRegister d,VectorRegister a, VectorRegister b) { emit_int32( VCMPEQUH_OPCODE | vrt(d) | vra(a) | vrb(b) | vcmp_rc(1)); }
inline void Assembler::vcmpequw_(VectorRegister d,VectorRegister a, VectorRegister b) { emit_int32( VCMPEQUW_OPCODE | vrt(d) | vra(a) | vrb(b) | vcmp_rc(1)); }
inline void Assembler::vcmpgtsh_(VectorRegister d,VectorRegister a, VectorRegister b) { emit_int32( VCMPGTSH_OPCODE | vrt(d) | vra(a) | vrb(b) | vcmp_rc(1)); }
inline void Assembler::vcmpgtsb_(VectorRegister d,VectorRegister a, VectorRegister b) { emit_int32( VCMPGTSB_OPCODE | vrt(d) | vra(a) | vrb(b) | vcmp_rc(1)); }
inline void Assembler::vcmpgtsw_(VectorRegister d,VectorRegister a, VectorRegister b) { emit_int32( VCMPGTSW_OPCODE | vrt(d) | vra(a) | vrb(b) | vcmp_rc(1)); }
inline void Assembler::vcmpgtub_(VectorRegister d,VectorRegister a, VectorRegister b) { emit_int32( VCMPGTUB_OPCODE | vrt(d) | vra(a) | vrb(b) | vcmp_rc(1)); }
inline void Assembler::vcmpgtuh_(VectorRegister d,VectorRegister a, VectorRegister b) { emit_int32( VCMPGTUH_OPCODE | vrt(d) | vra(a) | vrb(b) | vcmp_rc(1)); }
inline void Assembler::vcmpgtuw_(VectorRegister d,VectorRegister a, VectorRegister b) { emit_int32( VCMPGTUW_OPCODE | vrt(d) | vra(a) | vrb(b) | vcmp_rc(1)); }
inline void Assembler::vand(    VectorRegister d, VectorRegister a, VectorRegister b) { guarantee(VM_Version::has_vand(), "opcode not supported on this hardware");
                                                                                        emit_int32( VAND_OPCODE     | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vandc(   VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VANDC_OPCODE    | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vnor(    VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VNOR_OPCODE     | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vor(     VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VOR_OPCODE      | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vmr(     VectorRegister d, VectorRegister a)                   { emit_int32( VOR_OPCODE      | vrt(d) | vra(a) | vrb(a)); }
inline void Assembler::vxor(    VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VXOR_OPCODE     | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vrld(    VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VRLD_OPCODE     | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vrlb(    VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VRLB_OPCODE     | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vrlw(    VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VRLW_OPCODE     | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vrlh(    VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VRLH_OPCODE     | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vslb(    VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VSLB_OPCODE     | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vskw(    VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VSKW_OPCODE     | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vslh(    VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VSLH_OPCODE     | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vsrb(    VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VSRB_OPCODE     | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vsrw(    VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VSRW_OPCODE     | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vsrh(    VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VSRH_OPCODE     | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vsrab(   VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VSRAB_OPCODE    | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vsraw(   VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VSRAW_OPCODE    | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vsrah(   VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VSRAH_OPCODE    | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vpopcntw(VectorRegister d, VectorRegister b)                   { emit_int32( VPOPCNTW_OPCODE | vrt(d) | vrb(b)); }
inline void Assembler::mtvscr(  VectorRegister b)                                     { emit_int32( MTVSCR_OPCODE   | vrb(b)); }
inline void Assembler::mfvscr(  VectorRegister d)                                     { emit_int32( MFVSCR_OPCODE   | vrt(d)); }

// AES (introduced with Power 8)
inline void Assembler::vcipher(     VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VCIPHER_OPCODE      | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vcipherlast( VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VCIPHERLAST_OPCODE  | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vncipher(    VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VNCIPHER_OPCODE     | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vncipherlast(VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VNCIPHERLAST_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vsbox(       VectorRegister d, VectorRegister a)                   { emit_int32( VSBOX_OPCODE        | vrt(d) | vra(a)         ); }

// SHA (introduced with Power 8)
inline void Assembler::vshasigmad(VectorRegister d, VectorRegister a, bool st, int six) { emit_int32( VSHASIGMAD_OPCODE | vrt(d) | vra(a) | vst(st) | vsix(six)); }
inline void Assembler::vshasigmaw(VectorRegister d, VectorRegister a, bool st, int six) { emit_int32( VSHASIGMAW_OPCODE | vrt(d) | vra(a) | vst(st) | vsix(six)); }

// Vector Binary Polynomial Multiplication (introduced with Power 8)
inline void Assembler::vpmsumb(  VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VPMSUMB_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vpmsumd(  VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VPMSUMD_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vpmsumh(  VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VPMSUMH_OPCODE | vrt(d) | vra(a) | vrb(b)); }
inline void Assembler::vpmsumw(  VectorRegister d, VectorRegister a, VectorRegister b) { emit_int32( VPMSUMW_OPCODE | vrt(d) | vra(a) | vrb(b)); }

// Vector Permute and Xor (introduced with Power 8)
inline void Assembler::vpermxor( VectorRegister d, VectorRegister a, VectorRegister b, VectorRegister c) { emit_int32( VPERMXOR_OPCODE | vrt(d) | vra(a) | vrb(b) | vrc(c)); }

// Transactional Memory instructions (introduced with Power 8)
inline void Assembler::tbegin_()                                { emit_int32( TBEGIN_OPCODE | rc(1)); }
inline void Assembler::tbeginrot_()                             { emit_int32( TBEGIN_OPCODE | /*R=1*/ 1u << (31-10) | rc(1)); }
inline void Assembler::tend_()                                  { emit_int32( TEND_OPCODE | rc(1)); }
inline void Assembler::tendall_()                               { emit_int32( TEND_OPCODE | /*A=1*/ 1u << (31-6) | rc(1)); }
inline void Assembler::tabort_()                                { emit_int32( TABORT_OPCODE | rc(1)); }
inline void Assembler::tabort_(Register a)                      { assert(a != R0, "r0 not allowed"); emit_int32( TABORT_OPCODE | ra(a) | rc(1)); }
inline void Assembler::tabortwc_(int t, Register a, Register b) { emit_int32( TABORTWC_OPCODE | to(t) | ra(a) | rb(b) | rc(1)); }
inline void Assembler::tabortwci_(int t, Register a, int si)    { emit_int32( TABORTWCI_OPCODE | to(t) | ra(a) | sh1620(si) | rc(1)); }
inline void Assembler::tabortdc_(int t, Register a, Register b) { emit_int32( TABORTDC_OPCODE | to(t) | ra(a) | rb(b) | rc(1)); }
inline void Assembler::tabortdci_(int t, Register a, int si)    { emit_int32( TABORTDCI_OPCODE | to(t) | ra(a) | sh1620(si) | rc(1)); }
inline void Assembler::tsuspend_()                              { emit_int32( TSR_OPCODE | rc(1)); }
inline void Assembler::tresume_()                               { emit_int32( TSR_OPCODE | /*L=1*/ 1u << (31-10) | rc(1)); }
inline void Assembler::tcheck(int f)                            { emit_int32( TCHECK_OPCODE | bf(f)); }

// Deliver A Random Number (introduced with POWER9)
inline void Assembler::darn(Register d, int l /* =1 */) { emit_int32( DARN_OPCODE | rt(d) | l14(l)); }

// ra0 version
inline void Assembler::lwzx( Register d, Register s2) { emit_int32( LWZX_OPCODE | rt(d) | rb(s2));}
inline void Assembler::lwz(  Register d, int si16   ) { emit_int32( LWZ_OPCODE  | rt(d) | d1(si16));}
inline void Assembler::lwax( Register d, Register s2) { emit_int32( LWAX_OPCODE | rt(d) | rb(s2));}
inline void Assembler::lwa(  Register d, int si16   ) { emit_int32( LWA_OPCODE  | rt(d) | ds(si16));}
inline void Assembler::lwbrx(Register d, Register s2) { emit_int32( LWBRX_OPCODE| rt(d) | rb(s2));}
inline void Assembler::lhzx( Register d, Register s2) { emit_int32( LHZX_OPCODE | rt(d) | rb(s2));}
inline void Assembler::lhz(  Register d, int si16   ) { emit_int32( LHZ_OPCODE  | rt(d) | d1(si16));}
inline void Assembler::lhax( Register d, Register s2) { emit_int32( LHAX_OPCODE | rt(d) | rb(s2));}
inline void Assembler::lha(  Register d, int si16   ) { emit_int32( LHA_OPCODE  | rt(d) | d1(si16));}
inline void Assembler::lhbrx(Register d, Register s2) { emit_int32( LHBRX_OPCODE| rt(d) | rb(s2));}
inline void Assembler::lbzx( Register d, Register s2) { emit_int32( LBZX_OPCODE | rt(d) | rb(s2));}
inline void Assembler::lbz(  Register d, int si16   ) { emit_int32( LBZ_OPCODE  | rt(d) | d1(si16));}
inline void Assembler::ld(   Register d, int si16   ) { emit_int32( LD_OPCODE   | rt(d) | ds(si16));}
inline void Assembler::ldx(  Register d, Register s2) { emit_int32( LDX_OPCODE  | rt(d) | rb(s2));}
inline void Assembler::ldbrx(Register d, Register s2) { emit_int32( LDBRX_OPCODE| rt(d) | rb(s2));}
inline void Assembler::stwx( Register d, Register s2) { emit_int32( STWX_OPCODE | rs(d) | rb(s2));}
inline void Assembler::stw(  Register d, int si16   ) { emit_int32( STW_OPCODE  | rs(d) | d1(si16));}
inline void Assembler::stwbrx(Register d, Register s2){ emit_int32(STWBRX_OPCODE| rs(d) | rb(s2));}
inline void Assembler::sthx( Register d, Register s2) { emit_int32( STHX_OPCODE | rs(d) | rb(s2));}
inline void Assembler::sth(  Register d, int si16   ) { emit_int32( STH_OPCODE  | rs(d) | d1(si16));}
inline void Assembler::sthbrx(Register d, Register s2){ emit_int32(STHBRX_OPCODE| rs(d) | rb(s2));}
inline void Assembler::stbx( Register d, Register s2) { emit_int32( STBX_OPCODE | rs(d) | rb(s2));}
inline void Assembler::stb(  Register d, int si16   ) { emit_int32( STB_OPCODE  | rs(d) | d1(si16));}
inline void Assembler::std(  Register d, int si16   ) { emit_int32( STD_OPCODE  | rs(d) | ds(si16));}
inline void Assembler::stdx( Register d, Register s2) { emit_int32( STDX_OPCODE | rs(d) | rb(s2));}
inline void Assembler::stdbrx(Register d, Register s2){ emit_int32(STDBRX_OPCODE| rs(d) | rb(s2));}

// ra0 version
inline void Assembler::icbi(    Register s2)          { emit_int32( ICBI_OPCODE   | rb(s2)           ); }
//inline void Assembler::dcba(  Register s2)          { emit_int32( DCBA_OPCODE   | rb(s2)           ); }
inline void Assembler::dcbz(    Register s2)          { emit_int32( DCBZ_OPCODE   | rb(s2)           ); }
inline void Assembler::dcbst(   Register s2)          { emit_int32( DCBST_OPCODE  | rb(s2)           ); }
inline void Assembler::dcbf(    Register s2)          { emit_int32( DCBF_OPCODE   | rb(s2)           ); }
inline void Assembler::dcbt(    Register s2)          { emit_int32( DCBT_OPCODE   | rb(s2)           ); }
inline void Assembler::dcbtct(  Register s2, int ct)  { emit_int32( DCBT_OPCODE   | rb(s2) | thct(ct)); }
inline void Assembler::dcbtds(  Register s2, int ds)  { emit_int32( DCBT_OPCODE   | rb(s2) | thds(ds)); }
inline void Assembler::dcbtst(  Register s2)          { emit_int32( DCBTST_OPCODE | rb(s2)           ); }
inline void Assembler::dcbtstct(Register s2, int ct)  { emit_int32( DCBTST_OPCODE | rb(s2) | thct(ct)); }

// ra0 version
inline void Assembler::lbarx_unchecked(Register d, Register b, int eh1)          { emit_int32( LBARX_OPCODE | rt(d) | rb(b) | eh(eh1)); }
inline void Assembler::lharx_unchecked(Register d, Register b, int eh1)          { emit_int32( LHARX_OPCODE | rt(d) | rb(b) | eh(eh1)); }
inline void Assembler::lwarx_unchecked(Register d, Register b, int eh1)          { emit_int32( LWARX_OPCODE | rt(d) | rb(b) | eh(eh1)); }
inline void Assembler::ldarx_unchecked(Register d, Register b, int eh1)          { emit_int32( LDARX_OPCODE | rt(d) | rb(b) | eh(eh1)); }
inline void Assembler::lqarx_unchecked(Register d, Register b, int eh1)          { emit_int32( LQARX_OPCODE | rt(d) | rb(b) | eh(eh1)); }
inline void Assembler::lbarx( Register d, Register b, bool hint_exclusive_access){ lbarx_unchecked(d, b, (hint_exclusive_access && lxarx_hint_exclusive_access() && UseExtendedLoadAndReserveInstructionsPPC64) ? 1 : 0); }
inline void Assembler::lharx( Register d, Register b, bool hint_exclusive_access){ lharx_unchecked(d, b, (hint_exclusive_access && lxarx_hint_exclusive_access() && UseExtendedLoadAndReserveInstructionsPPC64) ? 1 : 0); }
inline void Assembler::lwarx( Register d, Register b, bool hint_exclusive_access){ lwarx_unchecked(d, b, (hint_exclusive_access && lxarx_hint_exclusive_access() && UseExtendedLoadAndReserveInstructionsPPC64) ? 1 : 0); }
inline void Assembler::ldarx( Register d, Register b, bool hint_exclusive_access){ ldarx_unchecked(d, b, (hint_exclusive_access && lxarx_hint_exclusive_access() && UseExtendedLoadAndReserveInstructionsPPC64) ? 1 : 0); }
inline void Assembler::lqarx( Register d, Register b, bool hint_exclusive_access){ lqarx_unchecked(d, b, (hint_exclusive_access && lxarx_hint_exclusive_access() && UseExtendedLoadAndReserveInstructionsPPC64) ? 1 : 0); }
inline void Assembler::stbcx_(Register s, Register b)                            { emit_int32( STBCX_OPCODE | rs(s) | rb(b) | rc(1)); }
inline void Assembler::sthcx_(Register s, Register b)                            { emit_int32( STHCX_OPCODE | rs(s) | rb(b) | rc(1)); }
inline void Assembler::stwcx_(Register s, Register b)                            { emit_int32( STWCX_OPCODE | rs(s) | rb(b) | rc(1)); }
inline void Assembler::stdcx_(Register s, Register b)                            { emit_int32( STDCX_OPCODE | rs(s) | rb(b) | rc(1)); }
inline void Assembler::stqcx_(Register s, Register b)                            { emit_int32( STQCX_OPCODE | rs(s) | rb(b) | rc(1)); }

// ra0 version
inline void Assembler::lfs( FloatRegister d, int si16)   { emit_int32( LFS_OPCODE  | frt(d) | simm(si16,16)); }
inline void Assembler::lfsx(FloatRegister d, Register b) { emit_int32( LFSX_OPCODE | frt(d) | rb(b)); }
inline void Assembler::lfd( FloatRegister d, int si16)   { emit_int32( LFD_OPCODE  | frt(d) | simm(si16,16)); }
inline void Assembler::lfdx(FloatRegister d, Register b) { emit_int32( LFDX_OPCODE | frt(d) | rb(b)); }

// ra0 version
inline void Assembler::stfs( FloatRegister s, int si16)   { emit_int32( STFS_OPCODE  | frs(s) | simm(si16, 16)); }
inline void Assembler::stfsx(FloatRegister s, Register b) { emit_int32( STFSX_OPCODE | frs(s) | rb(b)); }
inline void Assembler::stfd( FloatRegister s, int si16)   { emit_int32( STFD_OPCODE  | frs(s) | simm(si16, 16)); }
inline void Assembler::stfdx(FloatRegister s, Register b) { emit_int32( STFDX_OPCODE | frs(s) | rb(b)); }

// ra0 version
inline void Assembler::lvebx( VectorRegister d, Register s2) { emit_int32( LVEBX_OPCODE  | vrt(d) | rb(s2)); }
inline void Assembler::lvehx( VectorRegister d, Register s2) { emit_int32( LVEHX_OPCODE  | vrt(d) | rb(s2)); }
inline void Assembler::lvewx( VectorRegister d, Register s2) { emit_int32( LVEWX_OPCODE  | vrt(d) | rb(s2)); }
inline void Assembler::lvx(   VectorRegister d, Register s2) { emit_int32( LVX_OPCODE    | vrt(d) | rb(s2)); }
inline void Assembler::lvxl(  VectorRegister d, Register s2) { emit_int32( LVXL_OPCODE   | vrt(d) | rb(s2)); }
inline void Assembler::stvebx(VectorRegister d, Register s2) { emit_int32( STVEBX_OPCODE | vrt(d) | rb(s2)); }
inline void Assembler::stvehx(VectorRegister d, Register s2) { emit_int32( STVEHX_OPCODE | vrt(d) | rb(s2)); }
inline void Assembler::stvewx(VectorRegister d, Register s2) { emit_int32( STVEWX_OPCODE | vrt(d) | rb(s2)); }
inline void Assembler::stvx(  VectorRegister d, Register s2) { emit_int32( STVX_OPCODE   | vrt(d) | rb(s2)); }
inline void Assembler::stvxl( VectorRegister d, Register s2) { emit_int32( STVXL_OPCODE  | vrt(d) | rb(s2)); }
inline void Assembler::lvsl(  VectorRegister d, Register s2) { emit_int32( LVSL_OPCODE   | vrt(d) | rb(s2)); }
inline void Assembler::lvsr(  VectorRegister d, Register s2) { emit_int32( LVSR_OPCODE   | vrt(d) | rb(s2)); }

inline void Assembler::load_perm(VectorRegister perm, Register addr) {
#if defined(VM_LITTLE_ENDIAN)
  lvsr(perm, addr);
#else
  lvsl(perm, addr);
#endif
}

inline void Assembler::vec_perm(VectorRegister first_dest, VectorRegister second, VectorRegister perm) {
#if defined(VM_LITTLE_ENDIAN)
  vperm(first_dest, second, first_dest, perm);
#else
  vperm(first_dest, first_dest, second, perm);
#endif
}

inline void Assembler::vec_perm(VectorRegister dest, VectorRegister first, VectorRegister second, VectorRegister perm) {
#if defined(VM_LITTLE_ENDIAN)
  vperm(dest, second, first, perm);
#else
  vperm(dest, first, second, perm);
#endif
}

inline void Assembler::load_const(Register d, void* x, Register tmp) {
   load_const(d, (long)x, tmp);
}

// Load a 64 bit constant encoded by a `Label'. This works for bound
// labels as well as unbound ones. For unbound labels, the code will
// be patched as soon as the label gets bound.
inline void Assembler::load_const(Register d, Label& L, Register tmp) {
  load_const(d, target(L), tmp);
}

// Load a 64 bit constant encoded by an AddressLiteral. patchable.
inline void Assembler::load_const(Register d, AddressLiteral& a, Register tmp) {
  // First relocate (we don't change the offset in the RelocationHolder,
  // just pass a.rspec()), then delegate to load_const(Register, long).
  relocate(a.rspec());
  load_const(d, (long)a.value(), tmp);
}

inline void Assembler::load_const32(Register d, int i) {
  lis(d, i >> 16);
  ori(d, d, i & 0xFFFF);
}

#endif // CPU_PPC_ASSEMBLER_PPC_INLINE_HPP
