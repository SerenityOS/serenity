/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FixedArray.h>
#include <AK/RefPtr.h>
#include <LibGPU/Image.h>
#include <LibGPU/ImageDataLayout.h>
#include <LibGPU/ImageFormat.h>
#include <LibGfx/Vector3.h>
#include <LibGfx/Vector4.h>
#include <LibSoftGPU/Buffer/Typed3DBuffer.h>

namespace SoftGPU {

class Image final : public GPU::Image {
public:
    Image(void const* ownership_token, GPU::PixelFormat const&, u32 width, u32 height, u32 depth, u32 max_levels, u32 layers);

    u32 level_width(u32 level) const { return m_mipmap_buffers[level]->width(); }
    u32 level_height(u32 level) const { return m_mipmap_buffers[level]->height(); }
    u32 level_depth(u32 level) const { return m_mipmap_buffers[level]->depth(); }
    u32 num_levels() const { return m_num_levels; }
    u32 num_layers() const { return m_num_layers; }
    bool width_is_power_of_two() const { return m_width_is_power_of_two; }
    bool height_is_power_of_two() const { return m_height_is_power_of_two; }
    bool depth_is_power_of_two() const { return m_depth_is_power_of_two; }

    GPU::ImageDataLayout image_data_layout(u32 level, Vector3<i32> offset) const;

    FloatVector4 texel(u32 layer, u32 level, int x, int y, int z) const
    {
        return *texel_pointer(layer, level, x, y, z);
    }

    void set_texel(u32 layer, u32 level, int x, int y, int z, FloatVector4 const& color)
    {
        *texel_pointer(layer, level, x, y, z) = color;
    }

    virtual void write_texels(u32 layer, u32 level, Vector3<i32> const& output_offset, void const* input_data, GPU::ImageDataLayout const&) override;
    virtual void read_texels(u32 layer, u32 level, Vector3<i32> const& input_offset, void* output_data, GPU::ImageDataLayout const&) const override;
    virtual void copy_texels(GPU::Image const& source, u32 source_layer, u32 source_level, Vector3<u32> const& source_offset, Vector3<u32> const& size, u32 destination_layer, u32 destination_level, Vector3<u32> const& destination_offset) override;

    FloatVector4 const* texel_pointer(u32 layer, u32 level, int x, int y, int z) const
    {
        return m_mipmap_buffers[layer * m_num_layers + level]->buffer_pointer(x, y, z);
    }

    FloatVector4* texel_pointer(u32 layer, u32 level, int x, int y, int z)
    {
        return m_mipmap_buffers[layer * m_num_layers + level]->buffer_pointer(x, y, z);
    }

private:
    u32 m_num_levels { 0 };
    u32 m_num_layers { 0 };

    GPU::PixelFormat m_pixel_format;
    FixedArray<RefPtr<Typed3DBuffer<FloatVector4>>> m_mipmap_buffers;

    bool m_width_is_power_of_two { false };
    bool m_height_is_power_of_two { false };
    bool m_depth_is_power_of_two { false };
};

}
