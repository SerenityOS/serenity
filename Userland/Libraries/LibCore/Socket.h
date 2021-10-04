/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/Span.h>
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

    virtual bool connect(const String& hostname, int port);
    bool connect(const SocketAddress&, int port);
    bool connect(const SocketAddress&);

    ByteBuffer receive(int max_size);
    bool send(ReadonlyBytes);

    bool is_connected() const { return m_connected; }
    void set_blocking(bool blocking);

    SocketAddress source_address() const { return m_source_address; }
    int source_port() const { return m_source_port; }

    SocketAddress destination_address() const { return m_destination_address; }
    int destination_port() const { return m_destination_port; }

    virtual bool close() override;
    virtual void set_idle(bool);

    Function<void()> on_connected;
    Function<void()> on_error;
    Function<void()> on_ready_to_read;

protected:
    Socket(Type, Object* parent);

    SocketAddress m_source_address;
    SocketAddress m_destination_address;
    int m_source_port { -1 };
    int m_destination_port { -1 };
    bool m_connected { false };

    virtual void did_update_fd(int) override;
    virtual bool common_connect(const struct sockaddr*, socklen_t);

private:
    virtual bool open(OpenMode) override { VERIFY_NOT_REACHED(); }
    void ensure_read_notifier();

    Type m_type { Type::Invalid };
    RefPtr<Notifier> m_notifier;
    RefPtr<Notifier> m_read_notifier;
};

}

template<>
struct AK::Formatter<Core::Socket> : Formatter<Core::Object> {
};
