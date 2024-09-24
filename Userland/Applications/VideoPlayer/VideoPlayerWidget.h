/*
 * Copyright (c) 2022, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FixedArray.h>
#include <AK/NonnullRefPtr.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Forward.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Forward.h>
#include <LibMedia/DecoderError.h>
#include <LibMedia/PlaybackManager.h>

#include "VideoFrameWidget.h"

namespace VideoPlayer {

class VideoPlayerWidget final : public GUI::Widget {
    C_OBJECT_ABSTRACT(VideoPlayerWidget)

public:
    static ErrorOr<NonnullRefPtr<VideoPlayerWidget>> try_create();
    ErrorOr<void> initialize();
    virtual ~VideoPlayerWidget() override = default;
    void close_file();
    void open_file(FileSystemAccessClient::File filename);
    void resume_playback();
    void pause_playback();
    void toggle_pause();

    void update_title();

    Media::PlaybackManager::SeekMode seek_mode();
    void set_seek_mode(Media::PlaybackManager::SeekMode seek_mode);
    void set_sizing_mode(VideoSizingMode sizing_mode);

    ErrorOr<void> initialize_menubar(GUI::Window&);

private:
    VideoPlayerWidget() = default;
    void update_play_pause_icon();
    void update_seek_slider_max();
    void set_current_timestamp(Duration);
    void set_time_label(Duration);
    void on_decoding_error(Media::DecoderError const&);

    void cycle_sizing_modes();
    void set_current_sizing_mode_checked();

    void toggle_fullscreen();

    virtual void drop_event(GUI::DropEvent&) override;

    String m_path;

    RefPtr<VideoFrameWidget> m_video_display;
    RefPtr<GUI::HorizontalSlider> m_seek_slider;

    RefPtr<Gfx::Bitmap> m_play_icon;
    RefPtr<Gfx::Bitmap> m_pause_icon;

    RefPtr<GUI::Action> m_play_pause_action;
    RefPtr<GUI::Label> m_timestamp_label;
    RefPtr<GUI::Action> m_cycle_sizing_modes_action;
    RefPtr<GUI::HorizontalSlider> m_volume_slider;

    RefPtr<GUI::Action> m_use_fast_seeking;

    RefPtr<GUI::Action> m_toggle_fullscreen_action;

    OwnPtr<GUI::ActionGroup> m_sizing_mode_group;
    RefPtr<GUI::Action> m_size_fit_action;
    RefPtr<GUI::Action> m_size_fill_action;
    RefPtr<GUI::Action> m_size_stretch_action;
    RefPtr<GUI::Action> m_size_fullsize_action;

    OwnPtr<Media::PlaybackManager> m_playback_manager;

    bool m_was_playing_before_seek { false };
};

}
