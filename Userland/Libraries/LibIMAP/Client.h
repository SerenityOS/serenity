/*
 * Copyright (c) 2021, Kyle Pereira <hey@xylepereira.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <LibCore/Promise.h>
#include <LibIMAP/Parser.h>
#include <LibTLS/TLSv12.h>

namespace IMAP {
template<typename T>
using Promise = Core::Promise<T>;

class Client {
    friend class Parser;

public:
    Client(StringView host, unsigned port, bool start_with_tls);

    RefPtr<Promise<Empty>> connect();
    RefPtr<Promise<Optional<Response>>> send_command(Command&&);
    RefPtr<Promise<Optional<Response>>> send_simple_command(CommandType);
    void send_raw(StringView data);
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

    void close();

    Function<void(ResponseData&&)> unrequested_response_callback;

private:
    StringView m_host;
    unsigned m_port;
    RefPtr<Core::Socket> m_socket;
    RefPtr<TLS::TLSv12> m_tls_socket;

    void on_ready_to_receive();
    void on_tls_ready_to_receive();

    bool m_tls;
    int m_current_command = 1;

    bool connect_tls();
    bool connect_plaintext();

    // Sent but response not received
    Vector<RefPtr<Promise<Optional<Response>>>> m_pending_promises;
    // Not yet sent
    Vector<Command> m_command_queue {};

    RefPtr<Promise<Empty>> m_connect_pending {};

    ByteBuffer m_buffer;
    Parser m_parser;

    bool m_expecting_response { false };
    void handle_parsed_response(ParseStatus&& parse_status);
    void send_next_command();
};
}
