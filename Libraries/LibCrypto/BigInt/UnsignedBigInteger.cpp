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

#include "UnsignedBigInteger.h"
#include <AK/StringBuilder.h>

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
            u32 word = m_words[word_count - 1];
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

UnsignedBigInteger UnsignedBigInteger::from_base10(const String& str)
{
    UnsignedBigInteger result;
    UnsignedBigInteger ten { 10 };

    for (auto& c : str) {
        result = result.multiplied_by(ten).plus(c - '0');
    }
    return result;
}

String UnsignedBigInteger::to_base10() const
{
    if (*this == UnsignedBigInteger { 0 })
        return "0";

    StringBuilder builder;
    UnsignedBigInteger temp(*this);
    UnsignedBigInteger quotient;
    UnsignedBigInteger remainder;

    while (temp != UnsignedBigInteger { 0 }) {
        divide_u16_without_allocation(temp, 10, quotient, remainder);
        ASSERT(remainder.words()[0] < 10);
        builder.append(static_cast<char>(remainder.words()[0] + '0'));
        temp.set_to(quotient);
    }

    auto reversed_string = builder.to_string();
    builder.clear();
    for (int i = reversed_string.length() - 1; i >= 0; --i) {
        builder.append(reversed_string[i]);
    }

    return builder.to_string();
}

void UnsignedBigInteger::set_to_0()
{
    m_words.clear_with_capacity();
    m_is_invalid = false;
    m_cached_trimmed_length = {};
}

void UnsignedBigInteger::set_to(u32 other)
{
    m_is_invalid = false;
    m_words.resize_and_keep_capacity(1);
    m_words[0] = other;
    m_cached_trimmed_length = {};
}

