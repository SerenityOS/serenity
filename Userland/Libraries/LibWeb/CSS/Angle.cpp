/*
 * Copyright (c) 2022-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Angle.h"
#include <AK/Math.h>
#include <LibWeb/CSS/Percentage.h>

namespace Web::CSS {

Angle::Angle(int value, Type type)
    : m_type(type)
    , m_value(value)
{
}

Angle::Angle(float value, Type type)
    : m_type(type)
    , m_value(value)
{
}

Angle Angle::make_degrees(float value)
{
    return { value, Type::Deg };
}

Angle Angle::percentage_of(Percentage const& percentage) const
{
    return Angle { percentage.as_fraction() * m_value, m_type };
}

ErrorOr<String> Angle::to_string() const
{
    return String::formatted("{}deg", to_degrees());
}

float Angle::to_degrees() const
{
    switch (m_type) {
    case Type::Deg:
        return m_value;
    case Type::Grad:
        return m_value * (360.0f / 400.0f);
    case Type::Rad:
        return m_value * (180.0f / AK::Pi<float>);
    case Type::Turn:
        return m_value * 360.0f;
    }
    VERIFY_NOT_REACHED();
}

StringView Angle::unit_name() const
{
    switch (m_type) {
    case Type::Deg:
        return "deg"sv;
    case Type::Grad:
        return "grad"sv;
    case Type::Rad:
        return "rad"sv;
    case Type::Turn:
        return "turn"sv;
    }
    VERIFY_NOT_REACHED();
}

Optional<Angle::Type> Angle::unit_from_name(StringView name)
{
    if (name.equals_ignoring_ascii_case("deg"sv)) {
        return Type::Deg;
    }
    if (name.equals_ignoring_ascii_case("grad"sv)) {
        return Type::Grad;
    }
    if (name.equals_ignoring_ascii_case("rad"sv)) {
        return Type::Rad;
    }
    if (name.equals_ignoring_ascii_case("turn"sv)) {
        return Type::Turn;
    }
    return {};
}

}
