#pragma once

#include "ASMixer.h"
#include <LibCore/CEventLoop.h>
#include <LibCore/CLocalServer.h>
#include <LibCore/CNotifier.h>

class ASEventLoop {
public:
    ASEventLoop();
    int exec() { return m_event_loop.exec(); }

private:
    CEventLoop m_event_loop;
    RefPtr<CLocalServer> m_server;
    ASMixer m_mixer;
};
