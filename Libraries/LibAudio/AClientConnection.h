#pragma once

#include <LibCore/CLocalSocket.h>
#include <LibCore/CNotifier.h>
#include <LibCore/CoreIPCClient.h>
#include <LibAudio/ASAPI.h>

class ABuffer;

class AClientConnection : public IPC::Client::Connection<ASAPI_ServerMessage, ASAPI_ClientMessage> {
public:
    AClientConnection();

    void handshake() override;
    void play(const ABuffer& buffer);
};
