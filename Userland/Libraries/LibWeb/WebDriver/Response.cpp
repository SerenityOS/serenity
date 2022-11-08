/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibIPC/Decoder.h>
#include <LibIPC/Encoder.h>
#include <LibWeb/WebDriver/Response.h>

enum class ResponseType : u8 {
    Success,
    Error,
};

namespace Web::WebDriver {

Response::Response(JsonValue&& value)
    : m_value_or_error(move(value))
{
}

Response::Response(Error&& error)
    : m_value_or_error(move(error))
{
}

}

bool IPC::encode(Encoder& encoder, Web::WebDriver::Response const& response)
{
    response.visit(
        [](Empty) { VERIFY_NOT_REACHED(); },
        [&](JsonValue const& value) {
            encoder << ResponseType::Success;
            encoder << value;
        },
        [&](Web::WebDriver::Error const& error) {
            encoder << ResponseType::Error;
            encoder << error.http_status;
            encoder << error.error;
            encoder << error.message;
            encoder << error.data;
        });

    return true;
}

ErrorOr<void> IPC::decode(Decoder& decoder, Web::WebDriver::Response& response)
{
    ResponseType type {};
    TRY(decoder.decode(type));

    switch (type) {
    case ResponseType::Success: {
        JsonValue value;
        TRY(decoder.decode(value));

        response = move(value);
        break;
    }

    case ResponseType::Error: {
        Web::WebDriver::Error error {};
        TRY(decoder.decode(error.http_status));
        TRY(decoder.decode(error.error));
        TRY(decoder.decode(error.message));
        TRY(decoder.decode(error.data));

        response = move(error);
        break;
    }
    }

    return {};
}
