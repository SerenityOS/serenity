/*
 * Copyright (c) 2023, Rodrigo Tobar <rtobarc@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Forward.h>
#include <LibPDF/CommonNames.h>
#include <LibPDF/Error.h>
#include <LibPDF/Fonts/PDFFont.h>
#include <LibPDF/Fonts/SimpleFont.h>
#include <LibPDF/Fonts/TrueTypeFont.h>
#include <LibPDF/Fonts/Type1Font.h>

namespace PDF {

PDFErrorOr<void> SimpleFont::initialize(Document* document, NonnullRefPtr<DictObject> const& dict, float font_size)
{
    TRY(PDFFont::initialize(document, dict, font_size));
    if (dict->contains(CommonNames::Encoding)) {
        auto encoding_object = MUST(dict->get_object(document, CommonNames::Encoding));
        m_encoding = TRY(Encoding::from_object(document, encoding_object));
    }

    if (dict->contains(CommonNames::ToUnicode))
        m_to_unicode = TRY(dict->get_stream(document, CommonNames::ToUnicode));

    if (dict->contains(CommonNames::FirstChar) && dict->contains(CommonNames::LastChar) && dict->contains(CommonNames::Widths)) {
        auto first_char = dict->get_value(CommonNames::FirstChar).get<int>();
        auto last_char = dict->get_value(CommonNames::LastChar).get<int>();
        auto widths_array = TRY(dict->get_array(document, CommonNames::Widths));

        VERIFY(widths_array->size() == static_cast<size_t>(last_char - first_char + 1));

        for (size_t i = 0; i < widths_array->size(); i++)
            m_widths.set(first_char + i, widths_array->at(i).to_int());
    }

    if (dict->contains(CommonNames::FontDescriptor)) {
        auto descriptor = TRY(dict->get_dict(document, CommonNames::FontDescriptor));
        if (descriptor->contains(CommonNames::MissingWidth))
            m_missing_width = descriptor->get_value(CommonNames::MissingWidth).to_int();
    }

    return {};
}

PDFErrorOr<Gfx::FloatPoint> SimpleFont::draw_string(Gfx::Painter& painter, Gfx::FloatPoint glyph_position, DeprecatedString const& string, Color const& paint_color, float font_size, float character_spacing, float word_spacing, float horizontal_scaling)
{
    for (auto char_code : string.bytes()) {
        // Use the width specified in the font's dictionary if available,
        // and use the default width for the given font otherwise.
        float glyph_width;
        if (auto width = m_widths.get(char_code); width.has_value())
            glyph_width = font_size * width.value() / 1000.0f;
        else
            glyph_width = get_glyph_width(char_code);

        draw_glyph(painter, glyph_position, glyph_width, char_code, paint_color);

        auto tx = glyph_width;
        tx += character_spacing;

        // ISO 32000 (PDF 2.0), 9.3.3 Wordspacing
        // "Word spacing shall be applied to every occurrence of the single-byte character code 32
        // in a string when using a simple font (including Type 3) or a composite font that defines
        // code 32 as a single-byte code."
        if (char_code == ' ')
            tx += word_spacing;

        tx *= horizontal_scaling;
        glyph_position += { tx, 0.0f };
    }
    return glyph_position;
}

}
