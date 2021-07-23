/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Vinicius Sugimoto <vtmsugimoto@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Label.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>

REGISTER_WIDGET(GUI, Label)

namespace GUI {

Label::Label(String text)
    : m_text(move(text))
{
    REGISTER_TEXT_ALIGNMENT_PROPERTY("text_alignment", text_alignment, set_text_alignment);

    set_frame_thickness(0);
    set_frame_shadow(Gfx::FrameShadow::Plain);
    set_frame_shape(Gfx::FrameShape::NoFrame);

    set_foreground_role(Gfx::ColorRole::WindowText);

    REGISTER_STRING_PROPERTY("text", text, set_text);
    REGISTER_BOOL_PROPERTY("autosize", is_autosize, set_autosize);
    REGISTER_BOOL_PROPERTY("word_wrap", is_word_wrap, set_word_wrap);
    REGISTER_ENUM_PROPERTY(
        "word_wrap_mode", word_wrap_mode, set_word_wrap_mode, GUI::WordWrapMode,
        { GUI::WordWrapMode::Word, "Word" },
        { GUI::WordWrapMode::Anywhere, "Anywhere" });
}

Label::~Label()
{
}

void Label::set_autosize(bool autosize)
{
    if (m_autosize == autosize)
        return;
    m_autosize = autosize;
    if (m_autosize)
        size_to_fit();
}

void Label::set_word_wrap(bool wrap)
{
    if (m_word_wrap == wrap)
        return;
    m_word_wrap = wrap;
    if (is_word_wrap())
        wrap_text();
}

void Label::set_icon(const Gfx::Bitmap* icon)
{
    if (m_icon == icon)
        return;
    m_icon = icon;
    update();
}

void Label::set_text(String text)
{
    if (text == m_text)
        return;
    m_text = move(text);
    if (is_word_wrap())
        wrap_text();
    if (m_autosize)
        size_to_fit();
    update();
    did_change_text();
}

Gfx::IntRect Label::text_rect(size_t line) const
{
    int indent = 0;
    if (frame_thickness() > 0)
        indent = font().glyph_width('x') / 2;
    auto rect = frame_inner_rect();
    rect.translate_by(indent, line * (font().glyph_height() + 1));
    rect.set_width(rect.width() - indent * 2);
    return rect;
}

void Label::paint_event(PaintEvent& event)
{
    Frame::paint_event(event);

    Painter painter(*this);
    painter.add_clip_rect(event.rect());

    if (m_icon) {
        if (m_should_stretch_icon) {
            painter.draw_scaled_bitmap(frame_inner_rect(), *m_icon, m_icon->rect());
        } else {
            auto icon_location = frame_inner_rect().center().translated(-(m_icon->width() / 2), -(m_icon->height() / 2));
            painter.blit(icon_location, *m_icon, m_icon->rect());
        }
    }

    if (text().is_empty())
        return;

    if (is_word_wrap()) {
        wrap_text();
        for (size_t i = 0; i < m_lines.size(); i++) {
            auto& line = m_lines[i];
            auto text_rect = this->text_rect(i);
            if (is_enabled()) {
                painter.draw_text(text_rect, line, m_text_alignment, palette().color(foreground_role()), Gfx::TextElision::Right);
            } else {
                painter.draw_text(text_rect.translated(1, 1), line, font(), text_alignment(), Color::White, Gfx::TextElision::Right);
                painter.draw_text(text_rect, line, font(), text_alignment(), Color::from_rgb(0x808080), Gfx::TextElision::Right);
            }
        }
    } else {
        auto text_rect = this->text_rect();
        if (is_enabled()) {
            painter.draw_text(text_rect, text(), m_text_alignment, palette().color(foreground_role()), Gfx::TextElision::Right);
        } else {
            painter.draw_text(text_rect.translated(1, 1), text(), font(), text_alignment(), Color::White, Gfx::TextElision::Right);
            painter.draw_text(text_rect, text(), font(), text_alignment(), Color::from_rgb(0x808080), Gfx::TextElision::Right);
        }
    }
}

void Label::size_to_fit()
{
    set_fixed_width(font().width(m_text));
}

void Label::wrap_text()
{
    Vector<String> words;
    Optional<size_t> start;
    bool word_is_space = false;
    auto include_word = [this, &start, &words](size_t i) {
        if (start.has_value())
            words.append(m_text.substring(start.value(), i - start.value()));
        start.clear();
    };
    for (size_t i = 0; i < m_text.length(); i++) {
        switch (m_text[i]) {
        case '\n':
        case '\r': {
            include_word(i);
            words.append("\n");
            continue;
        }
        case '\t':
        case ' ': {
            if (m_word_wrap_mode != GUI::WordWrapMode::Anywhere) {
                word_is_space = true;
                include_word(i);
                start = i;
                continue;
            }
            [[fallthrough]];
        }
        default: {
            if (word_is_space)
                include_word(i);
            word_is_space = false;
            if (!start.has_value())
                start = i;
        }
        }
    }

    if (start.has_value())
        words.append(m_text.substring(start.value(), m_text.length() - start.value()));

    auto rect = frame_inner_rect();
    if (frame_thickness() > 0)
        rect.set_width(rect.width() - font().glyph_width('x'));

    Vector<String> lines;
    String line;
    StringBuilder builder;
    for (auto& word : words) {
        switch (m_word_wrap_mode) {
        case GUI::WordWrapMode::Word:
            line = builder.to_string();
            builder.append(word);
            if (font().width(builder.to_string()) > rect.width() && !line.is_empty()) {
                lines.append(line);
                line = String::empty();
                builder.clear();
                builder.append(word);
            }
            if (word == "\n") {
                if (!line.is_empty())
                    lines.append(line);
                builder.clear();
                lines.append(word);
            }
            continue;
        case GUI::WordWrapMode::Anywhere:
            size_t current_position = 0;
            while (current_position != word.length()) {
                size_t size = next_substring_size(&word, current_position);
                lines.append(word.substring(current_position, size));
                current_position += size;
            }
            continue;
        }
    }

    if (!builder.is_empty())
        lines.append(builder.to_string());

    m_lines = lines;
}

size_t Label::next_substring_size(String* word, size_t start)
{
    String sub_word = word->substring(start);
    size_t lower_index = 0, upper_index = sub_word.length();
    size_t index = (upper_index - lower_index) / 2;

    if (index == 0)
        return upper_index;

    double sub_word_width = font().width(sub_word.substring(0, index));

    auto rect = frame_inner_rect();
    if (frame_thickness() > 0)
        rect.set_width(rect.width() - font().glyph_width('x'));

    if (rect.width() == 0)
        return upper_index;

    while (upper_index != lower_index) {
        if (sub_word_width > rect.width()) {
            if (upper_index == index) {
                index--;
                continue;
            }
            upper_index = index;
            index -= (upper_index - lower_index) / 2;
        } else if (sub_word_width < rect.width()) {
            if (lower_index == index)
                index++;
            lower_index = index;
            index += (upper_index - lower_index) / 2;
        } else
            break;

        sub_word_width = font().width(sub_word.substring(0, index));
    }
    if (sub_word_width > rect.width())
        index--;

    return index;
}

}
