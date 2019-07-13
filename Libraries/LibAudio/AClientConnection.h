#pragma once

#include <LibCore/CLocalSocket.h>
#include <LibCore/CNotifier.h>

class AClientConnection {
public:
    AClientConnection();

private:
    CLocalSocket m_connection;
    OwnPtr<CNotifier> m_notifier;
};
