/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MemoryStream.h>
#include <LibCore/System.h>
#include <LibFileSystem/TempFile.h>
#include <LibSSH/DataTypes.h>
#include <LibTest/TestCase.h>
#include <Tests/LibSSH/SFTP/Shared.h>

namespace {

ErrorOr<ByteBuffer> make_open_packet(StringView path, u32 request_id)
{
    AllocatingMemoryStream inner;

    TRY(inner.write_value(SSH::SFTP::FXPMessageID::OPEN));
    TRY(inner.write_value<NetworkOrdered<u32>>(request_id));
    TRY(SSH::encode_string(inner, path));
    TRY(inner.write_value<NetworkOrdered<u32>>(1)); // SSH_FXF_READ
    TRY(inner.write_value<NetworkOrdered<u32>>(0)); // No attributes.

    auto payload = TRY(inner.read_until_eof());

    AllocatingMemoryStream packet;
    TRY(packet.write_value<NetworkOrdered<u32>>(payload.size()));
    TRY(packet.write_until_depleted(payload));

    return TRY(packet.read_until_eof());
}

ErrorOr<ByteBuffer> make_read_packet(ByteBuffer const& handle, u32 request_id, u64 offset, u32 length)
{
    AllocatingMemoryStream inner;

    TRY(inner.write_value(SSH::SFTP::FXPMessageID::READ));
    TRY(inner.write_value<NetworkOrdered<u32>>(request_id));
    TRY(SSH::encode_string(inner, handle));
    TRY(inner.write_value<NetworkOrdered<u64>>(offset));
    TRY(inner.write_value<NetworkOrdered<u32>>(length));

    auto payload = TRY(inner.read_until_eof());

    AllocatingMemoryStream packet;
    TRY(packet.write_value<NetworkOrdered<u32>>(payload.size()));
    TRY(packet.write_until_depleted(payload));

    return TRY(packet.read_until_eof());
}

ErrorOr<void> run_read_test(StringView path, u64 offset, u64 size, bool expect_eof = false)
{
    Optional<ByteBuffer> handle;
    u32 request_id = get_random<u32>();
    auto callback = [&](Message message) -> ErrorOr<void> {
        FixedMemoryStream stream { message.payload };
        if (!handle.has_value()) {

            EXPECT_EQ(message.type, SSH::SFTP::FXPMessageID::HANDLE);
            EXPECT_EQ(request_id, TRY(stream.read_value<NetworkOrdered<u32>>()));

            handle = TRY(SSH::decode_string(stream));
            return {};
        }

        if (expect_eof) {
            EXPECT_EQ(message.type, SSH::SFTP::FXPMessageID::STATUS);
            EXPECT_EQ(request_id, TRY(stream.read_value<NetworkOrdered<u32>>()));
            // SSH_FX_EOF
            EXPECT_EQ(1u, TRY(stream.read_value<NetworkOrdered<u32>>()));

            return {};
        }

        EXPECT_EQ(message.type, SSH::SFTP::FXPMessageID::DATA);
        EXPECT_EQ(request_id, TRY(stream.read_value<NetworkOrdered<u32>>()));

        auto remote_data = TRY(SSH::decode_string(stream));

        auto local_buffer = TRY(ByteBuffer::create_uninitialized(remote_data.size()));
        local_buffer.resize(remote_data.size());

        auto fd = TRY(Core::System::open(path, O_RDONLY));
        ssize_t nread = TRY(Core::System::pread(fd, local_buffer, offset));
        EXPECT_EQ(static_cast<unsigned long>(nread), local_buffer.size());

        EXPECT_EQ(remote_data, local_buffer);

        return {};
    };
    auto server = make_initialized_server(move(callback));

    auto open_packet = TRY(make_open_packet(path, request_id));
    FixedMemoryStream stream { open_packet.bytes() };
    TRY(server.handle_data(stream));

    if (!handle.has_value())
        return Error::from_string_literal("The handle should have been received");

    request_id = get_random<u32>();
    auto read_packet = TRY(make_read_packet(*handle, request_id, offset, size));
    stream = FixedMemoryStream { read_packet.bytes() };
    TRY(server.handle_data(stream));

    return {};
}

}

TEST_CASE(basic)
{
    auto temp_file = TRY_OR_FAIL(FileSystem::TempFile::create_temp_file());
    auto file = TRY_OR_FAIL(Core::File::open(temp_file->path(), Core::File::OpenMode::Write));
    TRY_OR_FAIL(file->write_until_depleted("hello world"sv));

    TRY_OR_FAIL(run_read_test(temp_file->path(), 0, 5)); // "hello"
    TRY_OR_FAIL(run_read_test(temp_file->path(), 6, 5)); // "world"
}

TEST_CASE(read_past_eof)
{
    auto temp_file = TRY_OR_FAIL(FileSystem::TempFile::create_temp_file());
    auto file = TRY_OR_FAIL(Core::File::open(temp_file->path(), Core::File::OpenMode::Write));
    TRY_OR_FAIL(file->write_until_depleted("abc"sv));

    TRY_OR_FAIL(run_read_test(temp_file->path(), 1000, 1000, true));
}

TEST_CASE(read_includes_eof)
{
    auto temp_file = TRY_OR_FAIL(FileSystem::TempFile::create_temp_file());
    auto file = TRY_OR_FAIL(Core::File::open(temp_file->path(), Core::File::OpenMode::Write));
    TRY_OR_FAIL(file->write_until_depleted("abc"sv));

    TRY_OR_FAIL(run_read_test(temp_file->path(), 1, 10));
}

TEST_CASE(read_zero_length)
{
    auto file = TRY_OR_FAIL(FileSystem::TempFile::create_temp_file());
    TRY_OR_FAIL(run_read_test(file->path(), 0, 0));
}
