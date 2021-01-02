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

#pragma once

#include <AK/Function.h>
#include <AK/RefCounted.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibGfx/BitmapFont.h>
#include <LibGfx/Font.h>
#include <LibTTF/Font.h>

namespace Gfx {

class Typeface : public RefCounted<Typeface> {
public:
    Typeface(const String& family, const String& variant)
        : m_family(family)
        , m_variant(variant)
    {
    }

    String family() const { return m_family; }
    String variant() const { return m_variant; }
    unsigned weight() const;

    bool is_fixed_width() const;
    bool is_fixed_size() const { return !m_bitmap_fonts.is_empty(); }
    void for_each_fixed_size_font(Function<void(const Font&)>) const;

    void add_bitmap_font(RefPtr<BitmapFont>);
    void set_ttf_font(RefPtr<TTF::Font>);

    RefPtr<Font> get_font(unsigned size);

private:
    String m_family;
    String m_variant;

    Vector<RefPtr<BitmapFont>> m_bitmap_fonts;
    RefPtr<TTF::Font> m_ttf_font;
};

}
