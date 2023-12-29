/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <AK/Utf16View.h>
#include <AK/Utf8View.h>
#include <LibTextCodec/Decoder.h>

namespace TextCodec {

static constexpr u32 replacement_code_point = 0xfffd;

namespace {
Latin1Decoder s_latin1_decoder;
UTF8Decoder s_utf8_decoder;
UTF16BEDecoder s_utf16be_decoder;
UTF16LEDecoder s_utf16le_decoder;
Latin2Decoder s_latin2_decoder;
HebrewDecoder s_hebrew_decoder;
CyrillicDecoder s_cyrillic_decoder;
Koi8RDecoder s_koi8r_decoder;
Latin9Decoder s_latin9_decoder;
MacRomanDecoder s_mac_roman_decoder;
PDFDocEncodingDecoder s_pdf_doc_encoding_decoder;
TurkishDecoder s_turkish_decoder;
XUserDefinedDecoder s_x_user_defined_decoder;
}

Optional<Decoder&> decoder_for(StringView a_encoding)
{
    auto encoding = get_standardized_encoding(a_encoding);
    if (encoding.has_value()) {
        if (encoding.value().equals_ignoring_ascii_case("windows-1252"sv))
            return s_latin1_decoder;
        if (encoding.value().equals_ignoring_ascii_case("utf-8"sv))
            return s_utf8_decoder;
        if (encoding.value().equals_ignoring_ascii_case("utf-16be"sv))
            return s_utf16be_decoder;
        if (encoding.value().equals_ignoring_ascii_case("utf-16le"sv))
            return s_utf16le_decoder;
        if (encoding.value().equals_ignoring_ascii_case("iso-8859-2"sv))
            return s_latin2_decoder;
        if (encoding.value().equals_ignoring_ascii_case("windows-1255"sv))
            return s_hebrew_decoder;
        if (encoding.value().equals_ignoring_ascii_case("windows-1251"sv))
            return s_cyrillic_decoder;
        if (encoding.value().equals_ignoring_ascii_case("koi8-r"sv))
            return s_koi8r_decoder;
        if (encoding.value().equals_ignoring_ascii_case("iso-8859-15"sv))
            return s_latin9_decoder;
        if (encoding.value().equals_ignoring_ascii_case("macintosh"sv))
            return s_mac_roman_decoder;
        if (encoding.value().equals_ignoring_ascii_case("PDFDocEncoding"sv))
            return s_pdf_doc_encoding_decoder;
        if (encoding.value().equals_ignoring_ascii_case("windows-1254"sv))
            return s_turkish_decoder;
        if (encoding.value().equals_ignoring_ascii_case("x-user-defined"sv))
            return s_x_user_defined_decoder;
    }
    dbgln("TextCodec: No decoder implemented for encoding '{}'", a_encoding);
    return {};
}

// https://encoding.spec.whatwg.org/#concept-encoding-get
Optional<StringView> get_standardized_encoding(StringView encoding)
{
    encoding = encoding.trim_whitespace();

    if (encoding.is_one_of_ignoring_ascii_case("unicode-1-1-utf-8"sv, "unicode11utf8"sv, "unicode20utf8"sv, "utf-8"sv, "utf8"sv, "x-unicode20utf8"sv))
        return "UTF-8"sv;
    if (encoding.is_one_of_ignoring_ascii_case("866"sv, "cp866"sv, "csibm866"sv, "ibm866"sv))
        return "IBM866"sv;
    if (encoding.is_one_of_ignoring_ascii_case("csisolatin2"sv, "iso-8859-2"sv, "iso-ir-101"sv, "iso8859-2"sv, "iso88592"sv, "iso_8859-2"sv, "iso_8859-2:1987"sv, "l2"sv, "latin2"sv))
        return "ISO-8859-2"sv;
    if (encoding.is_one_of_ignoring_ascii_case("csisolatin3"sv, "iso-8859-3"sv, "iso-ir-109"sv, "iso8859-3"sv, "iso88593"sv, "iso_8859-3"sv, "iso_8859-3:1988"sv, "l3"sv, "latin3"sv))
        return "ISO-8859-3"sv;
    if (encoding.is_one_of_ignoring_ascii_case("csisolatin4"sv, "iso-8859-4"sv, "iso-ir-110"sv, "iso8859-4"sv, "iso88594"sv, "iso_8859-4"sv, "iso_8859-4:1989"sv, "l4"sv, "latin4"sv))
        return "ISO-8859-4"sv;
    if (encoding.is_one_of_ignoring_ascii_case("csisolatincyrillic"sv, "cyrillic"sv, "iso-8859-5"sv, "iso-ir-144"sv, "iso8859-5"sv, "iso88595"sv, "iso_8859-5"sv, "iso_8859-5:1988"sv))
        return "ISO-8859-5"sv;
    if (encoding.is_one_of_ignoring_ascii_case("arabic"sv, "asmo-708"sv, "csiso88596e"sv, "csiso88596i"sv, "csisolatinarabic"sv, "ecma-114"sv, "iso-8859-6"sv, "iso-8859-6-e"sv, "iso-8859-6-i"sv, "iso-ir-127"sv, "iso8859-6"sv, "iso88596"sv, "iso_8859-6"sv, "iso_8859-6:1987"sv))
        return "ISO-8859-6"sv;
    if (encoding.is_one_of_ignoring_ascii_case("csisolatingreek"sv, "ecma-118"sv, "elot_928"sv, "greek"sv, "greek8"sv, "iso-8859-7"sv, "iso-ir-126"sv, "iso8859-7"sv, "iso88597"sv, "iso_8859-7"sv, "iso_8859-7:1987"sv, "sun_eu_greek"sv))
        return "ISO-8859-7"sv;
    if (encoding.is_one_of_ignoring_ascii_case("csiso88598e"sv, "csisolatinhebrew"sv, "hebrew"sv, "iso-8859-8"sv, "iso-8859-8-e"sv, "iso-ir-138"sv, "iso8859-8"sv, "iso88598"sv, "iso_8859-8"sv, "iso_8859-8:1988"sv, "visual"sv))
        return "ISO-8859-8"sv;
    if (encoding.is_one_of_ignoring_ascii_case("csiso88598i"sv, "iso-8859-8-i"sv, "logical"sv))
        return "ISO-8859-8-I"sv;
    if (encoding.is_one_of_ignoring_ascii_case("csisolatin6"sv, "iso8859-10"sv, "iso-ir-157"sv, "iso8859-10"sv, "iso885910"sv, "l6"sv, "latin6"sv))
        return "ISO-8859-10"sv;
    if (encoding.is_one_of_ignoring_ascii_case("iso-8859-13"sv, "iso8859-13"sv, "iso885913"sv))
        return "ISO-8859-13"sv;
    if (encoding.is_one_of_ignoring_ascii_case("iso-8859-14"sv, "iso8859-14"sv, "iso885914"sv))
        return "ISO-8859-14"sv;
    if (encoding.is_one_of_ignoring_ascii_case("csisolatin9"sv, "iso-8859-15"sv, "iso8859-15"sv, "iso885915"sv, "iso_8859-15"sv, "l9"sv))
        return "ISO-8859-15"sv;
    if (encoding.is_one_of_ignoring_ascii_case("iso-8859-16"sv))
        return "ISO-8859-16"sv;
    if (encoding.is_one_of_ignoring_ascii_case("cskoi8r"sv, "koi"sv, "koi8"sv, "koi8-r"sv, "koi8_r"sv))
        return "KOI8-R"sv;
    if (encoding.is_one_of_ignoring_ascii_case("koi8-ru"sv, "koi8-u"sv))
        return "KOI8-U"sv;
    if (encoding.is_one_of_ignoring_ascii_case("csmacintosh"sv, "mac"sv, "macintosh"sv, "x-mac-roman"sv))
        return "macintosh"sv;
    if (encoding.is_one_of_ignoring_ascii_case("pdfdocencoding"sv))
        return "PDFDocEncoding"sv;
    if (encoding.is_one_of_ignoring_ascii_case("dos-874"sv, "iso-8859-11"sv, "iso8859-11"sv, "iso885911"sv, "tis-620"sv, "windows-874"sv))
        return "windows-874"sv;
    if (encoding.is_one_of_ignoring_ascii_case("cp1250"sv, "windows-1250"sv, "x-cp1250"sv))
        return "windows-1250"sv;
    if (encoding.is_one_of_ignoring_ascii_case("cp1251"sv, "windows-1251"sv, "x-cp1251"sv))
        return "windows-1251"sv;
    if (encoding.is_one_of_ignoring_ascii_case("ansi_x3.4-1968"sv, "ascii"sv, "cp1252"sv, "cp819"sv, "csisolatin1"sv, "ibm819"sv, "iso-8859-1"sv, "iso-ir-100"sv, "iso8859-1"sv, "iso88591"sv, "iso_8859-1"sv, "iso_8859-1:1987"sv, "l1"sv, "latin1"sv, "us-ascii"sv, "windows-1252"sv, "x-cp1252"sv))
        return "windows-1252"sv;
    if (encoding.is_one_of_ignoring_ascii_case("cp1253"sv, "windows-1253"sv, "x-cp1253"sv))
        return "windows-1253"sv;
    if (encoding.is_one_of_ignoring_ascii_case("cp1254"sv, "csisolatin5"sv, "iso-8859-9"sv, "iso-ir-148"sv, "iso-8859-9"sv, "iso-88599"sv, "iso_8859-9"sv, "iso_8859-9:1989"sv, "l5"sv, "latin5"sv, "windows-1254"sv, "x-cp1254"sv))
        return "windows-1254"sv;
    if (encoding.is_one_of_ignoring_ascii_case("cp1255"sv, "windows-1255"sv, "x-cp1255"sv))
        return "windows-1255"sv;
    if (encoding.is_one_of_ignoring_ascii_case("cp1256"sv, "windows-1256"sv, "x-cp1256"sv))
        return "windows-1256"sv;
    if (encoding.is_one_of_ignoring_ascii_case("cp1257"sv, "windows-1257"sv, "x-cp1257"sv))
        return "windows-1257"sv;
    if (encoding.is_one_of_ignoring_ascii_case("cp1258"sv, "windows-1258"sv, "x-cp1258"sv))
        return "windows-1258"sv;
    if (encoding.is_one_of_ignoring_ascii_case("x-mac-cyrillic"sv, "x-mac-ukrainian"sv))
        return "x-mac-cyrillic"sv;
    if (encoding.is_one_of_ignoring_ascii_case("koi8-r"sv, "koi8r"sv))
        return "koi8-r"sv;
    if (encoding.is_one_of_ignoring_ascii_case("chinese"sv, "csgb2312"sv, "csiso58gb231280"sv, "gb2312"sv, "gb_2312"sv, "gb_2312-80"sv, "gbk"sv, "iso-ir-58"sv, "x-gbk"sv))
        return "GBK"sv;
    if (encoding.is_one_of_ignoring_ascii_case("gb18030"sv))
        return "gb18030"sv;
    if (encoding.is_one_of_ignoring_ascii_case("big5"sv, "big5-hkscs"sv, "cn-big5"sv, "csbig5"sv, "x-x-big5"sv))
        return "Big5"sv;
    if (encoding.is_one_of_ignoring_ascii_case("cseucpkdfmtjapanese"sv, "euc-jp"sv, "x-euc-jp"sv))
        return "EUC-JP"sv;
    if (encoding.is_one_of_ignoring_ascii_case("csiso2022jp"sv, "iso-2022-jp"sv))
        return "ISO-2022-JP"sv;
    if (encoding.is_one_of_ignoring_ascii_case("csshiftjis"sv, "ms932"sv, "ms_kanji"sv, "shift-jis"sv, "shift_jis"sv, "sjis"sv, "windows-31j"sv, "x-sjis"sv))
        return "Shift_JIS"sv;
    if (encoding.is_one_of_ignoring_ascii_case("cseuckr"sv, "csksc56011987"sv, "euc-kr"sv, "iso-ir-149"sv, "korean"sv, "ks_c_5601-1987"sv, "ks_c_5601-1989"sv, "ksc5601"sv, "ksc_5601"sv, "windows-949"sv))
        return "EUC-KR"sv;
    if (encoding.is_one_of_ignoring_ascii_case("csiso2022kr"sv, "hz-gb-2312"sv, "iso-2022-cn"sv, "iso-2022-cn-ext"sv, "iso-2022-kr"sv, "replacement"sv))
        return "replacement"sv;
    if (encoding.is_one_of_ignoring_ascii_case("unicodefffe"sv, "utf-16be"sv))
        return "UTF-16BE"sv;
    if (encoding.is_one_of_ignoring_ascii_case("csunicode"sv, "iso-10646-ucs-2"sv, "ucs-2"sv, "unicode"sv, "unicodefeff"sv, "utf-16"sv, "utf-16le"sv))
        return "UTF-16LE"sv;
    if (encoding.is_one_of_ignoring_ascii_case("x-user-defined"sv))
        return "x-user-defined"sv;

    dbgln("TextCodec: Unrecognized encoding: {}", encoding);
    return {};
}

// https://encoding.spec.whatwg.org/#bom-sniff
Optional<Decoder&> bom_sniff_to_decoder(StringView input)
{
    // 1. Let BOM be the result of peeking 3 bytes from ioQueue, converted to a byte sequence.
    // 2. For each of the rows in the table below, starting with the first one and going down,
    //    if BOM starts with the bytes given in the first column, then return the encoding given
    //    in the cell in the second column of that row. Otherwise, return null.

    // Byte Order Mark | Encoding
    // --------------------------
    // 0xEF 0xBB 0xBF  | UTF-8
    // 0xFE 0xFF       | UTF-16BE
    // 0xFF 0xFE       | UTF-16LE

    auto bytes = input.bytes();
    if (bytes.size() < 2)
        return {};

    auto first_byte = bytes[0];

    switch (first_byte) {
    case 0xEF: // UTF-8
        if (bytes.size() < 3)
            return {};
        if (bytes[1] == 0xBB && bytes[2] == 0xBF)
            return s_utf8_decoder;
        return {};
    case 0xFE: // UTF-16BE
        if (bytes[1] == 0xFF)
            return s_utf16be_decoder;
        return {};
    case 0xFF: // UTF-16LE
        if (bytes[1] == 0xFE)
            return s_utf16le_decoder;
        return {};
    }

    return {};
}

// https://encoding.spec.whatwg.org/#decode
ErrorOr<String> convert_input_to_utf8_using_given_decoder_unless_there_is_a_byte_order_mark(Decoder& fallback_decoder, StringView input)
{
    Decoder* actual_decoder = &fallback_decoder;

    // 1. Let BOMEncoding be the result of BOM sniffing ioQueue.
    // 2. If BOMEncoding is non-null:
    if (auto unicode_decoder = bom_sniff_to_decoder(input); unicode_decoder.has_value()) {
        // 1. Set encoding to BOMEncoding.
        actual_decoder = &unicode_decoder.value();

        // 2. Read three bytes from ioQueue, if BOMEncoding is UTF-8; otherwise read two bytes. (Do nothing with those bytes.)
        // FIXME: I imagine this will be pretty slow for large inputs, as it's regenerating the input without the first 2/3 bytes.
        input = input.substring_view(&unicode_decoder.value() == &s_utf8_decoder ? 3 : 2);
    }

    VERIFY(actual_decoder);

    // 3. Process a queue with an instance of encoding’s decoder, ioQueue, output, and "replacement".
    // FIXME: This isn't the exact same as the spec, which is written in terms of I/O queues.
    auto output = TRY(actual_decoder->to_utf8(input));

    // 4. Return output.
    return output;
}

// https://encoding.spec.whatwg.org/#get-an-output-encoding
StringView get_output_encoding(StringView encoding)
{
    // 1. If encoding is replacement or UTF-16BE/LE, then return UTF-8.
    if (encoding.is_one_of_ignoring_ascii_case("replacement"sv, "utf-16le"sv, "utf-16be"sv))
        return "UTF-8"sv;

    // 2. Return encoding.
    return encoding;
}

bool Decoder::validate(StringView)
{
    // By-default we assume that any input sequence is valid, character encodings that do not accept all inputs may override this
    return true;
}

ErrorOr<String> Decoder::to_utf8(StringView input)
{
    StringBuilder builder(input.length());
    TRY(process(input, [&builder](u32 c) { return builder.try_append_code_point(c); }));
    return builder.to_string_without_validation();
}

ErrorOr<void> UTF8Decoder::process(StringView input, Function<ErrorOr<void>(u32)> on_code_point)
{
    for (auto c : Utf8View(input)) {
        TRY(on_code_point(c));
    }
    return {};
}

bool UTF8Decoder::validate(StringView input)
{
    return Utf8View(input).validate();
}

ErrorOr<String> UTF8Decoder::to_utf8(StringView input)
{
    // Discard the BOM
    auto bomless_input = input;
    if (auto bytes = input.bytes(); bytes.size() >= 3 && bytes[0] == 0xEF && bytes[1] == 0xBB && bytes[2] == 0xBF) {
        bomless_input = input.substring_view(3);
    }

    return Decoder::to_utf8(bomless_input);
}

ErrorOr<void> UTF16BEDecoder::process(StringView input, Function<ErrorOr<void>(u32)> on_code_point)
{
    // rfc2781, 2.2 Decoding UTF-16
    size_t utf16_length = input.length() - (input.length() % 2);
    for (size_t i = 0; i < utf16_length; i += 2) {
        // 1) If W1 < 0xD800 or W1 > 0xDFFF, the character value U is the value
        //    of W1. Terminate.
        u16 w1 = (static_cast<u8>(input[i]) << 8) | static_cast<u8>(input[i + 1]);
        if (!is_unicode_surrogate(w1)) {
            TRY(on_code_point(w1));
            continue;
        }

        // 2) Determine if W1 is between 0xD800 and 0xDBFF. If not, the sequence
        //    is in error and no valid character can be obtained using W1.
        //    Terminate.
        // 3) If there is no W2 (that is, the sequence ends with W1), or if W2
        //    is not between 0xDC00 and 0xDFFF, the sequence is in error.
        //    Terminate.
        if (!Utf16View::is_high_surrogate(w1) || i + 2 == utf16_length) {
            TRY(on_code_point(replacement_code_point));
            continue;
        }

        u16 w2 = (static_cast<u8>(input[i + 2]) << 8) | static_cast<u8>(input[i + 3]);
        if (!Utf16View::is_low_surrogate(w2)) {
            TRY(on_code_point(replacement_code_point));
            continue;
        }

        // 4) Construct a 20-bit unsigned integer U', taking the 10 low-order
        //    bits of W1 as its 10 high-order bits and the 10 low-order bits of
        //    W2 as its 10 low-order bits.
        // 5) Add 0x10000 to U' to obtain the character value U. Terminate.
        TRY(on_code_point(Utf16View::decode_surrogate_pair(w1, w2)));
        i += 2;
    }

    return {};
}

bool UTF16BEDecoder::validate(StringView input)
{
    size_t utf16_length = input.length() - (input.length() % 2);
    for (size_t i = 0; i < utf16_length; i += 2) {
        u16 w1 = (static_cast<u8>(input[i]) << 8) | static_cast<u8>(input[i + 1]);
        if (!is_unicode_surrogate(w1))
            continue;

        if (!Utf16View::is_high_surrogate(w1) || i + 2 == utf16_length)
            return false;

        u16 w2 = (static_cast<u8>(input[i + 2]) << 8) | static_cast<u8>(input[i + 3]);
        if (!Utf16View::is_low_surrogate(w2))
            return false;

        i += 2;
    }
    return true;
}

ErrorOr<String> UTF16BEDecoder::to_utf8(StringView input)
{
    // Discard the BOM
    auto bomless_input = input;
    if (auto bytes = input.bytes(); bytes.size() >= 2 && bytes[0] == 0xFE && bytes[1] == 0xFF)
        bomless_input = input.substring_view(2);

    StringBuilder builder(bomless_input.length() / 2);
    TRY(process(bomless_input, [&builder](u32 c) { return builder.try_append_code_point(c); }));
    return builder.to_string();
}

ErrorOr<void> UTF16LEDecoder::process(StringView input, Function<ErrorOr<void>(u32)> on_code_point)
{
    // rfc2781, 2.2 Decoding UTF-16
    size_t utf16_length = input.length() - (input.length() % 2);
    for (size_t i = 0; i < utf16_length; i += 2) {
        // 1) If W1 < 0xD800 or W1 > 0xDFFF, the character value U is the value
        //    of W1. Terminate.
        u16 w1 = static_cast<u8>(input[i]) | (static_cast<u8>(input[i + 1]) << 8);
        if (!is_unicode_surrogate(w1)) {
            TRY(on_code_point(w1));
            continue;
        }

        // 2) Determine if W1 is between 0xD800 and 0xDBFF. If not, the sequence
        //    is in error and no valid character can be obtained using W1.
        //    Terminate.
        // 3) If there is no W2 (that is, the sequence ends with W1), or if W2
        //    is not between 0xDC00 and 0xDFFF, the sequence is in error.
        //    Terminate.
        if (!Utf16View::is_high_surrogate(w1) || i + 2 == utf16_length) {
            TRY(on_code_point(replacement_code_point));
            continue;
        }

        u16 w2 = static_cast<u8>(input[i + 2]) | (static_cast<u8>(input[i + 3]) << 8);
        if (!Utf16View::is_low_surrogate(w2)) {
            TRY(on_code_point(replacement_code_point));
            continue;
        }

        // 4) Construct a 20-bit unsigned integer U', taking the 10 low-order
        //    bits of W1 as its 10 high-order bits and the 10 low-order bits of
        //    W2 as its 10 low-order bits.
        // 5) Add 0x10000 to U' to obtain the character value U. Terminate.
        TRY(on_code_point(Utf16View::decode_surrogate_pair(w1, w2)));
        i += 2;
    }

    return {};
}

bool UTF16LEDecoder::validate(StringView input)
{
    size_t utf16_length = input.length() - (input.length() % 2);
    for (size_t i = 0; i < utf16_length; i += 2) {
        u16 w1 = static_cast<u8>(input[i]) | (static_cast<u8>(input[i + 1]) << 8);
        if (!is_unicode_surrogate(w1))
            continue;

        if (!Utf16View::is_high_surrogate(w1) || i + 2 == utf16_length)
            return false;

        u16 w2 = static_cast<u8>(input[i + 2]) | (static_cast<u8>(input[i + 3]) << 8);
        if (!Utf16View::is_low_surrogate(w2))
            return false;

        i += 2;
    }
    return true;
}

ErrorOr<String> UTF16LEDecoder::to_utf8(StringView input)
{
    // Discard the BOM
    auto bomless_input = input;
    if (auto bytes = input.bytes(); bytes.size() >= 2 && bytes[0] == 0xFF && bytes[1] == 0xFE)
        bomless_input = input.substring_view(2);

    StringBuilder builder(bomless_input.length() / 2);
    TRY(process(bomless_input, [&builder](u32 c) { return builder.try_append_code_point(c); }));
    return builder.to_string();
}

ErrorOr<void> Latin1Decoder::process(StringView input, Function<ErrorOr<void>(u32)> on_code_point)
{
    for (u8 ch : input) {
        // Latin1 is the same as the first 256 Unicode code_points, so no mapping is needed, just utf-8 encoding.
        TRY(on_code_point(ch));
    }

    return {};
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

ErrorOr<void> Latin2Decoder::process(StringView input, Function<ErrorOr<void>(u32)> on_code_point)
{
    for (auto c : input) {
        TRY(on_code_point(convert_latin2_to_utf8(c)));
    }

    return {};
}

ErrorOr<void> HebrewDecoder::process(StringView input, Function<ErrorOr<void>(u32)> on_code_point)
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
            TRY(on_code_point(ch));
        } else {
            TRY(on_code_point(translation_table[ch - 0x80]));
        }
    }

    return {};
}

