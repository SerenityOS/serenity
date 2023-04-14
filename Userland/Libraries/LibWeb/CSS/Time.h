/*
 * Copyright (c) 2022-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibWeb/Forward.h>

namespace Web::CSS {

class Time {
public:
    enum class Type {
        S,
        Ms,
    };

    static Optional<Type> unit_from_name(StringView);

    Time(int value, Type type);
    Time(float value, Type type);
    static Time make_seconds(float);
    Time percentage_of(Percentage const&) const;

    ErrorOr<String> to_string() const;
    float to_seconds() const;

    Type type() const { return m_type; }
    float raw_value() const { return m_value; }

    bool operator==(Time const& other) const
    {
        return m_type == other.m_type && m_value == other.m_value;
    }

private:
    StringView unit_name() const;

    Type m_type;
    float m_value { 0 };
};

}

template<>
struct AK::Formatter<Web::CSS::Time> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Web::CSS::Time const& time)
    {
        return Formatter<StringView>::format(builder, TRY(time.to_string()));
    }
};
