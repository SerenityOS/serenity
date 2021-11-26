/*
 * Copyright (c) 2020, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/RefCounted.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibGfx/BitmapFont.h>
#include <LibGfx/Font.h>
#include <LibGfx/TrueTypeFont/Font.h>

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

    RefPtr<Font> get_font(unsigned size) const;

private:
    String m_family;
    String m_variant;

    Vector<RefPtr<BitmapFont>> m_bitmap_fonts;
    RefPtr<TTF::Font> m_ttf_font;
};

}
