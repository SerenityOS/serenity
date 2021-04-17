/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
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

#include <AK/StringView.h>
#include <Libraries/LibTemplate/TemplateParser.h>

#include <ctype.h>

namespace Template {

const auto UNEXPECTED_EOF = "Unexpected end of file";

Result<Template, String> TemplateParser::parse()
{
    Template built_template;
    while (tell_remaining() > 0) {
        char next = peek();
        switch (next) {
        case '{': {
            if (tell_remaining() < 2)
                return String(UNEXPECTED_EOF);
            if (peek(1) == '{') {
                consume(2);
                built_template.add_component({ TemplateComponentType::STRING_LITERAL, "{" });
            } else {
                ignore();
                ignore_while([](char c) { return isspace(c); });

                if (tell_remaining() == 0)
                    return String(UNEXPECTED_EOF);
                if (char head = peek(); !isalnum(head))
                    return String::formatted("Unexpected character '{}'", head);

                auto identifier = consume_while([](char c) { return isalnum(c); });

                ignore_while([](char c) { return isspace(c); });

                if (tell_remaining() == 0)
                    return String(UNEXPECTED_EOF);
                if (char head = consume(); head != '}')
                    return String::formatted("Expected '}}', found '{}'", head);
                built_template.add_component({ TemplateComponentType::VARIABLE, identifier });
            }
            break;
        }
        case '}': {
            if (tell_remaining() < 2)
                return String(UNEXPECTED_EOF);
            if (peek(1) == '}') {
                consume(2);
                built_template.add_component({ TemplateComponentType::STRING_LITERAL, "}" });
            } else {
                return String("Unmatched }");
            }
            break;
        }
        default: {
            auto tokens = consume_until([](char c) { return c == '{' || c == '}'; });
            built_template.add_component({ TemplateComponentType::STRING_LITERAL, tokens });
        }
        }
    }
    return built_template;
}

}
