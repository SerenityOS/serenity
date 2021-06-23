/*
 * Copyright (c) 2021, Max Wipfli <max.wipfli@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/JsonObject.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <LibMatrix/Device.h>
#include <LibMatrix/Id.h>
#include <LibMatrix/Message.h>
#include <LibMatrix/Room.h>

namespace Matrix {

OwnPtr<Device> Device::create(String user_id)
{
    if (!UserId::is_valid(user_id))
        return {};
    UserId id { move(user_id) };
    URL home_server_url { String::formatted("http://{}/_matrix/client/r0/", id.home_server()) };
    if (!home_server_url.is_valid())
        return {};
    dbgln_if(MATRIX_DEBUG, "[Matrix] Create new device with user_id='{}', home_server_url='{}'", id, home_server_url.to_string());
    return adopt_own(*new Device(move(id), move(home_server_url)));
}

void Device::login(StringView const& password, Callback callback)
{
    StringBuilder builder;
    JsonObjectSerializer serializer { builder };
    serializer.add("type", "m.login.password");

    auto identifier = serializer.add_object("identifier");
    identifier.add("type", "m.id.user");
    identifier.add("user", m_user_id.local_part());
    identifier.finish();

    serializer.add("password", password);
    serializer.add("device_id", "LibMatrix (SerenityOS)");
    serializer.finish();

    m_connection->send_request("POST", "login", builder.string_view(), move(callback), [this](Result<JsonObject, ErrorResponse> result) {
        if (result.is_error())
            return;
        dbgln_if(MATRIX_DEBUG, "[Matrix] Login successful.");
        m_device_id = result.value().get("device_id").as_string();
        m_connection->set_access_token({}, result.value().get("access_token").as_string());
    });
}

void Device::logout(Callback callback)
{
    VERIFY(is_logged_in());
    m_connection->send_request("POST", "logout", {}, move(callback), [this](auto result) {
        if (result.is_error())
            return;
        dbgln_if(MATRIX_DEBUG, "[Matrix] Logout successful.");
        m_connection->unset_access_token({});
        m_device_id = {};
    });
}

// NOTE: If poll, the request will return as soon as new events are available, or if it times out (20000 ms).
void Device::sync(Poll poll, Callback callback)
{
    constexpr u32 timeout = 20000;
    dbgln_if(MATRIX_DEBUG, "[Matrix] sync() with Poll::{}", poll == Poll::Yes ? "Yes" : "No");
    VERIFY(is_logged_in());

    // FIXME: This should use something like URLSearchParams.
    StringBuilder builder;
    builder.append("sync");
    if (poll == Poll::Yes) {
        builder.appendff("?timeout={}", timeout);
        if (m_sync_next_batch.has_value())
            builder.appendff("&since={}", *m_sync_next_batch);
    } else if (m_sync_next_batch.has_value()) {
        builder.appendff("?since={}", *m_sync_next_batch);
    }

    m_connection->send_request("GET", builder.to_string(), {}, move(callback), [this](auto result) {
        if (result.is_error())
            return;
        process_sync_data(result.value());
    });
}

void Device::process_sync_data(JsonObject const& data)
{
    m_sync_next_batch = data.get("next_batch").as_string();

    if (data.has("rooms")) {
        auto rooms = data.get("rooms");

        if (rooms.as_object().has("join")) {
            auto joined_rooms = rooms.as_object().get("join");

            joined_rooms.as_object().for_each_member([this](String const& key, JsonValue const& value) {
                auto room_id = RoomId(key);
                if (!m_rooms.contains(room_id)) {
                    m_rooms.set(room_id, make<Room>(room_id, m_user_id));
                }

                Room& current_room = *(m_rooms.get(room_id).value());

                // NOTE: As long as the 'full_state' parameters is not set in the query, the 'state' object contains all
                //       message events between 'since' and the start of 'timeline', so we need to parse them first.
                if (value.as_object().has("state")) {
                    auto events = value.as_object().get("state").as_object().get("events");
                    events.as_array().for_each([&](JsonValue const& event) {
                        auto state_event = StateEvent::create_from_json(event.as_object());
                        if (state_event.has_value())
                            current_room.process_state_event(*state_event, false);
                        else
                            dbgln("[Matrix] 'state' object contains invalid state event:\n{}", event.to_string());
                    });
                }

                if (value.as_object().has("timeline")) {
                    auto events = value.as_object().get("timeline").as_object().get("events");
                    events.as_array().for_each([&](JsonValue const& event) {
                        if (event.as_object().has("state_key")) {
                            auto state_event = StateEvent::create_from_json(event.as_object());
                            if (state_event.has_value())
                                current_room.process_state_event(*state_event, true);
                            else
                                dbgln("[Matrix] Invalid state event:\n{}", event.to_string());
                        } else {
                            auto message = Message::create_from_json(event.as_object());
                            if (message)
                                current_room.add_message(message.release_nonnull());
                            else
                                dbgln_if(MATRIX_DEBUG, "[Matrix] Invalid or unimplemented message event ignored.");
                        }
                    });
                }

                // FIXME: Parse "summary", "ephemeral", "account_data" and "unread_notifications", if necessary.
            });
        }

        // FIXME: Also parse "invite" and "leave".
    }

    if (data.has("account_data")) {
        auto account_data = data.get("account_data");
        if (account_data.as_object().has("events")) {
            auto events = account_data.as_object().get("events");
            events.as_array().for_each([this](JsonValue const& value) {
                if (!value.is_object() || !value.as_object().has("type") || !value.as_object().get("type").is_string()) {
                    dbgln_if(MATRIX_DEBUG, "[Matrix] Invalid account data event ignored:\n{}", value.to_string());
                    return;
                }
                auto type = value.as_object().get("type").as_string();

                if (type == "m.direct") {
                    auto content = value.as_object().get("content");
                    content.as_object().for_each_member([this](auto&, auto& value) {
                        value.as_array().for_each([this](JsonValue const& value) {
                            RoomId room_id(value.as_string());
                            VERIFY(m_rooms.contains(room_id));
                            m_rooms.get(room_id).value()->set_direct(true);
                        });
                    });
                } else {
                    dbgln_if(MATRIX_DEBUG, "[Matrix] Invalid or unimplemented account data event ignored:\n{}", value.to_string());
                }
            });
        }
    }

    // FIXME: Parse everything else.
}

void Device::send_text_message(RoomId const& room_id, String message, Callback callback)
{
    VERIFY(m_rooms.contains(room_id));
    VERIFY(!message.is_empty());

    StringBuilder url_builder;
    url_builder.append("rooms/");
    url_builder.append(URL::percent_encode(room_id.value(), URL::PercentEncodeSet::Path));
    url_builder.appendff("/send/m.room.message/{}", m_next_transaction_id++);

    StringBuilder body_builder;
    JsonObjectSerializer serializer { body_builder };
    serializer.add("msgtype", "m.text");
    serializer.add("body", message);
    serializer.finish();

    m_connection->send_request("PUT", url_builder.string_view(), body_builder.string_view(), move(callback), [&, message = move(message)](Result<JsonObject, ErrorResponse> result) mutable {
        if (result.is_error())
            return;
        VERIFY(result.value().get("event_id").is_string());
        EventId event_id { result.value().get("event_id").as_string() };
        struct timeval tv;
        VERIFY(gettimeofday(&tv, nullptr) >= 0);
        u64 timestamp_in_milliseconds = ((u64)tv.tv_sec * 1000) + ((u64)tv.tv_usec / 1000);
        EventMetadata metadata { event_id, "m.room.message", m_user_id, timestamp_in_milliseconds };
        auto* room = *m_rooms.get(room_id);
        room->add_message(make<TextMessage>(move(metadata), Message::Type::Text, move(message)));
    });
}

}
