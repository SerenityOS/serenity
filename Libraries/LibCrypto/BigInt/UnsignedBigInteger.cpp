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

namespace Crypto {

UnsignedBigInteger UnsignedBigInteger::add(const UnsignedBigInteger& other)
{
    const UnsignedBigInteger* const longer = (length() > other.length()) ? this : &other;
    const UnsignedBigInteger* const shorter = (longer == &other) ? this : &other;
    UnsignedBigInteger result;

    u8 carry = 0;
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
        result.m_words.append(word_addition_result);
    }

    for (size_t i = shorter->length(); i < longer->length(); ++i) {
        u32 word_addition_result = longer->m_words[i] + carry;

        carry = 0;
        if (word_addition_result < longer->m_words[i]) {
            carry = 1;
        }
        result.m_words.append(word_addition_result);
    }
    if (carry) {
        result.m_words.append(carry);
    }
    return result;
}

UnsignedBigInteger UnsignedBigInteger::sub(const UnsignedBigInteger& other)
{
    UnsignedBigInteger result;

    if (*this < other) {
        dbg() << "WARNING: bigint subtraction creates a negative number!";
        return UnsignedBigInteger::create_invalid();
    }

    u8 borrow = 0;
    for (size_t i = 0; i < other.length(); ++i) {
        ASSERT(!(borrow == 1 && m_words[i] == 0));

        if (m_words[i] - borrow < other.m_words[i]) {
            u64 after_borrow = static_cast<u64>(m_words[i] - borrow) + (UINT32_MAX + 1);
            result.m_words.append(static_cast<u32>(after_borrow - static_cast<u64>(other.m_words[i])));
            borrow = 1;
        } else {
            result.m_words.append(m_words[i] - borrow - other.m_words[i]);
            borrow = 0;
        }
    }

    for (size_t i = other.length(); i < length(); ++i) {
        ASSERT(!(borrow == 1 && m_words[i] == 0));
        result.m_words.append(m_words[i] - borrow);
        borrow = 0;
    }

    return result;
}

bool UnsignedBigInteger::operator==(const UnsignedBigInteger& other) const
{
    if (trimmed_length() != other.trimmed_length()) {
        return false;
    }
    if (is_invalid() != other.is_invalid()) {
        return false;
    }

    for (size_t i = 0; i < trimmed_length(); ++i) {
        if (m_words[i] != other.words()[i])
            return false;
    }
    return true;
}

bool UnsignedBigInteger::operator<(const UnsignedBigInteger& other) const
{
    if (trimmed_length() < other.trimmed_length()) {
        return true;
    }
    if (trimmed_length() > other.trimmed_length()) {
        return false;
    }

    size_t length = trimmed_length();
    if (length == 0) {
        return false;
    }

    return m_words[length - 1] < other.m_words[length - 1];
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

UnsignedBigInteger UnsignedBigInteger::create_invalid()
{
    UnsignedBigInteger invalid(0);
    invalid.invalidate();
    return invalid;
}

}
