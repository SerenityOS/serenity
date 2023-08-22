/*
 * Copyright (c) 2022, Martin Falisse <mfalisse@outlook.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GridTrackSize.h"
#include <AK/String.h>
#include <LibWeb/CSS/Size.h>

namespace Web::CSS {

GridSize::GridSize(LengthPercentage length_percentage)
    : m_type(Type::LengthPercentage)
    , m_length_percentage(length_percentage) {};

GridSize::GridSize(double flex_factor)
    : m_type(Type::FlexibleLength)
    , m_length_percentage { Length::make_px(0) }
    , m_flex_factor(flex_factor)
{
}

GridSize::GridSize(Type type)
    : m_length_percentage { Length::make_auto() }
{
    VERIFY(type == Type::MinContent || type == Type::MaxContent);
    m_type = type;
}

GridSize::GridSize()
    : m_type(Type::LengthPercentage)
    , m_length_percentage { Length::make_auto() }
{
}

GridSize::~GridSize() = default;

bool GridSize::is_auto(Layout::AvailableSize const& available_size) const
{
    if (m_type == Type::LengthPercentage) {
        if (m_length_percentage.contains_percentage())
            return !available_size.is_definite();
        return m_length_percentage.is_auto();
    }

    return false;
}

bool GridSize::is_fixed(Layout::AvailableSize const& available_size) const
{
    if (m_type == Type::LengthPercentage) {
        if (m_length_percentage.contains_percentage())
            return available_size.is_definite();
        return !m_length_percentage.is_auto();
    }

    return false;
}

bool GridSize::is_intrinsic(Layout::AvailableSize const& available_size) const
{
    return is_auto(available_size) || is_max_content() || is_min_content();
}

GridSize GridSize::make_auto()
{
    return GridSize(CSS::Length::make_auto());
}

Size GridSize::css_size() const
{
    VERIFY(m_type == Type::LengthPercentage);
    if (m_length_percentage.is_auto())
        return CSS::Size::make_auto();
    if (m_length_percentage.is_length())
        return CSS::Size::make_length(m_length_percentage.length());
    if (m_length_percentage.is_calculated()) {
        return CSS::Size::make_calculated(m_length_percentage.calculated());
    }
    return CSS::Size::make_percentage(m_length_percentage.percentage());
}

String GridSize::to_string() const
{
    switch (m_type) {
    case Type::LengthPercentage:
        return m_length_percentage.to_string();
    case Type::FlexibleLength:
        return MUST(String::formatted("{}fr", m_flex_factor));
    case Type::MaxContent:
        return "max-content"_string;
    case Type::MinContent:
        return "min-content"_string;
    }
    VERIFY_NOT_REACHED();
}

GridMinMax::GridMinMax(GridSize min_grid_size, GridSize max_grid_size)
    : m_min_grid_size(min_grid_size)
    , m_max_grid_size(max_grid_size)
{
}

String GridMinMax::to_string() const
{
    StringBuilder builder;
    builder.append("minmax("sv);
    builder.appendff("{}", m_min_grid_size.to_string());
    builder.append(", "sv);
    builder.appendff("{}", m_max_grid_size.to_string());
    builder.append(")"sv);
    return MUST(builder.to_string());
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

String GridRepeat::to_string() const
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
    builder.appendff("{}", m_grid_track_size_list.to_string());
    builder.append(")"sv);
    return MUST(builder.to_string());
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

String ExplicitGridTrack::to_string() const
{
    switch (m_type) {
    case Type::MinMax:
        return m_grid_minmax.to_string();
    case Type::Repeat:
        return m_grid_repeat.to_string();
    case Type::Default:
        return m_grid_size.to_string();
    default:
        VERIFY_NOT_REACHED();
    }
}

GridTrackSizeList::GridTrackSizeList(Vector<CSS::ExplicitGridTrack> track_list, Vector<Vector<String>> line_names)
    : m_track_list(track_list)
    , m_line_names(line_names)
{
}

GridTrackSizeList::GridTrackSizeList()
    : m_track_list({})
    , m_line_names({})
{
}

GridTrackSizeList GridTrackSizeList::make_none()
{
    return GridTrackSizeList();
}

String GridTrackSizeList::to_string() const
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
        if (m_line_names.size() > 0 && m_line_names[i].size() > 0) {
            print_line_names(i);
            builder.append(" "sv);
        }
        builder.append(m_track_list[i].to_string());
        if (i < m_track_list.size() - 1)
            builder.append(" "sv);
    }
    if (m_line_names.size() > 0 && m_line_names[m_track_list.size()].size() > 0) {
        builder.append(" "sv);
        print_line_names(m_track_list.size());
    }
    return MUST(builder.to_string());
}

}
