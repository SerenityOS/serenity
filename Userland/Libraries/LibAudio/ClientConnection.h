/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AudioServer/AudioClientEndpoint.h>
#include <AudioServer/AudioServerEndpoint.h>
#include <LibIPC/ServerConnection.h>

namespace Audio {

class Buffer;

class ClientConnection final
    : public IPC::ServerConnection<AudioClientEndpoint, AudioServerEndpoint>
    , public AudioClientEndpoint {
    IPC_CLIENT_CONNECTION(ClientConnection, "/tmp/portal/audio")
public:
    void enqueue(Buffer const&);
    bool try_enqueue(Buffer const&);
    void async_enqueue(Buffer const&);

    Function<void(i32 buffer_id)> on_finish_playing_buffer;
    Function<void(bool muted)> on_main_mix_muted_state_change;
    Function<void(double volume)> on_main_mix_volume_change;
    Function<void(double volume)> on_client_volume_change;

private:
    ClientConnection(NonnullOwnPtr<Core::Stream::LocalSocket>);

    virtual void finished_playing_buffer(i32) override;
    virtual void main_mix_muted_state_changed(bool) override;
    virtual void main_mix_volume_changed(double) override;
    virtual void client_volume_changed(double) override;
};

}
