/*
 * Copyright (c) 2023, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "UsersMapWidget.h"
#include <AK/JsonParser.h>
#include <LibDesktop/Launcher.h>

namespace Maps {

UsersMapWidget::UsersMapWidget(Options const& options)
    : MapWidget::MapWidget(options)
{
    m_marker_gray_image = Gfx::Bitmap::load_from_file("/res/graphics/maps/marker-gray.png"sv).release_value_but_fixme_should_propagate_errors();
}

void UsersMapWidget::get_users()
{
    // Start HTTP GET request to load people.json
    HTTP::HeaderMap headers;
    headers.set("User-Agent", "SerenityOS Maps");
    headers.set("Accept", "application/json");
    URL::URL url("https://usermap.serenityos.org/people.json");
    auto request = request_client()->start_request("GET", url, headers, {});
    VERIFY(!request.is_null());
    m_request = request;

    request->set_buffered_request_finished_callback([this, request, url](bool success, auto, auto&, auto, ReadonlyBytes payload) {
        m_request.clear();
        if (!success) {
            dbgln("Maps: Can't load: {}", url);
            return;
        }

        // Parse JSON data
        JsonParser parser(payload);
        auto result = parser.parse();
        if (result.is_error()) {
            dbgln("Maps: Can't parse JSON: {}", url);
            return;
        }

        // Parse each user
        // FIXME: Handle JSON parsing errors
        m_users = Vector<User>();
        auto json_users = result.release_value().as_array();
        for (size_t i = 0; i < json_users.size(); i++) {
            auto const& json_user = json_users.at(i).as_object();
            auto const& coordinates = json_user.get_array("coordinates"sv).release_value();
            User user {
                MUST(String::from_byte_string(json_user.get_byte_string("nick"sv).release_value())),
                { coordinates[0].get_double_with_precision_loss().value(),
                    coordinates[1].get_double_with_precision_loss().value() },
                json_user.has_bool("contributor"sv),
            };
            m_users.value().append(user);
        }
        add_users_to_map();
    });

    request->on_certificate_requested = []() -> Protocol::Request::CertificateAndKey { return {}; };
}

void UsersMapWidget::add_users_to_map()
{
    if (!m_users.has_value())
        return;

    for (auto const& user : m_users.value()) {
        MapWidget::Marker marker = { user.coordinates, user.nick, {}, "users"_string };
        if (!user.contributor)
            marker.image = m_marker_gray_image;
        add_marker(marker);
    }

    add_panel({ MUST(String::formatted("{} users are already registered", m_users.value().size())),
        Panel::Position::TopRight,
        { { "https://github.com/SerenityOS/user-map" } },
        "users"_string });
}

}
