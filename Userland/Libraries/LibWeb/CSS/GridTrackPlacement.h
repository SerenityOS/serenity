/*
 * Copyright (c) 2022, Martin Falisse <mfalisse@outlook.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>

namespace Web::CSS {

class GridTrackPlacement {
public:
    enum class Type {
        Span,
        Position,
        Auto
    };

    GridTrackPlacement(DeprecatedString line_name, int span_count_or_position, bool has_span = false);
    GridTrackPlacement(int span_count_or_position, bool has_span = false);
    GridTrackPlacement(DeprecatedString line_name, bool has_span = false);
    GridTrackPlacement();

    static GridTrackPlacement make_auto() { return GridTrackPlacement(); };

    bool is_span() const { return m_type == Type::Span; }
    bool is_position() const { return m_type == Type::Position; }
    bool is_auto() const { return m_type == Type::Auto; }
    bool is_auto_positioned() const { return m_type == Type::Auto || (m_type == Type::Span && !has_line_name()); }

    bool has_line_name() const { return !m_line_name.is_empty(); }

    int raw_value() const { return m_span_count_or_position; }
    Type type() const { return m_type; }
    DeprecatedString line_name() const { return m_line_name; }

    DeprecatedString to_deprecated_string() const;
    bool operator==(GridTrackPlacement const& other) const
    {
        return m_type == other.type() && m_span_count_or_position == other.raw_value();
    }

private:
    Type m_type;
    int m_span_count_or_position { 0 };
    DeprecatedString m_line_name;
};

}
