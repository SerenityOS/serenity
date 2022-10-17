/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Event.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/Layout/Label.h>
#include <LibWeb/Painting/LabelablePaintable.h>

namespace Web::Painting {

LabelablePaintable::LabelablePaintable(Layout::LabelableNode const& layout_node)
    : PaintableBox(layout_node)
{
}

void LabelablePaintable::set_being_pressed(bool being_pressed)
{
    if (m_being_pressed == being_pressed)
        return;
    m_being_pressed = being_pressed;
    set_needs_display();
}

Layout::FormAssociatedLabelableNode const& LabelablePaintable::layout_box() const
{
    return static_cast<Layout::FormAssociatedLabelableNode const&>(PaintableBox::layout_box());
}

Layout::FormAssociatedLabelableNode& LabelablePaintable::layout_box()
{
    return static_cast<Layout::FormAssociatedLabelableNode&>(PaintableBox::layout_box());
}

LabelablePaintable::DispatchEventOfSameName LabelablePaintable::handle_mousedown(Badge<EventHandler>, Gfx::IntPoint const&, unsigned button, unsigned)
{
    if (button != GUI::MouseButton::Primary || !layout_box().dom_node().enabled())
        return DispatchEventOfSameName::No;

    set_being_pressed(true);
    m_tracking_mouse = true;
    browsing_context().event_handler().set_mouse_event_tracking_layout_node(&layout_box());
    return DispatchEventOfSameName::Yes;
}

LabelablePaintable::DispatchEventOfSameName LabelablePaintable::handle_mouseup(Badge<EventHandler>, Gfx::IntPoint const& position, unsigned button, unsigned)
{
    if (!m_tracking_mouse || button != GUI::MouseButton::Primary || !layout_box().dom_node().enabled())
        return DispatchEventOfSameName::No;

    bool is_inside_node_or_label = enclosing_int_rect(absolute_rect()).contains(position);
    if (!is_inside_node_or_label)
        is_inside_node_or_label = Layout::Label::is_inside_associated_label(layout_box(), position);

    set_being_pressed(false);
    m_tracking_mouse = false;
    browsing_context().event_handler().set_mouse_event_tracking_layout_node(nullptr);
    return DispatchEventOfSameName::Yes;
}

LabelablePaintable::DispatchEventOfSameName LabelablePaintable::handle_mousemove(Badge<EventHandler>, Gfx::IntPoint const& position, unsigned, unsigned)
{
    if (!m_tracking_mouse || !layout_box().dom_node().enabled())
        return DispatchEventOfSameName::No;

    bool is_inside_node_or_label = enclosing_int_rect(absolute_rect()).contains(position);
    if (!is_inside_node_or_label)
        is_inside_node_or_label = Layout::Label::is_inside_associated_label(layout_box(), position);

    set_being_pressed(is_inside_node_or_label);
    return DispatchEventOfSameName::Yes;
}

void LabelablePaintable::handle_associated_label_mousedown(Badge<Layout::Label>)
{
    set_being_pressed(true);
}

void LabelablePaintable::handle_associated_label_mouseup(Badge<Layout::Label>)
{
    // NOTE: Handling the click may run arbitrary JS, which could disappear this node.
    NonnullRefPtr protected_this = *this;
    JS::NonnullGCPtr protected_browsing_context { browsing_context() };

    set_being_pressed(false);
}

void LabelablePaintable::handle_associated_label_mousemove(Badge<Layout::Label>, bool is_inside_node_or_label)
{
    if (being_pressed() == is_inside_node_or_label)
        return;

    set_being_pressed(is_inside_node_or_label);
}

}
