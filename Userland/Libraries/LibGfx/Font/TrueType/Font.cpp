/*
 * Copyright (c) 2020, Srimanta Barua <srimanta.barua1@gmail.com>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Checked.h>
#include <AK/Try.h>
#include <LibCore/MappedFile.h>
#include <LibGfx/Font/TrueType/Cmap.h>
#include <LibGfx/Font/TrueType/Font.h>
#include <LibGfx/Font/TrueType/Glyf.h>
#include <LibGfx/Font/TrueType/Tables.h>
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

ErrorOr<Kern> Kern::from_slice(ReadonlyBytes slice)
{
    if (slice.size() < sizeof(u32))
        return Error::from_string_literal("Invalid kern table header"sv);

    // We only support the old (2x u16) version of the header
    auto version = be_u16(slice.data());
    auto number_of_subtables = be_u16(slice.offset(sizeof(u16)));
    if (version != 0)
        return Error::from_string_literal("Unsupported kern table version"sv);
    if (number_of_subtables == 0)
        return Error::from_string_literal("Kern table does not contain any subtables"sv);

    // Read all subtable offsets
    auto subtable_offsets = TRY(FixedArray<size_t>::try_create(number_of_subtables));
    size_t offset = 2 * sizeof(u16);
    for (size_t i = 0; i < number_of_subtables; ++i) {
        if (slice.size() < offset + Sizes::SubtableHeader)
            return Error::from_string_literal("Invalid kern subtable header"sv);

        subtable_offsets[i] = offset;
        auto subtable_size = be_u16(slice.offset(offset + sizeof(u16)));
        offset += subtable_size;
    }

    return Kern(slice, move(subtable_offsets));
}

i16 Kern::get_glyph_kerning(u16 left_glyph_id, u16 right_glyph_id) const
{
    VERIFY(left_glyph_id > 0 && right_glyph_id > 0);

    i16 glyph_kerning = 0;
    for (auto subtable_offset : m_subtable_offsets) {
        auto subtable_slice = m_slice.slice(subtable_offset);

        auto version = be_u16(subtable_slice.data());
        auto length = be_u16(subtable_slice.offset(sizeof(u16)));
        auto coverage = be_u16(subtable_slice.offset(2 * sizeof(u16)));

        if (version != 0) {
            dbgln("TTF::Kern: unsupported subtable version {}", version);
            continue;
        }

        if (subtable_slice.size() < length) {
            dbgln("TTF::Kern: subtable has an invalid size {}", length);
            continue;
        }

        auto is_horizontal = (coverage & (1 << 0)) > 0;
        auto is_minimum = (coverage & (1 << 1)) > 0;
        auto is_cross_stream = (coverage & (1 << 2)) > 0;
        auto is_override = (coverage & (1 << 3)) > 0;
        auto reserved_bits = (coverage & 0xF0);
        auto format = (coverage & 0xFF00) >> 8;

        // FIXME: implement support for these features
        if (!is_horizontal || is_minimum || is_cross_stream || (reserved_bits > 0)) {
            dbgln("TTF::Kern: FIXME: implement missing feature support for subtable");
            continue;
        }

        // FIXME: implement support for subtable formats other than 0
        Optional<i16> subtable_kerning;
        switch (format) {
        case 0:
            subtable_kerning = read_glyph_kerning_format0(subtable_slice.slice(Sizes::SubtableHeader), left_glyph_id, right_glyph_id);
            break;
        default:
            dbgln("TTF::Kern: FIXME: subtable format {} is unsupported", format);
            continue;
        }
        if (!subtable_kerning.has_value())
            continue;
        auto kerning_value = subtable_kerning.release_value();

        if (is_override)
            glyph_kerning = kerning_value;
        else
            glyph_kerning += kerning_value;
    }
    return glyph_kerning;
}

Optional<i16> Kern::read_glyph_kerning_format0(ReadonlyBytes slice, u16 left_glyph_id, u16 right_glyph_id)
{
    if (slice.size() < 4 * sizeof(u16))
        return {};

    u16 number_of_pairs = be_u16(slice.data());
    u16 search_range = be_u16(slice.offset_pointer(sizeof(u16)));
    u16 entry_selector = be_u16(slice.offset_pointer(2 * sizeof(u16)));
    u16 range_shift = be_u16(slice.offset_pointer(3 * sizeof(u16)));

    // Sanity checks for this table format
    auto pairs_in_search_range = search_range / Sizes::Format0Entry;
    if (number_of_pairs == 0)
        return {};
    if (pairs_in_search_range > number_of_pairs)
        return {};
    if ((1 << entry_selector) * Sizes::Format0Entry != search_range)
        return {};
    if ((number_of_pairs - pairs_in_search_range) * Sizes::Format0Entry != range_shift)
        return {};

    // FIXME: implement a possibly slightly more efficient binary search using the parameters above
    auto search_slice = slice.slice(4 * sizeof(u16));
    size_t left_idx = 0;
    size_t right_idx = number_of_pairs - 1;
    for (auto i = 0; i < 16; ++i) {
        size_t pivot_idx = (left_idx + right_idx) / 2;

        u16 pivot_left_glyph_id = be_u16(search_slice.offset(pivot_idx * Sizes::Format0Entry + 0));
        u16 pivot_right_glyph_id = be_u16(search_slice.offset(pivot_idx * Sizes::Format0Entry + 2));

        // Match
        if (pivot_left_glyph_id == left_glyph_id && pivot_right_glyph_id == right_glyph_id)
            return be_i16(search_slice.offset(pivot_idx * Sizes::Format0Entry + 4));

        // Narrow search area
        if (pivot_left_glyph_id < left_glyph_id || (pivot_left_glyph_id == left_glyph_id && pivot_right_glyph_id < right_glyph_id))
            left_idx = pivot_idx + 1;
        else if (pivot_idx == left_idx)
            break;
        else
            right_idx = pivot_idx - 1;
    }
    return 0;
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
    Optional<ReadonlyBytes> opt_kern_slice = {};

    Optional<Head> opt_head = {};
    Optional<Name> opt_name = {};
    Optional<Hhea> opt_hhea = {};
    Optional<Maxp> opt_maxp = {};
    Optional<Hmtx> opt_hmtx = {};
    Optional<Cmap> opt_cmap = {};
    Optional<Loca> opt_loca = {};
    Optional<OS2> opt_os2 = {};
    Optional<Kern> opt_kern = {};

    auto num_tables = be_u16(buffer.offset_pointer(offset + (u32)Offsets::NumTables));
    if (buffer.size() < offset + (u32)Sizes::OffsetTable + num_tables * (u32)Sizes::TableRecord)
        return Error::from_string_literal("Font file too small"sv);

    for (auto i = 0; i < num_tables; i++) {
        u32 record_offset = offset + (u32)Sizes::OffsetTable + i * (u32)Sizes::TableRecord;
        u32 tag = be_u32(buffer.offset_pointer(record_offset));
        u32 table_offset = be_u32(buffer.offset_pointer(record_offset + (u32)Offsets::TableRecord_Offset));
        u32 table_length = be_u32(buffer.offset_pointer(record_offset + (u32)Offsets::TableRecord_Length));

        if (Checked<u32>::addition_would_overflow(table_offset, table_length))
            return Error::from_string_literal("Invalid table offset or length in font"sv);

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
        } else if (tag == tag_from_str("kern")) {
            opt_kern_slice = buffer_here;
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

    Optional<Kern> kern {};
    if (opt_kern_slice.has_value())
        kern = TRY(Kern::from_slice(opt_kern_slice.value()));

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
            return Error::from_string_literal("Invalid Platform ID"sv);

        if (platform.value() == Cmap::Subtable::Platform::Windows) {
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

    return adopt_ref(*new Font(move(buffer), move(head), move(name), move(hhea), move(maxp), move(hmtx), move(cmap), move(loca), move(glyf), move(os2), move(kern)));
}

Gfx::ScaledFontMetrics Font::metrics([[maybe_unused]] float x_scale, float y_scale) const
{
    auto ascender = m_hhea.ascender() * y_scale;
    auto descender = m_hhea.descender() * y_scale;
    auto line_gap = m_hhea.line_gap() * y_scale;

    return Gfx::ScaledFontMetrics {
        .ascender = ascender,
        .descender = descender,
        .line_gap = line_gap,
    };
}

// FIXME: "loca" and "glyf" are not available for CFF fonts.
Gfx::ScaledGlyphMetrics Font::glyph_metrics(u32 glyph_id, float x_scale, float y_scale) const
{
    if (glyph_id >= glyph_count()) {
        glyph_id = 0;
    }
    auto horizontal_metrics = m_hmtx.get_glyph_horizontal_metrics(glyph_id);
    auto glyph_offset = m_loca.get_glyph_offset(glyph_id);
    auto glyph = m_glyf.glyph(glyph_offset);
    int ascender = glyph.ascender();
    int descender = glyph.descender();
    return Gfx::ScaledGlyphMetrics {
        .ascender = (int)roundf(ascender * y_scale),
        .descender = (int)roundf(descender * y_scale),
        .advance_width = (int)roundf(horizontal_metrics.advance_width * x_scale),
        .left_side_bearing = (int)roundf(horizontal_metrics.left_side_bearing * x_scale),
    };
}

float Font::glyphs_horizontal_kerning(u32 left_glyph_id, u32 right_glyph_id, float x_scale) const
{
    if (!m_kern.has_value())
        return 0.f;
    return m_kern->get_glyph_kerning(left_glyph_id, right_glyph_id) * x_scale;
}

// FIXME: "loca" and "glyf" are not available for CFF fonts.
RefPtr<Gfx::Bitmap> Font::rasterize_glyph(u32 glyph_id, float x_scale, float y_scale) const
{
    if (glyph_id >= glyph_count()) {
        glyph_id = 0;
    }
    auto glyph_offset = m_loca.get_glyph_offset(glyph_id);
    auto glyph = m_glyf.glyph(glyph_offset);
    return glyph.rasterize(m_hhea.ascender(), m_hhea.descender(), x_scale, y_scale, [&](u16 glyph_id) {
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

u8 Font::slope() const
{
    // https://docs.microsoft.com/en-us/typography/opentype/spec/os2
    constexpr u16 italic_selection_bit { 1 };
    constexpr u16 oblique_selection_bit { 512 };
    // https://docs.microsoft.com/en-us/typography/opentype/spec/head
    constexpr u16 italic_style_bit { 2 };

    if (m_os2.selection() & oblique_selection_bit)
        return 2;
    if (m_os2.selection() & italic_selection_bit)
        return 1;
    if (m_head.style() & italic_style_bit)
        return 1;

    return 0;
}

bool Font::is_fixed_width() const
{
    // FIXME: Read this information from the font file itself.
    // FIXME: Although, it appears some application do similar hacks
    return glyph_metrics(glyph_id_for_code_point('.'), 1, 1).advance_width == glyph_metrics(glyph_id_for_code_point('X'), 1, 1).advance_width;
}

u16 OS2::weight_class() const
{
    return be_u16(m_slice.offset_pointer((u32)Offsets::WeightClass));
}

u16 OS2::selection() const
{
    return be_u16(m_slice.offset_pointer((u32)Offsets::Selection));
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
