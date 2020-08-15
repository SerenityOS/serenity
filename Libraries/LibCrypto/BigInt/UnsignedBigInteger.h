/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/LogStream.h>
#include <AK/Span.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <AK/Vector.h>

namespace Crypto {

struct UnsignedDivisionResult;
constexpr size_t STARTING_WORD_SIZE = 512;

class UnsignedBigInteger {
public:
    UnsignedBigInteger(u32 x) { m_words.append(x); }

    explicit UnsignedBigInteger(AK::Vector<u32, STARTING_WORD_SIZE>&& words)
        : m_words(move(words))
    {
    }

    explicit UnsignedBigInteger(const u8* ptr, size_t length);

    UnsignedBigInteger() { }

    static UnsignedBigInteger create_invalid();

    static UnsignedBigInteger import_data(const AK::StringView& data) { return import_data((const u8*)data.characters_without_null_termination(), data.length()); }
    static UnsignedBigInteger import_data(const u8* ptr, size_t length)
    {
        return UnsignedBigInteger(ptr, length);
    }

    size_t export_data(Bytes, bool remove_leading_zeros = false) const;

    static UnsignedBigInteger from_base10(const String& str);
    String to_base10() const;

    const AK::Vector<u32, STARTING_WORD_SIZE>& words() const { return m_words; }

    void set_to_0();
    void set_to(u32 other);
    void set_to(const UnsignedBigInteger& other);

    void invalidate()
    {
        m_is_invalid = true;
        m_cached_trimmed_length = {};
    }

    bool is_invalid() const { return m_is_invalid; }

    size_t length() const { return m_words.size(); }
    // The "trimmed length" is the number of words after trimming leading zeroed words
    size_t trimmed_length() const;

    UnsignedBigInteger plus(const UnsignedBigInteger& other) const;
    UnsignedBigInteger minus(const UnsignedBigInteger& other) const;
    UnsignedBigInteger bitwise_or(const UnsignedBigInteger& other) const;
    UnsignedBigInteger bitwise_and(const UnsignedBigInteger& other) const;
    UnsignedBigInteger bitwise_xor(const UnsignedBigInteger& other) const;
    UnsignedBigInteger bitwise_not() const;
    UnsignedBigInteger shift_left(size_t num_bits) const;
    UnsignedBigInteger multiplied_by(const UnsignedBigInteger& other) const;
    UnsignedDivisionResult divided_by(const UnsignedBigInteger& divisor) const;

    void set_bit_inplace(size_t bit_index);

    static void add_without_allocation(const UnsignedBigInteger& left, const UnsignedBigInteger& right, UnsignedBigInteger& output);
    static void subtract_without_allocation(const UnsignedBigInteger& left, const UnsignedBigInteger& right, UnsignedBigInteger& output);
    static void bitwise_or_without_allocation(const UnsignedBigInteger& left, const UnsignedBigInteger& right, UnsignedBigInteger& output);
    static void bitwise_and_without_allocation(const UnsignedBigInteger& left, const UnsignedBigInteger& right, UnsignedBigInteger& output);
    static void bitwise_xor_without_allocation(const UnsignedBigInteger& left, const UnsignedBigInteger& right, UnsignedBigInteger& output);
    static void bitwise_not_without_allocation(const UnsignedBigInteger& left, UnsignedBigInteger& output);
    static void shift_left_without_allocation(const UnsignedBigInteger& number, size_t bits_to_shift_by, UnsignedBigInteger& temp_result, UnsignedBigInteger& temp_plus, UnsignedBigInteger& output);
    static void multiply_without_allocation(const UnsignedBigInteger& left, const UnsignedBigInteger& right, UnsignedBigInteger& temp_shift_result, UnsignedBigInteger& temp_shift_plus, UnsignedBigInteger& temp_shift, UnsignedBigInteger& temp_plus, UnsignedBigInteger& output);
    static void divide_without_allocation(const UnsignedBigInteger& numerator, const UnsignedBigInteger& denominator, UnsignedBigInteger& temp_shift_result, UnsignedBigInteger& temp_shift_plus, UnsignedBigInteger& temp_shift, UnsignedBigInteger& temp_minus, UnsignedBigInteger& quotient, UnsignedBigInteger& remainder);
    static void divide_u16_without_allocation(const UnsignedBigInteger& numerator, u32 denominator, UnsignedBigInteger& quotient, UnsignedBigInteger& remainder);

    bool operator==(const UnsignedBigInteger& other) const;
    bool operator!=(const UnsignedBigInteger& other) const;
    bool operator<(const UnsignedBigInteger& other) const;

private:
    ALWAYS_INLINE static void shift_left_by_n_words(const UnsignedBigInteger& number, size_t number_of_words, UnsignedBigInteger& output);
    ALWAYS_INLINE static u32 shift_left_get_one_word(const UnsignedBigInteger& number, size_t num_bits, size_t result_word_index);

    static constexpr size_t BITS_IN_WORD = 32;
    // Little endian
    // m_word[0] + m_word[1] * 256 + m_word[2] * 65536 + ...
    AK::Vector<u32, STARTING_WORD_SIZE> m_words;

    // Used to indicate a negative result, or a result of an invalid operation
    bool m_is_invalid { false };

    mutable Optional<size_t> m_cached_trimmed_length;
};

struct UnsignedDivisionResult {
    Crypto::UnsignedBigInteger quotient;
    Crypto::UnsignedBigInteger remainder;
};

}

inline const LogStream&
operator<<(const LogStream& stream, const Crypto::UnsignedBigInteger& value)
{
    if (value.is_invalid()) {
        stream << "Invalid BigInt";
        return stream;
    }
    for (int i = value.length() - 1; i >= 0; --i) {
        stream << value.words()[i] << "|";
    }
    return stream;
}

inline Crypto::UnsignedBigInteger
operator""_bigint(const char* string, size_t length)
{
    return Crypto::UnsignedBigInteger::from_base10({ string, length });
}
