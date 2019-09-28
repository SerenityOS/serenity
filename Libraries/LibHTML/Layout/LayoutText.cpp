#include <AK/StringBuilder.h>
#include <LibCore/CDirIterator.h>
#include <LibDraw/Font.h>
#include <LibGUI/GPainter.h>
#include <LibHTML/Layout/LayoutBlock.h>
#include <LibHTML/Layout/LayoutText.h>
#include <ctype.h>

LayoutText::LayoutText(const Text& text, StyleProperties&& style_properties)
    : LayoutNode(&text, move(style_properties))
{
}

LayoutText::~LayoutText()
{
}

void LayoutText::load_font()
{
    auto font_family = style_properties().string_or_fallback("font-family", "Katica");
    auto font_weight = style_properties().string_or_fallback("font-weight", "normal");

    String weight;
    if (font_weight == "lighter")
        weight = "Thin";
    else if (font_weight == "normal")
        weight = "";
    else if (font_weight == "bold")
        weight = "Bold";
    else
        ASSERT_NOT_REACHED();

    auto look_for_file = [](const StringView& expected_name) -> String {
        // TODO: handle font sizes properly?
        CDirIterator it { "/res/fonts/", CDirIterator::Flags::SkipDots };
        while (it.has_next()) {
            String name = it.next_path();
            ASSERT(name.ends_with(".font"));
            if (!name.starts_with(expected_name))
                continue;

            // Check that a numeric size immediately
            // follows the font name. This prevents,
            // for example, matching KaticaBold when
            // the regular Katica is requested.
            if (!isdigit(name[expected_name.length()]))
                continue;

            return name;
        }
        return {};
    };

    String file_name = look_for_file(String::format("%s%s", font_family.characters(), weight.characters()));
    if (file_name.is_null() && weight == "")
        file_name = look_for_file(String::format("%sRegular", font_family.characters()));

    if (file_name.is_null()) {
        dbg() << "Failed to find a font for family " << font_family << " weight " << font_weight;
        dbg() << "My text is " << node().data();
        ASSERT_NOT_REACHED();
    }

#ifdef HTML_DEBUG
    dbg() << "Found font " << file_name << " for family " << font_family << " weight " << font_weight;
#endif

    m_font = Font::load_from_file(String::format("/res/fonts/%s", file_name.characters()));
}

static bool is_all_whitespace(const String& string)
{
    for (int i = 0; i < string.length(); ++i) {
        if (!isspace(string[i]))
            return false;
    }
    return true;
}

const String& LayoutText::text() const
{
    static String one_space = " ";
    if (is_all_whitespace(node().data()))
        if (style_properties().string_or_fallback("white-space", "normal") == "normal")
            return one_space;
    return node().data();
}

static void split_first_word(const StringView& str, StringView& out_space, StringView& out_word)
{
    int first_nonspace = -1;
    for (int i = 0; i < str.length(); i++)
        if (!isspace(str[i])) {
            first_nonspace = i;
            break;
        }

    if (first_nonspace == -1) {
        out_space = str;
        out_word = {};
        return;
    }

    int first_space = str.length();
    for (int i = first_nonspace + 1; i < str.length(); i++)
        if (isspace(str[i])) {
            first_space = i;
            break;
        }

    out_space = str.substring_view(0, first_nonspace);
    out_word = str.substring_view(first_nonspace, first_space - first_nonspace);
}

void LayoutText::compute_runs()
{
    StringView remaining_text = node().data();
    if (remaining_text.is_empty())
        return;

    int right_border = containing_block()->rect().x() + containing_block()->rect().width();

    StringBuilder builder;
    Point run_origin = rect().location();

    int total_right_margin = style().full_margin().right;
    bool is_preformatted = style_properties().string_or_fallback("white-space", "normal") != "normal";

    while (!remaining_text.is_empty()) {
        String saved_text = builder.string_view();

        // Try to append a new word.
        StringView space;
        StringView word;
        split_first_word(remaining_text, space, word);

        int forced_line_break_index = -1;
        if (is_preformatted)
            for (int i = 0; i < space.length(); i++)
                if (space[i] == '\n') {
                    forced_line_break_index = i;
                    break;
                }

        if (!space.is_empty()) {
            if (!is_preformatted) {
                builder.append(' ');
            } else if (forced_line_break_index != -1) {
                builder.append(space.substring_view(0, forced_line_break_index));
            } else {
                builder.append(space);
            }
        }
        if (forced_line_break_index == -1)
            builder.append(word);

        if (forced_line_break_index != -1)
            remaining_text = remaining_text.substring_view(forced_line_break_index + 1, remaining_text.length() - forced_line_break_index - 1);
        else if (!word.is_null())
            remaining_text = remaining_text.substring_view_starting_after_substring(word);
        else
            remaining_text = {};

        // See if that fits.
        int width = m_font->width(builder.string_view());
        if (forced_line_break_index == -1 && run_origin.x() + width + total_right_margin < right_border)
            continue;

        // If it doesn't, create a run from
        // what we had there previously.
        if (forced_line_break_index == -1)
            m_runs.append({ run_origin, move(saved_text) });
        else
            m_runs.append({ run_origin, builder.string_view() });

        // Start a new run at the new line.
        int line_spacing = 4;
        run_origin.set_x(containing_block()->rect().x() + style().full_margin().left);
        run_origin.move_by(0, m_font->glyph_height() + line_spacing);
        builder = StringBuilder();
        if (forced_line_break_index != -1)
            continue;
        if (is_preformatted)
            builder.append(space);
        builder.append(word);
    }

    // Add the last run.
    m_runs.append({ run_origin, builder.build() });
}

void LayoutText::layout()
{
    ASSERT(!has_children());

    if (!m_font)
        load_font();

    int origin_x = -1;
    int origin_y = -1;
    if (previous_sibling() != nullptr) {
        auto& previous_sibling_rect = previous_sibling()->rect();
        auto& previous_sibling_style = previous_sibling()->style();
        origin_x = previous_sibling_rect.x() + previous_sibling_rect.width();
        origin_x += previous_sibling_style.full_margin().right;
        origin_y = previous_sibling_rect.y() + previous_sibling_rect.height() - m_font->glyph_height() - previous_sibling_style.full_margin().top;
    } else {
        origin_x = parent()->rect().x();
        origin_y = parent()->rect().y();
    }
    rect().set_x(origin_x + style().full_margin().left);
    rect().set_y(origin_y + style().full_margin().top);

    m_runs.clear();
    compute_runs();

    if (m_runs.is_empty())
        return;

    const Run& last_run = m_runs[m_runs.size() - 1];
    rect().set_right(last_run.pos.x() + m_font->width(last_run.text));
    rect().set_bottom(last_run.pos.y() + m_font->glyph_height());
}

void LayoutText::render(RenderingContext& context)
{
    auto& painter = context.painter();
    painter.set_font(*m_font);

    auto color = style_properties().color_or_fallback("color", Color::Black);

    for (auto& run : m_runs) {
        Rect rect {
            run.pos.x(),
            run.pos.y(),
            m_font->width(run.text),
            m_font->glyph_height()
        };
        painter.draw_text(rect, run.text, TextAlignment::TopLeft, color);
    }
}
