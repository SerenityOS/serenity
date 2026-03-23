/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SSHClient.h"

#include <AK/ByteBuffer.h>
#include <AK/Debug.h>
#include <AK/Format.h>
#include <AK/MemoryStream.h>
#include <AK/Random.h>
#include <LibCore/Account.h>
#include <LibCore/Command.h>
#include <LibCore/Socket.h>
#include <LibCrypto/Curves/Ed25519.h>
#include <LibCrypto/Curves/X25519.h>
#include <LibSSH/DataTypes.h>
#include <LibSSH/IdentificationString.h>
#include <Services/SSHServer/ServerConfiguration.h>

namespace SSH::Server {

ErrorOr<void> SSHClient::handle_data(ByteBuffer& data)
{
    switch (m_state) {
    case State::Constructed:
        return handle_protocol_version(data);
    case State::WaitingForKeyProtocolExchange:
        return handle_key_protocol_exchange(data);
    case State::WaitingForKeyExchange:
        return handle_key_exchange(data);
    case State::WaitingForNewKeysMessage:
        TRY(handle_new_keys_message(data));
        m_state = State::KeyExchanged;
        return {};
    case State::KeyExchanged:
        return handle_service_request(TRY(unpack_generic_message(data)));
    case State::WaitingForUserAuthentication:
        return handle_user_authentication(TRY(unpack_generic_message(data)));
    case State::Authentified:
        TRY(handle_generic_packet(TRY(unpack_generic_message(data))));
        return {};
    }
    VERIFY_NOT_REACHED();
}

// 4.2.  Protocol Version Exchange
// https://datatracker.ietf.org/doc/html/rfc4253#section-4.2

ErrorOr<void> SSHClient::handle_protocol_version(ByteBuffer& data)
{
    TRY(validate_identification_string(data));

    auto full_protocol_bytes = data.bytes().trim(data.bytes().size() - 2);
    m_key_exchange_data.client_identification_string = TRY(ByteBuffer::copy(full_protocol_bytes));
    m_key_exchange_data.server_identification_string = TRY(ByteBuffer::copy(
        PROTOCOL_STRING.substring_view(0, PROTOCOL_STRING.length() - 2).bytes()));

    data.clear();

    TRY(m_tcp_socket.write_until_depleted(PROTOCOL_STRING));

    m_state = State::WaitingForKeyProtocolExchange;

    return {};
}

// 7.  Key Exchange
// https://datatracker.ietf.org/doc/html/rfc4253#section-7
ErrorOr<void> SSHClient::handle_key_protocol_exchange(ByteBuffer& data)
{
    auto payload = TRY(read_packet(data));

    auto stream = FixedMemoryStream { payload.bytes() };

    m_key_exchange_data.client_key_init_payload = payload;

    auto message_id = TRY(stream.read_value<u8>());
    if (message_id != to_underlying(MessageID::KEXINIT))
        return Error::from_string_literal("Expected Key exchange message");

    // byte[16]     cookie (random bytes)
    TRY(stream.discard(16));

    // FIXME: Actually read the key exchange message.
    //        Right now, we just send back our favorites and assume that
    //        the client can use them. This is true for modern OpenSSH.

    TRY(send_key_protocol_message());

    m_state = State::WaitingForKeyExchange;

    dbgln_if(SSH_DEBUG, "KEXINIT message sent");

    return {};
}

// 7.1.  Algorithm Negotiation
// https://datatracker.ietf.org/doc/html/rfc4253#section-7.1

static constexpr auto KEX_ALGORITHMS = to_array({ "curve25519-sha256"sv });
static constexpr auto SERVER_HOST_KEY_ALGORITHMS = to_array({ "ssh-ed25519"sv });
static constexpr auto ENCRYPTION_ALGORITHMS_CLIENT_TO_SERVER = to_array({ "chacha20-poly1305@openssh.com"sv });
static constexpr auto ENCRYPTION_ALGORITHMS_SERVER_TO_CLIENT = to_array({ "chacha20-poly1305@openssh.com"sv });
static constexpr Array<StringView, 0> MAC_ALGORITHMS_CLIENT_TO_SERVER {};
static constexpr Array<StringView, 0> MAC_ALGORITHMS_SERVER_TO_CLIENT {};

// Per 5. Negotiation:
// https://datatracker.ietf.org/doc/html/draft-ietf-sshm-chacha20-poly1305-02#section-5
// The "chacha20-poly1305" offers both encryption and authentication. As
// such, no separate MAC is required. If the "chacha20-poly1305" cipher is
// selected in key exchange, the offered MAC algorithms are ignored and no
// MAC is required to be negotiated.

static constexpr auto COMPRESSION_ALGORITHMS_CLIENT_TO_SERVER = to_array({ "none"sv });
static constexpr auto COMPRESSION_ALGORITHMS_SERVER_TO_CLIENT = to_array({ "none"sv });
static constexpr Array<StringView, 0> LANGUAGES_CLIENT_TO_SERVER {};
static constexpr Array<StringView, 0> LANGUAGES_SERVER_TO_CLIENT {};

ErrorOr<void> SSHClient::send_key_protocol_message()
{
    AllocatingMemoryStream stream;

    TRY(stream.write_value<u8>(to_underlying(MessageID::KEXINIT)));

    auto cookie = TRY(ByteBuffer::create_uninitialized(16));
    fill_with_random(cookie);
    TRY(stream.write_until_depleted(cookie));

    TRY(encode_name_list(stream, KEX_ALGORITHMS));
    TRY(encode_name_list(stream, SERVER_HOST_KEY_ALGORITHMS));
    TRY(encode_name_list(stream, ENCRYPTION_ALGORITHMS_CLIENT_TO_SERVER));
    TRY(encode_name_list(stream, ENCRYPTION_ALGORITHMS_SERVER_TO_CLIENT));
    TRY(encode_name_list(stream, MAC_ALGORITHMS_CLIENT_TO_SERVER));
    TRY(encode_name_list(stream, MAC_ALGORITHMS_SERVER_TO_CLIENT));
    TRY(encode_name_list(stream, COMPRESSION_ALGORITHMS_CLIENT_TO_SERVER));
    TRY(encode_name_list(stream, COMPRESSION_ALGORITHMS_SERVER_TO_CLIENT));
    TRY(encode_name_list(stream, LANGUAGES_CLIENT_TO_SERVER));
    TRY(encode_name_list(stream, LANGUAGES_SERVER_TO_CLIENT));

    // first_kex_packet_follows
    TRY(stream.write_value(static_cast<u8>(false)));

    // "reserved for future extension"
    TRY(stream.write_value(static_cast<u32>(0)));

    auto payload = TRY(stream.read_until_eof());
    m_key_exchange_data.server_key_init_payload = payload;
    TRY(write_packet(payload));
    return {};
}

// 4.  ECDH Key Exchange
// https://datatracker.ietf.org/doc/html/rfc5656#section-4
ErrorOr<void> SSHClient::handle_key_exchange(ByteBuffer& data)
{
    auto packet = TRY(read_packet(data));

    auto stream = FixedMemoryStream { packet.bytes() };

    auto message_id = TRY(stream.read_value<u8>());
    if (message_id != to_underlying(MessageID::KEX_ECDH_INIT))
        return Error::from_string_literal("Expected Key ECDH exchange message");

    auto Q_C = TRY(decode_string(stream));

    // The host key type is determined, we can put it in the cryptographic data.
    m_key_exchange_data.server_public_host_key = ServerConfiguration::the().ssh_ed25519_server_public_key();

    TRY(send_ecdh_reply(move(Q_C)));

    TRY(send_new_keys_message());

    m_state = State::WaitingForNewKeysMessage;
    return {};
}

ErrorOr<void> SSHClient::send_ecdh_reply(ByteBuffer&& client_public_key)
{
    // "Verify received key is valid."
    // FIXME: Do the above step.
    if (client_public_key.size() != 32)
        return Error::from_string_literal("Expected 32 byte ECDH public key");

    m_key_exchange_data.client_ephemeral_publickey = client_public_key;

    // "Generate ephemeral key pair."
    Crypto::Curves::X25519 curve;
    auto private_key = TRY(curve.generate_private_key());
    auto public_key = TRY(curve.generate_public_key(private_key));
    m_key_exchange_data.server_ephemeral_publickey = public_key;

    // "Compute shared secret."
    auto shared_secret = TRY(curve.compute_coordinate(private_key, client_public_key));
    m_key_exchange_data.shared_secret = shared_secret;
    set_shared_secret(move(shared_secret));

    // FIXME: Abort if shared_point is not valid (at least when it's all zero, maybe there are other cases too).

    // Generate and sign exchange hash.
    auto hash = TRY(m_key_exchange_data.compute_sha_256());
    set_hash(hash);

    // "The server responds with:
    // byte     SSH_MSG_KEX_ECDH_REPLY
    // string   K_S, server's public host key
    // string   Q_S, server's ephemeral public key octet string
    // string   the signature on the exchange hash"

    AllocatingMemoryStream stream;

    TRY(stream.write_value(to_underlying(MessageID::KEX_ECDH_REPLY)));
    TRY(m_key_exchange_data.server_public_host_key.encode(stream));
    TRY(encode_string(stream, m_key_exchange_data.server_ephemeral_publickey));

    auto signature = TRY(Crypto::Curves::Ed25519::sign(
        ServerConfiguration::the().ssh_ed25519_server_public_key().key,
        ServerConfiguration::the().ssh_ed25519_server_private_key().key,
        hash.bytes()));

    TypedBlob signature_and_type {
        .type = TypedBlob::Type::SSH_ED25519,
        .key = move(signature),
    };

    TRY(signature_and_type.encode(stream));

    TRY(write_packet(TRY(stream.read_until_eof())));

    dbgln_if(SSH_DEBUG, "KEX_ECDH_REPLY message sent");

    return {};
}

ErrorOr<GenericMessage> SSHClient::unpack_generic_message(ByteBuffer& data)
{
    auto payload = TRY(read_packet(data));

    // This is ensured by read_packet().
    VERIFY(payload.size() >= 1);

    return GenericMessage { move(payload) };
}

// 10.  Service Request
// https://datatracker.ietf.org/doc/html/rfc4253#section-10
ErrorOr<void> SSHClient::handle_service_request(GenericMessage message)
{
    if (message.type != MessageID::SERVICE_REQUEST) {
        dbgln_if(SSH_DEBUG, "Received packet type: {}", to_underlying(message.type));
        return Error::from_string_literal("Expected packet of type SERVICE_REQUEST");
    }

    auto service_name = TRY(decode_string(message.payload));
    dbgln_if(SSH_DEBUG, "Service '{:s}' requested", service_name.bytes());

    if (service_name == "ssh-userauth"sv.bytes()) {
        // "If the server supports the service (and permits the client to use
        // it), it MUST respond with the following:
        //   byte      SSH_MSG_SERVICE_ACCEPT
        //   string    service name
        // "

        TRY(send_service_accept(service_name));
        m_state = State::WaitingForUserAuthentication;
        return {};
    }

    // FIXME: "If the server rejects the service request, it SHOULD send an
    // appropriate SSH_MSG_DISCONNECT message and MUST disconnect."

    return Error::from_string_literal("Unexpected service name");
}

ErrorOr<void> SSHClient::send_service_accept(StringView service_name)
{
    AllocatingMemoryStream stream;
    TRY(stream.write_value(MessageID::SERVICE_ACCEPT));
    TRY(encode_string(stream, service_name));
    TRY(write_packet(TRY(stream.read_until_eof())));
    return {};
}

// 5.  Authentication Requests
// https://datatracker.ietf.org/doc/html/rfc4252#section-5
ErrorOr<void> SSHClient::handle_user_authentication(GenericMessage message)
{
    if (message.type != MessageID::USERAUTH_REQUEST)
        return Error::from_string_literal("Expected packet of type USERAUTH_REQUEST");

    auto username = TRY(decode_string(message.payload));
    auto service_name = TRY(decode_string(message.payload));
    auto method_name = TRY(decode_string(message.payload));

    dbgln_if(SSH_DEBUG, "User authentication username: {:s}", username.bytes());
    dbgln_if(SSH_DEBUG, "User authentication service_name: {:s}", service_name.bytes());
    dbgln_if(SSH_DEBUG, "User authentication method name: {:s}", method_name.bytes());

    if (username != TRY(Core::Account::self(Core::Account::Read::PasswdOnly)).username())
        return Error::from_string_literal("Can't authenticate for another user account");

    if (method_name == "none"sv.bytes()) {
        // FIXME: Implement proper authentication!!!

        m_state = State::Authentified;
        TRY(send_user_authentication_success());

        // FIXME: Also send a cool banner :^)

        return {};
    }

    return Error::from_string_literal("Unsupported userauth method");
}

// 5.1.  Responses to Authentication Requests
// https://datatracker.ietf.org/doc/html/rfc4252#section-5.1
ErrorOr<void> SSHClient::send_user_authentication_success()
{
    AllocatingMemoryStream stream;
    TRY(stream.write_value(MessageID::USERAUTH_SUCCESS));
    TRY(write_packet(TRY(stream.read_until_eof())));
    return {};
}

ErrorOr<void> SSHClient::handle_generic_packet(GenericMessage&& message)
{
    switch (message.type) {
    case MessageID::CHANNEL_OPEN:
        return handle_channel_open_message(message);
    case MessageID::CHANNEL_REQUEST:
        return handle_channel_request(message);
    default:
        dbgln_if(SSH_DEBUG, "Unexpected packet: {}", to_underlying(message.type));
        return Error::from_string_literal("Unexpected packet type");
    }
    VERIFY_NOT_REACHED();
}

// 5.1.  Opening a Channel
// https://datatracker.ietf.org/doc/html/rfc4254#section-5.1
ErrorOr<void> SSHClient::handle_channel_open_message(GenericMessage& message)
{
    auto channel_type = TRY(decode_string(message.payload));
    u32 sender_channel_id = TRY(message.payload.read_value<NetworkOrdered<u32>>());
    u32 initial_window_size = TRY(message.payload.read_value<NetworkOrdered<u32>>());
    u32 maximum_packet_size = TRY(message.payload.read_value<NetworkOrdered<u32>>());

    dbgln_if(SSH_DEBUG, "Channel open request with: {:s} - {} - {} - {}",
        channel_type.bytes(), sender_channel_id, initial_window_size, maximum_packet_size);

    if (channel_type != "session"sv.bytes())
        return Error::from_string_literal("Unexpected channel type");

    m_sessions.empend(TRY(Session::create(sender_channel_id, initial_window_size, maximum_packet_size)));

    TRY(send_channel_open_confirmation(m_sessions.last()));

    return {};
}

// 5.1.  Opening a Channel
// https://datatracker.ietf.org/doc/html/rfc4254#section-5.1
ErrorOr<void> SSHClient::send_channel_open_confirmation(Session const& session)
{
    AllocatingMemoryStream stream;

    //  byte      SSH_MSG_CHANNEL_OPEN_CONFIRMATION
    //  uint32    recipient channel
    //  uint32    sender channel
    //  uint32    initial window size
    //  uint32    maximum packet size

    TRY(stream.write_value(MessageID::CHANNEL_OPEN_CONFIRMATION));
    // "The 'recipient channel' is the channel number given in the original
    // open request, and 'sender channel' is the channel number allocated by
    // the other side."
    TRY(stream.write_value<NetworkOrdered<u32>>(session.sender_channel_id));
    TRY(stream.write_value<NetworkOrdered<u32>>(session.local_channel_id));

    TRY(stream.write_value<NetworkOrdered<u32>>(session.window.size()));
    TRY(stream.write_value<NetworkOrdered<u32>>(session.maximum_packet_size));

    TRY(write_packet(TRY(stream.read_until_eof())));
    return {};
}

ErrorOr<Session*> SSHClient::find_session(u32 sender_channel_id)
{
    for (auto& session : m_sessions) {
        if (session.sender_channel_id == sender_channel_id)
            return &session;
    }
    return Error::from_string_literal("Session not found");
}

// 5.4.  Channel-Specific Requests
// https://datatracker.ietf.org/doc/html/rfc4254#section-5.4
ErrorOr<void> SSHClient::handle_channel_request(GenericMessage& message)
{
    auto recipient_channel_id = TRY(message.payload.read_value<NetworkOrdered<u32>>());
    auto request_type = TRY(decode_string(message.payload));
    auto want_reply = TRY(message.payload.read_value<bool>());

    auto& session = *TRY(find_session(recipient_channel_id));

    dbgln_if(SSH_DEBUG, "CHANNEL_REQUEST id({}): {:s}", session.local_channel_id, request_type.bytes());

    if (request_type == "env"sv.bytes() && !want_reply) {
        dbgln("FIXME: Ignored channel request: {:s}", request_type.bytes());
        return {};
    }

    if (request_type == "exec"sv.bytes()) {
        auto command = TRY(decode_string(message.payload));

        // FIXME: This is a naive implementation, we should stream the result back
        //        to the user and not block the event loop during the execution of
        //        the command.
        //        We should also use the user's shell rather than hardcoding it.

#ifdef AK_OS_SERENITY
        auto shell = "/bin/Shell"sv;
#else
        auto shell = "/bin/sh"sv;
#endif

        Vector<ByteString> args;
        args.append(shell);
        args.append("-c");
        args.append(ByteString(command.bytes()));

        Vector<char const*> raw_args;
        raw_args.ensure_capacity(args.size() + 1);
        for (auto& arg : args)
            raw_args.append(arg.characters());

        raw_args.append(nullptr);

        auto child = TRY(Core::Command::create(shell, raw_args.data()));
        auto output = TRY(child->read_all());
        auto status = TRY(child->status());

        if (status != Core::Command::ProcessResult::DoneWithZeroExitCode)
            return Error::from_string_literal("Unable to run command");

        TRY(send_channel_success_message(session));
        TRY(send_channel_data(session, output.standard_output));
        TRY(send_channel_close(session));
        return {};
    }

    return Error::from_string_literal("Unsupported channel request");
}

ErrorOr<void> SSHClient::send_channel_success_message(Session const& session)
{
    AllocatingMemoryStream stream;
    TRY(stream.write_value(MessageID::CHANNEL_SUCCESS));
    TRY(stream.write_value<NetworkOrdered<u32>>(session.local_channel_id));
    TRY(write_packet(TRY(stream.read_until_eof())));
    return {};
}

ErrorOr<void> SSHClient::send_channel_data(Session const& session, ByteBuffer const& data)
{
    AllocatingMemoryStream stream;
    TRY(stream.write_value(MessageID::CHANNEL_DATA));
    TRY(stream.write_value<NetworkOrdered<u32>>(session.local_channel_id));
    TRY(encode_string(stream, data));
    TRY(write_packet(TRY(stream.read_until_eof())));
    return {};
}

ErrorOr<void> SSHClient::send_channel_close(Session const& session)
{
    AllocatingMemoryStream stream;
    TRY(stream.write_value(MessageID::CHANNEL_CLOSE));
    TRY(stream.write_value<NetworkOrdered<u32>>(session.local_channel_id));
    TRY(write_packet(TRY(stream.read_until_eof())));
    return {};
}

} // SSHServer
