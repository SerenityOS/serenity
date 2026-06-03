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

inline ErrorOr<ByteBuffer> make_close_packet(StringView handle, u32 request_id)
{
    AllocatingMemoryStream inner;

    TRY(inner.write_value(SSH::SFTP::FXPMessageID::CLOSE));
    TRY(inner.write_value<NetworkOrdered<u32>>(request_id));
    TRY(SSH::encode_string(inner, handle));

    auto payload = TRY(inner.read_until_eof());

    AllocatingMemoryStream packet;
    TRY(packet.write_value<NetworkOrdered<u32>>(payload.size()));
    TRY(packet.write_until_depleted(payload));

    return TRY(packet.read_until_eof());
}

TEST_CASE(basic)
{
    auto temp_file = TRY_OR_FAIL(FileSystem::TempFile::create_temp_file());

    Optional<ByteBuffer> handle;
    u32 request_id = 42;

    SSH::SFTP::FXStatus expected_status = SSH::SFTP::FXStatus::OK;

    auto callback = [&](Message message) -> ErrorOr<void> {
        FixedMemoryStream stream { message.payload };
        if (!handle.has_value()) {

            EXPECT_EQ(message.type, SSH::SFTP::FXPMessageID::HANDLE);
            EXPECT_EQ(request_id, TRY(stream.read_value<NetworkOrdered<u32>>()));

            handle = TRY(SSH::decode_string(stream));
            return {};
        }

        return check_status_message(message, expected_status, request_id);
    };
    auto server = make_initialized_server(move(callback));

    auto open_packet = TRY_OR_FAIL(make_open_packet(temp_file->path(), request_id));
    TRY_OR_FAIL(server_process_data(server, open_packet));

    if (!handle.has_value())
        FAIL("The handle should have been received");

    auto close_1 = TRY_OR_FAIL(make_close_packet(*handle, ++request_id));
    expected_status = SSH::SFTP::FXStatus::OK;
    TRY_OR_FAIL(server_process_data(server, close_1));

    auto close_2 = TRY_OR_FAIL(make_close_packet(*handle, ++request_id));
    expected_status = SSH::SFTP::FXStatus::FAILURE;
    TRY_OR_FAIL(server_process_data(server, close_2));
}
