/*
 * Copyright (c) 2022, Martin Falisse <mfalisse@outlook.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

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
        // TODO: Repeat
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

}
