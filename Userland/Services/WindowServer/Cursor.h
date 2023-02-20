/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/CursorParams.h>
#include <LibGfx/StandardCursor.h>

namespace WindowServer {

class Cursor : public RefCounted<Cursor> {
public:
    static RefPtr<Cursor const> create(StringView, StringView);
    static NonnullRefPtr<Cursor const> create(NonnullRefPtr<Gfx::Bitmap const>&&, int);
    static RefPtr<Cursor const> create(Gfx::StandardCursor);
    ~Cursor() = default;

    Gfx::CursorParams const& params() const { return m_params; }
    Gfx::Bitmap const& bitmap(int scale_factor) const
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
    Cursor() = default;
    Cursor(NonnullRefPtr<Gfx::Bitmap const>&&, int, Gfx::CursorParams const&);

    bool load(StringView, StringView);
    void update_rect_if_animated();

    HashMap<int, NonnullRefPtr<Gfx::Bitmap const>> m_bitmaps;
    Gfx::CursorParams m_params;
    Gfx::IntRect m_rect;
};

}
