/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#pragma once

#include <LibWeb/DOM/HTMLElement.h>

namespace Web {

class HTMLIFrameElement final : public HTMLElement {
public:
    HTMLIFrameElement(Document&, const FlyString& tag_name);
    virtual ~HTMLIFrameElement() override;

    virtual RefPtr<LayoutNode> create_layout_node(const StyleProperties* parent_style) const override;

    Frame* hosted_frame() { return m_hosted_frame; }
    const Frame* hosted_frame() const { return m_hosted_frame; }

private:
    virtual void document_did_attach_to_frame(Frame&) override;
    virtual void document_will_detach_from_frame(Frame&) override;

    void load_src(const String&);

    RefPtr<Frame> m_hosted_frame;
};

template<>
inline bool is<HTMLIFrameElement>(const Node& node)
{
    return is<Element>(node) && to<Element>(node).tag_name().equals_ignoring_case("iframe");
}

}
