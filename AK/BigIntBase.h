/*
 * Copyright (c) 2022, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/BuiltinWrappers.h>
#include <AK/Checked.h>
#include <AK/StdLibExtras.h>
#include <AK/Types.h>

namespace AK {

// Ideally, we want to store data in the native processor's words. However, for some algorithms,
// particularly multiplication, we require double of the amount of the native word size.
#if defined(__SIZEOF_INT128__) && defined(AK_ARCH_64_BIT)
using NativeWord = u64;
using DoubleWord = unsigned __int128;
using SignedDoubleWord = __int128;
#else
using NativeWord = u32;
using DoubleWord = u64;
using SignedDoubleWord = i64;
#endif

template<bool sign>
using ConditionallySignedDoubleWord = Conditional<sign, DoubleWord, SignedDoubleWord>;

template<typename T>
concept BuiltInUFixedInt = OneOf<T, bool, u8, u16, u32, u64, unsigned long, unsigned long long, DoubleWord>;

constexpr size_t word_size = sizeof(NativeWord) * 8;
constexpr NativeWord max_word = ~static_cast<NativeWord>(0);
static_assert(word_size == 32 || word_size == 64);

// Max big integer length is 256 MiB (2.1e9 bits) for 32-bit, 4 GiB (3.4e10 bits) for 64-bit.
constexpr size_t max_big_int_length = 1 << (word_size == 32 ? 26 : 29);

// ===== Static storage for big integers =====
struct StorageTraits {
    size_t length;
    bool is_signed;
    enum {
        NonNegative,
        Negative,
        Signed,
    } sign;

    constexpr bool maybe_negative() const
    {
        return sign != NonNegative;
    }
};

// Sometimes we want to know the exact maximum amount of the bits required to represent the number.
// However, the bit size only acts as a hint for wide multiply operations. For all other purposes,
// `bit_size`-sized and `ceil(bit_size / word_size) * word_size`-sized `StaticStorage`s will act the
// same.
template<bool is_signed_, size_t bit_size>
requires(bit_size <= max_big_int_length * word_size) struct StaticStorage {
    constexpr static size_t static_size = (bit_size + word_size - 1) / word_size;
    constexpr static bool is_signed = is_signed_;

    // We store integers in little-endian regardless of the host endianness. We use two's complement
    // representation of negative numbers and do not bother at all if `bit_size % word_size != 0`
    // (i. e. do not properly handle overflows).
    NativeWord m_data[static_size];

    constexpr bool is_negative() const
    {
        return is_signed_ && m_data[0] >> (word_size - 1);
    }

    constexpr size_t length() const
    {
        return static_size;
    }

    constexpr NativeWord operator[](size_t i) const
    {
        return m_data[i];
    }

    constexpr NativeWord& operator[](size_t i)
    {
        return m_data[i];
    }

    constexpr NativeWord const* data() const
    {
        return m_data;
    }

    constexpr NativeWord* data()
    {
        return m_data;
    }

    consteval static StorageTraits get_traits()
    {
        return {
            .length = static_size,
            .is_signed = is_signed,
            .sign = is_signed ? StorageTraits::Signed : StorageTraits::NonNegative,
        };
    }
};

// There is no reason to ban u128{0} + 1 (although the second argument type is signed, the value is
// known at the compile time to be non-negative).
template<bool is_signed>
struct IntegerWrapper {
    StaticStorage<is_signed, sizeof(int) * 8> m_data;

    constexpr void init(NativeWord value)
    {
        m_data[0] = static_cast<NativeWord>(value);
    }

    consteval IntegerWrapper(int value)
    requires(!is_signed)
    {
        if (value < 0)
            (new int)&&"Requested implicit conversion of an integer to the unsigned one will underflow.";
        init(value);
    }

    constexpr IntegerWrapper(int value)
    requires(is_signed)
    {
        init(value);
    }
};

constexpr inline auto get_storage_of(IntegerWrapper<false> value) { return value.m_data; }

template<BuiltInUFixedInt T>
constexpr StaticStorage<false, sizeof(T) * 8> get_storage_of(T value)
{
    if constexpr (sizeof(T) > sizeof(NativeWord)) {
        static_assert(sizeof(T) == 2 * sizeof(NativeWord));
        return { static_cast<NativeWord>(value), static_cast<NativeWord>(value >> word_size) };
    }
    return { static_cast<NativeWord>(value) };
}

// ===== Utilities =====
template<typename T>
ALWAYS_INLINE constexpr void taint_for_optimizer(T& value)
{
    if (!__builtin_is_constant_evaluated()) {
        asm volatile(""
                     : "+rm"(value)
                     : "rm"(value)
                     : "memory");
    }
}

ALWAYS_INLINE constexpr NativeWord extend_sign(bool sign)
{
    return sign ? max_word : 0;
}

// FIXME: If available, we might try to use AVX2 and AVX512. We should use something similar to
//        __builtin_ia32_addcarryx_u64 for GCC on !x86_64 or get PR79713 fixed.
ALWAYS_INLINE constexpr void add_words(NativeWord word1, NativeWord word2, NativeWord& output, bool& carry)
{
    if (!__builtin_is_constant_evaluated()) {
#if ARCH(X86_64)
        carry = __builtin_ia32_addcarryx_u64(carry, word1, word2, reinterpret_cast<unsigned long long*>(&output));
        return;
#else
#    if defined(AK_COMPILER_CLANG)
        NativeWord ncarry;
        if constexpr (SameAs<NativeWord, unsigned int>)
            output = __builtin_addc(word1, word2, carry, reinterpret_cast<unsigned int*>(&ncarry));
        else if constexpr (SameAs<NativeWord, unsigned long>)
            output = __builtin_addcl(word1, word2, carry, reinterpret_cast<unsigned long*>(&ncarry));
        else if constexpr (SameAs<NativeWord, unsigned long long>)
            output = __builtin_addcll(word1, word2, carry, reinterpret_cast<unsigned long long*>(&ncarry));
        else
            VERIFY_NOT_REACHED();
        carry = ncarry;
        return;
#    endif
#endif
    }
    // Note: This is usually too confusing for both GCC and Clang.
    bool ncarry = __builtin_add_overflow(word1, word2, &output);
    if (carry) {
        ++output;
        if (output == 0)
            ncarry = true;
    }
    carry = ncarry;
}

ALWAYS_INLINE constexpr void sub_words(NativeWord word1, NativeWord word2, NativeWord& output, bool& carry)
{
    if (!__builtin_is_constant_evaluated()) {
#if ARCH(X86_64)
#    if defined(AK_COMPILER_CLANG)
        // This is named __builtin_ia32_sbb_u64 in G++. *facepalm*
        carry = __builtin_ia32_subborrow_u64(carry, word1, word2, reinterpret_cast<unsigned long long*>(&output));
#    else
        carry = __builtin_ia32_sbb_u64(carry, word1, word2, reinterpret_cast<unsigned long long*>(&output));
#    endif
        return;
#else
#    if defined(AK_COMPILER_CLANG)
        NativeWord ncarry;
        if constexpr (SameAs<NativeWord, unsigned int>)
            output = __builtin_subc(word1, word2, carry, reinterpret_cast<unsigned int*>(&ncarry));
        else if constexpr (SameAs<NativeWord, unsigned long>)
            output = __builtin_subcl(word1, word2, carry, reinterpret_cast<unsigned long*>(&ncarry));
        else if constexpr (SameAs<NativeWord, unsigned long long>)
            output = __builtin_subcll(word1, word2, carry, reinterpret_cast<unsigned long long*>(&ncarry));
        else
            VERIFY_NOT_REACHED();
        carry = ncarry;
        return;
#    endif
#endif
    }
    bool ncarry = __builtin_sub_overflow(word1, word2, &output);
    if (carry) {
        if (output == 0)
            ncarry = true;
        --output;
    }
    carry = ncarry;
}

// ===== Operations on integer storages =====
constexpr void storage_copy(auto const& operand, auto& result, size_t offset = 0)
{
    auto fill = extend_sign(operand.is_negative());
    size_t size1 = operand.length(), size = result.length();

    for (size_t i = 0; i < size; ++i)
        result[i] = i + offset < size1 ? operand[i + offset] : fill;
}

// `is_for_inequality' is a hint to compiler that we do not need to differentiate between < and >.
constexpr int storage_compare(auto const& operand1, auto const& operand2, bool is_for_inequality)
{
    bool sign1 = operand1.is_negative(), sign2 = operand2.is_negative();
    size_t size1 = operand1.length(), size2 = operand2.length();

    if (sign1 != sign2) {
        if (sign1)
            return -1;
        else
            return 1;
    }

    NativeWord compare_value = extend_sign(sign1);
    bool differ_in_high_bits = false;

    if (size1 > size2) {
        for (size_t i = size1; i-- > size2;)
            if (operand1[i] != compare_value)
                differ_in_high_bits = true;
    } else if (size1 < size2) {
        for (size_t i = size2; i-- > size1;)
            if (operand2[i] != compare_value)
                differ_in_high_bits = true;
    }

    if (differ_in_high_bits)
        return (size1 > size2) ^ sign1 ? 1 : -1;

    // FIXME: Using min(size1, size2) in the next line triggers -Warray-bounds on GCC with -O2 and
    //        -fsanitize=address. I have not reported this.
    //        Reduced testcase: https://godbolt.org/z/TE3MbfhnE
    for (size_t i = (size1 > size2 ? size2 : size1); i--;) {
        auto word1 = operand1[i], word2 = operand2[i];

        if (is_for_inequality) {
            if (word1 != word2)
                return 1;
        } else {
            if (word1 > word2)
                return 1;
            else if (word1 < word2)
                return -1;
        }
    }
    return 0;
}

enum BitwiseOperation {
    AND,
    OR,
    XOR,
    INVERT,
};

#ifdef AK_COMPILER_GCC
#    define IGNORE_VECTOR_DEPENDENCIES _Pragma("GCC ivdep")
#else
#    define IGNORE_VECTOR_DEPENDENCIES
#endif

// Requirements:
//  - !operand1.is_signed && !operand2.is_signed && !result.is_signed (the function will also work
//    for signed storages but will extend them with zeroes regardless of the actual sign).
template<BitwiseOperation operation>
constexpr void storage_compute_bitwise(auto const& operand1, auto const& operand2, auto& result)
{
    size_t size1 = operand1.length(), size2 = operand2.length(), size = result.length();

    IGNORE_VECTOR_DEPENDENCIES
    for (size_t i = 0; i < size; ++i) {
        auto word1 = i < size1 ? operand1[i] : 0;
        auto word2 = i < size2 ? operand2[i] : 0;

        if constexpr (operation == AND)
            result[i] = word1 & word2;
        else if constexpr (operation == OR)
            result[i] = word1 | word2;
        else if constexpr (operation == XOR)
            result[i] = word1 ^ word2;
        else if constexpr (operation == INVERT)
            result[i] = ~word1;
        else
            static_assert(false && operation);
    }
}

// See `storage_compute_bitwise` for the signedness requirements.
template<BitwiseOperation operation>
constexpr void storage_compute_inplace_bitwise(auto const&, auto const& operand2, auto& result)
{
    size_t size2 = min(result.length(), operand2.length());

    IGNORE_VECTOR_DEPENDENCIES
    for (size_t i = 0; i < size2; ++i) {
        if constexpr (operation == AND)
            result[i] &= operand2[i];
        else if constexpr (operation == OR)
            result[i] |= operand2[i];
        else if constexpr (operation == XOR)
            result[i] ^= operand2[i];
        else
            static_assert(false && operation);
    }
}

// Requirements for the next two functions:
//  - shift < result.length() * word_size;
//  - result.length() == operand.length().
constexpr void storage_shift_left(auto const& operand, size_t shift, auto& result)
{
    size_t size = operand.length();
    size_t offset = shift / word_size, remainder = shift % word_size;

    if (shift % word_size == 0) {
        for (size_t i = size; i-- > offset;)
            result[i] = operand[i - offset];
        for (size_t i = 0; i < offset; ++i)
            result[i] = 0;
    } else {
        for (size_t i = size; --i > offset;)
            result[i] = (operand[i - offset] << remainder) | (operand[i - offset - 1] >> (word_size - remainder));
        result[offset] = operand[0] << remainder;
        for (size_t i = 0; i < offset; ++i)
            result[i] = 0;
    }
}

constexpr void storage_shift_right(auto const& operand, size_t shift, auto& result)
{
    size_t size = operand.length();
    size_t offset = shift / word_size, remainder = shift % word_size;

    if (shift % word_size == 0) {
        for (size_t i = 0; i < size - offset; ++i)
            result[i] = operand[i + offset];
        for (size_t i = size - offset; i < size; ++i)
            result[i] = 0;
    } else {
        for (size_t i = 0; i < size - offset - 1; ++i)
            result[i] = (operand[i + offset] >> remainder) | (operand[i + offset + 1] << (word_size - remainder));
        result[size - offset - 1] = operand[size - 1] >> remainder;
        for (size_t i = size - offset; i < size; ++i)
            result[i] = 0;
    }
}

// Requirements:
//  - result.size() >= max(operand1.size(), operand2.size()) (not a real constraint but overflow
//    detection will not work otherwise).
//
// Return value:
// Let r be the return value of the function and a, b, c -- the integer values stored in `operand1`,
// `operand2` and `result`, respectively. Then,
//     a + b * (-1) ** subtract = c + r * 2 ** (result.length() * word_size).
// In particular, r equals 0 iff no overflow has happened.
template<bool subtract>
constexpr int storage_add(auto const& operand1, auto const& operand2, auto& result, bool carry = false)
{
    bool sign1 = operand1.is_negative(), sign2 = operand2.is_negative();
    auto fill1 = extend_sign(sign1), fill2 = extend_sign(sign2);
    size_t size1 = operand1.length(), size2 = operand2.length(), size = result.length();

    // GCC's default unrolling is too small even for u256.
#pragma GCC unroll 16
    for (size_t i = 0; i < size; ++i) {
        auto word1 = i < size1 ? operand1[i] : fill1;
        auto word2 = i < size2 ? operand2[i] : fill2;

        if constexpr (!subtract)
            add_words(word1, word2, result[i], carry);
        else
            sub_words(word1, word2, result[i], carry);
    }

    if constexpr (!subtract)
        return -sign1 - sign2 + carry + result.is_negative();
    else
        return -sign1 + sign2 - carry + result.is_negative();
}

// See `storage_add` for the meaning of the return value.
template<bool subtract>
constexpr int storage_increment(auto& operand)
{
    bool carry = true;
    bool sign = operand.is_negative();
    size_t size = operand.length();

#pragma GCC unroll 16
    for (size_t i = 0; i < size; ++i) {
        if constexpr (!subtract)
            add_words(operand[i], 0, operand[i], carry);
        else
            sub_words(operand[i], 0, operand[i], carry);
    }

    if constexpr (!subtract)
        return -sign + carry + operand.is_negative();
    else
        return -sign - carry + operand.is_negative();
}

// Requirements:
//  - result.length() == operand.length().
//
// Return value: operand != 0.
constexpr bool storage_invert(auto const& operand, auto& result)
{
    bool carry = false;
    size_t size = operand.length();

#pragma GCC unroll 16
    for (size_t i = 0; i < size; ++i)
        sub_words(0, operand[i], result[i], carry);

    return carry;
}

template<typename Operand1, typename Operand2, typename Result>
constexpr void storage_mul(Operand1 const& operand1, Operand2 const& operand2, Result& result, NativeWord* buffer)
{
    size_t size1 = operand1.length(), size2 = operand2.length(), size = result.length();
    bool sign1 = operand1.is_negative(), sign2 = operand2.is_negative();

    if (size1 == 1 && size2 == 1) {
        // We do not want to compete with the cleverness of the compiler of multiplying NativeWords.
        ConditionallySignedDoubleWord<Operand1::is_signed> word1 = operand1[0];
        ConditionallySignedDoubleWord<Operand2::is_signed> word2 = operand2[0];
        auto value = static_cast<DoubleWord>(word1) * word2;

        result[0] = value;
        if (size > 1) {
            result[1] = value >> word_size;

            auto fill = extend_sign(sign1 ^ sign2);
            for (size_t i = 2; i < result.length(); ++i)
                result[i] = fill;
        }
        return;
    }

    if (size1 < size2) {
        storage_mul(operand2, operand1, result, buffer);
        return;
    }
    // size1 >= size2

    auto mul_schoolbook = [&](auto const& data1, auto const& data2) {
        // Do O(size^2).
        DoubleWord carry = 0;
        for (size_t i = 0; i < size; ++i) {
            result[i] = static_cast<NativeWord>(carry);
            carry >>= word_size;

            size_t start_index = i >= size2 ? i - size2 + 1 : 0;
            size_t end_index = min(i + 1, size1);

#pragma GCC unroll 16
            for (size_t j = start_index; j < end_index; ++j) {
                auto x = static_cast<DoubleWord>(data1[j]) * data2[i - j];

                bool ncarry = false;
                add_words(result[i], static_cast<NativeWord>(x), result[i], ncarry);
                carry += (x >> word_size) + ncarry;
            }
        }
    };

    if (!Operand1::is_signed && !Operand2::is_signed) {
        mul_schoolbook(operand1, operand2);
        return;
    }

    size_t buffer_offset = 0;
    auto data1 = operand1.data(), data2 = operand2.data();

    if (size2 < size) {
        if (sign1) {
            auto* ptr = buffer + buffer_offset;
            storage_invert(operand1, ptr);
            data1 = ptr;
            buffer_offset += size1;
        }
        if (sign2) {
            auto* ptr = buffer + buffer_offset;
            storage_invert(operand2, ptr);
            data2 = ptr;
            buffer_offset += size2;
        }
    }

    size1 = min(size1, size), size2 = min(size2, size);

    mul_schoolbook(data1, data2);

    if (size2 < size && (sign1 ^ sign2))
        storage_invert(result, result);
}

constexpr size_t storage_mul_buffer_size(StorageTraits operand1, StorageTraits operand2, StorageTraits result)
{
    size_t size1 = operand1.length, size2 = operand2.length, size = result.length;

    if (size1 == 1 && size2 == 1)
        return 0;
    if (size1 < size2)
        return storage_mul_buffer_size(operand2, operand1, result);
    // size1 >= size2

    if (!operand1.is_signed && !operand2.is_signed)
        return 0;

    size_t amount = 0;
    if (size2 < size) {
        if (operand1.maybe_negative())
            amount += operand1.length;
        if (operand2.maybe_negative())
            amount += operand2.length;
    }

    return amount;
}

}
