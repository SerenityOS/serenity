/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2020-2021, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "UnsignedBigIntegerAlgorithms.h"

namespace Crypto {

/**
 * Complexity: O(N) where N is the number of words in the larger number
 */
void UnsignedBigIntegerAlgorithms::add_without_allocation(
    UnsignedBigInteger const& left,
    UnsignedBigInteger const& right,
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
void UnsignedBigIntegerAlgorithms::subtract_without_allocation(
    UnsignedBigInteger const& left,
    UnsignedBigInteger const& right,
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
    VERIFY(borrow == 0);
}

}
