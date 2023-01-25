/*
 * Copyright (c) 2022, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/FixedPoint.h>
#include <AK/Format.h>
#include <AK/HashMap.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <AK/Span.h>
#include <AK/String.h>
#include <AK/URL.h>
#include <LibCrypto/Hash/MD5.h>

namespace Gfx::ICC {

// The ICC spec uses FourCCs for many different things.
// This is used to give FourCCs for different roles distinct types, so that they can only be compared to the correct constants.
// (FourCCs that have only a small and fixed set of values should use an enum class instead, see e.g. DeviceClass and ColorSpace below.)
enum class FourCCType {
    PreferredCMMType,
    DeviceManufacturer,
    DeviceModel,
    Creator,
    TagSignature,
    TagTypeSignature,
};

template<FourCCType type>
struct [[gnu::packed]] DistinctFourCC {
    constexpr explicit DistinctFourCC(u32 value)
        : value(value)
    {
    }
    constexpr operator u32() const { return value; }

    char c0() const { return value >> 24; }
    char c1() const { return (value >> 16) & 0xff; }
    char c2() const { return (value >> 8) & 0xff; }
    char c3() const { return value & 0xff; }

    bool operator==(DistinctFourCC b) const { return value == b.value; }

    u32 value { 0 };
};

using PreferredCMMType = DistinctFourCC<FourCCType::PreferredCMMType>;     // ICC v4, "7.2.3 Preferred CMM type field"
using DeviceManufacturer = DistinctFourCC<FourCCType::DeviceManufacturer>; // ICC v4, "7.2.12 Device manufacturer field"
using DeviceModel = DistinctFourCC<FourCCType::DeviceModel>;               // ICC v4, "7.2.13 Device model field"
using Creator = DistinctFourCC<FourCCType::Creator>;                       // ICC v4, "7.2.17 Profile creator field"
using TagSignature = DistinctFourCC<FourCCType::TagSignature>;             // ICC v4, "9.2 Tag listing"
using TagTypeSignature = DistinctFourCC<FourCCType::TagTypeSignature>;     // ICC v4, "10 Tag type definitions"

URL device_manufacturer_url(DeviceManufacturer);
URL device_model_url(DeviceModel);

// ICC v4, 9.2 Tag listing
// FIXME: Add v2-only tags too.
#define ENUMERATE_TAG_SIGNATURES(TAG)                               \
    TAG(AToB0Tag, 0x41324230 /* 'A2B0' */)                          \
    TAG(AToB1Tag, 0x41324231 /* 'A2B1' */)                          \
    TAG(AToB2Tag, 0x41324232 /* 'A2B2' */)                          \
    TAG(blueMatrixColumnTag, 0x6258595A /* 'bXYZ' */)               \
    TAG(blueTRCTag, 0x62545243 /* 'bTRC' */)                        \
    TAG(BToA0Tag, 0x42324130 /* 'B2A0' */)                          \
    TAG(BToA1Tag, 0x42324131 /* 'B2A1' */)                          \
    TAG(BToA2Tag, 0x42324132 /* 'B2A2' */)                          \
    TAG(BToD0Tag, 0x42324430 /* 'B2D0' */)                          \
    TAG(BToD1Tag, 0x42324431 /* 'B2D1' */)                          \
    TAG(BToD2Tag, 0x42324432 /* 'B2D2' */)                          \
    TAG(BToD3Tag, 0x42324433 /* 'B2D3' */)                          \
    TAG(calibrationDateTimeTag, 0x63616C74 /* 'calt' */)            \
    TAG(charTargetTag, 0x74617267 /* 'targ' */)                     \
    TAG(chromaticAdaptationTag, 0x63686164 /* 'chad' */)            \
    TAG(chromaticityTag, 0x6368726D /* 'chrm' */)                   \
    TAG(cicpTag, 0x63696370 /* 'cicp' */)                           \
    TAG(colorantOrderTag, 0x636C726F /* 'clro' */)                  \
    TAG(colorantTableTag, 0x636C7274 /* 'clrt' */)                  \
    TAG(colorantTableOutTag, 0x636C6F74 /* 'clot' */)               \
    TAG(colorimetricIntentImageStateTag, 0x63696973 /* 'ciis' */)   \
    TAG(copyrightTag, 0x63707274 /* 'cprt' */)                      \
    TAG(deviceMfgDescTag, 0x646D6E64 /* 'dmnd' */)                  \
    TAG(deviceModelDescTag, 0x646D6464 /* 'dmdd' */)                \
    TAG(DToB0Tag, 0x44324230 /* 'D2B0' */)                          \
    TAG(DToB1Tag, 0x44324231 /* 'D2B1' */)                          \
    TAG(DToB2Tag, 0x44324232 /* 'D2B2' */)                          \
    TAG(DToB3Tag, 0x44324233 /* 'D2B3' */)                          \
    TAG(gamutTag, 0x67616D74 /* 'gamt' */)                          \
    TAG(grayTRCTag, 0x6B545243 /* 'kTRC' */)                        \
    TAG(greenMatrixColumnTag, 0x6758595A /* 'gXYZ' */)              \
    TAG(greenTRCTag, 0x67545243 /* 'gTRC' */)                       \
    TAG(luminanceTag, 0x6C756D69 /* 'lumi' */)                      \
    TAG(measurementTag, 0x6D656173 /* 'meas' */)                    \
    TAG(metadataTag, 0x6D657461 /* 'meta' */)                       \
    TAG(mediaWhitePointTag, 0x77747074 /* 'wtpt' */)                \
    TAG(namedColor2Tag, 0x6E636C32 /* 'ncl2' */)                    \
    TAG(outputResponseTag, 0x72657370 /* 'resp' */)                 \
    TAG(perceptualRenderingIntentGamutTag, 0x72696730 /* 'rig0' */) \
    TAG(preview0Tag, 0x70726530 /* 'pre0' */)                       \
    TAG(preview1Tag, 0x70726531 /* 'pre1' */)                       \
    TAG(preview2Tag, 0x70726532 /* 'pre2' */)                       \
    TAG(profileDescriptionTag, 0x64657363 /* 'desc' */)             \
    TAG(profileSequenceDescTag, 0x70736571 /* 'pseq' */)            \
    TAG(profileSequenceIdentifierTag, 0x70736964 /* 'psid' */)      \
    TAG(redMatrixColumnTag, 0x7258595A /* 'rXYZ' */)                \
    TAG(redTRCTag, 0x72545243 /* 'rTRC' */)                         \
    TAG(saturationRenderingIntentGamutTag, 0x72696732 /* 'rig2' */) \
    TAG(technologyTag, 0x74656368 /* 'tech' */)                     \
    TAG(viewingCondDescTag, 0x76756564 /* 'vued' */)                \
    TAG(viewingConditionsTag, 0x76696577 /* 'view' */)

