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

class OverflowStyleValue final : public StyleValueWithDefaultOperators<OverflowStyleValue> {
public:
    static ValueComparingNonnullRefPtr<OverflowStyleValue> create(ValueComparingNonnullRefPtr<StyleValue> overflow_x, ValueComparingNonnullRefPtr<StyleValue> overflow_y)
    {
        return adopt_ref(*new (nothrow) OverflowStyleValue(move(overflow_x), move(overflow_y)));
    }
    virtual ~OverflowStyleValue() override = default;

    ValueComparingNonnullRefPtr<StyleValue> overflow_x() const { return m_properties.overflow_x; }
    ValueComparingNonnullRefPtr<StyleValue> overflow_y() const { return m_properties.overflow_y; }

    virtual ErrorOr<String> to_string() const override;

    bool properties_equal(OverflowStyleValue const& other) const { return m_properties == other.m_properties; }

private:
    OverflowStyleValue(ValueComparingNonnullRefPtr<StyleValue> overflow_x, ValueComparingNonnullRefPtr<StyleValue> overflow_y)
        : StyleValueWithDefaultOperators(Type::Overflow)
        , m_properties { .overflow_x = move(overflow_x), .overflow_y = move(overflow_y) }
    {
    }

    struct Properties {
        ValueComparingNonnullRefPtr<StyleValue> overflow_x;
        ValueComparingNonnullRefPtr<StyleValue> overflow_y;
        bool operator==(Properties const&) const = default;
    } m_properties;
};

}
