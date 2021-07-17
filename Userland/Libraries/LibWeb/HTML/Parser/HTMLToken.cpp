/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/Parser/HTMLToken.h>

namespace Web::HTML {

String HTMLToken::to_string() const
{
    StringBuilder builder;

    switch (type()) {
    case HTMLToken::Type::DOCTYPE:
        builder.append("DOCTYPE");
        builder.append(" { name: '");
        builder.append(doctype_data().name);
        builder.append("' }");
        break;
    case HTMLToken::Type::StartTag:
        builder.append("StartTag");
        break;
    case HTMLToken::Type::EndTag:
        builder.append("EndTag");
        break;
    case HTMLToken::Type::Comment:
        builder.append("Comment");
        break;
    case HTMLToken::Type::Character:
        builder.append("Character");
        break;
    case HTMLToken::Type::EndOfFile:
        builder.append("EndOfFile");
        break;
    case HTMLToken::Type::Invalid:
        VERIFY_NOT_REACHED();
    }

    if (type() == HTMLToken::Type::StartTag || type() == HTMLToken::Type::EndTag) {
        builder.append(" { name: '");
        builder.append(tag_name());
        builder.append("', { ");
        for_each_attribute([&](auto& attribute) {
            builder.append(attribute.local_name);
            builder.append("=\"");
            builder.append(attribute.value);
            builder.append("\" ");
            return IterationDecision::Continue;
        });
        builder.append("} }");
    }

    if (is_comment()) {
        builder.append(" { data: '");
        builder.append(comment());
        builder.append("' }");
    }

    if (is_character()) {
        builder.append(" { data: '");
        builder.append_code_point(code_point());
        builder.append("' }");
    }

    if (type() == HTMLToken::Type::Character) {
        builder.appendff("@{}:{}", m_start_position.line, m_start_position.column);
    } else {
        builder.appendff("@{}:{}-{}:{}", m_start_position.line, m_start_position.column, m_end_position.line, m_end_position.column);
    }

    return builder.to_string();
}

}
