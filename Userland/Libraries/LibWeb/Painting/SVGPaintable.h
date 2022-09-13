/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Layout/SVGBox.h>
#include <LibWeb/Painting/PaintableBox.h>

namespace Web::Painting {

class SVGPaintable : public PaintableBox {
public:
    virtual void before_children_paint(PaintContext&, PaintPhase, ShouldClipOverflow) const override;
    virtual void after_children_paint(PaintContext&, PaintPhase, ShouldClipOverflow) const override;

    Layout::SVGBox const& layout_box() const;

protected:
    SVGPaintable(Layout::SVGBox const&);

    virtual Gfx::FloatRect compute_absolute_rect() const override;
};

}
