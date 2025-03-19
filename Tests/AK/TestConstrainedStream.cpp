/*
 * Copyright (c) 2025, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/Array.h>
#include <AK/ConstrainedStream.h>
#include <AK/MemoryStream.h>

static Array<u8, 4> base_data { 1, 2, 3, 4 };

TEST_CASE(basic_constraint)
{
    auto memory_stream = make<FixedMemoryStream>(base_data.span());
    ConstrainedStream constrained_stream { move(memory_stream), 2 };
    Array<u8, 2> read;
    TRY_OR_FAIL(constrained_stream.read_until_filled(read));
    EXPECT_EQ(read.span(), base_data.span().trim(2));

    EXPECT(constrained_stream.read_until_filled(read).is_error());
}

TEST_CASE(discard_until_constraint)
{
    auto memory_stream = make<FixedMemoryStream>(base_data.span());
    ConstrainedStream constrained_stream { move(memory_stream), 3 };
    Array<u8, 2> read;
    TRY_OR_FAIL(constrained_stream.read_until_filled(read));
    EXPECT_EQ(read.span(), base_data.span().trim(2));

    EXPECT(!constrained_stream.discard(1).is_error());
    EXPECT(constrained_stream.discard(1).is_error());
}
