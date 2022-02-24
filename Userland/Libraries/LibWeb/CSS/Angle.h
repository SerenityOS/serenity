/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <LibWeb/Forward.h>

namespace Web::CSS {

class Angle {
public:
    enum class Type {
        Calculated,
        Deg,
        Grad,
        Rad,
        Turn,
    };

    static Optional<Type> unit_from_name(StringView);

    Angle(int value, Type type);
    Angle(float value, Type type);
    static Angle make_calculated(NonnullRefPtr<CalculatedStyleValue>);
    static Angle make_degrees(float);
    Angle percentage_of(Percentage const&) const;

    bool is_calculated() const { return m_type == Type::Calculated; }

    String to_string() const;
    float to_degrees() const;

    bool operator==(Angle const& other) const
    {
        if (is_calculated())
            return m_calculated_style == other.m_calculated_style;
        return m_type == other.m_type && m_value == other.m_value;
    }

    bool operator!=(Angle const& other) const
    {
        return !(*this == other);
    }

private:
    StringView unit_name() const;

    Type m_type;
    float m_value { 0 };
    RefPtr<CalculatedStyleValue> m_calculated_style;
};

}
