/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MemoryStream.h>
#include <LibTest/TestCase.h>
#include <Tests/LibSSH/SFTP/Shared.h>

TEST_CASE(read_header)
{
    auto send_packet = [&](ReadonlyBytes) -> ErrorOr<void> {
        FAIL("This shouldn't be called"sv);
        return {};
    };
    PeerMock peer { move(send_packet) };

    // Valid packet
    auto stream = FixedMemoryStream { g_initialization_packet.bytes() };
    TRY_OR_FAIL(peer.read_header(stream));
    EXPECT_EQ(stream.remaining(), 4u);

    // Invalid packets
    auto message_copy = TRY_OR_FAIL(ByteBuffer::copy(g_initialization_packet.bytes()));

    // We modify the packet content to change the signaled size.
    auto altered_content = message_copy;
    EXPECT_EQ(altered_content[3], 5);
    altered_content[3] = 4;
    stream = FixedMemoryStream { altered_content.bytes() };
    EXPECT(peer.read_header(stream).is_error());

    // We append bytes to the packet to change the actual size.
    auto altered_length = message_copy;
    altered_length.append(0);
    stream = FixedMemoryStream { altered_length.bytes() };
    EXPECT(peer.read_header(stream).is_error());
}

TEST_CASE(write_packet)
{
    static constexpr auto payload = "Random Payload"sv;

    Optional<ByteBuffer> written_packet;
    auto send_packet = [&](ReadonlyBytes bytes) -> ErrorOr<void> {
        written_packet = TRY(ByteBuffer::copy(bytes));
        return {};
    };
    PeerMock peer { move(send_packet) };

    TRY_OR_FAIL(peer.write_packet(payload.bytes()));
    EXPECT(written_packet.has_value());

    auto expected_size = to_array<u8>({
        0x00,
        0x00,
        0x00,
        payload.length(),
    });

    EXPECT_EQ(written_packet->bytes().trim(4), expected_size);
    EXPECT_EQ(written_packet->bytes().slice(4), payload.bytes());
}
