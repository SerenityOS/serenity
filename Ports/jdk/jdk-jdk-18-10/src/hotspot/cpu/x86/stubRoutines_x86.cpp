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

#include "precompiled.hpp"
#include "runtime/deoptimization.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/stubRoutines.hpp"
#include "runtime/thread.inline.hpp"
#include "utilities/globalDefinitions.hpp"
#include "crc32c.h"

// Implementation of the platform-specific part of StubRoutines - for
// a description of how to extend it, see the stubRoutines.hpp file.

address StubRoutines::x86::_verify_mxcsr_entry = NULL;
address StubRoutines::x86::_key_shuffle_mask_addr = NULL;
address StubRoutines::x86::_counter_shuffle_mask_addr = NULL;
address StubRoutines::x86::_ghash_long_swap_mask_addr = NULL;
address StubRoutines::x86::_ghash_byte_swap_mask_addr = NULL;
address StubRoutines::x86::_ghash_poly_addr = NULL;
address StubRoutines::x86::_ghash_shuffmask_addr = NULL;
address StubRoutines::x86::_upper_word_mask_addr = NULL;
address StubRoutines::x86::_shuffle_byte_flip_mask_addr = NULL;
address StubRoutines::x86::_k256_adr = NULL;
address StubRoutines::x86::_vector_short_to_byte_mask = NULL;
address StubRoutines::x86::_vector_int_to_byte_mask = NULL;
address StubRoutines::x86::_vector_int_to_short_mask = NULL;
address StubRoutines::x86::_vector_all_bits_set = NULL;
address StubRoutines::x86::_vector_byte_shuffle_mask = NULL;
address StubRoutines::x86::_vector_short_shuffle_mask = NULL;
address StubRoutines::x86::_vector_int_shuffle_mask = NULL;
address StubRoutines::x86::_vector_long_shuffle_mask = NULL;
address StubRoutines::x86::_vector_float_sign_mask = NULL;
address StubRoutines::x86::_vector_float_sign_flip = NULL;
address StubRoutines::x86::_vector_double_sign_mask = NULL;
address StubRoutines::x86::_vector_double_sign_flip = NULL;
address StubRoutines::x86::_vector_byte_perm_mask = NULL;
address StubRoutines::x86::_vector_long_sign_mask = NULL;
address StubRoutines::x86::_vector_iota_indices = NULL;
address StubRoutines::x86::_vector_32_bit_mask = NULL;
address StubRoutines::x86::_vector_64_bit_mask = NULL;
#ifdef _LP64
address StubRoutines::x86::_k256_W_adr = NULL;
address StubRoutines::x86::_k512_W_addr = NULL;
address StubRoutines::x86::_pshuffle_byte_flip_mask_addr_sha512 = NULL;
// Base64 masks
address StubRoutines::x86::_encoding_table_base64 = NULL;
address StubRoutines::x86::_shuffle_base64 = NULL;
address StubRoutines::x86::_avx2_shuffle_base64 = NULL;
address StubRoutines::x86::_avx2_input_mask_base64 = NULL;
address StubRoutines::x86::_avx2_lut_base64 = NULL;
address StubRoutines::x86::_counter_mask_addr = NULL;
address StubRoutines::x86::_lookup_lo_base64 = NULL;
address StubRoutines::x86::_lookup_hi_base64 = NULL;
address StubRoutines::x86::_lookup_lo_base64url = NULL;
address StubRoutines::x86::_lookup_hi_base64url = NULL;
address StubRoutines::x86::_pack_vec_base64 = NULL;
address StubRoutines::x86::_join_0_1_base64 = NULL;
address StubRoutines::x86::_join_1_2_base64 = NULL;
address StubRoutines::x86::_join_2_3_base64 = NULL;
address StubRoutines::x86::_decoding_table_base64 = NULL;
#endif
address StubRoutines::x86::_pshuffle_byte_flip_mask_addr = NULL;

