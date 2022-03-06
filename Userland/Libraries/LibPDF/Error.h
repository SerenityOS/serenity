/*
 * Copyright (c) 2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>

namespace PDF {

class Error {
public:
    enum class Type {
        Parse,
        Internal,
        MalformedPDF,
    };

    Error(Type type, String const& message)
        : m_type(type)
    {
        switch (type) {
        case Type::Parse:
            m_message = String::formatted("Failed to parse PDF file: {}", message);
            break;
        case Type::Internal:
            m_message = String::formatted("Internal error while processing PDF file: {}", message);
            break;
        case Type::MalformedPDF:
            m_message = String::formatted("Malformed PDF file: {}", message);
            break;
        }
    }

    Type type() const { return m_type; }
    String const& message() const { return m_message; }

private:
    Type m_type;
    String m_message;
};

template<typename T>
using PDFErrorOr = ErrorOr<T, Error>;

}
