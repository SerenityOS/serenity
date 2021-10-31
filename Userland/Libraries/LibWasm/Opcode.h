/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DistinctNumeric.h>

namespace Wasm {

TYPEDEF_DISTINCT_ORDERED_ID(u32, OpCode);

namespace Instructions {

#define ENUMERATE_SINGLE_BYTE_WASM_OPCODES(M) \
    M(unreachable, 0x00)                      \
    M(nop, 0x01)                              \
    M(block, 0x02)                            \
    M(loop, 0x03)                             \
    M(if_, 0x04)                              \
    M(br, 0x0c)                               \
    M(br_if, 0x0d)                            \
    M(br_table, 0x0e)                         \
    M(return_, 0x0f)                          \
    M(call, 0x10)                             \
    M(call_indirect, 0x11)                    \
    M(drop, 0x1a)                             \
    M(select, 0x1b)                           \
    M(select_typed, 0x1c)                     \
    M(local_get, 0x20)                        \
    M(local_set, 0x21)                        \
    M(local_tee, 0x22)                        \
    M(global_get, 0x23)                       \
    M(global_set, 0x24)                       \
    M(table_get, 0x25)                        \
    M(table_set, 0x26)                        \
    M(i32_load, 0x28)                         \
    M(i64_load, 0x29)                         \
    M(f32_load, 0x2a)                         \
    M(f64_load, 0x2b)                         \
    M(i32_load8_s, 0x2c)                      \
    M(i32_load8_u, 0x2d)                      \
    M(i32_load16_s, 0x2e)                     \
    M(i32_load16_u, 0x2f)                     \
    M(i64_load8_s, 0x30)                      \
    M(i64_load8_u, 0x31)                      \
    M(i64_load16_s, 0x32)                     \
    M(i64_load16_u, 0x33)                     \
    M(i64_load32_s, 0x34)                     \
    M(i64_load32_u, 0x35)                     \
    M(i32_store, 0x36)                        \
    M(i64_store, 0x37)                        \
    M(f32_store, 0x38)                        \
    M(f64_store, 0x39)                        \
    M(i32_store8, 0x3a)                       \
    M(i32_store16, 0x3b)                      \
    M(i64_store8, 0x3c)                       \
    M(i64_store16, 0x3d)                      \
    M(i64_store32, 0x3e)                      \
    M(memory_size, 0x3f)                      \
    M(memory_grow, 0x40)                      \
    M(i32_const, 0x41)                        \
    M(i64_const, 0x42)                        \
    M(f32_const, 0x43)                        \
    M(f64_const, 0x44)                        \
    M(i32_eqz, 0x45)                          \
    M(i32_eq, 0x46)                           \
    M(i32_ne, 0x47)                           \
    M(i32_lts, 0x48)                          \
    M(i32_ltu, 0x49)                          \
    M(i32_gts, 0x4a)                          \
    M(i32_gtu, 0x4b)                          \
    M(i32_les, 0x4c)                          \
    M(i32_leu, 0x4d)                          \
    M(i32_ges, 0x4e)                          \
    M(i32_geu, 0x4f)                          \
    M(i64_eqz, 0x50)                          \
    M(i64_eq, 0x51)                           \
    M(i64_ne, 0x52)                           \
    M(i64_lts, 0x53)                          \
    M(i64_ltu, 0x54)                          \
    M(i64_gts, 0x55)                          \
    M(i64_gtu, 0x56)                          \
    M(i64_les, 0x57)                          \
    M(i64_leu, 0x58)                          \
    M(i64_ges, 0x59)                          \
    M(i64_geu, 0x5a)                          \
    M(f32_eq, 0x5b)                           \
    M(f32_ne, 0x5c)                           \
    M(f32_lt, 0x5d)                           \
    M(f32_gt, 0x5e)                           \
    M(f32_le, 0x5f)                           \
    M(f32_ge, 0x60)                           \
    M(f64_eq, 0x61)                           \
    M(f64_ne, 0x62)                           \
    M(f64_lt, 0x63)                           \
    M(f64_gt, 0x64)                           \
    M(f64_le, 0x65)                           \
    M(f64_ge, 0x66)                           \
    M(i32_clz, 0x67)                          \
    M(i32_ctz, 0x68)                          \
    M(i32_popcnt, 0x69)                       \
    M(i32_add, 0x6a)                          \
    M(i32_sub, 0x6b)                          \
    M(i32_mul, 0x6c)                          \
    M(i32_divs, 0x6d)                         \
    M(i32_divu, 0x6e)                         \
    M(i32_rems, 0x6f)                         \
    M(i32_remu, 0x70)                         \
    M(i32_and, 0x71)                          \
    M(i32_or, 0x72)                           \
    M(i32_xor, 0x73)                          \
    M(i32_shl, 0x74)                          \
    M(i32_shrs, 0x75)                         \
    M(i32_shru, 0x76)                         \
    M(i32_rotl, 0x77)                         \
    M(i32_rotr, 0x78)                         \
    M(i64_clz, 0x79)                          \
    M(i64_ctz, 0x7a)                          \
    M(i64_popcnt, 0x7b)                       \
    M(i64_add, 0x7c)                          \
    M(i64_sub, 0x7d)                          \
    M(i64_mul, 0x7e)                          \
    M(i64_divs, 0x7f)                         \
    M(i64_divu, 0x80)                         \
    M(i64_rems, 0x81)                         \
    M(i64_remu, 0x82)                         \
    M(i64_and, 0x83)                          \
    M(i64_or, 0x84)                           \
    M(i64_xor, 0x85)                          \
    M(i64_shl, 0x86)                          \
    M(i64_shrs, 0x87)                         \
    M(i64_shru, 0x88)                         \
    M(i64_rotl, 0x89)                         \
    M(i64_rotr, 0x8a)                         \
    M(f32_abs, 0x8b)                          \
    M(f32_neg, 0x8c)                          \
    M(f32_ceil, 0x8d)                         \
    M(f32_floor, 0x8e)                        \
    M(f32_trunc, 0x8f)                        \
    M(f32_nearest, 0x90)                      \
    M(f32_sqrt, 0x91)                         \
    M(f32_add, 0x92)                          \
    M(f32_sub, 0x93)                          \
    M(f32_mul, 0x94)                          \
    M(f32_div, 0x95)                          \
    M(f32_min, 0x96)                          \
    M(f32_max, 0x97)                          \
    M(f32_copysign, 0x98)                     \
    M(f64_abs, 0x99)                          \
    M(f64_neg, 0x9a)                          \
    M(f64_ceil, 0x9b)                         \
    M(f64_floor, 0x9c)                        \
    M(f64_trunc, 0x9d)                        \
    M(f64_nearest, 0x9e)                      \
    M(f64_sqrt, 0x9f)                         \
    M(f64_add, 0xa0)                          \
    M(f64_sub, 0xa1)                          \
    M(f64_mul, 0xa2)                          \
    M(f64_div, 0xa3)                          \
    M(f64_min, 0xa4)                          \
    M(f64_max, 0xa5)                          \
    M(f64_copysign, 0xa6)                     \
    M(i32_wrap_i64, 0xa7)                     \
    M(i32_trunc_sf32, 0xa8)                   \
    M(i32_trunc_uf32, 0xa9)                   \
    M(i32_trunc_sf64, 0xaa)                   \
    M(i32_trunc_uf64, 0xab)                   \
    M(i64_extend_si32, 0xac)                  \
    M(i64_extend_ui32, 0xad)                  \
    M(i64_trunc_sf32, 0xae)                   \
    M(i64_trunc_uf32, 0xaf)                   \
    M(i64_trunc_sf64, 0xb0)                   \
    M(i64_trunc_uf64, 0xb1)                   \
    M(f32_convert_si32, 0xb2)                 \
    M(f32_convert_ui32, 0xb3)                 \
    M(f32_convert_si64, 0xb4)                 \
    M(f32_convert_ui64, 0xb5)                 \
    M(f32_demote_f64, 0xb6)                   \
    M(f64_convert_si32, 0xb7)                 \
    M(f64_convert_ui32, 0xb8)                 \
    M(f64_convert_si64, 0xb9)                 \
    M(f64_convert_ui64, 0xba)                 \
    M(f64_promote_f32, 0xbb)                  \
    M(i32_reinterpret_f32, 0xbc)              \
    M(i64_reinterpret_f64, 0xbd)              \
    M(f32_reinterpret_i32, 0xbe)              \
    M(f64_reinterpret_i64, 0xbf)              \
    M(i32_extend8_s, 0xc0)                    \
    M(i32_extend16_s, 0xc1)                   \
    M(i64_extend8_s, 0xc2)                    \
    M(i64_extend16_s, 0xc3)                   \
    M(i64_extend32_s, 0xc4)                   \
    M(ref_null, 0xd0)                         \
    M(ref_is_null, 0xd1)                      \
    M(ref_func, 0xd2)

// These are synthetic opcodes, they are _not_ seen in wasm with these values.
#define ENUMERATE_MULTI_BYTE_WASM_OPCODES(M) \
    M(i32_trunc_sat_f32_s, 0xfc00)           \
    M(i32_trunc_sat_f32_u, 0xfc01)           \
    M(i32_trunc_sat_f64_s, 0xfc02)           \
    M(i32_trunc_sat_f64_u, 0xfc03)           \
    M(i64_trunc_sat_f32_s, 0xfc04)           \
    M(i64_trunc_sat_f32_u, 0xfc05)           \
    M(i64_trunc_sat_f64_s, 0xfc06)           \
    M(i64_trunc_sat_f64_u, 0xfc07)           \
    M(memory_init, 0xfc08)                   \
    M(data_drop, 0xfc09)                     \
    M(memory_copy, 0xfc0a)                   \
    M(memory_fill, 0x0fc0b)                  \
    M(table_init, 0xfc0c)                    \
    M(elem_drop, 0xfc0d)                     \
    M(table_copy, 0xfc0e)                    \
    M(table_grow, 0xfc0f)                    \
    M(table_size, 0xfc10)                    \
    M(table_fill, 0xfc11)                    \
    M(structured_else, 0xff00)               \
    M(structured_end, 0xff01)

#define ENUMERATE_WASM_OPCODES(M)         \
    ENUMERATE_SINGLE_BYTE_WASM_OPCODES(M) \
    ENUMERATE_MULTI_BYTE_WASM_OPCODES(M)

#define M(name, value) static constexpr OpCode name = value;
ENUMERATE_WASM_OPCODES(M)
#undef M

static constexpr u32 i32_trunc_sat_f32_s_second = 0,
                     i32_trunc_sat_f32_u_second = 1,
                     i32_trunc_sat_f64_s_second = 2,
                     i32_trunc_sat_f64_u_second = 3,
                     i64_trunc_sat_f32_s_second = 4,
                     i64_trunc_sat_f32_u_second = 5,
                     i64_trunc_sat_f64_s_second = 6,
                     i64_trunc_sat_f64_u_second = 7,
                     memory_init_second = 8,
                     data_drop_second = 9,
                     memory_copy_second = 10,
                     memory_fill_second = 11,
                     table_init_second = 12,
                     elem_drop_second = 13,
                     table_copy_second = 14,
                     table_grow_second = 15,
                     table_size_second = 16,
                     table_fill_second = 17;

}

}
