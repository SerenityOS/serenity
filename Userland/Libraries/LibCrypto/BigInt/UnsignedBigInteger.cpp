/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "UnsignedBigInteger.h"
#include <AK/CharacterTypes.h>
#include <AK/StringBuilder.h>
#include <AK/StringHash.h>
#include <LibCrypto/BigInt/Algorithms/UnsignedBigIntegerAlgorithms.h>

namespace Crypto {

UnsignedBigInteger::UnsignedBigInteger(const u8* ptr, size_t length)
{
    m_words.resize_and_keep_capacity((length + sizeof(u32) - 1) / sizeof(u32));
    size_t in = length, out = 0;
    while (in >= sizeof(u32)) {
        in -= sizeof(u32);
        u32 word = ((u32)ptr[in] << 24) | ((u32)ptr[in + 1] << 16) | ((u32)ptr[in + 2] << 8) | (u32)ptr[in + 3];
        m_words[out++] = word;
    }
    if (in > 0) {
        u32 word = 0;
        for (size_t i = 0; i < in; i++) {
            word <<= 8;
            word |= (u32)ptr[i];
        }
        m_words[out++] = word;
    }
}

UnsignedBigInteger UnsignedBigInteger::create_invalid()
{
    UnsignedBigInteger invalid(0);
    invalid.invalidate();
    return invalid;
}

size_t UnsignedBigInteger::export_data(Bytes data, bool remove_leading_zeros) const
{
    size_t word_count = trimmed_length();
    size_t out = 0;
    if (word_count > 0) {
        ssize_t leading_zeros = -1;
        if (remove_leading_zeros) {
            UnsignedBigInteger::Word word = m_words[word_count - 1];
            for (size_t i = 0; i < sizeof(u32); i++) {
                u8 byte = (u8)(word >> ((sizeof(u32) - i - 1) * 8));
                data[out++] = byte;
                if (leading_zeros < 0 && byte != 0)
                    leading_zeros = (int)i;
            }
        }
        for (size_t i = word_count - (remove_leading_zeros ? 1 : 0); i > 0; i--) {
            auto word = m_words[i - 1];
            data[out++] = (u8)(word >> 24);
            data[out++] = (u8)(word >> 16);
            data[out++] = (u8)(word >> 8);
            data[out++] = (u8)word;
        }
        if (leading_zeros > 0)
            out -= leading_zeros;
    }
    return out;
}

UnsignedBigInteger UnsignedBigInteger::from_base(u16 N, const String& str)
{
    VERIFY(N <= 36);
    UnsignedBigInteger result;
    UnsignedBigInteger base { N };

    for (auto& c : str) {
        if (c == '_')
            continue;
        result = result.multiplied_by(base).plus(parse_ascii_base36_digit(c));
    }
    return result;
}

String UnsignedBigInteger::to_base(u16 N) const
{
    VERIFY(N <= 36);
    if (*this == UnsignedBigInteger { 0 })
        return "0";

    StringBuilder builder;
    UnsignedBigInteger temp(*this);
    UnsignedBigInteger quotient;
    UnsignedBigInteger remainder;

    while (temp != UnsignedBigInteger { 0 }) {
        UnsignedBigIntegerAlgorithms::divide_u16_without_allocation(temp, N, quotient, remainder);
        VERIFY(remainder.words()[0] < N);
        builder.append(to_ascii_base36_digit(remainder.words()[0]));
        temp.set_to(quotient);
    }

    return builder.to_string().reverse();
}

u64 UnsignedBigInteger::to_u64() const
{
    VERIFY(sizeof(Word) == 4);
    if (!length())
        return 0;
    u64 value = m_words[0];
    if (length() > 1)
        value |= static_cast<u64>(m_words[1]) << 32;
    return value;
}

double UnsignedBigInteger::to_double() const
{
    // FIXME: I am naive
    return static_cast<double>(to_u64());
}

void UnsignedBigInteger::set_to_0()
{
    m_words.clear_with_capacity();
    m_is_invalid = false;
    m_cached_trimmed_length = {};
    m_cached_hash = 0;
}

void UnsignedBigInteger::set_to(UnsignedBigInteger::Word other)
{
    m_is_invalid = false;
    m_words.resize_and_keep_capacity(1);
    m_words[0] = other;
    m_cached_trimmed_length = {};
    m_cached_hash = 0;
}

void UnsignedBigInteger::set_to(const UnsignedBigInteger& other)
{
    m_is_invalid = other.m_is_invalid;
    m_words.resize_and_keep_capacity(other.m_words.size());
    __builtin_memcpy(m_words.data(), other.m_words.data(), other.m_words.size() * sizeof(u32));
    m_cached_trimmed_length = {};
    m_cached_hash = 0;
}

size_t UnsignedBigInteger::trimmed_length() const
{
    if (!m_cached_trimmed_length.has_value()) {
        size_t num_leading_zeroes = 0;
        for (int i = length() - 1; i >= 0; --i, ++num_leading_zeroes) {
            if (m_words[i] != 0)
                break;
        }
        m_cached_trimmed_length = length() - num_leading_zeroes;
    }
    return m_cached_trimmed_length.value();
}

void UnsignedBigInteger::clamp_to_trimmed_length()
{
    auto length = trimmed_length();
    if (m_words.size() > length)
        m_words.resize(length);
}

void UnsignedBigInteger::resize_with_leading_zeros(size_t new_length)
{
    size_t old_length = length();
    if (old_length < new_length) {
        m_words.resize_and_keep_capacity(new_length);
        __builtin_memset(&m_words.data()[old_length], 0, (new_length - old_length) * sizeof(u32));
    }
}

FLATTEN UnsignedBigInteger UnsignedBigInteger::plus(const UnsignedBigInteger& other) const
{
    UnsignedBigInteger result;

    UnsignedBigIntegerAlgorithms::add_without_allocation(*this, other, result);

    return result;
}

FLATTEN UnsignedBigInteger UnsignedBigInteger::minus(const UnsignedBigInteger& other) const
{
    UnsignedBigInteger result;

    UnsignedBigIntegerAlgorithms::subtract_without_allocation(*this, other, result);

    return result;
}

FLATTEN UnsignedBigInteger UnsignedBigInteger::bitwise_or(const UnsignedBigInteger& other) const
{
    UnsignedBigInteger result;

    UnsignedBigIntegerAlgorithms::bitwise_or_without_allocation(*this, other, result);

    return result;
}

FLATTEN UnsignedBigInteger UnsignedBigInteger::bitwise_and(const UnsignedBigInteger& other) const
{
    UnsignedBigInteger result;

    UnsignedBigIntegerAlgorithms::bitwise_and_without_allocation(*this, other, result);

    return result;
}

FLATTEN UnsignedBigInteger UnsignedBigInteger::bitwise_xor(const UnsignedBigInteger& other) const
{
    UnsignedBigInteger result;

    UnsignedBigIntegerAlgorithms::bitwise_xor_without_allocation(*this, other, result);

    return result;
}

FLATTEN UnsignedBigInteger UnsignedBigInteger::bitwise_not() const
{
    UnsignedBigInteger result;

    UnsignedBigIntegerAlgorithms::bitwise_not_without_allocation(*this, result);

    return result;
}

FLATTEN UnsignedBigInteger UnsignedBigInteger::shift_left(size_t num_bits) const
{
    UnsignedBigInteger output;
    UnsignedBigInteger temp_result;
    UnsignedBigInteger temp_plus;

    UnsignedBigIntegerAlgorithms::shift_left_without_allocation(*this, num_bits, temp_result, temp_plus, output);

    return output;
}

FLATTEN UnsignedBigInteger UnsignedBigInteger::multiplied_by(const UnsignedBigInteger& other) const
{
    UnsignedBigInteger result;
    UnsignedBigInteger temp_shift_result;
    UnsignedBigInteger temp_shift_plus;
    UnsignedBigInteger temp_shift;

    UnsignedBigIntegerAlgorithms::multiply_without_allocation(*this, other, temp_shift_result, temp_shift_plus, temp_shift, result);

    return result;
}

FLATTEN UnsignedDivisionResult UnsignedBigInteger::divided_by(const UnsignedBigInteger& divisor) const
{
    UnsignedBigInteger quotient;
    UnsignedBigInteger remainder;

    // If we actually have a u16-compatible divisor, short-circuit to the
    // less computationally-intensive "divide_u16_without_allocation" method.
    if (divisor.trimmed_length() == 1 && divisor.m_words[0] < (1 << 16)) {
        UnsignedBigIntegerAlgorithms::divide_u16_without_allocation(*this, divisor.m_words[0], quotient, remainder);
        return UnsignedDivisionResult { quotient, remainder };
    }

    UnsignedBigInteger temp_shift_result;
    UnsignedBigInteger temp_shift_plus;
    UnsignedBigInteger temp_shift;
    UnsignedBigInteger temp_minus;

    UnsignedBigIntegerAlgorithms::divide_without_allocation(*this, divisor, temp_shift_result, temp_shift_plus, temp_shift, temp_minus, quotient, remainder);

    return UnsignedDivisionResult { quotient, remainder };
}

u32 UnsignedBigInteger::hash() const
{
    if (m_cached_hash != 0)
        return m_cached_hash;

    return m_cached_hash = string_hash((const char*)m_words.data(), sizeof(Word) * m_words.size());
}

void UnsignedBigInteger::set_bit_inplace(size_t bit_index)
{
    const size_t word_index = bit_index / UnsignedBigInteger::BITS_IN_WORD;
    const size_t inner_word_index = bit_index % UnsignedBigInteger::BITS_IN_WORD;

    m_words.ensure_capacity(word_index + 1);

    for (size_t i = length(); i <= word_index; ++i) {
        m_words.unchecked_append(0);
    }
    m_words[word_index] |= (1 << inner_word_index);

    m_cached_trimmed_length = {};
    m_cached_hash = 0;
}

bool UnsignedBigInteger::operator==(const UnsignedBigInteger& other) const
{
    if (is_invalid() != other.is_invalid())
        return false;

    auto length = trimmed_length();

    if (length != other.trimmed_length())
        return false;

    return !__builtin_memcmp(m_words.data(), other.words().data(), length * (BITS_IN_WORD / 8));
}

bool UnsignedBigInteger::operator!=(const UnsignedBigInteger& other) const
{
    return !(*this == other);
}

bool UnsignedBigInteger::operator<(const UnsignedBigInteger& other) const
{
    auto length = trimmed_length();
    auto other_length = other.trimmed_length();

    if (length < other_length) {
        return true;
    }

    if (length > other_length) {
        return false;
    }

    if (length == 0) {
        return false;
    }
    for (int i = length - 1; i >= 0; --i) {
        if (m_words[i] == other.m_words[i])
            continue;
        return m_words[i] < other.m_words[i];
    }
    return false;
}

bool UnsignedBigInteger::operator>(const UnsignedBigInteger& other) const
{
    return *this != other && !(*this < other);
}

bool UnsignedBigInteger::operator>=(UnsignedBigInteger const& other) const
{
    return *this > other || *this == other;
}

}

ErrorOr<void> AK::Formatter<Crypto::UnsignedBigInteger>::format(FormatBuilder& fmtbuilder, const Crypto::UnsignedBigInteger& value)
{
    if (value.is_invalid())
        return Formatter<StringView>::format(fmtbuilder, "invalid");

    StringBuilder builder;
    for (int i = value.length() - 1; i >= 0; --i)
        TRY(builder.try_appendff("{}|", value.words()[i]));

    return Formatter<StringView>::format(fmtbuilder, builder.string_view());
}
