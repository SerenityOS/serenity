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
    enum class ShuffleMode {
        None,
        Shuffling,
    };

    explicit Player(Audio::ClientConnection& audio_client_connection);
    virtual ~Player() { }

    void play_file_path(String const& path);

    Playlist& playlist() { return m_playlist; }
    String const& loaded_filename() const { return m_loaded_filename; }

    PlayState play_state() const { return m_play_state; }
    void set_play_state(PlayState);

    LoopMode loop_mode() const { return m_loop_mode; }
    void set_loop_mode(LoopMode);

    ShuffleMode shuffle_mode() const { return m_shuffle_mode; }
    void set_shuffle_mode(ShuffleMode);

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
    virtual void playlist_loaded(StringView, bool) = 0;
    virtual void audio_load_error(StringView, StringView) = 0;
    virtual void shuffle_mode_changed(ShuffleMode) = 0;
    virtual void volume_changed(double) = 0;
    virtual void total_samples_changed(int) = 0;
    virtual void sound_buffer_played(RefPtr<Audio::Buffer>, [[maybe_unused]] int sample_rate, [[maybe_unused]] int samples_played) = 0;

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
    ShuffleMode m_shuffle_mode;

    Audio::ClientConnection& m_audio_client_connection;
    PlaybackManager m_playback_manager;

    String m_loaded_filename;
    double m_volume { 0 };
};
