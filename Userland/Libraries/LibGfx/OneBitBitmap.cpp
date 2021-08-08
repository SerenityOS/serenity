/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Bitmap.h>
#include <AK/ByteBuffer.h>
#include <LibGfx/OneBitBitmap.h>
#include <LibGfx/Remote/RemoteGfxServerConnection.h>

namespace Gfx {

OneBitBitmap::RemoteData::RemoteData([[maybe_unused]] RemoteGfx::RemoteGfxSession& session)
#ifdef __serenity__
    : session(session.make_weak_ptr<RemoteGfx::RemoteGfxSession>())
#endif
{
}

OneBitBitmap::OneBitBitmap(Type type, IntSize const& size)
    : m_size(size)
    , m_type(type)
{
}

OneBitBitmap::~OneBitBitmap()
{
    destroy_remote_data();
}

void OneBitBitmap::destroy_remote_data()
{
#ifdef __serenity__
    if (!m_remote_data)
        return;
    auto& remote_data = *m_remote_data;
    VERIFY(remote_data.onebit_bitmap_id > 0);
    if (auto* remote_gfx = remote_data.session.ptr())
        remote_gfx->connection().async_destroy_onebit_bitmap(remote_data.onebit_bitmap_id);
#endif
    m_remote_data = nullptr;
}

int OneBitBitmap::enable_remote_painting(bool enable)
{
    if (enable) {
#ifdef __serenity__
        if (m_remote_data) {
            VERIFY(m_remote_data->onebit_bitmap_id > 0);
            return 0;
        }
        auto remote_gfx_session = RemoteGfx::RemoteGfxServerConnection::the().session();
        if (!remote_gfx_session)
            return 0;
        if (!m_remote_data)
            m_remote_data = adopt_own(*new RemoteData(*remote_gfx_session));
        auto& remote_data = *m_remote_data;
        VERIFY(remote_data.onebit_bitmap_id == 0);
        static RemoteGfx::BitmapId s_onebit_bitmap_id = 0;
        remote_data.onebit_bitmap_id = ++s_onebit_bitmap_id;
        VERIFY(remote_data.onebit_bitmap_id > 0);
        remote_gfx_session->connection().async_create_onebit_bitmap(remote_data.onebit_bitmap_id, m_size, m_type, get_bits());
        return remote_data.onebit_bitmap_id;
#endif
    }

    m_remote_data = nullptr;
    return 0;
}

void OneBitBitmap::send_to_remote()
{
#ifdef __serenity__
    if (!m_remote_data)
        return;

    auto& remote_data = *m_remote_data;
    VERIFY(remote_data.onebit_bitmap_id > 0);
    if (auto* remote_gfx = remote_data.session.ptr()) {
        if (!m_remote_data->dirty)
            return;
        m_remote_data->dirty = false;
        remote_gfx->connection().async_set_onebit_bitmap_data(remote_data.onebit_bitmap_id, get_bits());
    } else {
        m_remote_data = nullptr;
    }
#endif
}

void OneBitBitmap::set_dirty()
{
    if (m_remote_data)
        m_remote_data->dirty = true;
}

void OneBitBitmap::set_bits(ByteBuffer const& bytes)
{
    AK::Bitmap bitmap(const_cast<u8*>(bytes.data()), bytes.size());
    for (int y = 0; y < m_size.height(); y++) {
        for (int x = 0; x < m_size.width(); x++)
            set_bit_at(x, y, bitmap.get(y * m_size.width() + x));
    }
}

ByteBuffer OneBitBitmap::get_bits() const
{
    auto bytes = ByteBuffer::create_zeroed(m_size.width() * m_size.height()).release_value();
    AK::Bitmap bitmap(bytes.data(), bytes.size());
    for (int y = 0; y < m_size.height(); y++) {
        for (int x = 0; x < m_size.width(); x++) {
            if (bit_at(x, y))
                bitmap.set(y * m_size.width() + x, true);
        }
    }
    return bytes;
}

}
