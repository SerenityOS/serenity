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

void Device::sync(Callback callback)
{
    VERIFY(is_logged_in());
    // FIXME: Acutally support relative sync.
    VERIFY(!m_sync_next_batch.has_value());
    m_connection->send_request("GET", "sync", {}, move(callback), [this](auto result) {
        if (result.is_error())
            return;
        process_sync_data(result.value());
    });
}

void Device::process_sync_data(JsonObject const& data)
{
    // FIXME: This currently only supports absolute sync.
    m_rooms.clear();
    m_sync_next_batch = data.get("next_batch").as_string();

    if (data.has("rooms")) {
        auto rooms = data.get("rooms");

        if (rooms.as_object().has("join")) {
            auto joined_rooms = rooms.as_object().get("join");

            joined_rooms.as_object().for_each_member([this](String const& key, JsonValue const& value) {
                auto room_id = RoomId(key);
                if (!m_rooms.contains(room_id)) {
                    m_rooms.set(room_id, make<Room>(room_id));
                }

                Room& current_room = *(m_rooms.get(room_id).value());

                if (value.as_object().has("timeline")) {
                    auto events = value.as_object().get("timeline").as_object().get("events");
                    events.as_array().for_each([&](JsonValue const& event) {
                        if (event.as_object().has("state_key")) {
                            // FIXME: Parse state events.
                        } else {
                            auto message = Message::create_from_json(event.as_object());
                            if (message)
                                current_room.add_message(message.release_nonnull());
                            else
                                dbgln_if(MATRIX_DEBUG, "[Matrix] Invalid or unimplemented message event ignored.");
                        }
                    });
                }

                // FIXME: Parse "summary", "state", "ephemeral", "account_data" and "unread_notifications", if necessary.
            });
        }

        // FIXME: Also parse "invite" and "leave".
    }

    // FIXME: Parse everything else.
}

}
