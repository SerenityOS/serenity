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
        sleep(1);
    }
}

int AClientConnection::get_main_mix_volume()
{
    ASAPI_ClientMessage request;
    request.type = ASAPI_ClientMessage::Type::GetMainMixVolume;
    auto response = sync_request(request, ASAPI_ServerMessage::Type::DidGetMainMixVolume);
    return response.value;
}

void AClientConnection::set_main_mix_volume(int volume)
{
    ASAPI_ClientMessage request;
    request.type = ASAPI_ClientMessage::Type::SetMainMixVolume;
    request.value = volume;
    sync_request(request, ASAPI_ServerMessage::Type::DidSetMainMixVolume);
}
