/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
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

RefPtr<Gfx::Font const> FontCache::get(FontSelector const& font_selector) const
{
    auto cached_font = m_fonts.get(font_selector);
    if (cached_font.has_value())
        return cached_font.value();
    return nullptr;
}

void FontCache::set(FontSelector const& font_selector, NonnullRefPtr<Gfx::Font const> font)
{
    m_fonts.set(font_selector, move(font));
}
