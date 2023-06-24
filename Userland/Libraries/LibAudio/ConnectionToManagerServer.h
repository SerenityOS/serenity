/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtr.h>
#include <LibIPC/ConnectionToServer.h>
#include <Userland/Services/AudioServer/AudioManagerClientEndpoint.h>
#include <Userland/Services/AudioServer/AudioManagerServerEndpoint.h>

namespace Audio {

class ConnectionToManagerServer final
    : public IPC::ConnectionToServer<AudioManagerClientEndpoint, AudioManagerServerEndpoint>
    , public AudioManagerClientEndpoint {
    IPC_CLIENT_CONNECTION(ConnectionToManagerServer, "/tmp/session/%sid/portal/audiomanager"sv)
public:
    virtual ~ConnectionToManagerServer() override;
    virtual void die() override;

    virtual void main_mix_volume_changed(double volume) override;
    virtual void main_mix_muted_state_changed(bool muted) override;
    virtual void device_sample_rate_changed(u32 sample_rate) override;

    Function<void(bool muted)> on_main_mix_muted_state_change;
    Function<void(double volume)> on_main_mix_volume_change;
    Function<void(u32 sample_rate)> on_device_sample_rate_change;

private:
    ConnectionToManagerServer(NonnullOwnPtr<Core::LocalSocket>);
};

}
