/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ClientConnection.h"
#include "Mixer.h"
#include <AudioServer/AudioClientEndpoint.h>
#include <LibAudio/Buffer.h>
#include <errno.h>
#include <stdio.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <sys/uio.h>
#include <unistd.h>

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

ClientConnection::ClientConnection(NonnullRefPtr<Core::LocalSocket> client_socket, int client_id, Mixer& mixer)
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

void ClientConnection::did_finish_playing_buffer(Badge<BufferQueue>, int buffer_id)
{
    post_message(Messages::AudioClient::FinishedPlayingBuffer(buffer_id));
}

void ClientConnection::did_change_muted_state(Badge<Mixer>, bool muted)
{
    post_message(Messages::AudioClient::MutedStateChanged(muted));
}

void ClientConnection::did_change_main_mix_volume(Badge<Mixer>, int volume)
{
    post_message(Messages::AudioClient::MainMixVolumeChanged(volume));
}

void ClientConnection::handle(const Messages::AudioServer::Greet&)
{
}

Messages::AudioServer::GetMainMixVolumeResponse ClientConnection::handle(const Messages::AudioServer::GetMainMixVolume&)
{
    return m_mixer.main_volume();
}

void ClientConnection::handle(const Messages::AudioServer::SetMainMixVolume& message)
{
    m_mixer.set_main_volume(message.volume());
}

Messages::AudioServer::EnqueueBufferResponse ClientConnection::handle(const Messages::AudioServer::EnqueueBuffer& message)
{
    if (!m_queue)
        m_queue = m_mixer.create_queue(*this);

    if (m_queue->is_full())
        return false;

    m_queue->enqueue(Audio::Buffer::create_with_anonymous_buffer(message.buffer(), message.buffer_id(), message.sample_count()));
    return true;
}

Messages::AudioServer::GetRemainingSamplesResponse ClientConnection::handle(const Messages::AudioServer::GetRemainingSamples&)
{
    int remaining = 0;
    if (m_queue)
        remaining = m_queue->get_remaining_samples();

    return remaining;
}

Messages::AudioServer::GetPlayedSamplesResponse ClientConnection::handle(const Messages::AudioServer::GetPlayedSamples&)
{
    int played = 0;
    if (m_queue)
        played = m_queue->get_played_samples();

    return played;
}

void ClientConnection::handle(const Messages::AudioServer::SetPaused& message)
{
    if (m_queue)
        m_queue->set_paused(message.paused());
}

void ClientConnection::handle(const Messages::AudioServer::ClearBuffer& message)
{
    if (m_queue)
        m_queue->clear(message.paused());
}

Messages::AudioServer::GetPlayingBufferResponse ClientConnection::handle(const Messages::AudioServer::GetPlayingBuffer&)
{
    int id = -1;
    if (m_queue)
        id = m_queue->get_playing_buffer();
    return id;
}

Messages::AudioServer::GetMutedResponse ClientConnection::handle(const Messages::AudioServer::GetMuted&)
{
    return m_mixer.is_muted();
}

void ClientConnection::handle(const Messages::AudioServer::SetMuted& message)
{
    m_mixer.set_muted(message.muted());
}
}
