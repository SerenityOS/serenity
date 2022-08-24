/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2022, David Tuin <davidot@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SignedBigInteger.h"
#include <AK/StringBuilder.h>
#include <math.h>

namespace Crypto {

SignedBigInteger SignedBigInteger::import_data(u8 const* ptr, size_t length)
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
    return ~(unsigned_value - 1); // equivalent to `-unsigned_value`, but doesn't trigger UBSAN
}

double SignedBigInteger::to_double() const
{
    double unsigned_value = m_unsigned_data.to_double();
    if (!m_sign)
        return unsigned_value;

    VERIFY(!is_zero());
    return -unsigned_value;
}

FLATTEN SignedBigInteger SignedBigInteger::plus(SignedBigInteger const& other) const
{
    // If both are of the same sign, just add the unsigned data and return.
    if (m_sign == other.m_sign)
        return { other.m_unsigned_data.plus(m_unsigned_data), m_sign };

    // One value is signed while the other is not.
    return m_sign ? other.minus(this->m_unsigned_data) : minus(other.m_unsigned_data);
}

FLATTEN SignedBigInteger SignedBigInteger::minus(SignedBigInteger const& other) const
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

FLATTEN SignedBigInteger SignedBigInteger::plus(UnsignedBigInteger const& other) const
{
    if (m_sign) {
        if (other < m_unsigned_data)
            return { m_unsigned_data.minus(other), true };

        return { other.minus(m_unsigned_data), false };
    }

    return { m_unsigned_data.plus(other), false };
}

FLATTEN SignedBigInteger SignedBigInteger::minus(UnsignedBigInteger const& other) const
{
    if (m_sign)
        return { m_unsigned_data.plus(m_unsigned_data), true };

    if (other < m_unsigned_data)
        return { m_unsigned_data.minus(other), false };

    return { other.minus(m_unsigned_data), true };
}