#define TAG(name, id) constexpr inline TagSignature name { id };
ENUMERATE_TAG_SIGNATURES(TAG)
#undef TAG

Optional<StringView> tag_signature_spec_name(TagSignature);

// ICC v4, 7.2.4 Profile version field
class Version {
public:
    Version() = default;
    Version(u8 major, u8 minor_and_bugfix)
        : m_major_version(major)
        , m_minor_and_bugfix_version(minor_and_bugfix)
    {
    }

    u8 major_version() const { return m_major_version; }
    u8 minor_version() const { return m_minor_and_bugfix_version >> 4; }
    u8 bugfix_version() const { return m_minor_and_bugfix_version & 0xf; }

private:
    u8 m_major_version = 0;
    u8 m_minor_and_bugfix_version = 0;
};

// ICC v4, 7.2.5 Profile/device class field
enum class DeviceClass : u32 {
    InputDevice = 0x73636E72,   // 'scnr'
    DisplayDevice = 0x6D6E7472, // 'mntr'
    OutputDevice = 0x70727472,  // 'prtr'
    DeviceLink = 0x6C696E6B,    // 'link'
    ColorSpace = 0x73706163,    // 'spac'
    Abstract = 0x61627374,      // 'abst'
    NamedColor = 0x6E6D636C,    // 'nmcl'
};
StringView device_class_name(DeviceClass);

