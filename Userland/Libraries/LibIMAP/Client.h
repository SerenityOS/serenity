/*
 * Copyright (c) 2021, Kyle Pereira <hey@xylepereira.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <LibCore/Promise.h>
#include <LibCore/Stream.h>
#include <LibIMAP/Parser.h>
#include <LibTLS/TLSv12.h>

namespace IMAP {
template<typename T>
using Promise = Core::Promise<T>;

class Client {
    AK_MAKE_NONCOPYABLE(Client);
    friend class Parser;

public:
    static ErrorOr<NonnullOwnPtr<Client>> connect_tls(StringView host, u16 port);
    static ErrorOr<NonnullOwnPtr<Client>> connect_plaintext(StringView host, u16 port);

    Client(Client&&);

    RefPtr<Promise<Empty>> connection_promise()
    {
        return m_connect_pending;
    }

    RefPtr<Promise<Optional<Response>>> send_command(Command&&);
    RefPtr<Promise<Optional<Response>>> send_simple_command(CommandType);
    ErrorOr<void> send_raw(StringView data);
    RefPtr<Promise<Optional<SolidResponse>>> login(StringView username, StringView password);
    RefPtr<Promise<Optional<SolidResponse>>> list(StringView reference_name, StringView mailbox_name);
    RefPtr<Promise<Optional<SolidResponse>>> lsub(StringView reference_name, StringView mailbox_name);
    RefPtr<Promise<Optional<SolidResponse>>> select(StringView string);
    RefPtr<Promise<Optional<SolidResponse>>> examine(StringView string);
    RefPtr<Promise<Optional<SolidResponse>>> search(Optional<String> charset, Vector<SearchKey>&& search_keys, bool uid);
    RefPtr<Promise<Optional<SolidResponse>>> fetch(FetchCommand request, bool uid);
    RefPtr<Promise<Optional<SolidResponse>>> store(StoreMethod, Sequence, bool silent, Vector<String> const& flags, bool uid);
    RefPtr<Promise<Optional<SolidResponse>>> copy(Sequence sequence_set, StringView name, bool uid);
    RefPtr<Promise<Optional<SolidResponse>>> create_mailbox(StringView name);
    RefPtr<Promise<Optional<SolidResponse>>> delete_mailbox(StringView name);
    RefPtr<Promise<Optional<SolidResponse>>> subscribe(StringView mailbox);
    RefPtr<Promise<Optional<SolidResponse>>> unsubscribe(StringView mailbox);
    RefPtr<Promise<Optional<SolidResponse>>> rename(StringView from, StringView to);
    RefPtr<Promise<Optional<Response>>> authenticate(StringView method);
    RefPtr<Promise<Optional<ContinueRequest>>> idle();
    RefPtr<Promise<Optional<SolidResponse>>> finish_idle();
    RefPtr<Promise<Optional<SolidResponse>>> status(StringView mailbox, Vector<StatusItemType> const& types);
    RefPtr<Promise<Optional<SolidResponse>>> append(StringView mailbox, Message&& message, Optional<Vector<String>> flags = {}, Optional<Core::DateTime> date_time = {});

    bool is_open();
    void close();

    Function<void(ResponseData&&)> unrequested_response_callback;

private:
    Client(StringView host, u16 port, NonnullOwnPtr<Core::Stream::Socket>);
    void setup_callbacks();

    ErrorOr<void> on_ready_to_receive();
    ErrorOr<void> on_tls_ready_to_receive();

    ErrorOr<void> handle_parsed_response(ParseStatus&& parse_status);
    ErrorOr<void> send_next_command();

    StringView m_host;
    u16 m_port;

    NonnullOwnPtr<Core::Stream::Socket> m_socket;
    RefPtr<Promise<Empty>> m_connect_pending {};

    int m_current_command = 1;

    // Sent but response not received
    Vector<RefPtr<Promise<Optional<Response>>>> m_pending_promises;
    // Not yet sent
    Vector<Command> m_command_queue {};

    ByteBuffer m_buffer;
    Parser m_parser {};

    bool m_expecting_response { false };
};
}
