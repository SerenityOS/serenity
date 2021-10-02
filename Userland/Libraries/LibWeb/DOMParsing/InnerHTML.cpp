/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/DocumentFragment.h>
#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/DOMParsing/InnerHTML.h>
#include <LibWeb/HTML/Parser/HTMLParser.h>

namespace Web::DOMParsing {

// https://w3c.github.io/DOM-Parsing/#dfn-fragment-parsing-algorithm
static DOM::ExceptionOr<NonnullRefPtr<DOM::DocumentFragment>> parse_fragment(String const& markup, DOM::Element& context_element)
{
    // FIXME: Handle XML documents.

    auto new_children = HTML::HTMLParser::parse_html_fragment(context_element, markup);
    auto fragment = make_ref_counted<DOM::DocumentFragment>(context_element.document());

    for (auto& child : new_children) {
        // I don't know if this can throw here, but let's be safe.
        auto result = fragment->append_child(child);
        if (result.is_exception())
            return result.exception();
    }

    return fragment;
}

// https://w3c.github.io/DOM-Parsing/#dom-innerhtml-innerhtml
DOM::ExceptionOr<void> inner_html_setter(NonnullRefPtr<DOM::Node> context_object, String const& value)
{
    // 1. Let context element be the context object's host if the context object is a ShadowRoot object, or the context object otherwise.
    //    (This is handled in Element and ShadowRoot)
    NonnullRefPtr<DOM::Element> context_element = is<DOM::ShadowRoot>(*context_object) ? *verify_cast<DOM::ShadowRoot>(*context_object).host() : verify_cast<DOM::Element>(*context_object);

    // 2. Let fragment be the result of invoking the fragment parsing algorithm with the new value as markup, and with context element.
    auto fragment_or_exception = parse_fragment(value, context_element);
    if (fragment_or_exception.is_exception())
        return fragment_or_exception.exception();
    auto fragment = fragment_or_exception.release_value();

    // 3. If the context object is a template element, then let context object be the template's template contents (a DocumentFragment).
    if (is<HTML::HTMLTemplateElement>(*context_object))
        context_object = verify_cast<HTML::HTMLTemplateElement>(*context_object).content();

    // 4. Replace all with fragment within the context object.
    context_object->replace_all(fragment);

    return {};
}

}
