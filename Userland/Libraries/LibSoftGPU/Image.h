/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <AK/Vector.h>
#include <LibGfx/Vector3.h>
#include <LibGfx/Vector4.h>
#include <LibSoftGPU/ImageDataLayout.h>
#include <LibSoftGPU/ImageFormat.h>

namespace SoftGPU {

class Image final : public RefCounted<Image> {
public:
    Image(ImageFormat format, unsigned width, unsigned height, unsigned depth, unsigned levels, unsigned layers);

    ImageFormat format() const { return m_format; }
    unsigned width() const { return m_width; }
    unsigned height() const { return m_height; }
    unsigned depth() const { return m_depth; }
    unsigned level_width(unsigned level) const { return m_mipmap_sizes[level].x(); }
    unsigned level_height(unsigned level) const { return m_mipmap_sizes[level].y(); }
    unsigned level_depth(unsigned level) const { return m_mipmap_sizes[level].z(); }
    unsigned num_levels() const { return m_num_levels; }
    unsigned num_layers() const { return m_num_layers; }
    bool width_is_power_of_two() const { return m_width_is_power_of_two; }
    bool height_is_power_of_two() const { return m_height_is_power_of_two; }
    bool depth_is_power_of_two() const { return m_depth_is_power_of_two; }

    FloatVector4 texel(unsigned layer, unsigned level, unsigned x, unsigned y, unsigned z) const
    {
        return unpack_color(texel_pointer(layer, level, x, y, z), m_format);
    }

    void set_texel(unsigned layer, unsigned level, unsigned x, unsigned y, unsigned z, FloatVector4 const& color)
    {
        pack_color(color, texel_pointer(layer, level, x, y, z), m_format);
    }

    void write_texels(unsigned layer, unsigned level, Vector3<unsigned> const& offset, Vector3<unsigned> const& size, void const* data, ImageDataLayout const& layout);
    void read_texels(unsigned layer, unsigned level, Vector3<unsigned> const& offset, Vector3<unsigned> const& size, void* data, ImageDataLayout const& layout) const;
    void copy_texels(Image const& source, unsigned source_layer, unsigned source_level, Vector3<unsigned> const& source_offset, Vector3<unsigned> const& size, unsigned destination_layer, unsigned destination_level, Vector3<unsigned> const& destination_offset);

private:
    void const* texel_pointer(unsigned layer, unsigned level, unsigned x, unsigned y, unsigned z) const
    {
        auto size = m_mipmap_sizes[level];
        return &m_data[m_mipchain_size * layer + m_mipmap_offsets[level] + (z * size.x() * size.y() + y * size.x() + x) * element_size(m_format)];
    }

    void* texel_pointer(unsigned layer, unsigned level, unsigned x, unsigned y, unsigned z)
    {
        auto size = m_mipmap_sizes[level];
        return &m_data[m_mipchain_size * layer + m_mipmap_offsets[level] + (z * size.x() * size.y() + y * size.x() + x) * element_size(m_format)];
    }

private:
    ImageFormat m_format { ImageFormat::RGBA8888 };
    unsigned m_width { 0 };
    unsigned m_height { 0 };
    unsigned m_depth { 0 };
    unsigned m_num_levels { 0 };
    unsigned m_num_layers { 0 };

    size_t m_mipchain_size { 0 };
    Vector<size_t, 16> m_mipmap_offsets;
    Vector<Vector3<unsigned>, 16> m_mipmap_sizes;
    Vector<u8> m_data;
    bool m_width_is_power_of_two { false };
    bool m_height_is_power_of_two { false };
    bool m_depth_is_power_of_two { false };
};

}
