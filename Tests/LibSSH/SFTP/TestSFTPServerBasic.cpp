/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MemoryStream.h>
#include <LibFileSystem/TempFile.h>
#include <LibTest/TestCase.h>
#include <Tests/LibSSH/SFTP/Shared.h>

TEST_CASE(handshake)
{
    bool already_called { false };
    auto send_packet = [&](ReadonlyBytes bytes) -> ErrorOr<void> {
        EXPECT_EQ(already_called, false);
        already_called = true;
        EXPECT_EQ(bytes, g_version_packet.bytes());
        return {};
    };

    SSH::SFTP::Server server { move(send_packet) };

    // We should handle one initialization packet and answer it.
    TRY_OR_FAIL(server_process_data(server, g_initialization_packet.bytes()));

    // We should fail when receiving a second initialization packet.
    EXPECT(server_process_data(server, g_initialization_packet.bytes()).is_error());
}

TEST_CASE(partial_packet)
{
    u32 call_count = 0;
    auto callback = [&](Message message) -> ErrorOr<void> {
        call_count++;
        EXPECT_EQ(message.type, SSH::SFTP::FXPMessageID::HANDLE);
        return {};
    };
    auto server = make_initialized_server(move(callback));

    auto tmp_file = TRY_OR_FAIL(FileSystem::TempFile::create_temp_file());
    auto open_packet = TRY_OR_FAIL(make_open_packet(tmp_file->path(), 42));

    auto session = TRY_OR_FAIL(SSH::Session::create(0, 0, 0));

    // Send the packet byte per byte.
    for (u32 i = 0; i < open_packet.size(); ++i) {
        session->channel_data.append(open_packet[i]);
        TRY_OR_FAIL(Core::run_async_in_new_event_loop([&]() { return server.handle_channel_data(session); }));
    }

    EXPECT_EQ(call_count, 1u);
}

TEST_CASE(consecutive_packets)
{
    u32 call_count = 0;
    auto callback = [&](Message message) -> ErrorOr<void> {
        call_count++;
        EXPECT_EQ(message.type, SSH::SFTP::FXPMessageID::HANDLE);
        return {};
    };
    auto server = make_initialized_server(move(callback));

    auto tmp_file = TRY_OR_FAIL(FileSystem::TempFile::create_temp_file());
    auto open_packet = TRY_OR_FAIL(make_open_packet(tmp_file->path(), 42));

    auto session = TRY_OR_FAIL(SSH::Session::create(0, 0, 0));
    session->channel_data.append(open_packet);
    session->channel_data.append(open_packet);

    TRY_OR_FAIL(Core::run_async_in_new_event_loop([&]() { return server.handle_channel_data(session); }));

    EXPECT_EQ(call_count, 2u);
}
