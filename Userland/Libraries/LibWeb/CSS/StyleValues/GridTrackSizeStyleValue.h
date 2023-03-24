/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/CSS/GridTrackSize.h>
#include <LibWeb/CSS/StyleValue.h>

namespace Web::CSS {

class GridTrackSizeStyleValue final : public StyleValueWithDefaultOperators<GridTrackSizeStyleValue> {
public:
    static ValueComparingNonnullRefPtr<GridTrackSizeStyleValue> create(CSS::GridTrackSizeList grid_track_size_list);
    virtual ~GridTrackSizeStyleValue() override = default;

    static ValueComparingNonnullRefPtr<GridTrackSizeStyleValue> make_auto();

    CSS::GridTrackSizeList grid_track_size_list() const { return m_grid_track_size_list; }

    virtual ErrorOr<String> to_string() const override;

    bool properties_equal(GridTrackSizeStyleValue const& other) const { return m_grid_track_size_list == other.m_grid_track_size_list; }

private:
    explicit GridTrackSizeStyleValue(CSS::GridTrackSizeList grid_track_size_list)
        : StyleValueWithDefaultOperators(Type::GridTrackSizeList)
        , m_grid_track_size_list(grid_track_size_list)
    {
    }

    CSS::GridTrackSizeList m_grid_track_size_list;
};

}
