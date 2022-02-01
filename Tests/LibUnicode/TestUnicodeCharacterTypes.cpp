/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
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

TEST_CASE(to_unicode_lowercase_special_casing_i)
{
    // LATIN CAPITAL LETTER I
    auto result = Unicode::to_unicode_lowercase_full("I"sv, "en"sv);
    EXPECT_EQ(result, "i"sv);

    result = Unicode::to_unicode_lowercase_full("I"sv, "az"sv);
    EXPECT_EQ(result, "\u0131"sv);

    result = Unicode::to_unicode_lowercase_full("I"sv, "tr"sv);
    EXPECT_EQ(result, "\u0131"sv);

    // LATIN CAPITAL LETTER I WITH DOT ABOVE
    result = Unicode::to_unicode_lowercase_full("\u0130"sv, "en"sv);
    EXPECT_EQ(result, "\u0069\u0307"sv);

    result = Unicode::to_unicode_lowercase_full("\u0130"sv, "az"sv);
    EXPECT_EQ(result, "i"sv);

    result = Unicode::to_unicode_lowercase_full("\u0130"sv, "tr"sv);
    EXPECT_EQ(result, "i"sv);

    // LATIN CAPITAL LETTER I followed by COMBINING DOT ABOVE
    result = Unicode::to_unicode_lowercase_full("I\u0307"sv, "en"sv);
    EXPECT_EQ(result, "i\u0307"sv);

    result = Unicode::to_unicode_lowercase_full("I\u0307"sv, "az"sv);
    EXPECT_EQ(result, "i"sv);

    result = Unicode::to_unicode_lowercase_full("I\u0307"sv, "tr"sv);
    EXPECT_EQ(result, "i"sv);

    // LATIN CAPITAL LETTER I followed by combining class 0 and COMBINING DOT ABOVE
    result = Unicode::to_unicode_lowercase_full("IA\u0307"sv, "en"sv);
    EXPECT_EQ(result, "ia\u0307"sv);

    result = Unicode::to_unicode_lowercase_full("IA\u0307"sv, "az"sv);
    EXPECT_EQ(result, "\u0131a\u0307"sv);

    result = Unicode::to_unicode_lowercase_full("IA\u0307"sv, "tr"sv);
    EXPECT_EQ(result, "\u0131a\u0307"sv);
}

TEST_CASE(to_unicode_lowercase_special_casing_more_above)
{
    // LATIN CAPITAL LETTER I
    auto result = Unicode::to_unicode_lowercase_full("I"sv, "en"sv);
    EXPECT_EQ(result, "i"sv);

    result = Unicode::to_unicode_lowercase_full("I"sv, "lt"sv);
    EXPECT_EQ(result, "i"sv);

    // LATIN CAPITAL LETTER J
    result = Unicode::to_unicode_lowercase_full("J"sv, "en"sv);
    EXPECT_EQ(result, "j"sv);

    result = Unicode::to_unicode_lowercase_full("J"sv, "lt"sv);
    EXPECT_EQ(result, "j"sv);

    // LATIN CAPITAL LETTER I WITH OGONEK
    result = Unicode::to_unicode_lowercase_full("\u012e"sv, "en"sv);
    EXPECT_EQ(result, "\u012f"sv);

    result = Unicode::to_unicode_lowercase_full("\u012e"sv, "lt"sv);
    EXPECT_EQ(result, "\u012f"sv);

    // LATIN CAPITAL LETTER I followed by COMBINING GRAVE ACCENT
    result = Unicode::to_unicode_lowercase_full("I\u0300"sv, "en"sv);
    EXPECT_EQ(result, "i\u0300"sv);

    result = Unicode::to_unicode_lowercase_full("I\u0300"sv, "lt"sv);
    EXPECT_EQ(result, "i\u0307\u0300"sv);

    // LATIN CAPITAL LETTER J followed by COMBINING GRAVE ACCENT
    result = Unicode::to_unicode_lowercase_full("J\u0300"sv, "en"sv);
    EXPECT_EQ(result, "j\u0300"sv);

    result = Unicode::to_unicode_lowercase_full("J\u0300"sv, "lt"sv);
    EXPECT_EQ(result, "j\u0307\u0300"sv);

    // LATIN CAPITAL LETTER I WITH OGONEK followed by COMBINING GRAVE ACCENT
    result = Unicode::to_unicode_lowercase_full("\u012e\u0300"sv, "en"sv);
    EXPECT_EQ(result, "\u012f\u0300"sv);

    result = Unicode::to_unicode_lowercase_full("\u012e\u0300"sv, "lt"sv);
    EXPECT_EQ(result, "\u012f\u0307\u0300"sv);
}

