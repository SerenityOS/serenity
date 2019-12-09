#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <LibGUI/GPainter.h>

#include "TextWidget.h"

TextWidget::TextWidget(GWidget* parent)
    : GFrame(parent)
{
}

TextWidget::TextWidget(const StringView& text, GWidget* parent)
    : GFrame(parent)
    , m_text(text)
{
}

TextWidget::~TextWidget()
{
}

void TextWidget::set_text(const StringView& text)
{
    if (text == m_text)
        return;
    m_text = text;
    wrap_and_set_height();
    update();
}

void TextWidget::paint_event(GPaintEvent& event)
{
    GFrame::paint_event(event);

    GPainter painter(*this);
    painter.add_clip_rect(event.rect());

    int indent = 0;
    if (frame_thickness() > 0)
        indent = font().glyph_width('x') / 2;

    for (int i = 0; i < m_lines.size(); i++) {
        auto& line = m_lines[i];

        auto text_rect = frame_inner_rect();
        text_rect.move_by(indent, i * m_line_height);
        if (!line.is_empty())
            text_rect.set_width(text_rect.width() - indent * 2);

        if (is_enabled()) {
            painter.draw_text(text_rect, line, m_text_alignment, foreground_color(), TextElision::None);
        } else {
            painter.draw_text(text_rect.translated(1, 1), line, font(), text_alignment(), Color::White, TextElision::Right);
            painter.draw_text(text_rect, line, font(), text_alignment(), Color::from_rgb(0x808080), TextElision::Right);
        }
    }
}

void TextWidget::resize_event(GResizeEvent& event)
{
    wrap_and_set_height();
    GWidget::resize_event(event);
}

void TextWidget::wrap_and_set_height()
{
    Vector<String> words;
    Optional<size_t> start;
    for (size_t i = 0; i < m_text.length(); i++) {
        auto ch = m_text[i];

        if (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n') {
            if (start.has_value())
                words.append(m_text.substring(start.value(), i - start.value()));
            start = -1;
        } else if (!start.has_value()) {
            start = i;
        }
    }
    if (start.has_value())
        words.append(m_text.substring(start, m_text.length() - start.value()));

    auto rect = frame_inner_rect();
    if (frame_thickness() > 0)
        rect.set_width(rect.width() - font().glyph_width('x'));

    StringBuilder builder;
    Vector<String> lines;
    int line_width = 0;
    for (auto& word : words) {
        int word_width = font().width(word);
        if (line_width != 0)
            word_width += font().glyph_width('x');

        if (line_width + word_width > rect.width()) {
            lines.append(builder.to_string());
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
    }

    m_lines = lines;

    set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    set_preferred_size(0, m_lines.size() * m_line_height + frame_thickness() * 2);
}
