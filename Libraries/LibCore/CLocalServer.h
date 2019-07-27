#pragma once

#include <LibCore/CNotifier.h>
#include <LibCore/CObject.h>

class CLocalSocket;

class CLocalServer : public CObject {
    C_OBJECT(CLocalServer)
public:
    explicit CLocalServer(CObject* parent = nullptr);
    virtual ~CLocalServer() override;

    bool is_listening() const { return m_listening; }
    bool listen(const String& address);

    CLocalSocket* accept();

    Function<void()> on_ready_to_accept;

private:
    int m_fd { -1 };
    bool m_listening { false };
    OwnPtr<CNotifier> m_notifier;
};
