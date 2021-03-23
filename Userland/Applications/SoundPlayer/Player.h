/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include "PlaybackManager.h"
#include "VisualizationBase.h"
#include <AK/RefPtr.h>

struct PlayerState {
    bool is_paused;
    bool is_stopped;
    bool has_loaded_file;
    bool is_looping;
    double volume;
    Audio::ClientConnection& connection;
    PlaybackManager& manager;
    StringView loaded_filename;
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
    bool looping() const { return m_player_state.is_looping; }
    StringView& loaded_filename() { return m_player_state.loaded_filename; }

    virtual void set_stopped(bool stopped) { m_player_state.is_stopped = stopped; }
    virtual void set_paused(bool paused) { m_player_state.is_paused = paused; }
    virtual void set_has_loaded_file(bool loaded) { m_player_state.has_loaded_file = loaded; }
    virtual void set_volume(double volume) { m_player_state.volume = volume; }
    virtual void set_looping(bool loop)
    {
        m_player_state.is_looping = loop;
        manager().loop(loop);
    }
    virtual void set_loaded_filename(StringView& filename) { m_player_state.loaded_filename = filename; }

    Audio::ClientConnection& client_connection() { return m_player_state.connection; }
    PlaybackManager& manager() { return m_player_state.manager; }

protected:
    PlayerState m_player_state;
};
