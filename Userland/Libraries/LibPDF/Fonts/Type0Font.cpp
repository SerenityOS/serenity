/*
 * Copyright (c) 2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Font/OpenType/Font.h>
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
    virtual void set_font_size(float) = 0;
};

class CIDFontType0 : public CIDFontType {
public:
    static PDFErrorOr<NonnullOwnPtr<CIDFontType0>> create(Document*, NonnullRefPtr<DictObject> const& descendant);

    virtual PDFErrorOr<void> draw_glyph(Gfx::Painter&, Gfx::FloatPoint, float width, u32 cid, Renderer const&) override;

    virtual void set_font_size(float) override { }

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
            // FIXME: Stop passing an external encoding to CFF::create().
            font_program = TRY(CFF::create(font_file_stream->bytes(), /*encoding=*/nullptr));
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

PDFErrorOr<void> CIDFontType0::draw_glyph(Gfx::Painter& painter, Gfx::FloatPoint point, float width, u32 cid, Renderer const& renderer)
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

    // FIXME: We currently only do the first.

    // FIXME: Do better than printing the cid to a string.
    auto char_name = ByteString::formatted("{}", cid);
    auto translation = m_font_program->glyph_translation(char_name, width);
    point = point.translated(translation);

    auto glyph_position = Gfx::GlyphRasterPosition::get_nearest_fit_for(point);

    // FIXME: Cache the font bitmap (but probably want to figure out rotation first).
    auto bitmap = m_font_program->rasterize_glyph(char_name, width, glyph_position.subpixel_offset);
    if (!bitmap)
        return Error::rendering_unsupported_error("Type0 font CIDFontType0: failed to rasterize glyph");

    auto style = renderer.state().paint_style;

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

class CIDFontType2 : public CIDFontType {
public:
    static PDFErrorOr<NonnullOwnPtr<CIDFontType2>> create(Document*, NonnullRefPtr<DictObject> const& descendant, float font_size);

    virtual PDFErrorOr<void> draw_glyph(Gfx::Painter&, Gfx::FloatPoint, float width, u32 cid, Renderer const&) override;

    virtual void set_font_size(float) override;

private:
    CIDFontType2(RefPtr<Gfx::ScaledFont> font)
        : m_font(move(font))
    {
    }

    RefPtr<Gfx::ScaledFont> m_font;
};

static PDFErrorOr<NonnullOwnPtr<OpenType::CharCodeToGlyphIndex>> create_cid_to_gid_map(Document* document, NonnullRefPtr<DictObject> const& dict)
{
    // "If the value is a stream, the bytes in the stream contain the mapping from CIDs to glyph indices:
    //  the glyph index for a particular CID value c is a 2-byte value stored in bytes 2×c and 2×c+1, where the first byte is the high-order byte.
    //  If the value of CIDToGIDMap is a name, it must be Identity, indicating that the mapping between CIDs and glyph indices is the identity mapping.
    //  Default value: Identity."

    class IdentityCIDToGIDMap : public OpenType::CharCodeToGlyphIndex {
    public:
        virtual u32 glyph_id_for_code_point(u32 char_code) const override { return char_code; }
    };

    class StreamCIDToGIDMap : public OpenType::CharCodeToGlyphIndex {
    public:
        StreamCIDToGIDMap(ReadonlyBytes bytes)
            : m_bytes(move(bytes))
        {
        }
        virtual u32 glyph_id_for_code_point(u32 char_code) const override
        {
            u32 index = char_code * 2;
            if (index + 1 >= m_bytes.size()) {
                // This can happen because Font::populate_glyph_page() with CIDs not used on the page and hence not in the font.
                return 0;
            }
            return (m_bytes[index] << 8) | m_bytes[index + 1];
        }

    private:
        ReadonlyBytes m_bytes;
    };

    if (!dict->contains(CommonNames::CIDToGIDMap))
        return make<IdentityCIDToGIDMap>();

    auto value = TRY(dict->get_object(document, CommonNames::CIDToGIDMap));
    if (value->is<StreamObject>())
        return make<StreamCIDToGIDMap>(value->cast<StreamObject>()->bytes());

    if (value->cast<NameObject>()->name() != "Identity")
        return Error::rendering_unsupported_error("Type0 font: The only valid CIDToGIDMap name is 'Identity'");
    return make<IdentityCIDToGIDMap>();
}

