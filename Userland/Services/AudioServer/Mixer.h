/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ConnectionFromClient.h"
#include "FadingProperty.h"
#include <AK/Atomic.h>
#include <AK/Badge.h>
#include <AK/ByteBuffer.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/Queue.h>
#include <AK/RefCounted.h>
#include <AK/WeakPtr.h>
#include <LibAudio/Queue.h>
#include <LibCore/File.h>
#include <LibCore/Timer.h>
#include <LibThreading/ConditionVariable.h>
#include <LibThreading/Mutex.h>
#include <LibThreading/Thread.h>
#include <sys/types.h>

namespace AudioServer {

// Headroom, i.e. fixed attenuation for all audio streams.
// This is to prevent clipping when two streams with low headroom (e.g. normalized & compressed) are playing.
constexpr double SAMPLE_HEADROOM = 0.95;

class ConnectionFromClient;

class ClientAudioStream : public RefCounted<ClientAudioStream> {
public:
    explicit ClientAudioStream(ConnectionFromClient&);
    ~ClientAudioStream() = default;

    bool get_next_sample(Audio::Sample& sample)
    {
        if (m_paused)
            return false;

        if (m_in_chunk_location >= m_current_audio_chunk.size()) {
            // FIXME: We should send a did_misbehave to the client if the queue is empty,
            //        but the lifetimes involved mean that we segfault if we try to do that.
            auto result = m_buffer->try_dequeue();
            if (result.is_error()) {
                if (result.error() == Audio::AudioQueue::QueueStatus::Empty)
                    dbgln("Audio client can't keep up!");

                return false;
            }
            m_current_audio_chunk = result.release_value();
            m_in_chunk_location = 0;
        }

        sample = m_current_audio_chunk[m_in_chunk_location++];

        return true;
    }

    ConnectionFromClient* client() { return m_client.ptr(); }

    void set_buffer(OwnPtr<Audio::AudioQueue> buffer) { m_buffer = move(buffer); }

    void clear()
    {
        ErrorOr<Array<Audio::Sample, Audio::AUDIO_BUFFER_SIZE>, Audio::AudioQueue::QueueStatus> result = Audio::AudioQueue::QueueStatus::Invalid;
        do {
            result = m_buffer->try_dequeue();
        } while (result.is_error() && result.error() != Audio::AudioQueue::QueueStatus::Empty);
    }

    void set_paused(bool paused) { m_paused = paused; }

    FadingProperty<double>& volume() { return m_volume; }
    double volume() const { return m_volume; }
    void set_volume(double const volume) { m_volume = volume; }
    bool is_muted() const { return m_muted; }
    void set_muted(bool muted) { m_muted = muted; }

private:
    OwnPtr<Audio::AudioQueue> m_buffer;
    Array<Audio::Sample, Audio::AUDIO_BUFFER_SIZE> m_current_audio_chunk;
    size_t m_in_chunk_location;

    bool m_paused { true };
    bool m_muted { false };

    WeakPtr<ConnectionFromClient> m_client;
    FadingProperty<double> m_volume { 1 };
};

class Mixer : public Core::Object {
    C_OBJECT(Mixer)
public:
    virtual ~Mixer() override = default;

    NonnullRefPtr<ClientAudioStream> create_queue(ConnectionFromClient&);

    // To the outside world, we pretend that the target volume is already reached, even though it may be still fading.
    double main_volume() const { return m_main_volume.target(); }
    void set_main_volume(double volume);

    bool is_muted() const { return m_muted; }
    void set_muted(bool);

    int audiodevice_set_sample_rate(u32 sample_rate);
    u32 audiodevice_get_sample_rate() const;

private:
    Mixer(NonnullRefPtr<Core::ConfigFile> config);

    void request_setting_sync();

    Vector<NonnullRefPtr<ClientAudioStream>> m_pending_mixing;
    Threading::Mutex m_pending_mutex;
    Threading::ConditionVariable m_mixing_necessary { m_pending_mutex };

    RefPtr<Core::File> m_device;

    NonnullRefPtr<Threading::Thread> m_sound_thread;

    bool m_muted { false };
    FadingProperty<double> m_main_volume { 1 };

    NonnullRefPtr<Core::ConfigFile> m_config;
    RefPtr<Core::Timer> m_config_write_timer;

    static u8 m_zero_filled_buffer[4096];

    void mix();
};

// Interval in ms when the server tries to save its configuration to disk.
constexpr unsigned AUDIO_CONFIG_WRITE_INTERVAL = 2000;

}
