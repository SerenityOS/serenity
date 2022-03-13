/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Event.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/HTMLImageElement.h>
#include <LibWeb/Layout/ButtonBox.h>
#include <LibWeb/Layout/Label.h>
#include <LibWeb/Painting/ButtonPaintable.h>

namespace Web::Painting {

NonnullRefPtr<ButtonPaintable> ButtonPaintable::create(Layout::ButtonBox const& layout_box)
{
    return adopt_ref(*new ButtonPaintable(layout_box));
}

ButtonPaintable::ButtonPaintable(Layout::ButtonBox const& layout_box)
    : LabelablePaintable(layout_box)
{
}

Layout::ButtonBox const& ButtonPaintable::layout_box() const
{
    return static_cast<Layout::ButtonBox const&>(layout_node());
}

Layout::ButtonBox& ButtonPaintable::layout_box()
{
    return static_cast<Layout::ButtonBox&>(layout_node());
}

void ButtonPaintable::paint(PaintContext& context, PaintPhase phase) const
{
    if (!is_visible())
        return;

    PaintableBox::paint(context, phase);

    if (phase == PaintPhase::Foreground) {
        auto text_rect = enclosing_int_rect(absolute_rect());
        if (m_being_pressed)
            text_rect.translate_by(1, 1);
        context.painter().draw_text(text_rect, layout_box().dom_node().value(), layout_box().font(), Gfx::TextAlignment::Center, computed_values().color());
    }
}

void ButtonPaintable::handle_mousedown(Badge<EventHandler>, const Gfx::IntPoint&, unsigned button, unsigned)
{
    if (button != GUI::MouseButton::Primary || !layout_box().dom_node().enabled())
        return;

    m_being_pressed = true;
    set_needs_display();

    m_tracking_mouse = true;
    browsing_context().event_handler().set_mouse_event_tracking_layout_node(&layout_box());
}

void ButtonPaintable::handle_mouseup(Badge<EventHandler>, const Gfx::IntPoint& position, unsigned button, unsigned)
{
    if (!m_tracking_mouse || button != GUI::MouseButton::Primary || !layout_box().dom_node().enabled())
        return;

    // NOTE: Handling the click may run arbitrary JS, which could disappear this node.
    NonnullRefPtr protected_this = *this;
    NonnullRefPtr protected_browsing_context = browsing_context();

    bool is_inside_node_or_label = enclosing_int_rect(absolute_rect()).contains(position);
    if (!is_inside_node_or_label)
        is_inside_node_or_label = Layout::Label::is_inside_associated_label(layout_box(), position);

    if (is_inside_node_or_label)
        const_cast<Layout::ButtonBox&>(layout_box()).dom_node().did_click_button({});

    m_being_pressed = false;
    m_tracking_mouse = false;

    protected_browsing_context->event_handler().set_mouse_event_tracking_layout_node(nullptr);
}

void ButtonPaintable::handle_mousemove(Badge<EventHandler>, const Gfx::IntPoint& position, unsigned, unsigned)
{
    if (!m_tracking_mouse || !layout_box().dom_node().enabled())
        return;

    bool is_inside_node_or_label = enclosing_int_rect(absolute_rect()).contains(position);
    if (!is_inside_node_or_label)
        is_inside_node_or_label = Layout::Label::is_inside_associated_label(layout_box(), position);

    if (m_being_pressed == is_inside_node_or_label)
        return;

    m_being_pressed = is_inside_node_or_label;
    set_needs_display();
}

void ButtonPaintable::handle_associated_label_mousedown(Badge<Layout::Label>)
{
    m_being_pressed = true;
    set_needs_display();
}

void ButtonPaintable::handle_associated_label_mouseup(Badge<Layout::Label>)
{
    // NOTE: Handling the click may run arbitrary JS, which could disappear this node.
    NonnullRefPtr protected_this = *this;
    NonnullRefPtr protected_browsing_context = browsing_context();

    layout_box().dom_node().did_click_button({});
    m_being_pressed = false;
}

void ButtonPaintable::handle_associated_label_mousemove(Badge<Layout::Label>, bool is_inside_node_or_label)
{
    if (m_being_pressed == is_inside_node_or_label)
        return;

    m_being_pressed = is_inside_node_or_label;
    set_needs_display();
}

}
