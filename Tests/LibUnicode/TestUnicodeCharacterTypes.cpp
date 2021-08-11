/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/StringView.h>
#include <LibUnicode/CharacterTypes.h>
#include <ctype.h>

static void compare_to_ascii(auto& old_function, auto& new_function)
{
    i64 result1 = 0;
    i64 result2 = 0;

    for (u32 i = 0; i < 0x80; ++i) {
        EXPECT_EQ(result1 = old_function(i), result2 = new_function(i));
        if (result1 != result2)
            dbgln("Function input value was {}.", i);
    }
}

TEST_CASE(to_unicode_lowercase)
{
    compare_to_ascii(tolower, Unicode::to_unicode_lowercase);

    EXPECT_EQ(Unicode::to_unicode_lowercase(0x03c9u), 0x03c9u); // "ω" to "ω"
    EXPECT_EQ(Unicode::to_unicode_lowercase(0x03a9u), 0x03c9u); // "Ω" to "ω"

    // Code points encoded by ranges in UnicodeData.txt
    EXPECT_EQ(Unicode::to_unicode_lowercase(0x3400u), 0x3400u);
    EXPECT_EQ(Unicode::to_unicode_lowercase(0x3401u), 0x3401u);
    EXPECT_EQ(Unicode::to_unicode_lowercase(0x3402u), 0x3402u);
    EXPECT_EQ(Unicode::to_unicode_lowercase(0x4dbfu), 0x4dbfu);
}

TEST_CASE(to_unicode_uppercase)
{
    compare_to_ascii(toupper, Unicode::to_unicode_uppercase);

    EXPECT_EQ(Unicode::to_unicode_uppercase(0x03c9u), 0x03a9u); // "ω" to "Ω"
    EXPECT_EQ(Unicode::to_unicode_uppercase(0x03a9u), 0x03a9u); // "Ω" to "Ω"

    // Code points encoded by ranges in UnicodeData.txt
    EXPECT_EQ(Unicode::to_unicode_uppercase(0x3400u), 0x3400u);
    EXPECT_EQ(Unicode::to_unicode_uppercase(0x3401u), 0x3401u);
    EXPECT_EQ(Unicode::to_unicode_uppercase(0x3402u), 0x3402u);
    EXPECT_EQ(Unicode::to_unicode_uppercase(0x4dbfu), 0x4dbfu);
}

TEST_CASE(to_unicode_lowercase_unconditional_special_casing)
{
    // LATIN SMALL LETTER SHARP S
    auto result = Unicode::to_unicode_lowercase_full("\u00DF"sv);
    EXPECT_EQ(result, "\u00DF");

    // LATIN CAPITAL LETTER I WITH DOT ABOVE
    result = Unicode::to_unicode_lowercase_full("\u0130"sv);
    EXPECT_EQ(result, "\u0069\u0307");

    // LATIN SMALL LIGATURE FF
    result = Unicode::to_unicode_lowercase_full("\uFB00"sv);
    EXPECT_EQ(result, "\uFB00");

    // LATIN SMALL LIGATURE FI
    result = Unicode::to_unicode_lowercase_full("\uFB01"sv);
    EXPECT_EQ(result, "\uFB01");

    // LATIN SMALL LIGATURE FL
    result = Unicode::to_unicode_lowercase_full("\uFB02"sv);
    EXPECT_EQ(result, "\uFB02");

    // LATIN SMALL LIGATURE FFI
    result = Unicode::to_unicode_lowercase_full("\uFB03"sv);
    EXPECT_EQ(result, "\uFB03");

    // LATIN SMALL LIGATURE FFL
    result = Unicode::to_unicode_lowercase_full("\uFB04"sv);
    EXPECT_EQ(result, "\uFB04");

    // LATIN SMALL LIGATURE LONG S T
    result = Unicode::to_unicode_lowercase_full("\uFB05"sv);
    EXPECT_EQ(result, "\uFB05");

    // LATIN SMALL LIGATURE ST
    result = Unicode::to_unicode_lowercase_full("\uFB06"sv);
    EXPECT_EQ(result, "\uFB06");

    // GREEK SMALL LETTER ALPHA WITH PERISPOMENI AND YPOGEGRAMMENI
    result = Unicode::to_unicode_lowercase_full("\u1FB7"sv);
    EXPECT_EQ(result, "\u1FB7");

    // GREEK SMALL LETTER ETA WITH PERISPOMENI AND YPOGEGRAMMENI
    result = Unicode::to_unicode_lowercase_full("\u1FC7"sv);
    EXPECT_EQ(result, "\u1FC7");

    // GREEK SMALL LETTER OMEGA WITH PERISPOMENI AND YPOGEGRAMMENI
    result = Unicode::to_unicode_lowercase_full("\u1FF7"sv);
    EXPECT_EQ(result, "\u1FF7");
}

