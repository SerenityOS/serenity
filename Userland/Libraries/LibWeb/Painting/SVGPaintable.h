/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Layout/SVGBox.h>
#include <LibWeb/Painting/Paintable.h>

namespace Web::Painting {

class SVGPaintable : public Paintable {
public:
    static NonnullOwnPtr<SVGPaintable> create(Layout::SVGBox const&);

    virtual void before_children_paint(PaintContext&, PaintPhase) const override;
    virtual void after_children_paint(PaintContext&, PaintPhase) const override;

    Layout::SVGBox const& layout_box() const;

protected:
    SVGPaintable(Layout::SVGBox const&);
};

}
