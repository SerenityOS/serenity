/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/FixedArray.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/OwnPtr.h>
#include <LibAudio/Queue.h>
#include <LibAudio/UserSampleQueue.h>
#include <LibCore/EventLoop.h>
#include <LibCore/Object.h>
#include <LibIPC/ConnectionToServer.h>
#include <LibThreading/Mutex.h>
#include <LibThreading/Thread.h>
#include <Userland/Services/AudioServer/AudioClientEndpoint.h>
#include <Userland/Services/AudioServer/AudioServerEndpoint.h>

namespace Audio {

class ConnectionFromClient final
    : public IPC::ConnectionToServer<AudioClientEndpoint, AudioServerEndpoint>
    , public AudioClientEndpoint {
    IPC_CLIENT_CONNECTION(ConnectionFromClient, "/tmp/portal/audio")
public:
    virtual ~ConnectionFromClient() override;

    // Both of these APIs are for convenience and when you don't care about real-time behavior.
    // They will not work properly in conjunction with realtime_enqueue.
    // If you don't refill the buffer in time with this API, the last shared buffer write is zero-padded to play all of the samples.
    template<ArrayLike<Sample> Samples>
    ErrorOr<void> async_enqueue(Samples&& samples)
    {
        return async_enqueue(TRY(FixedArray<Sample>::try_create(samples.span())));
    }

    ErrorOr<void> async_enqueue(FixedArray<Sample>&& samples);

    void clear_client_buffer();

    // Returns immediately with the appropriate status if the buffer is full; use in conjunction with remaining_buffers to get low latency.
    ErrorOr<void, AudioQueue::QueueStatus> realtime_enqueue(Array<Sample, AUDIO_BUFFER_SIZE> samples);

    // This information can be deducted from the shared audio buffer.
    unsigned total_played_samples() const;
    // How many samples remain in m_enqueued_samples.
    unsigned remaining_samples();
    // How many buffers (i.e. short sample arrays) the server hasn't played yet.
    // Non-realtime code needn't worry about this.
    size_t remaining_buffers() const;

    virtual void die() override;

    Function<void(bool muted)> on_main_mix_muted_state_change;
    Function<void(double volume)> on_main_mix_volume_change;
    Function<void(double volume)> on_client_volume_change;

private:
    ConnectionFromClient(NonnullOwnPtr<Core::Stream::LocalSocket>);

    virtual void main_mix_muted_state_changed(bool) override;
    virtual void main_mix_volume_changed(double) override;
    virtual void client_volume_changed(double) override;

    // We use this to perform the audio enqueuing on the background thread's event loop
    virtual void custom_event(Core::CustomEvent&) override;

    // FIXME: This should be called every time the sample rate changes, but we just cautiously call it on every non-realtime enqueue.
    void update_good_sleep_time();

    // Shared audio buffer: both server and client constantly read and write to/from this.
    // This needn't be mutex protected: it's internally multi-threading aware.
    OwnPtr<AudioQueue> m_buffer;

    // The queue of non-realtime audio provided by the user.
    NonnullOwnPtr<UserSampleQueue> m_user_queue;

    NonnullRefPtr<Threading::Thread> m_background_audio_enqueuer;
    Core::EventLoop* m_enqueuer_loop;
    Threading::Mutex m_enqueuer_loop_destruction;
    Atomic<bool> m_audio_enqueuer_active { false };

    // A good amount of time to sleep when the queue is full.
    // (Only used for non-realtime enqueues)
    timespec m_good_sleep_time {};
};

}
