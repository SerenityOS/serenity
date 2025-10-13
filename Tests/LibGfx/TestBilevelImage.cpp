/*
 * Copyright (c) 2025, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/ImageFormats/BilevelImage.h>
#include <LibTest/TestCase.h>

static ErrorOr<NonnullRefPtr<Gfx::BilevelImage>> create_bilevel()
{
    ByteBuffer buffer;
    buffer.append(0xCA);
    buffer.append(0xFE);

    return Gfx::BilevelImage::create_from_byte_buffer(buffer, 5, 2);
}

TEST_CASE(get_bit)
{
    auto bilevel = TRY_OR_FAIL(create_bilevel());

    EXPECT(bilevel->get_bit(0, 0));
    EXPECT(bilevel->get_bit(1, 0));
    EXPECT(!bilevel->get_bit(2, 0));
    EXPECT(!bilevel->get_bit(3, 0));
    EXPECT(bilevel->get_bit(4, 0));

    EXPECT(bilevel->get_bit(0, 1));
    EXPECT(bilevel->get_bit(1, 1));
    EXPECT(bilevel->get_bit(2, 1));
    EXPECT(bilevel->get_bit(3, 1));
    EXPECT(bilevel->get_bit(4, 1));
}
