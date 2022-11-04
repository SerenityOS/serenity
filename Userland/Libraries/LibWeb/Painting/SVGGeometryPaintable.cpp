/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "LibGfx/AffineTransform.h"
#include "LibGfx/Rasterizer.h"
#include <LibGfx/PathPainter.h>
#include <LibWeb/Layout/ImageBox.h>
#include <LibWeb/Painting/SVGGeometryPaintable.h>
#include <LibWeb/SVG/SVGSVGElement.h>
#include <cstdio>

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
    auto const* svg_element = geometry_element.first_ancestor_of_type<SVG::SVGSVGElement>();
    auto& svg_context = context.svg_context();
    auto offset = svg_context.svg_element_position();
    auto maybe_view_box = svg_element->view_box();
    Gfx::AffineTransform transform;
    transform.translate(Gfx::Point<float>(context.painter().translation()));
    transform.scale(context.painter().scale());
    transform.translate(offset);
    if (maybe_view_box.has_value()) {
        transform.scale(layout_box().viewbox_scaling(), layout_box().viewbox_scaling());
        transform.translate(-layout_box().viewbox_origin());
    }
    auto fill_color = geometry_element.fill_color().value_or(svg_context.fill_color());
    auto stroke_color = geometry_element.stroke_color().value_or(svg_context.stroke_color());
    auto stroke_width = geometry_element.stroke_width().value_or(svg_context.stroke_width()) * 2;
    auto clip_rect = context.painter().clip_rect().intersected(
        enclosing_int_rect(absolute_rect()).translated(context.painter().translation()));
    Gfx::Path path = const_cast<SVG::SVGGeometryElement&>(geometry_element).get_path();

    paint_internal(
        fill_color,
        stroke_color,
        stroke_width,
        transform,
        clip_rect,
        path,
        *context.painter().target());
}

void SVGGeometryPaintable::paint_internal(
    Gfx::Color fill_color,
    Gfx::Color stroke_color,
    float stroke_width,
    Gfx::AffineTransform const& transform,
    Gfx::IntRect clip_rect,
    Gfx::Path const& path,
    Gfx::Bitmap& bitmap) const
{
    Gfx::PathPainter painter { bitmap };
    painter.set_clip_rect(clip_rect);
    painter.set_transform(transform);
    // painter.begin_path(StrokeKind stroke_kind, FillKind fill_kind, float thickness)
    painter.begin_path(
        stroke_color.alpha() > 0 ? Gfx::PathPainter::StrokeKind::ClosedStroke : Gfx::PathPainter::StrokeKind::NoStroke,
        fill_color.alpha() > 0 ? Gfx::PathPainter::FillKind::Filled : Gfx::PathPainter::FillKind::NotFilled,
        stroke_width);
    for (auto& segment : path.segments()) {
        switch (segment.type()) {
        case Gfx::Segment::Type::Invalid:
            break;
        case Gfx::Segment::Type::MoveTo:
            painter.move_to(segment.point());
            break;
        case Gfx::Segment::Type::LineTo:
            painter.line_to(segment.point());
            break;
        case Gfx::Segment::Type::QuadraticBezierCurveTo: {
            auto& curve = static_cast<Gfx::QuadraticBezierCurveSegment const&>(segment);
            painter.quadratic_bezier_curve_to(curve.through(), curve.point());
            break;
        }
        case Gfx::Segment::Type::CubicBezierCurveTo: {
            auto& curve = static_cast<Gfx::CubicBezierCurveSegment const&>(segment);
            painter.cubic_bezier_curve_to(curve.through_0(), curve.through_1(), curve.point());
            break;
        }
        case Gfx::Segment::Type::EllipticalArcTo: {
            auto& arc = static_cast<Gfx::EllipticalArcSegment const&>(segment);
            painter.elliptical_arc_to(arc.point(), arc.center(), arc.radii(), arc.x_axis_rotation(), arc.theta_1(), arc.theta_delta());
            break;
        }
        }
    }
    painter.end_path(Gfx::Rasterizer::Paint { stroke_color }, Gfx::Rasterizer::Paint { fill_color });
}

}
