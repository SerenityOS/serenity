/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AudioServer/AudioClientEndpoint.h>
#include <AudioServer/AudioServerEndpoint.h>
#include <LibIPC/ConnectionFromClient.h>

namespace Audio {
class Buffer;
}

namespace AudioServer {

class ClientAudioStream;
class Mixer;

class ConnectionFromClient final : public IPC::ConnectionFromClient<AudioClientEndpoint, AudioServerEndpoint> {
    C_OBJECT(ConnectionFromClient)
public:
    ~ConnectionFromClient() override;

    void did_finish_playing_buffer(Badge<ClientAudioStream>, int buffer_id);
    void did_change_client_volume(Badge<ClientAudioStream>, double volume);
    void did_change_main_mix_muted_state(Badge<Mixer>, bool muted);
    void did_change_main_mix_volume(Badge<Mixer>, double volume);

    virtual void die() override;

    static void for_each(Function<void(ConnectionFromClient&)>);

private:
    explicit ConnectionFromClient(NonnullOwnPtr<Core::Stream::LocalSocket>, int client_id, Mixer& mixer);

    virtual Messages::AudioServer::GetMainMixVolumeResponse get_main_mix_volume() override;
    virtual void set_main_mix_volume(double) override;
    virtual Messages::AudioServer::GetSelfVolumeResponse get_self_volume() override;
    virtual void set_self_volume(double) override;
    virtual Messages::AudioServer::EnqueueBufferResponse enqueue_buffer(Core::AnonymousBuffer const&, i32, int) override;
    virtual Messages::AudioServer::GetRemainingSamplesResponse get_remaining_samples() override;
    virtual Messages::AudioServer::GetPlayedSamplesResponse get_played_samples() override;
    virtual void set_paused(bool) override;
    virtual void clear_buffer(bool) override;
    virtual Messages::AudioServer::GetPlayingBufferResponse get_playing_buffer() override;
    virtual Messages::AudioServer::IsMainMixMutedResponse is_main_mix_muted() override;
    virtual void set_main_mix_muted(bool) override;
    virtual Messages::AudioServer::IsSelfMutedResponse is_self_muted() override;
    virtual void set_self_muted(bool) override;
    virtual void set_sample_rate(u32 sample_rate) override;
    virtual Messages::AudioServer::GetSampleRateResponse get_sample_rate() override;

    Mixer& m_mixer;
    RefPtr<ClientAudioStream> m_queue;
};

}
