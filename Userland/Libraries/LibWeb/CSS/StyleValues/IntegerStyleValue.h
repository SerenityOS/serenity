/*
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/CSS/StyleValue.h>

namespace Web::CSS {

class IntegerStyleValue : public StyleValueWithDefaultOperators<IntegerStyleValue> {
public:
    static ErrorOr<ValueComparingNonnullRefPtr<IntegerStyleValue>> create(i64 value)
    {
        return adopt_nonnull_ref_or_enomem(new (nothrow) IntegerStyleValue(value));
    }

    i64 integer() const { return m_value; }

    virtual ErrorOr<String> to_string() const override;

    bool properties_equal(IntegerStyleValue const& other) const { return m_value == other.m_value; }

private:
    explicit IntegerStyleValue(i64 value)
        : StyleValueWithDefaultOperators(Type::Integer)
        , m_value(value)
    {
    }

    i64 m_value { 0 };
};

}
