/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibSSH/SFTP/Peer.h>
#include <LibSSH/SFTP/Server.h>

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

    FixedMemoryStream stream { g_initialization_packet.bytes() };
    if (server.handle_data(stream).is_error())
        FAIL("Unable to perform handshake with server");

    return server;
}
