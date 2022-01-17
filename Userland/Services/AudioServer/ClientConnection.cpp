/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ClientConnection.h"
#include "Mixer.h"
#include <AudioServer/AudioClientEndpoint.h>
#include <LibAudio/Buffer.h>

namespace AudioServer {

static HashMap<int, RefPtr<ClientConnection>> s_connections;

void ClientConnection::for_each(Function<void(ClientConnection&)> callback)
{
    NonnullRefPtrVector<ClientConnection> connections;
    for (auto& it : s_connections)
        connections.append(*it.value);
    for (auto& connection : connections)
        callback(connection);
}

ClientConnection::ClientConnection(NonnullOwnPtr<Core::Stream::LocalSocket> client_socket, int client_id, Mixer& mixer)
    : IPC::ClientConnection<AudioClientEndpoint, AudioServerEndpoint>(*this, move(client_socket), client_id)
    , m_mixer(mixer)
{
    s_connections.set(client_id, *this);
}

ClientConnection::~ClientConnection()
{
}

void ClientConnection::die()
{
    s_connections.remove(client_id());
}

void ClientConnection::did_finish_playing_buffer(Badge<ClientAudioStream>, int buffer_id)
{
    async_finished_playing_buffer(buffer_id);
}

void ClientConnection::did_change_main_mix_muted_state(Badge<Mixer>, bool muted)
{
    async_main_mix_muted_state_changed(muted);
}

void ClientConnection::did_change_main_mix_volume(Badge<Mixer>, double volume)
{
    async_main_mix_volume_changed(volume);
}

void ClientConnection::did_change_client_volume(Badge<ClientAudioStream>, double volume)
{
    async_client_volume_changed(volume);
}

Messages::AudioServer::GetMainMixVolumeResponse ClientConnection::get_main_mix_volume()
{
    return m_mixer.main_volume();
}

void ClientConnection::set_main_mix_volume(double volume)
{
    m_mixer.set_main_volume(volume);
}

Messages::AudioServer::GetSampleRateResponse ClientConnection::get_sample_rate()
{
    return { m_mixer.audiodevice_get_sample_rate() };
}

void ClientConnection::set_sample_rate(u32 sample_rate)
{
    m_mixer.audiodevice_set_sample_rate(sample_rate);
}

Messages::AudioServer::GetSelfVolumeResponse ClientConnection::get_self_volume()
{
    return m_queue->volume().target();
}

void ClientConnection::set_self_volume(double volume)
{
    if (m_queue)
        m_queue->set_volume(volume);
}

Messages::AudioServer::EnqueueBufferResponse ClientConnection::enqueue_buffer(Core::AnonymousBuffer const& buffer, i32 buffer_id, int sample_count)
{
    if (!m_queue)
        m_queue = m_mixer.create_queue(*this);

    if (m_queue->is_full())
        return false;

    // There's not a big allocation to worry about here.
    m_queue->enqueue(MUST(Audio::Buffer::create_with_anonymous_buffer(buffer, buffer_id, sample_count)));
    return true;
}

Messages::AudioServer::GetRemainingSamplesResponse ClientConnection::get_remaining_samples()
{
    int remaining = 0;
    if (m_queue)
        remaining = m_queue->get_remaining_samples();

    return remaining;
}

Messages::AudioServer::GetPlayedSamplesResponse ClientConnection::get_played_samples()
{
    int played = 0;
    if (m_queue)
        played = m_queue->get_played_samples();

    return played;
}

void ClientConnection::set_paused(bool paused)
{
    if (m_queue)
        m_queue->set_paused(paused);
}

void ClientConnection::clear_buffer(bool paused)
{
    if (m_queue)
        m_queue->clear(paused);
}

Messages::AudioServer::GetPlayingBufferResponse ClientConnection::get_playing_buffer()
{
    int id = -1;
    if (m_queue)
        id = m_queue->get_playing_buffer();
    return id;
}

Messages::AudioServer::IsMainMixMutedResponse ClientConnection::is_main_mix_muted()
{
    return m_mixer.is_muted();
}

void ClientConnection::set_main_mix_muted(bool muted)
{
    m_mixer.set_muted(muted);
}

Messages::AudioServer::IsSelfMutedResponse ClientConnection::is_self_muted()
{
    if (m_queue)
        return m_queue->is_muted();

    return false;
}

void ClientConnection::set_self_muted(bool muted)
{
    if (m_queue)
        m_queue->set_muted(muted);
}
}
