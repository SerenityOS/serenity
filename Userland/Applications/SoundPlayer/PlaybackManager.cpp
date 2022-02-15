/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PlaybackManager.h"

PlaybackManager::PlaybackManager(NonnullRefPtr<Audio::ClientConnection> connection)
    : m_connection(connection)
{
    m_timer = Core::Timer::construct(PlaybackManager::update_rate_ms, [&]() {
        if (!m_loader)
            return;
        // Make sure that we have some buffers queued up at all times: an audio dropout is the last thing we want.
        if (m_enqueued_buffers.size() < always_enqueued_buffer_count)
            next_buffer();
    });
    m_connection->on_finish_playing_buffer = [this](auto finished_buffer) {
        auto last_buffer_in_queue = m_enqueued_buffers.dequeue();
        // A fail here would mean that the server skipped one of our buffers, which is BAD.
        VERIFY(last_buffer_in_queue == finished_buffer);
        next_buffer();
    };
    m_timer->stop();
    m_device_sample_rate = connection->get_sample_rate();
}

void PlaybackManager::set_loader(NonnullRefPtr<Audio::Loader>&& loader)
{
    stop();
    m_loader = loader;
    if (m_loader) {
        m_total_length = m_loader->total_samples() / static_cast<float>(m_loader->sample_rate());
        m_device_samples_per_buffer = PlaybackManager::buffer_size_ms / 1000.0f * m_device_sample_rate;
        u32 source_samples_per_buffer = PlaybackManager::buffer_size_ms / 1000.0f * m_loader->sample_rate();
        m_source_buffer_size_bytes = source_samples_per_buffer * m_loader->num_channels() * m_loader->bits_per_sample() / 8;
        m_resampler = Audio::ResampleHelper<double>(m_loader->sample_rate(), m_device_sample_rate);
        m_timer->start();
    } else {
        m_timer->stop();
    }
}

void PlaybackManager::stop()
{
    set_paused(true);
    m_connection->clear_buffer(true);
    m_last_seek = 0;
    m_current_buffer = nullptr;

    if (m_loader)
        (void)m_loader->reset();
}

void PlaybackManager::play()
{
    set_paused(false);
}

void PlaybackManager::loop(bool loop)
{
    m_loop = loop;
}

void PlaybackManager::seek(const int position)
{
    if (!m_loader)
        return;

    m_last_seek = position;
    bool paused_state = m_paused;
    set_paused(true);

    m_connection->clear_buffer(true);
    m_current_buffer = nullptr;

    [[maybe_unused]] auto result = m_loader->seek(position);

    if (!paused_state)
        set_paused(false);
}

void PlaybackManager::pause()
{
    set_paused(true);
}

void PlaybackManager::set_paused(bool paused)
{
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

    u32 audio_server_remaining_samples = m_connection->get_remaining_samples();
    bool all_samples_loaded = (m_loader->loaded_samples() >= m_loader->total_samples());
    bool audio_server_done = (audio_server_remaining_samples == 0);

    if (all_samples_loaded && audio_server_done) {
        stop();
        if (on_finished_playing)
            on_finished_playing();
        return;
    }

    if (audio_server_remaining_samples < m_device_samples_per_buffer * always_enqueued_buffer_count) {
        auto maybe_buffer = m_loader->get_more_samples(m_source_buffer_size_bytes);
        if (!maybe_buffer.is_error()) {
            m_current_buffer = maybe_buffer.release_value();
            VERIFY(m_resampler.has_value());
            m_resampler->reset();
            // FIXME: Handle OOM better.
            m_current_buffer = MUST(Audio::resample_buffer(m_resampler.value(), *m_current_buffer));
            m_connection->enqueue(*m_current_buffer);
            m_enqueued_buffers.enqueue(m_current_buffer->id());
        }
    }
}
