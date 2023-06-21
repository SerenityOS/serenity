/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <WindowServer/Cursor.h>
#include <WindowServer/Screen.h>
#include <WindowServer/WindowManager.h>

namespace WindowServer {

Cursor::Cursor(NonnullRefPtr<Gfx::Bitmap const>&& bitmap, int scale_factor, Gfx::CursorParams const& cursor_params)
    : m_params(cursor_params.constrained(*bitmap))
    , m_rect(bitmap->rect())
{
    m_bitmaps.set(scale_factor, move(bitmap));
    update_rect_if_animated();
}

void Cursor::update_rect_if_animated()
{
    if (m_params.frames() > 1) {
        VERIFY(m_rect.width() % m_params.frames() == 0);
        m_rect.set_width(m_rect.width() / m_params.frames());
    }
}

NonnullRefPtr<Cursor const> Cursor::create(NonnullRefPtr<Gfx::Bitmap const>&& bitmap, int scale_factor)
{
    auto hotspot = bitmap->rect().center();
    return adopt_ref(*new Cursor(move(bitmap), scale_factor, Gfx::CursorParams(hotspot)));
}

RefPtr<Cursor const> Cursor::create(StringView filename, StringView default_filename)
{
    auto cursor = adopt_ref(*new Cursor());
    if (cursor->load(filename, default_filename))
        return cursor;
    return {};
}

bool Cursor::load(StringView filename, StringView default_filename)
{
    bool did_load_any = false;

    auto load_bitmap = [&](StringView path, int scale_factor) {
        auto bitmap_or_error = Gfx::Bitmap::load_from_file(path, scale_factor);
        if (bitmap_or_error.is_error())
            return;
        did_load_any = true;
        m_bitmaps.set(scale_factor, bitmap_or_error.release_value());
    };

    Screen::for_each_scale_factor_in_use([&](int scale_factor) {
        load_bitmap(filename, scale_factor);
        return IterationDecision::Continue;
    });
    if (!did_load_any) {
        Screen::for_each_scale_factor_in_use([&](int scale_factor) {
            load_bitmap(default_filename, scale_factor);
            return IterationDecision::Continue;
        });
    }
    if (did_load_any) {
        auto& bitmap = this->bitmap(1);
        m_rect = bitmap.rect();
        m_params = Gfx::CursorParams::parse_from_filename(filename, m_rect.center()).constrained(bitmap);
        update_rect_if_animated();
    }
    return did_load_any;
}

RefPtr<Cursor const> Cursor::create(Gfx::StandardCursor standard_cursor)
{
    switch (standard_cursor) {
    case Gfx::StandardCursor::None:
        return nullptr;
    case Gfx::StandardCursor::Hidden:
        return WindowManager::the().hidden_cursor();
    case Gfx::StandardCursor::Arrow:
        return WindowManager::the().arrow_cursor();
    case Gfx::StandardCursor::Crosshair:
        return WindowManager::the().crosshair_cursor();
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
    case Gfx::StandardCursor::DragCopy:
        return WindowManager::the().drag_copy_cursor();
    case Gfx::StandardCursor::Move:
        return WindowManager::the().move_cursor();
    case Gfx::StandardCursor::Wait:
        return WindowManager::the().wait_cursor();
    case Gfx::StandardCursor::Disallowed:
        return WindowManager::the().disallowed_cursor();
    case Gfx::StandardCursor::Eyedropper:
        return WindowManager::the().eyedropper_cursor();
    case Gfx::StandardCursor::Zoom:
        return WindowManager::the().zoom_cursor();
    default:
        VERIFY_NOT_REACHED();
    }
}

}
