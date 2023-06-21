/*
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <AK/Vector.h>
#include <LibGPU/ImageDataLayout.h>
#include <LibGPU/ImageFormat.h>
#include <LibGfx/Vector3.h>

namespace GPU {

class Image : public RefCounted<Image> {
public:
    Image(void const* ownership_token, PixelFormat const&, u32 width, u32 height, u32 depth, u32 max_levels);
    virtual ~Image() { }

    u32 width_at_level(u32 level) const { return m_mipmap_sizes[level].x(); }
    u32 height_at_level(u32 level) const { return m_mipmap_sizes[level].y(); }
    u32 depth_at_level(u32 level) const { return m_mipmap_sizes[level].z(); }
    u32 number_of_levels() const { return m_mipmap_sizes.size(); }

    PixelFormat pixel_format() const { return m_pixel_format; }

    virtual void regenerate_mipmaps() = 0;

    virtual void write_texels(u32 level, Vector3<i32> const& output_offset, void const* input_data, ImageDataLayout const&) = 0;
    virtual void read_texels(u32 level, Vector3<i32> const& input_offset, void* output_data, ImageDataLayout const&) const = 0;
    virtual void copy_texels(Image const& source, u32 source_level, Vector3<u32> const& source_offset, Vector3<u32> const& size, u32 destination_level, Vector3<u32> const& destination_offset) = 0;

    void const* ownership_token() const { return m_ownership_token; }
    bool has_same_ownership_token(Image const& other) const { return other.ownership_token() == ownership_token(); }

private:
    void const* const m_ownership_token { nullptr };
    Vector<Vector3<u32>> m_mipmap_sizes;
    PixelFormat m_pixel_format;
};

}