TEST_CASE(to_unicode_lowercase_special_casing_sigma)
{
    auto result = Unicode::to_unicode_lowercase_full("ABCI"sv);
    EXPECT_EQ(result, "abci");

    // Sigma preceded by A
    result = Unicode::to_unicode_lowercase_full("A\u03A3"sv);
    EXPECT_EQ(result, "a\u03C2");

    // Sigma preceded by FEMININE ORDINAL INDICATOR
    result = Unicode::to_unicode_lowercase_full("\u00AA\u03A3"sv);
    EXPECT_EQ(result, "\u00AA\u03C2");

    // Sigma preceded by ROMAN NUMERAL ONE
    result = Unicode::to_unicode_lowercase_full("\u2160\u03A3"sv);
    EXPECT_EQ(result, "\u2170\u03C2");

    // Sigma preceded by COMBINING GREEK YPOGEGRAMMENI
    result = Unicode::to_unicode_lowercase_full("\u0345\u03A3"sv);
    EXPECT_EQ(result, "\u0345\u03C3");

    // Sigma preceded by A and FULL STOP
    result = Unicode::to_unicode_lowercase_full("A.\u03A3"sv);
    EXPECT_EQ(result, "a.\u03C2");

    // Sigma preceded by A and MONGOLIAN VOWEL SEPARATOR
    result = Unicode::to_unicode_lowercase_full("A\u180E\u03A3"sv);
    EXPECT_EQ(result, "a\u180E\u03C2");

    // Sigma preceded by A and MONGOLIAN VOWEL SEPARATOR, followed by B
    result = Unicode::to_unicode_lowercase_full("A\u180E\u03A3B"sv);
    EXPECT_EQ(result, "a\u180E\u03C3b");

    // Sigma followed by A
    result = Unicode::to_unicode_lowercase_full("\u03A3A"sv);
    EXPECT_EQ(result, "\u03C3a");

    // Sigma preceded by A, followed by MONGOLIAN VOWEL SEPARATOR
    result = Unicode::to_unicode_lowercase_full("A\u03A3\u180E"sv);
    EXPECT_EQ(result, "a\u03C2\u180E");

    // Sigma preceded by A, followed by MONGOLIAN VOWEL SEPARATOR and B
    result = Unicode::to_unicode_lowercase_full("A\u03A3\u180EB"sv);
    EXPECT_EQ(result, "a\u03C3\u180Eb");

    // Sigma preceded by A and MONGOLIAN VOWEL SEPARATOR, followed by MONGOLIAN VOWEL SEPARATOR
    result = Unicode::to_unicode_lowercase_full("A\u180E\u03A3\u180E"sv);
    EXPECT_EQ(result, "a\u180E\u03C2\u180E");

    // Sigma preceded by A and MONGOLIAN VOWEL SEPARATOR, followed by MONGOLIAN VOWEL SEPARATOR and B
    result = Unicode::to_unicode_lowercase_full("A\u180E\u03A3\u180EB"sv);
    EXPECT_EQ(result, "a\u180E\u03C3\u180Eb");
}

