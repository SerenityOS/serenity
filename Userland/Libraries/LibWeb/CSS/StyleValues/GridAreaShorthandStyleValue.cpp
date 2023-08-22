/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GridAreaShorthandStyleValue.h"
#include <LibWeb/CSS/StyleValues/GridTrackPlacementStyleValue.h>

namespace Web::CSS {

ValueComparingNonnullRefPtr<GridAreaShorthandStyleValue> GridAreaShorthandStyleValue::create(
    ValueComparingNonnullRefPtr<GridTrackPlacementStyleValue const> row_start,
    ValueComparingNonnullRefPtr<GridTrackPlacementStyleValue const> column_start,
    ValueComparingNonnullRefPtr<GridTrackPlacementStyleValue const> row_end,
    ValueComparingNonnullRefPtr<GridTrackPlacementStyleValue const> column_end)
{
    return adopt_ref(*new (nothrow) GridAreaShorthandStyleValue(row_start, column_start, row_end, column_end));
}

ValueComparingNonnullRefPtr<GridAreaShorthandStyleValue> GridAreaShorthandStyleValue::create(GridTrackPlacement row_start, GridTrackPlacement column_start, GridTrackPlacement row_end, GridTrackPlacement column_end)
{
    return adopt_ref(*new (nothrow) GridAreaShorthandStyleValue(
        GridTrackPlacementStyleValue::create(row_start),
        GridTrackPlacementStyleValue::create(column_start),
        GridTrackPlacementStyleValue::create(row_end),
        GridTrackPlacementStyleValue::create(column_end)));
}

String GridAreaShorthandStyleValue::to_string() const
{
    StringBuilder builder;
    if (!m_properties.row_start->as_grid_track_placement().grid_track_placement().is_auto())
        builder.appendff("{}", m_properties.row_start->as_grid_track_placement().grid_track_placement().to_string());
    if (!m_properties.column_start->as_grid_track_placement().grid_track_placement().is_auto())
        builder.appendff(" / {}", m_properties.column_start->as_grid_track_placement().grid_track_placement().to_string());
    if (!m_properties.row_end->as_grid_track_placement().grid_track_placement().is_auto())
        builder.appendff(" / {}", m_properties.row_end->as_grid_track_placement().grid_track_placement().to_string());
    if (!m_properties.column_end->as_grid_track_placement().grid_track_placement().is_auto())
        builder.appendff(" / {}", m_properties.column_end->as_grid_track_placement().grid_track_placement().to_string());
    return MUST(builder.to_string());
}

}
