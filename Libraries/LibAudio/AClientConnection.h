#pragma once

#include <AudioServer/AudioServerEndpoint.h>
#include <LibCore/CoreIPCClient.h>

class ABuffer;

class AClientConnection : public IPC::Client::ConnectionNG<AudioServerEndpoint> {
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
};