TEST_CASE(to_unicode_uppercase_unconditional_special_casing)
{
    // LATIN SMALL LETTER SHARP S
    auto result = Unicode::to_unicode_uppercase_full("\u00DF"sv);
    EXPECT_EQ(result, "\u0053\u0053");

    // LATIN CAPITAL LETTER I WITH DOT ABOVE
    result = Unicode::to_unicode_uppercase_full("\u0130"sv);
    EXPECT_EQ(result, "\u0130");

    // LATIN SMALL LIGATURE FF
    result = Unicode::to_unicode_uppercase_full("\uFB00"sv);
    EXPECT_EQ(result, "\u0046\u0046");

    // LATIN SMALL LIGATURE FI
    result = Unicode::to_unicode_uppercase_full("\uFB01"sv);
    EXPECT_EQ(result, "\u0046\u0049");

    // LATIN SMALL LIGATURE FL
    result = Unicode::to_unicode_uppercase_full("\uFB02"sv);
    EXPECT_EQ(result, "\u0046\u004C");

    // LATIN SMALL LIGATURE FFI
    result = Unicode::to_unicode_uppercase_full("\uFB03"sv);
    EXPECT_EQ(result, "\u0046\u0046\u0049");

    // LATIN SMALL LIGATURE FFL
    result = Unicode::to_unicode_uppercase_full("\uFB04"sv);
    EXPECT_EQ(result, "\u0046\u0046\u004C");

    // LATIN SMALL LIGATURE LONG S T
    result = Unicode::to_unicode_uppercase_full("\uFB05"sv);
    EXPECT_EQ(result, "\u0053\u0054");

    // LATIN SMALL LIGATURE ST
    result = Unicode::to_unicode_uppercase_full("\uFB06"sv);
    EXPECT_EQ(result, "\u0053\u0054");

    // GREEK SMALL LETTER IOTA WITH DIALYTIKA AND TONOS
    result = Unicode::to_unicode_uppercase_full("\u0390"sv);
    EXPECT_EQ(result, "\u0399\u0308\u0301");

    // GREEK SMALL LETTER UPSILON WITH DIALYTIKA AND TONOS
    result = Unicode::to_unicode_uppercase_full("\u03B0"sv);
    EXPECT_EQ(result, "\u03A5\u0308\u0301");

    // GREEK SMALL LETTER ALPHA WITH PERISPOMENI AND YPOGEGRAMMENI
    result = Unicode::to_unicode_uppercase_full("\u1FB7"sv);
    EXPECT_EQ(result, "\u0391\u0342\u0399");

    // GREEK SMALL LETTER ETA WITH PERISPOMENI AND YPOGEGRAMMENI
    result = Unicode::to_unicode_uppercase_full("\u1FC7"sv);
    EXPECT_EQ(result, "\u0397\u0342\u0399");

    // GREEK SMALL LETTER OMEGA WITH PERISPOMENI AND YPOGEGRAMMENI
    result = Unicode::to_unicode_uppercase_full("\u1FF7"sv);
    EXPECT_EQ(result, "\u03A9\u0342\u0399");
}

