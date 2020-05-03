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

UnsignedBigInteger UnsignedBigInteger::create_invalid()
{
    UnsignedBigInteger invalid(0);
    invalid.invalidate();
    return invalid;
}

// FIXME: in great need of optimisation
UnsignedBigInteger UnsignedBigInteger::import_data(const u8* ptr, size_t length)
{
    UnsignedBigInteger integer { 0 };

    for (size_t i = 0; i < length; ++i) {
        auto part = UnsignedBigInteger { ptr[length - i - 1] }.shift_left(8 * i);
        integer = integer.plus(part);
    }

    return integer;
}

size_t UnsignedBigInteger::export_data(AK::ByteBuffer& data)
{
    UnsignedBigInteger copy { *this };
    UnsignedBigInteger quotient;
    UnsignedBigInteger remainder;

    size_t size = trimmed_length() * sizeof(u32);
    size_t i = 0;
    for (; i < size; ++i) {
        if (copy.trimmed_length() == 0)
            break;
        data[size - i - 1] = copy.m_words[0] & 0xff;
        divide_u16_without_allocation(copy, 256, quotient, remainder);
        copy.set_to(quotient);
    }
    return i;
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
}

void UnsignedBigInteger::set_to(u32 other)
{
    m_is_invalid = false;
    m_words.clear_with_capacity();
    m_words.append(other);
}

void UnsignedBigInteger::set_to(const UnsignedBigInteger& other)
{
    m_is_invalid = other.m_is_invalid;
    m_words.clear_with_capacity();
    m_words.ensure_capacity(other.m_words.size());
    for (size_t i = 0; i < other.m_words.size(); ++i)
        m_words.unchecked_append(other.m_words[i]);
}

size_t UnsignedBigInteger::trimmed_length() const
{
    size_t num_leading_zeroes = 0;
    for (int i = length() - 1; i >= 0; --i, ++num_leading_zeroes) {
        if (m_words[i] != 0)
            break;
    }
    return length() - num_leading_zeroes;
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
}

bool UnsignedBigInteger::operator==(const UnsignedBigInteger& other) const
{
    auto length = trimmed_length();

    if (length != other.trimmed_length()) {
        return false;
    }

    if (is_invalid() != other.is_invalid()) {
        return false;
    }

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
    output.m_words.ensure_capacity(longer->length() + 1);

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
        output.m_words.unchecked_append(word_addition_result);
    }

    for (size_t i = shorter->length(); i < longer->length(); ++i) {
        u32 word_addition_result = longer->m_words[i] + carry;

        carry = 0;
        if (word_addition_result < longer->m_words[i]) {
            carry = 1;
        }
        output.m_words.unchecked_append(word_addition_result);
    }
    if (carry) {
        output.m_words.unchecked_append(carry);
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
    output.m_words.ensure_capacity(own_length);

    for (size_t i = 0; i < own_length; ++i) {
        u32 other_word = (i < other_length) ? right.m_words[i] : 0;
        i64 temp = static_cast<i64>(left.m_words[i]) - static_cast<i64>(other_word) - static_cast<i64>(borrow);
        // If temp < 0, we had an underflow
        borrow = (temp >= 0) ? 0 : 1;
        if (temp < 0) {
            temp += (UINT32_MAX + 1);
        }
        output.m_words.append(temp);
    }

    // This assertion should not fail, because we verified that *this>=other at the beginning of the function
    ASSERT(borrow == 0);
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
    output.m_words.ensure_capacity(number_of_words + number.length());

    for (size_t i = 0; i < number_of_words; ++i) {
        output.m_words.unchecked_append(0);
    }
    for (size_t i = 0; i < number.length(); ++i) {
        output.m_words.unchecked_append(number.m_words[i]);
    }
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
