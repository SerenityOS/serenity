/*
 * Copyright (c) 2020, Matthew Olsson <matthewcolsson@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Bitmap.h>
#include <LibWeb/SVG/SVGGraphicsElement.h>
#include <LibWeb/SVG/ViewBox.h>

namespace Web::SVG {

class SVGSVGElement final : public SVGGraphicsElement {
public:
    using WrapperType = Bindings::SVGSVGElementWrapper;

    SVGSVGElement(DOM::Document&, DOM::QualifiedName);

    virtual RefPtr<Layout::Node> create_layout_node(NonnullRefPtr<CSS::StyleProperties>) override;

    virtual void apply_presentational_hints(CSS::StyleProperties&) const override;

    virtual bool requires_svg_container() const override { return false; }
    virtual bool is_svg_container() const override { return true; }

    Optional<ViewBox> const& view_box() const { return m_view_box; }

private:
    virtual bool is_svg_svg_element() const override { return true; }

    virtual void parse_attribute(FlyString const& name, String const& value) override;

    Optional<ViewBox> m_view_box;
};

}

namespace Web::DOM {

template<>
inline bool Node::fast_is<SVG::SVGSVGElement>() const { return is_svg_svg_element(); }

}
