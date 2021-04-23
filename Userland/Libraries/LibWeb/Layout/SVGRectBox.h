/*
 * Copyright (c) 2021, Simon Danner <danner.simon@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Layout/SVGGraphicsBox.h>
#include <LibWeb/SVG/SVGRectElement.h>

namespace Web::Layout {

class SVGRectBox final : public SVGGraphicsBox {
public:
    SVGRectBox(DOM::Document&, SVG::SVGRectElement&, NonnullRefPtr<CSS::StyleProperties>);
    virtual ~SVGRectBox() override = default;

    SVG::SVGRectElement& dom_node() { return downcast<SVG::SVGRectElement>(SVGGraphicsBox::dom_node()); }

    virtual void prepare_for_replaced_layout() override;
    virtual void paint(PaintContext& context, PaintPhase phase) override;
   };

}
