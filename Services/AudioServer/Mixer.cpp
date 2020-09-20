/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Array.h>
#include <AK/MemoryStream.h>
#include <AK/NumericLimits.h>
#include <AudioServer/ClientConnection.h>
#include <AudioServer/Mixer.h>
#include <pthread.h>

namespace AudioServer {

Mixer::Mixer()
    : m_device(Core::File::construct("/dev/audio", this))
    , m_sound_thread(
          [this] {
              mix();
              return 0;
          },
          "AudioServer[mixer]")
{
    if (!m_device->open(Core::IODevice::WriteOnly)) {
        dbgprintf("Can't open audio device: %s\n", m_device->error_string());
        return;
    }

    pthread_mutex_init(&m_pending_mutex, nullptr);
    pthread_cond_init(&m_pending_cond, nullptr);

    m_zero_filled_buffer = (u8*)malloc(4096);
    bzero(m_zero_filled_buffer, 4096);
    m_sound_thread.start();
}

Mixer::~Mixer()
{
}

NonnullRefPtr<BufferQueue> Mixer::create_queue(ClientConnection& client)
{
    auto queue = adopt(*new BufferQueue(client));
    pthread_mutex_lock(&m_pending_mutex);
    m_pending_mixing.append(*queue);
    pthread_cond_signal(&m_pending_cond);
    pthread_mutex_unlock(&m_pending_mutex);
    return queue;
}

void Mixer::mix()
{
    decltype(m_pending_mixing) active_mix_queues;

    for (;;) {
        if (active_mix_queues.is_empty()) {
            pthread_mutex_lock(&m_pending_mutex);
            pthread_cond_wait(&m_pending_cond, &m_pending_mutex);
            active_mix_queues.append(move(m_pending_mixing));
            pthread_mutex_unlock(&m_pending_mutex);
        }

        active_mix_queues.remove_all_matching([&](auto& entry) { return !entry->client(); });

        Audio::Sample mixed_buffer[1024];
        auto mixed_buffer_length = (int)(sizeof(mixed_buffer) / sizeof(Audio::Sample));

        // Mix the buffers together into the output
        for (auto& queue : active_mix_queues) {
            if (!queue->client()) {
                queue->clear();
                continue;
            }

            for (int i = 0; i < mixed_buffer_length; ++i) {
                auto& mixed_sample = mixed_buffer[i];
                Audio::Sample sample;
                if (!queue->get_next_sample(sample))
                    break;
                mixed_sample += sample;
            }
        }

        if (m_muted) {
            m_device->write(m_zero_filled_buffer, 4096);
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

            ASSERT(stream.is_end());
            ASSERT(!stream.has_any_error());
            m_device->write(stream.data(), stream.size());
        }
    }
}

void Mixer::set_main_volume(int volume)
{
    m_main_volume = volume;
    ClientConnection::for_each([volume](ClientConnection& client) {
        client.did_change_main_mix_volume({}, volume);
    });
}

void Mixer::set_muted(bool muted)
{
    if (m_muted == muted)
        return;
    m_muted = muted;
    ClientConnection::for_each([muted](ClientConnection& client) {
        client.did_change_muted_state({}, muted);
    });
}

BufferQueue::BufferQueue(ClientConnection& client)
    : m_client(client.make_weak_ptr())
{
}

void BufferQueue::enqueue(NonnullRefPtr<Audio::Buffer>&& buffer)
{
    m_remaining_samples += buffer->sample_count();
    m_queue.enqueue(move(buffer));
}
}
