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

#include "SoundPlayerWidget.h"
#include <AK/StringBuilder.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GMessageBox.h>
#include <LibM/math.h>

SoundPlayerWidget::SoundPlayerWidget(GWindow& window, NonnullRefPtr<AClientConnection> connection)
    : m_window(window)
    , m_connection(connection)
    , m_manager(connection)
{
    set_fill_with_background_color(true);
    set_layout(make<GBoxLayout>(Orientation::Vertical));
    layout()->set_margins({ 2, 2, 2, 2 });

    auto status_widget = GWidget::construct(this);
    status_widget->set_fill_with_background_color(true);
    status_widget->set_layout(make<GBoxLayout>(Orientation::Horizontal));

    m_elapsed = GLabel::construct(status_widget);
    m_elapsed->set_frame_shape(FrameShape::Container);
    m_elapsed->set_frame_shadow(FrameShadow::Sunken);
    m_elapsed->set_frame_thickness(2);
    m_elapsed->set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
    m_elapsed->set_preferred_size(80, 0);

    auto sample_widget_container = GWidget::construct(status_widget.ptr());
    sample_widget_container->set_layout(make<GBoxLayout>(Orientation::Horizontal));
    sample_widget_container->set_size_policy(SizePolicy::Fill, SizePolicy::Fill);

    m_sample_widget = SampleWidget::construct(sample_widget_container);

    m_remaining = GLabel::construct(status_widget);
    m_remaining->set_frame_shape(FrameShape::Container);
    m_remaining->set_frame_shadow(FrameShadow::Sunken);
    m_remaining->set_frame_thickness(2);
    m_remaining->set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
    m_remaining->set_preferred_size(80, 0);

    m_slider = Slider::construct(Orientation::Horizontal, this);
    m_slider->set_min(0);
    m_slider->set_enabled(false);
    m_slider->on_knob_released = [&](int value) { m_manager.seek(denormalize_rate(value)); };

    auto control_widget = GWidget::construct(this);
    control_widget->set_fill_with_background_color(true);
    control_widget->set_layout(make<GBoxLayout>(Orientation::Horizontal));
    control_widget->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    control_widget->set_preferred_size(0, 30);
    control_widget->layout()->set_margins({ 10, 2, 10, 2 });
    control_widget->layout()->set_spacing(10);

    m_play = GButton::construct(control_widget);
    m_play->set_icon(*m_pause_icon);
    m_play->set_enabled(false);
    m_play->on_click = [this](GButton& button) {
        button.set_icon(m_manager.toggle_pause() ? *m_play_icon : *m_pause_icon);
    };

    m_stop = GButton::construct(control_widget);
    m_stop->set_enabled(false);
    m_stop->set_icon(GraphicsBitmap::load_from_file("/res/icons/16x16/stop.png"));
    m_stop->on_click = [&](GButton&) { m_manager.stop(); };

    m_status = GLabel::construct(this);
    m_status->set_frame_shape(FrameShape::Box);
    m_status->set_frame_shadow(FrameShadow::Raised);
    m_status->set_frame_thickness(4);
    m_status->set_text_alignment(TextAlignment::CenterLeft);
    m_status->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    m_status->set_preferred_size(0, 18);
    m_status->set_text("No file open!");

    update_position(0);

    m_manager.on_update = [&]() { update_ui(); };
}

SoundPlayerWidget::~SoundPlayerWidget()
{
}

SoundPlayerWidget::Slider::~Slider()
{
}

void SoundPlayerWidget::hide_scope(bool hide)
{
    m_sample_widget->set_visible(!hide);
}

void SoundPlayerWidget::open_file(String path)
{
    if (!path.ends_with(".wav")) {
        GMessageBox::show("Selected file is not a \".wav\" file!", "Filetype error", GMessageBox::Type::Error);
        return;
    }

    OwnPtr<AWavLoader> loader = make<AWavLoader>(path);
    if (loader->has_error()) {
        GMessageBox::show(
            String::format(
                "Failed to load WAV file: %s (%s)",
                path.characters(),
                loader->error_string()),
            "Filetype error", GMessageBox::Type::Error);
        return;
    }

    m_sample_ratio = PLAYBACK_MANAGER_RATE / static_cast<float>(loader->sample_rate());

    m_slider->set_max(normalize_rate(static_cast<int>(loader->total_samples())));
    m_slider->set_enabled(true);
    m_play->set_enabled(true);
    m_stop->set_enabled(true);

    m_window.set_title(String::format("SoundPlayer - \"%s\"", loader->file()->filename().characters()));
    m_status->set_text(String::format(
        "Sample rate %uHz, %u %s, %u bits per sample",
        loader->sample_rate(),
        loader->num_channels(),
        (loader->num_channels() == 1) ? "channel" : "channels",
        loader->bits_per_sample()));

    m_manager.set_loader(move(loader));
    update_position(0);
}

int SoundPlayerWidget::normalize_rate(int rate) const
{
    return static_cast<int>(rate * m_sample_ratio);
}

int SoundPlayerWidget::denormalize_rate(int rate) const
{
    return static_cast<int>(rate / m_sample_ratio);
}

void SoundPlayerWidget::update_ui()
{
    m_sample_widget->set_buffer(m_manager.current_buffer());
    m_play->set_icon(m_manager.is_paused() ? *m_play_icon : *m_pause_icon);
    update_position(m_manager.connection()->get_played_samples());
}

void SoundPlayerWidget::update_position(const int position)
{
    int total_norm_samples = position + normalize_rate(m_manager.last_seek());
    float seconds = (total_norm_samples / static_cast<float>(PLAYBACK_MANAGER_RATE));
    float remaining_seconds = m_manager.total_length() - seconds;

    m_elapsed->set_text(String::format(
        "Elapsed:\n%u:%02u.%02u",
        static_cast<int>(seconds / 60),
        static_cast<int>(seconds) % 60,
        static_cast<int>(seconds * 100) % 100));

    m_remaining->set_text(String::format(
        "Remaining:\n%u:%02u.%02u",
        static_cast<int>(remaining_seconds / 60),
        static_cast<int>(remaining_seconds) % 60,
        static_cast<int>(remaining_seconds * 100) % 100));

    m_slider->set_value(total_norm_samples);
}
