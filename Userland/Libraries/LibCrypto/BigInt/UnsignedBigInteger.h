/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
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

    explicit UnsignedBigInteger(u8 const* ptr, size_t length);

    UnsignedBigInteger() = default;

    static UnsignedBigInteger create_invalid();

    static UnsignedBigInteger import_data(StringView data) { return import_data((u8 const*)data.characters_without_null_termination(), data.length()); }
    static UnsignedBigInteger import_data(u8 const* ptr, size_t length)
    {
        return UnsignedBigInteger(ptr, length);
    }

    static UnsignedBigInteger create_from(u64 value)
    {
        VERIFY(sizeof(Word) == 4);
        UnsignedBigInteger integer;
        integer.m_words.resize(2);
        integer.m_words[0] = static_cast<Word>(value & 0xFFFFFFFF);
        integer.m_words[1] = static_cast<Word>((value >> 32) & 0xFFFFFFFF);
        return integer;
    }

    size_t export_data(Bytes, bool remove_leading_zeros = false) const;

    static UnsignedBigInteger from_base(u16 N, StringView str);
    String to_base(u16 N) const;

    u64 to_u64() const;
    double to_double() const;

    Vector<Word, STARTING_WORD_SIZE> const& words() const { return m_words; }

    void set_to_0();
    void set_to(Word other);
    void set_to(UnsignedBigInteger const& other);

    void invalidate()
    {
        m_is_invalid = true;
        m_cached_trimmed_length = {};
        m_cached_hash = 0;
    }

    bool is_zero() const;
    bool is_odd() const { return m_words.size() && (m_words[0] & 1); }
    bool is_invalid() const { return m_is_invalid; }

    size_t length() const { return m_words.size(); }
    // The "trimmed length" is the number of words after trimming leading zeroed words
    size_t trimmed_length() const;

    void clamp_to_trimmed_length();
    void resize_with_leading_zeros(size_t num_words);

    size_t one_based_index_of_highest_set_bit() const;

    UnsignedBigInteger plus(UnsignedBigInteger const& other) const;
    UnsignedBigInteger minus(UnsignedBigInteger const& other) const;
    UnsignedBigInteger bitwise_or(UnsignedBigInteger const& other) const;
    UnsignedBigInteger bitwise_and(UnsignedBigInteger const& other) const;
    UnsignedBigInteger bitwise_xor(UnsignedBigInteger const& other) const;
    UnsignedBigInteger bitwise_not_fill_to_one_based_index(size_t) const;
    UnsignedBigInteger shift_left(size_t num_bits) const;
    UnsignedBigInteger multiplied_by(UnsignedBigInteger const& other) const;
    UnsignedDivisionResult divided_by(UnsignedBigInteger const& divisor) const;

    u32 hash() const;

    void set_bit_inplace(size_t bit_index);

    bool operator==(UnsignedBigInteger const& other) const;
    bool operator!=(UnsignedBigInteger const& other) const;
    bool operator<(UnsignedBigInteger const& other) const;
    bool operator>(UnsignedBigInteger const& other) const;
    bool operator>=(UnsignedBigInteger const& other) const;

private:
    friend class UnsignedBigIntegerAlgorithms;
    // Little endian
    // m_word[0] + m_word[1] * Word::MAX + m_word[2] * Word::MAX * Word::MAX + ...
    Vector<Word, STARTING_WORD_SIZE> m_words;

    mutable u32 m_cached_hash { 0 };

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
    ErrorOr<void> format(FormatBuilder&, Crypto::UnsignedBigInteger const&);
};

inline Crypto::UnsignedBigInteger
operator""_bigint(char const* string, size_t length)
{
    return Crypto::UnsignedBigInteger::from_base(10, { string, length });
}
