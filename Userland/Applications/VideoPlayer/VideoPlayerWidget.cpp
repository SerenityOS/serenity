/*
 * Copyright (c) 2022, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NumberFormat.h>
#include <LibFileSystemAccessClient/Client.h>
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
    m_video_display->on_doubleclick = [&]() { toggle_fullscreen(); };

    m_seek_slider = find_descendant_of_type_named<GUI::HorizontalSlider>("seek_slider");
    m_seek_slider->on_drag_start = [&]() {
        if (!m_playback_manager)
            return;
        m_was_playing_before_seek = m_playback_manager->is_playing();
        m_playback_manager->pause_playback();
    };
    m_seek_slider->on_drag_end = [&]() {
        if (!m_playback_manager || !m_was_playing_before_seek)
            return;
        m_was_playing_before_seek = false;
        m_playback_manager->resume_playback();
    };
    m_seek_slider->on_change = [&](int value) {
        if (!m_playback_manager)
            return;
        update_seek_slider_max();
        auto progress = value / static_cast<double>(m_seek_slider->max());
        auto duration = m_playback_manager->duration().to_milliseconds();
        Time timestamp = Time::from_milliseconds(static_cast<i64>(round(progress * static_cast<double>(duration))));
        auto seek_mode_to_use = m_seek_slider->knob_dragging() ? seek_mode() : Video::PlaybackManager::SeekMode::Accurate;
        m_playback_manager->seek_to_timestamp(timestamp, seek_mode_to_use);
        set_current_timestamp(m_playback_manager->current_playback_time());
    };

    m_play_icon = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/play.png"sv));
    m_pause_icon = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/pause.png"sv));

    m_play_pause_action = GUI::Action::create("Play", { Key_Space }, m_play_icon, [&](auto&) {
        toggle_pause();
    });

    m_cycle_sizing_modes_action = GUI::Action::create(
        "Sizing", TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/fit-image-to-view.png"sv)), [&](auto&) {
            cycle_sizing_modes();
        });

    m_toggle_fullscreen_action = GUI::CommonActions::make_fullscreen_action([&](auto&) {
        toggle_fullscreen();
    });

    m_timestamp_label = find_descendant_of_type_named<GUI::Label>("timestamp");
    m_volume_slider = find_descendant_of_type_named<GUI::HorizontalSlider>("volume_slider");
    find_descendant_of_type_named<GUI::Button>("playback")->set_action(*m_play_pause_action);
    find_descendant_of_type_named<GUI::Button>("sizing")->set_action(*m_cycle_sizing_modes_action);
    find_descendant_of_type_named<GUI::Button>("fullscreen")->set_action(*m_toggle_fullscreen_action);

    m_size_fit_action = GUI::Action::create_checkable("&Fit", [&](auto&) {
        m_video_display->set_sizing_mode(VideoSizingMode::Fit);
    });

    m_size_fill_action = GUI::Action::create_checkable("Fi&ll", [&](auto&) {
        m_video_display->set_sizing_mode(VideoSizingMode::Fill);
    });

    m_size_stretch_action = GUI::Action::create_checkable("&Stretch", [&](auto&) {
        m_video_display->set_sizing_mode(VideoSizingMode::Stretch);
    });

    m_size_fullsize_action = GUI::Action::create_checkable("F&ull Size", [&](auto&) {
        m_video_display->set_sizing_mode(VideoSizingMode::FullSize);
    });

    return {};
}

void VideoPlayerWidget::close_file()
{
    if (m_playback_manager)
        m_playback_manager = nullptr;
}

void VideoPlayerWidget::open_file(StringView filename)
{
    auto load_file_result = Video::PlaybackManager::from_file(filename);

    if (load_file_result.is_error()) {
        on_decoding_error(load_file_result.release_error());
        return;
    }

    m_path = filename;
    update_title();
    close_file();

    m_playback_manager = load_file_result.release_value();

    m_playback_manager->on_video_frame = [this](auto frame) {
        m_video_display->set_bitmap(move(frame));
        m_video_display->repaint();

        update_seek_slider_max();
        set_current_timestamp(m_playback_manager->current_playback_time());
    };

    m_playback_manager->on_playback_state_change = [this]() {
        update_play_pause_icon();
        // If we are seeking, do not set the timestamp, as that will override the seek position.
        if (!m_was_playing_before_seek && m_playback_manager->get_state() != Video::PlaybackState::Seeking) {
            set_current_timestamp(m_playback_manager->current_playback_time());
        }
    };

    m_playback_manager->on_decoder_error = [this](auto error) {
        on_decoding_error(error);
    };

    m_playback_manager->on_fatal_playback_error = [this](auto) {
        close_file();
    };

    update_seek_slider_max();
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

    if (m_playback_manager->is_playing() || m_was_playing_before_seek) {
        m_play_pause_action->set_icon(m_pause_icon);
        m_play_pause_action->set_text("Pause"sv);
    } else {
        m_play_pause_action->set_icon(m_play_icon);
        m_play_pause_action->set_text("Play"sv);
    }
}

void VideoPlayerWidget::resume_playback()
{
    if (!m_playback_manager || m_seek_slider->knob_dragging())
        return;
    m_playback_manager->resume_playback();
}

void VideoPlayerWidget::pause_playback()
{
    if (!m_playback_manager || m_seek_slider->knob_dragging())
        return;
    m_playback_manager->pause_playback();
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
        auto seconds = (time.to_milliseconds() + 500) / 1000;
        string_builder.append(human_readable_digital_time(seconds));
    };

    append_time(timestamp);

    if (m_playback_manager) {
        string_builder.append(" / "sv);
        append_time(m_playback_manager->duration());
    } else {
        string_builder.append(" / --:--:--"sv);
    }

    m_timestamp_label->set_text(string_builder.string_view());
}

void VideoPlayerWidget::drop_event(GUI::DropEvent& event)
{
    event.accept();
    window()->move_to_front();

    if (event.mime_data().has_urls()) {
        auto urls = event.mime_data().urls();
        if (urls.is_empty())
            return;
        if (urls.size() > 1) {
            GUI::MessageBox::show_error(window(), "VideoPlayer can only view one clip at a time!"sv);
            return;
        }
        auto response = FileSystemAccessClient::Client::the().request_file_read_only_approved(window(), urls.first().path());
        if (response.is_error())
            return;
        open_file(response.value().filename());
    }
}

void VideoPlayerWidget::cycle_sizing_modes()
{
    auto sizing_mode = m_video_display->sizing_mode();
    sizing_mode = static_cast<VideoSizingMode>((to_underlying(sizing_mode) + 1) % to_underlying(VideoSizingMode::Sentinel));
    m_video_display->set_sizing_mode(sizing_mode);

    switch (sizing_mode) {
    case VideoSizingMode::Fit:
        m_size_fit_action->set_checked(true);
        break;

    case VideoSizingMode::Fill:
        m_size_fill_action->set_checked(true);
        break;

    case VideoSizingMode::Stretch:
        m_size_stretch_action->set_checked(true);
        break;

    case VideoSizingMode::FullSize:
        m_size_fullsize_action->set_checked(true);
        break;

    case VideoSizingMode::Sentinel:
        break;
    }
}

void VideoPlayerWidget::toggle_fullscreen()
{
    auto* parent_window = window();
    parent_window->set_fullscreen(!parent_window->is_fullscreen());
    auto* bottom_container = find_descendant_of_type_named<GUI::Widget>("bottom_container");
    bottom_container->set_visible(!parent_window->is_fullscreen());
    auto* video_frame = find_descendant_of_type_named<VideoFrameWidget>("video_frame");
    video_frame->set_frame_thickness(parent_window->is_fullscreen() ? 0 : 2);
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
    m_use_fast_seeking = GUI::Action::create_checkable("&Fast Seeking", [&](auto&) {});
    TRY(playback_menu->try_add_action(*m_use_fast_seeking));
    set_seek_mode(Video::PlaybackManager::DEFAULT_SEEK_MODE);

    // View menu
    auto view_menu = TRY(window.try_add_menu("&View"));
    TRY(view_menu->try_add_action(*m_toggle_fullscreen_action));

    auto sizing_mode_menu = TRY(view_menu->try_add_submenu("&Sizing mode"));
    sizing_mode_menu->set_icon(TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/fit-image-to-view.png"sv)));

    m_sizing_mode_group = make<GUI::ActionGroup>();
    m_sizing_mode_group->set_exclusive(true);
    m_sizing_mode_group->add_action(*m_size_fit_action);
    m_sizing_mode_group->add_action(*m_size_fill_action);
    m_sizing_mode_group->add_action(*m_size_stretch_action);
    m_sizing_mode_group->add_action(*m_size_fullsize_action);
    m_size_fit_action->set_checked(true);

    TRY(sizing_mode_menu->try_add_action(*m_size_fit_action));
    TRY(sizing_mode_menu->try_add_action(*m_size_fill_action));
    TRY(sizing_mode_menu->try_add_action(*m_size_stretch_action));
    TRY(sizing_mode_menu->try_add_action(*m_size_fullsize_action));

    // Help menu
    auto help_menu = TRY(window.try_add_menu("&Help"));
    TRY(help_menu->try_add_action(GUI::CommonActions::make_about_action("Video Player", TRY(GUI::Icon::try_create_default_icon("app-video-player"sv)), &window)));

    return {};
}

}
