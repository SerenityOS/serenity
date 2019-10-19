#include <LibAudio/ABuffer.h>
#include <LibAudio/AClientConnection.h>
#include <SharedBuffer.h>

AClientConnection::AClientConnection()
    : ConnectionNG("/tmp/asportal")
{
}

void AClientConnection::handshake()
{
    auto response = send_sync<AudioServer::Greet>(getpid());
    set_server_pid(response->server_pid());
    set_my_client_id(response->client_id());
}

void AClientConnection::enqueue(const ABuffer& buffer)
{
    for (;;) {
        const_cast<ABuffer&>(buffer).shared_buffer().share_with(server_pid());
        auto response = send_sync<AudioServer::EnqueueBuffer>(buffer.shared_buffer_id(), buffer.sample_count());
        if (response->success())
            break;
        sleep(1);
    }
}

bool AClientConnection::try_enqueue(const ABuffer& buffer)
{
    const_cast<ABuffer&>(buffer).shared_buffer().share_with(server_pid());
    auto response = send_sync<AudioServer::EnqueueBuffer>(buffer.shared_buffer_id(), buffer.sample_count());
    return response->success();
}

int AClientConnection::get_main_mix_volume()
{
    return send_sync<AudioServer::GetMainMixVolume>()->volume();
}

void AClientConnection::set_main_mix_volume(int volume)
{
    send_sync<AudioServer::SetMainMixVolume>(volume);
}

int AClientConnection::get_remaining_samples()
{
    return send_sync<AudioServer::GetRemainingSamples>()->remaining_samples();
}
