/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Layout/SVGGeometryBox.h>
#include <LibWeb/Painting/SVGGraphicsPaintable.h>

namespace Web::Painting {

class SVGGeometryPaintable final : public SVGGraphicsPaintable {
    JS_CELL(SVGGeometryPaintable, SVGGraphicsPaintable);

public:
    class PathData {
    public:
        PathData(Gfx::Path path, Gfx::AffineTransform svg_to_viewbox_transform, Gfx::AffineTransform svg_transform)
            : m_computed_path(move(path))
            , m_svg_to_viewbox_transform(svg_to_viewbox_transform)
            , m_svg_transform(svg_transform)
        {
        }

        Gfx::Path const& computed_path() const { return m_computed_path; }

        Gfx::AffineTransform const& svg_to_viewbox_transform() const { return m_svg_to_viewbox_transform; }

        Gfx::AffineTransform const& svg_transform() const { return m_svg_transform; }

        Gfx::AffineTransform svg_to_css_pixels_transform(
            Optional<Gfx::AffineTransform const&> additional_svg_transform = {}) const
        {
            return Gfx::AffineTransform {}.multiply(svg_to_viewbox_transform()).multiply(additional_svg_transform.value_or(Gfx::AffineTransform {})).multiply(svg_transform());
        }

        Gfx::AffineTransform svg_to_device_pixels_transform(
            PaintContext const& context,
            Gfx::AffineTransform const& additional_svg_transform) const
        {
            auto css_scale = context.device_pixels_per_css_pixel();
            return Gfx::AffineTransform {}.scale({ css_scale, css_scale }).multiply(svg_to_css_pixels_transform(additional_svg_transform));
        }

    private:
        Gfx::Path m_computed_path;
        Gfx::AffineTransform m_svg_to_viewbox_transform;
        Gfx::AffineTransform m_svg_transform;
    };

    static JS::NonnullGCPtr<SVGGeometryPaintable> create(Layout::SVGGeometryBox const&);

    virtual Optional<HitTestResult> hit_test(CSSPixelPoint, HitTestType) const override;

    virtual void paint(PaintContext&, PaintPhase) const override;

    Layout::SVGGeometryBox const& layout_box() const;

    void set_path_data(PathData path_data)
    {
        m_path_data = move(path_data);
    }

    Optional<PathData> const& path_data() const { return m_path_data; }

protected:
    SVGGeometryPaintable(Layout::SVGGeometryBox const&);

    Optional<PathData> m_path_data = {};
};

}
