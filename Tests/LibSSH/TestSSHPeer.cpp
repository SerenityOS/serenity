/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/MemoryStream.h>
#include <LibSSH/Peer.h>
#include <errno.h>
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
        ++m_write_attempts;
        if (m_successful_write_limit > 0 && m_successful_writes >= m_successful_write_limit)
            return Error::from_errno(EWOULDBLOCK);

        ++m_successful_writes;
        if (m_maximum_write_size > 0 && bytes.size() > m_maximum_write_size)
            bytes = bytes.trim(m_maximum_write_size);
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
    size_t m_write_attempts { 0 };
    size_t m_successful_writes { 0 };
    size_t m_successful_write_limit { 0 };
    size_t m_maximum_write_size { 0 };
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

    PeerMock(SocketMock& socket, Crypto::Hash::Digest<256> hash, ByteBuffer shared_secret)
        : SSH::Peer(socket)
    {
        set_hash(hash);
        set_shared_secret(move(shared_secret));
    }

    using SSH::Peer::handle_disconnect_message;
    using SSH::Peer::handle_new_keys_message;
    using SSH::Peer::is_buffer_containing_a_full_packet;
    using SSH::Peer::read_packet;
    using SSH::Peer::send_new_keys_message;
    using SSH::Peer::session_id;
    using SSH::Peer::set_hash;
    using SSH::Peer::write_packet;
    using SSH::Peer::write_raw_bytes;
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

