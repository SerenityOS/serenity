#pragma once

#include <Kernel/Net/IPv4Socket.h>

class ICMPSocket final : public IPv4Socket {
public:
    static NonnullRefPtr<ICMPSocket> create();
    virtual ~ICMPSocket() override;

private:
    explicit ICMPSocket();
    virtual const char* class_name() const override { return "ICMPSocket"; }

    virtual int protocol_receive(const KBuffer&, void* buffer, size_t buffer_size, int flags) override;
    virtual int protocol_send(const void*, int) override;
    virtual KResult protocol_connect(FileDescription&, ShouldBlock) override;
    virtual int protocol_allocate_local_port() override;
    virtual KResult protocol_bind() override;
};
