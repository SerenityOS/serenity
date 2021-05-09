/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StdLibExtraDetails.h>
#include <AK/Types.h>

namespace AK::SIMD {

using i8x2 = i8 __attribute__((vector_size(2)));
using i8x4 = i8 __attribute__((vector_size(4)));
using i8x8 = i8 __attribute__((vector_size(8)));
using i8x16 = i8 __attribute__((vector_size(16)));
using i8x32 = i8 __attribute__((vector_size(32)));

using i16x2 = i16 __attribute__((vector_size(4)));
using i16x4 = i16 __attribute__((vector_size(8)));
using i16x8 = i16 __attribute__((vector_size(16)));
using i16x16 = i16 __attribute__((vector_size(32)));

using i32x2 = i32 __attribute__((vector_size(8)));
using i32x4 = i32 __attribute__((vector_size(16)));
using i32x8 = i32 __attribute__((vector_size(32)));

using i64x2 = i64 __attribute__((vector_size(16)));
using i64x4 = i64 __attribute__((vector_size(32)));

using u8x2 = u8 __attribute__((vector_size(2)));
using u8x4 = u8 __attribute__((vector_size(4)));
using u8x8 = u8 __attribute__((vector_size(8)));
using u8x16 = u8 __attribute__((vector_size(16)));
using u8x32 = u8 __attribute__((vector_size(32)));

using u16x2 = u16 __attribute__((vector_size(4)));
using u16x4 = u16 __attribute__((vector_size(8)));
using u16x8 = u16 __attribute__((vector_size(16)));
using u16x16 = u16 __attribute__((vector_size(32)));

using u32x2 = u32 __attribute__((vector_size(8)));
using u32x4 = u32 __attribute__((vector_size(16)));
using u32x8 = u32 __attribute__((vector_size(32)));

using u64x2 = u64 __attribute__((vector_size(16)));
using u64x4 = u64 __attribute__((vector_size(32)));

using f32x2 = float __attribute__((vector_size(8)));
using f32x4 = float __attribute__((vector_size(16)));
using f32x8 = float __attribute__((vector_size(32)));

using f64x2 = double __attribute__((vector_size(16)));
using f64x4 = double __attribute__((vector_size(32)));

}

namespace AK::Detail {

#define MAKE__MAKESIGNED(vec_size)                 \
    template<>                                     \
    struct __MakeSigned<AK::SIMD::i##vec_size> {   \
        using Type = AK::SIMD::i##vec_size;        \
    };                                             \
    template<>                                     \
    struct __MakeSigned<AK::SIMD::u##vec_size> {   \
        using Type = AK::SIMD::i##vec_size;        \
    };                                             \
    template<>                                     \
    struct __MakeUnsigned<AK::SIMD::i##vec_size> { \
        using Type = AK::SIMD::u##vec_size;        \
    };                                             \
    template<>                                     \
    struct __MakeUnsigned<AK::SIMD::u##vec_size> { \
        using Type = AK::SIMD::u##vec_size;        \
    };

MAKE__MAKESIGNED(8x2);
MAKE__MAKESIGNED(8x4);
MAKE__MAKESIGNED(8x8);
MAKE__MAKESIGNED(8x16);
MAKE__MAKESIGNED(8x32);
MAKE__MAKESIGNED(16x2);
MAKE__MAKESIGNED(16x4);
MAKE__MAKESIGNED(16x8);
MAKE__MAKESIGNED(16x16);
MAKE__MAKESIGNED(32x2);
MAKE__MAKESIGNED(32x4);
MAKE__MAKESIGNED(64x2);

#undef MAKE_MAKESIGNED
}