PDFErrorOr<NonnullOwnPtr<CIDFontType2>> CIDFontType2::create(Document* document, NonnullRefPtr<DictObject> const& descendant, float font_size)
{
    auto descriptor = TRY(descendant->get_dict(document, CommonNames::FontDescriptor));

    auto cid_to_gid_map = TRY(create_cid_to_gid_map(document, descendant));

    RefPtr<Gfx::ScaledFont> font;
    if (descriptor->contains(CommonNames::FontFile2)) {
        auto font_file_stream = TRY(descriptor->get_stream(document, CommonNames::FontFile2));
        float point_size = (font_size * POINTS_PER_INCH) / DEFAULT_DPI;
        auto ttf_font = TRY(OpenType::Font::try_load_from_externally_owned_memory(font_file_stream->bytes(), { .external_cmap = move(cid_to_gid_map), .skip_tables = pdf_skipped_opentype_tables }));
        font = adopt_ref(*new Gfx::ScaledFont(*ttf_font, point_size, point_size));
    }

    if (!font) {
        // FIXME: Should we use a fallback font? How common is this for type 0 fonts?
        return Error::malformed_error("CIDFontType2: missing FontFile2");
    }

    return TRY(adopt_nonnull_own_or_enomem(new (nothrow) CIDFontType2(move(font))));
}

PDFErrorOr<void> CIDFontType2::draw_glyph(Gfx::Painter& painter, Gfx::FloatPoint point, float width, u32 char_code, Renderer const& renderer)
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
    //    to refer to glyph indices in an external font program."

    // FIXME: We don't support non-embedded type0 truetype fonts yet.

    auto style = renderer.state().paint_style;

    // Undo shift in Glyf::Glyph::append_simple_path() via OpenType::Font::rasterize_glyph().
    auto position = point.translated(0, -m_font->pixel_metrics().ascent);

    if (style.has<Color>()) {
        painter.draw_glyph(position, char_code, *m_font, style.get<Color>());
    } else {
        // FIXME: Bounding box and sample point look to be pretty wrong
        style.get<NonnullRefPtr<Gfx::PaintStyle>>()->paint(Gfx::IntRect(position.x(), position.y(), width, 0), [&](auto sample) {
            painter.draw_glyph(position, char_code, *m_font, sample(Gfx::IntPoint(position.x(), position.y())));
        });
    }
    return {};
}

void CIDFontType2::set_font_size(float font_size)
{
    m_font = m_font->scaled_with_size((font_size * POINTS_PER_INCH) / DEFAULT_DPI);
}

Type0Font::Type0Font() = default;
Type0Font::~Type0Font() = default;

class IdentityType0CMap : public Type0CMap {
public:
    IdentityType0CMap(WritingMode writing_mode)
        : Type0CMap(writing_mode)
    {
    }

    virtual PDFErrorOr<NonnullOwnPtr<CIDIterator>> iterate(ReadonlyBytes bytes) const override
    {
        // 9.7.5.2 Predefined CMaps:
        // "When the current font is a Type 0 font whose Encoding entry is Identity-H or Identity-V,
        //  the string to be shown shall contain pairs of bytes representing CIDs, high-order byte first."
        if (bytes.size() % 2 != 0)
            return Error::malformed_error("Identity-H but length not multiple of 2");

        class IdentityCIDIterator : public CIDIterator {
        public:
            IdentityCIDIterator(ReadonlyBytes bytes)
                : m_bytes(bytes)
            {
            }

            virtual bool has_next() const override { return m_index < m_bytes.size(); }
            virtual u32 next() override
            {
                u32 result = (m_bytes[m_index] << 8) | m_bytes[m_index + 1];
                m_index += 2;
                return result;
            }

        private:
            ReadonlyBytes m_bytes;
            size_t m_index { 0 };
        };

        return make<IdentityCIDIterator>(bytes);
    }
};

