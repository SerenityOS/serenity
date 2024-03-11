/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Layout/SVGGraphicsBox.h>
#include <LibWeb/Painting/SVGPaintable.h>

namespace Web::Painting {

class SVGGraphicsPaintable : public SVGPaintable {
    JS_CELL(SVGGraphicsPaintable, SVGPaintable);

public:
    class ComputedTransforms {
    public:
        ComputedTransforms(Gfx::AffineTransform svg_to_viewbox_transform, Gfx::AffineTransform svg_transform)
            : m_svg_to_viewbox_transform(svg_to_viewbox_transform)
            , m_svg_transform(svg_transform)
        {
        }

        ComputedTransforms() = default;

        Gfx::AffineTransform const& svg_to_viewbox_transform() const { return m_svg_to_viewbox_transform; }
        Gfx::AffineTransform const& svg_transform() const { return m_svg_transform; }

        Gfx::AffineTransform svg_to_css_pixels_transform(
            Optional<Gfx::AffineTransform const&> additional_svg_transform = {}) const
        {
            return Gfx::AffineTransform {}.multiply(svg_to_viewbox_transform()).multiply(additional_svg_transform.value_or(Gfx::AffineTransform {})).multiply(svg_transform());
        }

        Gfx::AffineTransform svg_to_device_pixels_transform(PaintContext const& context) const
        {
            auto css_scale = context.device_pixels_per_css_pixel();
            return Gfx::AffineTransform {}.scale({ css_scale, css_scale }).multiply(svg_to_css_pixels_transform(context.svg_transform()));
        }

    private:
        Gfx::AffineTransform m_svg_to_viewbox_transform {};
        Gfx::AffineTransform m_svg_transform {};
    };

    static JS::NonnullGCPtr<SVGGraphicsPaintable> create(Layout::SVGGraphicsBox const&);

    Layout::SVGGraphicsBox const& layout_box() const;

    virtual Optional<CSSPixelRect> get_masking_area() const override;
    virtual Optional<Gfx::Bitmap::MaskKind> get_mask_type() const override;
    virtual RefPtr<Gfx::Bitmap> calculate_mask(PaintContext&, CSSPixelRect const& masking_area) const override;

    void set_computed_transforms(ComputedTransforms computed_transforms)
    {
        m_computed_transforms = computed_transforms;
    }

    ComputedTransforms const& computed_transforms() const
    {
        return m_computed_transforms;
    }

protected:
    SVGGraphicsPaintable(Layout::SVGGraphicsBox const&);

    ComputedTransforms m_computed_transforms;
};

}
