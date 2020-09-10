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

#include <WindowServer/Cursor.h>
#include <WindowServer/WindowManager.h>

namespace WindowServer {

Cursor::Cursor(NonnullRefPtr<Gfx::Bitmap>&& bitmap, const Gfx::IntPoint& hotspot)
    : m_bitmap(move(bitmap))
    , m_hotspot(hotspot)
{
}

Cursor::~Cursor()
{
}

NonnullRefPtr<Cursor> Cursor::create(NonnullRefPtr<Gfx::Bitmap>&& bitmap)
{
    return adopt(*new Cursor(move(bitmap), bitmap->rect().center()));
}

NonnullRefPtr<Cursor> Cursor::create(NonnullRefPtr<Gfx::Bitmap>&& bitmap, const Gfx::IntPoint& hotspot)
{
    return adopt(*new Cursor(move(bitmap), hotspot));
}

RefPtr<Cursor> Cursor::create(Gfx::StandardCursor standard_cursor)
{
    switch (standard_cursor) {
    case Gfx::StandardCursor::None:
        return nullptr;
    case Gfx::StandardCursor::Arrow:
        return WindowManager::the().arrow_cursor();
    case Gfx::StandardCursor::IBeam:
        return WindowManager::the().i_beam_cursor();
    case Gfx::StandardCursor::ResizeHorizontal:
        return WindowManager::the().resize_horizontally_cursor();
    case Gfx::StandardCursor::ResizeVertical:
        return WindowManager::the().resize_vertically_cursor();
    case Gfx::StandardCursor::ResizeDiagonalTLBR:
        return WindowManager::the().resize_diagonally_tlbr_cursor();
    case Gfx::StandardCursor::ResizeDiagonalBLTR:
        return WindowManager::the().resize_diagonally_bltr_cursor();
    case Gfx::StandardCursor::ResizeColumn:
        return WindowManager::the().resize_column_cursor();
    case Gfx::StandardCursor::ResizeRow:
        return WindowManager::the().resize_row_cursor();
    case Gfx::StandardCursor::Hand:
        return WindowManager::the().hand_cursor();
    case Gfx::StandardCursor::Help:
        return WindowManager::the().help_cursor();
    case Gfx::StandardCursor::Drag:
        return WindowManager::the().drag_cursor();
    case Gfx::StandardCursor::Move:
        return WindowManager::the().move_cursor();
    case Gfx::StandardCursor::Wait:
        return WindowManager::the().wait_cursor();
    }
    ASSERT_NOT_REACHED();
}

}
