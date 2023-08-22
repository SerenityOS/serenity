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
#include <LibWeb/CSS/StyleValues/BorderRadiusStyleValue.h>

namespace Web::CSS {

class BorderRadiusShorthandStyleValue final : public StyleValueWithDefaultOperators<BorderRadiusShorthandStyleValue> {
public:
    static ValueComparingNonnullRefPtr<BorderRadiusShorthandStyleValue> create(
        ValueComparingNonnullRefPtr<BorderRadiusStyleValue const> top_left,
        ValueComparingNonnullRefPtr<BorderRadiusStyleValue const> top_right,
        ValueComparingNonnullRefPtr<BorderRadiusStyleValue const> bottom_right,
        ValueComparingNonnullRefPtr<BorderRadiusStyleValue const> bottom_left)
    {
        return adopt_ref(*new (nothrow) BorderRadiusShorthandStyleValue(move(top_left), move(top_right), move(bottom_right), move(bottom_left)));
    }
    virtual ~BorderRadiusShorthandStyleValue() override = default;

    auto top_left() const { return m_properties.top_left; }
    auto top_right() const { return m_properties.top_right; }
    auto bottom_right() const { return m_properties.bottom_right; }
    auto bottom_left() const { return m_properties.bottom_left; }

    virtual String to_string() const override;

    bool properties_equal(BorderRadiusShorthandStyleValue const& other) const { return m_properties == other.m_properties; }

private:
    BorderRadiusShorthandStyleValue(
        ValueComparingNonnullRefPtr<BorderRadiusStyleValue const> top_left,
        ValueComparingNonnullRefPtr<BorderRadiusStyleValue const> top_right,
        ValueComparingNonnullRefPtr<BorderRadiusStyleValue const> bottom_right,
        ValueComparingNonnullRefPtr<BorderRadiusStyleValue const> bottom_left)
        : StyleValueWithDefaultOperators(Type::BorderRadiusShorthand)
        , m_properties { .top_left = move(top_left), .top_right = move(top_right), .bottom_right = move(bottom_right), .bottom_left = move(bottom_left) }
    {
    }

    struct Properties {
        ValueComparingNonnullRefPtr<BorderRadiusStyleValue const> top_left;
        ValueComparingNonnullRefPtr<BorderRadiusStyleValue const> top_right;
        ValueComparingNonnullRefPtr<BorderRadiusStyleValue const> bottom_right;
        ValueComparingNonnullRefPtr<BorderRadiusStyleValue const> bottom_left;
        bool operator==(Properties const&) const = default;
    } m_properties;
};

}
