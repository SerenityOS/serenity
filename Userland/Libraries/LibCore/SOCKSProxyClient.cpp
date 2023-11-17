/*
 * Copyright (c) 2022, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MemoryStream.h>
#include <LibCore/SOCKSProxyClient.h>

enum class Method : u8 {
    NoAuth = 0x00,
    GSSAPI = 0x01,
    UsernamePassword = 0x02,
    NoAcceptableMethods = 0xFF,
};

enum class AddressType : u8 {
    IPV4 = 0x01,
    DomainName = 0x03,
    IPV6 = 0x04,
};

enum class Reply {
    Succeeded = 0x00,
    GeneralSocksServerFailure = 0x01,
    ConnectionNotAllowedByRuleset = 0x02,
    NetworkUnreachable = 0x03,
    HostUnreachable = 0x04,
    ConnectionRefused = 0x05,
    TTLExpired = 0x06,
    CommandNotSupported = 0x07,
    AddressTypeNotSupported = 0x08,
};

struct [[gnu::packed]] Socks5VersionIdentifierAndMethodSelectionMessage {
    u8 version_identifier;
    u8 method_count;
    // NOTE: We only send a single method, so we don't need to make this variable-length.
    u8 methods[1];
};

template<>
struct AK::Traits<Socks5VersionIdentifierAndMethodSelectionMessage> : public AK::DefaultTraits<Socks5VersionIdentifierAndMethodSelectionMessage> {
    static constexpr bool is_trivially_serializable() { return true; }
};

struct [[gnu::packed]] Socks5InitialResponse {
    u8 version_identifier;
    u8 method;
};

template<>
struct AK::Traits<Socks5InitialResponse> : public AK::DefaultTraits<Socks5InitialResponse> {
    static constexpr bool is_trivially_serializable() { return true; }
};

struct [[gnu::packed]] Socks5ConnectRequestHeader {
    u8 version_identifier;
    u8 command;
    u8 reserved;
};

template<>
struct AK::Traits<Socks5ConnectRequestHeader> : public AK::DefaultTraits<Socks5ConnectRequestHeader> {
    static constexpr bool is_trivially_serializable() { return true; }
};

struct [[gnu::packed]] Socks5ConnectRequestTrailer {
    u16 port;
};

template<>
struct AK::Traits<Socks5ConnectRequestTrailer> : public AK::DefaultTraits<Socks5ConnectRequestTrailer> {
    static constexpr bool is_trivially_serializable() { return true; }
};

struct [[gnu::packed]] Socks5ConnectResponseHeader {
    u8 version_identifier;
    u8 status;
    u8 reserved;
};

template<>
struct AK::Traits<Socks5ConnectResponseHeader> : public AK::DefaultTraits<Socks5ConnectResponseHeader> {
    static constexpr bool is_trivially_serializable() { return true; }
};

struct [[gnu::packed]] Socks5ConnectResponseTrailer {
    u8 bind_port;
};

struct [[gnu::packed]] Socks5UsernamePasswordResponse {
    u8 version_identifier;
    u8 status;
};

template<>
struct AK::Traits<Socks5UsernamePasswordResponse> : public AK::DefaultTraits<Socks5UsernamePasswordResponse> {
    static constexpr bool is_trivially_serializable() { return true; }
};

namespace {
StringView reply_response_name(Reply reply)
{
    switch (reply) {
    case Reply::Succeeded:
        return "Succeeded"sv;
    case Reply::GeneralSocksServerFailure:
        return "GeneralSocksServerFailure"sv;
    case Reply::ConnectionNotAllowedByRuleset:
        return "ConnectionNotAllowedByRuleset"sv;
    case Reply::NetworkUnreachable:
        return "NetworkUnreachable"sv;
    case Reply::HostUnreachable:
        return "HostUnreachable"sv;
    case Reply::ConnectionRefused:
        return "ConnectionRefused"sv;
    case Reply::TTLExpired:
        return "TTLExpired"sv;
    case Reply::CommandNotSupported:
        return "CommandNotSupported"sv;
    case Reply::AddressTypeNotSupported:
        return "AddressTypeNotSupported"sv;
    }
    VERIFY_NOT_REACHED();
}

ErrorOr<void> send_version_identifier_and_method_selection_message(Core::Socket& socket, Core::SOCKSProxyClient::Version version, Method method)
{
    Socks5VersionIdentifierAndMethodSelectionMessage message {
        .version_identifier = to_underlying(version),
        .method_count = 1,
        .methods = { to_underlying(method) },
    };
    TRY(socket.write_value(message));

    auto response = TRY(socket.read_value<Socks5InitialResponse>());

    if (response.version_identifier != to_underlying(version))
        return Error::from_string_literal("SOCKS negotiation failed: Invalid version identifier");

    if (response.method != to_underlying(method))
        return Error::from_string_literal("SOCKS negotiation failed: Failed to negotiate a method");

    return {};
}

ErrorOr<Reply> send_connect_request_message(Core::Socket& socket, Core::SOCKSProxyClient::Version version, Core::SOCKSProxyClient::HostOrIPV4 target, int port, Core::SOCKSProxyClient::Command command)
{
    AllocatingMemoryStream stream;

    Socks5ConnectRequestHeader header {
        .version_identifier = to_underlying(version),
        .command = to_underlying(command),
        .reserved = 0,
    };
    Socks5ConnectRequestTrailer trailer {
        .port = htons(port),
    };

    TRY(stream.write_value(header));

    TRY(target.visit(
        [&](DeprecatedString const& hostname) -> ErrorOr<void> {
            u8 address_data[2];
            address_data[0] = to_underlying(AddressType::DomainName);
            address_data[1] = hostname.length();
            TRY(stream.write_until_depleted({ address_data, sizeof(address_data) }));
            TRY(stream.write_until_depleted({ hostname.characters(), hostname.length() }));
            return {};
        },
        [&](u32 ipv4) -> ErrorOr<void> {
            u8 address_data[5];
            address_data[0] = to_underlying(AddressType::IPV4);
            u32 network_ordered_ipv4 = NetworkOrdered<u32>(ipv4);
            memcpy(address_data + 1, &network_ordered_ipv4, sizeof(network_ordered_ipv4));
            TRY(stream.write_until_depleted({ address_data, sizeof(address_data) }));
            return {};
        }));

    TRY(stream.write_value(trailer));

    auto buffer = TRY(ByteBuffer::create_uninitialized(stream.used_buffer_size()));
    TRY(stream.read_until_filled(buffer.bytes()));
    TRY(socket.write_until_depleted(buffer));

    auto response_header = TRY(socket.read_value<Socks5ConnectResponseHeader>());

    if (response_header.version_identifier != to_underlying(version))
        return Error::from_string_literal("SOCKS negotiation failed: Invalid version identifier");

    auto response_address_type = TRY(socket.read_value<u8>());

    switch (AddressType(response_address_type)) {
    case AddressType::IPV4: {
        u8 response_address_data[4];
        TRY(socket.read_until_filled({ response_address_data, sizeof(response_address_data) }));
        break;
    }
    case AddressType::DomainName: {
        auto response_address_length = TRY(socket.read_value<u8>());
        auto buffer = TRY(ByteBuffer::create_uninitialized(response_address_length));
        TRY(socket.read_until_filled(buffer));
        break;
    }
    case AddressType::IPV6:
    default:
        return Error::from_string_literal("SOCKS negotiation failed: Invalid connect response address type");
    }

    [[maybe_unused]] auto bound_port = TRY(socket.read_value<u16>());

    return Reply(response_header.status);
}

ErrorOr<u8> send_username_password_authentication_message(Core::Socket& socket, Core::SOCKSProxyClient::UsernamePasswordAuthenticationData const& auth_data)
{
    AllocatingMemoryStream stream;

    u8 version = 0x01;
    TRY(stream.write_value(version));

    u8 username_length = auth_data.username.length();
    TRY(stream.write_value(username_length));

    TRY(stream.write_until_depleted({ auth_data.username.characters(), auth_data.username.length() }));

    u8 password_length = auth_data.password.length();
    TRY(stream.write_value(password_length));

    TRY(stream.write_until_depleted({ auth_data.password.characters(), auth_data.password.length() }));

    auto buffer = TRY(ByteBuffer::create_uninitialized(stream.used_buffer_size()));
    TRY(stream.read_until_filled(buffer.bytes()));

    TRY(socket.write_until_depleted(buffer));

    auto response = TRY(socket.read_value<Socks5UsernamePasswordResponse>());

    if (response.version_identifier != version)
        return Error::from_string_literal("SOCKS negotiation failed: Invalid version identifier");

    return response.status;
}
}

namespace Core {

SOCKSProxyClient::~SOCKSProxyClient()
{
    close();
    m_socket.on_ready_to_read = nullptr;
}

ErrorOr<NonnullOwnPtr<SOCKSProxyClient>> SOCKSProxyClient::connect(Socket& underlying, Version version, HostOrIPV4 const& target, int target_port, Variant<UsernamePasswordAuthenticationData, Empty> const& auth_data, Command command)
{
    if (version != Version::V5)
        return Error::from_string_literal("SOCKS version not supported");

    return auth_data.visit(
        [&](Empty) -> ErrorOr<NonnullOwnPtr<SOCKSProxyClient>> {
            TRY(send_version_identifier_and_method_selection_message(underlying, version, Method::NoAuth));
            auto reply = TRY(send_connect_request_message(underlying, version, target, target_port, command));
            if (reply != Reply::Succeeded) {
                underlying.close();
                return Error::from_string_view(reply_response_name(reply));
            }

            return adopt_nonnull_own_or_enomem(new SOCKSProxyClient {
                underlying,
                nullptr,
            });
        },
        [&](UsernamePasswordAuthenticationData const& auth_data) -> ErrorOr<NonnullOwnPtr<SOCKSProxyClient>> {
            TRY(send_version_identifier_and_method_selection_message(underlying, version, Method::UsernamePassword));
            auto auth_response = TRY(send_username_password_authentication_message(underlying, auth_data));
            if (auth_response != 0) {
                underlying.close();
                return Error::from_string_literal("SOCKS authentication failed");
            }

            auto reply = TRY(send_connect_request_message(underlying, version, target, target_port, command));
            if (reply != Reply::Succeeded) {
                underlying.close();
                return Error::from_string_view(reply_response_name(reply));
            }

            return adopt_nonnull_own_or_enomem(new SOCKSProxyClient {
                underlying,
                nullptr,
            });
        });
}

ErrorOr<NonnullOwnPtr<SOCKSProxyClient>> SOCKSProxyClient::connect(HostOrIPV4 const& server, int server_port, Version version, HostOrIPV4 const& target, int target_port, Variant<UsernamePasswordAuthenticationData, Empty> const& auth_data, Command command)
{
    auto underlying = TRY(server.visit(
        [&](u32 ipv4) {
            return Core::TCPSocket::connect({ IPv4Address(ipv4), static_cast<u16>(server_port) });
        },
        [&](DeprecatedString const& hostname) {
            return Core::TCPSocket::connect(hostname, static_cast<u16>(server_port));
        }));

    auto socket = TRY(connect(*underlying, version, target, target_port, auth_data, command));
    socket->m_own_underlying_socket = move(underlying);
    dbgln("SOCKS proxy connected, have {} available bytes", TRY(socket->m_socket.pending_bytes()));
    return socket;
}

}
