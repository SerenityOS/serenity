/*
 * Copyright (c) 2022, Martin Falisse <mfalisse@outlook.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibWeb/CSS/Length.h>
#include <LibWeb/CSS/Percentage.h>

namespace Web::CSS {

class GridTrackSize {
public:
    enum class Type {
        Length,
        Percentage,
        FlexibleLength,
        // TODO: MinMax
        // TODO: Max-Content
    };

    GridTrackSize(Length);
    GridTrackSize(Percentage);
    GridTrackSize(float);
    ~GridTrackSize();

    static GridTrackSize make_auto();

    Type type() const { return m_type; }

    bool is_length() const { return m_type == Type::Length; }
    bool is_percentage() const { return m_type == Type::Percentage; }
    bool is_flexible_length() const { return m_type == Type::FlexibleLength; }

    Length length() const;
    Percentage percentage() const { return m_percentage; }
    float flexible_length() const { return m_flexible_length; }

    // https://drafts.csswg.org/css-grid/#layout-algorithm
    // Intrinsic sizing function - min-content, max-content, auto, fit-content()
    // FIXME: Add missing properties once implemented.
    bool is_intrinsic_track_sizing() const
    {
        return (m_type == Type::Length && m_length.is_auto());
    }

    String to_string() const;
    bool operator==(GridTrackSize const& other) const
    {
        return m_type == other.type()
            && m_length == other.length()
            && m_percentage == other.percentage()
            && m_flexible_length == other.flexible_length();
    }

private:
    Type m_type;
    // Length includes a RefPtr<CalculatedStyleValue> member, but we can't include the header StyleValue.h as it includes
    // this file already. To break the cyclic dependency, we must initialize m_length in the constructor.
    Length m_length;
    Percentage m_percentage { Percentage(0) };
    float m_flexible_length { 0 };
};

class MetaGridTrackSize {
public:
    MetaGridTrackSize(CSS::GridTrackSize);

    GridTrackSize grid_track_size() const& { return m_min_grid_track_size; }
    GridTrackSize min_grid_track_size() const& { return m_min_grid_track_size; }
    GridTrackSize max_grid_track_size() const& { return m_max_grid_track_size; }

    String to_string() const;
    bool operator==(MetaGridTrackSize const& other) const
    {
        return m_min_grid_track_size == other.min_grid_track_size()
            && m_max_grid_track_size == other.max_grid_track_size();
    }

private:
    GridTrackSize m_min_grid_track_size;
    GridTrackSize m_max_grid_track_size;
};

class ExplicitTrackSizing {
public:
    ExplicitTrackSizing();
    ExplicitTrackSizing(Vector<CSS::MetaGridTrackSize>);
    ExplicitTrackSizing(Vector<CSS::MetaGridTrackSize>, int repeat_count);
    static ExplicitTrackSizing make_auto() { return ExplicitTrackSizing(); };

    bool is_repeat() const { return m_is_repeat; }
    int repeat_count() const { return m_repeat_count; }

    Vector<CSS::MetaGridTrackSize> meta_grid_track_sizes() const& { return m_meta_grid_track_sizes; }

    String to_string() const;
    bool operator==(ExplicitTrackSizing const& other) const
    {
        return m_meta_grid_track_sizes == other.meta_grid_track_sizes();
    }

private:
    Vector<CSS::MetaGridTrackSize> m_meta_grid_track_sizes;
    bool m_is_repeat { false };
    int m_repeat_count { 0 };
};

}
