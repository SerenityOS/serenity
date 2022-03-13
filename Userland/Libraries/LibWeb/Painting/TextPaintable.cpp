/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/Layout/Label.h>
#include <LibWeb/Page/EventHandler.h>
#include <LibWeb/Painting/TextPaintable.h>

namespace Web::Painting {

NonnullRefPtr<TextPaintable> TextPaintable::create(Layout::TextNode const& layout_node)
{
    return adopt_ref(*new TextPaintable(layout_node));
}

TextPaintable::TextPaintable(Layout::TextNode const& layout_node)
    : Paintable(layout_node)
{
}

bool TextPaintable::wants_mouse_events() const
{
    return layout_node().first_ancestor_of_type<Layout::Label>();
}

void TextPaintable::handle_mousedown(Badge<EventHandler>, const Gfx::IntPoint& position, unsigned button, unsigned)
{
    auto* label = layout_node().first_ancestor_of_type<Layout::Label>();
    if (!label)
        return;
    const_cast<Layout::Label*>(label)->handle_mousedown_on_label({}, position, button);
    const_cast<HTML::BrowsingContext&>(browsing_context()).event_handler().set_mouse_event_tracking_layout_node(&const_cast<Layout::TextNode&>(layout_node()));
}

void TextPaintable::handle_mouseup(Badge<EventHandler>, const Gfx::IntPoint& position, unsigned button, unsigned)
{
    auto* label = layout_node().first_ancestor_of_type<Layout::Label>();
    if (!label)
        return;

    // NOTE: Changing the state of the DOM node may run arbitrary JS, which could disappear this node.
    NonnullRefPtr protect = *this;

    const_cast<Layout::Label*>(label)->handle_mouseup_on_label({}, position, button);
    const_cast<HTML::BrowsingContext&>(browsing_context()).event_handler().set_mouse_event_tracking_layout_node(nullptr);
}

void TextPaintable::handle_mousemove(Badge<EventHandler>, const Gfx::IntPoint& position, unsigned button, unsigned)
{
    auto* label = layout_node().first_ancestor_of_type<Layout::Label>();
    if (!label)
        return;
    const_cast<Layout::Label*>(label)->handle_mousemove_on_label({}, position, button);
}

}
