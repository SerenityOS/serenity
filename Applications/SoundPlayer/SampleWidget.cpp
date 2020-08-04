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

#include "SampleWidget.h"
#include <LibAudio/Buffer.h>
#include <LibGUI/Painter.h>
#include <math.h>

SampleWidget::SampleWidget()
{
}

SampleWidget::~SampleWidget()
{
}

void SampleWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);
    GUI::Painter painter(*this);

    painter.add_clip_rect(event.rect());
    painter.fill_rect(frame_inner_rect(), Color::Black);

    float sample_max = 0;
    int count = 0;
    int x_offset = frame_inner_rect().x();
    int x = x_offset;
    int y_offset = frame_inner_rect().center().y();

    if (m_buffer) {
        int samples_per_pixel = m_buffer->sample_count() / frame_inner_rect().width();
        for (int sample_index = 0; sample_index < m_buffer->sample_count() && (x - x_offset) < frame_inner_rect().width(); ++sample_index) {
            float sample = fabsf((float)m_buffer->samples()[sample_index].left);

            sample_max = max(sample, sample_max);
            ++count;

            if (count >= samples_per_pixel) {
                Gfx::IntPoint min_point = { x, y_offset + static_cast<int>(-sample_max * frame_inner_rect().height() / 2) };
                Gfx::IntPoint max_point = { x++, y_offset + static_cast<int>(sample_max * frame_inner_rect().height() / 2) };
                painter.draw_line(min_point, max_point, Color::Green);

                count = 0;
                sample_max = 0;
            }
        }
    } else {
        painter.draw_line({ x, y_offset }, { frame_inner_rect().width(), y_offset }, Color::Green);
    }
}

void SampleWidget::set_buffer(Audio::Buffer* buffer)
{
    if (m_buffer == buffer)
        return;
    m_buffer = buffer;
    update();
}
