/*
 * Copyright (c) 2020, Stephan Unverwerth <s.unverwerth@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/Function.h>
#include <AK/RefCounted.h>
#include <AK/Vector.h>
#include <LibGfx/Font/BitmapFont.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Font/VectorFont.h>

namespace Gfx {

class Typeface : public RefCounted<Typeface> {
public:
    Typeface(String const& family, String const& variant)
        : m_family(family)
        , m_variant(variant)
    {
    }

    FlyString const& family() const { return m_family; }
    FlyString const& variant() const { return m_variant; }
    unsigned weight() const;
    u8 slope() const;

    bool is_fixed_width() const;
    bool is_fixed_size() const { return !m_bitmap_fonts.is_empty(); }
    void for_each_fixed_size_font(Function<void(Font const&)>) const;

    void add_bitmap_font(RefPtr<BitmapFont>);
    void set_vector_font(RefPtr<VectorFont>);

    RefPtr<Font> get_font(float point_size, Font::AllowInexactSizeMatch = Font::AllowInexactSizeMatch::No) const;

private:
    FlyString m_family;
    FlyString m_variant;

    Vector<RefPtr<BitmapFont>> m_bitmap_fonts;
    RefPtr<VectorFont> m_vector_font;
};

}
