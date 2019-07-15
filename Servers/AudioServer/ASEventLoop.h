#pragma once

#include <LibCore/CEventLoop.h>
#include <LibCore/CLocalSocket.h>
#include <LibCore/CNotifier.h>
#include "ASMixer.h"

class ASEventLoop
{
public:
    ASEventLoop();
    int exec() { return m_event_loop.exec(); }
private:
    CEventLoop m_event_loop;
    CLocalSocket m_server_sock;
    OwnPtr<CNotifier> m_server_notifier;
    ASMixer m_mixer;

    void drain_server();
};
