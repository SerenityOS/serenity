#pragma once

#include <AudioServer/AudioClientEndpoint.h>
#include <AudioServer/AudioServerEndpoint.h>
#include <LibIPC/IServerConnection.h>

class ABuffer;

class AClientConnection : public IServerConnection<AudioClientEndpoint, AudioServerEndpoint>
    , public AudioClientEndpoint {
    C_OBJECT(AClientConnection)
public:
    AClientConnection();

    virtual void handshake() override;
    void enqueue(const ABuffer&);
    bool try_enqueue(const ABuffer&);

    bool get_muted();
    void set_muted(bool);

    int get_main_mix_volume();
    void set_main_mix_volume(int);

    int get_remaining_samples();
    int get_played_samples();
    int get_playing_buffer();

    void set_paused(bool paused);
    void clear_buffer(bool paused = false);

    Function<void(i32 buffer_id)> on_finish_playing_buffer;
    Function<void(bool muted)> on_muted_state_change;

private:
    virtual void handle(const AudioClient::FinishedPlayingBuffer&) override;
    virtual void handle(const AudioClient::MutedStateChanged&) override;
};
