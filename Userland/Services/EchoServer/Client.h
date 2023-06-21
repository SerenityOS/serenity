/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Socket.h>

class Client : public RefCounted<Client> {
public:
    static NonnullRefPtr<Client> create(int id, NonnullOwnPtr<Core::TCPSocket> socket)
    {
        return adopt_ref(*new Client(id, move(socket)));
    }

    Function<void()> on_exit;

protected:
    Client(int id, NonnullOwnPtr<Core::TCPSocket> socket);

    ErrorOr<void> drain_socket();
    void quit();

private:
    int m_id { 0 };
    NonnullOwnPtr<Core::TCPSocket> m_socket;
};
