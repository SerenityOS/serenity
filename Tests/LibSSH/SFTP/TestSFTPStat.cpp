/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MemoryStream.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibFileSystem/TempFile.h>
#include <LibSSH/DataTypes.h>
#include <LibTest/TestCase.h>
#include <Tests/LibSSH/SFTP/Shared.h>

namespace {

struct Attributes {
    Optional<u64> size;
    Optional<u32> uid;
    Optional<u32> gid;
    Optional<u32> mode;
    Optional<u32> atim;
    Optional<u32> mtim;

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

    bool operator==(Attributes const&) const = default;
};

ErrorOr<Attributes> decode_attribute_message(FixedMemoryStream& stream)
{
    u32 flags = TRY(stream.read_value<NetworkOrdered<u32>>());
    // All flags, but EXTENDED set.
    EXPECT_EQ(flags, 0b1111u);
    Attributes attributes;
    attributes.size = TRY(stream.read_value<NetworkOrdered<u64>>());
    attributes.uid = TRY(stream.read_value<NetworkOrdered<u32>>());
    attributes.gid = TRY(stream.read_value<NetworkOrdered<u32>>());
    attributes.mode = TRY(stream.read_value<NetworkOrdered<u32>>());
    attributes.atim = TRY(stream.read_value<NetworkOrdered<u32>>());
    attributes.mtim = TRY(stream.read_value<NetworkOrdered<u32>>());

    return attributes;
}

ErrorOr<ByteBuffer> make_stat_packet(StringView path, bool use_lstat = false)
{
    AllocatingMemoryStream inner;
    TRY(inner.write_value(use_lstat ? SSH::SFTP::FXPMessageID::LSTAT : SSH::SFTP::FXPMessageID::STAT));
    TRY(inner.write_value<NetworkOrdered<u32>>(42));
    TRY(SSH::encode_string(inner, path));
    auto payload = TRY(inner.read_until_eof());

    AllocatingMemoryStream packet;
    TRY(packet.write_value<NetworkOrdered<u32>>(payload.size()));
    TRY(packet.write_until_depleted(payload));
    return TRY(packet.read_until_eof());
}

ErrorOr<void> run_for_path_impl(StringView path, bool use_lstat, Attributes const& expected)
{
    auto callback = [=](Message message) -> ErrorOr<void> {
        EXPECT_EQ(message.type, SSH::SFTP::FXPMessageID::ATTRS);
        FixedMemoryStream stream { message.payload };
        EXPECT_EQ(TRY(stream.read_value<NetworkOrdered<u32>>()), 42u);
        auto attributes = TRY(decode_attribute_message(stream));
        EXPECT_EQ(attributes, expected);
        return {};
    };
    auto server = make_initialized_server(move(callback));

    auto request = TRY(make_stat_packet(path, use_lstat));
    auto stream = FixedMemoryStream { request.bytes() };
    return server.handle_data(stream);
}

ErrorOr<void> run_for_path(StringView path)
{
    TRY(run_for_path_impl(path, false, Attributes::from_stat(TRY(Core::System::stat(path)))));
    TRY(run_for_path_impl(path, true, Attributes::from_stat(TRY(Core::System::lstat(path)))));
    return {};
}

ErrorOr<void> run_for_path_with_expected_stat_failure(StringView path)
{
    EXPECT(Core::System::stat(path).is_error());
    TRY(run_for_path_impl(path, true, Attributes::from_stat(TRY(Core::System::lstat(path)))));
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

    auto directory = TRY_OR_FAIL(FileSystem::TempFile::create_temp_directory());
    TRY_OR_FAIL(run_for_path(directory->path()));

    TRY_OR_FAIL(run_for_path("/dev/null"sv));

    // Different set of attributes.
    TRY_OR_FAIL(run_for_path("/etc/passwd"sv));
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
    TRY_OR_FAIL(run_for_path_with_expected_stat_failure("/tmp/b"sv));

    unlink("/tmp/c");
    TRY_OR_FAIL(Core::System::symlink("/i/dont/exist"sv, "/tmp/c"sv));
    TRY_OR_FAIL(run_for_path_with_expected_stat_failure("/tmp/c"sv));
}