// ICC v4, 7.2.6 Data colour space field, Table 19 — Data colour space signatures
enum class ColorSpace : u32 {
    nCIEXYZ = 0x58595A20,       // 'XYZ ', used in data color spaces.
    PCSXYZ = nCIEXYZ,           // Used in profile connection space instead.
    CIELAB = 0x4C616220,        // 'Lab ', used in data color spaces.
    PCSLAB = CIELAB,            // Used in profile connection space instead.
    CIELUV = 0x4C757620,        // 'Luv '
    YCbCr = 0x59436272,         // 'YCbr'
    CIEYxy = 0x59787920,        // 'Yxy '
    RGB = 0x52474220,           // 'RGB '
    Gray = 0x47524159,          // 'GRAY'
    HSV = 0x48535620,           // 'HSV '
    HLS = 0x484C5320,           // 'HLS '
    CMYK = 0x434D594B,          // 'CMYK'
    CMY = 0x434D5920,           // 'CMY '
    TwoColor = 0x32434C52,      // '2CLR'
    ThreeColor = 0x33434C52,    // '3CLR'
    FourColor = 0x34434C52,     // '4CLR'
    FiveColor = 0x35434C52,     // '5CLR'
    SixColor = 0x36434C52,      // '6CLR'
    SevenColor = 0x37434C52,    // '7CLR'
    EightColor = 0x38434C52,    // '8CLR'
    NineColor = 0x39434C52,     // '9CLR'
    TenColor = 0x41434C52,      // 'ACLR'
    ElevenColor = 0x42434C52,   // 'BCLR'
    TwelveColor = 0x43434C52,   // 'CCLR'
    ThirteenColor = 0x44434C52, // 'DCLR'
    FourteenColor = 0x45434C52, // 'ECLR'
    FifteenColor = 0x46434C52,  // 'FCLR'
};
StringView data_color_space_name(ColorSpace);
StringView profile_connection_space_name(ColorSpace);

// ICC v4, 7.2.10 Primary platform field, Table 20 — Primary platforms
enum class PrimaryPlatform : u32 {
    Apple = 0x4150504C,           // 'APPL'
    Microsoft = 0x4D534654,       // 'MSFT'
    SiliconGraphics = 0x53474920, // 'SGI '
    Sun = 0x53554E57,             // 'SUNW'
};
StringView primary_platform_name(PrimaryPlatform);

// ICC v4, 7.2.15 Rendering intent field
enum class RenderingIntent {
    Perceptual,
    MediaRelativeColorimetric,
    Saturation,
    ICCAbsoluteColorimetric,
};
StringView rendering_intent_name(RenderingIntent);

// ICC v4, 7.2.11 Profile flags field
class Flags {
public:
    Flags();

    // "The profile flags field contains flags."
    Flags(u32);

    u32 bits() const { return m_bits; }

    // "These can indicate various hints for the CMM such as distributed processing and caching options."
    // "The least-significant 16 bits are reserved for the ICC."
    u16 color_management_module_bits() const { return bits() >> 16; }
    u16 icc_bits() const { return bits() & 0xffff; }

    // "Bit position 0: Embedded profile (0 if not embedded, 1 if embedded in file)"
    bool is_embedded_in_file() const { return (icc_bits() & 1) != 0; }

    // "Bit position 1: Profile cannot be used independently of the embedded colour data (set to 1 if true, 0 if false)"
    // Double negation isn't unconfusing, so this function uses the inverted, positive sense.
    bool can_be_used_independently_of_embedded_color_data() const { return (icc_bits() & 2) == 0; }

