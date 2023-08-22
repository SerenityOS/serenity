/*
 * Copyright (c) 2022-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Resolution.h"

namespace Web::CSS {

Resolution::Resolution(double value, Type type)
    : m_type(type)
    , m_value(value)
{
}

String Resolution::to_string() const
{
    return MUST(String::formatted("{}dppx", to_dots_per_pixel()));
}

double Resolution::to_dots_per_pixel() const
{
    switch (m_type) {
    case Type::Dpi:
        return m_value * 96; // 1in = 2.54cm = 96px
    case Type::Dpcm:
        return m_value * (96.0 / 2.54); // 1cm = 96px/2.54
    case Type::Dppx:
        return m_value;
    }
    VERIFY_NOT_REACHED();
}

StringView Resolution::unit_name() const
{
    switch (m_type) {
    case Type::Dpi:
        return "dpi"sv;
    case Type::Dpcm:
        return "dpcm"sv;
    case Type::Dppx:
        return "dppx"sv;
    }
    VERIFY_NOT_REACHED();
}

Optional<Resolution::Type> Resolution::unit_from_name(StringView name)
{
    if (name.equals_ignoring_ascii_case("dpi"sv)) {
        return Type::Dpi;
    } else if (name.equals_ignoring_ascii_case("dpcm"sv)) {
        return Type::Dpcm;
    } else if (name.equals_ignoring_ascii_case("dppx"sv)) {
        return Type::Dppx;
    }
    return {};
}

}
