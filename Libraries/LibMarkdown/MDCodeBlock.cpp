#include <AK/StringBuilder.h>
#include <LibMarkdown/MDCodeBlock.h>

MDText::Style MDCodeBlock::style() const
{
    if (m_style_spec.spans().is_empty())
        return {};
    return m_style_spec.spans()[0].style;
}

String MDCodeBlock::style_language() const
{
    if (m_style_spec.spans().is_empty())
        return {};
    return m_style_spec.spans()[0].text;
}

String MDCodeBlock::render_to_html() const
{
    StringBuilder builder;

    String style_language = this->style_language();
    MDText::Style style = this->style();

    if (style.strong)
        builder.append("<b>");
    if (style.emph)
        builder.append("<i>");

    if (style_language.is_null())
        builder.append("<code style=\"white-space: pre;\">");
    else
        builder.appendf("<code style=\"white-space: pre;\" class=\"%s\">", style_language.characters());

    // TODO: This should also be done in other places.
    for (size_t i = 0; i < m_code.length(); i++)
        if (m_code[i] == '<')
            builder.append("&lt;");
        else if (m_code[i] == '>')
            builder.append("&gt;");
        else if (m_code[i] == '&')
            builder.append("&amp;");
        else
            builder.append(m_code[i]);

    builder.append("</code>");

    if (style.emph)
        builder.append("</i>");
    if (style.strong)
        builder.append("</b>");

    builder.append('\n');

    return builder.build();
}

String MDCodeBlock::render_for_terminal() const
{
    StringBuilder builder;

    MDText::Style style = this->style();
    bool needs_styling = style.strong || style.emph;
    if (needs_styling) {
        builder.append("\033[");
        bool first = true;
        if (style.strong) {
            builder.append('1');
            first = false;
        }
        if (style.emph) {
            if (!first)
                builder.append(';');
            builder.append('4');
        }
        builder.append('m');
    }

    builder.append(m_code);

    if (needs_styling)
        builder.append("\033[0m");

    builder.append("\n\n");

    return builder.build();
}

bool MDCodeBlock::parse(Vector<StringView>::ConstIterator& lines)
{
    if (lines.is_end())
        return false;

    constexpr auto tick_tick_tick = "```";

    StringView line = *lines;
    if (!line.starts_with(tick_tick_tick))
        return false;

    // Our Markdown extension: we allow
    // specifying a style and a language
    // for a code block, like so:
    //
    // ```**sh**
    // $ echo hello friends!
    // ````
    //
    // The code block will be made bold,
    // and if possible syntax-highlighted
    // as appropriate for a shell script.
    StringView style_spec = line.substring_view(3, line.length() - 3);
    bool success = m_style_spec.parse(style_spec);
    ASSERT(success);

    ++lines;

    bool first = true;
    StringBuilder builder;

    while (true) {
        if (lines.is_end())
            break;
        line = *lines;
        ++lines;
        if (line == tick_tick_tick)
            break;
        if (!first)
            builder.append('\n');
        builder.append(line);
        first = false;
    }

    m_code = builder.build();
    return true;
}
