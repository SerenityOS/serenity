/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TextLayout.h"

namespace Gfx {

enum class BlockType {
    Newline,
    Whitespace,
    Word
};

struct Block {
    BlockType type;
    Utf8View characters;
};

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
    Vector<Block> blocks;

    Optional<BlockType> current_block_type;
    size_t block_start_offset = 0;

    size_t offset = 0;
    for (auto it = m_text.begin(); !it.done(); ++it) {
        offset = m_text.iterator_offset(it);

        switch (*it) {
        case '\t':
        case ' ': {
            if (current_block_type.has_value() && current_block_type.value() != BlockType::Whitespace) {
                blocks.append({
                    current_block_type.value(),
                    m_text.substring_view(block_start_offset, offset - block_start_offset),
                });
                current_block_type.clear();
            }

            if (!current_block_type.has_value()) {
                current_block_type = BlockType::Whitespace;
                block_start_offset = offset;
            }

            continue;
        }
        case '\n':
        case '\r': {
            if (current_block_type.has_value()) {
                blocks.append({
                    current_block_type.value(),
                    m_text.substring_view(block_start_offset, offset - block_start_offset),
                });
                current_block_type.clear();
            }

            blocks.append({ BlockType::Newline, Utf8View {} });
            continue;
        }
        default: {
            if (current_block_type.has_value() && current_block_type.value() != BlockType::Word) {
                blocks.append({
                    current_block_type.value(),
                    m_text.substring_view(block_start_offset, offset - block_start_offset),
                });
                current_block_type.clear();
            }

            if (!current_block_type.has_value()) {
                current_block_type = BlockType::Word;
                block_start_offset = offset;
            }
        }
        }
    }

    if (current_block_type.has_value()) {
        blocks.append({
            current_block_type.value(),
            m_text.substring_view(block_start_offset, m_text.byte_length() - block_start_offset),
        });
    }

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
    size_t current_block = 0;
    bool did_not_finish = false;
    for (Block& block : blocks) {
        switch (block.type) {
        case BlockType::Newline: {
            lines.append(builder.to_string());
            builder.clear();
            line_width = 0;

            if (lines.size() == max_lines_that_can_fit && fit_within_rect == FitWithinRect::Yes) {
                did_not_finish = true;
                goto blocks_processed;
            }

            current_block++;
            continue;
        }
        case BlockType::Whitespace:
        case BlockType::Word: {
            size_t block_width = font().width(block.characters);
            // FIXME: This should look at the specific advance amount of the
            //        last character, but we don't support that yet.
            if (current_block != blocks.size() - 1) {
                block_width += font().glyph_spacing();
            }

            if (wrapping == TextWrapping::Wrap && line_width + block_width > static_cast<unsigned>(m_rect.width())) {
                lines.append(builder.to_string());
                builder.clear();
                line_width = 0;
            }

            if (lines.size() == max_lines_that_can_fit && fit_within_rect == FitWithinRect::Yes) {
                did_not_finish = true;
                goto blocks_processed;
            }

            builder.append(block.characters.as_string());
            line_width += block_width;
            current_block++;
        }
        }
    }

blocks_processed:
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
