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
#    define AK_CAN_CODEGEN_FOR_X86_AES 1
    X86_AES = 1ULL << 2,
#else
#    define AK_CAN_CODEGEN_FOR_X86_SSE42 0
    X86_SSE42 = Invalid,
#    define AK_CAN_CODEGEN_FOR_X86_SHA 0
    X86_SHA = Invalid,
#    define AK_CAN_CODEGEN_FOR_X86_AES 0
    X86_AES = Invalid,
#endif
};

AK_ENUM_BITWISE_OPERATORS(CPUFeatures);

#ifndef KERNEL
namespace Detail {
CPUFeatures detect_cpu_features_uncached();
}

inline CPUFeatures detect_cpu_features()
{
    static CPUFeatures const s_cached_features = Detail::detect_cpu_features_uncached();
    return s_cached_features;
}
#else
inline CPUFeatures detect_cpu_features()
{
    return CPUFeatures::None;
}
#endif

constexpr bool is_valid_feature(CPUFeatures feature)
{
    return !has_flag(feature, CPUFeatures::Invalid);
}

}

#ifdef USING_AK_GLOBALLY
using AK::CPUFeatures;
using AK::detect_cpu_features;
#endif
