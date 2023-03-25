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
#include <LibWeb/CSS/Percentage.h>
#include <LibWeb/CSS/StyleValue.h>

namespace Web::CSS {

class PositionStyleValue final : public StyleValueWithDefaultOperators<PositionStyleValue> {
public:
    static ValueComparingNonnullRefPtr<PositionStyleValue> create(PositionEdge edge_x, LengthPercentage const& offset_x, PositionEdge edge_y, LengthPercentage const& offset_y)
    {
        return adopt_ref(*new PositionStyleValue(edge_x, offset_x, edge_y, offset_y));
    }
    virtual ~PositionStyleValue() override = default;

    PositionEdge edge_x() const { return m_properties.edge_x; }
    LengthPercentage const& offset_x() const { return m_properties.offset_x; }
    PositionEdge edge_y() const { return m_properties.edge_y; }
    LengthPercentage const& offset_y() const { return m_properties.offset_y; }

    virtual ErrorOr<String> to_string() const override;

    bool properties_equal(PositionStyleValue const& other) const { return m_properties == other.m_properties; }

private:
    PositionStyleValue(PositionEdge edge_x, LengthPercentage const& offset_x, PositionEdge edge_y, LengthPercentage const& offset_y)
        : StyleValueWithDefaultOperators(Type::Position)
        , m_properties { .edge_x = edge_x, .offset_x = offset_x, .edge_y = edge_y, .offset_y = offset_y }
    {
    }

    struct Properties {
        PositionEdge edge_x;
        LengthPercentage offset_x;
        PositionEdge edge_y;
        LengthPercentage offset_y;
        bool operator==(Properties const&) const = default;
    } m_properties;
};

}
