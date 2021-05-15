/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibAudio/Buffer.h>
#include <LibAudio/ClientConnection.h>
#include <LibAudio/Loader.h>
#include <LibCore/Timer.h>

#define PLAYBACK_MANAGER_BUFFER_SIZE 48 * KiB
#define PLAYBACK_MANAGER_RATE 44100

class PlaybackManager final {
public:
    PlaybackManager(NonnullRefPtr<Audio::ClientConnection>);
    ~PlaybackManager();

    void play();
    void stop();
    void pause();
    void seek(const int position);
    void loop(bool);
    bool toggle_pause();
    void set_loader(NonnullRefPtr<Audio::Loader>&&);

    int last_seek() const { return m_last_seek; }
    bool is_paused() const { return m_paused; }
    float total_length() const { return m_total_length; }
    RefPtr<Audio::Buffer> current_buffer() const { return m_current_buffer; }

    NonnullRefPtr<Audio::ClientConnection> connection() const { return m_connection; }

    Function<void()> on_update;
    Function<void(Audio::Buffer&)> on_load_sample_buffer;
    Function<void()> on_finished_playing;

private:
    void next_buffer();
    void set_paused(bool);
    void load_next_buffer();
    void remove_dead_buffers();

    bool m_paused { true };
    bool m_loop = { false };
    size_t m_next_ptr { 0 };
    size_t m_last_seek { 0 };
    float m_total_length { 0 };
    RefPtr<Audio::Loader> m_loader { nullptr };
    NonnullRefPtr<Audio::ClientConnection> m_connection;
    RefPtr<Audio::Buffer> m_next_buffer;
    RefPtr<Audio::Buffer> m_current_buffer;
    Vector<RefPtr<Audio::Buffer>> m_buffers;
    RefPtr<Core::Timer> m_timer;
};
