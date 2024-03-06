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
#include <LibPDF/Fonts/AdobeGlyphList.h>
#include <LibPDF/Fonts/TrueTypeFont.h>
#include <LibPDF/Renderer.h>

namespace PDF {

TrueTypePainter::TrueTypePainter(AK::NonnullRefPtr<Gfx::ScaledFont> font, NonnullRefPtr<Encoding> encoding, bool encoding_is_mac_roman_or_win_ansi, bool is_nonsymbolic, Optional<u8> high_byte, bool is_zapf_dingbats)
    : m_font(move(font))
    , m_encoding(move(encoding))
    , m_encoding_is_mac_roman_or_win_ansi(encoding_is_mac_roman_or_win_ansi)
    , m_is_nonsymbolic(is_nonsymbolic)
    , m_high_byte(high_byte)
    , m_is_zapf_dingbats(is_zapf_dingbats)
{
}

NonnullOwnPtr<TrueTypePainter> TrueTypePainter::create(Document* document, NonnullRefPtr<DictObject> const& dict, SimpleFont const& containing_pdf_font, AK::NonnullRefPtr<Gfx::ScaledFont> font, NonnullRefPtr<Encoding> encoding, bool is_zapf_dingbats)
{
    bool encoding_is_mac_roman_or_win_ansi = false;
    if (dict->contains(CommonNames::Encoding)) {
        auto encoding_object = MUST(dict->get_object(document, CommonNames::Encoding));
        if (encoding_object->is<NameObject>()) {
            auto name = encoding_object->cast<NameObject>()->name();
            if (name == "MacRomanEncoding" || name == "WinAnsiEncoding")
                encoding_is_mac_roman_or_win_ansi = true;
        }
    }

    // See long spec comment in TrueTypeFont::draw_glyph().
    Optional<u8> high_byte;
    if (!dict->contains(CommonNames::Encoding) || containing_pdf_font.is_symbolic()) {
        Array<u8, 4> prefixes { 0x00, 0xF0, 0xF1, 0xF2 };
        Array<size_t, prefixes.size()> counts { 0, 0, 0, 0 };
        for (size_t i = 0; i < prefixes.size(); ++i) {
            for (unsigned suffix = 0x00; suffix <= 0xFF; ++suffix) {
                if (font->contains_glyph((prefixes[i] << 8) | suffix))
                    counts[i] += 1;
            }
        }
        size_t max = 0, max_index = -1;
        for (size_t i = 0; i < counts.size(); ++i) {
            if (counts[i] > max) {
                max = counts[i];
                max_index = i;
            }
        }
        if (max > 0)
            high_byte = max_index;
    }

    return adopt_own(*new TrueTypePainter { move(font), move(encoding), encoding_is_mac_roman_or_win_ansi, containing_pdf_font.is_nonsymbolic(), high_byte, is_zapf_dingbats });
}

static void do_draw_glyph(Gfx::Painter& painter, Gfx::FloatPoint point, float width, u32 unicode, Gfx::Font const& font, ColorOrStyle const& style)
{
    // Undo shift in Glyf::Glyph::append_simple_path() via OpenType::Font::rasterize_glyph().
    auto position = point.translated(0, -font.pixel_metrics().ascent);

    if (style.has<Color>()) {
        painter.draw_glyph(position, unicode, font, style.get<Color>());
    } else {
        // FIXME: Bounding box and sample point look to be pretty wrong
        style.get<NonnullRefPtr<Gfx::PaintStyle>>()->paint(Gfx::IntRect(position.x(), position.y(), width, 0), [&](auto sample) {
            painter.draw_glyph(position, unicode, font, sample(Gfx::IntPoint(position.x(), position.y())));
        });
    }
}

PDFErrorOr<void> TrueTypePainter::draw_glyph(Gfx::Painter& painter, Gfx::FloatPoint point, float width, u8 char_code, Renderer const& renderer)
{
    auto style = renderer.state().paint_style;

    // 5.5.5 Character Encoding, Encodings for TrueType Fonts
    u32 unicode;

    // "If the font has a named Encoding entry of either MacRomanEncoding or WinAnsiEncoding,
    //  or if the font descriptor’s Nonsymbolic flag (see Table 5.20) is set, the viewer creates
    //  a table that maps from character codes to glyph names:
    if (m_encoding_is_mac_roman_or_win_ansi || m_is_nonsymbolic) {
        //  • If the Encoding entry is one of the names MacRomanEncoding or WinAnsiEncoding,
        //    the table is initialized with the mappings described in Appendix D.
        //  • If the Encoding entry is a dictionary, the table is initialized with the entries
        //    from the dictionary’s BaseEncoding entry (see Table 5.11). Any entries in the
        //    Differences array are used to update the table. Finally, any undefined entries in
        //    the table are filled using StandardEncoding."
        // Implementor's note: This is (mostly) done in SimpleFont::initialize() and m_encoding stores the result.

        // "If a (3, 1) “cmap” subtable (Microsoft Unicode) is present:
        //  • A character code is first mapped to a glyph name using the table described above.
        //  • The glyph name is then mapped to a Unicode value by consulting the Adobe Glyph List (see the Bibliography).
        //  • Finally, the Unicode value is mapped to a glyph description according to the (3, 1) subtable.
        //
        //  If no (3, 1) subtable is present but a (1, 0) subtable (Macintosh Roman) is present:
        //  • A character code is first mapped to a glyph name using the table described above.
        //  • The glyph name is then mapped back to a character code according to the standard
        //    Roman encoding used on Mac OS (see note below).
        //  • Finally, the code is mapped to a glyph description according to the (1, 0) subtable."
        // Implementor's note: We currently don't know which tables are present, so we for now we always
        // use the (3, 1) algorithm.
        // FIXME: Implement (1, 0) subtable support.
        auto char_name = m_encoding->get_name(char_code);
        u32 unicode = glyph_name_to_unicode(char_name, m_is_zapf_dingbats).value_or(char_code);
        if (m_font->contains_glyph(unicode)) {
            do_draw_glyph(painter, point, width, unicode, *m_font, style);
            return {};
        }

        // "In either of the cases above, if the glyph name cannot be mapped as specified, the glyph name is looked up
        //  in the font program’s “post” table (if one is present) and the associated glyph description is used."
        // FIXME: Implement this.
        return Error::rendering_unsupported_error("Looking up glyph in 'post' table not yet implemented.");
    } else if (m_high_byte.has_value()) {
        // "When the font has no Encoding entry, or the font descriptor’s Symbolic flag is set (in which case the
        //  Encoding entry is ignored), the following occurs:
        //
        //  • If the font contains a (3, 0) subtable, the range of character codes must be one of the following:
        //    0x0000 - 0x00FF, 0xF000 - 0xF0FF, 0xF100 - 0xF1FF, or 0xF200 - 0xF2FF. Depending on the range of codes,
        //    each byte from the string is prepended with the high byte of the range, to form a two-byte character,
        //    which is used to select the associated glyph description from the subtable.
        //  • Otherwise, if the font contains a (1, 0) subtable, single bytes from the string are used to look up the
        //    associated glyph descriptions from the subtable."
        // Implementor's note: We currently don't know which tables are present, so we for now we always use the (3, 0) algorithm.
        unicode = (m_high_byte.value() << 8) | char_code;
    } else {
        // "If a character cannot be mapped in any of the ways described above, the results are implementation-dependent."
        // FIXME: Do something smarter?
        auto char_name = m_encoding->get_name(char_code);
        unicode = glyph_name_to_unicode(char_name, m_is_zapf_dingbats).value_or(char_code);
    }

    do_draw_glyph(painter, point, width, unicode, *m_font, style);
    return {};
}

Optional<float> TrueTypePainter::get_glyph_width(u8 char_code) const
{
    // FIXME: Make this use the full char_code lookup method used in draw_glyph() once that's complete.
    auto char_name = m_encoding->get_name(char_code);
    u32 unicode = glyph_name_to_unicode(char_name, m_is_zapf_dingbats).value_or(char_code);
    return m_font->glyph_width(unicode);
}

void TrueTypePainter::set_font_size(float font_size)
{
    m_font = m_font->scaled_with_size((font_size * POINTS_PER_INCH) / DEFAULT_DPI);
}

PDFErrorOr<void> TrueTypeFont::initialize(Document* document, NonnullRefPtr<DictObject> const& dict, float font_size)
{
    TRY(SimpleFont::initialize(document, dict, font_size));

    m_base_font_name = TRY(dict->get_name(document, CommonNames::BaseFont))->name();

    // If there's an embedded font program we use that; otherwise we try to find a replacement font
    RefPtr<Gfx::ScaledFont> font;
    if (dict->contains(CommonNames::FontDescriptor)) {
        auto descriptor = MUST(dict->get_dict(document, CommonNames::FontDescriptor));
        if (descriptor->contains(CommonNames::FontFile2)) {
            auto font_file_stream = TRY(descriptor->get_stream(document, CommonNames::FontFile2));
            auto ttf_font = TRY(OpenType::Font::try_load_from_externally_owned_memory(font_file_stream->bytes(), { .skip_tables = pdf_skipped_opentype_tables }));
            float point_size = (font_size * POINTS_PER_INCH) / DEFAULT_DPI;
            font = adopt_ref(*new Gfx::ScaledFont(*ttf_font, point_size, point_size));
        }
    }
    if (!font)
        font = TRY(replacement_for(base_font_name().to_lowercase(), font_size));

    auto effective_encoding = encoding();
    if (!effective_encoding)
        effective_encoding = Encoding::standard_encoding();

    bool const is_zapf_dingbats = false;
    m_font_painter = TrueTypePainter::create(document, dict, *this, *font, *effective_encoding, is_zapf_dingbats);

    return {};
}

Optional<float> TrueTypeFont::get_glyph_width(u8 char_code) const
{
    return m_font_painter->get_glyph_width(char_code);
}

void TrueTypeFont::set_font_size(float font_size)
{
    m_font_painter->set_font_size(font_size);
}

PDFErrorOr<void> TrueTypeFont::draw_glyph(Gfx::Painter& painter, Gfx::FloatPoint point, float width, u8 char_code, Renderer const& renderer)
{
    return m_font_painter->draw_glyph(painter, point, width, char_code, renderer);
}

}
