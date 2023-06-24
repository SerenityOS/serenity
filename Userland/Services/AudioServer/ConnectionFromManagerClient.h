/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtr.h>
#include <AudioServer/AudioManagerClientEndpoint.h>
#include <AudioServer/AudioManagerServerEndpoint.h>
#include <AudioServer/Mixer.h>
#include <LibIPC/ConnectionFromClient.h>

namespace AudioServer {

class ConnectionFromManagerClient final : public IPC::ConnectionFromClient<AudioManagerClientEndpoint, AudioManagerServerEndpoint> {
    C_OBJECT(ConnectionFromManagerClient)

public:
    ~ConnectionFromManagerClient() override = default;

    virtual void die() override;

    static void for_each(Function<void(ConnectionFromManagerClient&)>);

    void did_change_main_mix_muted_state(Badge<Mixer>, bool muted);
    void did_change_main_mix_volume(Badge<Mixer>, double volume);

private:
    ConnectionFromManagerClient(NonnullOwnPtr<Core::LocalSocket> client_socket, int client_id, Mixer& mixer);

    virtual Messages::AudioManagerServer::GetMainMixVolumeResponse get_main_mix_volume() override;
    virtual void set_main_mix_volume(double) override;
    virtual Messages::AudioManagerServer::IsMainMixMutedResponse is_main_mix_muted() override;
    virtual void set_main_mix_muted(bool) override;
    virtual void set_device_sample_rate(u32 sample_rate) override;
    virtual Messages::AudioManagerServer::GetDeviceSampleRateResponse get_device_sample_rate() override;

    Mixer& m_mixer;
};

}
