/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/HTML/FrameHostElement.h>
#include <LibWeb/Origin.h>
#include <LibWeb/Page/Frame.h>

namespace Web::HTML {

FrameHostElement::FrameHostElement(DOM::Document& document, QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

FrameHostElement::~FrameHostElement()
{
}

void FrameHostElement::inserted()
{
    HTMLElement::inserted();
    if (!is_connected())
        return;
    if (auto* frame = document().frame()) {
        m_content_frame = Frame::create_subframe(*this, frame->main_frame());
        m_content_frame->set_frame_nesting_levels(frame->frame_nesting_levels());
        m_content_frame->register_frame_nesting(document().url());
    }
}

Origin FrameHostElement::content_origin() const
{
    if (!m_content_frame || !m_content_frame->document())
        return {};
    return m_content_frame->document()->origin();
}

bool FrameHostElement::may_access_from_origin(const Origin& origin) const
{
    return origin.is_same(content_origin());
}

const DOM::Document* FrameHostElement::content_document() const
{
    return m_content_frame ? m_content_frame->document() : nullptr;
}

void FrameHostElement::content_frame_did_load(Badge<Fetch::FrameLoader>)
{
    dispatch_event(DOM::Event::create(EventNames::load));
}

}
