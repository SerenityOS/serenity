/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/TCPSocket.h>

class Client : public RefCounted<Client> {
public:
    static NonnullRefPtr<Client> create(int id, RefPtr<Core::TCPSocket> socket)
    {
        return adopt_ref(*new Client(id, move(socket)));
    }

    Function<void()> on_exit;

protected:
    Client(int id, RefPtr<Core::TCPSocket> socket);

    void drain_socket();
    void quit();

private:
    int m_id { 0 };
    RefPtr<Core::TCPSocket> m_socket;
};
