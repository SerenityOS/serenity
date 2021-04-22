/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/Text.h>
#include <LibWeb/Layout/Node.h>

namespace Web::Layout {

class LineBoxFragment;

class TextNode : public Node {
public:
    TextNode(DOM::Document&, DOM::Text&);
    virtual ~TextNode() override;

    const DOM::Text& dom_node() const { return static_cast<const DOM::Text&>(*Node::dom_node()); }

    const String& text_for_rendering() const { return m_text_for_rendering; }

    virtual void paint_fragment(PaintContext&, const LineBoxFragment&, PaintPhase) const override;

    virtual void split_into_lines(InlineFormattingContext&, LayoutMode) override;

private:
    virtual bool is_text_node() const final { return true; }
    virtual bool wants_mouse_events() const override;
    virtual void handle_mousedown(Badge<EventHandler>, const Gfx::IntPoint&, unsigned button, unsigned modifiers) override;
    virtual void handle_mouseup(Badge<EventHandler>, const Gfx::IntPoint&, unsigned button, unsigned modifiers) override;
    virtual void handle_mousemove(Badge<EventHandler>, const Gfx::IntPoint&, unsigned button, unsigned modifiers) override;
    void split_into_lines_by_rules(InlineFormattingContext&, LayoutMode, bool do_collapse, bool do_wrap_lines, bool do_wrap_breaks);
    void paint_cursor_if_needed(PaintContext&, const LineBoxFragment&) const;

    template<typename Callback>
    void for_each_chunk(Callback, LayoutMode, bool do_wrap_lines, bool do_wrap_breaks) const;

    String m_text_for_rendering;
};

template<>
inline bool Node::fast_is<TextNode>() const { return is_text_node(); }

}
