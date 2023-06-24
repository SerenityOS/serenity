/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibAudio/ConnectionToServer.h>
#include <LibAudio/Loader.h>
#include <LibAudio/Sample.h>
#include <LibWeb/Platform/Timer.h>
#include <WebContent/AudioCodecPluginSerenity.h>

namespace WebContent {

// These constants and this implementation is based heavily on SoundPlayer::PlaybackManager.
static constexpr u32 UPDATE_RATE_MS = 50;
static constexpr u32 BUFFER_SIZE_MS = 100;
static constexpr size_t ALWAYS_ENQUEUED_BUFFER_COUNT = 5;

ErrorOr<NonnullOwnPtr<AudioCodecPluginSerenity>> AudioCodecPluginSerenity::create(NonnullRefPtr<Audio::Loader> loader)
{
    auto connection = TRY(Audio::ConnectionToServer::try_create());
    return adopt_nonnull_own_or_enomem(new (nothrow) AudioCodecPluginSerenity(move(connection), move(loader)));
}

AudioCodecPluginSerenity::AudioCodecPluginSerenity(NonnullRefPtr<Audio::ConnectionToServer> connection, NonnullRefPtr<Audio::Loader> loader)
    : m_connection(move(connection))
    , m_loader(move(loader))
    , m_sample_timer(Web::Platform::Timer::create_repeating(UPDATE_RATE_MS, [this]() {
        if (play_next_samples().is_error()) {
            // FIXME: Propagate the error to the HTMLMediaElement.
        } else {
            if (on_playback_position_updated)
                on_playback_position_updated(m_position);
        }
    }))
{
    auto duration = static_cast<double>(m_loader->total_samples()) / static_cast<double>(m_loader->sample_rate());
    m_duration = Duration::from_milliseconds(static_cast<i64>(duration * 1000.0));

    m_samples_to_load_per_buffer = static_cast<size_t>(BUFFER_SIZE_MS / 1000.0 * static_cast<double>(m_loader->sample_rate()));

    m_connection->set_self_sample_rate(m_loader->sample_rate());
}

AudioCodecPluginSerenity::~AudioCodecPluginSerenity() = default;

ErrorOr<void> AudioCodecPluginSerenity::play_next_samples()
{
    while (m_connection->remaining_samples() < m_samples_to_load_per_buffer * ALWAYS_ENQUEUED_BUFFER_COUNT) {
        bool all_samples_loaded = m_loader->loaded_samples() >= m_loader->total_samples();
        bool audio_server_done = m_connection->remaining_samples() == 0;

        if (all_samples_loaded && audio_server_done) {
            pause_playback();

            m_connection->clear_client_buffer();
            m_connection->async_clear_buffer();
            (void)m_loader->reset();

            m_position = m_duration;
            break;
        }

        auto samples = TRY(read_samples_from_loader(m_loader, m_samples_to_load_per_buffer));
        TRY(m_connection->async_enqueue(move(samples)));

        m_position = current_loader_position(m_loader);
    }

    return {};
}

void AudioCodecPluginSerenity::resume_playback()
{
    m_connection->async_start_playback();
    m_sample_timer->start();
}

void AudioCodecPluginSerenity::pause_playback()
{
    m_connection->async_pause_playback();
    m_sample_timer->stop();
}

void AudioCodecPluginSerenity::set_volume(double volume)
{
    m_connection->async_set_self_volume(volume);
}

void AudioCodecPluginSerenity::seek(double position)
{
    m_position = set_loader_position(m_loader, position, m_duration);

    if (on_playback_position_updated)
        on_playback_position_updated(m_position);
}

Duration AudioCodecPluginSerenity::duration()
{
    return m_duration;
}

}
