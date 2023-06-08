/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Layout/SVGTextBox.h>
#include <LibWeb/Painting/SVGGraphicsPaintable.h>

namespace Web::Painting {

class SVGTextPaintable final : public SVGGraphicsPaintable {
    JS_CELL(SVGTextPaintable, SVGGraphicsPaintable);

public:
    static JS::NonnullGCPtr<SVGTextPaintable> create(Layout::SVGTextBox const&);

    virtual Optional<HitTestResult> hit_test(CSSPixelPoint, HitTestType) const override;

    virtual void paint(PaintContext&, PaintPhase) const override;

    Layout::SVGTextBox const& layout_box() const
    {
        return static_cast<Layout::SVGTextBox const&>(layout_node());
    }

protected:
    SVGTextPaintable(Layout::SVGTextBox const&);
};

}
