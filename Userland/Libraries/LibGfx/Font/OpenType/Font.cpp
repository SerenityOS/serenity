/*
 * Copyright (c) 2020, Srimanta Barua <srimanta.barua1@gmail.com>
 * Copyright (c) 2021-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 * Copyright (c) 2023, Lukas Affolter <git@lukasach.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Checked.h>
#include <AK/MemoryStream.h>
#include <AK/Try.h>
#include <LibCore/MappedFile.h>
#include <LibCore/Resource.h>
#include <LibGfx/Font/OpenType/Cmap.h>
#include <LibGfx/Font/OpenType/Font.h>
#include <LibGfx/Font/OpenType/Glyf.h>
#include <LibGfx/Font/OpenType/Tables.h>
#include <LibGfx/ImageFormats/PNGLoader.h>
#include <math.h>
#include <sys/mman.h>

namespace OpenType {

// https://learn.microsoft.com/en-us/typography/opentype/spec/otff#ttc-header
struct [[gnu::packed]] TTCHeaderV1 {
    Tag ttc_tag;                      // Font Collection ID string: 'ttcf' (used for fonts with CFF or CFF2 outlines as well as TrueType outlines)
    BigEndian<u16> major_version;     // Major version of the TTC Header, = 1.
    BigEndian<u16> minor_version;     // Minor version of the TTC Header, = 0.
    BigEndian<u32> num_fonts;         // Number of fonts in TTC
    Offset32 table_directory_offsets; // Array of offsets to the TableDirectory for each font from the beginning of the file
};
static_assert(AssertSize<TTCHeaderV1, 16>());

}

template<>
class AK::Traits<OpenType::TTCHeaderV1> : public GenericTraits<OpenType::TTCHeaderV1> {
public:
    static constexpr bool is_trivially_serializable() { return true; }
};

namespace OpenType {

u16 be_u16(u8 const*);
u32 be_u32(u8 const*);
i16 be_i16(u8 const*);
float be_fword(u8 const*);
u32 tag_from_str(char const*);

u16 be_u16(u8 const* ptr)
{
    return (((u16)ptr[0]) << 8) | ((u16)ptr[1]);
}

u32 be_u32(u8 const* ptr)
{
    return (((u32)ptr[0]) << 24) | (((u32)ptr[1]) << 16) | (((u32)ptr[2]) << 8) | ((u32)ptr[3]);
}

i16 be_i16(u8 const* ptr)
{
    return (((i16)ptr[0]) << 8) | ((i16)ptr[1]);
}

float be_fword(u8 const* ptr)
{
    return (float)be_i16(ptr) / (float)(1 << 14);
}

u32 tag_from_str(char const* str)
{
    return be_u32((u8 const*)str);
}

ErrorOr<NonnullRefPtr<Font>> Font::try_load_from_resource(Core::Resource const& resource, unsigned index)
{
    auto font = TRY(try_load_from_externally_owned_memory(resource.data(), index));
    font->m_resource = resource;
    return font;
}

ErrorOr<NonnullRefPtr<Font>> Font::try_load_from_externally_owned_memory(ReadonlyBytes buffer, unsigned index)
{
    FixedMemoryStream stream { buffer };

    auto tag = TRY(stream.read_value<Tag>());
    if (tag == tag_from_str("ttcf")) {
        // It's a font collection
        TRY(stream.seek(0, SeekMode::SetPosition));
        auto ttc_header_v1 = TRY(stream.read_in_place<TTCHeaderV1>());
        // FIXME: Check for major_version == 2.

        if (index >= ttc_header_v1->num_fonts)
            return Error::from_string_literal("Requested font index is too large");

        TRY(stream.seek(ttc_header_v1->table_directory_offsets + sizeof(u32) * index, SeekMode::SetPosition));
        auto offset = TRY(stream.read_value<BigEndian<u32>>());
        return try_load_from_offset(buffer, offset);
    }
    if (tag == tag_from_str("OTTO"))
        return Error::from_string_literal("CFF fonts not supported yet");

    if (tag != 0x00010000 && tag != tag_from_str("true"))
        return Error::from_string_literal("Not a valid font");

    return try_load_from_offset(buffer, 0);
}

// FIXME: "loca" and "glyf" are not available for CFF fonts.
ErrorOr<NonnullRefPtr<Font>> Font::try_load_from_offset(ReadonlyBytes buffer, u32 offset)
{
    if (Checked<u32>::addition_would_overflow(offset, (u32)Sizes::OffsetTable))
        return Error::from_string_literal("Invalid offset in font header");

    if (buffer.size() <= offset + (u32)Sizes::OffsetTable)
        return Error::from_string_literal("Font file too small");

    Optional<ReadonlyBytes> opt_head_slice = {};
    Optional<ReadonlyBytes> opt_name_slice = {};
    Optional<ReadonlyBytes> opt_hhea_slice = {};
    Optional<ReadonlyBytes> opt_maxp_slice = {};
    Optional<ReadonlyBytes> opt_hmtx_slice = {};
    Optional<ReadonlyBytes> opt_cmap_slice = {};
    Optional<ReadonlyBytes> opt_loca_slice = {};
    Optional<ReadonlyBytes> opt_glyf_slice = {};
    Optional<ReadonlyBytes> opt_os2_slice = {};
    Optional<ReadonlyBytes> opt_kern_slice = {};
    Optional<ReadonlyBytes> opt_fpgm_slice = {};
    Optional<ReadonlyBytes> opt_prep_slice = {};

    Optional<Head> opt_head = {};
    Optional<Name> opt_name = {};
    Optional<Hhea> opt_hhea = {};
    Optional<Maxp> opt_maxp = {};
    Optional<Hmtx> opt_hmtx = {};
    Optional<Cmap> opt_cmap = {};
    Optional<OS2> opt_os2 = {};
    Optional<Kern> opt_kern = {};
    Optional<Fpgm> opt_fpgm = {};
    Optional<Prep> opt_prep = {};
    Optional<CBLC> cblc;
    Optional<CBDT> cbdt;
    Optional<GPOS> gpos;

    auto num_tables = be_u16(buffer.offset(offset + (u32)Offsets::NumTables));
    if (buffer.size() <= offset + (u32)Sizes::OffsetTable + num_tables * (u32)Sizes::TableRecord)
        return Error::from_string_literal("Font file too small");

    for (auto i = 0; i < num_tables; i++) {
        u32 record_offset = offset + (u32)Sizes::OffsetTable + i * (u32)Sizes::TableRecord;
        u32 tag = be_u32(buffer.offset(record_offset));
        u32 table_offset = be_u32(buffer.offset(record_offset + (u32)Offsets::TableRecord_Offset));
        u32 table_length = be_u32(buffer.offset(record_offset + (u32)Offsets::TableRecord_Length));

        if (table_length == 0 || Checked<u32>::addition_would_overflow(table_offset, table_length))
            return Error::from_string_literal("Invalid table offset or length in font");

        if (buffer.size() < table_offset + table_length)
            return Error::from_string_literal("Font file too small");

        auto buffer_here = ReadonlyBytes(buffer.offset(table_offset), table_length);

        // Get the table offsets we need.
        if (tag == tag_from_str("head")) {
            opt_head_slice = buffer_here;
        } else if (tag == tag_from_str("name")) {
            opt_name_slice = buffer_here;
        } else if (tag == tag_from_str("hhea")) {
            opt_hhea_slice = buffer_here;
        } else if (tag == tag_from_str("maxp")) {
            opt_maxp_slice = buffer_here;
        } else if (tag == tag_from_str("hmtx")) {
            opt_hmtx_slice = buffer_here;
        } else if (tag == tag_from_str("cmap")) {
            opt_cmap_slice = buffer_here;
        } else if (tag == tag_from_str("loca")) {
            opt_loca_slice = buffer_here;
        } else if (tag == tag_from_str("glyf")) {
            opt_glyf_slice = buffer_here;
        } else if (tag == tag_from_str("OS/2")) {
            opt_os2_slice = buffer_here;
        } else if (tag == tag_from_str("kern")) {
            opt_kern_slice = buffer_here;
        } else if (tag == tag_from_str("fpgm")) {
            opt_fpgm_slice = buffer_here;
        } else if (tag == tag_from_str("prep")) {
            opt_prep_slice = buffer_here;
        } else if (tag == tag_from_str("CBLC")) {
            cblc = TRY(CBLC::from_slice(buffer_here));
        } else if (tag == tag_from_str("CBDT")) {
            cbdt = TRY(CBDT::from_slice(buffer_here));
        } else if (tag == tag_from_str("GPOS")) {
            gpos = TRY(GPOS::from_slice(buffer_here));
        }
    }

    if (!opt_head_slice.has_value() || !(opt_head = Head::from_slice(opt_head_slice.value())).has_value())
        return Error::from_string_literal("Could not load Head");
    auto head = opt_head.value();

    if (!opt_name_slice.has_value() || !(opt_name = Name::from_slice(opt_name_slice.value())).has_value())
        return Error::from_string_literal("Could not load Name");
    auto name = opt_name.value();

    if (!opt_hhea_slice.has_value() || !(opt_hhea = Hhea::from_slice(opt_hhea_slice.value())).has_value())
        return Error::from_string_literal("Could not load Hhea");
    auto hhea = opt_hhea.value();

    if (!opt_maxp_slice.has_value() || !(opt_maxp = Maxp::from_slice(opt_maxp_slice.value())).has_value())
        return Error::from_string_literal("Could not load Maxp");
    auto maxp = opt_maxp.value();

    if (!opt_hmtx_slice.has_value() || !(opt_hmtx = Hmtx::from_slice(opt_hmtx_slice.value(), maxp.num_glyphs(), hhea.number_of_h_metrics())).has_value())
        return Error::from_string_literal("Could not load Hmtx");
    auto hmtx = opt_hmtx.value();

    if (!opt_cmap_slice.has_value() || !(opt_cmap = Cmap::from_slice(opt_cmap_slice.value())).has_value())
        return Error::from_string_literal("Could not load Cmap");
    auto cmap = opt_cmap.value();

    Optional<Loca> loca;
    if (opt_loca_slice.has_value()) {
        loca = Loca::from_slice(opt_loca_slice.value(), maxp.num_glyphs(), head.index_to_loc_format());
        if (!loca.has_value())
            return Error::from_string_literal("Could not load Loca");
    }

    Optional<Glyf> glyf;
    if (opt_glyf_slice.has_value()) {
        glyf = Glyf(opt_glyf_slice.value());
    }

    Optional<OS2> os2;
    if (opt_os2_slice.has_value())
        os2 = OS2(opt_os2_slice.value());

    Optional<Kern> kern {};
    if (opt_kern_slice.has_value())
        kern = TRY(Kern::from_slice(opt_kern_slice.value()));

    Optional<Fpgm> fpgm;
    if (opt_fpgm_slice.has_value())
        fpgm = Fpgm(opt_fpgm_slice.value());

    Optional<Prep> prep;
    if (opt_prep_slice.has_value())
        prep = Prep(opt_prep_slice.value());

    // Select cmap table. FIXME: Do this better. Right now, just looks for platform "Windows"
    // and corresponding encoding "Unicode full repertoire", or failing that, "Unicode BMP"
    for (u32 i = 0; i < cmap.num_subtables(); i++) {
        auto opt_subtable = cmap.subtable(i);
        if (!opt_subtable.has_value()) {
            continue;
        }
        auto subtable = opt_subtable.value();
        auto platform = subtable.platform_id();
        if (!platform.has_value())
            return Error::from_string_literal("Invalid Platform ID");

        /* NOTE: The encoding records are sorted first by platform ID, then by encoding ID.
           This means that the Windows platform will take precedence over Macintosh, which is
           usually what we want here. */
        if (platform.value() == Cmap::Subtable::Platform::Windows) {
            if (subtable.encoding_id() == (u16)Cmap::Subtable::WindowsEncoding::UnicodeFullRepertoire) {
                cmap.set_active_index(i);
                break;
            }
            if (subtable.encoding_id() == (u16)Cmap::Subtable::WindowsEncoding::UnicodeBMP) {
                cmap.set_active_index(i);
                break;
            }
        } else if (platform.value() == Cmap::Subtable::Platform::Macintosh) {
            cmap.set_active_index(i);
        }
    }

    return adopt_ref(*new Font(
        move(buffer),
        move(head),
        move(name),
        move(hhea),
        move(maxp),
        move(hmtx),
        move(cmap),
        move(loca),
        move(glyf),
        move(os2),
        move(kern),
        move(fpgm),
        move(prep),
        move(cblc),
        move(cbdt),
        move(gpos)));
}

