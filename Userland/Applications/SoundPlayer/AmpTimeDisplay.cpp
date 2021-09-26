/*
 * Copyright (c) 2021, Leandro A. F. Pereira <leandro@tia.mat.br>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AmpTimeDisplay.h"
#include <LibGUI/Painter.h>

AmpTimeDisplay::AmpTimeDisplay(const Skin& skin)
    : m_skin(skin)
    , m_minutes(00)
    , m_seconds(01)
    , m_digits_visible { false }
{
    set_relative_rect({ 0, 0, 4 * (9 + 2) + 3, 13 });
}

void AmpTimeDisplay::set_minutes(int minutes)
{
    m_minutes = minutes % 100;
    update();
}

void AmpTimeDisplay::set_seconds(int seconds)
{
    m_seconds = seconds % 100;
    update();
}

void AmpTimeDisplay::set_digits_visible(bool visible)
{
    m_digits_visible = visible;
    update();
}

void AmpTimeDisplay::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    auto paint_digit = [&](int digit, int value) {
        static const int digit_x[] = { 0, 12, 26, 38 };
        painter.blit({ digit_x[digit], 0 }, *m_skin.numbers(), { value * 9, 0, 9, 13 });
    };

    if (m_digits_visible) {
        paint_digit(0, m_minutes / 10);
        paint_digit(1, m_minutes % 10);
        paint_digit(2, m_seconds / 10);
        paint_digit(3, m_seconds % 10);
    } else {
        paint_digit(0, 10);
        paint_digit(1, 10);
        paint_digit(2, 10);
        paint_digit(3, 10);
    }
}
