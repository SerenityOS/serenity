/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Span.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <AK/Vector.h>

namespace Crypto {

struct UnsignedDivisionResult;
constexpr size_t STARTING_WORD_SIZE = 32;

class UnsignedBigInteger {
public:
    using Word = u32;
    static constexpr size_t BITS_IN_WORD = 32;

    UnsignedBigInteger(Word x) { m_words.append(x); }

    explicit UnsignedBigInteger(Vector<Word, STARTING_WORD_SIZE>&& words)
        : m_words(move(words))
    {
    }

    explicit UnsignedBigInteger(const u8* ptr, size_t length);

    UnsignedBigInteger() { }

    static UnsignedBigInteger create_invalid();

    static UnsignedBigInteger import_data(const StringView& data) { return import_data((const u8*)data.characters_without_null_termination(), data.length()); }
    static UnsignedBigInteger import_data(const u8* ptr, size_t length)
    {
        return UnsignedBigInteger(ptr, length);
    }

    size_t export_data(Bytes, bool remove_leading_zeros = false) const;

    static UnsignedBigInteger from_base10(const String& str);
    String to_base10() const;

    const Vector<Word, STARTING_WORD_SIZE>& words() const { return m_words; }

    void set_to_0();
    void set_to(Word other);
    void set_to(const UnsignedBigInteger& other);

    void invalidate()
    {
        m_is_invalid = true;
        m_cached_trimmed_length = {};
    }

    bool is_odd() const { return m_words.size() && (m_words[0] & 1); }
    bool is_invalid() const { return m_is_invalid; }

    size_t length() const { return m_words.size(); }
    // The "trimmed length" is the number of words after trimming leading zeroed words
    size_t trimmed_length() const;

    void clamp_to_trimmed_length();
    void resize_with_leading_zeros(size_t num_words);

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

    bool operator==(const UnsignedBigInteger& other) const;
    bool operator!=(const UnsignedBigInteger& other) const;
    bool operator<(const UnsignedBigInteger& other) const;

private:
    friend class UnsignedBigIntegerAlgorithms;
    // Little endian
    // m_word[0] + m_word[1] * Word::MAX + m_word[2] * Word::MAX * Word::MAX + ...
    Vector<Word, STARTING_WORD_SIZE> m_words;

    // Used to indicate a negative result, or a result of an invalid operation
    bool m_is_invalid { false };

    mutable Optional<size_t> m_cached_trimmed_length;
};

struct UnsignedDivisionResult {
    Crypto::UnsignedBigInteger quotient;
    Crypto::UnsignedBigInteger remainder;
};

}

template<>
struct AK::Formatter<Crypto::UnsignedBigInteger> : Formatter<StringView> {
    void format(FormatBuilder&, const Crypto::UnsignedBigInteger&);
};

inline Crypto::UnsignedBigInteger
operator""_bigint(const char* string, size_t length)
{
    return Crypto::UnsignedBigInteger::from_base10({ string, length });
}
