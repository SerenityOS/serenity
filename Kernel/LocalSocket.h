#pragma once

#include <Kernel/Socket.h>
#include <Kernel/DoubleBuffer.h>

class FileDescriptor;

class LocalSocket final : public Socket {
public:
    static RetainPtr<LocalSocket> create(int type);
    virtual ~LocalSocket() override;

    virtual bool bind(const sockaddr*, socklen_t, int& error) override;
    virtual bool connect(const sockaddr*, socklen_t, int& error) override;
    virtual bool get_address(sockaddr*, socklen_t*) override;

    virtual bool can_read(SocketRole) const override;
    virtual ssize_t read(SocketRole, byte*, size_t) override;
    virtual ssize_t write(SocketRole, const byte*, size_t) override;
    virtual bool can_write(SocketRole) const override;

private:
    explicit LocalSocket(int type);
    virtual bool is_local() const override { return true; }

    RetainPtr<FileDescriptor> m_file;
    RetainPtr<LocalSocket> m_peer;

    bool m_bound { false };
    sockaddr_un m_address;

    DoubleBuffer m_for_client;
    DoubleBuffer m_for_server;
};

