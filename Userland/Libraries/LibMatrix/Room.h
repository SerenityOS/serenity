/*
 * Copyright (c) 2021, Max Wipfli <max.wipfli@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtrVector.h>
#include <LibMatrix/Id.h>
#include <LibMatrix/Message.h>
#include <LibMatrix/StateEvent.h>

namespace Matrix {

class Room {
public:
    enum class MembershipStatus {
        Invited,
        Joined,
        Left,
        Banned,
        Knocked
    };

    struct Membership {
        MembershipStatus status;
        Optional<String> display_name;
    };

    explicit Room(RoomId id)
        : m_id(move(id))
    {
    }

    RoomId const& id() const { return m_id; }
    NonnullOwnPtrVector<Message> const& messages() const { return m_messages; }
    HashMap<UserId, Membership> const& members() const { return m_members; }

    Optional<String> const& name() const { return m_name; }
    Optional<String> const& topic() const { return m_topic; }

    u64 last_message_timestamp_in_milliseconds() const;

    void add_message(NonnullOwnPtr<Message>);
    void process_state_event(StateEvent const&, bool should_append_to_message_log);

private:
    RoomId m_id;
    NonnullOwnPtrVector<Message> m_messages;
    HashTable<EventId> m_processed_state_events;
    HashMap<UserId, Membership> m_members;

    Optional<String> m_name;
    Optional<String> m_topic;
};

}
