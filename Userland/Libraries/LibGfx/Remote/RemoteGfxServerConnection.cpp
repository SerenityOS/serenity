/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <AK/Debug.h>
#include <LibGfx/Font.h>
#include <LibGfx/Remote/RemoteGfx.h>
#include <LibGfx/Remote/RemoteGfxServerConnection.h>

namespace RemoteGfx {

RemoteGfxServerConnection* RemoteGfxServerConnection::s_the;

RemoteGfxServerConnection& RemoteGfxServerConnection::the()
{
    if (!s_the)
        s_the = new RemoteGfxServerConnection();
    return *s_the;
}

RemoteGfxServerConnection::RemoteGfxServerConnection()
    : IPC::ServerConnection<RemoteGfxClientEndpoint, RemoteGfxServerEndpoint>(*this, "/tmp/portal/remotegfx")
{
}

void RemoteGfxServerConnection::enable_remote_gfx(u64 cookie)
{
    m_cookie = cookie;
    m_enabled = true;
    m_session = adopt_ref(*new RemoteGfxSession(*this));
    m_remote_fonts.clear();
    if (on_new_session)
        on_new_session(*m_session);
}

void RemoteGfxServerConnection::disable_remote_gfx()
{
    m_enabled = false;
    auto session = move(m_session);
    if (session && on_session_end)
        on_session_end(*session);
}

void RemoteGfxServerConnection::notify_remote_fonts(const Vector<ByteBuffer>& available_remote_fonts)
{
    for (auto& remote_font : available_remote_fonts) {
        VERIFY(remote_font.size() == RemoteGfxFontDatabase::FontDigestType::Size);
        RemoteGfxFontDatabase::FontDigestType digest;
        __builtin_memcpy(digest.data, remote_font.data(), RemoteGfxFontDatabase::FontDigestType::Size);
        m_remote_fonts.set(digest);
        dbgln("Received a font that is available remotely");
    }
}

void RemoteGfxServerConnection::create_font_and_send_if_needed(i32 id, Gfx::Font const& font)
{
    auto digest = RemoteGfxFontDatabase::calculate_digest(font);
    if (m_remote_fonts.find(digest) == m_remote_fonts.end()) {
        auto result = m_remote_fonts.set({ digest });
        VERIFY(result == AK::HashSetResult::InsertedNewEntry);
        switch (font.font_type()) {
        case Gfx::Font::Type::Bitmap:
            async_create_bitmap_font_from_data(id, ByteBuffer::copy(font.bytes()).release_value()); // TODO: avoid copy
            break;
        case Gfx::Font::Type::Scaled:
            async_create_scalable_font_from_data(id, ByteBuffer::copy(font.bytes()).release_value(), font.presentation_size()); // TODO: avoid copy
            break;
        default:
            VERIFY_NOT_REACHED();
        }
    } else {
        switch (font.font_type()) {
        case Gfx::Font::Type::Bitmap:
            async_create_bitmap_font_from_digest(id, ByteBuffer::copy(digest.data, digest.data_length()).release_value());
            break;
        case Gfx::Font::Type::Scaled:
            async_create_scalable_font_from_digest(id, ByteBuffer::copy(digest.data, digest.data_length()).release_value(), font.presentation_size());
            break;
        default:
            VERIFY_NOT_REACHED();
        }
    }
}

}
