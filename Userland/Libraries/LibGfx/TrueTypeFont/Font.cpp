/*
 * Copyright (c) 2020, Srimanta Barua <srimanta.barua1@gmail.com>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Checked.h>
#include <AK/Try.h>
#include <AK/Utf32View.h>
#include <AK/Utf8View.h>
#include <LibCore/File.h>
#include <LibCore/MappedFile.h>
#include <LibGfx/TrueTypeFont/Cmap.h>
#include <LibGfx/TrueTypeFont/Font.h>
#include <LibGfx/TrueTypeFont/Glyf.h>
#include <LibGfx/TrueTypeFont/Tables.h>
#include <LibTextCodec/Decoder.h>
#include <math.h>
#include <sys/mman.h>

namespace TTF {

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

Optional<Head> Head::from_slice(ReadonlyBytes slice)
{
    if (slice.size() < (size_t)Sizes::Table) {
        return {};
    }
    return Head(slice);
}

u16 Head::units_per_em() const
{
    return be_u16(m_slice.offset_pointer((u32)Offsets::UnitsPerEM));
}

i16 Head::xmin() const
{
    return be_i16(m_slice.offset_pointer((u32)Offsets::XMin));
}

i16 Head::ymin() const
{
    return be_i16(m_slice.offset_pointer((u32)Offsets::YMin));
}

i16 Head::xmax() const
{
    return be_i16(m_slice.offset_pointer((u32)Offsets::XMax));
}

i16 Head::ymax() const
{
    return be_i16(m_slice.offset_pointer((u32)Offsets::YMax));
}

u16 Head::style() const
{
    return be_u16(m_slice.offset_pointer((u32)Offsets::Style));
}

u16 Head::lowest_recommended_ppem() const
{
    return be_u16(m_slice.offset_pointer((u32)Offsets::LowestRecPPEM));
}

IndexToLocFormat Head::index_to_loc_format() const
{
    i16 raw = be_i16(m_slice.offset_pointer((u32)Offsets::IndexToLocFormat));
    switch (raw) {
    case 0:
        return IndexToLocFormat::Offset16;
    case 1:
        return IndexToLocFormat::Offset32;
    default:
        VERIFY_NOT_REACHED();
    }
}

Optional<Hhea> Hhea::from_slice(ReadonlyBytes slice)
{
    if (slice.size() < (size_t)Sizes::Table) {
        return {};
    }
    return Hhea(slice);
}

i16 Hhea::ascender() const
{
    return be_i16(m_slice.offset_pointer((u32)Offsets::Ascender));
}

i16 Hhea::descender() const
{
    return be_i16(m_slice.offset_pointer((u32)Offsets::Descender));
}

i16 Hhea::line_gap() const
{
    return be_i16(m_slice.offset_pointer((u32)Offsets::LineGap));
}

u16 Hhea::advance_width_max() const
{
    return be_u16(m_slice.offset_pointer((u32)Offsets::AdvanceWidthMax));
}

u16 Hhea::number_of_h_metrics() const
{
    return be_u16(m_slice.offset_pointer((u32)Offsets::NumberOfHMetrics));
}

Optional<Maxp> Maxp::from_slice(ReadonlyBytes slice)
{
    if (slice.size() < (size_t)Sizes::TableV0p5) {
        return {};
    }
    return Maxp(slice);
}

u16 Maxp::num_glyphs() const
{
    return be_u16(m_slice.offset_pointer((u32)Offsets::NumGlyphs));
}

Optional<Hmtx> Hmtx::from_slice(ReadonlyBytes slice, u32 num_glyphs, u32 number_of_h_metrics)
{
    if (slice.size() < number_of_h_metrics * (u32)Sizes::LongHorMetric + (num_glyphs - number_of_h_metrics) * (u32)Sizes::LeftSideBearing) {
        return {};
    }
    return Hmtx(slice, num_glyphs, number_of_h_metrics);
}

Optional<Name> Name::from_slice(ReadonlyBytes slice)
{
    return Name(slice);
}

String Name::string_for_id(NameId id) const
{
    auto num_entries = be_u16(m_slice.offset_pointer(2));
    auto string_offset = be_u16(m_slice.offset_pointer(4));

    Vector<int> valid_ids;

    for (int i = 0; i < num_entries; ++i) {
        auto this_id = be_u16(m_slice.offset_pointer(6 + i * 12 + 6));
        if (this_id == (u16)id)
            valid_ids.append(i);
    }

    if (valid_ids.is_empty())
        return String::empty();

    auto it = valid_ids.find_if([this](auto const& i) {
        // check if font has naming table for en-US language id
        auto platform = be_u16(m_slice.offset_pointer(6 + i * 12 + 0));
        auto language_id = be_u16(m_slice.offset_pointer(6 + i * 12 + 4));
        return (platform == (u16)Platform::Macintosh && language_id == (u16)MacintoshLanguage::English)
            || (platform == (u16)Platform::Windows && language_id == (u16)WindowsLanguage::EnglishUnitedStates);
    });
    auto i = it != valid_ids.end() ? *it : valid_ids.first();

    auto platform = be_u16(m_slice.offset_pointer(6 + i * 12 + 0));
    auto length = be_u16(m_slice.offset_pointer(6 + i * 12 + 8));
    auto offset = be_u16(m_slice.offset_pointer(6 + i * 12 + 10));

    if (platform == (u16)Platform::Windows) {
        static auto& decoder = *TextCodec::decoder_for("utf-16be");
        return decoder.to_utf8(StringView { (char const*)m_slice.offset_pointer(string_offset + offset), length });
    }

    return String((char const*)m_slice.offset_pointer(string_offset + offset), length);
}

GlyphHorizontalMetrics Hmtx::get_glyph_horizontal_metrics(u32 glyph_id) const
{
    VERIFY(glyph_id < m_num_glyphs);
    if (glyph_id < m_number_of_h_metrics) {
        auto offset = glyph_id * (u32)Sizes::LongHorMetric;
        u16 advance_width = be_u16(m_slice.offset_pointer(offset));
        i16 left_side_bearing = be_i16(m_slice.offset_pointer(offset + 2));
        return GlyphHorizontalMetrics {
            .advance_width = advance_width,
            .left_side_bearing = left_side_bearing,
        };
    }
    auto offset = m_number_of_h_metrics * (u32)Sizes::LongHorMetric + (glyph_id - m_number_of_h_metrics) * (u32)Sizes::LeftSideBearing;
    u16 advance_width = be_u16(m_slice.offset_pointer((m_number_of_h_metrics - 1) * (u32)Sizes::LongHorMetric));
    i16 left_side_bearing = be_i16(m_slice.offset_pointer(offset));
    return GlyphHorizontalMetrics {
        .advance_width = advance_width,
        .left_side_bearing = left_side_bearing,
    };
}

ErrorOr<NonnullRefPtr<Font>> Font::try_load_from_file(String path, unsigned index)
{
    auto file = TRY(Core::MappedFile::map(path));
    auto font = TRY(try_load_from_externally_owned_memory(file->bytes(), index));
    font->m_mapped_file = move(file);
    return font;
}

ErrorOr<NonnullRefPtr<Font>> Font::try_load_from_externally_owned_memory(ReadonlyBytes buffer, unsigned index)
{
    if (buffer.size() < 4)
        return Error::from_string_literal("Font file too small"sv);

    u32 tag = be_u32(buffer.data());
    if (tag == tag_from_str("ttcf")) {
        // It's a font collection
        if (buffer.size() < (u32)Sizes::TTCHeaderV1 + sizeof(u32) * (index + 1))
            return Error::from_string_literal("Font file too small"sv);

        u32 offset = be_u32(buffer.offset_pointer((u32)Sizes::TTCHeaderV1 + sizeof(u32) * index));
        return try_load_from_offset(buffer, offset);
    }
    if (tag == tag_from_str("OTTO"))
        return Error::from_string_literal("CFF fonts not supported yet"sv);

    if (tag != 0x00010000)
        return Error::from_string_literal("Not a valid font"sv);

    return try_load_from_offset(buffer, 0);
}

// FIXME: "loca" and "glyf" are not available for CFF fonts.
ErrorOr<NonnullRefPtr<Font>> Font::try_load_from_offset(ReadonlyBytes buffer, u32 offset)
{
    if (Checked<u32>::addition_would_overflow(offset, (u32)Sizes::OffsetTable))
        return Error::from_string_literal("Invalid offset in font header"sv);

    if (buffer.size() < offset + (u32)Sizes::OffsetTable)
        return Error::from_string_literal("Font file too small"sv);

    Optional<ReadonlyBytes> opt_head_slice = {};
    Optional<ReadonlyBytes> opt_name_slice = {};
    Optional<ReadonlyBytes> opt_hhea_slice = {};
    Optional<ReadonlyBytes> opt_maxp_slice = {};
    Optional<ReadonlyBytes> opt_hmtx_slice = {};
    Optional<ReadonlyBytes> opt_cmap_slice = {};
    Optional<ReadonlyBytes> opt_loca_slice = {};
    Optional<ReadonlyBytes> opt_glyf_slice = {};
    Optional<ReadonlyBytes> opt_os2_slice = {};

    Optional<Head> opt_head = {};
    Optional<Name> opt_name = {};
    Optional<Hhea> opt_hhea = {};
    Optional<Maxp> opt_maxp = {};
    Optional<Hmtx> opt_hmtx = {};
    Optional<Cmap> opt_cmap = {};
    Optional<Loca> opt_loca = {};
    Optional<OS2> opt_os2 = {};

    auto num_tables = be_u16(buffer.offset_pointer(offset + (u32)Offsets::NumTables));
    if (buffer.size() < offset + (u32)Sizes::OffsetTable + num_tables * (u32)Sizes::TableRecord)
        return Error::from_string_literal("Font file too small"sv);

    for (auto i = 0; i < num_tables; i++) {
        u32 record_offset = offset + (u32)Sizes::OffsetTable + i * (u32)Sizes::TableRecord;
        u32 tag = be_u32(buffer.offset_pointer(record_offset));
        u32 table_offset = be_u32(buffer.offset_pointer(record_offset + (u32)Offsets::TableRecord_Offset));
        u32 table_length = be_u32(buffer.offset_pointer(record_offset + (u32)Offsets::TableRecord_Length));

        if (Checked<u32>::addition_would_overflow(table_offset, table_length))
            return Error::from_string_literal("Invalid table offset/length in font."sv);

        if (buffer.size() < table_offset + table_length)
            return Error::from_string_literal("Font file too small"sv);

        auto buffer_here = ReadonlyBytes(buffer.offset_pointer(table_offset), table_length);

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
        }
    }

    if (!opt_head_slice.has_value() || !(opt_head = Head::from_slice(opt_head_slice.value())).has_value())
        return Error::from_string_literal("Could not load Head"sv);
    auto head = opt_head.value();

    if (!opt_name_slice.has_value() || !(opt_name = Name::from_slice(opt_name_slice.value())).has_value())
        return Error::from_string_literal("Could not load Name"sv);
    auto name = opt_name.value();

    if (!opt_hhea_slice.has_value() || !(opt_hhea = Hhea::from_slice(opt_hhea_slice.value())).has_value())
        return Error::from_string_literal("Could not load Hhea"sv);
    auto hhea = opt_hhea.value();

    if (!opt_maxp_slice.has_value() || !(opt_maxp = Maxp::from_slice(opt_maxp_slice.value())).has_value())
        return Error::from_string_literal("Could not load Maxp"sv);
    auto maxp = opt_maxp.value();

    if (!opt_hmtx_slice.has_value() || !(opt_hmtx = Hmtx::from_slice(opt_hmtx_slice.value(), maxp.num_glyphs(), hhea.number_of_h_metrics())).has_value())
        return Error::from_string_literal("Could not load Hmtx"sv);
    auto hmtx = opt_hmtx.value();

    if (!opt_cmap_slice.has_value() || !(opt_cmap = Cmap::from_slice(opt_cmap_slice.value())).has_value())
        return Error::from_string_literal("Could not load Cmap"sv);
    auto cmap = opt_cmap.value();

    if (!opt_loca_slice.has_value() || !(opt_loca = Loca::from_slice(opt_loca_slice.value(), maxp.num_glyphs(), head.index_to_loc_format())).has_value())
        return Error::from_string_literal("Could not load Loca"sv);
    auto loca = opt_loca.value();

    if (!opt_glyf_slice.has_value())
        return Error::from_string_literal("Could not load Glyf"sv);
    auto glyf = Glyf(opt_glyf_slice.value());

    if (!opt_os2_slice.has_value())
        return Error::from_string_literal("Could not load OS/2"sv);
    auto os2 = OS2(opt_os2_slice.value());

    // Select cmap table. FIXME: Do this better. Right now, just looks for platform "Windows"
    // and corresponding encoding "Unicode full repertoire", or failing that, "Unicode BMP"
    for (u32 i = 0; i < cmap.num_subtables(); i++) {
        auto opt_subtable = cmap.subtable(i);
        if (!opt_subtable.has_value()) {
            continue;
        }
        auto subtable = opt_subtable.value();
        if (subtable.platform_id() == Cmap::Subtable::Platform::Windows) {
            if (subtable.encoding_id() == (u16)Cmap::Subtable::WindowsEncoding::UnicodeFullRepertoire) {
                cmap.set_active_index(i);
                break;
            }
            if (subtable.encoding_id() == (u16)Cmap::Subtable::WindowsEncoding::UnicodeBMP) {
                cmap.set_active_index(i);
                break;
            }
        }
    }

    return adopt_ref(*new Font(move(buffer), move(head), move(name), move(hhea), move(maxp), move(hmtx), move(cmap), move(loca), move(glyf), move(os2)));
}

ScaledFontMetrics Font::metrics(float x_scale, float y_scale) const
{
    auto ascender = m_hhea.ascender() * y_scale;
    auto descender = m_hhea.descender() * y_scale;
    auto line_gap = m_hhea.line_gap() * y_scale;
    auto advance_width_max = m_hhea.advance_width_max() * x_scale;
    return ScaledFontMetrics {
        .ascender = (int)roundf(ascender),
        .descender = (int)roundf(descender),
        .line_gap = (int)roundf(line_gap),
        .advance_width_max = (int)roundf(advance_width_max),
    };
}

// FIXME: "loca" and "glyf" are not available for CFF fonts.
ScaledGlyphMetrics Font::glyph_metrics(u32 glyph_id, float x_scale, float y_scale) const
{
    if (glyph_id >= glyph_count()) {
        glyph_id = 0;
    }
    auto horizontal_metrics = m_hmtx.get_glyph_horizontal_metrics(glyph_id);
    auto glyph_offset = m_loca.get_glyph_offset(glyph_id);
    auto glyph = m_glyf.glyph(glyph_offset);
    int ascender = glyph.ascender();
    int descender = glyph.descender();
    return ScaledGlyphMetrics {
        .ascender = (int)roundf(ascender * y_scale),
        .descender = (int)roundf(descender * y_scale),
        .advance_width = (int)roundf(horizontal_metrics.advance_width * x_scale),
        .left_side_bearing = (int)roundf(horizontal_metrics.left_side_bearing * x_scale),
    };
}

// FIXME: "loca" and "glyf" are not available for CFF fonts.
RefPtr<Gfx::Bitmap> Font::rasterize_glyph(u32 glyph_id, float x_scale, float y_scale) const
{
    if (glyph_id >= glyph_count()) {
        glyph_id = 0;
    }
    auto glyph_offset = m_loca.get_glyph_offset(glyph_id);
    auto glyph = m_glyf.glyph(glyph_offset);
    return glyph.rasterize(m_os2.typographic_ascender(), m_os2.typographic_descender(), x_scale, y_scale, [&](u16 glyph_id) {
        if (glyph_id >= glyph_count()) {
            glyph_id = 0;
        }
        auto glyph_offset = m_loca.get_glyph_offset(glyph_id);
        return m_glyf.glyph(glyph_offset);
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
    if (m_os2.weight_class())
        return m_os2.weight_class();
    if (m_head.style() & bold_bit)
        return 700;

    return 400;
}

bool Font::is_fixed_width() const
{
    // FIXME: Read this information from the font file itself.
    // FIXME: Although, it appears some application do similar hacks
    return glyph_metrics(glyph_id_for_code_point('.'), 1, 1).advance_width == glyph_metrics(glyph_id_for_code_point('X'), 1, 1).advance_width;
}

int ScaledFont::width(StringView view) const { return unicode_view_width(Utf8View(view)); }
int ScaledFont::width(Utf8View const& view) const { return unicode_view_width(view); }
int ScaledFont::width(Utf32View const& view) const { return unicode_view_width(view); }

template<typename T>
ALWAYS_INLINE int ScaledFont::unicode_view_width(T const& view) const
{
    if (view.is_empty())
        return 0;
    int width = 0;
    int longest_width = 0;
    for (auto code_point : view) {
        if (code_point == '\n' || code_point == '\r') {
            longest_width = max(width, longest_width);
            width = 0;
            continue;
        }
        u32 glyph_id = glyph_id_for_code_point(code_point);
        auto metrics = glyph_metrics(glyph_id);
        width += metrics.advance_width;
    }
    longest_width = max(width, longest_width);
    return longest_width;
}

RefPtr<Gfx::Bitmap> ScaledFont::rasterize_glyph(u32 glyph_id) const
{
    auto glyph_iterator = m_cached_glyph_bitmaps.find(glyph_id);
    if (glyph_iterator != m_cached_glyph_bitmaps.end())
        return glyph_iterator->value;

    auto glyph_bitmap = m_font->rasterize_glyph(glyph_id, m_x_scale, m_y_scale);
    m_cached_glyph_bitmaps.set(glyph_id, glyph_bitmap);
    return glyph_bitmap;
}

Gfx::Glyph ScaledFont::glyph(u32 code_point) const
{
    auto id = glyph_id_for_code_point(code_point);
    auto bitmap = rasterize_glyph(id);
    auto metrics = glyph_metrics(id);
    return Gfx::Glyph(bitmap, metrics.left_side_bearing, metrics.advance_width, metrics.ascender);
}

u8 ScaledFont::glyph_width(u32 code_point) const
{
    auto id = glyph_id_for_code_point(code_point);
    auto metrics = glyph_metrics(id);
    return metrics.advance_width;
}

int ScaledFont::glyph_or_emoji_width(u32 code_point) const
{
    auto id = glyph_id_for_code_point(code_point);
    auto metrics = glyph_metrics(id);
    return metrics.advance_width;
}

u8 ScaledFont::glyph_fixed_width() const
{
    return glyph_metrics(glyph_id_for_code_point(' ')).advance_width;
}

u16 OS2::weight_class() const
{
    return be_u16(m_slice.offset_pointer((u32)Offsets::WeightClass));
}

i16 OS2::typographic_ascender() const
{
    return be_i16(m_slice.offset_pointer((u32)Offsets::TypographicAscender));
}

i16 OS2::typographic_descender() const
{
    return be_i16(m_slice.offset_pointer((u32)Offsets::TypographicDescender));
}

i16 OS2::typographic_line_gap() const
{
    return be_i16(m_slice.offset_pointer((u32)Offsets::TypographicLineGap));
}

}