static PDFErrorOr<NonnullOwnPtr<Type0CMap>> make_cmap(NonnullRefPtr<Object> const& cmap_value)
{
    // FIXME: Support arbitrary CMaps
    if (!cmap_value->is<NameObject>())
        return Error::rendering_unsupported_error("Type0 font: support for general type 0 cmaps not yet implemented");

    auto cmap_name = cmap_value->cast<NameObject>()->name();
    if (cmap_name != CommonNames::IdentityH && cmap_name != CommonNames::IdentityV)
        return Error::rendering_unsupported_error("Type0 font: unimplemented named type 0 cmap {}", cmap_name);

    WritingMode writing_mode = cmap_name.ends_with("-H"sv) ? WritingMode::Horizontal : WritingMode::Vertical;

    return make<IdentityType0CMap>(writing_mode);
}

PDFErrorOr<void> Type0Font::initialize(Document* document, NonnullRefPtr<DictObject> const& dict, float font_size)
{
    TRY(PDFFont::initialize(document, dict, font_size));

    m_base_font_name = TRY(dict->get_name(document, CommonNames::BaseFont))->name();

    m_cmap = TRY(make_cmap(TRY(dict->get_object(document, CommonNames::Encoding))));

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

    // PDF 1.7 spec, 5.6.3 CIDFonts, Glyph Metrics in CIDFonts, and TABLE 5.14 Entries in a CIDFont dictionary
    // "The DW entry defines the default width, which is used for all glyphs whose widths are not specified individually."
    u16 default_width = 1000;
    if (descendant_font->contains(CommonNames::DW))
        default_width = descendant_font->get_value(CommonNames::DW).to_int();

    // "The W array allows the definition of widths for individual CIDs."
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
                i++;

                for (u16 code = first_code; code <= last_code; code++)
                    widths.set(code, width);
            } else {
                auto array = TRY(document->resolve_to<ArrayObject>(value));
                auto code = pending_code.release_value();
                for (auto& width : *array)
                    widths.set(code++, width.to_int());
            }
        }
    }

    // "The default position vector and vertical displacement vector are specified by the DW2 entry in the CIDFont dictionary."
    int default_position_vector_y = 880;
    int default_displacement_vector_y = -1000;
    if (descendant_font->contains(CommonNames::DW2)) {
        auto widths_array = MUST(descendant_font->get_array(document, CommonNames::DW2));
        VERIFY(widths_array->size() == 2);
        default_position_vector_y = widths_array->at(0).to_int();
        default_displacement_vector_y = widths_array->at(1).to_int();
    }

    // "The W2 array allows the definition of vertical metrics for individual CIDs."
    HashMap<u16, VerticalMetric> vertical_metrics;
    if (descendant_font->contains(CommonNames::W2)) {
        auto widths_array = MUST(descendant_font->get_array(document, CommonNames::W2));
        Optional<u16> pending_code;

        for (size_t i = 0; i < widths_array->size(); i++) {
            auto& value = widths_array->at(i);
            if (!pending_code.has_value()) {
                pending_code = value.to_int();
            } else if (value.has_number()) {
                auto first_code = pending_code.release_value();
                auto last_code = value.to_int();
                auto vertical_displacement_vector_y = widths_array->at(i + 1).to_int();
                auto position_vector_x = widths_array->at(i + 2).to_int();
                auto position_vector_y = widths_array->at(i + 3).to_int();
                i += 3;

                for (u16 code = first_code; code <= last_code; code++)
                    vertical_metrics.set(code, VerticalMetric { vertical_displacement_vector_y, position_vector_x, position_vector_y });
            } else {
                auto array = TRY(document->resolve_to<ArrayObject>(value));
                VERIFY(array->size() % 3 == 0);
                auto code = pending_code.release_value();
                for (size_t j = 0; j < array->size(); j += 3) {
                    auto vertical_displacement_vector_y = array->at(j).to_int();
                    auto position_vector_x = array->at(j + 1).to_int();
                    auto position_vector_y = array->at(j + 2).to_int();
                    vertical_metrics.set(code++, VerticalMetric { vertical_displacement_vector_y, position_vector_x, position_vector_y });
                }
            }
        }
    }

    m_system_info = move(system_info);
    m_widths = move(widths);
    m_missing_width = default_width;
    m_default_position_vector_y = default_position_vector_y;
    m_default_displacement_vector_y = default_displacement_vector_y;
    m_vertical_metrics = move(vertical_metrics);
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

