/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibSSH/SFTP/Peer.h>

namespace SSH::SFTP {

class Server : public Peer {
public:
    Server(Function<ErrorOr<void>(ReadonlyBytes)> send_packet)
        : Peer(move(send_packet))
    {
    }

    ErrorOr<void> handle_data(FixedMemoryStream& stream);

private:
    enum class State : u8 {
        Constructed,
        Initialized,
    };

    ErrorOr<void> handle_init_message(FixedMemoryStream& stream);
    ErrorOr<void> send_version_message();

    State m_state { State::Constructed };
};

} // SSH::SFTP
