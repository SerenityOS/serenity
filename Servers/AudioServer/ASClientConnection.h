#pragma once

#include <LibCore/CoreIPCServer.h>
#include <LibAudio/ASAPI.h>

class ASMixer;

class ASClientConnection final : public IPC::Server::Connection<ASAPI_ServerMessage, ASAPI_ClientMessage>
{
public:
    explicit ASClientConnection(int fd, int client_id, ASMixer& mixer);
    ~ASClientConnection() override;
    void send_greeting() override;
    bool handle_message(const ASAPI_ClientMessage&, const ByteBuffer&& = {}) override;
    const char* class_name() const override { return "ASClientConnection"; }

private:
    ASMixer& m_mixer;
};

