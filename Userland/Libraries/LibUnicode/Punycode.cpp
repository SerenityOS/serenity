/*
 * Copyright (c) 2023, Simon Wanner <simon@skyrising.xyz>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Utf32View.h>
#include <LibUnicode/Punycode.h>

namespace Unicode::Punycode {

// https://www.rfc-editor.org/rfc/rfc3492.html#section-5
static constexpr u32 BASE = 36;
static constexpr u32 TMIN = 1;
static constexpr u32 TMAX = 26;
static constexpr u32 SKEW = 38;
static constexpr u32 DAMP = 700;
static constexpr u32 INITIAL_BIAS = 72;
static constexpr u32 INITIAL_N = 0x80;
static constexpr u32 DELIMITER = '-';

static Optional<u32> digit_value_of_code_point(u32 code_point)
{
    if (code_point >= 'A' && code_point <= 'Z')
        return code_point - 'A';
    if (code_point >= 'a' && code_point <= 'z')
        return code_point - 'a';
    if (code_point >= '0' && code_point <= '9')
        return code_point - '0' + 26;
    return {};
}

// https://www.rfc-editor.org/rfc/rfc3492.html#section-6.1
static u32 adapt(u32 delta, u32 num_points, bool first_time)
{
    // if firsttime then let delta = delta div damp
    if (first_time)
        delta = delta / DAMP;
    // else let delta = delta div 2
    else
        delta = delta / 2;

    // let delta = delta + (delta div numpoints)
    delta = delta + (delta / num_points);

    // let k = 0
    u32 k = 0;

    // while delta > ((base - tmin) * tmax) div 2 do begin
    while (delta > ((BASE - TMIN) * TMAX) / 2) {
        // let delta = delta div (base - tmin)
        delta = delta / (BASE - TMIN);

        // let k = k + base
        k = k + BASE;
    }

    // return k + (((base - tmin + 1) * delta) div (delta + skew))
    return k + (((BASE - TMIN + 1) * delta) / (delta + SKEW));
}

// https://www.rfc-editor.org/rfc/rfc3492.html#section-6.2
ErrorOr<String> decode(StringView input)
{
    size_t consumed = 0;

    // let n = initial_n
    Checked<size_t> n = INITIAL_N;

    // let i = 0
    Checked<u32> i = 0;

    // let bias = initial_bias
    u32 bias = INITIAL_BIAS;

    // let output = an empty string indexed from 0
    Vector<u32> output;

    // consume all code points before the last delimiter (if there is one)
    //   and copy them to output, fail on any non-basic code point
    Optional<size_t> last_delimiter_index = input.find_last(DELIMITER);
    if (last_delimiter_index.has_value()) {
        for (; consumed < last_delimiter_index.value(); consumed++) {
            if (!is_ascii(input[consumed]))
                return Error::from_string_literal("Unexpected non-basic code point");
            TRY(output.try_append(input[consumed]));
        }

        // if more than zero code points were consumed then consume one more
        //   (which will be the last delimiter)
        if (last_delimiter_index.value() > 0) {
            auto next = input[consumed++];
            VERIFY(next == DELIMITER);
        }
    }

    // while the input is not exhausted do begin
    while (consumed < input.length()) {
        // let oldi = i
        Checked<u32> old_i = i;

        // let w = 1
        Checked<u32> w = 1;

        // for k = base to infinity in steps of base do begin
        for (size_t k = BASE;; k += BASE) {
            // consume a code point, or fail if there was none to consume
            if (consumed >= input.length())
                return Error::from_string_literal("No more code points to consume");
            auto code_point = input[consumed++];

            // let digit = the code point's digit-value, fail if it has none
            auto digit = digit_value_of_code_point(code_point);
            if (!digit.has_value())
                return Error::from_string_literal("Invalid base-36 digit");

            // let i = i + digit * w, fail on overflow
            i = i + Checked(digit.value()) * w;
            if (i.has_overflow())
                return Error::from_string_literal("Numeric overflow");

            // let t = tmin if k <= bias {+ tmin}, or
            //         tmax if k >= bias + tmax, or k - bias otherwise
            u32 t = k <= bias ? TMIN : (k >= bias + TMAX ? TMAX : k - bias);

            // if digit < t then break
            if (digit.value() < t)
                break;

            // let w = w * (base - t), fail on overflow
            w = w * Checked(BASE - t);
            if (w.has_overflow())
                return Error::from_string_literal("Numeric overflow");
        }
        // let bias = adapt(i - oldi, length(output) + 1, test oldi is 0?)
        bias = adapt((i - old_i).value(), output.size() + 1, !old_i);

        // let n = n + i div (length(output) + 1), fail on overflow
        n = n + Checked(static_cast<size_t>(i.value() / static_cast<u32>(output.size() + 1)));
        if (n.has_overflow())
            return Error::from_string_literal("Numeric overflow");

        // let i = i mod (length(output) + 1)
        i = i % Checked(static_cast<u32>(output.size() + 1));

        // {if n is a basic code point then fail}
        // NOTE: The full statement enclosed in braces (checking whether n is a basic code point) can be omitted if initial_n exceeds all basic code points
        //       (which is true for Punycode), because n is never less than initial_n.
        VERIFY(!is_ascii(n.value()));

        // insert n into output at position i
        TRY(output.try_insert(i.value(), n.value()));

        // increment i
        i++;
    }

    StringBuilder builder;
    TRY(builder.try_append(Utf32View(output.data(), output.size())));
    return builder.to_string();
}

}
