/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Layout/SVGGraphicsBox.h>
#include <LibWeb/SVG/SVGGeometryElement.h>

namespace Web::Layout {

class SVGGeometryBox final : public SVGGraphicsBox {
public:
    SVGGeometryBox(DOM::Document&, SVG::SVGGeometryElement&, NonnullRefPtr<CSS::StyleProperties>);
    virtual ~SVGGeometryBox() override = default;

    SVG::SVGGeometryElement& dom_node() { return verify_cast<SVG::SVGGeometryElement>(SVGGraphicsBox::dom_node()); }
    SVG::SVGGeometryElement const& dom_node() const { return verify_cast<SVG::SVGGeometryElement>(SVGGraphicsBox::dom_node()); }

    virtual void paint(PaintContext& context, PaintPhase phase) override;

private:
    virtual bool is_svg_geometry_box() const final { return true; }
};

template<>
inline bool Node::fast_is<SVGGeometryBox>() const { return is_svg_geometry_box(); }

}
