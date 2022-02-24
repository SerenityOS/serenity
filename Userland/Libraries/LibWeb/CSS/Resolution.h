/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibWeb/Forward.h>

namespace Web::CSS {

class Resolution {
public:
    enum class Type {
        Dpi,
        Dpcm,
        Dppx,
    };

    static Optional<Type> unit_from_name(StringView);

    Resolution(int value, Type type);
    Resolution(float value, Type type);

    String to_string() const;
    float to_dots_per_pixel() const;

    bool operator==(Resolution const& other) const
    {
        return m_type == other.m_type && m_value == other.m_value;
    }

    bool operator!=(Resolution const& other) const
    {
        return !(*this == other);
    }

private:
    StringView unit_name() const;

    Type m_type;
    float m_value { 0 };
};
}
