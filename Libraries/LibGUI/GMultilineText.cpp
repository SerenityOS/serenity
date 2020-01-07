#include "GMultilineText.h"
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <LibDraw/Palette.h>
#include <LibGUI/GPainter.h>

GMultilineText::GMultilineText(GWidget* parent)
    : GFrame(parent)
{
}

GMultilineText::GMultilineText(const StringView& text, GWidget* parent)
    : GFrame(parent)
    , m_text(text)
{
}

GMultilineText::~GMultilineText()
{
}

void GMultilineText::set_text(const StringView& text)
{
    if (text == m_text)
        return;
    m_text = text;
    wrap_and_set_height(frame_inner_rect().width());
    update();
}

void GMultilineText::paint_event(GPaintEvent& event)
{
    GFrame::paint_event(event);

    GPainter painter(*this);
    painter.add_clip_rect(event.rect());

    int indent = frame_thickness();
    int top = frame_thickness();

    for (int i = 0; i < m_lines.size(); i++) {
        auto& line = m_lines[i];

        auto text_rect = frame_inner_rect();
        text_rect.move_by(indent, top);
        if (!line.is_empty())
            text_rect.set_width(text_rect.width() - indent * 2);

        if (is_enabled()) {
            painter.draw_text(text_rect, line, m_text_alignment, palette().color(foreground_role()), TextElision::None);
        } else {
            painter.draw_text(text_rect.translated(1, 1), line, font(), text_alignment(), Color::White, TextElision::Right);
            painter.draw_text(text_rect, line, font(), text_alignment(), Color::from_rgb(0x808080), TextElision::Right);
        }

        top += font().glyph_height() + m_line_spacing;
    }
}

void GMultilineText::resize_event(GResizeEvent& event)
{
    wrap_and_set_height(event.size().width() - frame_thickness() * 2);
    GWidget::resize_event(event);
}

void GMultilineText::wrap_and_set_height(int max_width)
{
    Vector<String> words;
    Optional<size_t> start;
    for (size_t i = 0; i < m_text.length(); i++) {
        auto ch = m_text[i];

        if (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n') {
            if (start.has_value())
                words.append(m_text.substring(start.value(), i - start.value()));
            start.clear();
        } else if (!start.has_value()) {
            start = i;
        }
    }
    if (start.has_value())
        words.append(m_text.substring(start.value(), m_text.length() - start.value()));

    StringBuilder builder;
    Vector<String> lines;
    int line_width = 0;
    int total_height = frame_thickness() * 2;
    for (auto& word : words) {
        int word_width = font().width(word);
        if (line_width != 0)
            word_width += font().glyph_width('x');

        if (line_width + word_width > max_width) {
            lines.append(builder.to_string());
            total_height += font().glyph_height() + m_line_spacing;
            line_width = 0;
        }

        if (line_width != 0)
            builder.append(' ');
        builder.append(word);
        line_width += word_width;
    }

    auto last_line = builder.to_string();
    if (!last_line.is_empty()) {
        lines.append(last_line);
        total_height += font().glyph_height();
    }

    m_lines = lines;

    set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    set_preferred_size(0, total_height);
}
