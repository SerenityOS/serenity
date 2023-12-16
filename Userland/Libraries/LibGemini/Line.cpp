/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibGemini/Document.h>

namespace Gemini {

ByteString Text::render_to_html() const
{
    StringBuilder builder;
    builder.append(escape_html_entities(m_text));
    builder.append("<br>\n"sv);
    return builder.to_byte_string();
}

ByteString Heading::render_to_html() const
{
    return ByteString::formatted("<h{}>{}</h{}>", m_level, escape_html_entities(m_text.substring_view(m_level, m_text.length() - m_level)), m_level);
}

ByteString UnorderedList::render_to_html() const
{
    // 1.3.5.4.2 "Advanced clients can take the space of the bullet symbol into account"
    //           FIXME: The spec is unclear about what the space means, or where it goes
    //                  somehow figure this out
    StringBuilder builder;
    builder.append("<li>"sv);
    builder.append(escape_html_entities(m_text.substring_view(1, m_text.length() - 1)));
    builder.append("</li>"sv);
    return builder.to_byte_string();
}

ByteString Control::render_to_html() const
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
        dbgln("Unknown control kind _{}_", (int)m_kind);
        VERIFY_NOT_REACHED();
        return "";
    }
}

Link::Link(ByteString text, Document const& document)
    : Line(move(text))
{
    size_t index = 2;
    while (index < m_text.length() && (m_text[index] == ' ' || m_text[index] == '\t'))
        ++index;
    auto url_string = m_text.substring_view(index, m_text.length() - index);
    auto space_offset = url_string.find_any_of(" \t"sv);
    ByteString url = url_string;
    if (space_offset.has_value()) {
        url = url_string.substring_view(0, space_offset.value());
        auto offset = space_offset.value();
        while (offset < url_string.length() && (url_string[offset] == ' ' || url_string[offset] == '\t'))
            ++offset;
        m_name = url_string.substring_view(offset, url_string.length() - offset);
    }
    m_url = document.url().complete_url(url);
    if (m_name.is_empty())
        m_name = m_url.to_byte_string();
}

ByteString Link::render_to_html() const
{
    StringBuilder builder;
    builder.append("<a href=\""sv);
    builder.append(escape_html_entities(m_url.to_byte_string()));
    builder.append("\">"sv);
    builder.append(escape_html_entities(m_name));
    builder.append("</a><br>\n"sv);
    return builder.to_byte_string();
}

ByteString Preformatted::render_to_html() const
{
    StringBuilder builder;
    builder.append(escape_html_entities(m_text));
    builder.append('\n');

    return builder.to_byte_string();
}

}
