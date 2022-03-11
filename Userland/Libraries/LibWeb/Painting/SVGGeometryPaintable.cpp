/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/AntiAliasingPainter.h>
#include <LibWeb/Layout/ImageBox.h>
#include <LibWeb/Painting/SVGGeometryPaintable.h>
#include <LibWeb/SVG/SVGSVGElement.h>

namespace Web::Painting {

NonnullRefPtr<SVGGeometryPaintable> SVGGeometryPaintable::create(Layout::SVGGeometryBox const& layout_box)
{
    return adopt_ref(*new SVGGeometryPaintable(layout_box));
}

SVGGeometryPaintable::SVGGeometryPaintable(Layout::SVGGeometryBox const& layout_box)
    : SVGGraphicsPaintable(layout_box)
{
}

Layout::SVGGeometryBox const& SVGGeometryPaintable::layout_box() const
{
    return static_cast<Layout::SVGGeometryBox const&>(layout_node());
}

void SVGGeometryPaintable::paint(PaintContext& context, PaintPhase phase) const
{
    if (!is_visible())
        return;

    SVGGraphicsPaintable::paint(context, phase);

    if (phase != PaintPhase::Foreground)
        return;

    auto& geometry_element = layout_box().dom_node();

    Gfx::AntiAliasingPainter painter { context.painter() };
    auto& svg_context = context.svg_context();

    auto offset = svg_context.svg_element_position();
    painter.translate(offset);

    auto const* svg_element = geometry_element.first_ancestor_of_type<SVG::SVGSVGElement>();
    auto maybe_view_box = svg_element->view_box();

    context.painter().add_clip_rect(enclosing_int_rect(absolute_rect()));

    Gfx::Path path = const_cast<SVG::SVGGeometryElement&>(geometry_element).get_path();

    if (maybe_view_box.has_value()) {
        Gfx::Path new_path;
        auto scaling = layout_box().viewbox_scaling();
        auto origin = layout_box().viewbox_origin();

        auto transform_point = [&scaling, &origin](Gfx::FloatPoint const& point) -> Gfx::FloatPoint {
            auto new_point = point;
            new_point.translate_by({ -origin.x(), -origin.y() });
            new_point.scale_by(scaling);
            return new_point;
        };

        for (auto& segment : path.segments()) {
            switch (segment.type()) {
            case Gfx::Segment::Type::Invalid:
                break;
            case Gfx::Segment::Type::MoveTo:
                new_path.move_to(transform_point(segment.point()));
                break;
            case Gfx::Segment::Type::LineTo:
                new_path.line_to(transform_point(segment.point()));
                break;
            case Gfx::Segment::Type::QuadraticBezierCurveTo: {
                auto& quadratic_bezier_segment = static_cast<Gfx::QuadraticBezierCurveSegment const&>(segment);
                new_path.quadratic_bezier_curve_to(transform_point(quadratic_bezier_segment.through()), transform_point(quadratic_bezier_segment.point()));
                break;
            }
            case Gfx::Segment::Type::CubicBezierCurveTo: {
                auto& cubic_bezier_segment = static_cast<Gfx::CubicBezierCurveSegment const&>(segment);
                new_path.cubic_bezier_curve_to(transform_point(cubic_bezier_segment.through_0()), transform_point(cubic_bezier_segment.through_1()), transform_point(cubic_bezier_segment.point()));
                break;
            }
            case Gfx::Segment::Type::EllipticalArcTo: {
                auto& elliptical_arc_segment = static_cast<Gfx::EllipticalArcSegment const&>(segment);
                new_path.elliptical_arc_to(transform_point(elliptical_arc_segment.point()), elliptical_arc_segment.radii().scaled(scaling, scaling), elliptical_arc_segment.x_axis_rotation(), elliptical_arc_segment.large_arc(), elliptical_arc_segment.sweep());
                break;
            }
            }
        }

        path = new_path;
    }

    if (auto fill_color = geometry_element.fill_color().value_or(svg_context.fill_color()); fill_color.alpha() > 0) {
        // We need to fill the path before applying the stroke, however the filled
        // path must be closed, whereas the stroke path may not necessary be closed.
        // Copy the path and close it for filling, but use the previous path for stroke
        auto closed_path = path;
        closed_path.close();

        // Fills are computed as though all paths are closed (https://svgwg.org/svg2-draft/painting.html#FillProperties)
        painter.fill_path(
            closed_path,
            fill_color,
            Gfx::Painter::WindingRule::EvenOdd);
    }

    if (auto stroke_color = geometry_element.stroke_color().value_or(svg_context.stroke_color()); stroke_color.alpha() > 0) {
        painter.stroke_path(
            path,
            stroke_color,
            geometry_element.stroke_width().value_or(svg_context.stroke_width()));
    }

    painter.translate(-offset);
    context.painter().clear_clip_rect();
}

}
