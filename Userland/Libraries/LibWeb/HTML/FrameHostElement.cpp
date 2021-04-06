/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
    if (auto* frame = document().frame())
        m_content_frame = Frame::create_subframe(*this, frame->main_frame());
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

void FrameHostElement::content_frame_did_load(Badge<FrameLoader>)
{
    dispatch_event(DOM::Event::create(EventNames::load));
}

}
