/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Event.h>
#include <LibGfx/StylePainter.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/HTMLInputElement.h>
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

void RadioButtonPaintable::paint(PaintContext& context, PaintPhase phase) const
{
    if (!is_visible())
        return;

    PaintableBox::paint(context, phase);

    auto const& radio_box = static_cast<HTML::HTMLInputElement const&>(layout_box().dom_node());
    if (phase == PaintPhase::Foreground)
        Gfx::StylePainter::paint_radio_button(context.painter(), enclosing_int_rect(absolute_rect()), context.palette(), radio_box.checked(), being_pressed());
}

}