//tables common for sin and cos
address StubRoutines::x86::_ONEHALF_adr = NULL;
address StubRoutines::x86::_P_2_adr = NULL;
address StubRoutines::x86::_SC_4_adr = NULL;
address StubRoutines::x86::_Ctable_adr = NULL;
address StubRoutines::x86::_SC_2_adr = NULL;
address StubRoutines::x86::_SC_3_adr = NULL;
address StubRoutines::x86::_SC_1_adr = NULL;
address StubRoutines::x86::_PI_INV_TABLE_adr = NULL;
address StubRoutines::x86::_PI_4_adr = NULL;
address StubRoutines::x86::_PI32INV_adr = NULL;
address StubRoutines::x86::_SIGN_MASK_adr = NULL;
address StubRoutines::x86::_P_1_adr = NULL;
address StubRoutines::x86::_P_3_adr = NULL;
address StubRoutines::x86::_NEG_ZERO_adr = NULL;

//tables common for sincos and tancot
address StubRoutines::x86::_L_2il0floatpacket_0_adr = NULL;
address StubRoutines::x86::_Pi4Inv_adr = NULL;
address StubRoutines::x86::_Pi4x3_adr = NULL;
address StubRoutines::x86::_Pi4x4_adr = NULL;
address StubRoutines::x86::_ones_adr = NULL;

uint64_t StubRoutines::x86::_crc_by128_masks[] =
{
  /* The fields in this structure are arranged so that they can be
   * picked up two at a time with 128-bit loads.
   *
   * Because of flipped bit order for this CRC polynomials
   * the constant for X**N is left-shifted by 1.  This is because
   * a 64 x 64 polynomial multiply produces a 127-bit result
   * but the highest term is always aligned to bit 0 in the container.
   * Pre-shifting by one fixes this, at the cost of potentially making
   * the 32-bit constant no longer fit in a 32-bit container (thus the
   * use of uint64_t, though this is also the size used by the carry-
   * less multiply instruction.
   *
   * In addition, the flipped bit order and highest-term-at-least-bit
   * multiply changes the constants used.  The 96-bit result will be
   * aligned to the high-term end of the target 128-bit container,
   * not the low-term end; that is, instead of a 512-bit or 576-bit fold,
   * instead it is a 480 (=512-32) or 544 (=512+64-32) bit fold.
   *
   * This cause additional problems in the 128-to-64-bit reduction; see the
   * code for details.  By storing a mask in the otherwise unused half of
   * a 128-bit constant, bits can be cleared before multiplication without
   * storing and reloading.  Note that staying on a 128-bit datapath means
   * that some data is uselessly stored and some unused data is intersected
   * with an irrelevant constant.
   */

  ((uint64_t) 0xffffffffUL),     /* low  of K_M_64    */
  ((uint64_t) 0xb1e6b092U << 1), /* high of K_M_64    */
  ((uint64_t) 0xba8ccbe8U << 1), /* low  of K_160_96  */
  ((uint64_t) 0x6655004fU << 1), /* high of K_160_96  */
  ((uint64_t) 0xaa2215eaU << 1), /* low  of K_544_480 */
  ((uint64_t) 0xe3720acbU << 1)  /* high of K_544_480 */
};

/**
 *  crc_table[] from jdk/src/share/native/java/util/zip/zlib-1.2.5/crc32.h
 */
