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

RefPtr<Promise<Empty>> Client::connect()
{
    bool success;
    if (m_tls) {
        success = connect_tls();
    } else {
        success = connect_plaintext();
    }
    if (!success)
        return {};
    m_connect_pending = Promise<Empty>::construct();
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
    case CommandType::Logout:
        return "LOGOUT"sv.bytes();
    case CommandType ::Idle:
        return "IDLE"sv.bytes();
    case CommandType::Login:
        return "LOGIN"sv.bytes();
    case CommandType::List:
        return "LIST"sv.bytes();
    case CommandType::Select:
        return "SELECT"sv.bytes();
    case CommandType::Fetch:
        return "FETCH"sv.bytes();
    case CommandType::Store:
        return "STORE"sv.bytes();
    case CommandType::Copy:
        return "COPY"sv.bytes();
    case CommandType::Create:
        return "CREATE"sv.bytes();
    case CommandType::Delete:
        return "DELETE"sv.bytes();
    case CommandType::Search:
        return "SEARCH"sv.bytes();
    case CommandType::UIDFetch:
        return "UID FETCH"sv.bytes();
    case CommandType::UIDStore:
        return "UID STORE"sv.bytes();
    case CommandType::UIDCopy:
        return "UID COPY"sv.bytes();
    case CommandType::UIDSearch:
        return "UID SEARCH"sv.bytes();
    case CommandType::Append:
        return "APPEND"sv.bytes();
    case CommandType::Examine:
        return "EXAMINE"sv.bytes();
    case CommandType::ListSub:
        return "LSUB"sv.bytes();
    case CommandType::Expunge:
        return "EXPUNGE"sv.bytes();
    case CommandType::Subscribe:
        return "SUBSCRIBE"sv.bytes();
    case CommandType::Unsubscribe:
        return "UNSUBSCRIBE"sv.bytes();
    case CommandType::Authenticate:
        return "AUTHENTICATE"sv.bytes();
    case CommandType::Check:
        return "CHECK"sv.bytes();
    case CommandType::Close:
        return "CLOSE"sv.bytes();
    case CommandType::Rename:
        return "RENAME"sv.bytes();
    case CommandType::Status:
        return "STATUS"sv.bytes();
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

template<typename T>
RefPtr<Promise<Optional<T>>> cast_promise(RefPtr<Promise<Optional<Response>>> promise_variant)
{
    auto new_promise = promise_variant->map<Optional<T>>(
        [](Optional<Response>& variant) {
            return variant.has_value() ? move(variant->get<T>()) : Optional<T>();
        });
    return new_promise;
}

RefPtr<Promise<Optional<SolidResponse>>> Client::login(StringView username, StringView password)
{
    auto command = Command { CommandType::Login, m_current_command, { serialize_astring(username), serialize_astring(password) } };
    return cast_promise<SolidResponse>(send_command(move(command)));
}

RefPtr<Promise<Optional<SolidResponse>>> Client::list(StringView reference_name, StringView mailbox)
{
    auto command = Command { CommandType::List, m_current_command,
        { String::formatted("\"{}\"", reference_name),
            String::formatted("\"{}\"", mailbox) } };
    return cast_promise<SolidResponse>(send_command(move(command)));
}

RefPtr<Promise<Optional<SolidResponse>>> Client::lsub(StringView reference_name, StringView mailbox)
{
    auto command = Command { CommandType::ListSub, m_current_command,
        { String::formatted("\"{}\"", reference_name),
            String::formatted("\"{}\"", mailbox) } };
    return cast_promise<SolidResponse>(send_command(move(command)));
}

RefPtr<Promise<Optional<SolidResponse>>> Client::fetch(FetchCommand request, bool uid)
{
    auto command = Command { uid ? CommandType::UIDFetch : CommandType::Fetch, m_current_command, { request.serialize() } };
    return cast_promise<SolidResponse>(send_command(move(command)));
}

RefPtr<Promise<Optional<Response>>> Client::send_simple_command(CommandType type)
{
    auto command = Command { type, m_current_command, {} };
    return send_command(move(command));
}

RefPtr<Promise<Optional<SolidResponse>>> Client::select(StringView string)
{
    auto command = Command { CommandType::Select, m_current_command, { serialize_astring(string) } };
    return cast_promise<SolidResponse>(send_command(move(command)));
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

RefPtr<Promise<Optional<SolidResponse>>> Client::examine(StringView string)
{
    auto command = Command { CommandType::Examine, m_current_command, { serialize_astring(string) } };
    return cast_promise<SolidResponse>(send_command(move(command)));
}

RefPtr<Promise<Optional<SolidResponse>>> Client::create_mailbox(StringView name)
{
    auto command = Command { CommandType::Create, m_current_command, { serialize_astring(name) } };
    return cast_promise<SolidResponse>(send_command(move(command)));
}

RefPtr<Promise<Optional<SolidResponse>>> Client::delete_mailbox(StringView name)
{
    auto command = Command { CommandType::Delete, m_current_command, { serialize_astring(name) } };
    return cast_promise<SolidResponse>(send_command(move(command)));
}

RefPtr<Promise<Optional<SolidResponse>>> Client::store(StoreMethod method, Sequence sequence_set, bool silent, Vector<String> const& flags, bool uid)
{
    StringBuilder data_item_name;
    switch (method) {
    case StoreMethod::Replace:
        data_item_name.append("FLAGS");
        break;
    case StoreMethod::Add:
        data_item_name.append("+FLAGS");
        break;
    case StoreMethod::Remove:
        data_item_name.append("-FLAGS");
        break;
    }
    if (silent) {
        data_item_name.append(".SILENT");
    }

    StringBuilder flags_builder;
    flags_builder.append('(');
    flags_builder.join(" ", flags);
    flags_builder.append(')');

    auto command = Command { uid ? CommandType::UIDStore : CommandType::Store, m_current_command, { sequence_set.serialize(), data_item_name.build(), flags_builder.build() } };
    return cast_promise<SolidResponse>(send_command(move(command)));
}
RefPtr<Promise<Optional<SolidResponse>>> Client::search(Optional<String> charset, Vector<SearchKey>&& keys, bool uid)
{
    Vector<String> args;
    if (charset.has_value()) {
        args.append("CHARSET ");
        args.append(charset.value());
    }
    for (const auto& item : keys) {
        args.append(item.serialize());
    }
    auto command = Command { uid ? CommandType::UIDSearch : CommandType::Search, m_current_command, args };
    return cast_promise<SolidResponse>(send_command(move(command)));
}

RefPtr<Promise<Optional<ContinueRequest>>> Client::idle()
{
    auto promise = send_simple_command(CommandType::Idle);
    return cast_promise<ContinueRequest>(promise);
}
RefPtr<Promise<Optional<SolidResponse>>> Client::finish_idle()
{
    auto promise = Promise<Optional<Response>>::construct();
    m_pending_promises.append(promise);
    send_raw("DONE");
    m_expecting_response = true;
    return cast_promise<SolidResponse>(promise);
}

RefPtr<Promise<Optional<SolidResponse>>> Client::status(StringView mailbox, Vector<StatusItemType> const& types)
{
    Vector<String> args;
    for (auto type : types) {
        switch (type) {
        case StatusItemType::Recent:
            args.append("RECENT");
            break;
        case StatusItemType::UIDNext:
            args.append("UIDNEXT");
            break;
        case StatusItemType::UIDValidity:
            args.append("UIDVALIDITY");
            break;
        case StatusItemType::Unseen:
            args.append("UNSEEN");
            break;
        case StatusItemType::Messages:
            args.append("MESSAGES");
            break;
        }
    }
    StringBuilder types_list;
    types_list.append('(');
    types_list.join(" ", args);
    types_list.append(')');
    auto command = Command { CommandType::Status, m_current_command, { mailbox, types_list.build() } };
    return cast_promise<SolidResponse>(send_command(move(command)));
}

RefPtr<Promise<Optional<SolidResponse>>> Client::append(StringView mailbox, Message&& message, Optional<Vector<String>> flags, Optional<Core::DateTime> date_time)
{
    Vector<String> args = { mailbox };
    if (flags.has_value()) {
        StringBuilder flags_sb;
        flags_sb.append('(');
        flags_sb.join(" ", flags.value());
        flags_sb.append(')');
        args.append(flags_sb.build());
    }
    if (date_time.has_value())
        args.append(date_time.value().to_string("\"%d-%b-%Y %H:%M:%S +0000\""));

    args.append(String::formatted("{{{}}}", message.data.length()));

    auto continue_req = send_command(Command { CommandType::Append, m_current_command, args });

    auto response_promise = Promise<Optional<Response>>::construct();
    m_pending_promises.append(response_promise);

    continue_req->on_resolved = [this, message2 { move(message) }](auto& data) {
        if (!data.has_value()) {
            handle_parsed_response({ .successful = false, .response = {} });
        } else {
            send_raw(message2.data);
            m_expecting_response = true;
        }
    };

    return cast_promise<SolidResponse>(response_promise);
}
RefPtr<Promise<Optional<SolidResponse>>> Client::subscribe(StringView mailbox)
{
    auto command = Command { CommandType::Subscribe, m_current_command, { serialize_astring(mailbox) } };
    return cast_promise<SolidResponse>(send_command(move(command)));
}
RefPtr<Promise<Optional<SolidResponse>>> Client::unsubscribe(StringView mailbox)
{
    auto command = Command { CommandType::Unsubscribe, m_current_command, { serialize_astring(mailbox) } };
    return cast_promise<SolidResponse>(send_command(move(command)));
}
RefPtr<Promise<Optional<Response>>> Client::authenticate(StringView method)
{
    auto command = Command { CommandType::Authenticate, m_current_command, { method } };
    return send_command(move(command));
}
RefPtr<Promise<Optional<SolidResponse>>> Client::rename(StringView from, StringView to)
{
    auto command = Command { CommandType::Rename, m_current_command, { serialize_astring(from), serialize_astring(to) } };
    return cast_promise<SolidResponse>(send_command(move(command)));
}
RefPtr<Promise<Optional<SolidResponse>>> Client::copy(Sequence sequence_set, StringView name, bool uid)
{
    auto command = Command {
        uid ? CommandType::UIDCopy : CommandType::Copy, m_current_command, { sequence_set.serialize(), serialize_astring(name) }
    };

    return cast_promise<SolidResponse>(send_command(move(command)));
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
