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

class GridTrackPlacementShorthandStyleValue final : public StyleValueWithDefaultOperators<GridTrackPlacementShorthandStyleValue> {
public:
    static ValueComparingNonnullRefPtr<GridTrackPlacementShorthandStyleValue> create(ValueComparingNonnullRefPtr<GridTrackPlacementStyleValue const> start, ValueComparingNonnullRefPtr<GridTrackPlacementStyleValue const> end);
    static ValueComparingNonnullRefPtr<GridTrackPlacementShorthandStyleValue> create(GridTrackPlacement start);
    virtual ~GridTrackPlacementShorthandStyleValue() override = default;

    auto start() const { return m_properties.start; }
    auto end() const { return m_properties.end; }

    virtual String to_string() const override;

    bool properties_equal(GridTrackPlacementShorthandStyleValue const& other) const { return m_properties == other.m_properties; }

private:
    GridTrackPlacementShorthandStyleValue(ValueComparingNonnullRefPtr<GridTrackPlacementStyleValue const> start, ValueComparingNonnullRefPtr<GridTrackPlacementStyleValue const> end)
        : StyleValueWithDefaultOperators(Type::GridTrackPlacementShorthand)
        , m_properties { .start = move(start), .end = move(end) }
    {
    }

    struct Properties {
        ValueComparingNonnullRefPtr<GridTrackPlacementStyleValue const> start;
        ValueComparingNonnullRefPtr<GridTrackPlacementStyleValue const> end;
        bool operator==(Properties const&) const = default;
    } m_properties;
};

}
