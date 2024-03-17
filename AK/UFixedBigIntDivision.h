/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Diagnostics.h>
#include <AK/UFixedBigInt.h>

namespace AK::Detail {

template<size_t dividend_bit_size, size_t divisor_bit_size, bool restore_remainder>
constexpr void div_mod_internal(
    StaticStorage<false, dividend_bit_size> const& operand1,
    StaticStorage<false, divisor_bit_size> const& operand2,
    StaticStorage<false, dividend_bit_size>& quotient,
    StaticStorage<false, divisor_bit_size>& remainder)
{
    using Ops = StorageOperations<>;

    size_t dividend_len = operand1.size(), divisor_len = operand2.size();
    while (divisor_len > 0 && !operand2[divisor_len - 1])
        --divisor_len;
    while (dividend_len > 0 && !operand1[dividend_len - 1])
        --dividend_len;

    // FIXME: Should raise SIGFPE instead
    VERIFY(divisor_len); // VERIFY(divisor != 0)

    // Fast paths
    if (divisor_len == 1 && operand2[0] == 1) { // divisor == 1
        quotient = operand1;
        if constexpr (restore_remainder)
            Ops::set(0, remainder);
        return;
    }

    if (dividend_len < divisor_len) { // dividend < divisor
        Ops::set(0, quotient);
        if constexpr (restore_remainder)
            Ops::copy(operand1, remainder);
        return;
    }

    if (divisor_len == 1 && dividend_len == 1) { // NativeWord / NativeWord
        Ops::set(operand1[0] / operand2[0], quotient);
        if constexpr (restore_remainder)
            Ops::set(operand1[0] % operand2[0], remainder);
        return;
    }

    if (divisor_len == 1) { // BigInt by NativeWord
        auto u = (static_cast<NativeDoubleWord>(operand1[dividend_len - 1]) << native_word_size) + operand1[dividend_len - 2];
        auto divisor = operand2[0];

        auto top = u / divisor;
        quotient[dividend_len - 1] = static_cast<NativeWord>(top >> native_word_size);
        quotient[dividend_len - 2] = static_cast<NativeWord>(top);

        auto carry = static_cast<NativeWord>(u % divisor);
        for (size_t i = dividend_len - 2; i--;)
            quotient[i] = div_mod_words(operand1[i], carry, divisor, carry);
        for (size_t i = dividend_len; i < quotient.size(); ++i)
            quotient[i] = 0;
        if constexpr (restore_remainder)
            Ops::set(carry, remainder);
        return;
    }

    // Knuth's algorithm D
    StaticStorage<false, dividend_bit_size + native_word_size> dividend;
    Ops::copy(operand1, dividend);
    auto divisor = operand2;

    // D1. Normalize
    // FIXME: Investigate GCC producing bogus -Warray-bounds when dividing u128 by u32. This code
    //        should not be reachable at all in this case because fast paths above cover all cases
    //        when `operand2.size() == 1`.
    AK_IGNORE_DIAGNOSTIC("-Warray-bounds", size_t shift = count_leading_zeroes(divisor[divisor_len - 1]);)
    Ops::shift_left(dividend, shift, dividend);
    Ops::shift_left(divisor, shift, divisor);

    auto divisor_approx = divisor[divisor_len - 1];

    for (size_t i = dividend_len + 1; i-- > divisor_len;) {
        // D3. Calculate qhat
        NativeWord qhat;
        VERIFY(dividend[i] <= divisor_approx);
        if (dividend[i] == divisor_approx) {
            qhat = max_native_word;
        } else {
            NativeWord rhat;
            qhat = div_mod_words(dividend[i - 1], dividend[i], divisor_approx, rhat);

            auto is_qhat_too_large = [&] {
                return UFixedBigInt<native_word_size> { qhat }.wide_multiply(divisor[divisor_len - 2]) > u128 { dividend[i - 2], rhat };
            };
            if (is_qhat_too_large()) {
                --qhat;
                bool carry = false;
                rhat = add_words(rhat, divisor_approx, carry);
                if (!carry && is_qhat_too_large())
                    --qhat;
            }
        }

        // D4. Multiply & subtract
        NativeWord mul_carry = 0;
        bool sub_carry = false;
        for (size_t j = 0; j < divisor_len; ++j) {
            auto mul_result = UFixedBigInt<native_word_size> { qhat }.wide_multiply(divisor[j]) + mul_carry;
            auto& output = dividend[i + j - divisor_len];
            output = sub_words(output, mul_result.low(), sub_carry);
            mul_carry = mul_result.high();
        }
        dividend[i] = sub_words(dividend[i], mul_carry, sub_carry);

        if (sub_carry) {
            // D6. Add back
            auto dividend_part = UnsignedStorageSpan { dividend.data() + i - divisor_len, divisor_len + 1 };
            VERIFY(Ops::add<false>(dividend_part, divisor, dividend_part));
        }

        quotient[i - divisor_len] = qhat - sub_carry;
    }

    for (size_t i = dividend_len - divisor_len + 1; i < quotient.size(); ++i)
        quotient[i] = 0;

    // D8. Unnormalize
    if constexpr (restore_remainder)
        Ops::shift_right(UnsignedStorageSpan { dividend.data(), remainder.size() }, shift, remainder);
}

}
