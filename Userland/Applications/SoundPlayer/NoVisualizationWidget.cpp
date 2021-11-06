/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "NoVisualizationWidget.h"
#include <LibGUI/Painter.h>

NoVisualizationWidget::NoVisualizationWidget()
{
}

NoVisualizationWidget::~NoVisualizationWidget()
{
}

void NoVisualizationWidget::paint_event(GUI::PaintEvent& event)
{
    Frame::paint_event(event);
    GUI::Painter painter(*this);

    if (!m_serenity_bg)
        m_serenity_bg = Gfx::Bitmap::try_load_from_file("/res/wallpapers/sunset-retro.png").release_value_but_fixme_should_propagate_errors();
    painter.draw_scaled_bitmap(frame_inner_rect(), *m_serenity_bg, m_serenity_bg->rect(), 1.0f);
}

void NoVisualizationWidget::set_buffer(RefPtr<Audio::Buffer>)
{
}
