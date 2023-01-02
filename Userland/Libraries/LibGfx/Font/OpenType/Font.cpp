/*
 * Copyright (c) 2020, Srimanta Barua <srimanta.barua1@gmail.com>
 * Copyright (c) 2021-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/BinarySearch.h>
#include <AK/Checked.h>
#include <AK/Try.h>
#include <LibCore/MappedFile.h>
#include <LibGfx/Font/OpenType/Cmap.h>
#include <LibGfx/Font/OpenType/Font.h>
#include <LibGfx/Font/OpenType/Glyf.h>
#include <LibGfx/Font/OpenType/Tables.h>
#include <LibTextCodec/Decoder.h>
#include <math.h>
#include <sys/mman.h>

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

Optional<Head> Head::from_slice(ReadonlyBytes slice)
{
    if (slice.size() < sizeof(FontHeaderTable)) {
        return {};
    }
    return Head(slice);
}

u16 Head::units_per_em() const
{
    return header().units_per_em;
}

i16 Head::xmin() const
{
    return header().x_min;
}

i16 Head::ymin() const
{
    return header().y_min;
}

i16 Head::xmax() const
{
    return header().x_max;
}

i16 Head::ymax() const
{
    return header().y_max;
}

u16 Head::style() const
{
    return header().mac_style;
}

u16 Head::lowest_recommended_ppem() const
{
    return header().lowest_rec_ppem;
}

IndexToLocFormat Head::index_to_loc_format() const
{
    switch (header().index_to_loc_format) {
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
    if (slice.size() < sizeof(HorizontalHeaderTable)) {
        return {};
    }
    return Hhea(slice);
}

i16 Hhea::ascender() const
{
    return header().ascender;
}

i16 Hhea::descender() const
{
    return header().descender;
}

i16 Hhea::line_gap() const
{
    return header().line_gap;
}

u16 Hhea::advance_width_max() const
{
    return header().advance_width_max;
}

u16 Hhea::number_of_h_metrics() const
{
    return header().number_of_h_metrics;
}

Optional<Maxp> Maxp::from_slice(ReadonlyBytes slice)
{
    if (slice.size() < sizeof(MaximumProfileVersion0_5)) {
        return {};
    }
    return Maxp(slice);
}

u16 Maxp::num_glyphs() const
{
    return header().num_glyphs;
}

Optional<Hmtx> Hmtx::from_slice(ReadonlyBytes slice, u32 num_glyphs, u32 number_of_h_metrics)
{
    if (slice.size() < number_of_h_metrics * sizeof(LongHorMetric) + (num_glyphs - number_of_h_metrics) * sizeof(u16)) {
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
    if (slice.size() < sizeof(Header))
        return Error::from_string_literal("Invalid kern table header");

    // We only support the old (2x u16) version of the header
    auto const& header = *bit_cast<Header const*>(slice.data());
    auto version = header.version;
    auto number_of_subtables = header.n_tables;
    if (version != 0)
        return Error::from_string_literal("Unsupported kern table version");
    if (number_of_subtables == 0)
        return Error::from_string_literal("Kern table does not contain any subtables");

    // Read all subtable offsets
    auto subtable_offsets = TRY(FixedArray<size_t>::try_create(number_of_subtables));
    size_t offset = sizeof(Header);
    for (size_t i = 0; i < number_of_subtables; ++i) {
        if (slice.size() < offset + sizeof(SubtableHeader))
            return Error::from_string_literal("Invalid kern subtable header");
        auto const& subtable_header = *bit_cast<SubtableHeader const*>(slice.offset_pointer(offset));
        subtable_offsets[i] = offset;
        offset += subtable_header.length;
    }

    return Kern(slice, move(subtable_offsets));
}

i16 Kern::get_glyph_kerning(u16 left_glyph_id, u16 right_glyph_id) const
{
    VERIFY(left_glyph_id > 0 && right_glyph_id > 0);

    i16 glyph_kerning = 0;
    for (auto subtable_offset : m_subtable_offsets) {
        auto subtable_slice = m_slice.slice(subtable_offset);
        auto const& subtable_header = *bit_cast<SubtableHeader const*>(subtable_slice.data());

        auto version = subtable_header.version;
        auto length = subtable_header.version;
        auto coverage = subtable_header.coverage;

        if (version != 0) {
            dbgln("OpenType::Kern: unsupported subtable version {}", version);
            continue;
        }

        if (subtable_slice.size() < length) {
            dbgln("OpenType::Kern: subtable has an invalid size {}", length);
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
            dbgln("OpenType::Kern: FIXME: implement missing feature support for subtable");
            continue;
        }

        // FIXME: implement support for subtable formats other than 0
        Optional<i16> subtable_kerning;
        switch (format) {
        case 0:
            subtable_kerning = read_glyph_kerning_format0(subtable_slice.slice(sizeof(SubtableHeader)), left_glyph_id, right_glyph_id);
            break;
        default:
            dbgln("OpenType::Kern: FIXME: subtable format {} is unsupported", format);
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
    if (slice.size() < sizeof(Format0))
        return {};

    auto const& format0 = *bit_cast<Format0 const*>(slice.data());
    u16 number_of_pairs = format0.n_pairs;
    u16 search_range = format0.search_range;
    u16 entry_selector = format0.entry_selector;
    u16 range_shift = format0.range_shift;

    // Sanity checks for this table format
    auto pairs_in_search_range = search_range / sizeof(Format0Pair);
    if (number_of_pairs == 0)
        return {};
    if (pairs_in_search_range > number_of_pairs)
        return {};
    if ((1 << entry_selector) * sizeof(Format0Pair) != search_range)
        return {};
    if ((number_of_pairs - pairs_in_search_range) * sizeof(Format0Pair) != range_shift)
        return {};

    // FIXME: implement a possibly slightly more efficient binary search using the parameters above
    Span<Format0Pair const> pairs { bit_cast<Format0Pair const*>(slice.slice(sizeof(Format0)).data()), number_of_pairs };

    // The left and right halves of the kerning pair make an unsigned 32-bit number, which is then used to order the kerning pairs numerically.
    auto needle = (static_cast<u32>(left_glyph_id) << 16u) | static_cast<u32>(right_glyph_id);
    auto* pair = binary_search(pairs, nullptr, nullptr, [&](void*, Format0Pair const& pair) {
        auto as_u32 = (static_cast<u32>(pair.left) << 16u) | static_cast<u32>(pair.right);
        return needle - as_u32;
    });

    if (!pair)
        return 0;
    return pair->value;
}

DeprecatedString Name::string_for_id(NameId id) const
{
    auto const count = header().count;
    auto const storage_offset = header().storage_offset;

    Vector<int> valid_ids;

    for (size_t i = 0; i < count; ++i) {
        auto this_id = header().name_record[i].name_id;
        if (this_id == to_underlying(id))
            valid_ids.append(i);
    }

    if (valid_ids.is_empty())
        return DeprecatedString::empty();

    auto it = valid_ids.find_if([this](auto const& i) {
        // check if font has naming table for en-US language id
        auto const& name_record = header().name_record[i];
        auto const platform_id = name_record.platform_id;
        auto const language_id = name_record.language_id;
        return (platform_id == to_underlying(Platform::Macintosh) && language_id == to_underlying(MacintoshLanguage::English))
            || (platform_id == to_underlying(Platform::Windows) && language_id == to_underlying(WindowsLanguage::EnglishUnitedStates));
    });
    auto i = it != valid_ids.end() ? *it : valid_ids.first();

    auto const& name_record = header().name_record[i];

    auto const platform_id = name_record.platform_id;
    auto const length = name_record.length;
    auto const offset = name_record.string_offset;

    if (platform_id == to_underlying(Platform::Windows)) {
        static auto& decoder = *TextCodec::decoder_for("utf-16be");
        return decoder.to_utf8(StringView { (char const*)m_slice.offset_pointer(storage_offset + offset), length });
    }

    return DeprecatedString((char const*)m_slice.offset_pointer(storage_offset + offset), length);
}

GlyphHorizontalMetrics Hmtx::get_glyph_horizontal_metrics(u32 glyph_id) const
{
    VERIFY(glyph_id < m_num_glyphs);
    auto const* long_hor_metrics = bit_cast<LongHorMetric const*>(m_slice.data());
    if (glyph_id < m_number_of_h_metrics) {
        return GlyphHorizontalMetrics {
            .advance_width = static_cast<u16>(long_hor_metrics[glyph_id].advance_width),
            .left_side_bearing = static_cast<i16>(long_hor_metrics[glyph_id].lsb),
        };
    }

    auto const* left_side_bearings = bit_cast<BigEndian<u16> const*>(m_slice.offset_pointer(m_number_of_h_metrics * sizeof(LongHorMetric)));
    return GlyphHorizontalMetrics {
        .advance_width = static_cast<u16>(long_hor_metrics[m_number_of_h_metrics - 1].advance_width),
        .left_side_bearing = static_cast<i16>(left_side_bearings[glyph_id - m_number_of_h_metrics]),
    };
}

ErrorOr<NonnullRefPtr<Font>> Font::try_load_from_file(DeprecatedString path, unsigned index)
{
    auto file = TRY(Core::MappedFile::map(path));
    auto font = TRY(try_load_from_externally_owned_memory(file->bytes(), index));
    font->m_mapped_file = move(file);
    return font;
}

ErrorOr<NonnullRefPtr<Font>> Font::try_load_from_externally_owned_memory(ReadonlyBytes buffer, unsigned index)
{
    if (buffer.size() < 4)
        return Error::from_string_literal("Font file too small");

    u32 tag = be_u32(buffer.data());
    if (tag == tag_from_str("ttcf")) {
        // It's a font collection
        if (buffer.size() < (u32)Sizes::TTCHeaderV1 + sizeof(u32) * (index + 1))
            return Error::from_string_literal("Font file too small");

        u32 offset = be_u32(buffer.offset_pointer((u32)Sizes::TTCHeaderV1 + sizeof(u32) * index));
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

    if (buffer.size() < offset + (u32)Sizes::OffsetTable)
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
        return Error::from_string_literal("Font file too small");

    for (auto i = 0; i < num_tables; i++) {
        u32 record_offset = offset + (u32)Sizes::OffsetTable + i * (u32)Sizes::TableRecord;
        u32 tag = be_u32(buffer.offset_pointer(record_offset));
        u32 table_offset = be_u32(buffer.offset_pointer(record_offset + (u32)Offsets::TableRecord_Offset));
        u32 table_length = be_u32(buffer.offset_pointer(record_offset + (u32)Offsets::TableRecord_Length));

        if (Checked<u32>::addition_would_overflow(table_offset, table_length))
            return Error::from_string_literal("Invalid table offset or length in font");

        if (buffer.size() < table_offset + table_length)
            return Error::from_string_literal("Font file too small");

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

    if (!opt_loca_slice.has_value() || !(opt_loca = Loca::from_slice(opt_loca_slice.value(), maxp.num_glyphs(), head.index_to_loc_format())).has_value())
        return Error::from_string_literal("Could not load Loca");
    auto loca = opt_loca.value();

    if (!opt_glyf_slice.has_value())
        return Error::from_string_literal("Could not load Glyf");
    auto glyf = Glyf(opt_glyf_slice.value());

    Optional<OS2> os2;
    if (opt_os2_slice.has_value())
        os2 = OS2(opt_os2_slice.value());

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
            return Error::from_string_literal("Invalid Platform ID");

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
    return Gfx::ScaledGlyphMetrics {
        .ascender = static_cast<float>(glyph.ascender()) * y_scale,
        .descender = static_cast<float>(glyph.descender()) * y_scale,
        .advance_width = static_cast<float>(horizontal_metrics.advance_width) * x_scale,
        .left_side_bearing = static_cast<float>(horizontal_metrics.left_side_bearing) * x_scale,
    };
}

float Font::glyphs_horizontal_kerning(u32 left_glyph_id, u32 right_glyph_id, float x_scale) const
{
    if (!m_kern.has_value())
        return 0.f;
    return m_kern->get_glyph_kerning(left_glyph_id, right_glyph_id) * x_scale;
}

// FIXME: "loca" and "glyf" are not available for CFF fonts.
RefPtr<Gfx::Bitmap> Font::rasterize_glyph(u32 glyph_id, float x_scale, float y_scale, Gfx::GlyphSubpixelOffset subpixel_offset) const
{
    if (glyph_id >= glyph_count()) {
        glyph_id = 0;
    }
    auto glyph_offset = m_loca.get_glyph_offset(glyph_id);
    auto glyph = m_glyf.glyph(glyph_offset);
    return glyph.rasterize(m_hhea.ascender(), m_hhea.descender(), x_scale, y_scale, subpixel_offset, [&](u16 glyph_id) {
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

DeprecatedString Font::family() const
{
    auto string = m_name.typographic_family_name();
    if (!string.is_empty())
        return string;
    return m_name.family_name();
}

DeprecatedString Font::variant() const
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
    return glyph_metrics(glyph_id_for_code_point('.'), 1, 1).advance_width == glyph_metrics(glyph_id_for_code_point('X'), 1, 1).advance_width;
}

u16 OS2::weight_class() const
{
    return header().us_weight_class;
}

u16 OS2::selection() const
{
    return header().fs_selection;
}

i16 OS2::typographic_ascender() const
{
    return header().s_typo_ascender;
}

i16 OS2::typographic_descender() const
{
    return header().s_typo_descender;
}

i16 OS2::typographic_line_gap() const
{
    return header().s_typo_line_gap;
}

}
