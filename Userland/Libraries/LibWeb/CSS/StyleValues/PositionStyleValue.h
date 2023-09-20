/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/CSS/Enums.h>
#include <LibWeb/CSS/PercentageOr.h>
#include <LibWeb/CSS/StyleValue.h>

namespace Web::CSS {

class PositionStyleValue final : public StyleValueWithDefaultOperators<PositionStyleValue> {
public:
    static ValueComparingNonnullRefPtr<PositionStyleValue> create(ValueComparingNonnullRefPtr<StyleValue> edge_x, ValueComparingNonnullRefPtr<StyleValue> edge_y)
    {
        return adopt_ref(*new (nothrow) PositionStyleValue(move(edge_x), move(edge_y)));
    }
    virtual ~PositionStyleValue() override = default;

    ValueComparingNonnullRefPtr<StyleValue> edge_x() const { return m_properties.edge_x; }
    ValueComparingNonnullRefPtr<StyleValue> edge_y() const { return m_properties.edge_y; }

    virtual String to_string() const override;

    bool properties_equal(PositionStyleValue const& other) const { return m_properties == other.m_properties; }

private:
    PositionStyleValue(ValueComparingNonnullRefPtr<StyleValue> edge_x, ValueComparingNonnullRefPtr<StyleValue> edge_y)
        : StyleValueWithDefaultOperators(Type::Position)
        , m_properties { .edge_x = edge_x, .edge_y = edge_y }
    {
    }

    struct Properties {
        ValueComparingNonnullRefPtr<StyleValue> edge_x;
        ValueComparingNonnullRefPtr<StyleValue> edge_y;
        bool operator==(Properties const&) const = default;
    } m_properties;
};

}
