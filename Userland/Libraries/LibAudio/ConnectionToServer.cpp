/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Atomic.h>
#include <AK/Debug.h>
#include <AK/Format.h>
#include <AK/OwnPtr.h>
#include <AK/Time.h>
#include <AK/Types.h>
#include <LibAudio/ConnectionToServer.h>
#include <LibAudio/Queue.h>
#include <LibAudio/UserSampleQueue.h>
#include <LibCore/Event.h>
#include <LibThreading/Mutex.h>
#include <Userland/Services/AudioServer/AudioClientEndpoint.h>
#include <sched.h>
#include <time.h>

namespace Audio {

ConnectionToServer::ConnectionToServer(NonnullOwnPtr<Core::LocalSocket> socket)
    : IPC::ConnectionToServer<AudioClientEndpoint, AudioServerEndpoint>(*this, move(socket))
    , m_buffer(make<AudioQueue>(MUST(AudioQueue::create())))
    , m_user_queue(make<UserSampleQueue>())
    , m_background_audio_enqueuer(Threading::Thread::construct([this]() {
        // All the background thread does is run an event loop.
        Core::EventLoop enqueuer_loop;
        m_enqueuer_loop = &enqueuer_loop;
        enqueuer_loop.exec();
        {
            Threading::MutexLocker const locker(m_enqueuer_loop_destruction);
            m_enqueuer_loop = nullptr;
        }
        return (intptr_t) nullptr;
    }))
{
    update_good_sleep_time();
    async_pause_playback();
    set_buffer(*m_buffer);
}

ConnectionToServer::~ConnectionToServer()
{
    die();
}

void ConnectionToServer::die()
{
    {
        Threading::MutexLocker const locker(m_enqueuer_loop_destruction);
        // We're sometimes getting here after the other thread has already exited and its event loop does no longer exist.
        if (m_enqueuer_loop != nullptr) {
            m_enqueuer_loop->wake();
            m_enqueuer_loop->quit(0);
        }
    }
    if (m_background_audio_enqueuer->is_started())
        (void)m_background_audio_enqueuer->join();
}

ErrorOr<void> ConnectionToServer::async_enqueue(FixedArray<Sample>&& samples)
{
    if (!m_background_audio_enqueuer->is_started()) {
        m_background_audio_enqueuer->start();
        // Wait until the enqueuer has constructed its loop. A pseudo-spinlock is fine since this happens as soon as the other thread gets scheduled.
        while (!m_enqueuer_loop)
            usleep(1);
        TRY(m_background_audio_enqueuer->set_priority(THREAD_PRIORITY_MAX));
    }

    m_user_queue->append(move(samples));
    // Wake the background thread to make sure it starts enqueuing audio.
    m_enqueuer_loop->post_event(*this, make<Core::CustomEvent>(0));
    m_enqueuer_loop->wake();
    async_start_playback();

    return {};
}

void ConnectionToServer::clear_client_buffer()
{
    m_user_queue->clear();
}

void ConnectionToServer::update_good_sleep_time()
{
    auto sample_rate = static_cast<double>(get_self_sample_rate());
    auto buffer_play_time_ns = 1'000'000'000.0 / (sample_rate / static_cast<double>(AUDIO_BUFFER_SIZE));
    // A factor of 1 should be good for now.
    m_good_sleep_time = Duration::from_nanoseconds(static_cast<unsigned>(buffer_play_time_ns)).to_timespec();
}

void ConnectionToServer::set_self_sample_rate(u32 sample_rate)
{
    IPC::ConnectionToServer<AudioClientEndpoint, AudioServerEndpoint>::set_self_sample_rate(sample_rate);
    update_good_sleep_time();
}

// Non-realtime audio writing loop
void ConnectionToServer::custom_event(Core::CustomEvent&)
{
    Array<Sample, AUDIO_BUFFER_SIZE> next_chunk;
    while (true) {
        if (m_user_queue->is_empty()) {
            dbgln_if(AUDIO_DEBUG, "Reached end of provided audio data, going to sleep");
            break;
        }

        auto available_samples = min(AUDIO_BUFFER_SIZE, m_user_queue->size());
        for (size_t i = 0; i < available_samples; ++i)
            next_chunk[i] = (*m_user_queue)[i];

        m_user_queue->discard_samples(available_samples);

        // FIXME: Could we receive interrupts in a good non-IPC way instead?
        auto result = m_buffer->blocking_enqueue(next_chunk, [this]() {
            nanosleep(&m_good_sleep_time, nullptr);
        });
        if (result.is_error())
            dbgln("Error while writing samples to shared buffer: {}", result.error());
    }
}

ErrorOr<void, AudioQueue::QueueStatus> ConnectionToServer::realtime_enqueue(Array<Sample, AUDIO_BUFFER_SIZE> samples)
{
    return m_buffer->enqueue(samples);
}

ErrorOr<void> ConnectionToServer::blocking_realtime_enqueue(Array<Sample, AUDIO_BUFFER_SIZE> samples, Function<void()> wait_function)
{
    return m_buffer->blocking_enqueue(samples, move(wait_function));
}

unsigned ConnectionToServer::total_played_samples() const
{
    return m_buffer->weak_tail() * AUDIO_BUFFER_SIZE;
}

unsigned ConnectionToServer::remaining_samples()
{
    return static_cast<unsigned>(m_user_queue->remaining_samples());
}

size_t ConnectionToServer::remaining_buffers() const
{
    return m_buffer->size() - m_buffer->weak_remaining_capacity();
}

bool ConnectionToServer::can_enqueue() const
{
    return m_buffer->can_enqueue();
}

void ConnectionToServer::client_volume_changed(double volume)
{
    if (on_client_volume_change)
        on_client_volume_change(volume);
}

}
