#pragma once

#include <LibAudio/ASAPI.h>
#include <LibCore/CoreIPCClient.h>

class ABuffer;

class AClientConnection : public IPC::Client::Connection<ASAPI_ServerMessage, ASAPI_ClientMessage> {
    C_OBJECT(AClientConnection)
public:
    AClientConnection();

    virtual void handshake() override;
    void enqueue(const ABuffer&);
};
