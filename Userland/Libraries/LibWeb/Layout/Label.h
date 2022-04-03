/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLLabelElement.h>
#include <LibWeb/Layout/BlockContainer.h>

namespace Web::Layout {

class Label final : public BlockContainer {
public:
    Label(DOM::Document&, HTML::HTMLLabelElement*, NonnullRefPtr<CSS::StyleProperties>);
    virtual ~Label() override;

    static bool is_inside_associated_label(LabelableNode const&, Gfx::IntPoint const&);
    static bool is_associated_label_hovered(LabelableNode const&);

    const HTML::HTMLLabelElement& dom_node() const { return static_cast<const HTML::HTMLLabelElement&>(*BlockContainer::dom_node()); }
    HTML::HTMLLabelElement& dom_node() { return static_cast<HTML::HTMLLabelElement&>(*BlockContainer::dom_node()); }

    void handle_mousedown_on_label(Badge<Painting::TextPaintable>, Gfx::IntPoint const&, unsigned button);
    void handle_mouseup_on_label(Badge<Painting::TextPaintable>, Gfx::IntPoint const&, unsigned button);
    void handle_mousemove_on_label(Badge<Painting::TextPaintable>, Gfx::IntPoint const&, unsigned button);

    LabelableNode* labeled_control();

private:
    virtual bool is_label() const override { return true; }

    static Label const* label_for_control_node(LabelableNode const&);

    bool m_tracking_mouse { false };
};

template<>
inline bool Node::fast_is<Label>() const { return is_label(); }

}
