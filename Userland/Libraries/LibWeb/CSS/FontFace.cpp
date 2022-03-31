/*
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/FontFace.h>

namespace Web::CSS {

FontFace::FontFace(FlyString font_family, Vector<Source> sources, Vector<UnicodeRange> unicode_ranges)
    : m_font_family(move(font_family))
    , m_sources(move(sources))
    , m_unicode_ranges(move(unicode_ranges))
{
}

}
