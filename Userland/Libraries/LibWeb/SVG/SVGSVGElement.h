/*
 * Copyright (c) 2020, Matthew Olsson <matthewcolsson@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Bitmap.h>
#include <LibWeb/SVG/AttributeParser.h>
#include <LibWeb/SVG/SVGGraphicsElement.h>
#include <LibWeb/SVG/ViewBox.h>

namespace Web::SVG {

class SVGSVGElement final : public SVGGraphicsElement {
    WEB_PLATFORM_OBJECT(SVGSVGElement, SVGGraphicsElement);

public:
    virtual JS::GCPtr<Layout::Node> create_layout_node(NonnullRefPtr<CSS::StyleProperties>) override;

    virtual void apply_presentational_hints(CSS::StyleProperties&) const override;

    virtual bool requires_svg_container() const override { return false; }
    virtual bool is_svg_container() const override { return true; }

    [[nodiscard]] Optional<ViewBox> view_box() const;
    Optional<PreserveAspectRatio> const& preserve_aspect_ratio() const { return m_preserve_aspect_ratio; }

    void set_fallback_view_box_for_svg_as_image(Optional<ViewBox>);

private:
    SVGSVGElement(DOM::Document&, DOM::QualifiedName);

    virtual void initialize(JS::Realm&) override;

    virtual bool is_svg_svg_element() const override { return true; }

    virtual void attribute_changed(DeprecatedFlyString const& name, DeprecatedString const& value) override;

    void update_fallback_view_box_for_svg_as_image();

    Optional<ViewBox> m_view_box;
    Optional<PreserveAspectRatio> m_preserve_aspect_ratio;

    Optional<ViewBox> m_fallback_view_box_for_svg_as_image;
};

}

namespace Web::DOM {

template<>
inline bool Node::fast_is<SVG::SVGSVGElement>() const { return is_svg_svg_element(); }

}
