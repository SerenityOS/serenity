/*
 * Copyright (c) 2021, Stephan Unverwerth <s.unverwerth@serenityos.org>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibSoftGPU/Image.h>

namespace SoftGPU {

Image::Image(unsigned width, unsigned height, unsigned depth, unsigned max_levels, unsigned layers)
    : m_num_layers(layers)
    , m_mipmap_buffers(FixedArray<RefPtr<Typed3DBuffer<ColorType>>>::must_create_but_fixme_should_propagate_errors(layers * max_levels))
{
    VERIFY(width > 0);
    VERIFY(height > 0);
    VERIFY(depth > 0);
    VERIFY(max_levels > 0);
    VERIFY(layers > 0);

    m_width_is_power_of_two = is_power_of_two(width);
    m_height_is_power_of_two = is_power_of_two(height);
    m_depth_is_power_of_two = is_power_of_two(depth);

    unsigned level;
    for (level = 0; level < max_levels; ++level) {
        for (unsigned layer = 0; layer < layers; ++layer)
            m_mipmap_buffers[layer * layers + level] = MUST(Typed3DBuffer<ColorType>::try_create(width, height, depth));

        if (width <= 1 && height <= 1 && depth <= 1)
            break;

        width = max(width / 2, 1);
        height = max(height / 2, 1);
        depth = max(depth / 2, 1);
    }

    m_num_levels = level + 1;
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
