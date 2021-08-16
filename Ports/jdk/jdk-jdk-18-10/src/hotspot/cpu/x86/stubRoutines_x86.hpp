/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef CPU_X86_STUBROUTINES_X86_HPP
#define CPU_X86_STUBROUTINES_X86_HPP

// This file holds the platform specific parts of the StubRoutines
// definition. See stubRoutines.hpp for a description on how to
// extend it.

static bool returns_to_call_stub(address return_pc) { return return_pc == _call_stub_return_address; }

enum platform_dependent_constants {
  code_size1 = 20000 LP64_ONLY(+10000),         // simply increase if too small (assembler will crash if too small)
  code_size2 = 35300 LP64_ONLY(+25000)          // simply increase if too small (assembler will crash if too small)
};

class x86 {
 friend class StubGenerator;
 friend class VMStructs;

#ifdef _LP64
 private:
  static address _get_previous_sp_entry;

  static address _f2i_fixup;
  static address _f2l_fixup;
  static address _d2i_fixup;
  static address _d2l_fixup;

  static address _float_sign_mask;
  static address _float_sign_flip;
  static address _double_sign_mask;
  static address _double_sign_flip;

 public:

  static address get_previous_sp_entry() {
    return _get_previous_sp_entry;
  }

  static address f2i_fixup() {
    return _f2i_fixup;
  }

  static address f2l_fixup() {
    return _f2l_fixup;
  }

  static address d2i_fixup() {
    return _d2i_fixup;
  }

  static address d2l_fixup() {
    return _d2l_fixup;
  }

  static address float_sign_mask() {
    return _float_sign_mask;
  }

  static address float_sign_flip() {
    return _float_sign_flip;
  }

  static address double_sign_mask() {
    return _double_sign_mask;
  }

  static address double_sign_flip() {
    return _double_sign_flip;
  }

#else // !LP64

 private:
  static address _verify_fpu_cntrl_wrd_entry;
  static address _d2i_wrapper;
  static address _d2l_wrapper;

  static jint    _fpu_cntrl_wrd_std;
  static jint    _fpu_cntrl_wrd_24;
  static jint    _fpu_cntrl_wrd_trunc;

  static jint    _fpu_subnormal_bias1[3];
  static jint    _fpu_subnormal_bias2[3];

 public:
  static address verify_fpu_cntrl_wrd_entry() { return _verify_fpu_cntrl_wrd_entry; }
  static address d2i_wrapper()                { return _d2i_wrapper; }
  static address d2l_wrapper()                { return _d2l_wrapper; }
  static address addr_fpu_cntrl_wrd_std()     { return (address)&_fpu_cntrl_wrd_std;   }
  static address addr_fpu_cntrl_wrd_24()      { return (address)&_fpu_cntrl_wrd_24;    }
  static address addr_fpu_cntrl_wrd_trunc()   { return (address)&_fpu_cntrl_wrd_trunc; }
  static address addr_fpu_subnormal_bias1()   { return (address)&_fpu_subnormal_bias1; }
  static address addr_fpu_subnormal_bias2()   { return (address)&_fpu_subnormal_bias2; }

  static jint    fpu_cntrl_wrd_std()          { return _fpu_cntrl_wrd_std; }
#endif // !LP64

 private:
  static jint    _mxcsr_std;

  static address _verify_mxcsr_entry;
  // shuffle mask for fixing up 128-bit words consisting of big-endian 32-bit integers
  static address _key_shuffle_mask_addr;

  //shuffle mask for big-endian 128-bit integers
  static address _counter_shuffle_mask_addr;

  static address _method_entry_barrier;

  // masks and table for CRC32
  static uint64_t _crc_by128_masks[];
  static juint    _crc_table[];
#ifdef _LP64
  static juint    _crc_by128_masks_avx512[];
  static juint    _crc_table_avx512[];
  static juint    _shuf_table_crc32_avx512[];
  static juint    _adler32_shuf0_table[];
  static juint    _adler32_shuf1_table[];
  static juint    _adler32_ascale_table[];
#endif // _LP64
  // table for CRC32C
  static juint* _crc32c_table;
  // swap mask for ghash
  static address _ghash_long_swap_mask_addr;
  static address _ghash_byte_swap_mask_addr;
  static address _ghash_poly_addr;
  static address _ghash_shuffmask_addr;

  // upper word mask for sha1
  static address _upper_word_mask_addr;
  // byte flip mask for sha1
  static address _shuffle_byte_flip_mask_addr;

