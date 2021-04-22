/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibAudio/Buffer.h>
#include <LibAudio/ClientConnection.h>

namespace Audio {

ClientConnection::ClientConnection()
    : IPC::ServerConnection<AudioClientEndpoint, AudioServerEndpoint>(*this, "/tmp/portal/audio")
{
}

void ClientConnection::handshake()
{
    send_sync<Messages::AudioServer::Greet>();
}

void ClientConnection::enqueue(const Buffer& buffer)
{
    for (;;) {
        auto response = send_sync<Messages::AudioServer::EnqueueBuffer>(buffer.anonymous_buffer(), buffer.id(), buffer.sample_count());
        if (response->success())
            break;
        sleep(1);
    }
}

bool ClientConnection::try_enqueue(const Buffer& buffer)
{
    auto response = send_sync<Messages::AudioServer::EnqueueBuffer>(buffer.anonymous_buffer(), buffer.id(), buffer.sample_count());
    return response->success();
}

bool ClientConnection::get_muted()
{
    return send_sync<Messages::AudioServer::GetMuted>()->muted();
}

void ClientConnection::set_muted(bool muted)
{
    send_sync<Messages::AudioServer::SetMuted>(muted);
}

int ClientConnection::get_main_mix_volume()
{
    return send_sync<Messages::AudioServer::GetMainMixVolume>()->volume();
}

void ClientConnection::set_main_mix_volume(int volume)
{
    send_sync<Messages::AudioServer::SetMainMixVolume>(volume);
}

int ClientConnection::get_remaining_samples()
{
    return send_sync<Messages::AudioServer::GetRemainingSamples>()->remaining_samples();
}

int ClientConnection::get_played_samples()
{
    return send_sync<Messages::AudioServer::GetPlayedSamples>()->played_samples();
}

void ClientConnection::set_paused(bool paused)
{
    send_sync<Messages::AudioServer::SetPaused>(paused);
}

void ClientConnection::clear_buffer(bool paused)
{
    send_sync<Messages::AudioServer::ClearBuffer>(paused);
}

int ClientConnection::get_playing_buffer()
{
    return send_sync<Messages::AudioServer::GetPlayingBuffer>()->buffer_id();
}

void ClientConnection::handle(const Messages::AudioClient::FinishedPlayingBuffer& message)
{
    if (on_finish_playing_buffer)
        on_finish_playing_buffer(message.buffer_id());
}

void ClientConnection::handle(const Messages::AudioClient::MutedStateChanged& message)
{
    if (on_muted_state_change)
        on_muted_state_change(message.muted());
}

void ClientConnection::handle(const Messages::AudioClient::MainMixVolumeChanged& message)
{
    if (on_main_mix_volume_change)
        on_main_mix_volume_change(message.volume());
}

}
