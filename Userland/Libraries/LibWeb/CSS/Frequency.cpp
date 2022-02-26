/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Frequency.h"
#include <LibWeb/CSS/StyleValue.h>

namespace Web::CSS {

Frequency::Frequency(int value, Type type)
    : m_type(type)
    , m_value(value)
{
}

Frequency::Frequency(float value, Type type)
    : m_type(type)
    , m_value(value)
{
}

Frequency Frequency::make_calculated(NonnullRefPtr<CalculatedStyleValue> calculated_style_value)
{
    Frequency frequency { 0, Type::Calculated };
    frequency.m_calculated_style = move(calculated_style_value);
    return frequency;
}

Frequency Frequency::make_hertz(float value)
{
    return { value, Type::Hz };
}

Frequency Frequency::percentage_of(Percentage const& percentage) const
{
    VERIFY(!is_calculated());

    return Frequency { percentage.as_fraction() * m_value, m_type };
}

String Frequency::to_string() const
{
    if (is_calculated())
        return m_calculated_style->to_string();
    return String::formatted("{}{}", m_value, unit_name());
}

float Frequency::to_hertz() const
{
    switch (m_type) {
    case Type::Calculated:
        return m_calculated_style->resolve_frequency()->to_hertz();
    case Type::Hz:
        return m_value;
    case Type::kHz:
        return m_value * 1000;
    }
    VERIFY_NOT_REACHED();
}

StringView Frequency::unit_name() const
{
    switch (m_type) {
    case Type::Calculated:
        return "calculated"sv;
    case Type::Hz:
        return "hz"sv;
    case Type::kHz:
        return "khz"sv;
    }
    VERIFY_NOT_REACHED();
}

Optional<Frequency::Type> Frequency::unit_from_name(StringView name)
{
    if (name.equals_ignoring_case("hz"sv)) {
        return Type::Hz;
    } else if (name.equals_ignoring_case("khz"sv)) {
        return Type::kHz;
    }
    return {};
}

}
