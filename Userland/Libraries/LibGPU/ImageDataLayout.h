/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGPU/ImageFormat.h>

namespace GPU {

// Order of bytes within a single component
enum class ComponentBytesOrder {
    Normal,
    Reversed,
};

struct PackingSpecification final {
    u32 depth_stride { 0 };
    u32 row_stride { 0 };
    u8 byte_alignment { 1 };
    ComponentBytesOrder component_bytes_order { ComponentBytesOrder::Normal };
};

// Full dimensions of the image
struct DimensionSpecification final {
    u32 width;
    u32 height;
    u32 depth;
};

// Subselection (source or target) within the image
struct ImageSelection final {
    i32 offset_x { 0 };
    i32 offset_y { 0 };
    i32 offset_z { 0 };
    u32 width;
    u32 height;
    u32 depth;
};

struct ImageDataLayout final {
    PixelType pixel_type;
    PackingSpecification packing {};
    DimensionSpecification dimensions;
    ImageSelection selection;
};

}
