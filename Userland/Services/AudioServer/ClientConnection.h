/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
    virtual OwnPtr<Messages::AudioServer::GreetResponse> handle(const Messages::AudioServer::Greet&) override;
    virtual OwnPtr<Messages::AudioServer::GetMainMixVolumeResponse> handle(const Messages::AudioServer::GetMainMixVolume&) override;
    virtual OwnPtr<Messages::AudioServer::SetMainMixVolumeResponse> handle(const Messages::AudioServer::SetMainMixVolume&) override;
    virtual OwnPtr<Messages::AudioServer::EnqueueBufferResponse> handle(const Messages::AudioServer::EnqueueBuffer&) override;
    virtual OwnPtr<Messages::AudioServer::GetRemainingSamplesResponse> handle(const Messages::AudioServer::GetRemainingSamples&) override;
    virtual OwnPtr<Messages::AudioServer::GetPlayedSamplesResponse> handle(const Messages::AudioServer::GetPlayedSamples&) override;
    virtual OwnPtr<Messages::AudioServer::SetPausedResponse> handle(const Messages::AudioServer::SetPaused&) override;
    virtual OwnPtr<Messages::AudioServer::ClearBufferResponse> handle(const Messages::AudioServer::ClearBuffer&) override;
    virtual OwnPtr<Messages::AudioServer::GetPlayingBufferResponse> handle(const Messages::AudioServer::GetPlayingBuffer&) override;
    virtual OwnPtr<Messages::AudioServer::GetMutedResponse> handle(const Messages::AudioServer::GetMuted&) override;
    virtual OwnPtr<Messages::AudioServer::SetMutedResponse> handle(const Messages::AudioServer::SetMuted&) override;

    Mixer& m_mixer;
    RefPtr<BufferQueue> m_queue;
};

}
