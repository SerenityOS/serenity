/*
 * Copyright (c) 2021, Max Wipfli <max.wipfli@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/JsonObject.h>
#include <AK/String.h>
#include <LibMatrix/Connection.h>
#include <LibMatrix/Device.h>
#include <LibProtocol/Request.h>

namespace Matrix {

void Connection::set_access_token(Badge<Device>, String token)
{
    VERIFY(!token.is_empty());
    dbgln_if(MATRIX_DEBUG, "[Matrix] Access token set to '{}'.", token);
    m_access_token = move(token);
    m_request_headers.set("Authorization", String::formatted("Bearer {}", *m_access_token));
}

void Connection::unset_access_token(Badge<Device>)
{
    dbgln_if(MATRIX_DEBUG, "[Matrix] Access token unset.");
    m_access_token.clear();
    m_request_headers.remove("Authorization");
}

static void log_error(ErrorResponse const& response)
{
    dbgln_if(MATRIX_DEBUG, "[Matrix] Error response:\nstatus_code: {}\nerrno:       {}\nerror:       {}", response.status_code, response.code, response.message);
}

static bool handle_response(String const& request_url, bool success, Optional<u32> const& status_code, ReadonlyBytes const& body, Function<void(Result<JsonObject, ErrorResponse>)> const& callback)
{
    if (!success || !status_code.has_value()) {
        ErrorResponse response { 0, "_REQUEST_FAILED", "Request failed.", nullptr };
        log_error(response);
        callback(move(response));
        return false;
    }

    auto body_view = StringView(body);
    dbgln_if(MATRIX_DEBUG, "[Matrix] Response received from {} with response code {} and {} bytes:\n{}", request_url, *status_code, body.size(), body_view);
    auto json = JsonValue::from_string(body_view);

    if (!json.has_value()) {
        ErrorResponse response { 0, "_INVALID_JSON_RESPONSE", "Response is not valid JSON.", nullptr };
        log_error(response);
        callback(move(response));
        return false;
    }

    dbgln_if(MATRIX_DEBUG, "[Matrix] Response:\n{}", json->to_string());

    if (!json->is_object()) {
        ErrorResponse response { 0, "_JSON_RESPONSE_NOT_OBJECT", "The JSON response is not a JSON object.", nullptr };
        log_error(response);
        callback(move(response));
        return false;
    }

    auto& json_object = json->as_object();

    if (*status_code < 400) {
        callback(json_object);
        return true;
    }

    if (!json_object.has("errcode") || !json_object.has("error")) {
        ErrorResponse response { 0, "_JSON_ERROR_RESPONSE_MISSING_FIELDS", "Error reponse is missing 'errcode' or 'error' field.", nullptr };
        log_error(response);
        callback(move(response));
        return false;
    }

    auto errcode = json_object.get("errcode");
    auto error = json_object.get("error");

    if (!errcode.is_string() || !error.is_string()) {
        ErrorResponse response { 0, "_JSON_ERROR_FIELDS_NOT_STRING", "Error reponse field 'errcode' or 'error' is not a string.", nullptr };
        log_error(response);
        callback(move(response));
        return false;
    }

    ErrorResponse response { *status_code, errcode.as_string(), error.as_string(), &json_object };
    log_error(response);
    callback(move(response));
    return false;
}

void Connection::send_request(String const& method, String const& url_suffix, StringView const& body, Callback user_callback, Function<void(Result<JsonObject, ErrorResponse>)> callback)
{
    auto full_url = m_api_base_url.complete_url(url_suffix);
    auto url_string = full_url.serialize();
    VERIFY(full_url.is_valid());

    dbgln_if(MATRIX_DEBUG, "[Matrix] Sending request to {}", url_string);
    auto request = m_request_client->start_request(method, full_url, m_request_headers, body.bytes());
    if (!request) {
        dbgln("[Matrix] RequestClient failed to start request.");
        return;
    }

    request->on_buffered_request_finish = [this, request, callback = move(callback), user_callback = move(user_callback), url_string](auto success, auto, auto&, auto response_code, auto response) mutable {
        auto success_for_user = handle_response(url_string, success, response_code, response, callback);
        if (user_callback) {
            dbgln_if(MATRIX_DEBUG, "[Matrix] Calling the user callback with success={}", success_for_user);
            user_callback(success_for_user);
        }

        deferred_invoke([request](auto&) mutable {
            request->on_buffered_request_finish = nullptr;
        });
    };
    request->set_should_buffer_all_input(true);
}

}
