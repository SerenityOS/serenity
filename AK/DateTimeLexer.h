/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/CharacterTypes.h>
#include <AK/GenericLexer.h>
#include <AK/Optional.h>

namespace AK {

class DateTimeLexer : public GenericLexer {
public:
    constexpr explicit DateTimeLexer(StringView input)
        : GenericLexer(input)
    {
    }

    Optional<StringView> consume_year()
    {
        if (tell_remaining() < 4)
            return {};

        for (auto i = 0; i < 4; ++i) {
            if (!is_ascii_digit(peek(i)))
                return {};
        }
        return consume(4);
    }

    Optional<StringView> consume_month()
    {
        if (tell_remaining() < 2)
            return {};

        auto tens = peek();
        if (tens != '0' && tens != '1')
            return {};

        auto ones = peek(1);
        if (!is_ascii_digit(ones))
            return {};

        if (tens == '0') { // 01, 02, 03, 04, 05, 06, 07, 08, 09
            if (ones == '0')
                return {};
        } else if (ones > '2') { // 10, 11, 12
            return {};
        }

        return consume(2);
    }

    Optional<StringView> consume_day()
    {
        if (tell_remaining() < 2)
            return {};

        auto tens = peek();
        if (tens < '0' || tens > '3')
            return {};

        auto ones = peek(1);
        if (!is_ascii_digit(ones))
            return {};

        if (tens == '0') { // 01, 02, 03, 04, 05, 06, 07, 08, 09
            if (ones == '0')
                return {};
        } else if (tens == '3') { // 30, 31
            if (ones != '0' && ones != '1')
                return {};
        } else if (!is_ascii_digit(ones)) { // 10 - 29
            return {};
        }

        return consume(2);
    }

    Optional<StringView> consume_sign()
    {
        if (!tell_remaining())
            return {};

        if (next_is("\xE2\x88\x92"sv))
            return consume(3);
        else if (next_is('-') || next_is('+'))
            return consume(1);
        else
            return {};
    };

    Optional<StringView> consume_hours()
    {
        if (tell_remaining() < 2)
            return {};

        char tens = peek();
        if (tens != '0' && tens != '1' && tens != '2')
            return {};

        char ones = peek(1);
        if (!is_ascii_digit(ones) || (tens == '2' && ones > '3'))
            return {};

        return consume(2);
    }

    Optional<StringView> consume_minutes_or_seconds()
    {
        if (tell_remaining() < 2)
            return {};

        char tens = peek();
        if (tens < '0' || tens > '5')
            return {};

        if (!is_ascii_digit(peek(1)))
            return {};

        return consume(2);
    }

    Optional<StringView> consume_fractional_seconds()
    {
        if (!tell_remaining())
            return {};

        auto length = min(tell_remaining(), 9);
        for (size_t i = 0; i < length; ++i) {
            if (is_ascii_digit(peek(i)))
                continue;
            length = i;
            break;
        }

        return consume(length);
    }
};

}

using AK::DateTimeLexer;
