#pragma once

#include <AK/Queue.h>
#include <LibAudio/ASAPI.h>
#include <LibCore/CoreIPCServer.h>

class ABuffer;
class ASMixer;

class ASClientConnection final : public IPC::Server::Connection<ASAPI_ServerMessage, ASAPI_ClientMessage> {
    C_OBJECT(ASClientConnection)
public:
    explicit ASClientConnection(CLocalSocket&, int client_id, ASMixer& mixer);
    ~ASClientConnection() override;
    void send_greeting() override;
    bool handle_message(const ASAPI_ClientMessage&, const ByteBuffer&& = {}) override;

    void did_finish_playing_buffer(Badge<ASMixer>, int buffer_id);

private:
    void play_next_in_queue();

    ASMixer& m_mixer;
    Queue<NonnullRefPtr<ABuffer>> m_buffer_queue;
    int m_playing_queued_buffer_id { -1 };
};
