/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SampleWidget.h"
#include <LibGUI/Painter.h>

SampleWidget::SampleWidget()
{
    MUST(set_render_sample_count(512));
}

void SampleWidget::render(GUI::PaintEvent& event, FixedArray<float> const& samples)
{
    GUI::Frame::paint_event(event);
    GUI::Painter painter(*this);

    painter.add_clip_rect(event.rect());
    painter.fill_rect(frame_inner_rect(), Color::Black);

    int x_offset = frame_inner_rect().x();
    int x = x_offset;
    int y_offset = frame_inner_rect().center().y();

    if (samples.size() > 0) {
        float samples_per_pixel = samples.size() / static_cast<float>(frame_inner_rect().width());
        float sample_index = 0;

        while (sample_index < samples.size()) {
            float sample = AK::abs(samples[sample_index]);
            for (size_t i = 1; i < static_cast<size_t>(samples_per_pixel + 0.5f); i++)
                sample = max(sample, AK::abs(samples[sample_index]));

            Gfx::IntPoint min_point = { x, y_offset + static_cast<int>(-sample * frame_inner_rect().height() / 2) };
            Gfx::IntPoint max_point = { x, y_offset + static_cast<int>(sample * frame_inner_rect().height() / 2) };
            painter.draw_line(min_point, max_point, Color::Green);
            sample_index += samples_per_pixel;
            x++;
        }
    } else {
        painter.draw_line({ x, y_offset }, { frame_inner_rect().width(), y_offset }, Color::Green);
    }
}
