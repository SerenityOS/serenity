/*
 * Copyright (c) 2022, Martin Falisse <mfalisse@outlook.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GridTrackSize.h"
#include <AK/DeprecatedString.h>
#include <LibWeb/CSS/Length.h>
#include <LibWeb/CSS/Percentage.h>
#include <LibWeb/CSS/StyleValue.h>

namespace Web::CSS {

GridSize::GridSize(Length length)
    : m_type(Type::Length)
    , m_length(length)
{
}

GridSize::GridSize(Percentage percentage)
    : m_type(Type::Percentage)
    , m_length { Length::make_px(0) }
    , m_percentage(percentage)
{
}

GridSize::GridSize(float flexible_length)
    : m_type(Type::FlexibleLength)
    , m_length { Length::make_px(0) }
    , m_flexible_length(flexible_length)
{
}

GridSize::GridSize()
    : m_length { Length::make_auto() }
{
}

GridSize::~GridSize() = default;

GridSize GridSize::make_auto()
{
    return GridSize(CSS::Length::make_auto());
}

DeprecatedString GridSize::to_deprecated_string() const
{
    switch (m_type) {
    case Type::Length:
        return m_length.to_deprecated_string();
    case Type::Percentage:
        return m_percentage.to_deprecated_string();
    case Type::FlexibleLength:
        return DeprecatedString::formatted("{}fr", m_flexible_length);
    }
    VERIFY_NOT_REACHED();
}

Length GridSize::length() const
{
    return m_length;
}

GridMinMax::GridMinMax(GridSize min_grid_size, GridSize max_grid_size)
    : m_min_grid_size(min_grid_size)
    , m_max_grid_size(max_grid_size)
{
}

DeprecatedString GridMinMax::to_deprecated_string() const
{
    StringBuilder builder;
    builder.append("minmax("sv);
    builder.appendff("{}", m_min_grid_size.to_deprecated_string());
    builder.append(", "sv);
    builder.appendff("{}", m_max_grid_size.to_deprecated_string());
    builder.append(")"sv);
    return builder.to_deprecated_string();
}

GridRepeat::GridRepeat(GridTrackSizeList grid_track_size_list, int repeat_count)
    : m_type(Type::Default)
    , m_grid_track_size_list(grid_track_size_list)
    , m_repeat_count(repeat_count)
{
}

GridRepeat::GridRepeat(GridTrackSizeList grid_track_size_list, Type type)
    : m_type(type)
    , m_grid_track_size_list(grid_track_size_list)
{
}

GridRepeat::GridRepeat()
{
}

DeprecatedString GridRepeat::to_deprecated_string() const
{
    StringBuilder builder;
    builder.append("repeat("sv);
    switch (m_type) {
    case Type::AutoFit:
        builder.append("auto-fill"sv);
        break;
    case Type::AutoFill:
        builder.append("auto-fit"sv);
        break;
    case Type::Default:
        builder.appendff("{}", m_repeat_count);
        break;
    default:
        VERIFY_NOT_REACHED();
    }
    builder.append(", "sv);
    builder.appendff("{}", m_grid_track_size_list.to_deprecated_string());
    builder.append(")"sv);
    return builder.to_deprecated_string();
}

ExplicitGridTrack::ExplicitGridTrack(CSS::GridMinMax grid_minmax)
    : m_type(Type::MinMax)
    , m_grid_minmax(grid_minmax)
{
}

ExplicitGridTrack::ExplicitGridTrack(CSS::GridRepeat grid_repeat)
    : m_type(Type::Repeat)
    , m_grid_repeat(grid_repeat)
{
}

ExplicitGridTrack::ExplicitGridTrack(CSS::GridSize grid_size)
    : m_type(Type::Default)
    , m_grid_size(grid_size)
{
}

DeprecatedString ExplicitGridTrack::to_deprecated_string() const
{
    switch (m_type) {
    case Type::MinMax:
        return m_grid_minmax.to_deprecated_string();
    case Type::Repeat:
        return m_grid_repeat.to_deprecated_string();
    case Type::Default:
        return m_grid_size.to_deprecated_string();
    default:
        VERIFY_NOT_REACHED();
    }
}

GridTrackSizeList::GridTrackSizeList(Vector<CSS::ExplicitGridTrack> track_list, Vector<Vector<DeprecatedString>> line_names)
    : m_track_list(track_list)
    , m_line_names(line_names)
{
}

GridTrackSizeList::GridTrackSizeList()
    : m_track_list({})
    , m_line_names({})
{
}

GridTrackSizeList GridTrackSizeList::make_auto()
{
    return GridTrackSizeList();
}

DeprecatedString GridTrackSizeList::to_deprecated_string() const
{
    StringBuilder builder;
    auto print_line_names = [&](size_t index) -> void {
        builder.append("["sv);
        for (size_t y = 0; y < m_line_names[index].size(); ++y) {
            builder.append(m_line_names[index][y]);
            if (y != m_line_names[index].size() - 1)
                builder.append(" "sv);
        }
        builder.append("]"sv);
    };

    for (size_t i = 0; i < m_track_list.size(); ++i) {
        if (m_line_names[i].size() > 0) {
            print_line_names(i);
            builder.append(" "sv);
        }
        builder.append(m_track_list[i].to_deprecated_string());
        if (i < m_track_list.size() - 1)
            builder.append(" "sv);
    }
    if (m_line_names[m_track_list.size()].size() > 0) {
        builder.append(" "sv);
        print_line_names(m_track_list.size());
    }
    return builder.to_deprecated_string();
}

}
