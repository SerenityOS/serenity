/*
 * Copyright (c) 2022, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2022, Julian Offenhäuser <offenhaeuser@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Painter.h>
#include <LibPDF/CommonNames.h>
#include <LibPDF/Fonts/Type1Font.h>

namespace PDF {

PDFErrorOr<Type1Font::Data> Type1Font::parse_data(Document* document, NonnullRefPtr<DictObject> dict, float font_size)
{
    Type1Font::Data data;
    TRY(data.load_from_dict(document, dict, font_size));

    if (!data.is_standard_font) {
        auto descriptor = TRY(dict->get_dict(document, CommonNames::FontDescriptor));
        if (!descriptor->contains(CommonNames::FontFile))
            return data;

        auto font_file_stream = TRY(descriptor->get_stream(document, CommonNames::FontFile));
        auto font_file_dict = font_file_stream->dict();

        if (!font_file_dict->contains(CommonNames::Length1, CommonNames::Length2))
            return Error { Error::Type::Parse, "Embedded type 1 font is incomplete" };

        auto length1 = TRY(document->resolve(font_file_dict->get_value(CommonNames::Length1))).get<int>();
        auto length2 = TRY(document->resolve(font_file_dict->get_value(CommonNames::Length2))).get<int>();

        data.font_program = adopt_ref(*new PS1FontProgram());
        TRY(data.font_program->create(font_file_stream->bytes(), data.encoding, length1, length2));

        if (!data.encoding)
            data.encoding = data.font_program->encoding();
    }

    return data;
}

PDFErrorOr<NonnullRefPtr<Type1Font>> Type1Font::create(Document* document, NonnullRefPtr<DictObject> dict, float font_size)
{
    auto data = TRY(Type1Font::parse_data(document, dict, font_size));
    return adopt_ref(*new Type1Font(data));
}

Type1Font::Type1Font(Data data)
    : m_data(move(data))
{
    m_is_standard_font = data.is_standard_font;
}

u32 Type1Font::char_code_to_code_point(u16 char_code) const
{
    if (m_data.to_unicode)
        TODO();

    if (m_data.encoding->should_map_to_bullet(char_code))
        return 8226; // Bullet.

    auto descriptor = m_data.encoding->get_char_code_descriptor(char_code);
    return descriptor.code_point;
}

float Type1Font::get_char_width(u16 char_code) const
{
    u16 width;
    if (auto char_code_width = m_data.widths.get(char_code); char_code_width.has_value()) {
        width = char_code_width.value();
    } else {
        width = m_data.missing_width;
    }

    return static_cast<float>(width) / 1000.0f;
}

void Type1Font::draw_glyph(Gfx::Painter& painter, Gfx::FloatPoint point, float width, u32 char_code, Color color)
{
    if (!m_data.font_program)
        return;
    auto translation = m_data.font_program->glyph_translation(char_code, width);
    point = point.translated(translation);

    auto glyph_position = Gfx::GlyphRasterPosition::get_nearest_fit_for(point);
    Gfx::GlyphIndexWithSubpixelOffset index { char_code, glyph_position.subpixel_offset };

    RefPtr<Gfx::Bitmap> bitmap;
    auto maybe_bitmap = m_glyph_cache.get(index);
    if (maybe_bitmap.has_value()) {
        bitmap = maybe_bitmap.value();
    } else {
        bitmap = m_data.font_program->rasterize_glyph(char_code, width, glyph_position.subpixel_offset);
        m_glyph_cache.set(index, bitmap);
    }

    painter.blit_filtered(glyph_position.blit_position, *bitmap, bitmap->rect(), [color](Color pixel) -> Color {
        return pixel.multiply(color);
    });
}
}
