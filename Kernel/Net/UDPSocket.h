#pragma once

#include <Kernel/Net/IPv4Socket.h>

class UDPSocket final : public IPv4Socket {
public:
    static NonnullRefPtr<UDPSocket> create(int protocol);
    virtual ~UDPSocket() override;

    static SocketHandle<UDPSocket> from_port(u16);

private:
    explicit UDPSocket(int protocol);
    virtual const char* class_name() const override { return "UDPSocket"; }
    static Lockable<HashMap<u16, UDPSocket*>>& sockets_by_port();

    virtual int protocol_receive(const KBuffer&, void* buffer, size_t buffer_size, int flags) override;
    virtual int protocol_send(const void*, int) override;
    virtual KResult protocol_connect(FileDescription&, ShouldBlock) override { return KSuccess; }
    virtual int protocol_allocate_local_port() override;
    virtual KResult protocol_bind() override;
};
