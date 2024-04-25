/*
 * Copyright (c) 2024, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Layout/SVGForeignObjectBox.h>
#include <LibWeb/Painting/PaintableBox.h>

namespace Web::Painting {

class SVGForeignObjectPaintable final : public PaintableWithLines {
    JS_CELL(SVGForeignObjectPaintable, PaintableWithLines);
    JS_DECLARE_ALLOCATOR(SVGForeignObjectPaintable);

public:
    static JS::NonnullGCPtr<SVGForeignObjectPaintable> create(Layout::SVGForeignObjectBox const&);

    virtual TraversalDecision hit_test(CSSPixelPoint, HitTestType, Function<TraversalDecision(HitTestResult)> const& callback) const override;

    virtual void paint(PaintContext&, PaintPhase) const override;

    Layout::SVGForeignObjectBox const& layout_box() const;

protected:
    SVGForeignObjectPaintable(Layout::SVGForeignObjectBox const&);
};

}
