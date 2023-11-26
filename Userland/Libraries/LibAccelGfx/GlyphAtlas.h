/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/Noncopyable.h>
#include <LibAccelGfx/GL.h>
#include <LibGfx/Font/Font.h>

namespace AccelGfx {

class GlyphAtlas {
    AK_MAKE_NONCOPYABLE(GlyphAtlas);

public:
    GlyphAtlas()
        : m_texture(GL::create_texture())
    {
    }

    ~GlyphAtlas()
    {
        GL::delete_texture(m_texture);
    }

    static GlyphAtlas& the();

    struct GlyphsTextureKey {
        Gfx::Font const* font;
        u32 code_point;

        bool operator==(GlyphsTextureKey const& other) const
        {
            return font == other.font && code_point == other.code_point;
        }
    };

    void update(HashMap<Gfx::Font const*, HashTable<u32>> const& unique_glyphs);
    Optional<Gfx::IntRect> get_glyph_rect(Gfx::Font const*, u32 code_point) const;

    GL::Texture const& texture() const { return m_texture; }

private:
    GL::Texture m_texture;
    HashMap<GlyphsTextureKey, Gfx::IntRect> m_glyphs_texture_map;
};

}

namespace AK {

template<>
struct Traits<AccelGfx::GlyphAtlas::GlyphsTextureKey> : public DefaultTraits<AccelGfx::GlyphAtlas::GlyphsTextureKey> {
    static unsigned hash(AccelGfx::GlyphAtlas::GlyphsTextureKey const& key)
    {
        return pair_int_hash(ptr_hash(key.font), key.code_point);
    }
};

}
