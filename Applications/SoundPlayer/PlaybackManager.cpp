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

#include "PlaybackManager.h"

PlaybackManager::PlaybackManager(NonnullRefPtr<Audio::ClientConnection> connection)
    : m_connection(connection)
{
    m_timer = Core::Timer::construct(100, [&]() {
        if (!m_loader)
            return;
        next_buffer();
    });
    m_timer->stop();
}

PlaybackManager::~PlaybackManager()
{
}

void PlaybackManager::set_loader(OwnPtr<Audio::WavLoader>&& loader)
{
    stop();
    m_loader = move(loader);
    if (m_loader) {
        m_total_length = m_loader->total_samples() / static_cast<float>(m_loader->sample_rate());
        m_timer->start();
        load_next_buffer();
    } else {
        m_timer->stop();
    }
}

void PlaybackManager::stop()
{
    set_paused(true);
    m_connection->clear_buffer(true);
    m_buffers.clear();
    m_last_seek = 0;
    m_next_buffer = nullptr;
    m_current_buffer = nullptr;
    m_next_ptr = 0;

    if (m_loader)
        m_loader->reset();
}

void PlaybackManager::play()
{
    set_paused(false);
}

void PlaybackManager::seek(const int position)
{
    if (!m_loader)
        return;

    m_last_seek = position;
    bool paused_state = m_paused;
    set_paused(true);

    m_connection->clear_buffer(true);
    m_next_buffer = nullptr;
    m_current_buffer = nullptr;
    m_next_ptr = 0;
    m_buffers.clear();
    m_loader->seek(position);

    if (!paused_state)
        set_paused(false);
}

void PlaybackManager::pause()
{
    set_paused(true);
}

void PlaybackManager::remove_dead_buffers()
{
    int id = m_connection->get_playing_buffer();
    int current_id = -1;
    if (m_current_buffer)
        current_id = m_current_buffer->shbuf_id();

    if (id >= 0 && id != current_id) {
        while (!m_buffers.is_empty()) {
            --m_next_ptr;
            auto buffer = m_buffers.take_first();

            if (buffer->shbuf_id() == id) {
                m_current_buffer = buffer;
                break;
            }
        }
    }
}

void PlaybackManager::load_next_buffer()
{
    if (m_buffers.size() < 10) {
        for (int i = 0; i < 20 && m_loader->loaded_samples() < m_loader->total_samples(); i++) {
            auto buffer = m_loader->get_more_samples(PLAYBACK_MANAGER_BUFFER_SIZE);
            if (buffer)
                m_buffers.append(buffer);
        }
    }

    if (m_next_ptr < m_buffers.size()) {
        m_next_buffer = m_buffers.at(m_next_ptr++);
    } else {
        m_next_buffer = nullptr;
    }
}

void PlaybackManager::set_paused(bool paused)
{
    if (!m_next_buffer && m_loader)
        load_next_buffer();

    m_paused = paused;
    m_connection->set_paused(paused);
}

bool PlaybackManager::toggle_pause()
{
    if (m_paused) {
        play();
    } else {
        pause();
    }
    return m_paused;
}

void PlaybackManager::next_buffer()
{
    if (on_update)
        on_update();

    if (m_paused)
        return;

    remove_dead_buffers();
    if (!m_next_buffer) {
        if (!m_connection->get_remaining_samples() && !m_paused) {
            dbgln("Exhausted samples :^)");
            stop();
        }

        return;
    }

    bool enqueued = m_connection->try_enqueue(*m_next_buffer);
    if (!enqueued)
        return;

    load_next_buffer();
}
