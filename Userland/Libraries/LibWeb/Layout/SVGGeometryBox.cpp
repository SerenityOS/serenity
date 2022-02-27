/*
 * Copyright (c) 2020, Matthew Olsson <matthewcolsson@gmail.com>
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/AntiAliasingPainter.h>
#include <LibGfx/Painter.h>
#include <LibWeb/Layout/SVGGeometryBox.h>
#include <LibWeb/SVG/SVGPathElement.h>
#include <LibWeb/SVG/SVGSVGElement.h>

namespace Web::Layout {

SVGGeometryBox::SVGGeometryBox(DOM::Document& document, SVG::SVGGeometryElement& element, NonnullRefPtr<CSS::StyleProperties> properties)
    : SVGGraphicsBox(document, element, properties)
{
}

void SVGGeometryBox::paint(PaintContext& context, PaintPhase phase)
{
    if (!is_visible())
        return;

    SVGGraphicsBox::paint(context, phase);

    if (phase != PaintPhase::Foreground)
        return;

    auto& geometry_element = dom_node();

    Gfx::AntiAliasingPainter painter { context.painter() };
    auto& svg_context = context.svg_context();

    auto offset = svg_context.svg_element_position();
    painter.translate(offset);

    SVG::SVGSVGElement* svg_element = geometry_element.first_ancestor_of_type<SVG::SVGSVGElement>();
    auto maybe_view_box = svg_element->view_box();

    context.painter().add_clip_rect((Gfx::Rect<int>)absolute_rect());

    Gfx::Path path = geometry_element.get_path();

    if (maybe_view_box.has_value()) {
        Gfx::Path new_path;
        auto scaling = viewbox_scaling();
        auto origin = viewbox_origin();

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
                new_path.elliptical_arc_to(transform_point(elliptical_arc_segment.point()), elliptical_arc_segment.radii().scaled(scaling, scaling), elliptical_arc_segment.x_axis_rotation(), false, false);
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

float SVGGeometryBox::viewbox_scaling() const
{
    auto* svg_box = dom_node().first_ancestor_of_type<SVG::SVGSVGElement>();

    if (!svg_box || !svg_box->view_box().has_value())
        return 1;

    auto view_box = svg_box->view_box().value();

    bool has_specified_width = svg_box->has_attribute(HTML::AttributeNames::width);
    auto specified_width = content_width();

    bool has_specified_height = svg_box->has_attribute(HTML::AttributeNames::height);
    auto specified_height = content_height();

    auto scale_width = has_specified_width ? specified_width / view_box.width : 1;
    auto scale_height = has_specified_height ? specified_height / view_box.height : 1;

    return min(scale_width, scale_height);
}
Gfx::FloatPoint SVGGeometryBox::viewbox_origin() const
{
    auto* svg_box = dom_node().first_ancestor_of_type<SVG::SVGSVGElement>();
    if (!svg_box || !svg_box->view_box().has_value())
        return { 0, 0 };
    return { svg_box->view_box().value().min_x, svg_box->view_box().value().min_y };
}

}
