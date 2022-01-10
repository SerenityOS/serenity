/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSoftGPU/Image.h>

namespace SoftGPU {

Image::Image(ImageFormat format, unsigned width, unsigned height, unsigned depth, unsigned levels, unsigned layers)
    : m_format(format)
    , m_width(width)
    , m_height(height)
    , m_depth(depth)
    , m_num_layers(layers)
{
    VERIFY(width > 0);
    VERIFY(height > 0);
    VERIFY(depth > 0);
    VERIFY(levels > 0);
    VERIFY(layers > 0);

    if ((width & (width - 1)) == 0)
        m_width_is_power_of_two = true;

    if ((height & (height - 1)) == 0)
        m_height_is_power_of_two = true;

    if ((depth & (depth - 1)) == 0)
        m_depth_is_power_of_two = true;

    m_mipmap_sizes.append({ width, height, depth });
    m_mipmap_offsets.append(0);

    m_mipchain_size += width * height * depth * element_size(format);

    while (--levels && (width > 1 || height > 1 || depth > 1)) {
        width = max(width / 2, 1);
        height = max(height / 2, 1);
        depth = max(depth / 2, 1);
        m_mipmap_sizes.append({ width, height, depth });
        m_mipmap_offsets.append(m_mipchain_size);

        m_mipchain_size += width * height * depth * element_size(format);
    }

    m_num_levels = m_mipmap_sizes.size();

    m_data.resize(m_mipchain_size * m_num_layers);
}

void Image::write_texels(unsigned layer, unsigned level, Vector3<unsigned> const& offset, Vector3<unsigned> const& size, void const* data, ImageDataLayout const& layout)
{
    VERIFY(layer < num_layers());
    VERIFY(level < num_levels());
    VERIFY(offset.x() + size.x() <= level_width(level));
    VERIFY(offset.y() + size.y() <= level_height(level));
    VERIFY(offset.z() + size.z() <= level_depth(level));

    for (unsigned z = 0; z < size.z(); ++z) {
        for (unsigned y = 0; y < size.y(); ++y) {
            for (unsigned x = 0; x < size.x(); ++x) {
                auto ptr = reinterpret_cast<u8 const*>(data) + layout.depth_stride * z + layout.row_stride * y + layout.column_stride * x;
                auto color = unpack_color(ptr, layout.format);
                set_texel(layer, level, offset.x() + x, offset.y() + y, offset.z() + z, color);
            }
        }
    }
}

void Image::read_texels(unsigned layer, unsigned level, Vector3<unsigned> const& offset, Vector3<unsigned> const& size, void* data, ImageDataLayout const& layout) const
{
    VERIFY(layer < num_layers());
    VERIFY(level < num_levels());
    VERIFY(offset.x() + size.x() <= level_width(level));
    VERIFY(offset.y() + size.y() <= level_height(level));
    VERIFY(offset.z() + size.z() <= level_depth(level));

    for (unsigned z = 0; z < size.z(); ++z) {
        for (unsigned y = 0; y < size.y(); ++y) {
            for (unsigned x = 0; x < size.x(); ++x) {
                auto color = texel(layer, level, offset.x() + x, offset.y() + y, offset.z() + z);
                auto ptr = reinterpret_cast<u8*>(data) + layout.depth_stride * z + layout.row_stride * y + layout.column_stride * x;
                pack_color(color, ptr, layout.format);
            }
        }
    }
}

void Image::copy_texels(Image const& source, unsigned source_layer, unsigned source_level, Vector3<unsigned> const& source_offset, Vector3<unsigned> const& size, unsigned destination_layer, unsigned destination_level, Vector3<unsigned> const& destination_offset)
{
    VERIFY(source_layer < source.num_layers());
    VERIFY(source_level < source.num_levels());
    VERIFY(source_offset.x() + size.x() <= source.level_width(source_level));
    VERIFY(source_offset.y() + size.y() <= source.level_height(source_level));
    VERIFY(source_offset.z() + size.z() <= source.level_depth(source_level));
    VERIFY(destination_layer < num_layers());
    VERIFY(destination_level < num_levels());
    VERIFY(destination_offset.x() + size.x() <= level_width(destination_level));
    VERIFY(destination_offset.y() + size.y() <= level_height(destination_level));
    VERIFY(destination_offset.z() + size.z() <= level_depth(destination_level));

    for (unsigned z = 0; z < size.z(); ++z) {
        for (unsigned y = 0; y < size.y(); ++y) {
            for (unsigned x = 0; x < size.x(); ++x) {
                auto color = source.texel(source_layer, source_level, source_offset.x() + x, source_offset.y() + y, source_offset.z() + z);
                set_texel(destination_layer, destination_level, destination_offset.x() + x, destination_offset.y() + y, destination_offset.z() + z, color);
            }
        }
    }
}

}
