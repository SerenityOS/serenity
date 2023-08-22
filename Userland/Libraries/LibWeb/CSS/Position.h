/*
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/CSS/PercentageOr.h>
#include <LibWeb/PixelUnits.h>

namespace Web::CSS {

// FIXME: Named PositionValue to avoid conflicts with enums, but this represents a <position>
struct PositionValue {
    enum class HorizontalPreset {
        Left,
        Center,
        Right
    };

    enum class VerticalPreset {
        Top,
        Center,
        Bottom
    };

    enum class HorizontalEdge {
        Left,
        Right
    };

    enum class VerticalEdge {
        Top,
        Bottom
    };

    static PositionValue center()
    {
        return PositionValue { HorizontalPreset::Center, VerticalPreset::Center };
    }

    Variant<HorizontalPreset, LengthPercentage> horizontal_position { HorizontalPreset::Left };
    Variant<VerticalPreset, LengthPercentage> vertical_position { VerticalPreset::Top };
    HorizontalEdge x_relative_to { HorizontalEdge::Left };
    VerticalEdge y_relative_to { VerticalEdge::Top };

    CSSPixelPoint resolved(Layout::Node const& node, CSSPixelRect const& rect) const;
    void serialize(StringBuilder&) const;
    bool operator==(PositionValue const&) const = default;
};

}
