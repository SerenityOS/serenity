/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Utf16View.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/Utf16String.h>
#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/Layout/SVGTextBox.h>
#include <LibWeb/SVG/AttributeNames.h>
#include <LibWeb/SVG/AttributeParser.h>
#include <LibWeb/SVG/SVGGeometryElement.h>
#include <LibWeb/SVG/SVGTextContentElement.h>

namespace Web::SVG {

SVGTextContentElement::SVGTextContentElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : SVGGraphicsElement(document, move(qualified_name))
{
}

void SVGTextContentElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::SVGTextContentElementPrototype>(realm, "SVGTextContentElement"));
}

Optional<TextAnchor> SVGTextContentElement::text_anchor() const
{
    if (!layout_node())
        return {};
    switch (layout_node()->computed_values().text_anchor()) {
    case CSS::TextAnchor::Start:
        return TextAnchor::Start;
    case CSS::TextAnchor::Middle:
        return TextAnchor::Middle;
    case CSS::TextAnchor::End:
        return TextAnchor::End;
    default:
        VERIFY_NOT_REACHED();
    }
}

// https://svgwg.org/svg2-draft/text.html#__svg__SVGTextContentElement__getNumberOfChars
WebIDL::ExceptionOr<int> SVGTextContentElement::get_number_of_chars() const
{
    auto chars = TRY_OR_THROW_OOM(vm(), utf8_to_utf16(child_text_content()));
    return static_cast<int>(chars.size());
}

}
