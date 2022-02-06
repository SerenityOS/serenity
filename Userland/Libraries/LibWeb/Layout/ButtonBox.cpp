/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Event.h>
#include <LibGfx/Font.h>
#include <LibGfx/Painter.h>
#include <LibGfx/StylePainter.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/Layout/ButtonBox.h>
#include <LibWeb/Layout/Label.h>

namespace Web::Layout {

ButtonBox::ButtonBox(DOM::Document& document, HTML::HTMLInputElement& element, NonnullRefPtr<CSS::StyleProperties> style)
    : LabelableNode(document, element, move(style))
{
}

ButtonBox::~ButtonBox()
{
}

void ButtonBox::prepare_for_replaced_layout()
{
    set_intrinsic_width(font().width(dom_node().value()) + 20);
    set_intrinsic_height(20);
}

void ButtonBox::paint(PaintContext& context, PaintPhase phase)
{
    if (!is_visible())
        return;

    LabelableNode::paint(context, phase);

    if (phase == PaintPhase::Foreground) {
        bool hovered = document().hovered_node() == &dom_node();
        if (!hovered)
            hovered = Label::is_associated_label_hovered(*this);

        Gfx::StylePainter::paint_button(context.painter(), enclosing_int_rect(absolute_rect()), context.palette(), Gfx::ButtonStyle::Normal, m_being_pressed, hovered, dom_node().checked(), dom_node().enabled());

        auto text_rect = enclosing_int_rect(absolute_rect());
        if (m_being_pressed)
            text_rect.translate_by(1, 1);
        context.painter().draw_text(text_rect, dom_node().value(), font(), Gfx::TextAlignment::Center, context.palette().button_text());
    }
}

void ButtonBox::handle_mousedown(Badge<EventHandler>, const Gfx::IntPoint&, unsigned button, unsigned)
{
    if (button != GUI::MouseButton::Primary || !dom_node().enabled())
        return;

    m_being_pressed = true;
    set_needs_display();

    m_tracking_mouse = true;
    browsing_context().event_handler().set_mouse_event_tracking_layout_node(this);
}

void ButtonBox::handle_mouseup(Badge<EventHandler>, const Gfx::IntPoint& position, unsigned button, unsigned)
{
    if (!m_tracking_mouse || button != GUI::MouseButton::Primary || !dom_node().enabled())
        return;

    // NOTE: Handling the click may run arbitrary JS, which could disappear this node.
    NonnullRefPtr protected_this = *this;
    NonnullRefPtr protected_browsing_context = browsing_context();

    bool is_inside_node_or_label = enclosing_int_rect(absolute_rect()).contains(position);
    if (!is_inside_node_or_label)
        is_inside_node_or_label = Label::is_inside_associated_label(*this, position);

    if (is_inside_node_or_label)
        dom_node().did_click_button({});

    m_being_pressed = false;
    m_tracking_mouse = false;

    protected_browsing_context->event_handler().set_mouse_event_tracking_layout_node(nullptr);
}

void ButtonBox::handle_mousemove(Badge<EventHandler>, const Gfx::IntPoint& position, unsigned, unsigned)
{
    if (!m_tracking_mouse || !dom_node().enabled())
        return;

    bool is_inside_node_or_label = enclosing_int_rect(absolute_rect()).contains(position);
    if (!is_inside_node_or_label)
        is_inside_node_or_label = Label::is_inside_associated_label(*this, position);

    if (m_being_pressed == is_inside_node_or_label)
        return;

    m_being_pressed = is_inside_node_or_label;
    set_needs_display();
}

void ButtonBox::handle_associated_label_mousedown(Badge<Label>)
{
    m_being_pressed = true;
    set_needs_display();
}

void ButtonBox::handle_associated_label_mouseup(Badge<Label>)
{
    // NOTE: Handling the click may run arbitrary JS, which could disappear this node.
    NonnullRefPtr protected_this = *this;
    NonnullRefPtr protected_browsing_context = browsing_context();

    dom_node().did_click_button({});
    m_being_pressed = false;
}

void ButtonBox::handle_associated_label_mousemove(Badge<Label>, bool is_inside_node_or_label)
{
    if (m_being_pressed == is_inside_node_or_label)
        return;

    m_being_pressed = is_inside_node_or_label;
    set_needs_display();
}

}
