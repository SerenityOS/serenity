/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Server.h"
#include <AK/Debug.h>
#include <AK/Endian.h>
#include <LibSSH/Session.h>

namespace SSH::SFTP {

Coroutine<ErrorOr<void>> Server::handle_channel_data(Session& session)
{
    // FIXME: For the moment everything is done sequentially, but we should
    //        probably make most IO related syscall asynchronous.

    FixedMemoryStream stream { session.channel_data.data() };
    ScopeGuard commit_read_bytes { [&] { session.channel_data.dequeue(stream.offset()); } };
    switch (m_state) {
    case State::Constructed:
        co_return handle_init_message(stream);
    case State::Initialized:
        co_return handle_packet(stream);
    }
    VERIFY_NOT_REACHED();
}

void Server::handle_channel_eof(Session const&)
{
    // There is nothing to do here.
}

// 4. Protocol Initialization
// https://datatracker.ietf.org/doc/html/draft-ietf-secsh-filexfer-02#section-4
ErrorOr<void> Server::handle_init_message(FixedMemoryStream& stream)
{
    TRY(read_header(stream));
    u32 version = TRY(stream.read_value<NetworkOrdered<u32>>());

    if (version != 3)
        return Error::from_string_literal("Invalid SFTP version");

    // FIXME: Read extension data.

    TRY(send_version_message());

    m_state = State::Initialized;

    return {};
}

// 4. Protocol Initialization
// https://datatracker.ietf.org/doc/html/draft-ietf-secsh-filexfer-02#section-4
ErrorOr<void> Server::send_version_message()
{
    AllocatingMemoryStream stream;
    TRY(stream.write_value(FXPMessageID::VERSION));
    // Protocol version
    TRY(stream.write_value<NetworkOrdered<u32>>(3));

    auto packet = TRY(stream.read_until_eof());

    TRY(write_packet(packet));
    return {};
}

ErrorOr<void> Server::handle_packet(FixedMemoryStream& stream)
{
    auto type = TRY(read_header(stream));

    switch (type) {
    default:
        dbgln_if(SSH_DEBUG, "Received packet with type: {}", to_underlying(type));
        return Error::from_string_literal("Unknown packet type");
    }
}

} // SSH::SFTP
