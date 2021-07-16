/*
 * Copyright (c) 2021, Max Wipfli <max.wipfli@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/String.h>
#include <AK/URL.h>
#include <LibCore/Object.h>
#include <LibProtocol/RequestClient.h>

namespace Matrix {

struct ErrorResponse {
    u32 status_code { 0 }; // A status code of 0 signifies a local error.
    String code;
    String message;
    JsonObject const* json { nullptr }; // This is needed for errors which contains additional fields.
};

using Callback = Function<void(bool success)>;

class Device;

class Connection : public Core::Object {
    C_OBJECT_ABSTRACT(Connection)
public:
    static NonnullRefPtr<Connection> construct(Badge<Device>, URL api_base_url)
    {
        return adopt_ref(*new Connection(move(api_base_url)));
    }

    void set_access_token(Badge<Device>, String);
    void unset_access_token(Badge<Device>);
    Optional<String> const& access_token() const { return m_access_token; }
    URL const& api_base_url() const { return m_api_base_url; }

    void send_request(String const& method, String const& url_suffix, StringView const& body, Callback user_callback, Function<void(Result<JsonObject, ErrorResponse> response)> callback);

private:
    Connection(URL api_base_url)
        : m_request_client(Protocol::RequestClient::construct())
        , m_api_base_url(move(api_base_url))
    {
    }

    NonnullRefPtr<Protocol::RequestClient> m_request_client;
    HashMap<String, String> m_request_headers;
    Optional<String> m_access_token;
    URL m_api_base_url;
};

}
