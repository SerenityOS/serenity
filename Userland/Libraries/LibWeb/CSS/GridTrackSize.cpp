/*
 * Copyright (c) 2022, Martin Falisse <mfalisse@outlook.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GridTrackSize.h"
#include <AK/String.h>
#include <LibWeb/CSS/Length.h>
#include <LibWeb/CSS/Percentage.h>
#include <LibWeb/CSS/StyleValue.h>

namespace Web::CSS {

GridTrackSize::GridTrackSize(Length length)
    : m_type(Type::Length)
    , m_length(length)
{
}

GridTrackSize::GridTrackSize(Percentage percentage)
    : m_type(Type::Percentage)
    , m_length { Length::make_px(0) }
    , m_percentage(percentage)
{
}

GridTrackSize::GridTrackSize(float flexible_length)
    : m_type(Type::FlexibleLength)
    , m_length { Length::make_px(0) }
    , m_flexible_length(flexible_length)
{
}

GridTrackSize::~GridTrackSize() = default;

GridTrackSize GridTrackSize::make_auto()
{
    return GridTrackSize(CSS::Length::make_auto());
}

String GridTrackSize::to_string() const
{
    switch (m_type) {
    case Type::Length:
        return m_length.to_string();
    case Type::Percentage:
        return m_percentage.to_string();
    case Type::FlexibleLength:
        return String::formatted("{}fr", m_flexible_length);
    }
    VERIFY_NOT_REACHED();
}

Length GridTrackSize::length() const
{
    return m_length;
}

MetaGridTrackSize::MetaGridTrackSize(GridTrackSize grid_track_size)
    : m_min_grid_track_size(grid_track_size)
    , m_max_grid_track_size(grid_track_size)
{
}

String MetaGridTrackSize::to_string() const
{
    return String::formatted("{}", m_min_grid_track_size.to_string());
}

ExplicitTrackSizing::ExplicitTrackSizing()
{
}

ExplicitTrackSizing::ExplicitTrackSizing(Vector<CSS::MetaGridTrackSize> meta_grid_track_sizes)
    : m_meta_grid_track_sizes(meta_grid_track_sizes)
{
}

ExplicitTrackSizing::ExplicitTrackSizing(Vector<CSS::MetaGridTrackSize> meta_grid_track_sizes, int repeat_count)
    : m_meta_grid_track_sizes(meta_grid_track_sizes)
    , m_is_repeat(true)
    , m_repeat_count(repeat_count)
{
}

String ExplicitTrackSizing::to_string() const
{
    StringBuilder builder;
    if (m_is_repeat) {
        builder.append("repeat("sv);
        builder.append(m_repeat_count);
        builder.append(", "sv);
    }
    for (int _ = 0; _ < m_repeat_count; ++_) {
        for (size_t y = 0; y < m_meta_grid_track_sizes.size(); ++y) {
            builder.append(m_meta_grid_track_sizes[y].to_string());
            if (y != m_meta_grid_track_sizes.size() - 1)
                builder.append(" "sv);
        }
    }
    if (m_is_repeat)
        builder.append(")"sv);
    return builder.to_string();
}

}
