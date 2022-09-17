/*
 * Copyright (c) 2022, Martin Falisse <mfalisse@outlook.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>

namespace Web::CSS {

class GridTrackPlacement {
public:
    enum class Type {
        Span,
        Position,
        Auto
    };

    GridTrackPlacement(int, bool = false);
    GridTrackPlacement();

    static GridTrackPlacement make_auto() { return GridTrackPlacement(); };

    bool is_span() const { return m_type == Type::Span; }
    bool is_position() const { return m_type == Type::Position; }
    bool is_auto() const { return m_type == Type::Auto; }
    bool is_auto_positioned() const { return m_type == Type::Auto || m_type == Type::Span; }

    int raw_value() const { return m_value; }
    Type type() const { return m_type; }

    String to_string() const;
    bool operator==(GridTrackPlacement const& other) const
    {
        return m_type == other.type() && m_value == other.raw_value();
    }

private:
    Type m_type;
    int m_value { 0 };
};

}
