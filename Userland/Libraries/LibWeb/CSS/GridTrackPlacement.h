/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 * Copyright (c) 2022, Martin Falisse <mfalisse@outlook.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>

namespace Web::CSS {

class GridTrackPlacement {
public:
    static GridTrackPlacement make_auto()
    {
        return GridTrackPlacement();
    }

    static GridTrackPlacement make_area(String name)
    {
        return GridTrackPlacement(Area { .name = name });
    }

    static GridTrackPlacement make_line(int value, Optional<String> name)
    {
        return GridTrackPlacement(Line { .value = value, .name = name });
    }

    static GridTrackPlacement make_span(int value)
    {
        return GridTrackPlacement(Span { .value = value });
    }

    bool is_auto() const { return m_value.has<Auto>(); }
    bool is_area() const { return m_value.has<Area>(); }
    bool is_line() const { return m_value.has<Line>(); }
    bool is_span() const { return m_value.has<Span>(); }

    bool is_auto_positioned() const { return is_auto() || is_span(); }
    bool is_positioned() const { return !is_auto_positioned(); }

    bool has_line_name() const
    {
        return is_line() && m_value.get<Line>().name.has_value();
    }

    String area_name() const { return m_value.get<Area>().name; }
    String line_name() const { return m_value.get<Line>().name.value(); }
    int line_number() const { return m_value.get<Line>().value; }
    int span() const { return m_value.get<Span>().value; }

    String to_string() const;

    bool operator==(GridTrackPlacement const& other) const = default;

private:
    struct Auto {
        bool operator==(Auto const&) const = default;
    };

    struct Area {
        String name;
        bool operator==(Area const& other) const = default;
    };

    struct Line {
        int value;
        Optional<String> name;
        bool operator==(Line const& other) const = default;
    };

    struct Span {
        int value;
        bool operator==(Span const& other) const = default;
    };

    GridTrackPlacement()
        : m_value(Auto {}) {};
    GridTrackPlacement(Area value)
        : m_value(value) {};
    GridTrackPlacement(Line value)
        : m_value(value) {};
    GridTrackPlacement(Span value)
        : m_value(value) {};

    Variant<Auto, Area, Line, Span> m_value;
};

}
