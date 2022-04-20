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

struct [[gnu::packed]] Socks5InitialResponse {
    u8 version_identifier;
    u8 method;
};

struct [[gnu::packed]] Socks5ConnectRequestHeader {
    u8 version_identifier;
    u8 command;
    u8 reserved;
};

struct [[gnu::packed]] Socks5ConnectRequestTrailer {
    u16 port;
};

struct [[gnu::packed]] Socks5ConnectResponseHeader {
    u8 version_identifier;
    u8 status;
    u8 reserved;
};

struct [[gnu::packed]] Socks5ConnectResponseTrailer {
    u8 bind_port;
};

struct [[gnu::packed]] Socks5UsernamePasswordResponse {
    u8 version_identifier;
    u8 status;
};

namespace {
StringView reply_response_name(Reply reply)
{
    switch (reply) {
    case Reply::Succeeded:
        return "Succeeded";
    case Reply::GeneralSocksServerFailure:
        return "GeneralSocksServerFailure";
    case Reply::ConnectionNotAllowedByRuleset:
        return "ConnectionNotAllowedByRuleset";
    case Reply::NetworkUnreachable:
        return "NetworkUnreachable";
    case Reply::HostUnreachable:
        return "HostUnreachable";
    case Reply::ConnectionRefused:
        return "ConnectionRefused";
    case Reply::TTLExpired:
        return "TTLExpired";
    case Reply::CommandNotSupported:
        return "CommandNotSupported";
    case Reply::AddressTypeNotSupported:
        return "AddressTypeNotSupported";
    }
    VERIFY_NOT_REACHED();
}

ErrorOr<void> send_version_identifier_and_method_selection_message(Core::Stream::Socket& socket, Core::SOCKSProxyClient::Version version, Method method)
{
    Socks5VersionIdentifierAndMethodSelectionMessage message {
        .version_identifier = to_underlying(version),
        .method_count = 1,
        .methods = { to_underlying(method) },
    };
    auto size = TRY(socket.write({ &message, sizeof(message) }));
    if (size != sizeof(message))
        return Error::from_string_literal("SOCKS negotiation failed: Failed to send version identifier and method selection message");

    Socks5InitialResponse response;
    size = TRY(socket.read({ &response, sizeof(response) })).size();
    if (size != sizeof(response))
        return Error::from_string_literal("SOCKS negotiation failed: Failed to receive initial response");

    if (response.version_identifier != to_underlying(version))
        return Error::from_string_literal("SOCKS negotiation failed: Invalid version identifier");

    if (response.method != to_underlying(method))
        return Error::from_string_literal("SOCKS negotiation failed: Failed to negotiate a method");

    return {};
}

ErrorOr<Reply> send_connect_request_message(Core::Stream::Socket& socket, Core::SOCKSProxyClient::Version version, Core::SOCKSProxyClient::HostOrIPV4 target, int port, Core::SOCKSProxyClient::Command command)
{
    DuplexMemoryStream stream;

    Socks5ConnectRequestHeader header {
        .version_identifier = to_underlying(version),
        .command = to_underlying(command),
        .reserved = 0,
    };
    Socks5ConnectRequestTrailer trailer {
        .port = htons(port),
    };

    auto size = stream.write({ &header, sizeof(header) });
    if (size != sizeof(header))
        return Error::from_string_literal("SOCKS negotiation failed: Failed to send connect request header");

    TRY(target.visit(
        [&](String const& hostname) -> ErrorOr<void> {
            u8 address_data[2];
            address_data[0] = to_underlying(AddressType::DomainName);
            address_data[1] = hostname.length();
            auto size = stream.write({ address_data, sizeof(address_data) });
            if (size != array_size(address_data))
                return Error::from_string_literal("SOCKS negotiation failed: Failed to send connect request address data");
            stream.write({ hostname.characters(), hostname.length() });
            return {};
        },
        [&](u32 ipv4) -> ErrorOr<void> {
            u8 address_data[5];
            address_data[0] = to_underlying(AddressType::IPV4);
            u32 network_ordered_ipv4 = NetworkOrdered<u32>(ipv4);
            memcpy(address_data + 1, &network_ordered_ipv4, sizeof(network_ordered_ipv4));
            auto size = stream.write({ address_data, sizeof(address_data) });
            if (size != array_size(address_data))
                return Error::from_string_literal("SOCKS negotiation failed: Failed to send connect request address data");
            return {};
        }));

    size = stream.write({ &trailer, sizeof(trailer) });
    if (size != sizeof(trailer))
        return Error::from_string_literal("SOCKS negotiation failed: Failed to send connect request trailer");

    auto buffer = stream.copy_into_contiguous_buffer();
    size = TRY(socket.write({ buffer.data(), buffer.size() }));
    if (size != buffer.size())
        return Error::from_string_literal("SOCKS negotiation failed: Failed to send connect request");

    Socks5ConnectResponseHeader response_header;
    size = TRY(socket.read({ &response_header, sizeof(response_header) })).size();
    if (size != sizeof(response_header))
        return Error::from_string_literal("SOCKS negotiation failed: Failed to receive connect response header");

    if (response_header.version_identifier != to_underlying(version))
        return Error::from_string_literal("SOCKS negotiation failed: Invalid version identifier");

    u8 response_address_type;
    size = TRY(socket.read({ &response_address_type, sizeof(response_address_type) })).size();
    if (size != sizeof(response_address_type))
        return Error::from_string_literal("SOCKS negotiation failed: Failed to receive connect response address type");

    switch (AddressType(response_address_type)) {
    case AddressType::IPV4: {
        u8 response_address_data[4];
        size = TRY(socket.read({ response_address_data, sizeof(response_address_data) })).size();
        if (size != sizeof(response_address_data))
            return Error::from_string_literal("SOCKS negotiation failed: Failed to receive connect response address data");
        break;
    }
    case AddressType::DomainName: {
        u8 response_address_length;
        size = TRY(socket.read({ &response_address_length, sizeof(response_address_length) })).size();
        if (size != sizeof(response_address_length))
            return Error::from_string_literal("SOCKS negotiation failed: Failed to receive connect response address length");
        ByteBuffer buffer;
        buffer.resize(response_address_length);
        size = TRY(socket.read(buffer)).size();
        if (size != response_address_length)
            return Error::from_string_literal("SOCKS negotiation failed: Failed to receive connect response address data");
        break;
    }
    case AddressType::IPV6:
    default:
        return Error::from_string_literal("SOCKS negotiation failed: Invalid connect response address type");
    }

    u16 bound_port;
    size = TRY(socket.read({ &bound_port, sizeof(bound_port) })).size();
    if (size != sizeof(bound_port))
        return Error::from_string_literal("SOCKS negotiation failed: Failed to receive connect response bound port");

    return Reply(response_header.status);
}

ErrorOr<u8> send_username_password_authentication_message(Core::Stream::Socket& socket, Core::SOCKSProxyClient::UsernamePasswordAuthenticationData const& auth_data)
{
    DuplexMemoryStream stream;

    u8 version = 0x01;
    auto size = stream.write({ &version, sizeof(version) });
    if (size != sizeof(version))
        return Error::from_string_literal("SOCKS negotiation failed: Failed to send username/password authentication message");

    u8 username_length = auth_data.username.length();
    size = stream.write({ &username_length, sizeof(username_length) });
    if (size != sizeof(username_length))
        return Error::from_string_literal("SOCKS negotiation failed: Failed to send username/password authentication message");

    size = stream.write({ auth_data.username.characters(), auth_data.username.length() });
    if (size != auth_data.username.length())
        return Error::from_string_literal("SOCKS negotiation failed: Failed to send username/password authentication message");

    u8 password_length = auth_data.password.length();
    size = stream.write({ &password_length, sizeof(password_length) });
    if (size != sizeof(password_length))
        return Error::from_string_literal("SOCKS negotiation failed: Failed to send username/password authentication message");

    size = stream.write({ auth_data.password.characters(), auth_data.password.length() });
    if (size != auth_data.password.length())
        return Error::from_string_literal("SOCKS negotiation failed: Failed to send username/password authentication message");

    auto buffer = stream.copy_into_contiguous_buffer();
    size = TRY(socket.write(buffer));
    if (size != buffer.size())
        return Error::from_string_literal("SOCKS negotiation failed: Failed to send username/password authentication message");

    Socks5UsernamePasswordResponse response;
    size = TRY(socket.read({ &response, sizeof(response) })).size();
    if (size != sizeof(response))
        return Error::from_string_literal("SOCKS negotiation failed: Failed to receive username/password authentication response");

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
                return Error::from_string_literal(reply_response_name(reply));
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
                return Error::from_string_literal(reply_response_name(reply));
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
            return Core::Stream::TCPSocket::connect({ IPv4Address(ipv4), static_cast<u16>(server_port) });
        },
        [&](String const& hostname) {
            return Core::Stream::TCPSocket::connect(hostname, static_cast<u16>(server_port));
        }));

    auto socket = TRY(connect(*underlying, version, target, target_port, auth_data, command));
    socket->m_own_underlying_socket = move(underlying);
    dbgln("SOCKS proxy connected, have {} available bytes", TRY(socket->m_socket.pending_bytes()));
    return socket;
}

}
