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

}
