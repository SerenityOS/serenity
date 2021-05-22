/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ClientConnection.h"
#include <AK/Atomic.h>
#include <AK/Badge.h>
#include <AK/ByteBuffer.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/Queue.h>
#include <AK/RefCounted.h>
#include <AK/WeakPtr.h>
#include <LibAudio/Buffer.h>
#include <LibCore/File.h>
#include <LibThreading/Lock.h>
#include <LibThreading/Thread.h>

namespace AudioServer {

class ClientConnection;

class BufferQueue : public RefCounted<BufferQueue> {
public:
    explicit BufferQueue(ClientConnection&);
    ~BufferQueue() { }

    bool is_full() const { return m_queue.size() >= 3; }
    void enqueue(NonnullRefPtr<Audio::Buffer>&&);

    bool get_next_sample(Audio::Frame& sample)
    {
        if (m_paused)
            return false;

        while (!m_current && !m_queue.is_empty())
            m_current = m_queue.dequeue();

        if (!m_current)
            return false;

        sample = m_current->samples()[m_position++];
        --m_remaining_samples;
        ++m_played_samples;

        if (m_position >= m_current->sample_count()) {
            m_client->did_finish_playing_buffer({}, m_current->id());
            m_current = nullptr;
            m_position = 0;
        }
        return true;
    }

    ClientConnection* client() { return m_client.ptr(); }

    void clear(bool paused = false)
    {
        m_queue.clear();
        m_position = 0;
        m_remaining_samples = 0;
        m_played_samples = 0;
        m_current = nullptr;
        m_paused = paused;
    }

    void set_paused(bool paused)
    {
        m_paused = paused;
    }

    int get_remaining_samples() const { return m_remaining_samples; }
    int get_played_samples() const { return m_played_samples; }
    int get_playing_buffer() const
    {
        if (m_current)
            return m_current->id();
        return -1;
    }

private:
    RefPtr<Audio::Buffer> m_current;
    Queue<NonnullRefPtr<Audio::Buffer>> m_queue;
    int m_position { 0 };
    int m_remaining_samples { 0 };
    int m_played_samples { 0 };
    bool m_paused { false };
    WeakPtr<ClientConnection> m_client;
};

class Mixer : public Core::Object {
    C_OBJECT(Mixer)
public:
    Mixer();
    virtual ~Mixer() override;

    NonnullRefPtr<BufferQueue> create_queue(ClientConnection&);

    int main_volume() const { return m_main_volume; }
    void set_main_volume(int volume);

    bool is_muted() const { return m_muted; }
    void set_muted(bool);

private:
    Vector<NonnullRefPtr<BufferQueue>> m_pending_mixing;
    Atomic<bool> m_added_queue { false };
    pthread_mutex_t m_pending_mutex;
    pthread_cond_t m_pending_cond;

    RefPtr<Core::File> m_device;

    NonnullRefPtr<Threading::Thread> m_sound_thread;

    bool m_muted { false };
    int m_main_volume { 100 };

    u8* m_zero_filled_buffer { nullptr };

    void mix();
};
}