    static constexpr u32 KnownBitsMask = 3;

private:
    u32 m_bits = 0;
};

// ICC v4, 7.2.14 Device attributes field
class DeviceAttributes {
public:
    DeviceAttributes();

    // "The device attributes field shall contain flags used to identify attributes
    // unique to the particular device setup for which the profile is applicable."
    DeviceAttributes(u64);

    u64 bits() const { return m_bits; }

    // "The least-significant 32 bits of this 64-bit value are defined by the ICC. "
    u32 icc_bits() const { return bits() & 0xffff'ffff; }

    // "Notice that bits 0, 1, 2, and 3 describe the media, not the device."

    // "0": "Reflective (0) or transparency (1)"
    enum class MediaReflectivity {
        Reflective,
        Transparent,
    };
    MediaReflectivity media_reflectivity() const { return MediaReflectivity(icc_bits() & 1); }

    // "1": "Glossy (0) or matte (1)"
    enum class MediaGlossiness {
        Glossy,
        Matte,
    };
    MediaGlossiness media_glossiness() const { return MediaGlossiness((icc_bits() >> 1) & 1); }

    // "2": "Media polarity, positive (0) or negative (1)"
    enum class MediaPolarity {
        Positive,
        Negative,
    };
    MediaPolarity media_polarity() const { return MediaPolarity((icc_bits() >> 2) & 1); }

    // "3": "Colour media (0), black & white media (1)"
    enum class MediaColor {
        Colored,
        BlackAndWhite,
    };
    MediaColor media_color() const { return MediaColor((icc_bits() >> 3) & 1); }

    // "4 to 31": Reserved (set to binary zero)"

    // "32 to 63": "Use not defined by ICC (vendor specific"
    u32 vendor_bits() const { return bits() >> 32; }

    static constexpr u64 KnownBitsMask = 0xf;

private:
    u64 m_bits = 0;
};

using S15Fixed16 = FixedPoint<16, i32>;

struct XYZ {
    double x { 0 };
    double y { 0 };
    double z { 0 };
};

class TagData : public RefCounted<TagData> {
public:
    u32 offset() const { return m_offset; }
    u32 size() const { return m_size; }
    TagTypeSignature type() const { return m_type; }

protected:
    TagData(u32 offset, u32 size, TagTypeSignature type)
        : m_offset(offset)
        , m_size(size)
        , m_type(type)
    {
    }

private:
    u32 m_offset;
    u32 m_size;
    TagTypeSignature m_type;
};

class UnknownTagData : public TagData {
public:
    UnknownTagData(u32 offset, u32 size, TagTypeSignature type)
        : TagData(offset, size, type)
    {
    }
};

// ICC v4, 10.6 curveType
class CurveTagData : public TagData {
public:
    static constexpr TagTypeSignature Type { 0x63757276 }; // 'curv'

    static ErrorOr<NonnullRefPtr<CurveTagData>> from_bytes(ReadonlyBytes, u32 offset, u32 size);

    CurveTagData(u32 offset, u32 size, Vector<u16> values)
        : TagData(offset, size, Type)
        , m_values(move(values))
    {
    }

    // "The curveType embodies a one-dimensional function which maps an input value in the domain of the function
    //  to an output value in the range of the function. The domain and range values are in the range of 0,0 to 1,0.
    //  - When n is equal to 0, an identity response is assumed.
    //  - When n is equal to 1, then the curve value shall be interpreted as a gamma value, encoded as a
    //    u8Fixed8Number. Gamma shall be interpreted as the exponent in the equation y = pow(x,γ) and not as an inverse.
    //  - When n is greater than 1, the curve values (which embody a sampled one-dimensional function) shall be
    //    defined as follows:
    //    - The first entry represents the input value 0,0, the last entry represents the input value 1,0, and intermediate
    //      entries are uniformly spaced using an increment of 1,0/(n-1). These entries are encoded as uInt16Numbers
    //      (i.e. the values represented by the entries, which are in the range 0,0 to 1,0 are encoded in the range 0 to
    //      65 535). Function values between the entries shall be obtained through linear interpolation."
    Vector<u16> const& values() const { return m_values; }

private:
    Vector<u16> m_values;
};

// ICC v4, 10.15 multiLocalizedUnicodeType
class MultiLocalizedUnicodeTagData : public TagData {
public:
    static constexpr TagTypeSignature Type { 0x6D6C7563 }; // 'mluc'

