/*
 * Copyright (c) 2023, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Client.h"
#include <AK/JsonObject.h>
#include <AK/JsonParser.h>
#include <LibProtocol/Request.h>

namespace Geolocation {

static RefPtr<Client> s_the = nullptr;

Client& Client::the()
{
    if (!s_the)
        s_the = Client::try_create().release_value_but_fixme_should_propagate_errors();
    return *s_the;
}

Client::Client()
{
    m_request_client = Protocol::RequestClient::try_create().release_value_but_fixme_should_propagate_errors();
}

void Client::get_current_position(Function<void(Optional<Position>)> callback)
{
    // FIXME: Move this code to GeolocationServer service
    // FIXME: This is a temp solution that uses ipinfo API
    HashMap<DeprecatedString, DeprecatedString> headers;
    headers.set("User-Agent", "SerenityOS LibGeolocation");
    headers.set("Accept", "application/json");
    URL url("https://ipinfo.io/json");
    auto request = m_request_client->start_request("GET", url, headers, {});
    VERIFY(!request.is_null());
    m_active_requests.append(request);
    request->on_buffered_request_finish = [this, request, url, callback = move(callback)](bool success, auto, auto&, auto, ReadonlyBytes payload) {
        auto was_active = m_active_requests.remove_first_matching([request](auto const& other_request) { return other_request->id() == request->id(); });
        if (!was_active)
            return;

        if (!success) {
            dbgln("LibGeolocation: Can't load: {}", url);
            callback({});
        }

        // Parse JSON data
        JsonParser parser(payload);
        auto result = parser.parse();
        if (result.is_error()) {
            dbgln("LibGeolocation: Can't parse JSON: {}", url);
            return;
        }

        // Parse position
        auto json_position = result.release_value().as_object();
        auto json_location = json_position.get_deprecated_string("loc"sv).release_value().split(',');
        callback(Position {
            json_location.at(0).to_double().release_value(),
            json_location.at(1).to_double().release_value(),
            0, // FIXME: implement accuracy
            { MUST(String::from_deprecated_string(json_position.get_deprecated_string("city"sv).release_value())) },
            { MUST(String::from_deprecated_string(json_position.get_deprecated_string("region"sv).release_value())) },
            { MUST(String::from_deprecated_string(json_position.get_deprecated_string("country"sv).release_value())) },
            UnixDateTime::now(),
        });
    };
    request->set_should_buffer_all_input(true);
    request->on_certificate_requested = []() -> Protocol::Request::CertificateAndKey { return {}; };
}

}
