/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/CSS/StyleValue.h>

namespace Web::CSS {

class NumberStyleValue : public StyleValueWithDefaultOperators<NumberStyleValue> {
public:
    static ErrorOr<ValueComparingNonnullRefPtr<NumberStyleValue>> create(float value)
    {
        return adopt_nonnull_ref_or_enomem(new (nothrow) NumberStyleValue(value));
    }

    float number() const { return m_value; }

    virtual ErrorOr<String> to_string() const override;

    bool properties_equal(NumberStyleValue const& other) const { return m_value == other.m_value; }

private:
    explicit NumberStyleValue(float value)
        : StyleValueWithDefaultOperators(Type::Number)
        , m_value(value)
    {
    }

    float m_value { 0 };
};

}
