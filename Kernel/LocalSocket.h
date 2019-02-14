#pragma once

#include <Kernel/Socket.h>
#include <Kernel/DoubleBuffer.h>

class LocalSocket final : public Socket {
public:
    static RetainPtr<LocalSocket> create(int type);
    virtual ~LocalSocket() override;

private:
    explicit LocalSocket(int type);

    DoubleBuffer m_for_client;
    DoubleBuffer m_for_server;
};

