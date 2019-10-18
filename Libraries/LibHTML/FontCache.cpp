#include <LibDraw/Font.h>
#include <LibHTML/FontCache.h>

FontCache& FontCache::the()
{
    static FontCache cache;
    return cache;
}

RefPtr<Font> FontCache::get(const FontSelector& font_selector) const
{
    auto cached_font = m_fonts.get(font_selector);
    if (cached_font.has_value())
        return cached_font.value();
    return nullptr;
}

void FontCache::set(const FontSelector& font_selector, NonnullRefPtr<Font> font)
{
    m_fonts.set(font_selector, move(font));
}
