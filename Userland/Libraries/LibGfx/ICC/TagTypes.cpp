/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DeprecatedString.h>
#include <AK/Endian.h>
#include <LibGfx/ICC/TagTypes.h>
#include <LibTextCodec/Decoder.h>

namespace Gfx::ICC {

namespace {

// ICC V4, 4.6 s15Fixed16Number
using s15Fixed16Number = i32;

// ICC V4, 4.14 XYZNumber
struct XYZNumber {
    BigEndian<s15Fixed16Number> x;
    BigEndian<s15Fixed16Number> y;
    BigEndian<s15Fixed16Number> z;

    operator XYZ() const
    {
        return XYZ { x / (double)0x1'0000, y / (double)0x1'0000, z / (double)0x1'0000 };
    }
};

// Common bits of ICC v4, Table 40 — lut16Type encoding and Table 44 — lut8Type encoding
struct LUTHeader {
    u8 number_of_input_channels;
    u8 number_of_output_channels;
    u8 number_of_clut_grid_points;
    u8 reserved_for_padding;
    BigEndian<s15Fixed16Number> e_parameters[9];
};
static_assert(AssertSize<LUTHeader, 40>());

ErrorOr<void> check_reserved(ReadonlyBytes tag_bytes)
{
    if (tag_bytes.size() < 2 * sizeof(u32))
        return Error::from_string_literal("ICC::Profile: Not enough data for tag reserved field");

    if (*bit_cast<BigEndian<u32> const*>(tag_bytes.data() + sizeof(u32)) != 0)
        return Error::from_string_literal("ICC::Profile: tag reserved field not 0");

    return {};
}

}

TagTypeSignature tag_type(ReadonlyBytes tag_bytes)
{
    VERIFY(tag_bytes.size() >= sizeof(u32));
    return *bit_cast<BigEndian<TagTypeSignature> const*>(tag_bytes.data());
}

ErrorOr<NonnullRefPtr<CurveTagData>> CurveTagData::from_bytes(ReadonlyBytes bytes, u32 offset, u32 size)
{
    // ICC v4, 10.6 curveType
    VERIFY(tag_type(bytes) == Type);
    TRY(check_reserved(bytes));

    if (bytes.size() < 3 * sizeof(u32))
        return Error::from_string_literal("ICC::Profile: curveType has not enough data for count");
    u32 count = *bit_cast<BigEndian<u32> const*>(bytes.data() + 8);

    if (bytes.size() < 3 * sizeof(u32) + count * sizeof(u16))
        return Error::from_string_literal("ICC::Profile: curveType has not enough data for curve points");

    auto* raw_values = bit_cast<BigEndian<u16> const*>(bytes.data() + 12);
    Vector<u16> values;
    TRY(values.try_resize(count));

    for (u32 i = 0; i < count; ++i)
        values[i] = raw_values[i];

    return adopt_ref(*new CurveTagData(offset, size, move(values)));
}

ErrorOr<NonnullRefPtr<Lut16TagData>> Lut16TagData::from_bytes(ReadonlyBytes bytes, u32 offset, u32 size)
{
    // ICC v4, 10.10 lut16Type
    VERIFY(tag_type(bytes) == Type);
    TRY(check_reserved(bytes));

    if (bytes.size() < 2 * sizeof(u32) + sizeof(LUTHeader) + 2 + sizeof(u16))
        return Error::from_string_literal("ICC::Profile: lut16Type has not enough data");

    auto& header = *bit_cast<LUTHeader const*>(bytes.data() + 8);
    if (header.reserved_for_padding != 0)
        return Error::from_string_literal("ICC::Profile: lut16Type reserved_for_padding not 0");

    u16 number_of_input_table_entries = *bit_cast<BigEndian<u16> const*>(bytes.data() + 8 + sizeof(LUTHeader));
    u16 number_of_output_table_entries = *bit_cast<BigEndian<u16> const*>(bytes.data() + 8 + sizeof(LUTHeader) + 2);
    ReadonlyBytes table_bytes = bytes.slice(8 + sizeof(LUTHeader) + 4);

    EMatrix3x3 e;
    for (int i = 0; i < 9; ++i)
        e.e[i] = S15Fixed16::create_raw(header.e_parameters[i]);

    u32 input_tables_size = number_of_input_table_entries * header.number_of_input_channels;
    u32 output_tables_size = number_of_output_table_entries * header.number_of_output_channels;
    u32 clut_values_size = header.number_of_output_channels;
    for (int i = 0; i < header.number_of_input_channels; ++i)
        clut_values_size *= header.number_of_clut_grid_points;

    if (table_bytes.size() < (input_tables_size + clut_values_size + output_tables_size) * sizeof(u16))
        return Error::from_string_literal("ICC::Profile: lut16Type has not enough data for tables");

    auto* raw_table_data = bit_cast<BigEndian<u16> const*>(table_bytes.data());

    Vector<u16> input_tables;
    input_tables.resize(input_tables_size);
    for (u32 i = 0; i < input_tables_size; ++i)
        input_tables[i] = raw_table_data[i];

    Vector<u16> clut_values;
    clut_values.resize(clut_values_size);
    for (u32 i = 0; i < clut_values_size; ++i)
        clut_values[i] = raw_table_data[input_tables_size + i];

    Vector<u16> output_tables;
    output_tables.resize(output_tables_size);
    for (u32 i = 0; i < output_tables_size; ++i)
        output_tables[i] = raw_table_data[input_tables_size + clut_values_size + i];

    return adopt_ref(*new Lut16TagData(offset, size, e,
        header.number_of_input_channels, header.number_of_output_channels, header.number_of_clut_grid_points,
        number_of_input_table_entries, number_of_output_table_entries,
        move(input_tables), move(clut_values), move(output_tables)));
}

ErrorOr<NonnullRefPtr<Lut8TagData>> Lut8TagData::from_bytes(ReadonlyBytes bytes, u32 offset, u32 size)
{
    // ICC v4, 10.11 lut8Type
    VERIFY(tag_type(bytes) == Type);
    TRY(check_reserved(bytes));

    if (bytes.size() < 8 + sizeof(LUTHeader))
        return Error::from_string_literal("ICC::Profile: lut8Type has not enough data");

    auto& header = *bit_cast<LUTHeader const*>(bytes.data() + 8);
    if (header.reserved_for_padding != 0)
        return Error::from_string_literal("ICC::Profile: lut16Type reserved_for_padding not 0");

    u16 number_of_input_table_entries = 256;
    u16 number_of_output_table_entries = 256;
    ReadonlyBytes table_bytes = bytes.slice(8 + sizeof(LUTHeader));

    EMatrix3x3 e;
    for (int i = 0; i < 9; ++i)
        e.e[i] = S15Fixed16::create_raw(header.e_parameters[i]);

    u32 input_tables_size = number_of_input_table_entries * header.number_of_input_channels;
    u32 output_tables_size = number_of_output_table_entries * header.number_of_output_channels;
    u32 clut_values_size = header.number_of_output_channels;
    for (int i = 0; i < header.number_of_input_channels; ++i)
        clut_values_size *= header.number_of_clut_grid_points;

    if (table_bytes.size() < input_tables_size + clut_values_size + output_tables_size)
        return Error::from_string_literal("ICC::Profile: lut8Type has not enough data for tables");

    Vector<u8> input_tables;
    input_tables.resize(input_tables_size);
    memcpy(input_tables.data(), table_bytes.data(), input_tables_size);

    Vector<u8> clut_values;
    clut_values.resize(clut_values_size);
    memcpy(clut_values.data(), table_bytes.data() + input_tables_size, clut_values_size);

    Vector<u8> output_tables;
    output_tables.resize(output_tables_size);
    memcpy(output_tables.data(), table_bytes.data() + input_tables_size + clut_values_size, output_tables_size);

    return adopt_ref(*new Lut8TagData(offset, size, e,
        header.number_of_input_channels, header.number_of_output_channels, header.number_of_clut_grid_points,
        number_of_input_table_entries, number_of_output_table_entries,
        move(input_tables), move(clut_values), move(output_tables)));
}

ErrorOr<NonnullRefPtr<MultiLocalizedUnicodeTagData>> MultiLocalizedUnicodeTagData::from_bytes(ReadonlyBytes bytes, u32 offset, u32 size)
{
    // ICC v4, 10.15 multiLocalizedUnicodeType
    VERIFY(tag_type(bytes) == Type);
    TRY(check_reserved(bytes));

    // "Multiple strings within this tag may share storage locations. For example, en/US and en/UK can refer to the
    //  same string data."
    // This implementation makes redudant string copies in that case.
    // Most of the time, this costs just a few bytes, so that seems ok.

    if (bytes.size() < 4 * sizeof(u32))
        return Error::from_string_literal("ICC::Profile: multiLocalizedUnicodeType has not enough data");

    // Table 54 — multiLocalizedUnicodeType
    u32 number_of_records = *bit_cast<BigEndian<u32> const*>(bytes.data() + 8);
    u32 record_size = *bit_cast<BigEndian<u32> const*>(bytes.data() + 12);

    // "The fourth field of this tag, the record size, should contain the value 12, which corresponds to the size in bytes
    // of each record. Any code that needs to access the nth record should determine the record’s offset by multiplying
    // n by the contents of this size field and adding 16. This minor extra effort allows for future expansion of the record
    // encoding, should the need arise, without having to define a new tag type."
    if (record_size < 12)
        return Error::from_string_literal("ICC::Profile: multiLocalizedUnicodeType record size too small");
    if (bytes.size() < 16 + number_of_records * record_size)
        return Error::from_string_literal("ICC::Profile: multiLocalizedUnicodeType not enough data for records");

    Vector<Record> records;
    TRY(records.try_resize(number_of_records));

    // "For the definition of language codes and country codes, see respectively
    //  ISO 639-1 and ISO 3166-1. The Unicode strings in storage should be encoded as 16-bit big-endian, UTF-16BE,
    //  and should not be NULL terminated."
    auto& utf_16be_decoder = *TextCodec::decoder_for("utf-16be");

    struct RawRecord {
        BigEndian<u16> language_code;
        BigEndian<u16> country_code;
        BigEndian<u32> string_length_in_bytes;
        BigEndian<u32> string_offset_in_bytes;
    };

    for (u32 i = 0; i < number_of_records; ++i) {
        size_t offset = 16 + i * record_size;
        RawRecord record = *bit_cast<RawRecord const*>(bytes.data() + offset);

        records[i].iso_639_1_language_code = record.language_code;
        records[i].iso_3166_1_country_code = record.country_code;

        if (record.string_length_in_bytes % 2 != 0)
            return Error::from_string_literal("ICC::Profile: multiLocalizedUnicodeType odd UTF-16 byte length");

        if (record.string_offset_in_bytes + record.string_length_in_bytes > bytes.size())
            return Error::from_string_literal("ICC::Profile: multiLocalizedUnicodeType string offset out of bounds");

        StringView utf_16be_data { bytes.data() + record.string_offset_in_bytes, record.string_length_in_bytes };
        records[i].text = TRY(String::from_deprecated_string(utf_16be_decoder.to_utf8(utf_16be_data)));
    }

    return adopt_ref(*new MultiLocalizedUnicodeTagData(offset, size, move(records)));
}

unsigned ParametricCurveTagData::parameter_count(FunctionType function_type)
{
    switch (function_type) {
    case FunctionType::Type0:
        return 1;
    case FunctionType::Type1:
        return 3;
    case FunctionType::Type2:
        return 4;
    case FunctionType::Type3:
        return 5;
    case FunctionType::Type4:
        return 7;
    }
    VERIFY_NOT_REACHED();
}

ErrorOr<NonnullRefPtr<ParametricCurveTagData>> ParametricCurveTagData::from_bytes(ReadonlyBytes bytes, u32 offset, u32 size)
{
    // ICC v4, 10.18 parametricCurveType
    VERIFY(tag_type(bytes) == Type);
    TRY(check_reserved(bytes));

    // "The parametricCurveType describes a one-dimensional curve by specifying one of a predefined set of functions
    //  using the parameters."

    if (bytes.size() < 2 * sizeof(u32) + 2 * sizeof(u16))
        return Error::from_string_literal("ICC::Profile: parametricCurveType has not enough data");

    u16 raw_function_type = *bit_cast<BigEndian<u16> const*>(bytes.data() + 8);
    u16 reserved = *bit_cast<BigEndian<u16> const*>(bytes.data() + 10);
    if (reserved != 0)
        return Error::from_string_literal("ICC::Profile: parametricCurveType reserved u16 after function type not 0");

    if (raw_function_type > 4)
        return Error::from_string_literal("ICC::Profile: parametricCurveType unknown function type");

    FunctionType function_type = (FunctionType)raw_function_type;
    unsigned count = parameter_count(function_type);

    if (bytes.size() < 2 * sizeof(u32) + 2 * sizeof(u16) + count * sizeof(s15Fixed16Number))
        return Error::from_string_literal("ICC::Profile: parametricCurveType has not enough data for parameters");

    auto* raw_parameters = bit_cast<BigEndian<s15Fixed16Number> const*>(bytes.data() + 12);
    Array<S15Fixed16, 7> parameters;
    parameters.fill(0);
    for (unsigned i = 0; i < count; ++i)
        parameters[i] = S15Fixed16::create_raw(raw_parameters[i]);

    return adopt_ref(*new ParametricCurveTagData(offset, size, function_type, move(parameters)));
}

ErrorOr<NonnullRefPtr<S15Fixed16ArrayTagData>> S15Fixed16ArrayTagData::from_bytes(ReadonlyBytes bytes, u32 offset, u32 size)
{
    // ICC v4, 10.22 s15Fixed16ArrayType
    VERIFY(tag_type(bytes) == Type);
    TRY(check_reserved(bytes));

    // "This type represents an array of generic 4-byte (32-bit) fixed point quantity. The number of values is determined
    //  from the size of the tag."
    size_t byte_size = bytes.size() - 8;
    if (byte_size % sizeof(s15Fixed16Number) != 0)
        return Error::from_string_literal("ICC::Profile: s15Fixed16ArrayType has wrong size");

    size_t count = byte_size / sizeof(s15Fixed16Number);
    auto* raw_values = bit_cast<BigEndian<s15Fixed16Number> const*>(bytes.data() + 8);
    Vector<S15Fixed16, 9> values;
    TRY(values.try_resize(count));
    for (size_t i = 0; i < count; ++i)
        values[i] = S15Fixed16::create_raw(raw_values[i]);

    return adopt_ref(*new S15Fixed16ArrayTagData(offset, size, move(values)));
}

ErrorOr<NonnullRefPtr<TextDescriptionTagData>> TextDescriptionTagData::from_bytes(ReadonlyBytes bytes, u32 offset, u32 size)
{
    // ICC v2, 6.5.17 textDescriptionType
    // textDescriptionType is no longer in the V4 spec.
    // In both the V2 and V4 specs, 'desc' is a required tag. In V4, it has type multiLocalizedUnicodeType,
    // but in V2 it has type textDescriptionType. Since 'desc' is required, this type is present in every
    // V2 icc file, and there are still many V2 files in use. So textDescriptionType is here to stay for now.
    // It's a very 90s type, preceding universal adoption of Unicode.

    // "The textDescriptionType is a complex structure that contains three types of text description structures:
    //  7-bit ASCII, Unicode and ScriptCode. Since no single standard method for specifying localizable character
    //  sets exists across the major platform vendors, including all three provides access for the major operating
    //  systems. The 7-bit ASCII description is to be an invariant, nonlocalizable name for consistent reference.
    //  It is preferred that both the Unicode and ScriptCode structures be properly localized."

    VERIFY(tag_type(bytes) == Type);
    TRY(check_reserved(bytes));

    // 7-bit ASCII

    // "ASCII: The count is the length of the string in bytes including the null terminator."
    if (bytes.size() < 3 * sizeof(u32))
        return Error::from_string_literal("ICC::Profile: textDescriptionType has not enough data for ASCII size");
    u32 ascii_description_length = *bit_cast<BigEndian<u32> const*>(bytes.data() + 8);

    if (bytes.size() < 3 * sizeof(u32) + ascii_description_length)
        return Error::from_string_literal("ICC::Profile: textDescriptionType has not enough data for ASCII description");

    u8 const* ascii_description_data = bytes.data() + 3 * sizeof(u32);
    for (u32 i = 0; i < ascii_description_length; ++i) {
        if (ascii_description_data[i] >= 128)
            return Error::from_string_literal("ICC::Profile: textDescriptionType ASCII description not 7-bit ASCII");
    }

    if (ascii_description_length == 0)
        return Error::from_string_literal("ICC::Profile: textDescriptionType ASCII description length does not include trailing \\0");

    if (ascii_description_data[ascii_description_length - 1] != '\0')
        return Error::from_string_literal("ICC::Profile: textDescriptionType ASCII description not \\0-terminated");

    StringView ascii_description { ascii_description_data, ascii_description_length - 1 };

    // Unicode

    if (bytes.size() < 3 * sizeof(u32) + ascii_description_length + 2 * sizeof(u32))
        return Error::from_string_literal("ICC::Profile: textDescriptionType has not enough data for Unicode metadata");

    // "Because the Unicode language code and Unicode count immediately follow the ASCII description,
    //  their alignment is not correct when the ASCII count is not a multiple of four"
    // So we can't use BigEndian<u32> here.
    u8 const* cursor = ascii_description_data + ascii_description_length;
    u32 unicode_language_code = (u32)(cursor[0] << 24) | (u32)(cursor[1] << 16) | (u32)(cursor[2] << 8) | (u32)cursor[3];
    cursor += 4;

    // "Unicode: The count is the number of characters including a Unicode null where a character is always two bytes."
    // This implies UCS-2.
    u32 unicode_description_length = (u32)(cursor[0] << 24) | (u32)(cursor[1] << 16) | (u32)(cursor[2] << 8) | (u32)cursor[3];
    cursor += 4;

    if (bytes.size() < 3 * sizeof(u32) + ascii_description_length + 2 * sizeof(u32) + 2 * unicode_description_length)
        return Error::from_string_literal("ICC::Profile: textDescriptionType has not enough data for Unicode description");

    u8 const* unicode_description_data = cursor;
    cursor += 2 * unicode_description_length;
    for (u32 i = 0; i < unicode_description_length; ++i) {
        u16 code_point = (u16)(unicode_description_data[2 * i] << 8) | (u16)unicode_description_data[2 * i + 1];
        if (is_unicode_surrogate(code_point))
            return Error::from_string_literal("ICC::Profile: textDescriptionType Unicode description is not valid UCS-2");
    }

    // If Unicode is not native on the platform, then the Unicode language code and Unicode count should be
    // filled in as 0, with no data placed in the Unicode localizable profile description area.
    Optional<String> unicode_description;
    if (unicode_description_length > 0) {
        u16 last_code_point = (u16)(unicode_description_data[2 * (unicode_description_length - 1)] << 8) | (u16)unicode_description_data[2 * (unicode_description_length - 1) + 1];
        if (last_code_point != 0)
            return Error::from_string_literal("ICC::Profile: textDescriptionType Unicode description not \\0-terminated");

        StringView utf_16be_data { unicode_description_data, 2 * (unicode_description_length - 1) };
        unicode_description = TRY(String::from_deprecated_string(TextCodec::decoder_for("utf-16be")->to_utf8(utf_16be_data)));
    }

    // ScriptCode

    // What is a script code? It's an old, obsolete mac thing. It looks like it's documented in
    // https://developer.apple.com/library/archive/documentation/mac/pdf/Text.pdf
    // "Script Codes, Language Codes, and Region Codes 1", PDF page 82.
    // I haven't found a complete explanation though. PDF page 84 suggests that:
    // - There are 16 script codes
    // - 0 is Roman, 1 is Japanese, 2 is Chinese, 3 is Korean, 9 is Devanagari
    // Roman uses https://en.wikipedia.org/wiki/Mac_OS_Roman as encoding (also on page 89),
    // and "All non-Roman script systems include Roman as a subscript" (page 87).

    // Aha, "Script Codes 6" on page 676 has the complete list! There are 32 of them.
    // The document mentions that each script code possibly has its own encoding, but I haven't found
    // details on the encodings for script codes other than 0 (which uses Mac OS Roman).
    // http://www.kreativekorp.com/charset/encoding/ has an unofficial list of old Mac OS encodings,
    // but it's not clear to me which script codes map to which encoding.

    // From here on, quotes are from the ICC spec on textDescriptionType again.

    // "The ScriptCode code is misaligned when the ASCII count is odd."
    // So don't use BigEndian<u16> here.
    u16 scriptcode_code = (u16)(cursor[0] << 8) | (u32)cursor[1];
    cursor += 2;

    // "ScriptCode: The count is the length of the string in bytes including the terminating null."
    u8 macintosh_description_length = *cursor;
    cursor += 1;

    if (macintosh_description_length > 67)
        return Error::from_string_literal("ICC::Profile: textDescriptionType ScriptCode description too long");

    u8 const* macintosh_description_data = cursor;

    // "If Scriptcode is not native on the platform, then the ScriptCode code and ScriptCode count should be filled
    // in as 0. The 67-byte localizable Macintosh profile description should be filled with 0’s."
    Optional<String> macintosh_description;
    if (macintosh_description_length > 0) {
        // ScriptCode is old-timey and a complicated to fully support. Lightroom Classic does write the ScriptCode section of textDescriptionType.
        // But supporting only ASCII MacRoman is good enough for those files, and easy to implement, so let's do only that for now.
        if (scriptcode_code == 0) { // MacRoman
            if (macintosh_description_data[macintosh_description_length - 1] != '\0')
                return Error::from_string_literal("ICC::Profile: textDescriptionType ScriptCode not \\0-terminated");

            macintosh_description = TRY(String::from_deprecated_string(TextCodec::decoder_for("x-mac-roman")->to_utf8({ macintosh_description_data, (size_t)macintosh_description_length - 1 })));
        } else {
            dbgln("TODO: ICCProfile textDescriptionType ScriptCode {}, length {}", scriptcode_code, macintosh_description_length);
        }
    }

    return adopt_ref(*new TextDescriptionTagData(offset, size, TRY(String::from_utf8(ascii_description)), unicode_language_code, move(unicode_description), move(macintosh_description)));
}

ErrorOr<NonnullRefPtr<TextTagData>> TextTagData::from_bytes(ReadonlyBytes bytes, u32 offset, u32 size)
{
    // ICC v4, 10.24 textType
    VERIFY(tag_type(bytes) == Type);
    TRY(check_reserved(bytes));

    // "The textType is a simple text structure that contains a 7-bit ASCII text string. The length of the string is obtained
    //  by subtracting 8 from the element size portion of the tag itself. This string shall be terminated with a 00h byte."
    u32 length = bytes.size() - 8;

    u8 const* text_data = bytes.data() + 8;
    for (u32 i = 0; i < length; ++i) {
        if (text_data[i] >= 128)
            return Error::from_string_literal("ICC::Profile: textType data not 7-bit ASCII");
    }

    if (length == 0)
        return Error::from_string_literal("ICC::Profile: textType too short for \\0 byte");

    if (text_data[length - 1] != '\0')
        return Error::from_string_literal("ICC::Profile: textType data not \\0-terminated");

    return adopt_ref(*new TextTagData(offset, size, TRY(String::from_utf8(StringView(text_data, length - 1)))));
}

ErrorOr<NonnullRefPtr<XYZTagData>> XYZTagData::from_bytes(ReadonlyBytes bytes, u32 offset, u32 size)
{
    // ICC v4, 10.31 XYZType
    VERIFY(tag_type(bytes) == Type);
    TRY(check_reserved(bytes));

    // "The XYZType contains an array of three encoded values for PCSXYZ, CIEXYZ, or nCIEXYZ values. The
    //  number of sets of values is determined from the size of the tag."
    size_t byte_size = bytes.size() - 8;
    if (byte_size % sizeof(XYZNumber) != 0)
        return Error::from_string_literal("ICC::Profile: XYZType has wrong size");

    size_t xyz_count = byte_size / sizeof(XYZNumber);
    auto* raw_xyzs = bit_cast<XYZNumber const*>(bytes.data() + 8);
    Vector<XYZ, 1> xyzs;
    TRY(xyzs.try_resize(xyz_count));
    for (size_t i = 0; i < xyz_count; ++i)
        xyzs[i] = (XYZ)raw_xyzs[i];

    return adopt_ref(*new XYZTagData(offset, size, move(xyzs)));
}

}
