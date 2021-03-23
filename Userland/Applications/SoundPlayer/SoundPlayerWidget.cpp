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
#include "Common.h"
#include <AK/StringBuilder.h>
#include <LibCore/MimeData.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>

SoundPlayerWidget::SoundPlayerWidget(GUI::Window& window, PlayerState& state)
    : Player(state)
    , m_window(window)
{
    window.set_resizable(false);
    window.resize(350, 140);

    set_fill_with_background_color(true);
    set_layout<GUI::VerticalBoxLayout>();
    layout()->set_margins({ 2, 2, 2, 2 });

    auto& status_widget = add<GUI::Widget>();
    status_widget.set_fill_with_background_color(true);
    status_widget.set_layout<GUI::HorizontalBoxLayout>();

    m_elapsed = status_widget.add<GUI::Label>();
    m_elapsed->set_frame_shape(Gfx::FrameShape::Container);
    m_elapsed->set_frame_shadow(Gfx::FrameShadow::Sunken);
    m_elapsed->set_frame_thickness(2);
    m_elapsed->set_fixed_width(80);

    auto& sample_widget_container = status_widget.add<GUI::Widget>();
    sample_widget_container.set_layout<GUI::HorizontalBoxLayout>();

    m_sample_widget = sample_widget_container.add<SampleWidget>();

    m_remaining = status_widget.add<GUI::Label>();
    m_remaining->set_frame_shape(Gfx::FrameShape::Container);
    m_remaining->set_frame_shadow(Gfx::FrameShadow::Sunken);
    m_remaining->set_frame_thickness(2);
    m_remaining->set_fixed_width(80);

    m_slider = add<Slider>(Orientation::Horizontal);
    m_slider->set_min(0);
    m_slider->set_enabled(has_loaded_file());
    m_slider->on_knob_released = [&](int value) { manager().seek(denormalize_rate(value)); };

    auto& control_widget = add<GUI::Widget>();
    control_widget.set_fill_with_background_color(true);
    control_widget.set_layout<GUI::HorizontalBoxLayout>();
    control_widget.set_fixed_height(30);
    control_widget.layout()->set_margins({ 10, 2, 10, 2 });
    control_widget.layout()->set_spacing(10);

    m_play = control_widget.add<GUI::Button>();
    m_play->set_icon(has_loaded_file() ? *m_play_icon : *m_pause_icon);
    m_play->set_enabled(has_loaded_file());
    m_play->on_click = [this](auto) {
        bool paused = manager().toggle_pause();
        set_paused(paused);
        m_play->set_icon(paused ? *m_play_icon : *m_pause_icon);
    };

    m_stop = control_widget.add<GUI::Button>();
    m_stop->set_enabled(has_loaded_file());
    m_stop->set_icon(Gfx::Bitmap::load_from_file("/res/icons/16x16/stop.png"));
    m_stop->on_click = [this](auto) {
        manager().stop();
        set_stopped(true);
    };

    m_status = add<GUI::Label>();
    m_status->set_frame_shape(Gfx::FrameShape::Box);
    m_status->set_frame_shadow(Gfx::FrameShadow::Raised);
    m_status->set_frame_thickness(4);
    m_status->set_text_alignment(Gfx::TextAlignment::CenterLeft);
    m_status->set_fixed_height(18);
    m_status->set_text(has_loaded_file() ? loaded_filename() : "No file open!");

    update_position(0);

    manager().on_update = [&]() { update_ui(); };
}

SoundPlayerWidget::~SoundPlayerWidget()
{
}

void SoundPlayerWidget::open_file(StringView path)
{
    NonnullRefPtr<Audio::Loader> loader = Audio::Loader::create(path);
    if (loader->has_error() || !loader->sample_rate()) {
        const String error_string = loader->error_string();
        GUI::MessageBox::show(window(),
            String::formatted("Failed to load audio file: {} ({})", path, error_string.is_null() ? "Unknown error" : error_string),
            "Filetype error", GUI::MessageBox::Type::Error);
        return;
    }

    m_sample_ratio = PLAYBACK_MANAGER_RATE / static_cast<float>(loader->sample_rate());

    m_slider->set_max(normalize_rate(static_cast<int>(loader->total_samples())));
    m_slider->set_enabled(true);
    m_play->set_enabled(true);
    m_stop->set_enabled(true);

    m_window.set_title(String::formatted("{} - SoundPlayer", loader->file()->filename()));
    m_status->set_text(String::formatted(
        "Sample rate {}Hz, {} channel(s), {} bits per sample",
        loader->sample_rate(),
        loader->num_channels(),
        loader->bits_per_sample()));

    manager().set_loader(move(loader));
    update_position(0);
    set_has_loaded_file(true);
    set_loaded_filename(path);
}

void SoundPlayerWidget::drop_event(GUI::DropEvent& event)
{
    event.accept();
    window()->move_to_front();

    if (event.mime_data().has_urls()) {
        auto urls = event.mime_data().urls();
        if (urls.is_empty())
            return;
        open_file(urls.first().path());
    }
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
    m_sample_widget->set_buffer(manager().current_buffer());
    m_play->set_icon(manager().is_paused() ? *m_play_icon : *m_pause_icon);
    update_position(manager().connection()->get_played_samples());
}

void SoundPlayerWidget::update_position(const int position)
{
    int total_norm_samples = position + normalize_rate(manager().last_seek());
    float seconds = (total_norm_samples / static_cast<float>(PLAYBACK_MANAGER_RATE));
    float remaining_seconds = manager().total_length() - seconds;

    m_elapsed->set_text(String::formatted(
        "Elapsed:\n{}:{:02}.{:02}",
        static_cast<int>(seconds / 60),
        static_cast<int>(seconds) % 60,
        static_cast<int>(seconds * 100) % 100));

    m_remaining->set_text(String::formatted(
        "Remaining:\n{}:{:02}.{:02}",
        static_cast<int>(remaining_seconds / 60),
        static_cast<int>(remaining_seconds) % 60,
        static_cast<int>(remaining_seconds * 100) % 100));

    m_slider->set_value(total_norm_samples);
}

void SoundPlayerWidget::hide_scope(bool hide)
{
    m_sample_widget->set_visible(!hide);
}

void SoundPlayerWidget::play()
{
    manager().play();
    set_paused(false);
    set_stopped(false);
}
