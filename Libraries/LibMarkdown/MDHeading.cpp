#include <AK/StringBuilder.h>
#include <LibMarkdown/MDHeading.h>

String MDHeading::render_to_html() const
{
    StringBuilder builder;
    builder.appendf("<h%d>", m_level);
    builder.append(m_text.render_to_html());
    builder.appendf("</h%d>\n", m_level);
    return builder.build();
}

String MDHeading::render_for_terminal() const
{
    StringBuilder builder;

    switch (m_level) {
    case 1:
    case 2:
        builder.append("\n\033[1m");
        builder.append(m_text.render_for_terminal().to_uppercase());
        builder.append("\033[0m\n");
        break;
    default:
        builder.append("\n\033[1m");
        builder.append(m_text.render_for_terminal());
        builder.append("\033[0m\n");
        break;
    }

    return builder.build();
}

bool MDHeading::parse(Vector<StringView>::ConstIterator& lines)
{
    if (lines.is_end())
        return false;

    const StringView& line = *lines;

    for (m_level = 0; m_level < (int)line.length(); m_level++)
        if (line[(size_t)m_level] != '#')
            break;

    if (m_level >= (int)line.length() || line[(size_t)m_level] != ' ')
        return false;

    StringView title_view = line.substring_view((size_t)m_level + 1, line.length() - (size_t)m_level - 1);
    bool success = m_text.parse(title_view);
    ASSERT(success);

    ++lines;
    return true;
}
