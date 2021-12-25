/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <Kernel/Locking/MutexProtected.h>
#include <Kernel/Net/IPv4Socket.h>

namespace Kernel {

class UDPSocket final : public IPv4Socket {
public:
    static ErrorOr<NonnullRefPtr<UDPSocket>> try_create(int protocol, NonnullOwnPtr<DoubleBuffer> receive_buffer);
    virtual ~UDPSocket() override;

    static RefPtr<UDPSocket> from_port(u16);
    static void for_each(Function<void(const UDPSocket&)>);

private:
    explicit UDPSocket(int protocol, NonnullOwnPtr<DoubleBuffer> receive_buffer);
    virtual StringView class_name() const override { return "UDPSocket"sv; }
    static MutexProtected<HashMap<u16, UDPSocket*>>& sockets_by_port();

    virtual ErrorOr<size_t> protocol_receive(ReadonlyBytes raw_ipv4_packet, UserOrKernelBuffer& buffer, size_t buffer_size, int flags) override;
    virtual ErrorOr<size_t> protocol_send(const UserOrKernelBuffer&, size_t) override;
    virtual ErrorOr<size_t> protocol_size(ReadonlyBytes raw_ipv4_packet) override;
    virtual ErrorOr<void> protocol_connect(OpenFileDescription&, ShouldBlock) override;
    virtual ErrorOr<u16> protocol_allocate_local_port() override;
    virtual ErrorOr<void> protocol_bind() override;
};

}
