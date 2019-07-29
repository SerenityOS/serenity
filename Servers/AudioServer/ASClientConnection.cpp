#include "ASClientConnection.h"
#include "ASMixer.h"

#include <LibAudio/ABuffer.h>
#include <LibAudio/ASAPI.h>
#include <LibCore/CEventLoop.h>
#include <SharedBuffer.h>

#include <errno.h>
#include <stdio.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <sys/uio.h>
#include <unistd.h>

ASClientConnection::ASClientConnection(CLocalSocket& client_socket, int client_id, ASMixer& mixer)
    : Connection(client_socket, client_id)
    , m_mixer(mixer)
{
}

ASClientConnection::~ASClientConnection()
{
}

void ASClientConnection::send_greeting()
{
    post_message(ASAPI_Server::Greeting(getpid(), client_id()));
}

bool ASClientConnection::handle_message(const ASAPI_ClientMessage& message, const ByteBuffer&&)
{
    switch (message.type) {
    case ASAPI_ClientMessage::Type::Greeting:
        set_client_pid(message.greeting.client_pid);
        break;
    case ASAPI_ClientMessage::Type::EnqueueBuffer: {
        auto shared_buffer = SharedBuffer::create_from_shared_buffer_id(message.play_buffer.buffer_id);
        if (!shared_buffer) {
            did_misbehave();
            return false;
        }

        ASAPI_ServerMessage reply;
        reply.type = ASAPI_ServerMessage::Type::EnqueueBufferResponse;
        reply.playing_buffer.buffer_id = message.play_buffer.buffer_id;

        if (!m_queue)
            m_queue = m_mixer.create_queue(*this);

        if (m_queue->is_full()) {
            post_message(ASAPI_Server::EnqueueBufferResponse(false, message.play_buffer.buffer_id));
            break;
        }
        m_queue->enqueue(ABuffer::create_with_shared_buffer(*shared_buffer));
        post_message(ASAPI_Server::EnqueueBufferResponse(true, message.play_buffer.buffer_id));
        break;
    }
    case ASAPI_ClientMessage::Type::GetMainMixVolume: {
        post_message(ASAPI_Server::DidGetMainMixVolume(m_mixer.main_volume()));
        break;
    }
    case ASAPI_ClientMessage::Type::SetMainMixVolume: {
        m_mixer.set_main_volume(message.value);
        post_message(ASAPI_Server::DidSetMainMixVolume());
        break;
    }
    case ASAPI_ClientMessage::Type::Invalid:
    default:
        dbgprintf("ASClientConnection: Unexpected message ID %d\n", int(message.type));
        did_misbehave();
    }

    return true;
}

void ASClientConnection::did_finish_playing_buffer(Badge<ASMixer>, int buffer_id)
{
    post_message(ASAPI_Server::FinishedPlayingBuffer(buffer_id));
}
