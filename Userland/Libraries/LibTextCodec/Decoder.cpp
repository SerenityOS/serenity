/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <LibTextCodec/Decoder.h>

namespace TextCodec {

namespace {
Latin1Decoder& latin1_decoder()
{
    static Latin1Decoder* decoder;
    if (!decoder)
        decoder = new Latin1Decoder;
    return *decoder;
}

UTF8Decoder& utf8_decoder()
{
    static UTF8Decoder* decoder;
    if (!decoder)
        decoder = new UTF8Decoder;
    return *decoder;
}

UTF16BEDecoder& utf16be_decoder()
{
    static UTF16BEDecoder* decoder;
    if (!decoder)
        decoder = new UTF16BEDecoder;
    return *decoder;
}

Latin2Decoder& latin2_decoder()
{
    static Latin2Decoder* decoder = nullptr;
    if (!decoder)
        decoder = new Latin2Decoder;
    return *decoder;
}

}

Decoder* decoder_for(const String& a_encoding)
{
    auto encoding = get_standardized_encoding(a_encoding);
    if (encoding.equals_ignoring_case("windows-1252"))
        return &latin1_decoder();
    if (encoding.equals_ignoring_case("utf-8"))
        return &utf8_decoder();
    if (encoding.equals_ignoring_case("utf-16be"))
        return &utf16be_decoder();
    if (encoding.equals_ignoring_case("iso-8859-2"))
        return &latin2_decoder();
    dbgln("TextCodec: No decoder implemented for encoding '{}'", a_encoding);
    return nullptr;
}

// https://encoding.spec.whatwg.org/#concept-encoding-get
String get_standardized_encoding(const String& encoding)
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

bool is_standardized_encoding(const String& encoding)
{
    return encoding.equals_ignoring_case(get_standardized_encoding(encoding));
}

String UTF8Decoder::to_utf8(const StringView& input)
{
    return input;
}

String UTF16BEDecoder::to_utf8(const StringView& input)
{
    StringBuilder builder(input.length() / 2);
    for (size_t i = 0; i < input.length(); i += 2) {
        u16 code_point = (input[i] << 8) | input[i + 1];
        builder.append_code_point(code_point);
    }
    return builder.to_string();
}

String Latin1Decoder::to_utf8(const StringView& input)
{
    StringBuilder builder(input.length());
    for (size_t i = 0; i < input.length(); ++i) {
        u8 ch = input[i];
        // Latin1 is the same as the first 256 Unicode code_points, so no mapping is needed, just utf-8 encoding.
        builder.append_code_point(ch);
    }
    return builder.to_string();
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

String Latin2Decoder::to_utf8(const StringView& input)
{
    StringBuilder builder(input.length());
    for (auto c : input) {
        builder.append_code_point(convert_latin2_to_utf8(c));
    }

    return builder.to_string();
}

}