Gfx::ScaledFontMetrics Font::metrics([[maybe_unused]] float x_scale, float y_scale) const
{
    i16 raw_ascender;
    i16 raw_descender;
    i16 raw_line_gap;
    Optional<i16> x_height;

    if (m_os2.has_value() && m_os2->use_typographic_metrics()) {
        raw_ascender = m_os2->typographic_ascender();
        raw_descender = m_os2->typographic_descender();
        raw_line_gap = m_os2->typographic_line_gap();
        x_height = m_os2->x_height();
    } else {
        raw_ascender = m_hhea.ascender();
        raw_descender = m_hhea.descender();
        raw_line_gap = m_hhea.line_gap();
    }

    if (!x_height.has_value()) {
        x_height = glyph_metrics(glyph_id_for_code_point('x'), 1, 1, 1, 1).ascender;
    }

    return Gfx::ScaledFontMetrics {
        .ascender = static_cast<float>(raw_ascender) * y_scale,
        .descender = -static_cast<float>(raw_descender) * y_scale,
        .line_gap = static_cast<float>(raw_line_gap) * y_scale,
        .x_height = static_cast<float>(x_height.value()) * y_scale,
    };
}

Font::EmbeddedBitmapData Font::embedded_bitmap_data_for_glyph(u32 glyph_id) const
{
    if (!has_color_bitmaps())
        return Empty {};

    u16 first_glyph_index {};
    u16 last_glyph_index {};
    auto maybe_index_subtable = m_cblc->index_subtable_for_glyph_id(glyph_id, first_glyph_index, last_glyph_index);
    if (!maybe_index_subtable.has_value())
        return Empty {};

    auto const& index_subtable = maybe_index_subtable.value();
    auto const& bitmap_size = m_cblc->bitmap_size_for_glyph_id(glyph_id).value();

    if (index_subtable.index_format == 1) {
        auto const& index_subtable1 = *bit_cast<EBLC::IndexSubTable1 const*>(&index_subtable);
        size_t size_of_array = (last_glyph_index - first_glyph_index + 1) + 1;
        auto sbit_offsets = ReadonlySpan<Offset32> { index_subtable1.sbit_offsets, size_of_array };
        auto sbit_offset = sbit_offsets[glyph_id - first_glyph_index];
        size_t glyph_data_offset = sbit_offset + index_subtable.image_data_offset;

        if (index_subtable.image_format == 17) {
            return EmbeddedBitmapWithFormat17 {
                .bitmap_size = bitmap_size,
                .format17 = *bit_cast<CBDT::Format17 const*>(m_cbdt->bytes().slice(glyph_data_offset, size_of_array).data()),
            };
        }
        dbgln("FIXME: Implement OpenType embedded bitmap image format {}", index_subtable.image_format);
    } else {
        dbgln("FIXME: Implement OpenType embedded bitmap index format {}", index_subtable.index_format);
    }

    return Empty {};
}

