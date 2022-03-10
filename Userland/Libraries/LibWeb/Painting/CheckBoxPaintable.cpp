/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Event.h>
#include <LibGfx/StylePainter.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/HTMLImageElement.h>
#include <LibWeb/Layout/CheckBox.h>
#include <LibWeb/Layout/Label.h>
#include <LibWeb/Painting/CheckBoxPaintable.h>

namespace Web::Painting {

NonnullRefPtr<CheckBoxPaintable> CheckBoxPaintable::create(Layout::CheckBox const& layout_box)
{
    return adopt_ref(*new CheckBoxPaintable(layout_box));
}

CheckBoxPaintable::CheckBoxPaintable(Layout::CheckBox const& layout_box)
    : LabelablePaintable(layout_box)
{
}

Layout::CheckBox const& CheckBoxPaintable::layout_box() const
{
    return static_cast<Layout::CheckBox const&>(layout_node());
}

Layout::CheckBox& CheckBoxPaintable::layout_box()
{
    return static_cast<Layout::CheckBox&>(layout_node());
}

void CheckBoxPaintable::paint(PaintContext& context, PaintPhase phase) const
{
    if (!is_visible())
        return;

    PaintableBox::paint(context, phase);

    if (phase == PaintPhase::Foreground)
        Gfx::StylePainter::paint_check_box(context.painter(), enclosing_int_rect(absolute_rect()), context.palette(), layout_box().dom_node().enabled(), layout_box().dom_node().checked(), m_being_pressed);
}

void CheckBoxPaintable::handle_mousedown(Badge<EventHandler>, Gfx::IntPoint const&, unsigned button, unsigned)
{
    if (button != GUI::MouseButton::Primary || !layout_box().dom_node().enabled())
        return;

    m_being_pressed = true;
    set_needs_display();

    m_tracking_mouse = true;
    browsing_context().event_handler().set_mouse_event_tracking_layout_node(&layout_box());
}

void CheckBoxPaintable::handle_mouseup(Badge<EventHandler>, const Gfx::IntPoint& position, unsigned button, unsigned)
{
    if (!m_tracking_mouse || button != GUI::MouseButton::Primary || !layout_box().dom_node().enabled())
        return;

    // NOTE: Changing the checked state of the DOM node may run arbitrary JS, which could disappear this node.
    NonnullRefPtr protect = *this;

    bool is_inside_node_or_label = enclosing_int_rect(absolute_rect()).contains(position);
    if (!is_inside_node_or_label)
        is_inside_node_or_label = Layout::Label::is_inside_associated_label(layout_box(), position);

    if (is_inside_node_or_label) {
        layout_box().dom_node().did_click_checkbox({});
        layout_box().dom_node().set_checked(!layout_box().dom_node().checked(), HTML::HTMLInputElement::ChangeSource::User);
    }

    m_being_pressed = false;
    m_tracking_mouse = false;
    browsing_context().event_handler().set_mouse_event_tracking_layout_node(nullptr);
}

void CheckBoxPaintable::handle_mousemove(Badge<EventHandler>, const Gfx::IntPoint& position, unsigned, unsigned)
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

void CheckBoxPaintable::handle_associated_label_mousedown(Badge<Layout::Label>)
{
    if (!layout_box().dom_node().enabled())
        return;

    m_being_pressed = true;
    set_needs_display();
}

void CheckBoxPaintable::handle_associated_label_mouseup(Badge<Layout::Label>)
{
    if (!layout_box().dom_node().enabled())
        return;

    // NOTE: Changing the checked state of the DOM node may run arbitrary JS, which could disappear this node.
    NonnullRefPtr protect = *this;

    layout_box().dom_node().did_click_checkbox({});
    layout_box().dom_node().set_checked(!layout_box().dom_node().checked(), HTML::HTMLInputElement::ChangeSource::User);
    m_being_pressed = false;
}

void CheckBoxPaintable::handle_associated_label_mousemove(Badge<Layout::Label>, bool is_inside_node_or_label)
{
    if (m_being_pressed == is_inside_node_or_label || !layout_box().dom_node().enabled())
        return;

    m_being_pressed = is_inside_node_or_label;
    set_needs_display();
}

}
