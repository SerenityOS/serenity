/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <LibCrypto/Hash/SHA1.h>
#include <LibGfx/FontDatabase.h>
#include <LibGfx/Remote/RemoteFontDatabase.h>

namespace RemoteGfx {

RemoteGfxFontDatabase::RemoteGfxFontDatabase()
{
}

RemoteGfxFontDatabase::~RemoteGfxFontDatabase()
{
    for (auto& it : m_fonts)
        it.value->m_database = nullptr;
}

RemoteGfxFontDatabase::RemoteFontData::RemoteFontData(RemoteGfxFontDatabase& database, FontDigestType const& digest, NonnullRefPtr<Gfx::Font> font)
    : m_database(&database)
    , m_digest(digest)
{
    switch (font->font_type()) {
    case Gfx::Font::Type::Bitmap:
        m_bitmap_font = static_cast<Gfx::BitmapFont&>(*font);
        break;
    case Gfx::Font::Type::Scaled:
        m_ttf_font = static_cast<TTF::ScaledFont&>(*font).ttf_font();
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}

RemoteGfxFontDatabase::RemoteFontData::RemoteFontData(RemoteGfxFontDatabase& database, FontDigestType const& digest, Gfx::Font::Type font_type, ByteBuffer&& bytes)
    : m_database(&database)
    , m_digest(digest)
    , m_bytes(move(bytes))
{
    switch (font_type) {
    case Gfx::Font::Type::Bitmap:
        m_bitmap_font = Gfx::BitmapFont::load_from_memory(m_bytes);
        VERIFY(m_bitmap_font);
        break;
    case Gfx::Font::Type::Scaled:
        m_ttf_font = TTF::Font::try_load_from_externally_owned_memory(m_bytes).release_value();
        VERIFY(m_ttf_font);
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}

NonnullRefPtr<Gfx::Font> RemoteGfxFontDatabase::RemoteFontData::bitmap_font()
{
    VERIFY(font_type() == Gfx::Font::Type::Bitmap);
    VERIFY(m_bitmap_font);
    return *m_bitmap_font;
}

NonnullRefPtr<Gfx::Font> RemoteGfxFontDatabase::RemoteFontData::scaled_font(u32 size)
{
    VERIFY(font_type() == Gfx::Font::Type::Scaled);
    VERIFY(m_ttf_font);
    return AK::adopt_ref(*static_cast<Gfx::Font*>(new TTF::ScaledFont(*m_ttf_font, size, size)));
}

RemoteGfxFontDatabase::RemoteFontData::~RemoteFontData()
{
    if (m_database)
        m_database->m_fonts.remove(m_digest);
}

void RemoteGfxFontDatabase::populate_own_fonts()
{
    Gfx::FontDatabase::the().for_each_font([&](auto& font) {
        ensure_font(font);
    });
}

auto RemoteGfxFontDatabase::add_font(Gfx::Font::Type font_type, ReadonlyBytes const& bytes) -> NonnullRefPtr<RemoteFontData>
{
    Crypto::Hash::SHA1 sha;
    sha.update(bytes);
    auto digest = sha.digest();
    if (auto existing_font = find_font(digest)) {
        VERIFY(existing_font->font_type() == font_type);
        return existing_font.release_nonnull();
    }
    auto font_data = adopt_ref(*new RemoteFontData(*this, digest, font_type, ByteBuffer::copy(bytes).release_value()));
    auto result = m_fonts.set(digest, font_data);
    VERIFY(result == AK::HashSetResult::InsertedNewEntry);
    return font_data;
}

auto RemoteGfxFontDatabase::find_font(FontDigestType const& digest) -> RefPtr<RemoteFontData>
{
    auto it = m_fonts.find(digest);
    if (it != m_fonts.end())
        return it->value;
    return {};
}

auto RemoteGfxFontDatabase::calculate_digest(Gfx::Font const& font) -> FontDigestType
{
    Crypto::Hash::SHA1 sha;
    sha.update(font.bytes());
    return sha.digest();
}

auto RemoteGfxFontDatabase::ensure_font(Gfx::Font const& font) -> NonnullRefPtr<RemoteFontData>
{
    auto digest = calculate_digest(font);
    auto it = m_fonts.find(digest);
    if (it != m_fonts.end())
        return it->value;

    auto font_data = adopt_ref(*new RemoteFontData(*this, digest, font));
    auto result = m_fonts.set(digest, font_data);
    VERIFY(result == AK::HashSetResult::InsertedNewEntry);
    return font_data;
}

void RemoteGfxFontDatabase::clear()
{
    m_fonts.clear();
}

}