FLATTEN SignedBigInteger SignedBigInteger::bitwise_not() const
{
    // Bitwise operators assume two's complement, while SignedBigInteger uses sign-magnitude.
    // In two's complement, -x := ~x + 1.
    // Hence, ~x == -x -1 == -(x + 1).
    SignedBigInteger result = plus(SignedBigInteger { 1 });
    result.negate();
    return result;
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

FLATTEN SignedBigInteger SignedBigInteger::bitwise_or(SignedBigInteger const& other) const
{
    // See bitwise_and() for derivations.
    if (!is_negative() && !other.is_negative())
        return { unsigned_value().bitwise_or(other.unsigned_value()), false };

    // -A | B == (~A + 1) | B == ~(A - 1) | B. The result is negative, so need to two's complement at the end to move the sign into the m_sign field.
    // That can be simplified to:
    //   -(-A | B) == ~(~(A - 1) | B) + 1 = (A - 1) & ~B + 1
    // This saves one ~.
    if (is_negative() && !other.is_negative()) {
        size_t index = unsigned_value().one_based_index_of_highest_set_bit();
        return { unsigned_value().minus(1).bitwise_and(other.unsigned_value().bitwise_not_fill_to_one_based_index(index)).plus(1), true };
    }

    // -(A | -B) == ~A & (B - 1) + 1
    if (!is_negative() && other.is_negative()) {
        size_t index = other.unsigned_value().one_based_index_of_highest_set_bit();
        return { unsigned_value().bitwise_not_fill_to_one_based_index(index).bitwise_and(other.unsigned_value().minus(1)).plus(1), true };
    }

    return { unsigned_value().minus(1).bitwise_and(other.unsigned_value().minus(1)).plus(1), true };
}

FLATTEN SignedBigInteger SignedBigInteger::bitwise_and(SignedBigInteger const& other) const
{
    if (!is_negative() && !other.is_negative())
        return { unsigned_value().bitwise_and(other.unsigned_value()), false };

    // These two just use that -x == ~x + 1 (see below).

    // -A & B == (~A + 1) & B.
    if (is_negative() && !other.is_negative()) {
        size_t index = other.unsigned_value().one_based_index_of_highest_set_bit();
        return { unsigned_value().bitwise_not_fill_to_one_based_index(index).plus(1).bitwise_and(other.unsigned_value()), false };
    }

    // A & -B == A & (~B + 1).
    if (!is_negative() && other.is_negative()) {
        size_t index = unsigned_value().one_based_index_of_highest_set_bit();
        return { unsigned_value().bitwise_and(other.unsigned_value().bitwise_not_fill_to_one_based_index(index).plus(1)), false };
    }

    // Both numbers are negative.
    // x + ~x == 0xff...ff, up to however many bits x is wide.
    // In two's complement, x + ~x + 1 == 0 since the 1 in the overflowing bit position is masked out.
    // Rearranging terms, ~x = -x - 1 (eq1).
    // Substituting x = y - 1, ~(y - 1) == -(y - 1) - 1 == -y +1 -1 == -y, or ~(y - 1) == -y (eq2).
    // Since both numbers are negative, we want to compute -A & -B.
    // Per (eq2):
    //   -A & -B == ~(A - 1) & ~(B - 1)
    // Inverting both sides:
    //   ~(-A & -B) == ~(~(A - 1) & ~(B - 1)) == ~~(A - 1) | ~~(B - 1) == (A - 1) | (B - 1).
    // Applying (q1) on the LHS:
    //   -(-A & -B) - 1 == (A - 1) | (B - 1)
    // Adding 1 on both sides and then multiplying both sides by -1:
    //   -A & -B == -( (A - 1) | (B - 1) + 1)
    // So we can compute the bitwise and by returning a negative number with magnitude (A - 1) | (B - 1) + 1.
    // This is better than the naive (~A + 1) & (~B + 1) because it needs just one O(n) scan for the or instead of 2 for the ~s.
    return { unsigned_value().minus(1).bitwise_or(other.unsigned_value().minus(1)).plus(1), true };
}

FLATTEN SignedBigInteger SignedBigInteger::bitwise_xor(SignedBigInteger const& other) const
{
    return bitwise_or(other).minus(bitwise_and(other));
}

bool SignedBigInteger::operator==(UnsignedBigInteger const& other) const
{
    if (m_sign)
        return false;
    return m_unsigned_data == other;
}

bool SignedBigInteger::operator!=(UnsignedBigInteger const& other) const
{
    if (m_sign)
        return true;
    return m_unsigned_data != other;
}

bool SignedBigInteger::operator<(UnsignedBigInteger const& other) const
{
    if (m_sign)
        return true;
    return m_unsigned_data < other;
}

bool SignedBigInteger::operator>(UnsignedBigInteger const& other) const
{
    return *this != other && !(*this < other);
}

FLATTEN SignedBigInteger SignedBigInteger::shift_left(size_t num_bits) const
{
    return SignedBigInteger { m_unsigned_data.shift_left(num_bits), m_sign };
}

FLATTEN SignedBigInteger SignedBigInteger::multiplied_by(SignedBigInteger const& other) const
{
    bool result_sign = m_sign ^ other.m_sign;
    return { m_unsigned_data.multiplied_by(other.m_unsigned_data), result_sign };
}

FLATTEN SignedDivisionResult SignedBigInteger::divided_by(SignedBigInteger const& divisor) const
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

bool SignedBigInteger::operator==(SignedBigInteger const& other) const
{
    if (is_invalid() != other.is_invalid())
        return false;

    if (m_unsigned_data == 0 && other.m_unsigned_data == 0)
        return true;

    return m_sign == other.m_sign && m_unsigned_data == other.m_unsigned_data;
}

bool SignedBigInteger::operator!=(SignedBigInteger const& other) const
{
    return !(*this == other);
}

bool SignedBigInteger::operator<(SignedBigInteger const& other) const
{
    if (m_sign ^ other.m_sign)
        return m_sign;

    if (m_sign)
        return other.m_unsigned_data < m_unsigned_data;

    return m_unsigned_data < other.m_unsigned_data;
}

bool SignedBigInteger::operator<=(SignedBigInteger const& other) const
{
    return *this < other || *this == other;
}

bool SignedBigInteger::operator>(SignedBigInteger const& other) const
{
    return *this != other && !(*this < other);
}

bool SignedBigInteger::operator>=(SignedBigInteger const& other) const
{
    return !(*this < other);
}

SignedBigInteger::CompareResult SignedBigInteger::compare_to_double(double value) const
{
    VERIFY(!isnan(value));

    if (isinf(value)) {
        bool is_positive_infinity = __builtin_isinf_sign(value) > 0;
        return is_positive_infinity ? CompareResult::DoubleGreaterThanBigInt : CompareResult::DoubleLessThanBigInt;
    }

    bool bigint_is_negative = m_sign;

    bool value_is_negative = value < 0;

    if (value_is_negative != bigint_is_negative)
        return bigint_is_negative ? CompareResult::DoubleGreaterThanBigInt : CompareResult::DoubleLessThanBigInt;

    // Value is zero, and from above the signs must be the same.
    if (value == 0.0) {
        VERIFY(!value_is_negative && !bigint_is_negative);
        // Either we are also zero or value is certainly less than us.
        return is_zero() ? CompareResult::DoubleEqualsBigInt : CompareResult::DoubleLessThanBigInt;
    }

    // If value is not zero but we are, then since the signs are the same value must be greater.
    if (is_zero())
        return CompareResult::DoubleGreaterThanBigInt;

    constexpr u64 mantissa_size = 52;
    constexpr u64 exponent_size = 11;
    constexpr auto exponent_bias = (1 << (exponent_size - 1)) - 1;
    union FloatExtractor {
        struct {
            unsigned long long mantissa : mantissa_size;
            unsigned exponent : exponent_size;
            unsigned sign : 1;
        };
        double d;
    } extractor;

    extractor.d = value;
    VERIFY(extractor.exponent != (1 << exponent_size) - 1);
    // Exponent cannot be filled as than we must be NaN or infinity.

    i32 real_exponent = extractor.exponent - exponent_bias;
    if (real_exponent < 0) {
        // |value| is less than 1, and we cannot be zero so if we are negative
        // value must be greater and vice versa.
        return bigint_is_negative ? CompareResult::DoubleGreaterThanBigInt : CompareResult::DoubleLessThanBigInt;
    }

    u64 bigint_bits_needed = m_unsigned_data.one_based_index_of_highest_set_bit();
    VERIFY(bigint_bits_needed > 0);

    // Double value is `-1^sign (1.mantissa) * 2^(exponent - bias)` so we need
    // `exponent - bias + 1` bit to represent doubles value,
    // for example `exponent - bias` = 3, sign = 0 and mantissa = 0 we get
    // `-1^0 * 2^3 * 1 = 8` which needs 4 bits to store 8 (0b1000).
    u32 double_bits_needed = real_exponent + 1;

    if (bigint_bits_needed > double_bits_needed) {
        // If we need more bits to represent us, we must be of greater magnitude
        // this means that if we are negative we are below value and if positive above value.
        return bigint_is_negative ? CompareResult::DoubleGreaterThanBigInt : CompareResult::DoubleLessThanBigInt;
    }

    if (bigint_bits_needed < double_bits_needed)
        return bigint_is_negative ? CompareResult::DoubleLessThanBigInt : CompareResult::DoubleGreaterThanBigInt;

    u64 mantissa_bits = extractor.mantissa;

    // We add the bit which represents the 1. of the double value calculation
    constexpr u64 mantissa_extended_bit = 1ull << mantissa_size;

    mantissa_bits |= mantissa_extended_bit;

    // Now we shift value to the left virtually, with `exponent - bias` steps
    // we then pretend both it and the big int are extended with virtual zeros.
    using Word = UnsignedBigInteger::Word;
    auto next_bigint_word = (UnsignedBigInteger::BITS_IN_WORD - 1 + bigint_bits_needed) / UnsignedBigInteger::BITS_IN_WORD;

    VERIFY(next_bigint_word + 1 == trimmed_length());

    auto msb_in_top_word_index = (bigint_bits_needed - 1) % UnsignedBigInteger::BITS_IN_WORD;
    VERIFY(msb_in_top_word_index == (UnsignedBigInteger::BITS_IN_WORD - count_leading_zeroes(words()[next_bigint_word - 1]) - 1));

    // We will keep the bits which are still valid in the mantissa at the top of mantissa bits.
    mantissa_bits <<= 64 - (mantissa_size + 1);

    auto bits_left_in_mantissa = mantissa_size + 1;

    auto get_next_value_bits = [&](size_t num_bits) -> Word {
        VERIFY(num_bits < 63);
        VERIFY(bits_left_in_mantissa > 0);
        if (num_bits > bits_left_in_mantissa)
            num_bits = bits_left_in_mantissa;

        bits_left_in_mantissa -= num_bits;

        u64 extracted_bits = mantissa_bits & (((1ull << num_bits) - 1) << (64 - num_bits));
        // Now shift the bits down to put the most significant bit on the num_bits position
        // this means the rest will be "virtual" zeros.
        extracted_bits >>= 32;

        // Now shift away the used bits and fit the result into a Word.
        mantissa_bits <<= num_bits;

        VERIFY(extracted_bits <= NumericLimits<Word>::max());
        return static_cast<Word>(extracted_bits);
    };

    auto bits_in_next_bigint_word = msb_in_top_word_index + 1;

    while (next_bigint_word > 0 && bits_left_in_mantissa > 0) {
        Word bigint_word = words()[next_bigint_word - 1];
        Word double_word = get_next_value_bits(bits_in_next_bigint_word);

        // For the first bit we have to align it with the top bit of bigint
        // and for all the other cases bits_in_next_bigint_word is 32 so this does nothing.
        double_word >>= 32 - bits_in_next_bigint_word;

        if (bigint_word < double_word)
            return value_is_negative ? CompareResult::DoubleLessThanBigInt : CompareResult::DoubleGreaterThanBigInt;

        if (bigint_word > double_word)
            return value_is_negative ? CompareResult::DoubleGreaterThanBigInt : CompareResult::DoubleLessThanBigInt;

        --next_bigint_word;
        bits_in_next_bigint_word = UnsignedBigInteger::BITS_IN_WORD;
    }

    // If there are still bits left in bigint than any non zero bit means it has greater magnitude.
    if (next_bigint_word > 0) {
        VERIFY(bits_left_in_mantissa == 0);
        while (next_bigint_word > 0) {
            if (words()[next_bigint_word - 1] != 0)
                return value_is_negative ? CompareResult::DoubleGreaterThanBigInt : CompareResult::DoubleLessThanBigInt;
            --next_bigint_word;
        }
    } else if (bits_left_in_mantissa > 0) {
        VERIFY(next_bigint_word == 0);
        // Similarly if there are still any bits set in the mantissa it has greater magnitude.
        if (mantissa_bits != 0)
            return value_is_negative ? CompareResult::DoubleLessThanBigInt : CompareResult::DoubleGreaterThanBigInt;
    }

    // Otherwise if both don't have bits left or the rest of the bits are zero they are equal.
    return CompareResult::DoubleEqualsBigInt;
}

}

ErrorOr<void> AK::Formatter<Crypto::SignedBigInteger>::format(FormatBuilder& fmtbuilder, Crypto::SignedBigInteger const& value)
{
    if (value.is_negative())
        TRY(fmtbuilder.put_string("-"sv));
    return Formatter<Crypto::UnsignedBigInteger>::format(fmtbuilder, value.unsigned_value());
}
