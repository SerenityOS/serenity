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
    FixedMemoryStream stream { g_initialization_packet.bytes() };
    TRY_OR_FAIL(server.handle_data(stream));

    // We should fail when receiving a second initialization packet.
    stream = FixedMemoryStream { g_initialization_packet.bytes() };
    EXPECT(server.handle_data(stream).is_error());
}