TEST_CASE(to_unicode_lowercase_special_casing_not_before_dot)
{
    // LATIN CAPITAL LETTER I
    auto result = Unicode::to_unicode_lowercase_full("I"sv, "en"sv);
    EXPECT_EQ(result, "i"sv);

    result = Unicode::to_unicode_lowercase_full("I"sv, "az"sv);
    EXPECT_EQ(result, "\u0131"sv);

    result = Unicode::to_unicode_lowercase_full("I"sv, "tr"sv);
    EXPECT_EQ(result, "\u0131"sv);

    // LATIN CAPITAL LETTER I followed by COMBINING DOT ABOVE
    result = Unicode::to_unicode_lowercase_full("I\u0307"sv, "en"sv);
    EXPECT_EQ(result, "i\u0307"sv);

    result = Unicode::to_unicode_lowercase_full("I\u0307"sv, "az"sv);
    EXPECT_EQ(result, "i"sv);

    result = Unicode::to_unicode_lowercase_full("I\u0307"sv, "tr"sv);
    EXPECT_EQ(result, "i"sv);
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

TEST_CASE(to_unicode_uppercase_special_casing_soft_dotted)
{
    // LATIN SMALL LETTER I
    auto result = Unicode::to_unicode_uppercase_full("i"sv, "en"sv);
    EXPECT_EQ(result, "I"sv);

    result = Unicode::to_unicode_uppercase_full("i"sv, "lt"sv);
    EXPECT_EQ(result, "I"sv);

    // LATIN SMALL LETTER J
    result = Unicode::to_unicode_uppercase_full("j"sv, "en"sv);
    EXPECT_EQ(result, "J"sv);

    result = Unicode::to_unicode_uppercase_full("j"sv, "lt"sv);
    EXPECT_EQ(result, "J"sv);

    // LATIN SMALL LETTER I followed by COMBINING DOT ABOVE
    result = Unicode::to_unicode_uppercase_full("i\u0307"sv, "en"sv);
    EXPECT_EQ(result, "I\u0307"sv);

    result = Unicode::to_unicode_uppercase_full("i\u0307"sv, "lt"sv);
    EXPECT_EQ(result, "I"sv);

    // LATIN SMALL LETTER J followed by COMBINING DOT ABOVE
    result = Unicode::to_unicode_uppercase_full("j\u0307"sv, "en"sv);
    EXPECT_EQ(result, "J\u0307"sv);

    result = Unicode::to_unicode_uppercase_full("j\u0307"sv, "lt"sv);
    EXPECT_EQ(result, "J"sv);
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

TEST_CASE(script)
{
    auto script = [](StringView name) {
        auto script = Unicode::script_from_string(name);
        VERIFY(script.has_value());
        return *script;
    };

    auto script_latin = script("Latin"sv);
    auto script_latn = script("Latn"sv);
    EXPECT_EQ(script_latin, script_latn);

    auto script_cyrillic = script("Cyrillic"sv);
    auto script_cyrl = script("Cyrl"sv);
    EXPECT_EQ(script_cyrillic, script_cyrl);

    auto script_greek = script("Greek"sv);
    auto script_grek = script("Grek"sv);
    EXPECT_EQ(script_greek, script_grek);

    for (u32 code_point = 0x41; code_point <= 0x5a; ++code_point) {
        EXPECT(Unicode::code_point_has_script(code_point, script_latin));
        EXPECT(Unicode::code_point_has_script_extension(code_point, script_latin));

        EXPECT(!Unicode::code_point_has_script(code_point, script_cyrillic));
        EXPECT(!Unicode::code_point_has_script(code_point, script_greek));
    }

    for (u32 code_point = 0x61; code_point <= 0x7a; ++code_point) {
        EXPECT(Unicode::code_point_has_script(code_point, script_latin));
        EXPECT(Unicode::code_point_has_script_extension(code_point, script_latin));

        EXPECT(!Unicode::code_point_has_script(code_point, script_cyrillic));
        EXPECT(!Unicode::code_point_has_script(code_point, script_greek));
    }

    for (u32 code_point = 0x400; code_point <= 0x481; ++code_point) {
        EXPECT(Unicode::code_point_has_script(code_point, script_cyrillic));
        EXPECT(Unicode::code_point_has_script_extension(code_point, script_cyrillic));

        EXPECT(!Unicode::code_point_has_script(code_point, script_latin));
        EXPECT(!Unicode::code_point_has_script(code_point, script_greek));
    }

    for (u32 code_point = 0x400; code_point <= 0x481; ++code_point) {
        EXPECT(Unicode::code_point_has_script(code_point, script_cyrillic));
        EXPECT(Unicode::code_point_has_script_extension(code_point, script_cyrillic));

        EXPECT(!Unicode::code_point_has_script(code_point, script_latin));
        EXPECT(!Unicode::code_point_has_script(code_point, script_greek));
    }

    for (u32 code_point = 0x1f80; code_point <= 0x1fb4; ++code_point) {
        EXPECT(Unicode::code_point_has_script(code_point, script_greek));
        EXPECT(Unicode::code_point_has_script_extension(code_point, script_greek));

        EXPECT(!Unicode::code_point_has_script(code_point, script_latin));
        EXPECT(!Unicode::code_point_has_script(code_point, script_cyrillic));
    }
}

TEST_CASE(script_extension)
{
    auto script = [](StringView name) {
        auto script = Unicode::script_from_string(name);
        VERIFY(script.has_value());
        return *script;
    };

    auto script_latin = script("Latin"sv);
    auto script_greek = script("Greek"sv);

    for (u32 code_point = 0x363; code_point <= 0x36f; ++code_point) {
        EXPECT(!Unicode::code_point_has_script(code_point, script_latin));
        EXPECT(Unicode::code_point_has_script_extension(code_point, script_latin));
    }

    EXPECT(!Unicode::code_point_has_script(0x342, script_greek));
    EXPECT(Unicode::code_point_has_script_extension(0x342, script_greek));

    EXPECT(!Unicode::code_point_has_script(0x345, script_greek));
    EXPECT(Unicode::code_point_has_script_extension(0x345, script_greek));

    EXPECT(!Unicode::code_point_has_script(0x1dc0, script_greek));
    EXPECT(Unicode::code_point_has_script_extension(0x1dc0, script_greek));

    EXPECT(!Unicode::code_point_has_script(0x1dc1, script_greek));
    EXPECT(Unicode::code_point_has_script_extension(0x1dc1, script_greek));

    auto script_common = script("Common"sv);
    auto script_zyyy = script("Zyyy"sv);
    EXPECT_EQ(script_common, script_zyyy);

    EXPECT(Unicode::code_point_has_script(0x202f, script_common));
    EXPECT(!Unicode::code_point_has_script_extension(0x202f, script_common));

    EXPECT(Unicode::code_point_has_script(0x3000, script_common));
    EXPECT(Unicode::code_point_has_script_extension(0x3000, script_common));

    auto script_inherited = script("Inherited"sv);
    auto script_qaai = script("Qaai"sv);
    auto script_zinh = script("Zinh"sv);
    EXPECT_EQ(script_inherited, script_qaai);
    EXPECT_EQ(script_inherited, script_zinh);

    EXPECT(Unicode::code_point_has_script(0x1ced, script_inherited));
    EXPECT(!Unicode::code_point_has_script_extension(0x1ced, script_inherited));

    EXPECT(Unicode::code_point_has_script(0x101fd, script_inherited));
    EXPECT(Unicode::code_point_has_script_extension(0x101fd, script_inherited));
}

TEST_CASE(code_point_display_name)
{
    auto code_point_display_name = [](u32 code_point) {
        auto name = Unicode::code_point_display_name(code_point);
        VERIFY(name.has_value());
        return name.release_value();
    };

    // Control code points.
    EXPECT_EQ(code_point_display_name(0), "NULL"sv);
    EXPECT_EQ(code_point_display_name(1), "START OF HEADING"sv);
    EXPECT_EQ(code_point_display_name(0xa), "LINE FEED"sv);

    // Ideographic code points (which already appeared in a range in UnicodeData.txt).
    EXPECT_EQ(code_point_display_name(0x3400), "CJK UNIFIED IDEOGRAPH-3400"sv);
    EXPECT_EQ(code_point_display_name(0x3401), "CJK UNIFIED IDEOGRAPH-3401"sv);
    EXPECT_EQ(code_point_display_name(0x3402), "CJK UNIFIED IDEOGRAPH-3402"sv);
    EXPECT_EQ(code_point_display_name(0x4dbf), "CJK UNIFIED IDEOGRAPH-4DBF"sv);

    EXPECT_EQ(code_point_display_name(0x20000), "CJK UNIFIED IDEOGRAPH-20000"sv);
    EXPECT_EQ(code_point_display_name(0x20001), "CJK UNIFIED IDEOGRAPH-20001"sv);
    EXPECT_EQ(code_point_display_name(0x20002), "CJK UNIFIED IDEOGRAPH-20002"sv);
    EXPECT(!Unicode::code_point_display_name(0x2a6df).has_value());

    // Ideographic code points (which appeared individually in UnicodeData.txt and were coalesced into a range).
    EXPECT_EQ(code_point_display_name(0x2f800), "CJK COMPATIBILITY IDEOGRAPH-2F800"sv);
    EXPECT_EQ(code_point_display_name(0x2f801), "CJK COMPATIBILITY IDEOGRAPH-2F801"sv);
    EXPECT_EQ(code_point_display_name(0x2f802), "CJK COMPATIBILITY IDEOGRAPH-2F802"sv);
    EXPECT_EQ(code_point_display_name(0x2fa1d), "CJK COMPATIBILITY IDEOGRAPH-2FA1D"sv);
}
