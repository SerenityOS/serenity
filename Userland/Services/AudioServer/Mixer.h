/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ClientConnection.h"
#include "FadingProperty.h"
#include <AK/Atomic.h>
#include <AK/Badge.h>
#include <AK/ByteBuffer.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/Queue.h>
#include <AK/RefCounted.h>
#include <AK/WeakPtr.h>
#include <LibAudio/Buffer.h>
#include <LibCore/File.h>
#include <LibCore/Timer.h>
#include <LibThreading/ConditionVariable.h>
#include <LibThreading/Mutex.h>
#include <LibThreading/Thread.h>
#include <sys/types.h>

namespace AudioServer {

// Headroom, i.e. fixed attenuation for all audio streams.
// This is to prevent clipping when two streams with low headroom (e.g. normalized & compressed) are playing.
constexpr double SAMPLE_HEADROOM = 0.7;

class ClientConnection;

class ClientAudioStream : public RefCounted<ClientAudioStream> {
public:
    explicit ClientAudioStream(ClientConnection&);
    ~ClientAudioStream() { }

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
        if (m_remaining_samples > 0)
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

    FadingProperty<double>& volume() { return m_volume; }
    double volume() const { return m_volume; }
    void set_volume(double const volume) { m_volume = volume; }
    bool is_muted() const { return m_muted; }
    void set_muted(bool muted) { m_muted = muted; }

private:
    RefPtr<Audio::Buffer> m_current;
    Queue<NonnullRefPtr<Audio::Buffer>> m_queue;
    int m_position { 0 };
    int m_remaining_samples { 0 };
    int m_played_samples { 0 };
    bool m_paused { false };
    bool m_muted { false };

    WeakPtr<ClientConnection> m_client;
    FadingProperty<double> m_volume { 1 };
};

class Mixer : public Core::Object {
    C_OBJECT(Mixer)
public:
    virtual ~Mixer() override;

    NonnullRefPtr<ClientAudioStream> create_queue(ClientConnection&);

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
