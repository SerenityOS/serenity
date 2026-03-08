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
        break;
    case State::KeyExchanged:
        return Error::from_string_literal("Draw the rest of the owl");
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

    m_state = State::KeyExchanged;
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

    // FIXME: Abort if shared_point is not valid (at least when it's all zero, maybe there are other cases too).

    // Generate and sign exchange hash.
    auto hash = TRY(m_key_exchange_data.compute_sha_256());

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

} // SSHServer
