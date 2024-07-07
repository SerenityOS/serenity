/*
 * Copyright (c) 2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/EnumBits.h>
#include <AK/Types.h>

namespace AK {

enum class CPUFeatures : u64 {
    None = 0ULL,
    Invalid = 1ULL << 63,

#if !defined(KERNEL) && (ARCH(I386) || ARCH(X86_64))
#    define AK_CAN_CODEGEN_FOR_X86_SSE42 1
    X86_SSE42 = 1ULL << 0,
#    define AK_CAN_CODEGEN_FOR_X86_SHA 1
    X86_SHA = 1ULL << 1,
#else
#    define AK_CAN_CODEGEN_FOR_X86_SSE42 0
    X86_SSE42 = Invalid,
#    define AK_CAN_CODEGEN_FOR_X86_SHA 0
    X86_SHA = Invalid,
#endif
};

AK_ENUM_BITWISE_OPERATORS(CPUFeatures);

}

#ifdef USING_AK_GLOBALLY
using AK::CPUFeatures;
#endif
