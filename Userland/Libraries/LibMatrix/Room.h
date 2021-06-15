/*
 * Copyright (c) 2021, Max Wipfli <max.wipfli@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtrVector.h>
#include <LibMatrix/Id.h>
#include <LibMatrix/Message.h>

namespace Matrix {

class Room {
public:
    explicit Room(RoomId id)
        : m_id(move(id))
    {
    }

    RoomId const& id() const { return m_id; }
    NonnullOwnPtrVector<Message> const& messages() const { return m_messages; }

    u64 last_message_timestamp_in_milliseconds() const;

    void add_message(NonnullOwnPtr<Message>);

private:
    RoomId m_id;
    NonnullOwnPtrVector<Message> m_messages;
};

}
