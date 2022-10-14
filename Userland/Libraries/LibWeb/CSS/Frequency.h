/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/RefPtr.h>
#include <LibWeb/Forward.h>

namespace Web::CSS {
class Frequency {
public:
    enum class Type {
        Calculated,
        Hz,
        kHz
    };

    static Optional<Type> unit_from_name(StringView);

    Frequency(int value, Type type);
    Frequency(float value, Type type);
    static Frequency make_calculated(NonnullRefPtr<CalculatedStyleValue>);
    static Frequency make_hertz(float);
    Frequency percentage_of(Percentage const&) const;

    bool is_calculated() const { return m_type == Type::Calculated; }
    NonnullRefPtr<CalculatedStyleValue> calculated_style_value() const;

    DeprecatedString to_deprecated_string() const;
    float to_hertz() const;

    bool operator==(Frequency const& other) const
    {
        if (is_calculated())
            return m_calculated_style == other.m_calculated_style;
        return m_type == other.m_type && m_value == other.m_value;
    }

private:
    StringView unit_name() const;

    Type m_type;
    float m_value { 0 };
    RefPtr<CalculatedStyleValue> m_calculated_style;
};

}

template<>
struct AK::Formatter<Web::CSS::Frequency> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Web::CSS::Frequency const& frequency)
    {
        return Formatter<StringView>::format(builder, frequency.to_deprecated_string());
    }
};