    static ErrorOr<NonnullRefPtr<MultiLocalizedUnicodeTagData>> from_bytes(ReadonlyBytes, u32 offset, u32 size);

    struct Record {
        u16 iso_639_1_language_code;
        u16 iso_3166_1_country_code;
        String text;
    };

    MultiLocalizedUnicodeTagData(u32 offset, u32 size, Vector<Record> records)
        : TagData(offset, size, Type)
        , m_records(move(records))
    {
    }

    Vector<Record> const& records() const { return m_records; }

private:
    Vector<Record> m_records;
};

// ICC v4, 10.18 parametricCurveType
class ParametricCurveTagData : public TagData {
public:
    // Table 68 — parametricCurveType function type encoding
    enum class FunctionType {
        // Y = X**g
        Type0,

        // Y = (a*X + b)**g       if X >= -b/a
        //   = 0                  else
        Type1,
        CIE_122_1966 = Type1,

        // Y = (a*X + b)**g + c   if X >= -b/a
        //   = c                  else
        Type2,
        IEC_61966_1 = Type2,

        // Y = (a*X + b)**g       if X >= d
        //   =  c*X               else
        Type3,
        IEC_61966_2_1 = Type3,
        sRGB = Type3,

        // Y = (a*X + b)**g + e   if X >= d
        //   =  c*X + f           else
        Type4,
    };

    // "The domain and range of each function shall be [0,0 1,0]. Any function value outside the range shall be clipped
    //  to the range of the function."
    // "NOTE 1 The parameters selected for a parametric curve can result in complex or undefined values for the input range
    //  used. This can occur, for example, if d < -b/a. In such cases the behaviour of the curve is undefined."

    static constexpr TagTypeSignature Type { 0x70617261 }; // 'para'

    static ErrorOr<NonnullRefPtr<ParametricCurveTagData>> from_bytes(ReadonlyBytes, u32 offset, u32 size);

    ParametricCurveTagData(u32 offset, u32 size, FunctionType function_type, Array<S15Fixed16, 7> parameters)
        : TagData(offset, size, Type)
        , m_function_type(function_type)
        , m_parameters(move(parameters))
    {
    }

    FunctionType function_type() const { return m_function_type; }

    static unsigned parameter_count(FunctionType);

    S15Fixed16 g() const { return m_parameters[0]; }
    S15Fixed16 a() const
    {
        VERIFY(function_type() >= FunctionType::Type1);
        return m_parameters[1];
    }
    S15Fixed16 b() const
    {
        VERIFY(function_type() >= FunctionType::Type1);
        return m_parameters[2];
    }
    S15Fixed16 c() const
    {
        VERIFY(function_type() >= FunctionType::Type2);
        return m_parameters[3];
    }
    S15Fixed16 d() const
    {
        VERIFY(function_type() >= FunctionType::Type3);
        return m_parameters[4];
    }
    S15Fixed16 e() const
    {
        VERIFY(function_type() >= FunctionType::Type4);
        return m_parameters[5];
    }
    S15Fixed16 f() const
    {
        VERIFY(function_type() >= FunctionType::Type4);
        return m_parameters[6];
    }

private:
    FunctionType m_function_type;

    // Contains, in this order, g a b c d e f.
    // Not all FunctionTypes use all parameters.
    Array<S15Fixed16, 7> m_parameters;
};

// ICC v4, 10.22 s15Fixed16ArrayType
class S15Fixed16ArrayTagData : public TagData {
public:
    static constexpr TagTypeSignature Type { 0x73663332 }; // 'sf32'

