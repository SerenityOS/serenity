/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MemoryStream.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <LibFileSystem/TempFile.h>
#include <LibSSH/DataTypes.h>
#include <LibTest/TestCase.h>
#include <Tests/LibSSH/SFTP/Shared.h>

namespace {

enum class StatType : u8 {
    Stat,
    LStat,
    FStat,
};

ErrorOr<ByteBuffer> make_stat_packet(StringView path, StatType stat_type)
{
    AllocatingMemoryStream inner;
    SSH::SFTP::FXPMessageID packet_type = SSH::SFTP::FXPMessageID::STAT;
    switch (stat_type) {
    case StatType::Stat:
        packet_type = SSH::SFTP::FXPMessageID::STAT;
        break;
    case StatType::LStat:
        packet_type = SSH::SFTP::FXPMessageID::LSTAT;
        break;
    case StatType::FStat:
        packet_type = SSH::SFTP::FXPMessageID::FSTAT;
        break;
    }
    TRY(inner.write_value(packet_type));
    TRY(inner.write_value<NetworkOrdered<u32>>(42));
    TRY(SSH::encode_string(inner, path));
    auto payload = TRY(inner.read_until_eof());

    AllocatingMemoryStream packet;
    TRY(packet.write_value<NetworkOrdered<u32>>(payload.size()));
    TRY(packet.write_until_depleted(payload));
    return TRY(packet.read_until_eof());
}

ErrorOr<void> run_server_with_callback(auto callback, StringView path, StatType stat_type, Optional<ByteBuffer&> handle = {})
{
    auto server = make_initialized_server(move(callback));
    if (stat_type == StatType::FStat) {
        auto request = TRY(make_open_packet(path, 43));
        TRY(server_process_data(server, request));
    }

    auto request = TRY(make_stat_packet(handle.has_value() && !handle->is_empty() ? handle.value() : path, stat_type));
    return server_process_data(server, request);
}

ErrorOr<void> run_for_path_impl(StringView path, StatType stat_type, SSH::SFTP::Attributes const& expected)
{
    ByteBuffer handle;
    auto callback = [=, &handle](Message message) -> ErrorOr<void> {
        FixedMemoryStream stream { message.payload };
        if (handle.is_empty() && stat_type == StatType::FStat) {
            EXPECT_EQ(message.type, SSH::SFTP::FXPMessageID::HANDLE);
            TRY(stream.read_value<NetworkOrdered<u32>>());
            handle = TRY(SSH::decode_string(stream));
            return {};
        }

        EXPECT_EQ(message.type, SSH::SFTP::FXPMessageID::ATTRS);
        EXPECT_EQ(TRY(stream.read_value<NetworkOrdered<u32>>()), 42u);
        auto attributes = TRY(SSH::SFTP::Attributes::from_stream(stream));
        EXPECT_EQ(attributes, expected);
        return {};
    };

    return run_server_with_callback(move(callback), path, stat_type, handle);
}

ErrorOr<void> run_for_path_impl(StringView path, StatType stat_type, SSH::SFTP::FXStatus expected_status)
{
    auto callback = [=](Message message) -> ErrorOr<void> {
        return check_status_message(message, expected_status);
    };

    return run_server_with_callback(move(callback), path, stat_type);
}

ErrorOr<void> run_for_path_no_fstat(StringView path)
{
    TRY(run_for_path_impl(path, StatType::Stat, SSH::SFTP::Attributes::from_stat(TRY(Core::System::stat(path)))));
    TRY(run_for_path_impl(path, StatType::LStat, SSH::SFTP::Attributes::from_stat(TRY(Core::System::lstat(path)))));
    return {};
}

ErrorOr<void> run_for_path(StringView path)
{
    TRY(run_for_path_no_fstat(path));
    TRY(run_for_path_impl(path, StatType::FStat, SSH::SFTP::Attributes::from_stat(TRY(Core::System::stat(path)))));
    return {};
}

ErrorOr<void> run_for_path_with_expected_stat_failure(StringView path, SSH::SFTP::FXStatus error_status)
{
    EXPECT(Core::System::stat(path).is_error());
    TRY(run_for_path_impl(path, StatType::Stat, error_status));
    TRY(run_for_path_impl(path, StatType::LStat, SSH::SFTP::Attributes::from_stat(TRY(Core::System::lstat(path)))));
    return {};
}

}

TEST_CASE(basic)
{
    auto file = TRY_OR_FAIL(FileSystem::TempFile::create_temp_file());
    TRY_OR_FAIL(run_for_path(file->path()));

    {
        auto stream = TRY_OR_FAIL(Core::File::open(file->path(), Core::File::OpenMode::Write));
        TRY_OR_FAIL(stream->write_until_depleted("Some content to make the file have a size"sv));
    }
    TRY_OR_FAIL(run_for_path(file->path()));

    TRY_OR_FAIL(run_for_path("/dev/null"sv));
}

TEST_CASE(non_openable_files)
{
    auto directory = TRY_OR_FAIL(FileSystem::TempFile::create_temp_directory());
    TRY_OR_FAIL(run_for_path_no_fstat(directory->path()));

    // Different set of attributes.
    TRY_OR_FAIL(run_for_path_no_fstat("/etc/passwd"sv));
}

TEST_CASE(symlink)
{
    auto file = TRY_OR_FAIL(FileSystem::TempFile::create_temp_file());
    unlink("/tmp/a");
    TRY_OR_FAIL(Core::System::symlink(file->path(), "/tmp/a"sv));
    TRY_OR_FAIL(run_for_path(file->path()));
    TRY_OR_FAIL(run_for_path("/tmp/a"sv));

    unlink("/tmp/b");
    TRY_OR_FAIL(Core::System::symlink("/tmp/b"sv, "/tmp/b"sv));
    TRY_OR_FAIL(run_for_path_with_expected_stat_failure("/tmp/b"sv, SSH::SFTP::FXStatus::FAILURE));

    unlink("/tmp/c");
    TRY_OR_FAIL(Core::System::symlink("/i/dont/exist"sv, "/tmp/c"sv));
    TRY_OR_FAIL(run_for_path_with_expected_stat_failure("/tmp/c"sv, SSH::SFTP::FXStatus::NO_SUCH_FILE));
}

TEST_CASE(no_permission)
{
    auto tmp_dir = TRY_OR_FAIL(FileSystem::TempFile::create_temp_directory());
    // The directory is created with 0700, remove the executable bit to make inner files inaccessible.
    TRY_OR_FAIL(Core::System::chmod(tmp_dir->path(), 0600));

    auto unauthorized_path = ByteString::formatted("{}/i/dont/exist", tmp_dir->path());

    TRY_OR_FAIL(run_for_path_impl(unauthorized_path, StatType::LStat, SSH::SFTP::FXStatus::PERMISSION_DENIED));
    TRY_OR_FAIL(run_for_path_impl(unauthorized_path, StatType::Stat, SSH::SFTP::FXStatus::PERMISSION_DENIED));
}
