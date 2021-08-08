/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/WeakPtr.h>
#include <LibGfx/Size.h>

namespace RemoteGfx {
class RemoteGfxSession;
}

namespace Gfx {

class OneBitBitmap {
private:
    struct RemoteData {
        WeakPtr<RemoteGfx::RemoteGfxSession> session;
        int onebit_bitmap_id { 0 };
        bool dirty { false };

        RemoteData(RemoteGfx::RemoteGfxSession&);
    };
    friend struct RemoteData;

public:
    enum class Type {
        Empty,
        GlyphBitmap,
        CharacterBitmap
    };

    OneBitBitmap() = default;
    OneBitBitmap(OneBitBitmap&&) = default;
    OneBitBitmap& operator=(OneBitBitmap&&) = default;
    OneBitBitmap(Type, IntSize const&);

    OneBitBitmap(OneBitBitmap const& other)
        : m_size(other.m_size)
        , m_type(other.m_type)
    {
        // Intentionally do not copy m_remote_data
    }
    OneBitBitmap& operator=(OneBitBitmap const& other)
    {
        if (&other != this) {
            // Intentionally do not copy m_remote_data
            m_size = other.m_size;
            m_type = other.m_type;
            m_remote_data = nullptr;
        }
        return *this;
    }

    virtual ~OneBitBitmap();

    Type type() const { return m_type; }
    IntSize size() const { return m_size; }
    void set_bits(ByteBuffer const&);
    ByteBuffer get_bits() const;

    virtual bool bit_at(int x, int y) const = 0;
    virtual void set_bit_at(int x, int y, bool) = 0;

    RemoteGfx::RemoteGfxSession* remote_session() { return m_remote_data ? m_remote_data->session.ptr() : nullptr; }
    [[nodiscard]] int remote_onebit_bitmap_id() const { return (m_remote_data && m_remote_data->session.ptr()) ? m_remote_data->onebit_bitmap_id : 0; }
    int enable_remote_painting(bool);
    void destroy_remote_data();
    void send_to_remote();

protected:
    void set_dirty();

    IntSize m_size;
    Type m_type { Type::Empty };

private:
    OwnPtr<RemoteData> m_remote_data;
};

}
