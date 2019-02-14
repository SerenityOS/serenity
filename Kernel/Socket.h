#pragma once

#include <AK/Retainable.h>
#include <AK/RetainPtr.h>

class Socket : public Retainable<Socket> {
public:
    static RetainPtr<Socket> create(int domain, int type, int protocol, int& error);
    virtual ~Socket();

    int domain() const { return m_domain; }
    int type() const { return m_type; }
    int protocol() const { return m_protocol; }

protected:
    Socket(int domain, int type, int protocol);

private:
    int m_domain { 0 };
    int m_type { 0 };
    int m_protocol { 0 };
};

