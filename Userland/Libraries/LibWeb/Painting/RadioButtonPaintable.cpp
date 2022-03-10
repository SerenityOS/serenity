/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Event.h>
#include <LibGfx/StylePainter.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/HTMLImageElement.h>
#include <LibWeb/Layout/Label.h>
#include <LibWeb/Layout/RadioButton.h>
#include <LibWeb/Painting/RadioButtonPaintable.h>

namespace Web::Painting {

NonnullRefPtr<RadioButtonPaintable> RadioButtonPaintable::create(Layout::RadioButton const& layout_box)
{
    return adopt_ref(*new RadioButtonPaintable(layout_box));
}

RadioButtonPaintable::RadioButtonPaintable(Layout::RadioButton const& layout_box)
    : LabelablePaintable(layout_box)
{
}

Layout::RadioButton const& RadioButtonPaintable::layout_box() const
{
    return static_cast<Layout::RadioButton const&>(layout_node());
}

Layout::RadioButton& RadioButtonPaintable::layout_box()
{
    return static_cast<Layout::RadioButton&>(layout_node());
}

void RadioButtonPaintable::paint(PaintContext& context, PaintPhase phase) const
{
    if (!is_visible())
        return;

    PaintableBox::paint(context, phase);

    if (phase == PaintPhase::Foreground)
        Gfx::StylePainter::paint_radio_button(context.painter(), enclosing_int_rect(absolute_rect()), context.palette(), layout_box().dom_node().checked(), m_being_pressed);
}

void RadioButtonPaintable::handle_mousedown(Badge<EventHandler>, const Gfx::IntPoint&, unsigned button, unsigned)
{
    if (button != GUI::MouseButton::Primary || !layout_box().dom_node().enabled())
        return;

    m_being_pressed = true;
    set_needs_display();

    m_tracking_mouse = true;
    browsing_context().event_handler().set_mouse_event_tracking_layout_node(&layout_box());
}

void RadioButtonPaintable::handle_mouseup(Badge<EventHandler>, const Gfx::IntPoint& position, unsigned button, unsigned)
{
    if (!m_tracking_mouse || button != GUI::MouseButton::Primary || !layout_box().dom_node().enabled())
        return;

    // NOTE: Changing the checked state of the DOM node may run arbitrary JS, which could disappear this node.
    NonnullRefPtr protect = *this;

    bool is_inside_node_or_label = enclosing_int_rect(absolute_rect()).contains(position);
    if (!is_inside_node_or_label)
        is_inside_node_or_label = Layout::Label::is_inside_associated_label(layout_box(), position);

    if (is_inside_node_or_label)
        set_checked_within_group();

    m_being_pressed = false;
    m_tracking_mouse = false;
    browsing_context().event_handler().set_mouse_event_tracking_layout_node(nullptr);
}

void RadioButtonPaintable::handle_mousemove(Badge<EventHandler>, const Gfx::IntPoint& position, unsigned, unsigned)
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

void RadioButtonPaintable::handle_associated_label_mousedown(Badge<Layout::Label>)
{
    m_being_pressed = true;
    set_needs_display();
}

void RadioButtonPaintable::handle_associated_label_mouseup(Badge<Layout::Label>)
{
    // NOTE: Changing the checked state of the DOM node may run arbitrary JS, which could disappear this node.
    NonnullRefPtr protect = *this;

    set_checked_within_group();
    m_being_pressed = false;
}

void RadioButtonPaintable::handle_associated_label_mousemove(Badge<Layout::Label>, bool is_inside_node_or_label)
{
    if (m_being_pressed == is_inside_node_or_label)
        return;

    m_being_pressed = is_inside_node_or_label;
    set_needs_display();
}

void RadioButtonPaintable::set_checked_within_group()
{
    if (layout_box().dom_node().checked())
        return;

    layout_box().dom_node().set_checked(true, HTML::HTMLInputElement::ChangeSource::User);
    String name = layout_box().dom_node().name();

    document().for_each_in_inclusive_subtree_of_type<HTML::HTMLInputElement>([&](auto& element) {
        if (element.checked() && (element.paintable() != this) && (element.name() == name))
            element.set_checked(false, HTML::HTMLInputElement::ChangeSource::User);
        return IterationDecision::Continue;
    });
}

}
