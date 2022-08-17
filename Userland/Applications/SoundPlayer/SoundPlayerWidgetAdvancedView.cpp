/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
#include <LibGUI/Toolbar.h>
#include <LibGUI/ToolbarContainer.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>

SoundPlayerWidgetAdvancedView::SoundPlayerWidgetAdvancedView(GUI::Window& window, Audio::ConnectionToServer& connection)
    : Player(connection)
    , m_window(window)
{
    window.resize(455, 350);
    window.set_resizable(true);
    set_fill_with_background_color(true);

    set_layout<GUI::VerticalBoxLayout>();
    m_splitter = add<GUI::HorizontalSplitter>();
    m_player_view = m_splitter->add<GUI::Widget>();

    m_playlist_widget = PlaylistWidget::construct();
    m_playlist_widget->set_data_model(playlist().model());
    m_playlist_widget->set_fixed_width(150);

    m_player_view->set_layout<GUI::VerticalBoxLayout>();

    m_play_icon = Gfx::Bitmap::try_load_from_file("/res/icons/16x16/play.png"sv).release_value_but_fixme_should_propagate_errors();
    m_pause_icon = Gfx::Bitmap::try_load_from_file("/res/icons/16x16/pause.png"sv).release_value_but_fixme_should_propagate_errors();
    m_stop_icon = Gfx::Bitmap::try_load_from_file("/res/icons/16x16/stop.png"sv).release_value_but_fixme_should_propagate_errors();
    m_back_icon = Gfx::Bitmap::try_load_from_file("/res/icons/16x16/go-back.png"sv).release_value_but_fixme_should_propagate_errors();
    m_next_icon = Gfx::Bitmap::try_load_from_file("/res/icons/16x16/go-forward.png"sv).release_value_but_fixme_should_propagate_errors();

    m_visualization = m_player_view->add<BarsVisualizationWidget>();

    m_playback_progress_slider = m_player_view->add<AutoSlider>(Orientation::Horizontal);
    m_playback_progress_slider->set_fixed_height(20);
    m_playback_progress_slider->set_jump_to_cursor(true);
    m_playback_progress_slider->set_min(0);
    m_playback_progress_slider->on_knob_released = [&](int value) {
        seek(value);
    };

    auto& toolbar_container = m_player_view->add<GUI::ToolbarContainer>();
    auto& menubar = toolbar_container.add<GUI::Toolbar>();

    m_play_action = GUI::Action::create("Play", { Key_Space }, m_play_icon, [&](auto&) {
        toggle_pause();
    });
    m_play_action->set_enabled(false);
    menubar.add_action(*m_play_action);

    m_stop_action = GUI::Action::create("Stop", { Key_S }, m_stop_icon, [&](auto&) {
        stop();
    });
    m_stop_action->set_enabled(false);
    menubar.add_action(*m_stop_action);

    menubar.add_separator();

    m_timestamp_label = menubar.add<GUI::Label>();
    m_timestamp_label->set_fixed_width(110);

    // Filler label
    menubar.add<GUI::Label>();

    m_back_action = GUI::Action::create("Back", m_back_icon, [&](auto&) {
        play_file_path(playlist().previous());
    });
    m_back_action->set_enabled(false);
    menubar.add_action(*m_back_action);

    m_next_action = GUI::Action::create("Next", m_next_icon, [&](auto&) {
        play_file_path(playlist().next());
    });
    m_next_action->set_enabled(false);
    menubar.add_action(*m_next_action);

    menubar.add_separator();

    m_volume_label = &menubar.add<GUI::Label>();
    m_volume_label->set_fixed_width(30);

    m_volume_slider = &menubar.add<GUI::HorizontalSlider>();
    m_volume_slider->set_fixed_width(95);
    m_volume_slider->set_min(0);
    m_volume_slider->set_max(150);
    m_volume_slider->set_value(100);

    m_volume_slider->on_change = [&](int value) {
        double volume = m_nonlinear_volume_slider ? (double)(value * value) / (100 * 100) : value / 100.;
        set_volume(volume);
    };

    set_nonlinear_volume_slider(false);

    done_initializing();
}

void SoundPlayerWidgetAdvancedView::set_nonlinear_volume_slider(bool nonlinear)
{
    m_nonlinear_volume_slider = nonlinear;
}