Gfx::ScaledGlyphMetrics Font::glyph_metrics(u32 glyph_id, float x_scale, float y_scale, float point_width, float point_height) const
{
    auto embedded_bitmap_metrics = embedded_bitmap_data_for_glyph(glyph_id).visit(
        [&](EmbeddedBitmapWithFormat17 const& data) -> Optional<Gfx::ScaledGlyphMetrics> {
            // FIXME: This is a pretty ugly hack to work out new scale factors based on the relationship between
            //        the pixels-per-em values and the font point size. It appears that bitmaps are not in the same
            //        coordinate space as the head table's "units per em" value.
            //        There's definitely some cleaner way to do this.
            float x_scale = (point_width * 1.3333333f) / static_cast<float>(data.bitmap_size.ppem_x);
            float y_scale = (point_height * 1.3333333f) / static_cast<float>(data.bitmap_size.ppem_y);

            return Gfx::ScaledGlyphMetrics {
                .ascender = static_cast<float>(data.bitmap_size.hori.ascender) * y_scale,
                .descender = static_cast<float>(data.bitmap_size.hori.descender) * y_scale,
                .advance_width = static_cast<float>(data.format17.glyph_metrics.advance) * x_scale,
                .left_side_bearing = static_cast<float>(data.format17.glyph_metrics.bearing_x) * x_scale,
            };
        },
        [&](Empty) -> Optional<Gfx::ScaledGlyphMetrics> {
            // Unsupported format or no embedded bitmap for this glyph ID.
            return {};
        });

    if (embedded_bitmap_metrics.has_value()) {
        return embedded_bitmap_metrics.release_value();
    }

    if (!m_loca.has_value() || !m_glyf.has_value()) {
        return Gfx::ScaledGlyphMetrics {};
    }

    if (glyph_id >= glyph_count()) {
        glyph_id = 0;
    }
    auto horizontal_metrics = m_hmtx.get_glyph_horizontal_metrics(glyph_id);
    auto glyph_offset = m_loca->get_glyph_offset(glyph_id);
    auto glyph = m_glyf->glyph(glyph_offset);
    return Gfx::ScaledGlyphMetrics {
        .ascender = glyph.has_value() ? static_cast<float>(glyph->ascender()) * y_scale : 0,
        .descender = glyph.has_value() ? static_cast<float>(glyph->descender()) * y_scale : 0,
        .advance_width = static_cast<float>(horizontal_metrics.advance_width) * x_scale,
        .left_side_bearing = static_cast<float>(horizontal_metrics.left_side_bearing) * x_scale,
    };
}

