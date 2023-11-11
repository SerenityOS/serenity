/*
 * Copyright (c) 2022, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Font/OpenType/Cmap.h>
#include <LibTest/TestCase.h>

TEST_CASE(test_cmap_format_4)
{
    // clang-format off
    // Big endian.
    Array<u8, 52> const cmap_table =
    {
        // https://docs.microsoft.com/en-us/typography/opentype/spec/cmap#cmap-header
        0, 0,  // uint16 version
        0, 1,  // uint16 numTables

        // https://docs.microsoft.com/en-us/typography/opentype/spec/cmap#encoding-records-and-encodings
        0, 0,  // uint16 platformID, 0 means "Unicode"
        0, 3,  // uint16 encodingID, 3 means "BMP only" for platformID==0.
        0, 0, 0, 12,  // Offset32 to encoding subtable.

        // https://docs.microsoft.com/en-us/typography/opentype/spec/cmap#format-4-segment-mapping-to-delta-values
        0, 4,  // uint16 format = 4
        0, 42,  // uint16 length in bytes
        0, 0,  // uint16 language, must be 0
        0, 6,  // segCount * 2
        0, 4,  // searchRange
        0, 1,  // entrySelector
        0, 2,  // rangeShift

        // endCode array, last entry must be 0xffff.
        0, 128,
        1, 0,
        0xff, 0xff,

        0, 0,  // uint16 reservedPad

        // startCode array
        0, 16,
        1, 0,
        0xff, 0xff,

        // delta array
        0, 0,
        0, 10,
        0, 0,

        // glyphID array
        0, 0,
        0, 0,
        0, 0,
    };
    // clang-format on
    auto cmap = OpenType::Cmap::from_slice(cmap_table.span()).value();
    cmap.set_active_index(0);

    // Format 4 can't handle code points > 0xffff.

    // First range is 16..128.
    EXPECT_EQ(cmap.glyph_id_for_code_point(15), 0u);
    EXPECT_EQ(cmap.glyph_id_for_code_point(16), 16u);
    EXPECT_EQ(cmap.glyph_id_for_code_point(128), 128u);
    EXPECT_EQ(cmap.glyph_id_for_code_point(129), 0u);

    // Second range is 256..256, with delta 10.
    EXPECT_EQ(cmap.glyph_id_for_code_point(255), 0u);
    EXPECT_EQ(cmap.glyph_id_for_code_point(256), 266u);
    EXPECT_EQ(cmap.glyph_id_for_code_point(257), 0u);

    // Third range is 0xffff..0xffff.
    // From https://docs.microsoft.com/en-us/typography/opentype/spec/cmap#format-4-segment-mapping-to-delta-values:
    // "the final start code and endCode values must be 0xFFFF. This segment need not contain any valid mappings.
    // (It can just map the single character code 0xFFFF to missingGlyph). However, the segment must be present."
    // FIXME: Make OpenType::Cmap::from_slice() reject inputs where this isn't true.
    EXPECT_EQ(cmap.glyph_id_for_code_point(0xfeff), 0u);
    EXPECT_EQ(cmap.glyph_id_for_code_point(0xffff), 0xffffu);
    EXPECT_EQ(cmap.glyph_id_for_code_point(0x1'0000), 0u);

    // Set the number of subtables to a value, where the record offset for the last subtable is greater than the
    // total table size. We should not crash if a Cmap table is truncated in this way.
    auto malformed_cmap_table = cmap_table;
    malformed_cmap_table[3] = 13;
    auto cmap_with_invalid_subtable_offset = OpenType::Cmap::from_slice(malformed_cmap_table.span()).value();
    EXPECT(!cmap_with_invalid_subtable_offset.subtable(12).has_value());
}
