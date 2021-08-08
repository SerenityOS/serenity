/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibCrypto/Hash/SHA1.h>
#include <LibGfx/BitmapFont.h>
#include <LibGfx/TrueTypeFont/Font.h>

namespace RemoteGfx {

class RemoteGfxFontDatabase {
public:
    using FontDigestType = Crypto::Hash::SHA1::DigestType;

    class RemoteFontData : public RefCounted<RemoteFontData> {
        friend class RemoteGfxFontDatabase;

    public:
        RemoteFontData(RemoteGfxFontDatabase&, FontDigestType const&, NonnullRefPtr<Gfx::Font>);
        RemoteFontData(RemoteGfxFontDatabase&, FontDigestType const&, Gfx::Font::Type, ByteBuffer&&);
        ~RemoteFontData();

        auto& digest() const { return m_digest; }

        Gfx::Font::Type font_type() const { return m_ttf_font ? Gfx::Font::Type::Scaled : Gfx::Font::Type::Bitmap; }

        NonnullRefPtr<Gfx::Font> bitmap_font();
        NonnullRefPtr<Gfx::Font> scaled_font(u32);

    private:
        RemoteGfxFontDatabase* m_database { nullptr };
        FontDigestType m_digest;
        ByteBuffer m_bytes;
        RefPtr<TTF::Font> m_ttf_font;
        RefPtr<Gfx::BitmapFont> m_bitmap_font;
    };
    friend struct FontData;

    RemoteGfxFontDatabase();
    ~RemoteGfxFontDatabase();

    void populate_own_fonts();
    void clear();

    template<typename F>
    void for_each(F f)
    {
        for (auto& it : m_fonts)
            f(*it.value);
    }

    NonnullRefPtr<RemoteFontData> add_font(Gfx::Font::Type, ReadonlyBytes const&);
    RefPtr<RemoteFontData> find_font(FontDigestType const&);
    static FontDigestType calculate_digest(Gfx::Font const&);

private:
    NonnullRefPtr<RemoteFontData> ensure_font(Gfx::Font const&);
    HashMap<Crypto::Hash::HashableDigest<FontDigestType>, NonnullRefPtr<RemoteFontData>> m_fonts;
};

}
