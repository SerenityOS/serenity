/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Client.h"

Client::Client(int id, RefPtr<Core::TCPSocket> socket)
    : m_id(id)
    , m_socket(move(socket))
{
    m_socket->on_ready_to_read = [this] { drain_socket(); };
}

void Client::drain_socket()
{
    NonnullRefPtr<Client> protect(*this);
    while (m_socket->can_read()) {
        auto buf = m_socket->read(1024);

        dbgln("Read {} bytes.", buf.size());

        if (m_socket->eof()) {
            quit();
            break;
        }

        m_socket->write(buf);
    }
}

void Client::quit()
{
    m_socket->close();
    if (on_exit)
        on_exit();
}
