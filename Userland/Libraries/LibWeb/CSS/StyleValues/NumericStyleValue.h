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

class NumericStyleValue : public StyleValueWithDefaultOperators<NumericStyleValue> {
public:
    static ErrorOr<ValueComparingNonnullRefPtr<NumericStyleValue>> create_float(float value)
    {
        return adopt_nonnull_ref_or_enomem(new (nothrow) NumericStyleValue(value));
    }

    static ErrorOr<ValueComparingNonnullRefPtr<NumericStyleValue>> create_integer(i64 value)
    {
        return adopt_nonnull_ref_or_enomem(new (nothrow) NumericStyleValue(value));
    }

    virtual bool has_length() const override { return number() == 0; }
    virtual Length to_length() const override { return Length::make_px(0); }

    float number() const
    {
        return m_value.visit(
            [](float value) { return value; },
            [](i64 value) { return (float)value; });
    }

    virtual bool has_integer() const override { return m_value.has<i64>(); }
    virtual float to_integer() const override { return m_value.get<i64>(); }

    virtual ErrorOr<String> to_string() const override;

    bool properties_equal(NumericStyleValue const& other) const { return m_value == other.m_value; }

private:
    explicit NumericStyleValue(Variant<float, i64> value)
        : StyleValueWithDefaultOperators(Type::Numeric)
        , m_value(move(value))
    {
    }

    Variant<float, i64> m_value { (i64)0 };
};

}
