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

#include <AK/Function.h>
#include <LibCore/IODevice.h>
#include <LibCore/SocketAddress.h>

namespace Core {

class Socket : public IODevice {
    C_OBJECT(Socket)
public:
    enum class Type {
        Invalid,
        TCP,
        UDP,
        Local,
    };
    virtual ~Socket() override;

    Type type() const { return m_type; }

    bool connect(const String& hostname, int port);
    bool connect(const SocketAddress&, int port);
    bool connect(const SocketAddress&);

    ByteBuffer receive(int max_size);
    bool send(const ByteBuffer&);

    bool is_connected() const { return m_connected; }
    void set_blocking(bool blocking);

    SocketAddress source_address() const { return m_source_address; }
    int source_port() const { return m_source_port; }

    SocketAddress destination_address() const { return m_destination_address; }
    int destination_port() const { return m_destination_port; }

    Function<void()> on_connected;
    Function<void()> on_ready_to_read;

protected:
    Socket(Type, Object* parent);

    SocketAddress m_source_address;
    SocketAddress m_destination_address;
    int m_source_port { -1 };
    int m_destination_port { -1 };
    bool m_connected { false };

    virtual void did_update_fd(int) override;

private:
    virtual bool open(IODevice::OpenMode) override { ASSERT_NOT_REACHED(); }
    bool common_connect(const struct sockaddr*, socklen_t);
    void ensure_read_notifier();

    Type m_type { Type::Invalid };
    RefPtr<Notifier> m_notifier;
    RefPtr<Notifier> m_read_notifier;
};

}
