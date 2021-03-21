/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
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

    if (m_serenity_bg.is_null()) {
        m_serenity_bg = Gfx::Bitmap::load_from_file("/res/wallpapers/sunset-retro.png");
    }
    painter.draw_scaled_bitmap(frame_inner_rect(), *m_serenity_bg, m_serenity_bg->rect(), 1.0f);
    return;
}

void NoVisualizationWidget::set_buffer(RefPtr<Audio::Buffer>)
{
}
