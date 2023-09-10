/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/ImageBox.h>
#include <LibWeb/Painting/SVGGraphicsPaintable.h>
#include <LibWeb/Painting/StackingContext.h>
#include <LibWeb/SVG/SVGMaskElement.h>

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
    if (auto mask = graphics_element.mask())
        return mask->resolve_masking_area(absolute_border_box_rect());
    return {};
}

void SVGGraphicsPaintable::apply_mask(PaintContext& context, Gfx::Bitmap& target, CSSPixelRect const& masking_area) const
{
    auto const& graphics_element = verify_cast<SVG::SVGGraphicsElement const>(*dom_node());
    auto mask = graphics_element.mask();
    VERIFY(mask);
    if (mask->mask_content_units() != SVG::MaskContentUnits::UserSpaceOnUse) {
        dbgln("SVG: maskContentUnits=objectBoundingBox is not supported");
        return;
    }
    auto mask_rect = context.enclosing_device_rect(masking_area);
    RefPtr<Gfx::Bitmap> mask_bitmap = {};
    if (mask && mask->layout_node() && is<PaintableBox>(mask->layout_node()->paintable())) {
        auto& mask_paintable = static_cast<PaintableBox const&>(*mask->layout_node()->paintable());
        auto mask_bitmap_or_error = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, mask_rect.size().to_type<int>());
        if (mask_bitmap_or_error.is_error())
            return;
        mask_bitmap = mask_bitmap_or_error.release_value();
        {
            Gfx::Painter painter(*mask_bitmap);
            painter.translate(-mask_rect.location().to_type<int>());
            auto paint_context = context.clone(painter);
            paint_context.set_svg_transform(graphics_element.get_transform());
            StackingContext::paint_node_as_stacking_context(mask_paintable, paint_context);
        }
    }
    // TODO: Follow mask-type attribute to select between alpha/luminance masks.
    if (mask_bitmap)
        target.apply_mask(*mask_bitmap, Gfx::Bitmap::MaskKind::Luminance);
    return;
}

}
