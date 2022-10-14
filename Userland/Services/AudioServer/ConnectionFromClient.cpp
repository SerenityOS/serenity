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
    NonnullRefPtrVector<ConnectionFromClient> connections;
    for (auto& it : s_connections)
        connections.append(*it.value);
    for (auto& connection : connections)
        callback(connection);
}

ConnectionFromClient::ConnectionFromClient(NonnullOwnPtr<Core::Stream::LocalSocket> client_socket, int client_id, Mixer& mixer)
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
    if (!m_queue)
        m_queue = m_mixer.create_queue(*this);

    // This is ugly but we know nobody uses the buffer afterwards anyways.
    m_queue->set_buffer(make<Audio::AudioQueue>(move(const_cast<Audio::AudioQueue&>(buffer))));
}

void ConnectionFromClient::did_change_main_mix_muted_state(Badge<Mixer>, bool muted)
{
    async_main_mix_muted_state_changed(muted);
}

void ConnectionFromClient::did_change_main_mix_volume(Badge<Mixer>, double volume)
{
    async_main_mix_volume_changed(volume);
}

void ConnectionFromClient::did_change_client_volume(Badge<ClientAudioStream>, double volume)
{
    async_client_volume_changed(volume);
}

Messages::AudioServer::GetMainMixVolumeResponse ConnectionFromClient::get_main_mix_volume()
{
    return m_mixer.main_volume();
}

void ConnectionFromClient::set_main_mix_volume(double volume)
{
    m_mixer.set_main_volume(volume);
}

Messages::AudioServer::GetSampleRateResponse ConnectionFromClient::get_sample_rate()
{
    return { m_mixer.audiodevice_get_sample_rate() };
}

void ConnectionFromClient::set_sample_rate(u32 sample_rate)
{
    m_mixer.audiodevice_set_sample_rate(sample_rate);
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

Messages::AudioServer::IsMainMixMutedResponse ConnectionFromClient::is_main_mix_muted()
{
    return m_mixer.is_muted();
}

void ConnectionFromClient::set_main_mix_muted(bool muted)
{
    m_mixer.set_muted(muted);
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
