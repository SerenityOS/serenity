/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Client.h"

#include <AK/ByteBuffer.h>
#include <AK/MemoryStream.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <LibCore/EventLoop.h>
#include <LibCore/Notifier.h>
#include <LibCore/Socket.h>
#include <stdio.h>
#include <unistd.h>

Client::Client(int id, NonnullOwnPtr<Core::TCPSocket> socket, int ptm_fd)
    : m_id(id)
    , m_socket(move(socket))
    , m_ptm_fd(ptm_fd)
    , m_ptm_notifier(Core::Notifier::construct(ptm_fd, Core::Notifier::Type::Read))
{
    m_socket->on_ready_to_read = [this] {
        auto result = drain_socket();
        if (result.is_error()) {
            dbgln("Failed to drain the socket: {}", result.error());
            Core::deferred_invoke([this, strong_this = NonnullRefPtr(*this)] { quit(); });
        }
    };

    m_ptm_notifier->on_activation = [this] {
        auto result = drain_pty();
        if (result.is_error()) {
            dbgln("Failed to drain the PTY: {}", result.error());
            Core::deferred_invoke([this, strong_this = NonnullRefPtr(*this)] { quit(); });
        }
    };

    m_parser.on_command = [this](Command const& command) {
        auto result = handle_command(command);
        if (result.is_error()) {
            dbgln("Failed to handle the command: {}", result.error());
            Core::deferred_invoke([this, strong_this = NonnullRefPtr(*this)] { quit(); });
        }
    };

    m_parser.on_data = [this](StringView data) { handle_data(data); };
    m_parser.on_error = [this]() { handle_error(); };
}

ErrorOr<NonnullRefPtr<Client>> Client::create(int id, NonnullOwnPtr<Core::TCPSocket> socket, int ptm_fd)
{
    auto client = adopt_ref(*new Client(id, move(socket), ptm_fd));

    auto result = client->send_commands({
        { CMD_WILL, SUB_SUPPRESS_GO_AHEAD },
        { CMD_WILL, SUB_ECHO },
        { CMD_DO, SUB_SUPPRESS_GO_AHEAD },
        { CMD_DONT, SUB_ECHO },
    });
    if (result.is_error()) {
        client->quit();
        return result.release_error();
    }

    return client;
}

ErrorOr<void> Client::drain_socket()
{
    NonnullRefPtr<Client> protect(*this);

    auto buffer = TRY(ByteBuffer::create_uninitialized(1024));

    while (TRY(m_socket->can_read_without_blocking())) {
        auto read_bytes = TRY(m_socket->read_some(buffer));

        m_parser.write(StringView { read_bytes });

        if (m_socket->is_eof()) {
            Core::deferred_invoke([this, strong_this = NonnullRefPtr(*this)] { quit(); });
            break;
        }
    }

    return {};
}

ErrorOr<void> Client::drain_pty()
{
    u8 buffer[BUFSIZ];
    ssize_t nread = read(m_ptm_fd, buffer, sizeof(buffer));
    if (nread < 0) {
        Core::deferred_invoke([this, strong_this = NonnullRefPtr(*this)] { quit(); });
        return static_cast<ErrnoCode>(errno);
    }
    if (nread == 0) {
        Core::deferred_invoke([this, strong_this = NonnullRefPtr(*this)] { quit(); });
        return {};
    }

    return send_data({ buffer, (size_t)nread });
}

void Client::handle_data(StringView data)
{
    write(m_ptm_fd, data.characters_without_null_termination(), data.length());
}

ErrorOr<void> Client::handle_command(Command const& command)
{
    switch (command.command) {
    case CMD_DO:
        // no response - we've already advertised our options, and none of
        // them can be disabled (or re-enabled) after connecting.
        break;
    case CMD_DONT:
        // no response - we only "support" two options (echo and suppress
        // go-ahead), and both of them are always enabled.
        break;
    case CMD_WILL:
        switch (command.subcommand) {
        case SUB_ECHO:
            // we always want to be the ones in control of the output. tell
            // the client to disable local echo.
            TRY(send_command({ CMD_DONT, SUB_ECHO }));
            break;
        case SUB_SUPPRESS_GO_AHEAD:
            TRY(send_command({ CMD_DO, SUB_SUPPRESS_GO_AHEAD }));
            break;
        default:
            // don't respond to unknown commands
            break;
        }
        break;
    case CMD_WONT:
        // no response - we don't care about anything the client says they
        // won't do.
        break;
    }

    return {};
}

void Client::handle_error()
{
    Core::deferred_invoke([this, strong_this = NonnullRefPtr(*this)] { quit(); });
}

ErrorOr<void> Client::send_data(StringView data)
{
    bool fast = true;
    for (size_t i = 0; i < data.length(); i++) {
        u8 c = data[i];
        if (c == '\n' || c == 0xff)
            fast = false;
    }

    if (fast) {
        TRY(m_socket->write_until_depleted({ data.characters_without_null_termination(), data.length() }));
        return {};
    }

    StringBuilder builder;
    for (size_t i = 0; i < data.length(); i++) {
        u8 c = data[i];

        switch (c) {
        case '\n':
            builder.append("\r\n"sv);
            break;
        case IAC:
            builder.append("\xff\xff"sv);
            break;
        default:
            builder.append(c);
            break;
        }
    }

    auto builder_contents = TRY(builder.to_byte_buffer());
    TRY(m_socket->write_until_depleted(builder_contents));
    return {};
}

ErrorOr<void> Client::send_command(Command command)
{
    return send_commands({ command });
}

ErrorOr<void> Client::send_commands(Vector<Command> commands)
{
    auto buffer = TRY(ByteBuffer::create_uninitialized(commands.size() * 3));
    FixedMemoryStream stream { buffer.span() };

    for (auto& command : commands) {
        MUST(stream.write_value<u8>(IAC));
        MUST(stream.write_value(command.command));
        MUST(stream.write_value(command.subcommand));
    }

    VERIFY(TRY(stream.tell()) == buffer.size());
    TRY(m_socket->write_until_depleted({ buffer.data(), buffer.size() }));
    return {};
}

void Client::quit()
{
    m_ptm_notifier->set_enabled(false);
    close(m_ptm_fd);
    m_socket->close();
    if (on_exit)
        on_exit();
}
