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
        builder.append("DOCTYPE"sv);
        builder.append(" { name: '"sv);
        builder.append(doctype_data().name);
        builder.append("' }"sv);
        break;
    case HTMLToken::Type::StartTag:
        builder.append("StartTag"sv);
        break;
    case HTMLToken::Type::EndTag:
        builder.append("EndTag"sv);
        break;
    case HTMLToken::Type::Comment:
        builder.append("Comment"sv);
        break;
    case HTMLToken::Type::Character:
        builder.append("Character"sv);
        break;
    case HTMLToken::Type::EndOfFile:
        builder.append("EndOfFile"sv);
        break;
    case HTMLToken::Type::Invalid:
        VERIFY_NOT_REACHED();
    }

    if (type() == HTMLToken::Type::StartTag || type() == HTMLToken::Type::EndTag) {
        builder.append(" { name: '"sv);
        builder.append(tag_name());
        builder.append("', { "sv);
        for_each_attribute([&](auto& attribute) {
            builder.append(attribute.local_name);
            builder.append("=\""sv);
            builder.append(attribute.value);
            builder.append("\" "sv);
            return IterationDecision::Continue;
        });
        builder.append("} }"sv);
    }

    if (is_comment()) {
        builder.append(" { data: '"sv);
        builder.append(comment());
        builder.append("' }"sv);
    }

    if (is_character()) {
        builder.append(" { data: '"sv);
        builder.append_code_point(code_point());
        builder.append("' }"sv);
    }

    if (type() == HTMLToken::Type::Character) {
        builder.appendff("@{}:{}", m_start_position.line, m_start_position.column);
    } else {
        builder.appendff("@{}:{}-{}:{}", m_start_position.line, m_start_position.column, m_end_position.line, m_end_position.column);
    }

    return MUST(builder.to_string());
}

}
