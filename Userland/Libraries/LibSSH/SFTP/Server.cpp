/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Server.h"
#include <AK/Debug.h>
#include <AK/Endian.h>
#include <AK/NumericLimits.h>
#include <AK/ScopeGuard.h>
#include <LibCore/System.h>
#include <LibSSH/DataTypes.h>
#include <LibSSH/Session.h>

namespace SSH::SFTP {

static constexpr u32 MAXIMUM_READ_REQUEST_SIZE = 32 * 1024;
static constexpr size_t MAXIMUM_OPEN_FILES = 64;

static bool offset_range_fits_off_t(u64 offset, u64 length)
{
    auto maximum_offset = static_cast<u64>(NumericLimits<off_t>::max());
    return offset <= maximum_offset && length <= maximum_offset - offset;
}

Coroutine<ErrorOr<BehaviorControl>> Server::handle_channel_data(Session& session)
{
    // FIXME: For the moment everything is done sequentially, but we should
    //        probably make most IO related syscall asynchronous.

    FixedMemoryStream stream { session.channel_data.data() };
    ScopeGuard commit_read_bytes { [&] { session.dequeue_channel_data(stream.offset()); } };

    if (m_state == State::Constructed) {
        auto packet_data = session.channel_data.data().slice(stream.offset());
        if (!CO_TRY(is_buffer_containing_a_full_packet(packet_data)))
            co_return BehaviorControl::WaitForMoreData;

        FixedMemoryStream packet_header { packet_data };
        u32 packet_length = CO_TRY(packet_header.read_value<NetworkOrdered<u32>>());
        FixedMemoryStream packet_stream { packet_data.trim(sizeof(u32) + packet_length) };
        CO_TRY(handle_init_message(packet_stream));
        CO_TRY(stream.discard(sizeof(u32) + packet_length));
    }

    VERIFY(m_state == State::Initialized);
    while (stream.remaining() > 0) {
        auto packet_data = session.channel_data.data().slice(stream.offset());
        if (!CO_TRY(is_buffer_containing_a_full_packet(packet_data)))
            co_return BehaviorControl::WaitForMoreData;

        FixedMemoryStream packet_header { packet_data };
        u32 packet_length = CO_TRY(packet_header.read_value<NetworkOrdered<u32>>());
        FixedMemoryStream packet_stream { packet_data.trim(sizeof(u32) + packet_length) };
        CO_TRY(handle_packet(packet_stream));
        CO_TRY(stream.discard(sizeof(u32) + packet_length));
    }

    if (session.has_received_eof)
        m_is_ready_to_be_closed = true;

    co_return BehaviorControl::ContinueExecution;
}

void Server::handle_channel_eof(Session const& session)
{
    if (session.channel_data.is_empty())
        m_is_ready_to_be_closed = true;
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
    case FXPMessageID::OPEN:
        return handle_open(stream);
    case FXPMessageID::READ:
        return handle_read(stream);
    case FXPMessageID::STAT:
        return handle_stat(stream, StatType::Normal);
    case FXPMessageID::LSTAT:
        return handle_stat(stream, StatType::LStat);
    case FXPMessageID::FSTAT:
        return handle_stat(stream, StatType::FStat);
    case FXPMessageID::WRITE:
        return handle_write(stream);
    case FXPMessageID::SETSTAT:
        return handle_setstat(stream, StatType::Normal);
    case FXPMessageID::FSETSTAT:
        return handle_setstat(stream, StatType::FStat);
    case FXPMessageID::CLOSE:
        return handle_close(stream);
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
    auto path_or_handle = TRY(decode_string(stream));

    // "The server responds to this request with either SSH_FXP_ATTRS or SSH_FXP_STATUS."
    auto maybe_stat = [&] -> ErrorOr<struct stat> {
        switch (type) {
        case StatType::LStat:
            return Core::System::lstat(path_or_handle);
        case StatType::FStat: {
            auto fd = TRY(find_file(path_or_handle))->file->fd();
            return Core::System::fstat(fd);
        }
        case StatType::Normal:
            return Core::System::stat(path_or_handle);
        }
        VERIFY_NOT_REACHED();
    }();

    if (maybe_stat.is_error()) {
        auto const& error = maybe_stat.error();
        VERIFY(error.is_errno());
        if (error.code() == ENOENT)
            TRY(send_status_message(id, FXStatus::NO_SUCH_FILE));
        else if (error.code() == EACCES)
            TRY(send_status_message(id, FXStatus::PERMISSION_DENIED));
        else
            TRY(send_status_message(id, FXStatus::FAILURE));
        return {};
    }

    auto stat = maybe_stat.release_value();
    TRY(send_file_attribute_message(id, stat));
    return {};
}

// 7. Responses from the Server to the Client
// https://datatracker.ietf.org/doc/html/draft-ietf-secsh-filexfer-02#section-7
ErrorOr<void> Server::send_file_attribute_message(u32 id, struct stat const& s)
{
    AllocatingMemoryStream stream;
    TRY(stream.write_value(FXPMessageID::ATTRS));
    TRY(stream.write_value<NetworkOrdered<u32>>(id));
    TRY(Attributes::from_stat(s).encode(stream));

    auto packet = TRY(stream.read_until_eof());
    TRY(write_packet(packet));
    return {};
}

// 6.9 Setting File Attributes
// https://datatracker.ietf.org/doc/html/draft-ietf-secsh-filexfer-02#section-6.9
ErrorOr<void> Server::handle_setstat(FixedMemoryStream& stream, StatType type)
{
    VERIFY(type != StatType::LStat);

    u32 id = TRY(stream.read_value<NetworkOrdered<u32>>());
    auto path_or_handle = TRY(decode_string(stream));
    auto attr = TRY(Attributes::from_stream(stream));

    if (type == StatType::FStat && attr.size.has_value()
        && *attr.size > static_cast<u64>(NumericLimits<off_t>::max()))
        return send_status_message(id, FXStatus::FAILURE);

    Optional<int> maybe_fd {};
    if (type == StatType::FStat)
        maybe_fd = TRY(find_file(path_or_handle))->file->fd();

    auto maybe_error = [&] -> ErrorOr<void> {
        if (attr.size.has_value()) {
            if (type == StatType::FStat)
                TRY(Core::System::ftruncate(*maybe_fd, static_cast<off_t>(*attr.size)));
            else
                dbgln("FIXME: FSETSTAT should set file size");
        }
        if (attr.uid.has_value()) {
            VERIFY(attr.gid.has_value());
            dbgln("FIXME: FSETSTAT should set user/group");
        }
        if (attr.mode.has_value()) {
            dbgln("FIXME: FSETSTAT should set mode");
        }
        if (attr.atim.has_value()) {
            VERIFY(attr.mtim.has_value());
            dbgln("FIXME: FSETSTAT should set atim/mtim");
        }
        return {};
    }();

    if (maybe_error.is_error()) {
        auto const& error = maybe_error.error();
        VERIFY(error.is_errno());
        if (error.code() == ENOENT)
            TRY(send_status_message(id, FXStatus::NO_SUCH_FILE));
        else if (error.code() == EACCES)
            TRY(send_status_message(id, FXStatus::PERMISSION_DENIED));
        else
            TRY(send_status_message(id, FXStatus::FAILURE));
        return {};
    }

    TRY(send_status_message(id, FXStatus::OK));
    return {};
}

// 6.3 Opening, Creating, and Closing Files
// https://datatracker.ietf.org/doc/html/draft-ietf-secsh-filexfer-02#section-6.3

enum class PFlag : u8 {
    None = 0, // This is AD-HOC.
    READ = 0x01,
    WRITE = 0x02,
    APPEND = 0x04,
    CREAT = 0x08,
    TRUNC = 0x10,
    EXCL = 0x20,
};

AK_ENUM_BITWISE_OPERATORS(PFlag)

struct CoreAndPosixFlags {
    Core::File::OpenMode open_mode {};
    int posix_flags {};
};

static ErrorOr<CoreAndPosixFlags> convert_pflags(PFlag& pflags)
{
    if (!has_flag(pflags, PFlag::READ) && !has_flag(pflags, PFlag::WRITE))
        return Error::from_string_literal("Open requires read or write access");

    int open_flags {};
    Core::File::OpenMode open_mode {};
    auto consume = [](PFlag& flags, PFlag mask) {
        bool had_flag = has_flag(flags, mask);
        flags &= ~mask;
        return had_flag;
    };

    if (has_flag(pflags, PFlag::READ) && has_flag(pflags, PFlag::WRITE)) {
        open_flags |= O_RDWR;
        open_mode = Core::File::OpenMode::ReadWrite;
        consume(pflags, PFlag::READ);
        consume(pflags, PFlag::WRITE);
    } else if (consume(pflags, PFlag::WRITE)) {
        open_flags |= O_WRONLY;
        open_mode = Core::File::OpenMode::Write;
    } else if (consume(pflags, PFlag::READ)) {
        open_flags |= O_RDONLY;
        open_mode = Core::File::OpenMode::Read;
    }
    if (consume(pflags, PFlag::CREAT))
        open_flags |= O_CREAT;

    // FIXME: Support other pflags.
    if (pflags != PFlag::None)
        return Error::from_string_literal("Unsupported pflags");

    return CoreAndPosixFlags { open_mode, open_flags };
}

ErrorOr<void> Server::handle_open(FixedMemoryStream& stream)
{
    u32 id = TRY(stream.read_value<NetworkOrdered<u32>>());
    auto filename = TRY(decode_string(stream));
    u32 raw_pflags = TRY(stream.read_value<NetworkOrdered<u32>>());
    PFlag pflags = static_cast<PFlag>(raw_pflags);
    if (raw_pflags != to_underlying(pflags))
        return Error::from_string_literal("Open flags exceed the supported flag width");
    auto attrs = TRY(Attributes::from_stream(stream));

    if (attrs.size.has_value() || attrs.uid.has_value() || attrs.gid.has_value()
        || attrs.atim.has_value() || attrs.mtim.has_value())
        return Error::from_string_literal("Unsupported file attribute");

    if (m_open_files.size() >= MAXIMUM_OPEN_FILES)
        return send_status_message(id, FXStatus::FAILURE);

    // FIXME: Support relative path.
    if (filename.is_empty() || filename[0] != '/')
        return Error::from_string_literal("Relative paths are unsupported");

    // SSH flags maps one-to-one to POSIX flags, so use that and call open
    // directly. This allows the SFTP server to be as transparent as possible.
    // We still need to map these flags to a  Core::File::OpenMode so we can
    // let Core::File adopt the fd later on (note that only read and write
    // matters when adopting).
    auto [open_mode, open_flags] = TRY(convert_pflags(pflags));

    auto maybe_fd = Core::System::open(StringView { filename.bytes() }, open_flags | O_CLOEXEC, attrs.mode.value_or(0644));

    // "The response to this message will be either SSH_FXP_HANDLE (if the
    //   operation is successful) or SSH_FXP_STATUS (if the operation fails)."
    if (maybe_fd.is_error()) {
        // FIXME: Send SSH_FXP_STATUS.
        return maybe_fd.release_error();
    }

    int fd = maybe_fd.release_value();
    ArmedScopeGuard close_fd { [&] { MUST(Core::System::close(fd)); } };
    auto file = TRY(Core::File::adopt_fd(fd, open_mode));
    close_fd.disarm();

    TRY(m_open_files.try_empend(move(file), Crypto::Hash::MD5::hash(filename)));

    TRY(send_file_handle(id, m_open_files.last()));

    return {};
}

// 7. Responses from the Server to the Client
// https://datatracker.ietf.org/doc/html/draft-ietf-secsh-filexfer-02#section-7
ErrorOr<void> Server::send_file_handle(u32 id, File const& file)
{
    AllocatingMemoryStream stream;
    TRY(stream.write_value(FXPMessageID::HANDLE));
    TRY(stream.write_value<NetworkOrdered<u32>>(id));
    TRY(encode_string(stream, file.handle.bytes()));

    auto packet = TRY(stream.read_until_eof());
    TRY(write_packet(packet));
    return {};
}

ErrorOr<void> Server::handle_close(FixedMemoryStream& stream)
{
    u32 id = TRY(stream.read_value<NetworkOrdered<u32>>());
    auto handle = TRY(decode_string(stream));

    if (m_open_files.remove_first_matching([&](auto const& file) { return file.handle.bytes() == handle; }))
        return send_status_message(id, FXStatus::OK);

    return send_status_message(id, FXStatus::FAILURE);
}

ErrorOr<Server::File*> Server::find_file(ReadonlyBytes handle)
{
    auto maybe_file = m_open_files.first_matching([&](auto const& file) {
        return file.handle.bytes() == handle;
    });

    if (maybe_file.has_value())
        return &maybe_file.value();

    return Error::from_string_literal("Unable to find file for given handle");
}

// 6.4 Reading and Writing
// https://datatracker.ietf.org/doc/html/draft-ietf-secsh-filexfer-02#section-6.4
ErrorOr<void> Server::handle_read(FixedMemoryStream& stream)
{
    u32 id = TRY(stream.read_value<NetworkOrdered<u32>>());
    auto handle = TRY(decode_string(stream));
    u64 offset = TRY(stream.read_value<NetworkOrdered<u64>>());
    u32 len = TRY(stream.read_value<NetworkOrdered<u32>>());

    // The requested length is client-controlled. Keep both the allocation and the
    // response packet bounded instead of trusting it as a ByteBuffer size.
    if (len > MAXIMUM_READ_REQUEST_SIZE)
        return send_status_message(id, FXStatus::FAILURE);
    if (len > 0 && !offset_range_fits_off_t(offset, len))
        return send_status_message(id, FXStatus::FAILURE);

    auto& file = *TRY(find_file(handle));

    // "If an error occurs or EOF is encountered before reading any
    //   data, the server will respond with SSH_FXP_STATUS."
    // FIXME: Send FXP_STATUS
    ByteBuffer buffer;
    bool should_send_eof { false };
    auto operation_result = [&]() -> ErrorOr<void> {
        TRY(buffer.try_resize(len));

        Optional<u64> last_read;
        u64 read {};
        while (!last_read.has_value() || last_read.value() != 0) {
            if (read == buffer.size())
                break;
            last_read = TRY(Core::System::pread(file.file->fd(), buffer.bytes().slice(read), offset + read));
            read += *last_read;
        }

        if (last_read == 0u) {
            buffer.trim(read, false);
            if (buffer.size() == 0)
                should_send_eof = true;
        }

        return {};
    }();

    if (operation_result.is_error()) {
        // FIXME: Send SSH_FXP_STATUS
        return operation_result.release_error();
    }

    if (should_send_eof)
        TRY(send_status_message(id, FXStatus::FX_EOF));
    else
        TRY(send_data(id, buffer));

    return {};
}

// 6.4 Reading and Writing
// https://datatracker.ietf.org/doc/html/draft-ietf-secsh-filexfer-02#section-6.4
ErrorOr<void> Server::handle_write(FixedMemoryStream& stream)
{
    u32 id = TRY(stream.read_value<NetworkOrdered<u32>>());
    auto handle = TRY(decode_string(stream));
    u64 offset = TRY(stream.read_value<NetworkOrdered<u64>>());
    auto data = TRY(decode_string(stream));

    if (!data.is_empty() && !offset_range_fits_off_t(offset, data.size()))
        return send_status_message(id, FXStatus::FAILURE);

    auto& file = *TRY(find_file(handle));

    u64 written = 0;
    while (written < data.size()) {
        auto amount_written = TRY(Core::System::pwrite(file.file->fd(), data.bytes().slice(written), offset + written));
        if (amount_written == 0)
            return send_status_message(id, FXStatus::FAILURE);
        written += static_cast<u64>(amount_written);
    }

    TRY(send_status_message(id, FXStatus::OK));

    return {};
}

ErrorOr<void> Server::send_data(u32 id, ReadonlyBytes data)
{
    AllocatingMemoryStream stream;
    TRY(stream.write_value(FXPMessageID::DATA));
    TRY(stream.write_value<NetworkOrdered<u32>>(id));
    TRY(encode_string(stream, data));

    auto packet = TRY(stream.read_until_eof());
    TRY(write_packet(packet));
    return {};
}

// 7. Responses from the Server to the Client
// https://datatracker.ietf.org/doc/html/draft-ietf-secsh-filexfer-02#section-7

ErrorOr<void> Server::send_status_message(u32 id, FXStatus status)
{
    AllocatingMemoryStream stream;
    TRY(stream.write_value(FXPMessageID::STATUS));
    TRY(stream.write_value<NetworkOrdered<u32>>(id));
    TRY(stream.write_value<NetworkOrdered<u32>>(to_underlying(status)));

    // error message and language tag
    TRY(encode_string(stream, ""sv));
    TRY(encode_string(stream, ""sv));

    auto packet = TRY(stream.read_until_eof());
    TRY(write_packet(packet));
    return {};
}

} // SSH::SFTP
