/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MemoryStream.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <LibFileSystem/TempFile.h>
#include <LibSSH/DataTypes.h>
#include <LibTest/TestCase.h>
#include <Tests/LibSSH/SFTP/Shared.h>

namespace {

ErrorOr<ByteBuffer> make_write_packet(ByteBuffer const& handle, u32 request_id, u64 offset, ReadonlyBytes data)
{
    AllocatingMemoryStream inner;

    TRY(inner.write_value(SSH::SFTP::FXPMessageID::WRITE));
    TRY(inner.write_value<NetworkOrdered<u32>>(request_id));
    TRY(SSH::encode_string(inner, handle));
    TRY(inner.write_value<NetworkOrdered<u64>>(offset));
    TRY(SSH::encode_string(inner, data));

    auto payload = TRY(inner.read_until_eof());

    AllocatingMemoryStream packet;
    TRY(packet.write_value<NetworkOrdered<u32>>(payload.size()));
    TRY(packet.write_until_depleted(payload));

    return TRY(packet.read_until_eof());
}

ErrorOr<void> compare_files(StringView path1, StringView path2)
{
    auto file1 = TRY(Core::File::open(path1, Core::File::OpenMode::Read));
    auto file2 = TRY(Core::File::open(path2, Core::File::OpenMode::Read));

    auto data1 = TRY(file1->read_until_eof());
    auto data2 = TRY(file2->read_until_eof());
    EXPECT_EQ(data1.bytes(), data2.bytes());
    return {};
}

ErrorOr<void> run_write_test(StringView path, u64 offset, ReadonlyBytes data)
{
    auto reference_file = TRY(FileSystem::TempFile::create_temp_file());
    auto reference_path = reference_file->path();

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

        EXPECT_EQ(message.type, SSH::SFTP::FXPMessageID::STATUS);
        EXPECT_EQ(request_id, TRY(stream.read_value<NetworkOrdered<u32>>()));
        EXPECT_EQ(to_underlying(SSH::SFTP::FXStatus::OK), TRY(stream.read_value<NetworkOrdered<u32>>()));
        // error message and language tag
        EXPECT(TRY(SSH::decode_string(stream)).is_empty());
        EXPECT(TRY(SSH::decode_string(stream)).is_empty());

        TRY(compare_files(reference_path, path));

        return {};
    };
    auto server = make_initialized_server(move(callback));

    auto open_packet = TRY(make_open_packet(path, request_id));
    TRY(server_process_data(server, open_packet));

    if (!handle.has_value())
        return Error::from_string_literal("The handle should have been received");

    TRY(FileSystem::copy_file_or_directory(reference_path, path, FileSystem::RecursionMode::Disallowed, FileSystem::LinkMode::Disallowed, FileSystem::AddDuplicateFileMarker::No));
    auto fd = TRY(Core::System::open(reference_path, O_WRONLY));
    u64 written = TRY(Core::System::pwrite(fd, data, offset));
    EXPECT_EQ(written, data.size());

    request_id = get_random<u32>();
    auto packet = TRY(make_write_packet(*handle, request_id, offset, data));
    TRY(server_process_data(server, packet));

    return {};
}

}

TEST_CASE(basic)
{
    auto temp_file = TRY_OR_FAIL(FileSystem::TempFile::create_temp_file());
    auto file = TRY_OR_FAIL(Core::File::open(temp_file->path(), Core::File::OpenMode::Write));
    TRY_OR_FAIL(file->write_until_depleted("hello world"sv));

    TRY_OR_FAIL(run_write_test(temp_file->path(), 0, "hello"sv.bytes()));
    TRY_OR_FAIL(run_write_test(temp_file->path(), 5, " world"sv.bytes()));
}

TEST_CASE(write_offset)
{
    auto temp_file = TRY_OR_FAIL(FileSystem::TempFile::create_temp_file());
    auto file = TRY_OR_FAIL(Core::File::open(temp_file->path(), Core::File::OpenMode::Write));
    TRY_OR_FAIL(file->write_until_depleted("hello world"sv));

    TRY_OR_FAIL(run_write_test(temp_file->path(), 1, "hello world"sv.bytes()));
}

TEST_CASE(write_past_eof)
{
    auto temp_file = TRY_OR_FAIL(FileSystem::TempFile::create_temp_file());
    auto file = TRY_OR_FAIL(Core::File::open(temp_file->path(), Core::File::OpenMode::Write));
    TRY_OR_FAIL(run_write_test(temp_file->path(), 512, "hello world"sv.bytes()));
}

TEST_CASE(write_zero_length)
{
    auto temp_file = TRY_OR_FAIL(FileSystem::TempFile::create_temp_file());
    auto file = TRY_OR_FAIL(Core::File::open(temp_file->path(), Core::File::OpenMode::Write));
    TRY_OR_FAIL(run_write_test(temp_file->path(), 0, {}));

    TRY_OR_FAIL(file->write_until_depleted("abc"sv));

    TRY_OR_FAIL(run_write_test(temp_file->path(), 5, {}));
}
