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

TEST_CASE(wctrans)
{
    // Test that existing character mappings return non-zero wctrans values.
    EXPECT(wctrans("tolower") != 0);
    EXPECT(wctrans("toupper") != 0);

    // Test that invalid character mappings return the "invalid" wctrans value (0).
    EXPECT(wctrans("") == 0);
    EXPECT(wctrans("abc") == 0);
}

TEST_CASE(iswctype)
{
    const wint_t test_chars[] = { L'A', L'a', L'F', L'f', L'Z', L'z', L'0', L'\n', L'.', L'\x00' };

    // Test that valid properties are wired to the correct implementation.
    for (unsigned int i = 0; i < sizeof(test_chars) / sizeof(test_chars[0]); i++) {
        EXPECT(iswctype(test_chars[i], wctype("alnum")) == iswalnum(test_chars[i]));
        EXPECT(iswctype(test_chars[i], wctype("alpha")) == iswalpha(test_chars[i]));
        EXPECT(iswctype(test_chars[i], wctype("blank")) == iswblank(test_chars[i]));
        EXPECT(iswctype(test_chars[i], wctype("cntrl")) == iswcntrl(test_chars[i]));
        EXPECT(iswctype(test_chars[i], wctype("digit")) == iswdigit(test_chars[i]));
        EXPECT(iswctype(test_chars[i], wctype("graph")) == iswgraph(test_chars[i]));
        EXPECT(iswctype(test_chars[i], wctype("lower")) == iswlower(test_chars[i]));
        EXPECT(iswctype(test_chars[i], wctype("print")) == iswprint(test_chars[i]));
        EXPECT(iswctype(test_chars[i], wctype("punct")) == iswpunct(test_chars[i]));
        EXPECT(iswctype(test_chars[i], wctype("space")) == iswspace(test_chars[i]));
        EXPECT(iswctype(test_chars[i], wctype("upper")) == iswupper(test_chars[i]));
        EXPECT(iswctype(test_chars[i], wctype("xdigit")) == iswxdigit(test_chars[i]));
    }

    // Test that invalid properties always return zero.
    for (unsigned int i = 0; i < sizeof(test_chars) / sizeof(test_chars[0]); i++) {
        EXPECT(iswctype(test_chars[i], 0) == 0);
        EXPECT(iswctype(test_chars[i], -1) == 0);
    }
}

TEST_CASE(towctrans)
{
    const wint_t test_chars[] = { L'A', L'a', L'F', L'f', L'Z', L'z', L'0', L'\n', L'.', L'\x00' };

    // Test that valid mappings are wired to the correct implementation.
    for (unsigned int i = 0; i < sizeof(test_chars) / sizeof(test_chars[0]); i++) {
        EXPECT(towctrans(test_chars[i], wctrans("tolower")) == towlower(test_chars[i]));
        EXPECT(towctrans(test_chars[i], wctrans("toupper")) == towupper(test_chars[i]));
    }

    // Test that invalid mappings always return the character unchanged.
    for (unsigned int i = 0; i < sizeof(test_chars) / sizeof(test_chars[0]); i++) {
        EXPECT(towctrans(test_chars[i], 0) == test_chars[i]);
        EXPECT(towctrans(test_chars[i], -1) == test_chars[i]);
    }
}
