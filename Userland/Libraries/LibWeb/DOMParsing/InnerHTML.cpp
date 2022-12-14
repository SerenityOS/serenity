/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Heap/Heap.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/DocumentFragment.h>
#include <LibWeb/DOMParsing/InnerHTML.h>
#include <LibWeb/HTML/Parser/HTMLParser.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::DOMParsing {

// https://w3c.github.io/DOM-Parsing/#dfn-fragment-parsing-algorithm
WebIDL::ExceptionOr<JS::NonnullGCPtr<DOM::DocumentFragment>> parse_fragment(DeprecatedString const& markup, DOM::Element& context_element)
{
    // FIXME: Handle XML documents.

    auto& realm = context_element.realm();

    auto new_children = HTML::HTMLParser::parse_html_fragment(context_element, markup);
    auto fragment = realm.heap().allocate<DOM::DocumentFragment>(realm, context_element.document());

    for (auto& child : new_children) {
        // I don't know if this can throw here, but let's be safe.
        (void)TRY(fragment->append_child(*child));
    }

    return fragment;
}

// https://w3c.github.io/DOM-Parsing/#dom-innerhtml-innerhtml
WebIDL::ExceptionOr<void> inner_html_setter(JS::NonnullGCPtr<DOM::Node> context_object, DeprecatedString const& value)
{
    // 1. Let context element be the context object's host if the context object is a ShadowRoot object, or the context object otherwise.
    //    (This is handled in Element and ShadowRoot)
    JS::NonnullGCPtr<DOM::Element> context_element = is<DOM::ShadowRoot>(*context_object) ? *verify_cast<DOM::ShadowRoot>(*context_object).host() : verify_cast<DOM::Element>(*context_object);

    // 2. Let fragment be the result of invoking the fragment parsing algorithm with the new value as markup, and with context element.
    auto fragment = TRY(parse_fragment(value, context_element));

    // 3. If the context object is a template element, then let context object be the template's template contents (a DocumentFragment).
    if (is<HTML::HTMLTemplateElement>(*context_object))
        context_object = verify_cast<HTML::HTMLTemplateElement>(*context_object).content();

    // 4. Replace all with fragment within the context object.
    context_object->replace_all(fragment);

    // NOTE: We don't invalidate style & layout for <template> elements since they don't affect rendering.
    if (!is<HTML::HTMLTemplateElement>(*context_object)) {
        context_object->set_needs_style_update(true);

        // NOTE: Since the DOM has changed, we have to rebuild the layout tree.
        context_object->document().invalidate_layout();
        context_object->document().set_needs_layout();
    }

    return {};
}

}
