/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, kleines Filmröllchen <malu.bertsch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Mixer.h"
#include <AK/Array.h>
#include <AK/MemoryStream.h>
#include <AK/NumericLimits.h>
#include <AudioServer/ClientConnection.h>
#include <AudioServer/Mixer.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/Timer.h>
#include <pthread.h>

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

    pthread_mutex_init(&m_pending_mutex, nullptr);
    pthread_cond_init(&m_pending_cond, nullptr);

    m_muted = m_config->read_bool_entry("Master", "Mute", false);
    m_main_volume = m_config->read_num_entry("Master", "Volume", 100);

    m_sound_thread->start();
}

Mixer::~Mixer()
{
}

NonnullRefPtr<BufferQueue> Mixer::create_queue(ClientConnection& client)
{
    auto queue = adopt_ref(*new BufferQueue(client));
    pthread_mutex_lock(&m_pending_mutex);
    m_pending_mixing.append(*queue);
    m_added_queue = true;
    pthread_cond_signal(&m_pending_cond);
    pthread_mutex_unlock(&m_pending_mutex);
    return queue;
}

void Mixer::mix()
{
    decltype(m_pending_mixing) active_mix_queues;

    for (;;) {
        if (active_mix_queues.is_empty() || m_added_queue) {
            pthread_mutex_lock(&m_pending_mutex);
            pthread_cond_wait(&m_pending_cond, &m_pending_mutex);
            active_mix_queues.extend(move(m_pending_mixing));
            pthread_mutex_unlock(&m_pending_mutex);
            m_added_queue = false;
        }

        active_mix_queues.remove_all_matching([&](auto& entry) { return !entry->client(); });

        Audio::Frame mixed_buffer[1024];
        auto mixed_buffer_length = (int)(sizeof(mixed_buffer) / sizeof(Audio::Frame));

        // Mix the buffers together into the output
        for (auto& queue : active_mix_queues) {
            if (!queue->client()) {
                queue->clear();
                continue;
            }

            for (int i = 0; i < mixed_buffer_length; ++i) {
                auto& mixed_sample = mixed_buffer[i];
                Audio::Frame sample;
                if (!queue->get_next_sample(sample))
                    break;
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

                mixed_sample.scale(m_main_volume);
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

void Mixer::set_main_volume(int volume)
{
    if (volume < 0)
        m_main_volume = 0;
    else if (volume > 200)
        m_main_volume = 200;
    else
        m_main_volume = volume;

    m_config->write_num_entry("Master", "Volume", volume);
    request_setting_sync();

    ClientConnection::for_each([&](ClientConnection& client) {
        client.did_change_main_mix_volume({}, m_main_volume);
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
        client.did_change_muted_state({}, muted);
    });
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

BufferQueue::BufferQueue(ClientConnection& client)
    : m_client(client)
{
}

void BufferQueue::enqueue(NonnullRefPtr<Audio::Buffer>&& buffer)
{
    m_remaining_samples += buffer->sample_count();
    m_queue.enqueue(move(buffer));
}
}
