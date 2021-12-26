/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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
        next_buffer();
    });
    m_timer->stop();
    m_device_sample_rate = connection->get_sample_rate();
}

PlaybackManager::~PlaybackManager()
{
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
        m_loader->reset();
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
    m_loader->seek(position);

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

    if (audio_server_remaining_samples < m_device_samples_per_buffer) {
        m_current_buffer = m_loader->get_more_samples(m_source_buffer_size_bytes);
        VERIFY(m_resampler.has_value());
        m_resampler->reset();
        m_current_buffer = Audio::resample_buffer(m_resampler.value(), *m_current_buffer);
        if (m_current_buffer)
            m_connection->enqueue(*m_current_buffer);
    }
}
