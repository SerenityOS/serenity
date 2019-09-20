#include <AK/StringBuilder.h>
#include <LibMarkdown/MDParagraph.h>

String MDParagraph::render_to_html() const
{
    StringBuilder builder;
    builder.appendf("<p>");
    builder.append(m_text.render_to_html());
    builder.appendf("</p>\n");
    return builder.build();
}

String MDParagraph::render_for_terminal() const
{
    StringBuilder builder;
    builder.append(m_text.render_for_terminal());
    builder.appendf("\n\n");
    return builder.build();
}

bool MDParagraph::parse(Vector<StringView>::ConstIterator& lines)
{
    if (lines.is_end())
        return false;

    bool first = true;
    StringBuilder builder;

    while (true) {
        if (lines.is_end())
            break;
        StringView line = *lines;
        if (line.is_empty())
            break;
        char ch = line[0];
        // See if it looks like a blockquote
        // or like an indented block.
        if (ch == '>' || ch == ' ')
            break;
        if (line.length() > 1) {
            // See if it looks like a heading.
            if (ch == '#' && (line[1] == '#' || line[1] == ' '))
                break;
            // See if it looks like a code block.
            if (ch == '`' && line[1] == '`')
                break;
            // See if it looks like a list.
            if (ch == '*' || ch == '-')
                if (line[1] == ' ')
                    break;
        }

        if (!first)
            builder.append(' ');
        builder.append(line);
        first = false;
        ++lines;
    }

    if (first)
        return false;

    bool success = m_text.parse(builder.build());
    ASSERT(success);
    return true;
}
