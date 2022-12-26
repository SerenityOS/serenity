/*
 * Copyright (c) 2022, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Endian.h>
#include <LibGfx/ICCProfile.h>

// V2 spec: https://color.org/specification/ICC.1-2001-04.pdf
// V4 spec: https://color.org/specification/ICC.1-2022-05.pdf

namespace Gfx::ICC {

namespace {

// ICC V4, 7.2 Profile header
struct ICCHeader {
    BigEndian<u32> profile_size;
    BigEndian<u32> preferred_cmm_type;

    u8 profile_version_major;
    u8 profile_version_minor_bugfix;
    BigEndian<u16> profile_version_zero;

    BigEndian<u32> profile_device_class;
    BigEndian<u32> data_color_space;
    BigEndian<u32> pcs; // "Profile Connection Space"

    BigEndian<u16> year;
    BigEndian<u16> month;
    BigEndian<u16> day;
    BigEndian<u16> hour;
    BigEndian<u16> minutes;
    BigEndian<u16> seconds;

    BigEndian<u32> profile_file_signature;
    BigEndian<u32> primary_platform;

    BigEndian<u32> profile_flags;
    BigEndian<u32> device_manufacturer;
    BigEndian<u32> device_model;
    BigEndian<u64> device_attributes;
    BigEndian<u32> rendering_intent;

    BigEndian<i32> pcs_illuminant_x;
    BigEndian<i32> pcs_illuminant_y;
    BigEndian<i32> pcs_illuminant_z;

    BigEndian<u32> profile_creator;

    u8 profile_md5[16];
    u8 reserved[28];
};
static_assert(sizeof(ICCHeader) == 128);

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
    case (u32)DeviceClass::InputDevce:
    case (u32)DeviceClass::DisplayDevice:
    case (u32)DeviceClass::OutputDevice:
    case (u32)DeviceClass::DeviceLink:
    case (u32)DeviceClass::ColorSpace:
    case (u32)DeviceClass::Abstract:
    case (u32)DeviceClass::NamedColor:
        return DeviceClass { u32 { header.profile_device_class } };
    }
    return Error::from_string_literal("ICC::Profile: Invalid device class");
}

ErrorOr<void> parse_file_signature(ICCHeader const& header)
{
    // iCC v4, 7.2.9 Profile file signature field
    if (header.profile_file_signature != 0x61637370)
        return Error::from_string_literal("ICC::Profile: profile file signature not 'acsp'");
    return {};
}
}

char const* device_class_name(DeviceClass device_class)
{
    switch (device_class) {
    case DeviceClass::InputDevce:
        return "InputDevce";
    case DeviceClass::DisplayDevice:
        return "DisplayDevice";
    case DeviceClass::OutputDevice:
        return "OutputDevice";
    case DeviceClass::DeviceLink:
        return "DeviceLink";
    case DeviceClass::ColorSpace:
        return "ColorSpace";
    case DeviceClass::Abstract:
        return "Abstract";
    case DeviceClass::NamedColor:
        return "NamedColor";
    default:
        return "(unknown device class)";
    }
}

ErrorOr<NonnullRefPtr<Profile>> Profile::try_load_from_externally_owned_memory(ReadonlyBytes bytes)
{
    auto profile = adopt_ref(*new Profile());

    if (bytes.size() < sizeof(ICCHeader))
        return Error::from_string_literal("ICC::Profile: Not enough data for header");

    auto header = *bit_cast<ICCHeader const*>(bytes.data());

    TRY(parse_file_signature(header));
    profile->m_version = TRY(parse_version(header));
    profile->m_device_class = TRY(parse_device_class(header));

    return profile;
}

}
