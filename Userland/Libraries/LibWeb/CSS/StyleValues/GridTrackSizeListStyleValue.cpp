/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GridTrackSizeListStyleValue.h"

namespace Web::CSS {

ErrorOr<String> GridTrackSizeListStyleValue::to_string() const
{
    return m_grid_track_size_list.to_string();
}

ErrorOr<ValueComparingNonnullRefPtr<GridTrackSizeListStyleValue>> GridTrackSizeListStyleValue::create(CSS::GridTrackSizeList grid_track_size_list)
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) GridTrackSizeListStyleValue(grid_track_size_list));
}

ErrorOr<ValueComparingNonnullRefPtr<GridTrackSizeListStyleValue>> GridTrackSizeListStyleValue::make_auto()
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) GridTrackSizeListStyleValue(CSS::GridTrackSizeList()));
}

}
