#include <AK/StringBuilder.h>
#include <LibMarkdown/MDList.h>

String MDList::render_to_html() const
{
    StringBuilder builder;

    const char* tag = m_is_ordered ? "ol" : "ul";
    builder.appendf("<%s>", tag);

    for (auto& item : m_items) {
        builder.append("<li>");
        builder.append(item.render_to_html());
        builder.append("</li>\n");
    }

    builder.appendf("</%s>\n", tag);

    return builder.build();
}

String MDList::render_for_terminal() const
{
    StringBuilder builder;

    int i = 0;
    for (auto& item : m_items) {
        builder.append("  ");
        if (m_is_ordered)
            builder.appendf("%d. ", ++i);
        else
            builder.append("* ");
        builder.append(item.render_for_terminal());
        builder.append("\n");
    }
    builder.append("\n");

    return builder.build();
}

bool MDList::parse(Vector<StringView>::ConstIterator& lines)
{
    bool first = true;
    while (true) {
        if (lines.is_end())
            break;
        const StringView& line = *lines;
        if (line.is_empty())
            break;

        bool appears_unordered = false;
        size_t offset = 0;
        if (line.length() > 2)
            if (line[1] == ' ' && (line[0] == '*' || line[0] == '-')) {
                appears_unordered = true;
                offset = 2;
            }

        bool appears_ordered = false;
        for (size_t i = 0; i < 10 && i < line.length(); i++) {
            char ch = line[i];
            if ('0' <= ch && ch <= '9')
                continue;
            if (ch == '.' || ch == ')')
                if (i + 1 < line.length() && line[i + 1] == ' ') {
                    appears_ordered = true;
                    offset = i + 1;
                }
            break;
        }

        ASSERT(!(appears_unordered && appears_ordered));
        if (!appears_unordered && !appears_ordered)
            return false;
        if (first)
            m_is_ordered = appears_ordered;
        else if (m_is_ordered != appears_ordered)
            return false;

        first = false;
        MDText text;
        bool success = text.parse(line.substring_view(offset, line.length() - offset));
        ASSERT(success);
        m_items.append(move(text));
        ++lines;
    }

    return !first;
}
