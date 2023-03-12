/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2019-2020, William McPherson <willmcpherson2@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WaveWidget.h"
#include "TrackManager.h"
#include <AK/NumericLimits.h>
#include <AK/StdLibExtras.h>
#include <LibGUI/Painter.h>

WaveWidget::WaveWidget(TrackManager& track_manager)
    : m_track_manager(track_manager)
{
}

int WaveWidget::sample_to_y(float sample, float sample_max) const
{
    if (sample_max < 1.0f)
        sample_max = 1.0f;
    sample_max *= rescale_factor;
    double percentage = static_cast<double>(sample) / static_cast<double>(sample_max);
    double portion_of_half_height = percentage * ((frame_inner_rect().height() - 1) / 2.0);
    double y = (frame_inner_rect().height() / 2.0) + portion_of_half_height;
    return y;
}

void WaveWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.fill_rect(frame_inner_rect(), Color::Black);
    painter.translate(frame_thickness(), frame_thickness());

    Color left_wave_color = left_wave_colors[m_track_manager.current_track()->synth()->wave()];
    Color right_wave_color = right_wave_colors[m_track_manager.current_track()->synth()->wave()];
    m_track_manager.current_track()->write_cached_signal_to(m_samples.span());
    double width_scale = static_cast<double>(frame_inner_rect().width()) / m_samples.size();

    auto const maximum = Audio::Sample::max_range(m_samples.span());
    int prev_x = 0;
    int prev_y_left = sample_to_y(m_samples[0].left, maximum.left);
    int prev_y_right = sample_to_y(m_samples[0].right, maximum.right);
    painter.set_pixel({ prev_x, prev_y_left }, left_wave_color);
    painter.set_pixel({ prev_x, prev_y_right }, right_wave_color);

    for (size_t x = 1; x < m_samples.size(); ++x) {
        int y_left = sample_to_y(m_samples[x].left, maximum.left);
        int y_right = sample_to_y(m_samples[x].right, maximum.right);

        Gfx::IntPoint point1_left(prev_x * width_scale, prev_y_left);
        Gfx::IntPoint point2_left(x * width_scale, y_left);
        painter.draw_line(point1_left, point2_left, left_wave_color);

        Gfx::IntPoint point1_right(prev_x * width_scale, prev_y_right);
        Gfx::IntPoint point2_right(x * width_scale, y_right);
        painter.draw_line(point1_right, point2_right, right_wave_color);

        prev_x = x;
        prev_y_left = y_left;
        prev_y_right = y_right;
    }

    GUI::Frame::paint_event(event);
}
