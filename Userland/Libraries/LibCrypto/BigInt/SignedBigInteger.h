/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Span.h>
#include <LibCrypto/BigInt/UnsignedBigInteger.h>

namespace Crypto {

struct SignedDivisionResult;

class SignedBigInteger {
public:
    SignedBigInteger(i32 x)
        : m_sign(x < 0)
        , m_unsigned_data(abs(x))
    {
    }

    SignedBigInteger(UnsignedBigInteger&& unsigned_data, bool sign)
        : m_sign(sign)
        , m_unsigned_data(move(unsigned_data))
    {
    }

    explicit SignedBigInteger(UnsignedBigInteger unsigned_data)
        : m_sign(false)
        , m_unsigned_data(move(unsigned_data))
    {
    }

    SignedBigInteger()
        : m_sign(false)
        , m_unsigned_data()
    {
    }

    static SignedBigInteger create_invalid()
    {
        return { UnsignedBigInteger::create_invalid(), false };
    }

    static SignedBigInteger import_data(StringView data) { return import_data((const u8*)data.characters_without_null_termination(), data.length()); }
    static SignedBigInteger import_data(const u8* ptr, size_t length);

    static SignedBigInteger create_from(i64 value)
    {
        auto sign = false;
        u64 unsigned_value;
        if (value < 0) {
            unsigned_value = static_cast<u64>(-(value + 1)) + 1;
            sign = true;
        } else {
            unsigned_value = value;
        }
        return SignedBigInteger { UnsignedBigInteger::create_from(unsigned_value), sign };
    }

    size_t export_data(Bytes, bool remove_leading_zeros = false) const;

    static SignedBigInteger from_base(u16 N, StringView str);
    String to_base(u16 N) const;

    u64 to_u64() const;
    double to_double() const;

    const UnsignedBigInteger& unsigned_value() const { return m_unsigned_data; }
    const Vector<u32, STARTING_WORD_SIZE> words() const { return m_unsigned_data.words(); }
    bool is_negative() const { return m_sign; }

    void negate() { m_sign = !m_sign; }

    void set_to_0() { m_unsigned_data.set_to_0(); }
    void set_to(i32 other)
    {
        m_unsigned_data.set_to((u32)other);
        m_sign = other < 0;
    }
    void set_to(const SignedBigInteger& other)
    {
        m_unsigned_data.set_to(other.m_unsigned_data);
        m_sign = other.m_sign;
    }

    void invalidate()
    {
        m_unsigned_data.invalidate();
    }

    bool is_invalid() const { return m_unsigned_data.is_invalid(); }

    // These get + 1 byte for the sign.
    size_t length() const { return m_unsigned_data.length() + 1; }
    size_t trimmed_length() const { return m_unsigned_data.trimmed_length() + 1; };

    SignedBigInteger plus(const SignedBigInteger& other) const;
    SignedBigInteger minus(const SignedBigInteger& other) const;
    SignedBigInteger bitwise_or(const SignedBigInteger& other) const;
    SignedBigInteger bitwise_and(const SignedBigInteger& other) const;
    SignedBigInteger bitwise_xor(const SignedBigInteger& other) const;
    SignedBigInteger bitwise_not() const;
    SignedBigInteger shift_left(size_t num_bits) const;
    SignedBigInteger multiplied_by(const SignedBigInteger& other) const;
    SignedDivisionResult divided_by(const SignedBigInteger& divisor) const;

    SignedBigInteger plus(const UnsignedBigInteger& other) const;
    SignedBigInteger minus(const UnsignedBigInteger& other) const;
    SignedBigInteger bitwise_or(const UnsignedBigInteger& other) const;
    SignedBigInteger bitwise_and(const UnsignedBigInteger& other) const;
    SignedBigInteger bitwise_xor(const UnsignedBigInteger& other) const;
    SignedBigInteger multiplied_by(const UnsignedBigInteger& other) const;
    SignedDivisionResult divided_by(const UnsignedBigInteger& divisor) const;

    u32 hash() const;

    void set_bit_inplace(size_t bit_index);

    bool operator==(const SignedBigInteger& other) const;
    bool operator!=(const SignedBigInteger& other) const;
    bool operator<(const SignedBigInteger& other) const;
    bool operator<=(const SignedBigInteger& other) const;
    bool operator>(const SignedBigInteger& other) const;
    bool operator>=(const SignedBigInteger& other) const;

    bool operator==(const UnsignedBigInteger& other) const;
    bool operator!=(const UnsignedBigInteger& other) const;
    bool operator<(const UnsignedBigInteger& other) const;
    bool operator>(const UnsignedBigInteger& other) const;

private:
    bool m_sign { false };
    UnsignedBigInteger m_unsigned_data;
};

struct SignedDivisionResult {
    Crypto::SignedBigInteger quotient;
    Crypto::SignedBigInteger remainder;
};

}

inline Crypto::SignedBigInteger
operator""_sbigint(const char* string, size_t length)
{
    return Crypto::SignedBigInteger::from_base(10, { string, length });
}
