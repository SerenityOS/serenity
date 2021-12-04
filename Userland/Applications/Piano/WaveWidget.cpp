/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2019-2020, William McPherson <willmcpherson2@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "WaveWidget.h"
#include "LibDSP/Synthesizers.h"
#include "Music.h"
#include <AK/NumericLimits.h>
#include <LibDSP/TrackManager.h>
#include <LibGUI/Painter.h>

WaveWidget::WaveWidget(NonnullRefPtr<LibDSP::TrackManager> track_manager, NonnullRefPtr<LibDSP::Transport> transport, NonnullRefPtr<RollWidget> roll_widget)
    : m_roll_widget(move(roll_widget))
    , m_track_manager(move(track_manager))
    , m_transport(move(transport))
{
}

WaveWidget::~WaveWidget()
{
}

int WaveWidget::sample_to_y(int sample) const
{
    constexpr int nice_scale_factor = 4;
    sample *= nice_scale_factor;
    constexpr double sample_max = NumericLimits<i16>::max();
    double percentage = sample / sample_max;
    double portion_of_half_height = percentage * ((frame_inner_rect().height() - 1) / 2.0);
    double y = (frame_inner_rect().height() / 2.0) + portion_of_half_height;
    return y;
}

void WaveWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.fill_rect(frame_inner_rect(), Color::Black);
    painter.translate(frame_thickness(), frame_thickness());

    Color left_wave_color = left_wave_colors[synth_wave_index()];
    Color right_wave_color = right_wave_colors[synth_wave_index()];
    auto buffer = m_track_manager->front_buffer();
    double width_scale = static_cast<double>(frame_inner_rect().width()) / buffer.size();

    int prev_x = 0;
    int prev_y_left = sample_to_y(buffer[0].left);
    int prev_y_right = sample_to_y(buffer[0].right);
    painter.set_pixel({ prev_x, prev_y_left }, left_wave_color);
    painter.set_pixel({ prev_x, prev_y_right }, right_wave_color);

    for (size_t x = 1; x < buffer.size(); ++x) {
        int y_left = sample_to_y(buffer[x].left);
        int y_right = sample_to_y(buffer[x].right);

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

size_t WaveWidget::synth_wave_index()
{
    // FIXME: We assume that the current track has a synth processor.
    auto const& synth = dynamic_cast<LibDSP::Synthesizers::Classic const&>(m_roll_widget->current_track()->processor_chain()[0]);
    return synth.wave();
}
