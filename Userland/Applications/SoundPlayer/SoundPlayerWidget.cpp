/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SoundPlayerWidget.h"
#include "AlbumCoverVisualizationWidget.h"
#include "BarsVisualizationWidget.h"
#include "M3UParser.h"
#include "PlaybackManager.h"
#include "SampleWidget.h"
#include <AK/ByteString.h>
#include <AK/LexicalPath.h>
#include <AK/NumberFormat.h>
#include <AK/SIMD.h>
#include <LibConfig/Client.h>
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

SoundPlayerWidget::SoundPlayerWidget(GUI::Window& window, Audio::ConnectionToServer& connection, ImageDecoderClient::Client& image_decoder_client)
    : Player(connection)
    , m_window(window)
    , m_image_decoder_client(image_decoder_client)
{
    window.resize(455, 350);
    window.set_resizable(true);
    set_fill_with_background_color(true);

    set_layout<GUI::VerticalBoxLayout>();
    m_splitter = add<GUI::HorizontalSplitter>();
    m_player_view = m_splitter->add<GUI::Widget>();

    m_playlist_widget = PlaylistWidget::construct();
    m_playlist_widget->set_data_model(playlist().model());
    m_playlist_widget->set_preferred_width(150);

    m_player_view->set_layout<GUI::VerticalBoxLayout>();

    m_play_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/play.png"sv).release_value_but_fixme_should_propagate_errors();
    m_pause_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/pause.png"sv).release_value_but_fixme_should_propagate_errors();
    m_stop_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/stop.png"sv).release_value_but_fixme_should_propagate_errors();
    m_back_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/go-back.png"sv).release_value_but_fixme_should_propagate_errors();
    m_next_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/go-forward.png"sv).release_value_but_fixme_should_propagate_errors();
    m_volume_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/audio-volume-medium.png"sv).release_value_but_fixme_should_propagate_errors();
    m_muted_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/audio-volume-muted.png"sv).release_value_but_fixme_should_propagate_errors();

    auto visualization = Config::read_string("SoundPlayer"sv, "Preferences"sv, "Visualization"sv, "bars"sv);
    if (visualization == "samples") {
        m_visualization = m_player_view->add<SampleWidget>();
    } else if (visualization == "album_cover") {
        m_visualization = m_player_view->add<AlbumCoverVisualizationWidget>([this]() {
            return get_image_from_music_file();
        });
    } else {
        m_visualization = m_player_view->add<BarsVisualizationWidget>();
    }

    m_playback_progress_slider = m_player_view->add<GUI::HorizontalSlider>();
    m_playback_progress_slider->set_fixed_height(20);
    m_playback_progress_slider->set_jump_to_cursor(true);
    m_playback_progress_slider->set_min(0);
    m_playback_progress_slider->on_change = [&](int value) {
        if (!m_playback_progress_slider->knob_dragging())
            seek(value);
    };
    m_playback_progress_slider->on_drag_end = [&]() {
        seek(m_playback_progress_slider->value());
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

    m_mute_action = GUI::Action::create("Mute", { Key_M }, m_volume_icon, [&](auto&) {
        toggle_mute();
    });
    m_mute_action->set_enabled(true);
    menubar.add_action(*m_mute_action);

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

void SoundPlayerWidget::set_nonlinear_volume_slider(bool nonlinear)
{
    m_nonlinear_volume_slider = nonlinear;
}

void SoundPlayerWidget::drag_enter_event(GUI::DragEvent& event)
{
    if (event.mime_data().has_urls())
        event.accept();
}

void SoundPlayerWidget::drop_event(GUI::DropEvent& event)
{
    event.accept();

    if (event.mime_data().has_urls()) {
        auto urls = event.mime_data().urls();
        if (urls.is_empty())
            return;
        window()->move_to_front();
        // FIXME: Add all paths from drop event to the playlist
        play_file_path(URL::percent_decode(urls.first().serialize_path()));
    }
}

void SoundPlayerWidget::keydown_event(GUI::KeyEvent& event)
{
    if (event.key() == Key_Up)
        m_volume_slider->increase_slider_by_page_steps(1);

    if (event.key() == Key_Down)
        m_volume_slider->decrease_slider_by_page_steps(1);

    GUI::Widget::keydown_event(event);
}

void SoundPlayerWidget::set_playlist_visible(bool visible)
{
    if (!visible) {
        m_playlist_widget->remove_from_parent();
        m_player_view->set_max_width(window()->width());
    } else if (!m_playlist_widget->parent()) {
        m_player_view->parent_widget()->add_child(*m_playlist_widget);
    }
}

RefPtr<Gfx::Bitmap> SoundPlayerWidget::get_image_from_music_file()
{
    auto const& pictures = this->pictures();
    if (pictures.is_empty())
        return {};

    // FIXME: We randomly select the first picture available for the track,
    //        We might want to hardcode or let the user set a preference.
    // FIXME: Refactor image decoding to be more async-aware, and don't await this promise
    auto decoded_image_or_error = m_image_decoder_client.decode_image(pictures[0].data, {}, {})->await();
    if (decoded_image_or_error.is_error())
        return {};

    auto const decoded_image = decoded_image_or_error.release_value();
    return decoded_image.frames[0].bitmap;
}

void SoundPlayerWidget::play_state_changed(Player::PlayState state)
{
    sync_previous_next_actions();

    m_play_action->set_enabled(state != PlayState::NoFileLoaded);
    m_play_action->set_icon(state == PlayState::Playing ? m_pause_icon : m_play_icon);
    m_play_action->set_text(state == PlayState::Playing ? "Pause"sv : "Play"sv);

    m_stop_action->set_enabled(state != PlayState::Stopped && state != PlayState::NoFileLoaded);

    m_playback_progress_slider->set_enabled(state != PlayState::NoFileLoaded);
    if (state == PlayState::Stopped) {
        m_playback_progress_slider->set_value(m_playback_progress_slider->min(), GUI::AllowCallback::No);
        m_visualization->reset_buffer();
    }
}

void SoundPlayerWidget::loop_mode_changed(Player::LoopMode)
{
}

void SoundPlayerWidget::mute_changed(bool muted)
{
    m_mute_action->set_text(muted ? "Unmute"sv : "Mute"sv);
    m_mute_action->set_icon(muted ? m_muted_icon : m_volume_icon);
    m_volume_slider->set_enabled(!muted);
}

void SoundPlayerWidget::sync_previous_next_actions()
{
    m_back_action->set_enabled(playlist().size() > 1 && !playlist().shuffling());
    m_next_action->set_enabled(playlist().size() > 1);
}

void SoundPlayerWidget::shuffle_mode_changed(Player::ShuffleMode)
{
    sync_previous_next_actions();
}

void SoundPlayerWidget::time_elapsed(int seconds)
{
    m_timestamp_label->set_text(String::formatted("Elapsed: {}", human_readable_digital_time(seconds)).release_value_but_fixme_should_propagate_errors());
}

void SoundPlayerWidget::file_name_changed(StringView name)
{
    m_visualization->start_new_file(name);
    ByteString title = name;
    if (playback_manager().loader()) {
        auto const& metadata = playback_manager().loader()->metadata();
        if (auto artists_or_error = metadata.all_artists(" / "_string);
            !artists_or_error.is_error() && artists_or_error.value().has_value() && metadata.title.has_value()) {
            title = ByteString::formatted("{} – {}", metadata.title.value(), artists_or_error.release_value().release_value());
        } else if (metadata.title.has_value()) {
            title = metadata.title.value().to_byte_string();
        }
    }
    m_window.set_title(ByteString::formatted("{} — Sound Player", title));
}

void SoundPlayerWidget::total_samples_changed(int total_samples)
{
    m_playback_progress_slider->set_max(total_samples);
    m_playback_progress_slider->set_page_step(total_samples / 10);
}

void SoundPlayerWidget::sound_buffer_played(FixedArray<Audio::Sample> const& buffer, int sample_rate, int samples_played)
{
    m_visualization->set_buffer(buffer);
    m_visualization->set_samplerate(sample_rate);
    // If the user is currently dragging the slider, don't interfere.
    if (!m_playback_progress_slider->knob_dragging())
        m_playback_progress_slider->set_value(samples_played, GUI::AllowCallback::No);
}

void SoundPlayerWidget::volume_changed(double volume)
{
    m_volume_label->set_text(String::formatted("{}%", static_cast<int>(volume * 100)).release_value_but_fixme_should_propagate_errors());
}

void SoundPlayerWidget::playlist_loaded(StringView path, bool loaded)
{
    if (!loaded) {
        GUI::MessageBox::show(&m_window, ByteString::formatted("Could not load playlist at \"{}\".", path), "Error opening playlist"sv, GUI::MessageBox::Type::Error);
        return;
    }
    set_playlist_visible(true);
    play_file_path(playlist().next());
}

void SoundPlayerWidget::audio_load_error(StringView path, StringView error_string)
{
    GUI::MessageBox::show(&m_window, ByteString::formatted("Failed to load audio file: {} ({})", path, error_string.is_null() ? "Unknown error"sv : error_string),
        "Filetype error"sv, GUI::MessageBox::Type::Error);
}
