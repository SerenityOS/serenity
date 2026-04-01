/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MemoryStream.h>
#include <LibSSH/Peer.h>
#include <LibTest/TestCase.h>

class SocketMock final : public Core::Socket {
public:
    SocketMock(AllocatingMemoryStream& stream)
        : stream(stream)
    {
    }

    // ^AK::Stream
    ErrorOr<Bytes> read_some(Bytes) override { VERIFY_NOT_REACHED(); }
    ErrorOr<size_t> write_some(ReadonlyBytes bytes) override
    {
        return stream.write_some(bytes);
    }

    bool is_eof() const override { VERIFY_NOT_REACHED(); }
    bool is_open() const override { return m_is_open; }
    void close() override { m_is_open = false; }

    // ^Core::Socket
    ErrorOr<size_t> pending_bytes() const override { VERIFY_NOT_REACHED(); }
    ErrorOr<bool> can_read_without_blocking(int) const override { VERIFY_NOT_REACHED(); }
    ErrorOr<void> set_blocking(bool) override { VERIFY_NOT_REACHED(); }
    ErrorOr<void> set_close_on_exec(bool) override { VERIFY_NOT_REACHED(); }

    AllocatingMemoryStream& stream;
    bool m_is_open { true };
};

class PeerMock final : public SSH::Peer {
public:
    PeerMock(SocketMock& socket)
        : SSH::Peer(socket)
    {
        Crypto::Hash::Digest<256> hash;
        fill_with_random(hash.data);
        set_hash(hash);
        auto shared_secret = MUST(ByteBuffer::copy(hash.bytes()));
        set_shared_secret(move(shared_secret));
    }

    ErrorOr<void> handle_new_keys_message(ByteBuffer& data)
    {
        return SSH::Peer::handle_new_keys_message(data);
    }

    ErrorOr<void> send_new_keys_message()
    {
        return SSH::Peer::send_new_keys_message();
    }

    ErrorOr<ByteBuffer> read_packet(ByteBuffer& data)
    {
        return SSH::Peer::read_packet(data);
    }

    ErrorOr<void> handle_disconnect_message(ByteBuffer& data)
    {
        return SSH::Peer::handle_disconnect_message(data);
    }

    void set_hash(Crypto::Hash::Digest<256> hash)
    {
        SSH::Peer::set_hash(hash);
    }

    ReadonlyBytes session_id() const
    {
        return SSH::Peer::session_id();
    }
};

// Copied from wireshark, sniffed from a connection between an openssh server and client.
static constexpr auto new_keys_message = "\000\000\000\f\n\025\000\000\000\000\000\000\000\000\000\000"sv;

TEST_CASE(new_keys)
{
    AllocatingMemoryStream stream;
    SocketMock socket(stream);
    auto peer = PeerMock(socket);

    TRY_OR_FAIL(peer.send_new_keys_message());
    auto written_packet = TRY_OR_FAIL(stream.read_until_eof());
    EXPECT_EQ(written_packet.size(), 16u);
    // The packet includes random bytes at the end.
    u8 size_without_padding = 6;
    EXPECT_EQ(written_packet.bytes().trim(size_without_padding), new_keys_message.bytes().trim(size_without_padding));

    auto message = TRY_OR_FAIL(ByteBuffer::copy(new_keys_message.bytes()));
    TRY_OR_FAIL(peer.handle_new_keys_message(message));
}

TEST_CASE(consecutive_packets)
{
    AllocatingMemoryStream stream;
    SocketMock socket(stream);
    auto peer = PeerMock(socket);

    auto input = TRY_OR_FAIL(ByteBuffer::copy(new_keys_message.bytes()));
    auto additional_data = "This is additional data that should still be present at the end of the test!"sv;

    input.append(additional_data.bytes());

    TRY_OR_FAIL(peer.read_packet(input));
    EXPECT_EQ(input.bytes(), additional_data.bytes());
}

TEST_CASE(disconnect_packet)
{
    AllocatingMemoryStream stream;
    SocketMock socket(stream);
    auto peer = PeerMock(socket);

    auto raw_message = "\x01\x00\x00\x00\x0b\x00\x00\x00\x14\x64\x69\x73\x63\x6f\x6e\x6e\x65\x63\x74\x65\x64\x20\x62\x79\x20\x75\x73\x65\x72\x00\x00\x00\x00"sv;
    auto message = TRY_OR_FAIL(ByteBuffer::copy(raw_message.bytes()));

    TRY_OR_FAIL(peer.handle_disconnect_message(message));
    EXPECT(!socket.is_open());
}

TEST_CASE(stable_session_id)
{
    // A session ID is stable, even after the peers rekeyed.
    AllocatingMemoryStream stream;
    SocketMock socket(stream);
    auto peer = PeerMock(socket);

    auto session_id = peer.session_id();

    Crypto::Hash::Digest<256> new_hash;
    fill_with_random({ new_hash.data, sizeof(new_hash.data) });
    peer.set_hash(new_hash);

    EXPECT_EQ(peer.session_id(), session_id);
}
