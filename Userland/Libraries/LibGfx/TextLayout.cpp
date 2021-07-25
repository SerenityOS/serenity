/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TextLayout.h"

namespace Gfx {

// HACK: We need to point to some valid memory with Utf8Views.
char const s_the_newline[] = "\n";

IntRect TextLayout::bounding_rect(TextWrapping wrapping, int line_spacing) const
{
    auto lines = wrap_lines(TextElision::None, wrapping, line_spacing, FitWithinRect::No);
    if (!lines.size()) {
        return {};
    }

    IntRect bounding_rect = {
        0, 0, 0, static_cast<int>((lines.size() * (m_font->glyph_height() + line_spacing)) - line_spacing)
    };

    for (auto& line : lines) {
        auto line_width = m_font->width(line);
        if (line_width > bounding_rect.width())
            bounding_rect.set_width(line_width);
    }

    return bounding_rect;
}

Vector<String, 32> TextLayout::wrap_lines(TextElision elision, TextWrapping wrapping, int line_spacing, FitWithinRect fit_within_rect) const
{
    Vector<Utf8View> words;

    Optional<size_t> start_byte_offset;
    size_t current_byte_offset = 0;
    for (auto it = m_text.begin(); !it.done(); ++it) {
        current_byte_offset = m_text.iterator_offset(it);

        switch (*it) {
        case '\n':
        case '\r':
        case '\t':
        case ' ': {
            if (start_byte_offset.has_value())
                words.append(m_text.substring_view(start_byte_offset.value(), current_byte_offset - start_byte_offset.value()));
            start_byte_offset.clear();

            if (*it == '\n') {
                words.append(Utf8View { s_the_newline });
            }

            continue;
        }
        default: {
            if (!start_byte_offset.has_value())
                start_byte_offset = current_byte_offset;
        }
        }
    }

    if (start_byte_offset.has_value())
        words.append(m_text.substring_view(start_byte_offset.value(), m_text.byte_length() - start_byte_offset.value()));

    size_t max_lines_that_can_fit = 0;
    if (m_rect.height() >= m_font->glyph_height()) {
        // NOTE: If glyph height is 10 and line spacing is 1, we can fit a
        // single line into a 10px rect and a 20px rect, but 2 lines into a
        // 21px rect.
        max_lines_that_can_fit = 1 + (m_rect.height() - m_font->glyph_height()) / (m_font->glyph_height() + line_spacing);
    }

    if (max_lines_that_can_fit == 0)
        return {};

    Vector<String> lines;
    StringBuilder builder;
    size_t line_width = 0;
    bool did_not_finish = false;
    for (auto& word : words) {

        if (word.as_string() == s_the_newline) {
            lines.append(builder.to_string());
            builder.clear();
            line_width = 0;

            if (lines.size() == max_lines_that_can_fit && fit_within_rect == FitWithinRect::Yes) {
                did_not_finish = true;
                break;
            }
        } else {
            size_t word_width = font().width(word);

            if (line_width > 0) {
                word_width += font().glyph_width('x');

                if (wrapping == TextWrapping::Wrap && line_width + word_width > static_cast<unsigned>(m_rect.width())) {
                    lines.append(builder.to_string());
                    builder.clear();
                    line_width = 0;

                    if (lines.size() == max_lines_that_can_fit && fit_within_rect == FitWithinRect::Yes) {
                        did_not_finish = true;
                        break;
                    }
                }

                builder.append(' ');
            }
            if (lines.size() == max_lines_that_can_fit && fit_within_rect == FitWithinRect::Yes) {
                did_not_finish = true;
                break;
            }

            builder.append(word.as_string());
            line_width += word_width;
        }
    }

    if (!did_not_finish) {
        auto last_line = builder.to_string();
        if (!last_line.is_empty())
            lines.append(last_line);
    }

    switch (elision) {
    case TextElision::None:
        break;
    case TextElision::Right: {
        lines.at(lines.size() - 1) = elide_text_from_right(Utf8View { lines.at(lines.size() - 1) }, did_not_finish);
        break;
    }
    }

    return lines;
}

String TextLayout::elide_text_from_right(Utf8View text, bool force_elision) const
{
    size_t text_width = m_font->width(text);
    if (force_elision || text_width > static_cast<unsigned>(m_rect.width())) {
        size_t ellipsis_width = m_font->width("...");
        size_t current_width = ellipsis_width;
        size_t glyph_spacing = m_font->glyph_spacing();

        // FIXME: This code will break when the font has glyphs with advance
        //        amounts different from the actual width of the glyph
        //        (which is the case with many TrueType fonts).
        if (ellipsis_width < text_width) {
            size_t offset = 0;
            for (auto it = text.begin(); !it.done(); ++it) {
                auto code_point = *it;
                int glyph_width = m_font->glyph_or_emoji_width(code_point);
                // NOTE: Glyph spacing should not be added after the last glyph on the line,
                //       but since we are here because the last glyph does not actually fit on the line,
                //       we don't have to worry about spacing.
                int width_with_this_glyph_included = current_width + glyph_width + glyph_spacing;
                if (width_with_this_glyph_included > m_rect.width())
                    break;
                current_width += glyph_width + glyph_spacing;
                offset = text.iterator_offset(it);
            }

            StringBuilder builder;
            builder.append(text.substring_view(0, offset).as_string());
            builder.append("...");
            return builder.to_string();
        }
    }

    return text.as_string();
}

}
