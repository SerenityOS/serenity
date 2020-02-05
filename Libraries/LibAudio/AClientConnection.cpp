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

#include <AK/SharedBuffer.h>
#include <LibAudio/ABuffer.h>
#include <LibAudio/AClientConnection.h>

AClientConnection::AClientConnection()
    : IPC::ServerConnection<AudioClientEndpoint, AudioServerEndpoint>(*this, "/tmp/portal/audio")
{
}

void AClientConnection::handshake()
{
    auto response = send_sync<AudioServer::Greet>();
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

bool AClientConnection::get_muted()
{
    return send_sync<AudioServer::GetMuted>()->muted();
}

void AClientConnection::set_muted(bool muted)
{
    send_sync<AudioServer::SetMuted>(muted);
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

int AClientConnection::get_played_samples()
{
    return send_sync<AudioServer::GetPlayedSamples>()->played_samples();
}

void AClientConnection::set_paused(bool paused)
{
    send_sync<AudioServer::SetPaused>(paused);
}

void AClientConnection::clear_buffer(bool paused)
{
    send_sync<AudioServer::ClearBuffer>(paused);
}

int AClientConnection::get_playing_buffer()
{
    return send_sync<AudioServer::GetPlayingBuffer>()->buffer_id();
}

void AClientConnection::handle(const AudioClient::FinishedPlayingBuffer& message)
{
    if (on_finish_playing_buffer)
        on_finish_playing_buffer(message.buffer_id());
}

void AClientConnection::handle(const AudioClient::MutedStateChanged& message)
{
    if (on_muted_state_change)
        on_muted_state_change(message.muted());
}
