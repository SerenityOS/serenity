/*
 * Copyright (c) 2021, Kyle Pereira <hey@xylepereira.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibIMAP/Client.h>

namespace IMAP {
Client::Client(StringView host, unsigned int port, bool start_with_tls)
    : m_host(host)
    , m_port(port)
    , m_tls(start_with_tls)
    , m_parser(Parser())
{
    if (start_with_tls) {
        m_tls_socket = TLS::TLSv12::construct(nullptr);
        m_tls_socket->set_root_certificates(DefaultRootCACertificates::the().certificates());
    } else {
        m_socket = Core::TCPSocket::construct();
    }
}

Optional<RefPtr<Promise<Empty>>> Client::connect()
{
    bool success;
    if (m_tls) {
        success = connect_tls();
    } else {
        success = connect_plaintext();
    }
    if (!success)
        return {};
    m_connect_pending = new Promise<bool> {};
    return m_connect_pending;
}

bool Client::connect_tls()
{
    m_tls_socket->on_tls_ready_to_read = [&](TLS::TLSv12&) {
        on_tls_ready_to_receive();
    };
    m_tls_socket->on_tls_error = [&](TLS::AlertDescription alert) {
        dbgln("failed: {}", alert_name(alert));
    };
    m_tls_socket->on_tls_connected = [&] {
        dbgln("connected");
    };
    auto success = m_tls_socket->connect(m_host, m_port);
    dbgln("connecting to {}:{} {}", m_host, m_port, success);
    return success;
}

bool Client::connect_plaintext()
{
    m_socket->on_ready_to_read = [&] {
        on_ready_to_receive();
    };
    auto success = m_socket->connect(m_host, m_port);
    dbgln("connecting to {}:{} {}", m_host, m_port, success);
    return success;
}

void Client::on_tls_ready_to_receive()
{
    if (!m_tls_socket->can_read())
        return;
    auto data = m_tls_socket->read();
    if (!data.has_value())
        return;

    // Once we get server hello we can start sending
    if (m_connect_pending) {
        m_connect_pending->resolve({});
        m_connect_pending.clear();
        return;
    }

    m_buffer += data.value();
    if (m_buffer[m_buffer.size() - 1] == '\n') {
        // Don't try parsing until we have a complete line.
        auto response = m_parser.parse(move(m_buffer), m_expecting_response);
        handle_parsed_response(move(response));
        m_buffer.clear();
    }
}

void Client::on_ready_to_receive()
{
    if (!m_socket->can_read())
        return;
    m_buffer += m_socket->read_all();

    // Once we get server hello we can start sending.
    if (m_connect_pending) {
        m_connect_pending->resolve({});
        m_connect_pending.clear();
        m_buffer.clear();
        return;
    }

    if (m_buffer[m_buffer.size() - 1] == '\n') {
        // Don't try parsing until we have a complete line.
        auto response = m_parser.parse(move(m_buffer), m_expecting_response);
        handle_parsed_response(move(response));
        m_buffer.clear();
    }
}

static ReadonlyBytes command_byte_buffer(CommandType command)
{
    switch (command) {
    case CommandType::Noop:
        return "NOOP"sv.bytes();
    case CommandType::Capability:
        return "CAPABILITY"sv.bytes();
    }
    VERIFY_NOT_REACHED();
}

void Client::send_raw(StringView data)
{
    if (m_tls) {
        m_tls_socket->write(data.bytes());
        m_tls_socket->write("\r\n"sv.bytes());
    } else {
        m_socket->write(data.bytes());
        m_socket->write("\r\n"sv.bytes());
    }
}

RefPtr<Promise<Optional<Response>>> Client::send_command(Command&& command)
{
    m_command_queue.append(move(command));
    m_current_command++;

    auto promise = Promise<Optional<Response>>::construct();
    m_pending_promises.append(promise);

    if (m_pending_promises.size() == 1)
        send_next_command();

    return promise;
}

RefPtr<Promise<Optional<Response>>> Client::send_simple_command(CommandType type)
{
    auto command = Command { type, m_current_command, {} };
    return send_command(move(command));
}

void Client::handle_parsed_response(ParseStatus&& parse_status)
{
    if (!m_expecting_response) {
        if (!parse_status.successful) {
            dbgln("Parsing failed on unrequested data!");
        } else if (parse_status.response.has_value()) {
            unrequested_response_callback(move(parse_status.response.value().get<SolidResponse>().data()));
        }
    } else {
        bool should_send_next = false;
        if (!parse_status.successful) {
            m_expecting_response = false;
            m_pending_promises.first()->resolve({});
            m_pending_promises.remove(0);
        }
        if (parse_status.response.has_value()) {
            m_expecting_response = false;
            should_send_next = parse_status.response->has<SolidResponse>();
            m_pending_promises.first()->resolve(move(parse_status.response));
            m_pending_promises.remove(0);
        }

        if (should_send_next && !m_command_queue.is_empty()) {
            send_next_command();
        }
    }
}

void Client::send_next_command()
{
    auto command = m_command_queue.take_first();
    ByteBuffer buffer;
    auto tag = AK::String::formatted("A{} ", m_current_command);
    buffer += tag.to_byte_buffer();
    auto command_type = command_byte_buffer(command.type);
    buffer.append(command_type.data(), command_type.size());

    for (auto& arg : command.args) {
        buffer.append(" ", 1);
        buffer.append(arg.bytes().data(), arg.length());
    }

    send_raw(buffer);
    m_expecting_response = true;
}

void Client::close()
{
    if (m_tls) {
        m_tls_socket->close();
    } else {
        m_socket->close();
    }
}
}
