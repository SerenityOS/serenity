/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Debug.h>
#include <AK/QuickSort.h>
#include <LibGfx/Color.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Path.h>

namespace Gfx::Detail {

[[maybe_unused]] inline static void approximately_place_on_int_grid(FloatPoint ffrom, FloatPoint fto, IntPoint& from, IntPoint& to, Optional<IntPoint> previous_to)
{
    auto diffs = fto - ffrom;
    // Truncate all first (round down).
    from = ffrom.to_type<int>();
    to = fto.to_type<int>();
    // There are 16 possible configurations, by deciding to round each
    // coord up or down (and there are four coords, from.x from.y to.x to.y)
    // we will simply choose one which most closely matches the correct slope
    // with the following heuristic:
    // - if the x diff is positive or zero (that is, a right-to-left slant), round 'from.x' up and 'to.x' down.
    // - if the x diff is negative         (that is, a left-to-right slant), round 'from.x' down and 'to.x' up.
    // Note that we do not need to touch the 'y' attribute, as that is our scanline.
    if (diffs.x() >= 0) {
        from.set_x(from.x() + 1);
    } else {
        to.set_x(to.x() + 1);
    }
    if (previous_to.has_value() && from.x() != previous_to.value().x()) // The points have to line up, since we're using these lines to fill a shape.
        from.set_x(previous_to.value().x());
}

enum class FillPathMode {
    PlaceOnIntGrid,
    AllowFloatingPoints,
};

template<FillPathMode fill_path_mode, typename Painter>
void fill_path(Painter& painter, Path const& path, Color color, Gfx::Painter::WindingRule winding_rule)
{
    using GridCoordinateType = Conditional<fill_path_mode == FillPathMode::PlaceOnIntGrid, int, float>;
    using PointType = Point<GridCoordinateType>;
    auto draw_line = [&](auto... args) {
        if constexpr (requires { painter.draw_aliased_line(args...); })
            painter.draw_aliased_line(args...);
        else
            painter.draw_line(args...);
    };

    auto const& segments = path.split_lines();

    if (segments.size() == 0)
        return;

    Vector<Path::SplitLineSegment> active_list;
    active_list.ensure_capacity(segments.size());

    // first, grab the segments for the very first scanline
    GridCoordinateType first_y = path.bounding_box().bottom_right().y() + 1;
    GridCoordinateType last_y = path.bounding_box().top_left().y() - 1;
    float scanline = first_y;

    size_t last_active_segment { 0 };

    for (auto& segment : segments) {
        if (segment.maximum_y != scanline)
            break;
        active_list.append(segment);
        ++last_active_segment;
    }

    auto is_inside_shape = [winding_rule](int winding_number) {
        if (winding_rule == Gfx::Painter::WindingRule::Nonzero)
            return winding_number != 0;

        if (winding_rule == Gfx::Painter::WindingRule::EvenOdd)
            return winding_number % 2 == 0;

        VERIFY_NOT_REACHED();
    };

    auto increment_winding = [winding_rule](int& winding_number, PointType const& from, PointType const& to) {
        if (winding_rule == Gfx::Painter::WindingRule::EvenOdd) {
            ++winding_number;
            return;
        }

        if (winding_rule == Gfx::Painter::WindingRule::Nonzero) {
            if (from.dy_relative_to(to) < 0)
                ++winding_number;
            else
                --winding_number;
            return;
        }

        VERIFY_NOT_REACHED();
    };

    while (scanline >= last_y) {
        Optional<PointType> previous_to;
        if (active_list.size()) {
            // sort the active list by 'x' from right to left
            quick_sort(active_list, [](auto const& line0, auto const& line1) {
                return line1.x < line0.x;
            });
            if constexpr (fill_path_mode == FillPathMode::PlaceOnIntGrid && FILL_PATH_DEBUG) {
                if ((int)scanline % 10 == 0) {
                    painter.draw_text(Gfx::Rect<GridCoordinateType>(active_list.last().x - 20, scanline, 20, 10), String::number((int)scanline));
                }
            }

            if (active_list.size() > 1) {
                auto winding_number { winding_rule == Gfx::Painter::WindingRule::Nonzero ? 1 : 0 };
                for (size_t i = 1; i < active_list.size(); ++i) {
                    auto& previous = active_list[i - 1];
                    auto& current = active_list[i];

                    PointType from, to;
                    PointType truncated_from { previous.x, scanline };
                    PointType truncated_to { current.x, scanline };
                    if constexpr (fill_path_mode == FillPathMode::PlaceOnIntGrid) {
                        approximately_place_on_int_grid({ previous.x, scanline }, { current.x, scanline }, from, to, previous_to);
                    } else {
                        from = truncated_from;
                        to = truncated_to;
                    }

                    if (is_inside_shape(winding_number)) {
                        // The points between this segment and the previous are
                        // inside the shape

                        dbgln_if(FILL_PATH_DEBUG, "y={}: {} at {}: {} -- {}", scanline, winding_number, i, from, to);
                        draw_line(from, to, color, 1);
                    }

                    auto is_passing_through_maxima = scanline == previous.maximum_y
                        || scanline == previous.minimum_y
                        || scanline == current.maximum_y
                        || scanline == current.minimum_y;

                    auto is_passing_through_vertex = false;

                    if (is_passing_through_maxima) {
                        is_passing_through_vertex = previous.x == current.x;
                    }

                    if (!is_passing_through_vertex || previous.inverse_slope * current.inverse_slope < 0)
                        increment_winding(winding_number, truncated_from, truncated_to);

                    // update the x coord
                    active_list[i - 1].x -= active_list[i - 1].inverse_slope;
                }
                active_list.last().x -= active_list.last().inverse_slope;
            } else {
                auto point = PointType(active_list[0].x, scanline);
                draw_line(point, point, color);

                // update the x coord
                active_list.first().x -= active_list.first().inverse_slope;
            }
        }

        --scanline;
        // remove any edge that goes out of bound from the active list
        for (size_t i = 0, count = active_list.size(); i < count; ++i) {
            if (scanline <= active_list[i].minimum_y) {
                active_list.remove(i);
                --count;
                --i;
            }
        }
        for (size_t j = last_active_segment; j < segments.size(); ++j, ++last_active_segment) {
            auto& segment = segments[j];
            if (segment.maximum_y < scanline)
                break;
            if (segment.minimum_y >= scanline)
                continue;

            active_list.append(segment);
        }
    }

    if constexpr (FILL_PATH_DEBUG) {
        size_t i { 0 };
        for (auto& segment : segments) {
            draw_line(PointType(segment.from), PointType(segment.to), Color::from_hsv(i++ * 360.0 / segments.size(), 1.0, 1.0), 1);
        }
    }
}
}