    static ErrorOr<NonnullRefPtr<S15Fixed16ArrayTagData>> from_bytes(ReadonlyBytes, u32 offset, u32 size);

    S15Fixed16ArrayTagData(u32 offset, u32 size, Vector<S15Fixed16, 9> values)
        : TagData(offset, size, Type)
        , m_values(move(values))
    {
    }

    Vector<S15Fixed16, 9> const& values() const { return m_values; }

private:
    Vector<S15Fixed16, 9> m_values;
};

// ICC v2, 6.5.17 textDescriptionType
class TextDescriptionTagData : public TagData {
public:
    static constexpr TagTypeSignature Type { 0x64657363 }; // 'desc'

    static ErrorOr<NonnullRefPtr<TextDescriptionTagData>> from_bytes(ReadonlyBytes, u32 offset, u32 size);

    TextDescriptionTagData(u32 offset, u32 size, String ascii_description, u32 unicode_language_code, Optional<String> unicode_description, Optional<String> macintosh_description)
        : TagData(offset, size, Type)
        , m_ascii_description(move(ascii_description))
        , m_unicode_language_code(unicode_language_code)
        , m_unicode_description(move(unicode_description))
        , m_macintosh_description(move(macintosh_description))
    {
    }

    // Guaranteed to be 7-bit ASCII.
    String const& ascii_description() const { return m_ascii_description; }

    u32 unicode_language_code() const { return m_unicode_language_code; }
    Optional<String> const& unicode_description() const { return m_unicode_description; }

    Optional<String> const& macintosh_description() const { return m_macintosh_description; }

private:
    String m_ascii_description;

    u32 m_unicode_language_code { 0 };
    Optional<String> m_unicode_description;

    Optional<String> m_macintosh_description;
};

// ICC v4, 10.24 textType
class TextTagData : public TagData {
public:
    static constexpr TagTypeSignature Type { 0x74657874 }; // 'text'

    static ErrorOr<NonnullRefPtr<TextTagData>> from_bytes(ReadonlyBytes, u32 offset, u32 size);

    TextTagData(u32 offset, u32 size, String text)
        : TagData(offset, size, Type)
        , m_text(move(text))
    {
    }

    // Guaranteed to be 7-bit ASCII.
    String const& text() const { return m_text; }

private:
    String m_text;
};

// ICC v4, 10.31 XYZType
class XYZTagData : public TagData {
public:
    static constexpr TagTypeSignature Type { 0x58595A20 }; // 'XYZ '

    static ErrorOr<NonnullRefPtr<XYZTagData>> from_bytes(ReadonlyBytes, u32 offset, u32 size);

    XYZTagData(u32 offset, u32 size, Vector<XYZ, 1> xyzs)
        : TagData(offset, size, Type)
        , m_xyzs(move(xyzs))
    {
    }

    Vector<XYZ, 1> const& xyzs() const { return m_xyzs; }

private:
    Vector<XYZ, 1> m_xyzs;
};

class Profile : public RefCounted<Profile> {
public:
    static ErrorOr<NonnullRefPtr<Profile>> try_load_from_externally_owned_memory(ReadonlyBytes);

    Optional<PreferredCMMType> preferred_cmm_type() const { return m_preferred_cmm_type; }
    Version version() const { return m_version; }
    DeviceClass device_class() const { return m_device_class; }
    ColorSpace data_color_space() const { return m_data_color_space; }

    // For non-DeviceLink profiles, always PCSXYZ or PCSLAB.
    ColorSpace connection_space() const { return m_connection_space; }

