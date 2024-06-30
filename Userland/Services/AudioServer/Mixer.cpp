/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Mixer.h"
#include <AK/Array.h>
#include <AK/Format.h>
#include <AK/MemoryStream.h>
#include <AK/NumericLimits.h>
#include <AudioServer/ConnectionFromClient.h>
#include <AudioServer/ConnectionFromManagerClient.h>
#include <AudioServer/Mixer.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/Timer.h>
#include <pthread.h>
#include <sys/ioctl.h>

namespace AudioServer {

Mixer::Mixer(NonnullRefPtr<Core::ConfigFile> config, OwnPtr<Core::File> device)
    : m_device(move(device))
    , m_sound_thread(Threading::Thread::construct(
          [this] {
              mix();
              return 0;
          },
          "AudioServer[mixer]"sv))
    , m_config(move(config))
{
    m_muted = m_config->read_bool_entry("Master", "Mute", false);
    m_main_volume = static_cast<double>(m_config->read_num_entry("Master", "Volume", 100)) / 100.0;

    m_sound_thread->start();
}

NonnullRefPtr<ClientAudioStream> Mixer::create_queue(ConnectionFromClient& client)
{
    auto queue = adopt_ref(*new ClientAudioStream(client));
    queue->set_sample_rate(audiodevice_get_sample_rate());
    {
        Threading::MutexLocker const locker(m_pending_mutex);
        m_pending_mixing.append(*queue);
    }
    // Signal the mixer thread to start back up, in case nobody was connected before.
    m_mixing_necessary.signal();

    return queue;
}

void Mixer::mix()
{
    decltype(m_pending_mixing) active_mix_queues;

    for (;;) {
        {
            Threading::MutexLocker const locker(m_pending_mutex);
            // While we have nothing to mix, wait on the condition.
            m_mixing_necessary.wait_while([this, &active_mix_queues]() { return m_pending_mixing.is_empty() && active_mix_queues.is_empty(); });
            if (!m_pending_mixing.is_empty()) {
                active_mix_queues.extend(move(m_pending_mixing));
                m_pending_mixing.clear();
            }
        }

        active_mix_queues.remove_all_matching([&](auto& entry) { return !entry->is_connected(); });

        Array<Audio::Sample, HARDWARE_BUFFER_SIZE> mixed_buffer;

        m_main_volume.advance_time();

        // Mix the buffers together into the output
        for (auto& queue : active_mix_queues) {
            if (!queue->client().has_value()) {
                queue->clear();
                continue;
            }
            queue->volume().advance_time();

            // FIXME: Perform sample extraction and mixing in two separate loops so they can be more easily vectorized.
            for (auto& mixed_sample : mixed_buffer) {
                auto sample_or_error = queue->get_next_sample(audiodevice_get_sample_rate());
                if (sample_or_error.is_error())
                    break;
                if (queue->is_muted())
                    continue;
                auto sample = sample_or_error.release_value();
                sample.log_multiply(SAMPLE_HEADROOM);
                sample.log_multiply(static_cast<float>(queue->volume()));
                mixed_sample += sample;
            }
        }

        // Even though it's not realistic, the user expects no sound at 0%.
        if (m_muted || m_main_volume < 0.01) {
            if (m_device)
                m_device->write_until_depleted(m_zero_filled_buffer).release_value_but_fixme_should_propagate_errors();
        } else {
            FixedMemoryStream stream { m_stream_buffer.span() };

            for (auto& mixed_sample : mixed_buffer) {
                mixed_sample.log_multiply(static_cast<float>(m_main_volume));
                mixed_sample.clip();

                LittleEndian<i16> out_sample;
                out_sample = static_cast<i16>(mixed_sample.left * NumericLimits<i16>::max());
                MUST(stream.write_value(out_sample));

                out_sample = static_cast<i16>(mixed_sample.right * NumericLimits<i16>::max());
                MUST(stream.write_value(out_sample));
            }

            auto buffered_bytes = MUST(stream.tell());
            VERIFY(buffered_bytes == m_stream_buffer.size());
            if (m_device)
                m_device->write_until_depleted({ m_stream_buffer.data(), buffered_bytes })
                    .release_value_but_fixme_should_propagate_errors();
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

    ConnectionFromManagerClient::for_each([&](auto& client) {
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

    ConnectionFromManagerClient::for_each([muted](auto& client) {
        client.did_change_main_mix_muted_state({}, muted);
    });
}

int Mixer::audiodevice_set_sample_rate(u32 sample_rate)
{
    if (!m_device)
        return ENOENT;

    int code = ioctl(m_device->fd(), SOUNDCARD_IOCTL_SET_SAMPLE_RATE, sample_rate);
    if (code != 0)
        dbgln("Error while setting sample rate to {}: ioctl error: {}", sample_rate, strerror(errno));
    // Note that the effective sample rate may be different depending on device restrictions.
    // Therefore, we delete our cache, but for efficency don't immediately read the sample rate back.
    m_cached_sample_rate = {};
    return code;
}

u32 Mixer::audiodevice_get_sample_rate() const
{
    if (m_cached_sample_rate.has_value())
        return m_cached_sample_rate.value();

    // We pretend that a non-existent device has a common sample rate (instead of returning something like 0 that would break clients).
    if (!m_device)
        return 44100;

    u32 sample_rate = 0;
    int code = ioctl(m_device->fd(), SOUNDCARD_IOCTL_GET_SAMPLE_RATE, &sample_rate);
    if (code != 0)
        dbgln("Error while getting sample rate: ioctl error: {}", strerror(errno));
    else
        m_cached_sample_rate = sample_rate;
    return sample_rate;
}

void Mixer::request_setting_sync()
{
    if (m_config_write_timer.is_null() || !m_config_write_timer->is_active()) {
        m_config_write_timer = Core::Timer::create_single_shot(
            AUDIO_CONFIG_WRITE_INTERVAL,
            [this] {
                if (auto result = m_config->sync(); result.is_error())
                    dbgln("Failed to write audio mixer config: {}", result.error());
            },
            this);
        m_config_write_timer->start();
    }
}

}
