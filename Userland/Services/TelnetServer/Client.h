/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <AK/Types.h>
#include <LibCore/Notifier.h>

#include "Command.h"
#include "Parser.h"

class Client : public RefCounted<Client> {
public:
    static ErrorOr<NonnullRefPtr<Client>> create(int id, NonnullOwnPtr<Core::TCPSocket> socket, int ptm_fd);

    Function<void()> on_exit;

private:
    Client(int id, NonnullOwnPtr<Core::TCPSocket> socket, int ptm_fd);

    ErrorOr<void> drain_socket();
    ErrorOr<void> drain_pty();
    ErrorOr<void> handle_command(Command const& command);
    ErrorOr<void> send_data(StringView str);
    ErrorOr<void> send_command(Command command);
    ErrorOr<void> send_commands(Vector<Command> commands);

    void handle_data(StringView);
    void handle_error();
    void quit();

    // client id
    int m_id { 0 };
    // client resources
    NonnullOwnPtr<Core::TCPSocket> m_socket;
    Parser m_parser;
    // pty resources
    int m_ptm_fd { -1 };
    RefPtr<Core::Notifier> m_ptm_notifier;
};
