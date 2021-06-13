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

namespace Matrix {

using Callback = Function<void(bool success)>;

class Device {
public:
    static OwnPtr<Device> create(String user_id);
    void login(StringView const& password, Callback = nullptr);
    void logout(Callback = nullptr);

    UserId const& user_id() const { return m_user_id; }
    bool is_logged_in() const { return m_connection->access_token().has_value(); }

private:
    Device(UserId user_id, URL home_server_url)
        : m_user_id(move(user_id))
        , m_connection(Connection::construct({}, move(home_server_url)))
    {
    }

    UserId m_user_id;
    Optional<String> m_device_id;
    NonnullRefPtr<Connection> m_connection;
};

}
