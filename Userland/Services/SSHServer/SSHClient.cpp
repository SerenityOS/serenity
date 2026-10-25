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
#include <AK/NumericLimits.h>
#include <AK/Random.h>
#include <AK/ScopeGuard.h>
#include <AK/StdLibExtras.h>
#include <AK/TemporaryChange.h>
#include <LibCore/Account.h>
#include <LibCore/EventLoop.h>
#include <LibCore/Process.h>
#include <LibCore/Socket.h>
#include <LibCore/System.h>
#include <LibCrypto/Curves/Ed25519.h>
#include <LibCrypto/Curves/X25519.h>
#include <LibSSH/DataTypes.h>
#include <LibSSH/IdentificationString.h>
#include <Services/SSHServer/ServerConfiguration.h>

namespace SSH::Server {

static constexpr u32 MAXIMUM_AUTHENTICATION_ATTEMPTS = 6;
static constexpr u32 SERVER_INITIAL_CHANNEL_WINDOW_SIZE = 512 * 1024;
static constexpr u32 SERVER_MAXIMUM_CHANNEL_PACKET_SIZE = 32 * 1024;
static constexpr u32 MINIMUM_PEER_CHANNEL_PACKET_SIZE = 1024;
// Bound memory retained for channel output while the peer's window is closed.
static constexpr size_t MAXIMUM_QUEUED_CHANNEL_DATA_SIZE = 512 * 1024;
static constexpr size_t MAXIMUM_QUEUED_CHANNEL_DATA_MESSAGES = 1024;
// Bounds the aggregate advertised receive window to 8 MiB per SSH connection.
static constexpr size_t MAXIMUM_CHANNELS_PER_CONNECTION = 16;

SSHClient::SSHClient(Core::TCPSocket& tcp_socket, Function<void()> disconnect)
    : Peer(tcp_socket)
    , m_disconnect(move(disconnect))
{
    m_sigchld_handler_id = Core::EventLoop::register_signal(SIGCHLD, [this](int) {
        if (auto maybe_error = manage_child_death(); maybe_error.is_error()) {
            // FIXME: Maybe disconnect?
            dbgln("Error while trying to manage child death: {}", maybe_error.error());
        }
    });
}

SSHClient::~SSHClient()
{
    shutdown();
}

void SSHClient::shutdown()
{
    if (m_is_shutdown)
        return;

    m_is_shutdown = true;
    revoke_weak_ptrs();
    set_output_pending_callback({});
    Core::EventLoop::unregister_signal(m_sigchld_handler_id);
    m_sigchld_handler_id = 0;

    for (auto& session : m_sessions) {
        session->is_closed = true;
        session->pending_outgoing_channel_data.clear();
        session->pending_outgoing_channel_data_bytes = 0;
        session->notify_remote_window_waiters();
        if (session->system.has<ExecData>())
            session->system.get<ExecData>().terminate();
    }
}

ErrorOr<BehaviorControl> SSHClient::handle_data(ByteBuffer& data)
{
    if (m_state != State::Constructed) {
        if (!TRY(is_buffer_containing_a_full_packet(data)))
            return BehaviorControl::WaitForMoreData;
    }

    switch (m_state) {
    case State::Constructed:
        TRY(handle_protocol_version(data));
        break;
    case State::WaitingForKeyProtocolExchange:
        TRY(handle_key_protocol_exchange(data));
        break;
    case State::WaitingForKeyExchange:
        TRY(handle_key_exchange(data));
        break;
    case State::WaitingForNewKeysMessage:
        TRY(handle_new_keys_message(data));
        m_state = State::KeyExchanged;
        break;
    case State::KeyExchanged:
        TRY(handle_service_request(TRY(unpack_generic_message(data))));
        break;
    case State::WaitingForUserAuthentication:
        TRY(handle_user_authentication(TRY(unpack_generic_message(data))));
        break;
    case State::Authentified:
        return handle_generic_packet(TRY(unpack_generic_message(data)));
    }
    return BehaviorControl::ContinueExecution;
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

    TRY(write_raw_bytes(PROTOCOL_STRING.bytes()));

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

    m_cookie = TRY(ByteBuffer::create_uninitialized(16));
    fill_with_random(m_cookie);
    TRY(stream.write_until_depleted(m_cookie));

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
    if (!KeyExchangeData::is_valid_x25519_shared_secret(shared_secret.bytes()))
        return Error::from_string_literal("ECDH produced an invalid shared secret");
    m_key_exchange_data.shared_secret = shared_secret;

    if (auto keylog_file = ServerConfiguration::the().keylog_file(); keylog_file.has_value()) {
        auto file = TRY(Core::File::open(*keylog_file, Core::File::OpenMode::Write | Core::File::OpenMode::Append));
        TRY(file->write_until_depleted(ByteString::formatted("{:hex-dump}"sv, m_cookie.bytes())));
        TRY(file->write_until_depleted(" SHARED_SECRET "sv));
        TRY(file->write_until_depleted(ByteString::formatted("{:hex-dump}\n"sv, shared_secret.bytes())));
    }
    m_cookie = {};

    set_shared_secret(move(shared_secret));

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

    if (m_authentication_attempts >= MAXIMUM_AUTHENTICATION_ATTEMPTS)
        return Error::from_string_literal("Too many authentication attempts");
    ++m_authentication_attempts;

    auto username = TRY(decode_string(message.payload));
    auto service_name = TRY(decode_string(message.payload));
    auto method_name = TRY(decode_string(message.payload));

    auto self = TRY(Core::Account::self(Core::Account::Read::PasswdOnly));
    auto need_to_change_user = username != self.username();

    if (getuid() != 0 && need_to_change_user)
        return Error::from_string_literal("Can't authenticate for another user account");

    // "The 'service name' specifies the service to start after authentication. There may
    // be several different authenticated services provided. If the requested service is
    // not available, the server MAY disconnect immediately or at any later time. Sending
    // a proper disconnect message is RECOMMENDED.  In any case, if the service does not
    // exist, authentication MUST NOT be accepted."
    if (service_name != "ssh-connection"sv.bytes())
        return Error::from_string_literal("Unknown service name.");

    Optional<Core::Account> user {};
    if (need_to_change_user) {
        auto maybe_user = Core::Account::from_name(username, Core::Account::Read::PasswdOnly);
        if (!maybe_user.is_error())
            user = maybe_user.release_value();
    } else {
        user = self;
    }

    if (user.has_value()) {
        if (method_name == "publickey"sv.bytes())
            return handle_publickey_message(message, *user, service_name);
    }

    TRY(send_available_authentication_methods());
    return {};
}

namespace {

// 7.  Public Key Authentication Method: "publickey"
// https://datatracker.ietf.org/doc/html/rfc4252#section-7
ErrorOr<ByteBuffer> make_authentication_message(
    ReadonlyBytes session_identifier,
    ReadonlyBytes user_name,
    ReadonlyBytes service_name,
    ReadonlyBytes algorithm_name,
    TypedBlob const& public_key)
{
    AllocatingMemoryStream stream;

    TRY(encode_string(stream, session_identifier));
    TRY(stream.write_value(MessageID::USERAUTH_REQUEST));
    TRY(encode_string(stream, user_name));
    TRY(encode_string(stream, service_name));
    TRY(encode_string(stream, "publickey"sv));
    TRY(stream.write_value(true));
    TRY(encode_string(stream, algorithm_name));
    TRY(public_key.encode(stream));

    return stream.read_until_eof();
}

}

// 7.  Public Key Authentication Method: "publickey"
// https://datatracker.ietf.org/doc/html/rfc4252#section-7
ErrorOr<void> SSHClient::handle_publickey_message(
    GenericMessage& message,
    Core::Account const& user,
    ReadonlyBytes service_name)
{
    bool contains_signature = TRY(message.payload.read_value<bool>());
    auto algorithm_name = TRY(decode_string(message.payload));
    auto blob = TRY(decode_string(message.payload));

    if (!contains_signature) {
        // FIXME: Stop being lazy and actually check if this could be a
        //        possible match.
        TRY(send_publickey_ok_message(algorithm_name, blob));
        return {};
    }

    auto signature = TRY(TypedBlob::decode(message.payload));

    auto authorized_keys = [&user] -> Vector<TypedBlob> {
        auto maybe_keys = ServerConfiguration::the().get_authorized_keys_for_user(user);
        if (maybe_keys.is_error()) {
            dbgln("warning: Impossible to load authorized keys: {}", maybe_keys.release_error());
            return {};
        }
        return maybe_keys.release_value();
    }();

    for (auto const& public_key : authorized_keys) {
        if (algorithm_name != TypedBlob::type_to_string(public_key.type).bytes())
            continue;

        auto username = user.username();
        auto authentication_message = TRY(make_authentication_message(
            session_id(),
            username.bytes(),
            service_name,
            algorithm_name,
            public_key));

        bool success { false };
        switch (public_key.type) {
        case TypedBlob::Type::SSH_ED25519:
            success = Crypto::Curves::Ed25519::verify(public_key.key, signature.key, authentication_message);
        }

        if (!success)
            continue;

        m_state = State::Authentified;
        TRY(send_user_authentication_success());

        if (!user.is_self())
            TRY(user.login());

        outln("Successful authentication for: {:s}", user.username());

        // FIXME: Also send a cool banner :^)
        return {};
    }

    // This is the way to tell the client the authentication didn't succeed.
    TRY(send_available_authentication_methods());
    return {};
}

ErrorOr<void> SSHClient::send_publickey_ok_message(ReadonlyBytes algorithm_name, ReadonlyBytes blob)
{
    AllocatingMemoryStream stream;
    TRY(stream.write_value(MessageID::USERAUTH_PK_OK));
    TRY(encode_string(stream, algorithm_name));
    TRY(encode_string(stream, blob));

    auto packet = TRY(stream.read_until_eof());
    TRY(write_packet(packet));

    return {};
}

// 5.1.  Responses to Authentication Requests
// https://datatracker.ietf.org/doc/html/rfc4252#section-5.1
ErrorOr<void> SSHClient::send_available_authentication_methods()
{
    AllocatingMemoryStream stream;
    TRY(stream.write_value(MessageID::USERAUTH_FAILURE));
    static constexpr auto available_methods = to_array({ "publickey"sv });
    TRY(encode_name_list(stream, available_methods));

    // "The value of 'partial success' MUST be TRUE if the authentication
    //   request to which this is a response was successful."
    TRY(stream.write_value(false));

    TRY(write_packet(TRY(stream.read_until_eof())));
    return {};
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

ErrorOr<BehaviorControl> SSHClient::handle_generic_packet(GenericMessage&& message)
{
    switch (message.type) {
    case MessageID::DISCONNECT:
        TRY(handle_disconnect_message(message.data));
        return BehaviorControl::Disconnect;
    case MessageID::CHANNEL_OPEN:
        TRY(handle_channel_open_message(message));
        break;
    case MessageID::CHANNEL_REQUEST:
        TRY(handle_channel_request(message));
        break;
    case MessageID::CHANNEL_DATA:
        TRY(handle_channel_data(message));
        break;
    case MessageID::CHANNEL_EOF:
        TRY(handle_channel_eof(message));
        break;
    case MessageID::CHANNEL_WINDOW_ADJUST:
        TRY(handle_channel_window_adjust_message(message));
        break;
    case MessageID::CHANNEL_CLOSE:
        TRY(handle_channel_close(message));
        break;
    default:
        dbgln_if(SSH_DEBUG, "Unexpected packet: {}", to_underlying(message.type));
        return Error::from_string_literal("Unexpected packet type");
    }
    return BehaviorControl::ContinueExecution;
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

    // Tiny peer limits can turn every byte of command output into a separate
    // SSH packet, so reject pathological values rather than amplify CPU work.
    if (maximum_packet_size < MINIMUM_PEER_CHANNEL_PACKET_SIZE)
        return Error::from_string_literal("Channel maximum packet size is too small");

    if (m_sessions.size() >= MAXIMUM_CHANNELS_PER_CONNECTION)
        return Error::from_string_literal("Too many channels for this SSH connection");

    for (auto const& session : m_sessions) {
        if (session->sender_channel_id == sender_channel_id)
            return Error::from_string_literal("Duplicate channel ID");
    }

    // Do not reuse the peer's window as our receive capacity: advertise a
    // bounded server window for each channel.
    TRY(m_sessions.try_empend(TRY(Session::create(sender_channel_id, SERVER_INITIAL_CHANNEL_WINDOW_SIZE, maximum_packet_size))));
    m_sessions.last()->remote_window_size = initial_window_size;

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

    TRY(stream.write_value<NetworkOrdered<u32>>(session.initial_window_size));
    TRY(stream.write_value<NetworkOrdered<u32>>(SERVER_MAXIMUM_CHANNEL_PACKET_SIZE));

    TRY(write_packet(TRY(stream.read_until_eof())));
    return {};
}

ErrorOr<NonnullRefPtr<Session>> SSHClient::find_session(u32 local_channel_id)
{
    for (auto& session : m_sessions) {
        if (session->local_channel_id == local_channel_id)
            return session;
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
        if (!want_reply)
            return Error::from_string_literal("Client requested exec but doesn't want a reply");
        TRY(handle_channel_exec(session, message));
        return {};
    }

    if (request_type == "subsystem"sv.bytes()) {
        TRY(handle_channel_subsystem(session, message));
        return {};
    }

    return Error::from_string_literal("Unsupported channel request");
}

// 5.2.  Data Transfer
// https://datatracker.ietf.org/doc/html/rfc4254#section-5.2
ErrorOr<void> SSHClient::handle_channel_data(GenericMessage& message)
{
    auto recipient_channel_id = TRY(message.payload.read_value<NetworkOrdered<u32>>());
    auto data = TRY(decode_string(message.payload));
    if (message.payload.remaining() != 0)
        return Error::from_string_literal("Trailing data in channel data message");
    if (data.is_empty())
        return Error::from_string_literal("Empty channel data message");

    auto& session = *TRY(find_session(recipient_channel_id));

    if (session.is_closed)
        return Error::from_string_literal("Received channel data for a closed channel");
    if (session.system.has<Empty>())
        return Error::from_string_literal("Received channel data while subsystem is unknown");

    if (session.has_received_eof)
        return Error::from_string_literal("Received channel data after EOF");

    if (data.size() > SERVER_MAXIMUM_CHANNEL_PACKET_SIZE)
        return Error::from_string_literal("Received oversized channel data");

    if (!session.can_receive_data(data.size()))
        return Error::from_string_literal("Received channel data beyond the advertised window");

    session.channel_data.append(data);
    session.total_received_bytes += data.size();

    // Only one consumer coroutine may dequeue this channel at a time. Shutdown
    // marks the session closed and wakes it so it can safely exit.
    if (!session.has_streaming_coroutine)
        Core::EventLoop::adopt_coroutine(async_stream_data_to_subsystem(session));

    return {};
}

Coroutine<void> SSHClient::async_stream_data_to_subsystem(NonnullRefPtr<Session> session)
{
    auto weak_this = make_weak_ptr();
    TemporaryChange t(session->has_streaming_coroutine, true);

    auto maybe_error = co_await [&]() -> Coroutine<ErrorOr<void>> {
        while (true) {
            if (session->is_closed || session->channel_data.data().is_empty())
                break;

            auto maybe_behavior = co_await session->system.visit(
                [&](auto& system) -> Coroutine<ErrorOr<BehaviorControl>> { return system.handle_channel_data(session); },
                [](Empty) -> Coroutine<ErrorOr<BehaviorControl>> { VERIFY_NOT_REACHED(); });
            if (weak_this.is_null() || session->is_closed)
                co_return {};
            auto behavior = CO_TRY(move(maybe_behavior));

            CO_TRY(adjust_channel_window_if_needed(*session));
            switch (behavior) {
            case BehaviorControl::ContinueExecution:
                break;
            case BehaviorControl::WaitForMoreData:
                co_return {};
            case BehaviorControl::Disconnect:
                co_return Error::from_string_literal("Subsystem disconnected");
            }
        }

        if (weak_this.is_null() || session->is_closed)
            co_return {};
        CO_TRY(close_session_if_needed(*session));

        co_return {};
    }();

    if (weak_this.is_null() || session->is_closed)
        co_return;

    if (maybe_error.is_error())
        disconnect(maybe_error.release_error());

    co_return;
}

// 6.5.  Starting a Shell or a Command
// https://datatracker.ietf.org/doc/html/rfc4254#section-6.5
ErrorOr<void> SSHClient::handle_channel_exec(NonnullRefPtr<Session> const& session, GenericMessage& message)
{
    if (session->is_closed || !session->system.has<Empty>())
        return Error::from_string_literal("A request is already active on this channel");

    auto command = TRY(decode_string(message.payload));
    if (message.payload.remaining() != 0)
        return Error::from_string_literal("Trailing data in exec request");

    // FIXME: We should use the user's shell rather than hardcoding it.

#ifdef AK_OS_SERENITY
    auto shell = "/bin/Shell"sv;
#else
    auto shell = "/bin/sh"sv;
#endif

    Vector<ByteString> args;
    args.append("-c");
    args.append(ByteString(command.bytes()));

    auto stdin_fds = TRY(Core::System::pipe2(O_CLOEXEC));
    ArmedScopeGuard close_stdin_read_fd { [&] { MUST(Core::System::close(stdin_fds[0])); } };
    auto stdin_read = TRY(Core::File::adopt_fd(stdin_fds[0], Core::File::OpenMode::Read));
    close_stdin_read_fd.disarm();
    ArmedScopeGuard close_stdin_write_fd { [&] { MUST(Core::System::close(stdin_fds[1])); } };
    auto stdin_write = TRY(Core::File::adopt_fd(stdin_fds[1], Core::File::OpenMode::Write));
    close_stdin_write_fd.disarm();

    auto stdout_fds = TRY(Core::System::pipe2(O_CLOEXEC));
    ArmedScopeGuard close_stdout_read_fd { [&] { MUST(Core::System::close(stdout_fds[0])); } };
    auto stdout_read = TRY(Core::File::adopt_fd(stdout_fds[0], Core::File::OpenMode::Read));
    close_stdout_read_fd.disarm();
    ArmedScopeGuard close_stdout_write_fd { [&] { MUST(Core::System::close(stdout_fds[1])); } };
    auto stdout_write = TRY(Core::File::adopt_fd(stdout_fds[1], Core::File::OpenMode::Write));
    close_stdout_write_fd.disarm();

    auto stderr_fds = TRY(Core::System::pipe2(O_CLOEXEC));
    ArmedScopeGuard close_stderr_read_fd { [&] { MUST(Core::System::close(stderr_fds[0])); } };
    auto stderr_read = TRY(Core::File::adopt_fd(stderr_fds[0], Core::File::OpenMode::Read));
    close_stderr_read_fd.disarm();
    ArmedScopeGuard close_stderr_write_fd { [&] { MUST(Core::System::close(stderr_fds[1])); } };
    auto stderr_write = TRY(Core::File::adopt_fd(stderr_fds[1], Core::File::OpenMode::Write));
    close_stderr_write_fd.disarm();

    TRY(stdin_write->set_blocking(false));
    TRY(stdout_read->set_blocking(false));
    TRY(stderr_read->set_blocking(false));

    auto child = TRY(Core::Process::spawn({
        .executable = shell,
        .arguments = args,
        .create_process_group = true,
        .file_actions = {
            Core::FileAction::DuplicateFile { stdin_read->fd(), STDIN_FILENO },
            Core::FileAction::DuplicateFile { stdout_write->fd(), STDOUT_FILENO },
            Core::FileAction::DuplicateFile { stderr_write->fd(), STDERR_FILENO },
        },
    }));

    stdin_read->close();
    stdout_write->close();
    stderr_write->close();

    session->system = ExecData::create(move(child), move(stdin_write), move(stdout_read), move(stderr_read));

    TRY(send_channel_success_message(session));

    auto weak_this = make_weak_ptr();
    auto send_stdout = [weak_this](Session& session, ReadonlyBytes data) -> ErrorOr<void> {
        auto* client = weak_this.ptr();
        if (!client)
            return Error::from_string_literal("SSH client has shut down");
        return client->send_channel_data(session, data);
    };
    auto send_stderr = [weak_this](Session& session, ReadonlyBytes data) -> ErrorOr<void> {
        auto* client = weak_this.ptr();
        if (!client)
            return Error::from_string_literal("SSH client has shut down");
        return client->send_channel_extended_data(session, data);
    };

    Core::EventLoop::adopt_coroutine(async_stream_std_data(session, [](ExecData& exec_data) -> Core::File& { return *exec_data.stdout_; }, move(send_stdout)));
    Core::EventLoop::adopt_coroutine(async_stream_std_data(session, [](ExecData& exec_data) -> Core::File& { return *exec_data.stderr_; }, move(send_stderr)));

    return {};
}

template<typename F, typename F2>
Coroutine<void> SSHClient::async_stream_std_data(NonnullRefPtr<Session> session, F file_extractor, F2 sender)
{
    auto weak_this = make_weak_ptr();
    auto maybe_error = co_await [&]() -> Coroutine<ErrorOr<void>> {
        while (true) {
            if (session->is_closed)
                co_return {};

            auto output_buffer = CO_TRY(ByteBuffer::create_uninitialized(PAGE_SIZE));
            auto maybe_output = co_await file_extractor(session->system.template get<ExecData>()).async_read_some(output_buffer);
            if (weak_this.is_null() || session->is_closed)
                co_return {};
            auto output = CO_TRY(move(maybe_output));

            if (output.is_empty())
                break;

            CO_TRY(sender(session, output));
            while (session->has_pending_outgoing_channel_data()) {
                auto maybe_window = co_await session->wait_for_remote_window();
                if (weak_this.is_null() || session->is_closed)
                    co_return {};
                CO_TRY(move(maybe_window));
                CO_TRY(flush_pending_channel_data(*session));
            }
        }

        if (weak_this.is_null() || session->is_closed)
            co_return {};
        CO_TRY(close_session_if_needed(session));

        co_return {};
    }();

    if (weak_this.is_null() || session->is_closed)
        co_return;

    if (maybe_error.is_error()) {
        auto error = maybe_error.release_error();
        dbgln("Unable to stream process output: {}", error);
        disconnect(move(error));
    }
    co_return;
}

// 6.5.  Starting a Shell or a Command
// https://datatracker.ietf.org/doc/html/rfc4254#section-6.5
ErrorOr<void> SSHClient::handle_channel_subsystem(Session& session, GenericMessage& message)
{
    if (session.is_closed || !session.system.has<Empty>())
        return Error::from_string_literal("A request is already active on this channel");

    auto subsystem = TRY(decode_string(message.payload));
    if (message.payload.remaining() != 0)
        return Error::from_string_literal("Trailing data in subsystem request");
    dbgln_if(SSH_DEBUG, "Subsystem requested: {:s}", subsystem.bytes());

    if (subsystem == "sftp"sv.bytes()) {
        // 2. Use with the SSH Connection Protocol
        // https://datatracker.ietf.org/doc/html/draft-ietf-secsh-filexfer-02#section-2

        session.system = SFTP::Server { [this, &session](ReadonlyBytes bytes) -> ErrorOr<void> {
            return send_channel_data(session, bytes);
        } };
        return TRY(send_channel_success_message(session));
    }

    return Error::from_string_literal("Unsupported subsystem");
}

ErrorOr<void> SSHClient::send_channel_success_message(Session const& session)
{
    AllocatingMemoryStream stream;
    TRY(stream.write_value(MessageID::CHANNEL_SUCCESS));
    TRY(stream.write_value<NetworkOrdered<u32>>(session.local_channel_id));
    TRY(write_packet(TRY(stream.read_until_eof())));
    return {};
}

ErrorOr<void> SSHClient::send_channel_data(Session& session, ReadonlyBytes data)
{
    return queue_channel_data(session, data, false);
}

ErrorOr<void> SSHClient::send_channel_extended_data(Session& session, ReadonlyBytes data)
{
    return queue_channel_data(session, data, true);
}

ErrorOr<void> SSHClient::queue_channel_data(Session& session, ReadonlyBytes data, bool is_extended_data)
{
    if (data.is_empty())
        return {};
    if (session.is_closed)
        return Error::from_string_literal("Cannot send data on a closed channel");
    if (session.pending_outgoing_channel_data.size() >= MAXIMUM_QUEUED_CHANNEL_DATA_MESSAGES
        || session.pending_outgoing_channel_data_bytes > MAXIMUM_QUEUED_CHANNEL_DATA_SIZE
        || data.size() > MAXIMUM_QUEUED_CHANNEL_DATA_SIZE - session.pending_outgoing_channel_data_bytes)
        return Error::from_string_literal("Pending channel data exceeds its limit");

    auto pending_bytes = TRY(ByteBuffer::copy(data));
    TRY(session.pending_outgoing_channel_data.try_empend(move(pending_bytes), 0, is_extended_data));
    session.pending_outgoing_channel_data_bytes += data.size();

    return flush_pending_channel_data(session);
}

ErrorOr<void> SSHClient::flush_pending_channel_data(Session& session)
{
    while (!session.pending_outgoing_channel_data.is_empty() && session.remote_window_size > 0) {
        auto& pending = session.pending_outgoing_channel_data.first();
        VERIFY(pending.offset <= pending.bytes.size());
        auto remaining_data = pending.bytes.bytes().slice(pending.offset);

        auto maximum_part_size = min(session.maximum_packet_size, SERVER_MAXIMUM_CHANNEL_PACKET_SIZE);
        maximum_part_size = min(maximum_part_size, session.remote_window_size);
        if (maximum_part_size == 0)
            return Error::from_string_literal("Channel packet size is zero");

        auto part = remaining_data.trim(maximum_part_size);
        TRY(send_channel_data_packet(session, part, pending.is_extended_data));
        session.consume_remote_window(part.size());
        pending.offset += part.size();

        if (pending.offset == pending.bytes.size()) {
            session.pending_outgoing_channel_data_bytes -= pending.bytes.size();
            session.pending_outgoing_channel_data.remove(0);
        }
    }

    return {};
}

ErrorOr<void> SSHClient::send_channel_data_packet(Session const& session, ReadonlyBytes data, bool is_extended_data)
{
    static constexpr u32 EXTENDED_DATA_STDERR = 1;

    AllocatingMemoryStream stream;
    if (is_extended_data) {
        TRY(stream.write_value(MessageID::CHANNEL_EXTENDED_DATA));
        TRY(stream.write_value<NetworkOrdered<u32>>(session.local_channel_id));
        TRY(stream.write_value<NetworkOrdered<u32>>(EXTENDED_DATA_STDERR));
    } else {
        TRY(stream.write_value(MessageID::CHANNEL_DATA));
        TRY(stream.write_value<NetworkOrdered<u32>>(session.local_channel_id));
    }
    TRY(encode_string(stream, data));
    TRY(write_packet(TRY(stream.read_until_eof())));
    return {};
}

ErrorOr<void> SSHClient::handle_channel_eof(GenericMessage& message)
{
    auto recipient_channel_id = TRY(message.payload.read_value<NetworkOrdered<u32>>());
    auto& session = *TRY(find_session(recipient_channel_id));

    // "When a party will no longer send more data to a channel, it SHOULD send SSH_MSG_CHANNEL_EOF."
    // "No explicit response is sent to this message.  However, the application may send EOF to
    // whatever is at the other end of the channel.  Note that the channel remains open after this
    // message, and more data may still be sent in the other direction.  This message does not
    // consume window space and can be sent even if no window space is available."

    session.has_received_eof = true;

    session.system.visit(
        [&](auto& system) { system.handle_channel_eof(session); },
        [](Empty) {});

    TRY(close_session_if_needed(session));

    return {};
}

// 5.2.  Data Transfer
// https://datatracker.ietf.org/doc/html/rfc4254#section-5.2
ErrorOr<void> SSHClient::handle_channel_window_adjust_message(GenericMessage& message)
{
    auto recipient_channel_id = TRY(message.payload.read_value<NetworkOrdered<u32>>());
    auto bytes_to_add = TRY(message.payload.read_value<NetworkOrdered<u32>>());
    if (message.payload.remaining() != 0)
        return Error::from_string_literal("Trailing data in channel window adjustment");

    auto& session = *TRY(find_session(recipient_channel_id));
    if (session.is_closed)
        return Error::from_string_literal("Window adjustment for a closed channel");

    TRY(session.adjust_remote_window(bytes_to_add));
    TRY(flush_pending_channel_data(session));

    if (!session.has_pending_outgoing_channel_data())
        TRY(close_session_if_needed(session));

    if (bytes_to_add > 0)
        session.notify_remote_window_waiters();
    return {};
}

ErrorOr<void> SSHClient::adjust_channel_window_if_needed(Session& session)
{
    if (session.total_received_bytes > session.local_window_size
        || session.total_consumed_bytes > session.total_received_bytes
        || session.total_window_adjusted_bytes > session.total_consumed_bytes)
        return Error::from_string_literal("Invalid channel window accounting");

    auto available_window = session.local_window_size - session.total_received_bytes;
    if (available_window > session.initial_window_size / 2)
        return {};

    auto consumed_since_last_adjustment = session.total_consumed_bytes - session.total_window_adjusted_bytes;
    if (consumed_since_last_adjustment == 0)
        return {};

    auto bytes_to_add = static_cast<u32>(min(consumed_since_last_adjustment, static_cast<u64>(NumericLimits<u32>::max())));
    return send_channel_window_adjust_message(session, bytes_to_add);
}

// 5.2.  Data Transfer
// https://datatracker.ietf.org/doc/html/rfc4254#section-5.2
ErrorOr<void> SSHClient::send_channel_window_adjust_message(Session& session, u32 bytes_to_add)
{
    if (bytes_to_add == 0)
        return {};

    if (session.local_window_size > NumericLimits<u64>::max() - bytes_to_add
        || session.total_window_adjusted_bytes > NumericLimits<u64>::max() - bytes_to_add)
        return Error::from_string_literal("Channel window accounting overflow");

    AllocatingMemoryStream stream;
    TRY(stream.write_value(MessageID::CHANNEL_WINDOW_ADJUST));
    TRY(stream.write_value<NetworkOrdered<u32>>(session.sender_channel_id));
    TRY(stream.write_value<NetworkOrdered<u32>>(bytes_to_add));
    TRY(write_packet(TRY(stream.read_until_eof())));

    session.local_window_size += bytes_to_add;
    session.total_window_adjusted_bytes += bytes_to_add;
    return {};
}

ErrorOr<void> SSHClient::handle_channel_close(GenericMessage& message)
{
    auto recipient_channel_id = TRY(message.payload.read_value<NetworkOrdered<u32>>());
    auto& session = *TRY(find_session(recipient_channel_id));

    if (!session.is_closed)
        TRY(send_channel_close(session));

    m_sessions.remove_first_matching([&](Session const& s) { return s.local_channel_id == session.local_channel_id; });

    return {};
}

ErrorOr<void> SSHClient::send_channel_close(Session& session)
{
    if (session.system.has<ExecData>())
        session.system.get<ExecData>().terminate();

    session.is_closed = true;
    session.pending_outgoing_channel_data.clear();
    session.pending_outgoing_channel_data_bytes = 0;
    session.notify_remote_window_waiters();

    AllocatingMemoryStream stream;
    TRY(stream.write_value(MessageID::CHANNEL_CLOSE));
    TRY(stream.write_value<NetworkOrdered<u32>>(session.local_channel_id));
    TRY(write_packet(TRY(stream.read_until_eof())));
    return {};
}

// 6.10.  Returning Exit Status
// https://datatracker.ietf.org/doc/html/rfc4254#section-6.10
ErrorOr<void> SSHClient::send_exit_status(Session const& session)
{
    AllocatingMemoryStream stream;
    TRY(stream.write_value(MessageID::CHANNEL_REQUEST));
    TRY(stream.write_value<NetworkOrdered<u32>>(session.sender_channel_id));
    TRY(encode_string(stream, "exit-status"sv));
    TRY(stream.write_value(false));
    TRY(stream.write_value<NetworkOrdered<u32>>(session.exit_status()));

    TRY(write_packet(TRY(stream.read_until_eof())));
    return {};
}

ErrorOr<void> SSHClient::manage_child_death()
{
    while (true) {
        // Observe the exit without reaping first so the process-group ID remains
        // pinned by its zombie leader while any background descendants are killed.
        auto maybe_result = Core::System::waitid(P_ALL, 0, WEXITED | WNOHANG | WNOWAIT);

        if (maybe_result.is_error()) {
            auto error = maybe_result.release_error();
            if (error.code() == EINTR)
                continue;
            if (error.code() == ECHILD)
                return {};
            return error;
        }

        auto result = maybe_result.release_value();
        if (result.si_pid == 0)
            return {};

        Session* exited_session = nullptr;
        for (auto& session : m_sessions) {
            if (!session->system.has<ExecData>())
                continue;

            auto& exec_data = session->system.get<ExecData>();
            if (exec_data.child.pid() != result.si_pid)
                continue;

            // A command may leave background descendants behind after its shell
            // exits. Terminate the whole group before reaping the leader, which
            // prevents both orphaned commands and a later PID-reuse race.
            auto maybe_kill_error = Core::System::killpg(result.si_pid, SIGKILL);
            if (maybe_kill_error.is_error()) {
                auto error = maybe_kill_error.release_error();
                if (!error.is_errno() || error.code() != ESRCH)
                    dbgln("Unable to terminate SSH child process group: {}", error);
            }

            bool exited_with_code_0 = result.si_code == CLD_EXITED && result.si_status == 0;
            // FIXME: Send the actual exit status.
            exec_data.exit_status = exited_with_code_0 ? 0 : -1;
            exited_session = session.ptr();
            break;
        }

        // waitid(WNOWAIT) did not reap the child; do so now to avoid retaining
        // zombies. Ignore EINTR and retry the same PID.
        while (true) {
            auto maybe_reaped = Core::System::waitpid(result.si_pid);
            if (!maybe_reaped.is_error())
                break;

            auto error = maybe_reaped.release_error();
            if (error.code() == EINTR)
                continue;
            if (error.code() == ECHILD)
                break;
            return error;
        }

        if (exited_session)
            TRY(close_session_if_needed(*exited_session));
    }
}

ErrorOr<void> SSHClient::close_session_if_needed(Session& session)
{
    if (session.is_closed || session.has_pending_outgoing_channel_data())
        return {};

    if (!session.is_ready_to_be_closed())
        return {};

    TRY(send_exit_status(session));

    TRY(send_channel_close(session));
    return {};
}

void SSHClient::disconnect(Error error)
{
    dbgln("Disconnecting: {}", error);
    m_disconnect();
}

} // SSHServer
