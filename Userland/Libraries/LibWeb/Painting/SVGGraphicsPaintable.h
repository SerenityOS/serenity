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
public:
    virtual void before_children_paint(PaintContext&, PaintPhase) const override;

    Layout::SVGGraphicsBox const& layout_box() const;

protected:
    SVGGraphicsPaintable(Layout::SVGGraphicsBox const&);
};

}
