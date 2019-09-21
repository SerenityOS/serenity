#pragma once

#include <LibCore/CNotifier.h>
#include <LibCore/CObject.h>

class CLocalSocket;

class CLocalServer : public CObject {
    C_OBJECT(CLocalServer)
public:
    virtual ~CLocalServer() override;

    bool is_listening() const { return m_listening; }
    bool listen(const String& address);

    ObjectPtr<CLocalSocket> accept();

    Function<void()> on_ready_to_accept;

private:
    explicit CLocalServer(CObject* parent = nullptr);

    int m_fd { -1 };
    bool m_listening { false };
    ObjectPtr<CNotifier> m_notifier;
};
