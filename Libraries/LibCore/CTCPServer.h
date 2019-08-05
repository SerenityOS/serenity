#pragma once

#include <AK/IPv4Address.h>
#include <LibCore/CNotifier.h>
#include <LibCore/CObject.h>

class CTCPSocket;

class CTCPServer : public CObject {
    C_OBJECT(CTCPServer)
public:
    explicit CTCPServer(CObject* parent = nullptr);
    virtual ~CTCPServer() override;

    bool is_listening() const { return m_listening; }
    bool listen(const IPv4Address& address, u16 port);

    CTCPSocket* accept();

    Function<void()> on_ready_to_accept;

private:
    int m_fd { -1 };
    bool m_listening { false };
    OwnPtr<CNotifier> m_notifier;
};
