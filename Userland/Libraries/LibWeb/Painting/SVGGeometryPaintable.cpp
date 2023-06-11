/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/AntiAliasingPainter.h>
#include <LibWeb/Layout/ImageBox.h>
#include <LibWeb/Painting/SVGGeometryPaintable.h>
#include <LibWeb/SVG/SVGSVGElement.h>

namespace Web::Painting {

JS::NonnullGCPtr<SVGGeometryPaintable> SVGGeometryPaintable::create(Layout::SVGGeometryBox const& layout_box)
{
    return layout_box.heap().allocate_without_realm<SVGGeometryPaintable>(layout_box);
}

SVGGeometryPaintable::SVGGeometryPaintable(Layout::SVGGeometryBox const& layout_box)
    : SVGGraphicsPaintable(layout_box)
{
}

Layout::SVGGeometryBox const& SVGGeometryPaintable::layout_box() const
{
    return static_cast<Layout::SVGGeometryBox const&>(layout_node());
}

Optional<HitTestResult> SVGGeometryPaintable::hit_test(CSSPixelPoint position, HitTestType type) const
{
    auto result = SVGGraphicsPaintable::hit_test(position, type);
    if (!result.has_value())
        return {};
    auto& geometry_element = layout_box().dom_node();
    if (auto transform = layout_box().layout_transform(); transform.has_value()) {
        auto transformed_bounding_box = transform->map_to_quad(
            const_cast<SVG::SVGGeometryElement&>(geometry_element).get_path().bounding_box());
        if (!transformed_bounding_box.contains(position.to_type<double>().to_type<float>()))
            return {};
    }
    return result;
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

    // FIXME: This should not be trucated to an int.
    Gfx::PainterStateSaver save_painter { context.painter() };
    auto offset = context.floored_device_point(svg_context.svg_element_position()).to_type<int>().to_type<float>();
    painter.translate(offset);

    auto const* svg_element = geometry_element.shadow_including_first_ancestor_of_type<SVG::SVGSVGElement>();
    auto maybe_view_box = svg_element->view_box();

    auto transform = layout_box().layout_transform();
    if (!transform.has_value())
        return;

    auto css_scale = context.device_pixels_per_css_pixel();
    auto paint_transform = Gfx::AffineTransform {}.scale(css_scale, css_scale).multiply(*transform);
    auto const& original_path = const_cast<SVG::SVGGeometryElement&>(geometry_element).get_path();
    Gfx::Path path = original_path.copy_transformed(paint_transform);

    // Fills are computed as though all paths are closed (https://svgwg.org/svg2-draft/painting.html#FillProperties)
    auto closed_path = [&] {
        // We need to fill the path before applying the stroke, however the filled
        // path must be closed, whereas the stroke path may not necessary be closed.
        // Copy the path and close it for filling, but use the previous path for stroke
        auto copy = path;
        copy.close();
        return copy;
    };

    // Note: This is assuming .x_scale() == .y_scale() (which it does currently).
    auto viewbox_scale = paint_transform.x_scale();

    auto svg_viewport = [&] {
        if (maybe_view_box.has_value())
            return Gfx::FloatRect { maybe_view_box->min_x, maybe_view_box->min_y, maybe_view_box->width, maybe_view_box->height };
        return Gfx::FloatRect { { 0, 0 }, svg_context.svg_element_size().to_type<double>().to_type<float>() };
    }();

    SVG::SVGPaintContext paint_context {
        .viewport = svg_viewport,
        .path_bounding_box = original_path.bounding_box(),
        .transform = paint_transform
    };

    auto fill_opacity = geometry_element.fill_opacity().value_or(svg_context.fill_opacity());
    if (auto paint_style = geometry_element.fill_paint_style(paint_context); paint_style.has_value()) {
        painter.fill_path(
            closed_path(),
            *paint_style,
            fill_opacity,
            Gfx::Painter::WindingRule::EvenOdd);
    } else if (auto fill_color = geometry_element.fill_color().value_or(svg_context.fill_color()).with_opacity(fill_opacity); fill_color.alpha() > 0) {
        painter.fill_path(
            closed_path(),
            fill_color,
            Gfx::Painter::WindingRule::EvenOdd);
    }

    auto stroke_opacity = geometry_element.stroke_opacity().value_or(svg_context.stroke_opacity());

    // Note: This is assuming .x_scale() == .y_scale() (which it does currently).
    float stroke_thickness = geometry_element.stroke_width().value_or(svg_context.stroke_width()) * viewbox_scale;

    if (auto paint_style = geometry_element.stroke_paint_style(paint_context); paint_style.has_value()) {
        painter.stroke_path(
            path,
            *paint_style,
            stroke_thickness);
    } else if (auto stroke_color = geometry_element.stroke_color().value_or(svg_context.stroke_color()).with_opacity(stroke_opacity); stroke_color.alpha() > 0) {
        painter.stroke_path(
            path,
            stroke_color,
            stroke_thickness);
    }
}

}