void SoundPlayerWidgetAdvancedView::drop_event(GUI::DropEvent& event)
{
    event.accept();

    if (event.mime_data().has_urls()) {
        auto urls = event.mime_data().urls();
        if (urls.is_empty())
            return;
        window()->move_to_front();
        // FIXME: Add all paths from drop event to the playlist
        play_file_path(urls.first().path());
    }
}

void SoundPlayerWidgetAdvancedView::keydown_event(GUI::KeyEvent& event)
{
    if (event.key() == Key_M)
        toggle_mute();

    if (event.key() == Key_Up)
        m_volume_slider->increase_slider_by_page_steps(1);

    if (event.key() == Key_Down)
        m_volume_slider->decrease_slider_by_page_steps(1);

    GUI::Widget::keydown_event(event);
}

void SoundPlayerWidgetAdvancedView::set_playlist_visible(bool visible)
{
    if (!visible) {
        m_playlist_widget->remove_from_parent();
        m_player_view->set_max_width(window()->width());
    } else if (!m_playlist_widget->parent()) {
        m_player_view->parent_widget()->add_child(*m_playlist_widget);
    }
}

void SoundPlayerWidgetAdvancedView::play_state_changed(Player::PlayState state)
{
    sync_previous_next_actions();

    m_play_action->set_enabled(state != PlayState::NoFileLoaded);
    m_play_action->set_icon(state == PlayState::Playing ? m_pause_icon : m_play_icon);

    m_stop_action->set_enabled(state != PlayState::Stopped && state != PlayState::NoFileLoaded);

    m_playback_progress_slider->set_enabled(state != PlayState::NoFileLoaded);
}

void SoundPlayerWidgetAdvancedView::loop_mode_changed(Player::LoopMode)
{
}

void SoundPlayerWidgetAdvancedView::mute_changed(bool)
{
    // FIXME: Update the volume slider when player is muted
}

void SoundPlayerWidgetAdvancedView::sync_previous_next_actions()
{
    m_back_action->set_enabled(playlist().size() > 1 && !playlist().shuffling());
    m_next_action->set_enabled(playlist().size() > 1);
}

void SoundPlayerWidgetAdvancedView::shuffle_mode_changed(Player::ShuffleMode)
{
    sync_previous_next_actions();
}

void SoundPlayerWidgetAdvancedView::time_elapsed(int seconds)
{
    m_timestamp_label->set_text(String::formatted("Elapsed: {:02}:{:02}:{:02}", seconds / 3600, seconds / 60, seconds % 60));
}

void SoundPlayerWidgetAdvancedView::file_name_changed(StringView name)
{
    m_visualization->start_new_file(name);
    m_window.set_title(String::formatted("{} - Sound Player", name));
}

void SoundPlayerWidgetAdvancedView::total_samples_changed(int total_samples)
{
    m_playback_progress_slider->set_max(total_samples);
    m_playback_progress_slider->set_page_step(total_samples / 10);
}

void SoundPlayerWidgetAdvancedView::sound_buffer_played(FixedArray<Audio::Sample> const& buffer, int sample_rate, int samples_played)
{
    m_visualization->set_buffer(buffer);
    m_visualization->set_samplerate(sample_rate);
    // If the user is currently dragging the slider, don't interfere.
    if (!m_playback_progress_slider->mouse_is_down())
        m_playback_progress_slider->set_value(samples_played);
}

void SoundPlayerWidgetAdvancedView::volume_changed(double volume)
{
    m_volume_label->set_text(String::formatted("{}%", static_cast<int>(volume * 100)));
}

void SoundPlayerWidgetAdvancedView::playlist_loaded(StringView path, bool loaded)
{
    if (!loaded) {
        GUI::MessageBox::show(&m_window, String::formatted("Could not load playlist at \"{}\".", path), "Error opening playlist"sv, GUI::MessageBox::Type::Error);
        return;
    }
    set_playlist_visible(true);
    play_file_path(playlist().next());
}

void SoundPlayerWidgetAdvancedView::audio_load_error(StringView path, StringView error_string)
{
    GUI::MessageBox::show(&m_window, String::formatted("Failed to load audio file: {} ({})", path, error_string.is_null() ? "Unknown error"sv : error_string),
        "Filetype error"sv, GUI::MessageBox::Type::Error);
}
