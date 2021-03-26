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
#include "Common.h"
#include "M3UParser.h"
#include "PlaybackManager.h"
#include <AK/LexicalPath.h>
#include <AK/SIMD.h>
#include <LibGUI/Action.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Slider.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/ToolBar.h>
#include <LibGUI/ToolBarContainer.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>

SoundPlayerWidgetAdvancedView::SoundPlayerWidgetAdvancedView(GUI::Window& window, PlayerState& state)
    : Player(state)
    , m_window(window)
{
    window.resize(455, 350);
    window.set_minimum_size(600, 130);
    window.set_resizable(true);
    set_fill_with_background_color(true);

    set_layout<GUI::VerticalBoxLayout>();
    m_splitter = add<GUI::HorizontalSplitter>();
    m_player_view = m_splitter->add<GUI::Widget>();
    m_playlist_model = adopt(*new PlaylistModel());

    m_player_view->set_layout<GUI::VerticalBoxLayout>();

    m_play_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/play.png");
    m_pause_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/pause.png");
    m_stop_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/stop.png");
    m_back_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/go-back.png");
    m_next_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/go-forward.png");

    m_visualization = m_player_view->add<BarsVisualizationWidget>();

    m_playback_progress_slider = m_player_view->add<AutoSlider>(Orientation::Horizontal);
    m_playback_progress_slider->set_fixed_height(20);
    m_playback_progress_slider->set_min(0);
    m_playback_progress_slider->set_max(this->manager().total_length() * 44100); //this value should be set when we load a new file
    m_playback_progress_slider->on_knob_released = [&](int value) {
        this->manager().seek(value);
    };

    auto& toolbar_container = m_player_view->add<GUI::ToolBarContainer>();
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
    m_back_button->on_click = [&](unsigned) {
        if (!m_playlist_model.is_null()) {
            auto it = m_playlist_model->items().find_if([&](const M3UEntry& e) { return e.path == loaded_filename(); });
            if (it.index() == 0) {
                open_file(m_playlist_model->items().at(m_playlist_model->items().size() - 1).path);
            } else
                open_file((it - 1)->path);
            play();
        }
    };

    m_next_button = menubar.add<GUI::Button>();
    m_next_button->set_fixed_width(50);
    m_next_button->set_icon(*m_next_icon);
    m_next_button->set_enabled(has_loaded_file());
    m_next_button->on_click = [&](unsigned) {
        if (!m_playlist_model.is_null()) {
            auto it = m_playlist_model->items().find_if([&](const M3UEntry& e) { return e.path == loaded_filename(); });
            if (it.index() + 1 == m_playlist_model->items().size()) {
                open_file(m_playlist_model->items().at(0).path);
            } else
                open_file((it + 1)->path);
            play();
        }
    };

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
        dynamic_cast<Visualization*>(m_visualization.ptr())->set_samplerate(loaded_file_samplerate());
    };

    manager().on_load_sample_buffer = [&](Audio::Buffer&) {
        //TODO: Implement an equalizer
        return;
    };

    manager().on_finished_playing = [&] {
        m_play_button->set_icon(*m_play_icon);
        if (looping()) {
            open_file(loaded_filename());
            return;
        }

        if (!m_playlist_model.is_null() && !m_playlist_model->items().is_empty()) {
            auto it = m_playlist_model->items().find_if([&](const M3UEntry& e) { return e.path == loaded_filename(); });
            if (it.index() + 1 == m_playlist_model->items().size()) {
                if (looping_playlist()) {
                    open_file(m_playlist_model->items().at(0).path);
                    return;
                }
            } else
                open_file((it + 1)->path);
        }
    };
}

void SoundPlayerWidgetAdvancedView::open_file(StringView path)
{
    if (!Core::File::exists(path)) {
        GUI::MessageBox::show(window(), String::formatted("File \"{}\" does not exist", path), "Error opening file", GUI::MessageBox::Type::Error);
        return;
    }

    if (path.ends_with(".m3u", AK::CaseSensitivity::CaseInsensitive) || path.ends_with(".m3u8", AK::CaseSensitivity::CaseInsensitive)) {
        read_playlist(path);
        return;
    }

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
    m_play_button->set_icon(*m_pause_icon);
    m_stop_button->set_enabled(true);
    m_playback_progress_slider->set_max(loader->total_samples());
    manager().set_loader(move(loader));
    set_has_loaded_file(true);
    set_loaded_file_samplerate(loader->sample_rate());
    set_loaded_filename(path);
    play();
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

void SoundPlayerWidgetAdvancedView::read_playlist(StringView path)
{
    auto parser = M3UParser::from_file(path);
    auto items = parser->parse(true);
    VERIFY(items->size() > 0);
    try_fill_missing_info(*items, path);
    for (auto& item : *items)
        m_playlist_model->items().append(item);
    set_playlist_visible(true);
    m_playlist_model->update();

    open_file(items->at(0).path);

    if (items->size() > 1) {
        m_back_button->set_enabled(true);
        m_next_button->set_enabled(true);
    } else {
        m_back_button->set_enabled(false);
        m_next_button->set_enabled(false);
    }
}

void SoundPlayerWidgetAdvancedView::set_playlist_visible(bool visible)
{
    if (visible) {
        m_playlist_widget = m_player_view->parent_widget()->add<PlaylistWidget>();
        m_playlist_widget->set_data_model(m_playlist_model);
        m_playlist_widget->set_fixed_width(150);
    } else {
        m_playlist_widget->remove_from_parent();
        m_player_view->set_max_width(window()->width());
    }
}

void SoundPlayerWidgetAdvancedView::try_fill_missing_info(Vector<M3UEntry>& entries, StringView playlist_p)
{
    LexicalPath playlist_path(playlist_p);
    Vector<M3UEntry*> to_delete;
    for (auto& entry : entries) {
        LexicalPath entry_path(entry.path);
        if (!entry_path.is_absolute()) {
            entry.path = String::formatted("{}/{}", playlist_path.dirname(), entry_path.basename());
        }

        if (!Core::File::exists(entry.path)) {
            GUI::MessageBox::show(window(), String::formatted("The file \"{}\" present in the playlist does not exist or was not found. This file will be ignored.", entry_path.basename()), "Error reading playlist", GUI::MessageBox::Type::Warning);
            to_delete.append(&entry);
            continue;
        }

        if (!entry.extended_info->track_display_title.has_value())
            entry.extended_info->track_display_title = LexicalPath(entry.path).title();
        if (!entry.extended_info->track_length_in_seconds.has_value()) {
            if (entry_path.has_extension("wav")) {
                auto wav_reader = Audio::Loader::create(entry.path);
                entry.extended_info->track_length_in_seconds = wav_reader->total_samples() / wav_reader->sample_rate();
            }
            //TODO: Implement embedded metadata extractor for other audio formats
        }
        //TODO: Implement a metadata parser for the uncomfortably numerous popular embedded metadata formats

        if (!entry.extended_info->file_size_in_bytes.has_value()) {
            FILE* f = fopen(entry.path.characters(), "r");
            VERIFY(f != nullptr);
            fseek(f, 0, SEEK_END);
            entry.extended_info->file_size_in_bytes = ftell(f);
            fclose(f);
        }
    }
    for (M3UEntry* entry : to_delete)
        entries.remove_first_matching([&](M3UEntry& e) { return &e == entry; });
}
