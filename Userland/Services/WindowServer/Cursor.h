/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/StandardCursor.h>

namespace WindowServer {

class CursorParams {
    friend class Cursor;

public:
    static CursorParams parse_from_filename(const StringView&, const Gfx::IntPoint&);
    CursorParams(const Gfx::IntPoint& hotspot)
        : m_hotspot(hotspot)
    {
    }
    CursorParams constrained(const Gfx::Bitmap&) const;

    const Gfx::IntPoint& hotspot() const { return m_hotspot; }
    unsigned frames() const { return m_frames; }
    unsigned frame_ms() const { return m_frame_ms; }

private:
    CursorParams() = default;
    Gfx::IntPoint m_hotspot;
    unsigned m_frames { 1 };
    unsigned m_frame_ms { 0 };
    bool m_have_hotspot { false };
};

class Cursor : public RefCounted<Cursor> {
public:
    static RefPtr<Cursor> create(const StringView&, const StringView&);
    static NonnullRefPtr<Cursor> create(NonnullRefPtr<Gfx::Bitmap>&&, int);
    static RefPtr<Cursor> create(Gfx::StandardCursor);
    ~Cursor() = default;

    const CursorParams& params() const { return m_params; }
    const Gfx::Bitmap& bitmap(int scale_factor) const
    {
        auto it = m_bitmaps.find(scale_factor);
        if (it == m_bitmaps.end()) {
            it = m_bitmaps.find(1);
            if (it == m_bitmaps.end())
                it = m_bitmaps.begin();
        }
        // We better found something
        if (it == m_bitmaps.end()) {
            dbgln("Could not find any bitmap in this Cursor");
            VERIFY_NOT_REACHED();
        }
        return it->value;
    }

    Gfx::IntRect source_rect(unsigned frame) const
    {
        return m_rect.translated(frame * m_rect.width(), 0);
    }

    Gfx::IntRect rect() const { return m_rect; }
    Gfx::IntSize size() const { return m_rect.size(); }

private:
    Cursor() { }
    Cursor(NonnullRefPtr<Gfx::Bitmap>&&, int, const CursorParams&);

    bool load(const StringView&, const StringView&);
    void update_rect_if_animated();

    HashMap<int, NonnullRefPtr<Gfx::Bitmap>> m_bitmaps;
    CursorParams m_params;
    Gfx::IntRect m_rect;
};

}
