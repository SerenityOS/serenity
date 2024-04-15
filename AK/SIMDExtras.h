/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/SIMD.h>

// Functions returning vectors or accepting vector arguments have different calling conventions
// depending on whether the target architecture supports SSE or not. GCC generates warning "psabi"
// when compiling for non-SSE architectures. We disable this warning because these functions
// are static and should never be visible from outside the translation unit that includes this header.
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wpsabi"

namespace AK::SIMD {

// SIMD Vector Expansion

ALWAYS_INLINE static constexpr f32x4 expand4(float f)
{
    return f32x4 { f, f, f, f };
}

ALWAYS_INLINE static constexpr i32x4 expand4(i32 i)
{
    return i32x4 { i, i, i, i };
}

ALWAYS_INLINE static constexpr u32x4 expand4(u32 u)
{
    return u32x4 { u, u, u, u };
}

// Casting

template<typename TSrc>
ALWAYS_INLINE static u8x4 to_u8x4(TSrc v)
{
    return __builtin_convertvector(v, u8x4);
}

template<typename TSrc>
ALWAYS_INLINE static u16x4 to_u16x4(TSrc v)
{
    return __builtin_convertvector(v, u16x4);
}

template<typename TSrc>
ALWAYS_INLINE static u32x4 to_u32x4(TSrc v)
{
    return __builtin_convertvector(v, u32x4);
}

template<typename TSrc>
ALWAYS_INLINE static i32x4 to_i32x4(TSrc v)
{
    return __builtin_convertvector(v, i32x4);
}

template<typename TSrc>
ALWAYS_INLINE static f32x4 to_f32x4(TSrc v)
{
    return __builtin_convertvector(v, f32x4);
}

// Masking

ALWAYS_INLINE static i32 maskbits(i32x4 mask)
{
#if defined(__SSE__)
    return __builtin_ia32_movmskps((f32x4)mask);
#else
    return ((mask[0] & 0x80000000) >> 31) | ((mask[1] & 0x80000000) >> 30) | ((mask[2] & 0x80000000) >> 29) | ((mask[3] & 0x80000000) >> 28);
#endif
}

ALWAYS_INLINE static bool all(i32x4 mask)
{
    return maskbits(mask) == 15;
}

ALWAYS_INLINE static bool any(i32x4 mask)
{
    return maskbits(mask) != 0;
}

ALWAYS_INLINE static bool none(i32x4 mask)
{
    return maskbits(mask) == 0;
}

ALWAYS_INLINE static int maskcount(i32x4 mask)
{
    constexpr static int count_lut[16] { 0, 1, 1, 2, 1, 2, 2, 3, 1, 2, 2, 3, 2, 3, 3, 4 };
    return count_lut[maskbits(mask)];
}

// Load / Store

template<SIMDVector VectorType>
ALWAYS_INLINE static VectorType load_unaligned(void const* a)
{
    VectorType v;
    __builtin_memcpy(&v, a, sizeof(VectorType));
    return v;
}

template<SIMDVector VectorType>
ALWAYS_INLINE static void store_unaligned(void* a, VectorType const& v)
{
    // FIXME: Does this generate the right instructions?
    __builtin_memcpy(a, &v, sizeof(VectorType));
}

ALWAYS_INLINE static f32x4 load4(float const* a, float const* b, float const* c, float const* d)
{
    return f32x4 { *a, *b, *c, *d };
}

ALWAYS_INLINE static u32x4 load4(u32 const* a, u32 const* b, u32 const* c, u32 const* d)
{
    return u32x4 { *a, *b, *c, *d };
}

ALWAYS_INLINE static f32x4 load4_masked(float const* a, float const* b, float const* c, float const* d, i32x4 mask)
{
    int bits = maskbits(mask);
    return f32x4 {
        bits & 1 ? *a : 0.f,
        bits & 2 ? *b : 0.f,
        bits & 4 ? *c : 0.f,
        bits & 8 ? *d : 0.f,
    };
}

ALWAYS_INLINE static i32x4 load4_masked(u8 const* a, u8 const* b, u8 const* c, u8 const* d, i32x4 mask)
{
    int bits = maskbits(mask);
    return i32x4 {
        bits & 1 ? *a : 0,
        bits & 2 ? *b : 0,
        bits & 4 ? *c : 0,
        bits & 8 ? *d : 0,
    };
}

ALWAYS_INLINE static u32x4 load4_masked(u32 const* a, u32 const* b, u32 const* c, u32 const* d, i32x4 mask)
{
    int bits = maskbits(mask);
    return u32x4 {
        bits & 1 ? *a : 0u,
        bits & 2 ? *b : 0u,
        bits & 4 ? *c : 0u,
        bits & 8 ? *d : 0u,
    };
}

template<typename VectorType, typename UnderlyingType = decltype(declval<VectorType>()[0])>
ALWAYS_INLINE static void store4(VectorType v, UnderlyingType* a, UnderlyingType* b, UnderlyingType* c, UnderlyingType* d)
{
    *a = v[0];
    *b = v[1];
    *c = v[2];
    *d = v[3];
}

template<typename VectorType, typename UnderlyingType = decltype(declval<VectorType>()[0])>
ALWAYS_INLINE static void store4_masked(VectorType v, UnderlyingType* a, UnderlyingType* b, UnderlyingType* c, UnderlyingType* d, i32x4 mask)
{
    int bits = maskbits(mask);
    if (bits & 1)
        *a = v[0];
    if (bits & 2)
        *b = v[1];
    if (bits & 4)
        *c = v[2];
    if (bits & 8)
        *d = v[3];
}

// Shuffle
namespace Detail {
template<SIMDVector T, SIMDVector Control, size_t... Idx>
ALWAYS_INLINE static T shuffle_impl(T a, Control control, IndexSequence<Idx...>)
{
    // FIXME: Maybe make the VERIFYs optional, eg on SIMD-DEBUG, to avoid the overhead in performance oriented users, like LibWasm::SIMD
    // Note: - instead of _ to make the linter happy, as SIMD-DEBUG does not (yet) exist
    constexpr Conditional<IsSigned<ElementOf<Control>>, ssize_t, size_t> N = vector_length<T>;
    (VERIFY(control[Idx] < N), ...);
    return T {
        a[control[Idx]]...
    };
}

template<SIMDVector T, size_t... Idx>
ALWAYS_INLINE static T item_reverse_impl(T a, IndexSequence<Idx...>)
{
    constexpr size_t N = vector_length<T>;
    return __builtin_shufflevector(a, a, N - 1 - Idx...);
}

template<SIMDVector T, size_t... Idx>
ALWAYS_INLINE static T byte_reverse_impl(T a, IndexSequence<Idx...>)
{
    static_assert(sizeof...(Idx) == sizeof(T));
    constexpr size_t N = sizeof(T);
    using BytesVector = Conditional<sizeof(T) == 2, u8x2, Conditional<sizeof(T) == 4, u8x4, Conditional<sizeof(T) == 8, u8x8, Conditional<sizeof(T) == 16, u8x16, Conditional<sizeof(T) == 32, u8x32, void>>>>>;
    static_assert(sizeof(BytesVector) == sizeof(T));
    auto tmp = __builtin_shufflevector(
        *reinterpret_cast<BytesVector*>(&a),
        *reinterpret_cast<BytesVector*>(&a),
        N - 1 - Idx...);
    return *reinterpret_cast<T*>(&tmp);
}

template<SIMDVector T, size_t... Idx>
ALWAYS_INLINE static T elementwise_byte_reverse_impl(T a, IndexSequence<Idx...>)
{
    static_assert(sizeof...(Idx) == vector_length<T>);
    using Element = ElementOf<T>;
    if constexpr (sizeof(Element) == 1) {
        return a;
    } else if constexpr (sizeof(Element) == 2) {
        return T {
            static_cast<Element>(__builtin_bswap16(static_cast<u16>(a[Idx])))...
        };
    } else if constexpr (sizeof(Element) == 4) {
        return T {
            static_cast<Element>(__builtin_bswap32(static_cast<u32>(a[Idx])))...
        };
    } else if constexpr (sizeof(Element) == 8) {
        return T {
            static_cast<Element>(__builtin_bswap64(static_cast<u64>(a[Idx])))...
        };
    } else {
        static_assert(DependentFalse<T>);
    }
}

}

// FIXME: Shuffles only work with integral types for now
template<SIMDVector T>
ALWAYS_INLINE static T shuffle(T a, IndexVectorFor<T> control)
{
    return Detail::shuffle_impl(a, control, MakeIndexSequence<vector_length<T>>());
}

template<SIMDVector T>
ALWAYS_INLINE static T item_reverse(T a)
{
    return Detail::item_reverse_impl(a, MakeIndexSequence<vector_length<T>>());
}

template<SIMDVector T>
ALWAYS_INLINE static T byte_reverse(T a)
{
    return Detail::byte_reverse_impl(a, MakeIndexSequence<sizeof(T)>());
}

template<SIMDVector T>
ALWAYS_INLINE static T elementwise_byte_reverse(T a)
{
    return Detail::elementwise_byte_reverse_impl(a, MakeIndexSequence<vector_length<T>>());
}

}

#pragma GCC diagnostic pop
