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
#include <LibGfx/Vector3.h>
#include <LibGfx/Vector4.h>
#include <LibSoftGPU/Buffer/Typed3DBuffer.h>

namespace SoftGPU {

class Image final : public GPU::Image {
public:
    Image(void* const ownership_token, unsigned width, unsigned height, unsigned depth, unsigned max_levels, unsigned layers);

    unsigned level_width(unsigned level) const { return m_mipmap_buffers[level]->width(); }
    unsigned level_height(unsigned level) const { return m_mipmap_buffers[level]->height(); }
    unsigned level_depth(unsigned level) const { return m_mipmap_buffers[level]->depth(); }
    unsigned num_levels() const { return m_num_levels; }
    unsigned num_layers() const { return m_num_layers; }
    bool width_is_power_of_two() const { return m_width_is_power_of_two; }
    bool height_is_power_of_two() const { return m_height_is_power_of_two; }
    bool depth_is_power_of_two() const { return m_depth_is_power_of_two; }

    FloatVector4 texel(unsigned layer, unsigned level, int x, int y, int z) const
    {
        return *texel_pointer(layer, level, x, y, z);
    }

    void set_texel(unsigned layer, unsigned level, int x, int y, int z, FloatVector4 const& color)
    {
        *texel_pointer(layer, level, x, y, z) = color;
    }

    virtual void write_texels(unsigned layer, unsigned level, Vector3<unsigned> const& offset, Vector3<unsigned> const& size, void const* data, GPU::ImageDataLayout const& layout) override;
    virtual void read_texels(unsigned layer, unsigned level, Vector3<unsigned> const& offset, Vector3<unsigned> const& size, void* data, GPU::ImageDataLayout const& layout) const override;
    virtual void copy_texels(GPU::Image const& source, unsigned source_layer, unsigned source_level, Vector3<unsigned> const& source_offset, Vector3<unsigned> const& size, unsigned destination_layer, unsigned destination_level, Vector3<unsigned> const& destination_offset) override;

private:
    FloatVector4 const* texel_pointer(unsigned layer, unsigned level, int x, int y, int z) const
    {
        return m_mipmap_buffers[layer * m_num_layers + level]->buffer_pointer(x, y, z);
    }

    FloatVector4* texel_pointer(unsigned layer, unsigned level, int x, int y, int z)
    {
        return m_mipmap_buffers[layer * m_num_layers + level]->buffer_pointer(x, y, z);
    }

private:
    unsigned m_num_levels { 0 };
    unsigned m_num_layers { 0 };

    FixedArray<RefPtr<Typed3DBuffer<FloatVector4>>> m_mipmap_buffers;

    bool m_width_is_power_of_two { false };
    bool m_height_is_power_of_two { false };
    bool m_depth_is_power_of_two { false };
};

}
