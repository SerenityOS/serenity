/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/DocumentFragment.h>
#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/HTML/Parser/HTMLDocumentParser.h>

namespace Web::DOMParsing {

// https://w3c.github.io/DOM-Parsing/#dfn-fragment-parsing-algorithm
static DOM::ExceptionOr<NonnullRefPtr<DOM::DocumentFragment>> parse_fragment(String const& markup, DOM::Element& context_element)
{
    // FIXME: Handle XML documents.

    auto new_children = HTML::HTMLDocumentParser::parse_html_fragment(context_element, markup);
    auto fragment = make_ref_counted<DOM::DocumentFragment>(context_element.document());

    for (auto& child : new_children) {
        // I don't know if this can throw here, but let's be safe.
        auto result = fragment->append_child(child);
        if (result.is_exception())
            return result.exception();
    }

    return fragment;
}

}
