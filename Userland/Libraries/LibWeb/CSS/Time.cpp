/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Time.h"
#include <LibWeb/CSS/StyleValue.h>

namespace Web::CSS {

Time::Time(int value, Type type)
    : m_type(type)
    , m_value(value)
{
}

Time::Time(float value, Type type)
    : m_type(type)
    , m_value(value)
{
}

Time Time::make_calculated(NonnullRefPtr<CalculatedStyleValue> calculated_style_value)
{
    Time frequency { 0, Type::Calculated };
    frequency.m_calculated_style = move(calculated_style_value);
    return frequency;
}

Time Time::make_seconds(float value)
{
    return { value, Type::S };
}

Time Time::percentage_of(Percentage const& percentage) const
{
    VERIFY(!is_calculated());

    return Time { percentage.as_fraction() * m_value, m_type };
}

String Time::to_string() const
{
    if (is_calculated())
        return m_calculated_style->to_string();
    return String::formatted("{}{}", m_value, unit_name());
}

float Time::to_seconds() const
{
    switch (m_type) {
    case Type::Calculated:
        return m_calculated_style->resolve_time()->to_seconds();
    case Type::S:
        return m_value;
    case Type::Ms:
        return m_value / 1000.0f;
    }
    VERIFY_NOT_REACHED();
}

StringView Time::unit_name() const
{
    switch (m_type) {
    case Type::Calculated:
        return "calculated"sv;
    case Type::S:
        return "s"sv;
    case Type::Ms:
        return "ms"sv;
    }
    VERIFY_NOT_REACHED();
}

Optional<Time::Type> Time::unit_from_name(StringView name)
{
    if (name.equals_ignoring_case("s"sv)) {
        return Type::S;
    } else if (name.equals_ignoring_case("ms"sv)) {
        return Type::Ms;
    }
    return {};
}

}
