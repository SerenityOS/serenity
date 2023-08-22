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

class GridAreaShorthandStyleValue final : public StyleValueWithDefaultOperators<GridAreaShorthandStyleValue> {
public:
    static ValueComparingNonnullRefPtr<GridAreaShorthandStyleValue> create(
        ValueComparingNonnullRefPtr<GridTrackPlacementStyleValue const> row_start,
        ValueComparingNonnullRefPtr<GridTrackPlacementStyleValue const> column_start,
        ValueComparingNonnullRefPtr<GridTrackPlacementStyleValue const> row_end,
        ValueComparingNonnullRefPtr<GridTrackPlacementStyleValue const> column_end);
    static ValueComparingNonnullRefPtr<GridAreaShorthandStyleValue> create(GridTrackPlacement row_start, GridTrackPlacement column_start, GridTrackPlacement row_end, GridTrackPlacement column_end);
    virtual ~GridAreaShorthandStyleValue() override = default;

    auto row_start() const { return m_properties.row_start; }
    auto column_start() const { return m_properties.column_start; }
    auto row_end() const { return m_properties.row_end; }
    auto column_end() const { return m_properties.column_end; }

    virtual String to_string() const override;

    bool properties_equal(GridAreaShorthandStyleValue const& other) const { return m_properties == other.m_properties; }

private:
    GridAreaShorthandStyleValue(ValueComparingNonnullRefPtr<GridTrackPlacementStyleValue const> row_start, ValueComparingNonnullRefPtr<GridTrackPlacementStyleValue const> column_start, ValueComparingNonnullRefPtr<GridTrackPlacementStyleValue const> row_end, ValueComparingNonnullRefPtr<GridTrackPlacementStyleValue const> column_end)
        : StyleValueWithDefaultOperators(Type::GridAreaShorthand)
        , m_properties { .row_start = move(row_start), .column_start = move(column_start), .row_end = move(row_end), .column_end = move(column_end) }
    {
    }

    struct Properties {
        ValueComparingNonnullRefPtr<GridTrackPlacementStyleValue const> row_start;
        ValueComparingNonnullRefPtr<GridTrackPlacementStyleValue const> column_start;
        ValueComparingNonnullRefPtr<GridTrackPlacementStyleValue const> row_end;
        ValueComparingNonnullRefPtr<GridTrackPlacementStyleValue const> column_end;
        bool operator==(Properties const&) const = default;
    } m_properties;
};

}
