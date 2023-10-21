/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DistinctNumeric.h>

namespace Disassembly::RISCV64 {

AK_TYPEDEF_DISTINCT_NUMERIC_GENERAL(u8, Register, CastToUnderlying);

// RISC-V ABIs Specification Version 1.0
// 1.1 Integer Register Convention, Table 1
enum class RegisterABINames : u8 {
    zero = 0,
    ra = 1,
    sp = 2,
    gp = 3,
    tp = 4,
    t0 = 5,
    t1 = 6,
    t2 = 7,
    s0 = 8,
    s1 = 9,
    a0 = 10,
    a1 = 11,
    a2 = 12,
    a3 = 13,
    a4 = 14,
    a5 = 15,
    a6 = 16,
    a7 = 17,
    s2 = 18,
    s3 = 19,
    s4 = 20,
    s5 = 21,
    s6 = 22,
    s7 = 23,
    s8 = 24,
    s9 = 25,
    s10 = 26,
    s11 = 27,
    t3 = 28,
    t4 = 29,
    t5 = 30,
    t6 = 31,
};

// As per 1.1 Integer Register Convention:
// "The presence of a frame pointer is optional.
//  If a frame pointer exists, it must reside in x8 (s0); the register remains callee-saved."
// On default compile settings, SerenityOS uses a frame pointer, but with different compiler flags on the same ABI,
// the frame pointer may be omitted or not on a function-by-function basis.
// Disassembly frontends can therfore decide whether to print this register as s0 or fp.
enum class RegisterABINamesWithFP : u8 {
    zero = 0,
    ra = 1,
    sp = 2,
    gp = 3,
    tp = 4,
    t0 = 5,
    t1 = 6,
    t2 = 7,
    fp = 8,
    s1 = 9,
    a0 = 10,
    a1 = 11,
    a2 = 12,
    a3 = 13,
    a4 = 14,
    a5 = 15,
    a6 = 16,
    a7 = 17,
    s2 = 18,
    s3 = 19,
    s4 = 20,
    s5 = 21,
    s6 = 22,
    s7 = 23,
    s8 = 24,
    s9 = 25,
    s10 = 26,
    s11 = 27,
    t3 = 28,
    t4 = 29,
    t5 = 30,
    t6 = 31,
};

AK_MAKE_DISTINCT_NUMERIC_COMPARABLE_TO_ENUM(Register, RegisterABINames);
AK_MAKE_DISTINCT_NUMERIC_COMPARABLE_TO_ENUM(Register, RegisterABINamesWithFP);

AK_TYPEDEF_DISTINCT_NUMERIC_GENERAL(u8, FloatRegister, CastToUnderlying);

// 1.2 Floating-point Register Convention, Table 2
enum class FloatRegisterABINames : u8 {
    ft0 = 0,
    ft1 = 1,
    ft2 = 2,
    ft3 = 3,
    ft4 = 4,
    ft5 = 5,
    ft6 = 6,
    ft7 = 7,
    fs0 = 8,
    fs1 = 9,
    fa0 = 10,
    fa1 = 11,
    fa2 = 12,
    fa3 = 13,
    fa4 = 14,
    fa5 = 15,
    fa6 = 16,
    fa7 = 17,
    fs2 = 18,
    fs3 = 19,
    fs4 = 20,
    fs5 = 21,
    fs6 = 22,
    fs7 = 23,
    fs8 = 24,
    fs9 = 25,
    fs10 = 26,
    fs11 = 27,
    ft8 = 28,
    ft9 = 29,
    ft10 = 30,
    ft11 = 31,
};

AK_MAKE_DISTINCT_NUMERIC_COMPARABLE_TO_ENUM(FloatRegister, FloatRegisterABINames);

// Specializations define two associated types:
// ABIType: The type to use for printing ABI names
// ABIWithFPType: The type to use for printing ABI names, with fp instead of s0.
template<typename BaseRegisterType>
struct RegisterNameTraits { };

template<>
struct RegisterNameTraits<Register> {
    using ABIType = RegisterABINames;
    using ABIWithFPType = RegisterABINamesWithFP;
};

template<>
struct RegisterNameTraits<FloatRegister> {
    using ABIType = FloatRegisterABINames;
    using ABIWithFPType = FloatRegisterABINames;
};

// This API is intended for decoding / encoding purposes only, be careful when using it.
constexpr FloatRegister as_float_register(Register reg)
{
    return static_cast<FloatRegister>(reg.value());
}

}