juint StubRoutines::x86::_crc_table[] =
{
    0x00000000UL, 0x77073096UL, 0xee0e612cUL, 0x990951baUL, 0x076dc419UL,
    0x706af48fUL, 0xe963a535UL, 0x9e6495a3UL, 0x0edb8832UL, 0x79dcb8a4UL,
    0xe0d5e91eUL, 0x97d2d988UL, 0x09b64c2bUL, 0x7eb17cbdUL, 0xe7b82d07UL,
    0x90bf1d91UL, 0x1db71064UL, 0x6ab020f2UL, 0xf3b97148UL, 0x84be41deUL,
    0x1adad47dUL, 0x6ddde4ebUL, 0xf4d4b551UL, 0x83d385c7UL, 0x136c9856UL,
    0x646ba8c0UL, 0xfd62f97aUL, 0x8a65c9ecUL, 0x14015c4fUL, 0x63066cd9UL,
    0xfa0f3d63UL, 0x8d080df5UL, 0x3b6e20c8UL, 0x4c69105eUL, 0xd56041e4UL,
    0xa2677172UL, 0x3c03e4d1UL, 0x4b04d447UL, 0xd20d85fdUL, 0xa50ab56bUL,
    0x35b5a8faUL, 0x42b2986cUL, 0xdbbbc9d6UL, 0xacbcf940UL, 0x32d86ce3UL,
    0x45df5c75UL, 0xdcd60dcfUL, 0xabd13d59UL, 0x26d930acUL, 0x51de003aUL,
    0xc8d75180UL, 0xbfd06116UL, 0x21b4f4b5UL, 0x56b3c423UL, 0xcfba9599UL,
    0xb8bda50fUL, 0x2802b89eUL, 0x5f058808UL, 0xc60cd9b2UL, 0xb10be924UL,
    0x2f6f7c87UL, 0x58684c11UL, 0xc1611dabUL, 0xb6662d3dUL, 0x76dc4190UL,
    0x01db7106UL, 0x98d220bcUL, 0xefd5102aUL, 0x71b18589UL, 0x06b6b51fUL,
    0x9fbfe4a5UL, 0xe8b8d433UL, 0x7807c9a2UL, 0x0f00f934UL, 0x9609a88eUL,
    0xe10e9818UL, 0x7f6a0dbbUL, 0x086d3d2dUL, 0x91646c97UL, 0xe6635c01UL,
    0x6b6b51f4UL, 0x1c6c6162UL, 0x856530d8UL, 0xf262004eUL, 0x6c0695edUL,
    0x1b01a57bUL, 0x8208f4c1UL, 0xf50fc457UL, 0x65b0d9c6UL, 0x12b7e950UL,
    0x8bbeb8eaUL, 0xfcb9887cUL, 0x62dd1ddfUL, 0x15da2d49UL, 0x8cd37cf3UL,
    0xfbd44c65UL, 0x4db26158UL, 0x3ab551ceUL, 0xa3bc0074UL, 0xd4bb30e2UL,
    0x4adfa541UL, 0x3dd895d7UL, 0xa4d1c46dUL, 0xd3d6f4fbUL, 0x4369e96aUL,
    0x346ed9fcUL, 0xad678846UL, 0xda60b8d0UL, 0x44042d73UL, 0x33031de5UL,
    0xaa0a4c5fUL, 0xdd0d7cc9UL, 0x5005713cUL, 0x270241aaUL, 0xbe0b1010UL,
    0xc90c2086UL, 0x5768b525UL, 0x206f85b3UL, 0xb966d409UL, 0xce61e49fUL,
    0x5edef90eUL, 0x29d9c998UL, 0xb0d09822UL, 0xc7d7a8b4UL, 0x59b33d17UL,
    0x2eb40d81UL, 0xb7bd5c3bUL, 0xc0ba6cadUL, 0xedb88320UL, 0x9abfb3b6UL,
    0x03b6e20cUL, 0x74b1d29aUL, 0xead54739UL, 0x9dd277afUL, 0x04db2615UL,
    0x73dc1683UL, 0xe3630b12UL, 0x94643b84UL, 0x0d6d6a3eUL, 0x7a6a5aa8UL,
    0xe40ecf0bUL, 0x9309ff9dUL, 0x0a00ae27UL, 0x7d079eb1UL, 0xf00f9344UL,
    0x8708a3d2UL, 0x1e01f268UL, 0x6906c2feUL, 0xf762575dUL, 0x806567cbUL,
    0x196c3671UL, 0x6e6b06e7UL, 0xfed41b76UL, 0x89d32be0UL, 0x10da7a5aUL,
    0x67dd4accUL, 0xf9b9df6fUL, 0x8ebeeff9UL, 0x17b7be43UL, 0x60b08ed5UL,
    0xd6d6a3e8UL, 0xa1d1937eUL, 0x38d8c2c4UL, 0x4fdff252UL, 0xd1bb67f1UL,
    0xa6bc5767UL, 0x3fb506ddUL, 0x48b2364bUL, 0xd80d2bdaUL, 0xaf0a1b4cUL,
    0x36034af6UL, 0x41047a60UL, 0xdf60efc3UL, 0xa867df55UL, 0x316e8eefUL,
    0x4669be79UL, 0xcb61b38cUL, 0xbc66831aUL, 0x256fd2a0UL, 0x5268e236UL,
    0xcc0c7795UL, 0xbb0b4703UL, 0x220216b9UL, 0x5505262fUL, 0xc5ba3bbeUL,
    0xb2bd0b28UL, 0x2bb45a92UL, 0x5cb36a04UL, 0xc2d7ffa7UL, 0xb5d0cf31UL,
    0x2cd99e8bUL, 0x5bdeae1dUL, 0x9b64c2b0UL, 0xec63f226UL, 0x756aa39cUL,
    0x026d930aUL, 0x9c0906a9UL, 0xeb0e363fUL, 0x72076785UL, 0x05005713UL,
    0x95bf4a82UL, 0xe2b87a14UL, 0x7bb12baeUL, 0x0cb61b38UL, 0x92d28e9bUL,
    0xe5d5be0dUL, 0x7cdcefb7UL, 0x0bdbdf21UL, 0x86d3d2d4UL, 0xf1d4e242UL,
    0x68ddb3f8UL, 0x1fda836eUL, 0x81be16cdUL, 0xf6b9265bUL, 0x6fb077e1UL,
    0x18b74777UL, 0x88085ae6UL, 0xff0f6a70UL, 0x66063bcaUL, 0x11010b5cUL,
    0x8f659effUL, 0xf862ae69UL, 0x616bffd3UL, 0x166ccf45UL, 0xa00ae278UL,
    0xd70dd2eeUL, 0x4e048354UL, 0x3903b3c2UL, 0xa7672661UL, 0xd06016f7UL,
    0x4969474dUL, 0x3e6e77dbUL, 0xaed16a4aUL, 0xd9d65adcUL, 0x40df0b66UL,
    0x37d83bf0UL, 0xa9bcae53UL, 0xdebb9ec5UL, 0x47b2cf7fUL, 0x30b5ffe9UL,
    0xbdbdf21cUL, 0xcabac28aUL, 0x53b39330UL, 0x24b4a3a6UL, 0xbad03605UL,
    0xcdd70693UL, 0x54de5729UL, 0x23d967bfUL, 0xb3667a2eUL, 0xc4614ab8UL,
    0x5d681b02UL, 0x2a6f2b94UL, 0xb40bbe37UL, 0xc30c8ea1UL, 0x5a05df1bUL,
    0x2d02ef8dUL
};

