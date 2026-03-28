/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MemoryStream.h>
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
