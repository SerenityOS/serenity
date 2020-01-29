/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <Kernel/Net/IPv4Socket.h>

class UDPSocket final : public IPv4Socket {
public:
    static NonnullRefPtr<UDPSocket> create(int protocol);
    virtual ~UDPSocket() override;

    static SocketHandle<UDPSocket> from_port(u16);
    static void for_each(Function<void(UDPSocket&)>);

private:
    explicit UDPSocket(int protocol);
    virtual const char* class_name() const override { return "UDPSocket"; }
    static Lockable<HashMap<u16, UDPSocket*>>& sockets_by_port();

    virtual int protocol_receive(const KBuffer&, void* buffer, size_t buffer_size, int flags) override;
    virtual int protocol_send(const void*, size_t) override;
    virtual KResult protocol_connect(FileDescription&, ShouldBlock) override;
    virtual int protocol_allocate_local_port() override;
    virtual KResult protocol_bind() override;
};
