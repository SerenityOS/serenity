/*
 * Copyright (c) 2022-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Time.h"
#include <LibWeb/CSS/Percentage.h>

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

Time Time::make_seconds(float value)
{
    return { value, Type::S };
}

Time Time::percentage_of(Percentage const& percentage) const
{
    return Time { percentage.as_fraction() * m_value, m_type };
}

ErrorOr<String> Time::to_string() const
{
    return String::formatted("{}s", to_seconds());
}

float Time::to_seconds() const
{
    switch (m_type) {
    case Type::S:
        return m_value;
    case Type::Ms:
        return m_value / 1000.0f;
    }
    VERIFY_NOT_REACHED();
}

double Time::to_milliseconds() const
{
    switch (m_type) {
    case Type::S:
        return static_cast<double>(m_value) * 1000.0;
    case Type::Ms:
        return static_cast<double>(m_value);
    }
    VERIFY_NOT_REACHED();
}

StringView Time::unit_name() const
{
    switch (m_type) {
    case Type::S:
        return "s"sv;
    case Type::Ms:
        return "ms"sv;
    }
    VERIFY_NOT_REACHED();
}

Optional<Time::Type> Time::unit_from_name(StringView name)
{
    if (name.equals_ignoring_ascii_case("s"sv)) {
        return Type::S;
    } else if (name.equals_ignoring_ascii_case("ms"sv)) {
        return Type::Ms;
    }
    return {};
}

}
