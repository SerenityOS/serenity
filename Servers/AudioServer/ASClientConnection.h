#pragma once

#include <AudioServer/AudioServerEndpoint.h>
#include <LibCore/CoreIPCServer.h>

class ABuffer;
class ASBufferQueue;
class ASMixer;

class ASClientConnection final : public IPC::Server::ConnectionNG<AudioServerEndpoint>
    , public AudioServerEndpoint {
    C_OBJECT(ASClientConnection)
public:
    explicit ASClientConnection(CLocalSocket&, int client_id, ASMixer& mixer);
    ~ASClientConnection() override;
    void did_finish_playing_buffer(Badge<ASMixer>, int buffer_id);

    virtual void die() override;

private:
    virtual OwnPtr<AudioServer::GreetResponse> handle(const AudioServer::Greet&) override;
    virtual OwnPtr<AudioServer::GetMainMixVolumeResponse> handle(const AudioServer::GetMainMixVolume&) override;
    virtual OwnPtr<AudioServer::SetMainMixVolumeResponse> handle(const AudioServer::SetMainMixVolume&) override;
    virtual OwnPtr<AudioServer::EnqueueBufferResponse> handle(const AudioServer::EnqueueBuffer&) override;

    ASMixer& m_mixer;
    RefPtr<ASBufferQueue> m_queue;
};
