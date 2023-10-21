/*
 * Copyright (c) 2020, Srimanta Barua <srimanta.barua1@gmail.com>
 * Copyright (c) 2021-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 * Copyright (c) 2023, Lukas Affolter <git@lukasach.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/BinarySearch.h>
#include <AK/BuiltinWrappers.h>
#include <AK/Debug.h>
#include <AK/MemoryStream.h>
#include <LibGfx/Font/OpenType/Tables.h>
#include <LibTextCodec/Decoder.h>

namespace OpenType {

static u32 be_u32(u8 const* ptr)
{
    return (((u32)ptr[0]) << 24) | (((u32)ptr[1]) << 16) | (((u32)ptr[2]) << 8) | ((u32)ptr[3]);
}

static u32 tag_from_str(char const* str)
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

    auto const* left_side_bearings = bit_cast<BigEndian<u16> const*>(m_slice.offset(m_number_of_h_metrics * sizeof(LongHorMetric)));
    return GlyphHorizontalMetrics {
        .advance_width = static_cast<u16>(long_hor_metrics[m_number_of_h_metrics - 1].advance_width),
        .left_side_bearing = static_cast<i16>(left_side_bearings[glyph_id - m_number_of_h_metrics]),
    };
}

Optional<Name> Name::from_slice(ReadonlyBytes slice)
{
    return Name(slice);
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
        return decoder.to_utf8(StringView { (char const*)m_slice.offset(storage_offset + offset), length }).release_value_but_fixme_should_propagate_errors();
    }

    return String::from_utf8(m_slice.slice(storage_offset + offset, length)).release_value_but_fixme_should_propagate_errors();
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

ErrorOr<CBDT> CBDT::from_slice(ReadonlyBytes slice)
{
    if (slice.size() < sizeof(CbdtHeader))
        return Error::from_string_literal("CBDT table too small");
    return CBDT { slice };
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