    u32 on_disk_size() const { return m_on_disk_size; }
    time_t creation_timestamp() const { return m_creation_timestamp; }
    PrimaryPlatform primary_platform() const { return m_primary_platform; }
    Flags flags() const { return m_flags; }
    Optional<DeviceManufacturer> device_manufacturer() const { return m_device_manufacturer; }
    Optional<DeviceModel> device_model() const { return m_device_model; }
    DeviceAttributes device_attributes() const { return m_device_attributes; }
    RenderingIntent rendering_intent() const { return m_rendering_intent; }
    XYZ const& pcs_illuminant() const { return m_pcs_illuminant; }
    Optional<Creator> creator() const { return m_creator; }
    Optional<Crypto::Hash::MD5::DigestType> const& id() const { return m_id; }

    static Crypto::Hash::MD5::DigestType compute_id(ReadonlyBytes);

    template<typename Callback>
    void for_each_tag(Callback callback) const
    {
        for (auto const& tag : m_tag_table)
            callback(tag.key, tag.value);
    }

    // Only versions 2 and 4 are in use.
    bool is_v2() const { return version().major_version() == 2; }
    bool is_v4() const { return version().major_version() == 4; }

private:
    ErrorOr<void> read_header(ReadonlyBytes);
    ErrorOr<NonnullRefPtr<TagData>> read_tag(ReadonlyBytes bytes, u32 offset_to_beginning_of_tag_data_element, u32 size_of_tag_data_element);
    ErrorOr<void> read_tag_table(ReadonlyBytes);
    ErrorOr<void> check_required_tags();

    u32 m_on_disk_size { 0 };
    Optional<PreferredCMMType> m_preferred_cmm_type;
    Version m_version;
    DeviceClass m_device_class {};
    ColorSpace m_data_color_space {};
    ColorSpace m_connection_space {};
    time_t m_creation_timestamp { 0 };
    PrimaryPlatform m_primary_platform {};
    Flags m_flags;
    Optional<DeviceManufacturer> m_device_manufacturer;
    Optional<DeviceModel> m_device_model;
    DeviceAttributes m_device_attributes;
    RenderingIntent m_rendering_intent {};
    XYZ m_pcs_illuminant;
    Optional<Creator> m_creator;
    Optional<Crypto::Hash::MD5::DigestType> m_id;

    OrderedHashMap<TagSignature, NonnullRefPtr<TagData>> m_tag_table;
};

}

namespace AK {
template<Gfx::ICC::FourCCType Type>
struct Formatter<Gfx::ICC::DistinctFourCC<Type>> : StandardFormatter {
    ErrorOr<void> format(FormatBuilder& builder, Gfx::ICC::DistinctFourCC<Type> const& four_cc)
    {
        TRY(builder.put_padding('\'', 1));
        TRY(builder.put_padding(four_cc.c0(), 1));
        TRY(builder.put_padding(four_cc.c1(), 1));
        TRY(builder.put_padding(four_cc.c2(), 1));
        TRY(builder.put_padding(four_cc.c3(), 1));
        TRY(builder.put_padding('\'', 1));
        return {};
    }
};

template<>
struct Formatter<Gfx::ICC::Version> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Gfx::ICC::Version const& version)
    {
        return Formatter<FormatString>::format(builder, "{}.{}.{}"sv, version.major_version(), version.minor_version(), version.bugfix_version());
    }
};

template<>
struct Formatter<Gfx::ICC::XYZ> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Gfx::ICC::XYZ const& xyz)
    {
        return Formatter<FormatString>::format(builder, "X = {}, Y = {}, Z = {}"sv, xyz.x, xyz.y, xyz.z);
    }
};

template<Gfx::ICC::FourCCType Type>
struct Traits<Gfx::ICC::DistinctFourCC<Type>> : public GenericTraits<Gfx::ICC::DistinctFourCC<Type>> {
    static unsigned hash(Gfx::ICC::DistinctFourCC<Type> const& key)
    {
        return int_hash(key.value);
    }

    static bool equals(Gfx::ICC::DistinctFourCC<Type> const& a, Gfx::ICC::DistinctFourCC<Type> const& b)
    {
        return a == b;
    }
};
}
