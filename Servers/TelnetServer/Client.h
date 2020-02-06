/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
        return adopt(*new Client(id, move(socket), ptm_fd));
    }

    Function<void()> on_exit;

protected:
    Client(int id, RefPtr<Core::TCPSocket> socket, int ptm_fd);

    void drain_socket();
    void drain_pty();
    void handle_data(const StringView&);
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
