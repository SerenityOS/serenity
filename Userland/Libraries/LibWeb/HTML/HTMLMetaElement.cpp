/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2023, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/HTMLMetaElement.h>

namespace Web::HTML {

HTMLMetaElement::HTMLMetaElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLMetaElement::~HTMLMetaElement() = default;

void HTMLMetaElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLMetaElementPrototype>(realm, "HTMLMetaElement"));
}

Optional<HTMLMetaElement::HttpEquivAttributeState> HTMLMetaElement::http_equiv_state() const
{
    auto value = attribute(HTML::AttributeNames::http_equiv);

#define __ENUMERATE_HTML_META_HTTP_EQUIV_ATTRIBUTE(keyword, state) \
    if (value.equals_ignoring_ascii_case(#keyword##sv))            \
        return HTMLMetaElement::HttpEquivAttributeState::state;
    ENUMERATE_HTML_META_HTTP_EQUIV_ATTRIBUTES
#undef __ENUMERATE_HTML_META_HTTP_EQUIV_ATTRIBUTE

    return OptionalNone {};
}

void HTMLMetaElement::inserted()
{
    Base::inserted();

    // https://html.spec.whatwg.org/multipage/semantics.html#pragma-directives
    // When a meta element is inserted into the document, if its http-equiv attribute is present and represents one of
    // the above states, then the user agent must run the algorithm appropriate for that state, as described in the
    // following list:
    auto http_equiv = http_equiv_state();
    if (http_equiv.has_value()) {
        switch (http_equiv.value()) {
        case HttpEquivAttributeState::Refresh: {
            // https://html.spec.whatwg.org/multipage/semantics.html#attr-meta-http-equiv-refresh
            // 1. If the meta element has no content attribute, or if that attribute's value is the empty string, then return.
            // 2. Let input be the value of the element's content attribute.
            if (!has_attribute(AttributeNames::content))
                break;

            auto input = attribute(AttributeNames::content);
            if (input.is_empty())
                break;

            // 3. Run the shared declarative refresh steps with the meta element's node document, input, and the meta element.
            document().shared_declarative_refresh_steps(input, this);
            break;
        }
        default:
            dbgln("FIXME: Implement '{}' http-equiv state", attribute(AttributeNames::http_equiv));
            break;
        }
    }
}

}