#ifdef _LP64
juint StubRoutines::x86::_crc_table_avx512[] =
{
    0xe95c1271UL, 0x00000000UL, 0xce3371cbUL, 0x00000000UL,
    0xccaa009eUL, 0x00000000UL, 0x751997d0UL, 0x00000001UL,
    0x4a7fe880UL, 0x00000001UL, 0xe88ef372UL, 0x00000001UL,
    0xccaa009eUL, 0x00000000UL, 0x63cd6124UL, 0x00000001UL,
    0xf7011640UL, 0x00000001UL, 0xdb710640UL, 0x00000001UL,
    0xd7cfc6acUL, 0x00000001UL, 0xea89367eUL, 0x00000001UL,
    0x8cb44e58UL, 0x00000001UL, 0xdf068dc2UL, 0x00000000UL,
    0xae0b5394UL, 0x00000000UL, 0xc7569e54UL, 0x00000001UL,
    0xc6e41596UL, 0x00000001UL, 0x54442bd4UL, 0x00000001UL,
    0x74359406UL, 0x00000001UL, 0x3db1ecdcUL, 0x00000000UL,
    0x5a546366UL, 0x00000001UL, 0xf1da05aaUL, 0x00000000UL,
    0xccaa009eUL, 0x00000000UL, 0x751997d0UL, 0x00000001UL,
    0x00000000UL, 0x00000000UL, 0x00000000UL, 0x00000000UL
};

juint StubRoutines::x86::_crc_by128_masks_avx512[] =
{
    0xffffffffUL, 0xffffffffUL, 0x00000000UL, 0x00000000UL,
    0x00000000UL, 0xffffffffUL, 0xffffffffUL, 0xffffffffUL,
    0x80808080UL, 0x80808080UL, 0x80808080UL, 0x80808080UL
};

