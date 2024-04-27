/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2023, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/HTMLMetaElementPrototype.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/CSS/Parser/ParsingContext.h>
#include <LibWeb/CSS/PropertyID.h>
#include <LibWeb/CSS/StyleValues/ColorStyleValue.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/HTMLMetaElement.h>
#include <LibWeb/Infra/CharacterTypes.h>
#include <LibWeb/Page/Page.h>

namespace Web::HTML {

JS_DEFINE_ALLOCATOR(HTMLMetaElement);

HTMLMetaElement::HTMLMetaElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLMetaElement::~HTMLMetaElement() = default;

void HTMLMetaElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    WEB_SET_PROTOTYPE_FOR_INTERFACE(HTMLMetaElement);
}

Optional<HTMLMetaElement::HttpEquivAttributeState> HTMLMetaElement::http_equiv_state() const
{
    auto value = get_attribute_value(HTML::AttributeNames::http_equiv);

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

    // https://html.spec.whatwg.org/multipage/semantics.html#meta-theme-color
    // 1. To obtain a page's theme color, user agents must run the following steps:
    //     * The element is in a document tree
    //     * The element has a name attribute, whose value is an ASCII case-insensitive match for theme-color
    //     * The element has a content attribute
    auto content = attribute(AttributeNames::content);
    if (name().has_value() && name()->equals_ignoring_ascii_case("theme-color"sv) && content.has_value()) {
        auto context = CSS::Parser::ParsingContext { document() };

        // 2. For each element in candidate elements:

        // 1. If element has a media attribute and the value of element's media attribute does not match the environment, then continue.
        auto media = attribute(AttributeNames::media);
        if (media.has_value()) {
            auto query = parse_media_query(context, media.value());
            if (document().window() && !query->evaluate(*document().window()))
                return;
        }

        // 2. Let value be the result of stripping leading and trailing ASCII whitespace from the value of element's content attribute.
        auto value = content->bytes_as_string_view().trim(Infra::ASCII_WHITESPACE);

        // 3. Let color be the result of parsing value.
        auto css_value = parse_css_value(context, value, CSS::PropertyID::Color);
        if (css_value.is_null() || !css_value->is_color())
            return;
        auto color = css_value->as_color().color();

        // 4. If color is not failure, then return color.
        document().page().client().page_did_change_theme_color(color);
        return;
    }

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

            auto input = get_attribute_value(AttributeNames::content);
            if (input.is_empty())
                break;

            // 3. Run the shared declarative refresh steps with the meta element's node document, input, and the meta element.
            document().shared_declarative_refresh_steps(input, this);
            break;
        }
        default:
            dbgln("FIXME: Implement '{}' http-equiv state", get_attribute_value(AttributeNames::http_equiv));
            break;
        }
    }
}

}
