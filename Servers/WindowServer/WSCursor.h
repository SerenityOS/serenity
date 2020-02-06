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

#include <LibDraw/GraphicsBitmap.h>

enum class WSStandardCursor {
    None = 0,
    Arrow,
    IBeam,
    ResizeHorizontal,
    ResizeVertical,
    ResizeDiagonalTLBR,
    ResizeDiagonalBLTR,
    Hand,
    Drag,
};

class WSCursor : public RefCounted<WSCursor> {
public:
    static NonnullRefPtr<WSCursor> create(NonnullRefPtr<Gfx::Bitmap>&&, const Gfx::Point& hotspot);
    static NonnullRefPtr<WSCursor> create(NonnullRefPtr<Gfx::Bitmap>&&);
    static RefPtr<WSCursor> create(WSStandardCursor);
    ~WSCursor();

    Point hotspot() const { return m_hotspot; }
    const Gfx::Bitmap& bitmap() const { return *m_bitmap; }

    Rect rect() const { return m_bitmap->rect(); }
    Size size() const { return m_bitmap->size(); }

private:
    WSCursor(NonnullRefPtr<Gfx::Bitmap>&&, const Gfx::Point&);

    RefPtr<Gfx::Bitmap> m_bitmap;
    Gfx::Point m_hotspot;
};
