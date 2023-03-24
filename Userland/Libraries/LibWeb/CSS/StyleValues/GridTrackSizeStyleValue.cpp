/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GridTrackSizeStyleValue.h"

namespace Web::CSS {

ErrorOr<String> GridTrackSizeStyleValue::to_string() const
{
    return m_grid_track_size_list.to_string();
}

ValueComparingNonnullRefPtr<GridTrackSizeStyleValue> GridTrackSizeStyleValue::create(CSS::GridTrackSizeList grid_track_size_list)
{
    return adopt_ref(*new GridTrackSizeStyleValue(grid_track_size_list));
}

ValueComparingNonnullRefPtr<GridTrackSizeStyleValue> GridTrackSizeStyleValue::make_auto()
{
    return adopt_ref(*new GridTrackSizeStyleValue(CSS::GridTrackSizeList()));
}

}
