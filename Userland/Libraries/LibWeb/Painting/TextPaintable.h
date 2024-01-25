/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Painting/PaintableBox.h>

namespace Web::Painting {

class TextPaintable final : public Paintable {
    JS_CELL(TextPaintable, Paintable);

public:
    static JS::NonnullGCPtr<TextPaintable> create(Layout::TextNode const&);

    Layout::TextNode const& layout_node() const { return static_cast<Layout::TextNode const&>(Paintable::layout_node()); }

    virtual bool wants_mouse_events() const override;
    virtual DOM::Node* mouse_event_target() const override;
    virtual DispatchEventOfSameName handle_mousedown(Badge<EventHandler>, CSSPixelPoint, unsigned button, unsigned modifiers) override;
    virtual DispatchEventOfSameName handle_mouseup(Badge<EventHandler>, CSSPixelPoint, unsigned button, unsigned modifiers) override;
    virtual DispatchEventOfSameName handle_mousemove(Badge<EventHandler>, CSSPixelPoint, unsigned button, unsigned modifiers) override;

    void set_text_decoration_thickness(CSSPixels thickness) { m_text_decoration_thickness = thickness; }
    CSSPixels text_decoration_thickness() const { return m_text_decoration_thickness; }

private:
    explicit TextPaintable(Layout::TextNode const&);

    CSSPixels m_text_decoration_thickness { 0 };
};

}
