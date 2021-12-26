/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibGUI/Painter.h>
#include <LibGUI/RadioButton.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>
#include <LibGfx/StylePainter.h>

namespace GUI {

RadioButton::RadioButton(const StringView& text)
    : AbstractButton(text)
{
}

RadioButton::~RadioButton()
{
}

Gfx::IntSize RadioButton::circle_size()
{
    return { 12, 12 };
}

void RadioButton::paint_event(PaintEvent& event)
{
    Painter painter(*this);
    painter.add_clip_rect(event.rect());

    if (fill_with_background_color())
        painter.fill_rect(rect(), palette().window());

    if (is_enabled() && is_hovered())
        painter.fill_rect(rect(), palette().hover_highlight());

    Gfx::IntRect circle_rect { { 2, 0 }, circle_size() };
    circle_rect.center_vertically_within(rect());

    Gfx::StylePainter::paint_radio_button(painter, circle_rect, palette(), is_checked(), is_being_pressed());

    Gfx::IntRect text_rect { circle_rect.right() + 4, 0, font().width(text()), font().glyph_height() };
    text_rect.center_vertically_within(rect());
    paint_text(painter, text_rect, font(), Gfx::TextAlignment::TopLeft);
}

template<typename Callback>
void RadioButton::for_each_in_group(Callback callback)
{
    if (!parent())
        return;
    parent()->for_each_child_of_type<RadioButton>([&](auto& child) {
        return callback(downcast<RadioButton>(child));
    });
}

void RadioButton::click(unsigned)
{
    if (!is_enabled())
        return;
    for_each_in_group([this](auto& button) {
        if (&button != this)
            button.set_checked(false);
        return IterationDecision::Continue;
    });
    set_checked(true);
}

}
