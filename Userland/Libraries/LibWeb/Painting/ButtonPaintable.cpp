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

JS::NonnullGCPtr<ButtonPaintable> ButtonPaintable::create(Layout::ButtonBox const& layout_box)
{
    return layout_box.heap().allocate_without_realm<ButtonPaintable>(layout_box);
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

    auto const& dom_node = layout_box().dom_node();
    if (is<HTML::HTMLInputElement>(dom_node) && phase == PaintPhase::Foreground) {
        auto text_rect = context.enclosing_device_rect(absolute_rect());
        if (being_pressed()) {
            auto offset = context.rounded_device_pixels(1);
            text_rect.translate_by(offset, offset);
        }
        context.painter().draw_text(text_rect.to_type<int>(), static_cast<HTML::HTMLInputElement const&>(dom_node).value(), layout_box().font(), Gfx::TextAlignment::Center, computed_values().color());
    }
}

}
