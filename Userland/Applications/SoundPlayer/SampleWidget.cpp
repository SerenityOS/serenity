/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SampleWidget.h"
#include <AK/Math.h>
#include <LibAudio/Buffer.h>
#include <LibGUI/Painter.h>

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
            float sample = AK::fabs((float)m_buffer->samples()[sample_index].left);

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

void SampleWidget::set_buffer(RefPtr<Audio::Buffer> buffer)
{
    if (m_buffer == buffer)
        return;
    m_buffer = buffer;
    update();
}
