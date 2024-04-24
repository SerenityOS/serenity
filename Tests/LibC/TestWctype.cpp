/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <wctype.h>

TEST_CASE(wctype)
{
    // Test that existing properties return non-zero wctypes.
    EXPECT_NE(wctype("alnum"), 0ul);
    EXPECT_NE(wctype("alpha"), 0ul);
    EXPECT_NE(wctype("blank"), 0ul);
    EXPECT_NE(wctype("cntrl"), 0ul);
    EXPECT_NE(wctype("digit"), 0ul);
    EXPECT_NE(wctype("graph"), 0ul);
    EXPECT_NE(wctype("lower"), 0ul);
    EXPECT_NE(wctype("print"), 0ul);
    EXPECT_NE(wctype("punct"), 0ul);
    EXPECT_NE(wctype("space"), 0ul);
    EXPECT_NE(wctype("upper"), 0ul);
    EXPECT_NE(wctype("xdigit"), 0ul);

    // Test that invalid properties return the "invalid" wctype (0).
    EXPECT_EQ(wctype(""), 0ul);
    EXPECT_EQ(wctype("abc"), 0ul);
}

TEST_CASE(wctrans)
{
    // Test that existing character mappings return non-zero wctrans values.
    EXPECT_NE(wctrans("tolower"), 0);
    EXPECT_NE(wctrans("toupper"), 0);

    // Test that invalid character mappings return the "invalid" wctrans value (0).
    EXPECT_EQ(wctrans(""), 0);
    EXPECT_EQ(wctrans("abc"), 0);
}

TEST_CASE(iswctype)
{
    wint_t const test_chars[] = { L'A', L'a', L'F', L'f', L'Z', L'z', L'0', L'\n', L'.', L'\x00' };

    // Test that valid properties are wired to the correct implementation.
    for (unsigned int i = 0; i < sizeof(test_chars) / sizeof(test_chars[0]); i++) {
        EXPECT_EQ_TRUTH(iswctype(test_chars[i], wctype("alnum")), iswalnum(test_chars[i]));
        EXPECT_EQ_TRUTH(iswctype(test_chars[i], wctype("alpha")), iswalpha(test_chars[i]));
        EXPECT_EQ_TRUTH(iswctype(test_chars[i], wctype("blank")), iswblank(test_chars[i]));
        EXPECT_EQ_TRUTH(iswctype(test_chars[i], wctype("cntrl")), iswcntrl(test_chars[i]));
        EXPECT_EQ_TRUTH(iswctype(test_chars[i], wctype("digit")), iswdigit(test_chars[i]));
        EXPECT_EQ_TRUTH(iswctype(test_chars[i], wctype("graph")), iswgraph(test_chars[i]));
        EXPECT_EQ_TRUTH(iswctype(test_chars[i], wctype("lower")), iswlower(test_chars[i]));
        EXPECT_EQ_TRUTH(iswctype(test_chars[i], wctype("print")), iswprint(test_chars[i]));
        EXPECT_EQ_TRUTH(iswctype(test_chars[i], wctype("punct")), iswpunct(test_chars[i]));
        EXPECT_EQ_TRUTH(iswctype(test_chars[i], wctype("space")), iswspace(test_chars[i]));
        EXPECT_EQ_TRUTH(iswctype(test_chars[i], wctype("upper")), iswupper(test_chars[i]));
        EXPECT_EQ_TRUTH(iswctype(test_chars[i], wctype("xdigit")), iswxdigit(test_chars[i]));
    }

    // Test that invalid properties always return zero.
    for (unsigned int i = 0; i < sizeof(test_chars) / sizeof(test_chars[0]); i++) {
        EXPECT_EQ(iswctype(test_chars[i], 0), 0);
        EXPECT_EQ(iswctype(test_chars[i], -1), 0);
    }
}

TEST_CASE(towctrans)
{
    wint_t const test_chars[] = { L'A', L'a', L'F', L'f', L'Z', L'z', L'0', L'\n', L'.', L'\x00' };

    // Test that valid mappings are wired to the correct implementation.
    for (unsigned int i = 0; i < sizeof(test_chars) / sizeof(test_chars[0]); i++) {
        EXPECT_EQ(towctrans(test_chars[i], wctrans("tolower")), towlower(test_chars[i]));
        EXPECT_EQ(towctrans(test_chars[i], wctrans("toupper")), towupper(test_chars[i]));
    }

    // Test that invalid mappings always return the character unchanged.
    for (unsigned int i = 0; i < sizeof(test_chars) / sizeof(test_chars[0]); i++) {
        EXPECT_EQ(towctrans(test_chars[i], 0), test_chars[i]);
        EXPECT_EQ(towctrans(test_chars[i], -1), test_chars[i]);
    }
}
