/*
 * Copyright (c) 2022, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Endian.h>
#include <LibGfx/ICCProfile.h>
#include <LibTextCodec/Decoder.h>
#include <math.h>
#include <time.h>

// V2 spec: https://color.org/specification/ICC.1-2001-04.pdf
// V4 spec: https://color.org/specification/ICC.1-2022-05.pdf

namespace Gfx::ICC {

namespace {

// ICC V4, 4.2 dateTimeNumber
// "All the dateTimeNumber values in a profile shall be in Coordinated Universal Time [...]."
struct DateTimeNumber {
    BigEndian<u16> year;
    BigEndian<u16> month;
    BigEndian<u16> day;
    BigEndian<u16> hours;
    BigEndian<u16> minutes;
    BigEndian<u16> seconds;
};

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

ErrorOr<time_t> parse_date_time_number(DateTimeNumber const& date_time)
{
    // ICC V4, 4.2 dateTimeNumber

    // "Number of the month (1 to 12)"
    if (date_time.month < 1 || date_time.month > 12)
        return Error::from_string_literal("ICC::Profile: dateTimeNumber month out of bounds");

    // "Number of the day of the month (1 to 31)"
    if (date_time.day < 1 || date_time.day > 31)
        return Error::from_string_literal("ICC::Profile: dateTimeNumber day out of bounds");

    // "Number of hours (0 to 23)"
    if (date_time.hours > 23)
        return Error::from_string_literal("ICC::Profile: dateTimeNumber hours out of bounds");

    // "Number of minutes (0 to 59)"
    if (date_time.minutes > 59)
        return Error::from_string_literal("ICC::Profile: dateTimeNumber minutes out of bounds");

    // "Number of seconds (0 to 59)"
    // ICC profiles apparently can't be created during leap seconds (seconds would be 60 there, but the spec doesn't allow that).
    if (date_time.seconds > 59)
        return Error::from_string_literal("ICC::Profile: dateTimeNumber seconds out of bounds");

    struct tm tm = {};
    tm.tm_year = date_time.year - 1900;
    tm.tm_mon = date_time.month - 1;
    tm.tm_mday = date_time.day;
    tm.tm_hour = date_time.hours;
    tm.tm_min = date_time.minutes;
    tm.tm_sec = date_time.seconds;
    // timegm() doesn't read tm.tm_isdst, tm.tm_wday, and tm.tm_yday, no need to fill them in.

    time_t timestamp = timegm(&tm);
    if (timestamp == -1)
        return Error::from_string_literal("ICC::Profile: dateTimeNumber not representable as timestamp");

    return timestamp;
}

// ICC V4, 7.2 Profile header
struct ICCHeader {
    BigEndian<u32> profile_size;
    BigEndian<PreferredCMMType> preferred_cmm_type;

    u8 profile_version_major;
    u8 profile_version_minor_bugfix;
    BigEndian<u16> profile_version_zero;

    BigEndian<DeviceClass> profile_device_class;
    BigEndian<ColorSpace> data_color_space;
    BigEndian<ColorSpace> profile_connection_space; // "PCS" in the spec.

    DateTimeNumber profile_creation_time;

    BigEndian<u32> profile_file_signature;
    BigEndian<PrimaryPlatform> primary_platform;

    BigEndian<u32> profile_flags;
    BigEndian<DeviceManufacturer> device_manufacturer;
    BigEndian<DeviceModel> device_model;
    BigEndian<u64> device_attributes;
    BigEndian<u32> rendering_intent;

    XYZNumber pcs_illuminant;

    BigEndian<Creator> profile_creator;

    u8 profile_id[16];
    u8 reserved[28];
};
static_assert(sizeof(ICCHeader) == 128);
}

// ICC V4, 7.3 Tag table, Table 24 - Tag table structure
struct Detail::TagTableEntry {
    BigEndian<TagSignature> tag_signature;
    BigEndian<u32> offset_to_beginning_of_tag_data_element;
    BigEndian<u32> size_of_tag_data_element;
};
static_assert(sizeof(Detail::TagTableEntry) == 12);

namespace {
ErrorOr<u32> parse_size(ICCHeader const& header, ReadonlyBytes icc_bytes)
{
    // ICC v4, 7.2.2 Profile size field
    // "The value in the profile size field shall be the exact size obtained by combining the profile header,
    // the tag table, and the tagged element data, including the pad bytes for the last tag."

    // Valid files have enough data for profile header and tag table entry count.
    if (header.profile_size < sizeof(ICCHeader) + sizeof(u32))
        return Error::from_string_literal("ICC::Profile: Profile size too small");

    if (header.profile_size > icc_bytes.size())
        return Error::from_string_literal("ICC::Profile: Profile size larger than input data");

    return header.profile_size;
}

Optional<PreferredCMMType> parse_preferred_cmm_type(ICCHeader const& header)
{
    // ICC v4, 7.2.3 Preferred CMM type field

    // "This field may be used to identify the preferred CMM to be used.
    //  If used, it shall match a CMM type signature registered in the ICC Tag Registry"
    // https://www.color.org/signatures2.xalter currently links to
    // https://www.color.org/registry/signature/TagRegistry-2021-03.pdf, which contains
    // some CMM signatures.
    // This requirement is often honored in practice, but not always. For example,
    // JPEGs exported in Adobe Lightroom contain profiles that set this to 'Lino',
    // which is not present in the "CMM Signatures" table in that PDF.

    // "If no preferred CMM is identified, this field shall be set to zero (00000000h)."
    if (header.preferred_cmm_type == PreferredCMMType { 0 })
        return {};
    return header.preferred_cmm_type;
}

ErrorOr<Version> parse_version(ICCHeader const& header)
{
    // ICC v4, 7.2.4 Profile version field
    if (header.profile_version_zero != 0)
        return Error::from_string_literal("ICC::Profile: Reserved version bytes not zero");
    return Version(header.profile_version_major, header.profile_version_minor_bugfix);
}

ErrorOr<DeviceClass> parse_device_class(ICCHeader const& header)
{
    // ICC v4, 7.2.5 Profile/device class field
    switch (header.profile_device_class) {
    case DeviceClass::InputDevce:
    case DeviceClass::DisplayDevice:
    case DeviceClass::OutputDevice:
    case DeviceClass::DeviceLink:
    case DeviceClass::ColorSpace:
    case DeviceClass::Abstract:
    case DeviceClass::NamedColor:
        return header.profile_device_class;
    }
    return Error::from_string_literal("ICC::Profile: Invalid device class");
}

ErrorOr<ColorSpace> parse_color_space(ColorSpace color_space)
{
    // ICC v4, Table 19 — Data colour space signatures
    switch (color_space) {
    case ColorSpace::nCIEXYZ:
    case ColorSpace::CIELAB:
    case ColorSpace::CIELUV:
    case ColorSpace::YCbCr:
    case ColorSpace::CIEYxy:
    case ColorSpace::RGB:
    case ColorSpace::Gray:
    case ColorSpace::HSV:
    case ColorSpace::HLS:
    case ColorSpace::CMYK:
    case ColorSpace::CMY:
    case ColorSpace::TwoColor:
    case ColorSpace::ThreeColor:
    case ColorSpace::FourColor:
    case ColorSpace::FiveColor:
    case ColorSpace::SixColor:
    case ColorSpace::SevenColor:
    case ColorSpace::EightColor:
    case ColorSpace::NineColor:
    case ColorSpace::TenColor:
    case ColorSpace::ElevenColor:
    case ColorSpace::TwelveColor:
    case ColorSpace::ThirteenColor:
    case ColorSpace::FourteenColor:
    case ColorSpace::FifteenColor:
        return color_space;
    }
    return Error::from_string_literal("ICC::Profile: Invalid color space");
}

ErrorOr<ColorSpace> parse_data_color_space(ICCHeader const& header)
{
    // ICC v4, 7.2.6 Data colour space field
    return parse_color_space(header.data_color_space);
}

ErrorOr<ColorSpace> parse_connection_space(ICCHeader const& header)
{
    // ICC v4, 7.2.7 PCS field
    //         and Annex D
    auto space = TRY(parse_color_space(header.profile_connection_space));

    if (header.profile_device_class != DeviceClass::DeviceLink && (space != ColorSpace::PCSXYZ && space != ColorSpace::PCSLAB))
        return Error::from_string_literal("ICC::Profile: Invalid profile connection space: Non-PCS space on non-DeviceLink profile");

    return space;
}

ErrorOr<time_t> parse_creation_date_time(ICCHeader const& header)
{
    // ICC v4, 7.2.8 Date and time field
    return parse_date_time_number(header.profile_creation_time);
}

ErrorOr<void> parse_file_signature(ICCHeader const& header)
{
    // ICC v4, 7.2.9 Profile file signature field
    // "The profile file signature field shall contain the value “acsp” (61637370h) as a profile file signature."
    if (header.profile_file_signature != 0x61637370)
        return Error::from_string_literal("ICC::Profile: profile file signature not 'acsp'");
    return {};
}

ErrorOr<PrimaryPlatform> parse_primary_platform(ICCHeader const& header)
{
    // ICC v4, 7.2.10 Primary platform field
    switch (header.primary_platform) {
    case PrimaryPlatform::Apple:
    case PrimaryPlatform::Microsoft:
    case PrimaryPlatform::SiliconGraphics:
    case PrimaryPlatform::Sun:
        return header.primary_platform;
    }
    return Error::from_string_literal("ICC::Profile: Invalid primary platform");
}

Optional<DeviceManufacturer> parse_device_manufacturer(ICCHeader const& header)
{
    // ICC v4, 7.2.12 Device manufacturer field
    // "This field may be used to identify a device manufacturer.
    //  If used the signature shall match the signature contained in the appropriate section of the ICC signature registry found at www.color.org"
    // Device manufacturers can be looked up at https://www.color.org/signatureRegistry/index.xalter
    // For example: https://www.color.org/signatureRegistry/?entityEntry=APPL-4150504C
    // Some icc files use codes not in that registry. For example. D50_XYZ.icc from https://www.color.org/XYZprofiles.xalter
    // has its device manufacturer set to 'none', but https://www.color.org/signatureRegistry/?entityEntry=none-6E6F6E65 does not exist.

    // "If not used this field shall be set to zero (00000000h)."
    if (header.device_manufacturer == DeviceManufacturer { 0 })
        return {};
    return header.device_manufacturer;
}

Optional<DeviceModel> parse_device_model(ICCHeader const& header)
{
    // ICC v4, 7.2.13 Device model field
    // "This field may be used to identify a device model.
    //  If used the signature shall match the signature contained in the appropriate section of the ICC signature registry found at www.color.org"
    // Device models can be looked up at https://www.color.org/signatureRegistry/deviceRegistry/index.xalter
    // For example: https://www.color.org/signatureRegistry/deviceRegistry/?entityEntry=7FD8-37464438
    // Some icc files use codes not in that registry. For example. D50_XYZ.icc from https://www.color.org/XYZprofiles.xalter
    // has its device model set to 'none', but https://www.color.org/signatureRegistry/deviceRegistry?entityEntry=none-6E6F6E65 does not exist.

    // "If not used this field shall be set to zero (00000000h)."
    if (header.device_model == DeviceModel { 0 })
        return {};
    return header.device_model;
}

ErrorOr<DeviceAttributes> parse_device_attributes(ICCHeader const& header)
{
    // ICC v4, 7.2.14 Device attributes field

    // "4 to 31": "Reserved (set to binary zero)"
    if (header.device_attributes & 0xffff'fff0)
        return Error::from_string_literal("ICC::Profile: Device attributes reserved bits not set to 0");

    return DeviceAttributes { header.device_attributes };
}

ErrorOr<RenderingIntent> parse_rendering_intent(ICCHeader const& header)
{
    // ICC v4, 7.2.15 Rendering intent field
    switch (header.rendering_intent) {
    case 0:
        return RenderingIntent::Perceptual;
    case 1:
        return RenderingIntent::MediaRelativeColorimetric;
    case 2:
        return RenderingIntent::Saturation;
    case 3:
        return RenderingIntent::ICCAbsoluteColorimetric;
    }
    return Error::from_string_literal("ICC::Profile: Invalid rendering intent");
}

ErrorOr<XYZ> parse_pcs_illuminant(ICCHeader const& header)
{
    // ICC v4, 7.2.16 PCS illuminant field
    XYZ xyz = (XYZ)header.pcs_illuminant;

    /// "The value, when rounded to four decimals, shall be X = 0,9642, Y = 1,0 and Z = 0,8249."
    if (round(xyz.x * 10'000) != 9'642 || round(xyz.y * 10'000) != 10'000 || round(xyz.z * 10'000) != 8'249)
        return Error::from_string_literal("ICC::Profile: Invalid pcs illuminant");

    return xyz;
}

Optional<Creator> parse_profile_creator(ICCHeader const& header)
{
    // ICC v4, 7.2.17 Profile creator field
    // "This field may be used to identify the creator of the profile.
    //  If used the signature should match the signature contained in the device manufacturer section of the ICC signature registry found at www.color.org."
    // This is not always true in practice.
    // For example, .icc files in /System/ColorSync/Profiles on macOS 12.6 set this to 'appl', which is a CMM signature, not a device signature (that one would be 'APPL').

    // "If not used this field shall be set to zero (00000000h)."
    if (header.profile_creator == Creator { 0 })
        return {};
    return header.profile_creator;
}

template<size_t N>
bool all_bytes_are_zero(const u8 (&bytes)[N])
{
    for (u8 byte : bytes) {
        if (byte != 0)
            return false;
    }
    return true;
}

ErrorOr<Optional<Crypto::Hash::MD5::DigestType>> parse_profile_id(ICCHeader const& header, ReadonlyBytes icc_bytes)
{
    // ICC v4, 7.2.18 Profile ID field
    // "A profile ID field value of zero (00h) shall indicate that a profile ID has not been calculated."
    if (all_bytes_are_zero(header.profile_id))
        return OptionalNone {};

    Crypto::Hash::MD5::DigestType id;
    static_assert(sizeof(id.data) == sizeof(header.profile_id));
    memcpy(id.data, header.profile_id, sizeof(id.data));

    auto computed_id = Profile::compute_id(icc_bytes);
    if (id != computed_id)
        return Error::from_string_literal("ICC::Profile: Invalid profile id");

    return id;
}

ErrorOr<void> parse_reserved(ICCHeader const& header)
{
    // ICC v4, 7.2.19 Reserved field
    // "This field of the profile header is reserved for future ICC definition and shall be set to zero."
    if (!all_bytes_are_zero(header.reserved))
        return Error::from_string_literal("ICC::Profile: Reserved header bytes are not zero");
    return {};
}
}

URL device_manufacturer_url(DeviceManufacturer device_manufacturer)
{
    return URL(DeprecatedString::formatted("https://www.color.org/signatureRegistry/?entityEntry={:c}{:c}{:c}{:c}-{:08X}",
        device_manufacturer.c0(), device_manufacturer.c1(), device_manufacturer.c2(), device_manufacturer.c3(), device_manufacturer.value));
}

URL device_model_url(DeviceModel device_model)
{
    return URL(DeprecatedString::formatted("https://www.color.org/signatureRegistry/deviceRegistry/?entityEntry={:c}{:c}{:c}{:c}-{:08X}",
        device_model.c0(), device_model.c1(), device_model.c2(), device_model.c3(), device_model.value));
}

StringView device_class_name(DeviceClass device_class)
{
    switch (device_class) {
    case DeviceClass::InputDevce:
        return "InputDevce"sv;
    case DeviceClass::DisplayDevice:
        return "DisplayDevice"sv;
    case DeviceClass::OutputDevice:
        return "OutputDevice"sv;
    case DeviceClass::DeviceLink:
        return "DeviceLink"sv;
    case DeviceClass::ColorSpace:
        return "ColorSpace"sv;
    case DeviceClass::Abstract:
        return "Abstract"sv;
    case DeviceClass::NamedColor:
        return "NamedColor"sv;
    }
    VERIFY_NOT_REACHED();
}

StringView data_color_space_name(ColorSpace color_space)
{
    switch (color_space) {
    case ColorSpace::nCIEXYZ:
        return "nCIEXYZ"sv;
    case ColorSpace::CIELAB:
        return "CIELAB"sv;
    case ColorSpace::CIELUV:
        return "CIELUV"sv;
    case ColorSpace::YCbCr:
        return "YCbCr"sv;
    case ColorSpace::CIEYxy:
        return "CIEYxy"sv;
    case ColorSpace::RGB:
        return "RGB"sv;
    case ColorSpace::Gray:
        return "Gray"sv;
    case ColorSpace::HSV:
        return "HSV"sv;
    case ColorSpace::HLS:
        return "HLS"sv;
    case ColorSpace::CMYK:
        return "CMYK"sv;
    case ColorSpace::CMY:
        return "CMY"sv;
    case ColorSpace::TwoColor:
        return "2 color"sv;
    case ColorSpace::ThreeColor:
        return "3 color (other than XYZ, Lab, Luv, YCbCr, CIEYxy, RGB, HSV, HLS, CMY)"sv;
    case ColorSpace::FourColor:
        return "4 color (other than CMYK)"sv;
    case ColorSpace::FiveColor:
        return "5 color"sv;
    case ColorSpace::SixColor:
        return "6 color"sv;
    case ColorSpace::SevenColor:
        return "7 color"sv;
    case ColorSpace::EightColor:
        return "8 color"sv;
    case ColorSpace::NineColor:
        return "9 color"sv;
    case ColorSpace::TenColor:
        return "10 color"sv;
    case ColorSpace::ElevenColor:
        return "11 color"sv;
    case ColorSpace::TwelveColor:
        return "12 color"sv;
    case ColorSpace::ThirteenColor:
        return "13 color"sv;
    case ColorSpace::FourteenColor:
        return "14 color"sv;
    case ColorSpace::FifteenColor:
        return "15 color"sv;
    }
    VERIFY_NOT_REACHED();
}

StringView profile_connection_space_name(ColorSpace color_space)
{
    switch (color_space) {
    case ColorSpace::PCSXYZ:
        return "PCSXYZ"sv;
    case ColorSpace::PCSLAB:
        return "PCSLAB"sv;
    default:
        return data_color_space_name(color_space);
    }
}

StringView primary_platform_name(PrimaryPlatform primary_platform)
{
    switch (primary_platform) {
    case PrimaryPlatform::Apple:
        return "Apple"sv;
    case PrimaryPlatform::Microsoft:
        return "Microsoft"sv;
    case PrimaryPlatform::SiliconGraphics:
        return "Silicon Graphics"sv;
    case PrimaryPlatform::Sun:
        return "Sun"sv;
    }
    VERIFY_NOT_REACHED();
}

StringView rendering_intent_name(RenderingIntent rendering_intent)
{
    switch (rendering_intent) {
    case RenderingIntent::Perceptual:
        return "Perceptual"sv;
    case RenderingIntent::MediaRelativeColorimetric:
        return "Media-relative colorimetric"sv;
    case RenderingIntent::Saturation:
        return "Saturation"sv;
    case RenderingIntent::ICCAbsoluteColorimetric:
        return "ICC-absolute colorimetric"sv;
    }
    VERIFY_NOT_REACHED();
}

Flags::Flags() = default;
Flags::Flags(u32 bits)
    : m_bits(bits)
{
}

DeviceAttributes::DeviceAttributes() = default;
DeviceAttributes::DeviceAttributes(u64 bits)
    : m_bits(bits)
{
}

static TagTypeSignature tag_type(ReadonlyBytes tag_bytes)
{
    VERIFY(tag_bytes.size() >= sizeof(u32));
    return *bit_cast<BigEndian<TagTypeSignature> const*>(tag_bytes.data());
}

static ErrorOr<void> check_reserved(ReadonlyBytes tag_bytes)
{
    if (tag_bytes.size() < 2 * sizeof(u32))
        return Error::from_string_literal("ICC::Profile: Not enough data for tag reserved field");

    if (*bit_cast<BigEndian<u32> const*>(tag_bytes.data() + sizeof(u32)) != 0)
        return Error::from_string_literal("ICC::Profile: tag reserved field not 0");

    return {};
}

ErrorOr<NonnullRefPtr<MultiLocalizedUnicodeTagData>> MultiLocalizedUnicodeTagData::from_bytes(ReadonlyBytes bytes, u32 offset, u32 size)
{
    // ICC v4, 10.15 multiLocalizedUnicodeType
    VERIFY(tag_type(bytes) == MultiLocalizedUnicodeTagData::Type);
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

    VERIFY(tag_type(bytes) == TextDescriptionTagData::Type);
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

            bool is_ascii = true;
            for (u32 i = 0; i < macintosh_description_length; ++i) {
                if (macintosh_description_data[i] >= 128)
                    is_ascii = false;
            }

            if (is_ascii) {
                macintosh_description = TRY(String::from_utf8(StringView { macintosh_description_data, (size_t)macintosh_description_length - 1 }));
            } else {
                dbgln("TODO: ICCProfile textDescriptionType non-ASCII MacRoman");
            }
        } else {
            dbgln("TODO: ICCProfile textDescriptionType ScriptCode {}, length {}", scriptcode_code, macintosh_description_length);
        }
    }

    return adopt_ref(*new TextDescriptionTagData(offset, size, TRY(String::from_utf8(ascii_description)), unicode_language_code, move(unicode_description), move(macintosh_description)));
}

ErrorOr<NonnullRefPtr<TextTagData>> TextTagData::from_bytes(ReadonlyBytes bytes, u32 offset, u32 size)
{
    // ICC v4, 10.24 textType
    VERIFY(tag_type(bytes) == TextTagData::Type);
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
    VERIFY(tag_type(bytes) == XYZTagData::Type);
    TRY(check_reserved(bytes));

    // "The XYZType contains an array of three encoded values for PCSXYZ, CIEXYZ, or nCIEXYZ values. The
    //  number of sets of values is determined from the size of the tag."
    size_t byte_size = bytes.size() - 8;
    if (byte_size % sizeof(XYZNumber) != 0)
        return Error::from_string_literal("ICC::Profile: XYZType wrong size");

    size_t xyz_count = byte_size / sizeof(XYZNumber);
    XYZNumber const* raw_xyzs = bit_cast<XYZNumber const*>(bytes.data() + 8);
    Vector<XYZ, 1> xyzs;
    TRY(xyzs.try_resize(xyz_count));
    for (size_t i = 0; i < xyz_count; ++i)
        xyzs[i] = (XYZ)raw_xyzs[i];

    return adopt_ref(*new XYZTagData(offset, size, move(xyzs)));
}

ErrorOr<void> Profile::read_header(ReadonlyBytes bytes)
{
    if (bytes.size() < sizeof(ICCHeader))
        return Error::from_string_literal("ICC::Profile: Not enough data for header");

    auto header = *bit_cast<ICCHeader const*>(bytes.data());

    TRY(parse_file_signature(header));
    m_on_disk_size = TRY(parse_size(header, bytes));
    m_preferred_cmm_type = parse_preferred_cmm_type(header);
    m_version = TRY(parse_version(header));
    m_device_class = TRY(parse_device_class(header));
    m_data_color_space = TRY(parse_data_color_space(header));
    m_connection_space = TRY(parse_connection_space(header));
    m_creation_timestamp = TRY(parse_creation_date_time(header));
    m_primary_platform = TRY(parse_primary_platform(header));
    m_flags = Flags { header.profile_flags };
    m_device_manufacturer = parse_device_manufacturer(header);
    m_device_model = parse_device_model(header);
    m_device_attributes = TRY(parse_device_attributes(header));
    m_rendering_intent = TRY(parse_rendering_intent(header));
    m_pcs_illuminant = TRY(parse_pcs_illuminant(header));
    m_creator = parse_profile_creator(header);
    m_id = TRY(parse_profile_id(header, bytes));
    TRY(parse_reserved(header));

    return {};
}

ErrorOr<NonnullRefPtr<TagData>> Profile::read_tag(ReadonlyBytes bytes, Detail::TagTableEntry const& entry)
{
    if (entry.offset_to_beginning_of_tag_data_element + entry.size_of_tag_data_element > bytes.size())
        return Error::from_string_literal("ICC::Profile: Tag data out of bounds");

    auto tag_bytes = bytes.slice(entry.offset_to_beginning_of_tag_data_element, entry.size_of_tag_data_element);

    // ICC v4, 9 Tag definitions
    // ICC v4, 9.1 General
    // "All tags, including private tags, have as their first four bytes a tag signature to identify to profile readers
    //  what kind of data is contained within a tag."
    if (tag_bytes.size() < sizeof(u32))
        return Error::from_string_literal("ICC::Profile: Not enough data for tag type");

    auto type = tag_type(tag_bytes);
    switch (type) {
    case MultiLocalizedUnicodeTagData::Type:
        return MultiLocalizedUnicodeTagData::from_bytes(tag_bytes, entry.offset_to_beginning_of_tag_data_element, entry.size_of_tag_data_element);
    case TextDescriptionTagData::Type:
        return TextDescriptionTagData::from_bytes(tag_bytes, entry.offset_to_beginning_of_tag_data_element, entry.size_of_tag_data_element);
    case TextTagData::Type:
        return TextTagData::from_bytes(tag_bytes, entry.offset_to_beginning_of_tag_data_element, entry.size_of_tag_data_element);
    case XYZTagData::Type:
        return XYZTagData::from_bytes(tag_bytes, entry.offset_to_beginning_of_tag_data_element, entry.size_of_tag_data_element);
    default:
        // FIXME: optionally ignore tags of unknown type
        return adopt_ref(*new UnknownTagData(entry.offset_to_beginning_of_tag_data_element, entry.size_of_tag_data_element, type));
    }
}

ErrorOr<void> Profile::read_tag_table(ReadonlyBytes bytes)
{
    // ICC v4, 7.3 Tag table
    // ICC v4, 7.3.1 Overview
    // "The tag table acts as a table of contents for the tags and an index into the tag data element in the profiles. It
    //  shall consist of a 4-byte entry that contains a count of the number of tags in the table followed by a series of 12-
    //  byte entries with one entry for each tag. The tag table therefore contains 4+12n bytes where n is the number of
    //  tags contained in the profile. The entries for the tags within the table are not required to be in any particular
    //  order nor are they required to match the sequence of tag data element within the profile.
    //  Each 12-byte tag entry following the tag count shall consist of a 4-byte tag signature, a 4-byte offset to define
    //  the beginning of the tag data element, and a 4-byte entry identifying the length of the tag data element in bytes.
    //  [...]
    //  The tag table shall define a contiguous sequence of unique tag elements, with no gaps between the last byte
    //  of any tag data element referenced from the tag table (inclusive of any necessary additional pad bytes required
    //  to reach a four-byte boundary) and the byte offset of the following tag element, or the end of the file.
    //  Duplicate tag signatures shall not be included in the tag table.
    //  Tag data elements shall not partially overlap, so there shall be no part of any tag data element that falls within
    //  the range defined for another tag in the tag table.
    //  The tag table may contain multiple tags signatures that all reference the same tag data element offset, allowing
    //  efficient reuse of tag data elements. In such cases, both the offset and size of the tag data elements in the tag
    //  table shall be the same."

    ReadonlyBytes tag_table_bytes = bytes.slice(sizeof(ICCHeader));

    if (tag_table_bytes.size() < sizeof(u32))
        return Error::from_string_literal("ICC::Profile: Not enough data for tag count");
    auto tag_count = *bit_cast<BigEndian<u32> const*>(tag_table_bytes.data());

    tag_table_bytes = tag_table_bytes.slice(sizeof(u32));
    if (tag_table_bytes.size() < tag_count * sizeof(Detail::TagTableEntry))
        return Error::from_string_literal("ICC::Profile: Not enough data for tag table entries");
    auto tag_table_entries = bit_cast<Detail::TagTableEntry const*>(tag_table_bytes.data());

    for (u32 i = 0; i < tag_count; ++i) {
        // FIXME: optionally ignore tags with unknown signature
        // FIXME: dedupe identical offset/sizes
        auto tag_data = TRY(read_tag(bytes, tag_table_entries[i]));
        // "Duplicate tag signatures shall not be included in the tag table."
        if (TRY(m_tag_table.try_set(tag_table_entries[i].tag_signature, move(tag_data))) != AK::HashSetResult::InsertedNewEntry)
            return Error::from_string_literal("ICC::Profile: duplicate tag signature");
    }

    return {};
}

ErrorOr<NonnullRefPtr<Profile>> Profile::try_load_from_externally_owned_memory(ReadonlyBytes bytes)
{
    auto profile = adopt_ref(*new Profile());
    TRY(profile->read_header(bytes));
    bytes = bytes.trim(profile->on_disk_size());
    TRY(profile->read_tag_table(bytes));

    return profile;
}

Crypto::Hash::MD5::DigestType Profile::compute_id(ReadonlyBytes bytes)
{
    // ICC v4, 7.2.18 Profile ID field
    // "The Profile ID shall be calculated using the MD5 fingerprinting method as defined in Internet RFC 1321.
    //  The entire profile, whose length is given by the size field in the header, with the
    //  profile flags field (bytes 44 to 47, see 7.2.11),
    //  rendering intent field (bytes 64 to 67, see 7.2.15),
    //  and profile ID field (bytes 84 to 99)
    //  in the profile header temporarily set to zeros (00h),
    //  shall be used to calculate the ID."
    const u8 zero[16] = {};
    Crypto::Hash::MD5 md5;
    md5.update(bytes.slice(0, 44));
    md5.update(ReadonlyBytes { zero, 4 }); // profile flags field
    md5.update(bytes.slice(48, 64 - 48));
    md5.update(ReadonlyBytes { zero, 4 }); // rendering intent field
    md5.update(bytes.slice(68, 84 - 68));
    md5.update(ReadonlyBytes { zero, 16 }); // profile ID field
    md5.update(bytes.slice(100));
    return md5.digest();
}

}
