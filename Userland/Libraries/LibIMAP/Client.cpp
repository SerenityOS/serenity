/*
 * Copyright (c) 2021, Kyle Pereira <hey@xylepereira.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/OwnPtr.h>
#include <LibIMAP/Client.h>

namespace IMAP {

Client::Client(StringView host, u16 port, NonnullOwnPtr<Core::Socket> socket)
    : m_host(host)
    , m_port(port)
    , m_socket(move(socket))
    , m_connect_pending(Promise<Empty>::construct())
{
    setup_callbacks();
}

Client::Client(Client&& other)
    : m_host(other.m_host)
    , m_port(other.m_port)
    , m_socket(move(other.m_socket))
    , m_connect_pending(move(other.m_connect_pending))
{
    setup_callbacks();
}

void Client::setup_callbacks()
{
    m_socket->on_ready_to_read = [&] {
        auto maybe_error = on_ready_to_receive();
        if (maybe_error.is_error()) {
            dbgln("Error receiving from the socket: {}", maybe_error.error());
            close();
        }
    };
}

ErrorOr<NonnullOwnPtr<Client>> Client::connect_tls(StringView host, u16 port)
{
    auto tls_socket = TRY(TLS::TLSv12::connect(host, port));
    dbgln("connecting to {}:{}", host, port);

    return adopt_nonnull_own_or_enomem(new (nothrow) Client(host, port, move(tls_socket)));
}

ErrorOr<NonnullOwnPtr<Client>> Client::connect_plaintext(StringView host, u16 port)
{
    auto socket = TRY(Core::TCPSocket::connect(host, port));
    dbgln("Connected to {}:{}", host, port);
    return adopt_nonnull_own_or_enomem(new (nothrow) Client(host, port, move(socket)));
}

bool Client::verify_response_is_complete()
{
    // FIXME: This is still more of a heuristic than a proper approach.
    //        I would imagine this breaks if we happen to get an email that
    //        contains this pattern we're looking for.
    dbgln("Waiting for a complete IMAP response, buffer size is now {}", m_buffer.size());
    Vector<StringView> statuses = { "OK"sv, "BAD"sv, "NO"sv };
    auto slice_size = m_buffer.size() >= 100 ? 100 : m_buffer.size(); // Arbitrary slice size, should contain what we're looking for.
    auto slice_data = MUST(m_buffer.slice(m_buffer.size() - slice_size, slice_size));
    StringView slice = StringView(slice_data);
    for (auto status : statuses) {
        ByteString pattern = ByteString::formatted("A{} {}", m_current_command, status);
        if (slice.contains(pattern)) {
            dbgln("IMAP server replied {}, sending to parser", pattern);
            return true;
        }
    }
    return false;
}

ErrorOr<void> Client::on_ready_to_receive()
{
    if (!TRY(m_socket->can_read_without_blocking()))
        return {};

    auto pending_bytes = TRY(m_socket->pending_bytes());
    auto receive_buffer = TRY(m_buffer.get_bytes_for_writing(pending_bytes));
    TRY(m_socket->read_until_filled(receive_buffer));

    // Once we get server hello we can start sending.
    if (m_connect_pending) {
        m_connect_pending->resolve({});
        m_connect_pending.clear();
        m_buffer.clear();
        return {};
    }

    // Don't try parsing until we have a complete response.
    if (verify_response_is_complete()) {
        auto response = m_parser.parse(move(m_buffer), m_expecting_response);
        TRY(handle_parsed_response(move(response)));
        m_buffer.clear();
    }

    return {};
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

ErrorOr<void> Client::send_raw(StringView data)
{
    TRY(m_socket->write_until_depleted(data.bytes()));
    TRY(m_socket->write_until_depleted("\r\n"sv.bytes()));

    return {};
}

NonnullRefPtr<Promise<Response>> Client::send_command(Command&& command)
{
    m_command_queue.append(move(command));
    m_current_command++;

    auto promise = Promise<Response>::construct();
    m_pending_promises.append(promise);

    if (m_pending_promises.size() == 1) {
        auto maybe_error = send_next_command();
        if (maybe_error.is_error())
            promise->reject(maybe_error.release_error());
    }

    return promise;
}

template<typename T>
NonnullRefPtr<Promise<T>> cast_promise(NonnullRefPtr<Promise<Response>> promise_variant)
{
    auto new_promise = promise_variant->map<T>(
        [](Response& variant) {
            return move(variant.get<T>());
        });
    return new_promise;
}

NonnullRefPtr<Promise<SolidResponse>> Client::login(StringView username, StringView password)
{
    auto command = Command { CommandType::Login, m_current_command, { serialize_astring(username), serialize_astring(password) } };
    return cast_promise<SolidResponse>(send_command(move(command)));
}

NonnullRefPtr<Promise<SolidResponse>> Client::list(StringView reference_name, StringView mailbox, bool unseen)
{
    auto command = Command { CommandType::List, m_current_command,
        { ByteString::formatted("\"{}\"", reference_name),
            ByteString::formatted("\"{}\"", mailbox),
            unseen ? "RETURN (STATUS (UNSEEN))" : "" } };
    return cast_promise<SolidResponse>(send_command(move(command)));
}

NonnullRefPtr<Promise<SolidResponse>> Client::lsub(StringView reference_name, StringView mailbox)
{
    auto command = Command { CommandType::ListSub, m_current_command,
        { ByteString::formatted("\"{}\"", reference_name),
            ByteString::formatted("\"{}\"", mailbox) } };
    return cast_promise<SolidResponse>(send_command(move(command)));
}

NonnullRefPtr<Promise<SolidResponse>> Client::fetch(FetchCommand request, bool uid)
{
    auto command = Command { uid ? CommandType::UIDFetch : CommandType::Fetch, m_current_command, { request.serialize() } };
    return cast_promise<SolidResponse>(send_command(move(command)));
}

NonnullRefPtr<Promise<Response>> Client::send_simple_command(CommandType type)
{
    auto command = Command { type, m_current_command, {} };
    return send_command(move(command));
}

NonnullRefPtr<Promise<SolidResponse>> Client::select(StringView string)
{
    auto command = Command { CommandType::Select, m_current_command, { serialize_astring(string) } };
    return cast_promise<SolidResponse>(send_command(move(command)));
}

ErrorOr<void> Client::handle_parsed_response(ParseStatus&& parse_status)
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
            m_pending_promises.take_first()->reject(Error::from_string_literal("Failed to parse message"));
        }
        if (parse_status.response.has_value()) {
            m_expecting_response = false;
            should_send_next = parse_status.response->has<SolidResponse>();
            m_pending_promises.take_first()->resolve(parse_status.response.release_value());
        }

        if (should_send_next && !m_command_queue.is_empty()) {
            TRY(send_next_command());
        }
    }

    return {};
}

ErrorOr<void> Client::send_next_command()
{
    auto command = m_command_queue.take_first();
    ByteBuffer buffer;
    auto tag = AK::ByteString::formatted("A{} ", m_current_command);
    buffer += tag.to_byte_buffer();
    auto command_type = command_byte_buffer(command.type);
    buffer.append(command_type.data(), command_type.size());

    for (auto& arg : command.args) {
        if (arg.length()) {
            buffer.append(" ", 1);
            buffer.append(arg.bytes().data(), arg.length());
        }
    }

    TRY(send_raw(buffer));
    m_expecting_response = true;
    return {};
}

NonnullRefPtr<Promise<SolidResponse>> Client::examine(StringView string)
{
    auto command = Command { CommandType::Examine, m_current_command, { serialize_astring(string) } };
    return cast_promise<SolidResponse>(send_command(move(command)));
}

NonnullRefPtr<Promise<SolidResponse>> Client::create_mailbox(StringView name)
{
    auto command = Command { CommandType::Create, m_current_command, { serialize_astring(name) } };
    return cast_promise<SolidResponse>(send_command(move(command)));
}

NonnullRefPtr<Promise<SolidResponse>> Client::delete_mailbox(StringView name)
{
    auto command = Command { CommandType::Delete, m_current_command, { serialize_astring(name) } };
    return cast_promise<SolidResponse>(send_command(move(command)));
}

NonnullRefPtr<Promise<SolidResponse>> Client::store(StoreMethod method, Sequence sequence_set, bool silent, Vector<ByteString> const& flags, bool uid)
{
    StringBuilder data_item_name;
    switch (method) {
    case StoreMethod::Replace:
        data_item_name.append("FLAGS"sv);
        break;
    case StoreMethod::Add:
        data_item_name.append("+FLAGS"sv);
        break;
    case StoreMethod::Remove:
        data_item_name.append("-FLAGS"sv);
        break;
    }
    if (silent) {
        data_item_name.append(".SILENT"sv);
    }

    StringBuilder flags_builder;
    flags_builder.append('(');
    flags_builder.join(' ', flags);
    flags_builder.append(')');

    auto command = Command { uid ? CommandType::UIDStore : CommandType::Store, m_current_command, { sequence_set.serialize(), data_item_name.to_byte_string(), flags_builder.to_byte_string() } };
    return cast_promise<SolidResponse>(send_command(move(command)));
}
NonnullRefPtr<Promise<SolidResponse>> Client::search(Optional<ByteString> charset, Vector<SearchKey>&& keys, bool uid)
{
    Vector<ByteString> args;
    if (charset.has_value()) {
        args.append("CHARSET "sv);
        args.append(charset.value());
    }
    for (auto const& item : keys) {
        args.append(item.serialize());
    }
    auto command = Command { uid ? CommandType::UIDSearch : CommandType::Search, m_current_command, args };
    return cast_promise<SolidResponse>(send_command(move(command)));
}

NonnullRefPtr<Promise<ContinueRequest>> Client::idle()
{
    auto promise = send_simple_command(CommandType::Idle);
    return cast_promise<ContinueRequest>(promise);
}
NonnullRefPtr<Promise<SolidResponse>> Client::finish_idle()
{
    auto promise = Promise<Response>::construct();
    m_pending_promises.append(promise);
    MUST(send_raw("DONE"sv));
    m_expecting_response = true;
    return cast_promise<SolidResponse>(promise);
}

NonnullRefPtr<Promise<SolidResponse>> Client::status(StringView mailbox, Vector<StatusItemType> const& types)
{
    Vector<ByteString> args;
    for (auto type : types) {
        switch (type) {
        case StatusItemType::Recent:
            args.append("RECENT"sv);
            break;
        case StatusItemType::UIDNext:
            args.append("UIDNEXT"sv);
            break;
        case StatusItemType::UIDValidity:
            args.append("UIDVALIDITY"sv);
            break;
        case StatusItemType::Unseen:
            args.append("UNSEEN"sv);
            break;
        case StatusItemType::Messages:
            args.append("MESSAGES"sv);
            break;
        }
    }
    StringBuilder types_list;
    types_list.append('(');
    types_list.join(' ', args);
    types_list.append(')');
    auto command = Command { CommandType::Status, m_current_command, { mailbox, types_list.to_byte_string() } };
    return cast_promise<SolidResponse>(send_command(move(command)));
}

NonnullRefPtr<Promise<SolidResponse>> Client::append(StringView mailbox, Message&& message, Optional<Vector<ByteString>> flags, Optional<Core::DateTime> date_time)
{
    Vector<ByteString> args = { mailbox };
    if (flags.has_value()) {
        StringBuilder flags_sb;
        flags_sb.append('(');
        flags_sb.join(' ', flags.value());
        flags_sb.append(')');
        args.append(flags_sb.to_byte_string());
    }
    if (date_time.has_value())
        args.append(date_time.value().to_byte_string("\"%d-%b-%Y %H:%M:%S +0000\""sv));

    args.append(ByteString::formatted("{{{}}}", message.data.length()));

    auto continue_req = send_command(Command { CommandType::Append, m_current_command, args });

    auto response_promise = Promise<Response>::construct();
    m_pending_promises.append(response_promise);

    continue_req->on_resolution = [this, message2 { move(message) }](auto&) -> ErrorOr<void> {
        TRY(send_raw(message2.data));
        m_expecting_response = true;
        return {};
    };
    continue_req->on_rejection = [this](Error&) {
        // NOTE: This never fails.
        MUST(handle_parsed_response({ .successful = false, .response = {} }));
    };

    return cast_promise<SolidResponse>(response_promise);
}
NonnullRefPtr<Promise<SolidResponse>> Client::subscribe(StringView mailbox)
{
    auto command = Command { CommandType::Subscribe, m_current_command, { serialize_astring(mailbox) } };
    return cast_promise<SolidResponse>(send_command(move(command)));
}
NonnullRefPtr<Promise<SolidResponse>> Client::unsubscribe(StringView mailbox)
{
    auto command = Command { CommandType::Unsubscribe, m_current_command, { serialize_astring(mailbox) } };
    return cast_promise<SolidResponse>(send_command(move(command)));
}
NonnullRefPtr<Promise<Response>> Client::authenticate(StringView method)
{
    auto command = Command { CommandType::Authenticate, m_current_command, { method } };
    return send_command(move(command));
}
NonnullRefPtr<Promise<SolidResponse>> Client::rename(StringView from, StringView to)
{
    auto command = Command { CommandType::Rename, m_current_command, { serialize_astring(from), serialize_astring(to) } };
    return cast_promise<SolidResponse>(send_command(move(command)));
}
NonnullRefPtr<Promise<SolidResponse>> Client::copy(Sequence sequence_set, StringView name, bool uid)
{
    auto command = Command {
        uid ? CommandType::UIDCopy : CommandType::Copy, m_current_command, { sequence_set.serialize(), serialize_astring(name) }
    };

    return cast_promise<SolidResponse>(send_command(move(command)));
}

void Client::close()
{
    m_socket->close();
}

bool Client::is_open()
{
    return m_socket->is_open();
}

}
