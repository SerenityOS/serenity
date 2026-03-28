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
