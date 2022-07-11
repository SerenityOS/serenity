/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Vector.h>
#include <LibTest/TestCase.h>
#include <LibTextCodec/Decoder.h>

TEST_CASE(test_utf8_decode)
{
    auto decoder = TextCodec::UTF8Decoder();
    // Bytes for U+1F600 GRINNING FACE
    auto test_string = "\xf0\x9f\x98\x80"sv;

    Vector<u32> processed_code_points;
    decoder.process(test_string, [&](u32 code_point) {
        processed_code_points.append(code_point);
    });
    EXPECT(processed_code_points.size() == 1);
    EXPECT(processed_code_points[0] == 0x1F600);

    EXPECT(decoder.to_utf8(test_string) == test_string);
}
