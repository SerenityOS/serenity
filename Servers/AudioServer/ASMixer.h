#pragma once

#include <AK/ByteBuffer.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/Queue.h>
#include <AK/RefCounted.h>
#include <AK/WeakPtr.h>
#include <LibAudio/ABuffer.h>
#include <LibCore/CFile.h>
#include <LibCore/CLock.h>

class ASClientConnection;

class ASBufferQueue : public RefCounted<ASBufferQueue> {
public:
    explicit ASBufferQueue(ASClientConnection&);
    ~ASBufferQueue() {}

    bool is_full() const { return m_queue.size() >= 3; }
    void enqueue(NonnullRefPtr<ABuffer>&&);

    bool get_next_sample(ASample& sample)
    {
        while (!m_current && !m_queue.is_empty())
            m_current = m_queue.dequeue();
        if (!m_current)
            return false;
        sample = m_current->samples()[m_position++];
        if (m_position >= m_current->sample_count()) {
            m_current = nullptr;
            m_position = 0;
        }
        return true;
    }

    ASClientConnection* client() { return m_client.ptr(); }
    void clear()
    {
        m_queue.clear();
        m_position = 0;
    }

private:
    RefPtr<ABuffer> m_current;
    Queue<NonnullRefPtr<ABuffer>> m_queue;
    int m_position { 0 };
    int m_playing_queued_buffer_id { -1 };
    WeakPtr<ASClientConnection> m_client;
};

class ASMixer : public RefCounted<ASMixer> {
public:
    ASMixer();

    NonnullRefPtr<ASBufferQueue> create_queue(ASClientConnection&);

    int main_volume() const { return m_main_volume; }
    void set_main_volume(int volume) { m_main_volume = volume; }

private:
    Vector<NonnullRefPtr<ASBufferQueue>> m_pending_mixing;

    CFile m_device;
    CLock m_lock;

    int m_main_volume { 100 };

    void mix();
};
