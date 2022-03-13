/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Layout/RadioButton.h>
#include <LibWeb/Painting/LabelablePaintable.h>

namespace Web::Painting {

class RadioButtonPaintable final : public LabelablePaintable {
public:
    static NonnullRefPtr<RadioButtonPaintable> create(Layout::RadioButton const&);

    virtual void paint(PaintContext&, PaintPhase) const override;

    Layout::RadioButton const& layout_box() const;
    Layout::RadioButton& layout_box();

    virtual bool wants_mouse_events() const override { return true; }
    virtual void handle_mousedown(Badge<EventHandler>, Gfx::IntPoint const&, unsigned button, unsigned modifiers) override;
    virtual void handle_mouseup(Badge<EventHandler>, Gfx::IntPoint const&, unsigned button, unsigned modifiers) override;
    virtual void handle_mousemove(Badge<EventHandler>, Gfx::IntPoint const&, unsigned buttons, unsigned modifiers) override;

private:
    RadioButtonPaintable(Layout::RadioButton const&);

    virtual void handle_associated_label_mousedown(Badge<Layout::Label>) override;
    virtual void handle_associated_label_mouseup(Badge<Layout::Label>) override;
    virtual void handle_associated_label_mousemove(Badge<Layout::Label>, bool is_inside_node_or_label) override;

    void set_checked_within_group();

    bool m_being_pressed { false };
    bool m_tracking_mouse { false };
};

}
