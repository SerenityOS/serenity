/*
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Position.h"
#include <LibGfx/Point.h>
#include <LibGfx/Rect.h>

namespace Web::CSS {

CSSPixelPoint PositionValue::resolved(Layout::Node const& node, CSSPixelRect const& rect) const
{
    // Note: A preset + a none default x/y_relative_to is impossible in the syntax (and makes little sense)
    CSSPixels x = horizontal_position.visit(
        [&](HorizontalPreset preset) -> CSSPixels {
            return rect.width() * [&] {
                switch (preset) {
                case HorizontalPreset::Left:
                    return 0.;
                case HorizontalPreset::Center:
                    return 0.5;
                case HorizontalPreset::Right:
                    return 1.;
                default:
                    VERIFY_NOT_REACHED();
                }
            }();
        },
        [&](LengthPercentage length_percentage) -> CSSPixels {
            return length_percentage.to_px(node, rect.width());
        });
    CSSPixels y = vertical_position.visit(
        [&](VerticalPreset preset) -> CSSPixels {
            return rect.height() * [&] {
                switch (preset) {
                case VerticalPreset::Top:
                    return 0.;
                case VerticalPreset::Center:
                    return 0.5;
                case VerticalPreset::Bottom:
                    return 1.;
                default:
                    VERIFY_NOT_REACHED();
                }
            }();
        },
        [&](LengthPercentage length_percentage) -> CSSPixels {
            return length_percentage.to_px(node, rect.height());
        });
    if (x_relative_to == HorizontalEdge::Right)
        x = rect.width() - x;
    if (y_relative_to == VerticalEdge::Bottom)
        y = rect.height() - y;
    return CSSPixelPoint { rect.x() + x, rect.y() + y };
}

void PositionValue::serialize(StringBuilder& builder) const
{
    // Note: This means our serialization with simplify any with explicit edges that are just `top left`.
    bool has_relative_edges = x_relative_to == HorizontalEdge::Right || y_relative_to == VerticalEdge::Bottom;
    if (has_relative_edges)
        builder.append(x_relative_to == HorizontalEdge::Left ? "left "sv : "right "sv);
    horizontal_position.visit(
        [&](HorizontalPreset preset) {
            builder.append([&] {
                switch (preset) {
                case HorizontalPreset::Left:
                    return "left"sv;
                case HorizontalPreset::Center:
                    return "center"sv;
                case HorizontalPreset::Right:
                    return "right"sv;
                default:
                    VERIFY_NOT_REACHED();
                }
            }());
        },
        [&](LengthPercentage length_percentage) {
            builder.append(length_percentage.to_string());
        });
    builder.append(' ');
    if (has_relative_edges)
        builder.append(y_relative_to == VerticalEdge::Top ? "top "sv : "bottom "sv);
    vertical_position.visit(
        [&](VerticalPreset preset) {
            builder.append([&] {
                switch (preset) {
                case VerticalPreset::Top:
                    return "top"sv;
                case VerticalPreset::Center:
                    return "center"sv;
                case VerticalPreset::Bottom:
                    return "bottom"sv;
                default:
                    VERIFY_NOT_REACHED();
                }
            }());
        },
        [&](LengthPercentage length_percentage) {
            builder.append(length_percentage.to_string());
        });
}

}
