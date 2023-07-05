/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AudioServer/AudioClientEndpoint.h>
#include <AudioServer/AudioServerEndpoint.h>
#include <LibAudio/Queue.h>
#include <LibCore/EventLoop.h>
#include <LibIPC/ConnectionFromClient.h>

namespace AudioServer {

class ClientAudioStream;
class Mixer;

class ConnectionFromClient final : public IPC::ConnectionFromClient<AudioClientEndpoint, AudioServerEndpoint> {
    C_OBJECT(ConnectionFromClient)
public:
    ~ConnectionFromClient() override = default;

    void did_change_client_volume(Badge<ClientAudioStream>, double volume);

    virtual void die() override;

    static void for_each(Function<void(ConnectionFromClient&)>);

private:
    explicit ConnectionFromClient(NonnullOwnPtr<Core::LocalSocket>, int client_id, Mixer& mixer);

    virtual Messages::AudioServer::GetSelfVolumeResponse get_self_volume() override;
    virtual void set_self_volume(double) override;
    virtual void set_buffer(Audio::AudioQueue const&) override;
    virtual void clear_buffer() override;
    virtual void start_playback() override;
    virtual void pause_playback() override;
    virtual Messages::AudioServer::IsSelfMutedResponse is_self_muted() override;
    virtual void set_self_muted(bool) override;
    virtual Messages::AudioServer::GetSelfSampleRateResponse get_self_sample_rate() override;
    virtual void set_self_sample_rate(u32 sample_rate) override;

    Mixer& m_mixer;
    RefPtr<ClientAudioStream> m_queue;
    Optional<u32> m_saved_sample_rate {};
};

}
