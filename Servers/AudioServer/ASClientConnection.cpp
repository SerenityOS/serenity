#include "ASClientConnection.h"
#include "ASMixer.h"
#include "AudioClientEndpoint.h"
#include <LibAudio/ABuffer.h>
#include <LibCore/CEventLoop.h>
#include <SharedBuffer.h>
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

ASClientConnection::ASClientConnection(CLocalSocket& client_socket, int client_id, ASMixer& mixer)
    : ConnectionNG(*this, client_socket, client_id)
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
    post_message(AudioClient::FinishedPlayingBuffer(buffer_id));
}

void ASClientConnection::did_change_muted_state(Badge<ASMixer>, bool muted)
{
    post_message(AudioClient::MutedStateChanged(muted));
}

OwnPtr<AudioServer::GreetResponse> ASClientConnection::handle(const AudioServer::Greet& message)
{
    set_client_pid(message.client_pid());
    return make<AudioServer::GreetResponse>(getpid(), client_id());
}

OwnPtr<AudioServer::GetMainMixVolumeResponse> ASClientConnection::handle(const AudioServer::GetMainMixVolume&)
{
    return make<AudioServer::GetMainMixVolumeResponse>(m_mixer.main_volume());
}

OwnPtr<AudioServer::SetMainMixVolumeResponse> ASClientConnection::handle(const AudioServer::SetMainMixVolume& message)
{
    m_mixer.set_main_volume(message.volume());
    return make<AudioServer::SetMainMixVolumeResponse>();
}

OwnPtr<AudioServer::EnqueueBufferResponse> ASClientConnection::handle(const AudioServer::EnqueueBuffer& message)
{
    auto shared_buffer = SharedBuffer::create_from_shared_buffer_id(message.buffer_id());
    if (!shared_buffer) {
        // FIXME: The shared buffer should have been retrieved for us already.
        //        We don't want to do IPC error checking at this layer.
        ASSERT_NOT_REACHED();
    }

    if (!m_queue)
        m_queue = m_mixer.create_queue(*this);

    if (m_queue->is_full())
        return make<AudioServer::EnqueueBufferResponse>(false);

    m_queue->enqueue(ABuffer::create_with_shared_buffer(*shared_buffer, message.sample_count()));
    return make<AudioServer::EnqueueBufferResponse>(true);
}

OwnPtr<AudioServer::GetRemainingSamplesResponse> ASClientConnection::handle(const AudioServer::GetRemainingSamples&)
{
    int remaining = 0;
    if (m_queue)
        remaining = m_queue->get_remaining_samples();

    return make<AudioServer::GetRemainingSamplesResponse>(remaining);
}

OwnPtr<AudioServer::GetPlayedSamplesResponse> ASClientConnection::handle(const AudioServer::GetPlayedSamples&)
{
    int played = 0;
    if (m_queue)
        played = m_queue->get_played_samples();

    return make<AudioServer::GetPlayedSamplesResponse>(played);
}

OwnPtr<AudioServer::SetPausedResponse> ASClientConnection::handle(const AudioServer::SetPaused& message)
{
    if (m_queue)
        m_queue->set_paused(message.paused());
    return make<AudioServer::SetPausedResponse>();
}

OwnPtr<AudioServer::ClearBufferResponse> ASClientConnection::handle(const AudioServer::ClearBuffer& message)
{
    if (m_queue)
        m_queue->clear(message.paused());
    return make<AudioServer::ClearBufferResponse>();
}

OwnPtr<AudioServer::GetPlayingBufferResponse> ASClientConnection::handle(const AudioServer::GetPlayingBuffer&)
{
    int id = -1;
    if (m_queue)
        id = m_queue->get_playing_buffer();
    return make<AudioServer::GetPlayingBufferResponse>(id);
}

OwnPtr<AudioServer::GetMutedResponse> ASClientConnection::handle(const AudioServer::GetMuted&)
{
    return make<AudioServer::GetMutedResponse>(m_mixer.is_muted());
}

OwnPtr<AudioServer::SetMutedResponse> ASClientConnection::handle(const AudioServer::SetMuted& message)
{
    m_mixer.set_muted(message.muted());
    return make<AudioServer::SetMutedResponse>();
}