juint StubRoutines::x86::_shuf_table_crc32_avx512[] =
{
    0x83828100UL, 0x87868584UL, 0x8b8a8988UL, 0x8f8e8d8cUL,
    0x03020100UL, 0x07060504UL, 0x0b0a0908UL, 0x000e0d0cUL
};

juint StubRoutines::x86::_adler32_ascale_table[] =
{
    0x00000000UL, 0x00000001UL, 0x00000002UL, 0x00000003UL,
    0x00000004UL, 0x00000005UL, 0x00000006UL, 0x00000007UL
};

juint StubRoutines::x86::_adler32_shuf0_table[] =
{
    0xFFFFFF00UL, 0xFFFFFF01UL, 0xFFFFFF02UL, 0xFFFFFF03UL,
    0xFFFFFF04UL, 0xFFFFFF05UL, 0xFFFFFF06UL, 0xFFFFFF07UL
};

juint StubRoutines::x86::_adler32_shuf1_table[] =
{
    0xFFFFFF08UL, 0xFFFFFF09, 0xFFFFFF0AUL, 0xFFFFFF0BUL,
    0xFFFFFF0CUL, 0xFFFFFF0D, 0xFFFFFF0EUL, 0xFFFFFF0FUL
};

#endif // _LP64

#define D 32
#define P 0x82F63B78 // Reflection of Castagnoli (0x11EDC6F41)

#define TILL_CYCLE 31
uint32_t _crc32c_pow_2k_table[TILL_CYCLE]; // because _crc32c_pow_2k_table[TILL_CYCLE == 31] == _crc32c_pow_2k_table[0]

// A. Kadatch and B. Jenkins / Everything we know about CRC but afraid to forget September 3, 2010 8
// Listing 1: Multiplication of normalized polynomials
// "a" and "b" occupy D least significant bits.
uint32_t crc32c_multiply(uint32_t a, uint32_t b) {
  uint32_t product = 0;
  uint32_t b_pow_x_table[D + 1]; // b_pow_x_table[k] = (b * x**k) mod P
  b_pow_x_table[0] = b;
  for (int k = 0; k < D; ++k) {
    // If "a" has non-zero coefficient at x**k,/ add ((b * x**k) mod P) to the result.
    if ((a & (((uint32_t)1) << (D - 1 - k))) != 0) product ^= b_pow_x_table[k];

    // Compute b_pow_x_table[k+1] = (b ** x**(k+1)) mod P.
    if (b_pow_x_table[k] & 1) {
      // If degree of (b_pow_x_table[k] * x) is D, then
      // degree of (b_pow_x_table[k] * x - P) is less than D.
      b_pow_x_table[k + 1] = (b_pow_x_table[k] >> 1) ^ P;
    }
    else {
      b_pow_x_table[k + 1] = b_pow_x_table[k] >> 1;
    }
  }
  return product;
}
#undef D
#undef P

// A. Kadatch and B. Jenkins / Everything we know about CRC but afraid to forget September 3, 2010 9
void crc32c_init_pow_2k(void) {
  // _crc32c_pow_2k_table(0) =
  // x^(2^k) mod P(x) = x mod P(x) = x
  // Since we are operating on a reflected values
  // x = 10b, reflect(x) = 0x40000000
  _crc32c_pow_2k_table[0] = 0x40000000;

  for (int k = 1; k < TILL_CYCLE; k++) {
    // _crc32c_pow_2k_table(k+1) = _crc32c_pow_2k_table(k-1)^2 mod P(x)
    uint32_t tmp = _crc32c_pow_2k_table[k - 1];
    _crc32c_pow_2k_table[k] = crc32c_multiply(tmp, tmp);
  }
}

// x^N mod P(x)
uint32_t crc32c_f_pow_n(uint32_t n) {
  //            result = 1 (polynomial)
  uint32_t one, result = 0x80000000, i = 0;

  while (one = (n & 1), (n == 1 || n - one > 0)) {
    if (one) {
      result = crc32c_multiply(result, _crc32c_pow_2k_table[i]);
    }
    n >>= 1;
    i++;
  }

  return result;
}

