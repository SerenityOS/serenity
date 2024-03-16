/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/CSS/StyleProperties.h>
#include <LibWeb/CSS/StyleValues/IdentifierStyleValue.h>
#include <LibWeb/HTML/HTMLPreElement.h>
#include <LibWeb/HTML/Numbers.h>

namespace Web::HTML {

JS_DEFINE_ALLOCATOR(HTMLPreElement);

HTMLPreElement::HTMLPreElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLPreElement::~HTMLPreElement() = default;

void HTMLPreElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    WEB_SET_PROTOTYPE_FOR_INTERFACE(HTMLPreElement);
}

void HTMLPreElement::apply_presentational_hints(CSS::StyleProperties& style) const
{
    HTMLElement::apply_presentational_hints(style);

    for_each_attribute([&](auto const& name, auto const&) {
        if (name.equals_ignoring_ascii_case(HTML::AttributeNames::wrap))
            style.set_property(CSS::PropertyID::WhiteSpace, CSS::IdentifierStyleValue::create(CSS::ValueID::PreWrap));
    });
}

// https://html.spec.whatwg.org/multipage/obsolete.html#dom-pre-width
WebIDL::Long HTMLPreElement::width() const
{
    // The width IDL attribute of the pre element must reflect the content attribute of the same name.
    if (auto width_string = get_attribute(HTML::AttributeNames::width); width_string.has_value()) {
        if (auto width = parse_integer(*width_string); width.has_value())
            return *width;
    }
    return 0;
}

WebIDL::ExceptionOr<void> HTMLPreElement::set_width(WebIDL::Long width)
{
    return set_attribute(HTML::AttributeNames::width, MUST(String::number(width)));
}

}
