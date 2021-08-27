/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "PlaybackManager.h"
#include "PlaylistWidget.h"
#include "VisualizationBase.h"
#include <AK/RefPtr.h>

struct PlayerState {
    bool is_paused;
    bool is_stopped;
    bool has_loaded_file;
    bool is_looping_file;
    bool is_looping_playlist;
    int loaded_file_samplerate;
    double volume;
    Audio::ClientConnection& connection;
    PlaybackManager& manager;
    String loaded_filename;
};

class Player {
public:
    explicit Player(PlayerState& state)
        : m_player_state(state) {};
    virtual void open_file(StringView path) = 0;
    virtual void play() = 0;

    PlayerState& get_player_state() { return m_player_state; }
    bool is_stopped() const { return m_player_state.is_stopped; }
    bool is_paused() const { return m_player_state.is_paused; }
    bool has_loaded_file() const { return m_player_state.has_loaded_file; }
    double volume() const { return m_player_state.volume; }
    bool looping() const { return m_player_state.is_looping_file; }
    bool looping_playlist() const { return m_player_state.is_looping_playlist; }
    const String& loaded_filename() { return m_player_state.loaded_filename; }
    int loaded_file_samplerate() { return m_player_state.loaded_file_samplerate; }

    virtual void set_stopped(bool stopped) { m_player_state.is_stopped = stopped; }
    virtual void set_paused(bool paused) { m_player_state.is_paused = paused; }
    virtual void set_has_loaded_file(bool loaded) { m_player_state.has_loaded_file = loaded; }
    virtual void set_volume(double volume)
    {
        m_player_state.volume = volume;
        client_connection().set_self_volume(volume);
    }
    virtual void set_loaded_file_samplerate(int samplerate) { m_player_state.loaded_file_samplerate = samplerate; }
    virtual void set_looping_file(bool loop)
    {
        m_player_state.is_looping_file = loop;
    }
    virtual void set_looping_playlist(bool loop)
    {
        m_player_state.is_looping_playlist = loop;
    }
    virtual void set_loaded_filename(StringView& filename) { m_player_state.loaded_filename = filename; }

    Audio::ClientConnection& client_connection() { return m_player_state.connection; }
    PlaybackManager& manager() { return m_player_state.manager; }

protected:
    virtual ~Player() = default;

    PlayerState m_player_state;
    RefPtr<PlaylistModel> m_playlist_model;
};
