/*
 * Copyright (c) 2023, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Endian.h>
#include <AK/ScopeGuard.h>
#include <AK/String.h>
#include <AK/Time.h>
#include <LibCore/File.h>
#include <LibIPC/Stub.h>
#include <LibIPC/TrafficDump.h>
#include <stdlib.h>

namespace IPC {

Optional<TrafficDump> TrafficDump::create_if_requested(Stub const& stub)
{
    if (!getenv("DUMP_LIBIPC_TRAFFIC")) {
        return {};
    }
    // This method is being called from the constructor of `ConnectionBase`.
    // However, most classes haphazardly just derive from *all* IPC classes.
    // Consider LibConfig's Config::Client. In its constructor, it passes `*this` as `ClientStub&` to
    // ConnectionToServer, which eventually calls the constructor of `ConnectionBase`, which in turn calls us.
    // However, since the constructor of `Config::Client` hasn't completed yet, this means that the vtables
    // aren't complete yet, and instead are in a weird intermediate state.
    // Therefore, we don't access any methods just yet, and instead do the initialization lazily.
    return TrafficDump(&stub);
}

static Array PCAP_MAGIC_HEADER = {
    0xA1B2C3D4, // Magic value to indicate pcap file format, version, endianess, and timestamp format.
    0x00400020, // Version
    0x00000000, // Pointless timestamp (ignored anyway)
    0x00000000, // Second pointless timestamp (ignored anyway)
    0x00400000, // "snaplen", I guess the largest possible packet size? FIXME: Clarify
    0x000000a0, // "linktype", let's use LINKTYPE_USER13=0xa0 to avoid collisions.
};

ErrorOr<void> TrafficDump::lazy_init_if_necessary()
{
    if (m_file.has<NonnullOwnPtr<Core::File>>())
        return {};
    Stub const& stub = *m_file.get<Stub const*>();

    // Open a file with a nice filename for writing:
    auto now = UnixDateTime::now();
    auto filename_pattern = DeprecatedString::formatted(
        "/tmp/{}_pid{}_t{}_XXXXXX.pcap"sv,
        stub.name(),
        getpid(),
        now.truncated_seconds_since_epoch());
    // FIXME: Core::System::mkstemp is terrible to use!
    int file_fd = mkstemps(const_cast<char*>(filename_pattern.characters()), 5);
    if (file_fd < 0)
        return Error::from_syscall("mkstemp"sv, -errno);
    ArmedScopeGuard close_fd_guard { [&] {
        close(file_fd);
    } };
    m_file = TRY(Core::File::adopt_fd(file_fd, Core::File::OpenMode::Write));
    close_fd_guard.disarm();
    // Note that we overwrote parts of filename_pattern during mkstemp!
    dbgln("Will dump all traffic to and from {} into file {}", stub.name(), filename_pattern);

    // Write the pcap header:
    for (u32 value : PCAP_MAGIC_HEADER) {
        // TODO: Optimize these many writes into a single one.
        TRY(write_u32(value));
    }

    return {};
}

ErrorOr<void> TrafficDump::write_u32(u32 value)
{
    LittleEndian<u32> value_le { value };
    static_assert(sizeof(value_le) == sizeof(u32));
    return m_file.get<NonnullOwnPtr<Core::File>>()->write_until_depleted({ &value_le, sizeof(value_le) });
}

ErrorOr<void> TrafficDump::notify_message(AK::ReadonlyBytes bytes, Direction direction)
{
    // TODO: Optimize these many writes into a single one.

    // timeval (u32 sec, u32 usec)
    auto now = UnixDateTime::now().to_timeval();
    TRY(write_u32(now.tv_sec));
    TRY(write_u32(now.tv_usec));

    // Length must effectively be provided twice.
    TRY(write_u32(4 + bytes.size()));
    TRY(write_u32(4 + bytes.size()));

    // Direction. The enum already is compatible with the Wireshark internal values.
    TRY(write_u32(static_cast<u32>(direction)));

    // And finally, the data themselves.
    TRY(m_file.get<NonnullOwnPtr<Core::File>>()->write_until_depleted(bytes));

    return {};
}

ErrorOr<void> TrafficDump::notify_outgoing_message(MessageBuffer const& message_buffer)
{
    TRY(lazy_init_if_necessary());
    return notify_message(message_buffer.data, Direction::P2P_DIR_SENT);
}

ErrorOr<void> TrafficDump::notify_incoming_message(ReadonlyBytes buffer)
{
    TRY(lazy_init_if_necessary());
    return notify_message(buffer, Direction::P2P_DIR_RECV);
}

}
