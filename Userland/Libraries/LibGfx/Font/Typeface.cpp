/*
 * Copyright (c) 2020, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Font/ScaledFont.h>
#include <LibGfx/Font/Typeface.h>

namespace Gfx {

unsigned Typeface::weight() const
{
    VERIFY(m_vector_font || m_bitmap_fonts.size() > 0);

    if (is_fixed_size())
        return m_bitmap_fonts[0]->weight();

    return m_vector_font->weight();
}

unsigned Typeface::width() const
{
    VERIFY(m_vector_font || m_bitmap_fonts.size() > 0);

    if (is_fixed_size())
        return Gfx::FontWidth::Normal;

    return m_vector_font->width();
}

u8 Typeface::slope() const
{
    VERIFY(m_vector_font || m_bitmap_fonts.size() > 0);

    if (is_fixed_size())
        return m_bitmap_fonts[0]->slope();

    return m_vector_font->slope();
}

bool Typeface::is_fixed_width() const
{
    VERIFY(m_vector_font || m_bitmap_fonts.size() > 0);

    if (is_fixed_size())
        return m_bitmap_fonts[0]->is_fixed_width();

    return m_vector_font->is_fixed_width();
}

void Typeface::add_bitmap_font(RefPtr<BitmapFont> font)
{
    m_bitmap_fonts.append(font);
}

void Typeface::set_vector_font(RefPtr<VectorFont> font)
{
    m_vector_font = move(font);
}

RefPtr<Font> Typeface::get_font(float point_size, Font::AllowInexactSizeMatch allow_inexact_size_match) const
{
    VERIFY(point_size >= 0);

    if (m_vector_font)
        return m_vector_font->scaled_font(point_size);

    RefPtr<BitmapFont> best_match;
    int size = roundf(point_size);
    int best_delta = NumericLimits<int>::max();

    for (auto font : m_bitmap_fonts) {
        if (font->presentation_size() == size)
            return font;
        if (allow_inexact_size_match != Font::AllowInexactSizeMatch::No) {
            int delta = static_cast<int>(font->presentation_size()) - static_cast<int>(size);
            if (abs(delta) == best_delta) {
                if (allow_inexact_size_match == Font::AllowInexactSizeMatch::Larger && delta > 0) {
                    best_match = font;
                } else if (allow_inexact_size_match == Font::AllowInexactSizeMatch::Smaller && delta < 0) {
                    best_match = font;
                }
            } else if (abs(delta) < best_delta) {
                best_match = font;
                best_delta = abs(delta);
            }
        }
    }

    if (allow_inexact_size_match != Font::AllowInexactSizeMatch::No && best_match)
        return best_match;

    return {};
}

void Typeface::for_each_fixed_size_font(Function<void(Font const&)> callback) const
{
    for (auto font : m_bitmap_fonts) {
        callback(*font);
    }
}

}
