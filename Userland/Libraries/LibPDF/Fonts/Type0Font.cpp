/*
 * Copyright (c) 2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Font/ScaledFont.h>
#include <LibPDF/CommonNames.h>
#include <LibPDF/Fonts/CFF.h>
#include <LibPDF/Fonts/Type0Font.h>
#include <LibPDF/Renderer.h>

namespace PDF {

class CIDFontType {
public:
    virtual ~CIDFontType() = default;
    virtual PDFErrorOr<void> draw_glyph(Gfx::Painter&, Gfx::FloatPoint, float width, u32 cid, Renderer const&) = 0;
};

class CIDFontType0 : public CIDFontType {
public:
    static PDFErrorOr<NonnullOwnPtr<CIDFontType0>> create(Document*, NonnullRefPtr<DictObject> const& descendant);

    virtual PDFErrorOr<void> draw_glyph(Gfx::Painter&, Gfx::FloatPoint, float width, u32 cid, Renderer const&) override;

private:
    CIDFontType0(RefPtr<Type1FontProgram> font_program)
        : m_font_program(move(font_program))
    {
    }

    RefPtr<Type1FontProgram> m_font_program;
};

PDFErrorOr<NonnullOwnPtr<CIDFontType0>> CIDFontType0::create(Document* document, NonnullRefPtr<DictObject> const& descendant)
{
    auto descriptor = TRY(descendant->get_dict(document, CommonNames::FontDescriptor));

    RefPtr<Type1FontProgram> font_program;

    // See spec comment in CIDFontType0::draw_glyph().
    if (descriptor->contains(CommonNames::FontFile3)) {
        auto font_file_stream = TRY(descriptor->get_stream(document, CommonNames::FontFile3));
        auto font_file_dict = font_file_stream->dict();
        DeprecatedFlyString subtype;
        if (font_file_dict->contains(CommonNames::Subtype))
            subtype = font_file_dict->get_name(CommonNames::Subtype)->name();
        if (subtype == CommonNames::CIDFontType0C) {
            // FIXME: Call CFF::create() and assign the result to font_program once CFF::create() can handle CID-keyed fonts.
            return Error::rendering_unsupported_error("Type0 font CIDFontType0: support for CIDFontType0C not yet implemented");
        } else {
            // FIXME: Add support for /OpenType.
            dbgln("CIDFontType0: unsupported FontFile3 subtype '{}'", subtype);
            return Error::rendering_unsupported_error("Type0 font CIDFontType0: support for non-CIDFontType0C not yet implemented");
        }
    }

    if (!font_program) {
        // FIXME: Should we use a fallback font? How common is this for type 0 fonts?
        return Error::malformed_error("CIDFontType0: missing FontFile3");
    }

    return TRY(adopt_nonnull_own_or_enomem(new (nothrow) CIDFontType0(move(font_program))));
}

PDFErrorOr<void> CIDFontType0::draw_glyph(Gfx::Painter&, Gfx::FloatPoint, float, u32, Renderer const&)
{
    // ISO 32000 (PDF 2.0) 9.7.4.2 Glyph selection in CIDFonts
    // "When the CIDFont contains an embedded font program that is represented in the Compact Font Format (CFF),
    //  the FontFile3 entry in the font descriptor (...) shall be either CIDFontType0C or OpenType.
    //  There are two cases, depending on the contents of the font program:
    //  * The "CFF" font program has a Top DICT that uses CIDFont operators: The CIDs shall be used to determine
    //    the GID value for the glyph procedure using the charset table in the CFF program.
    //    The GID value shall then be used to look up the glyph procedure using the CharStrings INDEX table [...]
    //  * The "CFF" font program has a Top DICT that does not use CIDFont operators: The CIDs shall be used
    //    directly as GID values, and the glyph procedure shall be retrieved using the CharStrings INDEX"
    return Error::rendering_unsupported_error("Type0 font CIDFontType0 not implemented yet");
}

class CIDFontType2 : public CIDFontType {
public:
    static PDFErrorOr<NonnullOwnPtr<CIDFontType2>> create(Document*, NonnullRefPtr<DictObject> const& descendant, float font_size);

    virtual PDFErrorOr<void> draw_glyph(Gfx::Painter&, Gfx::FloatPoint, float width, u32 cid, Renderer const&) override;
};

PDFErrorOr<NonnullOwnPtr<CIDFontType2>> CIDFontType2::create(Document* document, NonnullRefPtr<DictObject> const& descendant, float font_size)
{
    auto descriptor = TRY(descendant->get_dict(document, CommonNames::FontDescriptor));

    if (descendant->contains(CommonNames::CIDToGIDMap)) {
        auto value = TRY(descendant->get_object(document, CommonNames::CIDToGIDMap));
        if (value->is<StreamObject>()) {
            return Error::rendering_unsupported_error("Type0 font subtype 2: support for stream cid maps not yet implemented");
        } else if (value->cast<NameObject>()->name() != "Identity") {
            return Error::rendering_unsupported_error("Type0 font: support for non-Identity named cid maps not yet implemented");
        }
    }

    RefPtr<Gfx::Font> font;
    if (descriptor->contains(CommonNames::FontFile2)) {
        auto font_file_stream = TRY(descriptor->get_stream(document, CommonNames::FontFile2));
        float point_size = (font_size * POINTS_PER_INCH) / DEFAULT_DPI;
        // FIXME: Load font_file_stream->bytes() as TTF data, similar to TrueTypeFont::initialize().
        //        Unfortunately, TTF data in Type0 CIDFontType2 fonts don't contain the "cmap" table
        //        that's mandatory per TTF spec and the PDF stores that mapping in CIDToGIDMap instead.
        //        OpenType::Font::try_load currently rejects TTF data without "cmap" data.
        (void)font_file_stream;
        (void)point_size;
    }

    return TRY(adopt_nonnull_own_or_enomem(new (nothrow) CIDFontType2()));
}

PDFErrorOr<void> CIDFontType2::draw_glyph(Gfx::Painter&, Gfx::FloatPoint, float, u32, Renderer const&)
{
    // ISO 32000 (PDF 2.0) 9.7.4.2 Glyph selection in CIDFonts
    // "For Type 2, the CIDFont program is actually a TrueType font program, which has no native notion of CIDs.
    //  In a TrueType font program, glyph descriptions are identified by glyph index values.
    //  Glyph indices are internal to the font and are not defined consistently from one font to another.
    //  Instead, a TrueType font program contains a "cmap" table that provides mappings directly from
    //  character codes to glyph indices for one or more predefined encodings.
    //  TrueType font programs are integrated with the CID-keyed font architecture in one of two ways,
    //  depending on whether the font program is embedded in the PDF file:
    //  * If the TrueType font program is embedded, the Type 2 CIDFont dictionary shall contain a CIDToGIDMap entry
    //    that maps CIDs to the glyph indices for the appropriate glyph descriptions in that font program.
    //  * If the TrueType font program is not embedded but is referenced by name, and the Type 2 CIDFont dictionary
    //    contains a CIDToGIDMap entry, the CIDToGIDMap entry shall be ignored, since it is not meaningful
    ///   to refer to glyph indices in an external font program."
    return Error::rendering_unsupported_error("Type0 font CIDFontType2 not implemented yet");
}

Type0Font::Type0Font() = default;
Type0Font::~Type0Font() = default;

PDFErrorOr<void> Type0Font::initialize(Document* document, NonnullRefPtr<DictObject> const& dict, float font_size)
{
    TRY(PDFFont::initialize(document, dict, font_size));

    m_base_font_name = TRY(dict->get_name(document, CommonNames::BaseFont))->name();

    // FIXME: Support arbitrary CMaps
    auto cmap_value = TRY(dict->get_object(document, CommonNames::Encoding));
    if (!cmap_value->is<NameObject>())
        return Error::rendering_unsupported_error("Type0 font: support for general type 0 cmaps not yet implemented");

    auto cmap_name = cmap_value->cast<NameObject>()->name();
    if (cmap_name != CommonNames::IdentityH) {
        return Error::rendering_unsupported_error("Type0 font: unimplemented named type 0 cmap {}", cmap_name);
    }

    auto descendant_font_value = TRY(dict->get_array(document, CommonNames::DescendantFonts));
    auto descendant_font = TRY(descendant_font_value->get_dict_at(document, 0));

    auto system_info_dict = TRY(descendant_font->get_dict(document, CommonNames::CIDSystemInfo));
    auto registry = TRY(system_info_dict->get_string(document, CommonNames::Registry))->string();
    auto ordering = TRY(system_info_dict->get_string(document, CommonNames::Ordering))->string();
    u8 supplement = system_info_dict->get_value(CommonNames::Supplement).get<int>();
    CIDSystemInfo system_info { registry, ordering, supplement };

    auto subtype = TRY(descendant_font->get_name(document, CommonNames::Subtype))->name();
    if (subtype == CommonNames::CIDFontType0) {
        // CFF-based
        m_cid_font_type = TRY(CIDFontType0::create(document, descendant_font));
    } else if (subtype == CommonNames::CIDFontType2) {
        // TrueType-based
        m_cid_font_type = TRY(CIDFontType2::create(document, descendant_font, font_size));
    } else {
        return Error { Error::Type::MalformedPDF, "invalid /Subtype for Type 0 font" };
    }

    u16 default_width = 1000;
    if (descendant_font->contains(CommonNames::DW))
        default_width = descendant_font->get_value(CommonNames::DW).to_int();

    HashMap<u16, u16> widths;

    if (descendant_font->contains(CommonNames::W)) {
        auto widths_array = MUST(descendant_font->get_array(document, CommonNames::W));
        Optional<u16> pending_code;

        for (size_t i = 0; i < widths_array->size(); i++) {
            auto& value = widths_array->at(i);
            if (!pending_code.has_value()) {
                pending_code = value.to_int();
            } else if (value.has_number()) {
                auto first_code = pending_code.release_value();
                auto last_code = value.to_int();
                auto width = widths_array->at(i + 1).to_int();
                for (u16 code = first_code; code <= last_code; code++)
                    widths.set(code, width);

                i++;
            } else {
                auto array = TRY(document->resolve_to<ArrayObject>(value));
                auto code = pending_code.release_value();
                for (auto& width : *array)
                    widths.set(code++, width.to_int());
            }
        }
    }

    m_system_info = move(system_info);
    m_widths = move(widths);
    m_missing_width = default_width;
    return {};
}

float Type0Font::get_char_width(u16 char_code) const
{
    u16 width;
    if (auto char_code_width = m_widths.get(char_code); char_code_width.has_value()) {
        width = char_code_width.value();
    } else {
        width = m_missing_width;
    }

    return static_cast<float>(width) / 1000.0f;
}

void Type0Font::set_font_size(float)
{
}

PDFErrorOr<Gfx::FloatPoint> Type0Font::draw_string(Gfx::Painter& painter, Gfx::FloatPoint glyph_position, ByteString const& string, Renderer const& renderer)
{
    // Type0 fonts map bytes to character IDs ("CIDs"), and then CIDs to glyphs.

    // ISO 32000 (PDF 2.0) 9.7.6.2 CMap mapping describes how to map bytes to CIDs:
    // "The Encoding entry of a Type 0 font dictionary specifies a CMap [...]
    //  A sequence of one or more bytes shall be extracted from the string and matched against
    //  the codespace ranges in the CMap. That is, the first byte shall be matched against 1-byte codespace ranges;
    //  if no match is found, a second byte shall be extracted, and the 2-byte code shall be matched against 2-byte
    //  codespace ranges [...]"

    // 9.7.5.2 Predefined CMaps:
    // "When the current font is a Type 0 font whose Encoding entry is Identity-H or Identity-V,
    //  the string to be shown shall contain pairs of bytes representing CIDs, high-order byte first."
    // Type0Font::initialize() currently rejects everything except Identity-H.
    // FIXME: Support more.
    if (string.length() % 2 != 0)
        return Error::malformed_error("Identity-H but length not multiple of 2");

    auto cids = ReadonlySpan<BigEndian<u16>>(reinterpret_cast<BigEndian<u16> const*>(string.characters()), string.length() / 2);

    auto horizontal_scaling = renderer.text_state().horizontal_scaling;

    auto const& text_rendering_matrix = renderer.calculate_text_rendering_matrix();

    // TrueType fonts are prescaled to text_rendering_matrix.x_scale() * text_state().font_size / horizontal_scaling,
    // cf `Renderer::text_set_font()`. That's the width we get back from `get_glyph_width()` if we use a fallback
    // (or built-in) font. Scale the width size too, so the m_width.get() codepath is consistent.
    auto const font_size = text_rendering_matrix.x_scale() * renderer.text_state().font_size / horizontal_scaling;

    auto character_spacing = renderer.text_state().character_spacing;
    auto word_spacing = renderer.text_state().word_spacing;

    for (auto cid : cids) {
        // Use the width specified in the font's dictionary if available,
        // and use the default width for the given font otherwise.
        float glyph_width;
        if (auto width = m_widths.get(cid); width.has_value())
            glyph_width = font_size * width.value() / 1000.0f;
        else
            glyph_width = font_size * m_missing_width / 1000.0f;

        Gfx::FloatPoint glyph_render_position = text_rendering_matrix.map(glyph_position);
        TRY(m_cid_font_type->draw_glyph(painter, glyph_render_position, glyph_width, cid, renderer));

        // FIXME: Honor encoding's WMode for vertical text.

        // glyph_width is scaled by `text_rendering_matrix.x_scale() * renderer.text_state().font_size / horizontal_scaling`,
        // but it should only be scaled by `renderer.text_state().font_size`.
        // FIXME: Having to divide here isn't pretty. Refactor things so that this isn't needed.
        auto tx = glyph_width / text_rendering_matrix.x_scale() * horizontal_scaling;
        tx += character_spacing;

        // ISO 32000 (PDF 2.0), 9.3.3 Wordspacing
        // "Word spacing shall be applied to every occurrence of the single-byte character code 32
        // in a string when using a simple font (including Type 3) or a composite font that defines
        // code 32 as a single-byte code."
        // FIXME: Identity-H always uses 2 bytes, but this will be true once we support more encodings.
        bool was_single_byte_code = false;
        if (cid == ' ' && was_single_byte_code)
            tx += word_spacing;

        glyph_position += { tx, 0.0f };
    }
    return glyph_position;
}

}
