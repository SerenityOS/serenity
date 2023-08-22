/*
 * Copyright (c) 2022, Martin Falisse <mfalisse@outlook.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibWeb/CSS/PercentageOr.h>
#include <LibWeb/Layout/AvailableSpace.h>

namespace Web::CSS {

class GridSize {
public:
    enum class Type {
        LengthPercentage,
        FlexibleLength,
        MaxContent,
        MinContent,
    };

    GridSize(LengthPercentage);
    GridSize(double);
    GridSize(Type);
    GridSize();
    ~GridSize();

    static GridSize make_auto();

    Type type() const { return m_type; }

    bool is_auto(Layout::AvailableSize const&) const;
    bool is_fixed(Layout::AvailableSize const&) const;
    bool is_flexible_length() const { return m_type == Type::FlexibleLength; }
    bool is_max_content() const { return m_type == Type::MaxContent; }
    bool is_min_content() const { return m_type == Type::MinContent; }

    LengthPercentage length_percentage() const { return m_length_percentage; }
    double flex_factor() const { return m_flex_factor; }

    // https://www.w3.org/TR/css-grid-2/#layout-algorithm
    // An intrinsic sizing function (min-content, max-content, auto, fit-content()).
    // FIXME: Add missing properties once implemented.
    bool is_intrinsic(Layout::AvailableSize const&) const;

    bool is_definite() const
    {
        return type() == Type::LengthPercentage && !m_length_percentage.is_auto();
    }

    Size css_size() const;

    String to_string() const;
    bool operator==(GridSize const& other) const
    {
        return m_type == other.type()
            && m_length_percentage == other.length_percentage()
            && m_flex_factor == other.flex_factor();
    }

private:
    Type m_type;
    LengthPercentage m_length_percentage;
    double m_flex_factor { 0 };
};

class GridMinMax {
public:
    GridMinMax(CSS::GridSize min_grid_size, CSS::GridSize max_grid_size);
    GridMinMax() = default;

    GridSize min_grid_size() const& { return m_min_grid_size; }
    GridSize max_grid_size() const& { return m_max_grid_size; }

    String to_string() const;
    bool operator==(GridMinMax const& other) const
    {
        return m_min_grid_size == other.min_grid_size()
            && m_max_grid_size == other.max_grid_size();
    }

private:
    GridSize m_min_grid_size;
    GridSize m_max_grid_size;
};

class GridTrackSizeList {
public:
    GridTrackSizeList(Vector<CSS::ExplicitGridTrack> track_list, Vector<Vector<String>> line_names);
    GridTrackSizeList();

    static GridTrackSizeList make_none();

    Vector<CSS::ExplicitGridTrack> track_list() const { return m_track_list; }
    Vector<Vector<String>> line_names() const { return m_line_names; }

    String to_string() const;
    bool operator==(GridTrackSizeList const& other) const
    {
        return m_line_names == other.line_names() && m_track_list == other.track_list();
    }

private:
    Vector<CSS::ExplicitGridTrack> m_track_list;
    Vector<Vector<String>> m_line_names;
};

class GridRepeat {
public:
    enum class Type {
        AutoFit,
        AutoFill,
        Default,
    };
    GridRepeat(GridTrackSizeList, int repeat_count);
    GridRepeat(GridTrackSizeList, Type);
    GridRepeat();

    bool is_auto_fill() const { return m_type == Type::AutoFill; }
    bool is_auto_fit() const { return m_type == Type::AutoFit; }
    bool is_default() const { return m_type == Type::Default; }
    int repeat_count() const
    {
        VERIFY(is_default());
        return m_repeat_count;
    }
    GridTrackSizeList grid_track_size_list() const& { return m_grid_track_size_list; }
    Type type() const& { return m_type; }

    String to_string() const;
    bool operator==(GridRepeat const& other) const
    {
        if (m_type != other.type())
            return false;
        if (m_type == Type::Default && m_repeat_count != other.repeat_count())
            return false;
        return m_grid_track_size_list == other.grid_track_size_list();
    }

private:
    Type m_type;
    GridTrackSizeList m_grid_track_size_list;
    int m_repeat_count { 0 };
};

class ExplicitGridTrack {
public:
    enum class Type {
        MinMax,
        Repeat,
        Default,
    };
    ExplicitGridTrack(CSS::GridRepeat);
    ExplicitGridTrack(CSS::GridMinMax);
    ExplicitGridTrack(CSS::GridSize);

    bool is_repeat() const { return m_type == Type::Repeat; }
    GridRepeat repeat() const
    {
        VERIFY(is_repeat());
        return m_grid_repeat;
    }

    bool is_minmax() const { return m_type == Type::MinMax; }
    GridMinMax minmax() const
    {
        VERIFY(is_minmax());
        return m_grid_minmax;
    }

    bool is_default() const { return m_type == Type::Default; }
    GridSize grid_size() const
    {
        VERIFY(is_default());
        return m_grid_size;
    }

    Type type() const { return m_type; }

    String to_string() const;
    bool operator==(ExplicitGridTrack const& other) const
    {
        if (is_repeat() && other.is_repeat())
            return m_grid_repeat == other.repeat();
        if (is_minmax() && other.is_minmax())
            return m_grid_minmax == other.minmax();
        if (is_default() && other.is_default())
            return m_grid_size == other.grid_size();
        return false;
    }

private:
    Type m_type;
    GridRepeat m_grid_repeat;
    GridMinMax m_grid_minmax;
    GridSize m_grid_size;
};

}
