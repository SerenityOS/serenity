/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/SVG/AttributeParser.h>
#include <LibWeb/SVG/SVGGraphicsElement.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::SVG {

// https://svgwg.org/svg2-draft/text.html#InterfaceSVGTextContentElement
class SVGTextContentElement : public SVGGraphicsElement {
    WEB_PLATFORM_OBJECT(SVGTextContentElement, SVGGraphicsElement);

public:
    virtual JS::GCPtr<Layout::Node> create_layout_node(NonnullRefPtr<CSS::StyleProperties>) override;

    virtual void attribute_changed(DeprecatedFlyString const& name, DeprecatedString const& value) override;

    WebIDL::ExceptionOr<int> get_number_of_chars() const;

    Gfx::FloatPoint get_offset() const;

    Optional<TextAnchor> text_anchor() const;

protected:
    SVGTextContentElement(DOM::Document&, DOM::QualifiedName);

    virtual JS::ThrowCompletionOr<void> initialize(JS::Realm&) override;

private:
    float m_x { 0 };
    float m_y { 0 };
    float m_dx { 0 };
    float m_dy { 0 };
};

}
