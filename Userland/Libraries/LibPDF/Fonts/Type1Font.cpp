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
#include <LibPDF/Renderer.h>

namespace PDF {

PDFErrorOr<void> Type1Font::initialize(Document* document, NonnullRefPtr<DictObject> const& dict, float font_size)
{
    TRY(SimpleFont::initialize(document, dict, font_size));

    m_base_font_name = TRY(dict->get_name(document, CommonNames::BaseFont))->name();

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

    if (m_font_program && m_font_program->kind() == Type1FontProgram::Kind::CIDKeyed)
        return Error::parse_error("Type1 fonts must not be CID-keyed"sv);

    if (!m_font_program) {
        // NOTE: We use this both for the 14 built-in fonts and for replacement fonts.
        // We should probably separate these two cases.
        auto font = TRY(replacement_for(base_font_name().to_lowercase(), font_size));

        auto effective_encoding = encoding();
        bool is_standard_14_font = base_font_name() == "Helvetica" || base_font_name() == "Helvetica-Bold" || base_font_name() == "Helvetica-Oblique" || base_font_name() == "Helvetica-BoldOblique"
            || base_font_name() == "Times-Roman" || base_font_name() == "Times-Bold" || base_font_name() == "Times-Italic" || base_font_name() == "Times-BoldItalic"
            || base_font_name() == "Courier" || base_font_name() == "Courier-Bold" || base_font_name() == "Courier-Oblique" || base_font_name() == "Courier-BoldOblique"
            || base_font_name() == "Symbol" || base_font_name() == "ZapfDingbats";
        if (!effective_encoding) {
            // PDF 1.7 spec, APPENDIX D Character Sets and Encodings
            // "Sections D.4, “Symbol Set and Encoding,” and D.5, “ZapfDingbats Set and Encoding,”
            //  describe the character sets and built-in encodings for the Symbol and ZapfDingbats (ITC Zapf Dingbats)
            //  font programs, which are among the standard 14 predefined fonts. These fonts have built-in encodings
            //  that are unique to each font. (The characters for ZapfDingbats are ordered by code instead of by name,
            //  since the names in that font are meaningless.)"
            // FIXME: We use Liberation Sans for both Symbol and ZapfDingbats. It doesn't have all Symbol
            //        characters, or at least not under the codepoints used in AdobeGlpyhList. It doesn't
            //        have most of the ZapfDingbats characters. Not sure what to do about this -- we might need a different font.
            //        (For Helvetica / Times / Courier, the Liberation family also doesn't have the right metrics.)
            if (base_font_name() == "Symbol"sv)
                effective_encoding = Encoding::symbol_encoding();
            else if (base_font_name() == "ZapfDingbats"sv)
                effective_encoding = Encoding::zapf_encoding();
            else
                effective_encoding = Encoding::standard_encoding();
        }

        if (is_standard_14_font) {
            // We use the Liberation fonts as a replacement for the standard 14 fonts, and they're all non-symbolic.
            m_flags = (m_flags | NonSymbolic) & ~Symbolic;

            // FIXME: Set more m_flags bits (symbolic/nonsymbolic, italic, bold, fixed pitch, serif).
        }

        m_fallback_font_painter = TrueTypePainter::create(document, dict, *this, *font, *effective_encoding, base_font_name() == "ZapfDingbats"sv);
    }

    VERIFY(m_font_program || m_fallback_font_painter);
    return {};
}

Optional<float> Type1Font::get_glyph_width(u8 char_code) const
{
    if (m_fallback_font_painter)
        return m_fallback_font_painter->get_glyph_width(char_code);
    return OptionalNone {};
}

void Type1Font::set_font_size(float font_size)
{
    if (m_fallback_font_painter)
        m_fallback_font_painter->set_font_size(font_size);
}

PDFErrorOr<void> Type1Font::draw_glyph(Gfx::Painter& painter, Gfx::FloatPoint point, float width, u8 char_code, Renderer const& renderer)
{
    auto style = renderer.state().paint_style;

    if (!m_font_program)
        return m_fallback_font_painter->draw_glyph(painter, point, width, char_code, renderer);

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

    if (style.has<Color>()) {
        painter.blit_filtered(glyph_position.blit_position, *bitmap, bitmap->rect(), [style](Color pixel) -> Color {
            return pixel.multiply(style.get<Color>());
        });
    } else {
        style.get<NonnullRefPtr<Gfx::PaintStyle>>()->paint(bitmap->physical_rect(), [&](auto sample) {
            painter.blit_filtered(glyph_position.blit_position, *bitmap, bitmap->rect(), [&](Color pixel) -> Color {
                // FIXME: Presumably we need to sample at every point in the glyph, not just the top left?
                return pixel.multiply(sample(glyph_position.blit_position));
            });
        });
    }
    return {};
}
}
