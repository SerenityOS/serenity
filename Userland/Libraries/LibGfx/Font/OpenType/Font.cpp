/*
 * Copyright (c) 2020, Srimanta Barua <srimanta.barua1@gmail.com>
 * Copyright (c) 2021-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 * Copyright (c) 2023, Lukas Affolter <git@lukasach.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/BinarySearch.h>
#include <AK/Checked.h>
#include <AK/Debug.h>
#include <AK/MemoryStream.h>
#include <AK/Try.h>
#include <LibCore/MappedFile.h>
#include <LibGfx/Font/OpenType/Cmap.h>
#include <LibGfx/Font/OpenType/Font.h>
#include <LibGfx/Font/OpenType/Glyf.h>
#include <LibGfx/Font/OpenType/Tables.h>
#include <LibGfx/ImageFormats/PNGLoader.h>
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
    auto subtable_offsets = TRY(FixedArray<size_t>::create(number_of_subtables));
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
        auto length = subtable_header.length;
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
    ReadonlySpan<Format0Pair> pairs { bit_cast<Format0Pair const*>(slice.slice(sizeof(Format0)).data()), number_of_pairs };

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

String Name::string_for_id(NameId id) const
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
        return String {};

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
        static auto& decoder = *TextCodec::decoder_for("utf-16be"sv);
        return decoder.to_utf8(StringView { (char const*)m_slice.offset_pointer(storage_offset + offset), length }).release_value_but_fixme_should_propagate_errors();
    }

    return String::from_utf8(m_slice.slice(storage_offset + offset, length)).release_value_but_fixme_should_propagate_errors();
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

u16 OS2::weight_class() const
{
    return header().us_weight_class;
}

u16 OS2::width_class() const
{
    return header().us_width_class;
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

bool OS2::use_typographic_metrics() const
{
    return header().fs_selection & 0x80;
}

Optional<i16> OS2::x_height() const
{
    if (header().version < 2)
        return {};
    return header_v2().sx_height;
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

ErrorOr<CBLC> CBLC::from_slice(ReadonlyBytes slice)
{
    if (slice.size() < sizeof(CblcHeader))
        return Error::from_string_literal("CBLC table too small");
    auto const& header = *bit_cast<CblcHeader const*>(slice.data());

    size_t num_sizes = header.num_sizes;
    Checked<size_t> size_used_by_bitmap_sizes = num_sizes;
    size_used_by_bitmap_sizes *= sizeof(BitmapSize);
    if (size_used_by_bitmap_sizes.has_overflow())
        return Error::from_string_literal("Integer overflow in CBLC table");

    Checked<size_t> total_size = sizeof(CblcHeader);
    total_size += size_used_by_bitmap_sizes;
    if (total_size.has_overflow())
        return Error::from_string_literal("Integer overflow in CBLC table");

    if (slice.size() < total_size)
        return Error::from_string_literal("CBLC table too small");

    return CBLC { slice };
}

Optional<CBLC::BitmapSize const&> CBLC::bitmap_size_for_glyph_id(u32 glyph_id) const
{
    for (auto const& bitmap_size : this->bitmap_sizes()) {
        if (glyph_id >= bitmap_size.start_glyph_index && glyph_id <= bitmap_size.end_glyph_index) {
            return bitmap_size;
        }
    }
    return {};
}

ErrorOr<CBDT> CBDT::from_slice(ReadonlyBytes slice)
{
    if (slice.size() < sizeof(CbdtHeader))
        return Error::from_string_literal("CBDT table too small");
    return CBDT { slice };
}

bool Font::has_color_bitmaps() const
{
    return m_cblc.has_value() && m_cbdt.has_value();
}

Optional<EBLC::IndexSubHeader const&> CBLC::index_subtable_for_glyph_id(u32 glyph_id, u16& first_glyph_index, u16& last_glyph_index) const
{
    auto maybe_bitmap_size = bitmap_size_for_glyph_id(glyph_id);
    if (!maybe_bitmap_size.has_value()) {
        return {};
    }
    auto const& bitmap_size = maybe_bitmap_size.value();

    Checked<size_t> required_size = static_cast<u32>(bitmap_size.index_subtable_array_offset);
    required_size += bitmap_size.index_tables_size;

    if (m_slice.size() < required_size) {
        dbgln("CBLC index subtable array goes out of bounds");
        return {};
    }

    auto index_subtables_slice = m_slice.slice(bitmap_size.index_subtable_array_offset, bitmap_size.index_tables_size);
    ReadonlySpan<EBLC::IndexSubTableArray> index_subtable_arrays {
        bit_cast<EBLC::IndexSubTableArray const*>(index_subtables_slice.data()), bitmap_size.number_of_index_subtables
    };

    EBLC::IndexSubTableArray const* index_subtable_array = nullptr;
    for (auto const& array : index_subtable_arrays) {
        if (glyph_id >= array.first_glyph_index && glyph_id <= array.last_glyph_index)
            index_subtable_array = &array;
    }
    if (!index_subtable_array) {
        return {};
    }

    auto index_subtable_slice = m_slice.slice(bitmap_size.index_subtable_array_offset + index_subtable_array->additional_offset_to_index_subtable);
    first_glyph_index = index_subtable_array->first_glyph_index;
    last_glyph_index = index_subtable_array->last_glyph_index;
    return *bit_cast<EBLC::IndexSubHeader const*>(index_subtable_slice.data());
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

Optional<i16> GPOS::glyph_kerning(u16 left_glyph_id, u16 right_glyph_id) const
{
    auto read_value_record = [&](u16 value_format, FixedMemoryStream& stream) -> ValueRecord {
        ValueRecord value_record;
        if (value_format & static_cast<i16>(ValueFormat::X_PLACEMENT))
            value_record.x_placement = stream.read_value<BigEndian<i16>>().release_value_but_fixme_should_propagate_errors();
        if (value_format & static_cast<i16>(ValueFormat::Y_PLACEMENT))
            value_record.y_placement = stream.read_value<BigEndian<i16>>().release_value_but_fixme_should_propagate_errors();
        if (value_format & static_cast<i16>(ValueFormat::X_ADVANCE))
            value_record.x_advance = stream.read_value<BigEndian<i16>>().release_value_but_fixme_should_propagate_errors();
        if (value_format & static_cast<i16>(ValueFormat::Y_ADVANCE))
            value_record.y_advance = stream.read_value<BigEndian<i16>>().release_value_but_fixme_should_propagate_errors();
        if (value_format & static_cast<i16>(ValueFormat::X_PLACEMENT_DEVICE))
            value_record.x_placement_device_offset = stream.read_value<Offset16>().release_value_but_fixme_should_propagate_errors();
        if (value_format & static_cast<i16>(ValueFormat::Y_PLACEMENT_DEVICE))
            value_record.y_placement_device_offset = stream.read_value<Offset16>().release_value_but_fixme_should_propagate_errors();
        if (value_format & static_cast<i16>(ValueFormat::X_ADVANCE_DEVICE))
            value_record.x_advance_device_offset = stream.read_value<Offset16>().release_value_but_fixme_should_propagate_errors();
        if (value_format & static_cast<i16>(ValueFormat::Y_ADVANCE_DEVICE))
            value_record.y_advance_device_offset = stream.read_value<Offset16>().release_value_but_fixme_should_propagate_errors();
        return value_record;
    };

    auto const& header = this->header();
    dbgln_if(OPENTYPE_GPOS_DEBUG, "GPOS header:");
    dbgln_if(OPENTYPE_GPOS_DEBUG, "   Version: {}.{}", header.major_version, header.minor_version);
    dbgln_if(OPENTYPE_GPOS_DEBUG, "   Feature list offset: {}", header.feature_list_offset);

    // FIXME: Make sure everything is bounds-checked appropriately.

    auto feature_list_slice = m_slice.slice(header.feature_list_offset);
    if (feature_list_slice.size() < sizeof(FeatureList)) {
        dbgln_if(OPENTYPE_GPOS_DEBUG, "GPOS table feature list slice is too small");
        return {};
    }
    auto const& feature_list = *bit_cast<FeatureList const*>(feature_list_slice.data());

    auto lookup_list_slice = m_slice.slice(header.lookup_list_offset);
    if (lookup_list_slice.size() < sizeof(LookupList)) {
        dbgln_if(OPENTYPE_GPOS_DEBUG, "GPOS table lookup list slice is too small");
        return {};
    }
    auto const& lookup_list = *bit_cast<LookupList const*>(lookup_list_slice.data());

    Optional<Offset16> kern_feature_offset;
    for (size_t i = 0; i < feature_list.feature_count; ++i) {
        auto const& feature_record = feature_list.feature_records[i];
        if (feature_record.feature_tag == tag_from_str("kern")) {
            kern_feature_offset = feature_record.feature_offset;
            break;
        }
    }

    if (!kern_feature_offset.has_value()) {
        dbgln_if(OPENTYPE_GPOS_DEBUG, "No 'kern' feature found in GPOS table");
        return {};
    }

    auto feature_slice = feature_list_slice.slice(kern_feature_offset.value());
    auto const& feature = *bit_cast<Feature const*>(feature_slice.data());

    dbgln_if(OPENTYPE_GPOS_DEBUG, "Feature:");
    dbgln_if(OPENTYPE_GPOS_DEBUG, "   featureParamsOffset: {}", feature.feature_params_offset);
    dbgln_if(OPENTYPE_GPOS_DEBUG, "   lookupIndexCount: {}", feature.lookup_index_count);

    for (size_t i = 0; i < feature.lookup_index_count; ++i) {
        auto lookup_index = feature.lookup_list_indices[i];
        dbgln_if(OPENTYPE_GPOS_DEBUG, "Lookup index: {}", lookup_index);
        auto lookup_slice = lookup_list_slice.slice(lookup_list.lookup_offsets[lookup_index]);
        auto const& lookup = *bit_cast<Lookup const*>(lookup_slice.data());

        dbgln_if(OPENTYPE_GPOS_DEBUG, "Lookup:");
        dbgln_if(OPENTYPE_GPOS_DEBUG, "  lookupType: {}", lookup.lookup_type);
        dbgln_if(OPENTYPE_GPOS_DEBUG, "  lookupFlag: {}", lookup.lookup_flag);
        dbgln_if(OPENTYPE_GPOS_DEBUG, "  subtableCount: {}", lookup.subtable_count);

        // NOTE: We only support lookup type 2 (Pair adjustment) at the moment.
        if (lookup.lookup_type != 2) {
            dbgln_if(OPENTYPE_GPOS_DEBUG, "FIXME: Implement GPOS lookup type {}", lookup.lookup_type);
            continue;
        }

        for (size_t j = 0; j < lookup.subtable_count; ++j) {
            auto pair_pos_format_offset = lookup.subtable_offsets[j];
            auto pair_pos_format_slice = lookup_slice.slice(pair_pos_format_offset);

            auto const& pair_pos_format = *bit_cast<BigEndian<u16> const*>(pair_pos_format_slice.data());

            dbgln_if(OPENTYPE_GPOS_DEBUG, "PairPosFormat{}", pair_pos_format);

            if (pair_pos_format == 1) {
                auto const& pair_pos_format1 = *bit_cast<GPOS::PairPosFormat1 const*>(pair_pos_format_slice.data());

                dbgln_if(OPENTYPE_GPOS_DEBUG, "   posFormat: {}", pair_pos_format1.pos_format);
                dbgln_if(OPENTYPE_GPOS_DEBUG, "   valueFormat1: {}", pair_pos_format1.value_format1);
                dbgln_if(OPENTYPE_GPOS_DEBUG, "   valueFormat2: {}", pair_pos_format1.value_format2);
                dbgln_if(OPENTYPE_GPOS_DEBUG, "   pairSetCount: {}", pair_pos_format1.pair_set_count);

                auto get_coverage_index = [&](u16 glyph_id, Offset16 coverage_format_offset) -> Optional<u16> {
                    auto coverage_format_slice = pair_pos_format_slice.slice(coverage_format_offset);
                    auto const& coverage_format = *bit_cast<BigEndian<u16> const*>(coverage_format_slice.data());

                    dbgln_if(OPENTYPE_GPOS_DEBUG, "Coverage table format: {}", coverage_format);

                    if (coverage_format == 1) {
                        auto const& coverage_format1 = *bit_cast<CoverageFormat1 const*>(coverage_format_slice.data());

                        for (size_t k = 0; k < coverage_format1.glyph_count; ++k)
                            if (coverage_format1.glyph_array[k] == glyph_id)
                                return k;

                        dbgln_if(OPENTYPE_GPOS_DEBUG, "Glyph ID {} not covered", glyph_id);
                        return {};
                    }

                    else if (coverage_format == 2) {
                        auto const& coverage_format2 = *bit_cast<CoverageFormat2 const*>(coverage_format_slice.data());

                        for (size_t k = 0; k < coverage_format2.range_count; ++k) {
                            auto range_record = coverage_format2.range_records[k];
                            if ((range_record.start_glyph_id <= glyph_id) && (glyph_id <= range_record.end_glyph_id))
                                return range_record.start_coverage_index + glyph_id - range_record.start_glyph_id;
                        }
                        dbgln_if(OPENTYPE_GPOS_DEBUG, "Glyph ID {} not covered", glyph_id);
                        return {};
                    }

                    dbgln_if(OPENTYPE_GPOS_DEBUG, "No valid coverage table for format {}", coverage_format);
                    return {};
                };

                auto coverage_index = get_coverage_index(left_glyph_id, pair_pos_format1.coverage_offset);

                if (!coverage_index.has_value()) {
                    dbgln_if(OPENTYPE_GPOS_DEBUG, "Glyph ID not covered by table");
                    continue;
                }

                size_t value1_size = popcount(static_cast<u32>(pair_pos_format1.value_format1 & 0xff)) * sizeof(u16);
                size_t value2_size = popcount(static_cast<u32>(pair_pos_format1.value_format2 & 0xff)) * sizeof(u16);
                dbgln_if(OPENTYPE_GPOS_DEBUG, "ValueSizes: {}, {}", value1_size, value2_size);

                // Manually iterate over the PairSet table, as the size of each PairValueRecord is not known at compile time.
                auto pair_set_offset = pair_pos_format1.pair_set_offsets[coverage_index.value()];
                auto pair_set_slice = pair_pos_format_slice.slice(pair_set_offset);

                FixedMemoryStream stream(pair_set_slice);

                auto pair_value_count = stream.read_value<BigEndian<u16>>().release_value_but_fixme_should_propagate_errors();

                bool found_matching_glyph = false;
                for (size_t k = 0; k < pair_value_count; ++k) {
                    auto second_glyph = stream.read_value<BigEndian<u16>>().release_value_but_fixme_should_propagate_errors();

                    if (right_glyph_id == second_glyph) {
                        dbgln_if(OPENTYPE_GPOS_DEBUG, "Found matching second glyph {}", second_glyph);
                        found_matching_glyph = true;
                        break;
                    }

                    (void)stream.discard(value1_size + value2_size).release_value_but_fixme_should_propagate_errors();
                }

                if (!found_matching_glyph) {
                    dbgln_if(OPENTYPE_GPOS_DEBUG, "Did not find second glyph matching {}", right_glyph_id);
                    continue;
                }

                [[maybe_unused]] auto value_record1 = read_value_record(pair_pos_format1.value_format1, stream);
                [[maybe_unused]] auto value_record2 = read_value_record(pair_pos_format1.value_format2, stream);

                dbgln_if(OPENTYPE_GPOS_DEBUG, "Returning x advance {}", value_record1.x_advance);
                return value_record1.x_advance;
            }

            else if (pair_pos_format == 2) {
                auto const& pair_pos_format2 = *bit_cast<GPOS::PairPosFormat2 const*>(pair_pos_format_slice.data());

                dbgln_if(OPENTYPE_GPOS_DEBUG, "   posFormat: {}", pair_pos_format2.pos_format);
                dbgln_if(OPENTYPE_GPOS_DEBUG, "   valueFormat1: {}", pair_pos_format2.value_format1);
                dbgln_if(OPENTYPE_GPOS_DEBUG, "   valueFormat2: {}", pair_pos_format2.value_format2);
                dbgln_if(OPENTYPE_GPOS_DEBUG, "   class1Count: {}", pair_pos_format2.class1_count);
                dbgln_if(OPENTYPE_GPOS_DEBUG, "   class2Count: {}", pair_pos_format2.class2_count);

                auto get_class = [&](u16 glyph_id, Offset16 glyph_def_offset) -> Optional<u16> {
                    auto class_def_format_slice = pair_pos_format_slice.slice(glyph_def_offset);

                    auto const& class_def_format = *bit_cast<BigEndian<u16> const*>(class_def_format_slice.data());
                    if (class_def_format == 1) {
                        dbgln_if(OPENTYPE_GPOS_DEBUG, "FIXME: Implement ClassDefFormat1");
                        return {};
                    }

                    auto const& class_def_format2 = *bit_cast<ClassDefFormat2 const*>(class_def_format_slice.data());
                    dbgln_if(OPENTYPE_GPOS_DEBUG, "ClassDefFormat2:");
                    dbgln_if(OPENTYPE_GPOS_DEBUG, "  classFormat: {}", class_def_format2.class_format);
                    dbgln_if(OPENTYPE_GPOS_DEBUG, "  classRangeCount: {}", class_def_format2.class_range_count);

                    for (size_t i = 0; i < class_def_format2.class_range_count; ++i) {
                        auto const& range = class_def_format2.class_range_records[i];
                        if (glyph_id >= range.start_glyph_id && glyph_id <= range.end_glyph_id) {
                            dbgln_if(OPENTYPE_GPOS_DEBUG, "Found class {} for glyph ID {}", range.class_, glyph_id);
                            return range.class_;
                        }
                    }

                    dbgln_if(OPENTYPE_GPOS_DEBUG, "No class found for glyph {}", glyph_id);
                    return {};
                };

                auto left_class = get_class(left_glyph_id, pair_pos_format2.class_def1_offset);
                auto right_class = get_class(right_glyph_id, pair_pos_format2.class_def2_offset);

                if (!left_class.has_value() || !right_class.has_value()) {
                    dbgln_if(OPENTYPE_GPOS_DEBUG, "Need glyph class for both sides");
                    continue;
                }

                dbgln_if(OPENTYPE_GPOS_DEBUG, "Classes: {}, {}", left_class.value(), right_class.value());

                size_t value1_size = popcount(static_cast<u32>(pair_pos_format2.value_format1 & 0xff)) * sizeof(u16);
                size_t value2_size = popcount(static_cast<u32>(pair_pos_format2.value_format2 & 0xff)) * sizeof(u16);
                dbgln_if(OPENTYPE_GPOS_DEBUG, "ValueSizes: {}, {}", value1_size, value2_size);
                size_t class2_record_size = value1_size + value2_size;
                dbgln_if(OPENTYPE_GPOS_DEBUG, "Class2RecordSize: {}", class2_record_size);
                size_t class1_record_size = pair_pos_format2.class2_count * class2_record_size;
                dbgln_if(OPENTYPE_GPOS_DEBUG, "Class1RecordSize: {}", class1_record_size);
                size_t item_offset = (left_class.value() * class1_record_size) + (right_class.value() * class2_record_size);
                dbgln_if(OPENTYPE_GPOS_DEBUG, "Item offset: {}", item_offset);

                auto item_slice = pair_pos_format_slice.slice(sizeof(PairPosFormat2) + item_offset);
                FixedMemoryStream stream(item_slice);

                [[maybe_unused]] auto value_record1 = read_value_record(pair_pos_format2.value_format1, stream);
                [[maybe_unused]] auto value_record2 = read_value_record(pair_pos_format2.value_format2, stream);

                dbgln_if(OPENTYPE_GPOS_DEBUG, "Returning x advance {}", value_record1.x_advance);
                return value_record1.x_advance;
            }
        }
    }

    (void)left_glyph_id;
    (void)right_glyph_id;
    return {};
}

}
