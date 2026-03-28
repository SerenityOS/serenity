/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Peer.h"
#include <AK/Debug.h>
#include <AK/Endian.h>

namespace SSH::SFTP {

// 3. General Packet Format
// https://datatracker.ietf.org/doc/html/draft-ietf-secsh-filexfer-02#section-3
ErrorOr<FXPMessageID> Peer::read_header(FixedMemoryStream& stream)
{
    u32 length = TRY(stream.read_value<NetworkOrdered<u32>>());
    if (length != stream.remaining())
        return Error::from_string_literal("Invalid packet size");

    auto type = TRY(stream.read_value<FXPMessageID>());
    return type;
}

// 3. General Packet Format
// https://datatracker.ietf.org/doc/html/draft-ietf-secsh-filexfer-02#section-3
ErrorOr<void> Peer::write_packet(ReadonlyBytes bytes)
{
    AllocatingMemoryStream stream;
    TRY(stream.write_value<NetworkOrdered<u32>>(bytes.size()));
    TRY(stream.write_until_depleted(bytes));
    auto packet = TRY(stream.read_until_eof());
    TRY(m_send_packet(packet));
    return {};
}

} // SSH::SFTP
