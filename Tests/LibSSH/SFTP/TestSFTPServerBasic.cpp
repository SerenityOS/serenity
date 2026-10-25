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

TEST_CASE(partial_initialization_packet)
{
    bool sent_version = false;
    SSH::SFTP::Server server([&](ReadonlyBytes packet) -> ErrorOr<void> {
        EXPECT_EQ(packet, g_version_packet.bytes());
        sent_version = true;
        return {};
    });
    auto session = TRY_OR_FAIL(SSH::Session::create(0, 512 * 1024, 32 * 1024));

    for (size_t i = 0; i < g_initialization_packet.length(); ++i) {
        session->channel_data.append(g_initialization_packet.bytes()[i]);
        ++session->total_received_bytes;
        auto behavior = TRY_OR_FAIL(Core::run_async_in_new_event_loop([&]() { return server.handle_channel_data(session); }));
        if (i + 1 < g_initialization_packet.length())
            EXPECT_EQ(behavior, SSH::BehaviorControl::WaitForMoreData);
        else
            EXPECT_EQ(behavior, SSH::BehaviorControl::ContinueExecution);
    }

    EXPECT(sent_version);
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

    auto session = TRY_OR_FAIL(SSH::Session::create(0, 512 * 1024, 32 * 1024));

    // Send the packet byte per byte.
    for (u32 i = 0; i < open_packet.size(); ++i) {
        session->channel_data.append(open_packet[i]);
        ++session->total_received_bytes;
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

    auto session = TRY_OR_FAIL(SSH::Session::create(0, 512 * 1024, 32 * 1024));
    session->channel_data.append(open_packet);
    session->channel_data.append(open_packet);
    session->total_received_bytes = open_packet.size() * 2;

    TRY_OR_FAIL(Core::run_async_in_new_event_loop([&]() { return server.handle_channel_data(session); }));

    EXPECT_EQ(call_count, 2u);
}

TEST_CASE(reject_oversized_packet_length)
{
    auto server = make_initialized_server([](Message) -> ErrorOr<void> {
        FAIL("An oversized packet must not produce a response"sv);
        return {};
    });

    auto oversized_length = to_array<u8>({ 0xff, 0xff, 0xff, 0xff });
    EXPECT(server_process_data(server, oversized_length.span()).is_error());
}

TEST_CASE(reject_request_body_spanning_into_next_packet)
{
    bool got_response = false;
    auto server = make_initialized_server([&](Message) -> ErrorOr<void> {
        got_response = true;
        return {};
    });

    auto temp_file = TRY_OR_FAIL(FileSystem::TempFile::create_temp_file());
    auto path = temp_file->path().bytes();
    EXPECT(path.size() < 0x00ffffff);

    AllocatingMemoryStream malformed_data;
    // This packet claims to contain only the message type (OPEN), while its
    // purported request body is placed after the following packet's length field.
    TRY_OR_FAIL(malformed_data.write_value<NetworkOrdered<u32>>(1));
    TRY_OR_FAIL(malformed_data.write_value(SSH::SFTP::FXPMessageID::OPEN));

    // The following packet's length is consumed as the malformed OPEN request ID.
    TRY_OR_FAIL(malformed_data.write_value<NetworkOrdered<u32>>(path.size() + 12));
    // A deliberately invalid message type lets its first byte participate in the
    // malformed OPEN's filename-length field without forming a valid second packet.
    TRY_OR_FAIL(malformed_data.write_value<u8>(0));
    u32 fake_request_id = (static_cast<u32>(path.size()) << 8) | path[0];
    TRY_OR_FAIL(malformed_data.write_value<NetworkOrdered<u32>>(fake_request_id));
    TRY_OR_FAIL(malformed_data.write_until_depleted(path.slice(1)));
    TRY_OR_FAIL(malformed_data.write_value<NetworkOrdered<u32>>(3)); // READ | WRITE
    TRY_OR_FAIL(malformed_data.write_value<NetworkOrdered<u32>>(0)); // No attributes.

    auto packet = TRY_OR_FAIL(malformed_data.read_until_eof());
    EXPECT(server_process_data(server, packet).is_error());
    EXPECT(!got_response);
}

TEST_CASE(open_handles_are_bounded_per_sftp_session)
{
    auto temp_file = TRY_OR_FAIL(FileSystem::TempFile::create_temp_file());
    u32 handle_count = 0;
    u32 failure_count = 0;
    auto server = make_initialized_server([&](Message message) -> ErrorOr<void> {
        if (message.type == SSH::SFTP::FXPMessageID::HANDLE) {
            ++handle_count;
            return {};
        }
        if (message.type == SSH::SFTP::FXPMessageID::STATUS) {
            ++failure_count;
            return check_status_message(message, SSH::SFTP::FXStatus::FAILURE);
        }
        return Error::from_string_literal("Unexpected response type");
    });

    auto open_packet = TRY_OR_FAIL(make_open_packet(temp_file->path(), 42));
    for (size_t i = 0; i < 65; ++i)
        TRY_OR_FAIL(server_process_data(server, open_packet));

    EXPECT_EQ(handle_count, 64u);
    EXPECT_EQ(failure_count, 1u);
}

TEST_CASE(reject_open_flags_with_unsupported_high_bits)
{
    auto temp_file = TRY_OR_FAIL(FileSystem::TempFile::create_temp_file());
    auto server = make_initialized_server([](Message) -> ErrorOr<void> {
        FAIL("Unsupported open flags must not produce a response"sv);
        return {};
    });

    auto open_packet = TRY_OR_FAIL(make_open_packet(temp_file->path(), 42, 0x101)); // READ | unsupported high bit
    EXPECT(server_process_data(server, open_packet).is_error());
}

TEST_CASE(reject_open_without_access_mode_before_creating_file)
{
    auto temp_directory = TRY_OR_FAIL(FileSystem::TempFile::create_temp_directory());
    auto path = ByteString::formatted("{}/created"sv, temp_directory->path());
    auto server = make_initialized_server([](Message) -> ErrorOr<void> {
        FAIL("Invalid open flags must not produce a response"sv);
        return {};
    });

    auto open_packet = TRY_OR_FAIL(make_open_packet(path, 42, 0x08)); // SSH_FXF_CREAT only
    EXPECT(server_process_data(server, open_packet).is_error());

    auto maybe_stat = Core::System::stat(path);
    EXPECT(maybe_stat.is_error());
    if (!maybe_stat.is_error())
        TRY_OR_FAIL(Core::System::unlink(path));
}