juint *StubRoutines::x86::_crc32c_table;

void StubRoutines::x86::generate_CRC32C_table(bool is_pclmulqdq_table_supported) {

  static juint pow_n[CRC32C_NUM_PRECOMPUTED_CONSTANTS];

  crc32c_init_pow_2k();

  pow_n[0] = crc32c_f_pow_n(CRC32C_HIGH * 8);      // 8N * 8 = 64N
  pow_n[1] = crc32c_f_pow_n(CRC32C_HIGH * 8 * 2);  // 128N

  pow_n[2] = crc32c_f_pow_n(CRC32C_MIDDLE * 8);
  pow_n[3] = crc32c_f_pow_n(CRC32C_MIDDLE * 8 * 2);

  pow_n[4] = crc32c_f_pow_n(CRC32C_LOW * 8);
  pow_n[CRC32C_NUM_PRECOMPUTED_CONSTANTS - 1] =
            crc32c_f_pow_n(CRC32C_LOW * 8 * 2);

  if (is_pclmulqdq_table_supported) {
    _crc32c_table = pow_n;
  } else {
    static julong pclmulqdq_table[CRC32C_NUM_PRECOMPUTED_CONSTANTS * 256];

    for (int j = 0; j < CRC32C_NUM_PRECOMPUTED_CONSTANTS; j++) {
      static juint X_CONST = pow_n[j];
      for (int64_t i = 0; i < 256; i++) { // to force 64 bit wide computations
      // S. Gueron / Information Processing Letters 112 (2012) 184
      // Algorithm 3: Generating a carry-less multiplication lookup table.
      // Input: A 32-bit constant, X_CONST.
      // Output: A table of 256 entries, each one is a 64-bit quadword,
      // that can be used for computing "byte" * X_CONST, for a given byte.
        pclmulqdq_table[j * 256 + i] =
          ((i & 1) * X_CONST) ^ ((i & 2) * X_CONST) ^ ((i & 4) * X_CONST) ^
          ((i & 8) * X_CONST) ^ ((i & 16) * X_CONST) ^ ((i & 32) * X_CONST) ^
          ((i & 64) * X_CONST) ^ ((i & 128) * X_CONST);
      }
    }
    _crc32c_table = (juint*)pclmulqdq_table;
  }
}

ATTRIBUTE_ALIGNED(64) juint StubRoutines::x86::_k256[] =
{
    0x428a2f98UL, 0x71374491UL, 0xb5c0fbcfUL, 0xe9b5dba5UL,
    0x3956c25bUL, 0x59f111f1UL, 0x923f82a4UL, 0xab1c5ed5UL,
    0xd807aa98UL, 0x12835b01UL, 0x243185beUL, 0x550c7dc3UL,
    0x72be5d74UL, 0x80deb1feUL, 0x9bdc06a7UL, 0xc19bf174UL,
    0xe49b69c1UL, 0xefbe4786UL, 0x0fc19dc6UL, 0x240ca1ccUL,
    0x2de92c6fUL, 0x4a7484aaUL, 0x5cb0a9dcUL, 0x76f988daUL,
    0x983e5152UL, 0xa831c66dUL, 0xb00327c8UL, 0xbf597fc7UL,
    0xc6e00bf3UL, 0xd5a79147UL, 0x06ca6351UL, 0x14292967UL,
    0x27b70a85UL, 0x2e1b2138UL, 0x4d2c6dfcUL, 0x53380d13UL,
    0x650a7354UL, 0x766a0abbUL, 0x81c2c92eUL, 0x92722c85UL,
    0xa2bfe8a1UL, 0xa81a664bUL, 0xc24b8b70UL, 0xc76c51a3UL,
    0xd192e819UL, 0xd6990624UL, 0xf40e3585UL, 0x106aa070UL,
    0x19a4c116UL, 0x1e376c08UL, 0x2748774cUL, 0x34b0bcb5UL,
    0x391c0cb3UL, 0x4ed8aa4aUL, 0x5b9cca4fUL, 0x682e6ff3UL,
    0x748f82eeUL, 0x78a5636fUL, 0x84c87814UL, 0x8cc70208UL,
    0x90befffaUL, 0xa4506cebUL, 0xbef9a3f7UL, 0xc67178f2UL
};

