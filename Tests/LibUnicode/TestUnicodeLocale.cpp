/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <LibUnicode/Locale.h>

TEST_CASE(is_unicode_language_subtag)
{
    EXPECT(Unicode::is_unicode_language_subtag("aa"sv));
    EXPECT(Unicode::is_unicode_language_subtag("aaa"sv));
    EXPECT(Unicode::is_unicode_language_subtag("aaaaa"sv));
    EXPECT(Unicode::is_unicode_language_subtag("aaaaaa"sv));
    EXPECT(Unicode::is_unicode_language_subtag("aaaaaaa"sv));
    EXPECT(Unicode::is_unicode_language_subtag("aaaaaaaa"sv));

    EXPECT(!Unicode::is_unicode_language_subtag(""sv));
    EXPECT(!Unicode::is_unicode_language_subtag("a"sv));
    EXPECT(!Unicode::is_unicode_language_subtag("aaaa"sv));
    EXPECT(!Unicode::is_unicode_language_subtag("aaaaaaaaa"sv));
    EXPECT(!Unicode::is_unicode_language_subtag("123"sv));
}

TEST_CASE(is_unicode_script_subtag)
{
    EXPECT(Unicode::is_unicode_script_subtag("aaaa"sv));

    EXPECT(!Unicode::is_unicode_script_subtag(""sv));
    EXPECT(!Unicode::is_unicode_script_subtag("a"sv));
    EXPECT(!Unicode::is_unicode_script_subtag("aa"sv));
    EXPECT(!Unicode::is_unicode_script_subtag("aaa"sv));
    EXPECT(!Unicode::is_unicode_script_subtag("aaaaa"sv));
    EXPECT(!Unicode::is_unicode_script_subtag("1234"sv));
}

TEST_CASE(is_unicode_region_subtag)
{
    EXPECT(Unicode::is_unicode_region_subtag("aa"sv));
    EXPECT(Unicode::is_unicode_region_subtag("123"sv));

    EXPECT(!Unicode::is_unicode_region_subtag(""sv));
    EXPECT(!Unicode::is_unicode_region_subtag("a"sv));
    EXPECT(!Unicode::is_unicode_region_subtag("aaa"sv));
    EXPECT(!Unicode::is_unicode_region_subtag("12"sv));
    EXPECT(!Unicode::is_unicode_region_subtag("12a"sv));
}

TEST_CASE(is_unicode_variant_subtag)
{
    EXPECT(Unicode::is_unicode_variant_subtag("aaaaa"sv));
    EXPECT(Unicode::is_unicode_variant_subtag("aaaaaa"sv));
    EXPECT(Unicode::is_unicode_variant_subtag("aaaaaaa"sv));
    EXPECT(Unicode::is_unicode_variant_subtag("aaaaaaaa"sv));

    EXPECT(Unicode::is_unicode_variant_subtag("1aaa"sv));
    EXPECT(Unicode::is_unicode_variant_subtag("12aa"sv));
    EXPECT(Unicode::is_unicode_variant_subtag("123a"sv));
    EXPECT(Unicode::is_unicode_variant_subtag("1234"sv));

    EXPECT(!Unicode::is_unicode_variant_subtag(""sv));
    EXPECT(!Unicode::is_unicode_variant_subtag("a"sv));
    EXPECT(!Unicode::is_unicode_variant_subtag("aa"sv));
    EXPECT(!Unicode::is_unicode_variant_subtag("aaa"sv));
    EXPECT(!Unicode::is_unicode_variant_subtag("aaaa"sv));
    EXPECT(!Unicode::is_unicode_variant_subtag("aaaaaaaaa"sv));
    EXPECT(!Unicode::is_unicode_variant_subtag("a234"sv));
}

TEST_CASE(parse_unicode_locale_id)
{
    auto fail = [](StringView locale) {
        auto locale_id = Unicode::parse_unicode_locale_id(locale);
        EXPECT(!locale_id.has_value());
    };
    auto pass = [](StringView locale, Optional<StringView> expected_language, Optional<StringView> expected_script, Optional<StringView> expected_region, Vector<StringView> expected_variants) {
        auto locale_id = Unicode::parse_unicode_locale_id(locale);
        VERIFY(locale_id.has_value());

        EXPECT_EQ(locale_id->language_id.language, expected_language);
        EXPECT_EQ(locale_id->language_id.script, expected_script);
        EXPECT_EQ(locale_id->language_id.region, expected_region);
        EXPECT_EQ(locale_id->language_id.variants, expected_variants);
    };

    fail("a"sv);
    fail("1234"sv);
    fail("aaa-"sv);
    fail("aaa-cc-"sv);
    fail("aaa-bbbb-cc-"sv);
    fail("aaa-bbbb-cc-123"sv);

    pass("aaa"sv, "aaa"sv, {}, {}, {});
    pass("aaa-bbbb"sv, "aaa"sv, "bbbb"sv, {}, {});
    pass("aaa-cc"sv, "aaa"sv, {}, "cc"sv, {});
    pass("aaa-bbbb-cc"sv, "aaa"sv, "bbbb"sv, "cc"sv, {});
    pass("aaa-bbbb-cc-1234"sv, "aaa"sv, "bbbb"sv, "cc"sv, { "1234"sv });
    pass("aaa-bbbb-cc-1234-5678"sv, "aaa"sv, "bbbb"sv, "cc"sv, { "1234"sv, "5678"sv });
}

TEST_CASE(canonicalize_unicode_locale_id)
{
    auto test = [](StringView locale, StringView expected_canonical_locale) {
        auto locale_id = Unicode::parse_unicode_locale_id(locale);
        VERIFY(locale_id.has_value());

        auto canonical_locale = Unicode::canonicalize_unicode_locale_id(*locale_id);
        EXPECT_EQ(canonical_locale, expected_canonical_locale);
    };

    test("aaa"sv, "aaa"sv);
    test("AaA"sv, "aaa"sv);
    test("aaa-bbbb"sv, "aaa-Bbbb"sv);
    test("aaa-cc"sv, "aaa-CC"sv);
    test("aaa-bBBB-cC"sv, "aaa-Bbbb-CC"sv);
    test("aaa-bbbb-cc-1234"sv, "aaa-Bbbb-CC-1234"sv);
    test("aaa-bbbb-cc-ABCDE"sv, "aaa-Bbbb-CC-abcde"sv);
}
