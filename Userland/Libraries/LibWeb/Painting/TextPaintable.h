/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Painting/PaintableBox.h>

namespace Web::Painting {

class TextPaintable : public Paintable {
public:
    static NonnullRefPtr<TextPaintable> create(Layout::TextNode const&);

    Layout::TextNode const& layout_node() const { return static_cast<Layout::TextNode const&>(Paintable::layout_node()); }

    virtual bool wants_mouse_events() const override;
    virtual DOM::Node* mouse_event_target() const override;
    virtual DispatchEventOfSameName handle_mousedown(Badge<EventHandler>, Gfx::IntPoint const&, unsigned button, unsigned modifiers) override;
    virtual DispatchEventOfSameName handle_mouseup(Badge<EventHandler>, Gfx::IntPoint const&, unsigned button, unsigned modifiers) override;
    virtual DispatchEventOfSameName handle_mousemove(Badge<EventHandler>, Gfx::IntPoint const&, unsigned button, unsigned modifiers) override;

private:
    explicit TextPaintable(Layout::TextNode const&);
};

}
