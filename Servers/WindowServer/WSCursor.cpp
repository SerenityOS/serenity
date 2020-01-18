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

#include <WindowServer/WSCursor.h>
#include <WindowServer/WSWindowManager.h>

WSCursor::WSCursor(NonnullRefPtr<GraphicsBitmap>&& bitmap, const Point& hotspot)
    : m_bitmap(move(bitmap))
    , m_hotspot(hotspot)
{
}

WSCursor::~WSCursor()
{
}

NonnullRefPtr<WSCursor> WSCursor::create(NonnullRefPtr<GraphicsBitmap>&& bitmap)
{
    return adopt(*new WSCursor(move(bitmap), bitmap->rect().center()));
}

NonnullRefPtr<WSCursor> WSCursor::create(NonnullRefPtr<GraphicsBitmap>&& bitmap, const Point& hotspot)
{
    return adopt(*new WSCursor(move(bitmap), hotspot));
}

RefPtr<WSCursor> WSCursor::create(WSStandardCursor standard_cursor)
{
    switch (standard_cursor) {
    case WSStandardCursor::None:
        return nullptr;
    case WSStandardCursor::Arrow:
        return WSWindowManager::the().arrow_cursor();
    case WSStandardCursor::IBeam:
        return WSWindowManager::the().i_beam_cursor();
    case WSStandardCursor::ResizeHorizontal:
        return WSWindowManager::the().resize_horizontally_cursor();
    case WSStandardCursor::ResizeVertical:
        return WSWindowManager::the().resize_vertically_cursor();
    case WSStandardCursor::ResizeDiagonalTLBR:
        return WSWindowManager::the().resize_diagonally_tlbr_cursor();
    case WSStandardCursor::ResizeDiagonalBLTR:
        return WSWindowManager::the().resize_diagonally_bltr_cursor();
    case WSStandardCursor::Hand:
        return WSWindowManager::the().hand_cursor();
    case WSStandardCursor::Drag:
        return WSWindowManager::the().drag_cursor();
    }
    ASSERT_NOT_REACHED();
}
