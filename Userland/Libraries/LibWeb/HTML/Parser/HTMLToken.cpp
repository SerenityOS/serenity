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
        builder.append(m_doctype.name.to_string());
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
        builder.append(m_tag.tag_name.to_string());
        builder.append("', { ");
        for (auto& attribute : m_tag.attributes) {
            builder.append(attribute.local_name_builder.to_string());
            builder.append("=\"");
            builder.append(attribute.value_builder.to_string());
            builder.append("\" ");
        }
        builder.append("} }");
    }

    if (type() == HTMLToken::Type::Comment || type() == HTMLToken::Type::Character) {
        builder.append(" { data: '");
        builder.append(m_comment_or_character.data.to_string());
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
