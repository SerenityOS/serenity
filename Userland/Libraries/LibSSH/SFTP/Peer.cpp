/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Peer.h"
#include <AK/Debug.h>
#include <AK/Endian.h>
#include <AK/EnumBits.h>

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

enum class AttributesFlags : u32 {
    SIZE = 0x00000001,
    UIDGID = 0x00000002,
    PERMISSIONS = 0x00000004,
    ACMODTIME = 0x00000008,
    EXTENDED = 0x80000000,
};

AK_ENUM_BITWISE_OPERATORS(AttributesFlags)

ErrorOr<Attributes> Attributes::from_stream(FixedMemoryStream& stream)
{
    u32 raw_flags = TRY(stream.read_value<NetworkOrdered<u32>>());
    auto flags = static_cast<AttributesFlags>(raw_flags);

    SSH::SFTP::Attributes attributes;

    if (has_flag(flags, AttributesFlags::SIZE))
        attributes.size = TRY(stream.read_value<NetworkOrdered<u64>>());
    if (has_flag(flags, AttributesFlags::UIDGID)) {
        attributes.uid = TRY(stream.read_value<NetworkOrdered<u32>>());
        attributes.gid = TRY(stream.read_value<NetworkOrdered<u32>>());
    }
    if (has_flag(flags, AttributesFlags::PERMISSIONS))
        attributes.mode = TRY(stream.read_value<NetworkOrdered<u32>>());
    if (has_flag(flags, AttributesFlags::ACMODTIME)) {
        attributes.atim = TRY(stream.read_value<NetworkOrdered<u32>>());
        attributes.mtim = TRY(stream.read_value<NetworkOrdered<u32>>());
    }

    if (has_flag(flags, AttributesFlags::EXTENDED))
        return Error::from_string_literal("Unsupported attribute flag: extended");

    return attributes;
}

ErrorOr<void> Attributes::encode(AllocatingMemoryStream& stream)
{
    AttributesFlags flags {};
    if (size.has_value())
        flags |= AttributesFlags::SIZE;
    if (uid.has_value() && gid.has_value())
        flags |= AttributesFlags::UIDGID;
    if (mode.has_value())
        flags |= AttributesFlags::PERMISSIONS;
    if (atim.has_value() && mtim.has_value())
        flags |= AttributesFlags::ACMODTIME;

    TRY(stream.write_value<NetworkOrdered<u32>>(to_underlying(flags)));

    if (size.has_value())
        TRY(stream.write_value<NetworkOrdered<u64>>(*size));

    if (uid.has_value() && gid.has_value()) {
        TRY(stream.write_value<NetworkOrdered<u32>>(*uid));
        TRY(stream.write_value<NetworkOrdered<u32>>(*gid));
    }

    if (mode.has_value())
        TRY(stream.write_value<NetworkOrdered<u32>>(*mode));

    if (atim.has_value() && mtim.has_value()) {
        TRY(stream.write_value<NetworkOrdered<u32>>(*atim));
        TRY(stream.write_value<NetworkOrdered<u32>>(*mtim));
    }

    return {};
}

} // SSH::SFTP
