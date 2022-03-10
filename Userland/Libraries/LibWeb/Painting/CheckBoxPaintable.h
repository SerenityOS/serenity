/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Layout/CheckBox.h>
#include <LibWeb/Painting/LabelablePaintable.h>

namespace Web::Painting {

class CheckBoxPaintable final : public LabelablePaintable {
public:
    static NonnullRefPtr<CheckBoxPaintable> create(Layout::CheckBox const&);

    virtual void paint(PaintContext&, PaintPhase) const override;

    Layout::CheckBox const& layout_box() const;
    Layout::CheckBox& layout_box();

    virtual bool wants_mouse_events() const override { return true; }
    virtual void handle_mousedown(Badge<EventHandler>, Gfx::IntPoint const&, unsigned button, unsigned modifiers) override;
    virtual void handle_mouseup(Badge<EventHandler>, Gfx::IntPoint const&, unsigned button, unsigned modifiers) override;
    virtual void handle_mousemove(Badge<EventHandler>, Gfx::IntPoint const&, unsigned buttons, unsigned modifiers) override;

private:
    CheckBoxPaintable(Layout::CheckBox const&);

    virtual void handle_associated_label_mousedown(Badge<Layout::Label>) override;
    virtual void handle_associated_label_mouseup(Badge<Layout::Label>) override;
    virtual void handle_associated_label_mousemove(Badge<Layout::Label>, bool is_inside_node_or_label) override;

    bool m_being_pressed { false };
    bool m_tracking_mouse { false };
};

}
