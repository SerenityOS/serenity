/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibCore/File.h>
#include <LibCrypto/Hash/HashFunction.h>
#include <LibSSH/SFTP/Peer.h>
#include <sys/stat.h>

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

    struct File {
        NonnullOwnPtr<Core::File> file;
        Crypto::Hash::Digest<128> handle;
    };

    ErrorOr<void> handle_init_message(FixedMemoryStream& stream);
    ErrorOr<void> send_version_message();

    ErrorOr<void> handle_packet(FixedMemoryStream& stream);

    enum class StatType : u8 {
        Normal,
        LStat,
    };
    ErrorOr<void> handle_stat(FixedMemoryStream& stream, StatType);
    ErrorOr<void> send_file_attribute_message(u32 id, struct ::stat const&);

    ErrorOr<void> handle_open(FixedMemoryStream& stream);
    ErrorOr<void> send_file_handle(u32, File const&);

    ErrorOr<void> handle_read(FixedMemoryStream& stream);
    ErrorOr<void> send_data(u32, ReadonlyBytes);
    ErrorOr<void> send_eof(u32);

    ErrorOr<File*> find_file(ReadonlyBytes handle);

    State m_state { State::Constructed };

    Vector<File> m_open_files {};
};

} // SSH::SFTP
