/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Layout/SVGGraphicsBox.h>
#include <LibWeb/Painting/SVGGraphicsPaintable.h>

namespace Web::Painting {

class SVGPathPaintable final : public SVGGraphicsPaintable {
    JS_CELL(SVGPathPaintable, SVGGraphicsPaintable);

public:
    static JS::NonnullGCPtr<SVGPathPaintable> create(Layout::SVGGraphicsBox const&);

    virtual Optional<HitTestResult> hit_test(CSSPixelPoint, HitTestType) const override;

    virtual void paint(PaintContext&, PaintPhase) const override;

    Layout::SVGGraphicsBox const& layout_box() const;

    void set_computed_path(Gfx::Path path)
    {
        m_computed_path = move(path);
    }

    Optional<Gfx::Path> const& computed_path() const { return m_computed_path; }

protected:
    SVGPathPaintable(Layout::SVGGraphicsBox const&);

    Optional<Gfx::Path> m_computed_path = {};
};

}
