/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Server.h"
#include <AK/Debug.h>
#include <AK/Endian.h>
#include <LibCore/System.h>
#include <LibSSH/DataTypes.h>

namespace SSH::SFTP {

ErrorOr<void> Server::handle_data(FixedMemoryStream& stream)
{
    switch (m_state) {
    case State::Constructed:
        return handle_init_message(stream);
    case State::Initialized:
        return handle_packet(stream);
    }
    VERIFY_NOT_REACHED();
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
    case FXPMessageID::STAT:
        return handle_stat(stream, StatType::Normal);
    case FXPMessageID::LSTAT:
        return handle_stat(stream, StatType::LStat);
    default:
        dbgln_if(SSH_DEBUG, "Received packet with type: {}", to_underlying(type));
        return Error::from_string_literal("Unknown packet type");
    }
}

// 6.8 Retrieving File Attributes
// https://datatracker.ietf.org/doc/html/draft-ietf-secsh-filexfer-02#section-6.8
ErrorOr<void> Server::handle_stat(FixedMemoryStream& stream, StatType type)
{
    u32 id = TRY(stream.read_value<NetworkOrdered<u32>>());
    auto path = TRY(decode_string(stream));

    // "The server responds to this request with either SSH_FXP_ATTRS or SSH_FXP_STATUS."
    auto maybe_stat = [&]() {
        switch (type) {
        case StatType::LStat:
            return Core::System::lstat(path);
        case StatType::Normal:
            return Core::System::stat(path);
        }
        VERIFY_NOT_REACHED();
    }();

    if (maybe_stat.is_error()) {
        // FIXME: Send SSH_FXP_STATUS.
        return maybe_stat.release_error();
    }

    auto stat = maybe_stat.release_value();
    TRY(send_file_attribute_message(id, stat));
    return {};
}

// 5. File Attributes
// https://datatracker.ietf.org/doc/html/draft-ietf-secsh-filexfer-02#section-5

#define SSH_FILEXFER_ATTR_SIZE 0x00000001
#define SSH_FILEXFER_ATTR_UIDGID 0x00000002
#define SSH_FILEXFER_ATTR_PERMISSIONS 0x00000004
#define SSH_FILEXFER_ATTR_ACMODTIME 0x00000008
#define SSH_FILEXFER_ATTR_EXTENDED 0x80000000

static ErrorOr<void> encode_file_attributes(AllocatingMemoryStream& stream, struct ::stat const& s)
{
    TRY(stream.write_value<NetworkOrdered<u32>>(
        SSH_FILEXFER_ATTR_SIZE
        | SSH_FILEXFER_ATTR_UIDGID
        | SSH_FILEXFER_ATTR_PERMISSIONS
        | SSH_FILEXFER_ATTR_ACMODTIME));

    TRY(stream.write_value<NetworkOrdered<u64>>(s.st_size));

    TRY(stream.write_value<NetworkOrdered<u32>>(s.st_uid));
    TRY(stream.write_value<NetworkOrdered<u32>>(s.st_gid));

    TRY(stream.write_value<NetworkOrdered<u32>>(s.st_mode));

    TRY(stream.write_value<NetworkOrdered<u32>>(s.st_atime));
    TRY(stream.write_value<NetworkOrdered<u32>>(s.st_mtime));

    return {};
}

// 7. Responses from the Server to the Client
// https://datatracker.ietf.org/doc/html/draft-ietf-secsh-filexfer-02#section-7
ErrorOr<void> Server::send_file_attribute_message(u32 id, struct stat const& s)
{
    AllocatingMemoryStream stream;
    TRY(stream.write_value(FXPMessageID::ATTRS));
    TRY(stream.write_value<NetworkOrdered<u32>>(id));
    TRY(encode_file_attributes(stream, s));

    auto packet = TRY(stream.read_until_eof());
    TRY(write_packet(packet));
    return {};
}

} // SSH::SFTP
