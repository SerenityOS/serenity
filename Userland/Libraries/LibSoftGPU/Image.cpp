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

}
