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

u8 Typeface::slope() const
{
    VERIFY(m_ttf_font || m_bitmap_fonts.size() > 0);

    if (is_fixed_size())
        return m_bitmap_fonts[0]->slope();

    return m_ttf_font->slope();
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

RefPtr<Font> Typeface::get_font(unsigned size, Font::AllowInexactSizeMatch allow_inexact_size_match) const
{
    VERIFY(size < NumericLimits<int>::max());

    RefPtr<BitmapFont> best_match;
    int best_delta = NumericLimits<int>::max();

    for (auto font : m_bitmap_fonts) {
        if (font->presentation_size() == size)
            return font;
        if (allow_inexact_size_match == Font::AllowInexactSizeMatch::Yes) {
            int delta = static_cast<int>(font->presentation_size()) - static_cast<int>(size);
            if (abs(delta) < best_delta) {
                best_match = font;
                best_delta = abs(delta);
            }
        }
    }

    if (allow_inexact_size_match == Font::AllowInexactSizeMatch::Yes && best_match)
        return best_match;

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
