/*
 * Copyright (c) 2021, Max Wipfli <max.wipfli@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <LibMatrix/Room.h>

namespace Matrix {

String Room::display_name() const
{
    if (m_name.has_value())
        return *m_name;
    if (m_is_direct) {
        VERIFY(m_members.size() == 2);
        auto member_ids = m_members.keys();
        auto& other_user_id = member_ids[0] == m_user_id ? member_ids[1] : member_ids[0];
        auto other_user = m_members.get(other_user_id).value();
        if (other_user.display_name.has_value())
            return *other_user.display_name;
    }
    return m_id.value();
}

u64 Room::last_message_timestamp_in_milliseconds() const
{
    if (m_messages.is_empty())
        return 0;
    return m_messages.last().metadata().timestamp_in_milliseconds();
}

void Room::set_direct(bool value)
{
    m_is_direct = value;
    if (m_is_direct)
        VERIFY(m_members.size() == 2);
}

void Room::add_message(NonnullOwnPtr<Message> message)
{
    // FIXME: Maybe m_messages should be an ordered HashMap instead of a Vector.
    // NOTE: If the message we encounter already exists, we will replace it. This is needed for messages
    //       that are emitted as "local echo", which need to be replaced by the proper event as soon as
    //       it arrives.
    for (size_t i = 0; i < m_messages.size(); ++i) {
        if (m_messages[i].metadata().id() == message->metadata().id()) {
            dbgln_if(MATRIX_DEBUG, "Message {} has been replaced by a new version.", message->metadata().id());
            m_messages.ptr_at(i) = move(message);
            return;
        }
    }

    m_messages.append(move(message));
}

static void log_malformed_state_event(StateEvent const& event)
{
    dbgln("[Matrix] Malformed state event of type '{}' with content:\n{}", event.metadata().type(), event.content().to_string());
}

void Room::process_state_event(StateEvent const& event, bool should_append_to_message_log)
{
    if (m_processed_state_events.contains(event.metadata().id())) {
        dbgln("[Matrix] State event '{}' already processed.", event.metadata().id());
        return;
    }
    m_processed_state_events.set(event.metadata().id());

    auto append_message_if_requested = [&]<typename... Args>(String message, Args... args)
    {
        if (should_append_to_message_log)
            add_message(make<StateMessage>(event.metadata(), String::formatted(message, args...)));
    };

    if (event.metadata().type() == "m.room.create") {
        append_message_if_requested("{} has created the room.", event.metadata().sender());
        // FIXME: Actually parse the data in the event.
        return;
    }
    if (event.metadata().type() == "m.room.member") {
        // https://matrix.org/docs/spec/client_server/r0.6.1#m-room-member
        if (!event.content().has("membership") || !event.content().get("membership").is_string())
            return log_malformed_state_event(event);

        auto user = UserId(event.state_key());

        Optional<String> display_name;
        if (event.content().has("displayname") && event.content().get("displayname").is_string())
            display_name = event.content().get("displayname").as_string();
        // Unset the Optional if the value is the empty string.
        if (display_name.has_value() && display_name->is_empty())
            display_name.clear();
        // FIXME: Also parse 'avatar_url', 'is_direct' and 'thiry_party_invite'.

        auto new_membership_string = event.content().get("membership").as_string();
        MembershipStatus new_membership { MembershipStatus::Left };
        if (new_membership_string == "invite")
            new_membership = MembershipStatus::Invited;
        else if (new_membership_string == "join")
            new_membership = MembershipStatus::Joined;
        else if (new_membership_string == "leave")
            new_membership = MembershipStatus::Left;
        else if (new_membership_string == "ban")
            new_membership = MembershipStatus::Banned;
        else if (new_membership_string == "knock")
            new_membership = MembershipStatus::Knocked;
        else
            return log_malformed_state_event(event);

        auto old_membership = MembershipStatus::Left;
        if (m_members.contains(user))
            old_membership = m_members.ensure(user).status;

        if (old_membership == MembershipStatus::Joined && new_membership == MembershipStatus::Invited) {
            dbgln("[Matrix] Invalid membership status mutation from Joined to Invited.");
        } else if (old_membership == MembershipStatus::Left && new_membership == MembershipStatus::Invited) {
            append_message_if_requested("{} has been invited by {}.", user, event.metadata().sender());
        } else if (old_membership == MembershipStatus::Banned && new_membership == MembershipStatus::Invited) {
            dbgln("[Matrix] Invalid membership status mutation from Banned to Invited.");
        } else if (old_membership == MembershipStatus::Invited && new_membership == MembershipStatus::Joined) {
            append_message_if_requested("{} has joined.", user);
        } else if (old_membership == MembershipStatus::Joined && new_membership == MembershipStatus::Joined) {
            auto& old_display_name = m_members.ensure(user).display_name;
            if (old_display_name != display_name) {
                if (old_display_name.has_value()) {
                    if (display_name.has_value())
                        append_message_if_requested("{} has changed their display name to '{}'.", event.metadata().sender(), *display_name);
                    else
                        append_message_if_requested("{} has removed their display name.", *display_name);
                } else {
                    append_message_if_requested("{} has set their display name to '{}'.", event.metadata().sender(), *display_name);
                }
            }
            // FIXME: Maybe the 'avatar_url' field changed too.
        } else if (old_membership == MembershipStatus::Left && new_membership == MembershipStatus::Joined) {
            append_message_if_requested("{} has joined.", user);
        } else if (old_membership == MembershipStatus::Banned && new_membership == MembershipStatus::Joined) {
            dbgln("[Matrix] Invalid membership status mutation from Banned to Joined.");
        } else if (old_membership == MembershipStatus::Invited && new_membership == MembershipStatus::Left) {
            if (user == event.metadata().sender())
                append_message_if_requested("{} has rejected the invite.", user);
            else
                append_message_if_requested("{} had their invite revoked by {}.", user, event.metadata().sender());
        } else if (old_membership == MembershipStatus::Joined && new_membership == MembershipStatus::Left) {
            if (user == event.metadata().sender())
                append_message_if_requested("{} has left.", user);
            else
                append_message_if_requested("{} has been kicked by {}.", user, event.metadata().sender());
        } else if (old_membership == MembershipStatus::Banned && new_membership == MembershipStatus::Left) {
            append_message_if_requested("{} has been unbanned by {}.", user, event.metadata().sender());
        } else if (old_membership == MembershipStatus::Invited && new_membership == MembershipStatus::Banned) {
            append_message_if_requested("{} has been banned by {}.", user, event.metadata().sender());
        } else if (old_membership == MembershipStatus::Joined && new_membership == MembershipStatus::Banned) {
            append_message_if_requested("{} has been kicked and banned by {}.", user, event.metadata().sender());
        } else if (old_membership == MembershipStatus::Left && new_membership == MembershipStatus::Banned) {
            append_message_if_requested("{} has been banned by {}.", user, event.metadata().sender());
        } else if (old_membership == MembershipStatus::Knocked || new_membership == MembershipStatus::Knocked) {
            dbgln("[Matrix] Unimplemented membership change with either old or new membership 'Knocked'.");
        }

        m_members.set(user, { new_membership, display_name });
        return;
    }
    if (event.metadata().type() == "m.room.name") {
        String name;
        // An absent 'name' field should be treated like the empty string.
        if (event.content().has("name")) {
            if (event.content().get("name").is_string())
                name = event.content().get("name").as_string();
            else
                return log_malformed_state_event(event);
        }

        if (name.length() > 255)
            return log_malformed_state_event(event);

        if (name.is_empty()) {
            if (m_name.has_value()) {
                append_message_if_requested("{} has removed the room name.", event.metadata().sender());
                m_name.clear();
            } else {
                dbgln("[Matrix] Room name changed from unset to unset.");
            }
        } else {
            append_message_if_requested("{} has set the room name to '{}'.", event.metadata().sender(), name);
            m_name = name;
        }
        return;
    }
    if (event.metadata().type() == "m.room.topic") {
        String topic;
        // An absent 'topic' field should be treated like the empty string.
        if (event.content().has("topic")) {
            if (event.content().get("topic").is_string())
                topic = event.content().get("topic").as_string();
            else
                return log_malformed_state_event(event);
        }

        if (topic.length() > 255)
            return log_malformed_state_event(event);

        if (topic.is_empty()) {
            if (m_topic.has_value()) {
                append_message_if_requested("{} has removed the room topic.", event.metadata().sender());
                m_topic.clear();
            } else {
                dbgln("[Matrix] Room topic changed from unset to unset.");
            }
        } else {
            append_message_if_requested("{} has changed the room topic.", event.metadata().sender());
            m_topic = topic;
        }
        return;
    }

    dbgln("[Matrix] Unimplemented state event type: '{}'", event.metadata().type());
}

}
