/*
 * Copyright (c) 2022-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/FontFace.h>

namespace Web::CSS {

FontFace::FontFace(FlyString font_family, Optional<int> weight, Optional<int> slope, Vector<Source> sources, Vector<Gfx::UnicodeRange> unicode_ranges)
    : m_font_family(move(font_family))
    , m_weight(weight)
    , m_slope(slope)
    , m_sources(move(sources))
    , m_unicode_ranges(move(unicode_ranges))
{
}

}
