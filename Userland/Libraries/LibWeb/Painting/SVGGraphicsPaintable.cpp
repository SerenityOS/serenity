/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/ImageBox.h>
#include <LibWeb/Layout/SVGClipBox.h>
#include <LibWeb/Layout/SVGMaskBox.h>
#include <LibWeb/Painting/CommandExecutorCPU.h>
#include <LibWeb/Painting/SVGClipPaintable.h>
#include <LibWeb/Painting/SVGGraphicsPaintable.h>
#include <LibWeb/Painting/StackingContext.h>
#include <LibWeb/SVG/SVGMaskElement.h>
#include <LibWeb/SVG/SVGSVGElement.h>

namespace Web::Painting {

JS::NonnullGCPtr<SVGGraphicsPaintable> SVGGraphicsPaintable::create(Layout::SVGGraphicsBox const& layout_box)
{
    return layout_box.heap().allocate_without_realm<SVGGraphicsPaintable>(layout_box);
}

SVGGraphicsPaintable::SVGGraphicsPaintable(Layout::SVGGraphicsBox const& layout_box)
    : SVGPaintable(layout_box)
{
}

Layout::SVGGraphicsBox const& SVGGraphicsPaintable::layout_box() const
{
    return static_cast<Layout::SVGGraphicsBox const&>(layout_node());
}

Optional<CSSPixelRect> SVGGraphicsPaintable::get_masking_area() const
{
    auto const& graphics_element = verify_cast<SVG::SVGGraphicsElement const>(*dom_node());
    Optional<CSSPixelRect> masking_area = {};
    if (auto* mask_box = graphics_element.layout_node()->first_child_of_type<Layout::SVGMaskBox>())
        masking_area = mask_box->dom_node().resolve_masking_area(mask_box->paintable_box()->absolute_border_box_rect());
    if (auto* clip_box = graphics_element.layout_node()->first_child_of_type<Layout::SVGClipBox>()) {
        // This is a bit ad-hoc, but if we have both a mask and a clip-path, intersect the two areas to find the masking area.
        auto clip_area = clip_box->paintable_box()->absolute_border_box_rect();
        if (masking_area.has_value())
            masking_area = masking_area->intersected(clip_area);
        else
            masking_area = clip_area;
    }
    return masking_area;
}

static Gfx::Bitmap::MaskKind mask_type_to_gfx_mask_kind(CSS::MaskType mask_type)
{
    switch (mask_type) {
    case CSS::MaskType::Alpha:
        return Gfx::Bitmap::MaskKind::Alpha;
    case CSS::MaskType::Luminance:
        return Gfx::Bitmap::MaskKind::Luminance;
    default:
        VERIFY_NOT_REACHED();
    }
}

Optional<Gfx::Bitmap::MaskKind> SVGGraphicsPaintable::get_mask_type() const
{
    auto const& graphics_element = verify_cast<SVG::SVGGraphicsElement const>(*dom_node());
    if (auto mask = graphics_element.mask())
        return mask_type_to_gfx_mask_kind(mask->layout_node()->computed_values().mask_type());
    if (graphics_element.clip_path())
        return Gfx::Bitmap::MaskKind::Alpha;
    return {};
}

RefPtr<Gfx::Bitmap> SVGGraphicsPaintable::calculate_mask(PaintContext& context, CSSPixelRect const& masking_area) const
{
    auto const& graphics_element = verify_cast<SVG::SVGGraphicsElement const>(*dom_node());
    auto mask_rect = context.enclosing_device_rect(masking_area);
    auto paint_mask_or_clip = [&](PaintableBox const& paintable) -> RefPtr<Gfx::Bitmap> {
        auto mask_bitmap_or_error = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, mask_rect.size().to_type<int>());
        RefPtr<Gfx::Bitmap> mask_bitmap = {};
        if (mask_bitmap_or_error.is_error())
            return {};
        mask_bitmap = mask_bitmap_or_error.release_value();
        CommandList painting_commands;
        RecordingPainter recording_painter(painting_commands);
        recording_painter.translate(-mask_rect.location().to_type<int>());
        auto paint_context = context.clone(recording_painter);
        paint_context.set_svg_transform(graphics_element.get_transform());
        paint_context.set_draw_svg_geometry_for_clip_path(is<SVGClipPaintable>(paintable));
        StackingContext::paint_node_as_stacking_context(paintable, paint_context);
        CommandExecutorCPU executor { *mask_bitmap };
        painting_commands.execute(executor);
        return mask_bitmap;
    };
    RefPtr<Gfx::Bitmap> mask_bitmap = {};
    if (auto* mask_box = graphics_element.layout_node()->first_child_of_type<Layout::SVGMaskBox>()) {
        auto& mask_paintable = static_cast<PaintableBox const&>(*mask_box->paintable());
        mask_bitmap = paint_mask_or_clip(mask_paintable);
    }
    if (auto* clip_box = graphics_element.layout_node()->first_child_of_type<Layout::SVGClipBox>()) {
        auto& clip_paintable = static_cast<PaintableBox const&>(*clip_box->paintable());
        auto clip_bitmap = paint_mask_or_clip(clip_paintable);
        // Combine the clip-path with the mask (if present).
        if (mask_bitmap && clip_bitmap)
            mask_bitmap->apply_mask(*clip_bitmap, Gfx::Bitmap::MaskKind::Alpha);
        if (!mask_bitmap)
            mask_bitmap = clip_bitmap;
    }
    return mask_bitmap;
}

}
