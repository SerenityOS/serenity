#pragma once

#include <LibCore/CLocalSocket.h>
#include <LibCore/CNotifier.h>
#include <LibCore/CIPCClientSideConnection.h>
#include <LibAudio/ASAPI.h>

class ABuffer;

class AClientConnection : public CIPCClientSideConnection<ASAPI_ServerMessage, ASAPI_ClientMessage> {
public:
    AClientConnection();

    void send_greeting() override;
    void play(const ABuffer& buffer);
};
