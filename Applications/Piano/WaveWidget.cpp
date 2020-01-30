/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2019-2020, William McPherson <willmcpherson2@gmail.com>
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

#include "WaveWidget.h"
#include "AudioEngine.h"
#include <LibGUI/GPainter.h>
#include <limits>

WaveWidget::WaveWidget(GWidget* parent, AudioEngine& audio_engine)
    : GFrame(parent)
    , m_audio_engine(audio_engine)
{
    set_frame_thickness(2);
    set_frame_shadow(FrameShadow::Sunken);
    set_frame_shape(FrameShape::Container);
}

WaveWidget::~WaveWidget()
{
}

static const Color wave_colors[] = {
    // Sine
    {
        255,
        192,
        0,
    },
    // Triangle
    {
        35,
        171,
        35,
    },
    // Square
    {
        128,
        160,
        255,
    },
    // Saw
    {
        240,
        100,
        128,
    },
    // Noise
    {
        197,
        214,
        225,
    },
};

int WaveWidget::sample_to_y(int sample) const
{
    constexpr double sample_max = std::numeric_limits<i16>::max();
    double percentage = sample / sample_max;
    double portion_of_height = percentage * frame_inner_rect().height();
    int y = (frame_inner_rect().height() / 2) + portion_of_height;
    return y;
}

void WaveWidget::paint_event(GPaintEvent& event)
{
    GPainter painter(*this);
    painter.fill_rect(frame_inner_rect(), Color::Black);
    painter.translate(frame_thickness(), frame_thickness());

    Color wave_color = wave_colors[m_audio_engine.wave()];
    auto buffer = m_audio_engine.buffer();
    double width_scale = static_cast<double>(frame_inner_rect().width()) / buffer.size();

    int prev_x = 0;
    int prev_y = sample_to_y(buffer[0].left);
    painter.set_pixel({ prev_x, prev_y }, wave_color);

    for (size_t x = 1; x < buffer.size(); ++x) {
        int y = sample_to_y(buffer[x].left);

        Point point1(prev_x * width_scale, prev_y);
        Point point2(x * width_scale, y);
        painter.draw_line(point1, point2, wave_color);

        prev_x = x;
        prev_y = y;
    }

    GFrame::paint_event(event);
}
