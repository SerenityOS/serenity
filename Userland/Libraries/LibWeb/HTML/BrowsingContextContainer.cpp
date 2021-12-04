/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
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
    if (auto* frame = document().browsing_context()) {
        VERIFY(frame->page());
        m_nested_browsing_context = BrowsingContext::create_nested(*frame->page(), *this);
        m_nested_browsing_context->set_frame_nesting_levels(frame->frame_nesting_levels());
        m_nested_browsing_context->register_frame_nesting(document().url());
    }
}

Origin BrowsingContextContainer::content_origin() const
{
    if (!m_nested_browsing_context || !m_nested_browsing_context->active_document())
        return {};
    return m_nested_browsing_context->active_document()->origin();
}

bool BrowsingContextContainer::may_access_from_origin(const Origin& origin) const
{
    if (auto* page = document().page()) {
        if (!page->is_same_origin_policy_enabled())
            return true;
    }
    return origin.is_same(content_origin());
}

const DOM::Document* BrowsingContextContainer::content_document() const
{
    return m_nested_browsing_context ? m_nested_browsing_context->active_document() : nullptr;
}

}
