/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DoubleClickArrowWidget.h"
#include <LibGUI/Painter.h>
#include <LibGfx/Font.h>

REGISTER_WIDGET(MouseSettings, DoubleClickArrowWidget);

namespace MouseSettings {

DoubleClickArrowWidget::~DoubleClickArrowWidget()
{
}

void DoubleClickArrowWidget::set_double_click_speed(int speed)
{
    if (m_double_click_speed == speed)
        return;
    m_double_click_speed = speed;
    update();
}

DoubleClickArrowWidget::DoubleClickArrowWidget()
{
    m_arrow_bitmap = Gfx::Bitmap::load_from_file("/res/graphics/double-click-down-arrow.png");
}

void DoubleClickArrowWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    auto bottom_arrow_rect = m_arrow_bitmap->rect();
    bottom_arrow_rect.center_within(rect());
    bottom_arrow_rect.translate_by(0, m_arrow_bitmap->height() / 2);

    painter.blit_filtered(bottom_arrow_rect.location(), *m_arrow_bitmap, m_arrow_bitmap->rect(), [&](Color color) {
        return m_inverted ? color.inverted() : color;
    });

    auto top_arrow_rect = bottom_arrow_rect;
    top_arrow_rect.translate_by(0, -(m_double_click_speed / 50));

    painter.blit_filtered(top_arrow_rect.location(), *m_arrow_bitmap, m_arrow_bitmap->rect(), [&](Color color) {
        return m_inverted ? color.inverted() : color;
    });

    auto text_rect = rect();
    text_rect.set_y(bottom_arrow_rect.bottom());
    text_rect.set_height(font().glyph_height());
}

void DoubleClickArrowWidget::doubleclick_event(GUI::MouseEvent&)
{
    m_inverted = !m_inverted;
    update();
}

}
