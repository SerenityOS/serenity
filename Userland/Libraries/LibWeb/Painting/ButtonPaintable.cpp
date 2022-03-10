/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLImageElement.h>
#include <LibWeb/Layout/ButtonBox.h>
#include <LibWeb/Painting/ButtonPaintable.h>

namespace Web::Painting {

NonnullOwnPtr<ButtonPaintable> ButtonPaintable::create(Layout::ButtonBox const& layout_box)
{
    return adopt_own(*new ButtonPaintable(layout_box));
}

ButtonPaintable::ButtonPaintable(Layout::ButtonBox const& layout_box)
    : Paintable(layout_box)
{
}

Layout::ButtonBox const& ButtonPaintable::layout_box() const
{
    return static_cast<Layout::ButtonBox const&>(m_layout_box);
}

void ButtonPaintable::paint(PaintContext& context, PaintPhase phase) const
{
    if (!is_visible())
        return;

    Paintable::paint(context, phase);

    if (phase == PaintPhase::Foreground) {
        auto text_rect = enclosing_int_rect(absolute_rect());
        if (layout_box().being_pressed())
            text_rect.translate_by(1, 1);
        context.painter().draw_text(text_rect, layout_box().dom_node().value(), layout_box().font(), Gfx::TextAlignment::Center, computed_values().color());
    }
}

}
