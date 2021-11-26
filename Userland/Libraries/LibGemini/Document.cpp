/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
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

NonnullRefPtr<Document> Document::parse(StringView lines, const URL& url)
{
    auto document = adopt_ref(*new Document(url));
    document->read_lines(lines);
    return document;
}

void Document::read_lines(StringView source)
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
