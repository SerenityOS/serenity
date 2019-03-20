#pragma once

#include <Kernel/Socket.h>
#include <Kernel/DoubleBuffer.h>

class FileDescriptor;

class LocalSocket final : public Socket {
public:
    static Retained<LocalSocket> create(int type);
    virtual ~LocalSocket() override;

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
    virtual ssize_t recvfrom(void*, size_t, int flags, sockaddr*, socklen_t*) override;

private:
    explicit LocalSocket(int type);
    virtual bool is_local() const override { return true; }

    RetainPtr<FileDescriptor> m_file;
    RetainPtr<LocalSocket> m_peer;

    bool m_bound { false };
    int m_accepted_fds_open { 0 };
    int m_connected_fds_open { 0 };
    int m_connecting_fds_open { 0 };
    sockaddr_un m_address;

    DoubleBuffer m_for_client;
    DoubleBuffer m_for_server;
};