#ifdef _LP64
// used in MacroAssembler::sha256_AVX2
// dynamically built from _k256
ATTRIBUTE_ALIGNED(64) juint StubRoutines::x86::_k256_W[2*sizeof(StubRoutines::x86::_k256)];

// used in MacroAssembler::sha512_AVX2
ATTRIBUTE_ALIGNED(64) julong StubRoutines::x86::_k512_W[] =
{
    0x428a2f98d728ae22ULL, 0x7137449123ef65cdULL,
    0xb5c0fbcfec4d3b2fULL, 0xe9b5dba58189dbbcULL,
    0x3956c25bf348b538ULL, 0x59f111f1b605d019ULL,
    0x923f82a4af194f9bULL, 0xab1c5ed5da6d8118ULL,
    0xd807aa98a3030242ULL, 0x12835b0145706fbeULL,
    0x243185be4ee4b28cULL, 0x550c7dc3d5ffb4e2ULL,
    0x72be5d74f27b896fULL, 0x80deb1fe3b1696b1ULL,
    0x9bdc06a725c71235ULL, 0xc19bf174cf692694ULL,
    0xe49b69c19ef14ad2ULL, 0xefbe4786384f25e3ULL,
    0x0fc19dc68b8cd5b5ULL, 0x240ca1cc77ac9c65ULL,
    0x2de92c6f592b0275ULL, 0x4a7484aa6ea6e483ULL,
    0x5cb0a9dcbd41fbd4ULL, 0x76f988da831153b5ULL,
    0x983e5152ee66dfabULL, 0xa831c66d2db43210ULL,
    0xb00327c898fb213fULL, 0xbf597fc7beef0ee4ULL,
    0xc6e00bf33da88fc2ULL, 0xd5a79147930aa725ULL,
    0x06ca6351e003826fULL, 0x142929670a0e6e70ULL,
    0x27b70a8546d22ffcULL, 0x2e1b21385c26c926ULL,
    0x4d2c6dfc5ac42aedULL, 0x53380d139d95b3dfULL,
    0x650a73548baf63deULL, 0x766a0abb3c77b2a8ULL,
    0x81c2c92e47edaee6ULL, 0x92722c851482353bULL,
    0xa2bfe8a14cf10364ULL, 0xa81a664bbc423001ULL,
    0xc24b8b70d0f89791ULL, 0xc76c51a30654be30ULL,
    0xd192e819d6ef5218ULL, 0xd69906245565a910ULL,
    0xf40e35855771202aULL, 0x106aa07032bbd1b8ULL,
    0x19a4c116b8d2d0c8ULL, 0x1e376c085141ab53ULL,
    0x2748774cdf8eeb99ULL, 0x34b0bcb5e19b48a8ULL,
    0x391c0cb3c5c95a63ULL, 0x4ed8aa4ae3418acbULL,
    0x5b9cca4f7763e373ULL, 0x682e6ff3d6b2b8a3ULL,
    0x748f82ee5defb2fcULL, 0x78a5636f43172f60ULL,
    0x84c87814a1f0ab72ULL, 0x8cc702081a6439ecULL,
    0x90befffa23631e28ULL, 0xa4506cebde82bde9ULL,
    0xbef9a3f7b2c67915ULL, 0xc67178f2e372532bULL,
    0xca273eceea26619cULL, 0xd186b8c721c0c207ULL,
    0xeada7dd6cde0eb1eULL, 0xf57d4f7fee6ed178ULL,
    0x06f067aa72176fbaULL, 0x0a637dc5a2c898a6ULL,
    0x113f9804bef90daeULL, 0x1b710b35131c471bULL,
    0x28db77f523047d84ULL, 0x32caab7b40c72493ULL,
    0x3c9ebe0a15c9bebcULL, 0x431d67c49c100d4cULL,
    0x4cc5d4becb3e42b6ULL, 0x597f299cfc657e2aULL,
    0x5fcb6fab3ad6faecULL, 0x6c44198c4a475817ULL,
};
#endif