TEST_CASE(general_category)
{
    auto general_category = [](StringView name) {
        auto general_category = Unicode::general_category_from_string(name);
        VERIFY(general_category.has_value());
        return *general_category;
    };

    auto general_category_c = general_category("C"sv);
    auto general_category_other = general_category("Other"sv);
    EXPECT_EQ(general_category_c, general_category_other);

    auto general_category_cc = general_category("Cc"sv);
    auto general_category_control = general_category("Control"sv);
    EXPECT_EQ(general_category_cc, general_category_control);

    auto general_category_co = general_category("Co"sv);
    auto general_category_private_use = general_category("Private_Use"sv);
    EXPECT_EQ(general_category_co, general_category_private_use);

    auto general_category_cn = general_category("Cn"sv);
    auto general_category_unassigned = general_category("Unassigned"sv);
    EXPECT_EQ(general_category_cn, general_category_unassigned);

    auto general_category_lc = general_category("LC"sv);
    auto general_category_cased_letter = general_category("Cased_Letter"sv);
    EXPECT_EQ(general_category_lc, general_category_cased_letter);

    auto general_category_ll = general_category("Ll"sv);
    auto general_category_lowercase_letter = general_category("Lowercase_Letter"sv);
    EXPECT_EQ(general_category_ll, general_category_lowercase_letter);

    auto general_category_lu = general_category("Lu"sv);
    auto general_category_uppercase_letter = general_category("Uppercase_Letter"sv);
    EXPECT_EQ(general_category_lu, general_category_uppercase_letter);

    for (u32 code_point = 0; code_point <= 0x1f; ++code_point) {
        EXPECT(Unicode::code_point_has_general_category(code_point, general_category_c));
        EXPECT(Unicode::code_point_has_general_category(code_point, general_category_cc));

        EXPECT(!Unicode::code_point_has_general_category(code_point, general_category_co));
        EXPECT(!Unicode::code_point_has_general_category(code_point, general_category_cn));
        EXPECT(!Unicode::code_point_has_general_category(code_point, general_category_lc));
        EXPECT(!Unicode::code_point_has_general_category(code_point, general_category_ll));
        EXPECT(!Unicode::code_point_has_general_category(code_point, general_category_lu));
    }

    for (u32 code_point = 0xe000; code_point <= 0xe100; ++code_point) {
        EXPECT(Unicode::code_point_has_general_category(code_point, general_category_c));
        EXPECT(Unicode::code_point_has_general_category(code_point, general_category_co));

        EXPECT(!Unicode::code_point_has_general_category(code_point, general_category_cc));
        EXPECT(!Unicode::code_point_has_general_category(code_point, general_category_cn));
        EXPECT(!Unicode::code_point_has_general_category(code_point, general_category_lc));
        EXPECT(!Unicode::code_point_has_general_category(code_point, general_category_ll));
        EXPECT(!Unicode::code_point_has_general_category(code_point, general_category_lu));
    }

    for (u32 code_point = 0x101fe; code_point <= 0x1027f; ++code_point) {
        EXPECT(Unicode::code_point_has_general_category(code_point, general_category_c));
        EXPECT(Unicode::code_point_has_general_category(code_point, general_category_cn));

        EXPECT(!Unicode::code_point_has_general_category(code_point, general_category_cc));
        EXPECT(!Unicode::code_point_has_general_category(code_point, general_category_co));
        EXPECT(!Unicode::code_point_has_general_category(code_point, general_category_lc));
        EXPECT(!Unicode::code_point_has_general_category(code_point, general_category_ll));
        EXPECT(!Unicode::code_point_has_general_category(code_point, general_category_lu));
    }

    for (u32 code_point = 0x61; code_point <= 0x7a; ++code_point) {
        EXPECT(Unicode::code_point_has_general_category(code_point, general_category_lc));
        EXPECT(Unicode::code_point_has_general_category(code_point, general_category_ll));

        EXPECT(!Unicode::code_point_has_general_category(code_point, general_category_c));
        EXPECT(!Unicode::code_point_has_general_category(code_point, general_category_cc));
        EXPECT(!Unicode::code_point_has_general_category(code_point, general_category_co));
        EXPECT(!Unicode::code_point_has_general_category(code_point, general_category_cn));
        EXPECT(!Unicode::code_point_has_general_category(code_point, general_category_lu));
    }

    for (u32 code_point = 0x41; code_point <= 0x5a; ++code_point) {
        EXPECT(Unicode::code_point_has_general_category(code_point, general_category_lc));
        EXPECT(Unicode::code_point_has_general_category(code_point, general_category_lu));

        EXPECT(!Unicode::code_point_has_general_category(code_point, general_category_c));
        EXPECT(!Unicode::code_point_has_general_category(code_point, general_category_cc));
        EXPECT(!Unicode::code_point_has_general_category(code_point, general_category_co));
        EXPECT(!Unicode::code_point_has_general_category(code_point, general_category_cn));
        EXPECT(!Unicode::code_point_has_general_category(code_point, general_category_ll));
    }
}

