/*
 * Copyright (c) 2021, Leandro A. F. Pereira <leandro@tia.mat.br>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AmpPlayerView.h"
#include "BarsVisualizationWidget.h"
#include "M3UParser.h"
#include "PlaybackManager.h"
#include <AK/LexicalPath.h>
#include <AK/SIMD.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Window.h>

AmpPlayerView::AmpPlayerView(GUI::Window& window, Audio::ClientConnection& audio_connection)
    : Player(audio_connection)
    , m_window(window)
{
    window.resize(275, 116);
    window.set_resizable(false);
    window.set_forced_shadow(true);
    window.set_minimizable(true);
    window.set_modal(false);
    window.set_frameless(true);
    window.set_always_show_in_taskbar(true);

    m_playlist_widget = PlaylistWidget::construct();
    m_playlist_widget->set_data_model(playlist().model());
    m_playlist_widget->set_fixed_width(150);

    m_playlist_window = GUI::Window::construct();
    m_playlist_window->set_title("Playlist");
    m_playlist_window->set_main_widget(m_playlist_widget);
    m_playlist_window->on_close = [&]() {
        m_pl_button->set_checked(false, GUI::AllowCallback::No);
    };

    set_visualization<BarsVisualizationWidget>();

    m_pos_slider->set_jump_to_cursor(true);

    m_pos_slider->on_knob_released = [&](int value) { seek(value); };
    m_pl_button->on_checked = [&](bool checked) { set_playlist_visible(checked); };
    m_repeat_button->on_checked = [&](bool checked) {
        if (checked)
            set_loop_mode(Player::LoopMode::File);
        else
            set_loop_mode(Player::LoopMode::None);
    };
    m_play_button->on_click = [&](unsigned) { play(); };
    m_stop_button->on_click = [&](unsigned) { stop(); };
    m_pause_button->on_click = [&](unsigned) {
        // FIXME: Player class toggles play/pause when pause() is called.
        // Move that functionality to the advanced view.
        if (play_state() == PlayState::Playing)
            pause();
    };
    m_prev_button->on_click = [&](unsigned) { play_file_path(playlist().previous()); };
    m_next_button->on_click = [&](unsigned) { play_file_path(playlist().next()); };

    m_vol_slider->set_min(0);
    m_vol_slider->set_max(100);
    m_vol_slider->set_value(100);
    m_vol_slider->on_change = [&](int value) { set_volume(value / 100.); };

    done_initializing();
}

void AmpPlayerView::drop_event(GUI::DropEvent& event)
{
    event.accept();

    if (event.mime_data().has_urls()) {
        auto urls = event.mime_data().urls();
        if (urls.is_empty())
            return;
        window()->move_to_front();
        play_file_path(urls.first().path());
    }
}

void AmpPlayerView::set_playlist_visible(bool visible)
{
    if (visible)
        m_playlist_window->show();
    else
        m_playlist_window->hide();
    m_pl_button->set_checked(visible, GUI::AllowCallback::No);
}

void AmpPlayerView::play_state_changed(Player::PlayState state)
{
    m_play_button->set_enabled(state != PlayState::NoFileLoaded);
    m_pause_button->set_enabled(state != PlayState::NoFileLoaded);
    m_prev_button->set_enabled(state != PlayState::NoFileLoaded);
    m_next_button->set_enabled(state != PlayState::NoFileLoaded);

    switch (state) {
    case PlayState::Stopped:
    case PlayState::NoFileLoaded:
        m_time_display_blink_timer->stop();
        m_time_display->set_digits_visible(false);
        m_pos_slider->set_enabled(false);
        break;
    case PlayState::Playing:
        m_time_display_blink_timer->stop();
        m_time_display->set_digits_visible(true);
        m_pos_slider->set_enabled(true);
        break;
    case PlayState::Paused:
        m_time_display_blink_timer->start();
        m_pos_slider->set_enabled(true);
        break;
    }

    AmpWidget::set_play_state(state);
}

void AmpPlayerView::loop_mode_changed(Player::LoopMode)
{
}

void AmpPlayerView::time_elapsed(int seconds)
{
    m_time_display->set_time(seconds / 60, seconds % 60);
}

void AmpPlayerView::file_name_changed(StringView)
{
}

void AmpPlayerView::total_samples_changed(int total_samples)
{
    m_pos_slider->set_max(total_samples);
    m_pos_slider->set_page_step(total_samples / 10);
}

void AmpPlayerView::sound_buffer_played(RefPtr<Audio::Buffer> buffer, int sample_rate, int samples_played)
{
    m_visualization->set_buffer(buffer);
    m_visualization->set_samplerate(sample_rate);
    m_pos_slider->set_value(samples_played);
}

void AmpPlayerView::volume_changed(double volume)
{
    m_vol_slider->set_value((int)(volume * 100));
}

void AmpPlayerView::playlist_loaded(StringView, bool)
{
}

void AmpPlayerView::audio_load_error(StringView, StringView)
{
}

void AmpPlayerView::shuffle_mode_changed(ShuffleMode)
{
}
