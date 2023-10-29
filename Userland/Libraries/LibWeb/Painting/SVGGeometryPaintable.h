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
    static JS::NonnullGCPtr<SVGGeometryPaintable> create(Layout::SVGGeometryBox const&);

    virtual Optional<HitTestResult> hit_test(CSSPixelPoint, HitTestType) const override;

    virtual void paint(PaintContext&, PaintPhase) const override;

    Layout::SVGGeometryBox const& layout_box() const;

    void set_computed_path(Gfx::Path path)
    {
        m_computed_path = move(path);
    }

    Optional<Gfx::Path> const& computed_path() const { return m_computed_path; }

protected:
    SVGGeometryPaintable(Layout::SVGGeometryBox const&);

    Optional<Gfx::Path> m_computed_path = {};
};

}
