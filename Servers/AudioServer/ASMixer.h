#pragma once

#include <AK/ByteBuffer.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/Queue.h>
#include <AK/RefCounted.h>
#include <AK/WeakPtr.h>
#include <LibAudio/ABuffer.h>
#include <LibCore/CFile.h>
#include <LibThread/Lock.h>
#include <LibThread/Thread.h>

class ASClientConnection;

class ASBufferQueue : public RefCounted<ASBufferQueue> {
public:
    explicit ASBufferQueue(ASClientConnection&);
    ~ASBufferQueue() {}

    bool is_full() const { return m_queue.size() >= 3; }
    void enqueue(NonnullRefPtr<ABuffer>&&);

    bool get_next_sample(ASample& sample)
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
            m_current = nullptr;
            m_position = 0;
        }
        return true;
    }

    ASClientConnection* client() { return m_client.ptr(); }

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
    int get_playing_buffer() const {
        if(m_current) return m_current->shared_buffer_id();
        return -1;
    }

private:
    RefPtr<ABuffer> m_current;
    Queue<NonnullRefPtr<ABuffer>> m_queue;
    int m_position { 0 };
    int m_remaining_samples { 0 };
    int m_played_samples { 0 };
    bool m_paused { false };
    WeakPtr<ASClientConnection> m_client;
};

class ASMixer : public CObject {
    C_OBJECT(ASMixer)
public:
    ASMixer();
    virtual ~ASMixer() override;

    NonnullRefPtr<ASBufferQueue> create_queue(ASClientConnection&);

    int main_volume() const { return m_main_volume; }
    void set_main_volume(int volume) { m_main_volume = volume; }

    bool is_muted() const { return m_muted; }
    void set_muted(bool);

private:
    Vector<NonnullRefPtr<ASBufferQueue>> m_pending_mixing;

    RefPtr<CFile> m_device;
    LibThread::Lock m_lock;

    LibThread::Thread m_sound_thread;

    bool m_muted { false };
    int m_main_volume { 100 };

    u8* m_zero_filled_buffer { nullptr };

    void mix();
};
