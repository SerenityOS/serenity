/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AudioServer/AudioClientEndpoint.h>
#include <AudioServer/AudioServerEndpoint.h>
#include <LibIPC/ClientConnection.h>

namespace Audio {
class Buffer;
}

namespace AudioServer {

class ClientAudioStream;
class Mixer;

class ClientConnection final : public IPC::ClientConnection<AudioClientEndpoint, AudioServerEndpoint> {
    C_OBJECT(ClientConnection)
public:
    ~ClientConnection() override;

    void did_finish_playing_buffer(Badge<ClientAudioStream>, int buffer_id);
    void did_change_client_volume(Badge<ClientAudioStream>, double volume);
    void did_change_muted_state(Badge<Mixer>, bool muted);
    void did_change_main_mix_volume(Badge<Mixer>, double volume);

    virtual void die() override;

    static void for_each(Function<void(ClientConnection&)>);

private:
    explicit ClientConnection(NonnullRefPtr<Core::LocalSocket>, int client_id, Mixer& mixer);

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
    virtual Messages::AudioServer::GetMutedResponse get_muted() override;
    virtual void set_muted(bool) override;
    virtual void set_sample_rate(u16 sample_rate) override;
    virtual Messages::AudioServer::GetSampleRateResponse get_sample_rate() override;

    Mixer& m_mixer;
    RefPtr<ClientAudioStream> m_queue;
};

}
