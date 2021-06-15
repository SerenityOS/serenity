/*
 * Copyright (c) 2021, Max Wipfli <max.wipfli@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibMatrix/Room.h>

namespace Matrix {

u64 Room::last_message_timestamp_in_milliseconds() const
{
    if (m_messages.is_empty())
        return 0;
    return m_messages.last().metadata().timestamp_in_milliseconds();
}

void Room::add_message(NonnullOwnPtr<Message> message)
{
    // FIXME: Implement message deduplication.
    m_messages.append(move(message));
}

}
