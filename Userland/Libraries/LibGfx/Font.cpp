/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/BitmapView.h>
#include <LibGfx/Font.h>
#include <LibGfx/Remote/RemoteGfxServerConnection.h>

namespace Gfx {

GlyphBitmap::GlyphBitmap(IntSize const& size, BitmapView const& bitmap)
    : OneBitBitmap(OneBitBitmap::Type::GlyphBitmap, size)
    , m_own_rows(true)
{
    VERIFY((size_t)size.width() <= sizeof(m_rows[0]) * 8);
    auto* rows = new u8[size.height() * bytes_per_row()];
    for (int y = 0; y < size.height(); y++) {
        auto& row_bits = rows[y * bytes_per_row()];
        row_bits = 0;
        for (int x = 0; x < size.width(); x++) {
            if (bitmap.get(y * size.width() + x))
                row_bits |= 1 << x;
        }
    }

    m_rows = rows;
}

GlyphBitmap::~GlyphBitmap()
{
    if (m_own_rows)
        delete[] const_cast<u8*>(m_rows);
}

Font::RemoteData::RemoteData(RemoteGfx::RemoteGfxSession& session)
    : session(session)
{
}

Font::RemoteData::~RemoteData()
{
}

int Font::enable_remote_painting(bool enable)
{
    if (enable) {
#ifdef __serenity__
        if (m_remote_data) {
            VERIFY(m_remote_data->font_id > 0);
            return 0;
        }
        auto remote_gfx_session = RemoteGfx::RemoteGfxServerConnection::the().session();
        if (!remote_gfx_session)
            return 0;
        if (!m_remote_data)
            m_remote_data = adopt_own(*new RemoteData(*remote_gfx_session));
        auto& remote_data = *m_remote_data;
        VERIFY(remote_data.font_id == 0);
        static i32 s_font_id = 0;
        remote_data.font_id = ++s_font_id;
        VERIFY(remote_data.font_id > 0);

        remote_gfx_session->connection().create_font_and_send_if_needed(remote_data.font_id, *this);
        return remote_data.font_id;
#endif
    }

    m_remote_data = nullptr;
    return 0;
}

}
