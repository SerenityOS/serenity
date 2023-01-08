/*
 * Copyright (c) 2022, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Action.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Label.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/SeparatorWidget.h>
#include <LibGUI/Slider.h>
#include <LibGUI/Toolbar.h>
#include <LibGUI/ToolbarContainer.h>
#include <LibGUI/Window.h>
#include <Userland/Applications/VideoPlayer/VideoPlayerWindowGML.h>

#include "VideoPlayerWidget.h"

namespace VideoPlayer {

ErrorOr<NonnullRefPtr<VideoPlayerWidget>> VideoPlayerWidget::try_create()
{
    auto main_widget = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) VideoPlayerWidget()));
    TRY(main_widget->load_from_gml(videoplayer_window_gml));

    TRY(main_widget->setup_interface());

    return main_widget;
}

ErrorOr<void> VideoPlayerWidget::setup_interface()
{
    m_video_display = find_descendant_of_type_named<VideoPlayer::VideoFrameWidget>("video_frame");
    m_video_display->on_click = [&]() { toggle_pause(); };

    m_seek_slider = find_descendant_of_type_named<GUI::HorizontalSlider>("seek_slider");
    m_seek_slider->on_change = [&](int value) {
        if (!m_playback_manager)
            return;
        update_seek_slider_max();
        auto progress = value / static_cast<double>(m_seek_slider->max());
        auto duration = m_playback_manager->duration().to_milliseconds();
        Time timestamp = Time::from_milliseconds(static_cast<i64>(round(progress * static_cast<double>(duration))));
        set_current_timestamp(timestamp);
        m_playback_manager->seek_to_timestamp(timestamp);
    };

    m_play_icon = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/play.png"sv));
    m_pause_icon = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/pause.png"sv));

    m_play_pause_action = GUI::Action::create("Play", { Key_Space }, m_play_icon, [&](auto&) {
        toggle_pause();
    });

    m_cycle_sizing_modes_action = GUI::Action::create("Sizing", [&](auto&) {
        cycle_sizing_modes();
    });

    m_timestamp_label = find_descendant_of_type_named<GUI::Label>("timestamp");
    m_volume_slider = find_descendant_of_type_named<GUI::HorizontalSlider>("volume_slider");
    find_descendant_of_type_named<GUI::Button>("playback")->set_action(*m_play_pause_action);
    find_descendant_of_type_named<GUI::Button>("sizing")->set_action(*m_cycle_sizing_modes_action);

    return {};
}

void VideoPlayerWidget::close_file()
{
    if (m_playback_manager)
        m_playback_manager = nullptr;
}

void VideoPlayerWidget::open_file(StringView filename)
{
    auto load_file_result = Video::PlaybackManager::from_file(*this, filename);

    if (load_file_result.is_error()) {
        on_decoding_error(load_file_result.release_error());
        return;
    }

    m_path = filename;
    update_title();

    close_file();
    m_playback_manager = load_file_result.release_value();
    update_seek_slider_max();
    update_seek_mode();
    resume_playback();
}

void VideoPlayerWidget::update_play_pause_icon()
{
    if (!m_playback_manager) {
        m_play_pause_action->set_enabled(false);
        m_play_pause_action->set_icon(m_play_icon);
        m_play_pause_action->set_text("Play"sv);
        return;
    }

    m_play_pause_action->set_enabled(true);

    if (m_playback_manager->is_playing()) {
        m_play_pause_action->set_icon(m_pause_icon);
        m_play_pause_action->set_text("Pause"sv);
    } else {
        m_play_pause_action->set_icon(m_play_icon);
        m_play_pause_action->set_text("Play"sv);
    }
}

void VideoPlayerWidget::resume_playback()
{
    if (!m_playback_manager)
        return;
    m_playback_manager->resume_playback();
    update_play_pause_icon();
}

void VideoPlayerWidget::pause_playback()
{
    if (!m_playback_manager)
        return;
    m_playback_manager->pause_playback();
    update_play_pause_icon();
}

void VideoPlayerWidget::toggle_pause()
{
    if (!m_playback_manager)
        return;
    if (m_playback_manager->is_playing())
        pause_playback();
    else
        resume_playback();
}

void VideoPlayerWidget::on_decoding_error(Video::DecoderError const& error)
{
    StringView text_format;

    switch (error.category()) {
    case Video::DecoderErrorCategory::IO:
        text_format = "Error while reading video:\n{}"sv;
        break;
    case Video::DecoderErrorCategory::Memory:
        text_format = "Ran out of memory:\n{}"sv;
        break;
    case Video::DecoderErrorCategory::Corrupted:
        text_format = "Video was corrupted:\n{}"sv;
        break;
    case Video::DecoderErrorCategory::Invalid:
        text_format = "Invalid call:\n{}"sv;
        break;
    case Video::DecoderErrorCategory::NotImplemented:
        text_format = "Video feature is not yet implemented:\n{}"sv;
        break;
    default:
        text_format = "Unexpected error:\n{}"sv;
        break;
    }

    GUI::MessageBox::show(window(), DeprecatedString::formatted(text_format, error.string_literal()), "Video Player encountered an error"sv);
}

void VideoPlayerWidget::update_seek_slider_max()
{
    if (!m_playback_manager) {
        m_seek_slider->set_enabled(false);
        return;
    }

    m_seek_slider->set_max(static_cast<int>(min(m_playback_manager->duration().to_milliseconds(), NumericLimits<int>::max())));
    m_seek_slider->set_enabled(true);
}

void VideoPlayerWidget::set_current_timestamp(Time timestamp)
{
    set_time_label(timestamp);
    if (!m_playback_manager)
        return;
    auto progress = static_cast<double>(timestamp.to_milliseconds()) / static_cast<double>(m_playback_manager->duration().to_milliseconds());
    m_seek_slider->set_value(static_cast<int>(round(progress * m_seek_slider->max())), GUI::AllowCallback::No);
}

void VideoPlayerWidget::set_time_label(Time timestamp)
{
    StringBuilder string_builder;
    auto append_time = [&](Time time) {
        auto seconds = time.to_seconds();
        string_builder.appendff("{:02}:{:02}:{:02}", seconds / 3600, seconds / 60, seconds % 60);
    };

    append_time(timestamp);

    if (m_playback_manager) {
        string_builder.append(" / "sv);
        append_time(m_playback_manager->duration());
    } else {
        string_builder.append(" / --:--:--.---"sv);
    }

    m_timestamp_label->set_text(string_builder.string_view());
}

void VideoPlayerWidget::event(Core::Event& event)
{
    if (event.type() == Video::EventType::DecoderErrorOccurred) {
        auto& error_event = static_cast<Video::DecoderErrorEvent&>(event);
        on_decoding_error(error_event.error());
        error_event.accept();
    } else if (event.type() == Video::EventType::VideoFramePresent) {
        auto& frame_event = static_cast<Video::VideoFramePresentEvent&>(event);

        m_video_display->set_bitmap(frame_event.frame());
        m_video_display->repaint();

        update_seek_slider_max();
        set_current_timestamp(m_playback_manager->current_playback_time());

        frame_event.accept();
    } else if (event.type() == Video::EventType::PlaybackStatusChange) {
        update_play_pause_icon();
        event.accept();
    }

    Widget::event(event);
}

void VideoPlayerWidget::cycle_sizing_modes()
{
    auto sizing_mode = m_video_display->sizing_mode();
    sizing_mode = static_cast<VideoSizingMode>((to_underlying(sizing_mode) + 1) % to_underlying(VideoSizingMode::Sentinel));
    m_video_display->set_sizing_mode(sizing_mode);
    m_video_display->update();
}

void VideoPlayerWidget::update_title()
{
    StringBuilder string_builder;
    if (m_path.is_empty()) {
        string_builder.append("No video"sv);
    } else {
        string_builder.append(m_path.view());
    }

    string_builder.append("[*] - Video Player"sv);
    window()->set_title(string_builder.to_deprecated_string());
}

Video::PlaybackManager::SeekMode VideoPlayerWidget::seek_mode()
{
    if (m_use_fast_seeking->is_checked())
        return Video::PlaybackManager::SeekMode::Fast;
    return Video::PlaybackManager::SeekMode::Accurate;
}

void VideoPlayerWidget::set_seek_mode(Video::PlaybackManager::SeekMode seek_mode)
{
    m_use_fast_seeking->set_checked(seek_mode == Video::PlaybackManager::SeekMode::Fast);
}

void VideoPlayerWidget::update_seek_mode()
{
    if (!m_playback_manager)
        return;
    m_playback_manager->set_seek_mode(seek_mode());
}

ErrorOr<void> VideoPlayerWidget::initialize_menubar(GUI::Window& window)
{
    // File menu
    auto file_menu = TRY(window.try_add_menu("&File"));
    TRY(file_menu->try_add_action(GUI::CommonActions::make_open_action([&](auto&) {
        Optional<DeprecatedString> path = GUI::FilePicker::get_open_filepath(&window, "Open video file...");
        if (path.has_value())
            open_file(path.value());
    })));
    TRY(file_menu->try_add_separator());
    TRY(file_menu->try_add_action(GUI::CommonActions::make_quit_action([&](auto&) {
        window.close();
    })));

    // Playback menu
    auto playback_menu = TRY(window.try_add_menu("&Playback"));

    // FIXME: Maybe seek mode should be in an options dialog instead. The playback menu may get crowded.
    //        For now, leave it here for convenience.
    m_use_fast_seeking = GUI::Action::create_checkable("&Fast Seeking", [&](auto&) {
        update_seek_mode();
    });
    TRY(playback_menu->try_add_action(*m_use_fast_seeking));
    set_seek_mode(Video::PlaybackManager::DEFAULT_SEEK_MODE);

    // Help menu
    auto help_menu = TRY(window.try_add_menu("&Help"));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_about_action("Video Player", TRY(GUI::Icon::try_create_default_icon("app-video-player"sv)), &window)));

    return {};
}

}
