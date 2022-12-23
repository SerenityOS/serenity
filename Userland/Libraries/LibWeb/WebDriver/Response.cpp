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

template<>
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

template<>
ErrorOr<Web::WebDriver::Response> IPC::decode(Decoder& decoder)
{
    auto type = TRY(decoder.decode<ResponseType>());

    switch (type) {
    case ResponseType::Success:
        return TRY(decoder.decode<JsonValue>());

    case ResponseType::Error: {
        auto http_status = TRY(decoder.decode<unsigned>());
        auto error = TRY(decoder.decode<DeprecatedString>());
        auto message = TRY(decoder.decode<DeprecatedString>());
        auto data = TRY(decoder.decode<Optional<JsonValue>>());

        return Web::WebDriver::Error { http_status, move(error), move(message), move(data) };
    }
    }

    VERIFY_NOT_REACHED();
}
