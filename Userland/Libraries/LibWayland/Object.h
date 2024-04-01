/*
 * Copyright (c) 2024, jane400 <serenity-os@j4ne.de>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <LibWayland/Connection.h>
#include <LibWayland/Message.h>

namespace Wayland {

struct Interface;

class Object : public RefCounted<Object> {
    friend class MessageOutgoing;
    friend class MessageIncoming;

public:
    uint32_t id() const
    {
        return m_id;
    }

    const struct Interface& interface() const
    {
        return m_interface;
    }

protected:
    Object(Connection& connection, uint32_t id, Interface const& interface)
        : m_connection(connection)
        , m_id(id)
        , m_interface(interface)
    {
    }

    void submit_message()
    {

        // new Message(MessageType::Event, m_id, )
    }

    Connection& m_connection;
    uint32_t m_id;
    const struct Interface& m_interface;
};

}
