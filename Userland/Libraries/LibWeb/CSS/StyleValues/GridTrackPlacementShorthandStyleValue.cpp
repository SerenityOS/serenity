/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GridTrackPlacementShorthandStyleValue.h"
#include <LibWeb/CSS/StyleValues/GridTrackPlacementStyleValue.h>

namespace Web::CSS {

ValueComparingNonnullRefPtr<GridTrackPlacementShorthandStyleValue> GridTrackPlacementShorthandStyleValue::create(ValueComparingNonnullRefPtr<GridTrackPlacementStyleValue const> start, ValueComparingNonnullRefPtr<GridTrackPlacementStyleValue const> end)
{
    return adopt_ref(*new (nothrow) GridTrackPlacementShorthandStyleValue(move(start), move(end)));
}

ValueComparingNonnullRefPtr<GridTrackPlacementShorthandStyleValue> GridTrackPlacementShorthandStyleValue::create(GridTrackPlacement start)
{
    return adopt_ref(*new (nothrow) GridTrackPlacementShorthandStyleValue(
        GridTrackPlacementStyleValue::create(start),
        GridTrackPlacementStyleValue::create(GridTrackPlacement::make_auto())));
}

ErrorOr<String> GridTrackPlacementShorthandStyleValue::to_string() const
{
    if (m_properties.end->grid_track_placement().is_auto())
        return String::formatted("{}", TRY(m_properties.start->grid_track_placement().to_string()));
    return String::formatted("{} / {}", TRY(m_properties.start->grid_track_placement().to_string()), TRY(m_properties.end->grid_track_placement().to_string()));
}

}
