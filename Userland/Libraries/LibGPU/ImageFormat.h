/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>

namespace GPU {

// The pixel data's representation
enum class PixelFormat {
    Alpha,
    BGR,
    BGRA,
    Blue,
    ColorIndex,
    DepthComponent,
    Green,
    Intensity,
    Luminance,
    LuminanceAlpha,
    Red,
    RGB,
    RGBA,
    StencilIndex,
};

// Bit width assigned to individual components within a single pixel's value
enum class PixelComponentBits {
    AllBits,
    B1_5_5_5,
    B2_3_3,
    B2_10_10_10,
    B3_3_2,
    B4_4_4_4,
    B5_5_5_1,
    B5_6_5,
    B8_8_8_8,
    B10_10_10_2,
};

// The base data type used as pixel storage
enum class PixelDataType {
    Bitmap,
    Byte,
    Float,
    HalfFloat,
    Int,
    Short,
    UnsignedByte,
    UnsignedInt,
    UnsignedShort,
};

// Order of components within a single pixel
enum class ComponentsOrder {
    Normal,
    Reversed,
};

struct PixelType final {
    PixelFormat format;
    PixelComponentBits bits;
    PixelDataType data_type;
    ComponentsOrder components_order { ComponentsOrder::Normal };
};

static constexpr int number_of_components(PixelFormat format)
{
    switch (format) {
    case PixelFormat::Alpha:
    case PixelFormat::Blue:
    case PixelFormat::ColorIndex:
    case PixelFormat::DepthComponent:
    case PixelFormat::Green:
    case PixelFormat::Intensity:
    case PixelFormat::Luminance:
    case PixelFormat::Red:
    case PixelFormat::StencilIndex:
        return 1;
    case PixelFormat::LuminanceAlpha:
        return 2;
    case PixelFormat::BGR:
    case PixelFormat::RGB:
        return 3;
    case PixelFormat::BGRA:
    case PixelFormat::RGBA:
        return 4;
    }
    VERIFY_NOT_REACHED();
}

static constexpr int number_of_components(PixelComponentBits bits)
{
    switch (bits) {
    case PixelComponentBits::AllBits:
        return 1;
    case PixelComponentBits::B2_3_3:
    case PixelComponentBits::B3_3_2:
    case PixelComponentBits::B5_6_5:
        return 3;
    case PixelComponentBits::B1_5_5_5:
    case PixelComponentBits::B2_10_10_10:
    case PixelComponentBits::B4_4_4_4:
    case PixelComponentBits::B5_5_5_1:
    case PixelComponentBits::B8_8_8_8:
    case PixelComponentBits::B10_10_10_2:
        return 4;
    }
    VERIFY_NOT_REACHED();
}

static constexpr Array<u8, 4> pixel_component_bitfield_lengths(PixelComponentBits bits)
{
    switch (bits) {
    case PixelComponentBits::AllBits:
        VERIFY_NOT_REACHED();
    case PixelComponentBits::B1_5_5_5:
        return { 1, 5, 5, 5 };
    case PixelComponentBits::B2_3_3:
        return { 2, 3, 3 };
    case PixelComponentBits::B2_10_10_10:
        return { 2, 10, 10, 10 };
    case PixelComponentBits::B3_3_2:
        return { 3, 3, 2 };
    case PixelComponentBits::B4_4_4_4:
        return { 4, 4, 4, 4 };
    case PixelComponentBits::B5_5_5_1:
        return { 5, 5, 5, 1 };
    case PixelComponentBits::B5_6_5:
        return { 5, 6, 5 };
    case PixelComponentBits::B8_8_8_8:
        return { 8, 8, 8, 8 };
    case PixelComponentBits::B10_10_10_2:
        return { 10, 10, 10, 2 };
    }
    VERIFY_NOT_REACHED();
}

static constexpr size_t pixel_data_type_size_in_bytes(PixelDataType data_type)
{
    switch (data_type) {
    case PixelDataType::Bitmap:
        return sizeof(u8);
    case PixelDataType::Byte:
        return sizeof(u8);
    case PixelDataType::Float:
        return sizeof(float);
    case PixelDataType::HalfFloat:
        return sizeof(float) / 2;
    case PixelDataType::Int:
        return sizeof(i32);
    case PixelDataType::Short:
        return sizeof(i16);
    case PixelDataType::UnsignedByte:
        return sizeof(u8);
    case PixelDataType::UnsignedInt:
        return sizeof(u32);
    case PixelDataType::UnsignedShort:
        return sizeof(u16);
    }
    VERIFY_NOT_REACHED();
}

static constexpr u8 pixel_size_in_bytes(PixelType pixel_type)
{
    auto component_size_in_bytes = pixel_data_type_size_in_bytes(pixel_type.data_type);
    if (pixel_type.bits == PixelComponentBits::AllBits)
        return component_size_in_bytes * number_of_components(pixel_type.format);
    return component_size_in_bytes;
}

}
