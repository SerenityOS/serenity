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
    JS_CELL(SVGGraphicsPaintable, SVGPaintable);

public:
    static JS::NonnullGCPtr<SVGGraphicsPaintable> create(Layout::SVGGraphicsBox const&);

    Layout::SVGGraphicsBox const& layout_box() const;

    virtual bool forms_unconnected_subtree() const override;

protected:
    SVGGraphicsPaintable(Layout::SVGGraphicsBox const&);
};

}
