/*
 * Copyright (c) 2020, The SerenityOS developers.
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

#include <AK/NonnullRefPtr.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <LibGemini/Document.h>

namespace Gemini {

String Document::render_to_html() const
{
    StringBuilder html_builder;
    html_builder.append("<!DOCTYPE html>\n<html>\n");
    html_builder.append("<head>\n<title>");
    html_builder.append(m_url.path());
    html_builder.append("</title>\n</head>\n");
    html_builder.append("<body>\n");
    for (auto& line : m_lines) {
        html_builder.append(line.render_to_html());
    }
    html_builder.append("</body>");
    html_builder.append("</html>");
    return html_builder.build();
}

NonnullRefPtr<Document> Document::parse(const StringView& lines, const URL& url)
{
    auto document = adopt(*new Document(url));
    document->read_lines(lines);
    return document;
}

void Document::read_lines(const StringView& source)
{
    auto close_list_if_needed = [&] {
        if (m_inside_unordered_list) {
            m_inside_unordered_list = false;
            m_lines.append(make<Control>(Control::UnorderedListEnd));
        }
    };

    for (auto& line : source.lines()) {
        if (line.starts_with("```")) {
            close_list_if_needed();

            m_inside_preformatted_block = !m_inside_preformatted_block;
            if (m_inside_preformatted_block) {
                m_lines.append(make<Control>(Control::PreformattedStart));
            } else {
                m_lines.append(make<Control>(Control::PreformattedEnd));
            }
            continue;
        }

        if (m_inside_preformatted_block) {
            m_lines.append(make<Preformatted>(move(line)));
            continue;
        }

        if (line.starts_with("*")) {
            if (!m_inside_unordered_list)
                m_lines.append(make<Control>(Control::UnorderedListStart));
            m_lines.append(make<UnorderedList>(move(line)));
            m_inside_unordered_list = true;
            continue;
        }

        close_list_if_needed();

        if (line.starts_with("=>")) {
            m_lines.append(make<Link>(move(line), *this));
            continue;
        }

        if (line.starts_with("#")) {
            size_t level = 0;
            while (line.length() > level && line[level] == '#')
                ++level;

            m_lines.append(make<Heading>(move(line), level));
            continue;
        }

        m_lines.append(make<Text>(move(line)));
    }
}

}
