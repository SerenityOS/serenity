/*
 * Copyright (c) 2022, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Format.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <AK/Span.h>
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
};

template<FourCCType type>
struct DistinctFourCC {
    u32 value;

    char c0() const { return value >> 24; }
    char c1() const { return (value >> 16) & 0xff; }
    char c2() const { return (value >> 8) & 0xff; }
    char c3() const { return value & 0xff; }
};

using PreferredCMMType = DistinctFourCC<FourCCType::PreferredCMMType>;     // ICC v4, "7.2.3 Preferred CMM type field"
using DeviceManufacturer = DistinctFourCC<FourCCType::DeviceManufacturer>; // ICC v4, "7.2.12 Device manufacturer field"
using DeviceModel = DistinctFourCC<FourCCType::DeviceModel>;               // ICC v4, "7.2.13 Device model field"
using Creator = DistinctFourCC<FourCCType::Creator>;                       // ICC v4, "7.2.17 Profile creator field"

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
    InputDevce = 0x73636E72,    // 'scnr'
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

struct XYZ {
    double x { 0 };
    double y { 0 };
    double z { 0 };
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

private:
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
}
