/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "PlaybackManager.h"
#include "Playlist.h"
#include "PlaylistWidget.h"
#include <AK/RefPtr.h>

class Player {
public:
    enum class PlayState {
        NoFileLoaded,
        Paused,
        Stopped,
        Playing,
    };
    enum class LoopMode {
        None,
        File,
        Playlist,
    };

    explicit Player(Audio::ClientConnection& audio_client_connection);
    virtual ~Player() { }

    void play_file_path(StringView path);

    Playlist& playlist() { return m_playlist; }
    StringView loaded_filename() const { return m_loaded_filename; }

    PlayState play_state() const { return m_play_state; }
    void set_play_state(PlayState state);

    LoopMode loop_mode() const { return m_loop_mode; }
    void set_loop_mode(LoopMode mode);

    double volume() const { return m_volume; }
    void set_volume(double value);

    void play();
    void pause();
    void toggle_pause();
    void stop();
    void seek(int sample);

    virtual void play_state_changed(PlayState) = 0;
    virtual void loop_mode_changed(LoopMode) = 0;
    virtual void time_elapsed(int) = 0;
    virtual void file_name_changed(StringView) = 0;
    virtual void playlist_loaded(StringView, bool) { }
    virtual void audio_load_error(StringView, StringView) { }
    virtual void volume_changed(double) { }
    virtual void total_samples_changed(int) { }
    virtual void sound_buffer_played(RefPtr<Audio::Buffer>, [[maybe_unused]] int sample_rate, [[maybe_unused]] int samples_played) { }

protected:
    void done_initializing()
    {
        set_play_state(PlayState::NoFileLoaded);
        set_loop_mode(LoopMode::None);
        time_elapsed(0);
        set_volume(1.);
    }

private:
    Playlist m_playlist;
    PlayState m_play_state;
    LoopMode m_loop_mode;

    Audio::ClientConnection& m_audio_client_connection;
    PlaybackManager m_playback_manager;

    StringView m_loaded_filename;
    double m_volume { 0 };
};
