#pragma once

#include <LibCore/CEventLoop.h>
#include <LibCore/CLocalServer.h>
#include <LibCore/CNotifier.h>
#include "ASMixer.h"

class ASEventLoop
{
public:
    ASEventLoop();
    int exec() { return m_event_loop.exec(); }
private:
    CEventLoop m_event_loop;
    CLocalServer m_server_sock;
    ASMixer m_mixer;
};
