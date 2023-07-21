/*
 * Copyright (c) 2022, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2022, Julian Offenhäuser <offenhaeuser@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Painter.h>
#include <LibPDF/CommonNames.h>
#include <LibPDF/Fonts/CFF.h>
#include <LibPDF/Fonts/PS1FontProgram.h>
#include <LibPDF/Fonts/Type1Font.h>

namespace PDF {

PDFErrorOr<void> Type1Font::initialize(Document* document, NonnullRefPtr<DictObject> const& dict, float font_size)
{
    TRY(SimpleFont::initialize(document, dict, font_size));

    // auto is_standard_font = is_standard_latin_font(font->base_font_name());

    // If there's an embedded font program we use that; otherwise we try to find a replacement font
    if (dict->contains(CommonNames::FontDescriptor)) {
        auto descriptor = TRY(dict->get_dict(document, CommonNames::FontDescriptor));
        if (descriptor->contains(CommonNames::FontFile3)) {
            auto font_file_stream = TRY(descriptor->get_stream(document, CommonNames::FontFile3));
            auto font_file_dict = font_file_stream->dict();
            if (font_file_dict->contains(CommonNames::Subtype) && font_file_dict->get_name(CommonNames::Subtype)->name() == CommonNames::Type1C) {
                m_font_program = TRY(CFF::create(font_file_stream->bytes(), encoding()));
            }
        } else if (descriptor->contains(CommonNames::FontFile)) {
            auto font_file_stream = TRY(descriptor->get_stream(document, CommonNames::FontFile));
            auto font_file_dict = font_file_stream->dict();

            if (!font_file_dict->contains(CommonNames::Length1, CommonNames::Length2))
                return Error::parse_error("Embedded type 1 font is incomplete"sv);

            auto length1 = TRY(document->resolve(font_file_dict->get_value(CommonNames::Length1))).get<int>();
            auto length2 = TRY(document->resolve(font_file_dict->get_value(CommonNames::Length2))).get<int>();

            m_font_program = TRY(PS1FontProgram::create(font_file_stream->bytes(), encoding(), length1, length2));
        }
    }
    if (!m_font_program) {
        m_font = TRY(replacement_for(base_font_name().to_lowercase(), font_size));
    }

    VERIFY(m_font_program || m_font);
    return {};
}

float Type1Font::get_glyph_width(u8 char_code) const
{
    return m_font->glyph_width(char_code);
}

void Type1Font::set_font_size(float font_size)
{
    if (m_font)
        m_font = m_font->with_size((font_size * POINTS_PER_INCH) / DEFAULT_DPI);
}

void Type1Font::draw_glyph(Gfx::Painter& painter, Gfx::FloatPoint point, float width, u8 char_code, Color color)
{
    if (!m_font_program) {
        // Account for the reversed font baseline
        auto position = point.translated(0, -m_font->baseline());
        painter.draw_glyph(position, char_code, *m_font, color);
        return;
    }

    auto effective_encoding = encoding();
    if (!effective_encoding)
        effective_encoding = m_font_program->encoding();
    if (!effective_encoding)
        effective_encoding = Encoding::standard_encoding();
    auto char_name = effective_encoding->get_name(char_code);
    auto translation = m_font_program->glyph_translation(char_name, width);
    point = point.translated(translation);

    auto glyph_position = Gfx::GlyphRasterPosition::get_nearest_fit_for(point);
    Type1GlyphCacheKey index { char_code, glyph_position.subpixel_offset, width };

    RefPtr<Gfx::Bitmap> bitmap;
    auto maybe_bitmap = m_glyph_cache.get(index);
    if (maybe_bitmap.has_value()) {
        bitmap = maybe_bitmap.value();
    } else {
        bitmap = m_font_program->rasterize_glyph(char_name, width, glyph_position.subpixel_offset);
        m_glyph_cache.set(index, bitmap);
    }

    painter.blit_filtered(glyph_position.blit_position, *bitmap, bitmap->rect(), [color](Color pixel) -> Color {
        return pixel.multiply(color);
    });
}
}
