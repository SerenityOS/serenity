/*
 * Copyright (c) 2020-2020, William McPherson <willmcpherson2@gmail.com>
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

#include "SamplerWidget.h"
#include "AudioEngine.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>

WaveEditor::WaveEditor(AudioEngine& audio_engine)
    : m_audio_engine(audio_engine)
{
}

WaveEditor::~WaveEditor()
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

    auto recorded_sample = m_audio_engine.recorded_sample();
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

        Gfx::Point left_point1(prev_x * width_scale, left_prev_y);
        Gfx::Point left_point2(x * width_scale, left_y);
        painter.draw_line(left_point1, left_point2, left_wave_colors[RecordedSample]);

        Gfx::Point right_point1(prev_x * width_scale, right_prev_y);
        Gfx::Point right_point2(x * width_scale, right_y);
        painter.draw_line(right_point1, right_point2, right_wave_colors[RecordedSample]);

        prev_x = x;
        left_prev_y = left_y;
        right_prev_y = right_y;
    }
}

SamplerWidget::SamplerWidget(AudioEngine& audio_engine)
    : m_audio_engine(audio_engine)
{
    set_layout(make<GUI::VerticalBoxLayout>());
    layout()->set_margins({ 10, 10, 10, 10 });
    layout()->set_spacing(10);
    set_fill_with_background_color(true);

    m_open_button_and_recorded_sample_name_container = add<GUI::Widget>();
    m_open_button_and_recorded_sample_name_container->set_layout(make<GUI::HorizontalBoxLayout>());
    m_open_button_and_recorded_sample_name_container->layout()->set_spacing(10);
    m_open_button_and_recorded_sample_name_container->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    m_open_button_and_recorded_sample_name_container->set_preferred_size(0, 24);

    m_open_button = m_open_button_and_recorded_sample_name_container->add<GUI::Button>();
    m_open_button->set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
    m_open_button->set_preferred_size(24, 24);
    m_open_button->set_focusable(false);
    m_open_button->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/open.png"));
    m_open_button->on_click = [this](const auto&) {
        Optional<String> open_path = GUI::FilePicker::get_open_filepath();
        if (!open_path.has_value())
            return;
        String error_string = m_audio_engine.set_recorded_sample(open_path.value());
        if (!error_string.is_empty()) {
            GUI::MessageBox::show(String::format("Failed to load WAV file: %s", error_string.characters()), "Error", GUI::MessageBox::Type::Error);
            return;
        }
        m_recorded_sample_name->set_text(open_path.value());
        m_wave_editor->update();
    };

    m_recorded_sample_name = m_open_button_and_recorded_sample_name_container->add<GUI::Label>("No sample loaded");
    m_recorded_sample_name->set_text_alignment(Gfx::TextAlignment::CenterLeft);

    m_wave_editor = add<WaveEditor>(m_audio_engine);
    m_wave_editor->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    m_wave_editor->set_preferred_size(0, 100);
}

SamplerWidget::~SamplerWidget()
{
}
