/*
 * Copyright (c) 2024, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/AsyncStream.h>
#include <AK/ByteString.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/OwnPtr.h>
#include <AK/TemporaryChange.h>
#include <AK/Vector.h>

namespace HTTP {

class Http11Connection;
class Http11Response;

#define ENUMERATE_METHODS(F) \
    F(Invalid)               \
    F(HEAD)                  \
    F(GET)                   \
    F(POST)                  \
    F(DELETE)                \
    F(PATCH)                 \
    F(OPTIONS)               \
    F(TRACE)                 \
    F(CONNECT)               \
    F(PUT)

enum class Method {
#define ID(x) x,
    ENUMERATE_METHODS(ID)
#undef ID
};

struct Header {
    ByteString header;
    ByteString value;
};

struct RequestData {
    struct PlainBody {
        StringView data;
    };

    Method method;
    StringView url;
    Vector<Header> headers;
    Variant<Empty, PlainBody> body = Empty {};
};

class Http11Response final : public StreamWrapper<AsyncInputStream> {
public:
    static Coroutine<ErrorOr<NonnullOwnPtr<Http11Response>>> create(Badge<Http11Connection>, RequestData&& data, AsyncStream& stream);

    u16 status_code() const { return m_status_code; }
    Vector<Header> const& headers() const { return m_headers; }

    AsyncInputStream& body() { return *m_stream; }

private:
    Http11Response(NonnullOwnPtr<AsyncInputStream>&& body, u16 status_code, Vector<Header>&& headers)
        : StreamWrapper(move(body))
        , m_status_code(status_code)
        , m_headers(move(headers))
    {
    }

    u16 m_status_code { 0 };
    Vector<Header> m_headers;
};

class Http11Connection final : public StreamWrapper<AsyncStream> {
public:
    using StreamWrapper::StreamWrapper;

    template<
        typename Func,
        typename T = InvokeResult<Func, Http11Response&>::ReturnType::ResultType>
    Coroutine<ErrorOr<T>> request(RequestData&& data, Func&& func)
    {
        VERIFY(!m_request_in_flight);
        TemporaryChange request_in_flight { m_request_in_flight, true };

        auto response = CO_TRY(co_await Http11Response::create({}, move(data), *m_stream));
        auto result = co_await func(*response);
        if (response->is_open()) {
            auto close_result = co_await response->close();
            if (!result.is_error()) // Preserve callback error in case it has failed.
                CO_TRY(move(close_result));
        }
        co_return result;
    }

private:
    bool m_request_in_flight { false };
};

}
