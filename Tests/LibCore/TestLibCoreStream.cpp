/*
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/String.h>
#include <LibCore/BitStream.h>
#include <LibCore/EventLoop.h>
#include <LibCore/LocalServer.h>
#include <LibCore/MemoryStream.h>
#include <LibCore/Stream.h>
#include <LibCore/TCPServer.h>
#include <LibCore/Timer.h>
#include <LibCore/UDPServer.h>
#include <LibTest/TestCase.h>
#include <LibThreading/BackgroundAction.h>
#include <fcntl.h>
#include <unistd.h>

// File tests

TEST_CASE(file_open)
{
    auto maybe_file = Core::Stream::File::open("/tmp/file-open-test.txt"sv, Core::Stream::OpenMode::Write);
    if (maybe_file.is_error()) {
        warnln("Failed to open the file: {}", strerror(maybe_file.error().code()));
        VERIFY_NOT_REACHED();
    }

    // Testing out some basic file properties.
    auto file = maybe_file.release_value();
    EXPECT(file->is_open());
    EXPECT(!file->is_eof());

    auto maybe_size = file->size();
    EXPECT(!maybe_size.is_error());
    EXPECT_EQ(maybe_size.value(), 0ul);
}

TEST_CASE(file_write_bytes)
{
    auto maybe_file = Core::Stream::File::open("/tmp/file-write-bytes-test.txt"sv, Core::Stream::OpenMode::Write);
    auto file = maybe_file.release_value();

    constexpr auto some_words = "These are some words"sv;
    ReadonlyBytes buffer { some_words.characters_without_null_termination(), some_words.length() };
    auto result = file->write(buffer);
    EXPECT(!result.is_error());
}

constexpr auto expected_buffer_contents = "&lt;small&gt;(Please consider translating this message for the benefit of your fellow Wikimedians. Please also consider translating"sv;

TEST_CASE(file_read_bytes)
{
    auto maybe_file = Core::Stream::File::open("/usr/Tests/LibCore/long_lines.txt"sv, Core::Stream::OpenMode::Read);
    EXPECT(!maybe_file.is_error());
    auto file = maybe_file.release_value();

    auto maybe_buffer = ByteBuffer::create_uninitialized(131);
    EXPECT(!maybe_buffer.is_error());
    auto buffer = maybe_buffer.release_value();

    auto result = file->read(buffer);
    EXPECT(!result.is_error());
    EXPECT_EQ(result.value().size(), 131ul);

    StringView buffer_contents { buffer.bytes() };
    EXPECT_EQ(buffer_contents, expected_buffer_contents);
}

constexpr auto expected_seek_contents1 = "|Lleer esti mens"sv;
constexpr auto expected_seek_contents2 = "s of advanced ad"sv;
constexpr auto expected_seek_contents3 = "levels of advanc"sv;

TEST_CASE(file_seeking_around)
{
    auto maybe_file = Core::Stream::File::open("/usr/Tests/LibCore/long_lines.txt"sv, Core::Stream::OpenMode::Read);
    EXPECT(!maybe_file.is_error());
    auto file = maybe_file.release_value();

    EXPECT_EQ(file->size().release_value(), 8702ul);

    auto maybe_buffer = ByteBuffer::create_uninitialized(16);
    EXPECT(!maybe_buffer.is_error());
    auto buffer = maybe_buffer.release_value();

    StringView buffer_contents { buffer.bytes() };

    EXPECT(!file->seek(500, Core::Stream::SeekMode::SetPosition).is_error());
    EXPECT_EQ(file->tell().release_value(), 500ul);
    EXPECT(!file->read_entire_buffer(buffer).is_error());
    EXPECT_EQ(buffer_contents, expected_seek_contents1);

    EXPECT(!file->seek(234, Core::Stream::SeekMode::FromCurrentPosition).is_error());
    EXPECT_EQ(file->tell().release_value(), 750ul);
    EXPECT(!file->read_entire_buffer(buffer).is_error());
    EXPECT_EQ(buffer_contents, expected_seek_contents2);

    EXPECT(!file->seek(-105, Core::Stream::SeekMode::FromEndPosition).is_error());
    EXPECT_EQ(file->tell().release_value(), 8597ul);
    EXPECT(!file->read_entire_buffer(buffer).is_error());
    EXPECT_EQ(buffer_contents, expected_seek_contents3);
}

TEST_CASE(file_adopt_fd)
{
    int rc = ::open("/usr/Tests/LibCore/long_lines.txt", O_RDONLY);
    EXPECT(rc >= 0);

    auto maybe_file = Core::Stream::File::adopt_fd(rc, Core::Stream::OpenMode::Read);
    EXPECT(!maybe_file.is_error());
    auto file = maybe_file.release_value();

    EXPECT_EQ(file->size().release_value(), 8702ul);

    auto maybe_buffer = ByteBuffer::create_uninitialized(16);
    EXPECT(!maybe_buffer.is_error());
    auto buffer = maybe_buffer.release_value();

    StringView buffer_contents { buffer.bytes() };

    EXPECT(!file->seek(500, Core::Stream::SeekMode::SetPosition).is_error());
    EXPECT_EQ(file->tell().release_value(), 500ul);
    EXPECT(!file->read_entire_buffer(buffer).is_error());
    EXPECT_EQ(buffer_contents, expected_seek_contents1);

    // A single seek & read test should be fine for now.
}

TEST_CASE(file_adopt_invalid_fd)
{
    auto maybe_file = Core::Stream::File::adopt_fd(-1, Core::Stream::OpenMode::Read);
    EXPECT(maybe_file.is_error());
    EXPECT_EQ(maybe_file.error().code(), EBADF);
}

TEST_CASE(file_truncate)
{
    auto maybe_file = Core::Stream::File::open("/tmp/file-truncate-test.txt"sv, Core::Stream::OpenMode::Write);
    auto file = maybe_file.release_value();

    EXPECT(!file->truncate(999).is_error());
    EXPECT_EQ(file->size().release_value(), 999ul);

    EXPECT(!file->truncate(42).is_error());
    EXPECT_EQ(file->size().release_value(), 42ul);
}

// TCPSocket tests

TEST_CASE(should_error_when_connection_fails)
{
    // NOTE: This is required here because Core::Stream::TCPSocket requires
    //       Core::EventLoop through Core::Notifier.
    Core::EventLoop event_loop;

    auto maybe_tcp_socket = Core::Stream::TCPSocket::connect({ { 127, 0, 0, 1 }, 1234 });
    EXPECT(maybe_tcp_socket.is_error());
    EXPECT(maybe_tcp_socket.error().is_syscall());
    EXPECT(maybe_tcp_socket.error().code() == ECONNREFUSED);
}

constexpr auto sent_data = "Mr. Watson, come here. I want to see you."sv;

TEST_CASE(tcp_socket_read)
{
    // NOTE: This is required here because Core::TCPServer requires
    //       Core::EventLoop through Core::Notifier.
    Core::EventLoop event_loop;

    auto maybe_tcp_server = Core::TCPServer::try_create();
    EXPECT(!maybe_tcp_server.is_error());
    auto tcp_server = maybe_tcp_server.release_value();
    EXPECT(!tcp_server->listen({ 127, 0, 0, 1 }, 9090).is_error());
    EXPECT(!tcp_server->set_blocking(true).is_error());

    auto maybe_client_socket = Core::Stream::TCPSocket::connect({ { 127, 0, 0, 1 }, 9090 });
    EXPECT(!maybe_client_socket.is_error());
    auto client_socket = maybe_client_socket.release_value();

    EXPECT(client_socket->is_open());

    auto maybe_server_socket = tcp_server->accept();
    EXPECT(!maybe_server_socket.is_error());
    auto server_socket = maybe_server_socket.release_value();
    EXPECT(!server_socket->write({ sent_data.characters_without_null_termination(), sent_data.length() }).is_error());
    server_socket->close();

    EXPECT(client_socket->can_read_without_blocking(100).release_value());
    EXPECT_EQ(client_socket->pending_bytes().release_value(), sent_data.length());

    auto maybe_receive_buffer = ByteBuffer::create_uninitialized(64);
    EXPECT(!maybe_receive_buffer.is_error());
    auto receive_buffer = maybe_receive_buffer.release_value();
    auto maybe_read_bytes = client_socket->read(receive_buffer);
    EXPECT(!maybe_read_bytes.is_error());
    auto read_bytes = maybe_read_bytes.release_value();

    StringView received_data { read_bytes };
    EXPECT_EQ(sent_data, received_data);
}

TEST_CASE(tcp_socket_write)
{
    Core::EventLoop event_loop;

    auto maybe_tcp_server = Core::TCPServer::try_create();
    EXPECT(!maybe_tcp_server.is_error());
    auto tcp_server = maybe_tcp_server.release_value();
    EXPECT(!tcp_server->listen({ 127, 0, 0, 1 }, 9090).is_error());
    EXPECT(!tcp_server->set_blocking(true).is_error());

    auto maybe_client_socket = Core::Stream::TCPSocket::connect({ { 127, 0, 0, 1 }, 9090 });
    EXPECT(!maybe_client_socket.is_error());
    auto client_socket = maybe_client_socket.release_value();

    auto maybe_server_socket = tcp_server->accept();
    EXPECT(!maybe_server_socket.is_error());
    auto server_socket = maybe_server_socket.release_value();
    EXPECT(!server_socket->set_blocking(true).is_error());

    EXPECT(!client_socket->write_entire_buffer({ sent_data.characters_without_null_termination(), sent_data.length() }).is_error());
    client_socket->close();

    auto maybe_receive_buffer = ByteBuffer::create_uninitialized(64);
    EXPECT(!maybe_receive_buffer.is_error());
    auto receive_buffer = maybe_receive_buffer.release_value();
    auto maybe_read_bytes = server_socket->read(receive_buffer);
    EXPECT(!maybe_read_bytes.is_error());
    auto read_bytes = maybe_read_bytes.release_value();

    StringView received_data { read_bytes };
    EXPECT_EQ(sent_data, received_data);
}

TEST_CASE(tcp_socket_eof)
{
    Core::EventLoop event_loop;

    auto maybe_tcp_server = Core::TCPServer::try_create();
    EXPECT(!maybe_tcp_server.is_error());
    auto tcp_server = maybe_tcp_server.release_value();
    EXPECT(!tcp_server->listen({ 127, 0, 0, 1 }, 9090).is_error());
    EXPECT(!tcp_server->set_blocking(true).is_error());

    auto maybe_client_socket = Core::Stream::TCPSocket::connect({ { 127, 0, 0, 1 }, 9090 });
    EXPECT(!maybe_client_socket.is_error());
    auto client_socket = maybe_client_socket.release_value();

    EXPECT(client_socket->is_open());

    auto server_socket = tcp_server->accept().release_value();
    server_socket->close();

    // NOTE: This may seem unintuitive, but poll will mark a fd which has
    //       reached EOF (i.e. in the case of the other side disconnecting) as
    //       POLLIN.
    EXPECT(client_socket->can_read_without_blocking(100).release_value());
    EXPECT_EQ(client_socket->pending_bytes().release_value(), 0ul);

    auto maybe_receive_buffer = ByteBuffer::create_uninitialized(1);
    EXPECT(!maybe_receive_buffer.is_error());
    auto receive_buffer = maybe_receive_buffer.release_value();
    EXPECT(client_socket->read(receive_buffer).release_value().is_empty());
    EXPECT(client_socket->is_eof());
}

// UDPSocket tests

constexpr auto udp_reply_data = "Well hello friends!"sv;

TEST_CASE(udp_socket_read_write)
{
    // NOTE: This is required here because Core::UDPServer requires
    //       Core::EventLoop through Core::Notifier.
    Core::EventLoop event_loop;

    auto udp_server = Core::UDPServer::construct();
    EXPECT(udp_server->bind({ 127, 0, 0, 1 }, 9090));

    auto maybe_client_socket = Core::Stream::UDPSocket::connect({ { 127, 0, 0, 1 }, 9090 });
    EXPECT(!maybe_client_socket.is_error());
    auto client_socket = maybe_client_socket.release_value();

    EXPECT(client_socket->is_open());
    EXPECT(!client_socket->write_entire_buffer({ sent_data.characters_without_null_termination(), sent_data.length() }).is_error());

    // FIXME: UDPServer::receive sadly doesn't give us a way to block on it,
    // currently.
    usleep(100000);

    struct sockaddr_in client_address;
    auto server_receive_buffer_or_error = udp_server->receive(64, client_address);
    EXPECT(!server_receive_buffer_or_error.is_error());
    auto server_receive_buffer = server_receive_buffer_or_error.release_value();
    EXPECT(!server_receive_buffer.is_empty());

    StringView server_received_data { server_receive_buffer.bytes() };
    EXPECT_EQ(server_received_data, sent_data);

    EXPECT(!udp_server->send({ udp_reply_data.characters_without_null_termination(), udp_reply_data.length() }, client_address).is_error());

    EXPECT(client_socket->can_read_without_blocking(100).release_value());
    EXPECT_EQ(client_socket->pending_bytes().release_value(), udp_reply_data.length());

    // Testing that supplying a smaller buffer than required causes a failure.
    auto small_buffer = ByteBuffer::create_uninitialized(8).release_value();
    EXPECT_EQ(client_socket->read(small_buffer).error().code(), EMSGSIZE);

    auto maybe_client_receive_buffer = ByteBuffer::create_uninitialized(64);
    EXPECT(!maybe_client_receive_buffer.is_error());
    auto client_receive_buffer = maybe_client_receive_buffer.release_value();
    auto maybe_read_bytes = client_socket->read(client_receive_buffer);
    EXPECT(!maybe_read_bytes.is_error());
    auto read_bytes = maybe_read_bytes.release_value();

    StringView client_received_data { read_bytes };
    EXPECT_EQ(udp_reply_data, client_received_data);
}

// LocalSocket tests

TEST_CASE(local_socket_read)
{
    Core::EventLoop event_loop;

    auto local_server = Core::LocalServer::construct();
    EXPECT(local_server->listen("/tmp/test-socket"));

    local_server->on_accept = [&](NonnullOwnPtr<Core::Stream::LocalSocket> server_socket) {
        EXPECT(!server_socket->write(sent_data.bytes()).is_error());

        event_loop.quit(0);
        event_loop.pump();
    };

    // NOTE: Doing this on another thread, because otherwise we're at an
    //       impasse. LocalSocket::connect blocks because there's nobody to
    //       accept, and LocalServer::accept blocks because there's nobody
    //       connected.
    auto background_action = Threading::BackgroundAction<int>::construct(
        [](auto&) {
            Core::EventLoop event_loop;

            auto maybe_client_socket = Core::Stream::LocalSocket::connect("/tmp/test-socket");
            EXPECT(!maybe_client_socket.is_error());
            auto client_socket = maybe_client_socket.release_value();

            EXPECT(client_socket->is_open());

            EXPECT(client_socket->can_read_without_blocking(100).release_value());
            EXPECT_EQ(client_socket->pending_bytes().release_value(), sent_data.length());

            auto maybe_receive_buffer = ByteBuffer::create_uninitialized(64);
            EXPECT(!maybe_receive_buffer.is_error());
            auto receive_buffer = maybe_receive_buffer.release_value();
            auto maybe_read_bytes = client_socket->read(receive_buffer);
            EXPECT(!maybe_read_bytes.is_error());
            auto read_bytes = maybe_read_bytes.release_value();

            StringView received_data { read_bytes };
            EXPECT_EQ(sent_data, received_data);

            return 0;
        },
        nullptr);

    event_loop.exec();
    ::unlink("/tmp/test-socket");
}

TEST_CASE(local_socket_write)
{
    Core::EventLoop event_loop;

    auto local_server = Core::LocalServer::construct();
    EXPECT(local_server->listen("/tmp/test-socket"));

    local_server->on_accept = [&](NonnullOwnPtr<Core::Stream::LocalSocket> server_socket) {
        // NOTE: For some reason LocalServer gives us a nonblocking socket..?
        MUST(server_socket->set_blocking(true));

        EXPECT(MUST(server_socket->can_read_without_blocking(100)));
        auto pending_bytes = MUST(server_socket->pending_bytes());
        auto maybe_receive_buffer = ByteBuffer::create_uninitialized(pending_bytes);
        EXPECT(!maybe_receive_buffer.is_error());
        auto receive_buffer = maybe_receive_buffer.release_value();
        auto maybe_read_bytes = server_socket->read(receive_buffer);
        EXPECT(!maybe_read_bytes.is_error());
        EXPECT_EQ(maybe_read_bytes.value().size(), sent_data.length());

        StringView received_data { maybe_read_bytes.value() };
        EXPECT_EQ(sent_data, received_data);

        event_loop.quit(0);
        event_loop.pump();
    };

    // NOTE: Same reason as in the local_socket_read test.
    auto background_action = Threading::BackgroundAction<int>::construct(
        [](auto&) {
            auto maybe_client_socket = Core::Stream::LocalSocket::connect("/tmp/test-socket");
            EXPECT(!maybe_client_socket.is_error());
            auto client_socket = maybe_client_socket.release_value();

            EXPECT(!client_socket->write_entire_buffer({ sent_data.characters_without_null_termination(), sent_data.length() }).is_error());
            client_socket->close();

            return 0;
        },
        nullptr);

    event_loop.exec();
    ::unlink("/tmp/test-socket");
}

// Buffered stream tests

TEST_CASE(buffered_long_file_read)
{
    auto maybe_file = Core::Stream::File::open("/usr/Tests/LibCore/long_lines.txt"sv, Core::Stream::OpenMode::Read);
    EXPECT(!maybe_file.is_error());
    auto maybe_buffered_file = Core::Stream::BufferedFile::create(maybe_file.release_value());
    EXPECT(!maybe_buffered_file.is_error());
    auto file = maybe_buffered_file.release_value();

    auto buffer = ByteBuffer::create_uninitialized(4096).release_value();
    EXPECT(!file->seek(255, Core::Stream::SeekMode::SetPosition).is_error());
    EXPECT(file->can_read_line().release_value());
    auto maybe_line = file->read_line(buffer);
    EXPECT(!maybe_line.is_error());
    EXPECT_EQ(maybe_line.value().length(), 4095ul); // 4095 bytes on the third line

    // Testing that buffering with seeking works properly
    EXPECT(!file->seek(365, Core::Stream::SeekMode::SetPosition).is_error());
    auto maybe_after_seek_line = file->read_line(buffer);
    EXPECT(!maybe_after_seek_line.is_error());
    EXPECT_EQ(maybe_after_seek_line.value().length(), 3985ul); // 4095 - 110
}

TEST_CASE(buffered_small_file_read)
{
    auto maybe_file = Core::Stream::File::open("/usr/Tests/LibCore/small.txt"sv, Core::Stream::OpenMode::Read);
    EXPECT(!maybe_file.is_error());
    auto maybe_buffered_file = Core::Stream::BufferedFile::create(maybe_file.release_value());
    EXPECT(!maybe_buffered_file.is_error());
    auto file = maybe_buffered_file.release_value();

    static constexpr StringView expected_lines[] {
        "Well"sv,
        "hello"sv,
        "friends!"sv,
        ":^)"sv
    };

    // Testing that we don't read out of bounds when the entire file fits into the buffer
    auto buffer = ByteBuffer::create_uninitialized(4096).release_value();
    for (auto const& line : expected_lines) {
        VERIFY(file->can_read_line().release_value());
        auto maybe_read_line = file->read_line(buffer);
        EXPECT(!maybe_read_line.is_error());
        EXPECT_EQ(maybe_read_line.value().length(), line.length());
        EXPECT_EQ(StringView(buffer.span().trim(maybe_read_line.value().length())), line);
    }
    EXPECT(!file->can_read_line().is_error());
    EXPECT(!file->can_read_line().value());
}

TEST_CASE(buffered_file_tell_and_seek)
{
    // We choose a buffer size of 12 bytes to cover half of the input file.
    auto file = Core::Stream::File::open("/usr/Tests/LibCore/small.txt"sv, Core::Stream::OpenMode::Read).release_value();
    auto buffered_file = Core::Stream::BufferedFile::create(move(file), 12).release_value();

    // Initial state.
    {
        auto current_offset = buffered_file->tell().release_value();
        EXPECT_EQ(current_offset, 0ul);
    }

    // Read a character.
    {
        auto character = buffered_file->read_value<char>().release_value();
        EXPECT_EQ(character, 'W');
        auto current_offset = buffered_file->tell().release_value();
        EXPECT_EQ(current_offset, 1ul);
    }

    // Read one more character.
    {
        auto character = buffered_file->read_value<char>().release_value();
        EXPECT_EQ(character, 'e');
        auto current_offset = buffered_file->tell().release_value();
        EXPECT_EQ(current_offset, 2ul);
    }

    // Seek seven characters forward.
    {
        auto current_offset = buffered_file->seek(7, Core::Stream::SeekMode::FromCurrentPosition).release_value();
        EXPECT_EQ(current_offset, 9ul);
    }

    // Read a character again.
    {
        auto character = buffered_file->read_value<char>().release_value();
        EXPECT_EQ(character, 'o');
        auto current_offset = buffered_file->tell().release_value();
        EXPECT_EQ(current_offset, 10ul);
    }

    // Seek five characters backwards.
    {
        auto current_offset = buffered_file->seek(-5, Core::Stream::SeekMode::FromCurrentPosition).release_value();
        EXPECT_EQ(current_offset, 5ul);
    }

    // Read a character.
    {
        auto character = buffered_file->read_value<char>().release_value();
        EXPECT_EQ(character, 'h');
        auto current_offset = buffered_file->tell().release_value();
        EXPECT_EQ(current_offset, 6ul);
    }

    // Seek back to the beginning.
    {
        auto current_offset = buffered_file->seek(0, Core::Stream::SeekMode::SetPosition).release_value();
        EXPECT_EQ(current_offset, 0ul);
    }

    // Read the first character. This should prime the buffer if it hasn't happened already.
    {
        auto character = buffered_file->read_value<char>().release_value();
        EXPECT_EQ(character, 'W');
        auto current_offset = buffered_file->tell().release_value();
        EXPECT_EQ(current_offset, 1ul);
    }

    // Seek beyond the buffer size, which should invalidate the buffer.
    {
        auto current_offset = buffered_file->seek(12, Core::Stream::SeekMode::SetPosition).release_value();
        EXPECT_EQ(current_offset, 12ul);
    }

    // Ensure that we still read the correct contents from the new offset with a (presumably) freshly filled buffer.
    {
        auto character = buffered_file->read_value<char>().release_value();
        EXPECT_EQ(character, 'r');
        auto current_offset = buffered_file->tell().release_value();
        EXPECT_EQ(current_offset, 13ul);
    }
}

constexpr auto buffered_sent_data = "Well hello friends!\n:^)\nThis shouldn't be present. :^("sv;
constexpr auto first_line = "Well hello friends!"sv;
constexpr auto second_line = ":^)"sv;

TEST_CASE(buffered_tcp_socket_read)
{
    Core::EventLoop event_loop;

    auto maybe_tcp_server = Core::TCPServer::try_create();
    EXPECT(!maybe_tcp_server.is_error());
    auto tcp_server = maybe_tcp_server.release_value();
    EXPECT(!tcp_server->listen({ 127, 0, 0, 1 }, 9090).is_error());
    EXPECT(!tcp_server->set_blocking(true).is_error());

    auto maybe_client_socket = Core::Stream::TCPSocket::connect({ { 127, 0, 0, 1 }, 9090 });
    EXPECT(!maybe_client_socket.is_error());
    auto maybe_buffered_socket = Core::Stream::BufferedTCPSocket::create(maybe_client_socket.release_value());
    EXPECT(!maybe_buffered_socket.is_error());
    auto client_socket = maybe_buffered_socket.release_value();

    EXPECT(client_socket->is_open());

    auto maybe_server_socket = tcp_server->accept();
    EXPECT(!maybe_server_socket.is_error());
    auto server_socket = maybe_server_socket.release_value();
    EXPECT(!server_socket->write({ buffered_sent_data.characters_without_null_termination(), sent_data.length() }).is_error());

    EXPECT(client_socket->can_read_without_blocking(100).release_value());

    auto receive_buffer = ByteBuffer::create_uninitialized(64).release_value();

    auto maybe_first_received_line = client_socket->read_line(receive_buffer);
    EXPECT(!maybe_first_received_line.is_error());
    auto first_received_line = maybe_first_received_line.value();
    EXPECT_EQ(first_received_line, first_line);

    auto maybe_second_received_line = client_socket->read_line(receive_buffer);
    EXPECT(!maybe_second_received_line.is_error());
    auto second_received_line = maybe_second_received_line.value();
    EXPECT_EQ(second_received_line, second_line);
}

// Allocating memory stream tests

TEST_CASE(allocating_memory_stream_empty)
{
    Core::Stream::AllocatingMemoryStream stream;

    EXPECT_EQ(stream.used_buffer_size(), 0ul);

    {
        Array<u8, 32> array;
        auto read_bytes = MUST(stream.read(array));
        EXPECT_EQ(read_bytes.size(), 0ul);
    }

    {
        auto offset = MUST(stream.offset_of("test"sv.bytes()));
        EXPECT(!offset.has_value());
    }
}

TEST_CASE(allocating_memory_stream_offset_of)
{
    Core::Stream::AllocatingMemoryStream stream;
    MUST(stream.write_entire_buffer("Well Hello Friends! :^)"sv.bytes()));

    {
        auto offset = MUST(stream.offset_of(" "sv.bytes()));
        EXPECT(offset.has_value());
        EXPECT_EQ(offset.value(), 4ul);
    }

    {
        auto offset = MUST(stream.offset_of("W"sv.bytes()));
        EXPECT(offset.has_value());
        EXPECT_EQ(offset.value(), 0ul);
    }

    {
        auto offset = MUST(stream.offset_of(")"sv.bytes()));
        EXPECT(offset.has_value());
        EXPECT_EQ(offset.value(), 22ul);
    }

    {
        auto offset = MUST(stream.offset_of("-"sv.bytes()));
        EXPECT(!offset.has_value());
    }

    MUST(stream.discard(1));

    {
        auto offset = MUST(stream.offset_of("W"sv.bytes()));
        EXPECT(!offset.has_value());
    }

    {
        auto offset = MUST(stream.offset_of("e"sv.bytes()));
        EXPECT(offset.has_value());
        EXPECT_EQ(offset.value(), 0ul);
    }
}

TEST_CASE(allocating_memory_stream_10kb)
{
    auto file = MUST(Core::Stream::File::open("/usr/Tests/LibCore/10kb.txt"sv, Core::Stream::OpenMode::Read));
    size_t const file_size = MUST(file->size());
    size_t constexpr test_chunk_size = 4096;

    // Read file contents into the memory stream.
    Core::Stream::AllocatingMemoryStream stream;
    while (!file->is_eof()) {
        Array<u8, test_chunk_size> array;
        MUST(stream.write(MUST(file->read(array))));
    }

    EXPECT_EQ(stream.used_buffer_size(), file_size);

    MUST(file->seek(0, Core::Stream::SeekMode::SetPosition));

    // Check the stream contents when reading back.
    size_t offset = 0;
    while (!file->is_eof()) {
        Array<u8, test_chunk_size> file_array;
        Array<u8, test_chunk_size> stream_array;
        auto file_span = MUST(file->read(file_array));
        auto stream_span = MUST(stream.read(stream_array));
        EXPECT_EQ(file_span.size(), stream_span.size());

        for (size_t i = 0; i < file_span.size(); i++) {
            if (file_array[i] == stream_array[i])
                continue;

            FAIL(String::formatted("Data started to diverge at index {}: file={}, stream={}", offset + i, file_array[i], stream_array[i]));
        }
        offset += file_span.size();
    }
}

// Bit stream tests

// Note: This does not do any checks on the internal representation, it just ensures that the behavior of the input and output streams match.
TEST_CASE(little_endian_bit_stream_input_output_match)
{
    auto memory_stream = make<Core::Stream::AllocatingMemoryStream>();

    // Note: The bit stream only ever reads from/writes to the underlying stream in one byte chunks,
    // so testing with sizes that will not trigger a write will yield unexpected results.
    auto bit_write_stream = MUST(Core::Stream::LittleEndianOutputBitStream::construct(Core::Stream::Handle<Core::Stream::Stream>(*memory_stream)));
    auto bit_read_stream = MUST(Core::Stream::LittleEndianInputBitStream::construct(Core::Stream::Handle<Core::Stream::Stream>(*memory_stream)));

    // Test two mirrored chunks of a fully mirrored pattern to check that we are not dropping bits.
    {
        MUST(bit_write_stream->write_bits(0b1111u, 4));
        MUST(bit_write_stream->write_bits(0b1111u, 4));
        auto result = MUST(bit_read_stream->read_bits(4));
        EXPECT_EQ(0b1111u, result);
        result = MUST(bit_read_stream->read_bits(4));
        EXPECT_EQ(0b1111u, result);
    }
    {
        MUST(bit_write_stream->write_bits(0b0000u, 4));
        MUST(bit_write_stream->write_bits(0b0000u, 4));
        auto result = MUST(bit_read_stream->read_bits(4));
        EXPECT_EQ(0b0000u, result);
        result = MUST(bit_read_stream->read_bits(4));
        EXPECT_EQ(0b0000u, result);
    }

    // Test two mirrored chunks of a non-mirrored pattern to check that we are writing bits within a pattern in the correct order.
    {
        MUST(bit_write_stream->write_bits(0b1000u, 4));
        MUST(bit_write_stream->write_bits(0b1000u, 4));
        auto result = MUST(bit_read_stream->read_bits(4));
        EXPECT_EQ(0b1000u, result);
        result = MUST(bit_read_stream->read_bits(4));
        EXPECT_EQ(0b1000u, result);
    }

    // Test two different chunks to check that we are not confusing their order.
    {
        MUST(bit_write_stream->write_bits(0b1000u, 4));
        MUST(bit_write_stream->write_bits(0b0100u, 4));
        auto result = MUST(bit_read_stream->read_bits(4));
        EXPECT_EQ(0b1000u, result);
        result = MUST(bit_read_stream->read_bits(4));
        EXPECT_EQ(0b0100u, result);
    }

    // Test a pattern that spans multiple bytes.
    {
        MUST(bit_write_stream->write_bits(0b1101001000100001u, 16));
        auto result = MUST(bit_read_stream->read_bits(16));
        EXPECT_EQ(0b1101001000100001u, result);
    }
}

// Note: This does not do any checks on the internal representation, it just ensures that the behavior of the input and output streams match.
TEST_CASE(big_endian_bit_stream_input_output_match)
{
    auto memory_stream = make<Core::Stream::AllocatingMemoryStream>();

    // Note: The bit stream only ever reads from/writes to the underlying stream in one byte chunks,
    // so testing with sizes that will not trigger a write will yield unexpected results.
    auto bit_write_stream = MUST(Core::Stream::BigEndianOutputBitStream::construct(Core::Stream::Handle<Core::Stream::Stream>(*memory_stream)));
    auto bit_read_stream = MUST(Core::Stream::BigEndianInputBitStream::construct(Core::Stream::Handle<Core::Stream::Stream>(*memory_stream)));

    // Test two mirrored chunks of a fully mirrored pattern to check that we are not dropping bits.
    {
        MUST(bit_write_stream->write_bits(0b1111u, 4));
        MUST(bit_write_stream->write_bits(0b1111u, 4));
        auto result = MUST(bit_read_stream->read_bits(4));
        EXPECT_EQ(0b1111u, result);
        result = MUST(bit_read_stream->read_bits(4));
        EXPECT_EQ(0b1111u, result);
    }
    {
        MUST(bit_write_stream->write_bits(0b0000u, 4));
        MUST(bit_write_stream->write_bits(0b0000u, 4));
        auto result = MUST(bit_read_stream->read_bits(4));
        EXPECT_EQ(0b0000u, result);
        result = MUST(bit_read_stream->read_bits(4));
        EXPECT_EQ(0b0000u, result);
    }

    // Test two mirrored chunks of a non-mirrored pattern to check that we are writing bits within a pattern in the correct order.
    {
        MUST(bit_write_stream->write_bits(0b1000u, 4));
        MUST(bit_write_stream->write_bits(0b1000u, 4));
        auto result = MUST(bit_read_stream->read_bits(4));
        EXPECT_EQ(0b1000u, result);
        result = MUST(bit_read_stream->read_bits(4));
        EXPECT_EQ(0b1000u, result);
    }

    // Test two different chunks to check that we are not confusing their order.
    {
        MUST(bit_write_stream->write_bits(0b1000u, 4));
        MUST(bit_write_stream->write_bits(0b0100u, 4));
        auto result = MUST(bit_read_stream->read_bits(4));
        EXPECT_EQ(0b1000u, result);
        result = MUST(bit_read_stream->read_bits(4));
        EXPECT_EQ(0b0100u, result);
    }

    // Test a pattern that spans multiple bytes.
    {
        MUST(bit_write_stream->write_bits(0b1101001000100001u, 16));
        auto result = MUST(bit_read_stream->read_bits(16));
        EXPECT_EQ(0b1101001000100001u, result);
    }
}
