/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
