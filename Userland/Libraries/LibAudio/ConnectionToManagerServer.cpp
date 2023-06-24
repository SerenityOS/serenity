/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ConnectionToManagerServer.h"

namespace Audio {

ConnectionToManagerServer::ConnectionToManagerServer(NonnullOwnPtr<Core::LocalSocket> socket)
    : IPC::ConnectionToServer<AudioManagerClientEndpoint, AudioManagerServerEndpoint>(*this, move(socket))
{
}

ConnectionToManagerServer::~ConnectionToManagerServer()
{
    die();
}

void ConnectionToManagerServer::die() { }

void ConnectionToManagerServer::main_mix_muted_state_changed(bool muted)
{
    if (on_main_mix_muted_state_change)
        on_main_mix_muted_state_change(muted);
}

void ConnectionToManagerServer::main_mix_volume_changed(double volume)
{
    if (on_main_mix_volume_change)
        on_main_mix_volume_change(volume);
}

void ConnectionToManagerServer::device_sample_rate_changed(u32 sample_rate)
{
    if (on_device_sample_rate_change)
        on_device_sample_rate_change(sample_rate);
}

}
