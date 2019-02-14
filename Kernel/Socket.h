#pragma once

#include <AK/Lock.h>
#include <AK/Retainable.h>
#include <AK/RetainPtr.h>
#include <AK/HashTable.h>
#include <AK/Vector.h>
#include <Kernel/UnixTypes.h>

class Socket : public Retainable<Socket> {
public:
    static RetainPtr<Socket> create(int domain, int type, int protocol, int& error);
    virtual ~Socket();

    bool is_listening() const { return m_listening; }
    int domain() const { return m_domain; }
    int type() const { return m_type; }
    int protocol() const { return m_protocol; }

    bool can_accept() const { return m_pending.is_empty(); }
    RetainPtr<Socket> accept();

    bool listen(int backlog, int& error);

    virtual bool bind(const sockaddr*, socklen_t, int& error) = 0;
    virtual RetainPtr<Socket> connect(const sockaddr*, socklen_t, int& error) = 0;
    virtual bool get_address(sockaddr*, socklen_t*) = 0;
    virtual bool is_local() const { return false; }

protected:
    Socket(int domain, int type, int protocol);

private:
    Lock m_lock;
    int m_domain { 0 };
    int m_type { 0 };
    int m_protocol { 0 };
    int m_backlog { 0 };
    bool m_listening { false };

    Vector<RetainPtr<Socket>> m_pending;
    Vector<RetainPtr<Socket>> m_clients;
};
