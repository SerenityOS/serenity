/*
 * Copyright (c) 2022, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FixedArray.h>
#include <AK/NonnullRefPtr.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Forward.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Forward.h>
#include <LibVideo/DecoderError.h>
#include <LibVideo/PlaybackManager.h>

#include "VideoFrameWidget.h"

namespace VideoPlayer {

class VideoPlayerWidget final : public GUI::Widget {
    C_OBJECT_ABSTRACT(VideoPlayerWidget)

public:
    static ErrorOr<NonnullRefPtr<VideoPlayerWidget>> try_create();
    virtual ~VideoPlayerWidget() override = default;
    void close_file();
    void open_file(StringView filename);
    void resume_playback();
    void pause_playback();
    void toggle_pause();

    void update_title();

    Video::PlaybackManager::SeekMode seek_mode();
    void set_seek_mode(Video::PlaybackManager::SeekMode seek_mode);

    ErrorOr<void> initialize_menubar(GUI::Window&);

private:
    VideoPlayerWidget() = default;
    ErrorOr<void> setup_interface();
    void update_play_pause_icon();
    void update_seek_slider_max();
    void set_current_timestamp(Time);
    void set_time_label(Time);
    void on_decoding_error(Video::DecoderError const&);
    void update_seek_mode();

    void cycle_sizing_modes();

    void event(Core::Event&) override;

    DeprecatedString m_path;

    RefPtr<VideoFrameWidget> m_video_display;
    RefPtr<GUI::HorizontalSlider> m_seek_slider;

    RefPtr<Gfx::Bitmap> m_play_icon;
    RefPtr<Gfx::Bitmap> m_pause_icon;

    RefPtr<GUI::Action> m_play_pause_action;
    RefPtr<GUI::Label> m_timestamp_label;
    RefPtr<GUI::Action> m_cycle_sizing_modes_action;
    RefPtr<GUI::HorizontalSlider> m_volume_slider;

    RefPtr<GUI::Action> m_use_fast_seeking;

    OwnPtr<Video::PlaybackManager> m_playback_manager;

    bool m_was_playing_before_seek { false };
};

}