void Type0Font::set_font_size(float font_size)
{
    m_cid_font_type->set_font_size(font_size);
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

    auto horizontal_scaling = renderer.text_state().horizontal_scaling;

    auto const& text_rendering_matrix = renderer.calculate_text_rendering_matrix();

    // TrueType fonts are prescaled to text_rendering_matrix.x_scale() * text_state().font_size / horizontal_scaling,
    // cf `Renderer::text_set_font()`. That's the width we get back from `get_glyph_width()` if we use a fallback
    // (or built-in) font. Scale the width size too, so the m_width.get() codepath is consistent.
    auto const font_size = text_rendering_matrix.x_scale() * renderer.text_state().font_size / horizontal_scaling;

    auto character_spacing = renderer.text_state().character_spacing;
    auto word_spacing = renderer.text_state().word_spacing;

    WritingMode writing_mode = m_cmap->writing_mode();

    auto cids = TRY(m_cmap->iterate(string.bytes()));
    while (cids->has_next()) {
        auto cid = cids->next();

        // FIGURE 5.5 Metrics for horizontal and vertical writing modes

        // Use the width specified in the font's dictionary if available,
        // and use the default width for the given font otherwise.
        float glyph_width;
        if (auto width = m_widths.get(cid); width.has_value())
            glyph_width = font_size * width.value() / 1000.0f;
        else
            glyph_width = font_size * m_missing_width / 1000.0f;

        VerticalMetric vertical_metric;
        float vertical_displacement_vector_y;
        float position_vector_x = 0.0f;
        float position_vector_y = 0.0f;
        if (writing_mode == WritingMode::Vertical) {
            if (auto metric = m_vertical_metrics.get(cid); metric.has_value()) {
                vertical_metric = metric.value();
                vertical_displacement_vector_y = renderer.text_state().font_size * vertical_metric.vertical_displacement_vector_y / 1000.0f;
                position_vector_x = vertical_metric.position_vector_x / 1000.0f;
                position_vector_y = vertical_metric.position_vector_y / 1000.0f;
            } else {
                vertical_displacement_vector_y = renderer.text_state().font_size * m_default_displacement_vector_y / 1000.0f;
                position_vector_x = glyph_width / 2.0f / font_size;
                position_vector_y = m_default_position_vector_y / 1000.0f;
            }
        }

        if (renderer.text_state().rendering_mode != TextRenderingMode::Invisible || renderer.show_hidden_text()) {
            Gfx::FloatPoint glyph_render_position = text_rendering_matrix.map(glyph_position - Gfx::FloatPoint { position_vector_x, position_vector_y });
            TRY(m_cid_font_type->draw_glyph(painter, glyph_render_position, glyph_width, cid, renderer));
        }

        // glyph_width is scaled by `text_rendering_matrix.x_scale() * renderer.text_state().font_size / horizontal_scaling`,
        // but it should only be scaled by `renderer.text_state().font_size`.
        // FIXME: Having to divide here isn't pretty. Refactor things so that this isn't needed.
        float displacement;
        if (writing_mode == WritingMode::Horizontal)
            displacement = glyph_width / text_rendering_matrix.x_scale() * horizontal_scaling;
        else
            displacement = vertical_displacement_vector_y;
        displacement += character_spacing;

        // ISO 32000 (PDF 2.0), 9.3.3 Wordspacing
        // "Word spacing shall be applied to every occurrence of the single-byte character code 32
        // in a string when using a simple font (including Type 3) or a composite font that defines
        // code 32 as a single-byte code."
        // FIXME: Identity-H always uses 2 bytes, but this will be true once we support more encodings.
        bool was_single_byte_code = false;
        if (cid == ' ' && was_single_byte_code)
            displacement += word_spacing;

        if (writing_mode == WritingMode::Horizontal)
            glyph_position += { displacement, 0.0f };
        else
            glyph_position += { 0.0f, displacement };
    }
    return glyph_position;
}

}
