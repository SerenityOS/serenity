/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Parser/ParseError.h"

namespace JSSpecCompiler {

NonnullRefPtr<ParseError> ParseError::create(String message, XML::Node const* node)
{
    return make_ref_counted<ParseError>(move(message), node);
}

NonnullRefPtr<ParseError> ParseError::create(StringView message, XML::Node const* node)
{
    return create(MUST(String::from_utf8(message)), node);
}

// FIXME: Remove once String::formatted becomes infallible.
NonnullRefPtr<ParseError> ParseError::create(ErrorOr<String> message, XML::Node const* node)
{
    return create(MUST(message), node);
}

String ParseError::to_string() const
{
    StringBuilder builder;
    builder.appendff("error: {}\n", m_message);

    XML::Node const* current = m_node;
    while (current != nullptr) {
        builder.appendff("  at {}:{} ", current->offset.line + 1, current->offset.column + 1);
        if (current->is_element()) {
            builder.append("<"sv);
            builder.append(current->as_element().name);
            for (auto [key, value] : current->as_element().attributes)
                builder.appendff(" {}=\"{}\"", key, value);
            builder.append(">\n"sv);
        } else if (current->is_text()) {
            builder.appendff("text \"{}\"\n", current->as_text().builder.string_view().trim_whitespace());
        } else {
            builder.appendff("comment");
        }
        current = current->parent;
    }
    return MUST(builder.to_string());
}

}