ErrorOr<void> CyrillicDecoder::process(StringView input, Function<ErrorOr<void>(u32)> on_code_point)
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
            TRY(on_code_point(ch));
        } else {
            TRY(on_code_point(translation_table[ch - 0x80]));
        }
    }

    return {};
}

ErrorOr<void> Koi8RDecoder::process(StringView input, Function<ErrorOr<void>(u32)> on_code_point)
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
            TRY(on_code_point(ch));
        } else {
            TRY(on_code_point(translation_table[ch - 0x80]));
        }
    }

    return {};
}

ErrorOr<void> Latin9Decoder::process(StringView input, Function<ErrorOr<void>(u32)> on_code_point)
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
        TRY(on_code_point(convert_latin9_to_utf8(ch)));
    }

    return {};
}

ErrorOr<void> MacRomanDecoder::process(StringView input, Function<ErrorOr<void>(u32)> on_code_point)
{
    // https://encoding.spec.whatwg.org/index-macintosh.txt
    // clang-format off
    static constexpr Array<u32, 128> translation_table = {
        0x00C4, 0x00C5, 0x00C7, 0x00C9, 0x00D1, 0x00D6, 0x00DC, 0x00E1, 0x00E0, 0x00E2, 0x00E4, 0x00E3, 0x00E5, 0x00E7, 0x00E9, 0x00E8,
        0x00EA, 0x00EB, 0x00ED, 0x00EC, 0x00EE, 0x00EF, 0x00F1, 0x00F3, 0x00F2, 0x00F4, 0x00F6, 0x00F5, 0x00FA, 0x00F9, 0x00FB, 0x00FC,
        0x2020, 0x00B0, 0x00A2, 0x00A3, 0x00A7, 0x2022, 0x00B6, 0x00DF, 0x00AE, 0x00A9, 0x2122, 0x00B4, 0x00A8, 0x2260, 0x00C6, 0x00D8,
        0x221E, 0x00B1, 0x2264, 0x2265, 0x00A5, 0x00B5, 0x2202, 0x2211, 0x220F, 0x03C0, 0x222B, 0x00AA, 0x00BA, 0x03A9, 0x00E6, 0x00F8,
        0x00BF, 0x00A1, 0x00AC, 0x221A, 0x0192, 0x2248, 0x2206, 0x00AB, 0x00BB, 0x2026, 0x00A0, 0x00C0, 0x00C3, 0x00D5, 0x0152, 0x0153,
        0x2013, 0x2014, 0x201C, 0x201D, 0x2018, 0x2019, 0x00F7, 0x25CA, 0x00FF, 0x0178, 0x2044, 0x20AC, 0x2039, 0x203A, 0xFB01, 0xFB02,
        0x2021, 0x00B7, 0x201A, 0x201E, 0x2030, 0x00C2, 0x00CA, 0x00C1, 0x00CB, 0x00C8, 0x00CD, 0x00CE, 0x00CF, 0x00CC, 0x00D3, 0x00D4,
        0xF8FF, 0x00D2, 0x00DA, 0x00DB, 0x00D9, 0x0131, 0x02C6, 0x02DC, 0x00AF, 0x02D8, 0x02D9, 0x02DA, 0x00B8, 0x02DD, 0x02DB, 0x02C7,
    };
    // clang-format on

    for (u8 ch : input) {
        if (ch < 0x80) { // Superset of ASCII
            TRY(on_code_point(ch));
        } else {
            TRY(on_code_point(translation_table[ch - 0x80]));
        }
    }

    return {};
}

