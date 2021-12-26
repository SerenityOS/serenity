#include <AK/StringBuilder.h>
#include <AK/Utf8View.h>
#include <LibCore/CDirIterator.h>
#include <LibDraw/Font.h>
#include <LibGUI/GPainter.h>
#include <LibHTML/Layout/LayoutBlock.h>
#include <LibHTML/Layout/LayoutText.h>
#include <ctype.h>

LayoutText::LayoutText(const Text& text)
    : LayoutNode(&text)
{
    set_inline(true);
}

LayoutText::~LayoutText()
{
}

static bool is_all_whitespace(const String& string)
{
    for (int i = 0; i < string.length(); ++i) {
        if (!isspace(string[i]))
            return false;
    }
    return true;
}

const String& LayoutText::text_for_style(const StyleProperties& style) const
{
    static String one_space = " ";
    if (is_all_whitespace(node().data())) {
        if (style.string_or_fallback(CSS::PropertyID::WhiteSpace, "normal") == "normal")
            return one_space;
    }
    return node().data();
}

void LayoutText::render_fragment(RenderingContext& context, const LineBoxFragment& fragment) const
{
    auto& painter = context.painter();
    painter.set_font(style().font());

    auto color = style().color_or_fallback(CSS::PropertyID::Color, document(), Color::Black);
    auto text_decoration = style().string_or_fallback(CSS::PropertyID::TextDecoration, "none");

    bool is_underline = text_decoration == "underline";
    if (is_underline)
        painter.draw_line(fragment.rect().bottom_left().translated(0, -1), fragment.rect().bottom_right().translated(0, -1), color);

    painter.draw_text(fragment.rect(), m_text_for_rendering.substring_view(fragment.start(), fragment.length()), TextAlignment::TopLeft, color);
}

template<typename Callback>
void LayoutText::for_each_word(Callback callback) const
{
    Utf8View view(m_text_for_rendering);
    if (view.is_empty())
        return;

    auto start_of_word = view.begin();

    auto commit_word = [&](auto it) {
        int start = view.byte_offset_of(start_of_word);
        int length = view.byte_offset_of(it) - view.byte_offset_of(start_of_word);

        if (length > 0) {
            callback(view.substring_view(start, length), start, length);
        }

        start_of_word = it;
    };

    bool last_was_space = isspace(*view.begin());

    for (auto it = view.begin(); it != view.end();) {
        bool is_space = isspace(*it);
        if (is_space == last_was_space) {
            ++it;
            continue;
        }
        last_was_space = is_space;
        commit_word(it);
        ++it;
    }
    if (start_of_word != view.end())
        commit_word(view.end());
}

template<typename Callback>
void LayoutText::for_each_source_line(Callback callback) const
{
    Utf8View view(m_text_for_rendering);
    if (view.is_empty())
        return;

    auto start_of_line = view.begin();

    auto commit_line = [&](auto it) {
        int start = view.byte_offset_of(start_of_line);
        int length = view.byte_offset_of(it) - view.byte_offset_of(start_of_line);

        if (length > 0) {
            callback(view.substring_view(start, length), start, length);
        }
    };

    for (auto it = view.begin(); it != view.end();) {
        bool did_commit = false;
        if (*it == '\n') {
            commit_line(it);
            did_commit = true;
        }
        ++it;
        if (did_commit)
            start_of_line = it;
    }
    if (start_of_line != view.end())
        commit_line(view.end());
}

void LayoutText::split_into_lines(LayoutBlock& container)
{
    auto& font = style().font();
    int space_width = font.glyph_width(' ') + font.glyph_spacing();
    int line_height = style().line_height();

    auto& line_boxes = container.line_boxes();
    if (line_boxes.is_empty())
        line_boxes.append(LineBox());
    int available_width = container.width() - line_boxes.last().width();

    bool is_preformatted = style().string_or_fallback(CSS::PropertyID::WhiteSpace, "normal") == "pre";
    if (is_preformatted) {
        m_text_for_rendering = node().data();
        for_each_source_line([&](const Utf8View& view, int start, int length) {
            line_boxes.last().add_fragment(*this, start, length, font.width(view), line_height);
            line_boxes.append(LineBox());
        });
        return;
    }

    // Collapse whitespace into single spaces
    auto utf8_view = Utf8View(node().data());
    StringBuilder builder(node().data().length());
    for (auto it = utf8_view.begin(); it != utf8_view.end(); ++it) {
        if (!isspace(*it)) {
            builder.append(utf8_view.as_string().characters_without_null_termination() + utf8_view.byte_offset_of(it), it.codepoint_length_in_bytes());
        } else {
            builder.append(' ');
            auto prev = it;
            while (it != utf8_view.end() && isspace(*it)) {
                prev = it;
                ++it;
            }
            it = prev;
        }
    }
    m_text_for_rendering = builder.to_string();

    struct Word {
        Utf8View view;
        int start;
        int length;
    };
    Vector<Word> words;

    for_each_word([&](const Utf8View& view, int start, int length) {
        words.append({ Utf8View(view), start, length });
    });

    for (int i = 0; i < words.size(); ++i) {
        auto& word = words[i];

        int word_width;
        bool is_whitespace = isspace(*word.view.begin());

        if (is_whitespace)
            word_width = space_width;
        else
            word_width = font.width(word.view) + font.glyph_spacing();

        if (word_width > available_width) {
            line_boxes.append(LineBox());
            available_width = container.width();
        }

        if (is_whitespace && line_boxes.last().fragments().is_empty())
            continue;

        line_boxes.last().add_fragment(*this, word.start, is_whitespace ? 1 : word.length, word_width, line_height);
        available_width -= word_width;

        if (available_width < 0) {
            line_boxes.append(LineBox());
            available_width = container.width();
        }
    }
}
