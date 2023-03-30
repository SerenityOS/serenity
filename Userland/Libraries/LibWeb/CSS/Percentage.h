/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/Variant.h>
#include <LibWeb/CSS/Angle.h>
#include <LibWeb/CSS/Frequency.h>
#include <LibWeb/CSS/Length.h>
#include <LibWeb/CSS/Number.h>
#include <LibWeb/CSS/Time.h>

namespace Web::CSS {

class Percentage {
public:
    explicit Percentage(int value)
        : m_value(value)
    {
    }

    explicit Percentage(float value)
        : m_value(value)
    {
    }

    float value() const { return m_value; }
    float as_fraction() const { return m_value * 0.01f; }

    ErrorOr<String> to_string() const
    {
        return String::formatted("{}%", m_value);
    }

    bool operator==(Percentage const& other) const { return m_value == other.m_value; }

private:
    float m_value;
};

}
