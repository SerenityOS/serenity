/*
 * Copyright (c) 2022, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <LibCore/Proxy.h>
#include <LibCore/Socket.h>

namespace Core {
class SOCKSProxyClient final : public Socket {
public:
    enum class Version : u8 {
        V4 = 0x04,
        V5 = 0x05,
    };

    struct UsernamePasswordAuthenticationData {
        ByteString username;
        ByteString password;
    };

    enum class Command : u8 {
        Connect = 0x01,
        Bind = 0x02,
        UDPAssociate = 0x03,
    };

    using HostOrIPV4 = Variant<ByteString, u32>;

    static ErrorOr<NonnullOwnPtr<SOCKSProxyClient>> connect(Socket& underlying, Version, HostOrIPV4 const& target, int target_port, Variant<UsernamePasswordAuthenticationData, Empty> const& auth_data = {}, Command = Command::Connect);
    static ErrorOr<NonnullOwnPtr<SOCKSProxyClient>> connect(HostOrIPV4 const& server, int server_port, Version, HostOrIPV4 const& target, int target_port, Variant<UsernamePasswordAuthenticationData, Empty> const& auth_data = {}, Command = Command::Connect);

    static Coroutine<ErrorOr<NonnullOwnPtr<SOCKSProxyClient>>> async_connect(Socket& underlying, Version, HostOrIPV4 const& target, int target_port, Variant<UsernamePasswordAuthenticationData, Empty> const& auth_data = {}, Command = Command::Connect);
    static Coroutine<ErrorOr<NonnullOwnPtr<SOCKSProxyClient>>> async_connect(HostOrIPV4 const& server, int server_port, Version, HostOrIPV4 const& target, int target_port, Variant<UsernamePasswordAuthenticationData, Empty> const& auth_data = {}, Command = Command::Connect);

    virtual ~SOCKSProxyClient() override;

    // ^Stream::Stream
    virtual ErrorOr<Bytes> read_some(Bytes bytes) override { return m_socket.read_some(bytes); }
    virtual ErrorOr<size_t> write_some(ReadonlyBytes bytes) override { return m_socket.write_some(bytes); }
    virtual bool is_eof() const override { return m_socket.is_eof(); }
    virtual bool is_open() const override { return m_socket.is_open(); }
    virtual void close() override { m_socket.close(); }

    // ^Stream::Socket
    virtual ErrorOr<size_t> pending_bytes() const override { return m_socket.pending_bytes(); }
    virtual ErrorOr<bool> can_read_without_blocking(int timeout = 0) const override { return m_socket.can_read_without_blocking(timeout); }
    virtual ErrorOr<void> set_blocking(bool enabled) override { return m_socket.set_blocking(enabled); }
    virtual ErrorOr<void> set_close_on_exec(bool enabled) override { return m_socket.set_close_on_exec(enabled); }
    virtual void set_notifications_enabled(bool enabled) override { m_socket.set_notifications_enabled(enabled); }

private:
    SOCKSProxyClient(Socket& socket, OwnPtr<Socket> own_socket)
        : m_socket(socket)
        , m_own_underlying_socket(move(own_socket))
    {
        m_socket.on_ready_to_read = [this] { on_ready_to_read(); };
    }

    Socket& m_socket;
    OwnPtr<Socket> m_own_underlying_socket;
};
}
