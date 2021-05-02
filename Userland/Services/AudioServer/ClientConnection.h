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

class BufferQueue;
class Mixer;

class ClientConnection final : public IPC::ClientConnection<AudioClientEndpoint, AudioServerEndpoint>
    , public AudioServerEndpoint {
    C_OBJECT(ClientConnection)
public:
    explicit ClientConnection(NonnullRefPtr<Core::LocalSocket>, int client_id, Mixer& mixer);
    ~ClientConnection() override;

    void did_finish_playing_buffer(Badge<BufferQueue>, int buffer_id);
    void did_change_muted_state(Badge<Mixer>, bool muted);
    void did_change_main_mix_volume(Badge<Mixer>, int volume);

    virtual void die() override;

    static void for_each(Function<void(ClientConnection&)>);

private:
    virtual Messages::AudioServer::GreetResponse handle(const Messages::AudioServer::Greet&) override;
    virtual Messages::AudioServer::GetMainMixVolumeResponse handle(const Messages::AudioServer::GetMainMixVolume&) override;
    virtual Messages::AudioServer::SetMainMixVolumeResponse handle(const Messages::AudioServer::SetMainMixVolume&) override;
    virtual Messages::AudioServer::EnqueueBufferResponse handle(const Messages::AudioServer::EnqueueBuffer&) override;
    virtual Messages::AudioServer::GetRemainingSamplesResponse handle(const Messages::AudioServer::GetRemainingSamples&) override;
    virtual Messages::AudioServer::GetPlayedSamplesResponse handle(const Messages::AudioServer::GetPlayedSamples&) override;
    virtual Messages::AudioServer::SetPausedResponse handle(const Messages::AudioServer::SetPaused&) override;
    virtual Messages::AudioServer::ClearBufferResponse handle(const Messages::AudioServer::ClearBuffer&) override;
    virtual Messages::AudioServer::GetPlayingBufferResponse handle(const Messages::AudioServer::GetPlayingBuffer&) override;
    virtual Messages::AudioServer::GetMutedResponse handle(const Messages::AudioServer::GetMuted&) override;
    virtual Messages::AudioServer::SetMutedResponse handle(const Messages::AudioServer::SetMuted&) override;

    Mixer& m_mixer;
    RefPtr<BufferQueue> m_queue;
};

}
