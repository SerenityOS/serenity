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

#include "ASClientConnection.h"
#include "ASMixer.h"
#include "AudioClientEndpoint.h"
#include <AK/SharedBuffer.h>
#include <LibAudio/Buffer.h>
#include <LibCore/EventLoop.h>
#include <errno.h>
#include <stdio.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <sys/uio.h>
#include <unistd.h>

static HashMap<int, RefPtr<ASClientConnection>> s_connections;

void ASClientConnection::for_each(Function<void(ASClientConnection&)> callback)
{
    NonnullRefPtrVector<ASClientConnection> connections;
    for (auto& it : s_connections)
        connections.append(*it.value);
    for (auto& connection : connections)
        callback(connection);
}

ASClientConnection::ASClientConnection(Core::LocalSocket& client_socket, int client_id, ASMixer& mixer)
    : IPC::ClientConnection<AudioServerEndpoint>(*this, client_socket, client_id)
    , m_mixer(mixer)
{
    s_connections.set(client_id, *this);
}

ASClientConnection::~ASClientConnection()
{
}

void ASClientConnection::die()
{
    s_connections.remove(client_id());
}

void ASClientConnection::did_finish_playing_buffer(Badge<ASBufferQueue>, int buffer_id)
{
    post_message(Messages::AudioClient::FinishedPlayingBuffer(buffer_id));
}

void ASClientConnection::did_change_muted_state(Badge<ASMixer>, bool muted)
{
    post_message(Messages::AudioClient::MutedStateChanged(muted));
}

OwnPtr<Messages::AudioServer::GreetResponse> ASClientConnection::handle(const Messages::AudioServer::Greet&)
{
    return make<Messages::AudioServer::GreetResponse>(client_id());
}

OwnPtr<Messages::AudioServer::GetMainMixVolumeResponse> ASClientConnection::handle(const Messages::AudioServer::GetMainMixVolume&)
{
    return make<Messages::AudioServer::GetMainMixVolumeResponse>(m_mixer.main_volume());
}

OwnPtr<Messages::AudioServer::SetMainMixVolumeResponse> ASClientConnection::handle(const Messages::AudioServer::SetMainMixVolume& message)
{
    m_mixer.set_main_volume(message.volume());
    return make<Messages::AudioServer::SetMainMixVolumeResponse>();
}

OwnPtr<Messages::AudioServer::EnqueueBufferResponse> ASClientConnection::handle(const Messages::AudioServer::EnqueueBuffer& message)
{
    auto shared_buffer = SharedBuffer::create_from_shbuf_id(message.buffer_id());
    if (!shared_buffer) {
        // FIXME: The shared buffer should have been retrieved for us already.
        //        We don't want to do IPC error checking at this layer.
        ASSERT_NOT_REACHED();
    }

    if (!m_queue)
        m_queue = m_mixer.create_queue(*this);

    if (m_queue->is_full())
        return make<Messages::AudioServer::EnqueueBufferResponse>(false);

    m_queue->enqueue(Audio::Buffer::create_with_shared_buffer(*shared_buffer, message.sample_count()));
    return make<Messages::AudioServer::EnqueueBufferResponse>(true);
}

OwnPtr<Messages::AudioServer::GetRemainingSamplesResponse> ASClientConnection::handle(const Messages::AudioServer::GetRemainingSamples&)
{
    int remaining = 0;
    if (m_queue)
        remaining = m_queue->get_remaining_samples();

    return make<Messages::AudioServer::GetRemainingSamplesResponse>(remaining);
}

OwnPtr<Messages::AudioServer::GetPlayedSamplesResponse> ASClientConnection::handle(const Messages::AudioServer::GetPlayedSamples&)
{
    int played = 0;
    if (m_queue)
        played = m_queue->get_played_samples();

    return make<Messages::AudioServer::GetPlayedSamplesResponse>(played);
}

OwnPtr<Messages::AudioServer::SetPausedResponse> ASClientConnection::handle(const Messages::AudioServer::SetPaused& message)
{
    if (m_queue)
        m_queue->set_paused(message.paused());
    return make<Messages::AudioServer::SetPausedResponse>();
}

OwnPtr<Messages::AudioServer::ClearBufferResponse> ASClientConnection::handle(const Messages::AudioServer::ClearBuffer& message)
{
    if (m_queue)
        m_queue->clear(message.paused());
    return make<Messages::AudioServer::ClearBufferResponse>();
}

OwnPtr<Messages::AudioServer::GetPlayingBufferResponse> ASClientConnection::handle(const Messages::AudioServer::GetPlayingBuffer&)
{
    int id = -1;
    if (m_queue)
        id = m_queue->get_playing_buffer();
    return make<Messages::AudioServer::GetPlayingBufferResponse>(id);
}

OwnPtr<Messages::AudioServer::GetMutedResponse> ASClientConnection::handle(const Messages::AudioServer::GetMuted&)
{
    return make<Messages::AudioServer::GetMutedResponse>(m_mixer.is_muted());
}

OwnPtr<Messages::AudioServer::SetMutedResponse> ASClientConnection::handle(const Messages::AudioServer::SetMuted& message)
{
    m_mixer.set_muted(message.muted());
    return make<Messages::AudioServer::SetMutedResponse>();
}
