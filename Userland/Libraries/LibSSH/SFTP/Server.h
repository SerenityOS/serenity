/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibSSH/Forward.h>
#include <LibSSH/SFTP/Peer.h>

namespace SSH::SFTP {

class Server : public Peer {
public:
    Server(Function<ErrorOr<void>(ReadonlyBytes)> send_packet)
        : Peer(move(send_packet))
    {
    }

    Coroutine<ErrorOr<void>> handle_channel_data(Session&);
    void handle_channel_eof(Session const&);

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
