#pragma once

#include <AudioServer/AudioServerEndpoint.h>
#include <LibIPC/IClientConnection.h>

class ABuffer;
class ASBufferQueue;
class ASMixer;

class ASClientConnection final : public IPC::Server::ConnectionNG<AudioServerEndpoint>
    , public AudioServerEndpoint {
    C_OBJECT(ASClientConnection)
public:
    explicit ASClientConnection(CLocalSocket&, int client_id, ASMixer& mixer);
    ~ASClientConnection() override;

    void did_finish_playing_buffer(Badge<ASBufferQueue>, int buffer_id);
    void did_change_muted_state(Badge<ASMixer>, bool muted);

    virtual void die() override;

    static void for_each(Function<void(ASClientConnection&)>);

private:
    virtual OwnPtr<AudioServer::GreetResponse> handle(const AudioServer::Greet&) override;
    virtual OwnPtr<AudioServer::GetMainMixVolumeResponse> handle(const AudioServer::GetMainMixVolume&) override;
    virtual OwnPtr<AudioServer::SetMainMixVolumeResponse> handle(const AudioServer::SetMainMixVolume&) override;
    virtual OwnPtr<AudioServer::EnqueueBufferResponse> handle(const AudioServer::EnqueueBuffer&) override;
    virtual OwnPtr<AudioServer::GetRemainingSamplesResponse> handle(const AudioServer::GetRemainingSamples&) override;
    virtual OwnPtr<AudioServer::GetPlayedSamplesResponse> handle(const AudioServer::GetPlayedSamples&) override;
    virtual OwnPtr<AudioServer::SetPausedResponse> handle(const AudioServer::SetPaused&) override;
    virtual OwnPtr<AudioServer::ClearBufferResponse> handle(const AudioServer::ClearBuffer&) override;
    virtual OwnPtr<AudioServer::GetPlayingBufferResponse> handle(const AudioServer::GetPlayingBuffer&) override;
    virtual OwnPtr<AudioServer::GetMutedResponse> handle(const AudioServer::GetMuted&) override;
    virtual OwnPtr<AudioServer::SetMutedResponse> handle(const AudioServer::SetMuted&) override;

    ASMixer& m_mixer;
    RefPtr<ASBufferQueue> m_queue;
};
