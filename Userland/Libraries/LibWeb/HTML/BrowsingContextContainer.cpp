/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/BrowsingContextContainer.h>
#include <LibWeb/Origin.h>
#include <LibWeb/Page/Page.h>

namespace Web::HTML {

BrowsingContextContainer::BrowsingContextContainer(DOM::Document& document, QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

BrowsingContextContainer::~BrowsingContextContainer()
{
}

void BrowsingContextContainer::inserted()
{
    HTMLElement::inserted();
    if (!is_connected())
        return;
    if (auto* browsing_context = document().browsing_context()) {
        VERIFY(browsing_context->page());
        m_nested_browsing_context = BrowsingContext::create_nested(*browsing_context->page(), *this);
        m_nested_browsing_context->set_frame_nesting_levels(browsing_context->frame_nesting_levels());
        m_nested_browsing_context->register_frame_nesting(document().url());
    }
}

// https://html.spec.whatwg.org/multipage/browsers.html#concept-bcc-content-document
const DOM::Document* BrowsingContextContainer::content_document() const
{
    // 1. If container's nested browsing context is null, then return null.
    if (m_nested_browsing_context == nullptr)
        return nullptr;

    // 2. Let context be container's nested browsing context.
    auto const& context = *m_nested_browsing_context;

    // 3. Let document be context's active document.
    auto const* document = context.active_document();

    VERIFY(document);
    VERIFY(m_document);

    // 4. If document's origin and container's node document's origin are not same origin-domain, then return null.
    if (!document->origin().is_same_origin_domain(m_document->origin()))
        return nullptr;

    // 5. Return document.
    return document;
}

DOM::Document const* BrowsingContextContainer::content_document_without_origin_check() const
{
    if (!m_nested_browsing_context)
        return nullptr;
    return m_nested_browsing_context->active_document();
}

}
