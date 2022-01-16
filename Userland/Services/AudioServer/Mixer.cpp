/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Mixer.h"
#include "AK/Format.h"
#include <AK/Array.h>
#include <AK/MemoryStream.h>
#include <AK/NumericLimits.h>
#include <AudioServer/ClientConnection.h>
#include <AudioServer/Mixer.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/Timer.h>
#include <pthread.h>
#include <stdlib.h>
#include <sys/ioctl.h>

namespace AudioServer {

u8 Mixer::m_zero_filled_buffer[4096];

Mixer::Mixer(NonnullRefPtr<Core::ConfigFile> config)
    : m_device(Core::File::construct("/dev/audio", this))
    , m_sound_thread(Threading::Thread::construct(
          [this] {
              mix();
              return 0;
          },
          "AudioServer[mixer]"))
    , m_config(move(config))
{
    if (!m_device->open(Core::OpenMode::WriteOnly)) {
        dbgln("Can't open audio device: {}", m_device->error_string());
        return;
    }

    m_muted = m_config->read_bool_entry("Master", "Mute", false);
    m_main_volume = static_cast<double>(m_config->read_num_entry("Master", "Volume", 100)) / 100.0;

    m_sound_thread->start();
}

Mixer::~Mixer()
{
}

NonnullRefPtr<ClientAudioStream> Mixer::create_queue(ClientConnection& client)
{
    auto queue = adopt_ref(*new ClientAudioStream(client));
    m_pending_mutex.lock();

    m_pending_mixing.append(*queue);

    m_pending_mutex.unlock();
    // Signal the mixer thread to start back up, in case nobody was connected before.
    m_mixing_necessary.signal();

    return queue;
}

void Mixer::mix()
{
    decltype(m_pending_mixing) active_mix_queues;

    for (;;) {
        m_pending_mutex.lock();
        // While we have nothing to mix, wait on the condition.
        m_mixing_necessary.wait_while([this, &active_mix_queues]() { return m_pending_mixing.is_empty() && active_mix_queues.is_empty(); });
        if (!m_pending_mixing.is_empty()) {
            active_mix_queues.extend(move(m_pending_mixing));
            m_pending_mixing.clear();
        }
        m_pending_mutex.unlock();

        active_mix_queues.remove_all_matching([&](auto& entry) { return !entry->client(); });

        Audio::Sample mixed_buffer[1024];
        auto mixed_buffer_length = (int)(sizeof(mixed_buffer) / sizeof(Audio::Sample));

        m_main_volume.advance_time();

        int active_queues = 0;
        // Mix the buffers together into the output
        for (auto& queue : active_mix_queues) {
            if (!queue->client()) {
                queue->clear();
                continue;
            }
            ++active_queues;
            queue->volume().advance_time();

            for (int i = 0; i < mixed_buffer_length; ++i) {
                auto& mixed_sample = mixed_buffer[i];
                Audio::Sample sample;
                if (!queue->get_next_sample(sample))
                    break;
                if (queue->is_muted())
                    continue;
                sample.log_multiply(SAMPLE_HEADROOM);
                sample.log_multiply(queue->volume());
                mixed_sample += sample;
            }
        }

        if (m_muted) {
            m_device->write(m_zero_filled_buffer, sizeof(m_zero_filled_buffer));
        } else {
            Array<u8, 4096> buffer;
            OutputMemoryStream stream { buffer };

            for (int i = 0; i < mixed_buffer_length; ++i) {
                auto& mixed_sample = mixed_buffer[i];

                // Even though it's not realistic, the user expects no sound at 0%.
                if (m_main_volume < 0.01)
                    mixed_sample = Audio::Sample { 0 };
                else
                    mixed_sample.log_multiply(m_main_volume);
                mixed_sample.clip();

                LittleEndian<i16> out_sample;
                out_sample = mixed_sample.left * NumericLimits<i16>::max();
                stream << out_sample;

                out_sample = mixed_sample.right * NumericLimits<i16>::max();
                stream << out_sample;
            }

            VERIFY(stream.is_end());
            VERIFY(!stream.has_any_error());
            m_device->write(stream.data(), stream.size());
        }
    }
}

void Mixer::set_main_volume(double volume)
{
    if (volume < 0)
        m_main_volume = 0;
    else if (volume > 2)
        m_main_volume = 2;
    else
        m_main_volume = volume;

    m_config->write_num_entry("Master", "Volume", static_cast<int>(volume * 100));
    request_setting_sync();

    ClientConnection::for_each([&](ClientConnection& client) {
        client.did_change_main_mix_volume({}, main_volume());
    });
}

void Mixer::set_muted(bool muted)
{
    if (m_muted == muted)
        return;
    m_muted = muted;

    m_config->write_bool_entry("Master", "Mute", m_muted);
    request_setting_sync();

    ClientConnection::for_each([muted](ClientConnection& client) {
        client.did_change_main_mix_muted_state({}, muted);
    });
}

int Mixer::audiodevice_set_sample_rate(u32 sample_rate)
{
    int code = ioctl(m_device->fd(), SOUNDCARD_IOCTL_SET_SAMPLE_RATE, sample_rate);
    if (code != 0)
        dbgln("Error while setting sample rate to {}: ioctl error: {}", sample_rate, strerror(errno));
    return code;
}

u32 Mixer::audiodevice_get_sample_rate() const
{
    u32 sample_rate = 0;
    int code = ioctl(m_device->fd(), SOUNDCARD_IOCTL_GET_SAMPLE_RATE, &sample_rate);
    if (code != 0)
        dbgln("Error while getting sample rate: ioctl error: {}", strerror(errno));
    return sample_rate;
}

void Mixer::request_setting_sync()
{
    if (m_config_write_timer.is_null() || !m_config_write_timer->is_active()) {
        m_config_write_timer = Core::Timer::create_single_shot(
            AUDIO_CONFIG_WRITE_INTERVAL,
            [this] {
                m_config->sync();
            },
            this);
        m_config_write_timer->start();
    }
}

ClientAudioStream::ClientAudioStream(ClientConnection& client)
    : m_client(client)
{
}

void ClientAudioStream::enqueue(NonnullRefPtr<Audio::Buffer>&& buffer)
{
    m_remaining_samples += buffer->sample_count();
    m_queue.enqueue(move(buffer));
}
}