  //k256 table for sha256
  static juint _k256[];
  static address _k256_adr;
  static address _vector_short_to_byte_mask;
  static address _vector_float_sign_mask;
  static address _vector_float_sign_flip;
  static address _vector_double_sign_mask;
  static address _vector_double_sign_flip;
  static address _vector_long_sign_mask;
  static address _vector_all_bits_set;
  static address _vector_byte_perm_mask;
  static address _vector_int_to_byte_mask;
  static address _vector_int_to_short_mask;
  static address _vector_32_bit_mask;
  static address _vector_64_bit_mask;
  static address _vector_int_shuffle_mask;
  static address _vector_byte_shuffle_mask;
  static address _vector_short_shuffle_mask;
  static address _vector_long_shuffle_mask;
  static address _vector_iota_indices;
#ifdef _LP64
  static juint _k256_W[];
  static address _k256_W_adr;
  static julong _k512_W[];
  static address _k512_W_addr;
  // byte flip mask for sha512
  static address _pshuffle_byte_flip_mask_addr_sha512;
  static address _counter_mask_addr;
  // Masks for base64
  static address _encoding_table_base64;
  static address _shuffle_base64;
  static address _avx2_shuffle_base64;
  static address _avx2_input_mask_base64;
  static address _avx2_lut_base64;
  static address _lookup_lo_base64;
  static address _lookup_hi_base64;
  static address _lookup_lo_base64url;
  static address _lookup_hi_base64url;
  static address _pack_vec_base64;
  static address _join_0_1_base64;
  static address _join_1_2_base64;
  static address _join_2_3_base64;
  static address _decoding_table_base64;
#endif
  // byte flip mask for sha256
  static address _pshuffle_byte_flip_mask_addr;

  //tables common for LIBM sin and cos
  static juint _ONEHALF[];
  static address _ONEHALF_adr;
  static juint _P_2[];
  static address _P_2_adr;
  static juint _SC_4[];
  static address _SC_4_adr;
  static juint _Ctable[];
  static address _Ctable_adr;
  static juint _SC_2[];
  static address _SC_2_adr;
  static juint _SC_3[];
  static address _SC_3_adr;
  static juint _SC_1[];
  static address _SC_1_adr;
  static juint _PI_INV_TABLE[];
  static address _PI_INV_TABLE_adr;
  static juint _PI_4[];
  static address _PI_4_adr;
  static juint _PI32INV[];
  static address _PI32INV_adr;
  static juint _SIGN_MASK[];
  static address _SIGN_MASK_adr;
  static juint _P_1[];
  static address _P_1_adr;
  static juint _P_3[];
  static address _P_3_adr;
  static juint _NEG_ZERO[];
  static address _NEG_ZERO_adr;

  //tables common for LIBM sincos and tancot
  static juint _L_2il0floatpacket_0[];
  static address _L_2il0floatpacket_0_adr;
  static juint _Pi4Inv[];
  static address _Pi4Inv_adr;
  static juint _Pi4x3[];
  static address _Pi4x3_adr;
  static juint _Pi4x4[];
  static address _Pi4x4_adr;
  static juint _ones[];
  static address _ones_adr;

 public:
  static address addr_mxcsr_std()        { return (address)&_mxcsr_std; }
  static address verify_mxcsr_entry()    { return _verify_mxcsr_entry; }
  static address key_shuffle_mask_addr() { return _key_shuffle_mask_addr; }
  static address counter_shuffle_mask_addr() { return _counter_shuffle_mask_addr; }
  static address crc_by128_masks_addr()  { return (address)_crc_by128_masks; }
#ifdef _LP64
  static address crc_by128_masks_avx512_addr()  { return (address)_crc_by128_masks_avx512; }
  static address shuf_table_crc32_avx512_addr()  { return (address)_shuf_table_crc32_avx512; }
  static address crc_table_avx512_addr()  { return (address)_crc_table_avx512; }
#endif // _LP64
  static address ghash_long_swap_mask_addr() { return _ghash_long_swap_mask_addr; }
  static address ghash_byte_swap_mask_addr() { return _ghash_byte_swap_mask_addr; }
  static address ghash_shufflemask_addr() { return _ghash_shuffmask_addr; }
  static address ghash_polynomial_addr() { return _ghash_poly_addr; }
  static address upper_word_mask_addr() { return _upper_word_mask_addr; }
  static address shuffle_byte_flip_mask_addr() { return _shuffle_byte_flip_mask_addr; }
  static address k256_addr()      { return _k256_adr; }
  static address method_entry_barrier() { return _method_entry_barrier; }

  static address vector_short_to_byte_mask() {
    return _vector_short_to_byte_mask;
  }
  static address vector_float_sign_mask() {
    return _vector_float_sign_mask;
  }

