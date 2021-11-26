/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <LibCore/Notifier.h>
#include <LibCore/TCPSocket.h>

#include "Command.h"
#include "Parser.h"

class Client : public RefCounted<Client> {
public:
    static NonnullRefPtr<Client> create(int id, RefPtr<Core::TCPSocket> socket, int ptm_fd)
    {
        return adopt_ref(*new Client(id, move(socket), ptm_fd));
    }

    Function<void()> on_exit;

protected:
    Client(int id, RefPtr<Core::TCPSocket> socket, int ptm_fd);

    void drain_socket();
    void drain_pty();
    void handle_data(StringView);
    void handle_command(const Command& command);
    void handle_error();
    void send_data(StringView str);
    void send_command(Command command);
    void send_commands(Vector<Command> commands);
    void quit();

private:
    // client id
    int m_id { 0 };
    // client resources
    RefPtr<Core::TCPSocket> m_socket;
    Parser m_parser;
    // pty resources
    int m_ptm_fd { -1 };
    RefPtr<Core::Notifier> m_ptm_notifier;
};
