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
    const_cast<ABuffer&>(buffer).shared_buffer().share_with(server_pid());
    ASAPI_ClientMessage request;
    request.type = ASAPI_ClientMessage::Type::PlayBuffer;
    request.play_buffer.buffer_id = buffer.shared_buffer_id();
    sync_request(request, block ? ASAPI_ServerMessage::Type::FinishedPlayingBuffer : ASAPI_ServerMessage::Type::PlayingBuffer);
}

void AClientConnection::enqueue(const ABuffer& buffer)
{
    for (;;) {
        const_cast<ABuffer&>(buffer).shared_buffer().share_with(server_pid());
        ASAPI_ClientMessage request;
        request.type = ASAPI_ClientMessage::Type::EnqueueBuffer;
        request.play_buffer.buffer_id = buffer.shared_buffer_id();
        auto response = sync_request(request, ASAPI_ServerMessage::Type::EnqueueBufferResponse);
        if (response.success)
            break;
        dbg() << "EnqueueBuffer failed, retrying...";
        sleep(1);
    }
}
