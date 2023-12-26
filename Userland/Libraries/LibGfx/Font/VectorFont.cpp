/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Font/ScaledFont.h>
#include <LibGfx/Font/VectorFont.h>

namespace Gfx {

VectorFont::VectorFont() = default;
VectorFont::~VectorFont() = default;

NonnullRefPtr<ScaledFont> VectorFont::scaled_font(float point_size) const
{
    auto it = m_scaled_fonts.find(point_size);
    if (it != m_scaled_fonts.end())
        return *it->value;

    // FIXME: It might be nice to have a global cap on the number of fonts we cache
    //        instead of doing it at the per-VectorFont level like this.
    constexpr size_t max_cached_font_size_count = 128;
    if (m_scaled_fonts.size() > max_cached_font_size_count)
        m_scaled_fonts.remove(m_scaled_fonts.begin());

    auto scaled_font = adopt_ref(*new ScaledFont(*this, point_size, point_size));
    m_scaled_fonts.set(point_size, scaled_font);
    return scaled_font;
}
}
