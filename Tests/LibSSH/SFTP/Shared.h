/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/EventLoop.h>
#include <LibSSH/DataTypes.h>
#include <LibSSH/SFTP/Peer.h>
#include <LibSSH/SFTP/Server.h>
#include <LibSSH/Session.h>

class PeerMock : public SSH::SFTP::Peer {
public:
    PeerMock(Function<ErrorOr<void>(ReadonlyBytes)> send_packet)
        : SSH::SFTP::Peer(move(send_packet))
    {
    }

    using SSH::SFTP::Peer::read_header;
    using SSH::SFTP::Peer::write_packet;
};

inline constexpr auto g_initialization_packet = "\x00\x00\x00\x05\x01\x00\x00\x00\x03"sv;
inline constexpr auto g_version_packet = "\x00\x00\x00\x05\x02\x00\x00\x00\x03"sv;

inline ErrorOr<void> server_process_data(SSH::SFTP::Server& server, ReadonlyBytes bytes)
{
    auto session = MUST(SSH::Session::create(0, 0, 0));
    session->channel_data.append(bytes);

    return Core::run_async_in_new_event_loop([&]() { return server.handle_channel_data(session); });
}

struct Message {
    SSH::SFTP::FXPMessageID type;
    ReadonlyBytes payload;
};

inline SSH::SFTP::Server make_initialized_server(Function<ErrorOr<void>(Message)> callback)
{
    bool was_called { false };
    SSH::SFTP::Server server([=, callback = move(callback)](ReadonlyBytes bytes) mutable -> ErrorOr<void> {
        if (was_called) {
            PeerMock peer { [](ReadonlyBytes) -> ErrorOr<void> { VERIFY_NOT_REACHED(); } };
            FixedMemoryStream stream { bytes };
            auto type = TRY(peer.read_header(stream));
            return callback({ type, bytes.slice(bytes.size() - stream.remaining()) });
        }
        EXPECT_EQ(bytes, g_version_packet.bytes());
        was_called = true;
        return {};
    });

    if (server_process_data(server, g_initialization_packet.bytes()).is_error())
        FAIL("Unable to perform handshake with server");

    return server;
}

inline ErrorOr<ByteBuffer> make_open_packet(StringView path, u32 request_id)
{
    AllocatingMemoryStream inner;

    TRY(inner.write_value(SSH::SFTP::FXPMessageID::OPEN));
    TRY(inner.write_value<NetworkOrdered<u32>>(request_id));
    TRY(SSH::encode_string(inner, path));
    TRY(inner.write_value<NetworkOrdered<u32>>(1 | 2)); // SSH_FXF_READ | SSH_FXF_WRITE
    TRY(inner.write_value<NetworkOrdered<u32>>(0));     // No attributes.

    auto payload = TRY(inner.read_until_eof());

    AllocatingMemoryStream packet;
    TRY(packet.write_value<NetworkOrdered<u32>>(payload.size()));
    TRY(packet.write_until_depleted(payload));

    return TRY(packet.read_until_eof());
}

inline ErrorOr<void> check_status_message(Message const& message, SSH::SFTP::FXStatus expected_status, u32 request_id = 42)
{
    EXPECT_EQ(message.type, SSH::SFTP::FXPMessageID::STATUS);
    FixedMemoryStream stream { message.payload };
    EXPECT_EQ(TRY(stream.read_value<NetworkOrdered<u32>>()), request_id);
    u32 raw_status_code = TRY(stream.read_value<NetworkOrdered<u32>>());
    auto status_code = static_cast<SSH::SFTP::FXStatus>(raw_status_code);
    EXPECT_EQ(status_code, expected_status);

    // `error message` and `language tag`
    EXPECT(!SSH::decode_string(stream).is_error());
    EXPECT(!SSH::decode_string(stream).is_error());

    EXPECT(stream.remaining() == 0);
    return {};
}
