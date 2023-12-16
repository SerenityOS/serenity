/*
 * Copyright (c) 2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/String.h>
#include <AK/Vector.h>

namespace PDF {

class [[nodiscard]] Error {
public:
    enum class Type {
        Parse,
        Internal,
        MalformedPDF,
        RenderingUnsupported
    };

    Error(AK::Error error)
        : m_type(Type::Internal)
        , m_message(ByteString::formatted("Internal error while processing PDF file: {}", error.string_literal()))
    {
    }

    Error(Type type, ByteString const& message)
        : Error(type, String::from_byte_string(message).release_value_but_fixme_should_propagate_errors())
    {
    }

    Error(Type type, String const& message)
        : m_type(type)
    {
        switch (type) {
        case Type::Parse:
            m_message = ByteString::formatted("Failed to parse PDF file: {}", message);
            break;
        case Type::Internal:
            m_message = ByteString::formatted("Internal error while processing PDF file: {}", message);
            break;
        case Type::MalformedPDF:
            m_message = ByteString::formatted("Malformed PDF file: {}", message);
            break;
        case Type::RenderingUnsupported:
            m_message = ByteString::formatted("Rendering of feature not supported: {}", message);
            break;
        }
    }

    Type type() const { return m_type; }
    ByteString const& message() const { return m_message; }

#define DEFINE_STATIC_ERROR_FUNCTIONS(name, type)                                                           \
    static Error name##_error(StringView message)                                                           \
    {                                                                                                       \
        return maybe_with_string(Type::type, String::from_utf8(message));                                   \
    }                                                                                                       \
                                                                                                            \
    template<typename... Parameters>                                                                        \
    static Error name##_error(CheckedFormatString<Parameters...>&& fmtstr, Parameters const&... parameters) \
    {                                                                                                       \
        return maybe_with_string(Type::type, String::formatted(move(fmtstr), parameters...));               \
    }

    DEFINE_STATIC_ERROR_FUNCTIONS(parse, Parse)
    DEFINE_STATIC_ERROR_FUNCTIONS(internal, Internal)
    DEFINE_STATIC_ERROR_FUNCTIONS(malformed, MalformedPDF)
    DEFINE_STATIC_ERROR_FUNCTIONS(rendering_unsupported, RenderingUnsupported)

private:
    Type m_type;
    ByteString m_message;

    static Error maybe_with_string(Type type, ErrorOr<String> maybe_string)
    {
        if (maybe_string.is_error())
            return Error { type, String {} };
        return Error { type, maybe_string.release_value() };
    }
};

class Errors {

public:
    Errors() = default;
    Errors(Error&& error)
    {
        m_errors.empend(move(error));
    }

    Vector<Error> const& errors() const
    {
        return m_errors;
    }

    void add_error(Error&& error)
    {
        m_errors.empend(move(error));
    }

private:
    Vector<Error> m_errors;
};

template<typename T>
using PDFErrorOr = ErrorOr<T, Error>;

template<typename T>
using PDFErrorsOr = ErrorOr<T, Errors>;

}
