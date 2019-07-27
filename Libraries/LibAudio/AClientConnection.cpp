#include <LibAudio/ABuffer.h>
#include <LibAudio/AClientConnection.h>
#include <SharedBuffer.h>

AClientConnection::AClientConnection()
    : Connection("/tmp/asportal")
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

void AClientConnection::play(const ABuffer& buffer, bool block)
{
    auto shared_buf = SharedBuffer::create_with_size(buffer.size_in_bytes());
    if (!shared_buf) {
        dbg() << "Failed to create a shared buffer!";
        return;
    }

    memcpy(shared_buf->data(), buffer.data(), buffer.size_in_bytes());
    shared_buf->seal();
    shared_buf->share_with(server_pid());
    ASAPI_ClientMessage request;
    request.type = ASAPI_ClientMessage::Type::PlayBuffer;
    request.play_buffer.buffer_id = shared_buf->shared_buffer_id();
    sync_request(request, block ? ASAPI_ServerMessage::Type::FinishedPlayingBuffer : ASAPI_ServerMessage::Type::PlayingBuffer);
}
