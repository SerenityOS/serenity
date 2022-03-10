/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Layout/SVGGeometryBox.h>
#include <LibWeb/Painting/SVGGraphicsPaintable.h>

namespace Web::Painting {

class SVGGeometryPaintable : public SVGGraphicsPaintable {
public:
    static NonnullRefPtr<SVGGeometryPaintable> create(Layout::SVGGeometryBox const&);

    virtual void paint(PaintContext&, PaintPhase) const override;

    Layout::SVGGeometryBox const& layout_box() const;

protected:
    SVGGeometryPaintable(Layout::SVGGeometryBox const&);
};

}