void UnsignedBigInteger::set_to(const UnsignedBigInteger& other)
{
    m_is_invalid = other.m_is_invalid;
    m_words.resize_and_keep_capacity(other.m_words.size());
    __builtin_memcpy(m_words.data(), other.m_words.data(), other.m_words.size() * sizeof(u32));
    m_cached_trimmed_length = {};
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

FLATTEN UnsignedBigInteger UnsignedBigInteger::plus(const UnsignedBigInteger& other) const
{
    UnsignedBigInteger result;

    add_without_allocation(*this, other, result);

    return result;
}

FLATTEN UnsignedBigInteger UnsignedBigInteger::minus(const UnsignedBigInteger& other) const
{
    UnsignedBigInteger result;

    subtract_without_allocation(*this, other, result);

    return result;
}

FLATTEN UnsignedBigInteger UnsignedBigInteger::bitwise_or(const UnsignedBigInteger& other) const
{
    UnsignedBigInteger result;

    bitwise_or_without_allocation(*this, other, result);

    return result;
}

FLATTEN UnsignedBigInteger UnsignedBigInteger::bitwise_and(const UnsignedBigInteger& other) const
{
    UnsignedBigInteger result;

    bitwise_and_without_allocation(*this, other, result);

    return result;
}

FLATTEN UnsignedBigInteger UnsignedBigInteger::bitwise_xor(const UnsignedBigInteger& other) const
{
    UnsignedBigInteger result;

    bitwise_xor_without_allocation(*this, other, result);

    return result;
}

FLATTEN UnsignedBigInteger UnsignedBigInteger::bitwise_not() const
{
    UnsignedBigInteger result;

    bitwise_not_without_allocation(*this, result);

    return result;
}

FLATTEN UnsignedBigInteger UnsignedBigInteger::shift_left(size_t num_bits) const
{
    UnsignedBigInteger output;
    UnsignedBigInteger temp_result;
    UnsignedBigInteger temp_plus;

    shift_left_without_allocation(*this, num_bits, temp_result, temp_plus, output);

    return output;
}

FLATTEN UnsignedBigInteger UnsignedBigInteger::multiplied_by(const UnsignedBigInteger& other) const
{
    UnsignedBigInteger result;
    UnsignedBigInteger temp_shift_result;
    UnsignedBigInteger temp_shift_plus;
    UnsignedBigInteger temp_shift;
    UnsignedBigInteger temp_plus;

    multiply_without_allocation(*this, other, temp_shift_result, temp_shift_plus, temp_shift, temp_plus, result);

    return result;
}

FLATTEN UnsignedDivisionResult UnsignedBigInteger::divided_by(const UnsignedBigInteger& divisor) const
{
    UnsignedBigInteger quotient;
    UnsignedBigInteger remainder;

    // If we actually have a u16-compatible divisor, short-circuit to the
    // less computationally-intensive "divide_u16_without_allocation" method.
    if (divisor.trimmed_length() == 1 && divisor.m_words[0] < (1 << 16)) {
        divide_u16_without_allocation(*this, divisor.m_words[0], quotient, remainder);
        return UnsignedDivisionResult { quotient, remainder };
    }

    UnsignedBigInteger temp_shift_result;
    UnsignedBigInteger temp_shift_plus;
    UnsignedBigInteger temp_shift;
    UnsignedBigInteger temp_minus;

    divide_without_allocation(*this, divisor, temp_shift_result, temp_shift_plus, temp_shift, temp_minus, quotient, remainder);

    return UnsignedDivisionResult { quotient, remainder };
}

void UnsignedBigInteger::set_bit_inplace(size_t bit_index)
{
    const size_t word_index = bit_index / UnsignedBigInteger::BITS_IN_WORD;
    const size_t inner_word_index = bit_index % UnsignedBigInteger::BITS_IN_WORD;

    m_words.ensure_capacity(word_index);

    for (size_t i = length(); i <= word_index; ++i) {
        m_words.unchecked_append(0);
    }
    m_words[word_index] |= (1 << inner_word_index);

    m_cached_trimmed_length = {};
}

bool UnsignedBigInteger::operator==(const UnsignedBigInteger& other) const
{
    if (is_invalid() != other.is_invalid())
        return false;

    auto length = trimmed_length();

    if (length != other.trimmed_length())
        return false;

    return !__builtin_memcmp(m_words.data(), other.words().data(), length);
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

/**
 * Complexity: O(N) where N is the number of words in the larger number
 */
void UnsignedBigInteger::add_without_allocation(
    const UnsignedBigInteger& left,
    const UnsignedBigInteger& right,
    UnsignedBigInteger& output)
{
    const UnsignedBigInteger* const longer = (left.length() > right.length()) ? &left : &right;
    const UnsignedBigInteger* const shorter = (longer == &right) ? &left : &right;

    u8 carry = 0;

    output.set_to_0();
    output.m_words.resize_and_keep_capacity(longer->length());

    for (size_t i = 0; i < shorter->length(); ++i) {
        u32 word_addition_result = shorter->m_words[i] + longer->m_words[i];
        u8 carry_out = 0;
        // if there was a carry, the result will be smaller than any of the operands
        if (word_addition_result + carry < shorter->m_words[i]) {
            carry_out = 1;
        }
        if (carry) {
            word_addition_result++;
        }
        carry = carry_out;
        output.m_words[i] = word_addition_result;
    }

    for (size_t i = shorter->length(); i < longer->length(); ++i) {
        u32 word_addition_result = longer->m_words[i] + carry;

        carry = 0;
        if (word_addition_result < longer->m_words[i]) {
            carry = 1;
        }
        output.m_words[i] = word_addition_result;
    }
    if (carry) {
        output.m_words.append(carry);
    }
}

/**
 * Complexity: O(N) where N is the number of words in the larger number
 */
void UnsignedBigInteger::subtract_without_allocation(
    const UnsignedBigInteger& left,
    const UnsignedBigInteger& right,
    UnsignedBigInteger& output)
{
    if (left < right) {
        output.invalidate();
        return;
    }

    u8 borrow = 0;
    auto own_length = left.length();
    auto other_length = right.length();

    output.set_to_0();
    output.m_words.resize_and_keep_capacity(own_length);

    for (size_t i = 0; i < own_length; ++i) {
        u32 other_word = (i < other_length) ? right.m_words[i] : 0;
        i64 temp = static_cast<i64>(left.m_words[i]) - static_cast<i64>(other_word) - static_cast<i64>(borrow);
        // If temp < 0, we had an underflow
        borrow = (temp >= 0) ? 0 : 1;
        if (temp < 0) {
            temp += (UINT32_MAX + 1);
        }
        output.m_words[i] = temp;
    }

    // This assertion should not fail, because we verified that *this>=other at the beginning of the function
    ASSERT(borrow == 0);
}

/**
 * Complexity: O(N) where N is the number of words in the shorter value
 * Method:
 * Apply <op> word-wise until words in the shorter value are used up
 * then copy the rest of the words verbatim from the longer value.
 */
FLATTEN void UnsignedBigInteger::bitwise_or_without_allocation(
    const UnsignedBigInteger& left,
    const UnsignedBigInteger& right,
    UnsignedBigInteger& output)
{
    // If either of the BigInts are invalid, the output is just the other one.
    if (left.is_invalid()) {
        output.set_to(right);
        return;
    }
    if (right.is_invalid()) {
        output.set_to(left);
        return;
    }

    const UnsignedBigInteger *shorter, *longer;
    if (left.length() < right.length()) {
        shorter = &left;
        longer = &right;
    } else {
        shorter = &right;
        longer = &left;
    }

    output.m_words.resize_and_keep_capacity(longer->length());

    size_t longer_offset = longer->length() - shorter->length();
    for (size_t i = 0; i < shorter->length(); ++i)
        output.m_words[i] = longer->words()[i] | shorter->words()[i];

    __builtin_memcpy(output.m_words.data() + shorter->length(), longer->words().data() + shorter->length(), sizeof(u32) * longer_offset);
}

/**
 * Complexity: O(N) where N is the number of words in the shorter value
 * Method:
 * Apply 'and' word-wise until words in the shorter value are used up
 * and zero the rest.
 */
FLATTEN void UnsignedBigInteger::bitwise_and_without_allocation(
    const UnsignedBigInteger& left,
    const UnsignedBigInteger& right,
    UnsignedBigInteger& output)
{
    // If either of the BigInts are invalid, the output is just the other one.
    if (left.is_invalid()) {
        output.set_to(right);
        return;
    }
    if (right.is_invalid()) {
        output.set_to(left);
        return;
    }

    const UnsignedBigInteger *shorter, *longer;
    if (left.length() < right.length()) {
        shorter = &left;
        longer = &right;
    } else {
        shorter = &right;
        longer = &left;
    }

    output.m_words.resize_and_keep_capacity(longer->length());

    size_t longer_offset = longer->length() - shorter->length();
    for (size_t i = 0; i < shorter->length(); ++i)
        output.m_words[i] = longer->words()[i] & shorter->words()[i];

    __builtin_memset(output.m_words.data() + shorter->length(), 0, sizeof(u32) * longer_offset);
}

/**
 * Complexity: O(N) where N is the number of words in the shorter value
 * Method:
 * Apply 'xor' word-wise until words in the shorter value are used up
 * and copy the rest.
 */
FLATTEN void UnsignedBigInteger::bitwise_xor_without_allocation(
    const UnsignedBigInteger& left,
    const UnsignedBigInteger& right,
    UnsignedBigInteger& output)
{
    // If either of the BigInts are invalid, the output is just the other one.
    if (left.is_invalid()) {
        output.set_to(right);
        return;
    }
    if (right.is_invalid()) {
        output.set_to(left);
        return;
    }

    const UnsignedBigInteger *shorter, *longer;
    if (left.length() < right.length()) {
        shorter = &left;
        longer = &right;
    } else {
        shorter = &right;
        longer = &left;
    }

    output.m_words.resize_and_keep_capacity(longer->length());

    size_t longer_offset = longer->length() - shorter->length();
    for (size_t i = 0; i < shorter->length(); ++i)
        output.m_words[i] = longer->words()[i] ^ shorter->words()[i];

    __builtin_memcpy(output.m_words.data() + shorter->length(), longer->words().data() + shorter->length(), sizeof(u32) * longer_offset);
}

/**
 * Complexity: O(N) where N is the number of words
 */
FLATTEN void UnsignedBigInteger::bitwise_not_without_allocation(
    const UnsignedBigInteger& right,
    UnsignedBigInteger& output)
{
    // If the value is invalid, the output value is invalid as well.
    if (right.is_invalid()) {
        output.invalidate();
        return;
    }
    if (right.length() == 0) {
        output.set_to_0();
        return;
    }

    output.m_words.resize_and_keep_capacity(right.length());

    if (right.length() > 1) {
        for (size_t i = 0; i < right.length() - 1; ++i)
            output.m_words[i] = ~right.words()[i];
    }

    auto last_word_index = right.length() - 1;
    auto last_word = right.words()[last_word_index];

    output.m_words[last_word_index] = ((u32)0xffffffffffffffff >> __builtin_clz(last_word)) & ~last_word;
}

/**
 * Complexity : O(N + num_bits % 8) where N is the number of words in the number
 * Shift method :
 * Start by shifting by whole words in num_bits (by putting missing words at the start),
 * then shift the number's words two by two by the remaining amount of bits.
 */
FLATTEN void UnsignedBigInteger::shift_left_without_allocation(
    const UnsignedBigInteger& number,
    size_t num_bits,
    UnsignedBigInteger& temp_result,
    UnsignedBigInteger& temp_plus,
    UnsignedBigInteger& output)
{
    // We can only do shift operations on individual words
    // where the shift amount is <= size of word (32).
    // But we do know how to shift by a multiple of word size (e.g 64=32*2)
    // So we first shift the result by how many whole words fit in 'num_bits'
    shift_left_by_n_words(number, num_bits / UnsignedBigInteger::BITS_IN_WORD, temp_result);

    output.set_to(temp_result);

    // And now we shift by the leftover amount of bits
    num_bits %= UnsignedBigInteger::BITS_IN_WORD;

    if (num_bits == 0) {
        return;
    }

    for (size_t i = 0; i < temp_result.length(); ++i) {
        u32 current_word_of_temp_result = shift_left_get_one_word(temp_result, num_bits, i);
        output.m_words[i] = current_word_of_temp_result;
    }

    // Shifting the last word can produce a carry
    u32 carry_word = shift_left_get_one_word(temp_result, num_bits, temp_result.length());
    if (carry_word != 0) {

        // output += (carry_word << temp_result.length())
        // FIXME : Using temp_plus this way to transform carry_word into a bigint is not
        // efficient nor pretty. Maybe we should have an "add_with_shift" method ?
        temp_plus.set_to_0();
        temp_plus.m_words.append(carry_word);
        shift_left_by_n_words(temp_plus, temp_result.length(), temp_result);
        add_without_allocation(output, temp_result, temp_plus);
        output.set_to(temp_plus);
    }
}

/**
 * Complexity: O(N^2) where N is the number of words in the larger number
 * Multiplication method:
 * An integer is equal to the sum of the powers of two
 * according to the indexes of its 'on' bits.
 * So to multiple x*y, we go over each '1' bit in x (say the i'th bit), 
 * and add y<<i to the result.
 */
FLATTEN void UnsignedBigInteger::multiply_without_allocation(
    const UnsignedBigInteger& left,
    const UnsignedBigInteger& right,
    UnsignedBigInteger& temp_shift_result,
    UnsignedBigInteger& temp_shift_plus,
    UnsignedBigInteger& temp_shift,
    UnsignedBigInteger& temp_plus,
    UnsignedBigInteger& output)
{
    output.set_to_0();

    // iterate all bits
    for (size_t word_index = 0; word_index < left.length(); ++word_index) {
        for (size_t bit_index = 0; bit_index < UnsignedBigInteger::BITS_IN_WORD; ++bit_index) {
            // If the bit is off - skip over it
            if (!(left.m_words[word_index] & (1 << bit_index)))
                continue;

            const size_t shift_amount = word_index * UnsignedBigInteger::BITS_IN_WORD + bit_index;

            // output += (right << shift_amount);
            shift_left_without_allocation(right, shift_amount, temp_shift_result, temp_shift_plus, temp_shift);
            add_without_allocation(output, temp_shift, temp_plus);
            output.set_to(temp_plus);
        }
    }
}

/**
 * Complexity: O(N^2) where N is the number of words in the larger number
 * Division method:
 * We loop over the bits of the divisor, attempting to subtract divisor<<i from the dividend.
 * If the result is non-negative, it means that divisor*2^i "fits" in the dividend,
 * so we set the ith bit in the quotient and reduce divisor<<i from the dividend.
 * When we're done, what's left from the dividend is the remainder.
 */
FLATTEN void UnsignedBigInteger::divide_without_allocation(
    const UnsignedBigInteger& numerator,
    const UnsignedBigInteger& denominator,
    UnsignedBigInteger& temp_shift_result,
    UnsignedBigInteger& temp_shift_plus,
    UnsignedBigInteger& temp_shift,
    UnsignedBigInteger& temp_minus,
    UnsignedBigInteger& quotient,
    UnsignedBigInteger& remainder)
{
    quotient.set_to_0();
    remainder.set_to(numerator);

    // iterate all bits
    for (int word_index = numerator.trimmed_length() - 1; word_index >= 0; --word_index) {
        for (int bit_index = UnsignedBigInteger::BITS_IN_WORD - 1; bit_index >= 0; --bit_index) {
            const size_t shift_amount = word_index * UnsignedBigInteger::BITS_IN_WORD + bit_index;
            shift_left_without_allocation(denominator, shift_amount, temp_shift_result, temp_shift_plus, temp_shift);

            subtract_without_allocation(remainder, temp_shift, temp_minus);
            if (!temp_minus.is_invalid()) {
                remainder.set_to(temp_minus);
                quotient.set_bit_inplace(shift_amount);
            }
        }
    }
}

/**
 * Complexity : O(N) where N is the number of digits in the numerator
 * Division method :
 * Starting from the most significant one, for each half-word of the numerator, combine it
 * with the existing remainder if any, divide the combined number as a u32 operation and
 * update the quotient / remainder as needed.
 */
FLATTEN void UnsignedBigInteger::divide_u16_without_allocation(
    const UnsignedBigInteger& numerator,
    u32 denominator,
    UnsignedBigInteger& quotient,
    UnsignedBigInteger& remainder)
{
    ASSERT(denominator < (1 << 16));
    u32 remainder_word = 0;
    auto numerator_length = numerator.trimmed_length();
    quotient.set_to_0();
    quotient.m_words.resize(numerator_length);
    for (int word_index = numerator_length - 1; word_index >= 0; --word_index) {
        auto word_high = numerator.m_words[word_index] >> 16;
        auto word_low = numerator.m_words[word_index] & ((1 << 16) - 1);

        auto number_to_divide_high = (remainder_word << 16) | word_high;
        auto quotient_high = number_to_divide_high / denominator;
        remainder_word = number_to_divide_high % denominator;

        auto number_to_divide_low = remainder_word << 16 | word_low;
        auto quotient_low = number_to_divide_low / denominator;
        remainder_word = number_to_divide_low % denominator;

        quotient.m_words[word_index] = (quotient_high << 16) | quotient_low;
    }
    remainder.set_to(remainder_word);
}

ALWAYS_INLINE void UnsignedBigInteger::shift_left_by_n_words(
    const UnsignedBigInteger& number,
    const size_t number_of_words,
    UnsignedBigInteger& output)
{
    // shifting left by N words means just inserting N zeroes to the beginning of the words vector
    output.set_to_0();
    output.m_words.resize_and_keep_capacity(number_of_words + number.length());

    __builtin_memset(output.m_words.data(), 0, number_of_words * sizeof(unsigned));
    __builtin_memcpy(&output.m_words.data()[number_of_words], number.m_words.data(), number.m_words.size() * sizeof(unsigned));
}

/**
 * Returns the word at a requested index in the result of a shift operation
 */
ALWAYS_INLINE u32 UnsignedBigInteger::shift_left_get_one_word(
    const UnsignedBigInteger& number,
    const size_t num_bits,
    const size_t result_word_index)
{
    // "<= length()" (rather than length() - 1) is intentional,
    // The result inedx of length() is used when calculating the carry word
    ASSERT(result_word_index <= number.length());
    ASSERT(num_bits <= UnsignedBigInteger::BITS_IN_WORD);
    u32 result = 0;

    // we need to check for "num_bits != 0" since shifting right by 32 is apparently undefined behaviour!
    if (result_word_index > 0 && num_bits != 0) {
        result += number.m_words[result_word_index - 1] >> (UnsignedBigInteger::BITS_IN_WORD - num_bits);
    }
    if (result_word_index < number.length() && num_bits < 32) {
        result += number.m_words[result_word_index] << num_bits;
    }
    return result;
}
}
