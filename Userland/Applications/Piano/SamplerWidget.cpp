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

    // FIXME: This widget currently can't display anything.
    return;
}

SamplerWidget::SamplerWidget(TrackManager& track_manager)
    : m_track_manager(track_manager)
{
    set_layout<GUI::VerticalBoxLayout>(10, 10);
    set_fill_with_background_color(true);

    m_open_button_and_recorded_sample_name_container = add<GUI::Widget>();
    m_open_button_and_recorded_sample_name_container->set_layout<GUI::HorizontalBoxLayout>(GUI::Margins {}, 10);
    m_open_button_and_recorded_sample_name_container->set_fixed_height(24);

    m_open_button = m_open_button_and_recorded_sample_name_container->add<GUI::Button>();
    m_open_button->set_fixed_size(24, 24);
    m_open_button->set_focus_policy(GUI::FocusPolicy::TabFocus);
    m_open_button->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/open.png"sv).release_value_but_fixme_should_propagate_errors());
    m_open_button->on_click = [this](auto) {
        Optional<ByteString> open_path = GUI::FilePicker::get_open_filepath(window());
        if (!open_path.has_value())
            return;
        // TODO: We don't actually load the sample.
        m_recorded_sample_name->set_text(String::from_byte_string(open_path.value()).release_value_but_fixme_should_propagate_errors());
        m_wave_editor->update();
    };

    m_recorded_sample_name = m_open_button_and_recorded_sample_name_container->add<GUI::Label>("No sample loaded"_string);
    m_recorded_sample_name->set_text_alignment(Gfx::TextAlignment::CenterLeft);

    m_wave_editor = add<WaveEditor>(m_track_manager);
    m_wave_editor->set_fixed_height(100);
}
