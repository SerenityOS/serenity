/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Event.h>
#include <LibGfx/Font.h>
#include <LibGfx/Painter.h>
#include <LibGfx/StylePainter.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/HTMLInputElement.h>
#include <LibWeb/Layout/CheckBox.h>
#include <LibWeb/Layout/Label.h>

namespace Web::Layout {

CheckBox::CheckBox(DOM::Document& document, HTML::HTMLInputElement& element, NonnullRefPtr<CSS::StyleProperties> style)
    : LabelableNode(document, element, move(style))
{
    set_intrinsic_width(13);
    set_intrinsic_height(13);
}

CheckBox::~CheckBox()
{
}

void CheckBox::paint(PaintContext& context, PaintPhase phase)
{
    if (!is_visible())
        return;

    LabelableNode::paint(context, phase);

    if (phase == PaintPhase::Foreground) {
        Gfx::StylePainter::paint_check_box(context.painter(), enclosing_int_rect(absolute_rect()), context.palette(), dom_node().enabled(), dom_node().checked(), m_being_pressed);
    }
}

void CheckBox::handle_mousedown(Badge<EventHandler>, const Gfx::IntPoint&, unsigned button, unsigned)
{
    if (button != GUI::MouseButton::Primary || !dom_node().enabled())
        return;

    m_being_pressed = true;
    set_needs_display();

    m_tracking_mouse = true;
    browsing_context().event_handler().set_mouse_event_tracking_layout_node(this);
}

void CheckBox::handle_mouseup(Badge<EventHandler>, const Gfx::IntPoint& position, unsigned button, unsigned)
{
    if (!m_tracking_mouse || button != GUI::MouseButton::Primary || !dom_node().enabled())
        return;

    // NOTE: Changing the checked state of the DOM node may run arbitrary JS, which could disappear this node.
    NonnullRefPtr protect = *this;

    bool is_inside_node_or_label = enclosing_int_rect(absolute_rect()).contains(position);
    if (!is_inside_node_or_label)
        is_inside_node_or_label = Label::is_inside_associated_label(*this, position);

    if (is_inside_node_or_label) {
        dom_node().did_click_checkbox({});
        dom_node().set_checked(!dom_node().checked(), HTML::HTMLInputElement::ChangeSource::User);
    }

    m_being_pressed = false;
    m_tracking_mouse = false;
    browsing_context().event_handler().set_mouse_event_tracking_layout_node(nullptr);
}

void CheckBox::handle_mousemove(Badge<EventHandler>, const Gfx::IntPoint& position, unsigned, unsigned)
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

void CheckBox::handle_associated_label_mousedown(Badge<Label>)
{
    if (!dom_node().enabled())
        return;

    m_being_pressed = true;
    set_needs_display();
}

void CheckBox::handle_associated_label_mouseup(Badge<Label>)
{
    if (!dom_node().enabled())
        return;

    // NOTE: Changing the checked state of the DOM node may run arbitrary JS, which could disappear this node.
    NonnullRefPtr protect = *this;

    dom_node().did_click_checkbox({});
    dom_node().set_checked(!dom_node().checked(), HTML::HTMLInputElement::ChangeSource::User);
    m_being_pressed = false;
}

void CheckBox::handle_associated_label_mousemove(Badge<Label>, bool is_inside_node_or_label)
{
    if (m_being_pressed == is_inside_node_or_label || !dom_node().enabled())
        return;

    m_being_pressed = is_inside_node_or_label;
    set_needs_display();
}

}
