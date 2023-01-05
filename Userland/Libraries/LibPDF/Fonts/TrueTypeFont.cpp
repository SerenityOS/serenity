/*
 * Copyright (c) 2022, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2022, Julian Offenhäuser <offenhaeuser@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Font/OpenType/Font.h>
#include <LibGfx/Font/ScaledFont.h>
#include <LibGfx/Painter.h>
#include <LibPDF/CommonNames.h>
#include <LibPDF/Fonts/TrueTypeFont.h>

namespace PDF {

PDFErrorOr<PDFFont::CommonData> TrueTypeFont::parse_data(Document* document, NonnullRefPtr<DictObject> dict, float font_size)
{
    PDFFont::CommonData data;
    TRY(data.load_from_dict(document, dict, font_size));

    if (!data.is_standard_font) {
        auto descriptor = MUST(dict->get_dict(document, CommonNames::FontDescriptor));
        if (!descriptor->contains(CommonNames::FontFile2))
            return data;

        auto font_file_stream = TRY(descriptor->get_stream(document, CommonNames::FontFile2));
        auto ttf_font = TRY(OpenType::Font::try_load_from_externally_owned_memory(font_file_stream->bytes()));
        data.font = adopt_ref(*new Gfx::ScaledFont(*ttf_font, font_size, font_size));
    }

    return data;
}

PDFErrorOr<NonnullRefPtr<PDFFont>> TrueTypeFont::create(Document* document, NonnullRefPtr<DictObject> dict, float font_size)
{
    auto data = TRY(parse_data(document, dict, font_size));
    return adopt_ref(*new TrueTypeFont(move(data)));
}

TrueTypeFont::TrueTypeFont(PDFFont::CommonData data)
    : m_data(data)
{
    m_is_standard_font = data.is_standard_font;
}

u32 TrueTypeFont::char_code_to_code_point(u16 char_code) const
{
    if (m_data.to_unicode)
        TODO();

    if (m_data.encoding->should_map_to_bullet(char_code))
        return 8226; // Bullet.

    auto descriptor = m_data.encoding->get_char_code_descriptor(char_code);
    return descriptor.code_point;
}

float TrueTypeFont::get_char_width(u16 char_code) const
{
    u16 width;
    if (auto char_code_width = m_data.widths.get(char_code); char_code_width.has_value()) {
        width = char_code_width.value();
    } else {
        // FIXME: Should we do something with m_data.missing_width here?
        width = m_data.font->glyph_width(char_code);
    }

    return static_cast<float>(width) / 1000.0f;
}

void TrueTypeFont::draw_glyph(Gfx::Painter& painter, Gfx::FloatPoint point, float, u32 char_code, Color color)
{
    if (!m_data.font)
        return;

    // Account for the reversed font baseline
    auto position = point.translated(0, -m_data.font->baseline());
    painter.draw_glyph(position, char_code, *m_data.font, color);
}

}
