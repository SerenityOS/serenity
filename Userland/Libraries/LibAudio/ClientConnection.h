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

class ClientConnection : public IPC::ServerConnection<AudioClientEndpoint, AudioServerEndpoint>
    , public AudioClientEndpoint {
    C_OBJECT(ClientConnection)
public:
    ClientConnection();

    virtual void handshake() override;
    void enqueue(const Buffer&);
    bool try_enqueue(const Buffer&);

    bool get_muted();
    void set_muted(bool);

    int get_main_mix_volume();
    void set_main_mix_volume(int);

    int get_remaining_samples();
    int get_played_samples();
    int get_playing_buffer();

    void set_paused(bool paused);
    void clear_buffer(bool paused = false);

    Function<void(i32 buffer_id)> on_finish_playing_buffer;
    Function<void(bool muted)> on_muted_state_change;
    Function<void(int volume)> on_main_mix_volume_change;

private:
    virtual void finished_playing_buffer(i32) override;
    virtual void muted_state_changed(bool) override;
    virtual void main_mix_volume_changed(i32) override;
};

}
