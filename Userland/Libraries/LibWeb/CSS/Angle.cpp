/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Angle.h"
#include <AK/Math.h>
#include <LibWeb/CSS/StyleValue.h>

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

Angle Angle::make_calculated(NonnullRefPtr<CalculatedStyleValue> calculated_style_value)
{
    Angle angle { 0, Type::Calculated };
    angle.m_calculated_style = move(calculated_style_value);
    return angle;
}

Angle Angle::make_degrees(float value)
{
    return { value, Type::Deg };
}

Angle Angle::percentage_of(Percentage const& percentage) const
{
    VERIFY(!is_calculated());

    return Angle { percentage.as_fraction() * m_value, m_type };
}

String Angle::to_string() const
{
    if (is_calculated())
        return m_calculated_style->to_string();
    return String::formatted("{}{}", m_value, unit_name());
}

float Angle::to_degrees() const
{
    switch (m_type) {
    case Type::Calculated:
        return m_calculated_style->resolve_angle()->to_degrees();
    case Type::Deg:
        return m_value;
    case Type::Grad:
        return m_value * (360.0f / 400.0f);
    case Type::Rad:
        return m_value * (360.0f / 2 * AK::Pi<float>);
    case Type::Turn:
        return m_value * 360.0f;
    }
    VERIFY_NOT_REACHED();
}

StringView Angle::unit_name() const
{
    switch (m_type) {
    case Type::Calculated:
        return "calculated"sv;
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
    if (name.equals_ignoring_case("deg"sv)) {
        return Type::Deg;
    } else if (name.equals_ignoring_case("grad"sv)) {
        return Type::Grad;
    } else if (name.equals_ignoring_case("rad"sv)) {
        return Type::Rad;
    } else if (name.equals_ignoring_case("turn"sv)) {
        return Type::Turn;
    }
    return {};
}

}
