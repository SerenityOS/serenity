/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/MemoryStream.h>
#include <sys/stat.h>

namespace SSH::SFTP {

// 3. General Packet Format
// https://datatracker.ietf.org/doc/html/draft-ietf-secsh-filexfer-02#section-3
enum class FXPMessageID : u8 {
    INIT = 1,
    VERSION = 2,
    OPEN = 3,
    CLOSE = 4,
    READ = 5,
    WRITE = 6,
    LSTAT = 7,
    FSTAT = 8,
    SETSTAT = 9,
    FSETSTAT = 10,
    OPENDIR = 11,
    READDIR = 12,
    REMOVE = 13,
    MKDIR = 14,
    RMDIR = 15,
    REALPATH = 16,
    STAT = 17,
    RENAME = 18,
    READLINK = 19,
    SYMLINK = 20,
    STATUS = 101,
    HANDLE = 102,
    DATA = 103,
    NAME = 104,
    ATTRS = 105,
    EXTENDED = 200,
    EXTENDED_REPLY = 201,
};

// 7. Responses from the Server to the Client
// https://datatracker.ietf.org/doc/html/draft-ietf-secsh-filexfer-02#section-7
enum class FXStatus : u8 {
    OK = 0,
    FX_EOF = 1, // Renamed EOF => FX_EOF to avoid collision with the EOF macro.
    NO_SUCH_FILE = 2,
    PERMISSION_DENIED = 3,
    FAILURE = 4,
    BAD_MESSAGE = 5,
    NO_CONNECTION = 6,
    CONNECTION_LOST = 7,
    OP_UNSUPPORTED = 8,
};

// 5. File Attributes
// https://datatracker.ietf.org/doc/html/draft-ietf-secsh-filexfer-02#section-5
struct Attributes {
    Optional<u64> size;
    Optional<u32> uid;
    Optional<u32> gid;
    Optional<u32> mode;
    Optional<u32> atim;
    Optional<u32> mtim;

    bool operator==(Attributes const&) const = default;

    static Attributes from_stat(struct ::stat const& s)
    {
        return {
            .size = s.st_size,
            .uid = s.st_uid,
            .gid = s.st_gid,
            .mode = s.st_mode,
            .atim = s.st_atime,
            .mtim = s.st_mtime,
        };
    }
};

// This class implement the SFTP protocol v3. Newer version exist, but they
// never really took off, this is the same version that openssh implements.
// https://datatracker.ietf.org/doc/html/draft-ietf-secsh-filexfer-02
class Peer {
public:
    Peer(Function<ErrorOr<void>(ReadonlyBytes)> send_packet)
        : m_send_packet(move(send_packet))
    {
    }

protected:
    ErrorOr<FXPMessageID> read_header(FixedMemoryStream& stream);
    ErrorOr<void> write_packet(ReadonlyBytes bytes);

private:
    Function<ErrorOr<void>(ReadonlyBytes)> m_send_packet;
};

} // SFTP::SSH
