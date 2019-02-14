#pragma once

#include <Kernel/Socket.h>
#include <Kernel/DoubleBuffer.h>

class FileDescriptor;

class LocalSocket final : public Socket {
public:
    static RetainPtr<LocalSocket> create(int type);
    virtual ~LocalSocket() override;

    virtual bool bind(const sockaddr*, socklen_t, int& error) override;
    virtual bool get_address(sockaddr*, socklen_t*) override;

private:
    explicit LocalSocket(int type);

    RetainPtr<FileDescriptor> m_file;

    bool m_bound { false };
    sockaddr_un m_address;

    DoubleBuffer m_for_client;
    DoubleBuffer m_for_server;
};

