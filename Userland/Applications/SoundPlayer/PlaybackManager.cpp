/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PlaybackManager.h"

PlaybackManager::PlaybackManager(NonnullRefPtr<Audio::ConnectionFromClient> connection)
    : m_connection(connection)
{
    // FIXME: The buffer enqueuing should happen on a wholly independend second thread.
    m_timer = Core::Timer::construct(PlaybackManager::update_rate_ms, [&]() {
        if (!m_loader)
            return;
        next_buffer();
    });
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
        m_samples_to_load_per_buffer = PlaybackManager::buffer_size_ms / 1000.0f * m_loader->sample_rate();
        m_resampler = Audio::ResampleHelper<Audio::Sample>(m_loader->sample_rate(), m_device_sample_rate);
        m_timer->start();
    } else {
        m_timer->stop();
    }
}

void PlaybackManager::stop()
{
    set_paused(true);
    m_connection->async_clear_buffer();
    m_last_seek = 0;

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

void PlaybackManager::seek(int const position)
{
    if (!m_loader)
        return;

    m_last_seek = position;
    bool paused_state = m_paused;
    set_paused(true);

    [[maybe_unused]] auto result = m_loader->seek(position);

    m_connection->clear_client_buffer();
    m_connection->async_clear_buffer();
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
    if (m_paused)
        m_connection->async_pause_playback();
    else
        m_connection->async_start_playback();
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

    while (m_connection->remaining_samples() < m_device_samples_per_buffer * always_enqueued_buffer_count) {
        bool all_samples_loaded = (m_loader->loaded_samples() >= m_loader->total_samples());
        bool audio_server_done = (m_connection->remaining_samples() == 0);

        if (all_samples_loaded && audio_server_done) {
            stop();
            if (on_finished_playing)
                on_finished_playing();
            return;
        }

        auto maybe_buffer = m_loader->get_more_samples(m_samples_to_load_per_buffer);
        if (!maybe_buffer.is_error()) {
            m_current_buffer.swap(maybe_buffer.value());
            VERIFY(m_resampler.has_value());
            m_resampler->reset();
            // FIXME: Handle OOM better.
            auto resampled = MUST(FixedArray<Audio::Sample>::try_create(m_resampler->resample(move(m_current_buffer)).span()));
            m_current_buffer.swap(resampled);
            MUST(m_connection->async_enqueue(m_current_buffer));
        }
    }
}
