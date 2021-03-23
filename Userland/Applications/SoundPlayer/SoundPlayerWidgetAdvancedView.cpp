/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
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

#include "SoundPlayerWidgetAdvancedView.h"
#include "BarsVisualizationWidget.h"
#include "PlaybackManager.h"
#include "SoundPlayerWidget.h"
#include <AK/SIMD.h>
#include <LibGUI/Action.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/DragOperation.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Slider.h>
#include <LibGUI/ToolBar.h>
#include <LibGUI/ToolBarContainer.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>

SoundPlayerWidgetAdvancedView::SoundPlayerWidgetAdvancedView(GUI::Window& window, PlayerState& state)
    : Player(state)
    , m_window(window)
{
    window.resize(455, 350);
    window.set_minimum_size(440, 130);
    window.set_resizable(true);

    set_fill_with_background_color(true);
    set_layout<GUI::VerticalBoxLayout>();

    m_play_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/play.png");
    m_pause_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/pause.png");
    m_stop_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/stop.png");
    m_back_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/go-back.png");
    m_next_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/go-forward.png");

    m_visualization = add<BarsVisualizationWidget>();

    m_playback_progress_slider = add<Slider>(Orientation::Horizontal);
    m_playback_progress_slider->set_fixed_height(20);
    m_playback_progress_slider->set_min(0);
    m_playback_progress_slider->set_max(this->manager().total_length() * 44100); //this value should be set when we load a new file
    m_playback_progress_slider->on_knob_released = [&](int value) {
        this->manager().seek(value);
    };

    auto& toolbar_container = add<GUI::ToolBarContainer>();

    auto& menubar = toolbar_container.add<GUI::ToolBar>();

    m_play_button = menubar.add<GUI::Button>();
    m_play_button->set_icon(is_paused() ? (!has_loaded_file() ? *m_play_icon : *m_pause_icon) : *m_pause_icon);
    m_play_button->set_fixed_width(50);
    m_play_button->set_enabled(has_loaded_file());
    m_play_button->on_click = [&](unsigned) {
        bool paused = this->manager().toggle_pause();
        set_paused(paused);
        m_play_button->set_icon(paused ? *m_play_icon : *m_pause_icon);
    };

    m_stop_button = menubar.add<GUI::Button>();
    m_stop_button->set_icon(*m_stop_icon);
    m_stop_button->set_fixed_width(50);
    m_stop_button->set_enabled(has_loaded_file());
    m_stop_button->on_click = [&](unsigned) {
        this->manager().stop();
        set_stopped(true);
        m_play_button->set_icon(*m_play_icon);
        m_stop_button->set_enabled(false);
    };

    auto& timestamp_label = menubar.add<GUI::Label>();
    timestamp_label.set_fixed_width(110);
    timestamp_label.set_text("Elapsed: 00:00:00");

    // filler_label
    menubar.add<GUI::Label>();

    m_back_button = menubar.add<GUI::Button>();
    m_back_button->set_fixed_width(50);
    m_back_button->set_icon(*m_back_icon);
    m_back_button->set_enabled(has_loaded_file());

    m_next_button = menubar.add<GUI::Button>();
    m_next_button->set_fixed_width(50);
    m_next_button->set_icon(*m_next_icon);
    m_next_button->set_enabled(has_loaded_file());

    m_volume_label = &menubar.add<GUI::Label>();
    m_volume_label->set_fixed_width(30);
    m_volume_label->set_text("100%");

    auto& volume_slider = menubar.add<GUI::HorizontalSlider>();
    volume_slider.set_fixed_width(95);
    volume_slider.set_min(0);
    volume_slider.set_max(150);
    volume_slider.set_value(100);

    volume_slider.on_change = [&](int value) {
        double volume = m_nonlinear_volume_slider ? (double)(value * value) / (100 * 100) : value / 100.;
        m_volume_label->set_text(String::formatted("{}%", (int)(volume * 100)));
        set_volume(volume);
    };

    set_volume(1.);
    set_nonlinear_volume_slider(false);

    manager().on_update = [&]() {
        //TODO: make this program support other sample rates
        int samples_played = client_connection().get_played_samples() + this->manager().last_seek();
        int current_second = samples_played / 44100;
        timestamp_label.set_text(String::formatted("Elapsed: {:02}:{:02}:{:02}", current_second / 3600, current_second / 60, current_second % 60));
        m_playback_progress_slider->set_value(samples_played);

        dynamic_cast<Visualization*>(m_visualization.ptr())->set_buffer(this->manager().current_buffer());
    };

    this->manager().on_load_sample_buffer = [&](Audio::Buffer& buffer) {
        if (volume() == 1.)
            return;
        auto sample_count = buffer.sample_count();
        if (sample_count % 4 == 0) {
            const int total_iter = sample_count / (sizeof(AK::SIMD::f64x4) / sizeof(double) / 2);
            AK::SIMD::f64x4* sample_ptr = const_cast<AK::SIMD::f64x4*>(reinterpret_cast<const AK::SIMD::f64x4*>((buffer.data())));
            for (int i = 0; i < total_iter; ++i) {
                sample_ptr[i] = sample_ptr[i] * volume();
            }
        } else {
            const int total_iter = sample_count / (sizeof(AK::SIMD::f64x2) / sizeof(double) / 2);
            AK::SIMD::f64x2* sample_ptr = const_cast<AK::SIMD::f64x2*>(reinterpret_cast<const AK::SIMD::f64x2*>((buffer.data())));
            for (int i = 0; i < total_iter; ++i) {
                sample_ptr[i] = sample_ptr[i] * volume();
            }
        }
    };
}

void SoundPlayerWidgetAdvancedView::open_file(StringView path)
{
    NonnullRefPtr<Audio::Loader> loader = Audio::Loader::create(path);
    if (loader->has_error() || !loader->sample_rate()) {
        const String error_string = loader->error_string();
        GUI::MessageBox::show(&m_window, String::formatted("Failed to load audio file: {} ({})", path, error_string.is_null() ? "Unknown error" : error_string),
            "Filetype error", GUI::MessageBox::Type::Error);
        return;
    }
    m_window.set_title(String::formatted("{} - SoundPlayer", loader->file()->filename()));
    m_playback_progress_slider->set_max(loader->total_samples());
    m_playback_progress_slider->set_enabled(true);
    m_play_button->set_enabled(true);
    m_stop_button->set_enabled(true);
    manager().set_loader(move(loader));
    set_has_loaded_file(true);
    set_loaded_filename(path);
}

void SoundPlayerWidgetAdvancedView::set_nonlinear_volume_slider(bool nonlinear)
{
    m_nonlinear_volume_slider = nonlinear;
}

void SoundPlayerWidgetAdvancedView::drop_event(GUI::DropEvent& event)
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

SoundPlayerWidgetAdvancedView::~SoundPlayerWidgetAdvancedView()
{
    manager().on_load_sample_buffer = nullptr;
    manager().on_update = nullptr;
}

void SoundPlayerWidgetAdvancedView::play()
{
    manager().play();
    set_paused(false);
    set_stopped(false);
}
