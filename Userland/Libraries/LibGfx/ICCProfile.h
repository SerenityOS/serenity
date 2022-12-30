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

namespace Gfx::ICC {

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
char const* device_class_name(DeviceClass);

// ICC v4, 7.2.6 Data colour space field, Table 19 — Data colour space signatures
enum class ColorSpace : u32 {
    nCIEXYZ = 0x58595A20,       // 'XYZ '
    CIELAB = 0x4C616220,        // 'Lab '
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
char const* color_space_name(ColorSpace);

class Profile : public RefCounted<Profile> {
public:
    static ErrorOr<NonnullRefPtr<Profile>> try_load_from_externally_owned_memory(ReadonlyBytes bytes);

    Version version() const { return m_version; }
    DeviceClass device_class() const { return m_device_class; }
    ColorSpace data_color_space() const { return m_data_color_space; }
    time_t creation_timestamp() const { return m_creation_timestamp; }

private:
    Version m_version;
    DeviceClass m_device_class;
    ColorSpace m_data_color_space;
    time_t m_creation_timestamp;
};

}

namespace AK {
template<>
struct Formatter<Gfx::ICC::Version> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Gfx::ICC::Version const& version)
    {
        return Formatter<FormatString>::format(builder, "{}.{}.{}"sv, version.major_version(), version.minor_version(), version.bugfix_version());
    }
};
}
