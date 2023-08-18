/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <LibXML/DOM/Node.h>

namespace JSSpecCompiler {

class ParseError : public RefCounted<ParseError> {
public:
    ParseError(String&& message, XML::Node const* node)
        : m_message(move(message))
        , m_node(node)
    {
    }

    static NonnullRefPtr<ParseError> create(String message, XML::Node const* node);
    static NonnullRefPtr<ParseError> create(StringView message, XML::Node const* node);
    static NonnullRefPtr<ParseError> create(ErrorOr<String> message, XML::Node const* node);

    String to_string() const;

private:
    String m_message;
    XML::Node const* m_node;
    // TODO: Support chained parse errors
};

template<typename T>
using ParseErrorOr = ErrorOr<T, NonnullRefPtr<ParseError>>;

}
