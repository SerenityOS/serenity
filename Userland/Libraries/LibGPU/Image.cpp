/*
 * Copyright (c) 2022, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/IntegralMath.h>
#include <AK/Math.h>
#include <LibGPU/Image.h>

namespace GPU {

Image::Image(void const* ownership_token, GPU::PixelFormat const& pixel_format, u32 width, u32 height, u32 depth, u32 max_levels)
    : m_ownership_token { ownership_token }
    , m_pixel_format { pixel_format }
{
    VERIFY(width > 0);
    VERIFY(height > 0);
    VERIFY(depth > 0);
    VERIFY(max_levels > 0);

    u32 number_of_levels_in_full_chain = max(max(AK::log2(width), AK::log2(height)), AK::log2(depth)) + 1;
    m_mipmap_sizes.resize(min(max_levels, number_of_levels_in_full_chain));

    for (u32 level = 0; level < m_mipmap_sizes.size(); ++level) {
        m_mipmap_sizes[level] = { width, height, depth };
        width = max(width / 2, 1);
        height = max(height / 2, 1);
        depth = max(depth / 2, 1);
    }
}

}
