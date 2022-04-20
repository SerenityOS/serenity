/*
 * Copyright (c) 2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Font/ScaledFont.h>
#include <LibPDF/CommonNames.h>
#include <LibPDF/Fonts/TrueTypeFont.h>
#include <LibPDF/Fonts/Type1Font.h>

namespace PDF {

PDFErrorOr<NonnullRefPtr<PDFFont>> TrueTypeFont::create(Document* document, NonnullRefPtr<DictObject> dict)
{
    auto font_descriptor = TRY(dict->get_dict(document, CommonNames::FontDescriptor));

    if (!dict->contains(CommonNames::FontFile2)) {
        // FIXME: The TTF is one of the standard 14 fonts. These should be built into
        //        the system, and their attributes hardcoded. Until we have them, just
        //        treat this as a Type1 font (which are very similar to TTF fonts)
        return TRY(Type1Font::create(document, dict));
    }

    auto font_file = TRY(dict->get_stream(document, CommonNames::FontFile2));
    auto ttf_font = TRY(TTF::Font::try_load_from_externally_owned_memory(font_file->bytes()));
    auto data = TRY(Type1Font::parse_data(document, dict));

    return adopt_ref(*new TrueTypeFont(ttf_font, move(data)));
}

TrueTypeFont::TrueTypeFont(NonnullRefPtr<TTF::Font> ttf_font, Type1Font::Data data)
    : m_ttf_font(ttf_font)
    , m_data(data)
{
}

u32 TrueTypeFont::char_code_to_code_point(u16 char_code) const
{
    if (m_data.to_unicode)
        TODO();

    auto descriptor = m_data.encoding->get_char_code_descriptor(char_code);
    return descriptor.code_point;
}

float TrueTypeFont::get_char_width(u16 char_code, float font_size) const
{
    u16 width;
    if (auto char_code_width = m_data.widths.get(char_code); char_code_width.has_value()) {
        width = char_code_width.value();
    } else {
        // FIXME: Should we do something with m_data.missing_width here?
        float units_per_em = m_ttf_font->units_per_em();
        auto scale = (font_size * DEFAULT_DPI) / (POINTS_PER_INCH * units_per_em);

        auto code_point = char_code_to_code_point(char_code);
        auto id = m_ttf_font->glyph_id_for_code_point(code_point);
        auto metrics = m_ttf_font->glyph_metrics(id, scale, scale);
        width = metrics.advance_width;
    }

    return static_cast<float>(width) / 1000.0f;
}

}
