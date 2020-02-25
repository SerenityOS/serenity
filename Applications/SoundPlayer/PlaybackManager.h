/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/Vector.h>
#include <LibAudio/ClientConnection.h>
#include <LibAudio/WavLoader.h>
#include <LibCore/Timer.h>

#define PLAYBACK_MANAGER_BUFFER_SIZE 64 * KB
#define PLAYBACK_MANAGER_RATE 44100

class PlaybackManager final {
public:
    PlaybackManager(NonnullRefPtr<Audio::ClientConnection>);
    ~PlaybackManager();

    void play();
    void stop();
    void pause();
    void seek(const int position);
    bool toggle_pause();
    void set_loader(OwnPtr<Audio::WavLoader>&&);

    int last_seek() const { return m_last_seek; }
    bool is_paused() const { return m_paused; }
    float total_length() const { return m_total_length; }
    RefPtr<Audio::Buffer> current_buffer() const { return m_current_buffer; }

    NonnullRefPtr<Audio::ClientConnection> connection() const { return m_connection; }

    Function<void()> on_update;

private:
    void next_buffer();
    void set_paused(bool);
    void load_next_buffer();
    void remove_dead_buffers();

    bool m_paused { true };
    size_t m_next_ptr { 0 };
    size_t m_last_seek { 0 };
    float m_total_length { 0 };
    OwnPtr<Audio::WavLoader> m_loader { nullptr };
    NonnullRefPtr<Audio::ClientConnection> m_connection;
    RefPtr<Audio::Buffer> m_next_buffer;
    RefPtr<Audio::Buffer> m_current_buffer;
    Vector<RefPtr<Audio::Buffer>> m_buffers;
    RefPtr<Core::Timer> m_timer;
};
