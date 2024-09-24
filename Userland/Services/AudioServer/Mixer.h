/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "ClientAudioStream.h"
#include "ConnectionFromClient.h"
#include "FadingProperty.h"
#include <AK/Atomic.h>
#include <AK/Badge.h>
#include <AK/ByteBuffer.h>
#include <AK/Debug.h>
#include <AK/Queue.h>
#include <AK/RefCounted.h>
#include <AK/WeakPtr.h>
#include <LibAudio/Queue.h>
#include <LibAudio/Resampler.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/File.h>
#include <LibCore/Timer.h>
#include <LibThreading/ConditionVariable.h>
#include <LibThreading/Mutex.h>
#include <LibThreading/Thread.h>

namespace AudioServer {

// Headroom, i.e. fixed attenuation for all audio streams.
// This is to prevent clipping when two streams with low headroom (e.g. normalized & compressed) are playing.
constexpr double SAMPLE_HEADROOM = 0.95;
// The size of the buffer in samples that the hardware receives through write() calls to the audio device.
constexpr size_t HARDWARE_BUFFER_SIZE = 512;
// The hardware buffer size in bytes; there's two channels of 16-bit samples.
constexpr size_t HARDWARE_BUFFER_SIZE_BYTES = HARDWARE_BUFFER_SIZE * 2 * sizeof(i16);

class Mixer : public Core::EventReceiver {
    C_OBJECT_ABSTRACT(Mixer)
public:
    static ErrorOr<NonnullRefPtr<Mixer>> try_create(NonnullRefPtr<Core::ConfigFile> config)
    {
        // FIXME: Allow AudioServer to use other audio channels as well
        auto maybe_device = Core::File::open("/dev/audio/0"sv, Core::File::OpenMode::Write);
        OwnPtr<Core::File> device;
        if (maybe_device.is_error())
            dbgln("Couldn't open first audio channel: {}", maybe_device.error());
        else
            device = maybe_device.release_value();
        return adopt_nonnull_ref_or_enomem(new (nothrow) Mixer(move(config), move(device)));
    }

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
    Mixer(NonnullRefPtr<Core::ConfigFile> config, OwnPtr<Core::File> device);

    void request_setting_sync();

    Vector<NonnullRefPtr<ClientAudioStream>> m_pending_mixing;
    Threading::Mutex m_pending_mutex;
    Threading::ConditionVariable m_mixing_necessary { m_pending_mutex };

    OwnPtr<Core::File> m_device;
    mutable Optional<u32> m_cached_sample_rate {};

    NonnullRefPtr<Threading::Thread> m_sound_thread;

    bool m_muted { false };
    FadingProperty<double> m_main_volume { 1 };

    NonnullRefPtr<Core::ConfigFile> m_config;
    RefPtr<Core::Timer> m_config_write_timer;

    Array<u8, HARDWARE_BUFFER_SIZE_BYTES> m_stream_buffer;
    Array<u8, HARDWARE_BUFFER_SIZE_BYTES> const m_zero_filled_buffer {};

    void mix();
};

// Interval in ms when the server tries to save its configuration to disk.
constexpr unsigned AUDIO_CONFIG_WRITE_INTERVAL = 2000;

}
