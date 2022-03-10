/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/StylePainter.h>
#include <LibWeb/HTML/HTMLImageElement.h>
#include <LibWeb/Layout/RadioButton.h>
#include <LibWeb/Painting/RadioButtonPaintable.h>

namespace Web::Painting {

NonnullOwnPtr<RadioButtonPaintable> RadioButtonPaintable::create(Layout::RadioButton const& layout_box)
{
    return adopt_own(*new RadioButtonPaintable(layout_box));
}

RadioButtonPaintable::RadioButtonPaintable(Layout::RadioButton const& layout_box)
    : PaintableBox(layout_box)
{
}

Layout::RadioButton const& RadioButtonPaintable::layout_box() const
{
    return static_cast<Layout::RadioButton const&>(layout_node());
}

void RadioButtonPaintable::paint(PaintContext& context, PaintPhase phase) const
{
    if (!is_visible())
        return;

    PaintableBox::paint(context, phase);

    if (phase == PaintPhase::Foreground)
        Gfx::StylePainter::paint_radio_button(context.painter(), enclosing_int_rect(absolute_rect()), context.palette(), layout_box().dom_node().checked(), layout_box().being_pressed());
}

}
