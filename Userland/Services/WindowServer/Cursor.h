/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <LibGfx/Bitmap.h>
#include <LibGfx/StandardCursor.h>

namespace WindowServer {

class CursorParams {
public:
    static CursorParams parse_from_file_name(const StringView&, const Gfx::IntPoint&);
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