float Font::glyphs_horizontal_kerning(u32 left_glyph_id, u32 right_glyph_id, float x_scale) const
{
    if (!m_gpos.has_value() && !m_kern.has_value())
        return 0.0f;

    // NOTE: OpenType glyph IDs are 16-bit, so this is safe.
    auto cache_key = (left_glyph_id << 16) | right_glyph_id;
    if (auto it = m_kerning_cache.find(cache_key); it != m_kerning_cache.end()) {
        return it->value * x_scale;
    }

    if (m_gpos.has_value()) {
        auto kerning = m_gpos->glyph_kerning(left_glyph_id, right_glyph_id);
        if (kerning.has_value()) {
            m_kerning_cache.set(cache_key, kerning.value());
            return kerning.value() * x_scale;
        }
    }

    if (m_kern.has_value()) {
        auto kerning = m_kern->get_glyph_kerning(left_glyph_id, right_glyph_id);
        m_kerning_cache.set(cache_key, kerning);
        return kerning * x_scale;
    }

    m_kerning_cache.set(cache_key, 0);
    return 0.0f;
}

RefPtr<Gfx::Bitmap> Font::rasterize_glyph(u32 glyph_id, float x_scale, float y_scale, Gfx::GlyphSubpixelOffset subpixel_offset) const
{
    if (auto bitmap = color_bitmap(glyph_id)) {
        return bitmap;
    }

    if (!m_loca.has_value() || !m_glyf.has_value()) {
        return nullptr;
    }

    if (glyph_id >= glyph_count()) {
        glyph_id = 0;
    }

    auto glyph_offset0 = m_loca->get_glyph_offset(glyph_id);
    auto glyph_offset1 = m_loca->get_glyph_offset(glyph_id + 1);

    // If a glyph has no outline, then loca[n] = loca [n+1].
    if (glyph_offset0 == glyph_offset1)
        return nullptr;

    auto glyph = m_glyf->glyph(glyph_offset0);
    if (!glyph.has_value())
        return nullptr;

    i16 ascender = 0;
    i16 descender = 0;

    if (m_os2.has_value() && m_os2->use_typographic_metrics()) {
        ascender = m_os2->typographic_ascender();
        descender = m_os2->typographic_descender();
    } else {
        ascender = m_hhea.ascender();
        descender = m_hhea.descender();
    }

    return glyph->rasterize(ascender, descender, x_scale, y_scale, subpixel_offset, [&](u16 glyph_id) {
        if (glyph_id >= glyph_count()) {
            glyph_id = 0;
        }
        auto glyph_offset = m_loca->get_glyph_offset(glyph_id);
        return m_glyf->glyph(glyph_offset);
    });
}

