/*
 * Copyright (c) 2022, Leon Albrecht <leon.a@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/EnumBits.h>
#include <AK/FPControl.h>
#include <AK/Platform.h>
#include <AK/Types.h>

namespace Kernel::SIMD {

// Intel-Manual Vol 1 Chp 13.4
enum StateComponent : u64 {
    X87 = 1ull << 0ull,
    SSE = 1ull << 1ull, // xmm0-xmm7(15)
    AVX = 1ull << 2ull, // ymm0-ymm7(15) hi
    MPX_BNDREGS = 1ull << 3ull,
    MPX_BNDCSR = 1ull << 4ull,
    AVX512_opmask = 1ull << 5ull, // k0 - k9
    AVX512_ZMM_hi = 1ull << 6ull, // 0 - 15
    AVX512_ZMM = 1ull << 7ull,    // 16 - 31 full
    PT = 1ull << 8ull,
    PKRU = 1ull << 9ull,

    CET_U = 1ull << 11ull,
    CET_S = 1ull << 12ull,
    HDC = 1ull << 13ull,

    LBR = 1ull << 15ull,
    HWP = 1ull << 16ull,

    XCOMP_ENABLE = 1ull << 63ull
};
AK_ENUM_BITWISE_OPERATORS(StateComponent);

struct [[gnu::packed]] LegacyRegion {
    AK::X87ControlWord FCW;
    u16 FSW;
    u8 FTW;
    u8 : 8;
    u16 FOP;

    // 64-bit version
    u64 FIP_64;
    u64 FDP_64;

    AK::MXCSR MXCSR;
    u32 MXCSR_mask;
    u8 st_mmx[128];
    u8 xmm[256];
    u8 available[96]; // Extra available space
};

static_assert(sizeof(LegacyRegion) == 512);

struct [[gnu::packed]] Header {
    StateComponent xstate_bv;
    StateComponent xcomp_bv;
    u8 reserved[48];
};
static_assert(sizeof(Header) == 64);

}
