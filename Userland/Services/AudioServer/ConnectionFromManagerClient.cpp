/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ConnectionFromManagerClient.h"

namespace AudioServer {

static HashMap<int, RefPtr<ConnectionFromManagerClient>> s_connections;

ConnectionFromManagerClient::ConnectionFromManagerClient(NonnullOwnPtr<Core::LocalSocket> client_socket, int client_id, Mixer& mixer)
    : IPC::ConnectionFromClient<AudioManagerClientEndpoint, AudioManagerServerEndpoint>(*this, move(client_socket), client_id)
    , m_mixer(mixer)
{
    s_connections.set(client_id, *this);
}

void ConnectionFromManagerClient::die()
{
    s_connections.remove(client_id());
}

void ConnectionFromManagerClient::for_each(Function<void(ConnectionFromManagerClient&)> callback)
{
    Vector<NonnullRefPtr<ConnectionFromManagerClient>> connections;
    for (auto& it : s_connections)
        connections.append(*it.value);
    for (auto& connection : connections)
        callback(connection);
}

void ConnectionFromManagerClient::did_change_main_mix_muted_state(Badge<Mixer>, bool muted)
{
    async_main_mix_muted_state_changed(muted);
}

void ConnectionFromManagerClient::did_change_main_mix_volume(Badge<Mixer>, double volume)
{
    async_main_mix_volume_changed(volume);
}

Messages::AudioManagerServer::GetMainMixVolumeResponse ConnectionFromManagerClient::get_main_mix_volume()
{
    return m_mixer.main_volume();
}

void ConnectionFromManagerClient::set_main_mix_volume(double volume)
{
    m_mixer.set_main_volume(volume);
}

Messages::AudioManagerServer::GetDeviceSampleRateResponse ConnectionFromManagerClient::get_device_sample_rate()
{
    return { m_mixer.audiodevice_get_sample_rate() };
}

void ConnectionFromManagerClient::set_device_sample_rate(u32 sample_rate)
{
    m_mixer.audiodevice_set_sample_rate(sample_rate);
}

Messages::AudioManagerServer::IsMainMixMutedResponse ConnectionFromManagerClient::is_main_mix_muted()
{
    return m_mixer.is_muted();
}

void ConnectionFromManagerClient::set_main_mix_muted(bool muted)
{
    m_mixer.set_muted(muted);
}

}
