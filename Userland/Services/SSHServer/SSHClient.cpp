/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SSHClient.h"

#include <AK/ByteBuffer.h>
#include <AK/Format.h>
#include <LibCore/Socket.h>
#include <LibSSH/IdentificationString.h>

namespace SSH::Server {

ErrorOr<void> SSHClient::handle_data(ByteBuffer& data)
{

    switch (m_state) {
    case State::Constructed:
        return handle_protocol_version(data);
    case State::ProtocolVersionExchanged:
        return Error::from_string_literal("Draw the rest of the owl");
    }
    VERIFY_NOT_REACHED();
}

// 4.2.  Protocol Version Exchange
// https://datatracker.ietf.org/doc/html/rfc4253#section-4.2

ErrorOr<void> SSHClient::handle_protocol_version(ByteBuffer& data)
{
    TRY(validate_identification_string(data));

    data.clear();

    TRY(m_tcp_socket.write_until_depleted(PROTOCOL_STRING));

    m_state = State::ProtocolVersionExchanged;

    return {};
}

} // SSHServer
