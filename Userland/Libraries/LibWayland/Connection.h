/*
 * Copyright (c) 2024, jane400 <serenity-os@j4ne.de>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <LibCore/Socket.h>
#include <LibWayland/Object.h>

namespace Wayland {

class Display;

class Connection {
public:
    static ErrorOr<Connection> open();
    static ErrorOr<Connection> open_with_path(ByteString path);

    //Display& get_display();

    void read();
    void write();

private:
    Connection(NonnullOwnPtr<Core::LocalSocket> socket);

    NonnullOwnPtr<Core::LocalSocket> m_socket;
    RefPtr<Core::Notifier> m_notifier_error;
    RefPtr<Core::Notifier> m_notifier_read;

    Object* get_object_by_id(uint32_t id);

    HashMap<uint32_t, Object*> m_objects_map;
    ByteBuffer m_unprocessed_bytes;
};

}