u32 Font::glyph_count() const
{
    return m_maxp.num_glyphs();
}

u16 Font::units_per_em() const
{
    return m_head.units_per_em();
}

String Font::family() const
{
    auto string = m_name.typographic_family_name();
    if (!string.is_empty())
        return string;
    return m_name.family_name();
}

String Font::variant() const
{
    auto string = m_name.typographic_subfamily_name();
    if (!string.is_empty())
        return string;
    return m_name.subfamily_name();
}

u16 Font::weight() const
{
    constexpr u16 bold_bit { 1 };
    if (m_os2.has_value() && m_os2->weight_class())
        return m_os2->weight_class();
    if (m_head.style() & bold_bit)
        return 700;

    return 400;
}

u16 Font::width() const
{
    if (m_os2.has_value()) {
        return m_os2->width_class();
    }

    return Gfx::FontWidth::Normal;
}

u8 Font::slope() const
{
    // https://docs.microsoft.com/en-us/typography/opentype/spec/os2
    constexpr u16 italic_selection_bit { 1 };
    constexpr u16 oblique_selection_bit { 512 };
    // https://docs.microsoft.com/en-us/typography/opentype/spec/head
    constexpr u16 italic_style_bit { 2 };

    if (m_os2.has_value() && m_os2->selection() & oblique_selection_bit)
        return 2;
    if (m_os2.has_value() && m_os2->selection() & italic_selection_bit)
        return 1;
    if (m_head.style() & italic_style_bit)
        return 1;

    return 0;
}

