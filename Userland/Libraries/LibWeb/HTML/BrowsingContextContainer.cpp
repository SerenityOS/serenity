/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/HTML/BrowsingContextContainer.h>
#include <LibWeb/Origin.h>
#include <LibWeb/Page/BrowsingContext.h>

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
        m_nested_browsing_context = BrowsingContext::create_nested(*this, frame->top_level_browsing_context());
        m_nested_browsing_context->set_frame_nesting_levels(frame->frame_nesting_levels());
        m_nested_browsing_context->register_frame_nesting(document().url());
    }
}

Origin BrowsingContextContainer::content_origin() const
{
    if (!m_nested_browsing_context || !m_nested_browsing_context->document())
        return {};
    return m_nested_browsing_context->document()->origin();
}

bool BrowsingContextContainer::may_access_from_origin(const Origin& origin) const
{
    return origin.is_same(content_origin());
}

const DOM::Document* BrowsingContextContainer::content_document() const
{
    return m_nested_browsing_context ? m_nested_browsing_context->document() : nullptr;
}

void BrowsingContextContainer::nested_browsing_context_did_load(Badge<FrameLoader>)
{
    dispatch_event(DOM::Event::create(EventNames::load));
}

}