TEST_CASE(nonblocking_output_queue)
{
    AllocatingMemoryStream stream;
    SocketMock socket(stream);
    socket.m_maximum_write_size = 2;
    socket.m_successful_write_limit = 1;
    auto peer = PeerMock(socket);

    bool output_pending = false;
    peer.set_output_pending_callback([&](bool is_pending) { output_pending = is_pending; });

    auto identification = "banner"sv;
    TRY_OR_FAIL(peer.write_raw_bytes(identification.bytes()));
    EXPECT_EQ(socket.m_write_attempts, 2u);
    EXPECT(output_pending);

    TRY_OR_FAIL(peer.write_packet("first"sv.bytes()));
    EXPECT_EQ(socket.m_write_attempts, 3u);
    EXPECT(output_pending);

    socket.m_successful_write_limit = 0;
    TRY_OR_FAIL(peer.flush_pending_output());
    EXPECT(!output_pending);

    auto output = TRY_OR_FAIL(stream.read_until_eof());
    EXPECT_EQ(output.bytes().slice(0, identification.length()), identification.bytes());

    auto packet_length = output.size() - identification.length();
    auto packet_bytes = TRY_OR_FAIL(ByteBuffer::copy(output.bytes().slice(identification.length(), packet_length)));
    auto packet = TRY_OR_FAIL(peer.read_packet(packet_bytes));
    EXPECT_EQ(packet.bytes(), "first"sv.bytes());
    EXPECT(packet_bytes.is_empty());
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

TEST_CASE(packet_without_payload)
{
    AllocatingMemoryStream stream;
    SocketMock socket(stream);
    auto peer = PeerMock(socket);

    // packet_length only covers padding_length and the padding, leaving no payload.
    auto raw_message = "\x00\x00\x00\x05\x04\x00\x00\x00\x00"sv;
    auto message = TRY_OR_FAIL(ByteBuffer::copy(raw_message.bytes()));

    EXPECT(peer.read_packet(message).is_error());
}

TEST_CASE(padding_longer_than_packet)
{
    AllocatingMemoryStream stream;
    SocketMock socket(stream);
    auto peer = PeerMock(socket);

    // padding_length is bigger than packet_length, which used to underflow the payload length.
    auto raw_message = "\x00\x00\x00\x04\xff\x00\x00\x00\x00"sv;
    auto message = TRY_OR_FAIL(ByteBuffer::copy(raw_message.bytes()));

    EXPECT(peer.read_packet(message).is_error());
}

TEST_CASE(is_buffer_containing_a_full_packet)
{
    // A session ID is stable, even after the peers rekeyed.
    AllocatingMemoryStream stream;
    SocketMock socket(stream);
    auto peer = PeerMock(socket);

    auto message = TRY_OR_FAIL(ByteBuffer::copy(new_keys_message.bytes()));
    message.append("Additional data"sv.bytes());

    EXPECT(TRY_OR_FAIL(peer.is_buffer_containing_a_full_packet(new_keys_message.bytes())));
    EXPECT(TRY_OR_FAIL(peer.is_buffer_containing_a_full_packet(new_keys_message.bytes().trim(new_keys_message.length()))));
    EXPECT(!TRY_OR_FAIL(peer.is_buffer_containing_a_full_packet(new_keys_message.bytes().trim(new_keys_message.length() - 1))));
}

TEST_CASE(is_buffer_containing_a_full_packet_encrypted)
{
    // The data comes from ChaCha20Poly1305_decrypt.
    auto shared_secret_raw = "\x53\x22\xc2\x28\x50\xfe\x85\xbd\xb3\x35\xcf\xb4\x67\x4a\x82\x0b\xd0\x54\xa3\xd9\x4a\xc8\x3c\x55\x91\xb4\xd5\xf7\x52\x28\xd2\x6c"sv.bytes();
    auto hash_raw = "\xc0\x73\x95\x08\xd8\xc0\xbf\x2a\x6d\x13\x1b\xfd\x67\x88\x32\xdf\x15\xeb\x5a\x2e\x5a\xf3\xdf\x98\xa1\x1c\x41\x2b\x2a\x26\x4d\x62"sv.bytes();

    auto shared_secret = TRY_OR_FAIL(ByteBuffer::copy(shared_secret_raw));
    SSH::ChaCha20Poly1305Cipher::Digest hash;
    hash_raw.copy_to({ &hash.data, decltype(hash)::Size });

    // A session ID is stable, even after the peers rekeyed.
    AllocatingMemoryStream stream;
    SocketMock socket(stream);
    auto peer = PeerMock(socket, hash, shared_secret);

    auto make_message = []() {
        return ByteBuffer::copy(new_keys_message.bytes());
    };

    // This is hackish, we call read_packet multiple times to bump Peer's private variable
    // m_incoming_packet_sequence_number to 3. This is required to decrypt the packet below.
    for (u32 i = 0; i < 2; ++i) {
        auto message = TRY_OR_FAIL(make_message());
        TRY_OR_FAIL(peer.read_packet(message));
    }

    auto message = TRY_OR_FAIL(make_message());
    TRY_OR_FAIL(peer.handle_new_keys_message(message));

    // Peer is properly initialized to decrypt messages.

    auto encrypted_raw = "\x0c\x3b\x6d\x66\xb1\x72\xf3\x85\xa4\x88\x35\xf2\x0a\x6d\xa5\x9b\x29\xcf\xe6\xe8\x5f\xad\x05\x6b\x94\x89\xae\xab\x10\x37\x88\x7a\x6b\x58\x30\xb1\x9b\x6f\xc1\x8f\x6c\x89\x68\x24"sv.bytes();
    auto packet = TRY_OR_FAIL(ByteBuffer::copy(encrypted_raw));
    auto additional_data = "Additional data"sv.bytes();
    packet.append(additional_data);

    EXPECT(TRY_OR_FAIL(peer.is_buffer_containing_a_full_packet(packet)));
    EXPECT(TRY_OR_FAIL(peer.is_buffer_containing_a_full_packet(packet.bytes().trim(encrypted_raw.size()))));
    EXPECT(!TRY_OR_FAIL(peer.is_buffer_containing_a_full_packet(packet.bytes().trim(encrypted_raw.size() - 1))));

    auto decrypted_packet = TRY_OR_FAIL(peer.read_packet(packet));
    EXPECT(!decrypted_packet.is_empty());
    EXPECT_EQ(packet.bytes(), additional_data);
}

TEST_CASE(reject_oversized_packet_length_before_body_is_received)
{
    AllocatingMemoryStream stream;
    SocketMock socket(stream);
    auto peer = PeerMock(socket);

    auto oversized_length = to_array<u8>({ 0xff, 0xff, 0xff, 0xff });
    EXPECT(peer.is_buffer_containing_a_full_packet(oversized_length.span()).is_error());

    // Direct packet decoding also validates the length before a cipher can use
    // it to construct a span into a packet buffer.
    auto truncated_packet = to_array<u8>({ 0xff, 0xff, 0xff, 0xff, 0, 0, 0, 0, 0 });
    auto packet = TRY_OR_FAIL(ByteBuffer::copy(truncated_packet.span()));
    EXPECT(peer.read_packet(packet).is_error());
}
