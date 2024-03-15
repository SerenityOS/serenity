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
#include <LibPDF/Renderer.h>

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

PDFErrorOr<Gfx::FloatPoint> SimpleFont::draw_string(Gfx::Painter& painter, Gfx::FloatPoint glyph_position, ByteString const& string, Renderer const& renderer)
{
    auto horizontal_scaling = renderer.text_state().horizontal_scaling;

    auto const& text_rendering_matrix = renderer.calculate_text_rendering_matrix();

    // TrueType fonts are prescaled to text_rendering_matrix.x_scale() * text_state().font_size / horizontal_scaling,
    // cf `Renderer::text_set_font()`. That's the width we get back from `get_glyph_width()` if we use a fallback
    // (or built-in) font. Scale the width size too, so the m_width.get() codepath is consistent.
    auto const font_size = text_rendering_matrix.x_scale() * renderer.text_state().font_size / horizontal_scaling;

    auto character_spacing = renderer.text_state().character_spacing;
    auto word_spacing = renderer.text_state().word_spacing;

    for (auto char_code : string.bytes()) {
        // Use the width specified in the font's dictionary if available,
        // and use the default width for the given font otherwise.
        float glyph_width;
        if (auto width = m_widths.get(char_code); width.has_value())
            glyph_width = font_size * width.value() * m_font_matrix.x_scale();
        else if (auto width = get_glyph_width(char_code); width.has_value())
            glyph_width = width.value();
        else
            glyph_width = font_size * m_missing_width * m_font_matrix.x_scale();

        if (renderer.text_state().rendering_mode != TextRenderingMode::Invisible || renderer.show_hidden_text()) {
            Gfx::FloatPoint glyph_render_position = text_rendering_matrix.map(glyph_position);
            TRY(draw_glyph(painter, glyph_render_position, glyph_width, char_code, renderer));
        }

        // glyph_width is scaled by `text_rendering_matrix.x_scale() * renderer.text_state().font_size / horizontal_scaling`,
        // but it should only be scaled by `renderer.text_state().font_size`.
        // FIXME: Having to divide here isn't pretty. Refactor things so that this isn't needed.
        auto tx = glyph_width / text_rendering_matrix.x_scale() * horizontal_scaling;
        tx += character_spacing;

        // ISO 32000 (PDF 2.0), 9.3.3 Wordspacing
        // "Word spacing shall be applied to every occurrence of the single-byte character code 32
        // in a string when using a simple font (including Type 3) or a composite font that defines
        // code 32 as a single-byte code."
        if (char_code == ' ')
            tx += word_spacing;

        glyph_position += { tx, 0.0f };
    }
    return glyph_position;
}

}
