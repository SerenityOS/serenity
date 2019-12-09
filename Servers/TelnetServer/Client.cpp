#include "Client.h"
#include <AK/BufferStream.h>
#include <AK/ByteBuffer.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <LibCore/CNotifier.h>
#include <LibCore/CTCPSocket.h>
#include <stdio.h>
#include <unistd.h>

Client::Client(int id, RefPtr<CTCPSocket> socket, int ptm_fd)
    : m_id(id)
    , m_socket(move(socket))
    , m_ptm_fd(ptm_fd)
    , m_ptm_notifier(CNotifier::construct(ptm_fd, CNotifier::Read))
{
    m_socket->on_ready_to_read = [this] { drain_socket(); };
    m_ptm_notifier->on_ready_to_read = [this] { drain_pty(); };
    m_parser.on_command = [this](const Command& command) { handle_command(command); };
    m_parser.on_data = [this](const StringView& data) { handle_data(data); };
    m_parser.on_error = [this]() { handle_error(); };
    send_commands({
        { CMD_WILL, SUB_SUPPRESS_GO_AHEAD },
        { CMD_WILL, SUB_ECHO },
        { CMD_DO, SUB_SUPPRESS_GO_AHEAD },
        { CMD_DONT, SUB_ECHO },
    });
}

void Client::drain_socket()
{
    while (m_socket->can_read()) {
        auto buf = m_socket->read(1024);

        m_parser.write(buf);

        if (m_socket->eof()) {
            quit();
            break;
        }
    }
}

void Client::drain_pty()
{
    u8 buffer[BUFSIZ];
    ssize_t nread = read(m_ptm_fd, buffer, sizeof(buffer));
    if (nread < 0) {
        perror("read(ptm)");
        quit();
        return;
    }
    if (nread == 0) {
        quit();
        return;
    }
    send_data(StringView(buffer, (size_t)nread));
}

void Client::handle_data(const StringView& data)
{
    write(m_ptm_fd, data.characters_without_null_termination(), data.length());
}

void Client::handle_command(const Command& command)
{
    switch (command.command) {
    case CMD_DO:
        // no response - we've already advertised our options, and none of
        // them can be disabled (or re-enabled) after connecting.
        break;
    case CMD_DONT:
        // no response - we only "support" two options (echo and suppres
        // go-ahead), and both of them are always enabled.
        break;
    case CMD_WILL:
        switch (command.subcommand) {
        case SUB_ECHO:
            // we always want to be the ones in control of the output. tell
            // the client to disable local echo.
            send_command({ CMD_DONT, SUB_ECHO });
            break;
        case SUB_SUPPRESS_GO_AHEAD:
            send_command({ CMD_DO, SUB_SUPPRESS_GO_AHEAD });
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
}

void Client::handle_error()
{
    quit();
}

void Client::send_data(StringView data)
{
    bool fast = true;
    for (size_t i = 0; i < data.length(); i++) {
        u8 c = data[i];
        if (c == '\n' || c == 0xff)
            fast = false;
    }

    if (fast) {
        m_socket->write(data);
        return;
    }

    StringBuilder builder;
    for (size_t i = 0; i < data.length(); i++) {
        u8 c = data[i];

        switch (c) {
        case '\n':
            builder.append("\r\n");
            break;
        case IAC:
            builder.append("\xff\xff");
            break;
        default:
            builder.append(c);
            break;
        }
    }

    m_socket->write(builder.to_string());
}

void Client::send_command(Command command)
{
    send_commands({ command });
}

void Client::send_commands(Vector<Command> commands)
{
    auto buffer = ByteBuffer::create_uninitialized(commands.size() * 3);
    BufferStream stream(buffer);
    for (auto& command : commands)
        stream << (u8)IAC << command.command << command.subcommand;
    stream.snip();
    m_socket->write(buffer.data(), buffer.size());
}

void Client::quit()
{
    m_ptm_notifier->set_enabled(false);
    close(m_ptm_fd);
    m_socket->close();
    if (on_exit)
        on_exit();
}