  static address vector_float_sign_flip() {
    return _vector_float_sign_flip;
  }

  static address vector_double_sign_mask() {
    return _vector_double_sign_mask;
  }

  static address vector_double_sign_flip() {
    return _vector_double_sign_flip;
  }

  static address vector_all_bits_set() {
    return _vector_all_bits_set;
  }

  static address vector_byte_perm_mask() {
    return _vector_byte_perm_mask;
  }

  static address vector_int_to_byte_mask() {
    return _vector_int_to_byte_mask;
  }

  static address vector_int_to_short_mask() {
    return _vector_int_to_short_mask;
  }

  static address vector_32_bit_mask() {
    return _vector_32_bit_mask;
  }

  static address vector_64_bit_mask() {
    return _vector_64_bit_mask;
  }

  static address vector_int_shuffle_mask() {
    return _vector_int_shuffle_mask;
  }

  static address vector_byte_shuffle_mask() {
    return _vector_byte_shuffle_mask;
  }

  static address vector_short_shuffle_mask() {
    return _vector_short_shuffle_mask;
  }

  static address vector_long_shuffle_mask() {
    return _vector_long_shuffle_mask;
  }

  static address vector_long_sign_mask() {
    return _vector_long_sign_mask;
  }

  static address vector_iota_indices() {
    return _vector_iota_indices;
  }

#ifdef _LP64
  static address k256_W_addr()    { return _k256_W_adr; }
  static address k512_W_addr()    { return _k512_W_addr; }
  static address pshuffle_byte_flip_mask_addr_sha512() { return _pshuffle_byte_flip_mask_addr_sha512; }
  static address base64_encoding_table_addr() { return _encoding_table_base64; }
  static address base64_shuffle_addr() { return _shuffle_base64; }
  static address base64_avx2_shuffle_addr() { return _avx2_shuffle_base64; }
  static address base64_avx2_input_mask_addr() { return _avx2_input_mask_base64; }
  static address base64_avx2_lut_addr() { return _avx2_lut_base64; }
  static address counter_mask_addr() { return _counter_mask_addr; }
  static address base64_vbmi_lookup_lo_addr() { return _lookup_lo_base64; }
  static address base64_vbmi_lookup_hi_addr() { return _lookup_hi_base64; }
  static address base64_vbmi_lookup_lo_url_addr() { return _lookup_lo_base64url; }
  static address base64_vbmi_lookup_hi_url_addr() { return _lookup_hi_base64url; }
  static address base64_vbmi_pack_vec_addr() { return _pack_vec_base64; }
  static address base64_vbmi_join_0_1_addr() { return _join_0_1_base64; }
  static address base64_vbmi_join_1_2_addr() { return _join_1_2_base64; }
  static address base64_vbmi_join_2_3_addr() { return _join_2_3_base64; }
  static address base64_decoding_table_addr() { return _decoding_table_base64; }
#endif
  static address pshuffle_byte_flip_mask_addr() { return _pshuffle_byte_flip_mask_addr; }
  static void generate_CRC32C_table(bool is_pclmulqdq_supported);
  static address _ONEHALF_addr()      { return _ONEHALF_adr; }
  static address _P_2_addr()      { return _P_2_adr; }
  static address _SC_4_addr()      { return _SC_4_adr; }
  static address _Ctable_addr()      { return _Ctable_adr; }
  static address _SC_2_addr()      { return _SC_2_adr; }
  static address _SC_3_addr()      { return _SC_3_adr; }
  static address _SC_1_addr()      { return _SC_1_adr; }
  static address _PI_INV_TABLE_addr()      { return _PI_INV_TABLE_adr; }
  static address _PI_4_addr()      { return _PI_4_adr; }
  static address _PI32INV_addr()      { return _PI32INV_adr; }
  static address _SIGN_MASK_addr()      { return _SIGN_MASK_adr; }
  static address _P_1_addr()      { return _P_1_adr; }
  static address _P_3_addr()      { return _P_3_adr; }
  static address _NEG_ZERO_addr()      { return _NEG_ZERO_adr; }
  static address _L_2il0floatpacket_0_addr()      { return _L_2il0floatpacket_0_adr; }
  static address _Pi4Inv_addr()      { return _Pi4Inv_adr; }
  static address _Pi4x3_addr()      { return _Pi4x3_adr; }
  static address _Pi4x4_addr()      { return _Pi4x4_adr; }
  static address _ones_addr()      { return _ones_adr; }

};

#endif // CPU_X86_STUBROUTINES_X86_HPP
