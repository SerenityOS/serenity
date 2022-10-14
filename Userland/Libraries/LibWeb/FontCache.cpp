/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Font/Font.h>
#include <LibWeb/FontCache.h>

FontCache& FontCache::the()
{
    static FontCache cache;
    return cache;
}

RefPtr<Gfx::Font> FontCache::get(FontSelector const& font_selector) const
{
    auto cached_font = m_fonts.get(font_selector);
    if (cached_font.has_value())
        return cached_font.value();
    return nullptr;
}

void FontCache::set(FontSelector const& font_selector, NonnullRefPtr<Gfx::Font> font)
{
    m_fonts.set(font_selector, move(font));
}
