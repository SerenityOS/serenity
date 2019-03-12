#pragma once

#include <Kernel/Socket.h>
#include <Kernel/DoubleBuffer.h>
#include <Kernel/IPv4.h>

class IPv4Socket final : public Socket {
public:
    static Retained<IPv4Socket> create(int type, int protocol);
    virtual ~IPv4Socket() override;

    virtual KResult bind(const sockaddr*, socklen_t) override;
    virtual KResult connect(const sockaddr*, socklen_t) override;
    virtual bool get_address(sockaddr*, socklen_t*) override;
    virtual void attach_fd(SocketRole) override;
    virtual void detach_fd(SocketRole) override;
    virtual bool can_read(SocketRole) const override;
    virtual ssize_t read(SocketRole, byte*, ssize_t) override;
    virtual ssize_t write(SocketRole, const byte*, ssize_t) override;
    virtual bool can_write(SocketRole) const override;
    virtual ssize_t sendto(const void*, size_t, int, const sockaddr*, socklen_t) override;

private:
    IPv4Socket(int type, int protocol);
    virtual bool is_ipv4() const override { return true; }

    bool m_bound { false };
    int m_attached_fds { 0 };
    IPv4Address m_peer_address;

    DoubleBuffer m_for_client;
    DoubleBuffer m_for_server;
};

