/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/ImageBox.h>
#include <LibWeb/Painting/CommandExecutorCPU.h>
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

bool SVGGraphicsPaintable::forms_unconnected_subtree() const
{
    // Masks should not be painted (i.e. reachable) unless referenced by another element.
    return is<SVG::SVGMaskElement>(dom_node());
}

Layout::SVGGraphicsBox const& SVGGraphicsPaintable::layout_box() const
{
    return static_cast<Layout::SVGGraphicsBox const&>(layout_node());
}

Optional<CSSPixelRect> SVGGraphicsPaintable::get_masking_area() const
{
    auto const& graphics_element = verify_cast<SVG::SVGGraphicsElement const>(*dom_node());
    auto mask = graphics_element.mask();
    if (!mask)
        return {};
    auto target_area = [&] {
        auto mask_units = mask->mask_units();
        if (mask_units == SVG::MaskUnits::UserSpaceOnUse) {
            auto const* svg_element = graphics_element.shadow_including_first_ancestor_of_type<SVG::SVGSVGElement>();
            return svg_element->paintable_box()->absolute_border_box_rect();
        } else {
            VERIFY(mask_units == SVG::MaskUnits::ObjectBoundingBox);
            return absolute_border_box_rect();
        }
    }();
    return mask->resolve_masking_area(target_area);
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
    auto mask = graphics_element.mask();
    if (!mask)
        return {};
    return mask_type_to_gfx_mask_kind(mask->layout_node()->computed_values().mask_type());
}

RefPtr<Gfx::Bitmap> SVGGraphicsPaintable::calculate_mask(PaintContext& context, CSSPixelRect const& masking_area) const
{
    auto const& graphics_element = verify_cast<SVG::SVGGraphicsElement const>(*dom_node());
    auto mask = graphics_element.mask();
    VERIFY(mask);
    if (mask->mask_content_units() != SVG::MaskContentUnits::UserSpaceOnUse) {
        dbgln("SVG: maskContentUnits=objectBoundingBox is not supported");
        return {};
    }
    auto mask_rect = context.enclosing_device_rect(masking_area);
    RefPtr<Gfx::Bitmap> mask_bitmap = {};
    if (mask && mask->layout_node() && is<PaintableBox>(mask->layout_node()->paintable())) {
        auto& mask_paintable = static_cast<PaintableBox const&>(*mask->layout_node()->paintable());
        auto mask_bitmap_or_error = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, mask_rect.size().to_type<int>());
        if (mask_bitmap_or_error.is_error())
            return {};
        mask_bitmap = mask_bitmap_or_error.release_value();
        {
            CommandList painting_commands;
            RecordingPainter recording_painter(painting_commands);
            recording_painter.translate(-mask_rect.location().to_type<int>());
            auto paint_context = context.clone(recording_painter);
            paint_context.set_svg_transform(graphics_element.get_transform());
            StackingContext::paint_node_as_stacking_context(mask_paintable, paint_context);
            CommandExecutorCPU executor { *mask_bitmap };
            painting_commands.execute(executor);
        }
    }
    return mask_bitmap;
}

}
