/*
 * Copyright (c) 2021, the SerenityOS developers
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <wctype.h>

TEST_CASE(wctype)
{
    // Test that existing properties return non-zero wctypes.
    EXPECT(wctype("alnum") != 0);
    EXPECT(wctype("alpha") != 0);
    EXPECT(wctype("blank") != 0);
    EXPECT(wctype("cntrl") != 0);
    EXPECT(wctype("digit") != 0);
    EXPECT(wctype("graph") != 0);
    EXPECT(wctype("lower") != 0);
    EXPECT(wctype("print") != 0);
    EXPECT(wctype("punct") != 0);
    EXPECT(wctype("space") != 0);
    EXPECT(wctype("upper") != 0);
    EXPECT(wctype("xdigit") != 0);

    // Test that invalid properties return the "invalid" wctype (0).
    EXPECT(wctype("") == 0);
    EXPECT(wctype("abc") == 0);
}
