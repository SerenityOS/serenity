/*
 * Copyright (c) 2020, Stephan Unverwerth <s.unverwerth@gmx.de>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibGfx/Typeface.h>

namespace Gfx {

unsigned Typeface::weight() const
{
    ASSERT(m_ttf_font || m_bitmap_fonts.size() > 0);

    if (is_fixed_size())
        return m_bitmap_fonts[0]->weight();

    return m_ttf_font->weight();
}

void Typeface::add_bitmap_font(RefPtr<BitmapFont> font)
{
    m_bitmap_fonts.append(font);
}

void Typeface::set_ttf_font(RefPtr<TTF::Font> font)
{
    m_ttf_font = font;
}

RefPtr<Font> Typeface::get_font(unsigned size)
{
    for (auto font : m_bitmap_fonts) {
        if (font->presentation_size() == size)
            return font;
    }

    if (m_ttf_font) {
        auto font = adopt(*new TTF::ScaledFont(m_ttf_font, size, size));
        return font;
    }

    return {};
}

void Typeface::for_each_fixed_size_font(Function<void(const Font&)> callback) const
{
    for (auto font : m_bitmap_fonts) {
        callback(*font);
    }
}

}
