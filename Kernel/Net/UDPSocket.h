/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/KResult.h>
#include <Kernel/Net/IPv4Socket.h>

namespace Kernel {

class UDPSocket final : public IPv4Socket {
public:
    static KResultOr<NonnullRefPtr<UDPSocket>> create(int protocol);
    virtual ~UDPSocket() override;

    static SocketHandle<UDPSocket> from_port(u16);
    static void for_each(Function<void(const UDPSocket&)>);

private:
    explicit UDPSocket(int protocol);
    virtual const char* class_name() const override { return "UDPSocket"; }
    static Lockable<HashMap<u16, UDPSocket*>>& sockets_by_port();

    virtual KResultOr<size_t> protocol_receive(ReadonlyBytes raw_ipv4_packet, UserOrKernelBuffer& buffer, size_t buffer_size, int flags) override;
    virtual KResultOr<size_t> protocol_send(const UserOrKernelBuffer&, size_t) override;
    virtual KResult protocol_connect(FileDescription&, ShouldBlock) override;
    virtual KResultOr<u16> protocol_allocate_local_port() override;
    virtual KResult protocol_bind() override;
};

}
