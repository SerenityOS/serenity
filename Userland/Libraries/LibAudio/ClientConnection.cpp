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

void ClientConnection::enqueue(Buffer const& buffer)
{
    for (;;) {
        auto success = enqueue_buffer(buffer.anonymous_buffer(), buffer.id(), buffer.sample_count());
        if (success)
            break;
        // FIXME: We don't know what is a good value for this.
        // For now, decrease it to enable better real-time audio.
        usleep(10000);
    }
}

void ClientConnection::async_enqueue(Buffer const& buffer)
{
    async_enqueue_buffer(buffer.anonymous_buffer(), buffer.id(), buffer.sample_count());
}

bool ClientConnection::try_enqueue(Buffer const& buffer)
{
    return enqueue_buffer(buffer.anonymous_buffer(), buffer.id(), buffer.sample_count());
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

void ClientConnection::main_mix_volume_changed(double volume)
{
    if (on_main_mix_volume_change)
        on_main_mix_volume_change(volume);
}

}
