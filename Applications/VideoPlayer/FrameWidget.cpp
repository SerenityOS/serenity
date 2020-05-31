/*
 * Copyright (c) 2020, Dominic Szablewski <dominic@phoboslab.org>
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

#include "FrameWidget.h"

#include <LibGUI/Painter.h>
#include <LibGfx/Bitmap.h>

FrameWidget::FrameWidget()
{
    m_keep_aspect_ratio = true;
    m_bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::RGB32, { 320, 240 });
}

FrameWidget::~FrameWidget()
{
}

void FrameWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    Gfx::Rect rect = event.rect();

    if (m_keep_aspect_ratio) {
        float video_aspect = (float)m_bitmap->width() / (float)m_bitmap->height();
        float widget_aspect = (float)rect.width() / (float)rect.height();
        
        if (video_aspect > widget_aspect) {
            // Full width, reduced height
            int height = (float)rect.width()/video_aspect;
            rect.set_y((rect.height() - height)/2);
            rect.set_height(height);
        }
        else if (video_aspect < widget_aspect) {
            // Full height, reduced width
            int width = (float)rect.height()*video_aspect;
            rect.set_x((rect.width() - width)/2);
            rect.set_width(width);
        }
    }
    
    painter.add_clip_rect(rect);
    painter.draw_scaled_bitmap(rect, *m_bitmap, m_bitmap->rect());
}

void FrameWidget::keep_aspect_ratio(bool keep_aspect_ratio)
{
    m_keep_aspect_ratio = keep_aspect_ratio;
}

void FrameWidget::receive_frame(plm_frame_t *frame) {
    int width = frame->width;
    int height = frame->height;
    if (m_bitmap->width() != width || m_bitmap->height() != height) {
        m_bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::RGB32, { width, height });
    }

    plm_frame_to_bgra(frame, m_bitmap->bits(0), m_bitmap->pitch());
    update();
}
