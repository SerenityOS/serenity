/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 * Copyright (c) 2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/QuickSort.h>
#include <LibGfx/Color.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Path.h>

#if defined(AK_COMPILER_GCC)
#    pragma GCC optimize("O3")
#endif

namespace Gfx {

template<typename T, typename TColorOrFunction>
ALWAYS_INLINE void Painter::draw_scanline_for_fill_path(int y, T x_start, T x_end, TColorOrFunction color)
{
    // Fill path should scale the scanlines before calling this.
    VERIFY(scale() == 1);

    constexpr bool is_floating_point = IsSameIgnoringCV<T, float>;
    constexpr bool has_constant_color = IsSameIgnoringCV<TColorOrFunction, Color>;

    int x1 = 0;
    int x2 = 0;
    u8 left_subpixel_alpha = 0;
    u8 right_subpixel_alpha = 0;
    if constexpr (is_floating_point) {
        x1 = ceilf(x_start);
        x2 = floorf(x_end);
        left_subpixel_alpha = (x1 - x_start) * 255;
        right_subpixel_alpha = (x_end - x2) * 255;
        x1 -= left_subpixel_alpha > 0;
        x2 += right_subpixel_alpha > 0;
    } else {
        x1 = x_start;
        x2 = x_end;
    }

    IntRect scanline(x1, y, x2 - x1, 1);
    scanline = scanline.translated(translation());
    auto clipped = scanline.intersected(clip_rect());
    if (clipped.is_empty())
        return;

    auto get_color = [&](int offset) {
        if constexpr (has_constant_color) {
            return color;
        } else {
            return color(offset);
        }
    };

    if constexpr (is_floating_point) {
        // Paint left and right subpixels (then remove them from the scanline).
        auto get_color_with_alpha = [&](int offset, u8 alpha) {
            auto color_at_offset = get_color(offset);
            u8 color_alpha = (alpha * color_at_offset.alpha()) / 255;
            return color_at_offset.with_alpha(color_alpha);
        };
        bool paint_left_subpixel = clipped.left() == scanline.left() && left_subpixel_alpha;
        bool paint_right_subpixel = clipped.right() == scanline.right() && right_subpixel_alpha;
        if (paint_left_subpixel)
            set_physical_pixel(clipped.top_left(), get_color_with_alpha(0, left_subpixel_alpha), true);
        if (paint_right_subpixel)
            set_physical_pixel(clipped.top_right(), get_color_with_alpha(scanline.width(), right_subpixel_alpha), true);
        clipped.shrink(0, paint_right_subpixel, 0, paint_left_subpixel);
        if (clipped.is_empty())
            return;
    }

    if constexpr (has_constant_color) {
        if (color.alpha() == 255) {
            // Speedy path: Constant color and no alpha blending.
            fast_u32_fill(m_target->scanline(clipped.y()) + clipped.x(), color.value(), clipped.width());
            return;
        }
    }

    for (int x = clipped.x(); x <= clipped.right(); x++) {
        set_physical_pixel({ x, clipped.y() }, get_color(x - scanline.x()), true);
    }
}

[[maybe_unused]] inline void approximately_place_on_int_grid(FloatPoint ffrom, FloatPoint fto, IntPoint& from, IntPoint& to, Optional<IntPoint> previous_to)
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

template<Painter::FillPathMode fill_path_mode, typename ColorOrFunction>
void Painter::fill_path_impl(Path const& path, ColorOrFunction color, Gfx::Painter::WindingRule winding_rule, Optional<FloatPoint> offset)
{
    using GridCoordinateType = Conditional<fill_path_mode == FillPathMode::PlaceOnIntGrid, int, float>;
    using PointType = Point<GridCoordinateType>;

    auto draw_scanline = [&](int y, GridCoordinateType x1, GridCoordinateType x2) {
        const auto draw_offset = offset.value_or({ 0, 0 });
        // Note: .to_floored() is used here to be consistent with enclosing_int_rect()
        const auto draw_origin = (path.bounding_box().top_left() + draw_offset).to_floored<int>();
        // FIMXE: Offset is added here to handle floating point translations in the AA painter,
        // really this should be done there but this function is a bit too specialised.
        y = floorf(y + draw_offset.y());
        x1 += draw_offset.x();
        x2 += draw_offset.x();
        if (x1 > x2)
            swap(x1, x2);
        if constexpr (IsSameIgnoringCV<ColorOrFunction, Color>) {
            draw_scanline_for_fill_path(y, x1, x2, color);
        } else {
            draw_scanline_for_fill_path(y, x1, x2, [&](int offset) {
                return color(IntPoint(x1 + offset, y) - draw_origin);
            });
        }
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
                    draw_text(Gfx::Rect<GridCoordinateType>(active_list.last().x - 20, scanline, 20, 10), DeprecatedString::number((int)scanline));
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
                        draw_scanline(floorf(scanline), from.x(), to.x());
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
                draw_scanline(floorf(scanline), point.x(), point.x());

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
}

void Painter::fill_path(Path const& path, Color color, WindingRule winding_rule)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.
    fill_path_impl<FillPathMode::PlaceOnIntGrid>(path, color, winding_rule);
}

void Painter::fill_path(Path const& path, PaintStyle const& paint_style, Painter::WindingRule rule)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.
    paint_style.paint(enclosing_int_rect(path.bounding_box()), [&](PaintStyle::SamplerFunction sampler) {
        fill_path_impl<FillPathMode::PlaceOnIntGrid>(path, move(sampler), rule);
    });
}

void Painter::antialiased_fill_path(Path const& path, Color color, WindingRule rule, FloatPoint translation)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.
    fill_path_impl<FillPathMode::AllowFloatingPoints>(path, color, rule, translation);
}

void Painter::antialiased_fill_path(Path const& path, PaintStyle const& paint_style, WindingRule rule, FloatPoint translation)
{
    VERIFY(scale() == 1); // FIXME: Add scaling support.
    paint_style.paint(enclosing_int_rect(path.bounding_box()), [&](PaintStyle::SamplerFunction sampler) {
        fill_path_impl<FillPathMode::AllowFloatingPoints>(path, move(sampler), rule, translation);
    });
}

}
