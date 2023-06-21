/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DeprecatedFlyString.h>
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

NonnullRefPtr<Gfx::Font const> FontCache::scaled_font(Gfx::Font const& font, float scale_factor)
{
    auto device_font_pt_size = font.point_size() * scale_factor;
    FontSelector font_selector = { FlyString::from_deprecated_fly_string(font.family()).release_value_but_fixme_should_propagate_errors(), device_font_pt_size, font.weight(), font.width(), font.slope() };
    if (auto cached_font = FontCache::the().get(font_selector)) {
        return *cached_font;
    }

    if (auto font_with_device_pt_size = font.with_size(device_font_pt_size)) {
        set(font_selector, *font_with_device_pt_size);
        return font_with_device_pt_size.release_nonnull();
    }

    return font;
}

void FontCache::set(FontSelector const& font_selector, NonnullRefPtr<Gfx::Font const> font)
{
    m_fonts.set(font_selector, move(font));
}