namespace AK::SIMD {
namespace Detail {

template<typename T>
inline constexpr bool IsSIMD = false;
template<typename T>
struct __SIMDBase {
};

template<>
inline constexpr bool IsSIMD<u8x2> = true;
template<>
struct __SIMDBase<u8x2> {
    using Type = u8;
};
template<>
inline constexpr bool IsSIMD<u8x4> = true;
template<>
struct __SIMDBase<u8x4> {
    using Type = u8;
};
template<>
inline constexpr bool IsSIMD<u8x8> = true;
template<>
struct __SIMDBase<u8x8> {
    using Type = u8;
};
template<>
inline constexpr bool IsSIMD<u8x16> = true;
template<>
struct __SIMDBase<u8x16> {
    using Type = u8;
};
template<>
inline constexpr bool IsSIMD<u8x32> = true;
template<>
struct __SIMDBase<u8x32> {
    using Type = u8;
};
template<>
inline constexpr bool IsSIMD<i8x2> = true;
template<>
struct __SIMDBase<i8x2> {
    using Type = i8;
};
template<>
inline constexpr bool IsSIMD<i8x4> = true;
template<>
struct __SIMDBase<i8x4> {
    using Type = i8;
};
template<>
inline constexpr bool IsSIMD<i8x8> = true;
template<>
struct __SIMDBase<i8x8> {
    using Type = i8;
};
template<>
inline constexpr bool IsSIMD<i8x16> = true;
template<>
struct __SIMDBase<i8x16> {
    using Type = i8;
};

template<>
inline constexpr bool IsSIMD<i8x32> = true;
template<>
struct __SIMDBase<i8x32> {
    using Type = i8;
};

template<>
inline constexpr bool IsSIMD<u16x2> = true;
template<>
struct __SIMDBase<u16x2> {
    using Type = u16;
};
template<>
inline constexpr bool IsSIMD<u16x4> = true;
template<>
struct __SIMDBase<u16x4> {
    using Type = u16;
};
template<>
inline constexpr bool IsSIMD<u16x8> = true;
template<>
struct __SIMDBase<u16x8> {
    using Type = u16;
};
template<>
inline constexpr bool IsSIMD<u16x16> = true;
template<>
struct __SIMDBase<u16x16> {
    using Type = u16;
};
template<>
inline constexpr bool IsSIMD<i16x2> = true;
template<>
struct __SIMDBase<i16x2> {
    using Type = i16;
};
template<>
inline constexpr bool IsSIMD<i16x4> = true;
template<>
struct __SIMDBase<i16x4> {
    using Type = i16;
};
template<>
inline constexpr bool IsSIMD<i16x8> = true;
template<>
struct __SIMDBase<i16x8> {
    using Type = i16;
};
template<>
inline constexpr bool IsSIMD<i16x16> = true;
template<>
struct __SIMDBase<i16x16> {
    using Type = i16;
};

template<>
inline constexpr bool IsSIMD<u32x2> = true;
template<>
struct __SIMDBase<u32x2> {
    using Type = u32;
};
template<>
inline constexpr bool IsSIMD<u32x4> = true;
template<>
struct __SIMDBase<u32x4> {
    using Type = u32;
};
template<>
inline constexpr bool IsSIMD<u32x8> = true;
template<>
struct __SIMDBase<u32x8> {
    using Type = u32;
};
template<>
inline constexpr bool IsSIMD<i32x2> = true;
template<>
struct __SIMDBase<i32x2> {
    using Type = i32;
};
template<>
inline constexpr bool IsSIMD<i32x4> = true;
template<>
struct __SIMDBase<i32x4> {
    using Type = i32;
};
template<>
inline constexpr bool IsSIMD<i32x8> = true;
template<>
struct __SIMDBase<i32x8> {
    using Type = i32;
};

template<>
inline constexpr bool IsSIMD<f32x2> = true;
template<>
struct __SIMDBase<f32x2> {
    using Type = float;
};
template<>
inline constexpr bool IsSIMD<f32x4> = true;
template<>
struct __SIMDBase<f32x4> {
    using Type = float;
};
template<>
inline constexpr bool IsSIMD<f32x8> = true;
template<>
struct __SIMDBase<f32x8> {
    using Type = float;
};
template<>
inline constexpr bool IsSIMD<f64x2> = true;
template<>
struct __SIMDBase<f64x2> {
    using Type = double;
};
template<>
inline constexpr bool IsSIMD<f64x4> = true;
template<>
struct __SIMDBase<f64x4> {
    using Type = double;
};

template<typename T>
using SIMDBase = typename __SIMDBase<T>::Type;

template<typename T>
concept SIMD = IsSIMD<T>;
template<typename T>
concept SignedSIMD = IsSIMD<T>&& IsSigned<T>;
template<typename T>
concept UnsignedSIMD = IsSIMD<T>&& IsUnsigned<T>;
template<typename T>
concept SIMDIntegral = IsIntegral<SIMDBase<T>>;
template<typename T>
concept SIMDFloatingPoint = IsFloatingPoint<SIMDBase<T>>;

template<typename T>
concept SIMDBytes = sizeof(SIMDBase<T>) == 1;
template<typename T>
concept SIMDWords = sizeof(SIMDBase<T>) == 2;
template<typename T>
concept SIMDDoubleWords = sizeof(SIMDBase<T>) == 4;
template<typename T>
concept SIMDQuadWords = sizeof(SIMDBase<T>) == 8;

}

using Detail::SignedSIMD;
using Detail::SIMD;
using Detail::UnsignedSIMD;

}