TEST_CASE(property)
{
    auto property = [](StringView name) {
        auto property = Unicode::property_from_string(name);
        VERIFY(property.has_value());
        return *property;
    };

    auto property_any = property("Any"sv);
    auto property_assigned = property("Assigned"sv);
    auto property_ascii = property("ASCII"sv);

    auto property_white_space = property("White_Space"sv);
    auto property_wspace = property("WSpace"sv);
    auto property_space = property("space"sv);
    EXPECT_EQ(property_white_space, property_wspace);
    EXPECT_EQ(property_white_space, property_space);

    auto property_emoji_presentation = property("Emoji_Presentation"sv);
    auto property_epres = property("EPres"sv);
    EXPECT_EQ(property_emoji_presentation, property_epres);

    for (u32 code_point = 0; code_point <= 0x10ffff; code_point += 1000)
        EXPECT(Unicode::code_point_has_property(code_point, property_any));

    for (u32 code_point = 0x101d0; code_point <= 0x101fd; ++code_point) {
        EXPECT(Unicode::code_point_has_property(code_point, property_any));
        EXPECT(Unicode::code_point_has_property(code_point, property_assigned));

        EXPECT(!Unicode::code_point_has_property(code_point, property_ascii));
        EXPECT(!Unicode::code_point_has_property(code_point, property_white_space));
        EXPECT(!Unicode::code_point_has_property(code_point, property_emoji_presentation));
    }

    for (u32 code_point = 0x101fe; code_point <= 0x1027f; ++code_point) {
        EXPECT(Unicode::code_point_has_property(code_point, property_any));

        EXPECT(!Unicode::code_point_has_property(code_point, property_assigned));
        EXPECT(!Unicode::code_point_has_property(code_point, property_ascii));
        EXPECT(!Unicode::code_point_has_property(code_point, property_white_space));
        EXPECT(!Unicode::code_point_has_property(code_point, property_emoji_presentation));
    }

    for (u32 code_point = 0; code_point <= 0x7f; ++code_point) {
        EXPECT(Unicode::code_point_has_property(code_point, property_any));
        EXPECT(Unicode::code_point_has_property(code_point, property_assigned));
        EXPECT(Unicode::code_point_has_property(code_point, property_ascii));

        EXPECT(!Unicode::code_point_has_property(code_point, property_emoji_presentation));
    }

    for (u32 code_point = 0x9; code_point <= 0xd; ++code_point) {
        EXPECT(Unicode::code_point_has_property(code_point, property_any));
        EXPECT(Unicode::code_point_has_property(code_point, property_assigned));
        EXPECT(Unicode::code_point_has_property(code_point, property_ascii));
        EXPECT(Unicode::code_point_has_property(code_point, property_white_space));

        EXPECT(!Unicode::code_point_has_property(code_point, property_emoji_presentation));
    }

    for (u32 code_point = 0x1f3e5; code_point <= 0x1f3f0; ++code_point) {
        EXPECT(Unicode::code_point_has_property(code_point, property_any));
        EXPECT(Unicode::code_point_has_property(code_point, property_assigned));
        EXPECT(Unicode::code_point_has_property(code_point, property_emoji_presentation));

        EXPECT(!Unicode::code_point_has_property(code_point, property_ascii));
        EXPECT(!Unicode::code_point_has_property(code_point, property_white_space));
    }
}
