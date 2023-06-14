/*
 * Copyright (c) 2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibAudio/ConnectionToServer.h>
#include <WebContent/AudioCodecPluginSerenity.h>

namespace WebContent {

ErrorOr<NonnullOwnPtr<AudioCodecPluginSerenity>> AudioCodecPluginSerenity::create()
{
    auto connection = TRY(Audio::ConnectionToServer::try_create());
    return adopt_nonnull_own_or_enomem(new (nothrow) AudioCodecPluginSerenity(move(connection)));
}

AudioCodecPluginSerenity::AudioCodecPluginSerenity(NonnullRefPtr<Audio::ConnectionToServer> connection)
    : m_connection(move(connection))
{
}

AudioCodecPluginSerenity::~AudioCodecPluginSerenity() = default;

size_t AudioCodecPluginSerenity::device_sample_rate()
{
    if (!m_device_sample_rate.has_value())
        m_device_sample_rate = m_connection->get_sample_rate();
    return *m_device_sample_rate;
}

void AudioCodecPluginSerenity::enqueue_samples(FixedArray<Audio::Sample> samples)
{
    m_connection->async_enqueue(move(samples)).release_value_but_fixme_should_propagate_errors();
}

size_t AudioCodecPluginSerenity::remaining_samples() const
{
    return m_connection->remaining_samples();
}

void AudioCodecPluginSerenity::resume_playback()
{
    m_connection->async_start_playback();
}

void AudioCodecPluginSerenity::pause_playback()
{
    m_connection->async_start_playback();
}

void AudioCodecPluginSerenity::playback_ended()
{
    m_connection->async_pause_playback();
    m_connection->clear_client_buffer();
    m_connection->async_clear_buffer();
}

void AudioCodecPluginSerenity::set_volume(double volume)
{
    m_connection->async_set_self_volume(volume);
}

}
