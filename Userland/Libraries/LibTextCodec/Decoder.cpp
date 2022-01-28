/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <LibTextCodec/Decoder.h>

namespace TextCodec {

namespace {
Latin1Decoder s_latin1_decoder;
UTF8Decoder s_utf8_decoder;
UTF16BEDecoder s_utf16be_decoder;
Latin2Decoder s_latin2_decoder;
HebrewDecoder s_hebrew_decoder;
CyrillicDecoder s_cyrillic_decoder;
Koi8RDecoder s_koi8r_decoder;
Latin9Decoder s_latin9_decoder;
TurkishDecoder s_turkish_decoder;
}

Decoder* decoder_for(const String& a_encoding)
{
    auto encoding = get_standardized_encoding(a_encoding);
    if (encoding.has_value()) {
        if (encoding.value().equals_ignoring_case("windows-1252"))
            return &s_latin1_decoder;
        if (encoding.value().equals_ignoring_case("utf-8"))
            return &s_utf8_decoder;
        if (encoding.value().equals_ignoring_case("utf-16be"))
            return &s_utf16be_decoder;
        if (encoding.value().equals_ignoring_case("iso-8859-2"))
            return &s_latin2_decoder;
        if (encoding.value().equals_ignoring_case("windows-1255"))
            return &s_hebrew_decoder;
        if (encoding.value().equals_ignoring_case("windows-1251"))
            return &s_cyrillic_decoder;
        if (encoding.value().equals_ignoring_case("koi8-r"))
            return &s_koi8r_decoder;
        if (encoding.value().equals_ignoring_case("iso-8859-15"))
            return &s_latin9_decoder;
        if (encoding.value().equals_ignoring_case("windows-1254"))
            return &s_turkish_decoder;
    }
    dbgln("TextCodec: No decoder implemented for encoding '{}'", a_encoding);
    return nullptr;
}

// https://encoding.spec.whatwg.org/#concept-encoding-get
Optional<String> get_standardized_encoding(const String& encoding)
{
    String trimmed_lowercase_encoding = encoding.trim_whitespace().to_lowercase();

    if (trimmed_lowercase_encoding.is_one_of("unicode-1-1-utf-8", "unicode11utf8", "unicode20utf8", "utf-8", "utf8", "x-unicode20utf8"))
        return "UTF-8";
    if (trimmed_lowercase_encoding.is_one_of("866", "cp866", "csibm866", "ibm866"))
        return "IBM866";
    if (trimmed_lowercase_encoding.is_one_of("csisolatin2", "iso-8859-2", "iso-ir-101", "iso8859-2", "iso88592", "iso_8859-2", "iso_8859-2:1987", "l2", "latin2"))
        return "ISO-8859-2";
    if (trimmed_lowercase_encoding.is_one_of("csisolatin3", "iso-8859-3", "iso-ir-109", "iso8859-3", "iso88593", "iso_8859-3", "iso_8859-3:1988", "l3", "latin3"))
        return "ISO-8859-3";
    if (trimmed_lowercase_encoding.is_one_of("csisolatin4", "iso-8859-4", "iso-ir-110", "iso8859-4", "iso88594", "iso_8859-4", "iso_8859-4:1989", "l4", "latin4"))
        return "ISO-8859-4";
    if (trimmed_lowercase_encoding.is_one_of("csisolatincyrillic", "cyrillic", "iso-8859-5", "iso-ir-144", "iso8859-5", "iso88595", "iso_8859-5", "iso_8859-5:1988"))
        return "ISO-8859-5";
    if (trimmed_lowercase_encoding.is_one_of("arabic", "asmo-708", "csiso88596e", "csiso88596i", "csisolatinarabic", "ecma-114", "iso-8859-6", "iso-8859-6-e", "iso-8859-6-i", "iso-ir-127", "iso8859-6", "iso88596", "iso_8859-6", "iso_8859-6:1987"))
        return "ISO-8859-6";
    if (trimmed_lowercase_encoding.is_one_of("csisolatingreek", "ecma-118", "elot_928", "greek", "greek8", "iso-8859-7", "iso-ir-126", "iso8859-7", "iso88597", "iso_8859-7", "iso_8859-7:1987", "sun_eu_greek"))
        return "ISO-8859-7";
    if (trimmed_lowercase_encoding.is_one_of("csiso88598e", "csisolatinhebrew", "hebrew", "iso-8859-8", "iso-8859-8-e", "iso-ir-138", "iso8859-8", "iso88598", "iso_8859-8", "iso_8859-8:1988", "visual"))
        return "ISO-8859-8";
    if (trimmed_lowercase_encoding.is_one_of("csiso88598i", "iso-8859-8-i", "logical"))
        return "ISO-8859-8-I";
    if (trimmed_lowercase_encoding.is_one_of("csisolatin6", "iso8859-10", "iso-ir-157", "iso8859-10", "iso885910", "l6", "latin6"))
        return "ISO-8859-10";
    if (trimmed_lowercase_encoding.is_one_of("iso-8859-13", "iso8859-13", "iso885913"))
        return "ISO-8859-13";
    if (trimmed_lowercase_encoding.is_one_of("iso-8859-14", "iso8859-14", "iso885914"))
        return "ISO-8859-14";
    if (trimmed_lowercase_encoding.is_one_of("csisolatin9", "iso-8859-15", "iso8859-15", "iso885915", "iso_8859-15", "l9"))
        return "ISO-8859-15";
    if (trimmed_lowercase_encoding == "iso-8859-16")
        return "ISO-8859-16";
    if (trimmed_lowercase_encoding.is_one_of("cskoi8r", "koi", "koi8", "koi8-r", "koi8_r"))
        return "KOI8-R";
    if (trimmed_lowercase_encoding.is_one_of("koi8-ru", "koi8-u"))
        return "KOI8-U";
    if (trimmed_lowercase_encoding.is_one_of("csmacintosh", "mac", "macintosh", "x-mac-roman"))
        return "macintosh";
    if (trimmed_lowercase_encoding.is_one_of("dos-874", "iso-8859-11", "iso8859-11", "iso885911", "tis-620", "windows-874"))
        return "windows-874";
    if (trimmed_lowercase_encoding.is_one_of("cp1250", "windows-1250", "x-cp1250"))
        return "windows-1250";
    if (trimmed_lowercase_encoding.is_one_of("cp1251", "windows-1251", "x-cp1251"))
        return "windows-1251";
    if (trimmed_lowercase_encoding.is_one_of("ansi_x3.4-1968", "ascii", "cp1252", "cp819", "csisolatin1", "ibm819", "iso-8859-1", "iso-ir-100", "iso8859-1", "iso88591", "iso_8859-1", "iso_8859-1:1987", "l1", "latin1", "us-ascii", "windows-1252", "x-cp1252"))
        return "windows-1252";
    if (trimmed_lowercase_encoding.is_one_of("cp1253", "windows-1253", "x-cp1253"))
        return "windows-1253";
    if (trimmed_lowercase_encoding.is_one_of("cp1254", "csisolatin5", "iso-8859-9", "iso-ir-148", "iso-8859-9", "iso-88599", "iso_8859-9", "iso_8859-9:1989", "l5", "latin5", "windows-1254", "x-cp1254"))
        return "windows-1254";
    if (trimmed_lowercase_encoding.is_one_of("cp1255", "windows-1255", "x-cp1255"))
        return "windows-1255";
    if (trimmed_lowercase_encoding.is_one_of("cp1256", "windows-1256", "x-cp1256"))
        return "windows-1256";
    if (trimmed_lowercase_encoding.is_one_of("cp1257", "windows-1257", "x-cp1257"))
        return "windows-1257";
    if (trimmed_lowercase_encoding.is_one_of("cp1258", "windows-1258", "x-cp1258"))
        return "windows-1258";
    if (trimmed_lowercase_encoding.is_one_of("x-mac-cyrillic", "x-mac-ukrainian"))
        return "x-mac-cyrillic";
    if (trimmed_lowercase_encoding.is_one_of("koi8-r", "koi8r"))
        return "koi8-r";
    if (trimmed_lowercase_encoding.is_one_of("chinese", "csgb2312", "csiso58gb231280", "gb2312", "gb_2312", "gb_2312-80", "gbk", "iso-ir-58", "x-gbk"))
        return "GBK";
    if (trimmed_lowercase_encoding == "gb18030")
        return "gb18030";
    if (trimmed_lowercase_encoding.is_one_of("big5", "big5-hkscs", "cn-big5", "csbig5", "x-x-big5"))
        return "Big5";
    if (trimmed_lowercase_encoding.is_one_of("cseucpkdfmtjapanese", "euc-jp", "x-euc-jp"))
        return "EUC-JP";
    if (trimmed_lowercase_encoding.is_one_of("csiso2022jp", "iso-2022-jp"))
        return "ISO-2022-JP";
    if (trimmed_lowercase_encoding.is_one_of("csshiftjis", "ms932", "ms_kanji", "shift-jis", "shift_jis", "sjis", "windows-31j", "x-sjis"))
        return "Shift_JIS";
    if (trimmed_lowercase_encoding.is_one_of("cseuckr", "csksc56011987", "euc-kr", "iso-ir-149", "korean", "ks_c_5601-1987", "ks_c_5601-1989", "ksc5601", "ksc_5601", "windows-949"))
        return "EUC-KR";
    if (trimmed_lowercase_encoding.is_one_of("csiso2022kr", "hz-gb-2312", "iso-2022-cn", "iso-2022-cn-ext", "iso-2022-kr", "replacement"))
        return "replacement";
    if (trimmed_lowercase_encoding.is_one_of("unicodefffe", "utf-16be"))
        return "UTF-16BE";
    if (trimmed_lowercase_encoding.is_one_of("csunicode", "iso-10646-ucs-2", "ucs-2", "unicode", "unicodefeff", "utf-16", "utf-16le"))
        return "UTF-16LE";
    if (trimmed_lowercase_encoding == "x-user-defined")
        return "x-user-defined";

    dbgln("TextCodec: Unrecognized encoding: {}", encoding);
    return {};
}

String Decoder::to_utf8(StringView input)
{
    StringBuilder builder(input.length());
    process(input, [&builder](u32 c) { builder.append_code_point(c); });
    return builder.to_string();
}

void UTF8Decoder::process(StringView input, Function<void(u32)> on_code_point)
{
    for (auto c : input) {
        on_code_point(c);
    }
}

String UTF8Decoder::to_utf8(StringView input)
{
    // Discard the BOM
    auto bomless_input = input;
    if (auto bytes = input.bytes(); bytes.size() >= 3 && bytes[0] == 0xEF && bytes[1] == 0xBB && bytes[2] == 0xBF) {
        bomless_input = input.substring_view(3);
    }

    return bomless_input;
}

void UTF16BEDecoder::process(StringView input, Function<void(u32)> on_code_point)
{
    size_t utf16_length = input.length() - (input.length() % 2);
    for (size_t i = 0; i < utf16_length; i += 2) {
        u16 code_point = (input[i] << 8) | input[i + 1];
        on_code_point(code_point);
    }
}

String UTF16BEDecoder::to_utf8(StringView input)
{
    // Discard the BOM
    auto bomless_input = input;
    if (auto bytes = input.bytes(); bytes.size() >= 2 && bytes[0] == 0xFE && bytes[1] == 0xFF) {
        bomless_input = input.substring_view(2);
    }

    StringBuilder builder(bomless_input.length() / 2);
    process(bomless_input, [&builder](u32 c) { builder.append_code_point(c); });
    return builder.to_string();
}

void Latin1Decoder::process(StringView input, Function<void(u32)> on_code_point)
{
    for (size_t i = 0; i < input.length(); ++i) {
        u8 ch = input[i];
        // Latin1 is the same as the first 256 Unicode code_points, so no mapping is needed, just utf-8 encoding.
        on_code_point(ch);
    }
}

namespace {
u32 convert_latin2_to_utf8(u8 in)
{
    switch (in) {

#define MAP(X, Y) \
    case X:       \
        return Y

        MAP(0xA1, 0x104);
        MAP(0xA2, 0x2D8);
        MAP(0xA3, 0x141);
        MAP(0xA5, 0x13D);
        MAP(0xA6, 0x15A);
        MAP(0xA9, 0x160);
        MAP(0xAA, 0x15E);
        MAP(0xAB, 0x164);
        MAP(0xAC, 0x179);
        MAP(0xAE, 0x17D);
        MAP(0xAF, 0x17B);

        MAP(0xB1, 0x105);
        MAP(0xB2, 0x2DB);
        MAP(0xB3, 0x142);
        MAP(0xB5, 0x13E);
        MAP(0xB6, 0x15B);
        MAP(0xB7, 0x2C7);
        MAP(0xB9, 0x161);
        MAP(0xBA, 0x15F);
        MAP(0xBB, 0x165);
        MAP(0xBC, 0x17A);
        MAP(0xBD, 0x2DD);
        MAP(0xBE, 0x17E);
        MAP(0xBF, 0x17C);

        MAP(0xC0, 0x154);
        MAP(0xC3, 0x102);
        MAP(0xC5, 0x139);
        MAP(0xC6, 0x106);
        MAP(0xC8, 0x10C);
        MAP(0xCA, 0x118);
        MAP(0xCC, 0x11A);
        MAP(0xCF, 0x10E);

        MAP(0xD0, 0x110);
        MAP(0xD1, 0x143);
        MAP(0xD2, 0x147);
        MAP(0xD5, 0x150);
        MAP(0xD8, 0x158);
        MAP(0xD9, 0x16E);
        MAP(0xDB, 0x170);
        MAP(0xDE, 0x162);

        MAP(0xE0, 0x155);
        MAP(0xE3, 0x103);
        MAP(0xE5, 0x13A);
        MAP(0xE6, 0x107);
        MAP(0xE8, 0x10D);
        MAP(0xEA, 0x119);
        MAP(0xEC, 0x11B);
        MAP(0xEF, 0x10F);

        MAP(0xF0, 0x111);
        MAP(0xF1, 0x144);
        MAP(0xF2, 0x148);
        MAP(0xF5, 0x151);
        MAP(0xF8, 0x159);
        MAP(0xF9, 0x16F);
        MAP(0xFB, 0x171);
        MAP(0xFE, 0x163);
        MAP(0xFF, 0x2D9);
#undef MAP

    default:
        return in;
    }
}
}

void Latin2Decoder::process(StringView input, Function<void(u32)> on_code_point)
{
    for (auto c : input) {
        on_code_point(convert_latin2_to_utf8(c));
    }
}

void HebrewDecoder::process(StringView input, Function<void(u32)> on_code_point)
{
    static constexpr Array<u32, 128> translation_table = {
        0x20AC, 0xFFFD, 0x201A, 0x192, 0x201E, 0x2026, 0x2020, 0x2021, 0x2C6, 0x2030, 0xFFFD, 0x2039, 0xFFFD, 0xFFFD, 0xFFFD, 0xFFFD,
        0xFFFD, 0x2018, 0x2019, 0x201C, 0x201D, 0x2022, 0x2013, 0x2014, 0x2DC, 0x2122, 0xFFFD, 0x203A, 0xFFFD, 0xFFFD, 0xFFFD, 0xFFFD,
        0xA0, 0xA1, 0xA2, 0xA3, 0x20AA, 0xA5, 0xA6, 0xA7, 0xA8, 0xA9, 0xD7, 0xAB, 0xAC, 0xAD, 0xAE, 0xAF,
        0xB0, 0xB1, 0xB2, 0xB3, 0xB4, 0xB5, 0xB6, 0xB7, 0xB8, 0xB9, 0xF7, 0xBB, 0xBC, 0xBD, 0xBE, 0xBF,
        0x5B0, 0x5B1, 0x5B2, 0x5B3, 0x5B4, 0x5B5, 0x5B6, 0x5B7, 0x5B8, 0x5B9, 0x5BA, 0x5BB, 0x5BC, 0x5BD, 0x5BE, 0x5BF,
        0x5C0, 0x5C1, 0x5C2, 0x5C3, 0x5F0, 0x5F1, 0x5F2, 0x5F3, 0x5F4, 0xFFFD, 0xFFFD, 0xFFFD, 0xFFFD, 0xFFFD, 0xFFFD, 0xFFFD,
        0x5D0, 0x5D1, 0x5D2, 0x5D3, 0x5D4, 0x5D5, 0x5D6, 0x5D7, 0x5D8, 0x5D9, 0x5DA, 0x5DB, 0x5DC, 0x5DD, 0x5DE, 0x5DF,
        0x5E0, 0x5E1, 0x5E2, 0x5E3, 0x5E4, 0x5E5, 0x5E6, 0x5E7, 0x5E8, 0x5E9, 0x5EA, 0xFFFD, 0xFFFD, 0x200E, 0x200F, 0xFFFD
    };
    for (unsigned char ch : input) {
        if (ch < 0x80) { // Superset of ASCII
            on_code_point(ch);
        } else {
            on_code_point(translation_table[ch - 0x80]);
        }
    }
}

void CyrillicDecoder::process(StringView input, Function<void(u32)> on_code_point)
{
    static constexpr Array<u32, 128> translation_table = {
        0x402, 0x403, 0x201A, 0x453, 0x201E, 0x2026, 0x2020, 0x2021, 0x20AC, 0x2030, 0x409, 0x2039, 0x40A, 0x40C, 0x40B, 0x40F,
        0x452, 0x2018, 0x2019, 0x201C, 0x201D, 0x2022, 0x2013, 0x2014, 0xFFFD, 0x2122, 0x459, 0x203A, 0x45A, 0x45C, 0x45B, 0x45F,
        0xA0, 0x40E, 0x45E, 0x408, 0xA4, 0x490, 0xA6, 0xA7, 0x401, 0xA9, 0x404, 0xAB, 0xAC, 0xAD, 0xAE, 0x407,
        0xB0, 0xB1, 0x406, 0x456, 0x491, 0xB5, 0xB6, 0xB7, 0x451, 0x2116, 0x454, 0xBB, 0x458, 0x405, 0x455, 0x457,
        0x410, 0x411, 0x412, 0x413, 0x414, 0x415, 0x416, 0x417, 0x418, 0x419, 0x41A, 0x41B, 0x41C, 0x41D, 0x41E, 0x41F,
        0x420, 0x421, 0x422, 0x423, 0x424, 0x425, 0x426, 0x427, 0x428, 0x429, 0x42A, 0x42B, 0x42C, 0x42D, 0x42E, 0x42F,
        0x430, 0x431, 0x432, 0x433, 0x434, 0x435, 0x436, 0x437, 0x438, 0x439, 0x43A, 0x43B, 0x43C, 0x43D, 0x43E, 0x43F,
        0x440, 0x441, 0x442, 0x443, 0x444, 0x445, 0x446, 0x447, 0x448, 0x449, 0x44A, 0x44B, 0x44C, 0x44D, 0x44E, 0x44F
    };
    for (unsigned char ch : input) {
        if (ch < 0x80) { // Superset of ASCII
            on_code_point(ch);
        } else {
            on_code_point(translation_table[ch - 0x80]);
        }
    }
}

void Koi8RDecoder::process(StringView input, Function<void(u32)> on_code_point)
{
    // clang-format off
    static constexpr Array<u32, 128> translation_table = {
        0x2500,0x2502,0x250c,0x2510,0x2514,0x2518,0x251c,0x2524,0x252c,0x2534,0x253c,0x2580,0x2584,0x2588,0x258c,0x2590,
        0x2591,0x2592,0x2593,0x2320,0x25a0,0x2219,0x221a,0x2248,0x2264,0x2265,0xA0,0x2321,0xb0,0xb2,0xb7,0xf7,
        0x2550,0x2551,0x2552,0xd191,0x2553,0x2554,0x2555,0x2556,0x2557,0x2558,0x2559,0x255a,0x255b,0x255c,0x255d,0x255e,
        0x255f,0x2560,0x2561,0xd081,0x2562,0x2563,0x2564,0x2565,0x2566,0x2567,0x2568,0x2569,0x256a,0x256b,0x256c,0xa9,
        0x44e,0x430,0x431,0x446,0x434,0x435,0x444,0x433,0x445,0x438,0x439,0x43a,0x43b,0x43c,0x43d,0x43e,
        0x43f,0x44f,0x440,0x441,0x442,0x443,0x436,0x432,0x44c,0x44b,0x437,0x448,0x44d,0x449,0x447,0x44a,
        0x42e,0x410,0x441,0x426,0x414,0x415,0x424,0x413,0x425,0x418,0x419,0x41a,0x41b,0x41c,0x41d,0x41e,
        0x41f,0x42f,0x420,0x421,0x422,0x423,0x416,0x412,0x42c,0x42b,0x417,0x428,0x42d,0x429,0x427,0x42a,
    };
    // clang-format on

    for (unsigned char ch : input) {
        if (ch < 0x80) { // Superset of ASCII
            on_code_point(ch);
        } else {
            on_code_point(translation_table[ch - 0x80]);
        }
    }
}

void Latin9Decoder::process(StringView input, Function<void(u32)> on_code_point)
{
    auto convert_latin9_to_utf8 = [](u8 ch) -> u32 {
        // Latin9 is the same as the first 256 Unicode code points, except for 8 characters.
        switch (ch) {
        case 0xA4:
            return 0x20AC;
        case 0xA6:
            return 0x160;
        case 0xA8:
            return 0x161;
        case 0xB4:
            return 0x17D;
        case 0xB8:
            return 0x17E;
        case 0xBC:
            return 0x152;
        case 0xBD:
            return 0x153;
        case 0xBE:
            return 0x178;
        default:
            return ch;
        }
    };

    for (auto ch : input) {
        on_code_point(convert_latin9_to_utf8(ch));
    }
}

void TurkishDecoder::process(StringView input, Function<void(u32)> on_code_point)
{
    auto convert_turkish_to_utf8 = [](u8 ch) -> u32 {
        // Turkish (aka ISO-8859-9, Windows-1254) is the same as the first 256 Unicode code points, except for 6 characters.
        switch (ch) {
        case 0xD0:
            return 0x11E;
        case 0xDD:
            return 0x130;
        case 0xDE:
            return 0x15E;
        case 0xF0:
            return 0x11F;
        case 0xFD:
            return 0x131;
        case 0xFE:
            return 0x15F;
        default:
            return ch;
        }
    };

    for (auto ch : input) {
        on_code_point(convert_turkish_to_utf8(ch));
    }
}

}
