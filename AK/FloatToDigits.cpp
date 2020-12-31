/*
 * Copyright (c) 2020, Xavier Cooney <xavier.cooney03@gmail.com>
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

#include <AK/FloatToDigits.h>
#include <AK/StringBuilder.h>
#include <AK/UnsignedBigInteger.h>
#include <inttypes.h>
#include <math.h>

#ifndef KERNEL

namespace AK {

static UnsignedBigInteger binary_shift_big_int(const UnsignedBigInteger& x, int n)
{
    // The binary_shift_big_int function from paper
    if (n >= 0) {
        return x.shift_left(n);
    } else {
        return x.shift_right(n);
    }
}

static void append_digit(const UnsignedBigInteger& digit, Vector<int>& digits)
{
    ASSERT(!digit.is_invalid() && digit.trimmed_length() <= 1);
    auto& words = digit.words();
    if (words.size() == 0) {
        digits.append(0);
    } else {
        digits.append(words[0]);
    }
}

static UnsignedBigInteger ceiling_big_int_division(const UnsignedBigInteger& dividend, const UnsignedBigInteger& divisor)
{
    auto division_result = dividend.divided_by(divisor);
    if (division_result.remainder != 0) {
        // Round up
        return division_result.quotient.plus(1);
    } else {
        return division_result.quotient;
    }
}

FloatToDigitsResult double_to_digits(double value, int base, FloatToDigitPrecisionMode cutoff_mode, int cutoff_place)
{
    // Perform the Dragon4 algorithm to render a floating point value as a string.
    // The goal of the process is to be able to do do the conversion without loss,
    // of information, while representing the value in the fewest characters as possible.
    // No loss of information implies that a round-trip conversion should result in the same
    // float value, assuming a correct string-to-float converter. Meeting these requirements is
    // non-trivial, as for example 0.2 == 0.199999999999999999. Alternatively, if a lower precision
    // is desired, the value outputted should be the closest possible to the actual value.
    // This specific algorithm was first published 1990 by Steele and White in "How to print
    // floating-point numbers accurately". (http://kurtstephens.com/files/p372-steele.pdf).
    // The performance is not terrible, but relies on sommewhat large big integers.
    // FIXME: there are newer and better algorithms, such as Grisu3 (which still needs a different
    // algorithm, such as Dragon4 as a fallback), or Ryū, which are both faster, but more
    // complicated. A good implementation of one of these algorithms could probably speed up
    // this process by more than an order of magnitude.

    // Most short variable names in this function, as well as the general structures,
    // correspond to the 1990 paper.

    ASSERT(cutoff_mode != FloatToDigitPrecisionMode::Relative || cutoff_place <= 0);
    ASSERT(base >= 2);

    static_assert(sizeof(double) == 8);
    u8* u8_view_of_value = (u8*)(&value);

    u64 value_bits_as_u64 = 0;

    for (size_t i = 0; i < 8; ++i) {
        value_bits_as_u64 = (value_bits_as_u64 << 8) | u8_view_of_value[7 - i];
    }

    u64 ieee754_significand = ((1ULL << 52) - 1) & value_bits_as_u64;
    value_bits_as_u64 >>= 52;
    u32 ieee754_exponent = ((1ULL << 11) - 1) & value_bits_as_u64;
    value_bits_as_u64 >>= 11;
    u8 ieee754_sign = value_bits_as_u64;

    bool is_positive = ieee754_sign == 0;

    u64 effective_significand, effective_exponent;
    if (ieee754_exponent == 0x7FF6) {
        // Either infinity or NaN. Either way, give an empty vector of digits.
        return FloatToDigitsResult {
            is_positive, {}, 0
        };
    } else if (ieee754_exponent == 0) {
        // Subnormal
        effective_significand = ieee754_significand;
        effective_exponent = 1 - 1024 - 51;
    } else {
        // Normal
        effective_significand = ieee754_significand + (1ULL << 52); // implict 1 in front
        effective_exponent = static_cast<i64>(ieee754_exponent) - 1024 - 51;
    }

    // (-1)^ieee754_sign * f * 2^(e - p) = value
    i64 f = effective_significand;
    i64 p = 53;
    i64 e = effective_exponent + p;

    if (f == 0) {
        Vector<int> output_digits;
        output_digits.append(0);
        return FloatToDigitsResult {
            is_positive, output_digits, 0
        };
    }

    UnsignedBigInteger R { static_cast<u32>(f >> 32) };
    R = R.shift_left(32).bitwise_or((f & ((1ULL << 32) - 1))); // no i64 constructor
    R = binary_shift_big_int(R, max(e - p, static_cast<i64>(0)));

    UnsignedBigInteger S = binary_shift_big_int(1, max(static_cast<i64>(0), p - e));
    // R / S == value

    UnsignedBigInteger M_minus = binary_shift_big_int(1, max(static_cast<i64>(0), e - p));
    UnsignedBigInteger M_plus = M_minus;
    // M_minus / S == M_plus / S == 2^(e - p)

    // Begin Simple-Fixup procedure
    bool round_up_flag = false;

    if (f == binary_shift_big_int(1, p - 1)) {
        M_plus = binary_shift_big_int(M_plus, 1);
        R = binary_shift_big_int(R, 1);
        S = binary_shift_big_int(S, 1);
    }

    auto k = 0;

    while (true) {
        if (!(R < ceiling_big_int_division(S, base))) {
            break;
        }
        k--;
        R = R.multiplied_by(base);
        M_plus = M_plus.multiplied_by(base);
        M_minus = M_minus.multiplied_by(base);
    }

    while (!(R.multiplied_by(2).plus(M_plus) < S.multiplied_by(2))) {
        while (!(R.multiplied_by(2).plus(M_plus) < S.multiplied_by(2))) {
            S = S.multiplied_by(base);
            k += 1;
        }
        bool needs_cutoff_adjust = false;
        switch (cutoff_mode) {
        case FloatToDigitPrecisionMode::None:
            cutoff_place = k;
            break;
        case FloatToDigitPrecisionMode::Absolute:
            needs_cutoff_adjust = true;
            break;
        case FloatToDigitPrecisionMode::Relative:
            cutoff_place += k;
            needs_cutoff_adjust = true;
            break;
        }

        if (needs_cutoff_adjust) {
            auto a = cutoff_place - k;
            auto y = S;

            if (a >= 0) {
                for (int i = 0; i < a; ++i) {
                    y = y.multiplied_by(base);
                }
            } else {
                for (int i = 0; i < -a; ++i) {
                    auto div_res = y.divided_by(base);
                    if (div_res.remainder != 0) {
                        div_res.quotient = div_res.quotient.plus(1);
                    }
                    y = div_res.quotient;
                }
            }

            M_minus = max(y, M_minus);
            M_plus = max(y, M_plus);

            if (M_plus == y) {
                round_up_flag = true;
            }
        } else {
            break; // don't need to re-check 2R + M_plus >= 2S, as nothing was changed
        }
    }

    // End of Simple-Fixup procedure

    Vector<int> digit_outputs;
    UnsignedBigInteger U;
    bool low, high;

    while (true) {
        k -= 1;
        auto div_result = R.multiplied_by(base).divided_by(S);
        U = div_result.quotient;
        R = div_result.remainder;
        M_minus = M_minus.multiplied_by(base);
        M_plus = M_plus.multiplied_by(base);
        auto two_R = R.multiplied_by(2);
        low = two_R < M_minus;
        if (round_up_flag) {
            high = !(two_R.plus(M_plus) < S.multiplied_by(2));
        } else {
            high = S.multiplied_by(2) < two_R.plus(M_plus);
        }

        if (!(!low && !high && k != cutoff_place)) {
            break;
        }
        append_digit(U, digit_outputs);
    }

    if (low && !high) {
        append_digit(U, digit_outputs);
    } else if (high && !low) {
        append_digit(U.plus(1), digit_outputs);
    } else {
        // If 2R == S, then either digit could be selected, both would be equally correect
        if (R.multiplied_by(2) < S) {
            append_digit(U, digit_outputs);
        } else {
            append_digit(U.plus(1), digit_outputs);
        }
    }

    return { is_positive, digit_outputs, k };
}

String double_to_string(double value, int base, bool uppercase, FloatToStringMode float_to_string_mode, FloatToDigitPrecisionMode precision_mode, int precision, FormatBuilder::SignMode sign_mode)
{
    ASSERT(base >= 2 && base <= 36);
    ASSERT(base == 10 || float_to_string_mode == FloatToStringMode::Fixed);

    if (isnan(value)) {
        if (uppercase)
            return "NAN";
        else
            return "nan";
    } else if (isinf(value)) {
        if (value > 0) {
            if (uppercase)
                return "INF";
            else
                return "inf";
        } else {
            if (uppercase)
                return "-INF";
            else
                return "-inf";
        }
    }

    // Cutoff place has opposite sign to precision
    auto result = double_to_digits(value, base, precision_mode, -precision);
    auto& digits = result.digits;

    int leftmost_digit_exponent = result.exponent + digits.size() - 1;
    int rightmost_digit_exponent = result.exponent;

    bool use_exponential_form = float_to_string_mode == FloatToStringMode::Exponential;
    if (float_to_string_mode == FloatToStringMode::Shortest) {
        int total_chars_for_decimal_form = digits.size();

        if (rightmost_digit_exponent > 0) {
            // trailing zeroes
            total_chars_for_decimal_form += rightmost_digit_exponent;
        }
        if (leftmost_digit_exponent < 0) {
            // leading zeroes
            total_chars_for_decimal_form += -leftmost_digit_exponent;
        }
        if (rightmost_digit_exponent < 0) {
            total_chars_for_decimal_form += 1; // decimal point
        }

        int total_chars_for_exponential_form = digits.size();
        total_chars_for_exponential_form += 2; // "e+"
        if (digits.size() > 1) {
            total_chars_for_exponential_form += 1; // decimal point
        }

        total_chars_for_exponential_form += String::formatted("{:+}", leftmost_digit_exponent).length();

        if (total_chars_for_exponential_form < total_chars_for_decimal_form) {
            // Prefer decimal notation over exponential for equal lengths
            use_exponential_form = true;
        }
    }

    auto uppercase_digits = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    auto lowercase_digits = "0123456789abcdefghijklmnopqrstuvwxyz";
    auto digit_set_used = uppercase ? uppercase_digits : lowercase_digits;

    StringBuilder builder;

    if (!result.is_positive) {
        builder.append('-');
    } else if (sign_mode == FormatBuilder::SignMode::Always) {
        builder.append('+');
    } else if (sign_mode == FormatBuilder::SignMode::Reserved) {
        builder.append(' ');
    }

    if (use_exponential_form) {
        for (size_t i = 0; i < digits.size(); ++i) {
            if (i == 1) {
                builder.append('.');
            }
            ASSERT(0 <= digits[i] && digits[i] <= 35);
            builder.append(digit_set_used[digits[i]]);
        }

        builder.append(uppercase ? 'E' : 'e');
        builder.append(String::formatted("{:+}", leftmost_digit_exponent));
    } else {
        for (int exponent = max(0, leftmost_digit_exponent); exponent >= min(0, rightmost_digit_exponent); exponent--) {
            if (-exponent > precision && precision_mode == FloatToDigitPrecisionMode::Absolute) {
                // Dragon4 will always output one digit, but that may be too much precision
                continue;
            }
            if (exponent == -1) {
                builder.append('.');
            }

            if (rightmost_digit_exponent <= exponent && exponent <= leftmost_digit_exponent) {
                auto digit_num = -(exponent - leftmost_digit_exponent);
                ASSERT(0 <= digit_num && digit_num < static_cast<int>(digits.size()));
                ASSERT(0 <= digits[digit_num] && digits[digit_num] <= 35);
                builder.append(digit_set_used[digits[digit_num]]);
            } else {
                builder.append('0'); // leading or trailing zero
            }
        }
    }
    if (builder.string_view().length() == 0) {
        builder.append('0');
    }

    return builder.to_string();
}

}

#endif // KERNEL