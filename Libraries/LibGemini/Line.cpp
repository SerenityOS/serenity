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

#include <AK/StringBuilder.h>
#include <LibGemini/Document.h>

namespace Gemini {

String Text::render_to_html() const
{
    StringBuilder builder;
    builder.append(escape_html_entities(m_text));
    builder.append("<br>\n");
    return builder.build();
}

Text::~Text()
{
}

String Heading::render_to_html() const
{
    StringBuilder builder;
    builder.appendf("<h%d>", m_level);
    builder.append(escape_html_entities(m_text.substring_view(m_level, m_text.length() - m_level)));
    builder.appendf("</h%d>", m_level);
    return builder.build();
}
Heading::~Heading()
{
}

String UnorderedList::render_to_html() const
{
    // 1.3.5.4.2 "Advanced clients can take the space of the bullet symbol into account"
    //           FIXME: The spec is unclear about what the space means, or where it goes
    //                  somehow figure this out
    StringBuilder builder;
    builder.append("<li>");
    builder.append(escape_html_entities(m_text.substring_view(1, m_text.length() - 1)));
    builder.append("</li>");
    return builder.build();
}
UnorderedList::~UnorderedList()
{
}

String Control::render_to_html() const
{
    switch (m_kind) {
    case Kind::PreformattedEnd:
        return "</pre>";
    case Kind::PreformattedStart:
        return "<pre>";
    case Kind::UnorderedListStart:
        return "<ul>";
    case Kind::UnorderedListEnd:
        return "</ul>";
    default:
        dbg() << "Unknown control kind _" << m_kind << "_";
        ASSERT_NOT_REACHED();
        return "";
    }
}
Control::~Control()
{
}

Link::Link(String text, const Document& document)
    : Line(move(text))
{
    size_t index = 2;
    while (index < m_text.length() && (m_text[index] == ' ' || m_text[index] == '\t'))
        ++index;
    auto url_string = m_text.substring_view(index, m_text.length() - index);
    auto space_offset = url_string.find_first_of(" \t");
    String url = url_string;
    if (space_offset.has_value()) {
        url = url_string.substring_view(0, space_offset.value());
        auto offset = space_offset.value();
        while (offset < url_string.length() && (url_string[offset] == ' ' || url_string[offset] == '\t'))
            ++offset;
        m_name = url_string.substring_view(offset, url_string.length() - offset);
    }
    m_url = document.url().complete_url(url);
    if (m_name.is_null())
        m_name = m_url.to_string();
}
Link::~Link()
{
}

String Link::render_to_html() const
{
    StringBuilder builder;
    builder.append("<a href=\"");
    builder.append(escape_html_entities(m_url.to_string()));
    builder.append("\">");
    builder.append(escape_html_entities(m_name));
    builder.append("</a><br>\n");
    return builder.build();
}

String Preformatted::render_to_html() const
{
    StringBuilder builder;
    builder.append(escape_html_entities(m_text));
    builder.append("\n");

    return builder.build();
}
Preformatted::~Preformatted()
{
}

Line::~Line()
{
}

}
