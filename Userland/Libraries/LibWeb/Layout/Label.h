/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
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

#include <LibWeb/HTML/HTMLLabelElement.h>
#include <LibWeb/Layout/BlockBox.h>

namespace Web::Layout {

class Label : public BlockBox {
public:
    Label(DOM::Document&, HTML::HTMLLabelElement*, NonnullRefPtr<CSS::StyleProperties>);
    virtual ~Label() override;

    static bool is_inside_associated_label(LabelableNode&, const Gfx::IntPoint&);
    static bool is_associated_label_hovered(LabelableNode&);

    const HTML::HTMLLabelElement& dom_node() const { return static_cast<const HTML::HTMLLabelElement&>(*BlockBox::dom_node()); }
    HTML::HTMLLabelElement& dom_node() { return static_cast<HTML::HTMLLabelElement&>(*BlockBox::dom_node()); }

    void handle_mousedown_on_label(Badge<TextNode>, const Gfx::IntPoint&, unsigned button);
    void handle_mouseup_on_label(Badge<TextNode>, const Gfx::IntPoint&, unsigned button);
    void handle_mousemove_on_label(Badge<TextNode>, const Gfx::IntPoint&, unsigned button);

private:
    static Label* label_for_control_node(LabelableNode&);
    LabelableNode* control_node();

    bool m_tracking_mouse { false };
};

}
