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
    WebIDL::ExceptionOr<int> get_number_of_chars() const;

    Optional<TextAnchor> text_anchor() const;

protected:
    SVGTextContentElement(DOM::Document&, DOM::QualifiedName);

    virtual JS::ThrowCompletionOr<void> initialize(JS::Realm&) override;
};

}