bool Font::is_fixed_width() const
{
    // FIXME: Read this information from the font file itself.
    // FIXME: Although, it appears some application do similar hacks
    return glyph_metrics(glyph_id_for_code_point('.'), 1, 1, 1, 1).advance_width == glyph_metrics(glyph_id_for_code_point('X'), 1, 1, 1, 1).advance_width;
}

Optional<ReadonlyBytes> Font::font_program() const
{
    if (m_fpgm.has_value())
        return m_fpgm->program_data();
    return {};
}

Optional<ReadonlyBytes> Font::control_value_program() const
{
    if (m_prep.has_value())
        return m_prep->program_data();
    return {};
}

Optional<ReadonlyBytes> Font::glyph_program(u32 glyph_id) const
{
    if (!m_loca.has_value() || !m_glyf.has_value()) {
        return {};
    }

    auto glyph_offset = m_loca->get_glyph_offset(glyph_id);
    auto glyph = m_glyf->glyph(glyph_offset);
    if (!glyph.has_value())
        return {};
    return glyph->program();
}

u32 Font::glyph_id_for_code_point(u32 code_point) const
{
    return glyph_page(code_point / GlyphPage::glyphs_per_page).glyph_ids[code_point % GlyphPage::glyphs_per_page];
}

Font::GlyphPage const& Font::glyph_page(size_t page_index) const
{
    if (page_index == 0) {
        if (!m_glyph_page_zero) {
            m_glyph_page_zero = make<GlyphPage>();
            populate_glyph_page(*m_glyph_page_zero, 0);
        }
        return *m_glyph_page_zero;
    }
    if (auto it = m_glyph_pages.find(page_index); it != m_glyph_pages.end()) {
        return *it->value;
    }

    auto glyph_page = make<GlyphPage>();
    populate_glyph_page(*glyph_page, page_index);
    auto const* glyph_page_ptr = glyph_page.ptr();
    m_glyph_pages.set(page_index, move(glyph_page));
    return *glyph_page_ptr;
}

void Font::populate_glyph_page(GlyphPage& glyph_page, size_t page_index) const
{
    u32 first_code_point = page_index * GlyphPage::glyphs_per_page;
    for (size_t i = 0; i < GlyphPage::glyphs_per_page; ++i) {
        u32 code_point = first_code_point + i;
        glyph_page.glyph_ids[i] = m_cmap.glyph_id_for_code_point(code_point);
    }
}
bool Font::has_color_bitmaps() const
{
    return m_cblc.has_value() && m_cbdt.has_value();
}

RefPtr<Gfx::Bitmap> Font::color_bitmap(u32 glyph_id) const
{
    return embedded_bitmap_data_for_glyph(glyph_id).visit(
        [&](EmbeddedBitmapWithFormat17 const& data) -> RefPtr<Gfx::Bitmap> {
            auto data_slice = ReadonlyBytes { data.format17.data, static_cast<u32>(data.format17.data_len) };
            auto decoder = Gfx::PNGImageDecoderPlugin::create(data_slice).release_value_but_fixme_should_propagate_errors();
            auto frame = decoder->frame(0);
            if (frame.is_error()) {
                dbgln("PNG decode failed");
                return nullptr;
            }
            return frame.value().image;
        },
        [&](Empty) -> RefPtr<Gfx::Bitmap> {
            // Unsupported format or no image for this glyph ID.
            return nullptr;
        });
}

}
