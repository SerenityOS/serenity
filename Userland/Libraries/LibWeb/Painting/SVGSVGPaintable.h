/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Layout/SVGSVGBox.h>
#include <LibWeb/Painting/PaintableBox.h>

namespace Web::Painting {

class SVGSVGPaintable : public PaintableBox {
public:
    static NonnullRefPtr<SVGSVGPaintable> create(Layout::SVGSVGBox const&);

    virtual void before_children_paint(PaintContext&, PaintPhase, ShouldClipOverflow) const override;
    virtual void after_children_paint(PaintContext&, PaintPhase, ShouldClipOverflow) const override;

    Layout::SVGSVGBox const& layout_box() const;

protected:
    SVGSVGPaintable(Layout::SVGSVGBox const&);
};

}
