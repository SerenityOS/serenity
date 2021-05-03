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
    greet();
}

void ClientConnection::enqueue(const Buffer& buffer)
{
    for (;;) {
        auto response = enqueue_buffer(buffer.anonymous_buffer(), buffer.id(), buffer.sample_count());
        if (response.success())
            break;
        sleep(1);
    }
}

bool ClientConnection::try_enqueue(const Buffer& buffer)
{
    auto response = enqueue_buffer(buffer.anonymous_buffer(), buffer.id(), buffer.sample_count());
    return response.success();
}

bool ClientConnection::get_muted()
{
    return IPCProxy::get_muted().muted();
}

void ClientConnection::set_muted(bool muted)
{
    IPCProxy::set_muted(muted);
}

int ClientConnection::get_main_mix_volume()
{
    return IPCProxy::get_main_mix_volume().volume();
}

void ClientConnection::set_main_mix_volume(int volume)
{
    IPCProxy::set_main_mix_volume(volume);
}

int ClientConnection::get_remaining_samples()
{
    return IPCProxy::get_remaining_samples().remaining_samples();
}

int ClientConnection::get_played_samples()
{
    return IPCProxy::get_played_samples().played_samples();
}

void ClientConnection::set_paused(bool paused)
{
    IPCProxy::set_paused(paused);
}

void ClientConnection::clear_buffer(bool paused)
{
    IPCProxy::clear_buffer(paused);
}

int ClientConnection::get_playing_buffer()
{
    return IPCProxy::get_playing_buffer().buffer_id();
}

void ClientConnection::finished_playing_buffer(i32 buffer_id)
{
    if (on_finish_playing_buffer)
        on_finish_playing_buffer(buffer_id);
}

void ClientConnection::muted_state_changed(bool muted)
{
    if (on_muted_state_change)
        on_muted_state_change(muted);
}

void ClientConnection::main_mix_volume_changed(i32 volume)
{
    if (on_main_mix_volume_change)
        on_main_mix_volume_change(volume);
}

}
