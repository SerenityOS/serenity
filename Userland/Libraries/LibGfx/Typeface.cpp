/*
 * Copyright (c) 2020, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Typeface.h>

namespace Gfx {

unsigned Typeface::weight() const
{
    VERIFY(m_ttf_font || m_bitmap_fonts.size() > 0);

    if (is_fixed_size())
        return m_bitmap_fonts[0]->weight();

    return m_ttf_font->weight();
}

bool Typeface::is_fixed_width() const
{
    VERIFY(m_ttf_font || m_bitmap_fonts.size() > 0);

    if (is_fixed_size())
        return m_bitmap_fonts[0]->is_fixed_width();

    return m_ttf_font->is_fixed_width();
}

void Typeface::add_bitmap_font(RefPtr<BitmapFont> font)
{
    m_bitmap_fonts.append(font);
}

void Typeface::set_ttf_font(RefPtr<TTF::Font> font)
{
    m_ttf_font = move(font);
}

RefPtr<Font> Typeface::get_font(unsigned size) const
{
    for (auto font : m_bitmap_fonts) {
        if (font->presentation_size() == size)
            return font;
    }

    if (m_ttf_font)
        return adopt_ref(*new TTF::ScaledFont(*m_ttf_font, size, size));

    return {};
}

void Typeface::for_each_fixed_size_font(Function<void(const Font&)> callback) const
{
    for (auto font : m_bitmap_fonts) {
        callback(*font);
    }
}

}
