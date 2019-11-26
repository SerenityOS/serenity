#pragma once

#include <LibCore/CNotifier.h>
#include <LibCore/CObject.h>

class CLocalSocket;

class CLocalServer : public CObject {
    C_OBJECT(CLocalServer)
public:
    virtual ~CLocalServer() override;

    bool take_over_from_system_server();
    bool is_listening() const { return m_listening; }
    bool listen(const String& address);

    RefPtr<CLocalSocket> accept();

    Function<void()> on_ready_to_accept;

private:
    explicit CLocalServer(CObject* parent = nullptr);

    void setup_notifier();

    int m_fd { -1 };
    bool m_listening { false };
    RefPtr<CNotifier> m_notifier;
};
