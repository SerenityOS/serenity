#include <SharedBuffer.h>
#include <LibAudio/ABuffer.h>
#include "AClientConnection.h"

AClientConnection::AClientConnection()
    : CIPCClientSideConnection("/tmp/asportal")
{
}

void AClientConnection::handshake()
{
    ASAPI_ClientMessage request;
    request.type = ASAPI_ClientMessage::Type::Greeting;
    request.greeting.client_pid = getpid();
    auto response = sync_request(request, ASAPI_ServerMessage::Type::Greeting);
    set_server_pid(response.greeting.server_pid);
    set_my_client_id(response.greeting.your_client_id);
}

void AClientConnection::play(const ABuffer& buffer)
{
    auto shared_buf = SharedBuffer::create(server_pid(), buffer.size_in_bytes());
    if (!shared_buf) {
        dbg() << "Failed to create a shared buffer!";
        return;
    }

    memcpy(shared_buf->data(), buffer.data(), buffer.size_in_bytes());
    shared_buf->seal();
    ASAPI_ClientMessage request;
    request.type = ASAPI_ClientMessage::Type::PlayBuffer;
    request.play_buffer.buffer_id = shared_buf->shared_buffer_id();
    sync_request(request, ASAPI_ServerMessage::Type::PlayingBuffer);
}
