/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
        ASSERT_NOT_REACHED();
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

    return builder.to_string();

    //dbg() << "[" << String::format("%42s", state_name(m_state)) << "] " << builder.to_string();
    //m_current_token = {};
}

}
