/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SignedBigInteger.h"
#include <AK/StringBuilder.h>

namespace Crypto {

SignedBigInteger SignedBigInteger::import_data(const u8* ptr, size_t length)
{
    bool sign = *ptr;
    auto unsigned_data = UnsignedBigInteger::import_data(ptr + 1, length - 1);
    return { move(unsigned_data), sign };
}

size_t SignedBigInteger::export_data(Bytes data, bool remove_leading_zeros) const
{
    // FIXME: Support this:
    //        m <0XX> -> m <XX> (if remove_leading_zeros)
    VERIFY(!remove_leading_zeros);

    data[0] = m_sign;
    auto bytes_view = data.slice(1, data.size() - 1);
    return m_unsigned_data.export_data(bytes_view, remove_leading_zeros) + 1;
}

SignedBigInteger SignedBigInteger::from_base(u16 N, StringView str)
{
    auto sign = false;
    if (str.length() > 1) {
        auto maybe_sign = str[0];
        if (maybe_sign == '-') {
            str = str.substring_view(1);
            sign = true;
        }
        if (maybe_sign == '+')
            str = str.substring_view(1);
    }
    auto unsigned_data = UnsignedBigInteger::from_base(N, str);
    return { move(unsigned_data), sign };
}

String SignedBigInteger::to_base(u16 N) const
{
    StringBuilder builder;

    if (m_sign)
        builder.append('-');

    builder.append(m_unsigned_data.to_base(N));

    return builder.to_string();
}

u64 SignedBigInteger::to_u64() const
{
    u64 unsigned_value = m_unsigned_data.to_u64();
    if (!m_sign)
        return unsigned_value;
    return ~(unsigned_value - 1); // equivalent to `-unsigned_value`, but doesnt trigger UBSAN
}

double SignedBigInteger::to_double() const
{
    double unsigned_value = m_unsigned_data.to_double();
    if (!m_sign)
        return unsigned_value;
    return -unsigned_value;
}

FLATTEN SignedBigInteger SignedBigInteger::plus(const SignedBigInteger& other) const
{
    // If both are of the same sign, just add the unsigned data and return.
    if (m_sign == other.m_sign)
        return { other.m_unsigned_data.plus(m_unsigned_data), m_sign };

    // One value is signed while the other is not.
    return m_sign ? other.minus(this->m_unsigned_data) : minus(other.m_unsigned_data);
}

FLATTEN SignedBigInteger SignedBigInteger::minus(const SignedBigInteger& other) const
{
    // If the signs are different, convert the op to an addition.
    if (m_sign != other.m_sign) {
        // -x - y = - (x + y)
        // x - -y = (x + y)
        SignedBigInteger result { other.m_unsigned_data.plus(this->m_unsigned_data) };
        if (m_sign)
            result.negate();
        return result;
    }

    if (!m_sign) {
        // Both operands are positive.
        // x - y = - (y - x)
        if (m_unsigned_data < other.m_unsigned_data) {
            // The result will be negative.
            return { other.m_unsigned_data.minus(m_unsigned_data), true };
        }

        // The result will be either zero, or positive.
        return SignedBigInteger { m_unsigned_data.minus(other.m_unsigned_data) };
    }

    // Both operands are negative.
    // -x - -y = y - x
    if (m_unsigned_data < other.m_unsigned_data) {
        // The result will be positive.
        return SignedBigInteger { other.m_unsigned_data.minus(m_unsigned_data) };
    }
    // y - x = - (x - y)
    if (m_unsigned_data > other.m_unsigned_data) {
        // The result will be negative.
        return SignedBigInteger { m_unsigned_data.minus(other.m_unsigned_data), true };
    }
    // Both operands have the same magnitude, the result is positive zero.
    return SignedBigInteger { 0 };
}

FLATTEN SignedBigInteger SignedBigInteger::plus(const UnsignedBigInteger& other) const
{
    if (m_sign) {
        if (other < m_unsigned_data)
            return { m_unsigned_data.minus(other), true };

        return { other.minus(m_unsigned_data), false };
    }

    return { m_unsigned_data.plus(other), false };
}

FLATTEN SignedBigInteger SignedBigInteger::minus(const UnsignedBigInteger& other) const
{
    if (m_sign)
        return { m_unsigned_data.plus(m_unsigned_data), true };

    if (other < m_unsigned_data)
        return { m_unsigned_data.minus(other), false };

    return { other.minus(m_unsigned_data), true };
}

FLATTEN SignedBigInteger SignedBigInteger::bitwise_or(const UnsignedBigInteger& other) const
{
    return { unsigned_value().bitwise_or(other), m_sign };
}

FLATTEN SignedBigInteger SignedBigInteger::bitwise_and(const UnsignedBigInteger& other) const
{
    return { unsigned_value().bitwise_and(other), false };
}

FLATTEN SignedBigInteger SignedBigInteger::bitwise_xor(const UnsignedBigInteger& other) const
{
    return { unsigned_value().bitwise_xor(other), m_sign };
}

FLATTEN SignedBigInteger SignedBigInteger::bitwise_not() const
{
    return { unsigned_value().bitwise_not(), !m_sign };
}

FLATTEN SignedBigInteger SignedBigInteger::multiplied_by(UnsignedBigInteger const& other) const
{
    return { unsigned_value().multiplied_by(other), m_sign };
}

FLATTEN SignedDivisionResult SignedBigInteger::divided_by(UnsignedBigInteger const& divisor) const
{
    auto division_result = unsigned_value().divided_by(divisor);
    return {
        { move(division_result.quotient), m_sign },
        { move(division_result.remainder), m_sign },
    };
}

FLATTEN SignedBigInteger SignedBigInteger::bitwise_or(const SignedBigInteger& other) const
{
    auto result = bitwise_or(other.unsigned_value());

    // The sign bit will have to be OR'd manually.
    result.m_sign = is_negative() || other.is_negative();

    return result;
}

FLATTEN SignedBigInteger SignedBigInteger::bitwise_and(const SignedBigInteger& other) const
{
    auto result = bitwise_and(other.unsigned_value());

    // The sign bit will have to be AND'd manually.
    result.m_sign = is_negative() && other.is_negative();

    return result;
}

FLATTEN SignedBigInteger SignedBigInteger::bitwise_xor(const SignedBigInteger& other) const
{
    auto result = bitwise_xor(other.unsigned_value());

    // The sign bit will have to be XOR'd manually.
    result.m_sign = is_negative() ^ other.is_negative();

    return result;
}

bool SignedBigInteger::operator==(const UnsignedBigInteger& other) const
{
    if (m_sign)
        return false;
    return m_unsigned_data == other;
}

bool SignedBigInteger::operator!=(const UnsignedBigInteger& other) const
{
    if (m_sign)
        return true;
    return m_unsigned_data != other;
}

bool SignedBigInteger::operator<(const UnsignedBigInteger& other) const
{
    if (m_sign)
        return true;
    return m_unsigned_data < other;
}

bool SignedBigInteger::operator>(const UnsignedBigInteger& other) const
{
    return *this != other && !(*this < other);
}

FLATTEN SignedBigInteger SignedBigInteger::shift_left(size_t num_bits) const
{
    return SignedBigInteger { m_unsigned_data.shift_left(num_bits), m_sign };
}

FLATTEN SignedBigInteger SignedBigInteger::multiplied_by(const SignedBigInteger& other) const
{
    bool result_sign = m_sign ^ other.m_sign;
    return { m_unsigned_data.multiplied_by(other.m_unsigned_data), result_sign };
}

FLATTEN SignedDivisionResult SignedBigInteger::divided_by(const SignedBigInteger& divisor) const
{
    // Aa / Bb -> (A^B)q, Ar
    bool result_sign = m_sign ^ divisor.m_sign;
    auto unsigned_division_result = m_unsigned_data.divided_by(divisor.m_unsigned_data);
    return {
        { move(unsigned_division_result.quotient), result_sign },
        { move(unsigned_division_result.remainder), m_sign }
    };
}

u32 SignedBigInteger::hash() const
{
    return m_unsigned_data.hash() * (1 - (2 * m_sign));
}

void SignedBigInteger::set_bit_inplace(size_t bit_index)
{
    m_unsigned_data.set_bit_inplace(bit_index);
}

bool SignedBigInteger::operator==(const SignedBigInteger& other) const
{
    if (is_invalid() != other.is_invalid())
        return false;

    if (m_unsigned_data == 0 && other.m_unsigned_data == 0)
        return true;

    return m_sign == other.m_sign && m_unsigned_data == other.m_unsigned_data;
}

bool SignedBigInteger::operator!=(const SignedBigInteger& other) const
{
    return !(*this == other);
}

bool SignedBigInteger::operator<(const SignedBigInteger& other) const
{
    if (m_sign ^ other.m_sign)
        return m_sign;

    if (m_sign)
        return other.m_unsigned_data < m_unsigned_data;

    return m_unsigned_data < other.m_unsigned_data;
}

bool SignedBigInteger::operator<=(const SignedBigInteger& other) const
{
    return *this < other || *this == other;
}

bool SignedBigInteger::operator>(const SignedBigInteger& other) const
{
    return *this != other && !(*this < other);
}

bool SignedBigInteger::operator>=(const SignedBigInteger& other) const
{
    return !(*this < other);
}

}