ErrorOr<void> PDFDocEncodingDecoder::process(StringView input, Function<ErrorOr<void>(u32)> on_code_point)
{
    // PDF 1.7 spec, Appendix D.2 "PDFDocEncoding Character Set"
    // Character codes 0-8, 11-12, 14-23, 127, 159, 173 are not defined per spec.
    // clang-format off
    static constexpr Array<u32, 256> translation_table = {
        0x0000, 0x0001, 0x0002, 0x0003, 0x0004, 0x0005, 0x0006, 0x0007,
        0x0008, 0x0009, 0x000A, 0x000B, 0x000C, 0x000D, 0x000E, 0x000F,
        0x0010, 0x0011, 0x0012, 0x0013, 0x0014, 0x0015, 0x0017, 0x0017,
        0x02D8, 0x02C7, 0x02C6, 0x02D9, 0x02DD, 0x02DB, 0x02DA, 0x02DC,
        0x0020, 0x0021, 0x0022, 0x0023, 0x0024, 0x0025, 0x0026, 0x0027,
        0x0028, 0x0029, 0x002A, 0x002B, 0x002C, 0x002D, 0x002E, 0x002F,
        0x0030, 0x0031, 0x0032, 0x0033, 0x0034, 0x0035, 0x0036, 0x0037,
        0x0038, 0x0039, 0x003A, 0x003B, 0x003C, 0x003D, 0x003E, 0x003F,
        0x0040, 0x0041, 0x0042, 0x0043, 0x0044, 0x0045, 0x0046, 0x0047,
        0x0048, 0x0049, 0x004A, 0x004B, 0x004C, 0x004D, 0x004E, 0x004F,
        0x0050, 0x0051, 0x0052, 0x0053, 0x0054, 0x0055, 0x0056, 0x0057,
        0x0058, 0x0059, 0x005A, 0x005B, 0x005C, 0x005D, 0x005E, 0x005F,
        0x0060, 0x0061, 0x0062, 0x0063, 0x0064, 0x0065, 0x0066, 0x0067,
        0x0068, 0x0069, 0x006A, 0x006B, 0x006C, 0x006D, 0x006E, 0x006F,
        0x0070, 0x0071, 0x0072, 0x0073, 0x0074, 0x0075, 0x0076, 0x0077,
        0x0078, 0x0079, 0x007A, 0x007B, 0x007C, 0x007D, 0x007E, 0xFFFC,
        0x2022, 0x2020, 0x2021, 0x2026, 0x2014, 0x2013, 0x0192, 0x2044,
        0x2039, 0x203A, 0x2212, 0x2030, 0x201E, 0x201C, 0x201D, 0x2018,
        0x2019, 0x201A, 0x2122, 0xFB01, 0xFB02, 0x0141, 0x0152, 0x0160,
        0x0178, 0x017D, 0x0131, 0x0142, 0x0153, 0x0161, 0x017E, 0xFFFC,
        0x20AC, 0x00A1, 0x00A2, 0x00A3, 0x00A4, 0x00A5, 0x00A6, 0x00A7,
        0x00A8, 0x00A9, 0x00AA, 0x00AB, 0x00AC, 0xFFFC, 0x00AE, 0x00AF,
        0x00B0, 0x00B1, 0x00B2, 0x00B3, 0x00B4, 0x00B5, 0x00B6, 0x00B7,
        0x00B8, 0x00B9, 0x00BA, 0x00BB, 0x00BC, 0x00BD, 0x00BE, 0x00BF,
        0x00C0, 0x00C1, 0x00C2, 0x00C3, 0x00C4, 0x00C5, 0x00C6, 0x00C7,
        0x00C8, 0x00C9, 0x00CA, 0x00CB, 0x00CC, 0x00CD, 0x00CE, 0x00CF,
        0x00D0, 0x00D1, 0x00D2, 0x00D3, 0x00D4, 0x00D5, 0x00D6, 0x00D7,
        0x00D8, 0x00D9, 0x00DA, 0x00DB, 0x00DC, 0x00DD, 0x00DE, 0x00DF,
        0x00E0, 0x00E1, 0x00E2, 0x00E3, 0x00E4, 0x00E5, 0x00E6, 0x00E7,
        0x00E8, 0x00E9, 0x00EA, 0x00EB, 0x00EC, 0x00ED, 0x00EE, 0x00EF,
        0x00F0, 0x00F1, 0x00F2, 0x00F3, 0x00F4, 0x00F5, 0x00F6, 0x00F7,
        0x00F8, 0x00F9, 0x00FA, 0x00FB, 0x00FC, 0x00FD, 0x00FE, 0x00FF,
    };
    // clang-format on

    for (u8 ch : input)
        TRY(on_code_point(translation_table[ch]));

    return {};
}

ErrorOr<void> TurkishDecoder::process(StringView input, Function<ErrorOr<void>(u32)> on_code_point)
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
        TRY(on_code_point(convert_turkish_to_utf8(ch)));
    }

    return {};
}

// https://encoding.spec.whatwg.org/#x-user-defined-decoder
ErrorOr<void> XUserDefinedDecoder::process(StringView input, Function<ErrorOr<void>(u32)> on_code_point)
{
    auto convert_x_user_defined_to_utf8 = [](u8 ch) -> u32 {
        // 2. If byte is an ASCII byte, return a code point whose value is byte.
        // https://infra.spec.whatwg.org/#ascii-byte
        // An ASCII byte is a byte in the range 0x00 (NUL) to 0x7F (DEL), inclusive.
        // NOTE: This doesn't check for ch >= 0x00, as that would always be true due to being unsigned.
        if (ch <= 0x7f)
            return ch;

        // 3. Return a code point whose value is 0xF780 + byte − 0x80.
        return 0xF780 + ch - 0x80;
    };

    for (auto ch : input) {
        TRY(on_code_point(convert_x_user_defined_to_utf8(ch)));
    }

    // 1. If byte is end-of-queue, return finished.

    return {};
}

}
