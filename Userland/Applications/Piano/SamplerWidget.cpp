/*
 * Copyright (c) 2020, William McPherson <willmcpherson2@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SamplerWidget.h"
#include "TrackManager.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>

WaveEditor::WaveEditor(TrackManager& track_manager)
    : m_track_manager(track_manager)
{
}

int WaveEditor::sample_to_y(double percentage) const
{
    double portion_of_half_height = percentage * ((frame_inner_rect().height() - 1) / 2.0);
    double y = (frame_inner_rect().height() / 2.0) + portion_of_half_height;
    return y;
}

void WaveEditor::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);

    GUI::Painter painter(*this);
    painter.fill_rect(frame_inner_rect(), Color::Black);

    auto recorded_sample = m_track_manager.current_track().recorded_sample();
    if (recorded_sample.is_empty())
        return;

    double width_scale = static_cast<double>(frame_inner_rect().width()) / recorded_sample.size();

    painter.translate(frame_thickness(), frame_thickness());

    int prev_x = 0;
    int left_prev_y = sample_to_y(recorded_sample[0].left);
    int right_prev_y = sample_to_y(recorded_sample[0].right);
    painter.set_pixel({ prev_x, left_prev_y }, left_wave_colors[RecordedSample]);
    painter.set_pixel({ prev_x, right_prev_y }, right_wave_colors[RecordedSample]);

    for (size_t x = 1; x < recorded_sample.size(); ++x) {
        int left_y = sample_to_y(recorded_sample[x].left);
        int right_y = sample_to_y(recorded_sample[x].right);

        Gfx::IntPoint left_point1(prev_x * width_scale, left_prev_y);
        Gfx::IntPoint left_point2(x * width_scale, left_y);
        painter.draw_line(left_point1, left_point2, left_wave_colors[RecordedSample]);

        Gfx::IntPoint right_point1(prev_x * width_scale, right_prev_y);
        Gfx::IntPoint right_point2(x * width_scale, right_y);
        painter.draw_line(right_point1, right_point2, right_wave_colors[RecordedSample]);

        prev_x = x;
        left_prev_y = left_y;
        right_prev_y = right_y;
    }
}

SamplerWidget::SamplerWidget(TrackManager& track_manager)
    : m_track_manager(track_manager)
{
    set_layout<GUI::VerticalBoxLayout>();
    layout()->set_margins(10);
    layout()->set_spacing(10);
    set_fill_with_background_color(true);

    m_open_button_and_recorded_sample_name_container = add<GUI::Widget>();
    m_open_button_and_recorded_sample_name_container->set_layout<GUI::HorizontalBoxLayout>();
    m_open_button_and_recorded_sample_name_container->layout()->set_spacing(10);
    m_open_button_and_recorded_sample_name_container->set_fixed_height(24);

    m_open_button = m_open_button_and_recorded_sample_name_container->add<GUI::Button>();
    m_open_button->set_fixed_size(24, 24);
    m_open_button->set_focus_policy(GUI::FocusPolicy::TabFocus);
    m_open_button->set_icon(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/open.png").release_value_but_fixme_should_propagate_errors());
    m_open_button->on_click = [this](auto) {
        Optional<String> open_path = GUI::FilePicker::get_open_filepath(window());
        if (!open_path.has_value())
            return;
        // TODO: We don't actually load the sample.
        m_recorded_sample_name->set_text(open_path.value());
        m_wave_editor->update();
    };

    m_recorded_sample_name = m_open_button_and_recorded_sample_name_container->add<GUI::Label>("No sample loaded");
    m_recorded_sample_name->set_text_alignment(Gfx::TextAlignment::CenterLeft);

    m_wave_editor = add<WaveEditor>(m_track_manager);
    m_wave_editor->set_fixed_height(100);
}
