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

#include "VideoPlayerWidget.h"

namespace VideoPlayer {

VideoPlayerWidget::VideoPlayerWidget(GUI::Window& window)
    : m_window(window)
{
    set_fill_with_background_color(true);

    set_layout<GUI::VerticalBoxLayout>();

    m_video_display = add<VideoFrameWidget>();
    m_video_display->set_auto_resize(false);
    m_video_display->on_click = [&]() { toggle_pause(); };

    auto& player_controls_widget = add<GUI::Widget>();
    player_controls_widget.set_layout<GUI::VerticalBoxLayout>();
    player_controls_widget.set_max_height(50);

    m_seek_slider = player_controls_widget.add<GUI::HorizontalSlider>();
    m_seek_slider->set_fixed_height(20);
    m_seek_slider->set_enabled(false);

    auto& toolbar_container = player_controls_widget.add<GUI::ToolbarContainer>();
    m_toolbar = toolbar_container.add<GUI::Toolbar>();

    m_play_icon = Gfx::Bitmap::try_load_from_file("/res/icons/16x16/play.png"sv).release_value_but_fixme_should_propagate_errors();
    m_pause_icon = Gfx::Bitmap::try_load_from_file("/res/icons/16x16/pause.png"sv).release_value_but_fixme_should_propagate_errors();

    m_play_pause_action = GUI::Action::create("Play", { Key_Space }, m_play_icon, [&](auto&) {
        toggle_pause();
    });

    m_cycle_sizing_modes_action = GUI::Action::create("Sizing", [&](auto&) {
        cycle_sizing_modes();
    });

    m_toolbar->add_action(*m_play_pause_action);
    m_toolbar->add<GUI::VerticalSeparator>();
    m_timestamp_label = m_toolbar->add<GUI::Label>();
    m_timestamp_label->set_fixed_width(50);

    m_toolbar->add<GUI::Widget>(); // Filler widget

    m_toolbar->add_action(*m_cycle_sizing_modes_action);

    m_toolbar->add<GUI::VerticalSeparator>();
    m_volume_slider = m_toolbar->add<GUI::HorizontalSlider>();
    m_volume_slider->set_min(0);
    m_volume_slider->set_max(100);
    m_volume_slider->set_fixed_width(100);
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
    resume_playback();
}

void VideoPlayerWidget::update_play_pause_icon()
{
    if (!m_playback_manager) {
        m_play_pause_action->set_enabled(false);
        m_play_pause_action->set_icon(m_play_icon);
        return;
    }

    m_play_pause_action->set_enabled(true);

    if (m_playback_manager->is_playing() || m_playback_manager->is_buffering())
        m_play_pause_action->set_icon(m_pause_icon);
    else
        m_play_pause_action->set_icon(m_play_icon);
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
    if (m_playback_manager->is_playing() || m_playback_manager->is_buffering())
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

    GUI::MessageBox::show(&m_window, String::formatted(text_format, error.string_literal()), "Video Player encountered an error"sv);
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

        m_seek_slider->set_max(m_playback_manager->duration().to_milliseconds());
        m_seek_slider->set_value(m_playback_manager->current_playback_time().to_milliseconds());
        m_seek_slider->set_enabled(true);

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
    window()->set_title(string_builder.to_string());
}

void VideoPlayerWidget::initialize_menubar(GUI::Window& window)
{
    auto& file_menu = window.add_menu("&File");
    file_menu.add_action(GUI::CommonActions::make_open_action([&](auto&) {
        Optional<String> path = GUI::FilePicker::get_open_filepath(&window, "Open video file...");
        if (path.has_value())
            open_file(path.value());
    }));
    file_menu.add_separator();
    file_menu.add_action(GUI::CommonActions::make_quit_action([&](auto&) {
        window.close();
    }));

    auto& help_menu = window.add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_about_action("Video Player", GUI::Icon::default_icon("app-video-player"sv), &window));
}

}
