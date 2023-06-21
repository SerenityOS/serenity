/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/CheckBox.h>
#include <LibGUI/Painter.h>
#include <LibGfx/CharacterBitmap.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Palette.h>
#include <LibGfx/StylePainter.h>

REGISTER_WIDGET(GUI, CheckBox)

namespace GUI {

CheckBox::CheckBox(String text)
    : AbstractButton(move(text))
{
    REGISTER_BOOL_PROPERTY("autosize", is_autosize, set_autosize);

    REGISTER_ENUM_PROPERTY(
        "checkbox_position", checkbox_position, set_checkbox_position, CheckBoxPosition,
        { CheckBoxPosition::Left, "Left" },
        { CheckBoxPosition::Right, "Right" });

    set_min_size(SpecialDimension::Shrink, SpecialDimension::Shrink);
    set_preferred_size(SpecialDimension::OpportunisticGrow, SpecialDimension::Shrink);
}

int CheckBox::gap_between_box_and_rect() const
{
    return 6;
}

int CheckBox::horizontal_padding() const
{
    return 2;
}

Gfx::IntRect CheckBox::box_rect() const
{
    int box_size = max(13, height() - 10);

    Gfx::IntRect box_rect {
        0,
        height() / 2 - box_size / 2 - 1,
        box_size,
        box_size,
    };
    if (m_checkbox_position == CheckBoxPosition::Right)
        box_rect.set_right_without_resize(rect().right());

    return box_rect;
}

void CheckBox::paint_event(PaintEvent& event)
{
    Painter painter(*this);
    painter.add_clip_rect(event.rect());

    auto box_rect = this->box_rect();
    auto text_rect = rect();
    if (m_checkbox_position == CheckBoxPosition::Left)
        text_rect.set_left(box_rect.right() + gap_between_box_and_rect());
    text_rect.set_width(font().width_rounded_up(text()));
    text_rect.set_top(height() / 2 - font().pixel_size_rounded_up() / 2);
    text_rect.set_height(font().pixel_size_rounded_up());

    if (fill_with_background_color())
        painter.fill_rect(rect(), palette().window());

    if (is_enabled() && is_hovered())
        painter.fill_rect(rect(), palette().hover_highlight());

    Gfx::StylePainter::paint_check_box(painter, box_rect, palette(), is_enabled(), is_checked(), is_being_pressed());

    paint_text(painter, text_rect, font(), Gfx::TextAlignment::TopLeft);

    if (is_focused())
        painter.draw_focus_rect(text_rect.inflated(6, 6), palette().focus_outline());
}

void CheckBox::click(unsigned)
{
    if (!is_enabled())
        return;
    set_checked(!is_checked());
}

void CheckBox::set_autosize(bool autosize)
{
    if (m_autosize == autosize)
        return;
    m_autosize = autosize;
    if (m_autosize)
        size_to_fit();
}

void CheckBox::size_to_fit()
{
    set_fixed_width(box_rect().width() + gap_between_box_and_rect() + font().width_rounded_up(text()) + horizontal_padding() * 2);
}

Optional<UISize> CheckBox::calculated_min_size() const
{
    auto const& font = this->font();
    int width = box_rect().width();
    int height = max(22, max(static_cast<int>(ceilf(font.pixel_size())) + 8, box_rect().height()));
    return UISize(width, height);
}

}
