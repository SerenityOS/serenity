/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLInputElement.h>
#include <LibWeb/Layout/LabelableNode.h>

namespace Web::Layout {

class RadioButton : public LabelableNode {
public:
    RadioButton(DOM::Document&, HTML::HTMLInputElement&, NonnullRefPtr<CSS::StyleProperties>);
    virtual ~RadioButton() override;

    virtual void paint(PaintContext&, PaintPhase) override;

    const HTML::HTMLInputElement& dom_node() const { return static_cast<const HTML::HTMLInputElement&>(LabelableNode::dom_node()); }
    HTML::HTMLInputElement& dom_node() { return static_cast<HTML::HTMLInputElement&>(LabelableNode::dom_node()); }

private:
    virtual bool wants_mouse_events() const override { return true; }
    virtual void handle_mousedown(Badge<EventHandler>, const Gfx::IntPoint&, unsigned button, unsigned modifiers) override;
    virtual void handle_mouseup(Badge<EventHandler>, const Gfx::IntPoint&, unsigned button, unsigned modifiers) override;
    virtual void handle_mousemove(Badge<EventHandler>, const Gfx::IntPoint&, unsigned buttons, unsigned modifiers) override;

    virtual void handle_associated_label_mousedown(Badge<Label>) override;
    virtual void handle_associated_label_mouseup(Badge<Label>) override;
    virtual void handle_associated_label_mousemove(Badge<Label>, bool is_inside_node_or_label) override;

    void set_checked_within_group();

    bool m_being_pressed { false };
    bool m_tracking_mouse { false };
};

}
