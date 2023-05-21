/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mathias Jakobsen <mathias@jbcoding.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DoubleClickArrowWidget.h"
#include <LibGUI/Painter.h>
#include <LibGfx/Font/Font.h>

REGISTER_WIDGET(MouseSettings, DoubleClickArrowWidget);

namespace MouseSettings {

void DoubleClickArrowWidget::set_double_click_speed(int speed)
{
    if (m_double_click_speed == speed)
        return;
    m_double_click_speed = speed;
    update();
}

DoubleClickArrowWidget::DoubleClickArrowWidget()
{
    m_arrow_bitmap = Gfx::Bitmap::load_from_file("/res/graphics/double-click-down-arrow.png"sv).release_value_but_fixme_should_propagate_errors();
}

void DoubleClickArrowWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    auto bottom_arrow_rect = m_arrow_bitmap->rect().centered_within(rect()).translated(0, m_arrow_bitmap->height() / 2);

    painter.blit_filtered(bottom_arrow_rect.location(), *m_arrow_bitmap, m_arrow_bitmap->rect(), [&](Color color) {
        return m_inverted ? color.inverted() : color;
    });

    auto top_arrow_rect = bottom_arrow_rect;
    top_arrow_rect.translate_by(0, -(m_double_click_speed / 50));

    painter.blit_filtered(top_arrow_rect.location(), *m_arrow_bitmap, m_arrow_bitmap->rect(), [&](Color color) {
        return m_inverted ? color.inverted() : color;
    });

    auto text_rect = rect();
    text_rect.set_y(bottom_arrow_rect.bottom() - 1);
    text_rect.set_height(font().pixel_size_rounded_up());
}

void DoubleClickArrowWidget::mousedown_event(GUI::MouseEvent&)
{
    auto double_click_in_progress = m_double_click_timer.is_valid();
    auto elapsed_ms = double_click_in_progress ? m_double_click_timer.elapsed() : 0;

    if (!double_click_in_progress || elapsed_ms > m_double_click_speed) {
        m_double_click_timer.start();
        return;
    }

    dbgln("Double-click in {}ms", elapsed_ms);
    m_inverted = !m_inverted;
    update();

    m_double_click_timer.reset();
}

}
