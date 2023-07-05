/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ConnectionFromClient.h"
#include "Mixer.h"
#include <AudioServer/AudioClientEndpoint.h>
#include <LibAudio/Queue.h>

namespace AudioServer {

static HashMap<int, RefPtr<ConnectionFromClient>> s_connections;

void ConnectionFromClient::for_each(Function<void(ConnectionFromClient&)> callback)
{
    Vector<NonnullRefPtr<ConnectionFromClient>> connections;
    for (auto& it : s_connections)
        connections.append(*it.value);
    for (auto& connection : connections)
        callback(connection);
}

ConnectionFromClient::ConnectionFromClient(NonnullOwnPtr<Core::LocalSocket> client_socket, int client_id, Mixer& mixer)
    : IPC::ConnectionFromClient<AudioClientEndpoint, AudioServerEndpoint>(*this, move(client_socket), client_id)
    , m_mixer(mixer)
{
    s_connections.set(client_id, *this);
}

void ConnectionFromClient::die()
{
    s_connections.remove(client_id());
}

void ConnectionFromClient::set_buffer(Audio::AudioQueue const& buffer)
{
    if (!buffer.is_valid()) {
        did_misbehave("Received an invalid buffer");
        return;
    }
    if (!m_queue) {
        m_queue = m_mixer.create_queue(*this);
        if (m_saved_sample_rate.has_value())
            m_queue->set_sample_rate(m_saved_sample_rate.release_value());
    }

    // This is ugly but we know nobody uses the buffer afterwards anyways.
    m_queue->set_buffer(make<Audio::AudioQueue>(move(const_cast<Audio::AudioQueue&>(buffer))));
}

void ConnectionFromClient::did_change_client_volume(Badge<ClientAudioStream>, double volume)
{
    async_client_volume_changed(volume);
}

Messages::AudioServer::GetSelfSampleRateResponse ConnectionFromClient::get_self_sample_rate()
{
    if (m_queue)
        return { m_queue->sample_rate() };
    // Fall back to device sample rate since that would mean no resampling.
    return m_mixer.audiodevice_get_sample_rate();
}
void ConnectionFromClient::set_self_sample_rate(u32 sample_rate)
{
    if (m_queue)
        m_queue->set_sample_rate(sample_rate);
    else
        m_saved_sample_rate = sample_rate;
}

Messages::AudioServer::GetSelfVolumeResponse ConnectionFromClient::get_self_volume()
{
    return m_queue->volume().target();
}

void ConnectionFromClient::set_self_volume(double volume)
{
    if (m_queue)
        m_queue->set_volume(volume);
}

void ConnectionFromClient::start_playback()
{
    if (m_queue)
        m_queue->set_paused(false);
}

void ConnectionFromClient::pause_playback()
{
    if (m_queue)
        m_queue->set_paused(true);
}

void ConnectionFromClient::clear_buffer()
{
    if (m_queue)
        m_queue->clear();
}

Messages::AudioServer::IsSelfMutedResponse ConnectionFromClient::is_self_muted()
{
    if (m_queue)
        return m_queue->is_muted();

    return false;
}

void ConnectionFromClient::set_self_muted(bool muted)
{
    if (m_queue)
        m_queue->set_muted(muted);
}
}
