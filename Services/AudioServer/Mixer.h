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

#include "ClientConnection.h"
#include <AK/Badge.h>
#include <AK/ByteBuffer.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/Queue.h>
#include <AK/RefCounted.h>
#include <AK/WeakPtr.h>
#include <LibAudio/Buffer.h>
#include <LibCore/File.h>
#include <LibThread/Lock.h>
#include <LibThread/Thread.h>

namespace AudioServer {

class ClientConnection;

class BufferQueue : public RefCounted<BufferQueue> {
public:
    explicit BufferQueue(ClientConnection&);
    ~BufferQueue() { }

    bool is_full() const { return m_queue.size() >= 3; }
    void enqueue(NonnullRefPtr<Audio::Buffer>&&);

    bool get_next_sample(Audio::Sample& sample)
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
            m_client->did_finish_playing_buffer({}, m_current->shbuf_id());
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
            return m_current->shbuf_id();
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
    pthread_mutex_t m_pending_mutex;
    pthread_cond_t m_pending_cond;

    RefPtr<Core::File> m_device;

    LibThread::Thread m_sound_thread;

    bool m_muted { false };
    int m_main_volume { 100 };

    u8* m_zero_filled_buffer { nullptr };

    void mix();
};
}
