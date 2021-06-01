/*
 * Copyright (c) 2021, Kyle Pereira <hey@xylepereira.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <AK/Function.h>
#include <AK/Tuple.h>
#include <AK/Variant.h>
#include <LibCore/DateTime.h>
#include <LibCore/EventLoop.h>
#include <LibCore/Object.h>
#include <utility>

namespace IMAP {
enum class CommandType {
    Capability,
    Noop,
};

enum class ResponseType : unsigned {
    Capability = 1u << 0,
};

class Parser;

struct Command {
public:
    CommandType type;
    int tag;
    Vector<String> args;
};

enum class ResponseStatus {
    Bad,
    No,
    OK,
};

class ResponseData {
public:
    [[nodiscard]] unsigned response_type() const
    {
        return m_response_type;
    }

    ResponseData()
        : m_response_type(0)
    {
    }

    ResponseData(ResponseData&) = delete;
    ResponseData(ResponseData&&) = default;
    ResponseData& operator=(const ResponseData&) = delete;
    ResponseData& operator=(ResponseData&&) = default;

    [[nodiscard]] bool contains_response_type(ResponseType response_type) const
    {
        return (static_cast<unsigned>(response_type) & m_response_type) != 0;
    }

    void add_response_type(ResponseType response_type)
    {
        m_response_type = m_response_type | static_cast<unsigned>(response_type);
    }

    void add_capabilities(Vector<String>&& capabilities)
    {
        m_capabilities = move(capabilities);
        add_response_type(ResponseType::Capability);
    }

    Vector<String>& capabilities()
    {
        VERIFY(contains_response_type(ResponseType::Capability));
        return m_capabilities;
    }

private:
    unsigned m_response_type;

    Vector<String> m_capabilities;
};

class SolidResponse {
    // Parser is allowed to set up fields
    friend class Parser;

public:
    ResponseStatus status() { return m_status; }

    int tag() const { return m_tag; }

    ResponseData& data() { return m_data; }

    String response_text() { return m_response_text; };

    SolidResponse()
        : SolidResponse(ResponseStatus::Bad, -1)
    {
    }

    SolidResponse(ResponseStatus status, int tag)
        : m_status(status)
        , m_tag(tag)
        , m_data(ResponseData())
    {
    }

private:
    ResponseStatus m_status;
    String m_response_text;
    unsigned m_tag;

    ResponseData m_data;
};

struct ContinueRequest {
    String data;
};

template<typename Result>
class Promise : public Core::Object {
    C_OBJECT(Promise);

private:
    Optional<Result> m_pending;

public:
    Function<void(Result&)> on_resolved;

    void resolve(Result&& result)
    {
        m_pending = move(result);
        if (on_resolved)
            on_resolved(m_pending.value());
    }

    bool is_resolved()
    {
        return m_pending.has_value();
    };

    Result await()
    {
        while (!is_resolved()) {
            Core::EventLoop::current().pump();
        }
        return m_pending.release_value();
    }

    // Converts a Promise<A> to a Promise<B> using a function func: A -> B
    template<typename T>
    RefPtr<Promise<T>> map(Function<T(Result&)> func)
    {
        RefPtr<Promise<T>> new_promise = Promise<T>::construct();
        on_resolved = [new_promise, func](Result& result) mutable {
            auto t = func(result);
            new_promise->resolve(move(t));
        };
        return new_promise;
    }
};
using Response = Variant<SolidResponse, ContinueRequest>;
}

// An RFC 2822 message
// https://datatracker.ietf.org/doc/html/rfc2822
struct Message {
    String data;
};
