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

    NonnullRefPtr<Promise<Response>> send_command(Command&&);
    NonnullRefPtr<Promise<Response>> send_simple_command(CommandType);
    ErrorOr<void> send_raw(StringView data);
    NonnullRefPtr<Promise<SolidResponse>> login(StringView username, StringView password);
    NonnullRefPtr<Promise<SolidResponse>> list(StringView reference_name, StringView mailbox_name, bool unseen = false);
    NonnullRefPtr<Promise<SolidResponse>> lsub(StringView reference_name, StringView mailbox_name);
    NonnullRefPtr<Promise<SolidResponse>> select(StringView string);
    NonnullRefPtr<Promise<SolidResponse>> examine(StringView string);
    NonnullRefPtr<Promise<SolidResponse>> search(Optional<ByteString> charset, Vector<SearchKey>&& search_keys, bool uid);
    NonnullRefPtr<Promise<SolidResponse>> fetch(FetchCommand request, bool uid);
    NonnullRefPtr<Promise<SolidResponse>> store(StoreMethod, Sequence, bool silent, Vector<ByteString> const& flags, bool uid);
    NonnullRefPtr<Promise<SolidResponse>> copy(Sequence sequence_set, StringView name, bool uid);
    NonnullRefPtr<Promise<SolidResponse>> create_mailbox(StringView name);
    NonnullRefPtr<Promise<SolidResponse>> delete_mailbox(StringView name);
    NonnullRefPtr<Promise<SolidResponse>> subscribe(StringView mailbox);
    NonnullRefPtr<Promise<SolidResponse>> unsubscribe(StringView mailbox);
    NonnullRefPtr<Promise<SolidResponse>> rename(StringView from, StringView to);
    NonnullRefPtr<Promise<Response>> authenticate(StringView method);
    NonnullRefPtr<Promise<ContinueRequest>> idle();
    NonnullRefPtr<Promise<SolidResponse>> finish_idle();
    NonnullRefPtr<Promise<SolidResponse>> status(StringView mailbox, Vector<StatusItemType> const& types);
    NonnullRefPtr<Promise<SolidResponse>> append(StringView mailbox, Message&& message, Optional<Vector<ByteString>> flags = {}, Optional<Core::DateTime> date_time = {});

    bool is_open();
    void close();

    Function<void(ResponseData&&)> unrequested_response_callback;

private:
    Client(StringView host, u16 port, NonnullOwnPtr<Core::Socket>);
    void setup_callbacks();

    ErrorOr<void> on_ready_to_receive();
    bool verify_response_is_complete();

    ErrorOr<void> handle_parsed_response(ParseStatus&& parse_status);
    ErrorOr<void> send_next_command();

    StringView m_host;
    u16 m_port;

    NonnullOwnPtr<Core::Socket> m_socket;
    RefPtr<Promise<Empty>> m_connect_pending {};

    int m_current_command = 1;

    // Sent but response not received
    Vector<NonnullRefPtr<Promise<Response>>> m_pending_promises;
    // Not yet sent
    Vector<Command> m_command_queue {};

    ByteBuffer m_buffer;
    Parser m_parser {};

    bool m_expecting_response { false };
};
}
