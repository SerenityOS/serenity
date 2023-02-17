/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Endian.h>
#include <LibGfx/ICC/DistinctFourCC.h>
#include <LibGfx/ICC/Profile.h>
#include <LibGfx/ICC/TagTypes.h>

namespace Gfx::ICC {

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

// ICC V4, 4.7 u16Fixed16Number
using u16Fixed16Number = u32;

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
    BigEndian<RenderingIntent> rendering_intent;

    XYZNumber pcs_illuminant;

    BigEndian<Creator> profile_creator;

    u8 profile_id[16];
    u8 reserved[28];
};
static_assert(AssertSize<ICCHeader, 128>());

// Common bits of ICC v4, Table 40 — lut16Type encoding and Table 44 — lut8Type encoding
struct LUTHeader {
    u8 number_of_input_channels;
    u8 number_of_output_channels;
    u8 number_of_clut_grid_points;
    u8 reserved_for_padding;
    BigEndian<s15Fixed16Number> e_parameters[9];
};
static_assert(AssertSize<LUTHeader, 40>());

// Common bits of ICC v4, Table 45 — lutAToBType encoding and Table 47 — lutBToAType encoding
struct AdvancedLUTHeader {
    u8 number_of_input_channels;
    u8 number_of_output_channels;
    BigEndian<u16> reserved_for_padding;
    BigEndian<u32> offset_to_b_curves;
    BigEndian<u32> offset_to_matrix;
    BigEndian<u32> offset_to_m_curves;
    BigEndian<u32> offset_to_clut;
    BigEndian<u32> offset_to_a_curves;
};
static_assert(AssertSize<AdvancedLUTHeader, 24>());

// ICC v4, Table 46 — lutAToBType CLUT encoding
// ICC v4, Table 48 — lutBToAType CLUT encoding
// (They're identical.)
struct CLUTHeader {
    u8 number_of_grid_points_in_dimension[16];
    u8 precision_of_data_elements; // 1 for u8 entries, 2 for u16 entries.
    u8 reserved_for_padding[3];
};
static_assert(AssertSize<CLUTHeader, 20>());

}
