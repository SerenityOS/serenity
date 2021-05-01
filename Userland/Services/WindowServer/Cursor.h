/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Bitmap.h>
#include <LibGfx/StandardCursor.h>

namespace WindowServer {

class CursorParams {
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
    static NonnullRefPtr<Cursor> create(NonnullRefPtr<Gfx::Bitmap>&&, const StringView&);
    static NonnullRefPtr<Cursor> create(NonnullRefPtr<Gfx::Bitmap>&&);
    static RefPtr<Cursor> create(Gfx::StandardCursor);
    ~Cursor();

    const CursorParams& params() const { return m_params; }
    const Gfx::Bitmap& bitmap() const { return *m_bitmap; }

    Gfx::IntRect source_rect(unsigned frame) const
    {
        return m_rect.translated(frame * m_rect.width(), 0);
    }

    Gfx::IntRect rect() const { return m_rect; }
    Gfx::IntSize size() const { return m_rect.size(); }

private:
    Cursor(NonnullRefPtr<Gfx::Bitmap>&&, const CursorParams&);

    RefPtr<Gfx::Bitmap> m_bitmap;
    CursorParams m_params;
    Gfx::IntRect m_rect;
};

}
