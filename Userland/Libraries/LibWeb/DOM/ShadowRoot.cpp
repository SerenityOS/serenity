/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/ShadowRoot.h>
#include <LibWeb/DOMParsing/InnerHTML.h>
#include <LibWeb/Layout/BlockContainer.h>

namespace Web::DOM {

ShadowRoot::ShadowRoot(Document& document, Element& host)
    : DocumentFragment(document)
{
    set_host(host);
}

// https://dom.spec.whatwg.org/#ref-for-get-the-parent%E2%91%A6
EventTarget* ShadowRoot::get_parent(const Event& event)
{
    if (!event.composed()) {
        auto& events_first_invocation_target = verify_cast<Node>(*event.path().first().invocation_target);
        if (&events_first_invocation_target.root() == this)
            return nullptr;
    }

    return host();
}

// https://w3c.github.io/DOM-Parsing/#dom-innerhtml-innerhtml
String ShadowRoot::inner_html() const
{
    return serialize_fragment(/* FIXME: Providing true for the require well-formed flag (which may throw) */);
}

// https://w3c.github.io/DOM-Parsing/#dom-innerhtml-innerhtml
ExceptionOr<void> ShadowRoot::set_inner_html(String const& markup)
{
    auto result = DOMParsing::inner_html_setter(*this, markup);
    if (result.is_exception())
        return result.exception();

    set_needs_style_update(true);
    return {};
}

}
