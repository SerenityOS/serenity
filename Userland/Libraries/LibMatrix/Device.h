/*
 * Copyright (c) 2021, Max Wipfli <max.wipfli@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/String.h>
#include <AK/URL.h>
#include <LibMatrix/Connection.h>
#include <LibMatrix/Id.h>
#include <LibMatrix/Room.h>

namespace Matrix {

using Callback = Function<void(bool success)>;

class Device {
public:
    enum class Poll {
        No,
        Yes
    };

    static OwnPtr<Device> create(String user_id);
    void login(StringView const& password, Callback = nullptr);
    void logout(Callback = nullptr);

    UserId const& user_id() const { return m_user_id; }
    bool is_logged_in() const { return m_connection->access_token().has_value(); }
    HashMap<RoomId, NonnullOwnPtr<Room>> const& rooms() const { return m_rooms; }

    void sync(Poll, Callback = nullptr);
    // NOTE: When the callback is fired, a local echo has already been output to the rooms messages,
    //       so the caller can refresh it's screen if necessary.
    void send_text_message(RoomId const&, String content, Callback = nullptr);

private:
    Device(UserId user_id, URL home_server_url)
        : m_user_id(move(user_id))
        , m_connection(Connection::construct({}, move(home_server_url)))
    {
    }

    void process_sync_data(JsonObject const&);

    UserId m_user_id;
    Optional<String> m_device_id;
    NonnullRefPtr<Connection> m_connection;
    u32 m_next_transaction_id { 0 };

    HashMap<RoomId, NonnullOwnPtr<Room>> m_rooms;
    Optional<String> m_sync_next_batch;
};

}
