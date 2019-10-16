#include "ASClientConnection.h"
#include "ASMixer.h"

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

void ASClientConnection::did_finish_playing_buffer(Badge<ASMixer>, int buffer_id)
{
    (void)buffer_id;
    //post_message(AudioClient::FinishedPlayingBuffer(buffer_id));
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
