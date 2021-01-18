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

#include <AK/LexicalPath.h>
#include <WindowServer/Cursor.h>
#include <WindowServer/WindowManager.h>

namespace WindowServer {

CursorParams CursorParams::parse_from_file_name(const StringView& cursor_path, const Gfx::IntPoint& default_hotspot)
{
    LexicalPath path(cursor_path);
    if (!path.is_valid()) {
        dbgln("Cannot parse invalid cursor path, use default cursor params");
        return { default_hotspot };
    }
    auto file_title = path.title();
    auto last_dot_in_title = StringView(file_title).find_last_of('.');
    if (!last_dot_in_title.has_value() || last_dot_in_title.value() == 0) {
        // No encoded params in filename. Not an error, we'll just use defaults
        return { default_hotspot };
    }
    auto params_str = file_title.substring_view(last_dot_in_title.value() + 1);

    CursorParams params(default_hotspot);
    bool in_display_scale_part = false;
    for (size_t i = 0; i + 1 < params_str.length() && !in_display_scale_part;) {
        auto property = params_str[i++];

        auto value = [&]() -> Optional<size_t> {
            size_t k = i;
            while (k < params_str.length()) {
                auto ch = params_str[k];
                if (ch < '0' || ch > '9')
                    break;
                k++;
            }
            if (k == i)
                return {};
            auto parsed_number = params_str.substring_view(i, k - i).to_uint();
            if (!parsed_number.has_value())
                return {};
            i = k;
            return parsed_number.value();
        }();
        if (!value.has_value()) {
            dbg() << "Failed to parse value for property '" << property << "' from parsed cursor path: " << cursor_path;
            return { default_hotspot };
        }
        switch (property) {
        case 'x':
            params.m_hotspot.set_x(value.value());
            params.m_have_hotspot = true;
            break;
        case 'y':
            params.m_hotspot.set_y(value.value());
            params.m_have_hotspot = true;
            break;
        case 'f':
            if (value.value() > 1)
                params.m_frames = value.value();
            break;
        case 't':
            if (value.value() >= 100 && value.value() <= 1000)
                params.m_frame_ms = value.value();
            else
                dbgln("Cursor frame rate outside of valid range (100-1000ms)");
            break;
        case '-':
            in_display_scale_part = true;
            break;
        default:
            dbg() << "Ignore unknown property '" << property << "' with value " << value.value() << " parsed from cursor path: " << cursor_path;
            return { default_hotspot };
        }
    }
    return params;
}

CursorParams CursorParams::constrained(const Gfx::Bitmap& bitmap) const
{
    CursorParams params(*this);
    auto rect = bitmap.rect();
    if (params.m_frames > 1) {
        if (rect.width() % params.m_frames == 0) {
            rect.set_width(rect.width() / (int)params.m_frames);
        } else {
            dbg() << "Cannot divide cursor dimensions " << rect << " into " << params.m_frames << " frames";
            params.m_frames = 1;
        }
    }
    if (params.m_have_hotspot)
        params.m_hotspot = params.m_hotspot.constrained(rect);
    else
        params.m_hotspot = rect.center();
    return params;
}

Cursor::Cursor(NonnullRefPtr<Gfx::Bitmap>&& bitmap, const CursorParams& cursor_params)
    : m_bitmap(move(bitmap))
    , m_params(cursor_params.constrained(*m_bitmap))
    , m_rect(m_bitmap->rect())
{
    if (m_params.frames() > 1) {
        ASSERT(m_rect.width() % m_params.frames() == 0);
        m_rect.set_width(m_rect.width() / m_params.frames());
    }
}

Cursor::~Cursor()
{
}

NonnullRefPtr<Cursor> Cursor::create(NonnullRefPtr<Gfx::Bitmap>&& bitmap)
{
    auto hotspot = bitmap->rect().center();
    return adopt(*new Cursor(move(bitmap), CursorParams(hotspot)));
}

NonnullRefPtr<Cursor> Cursor::create(NonnullRefPtr<Gfx::Bitmap>&& bitmap, const StringView& filename)
{
    auto default_hotspot = bitmap->rect().center();
    return adopt(*new Cursor(move(bitmap), CursorParams::parse_from_file_name(filename, default_hotspot)));
}

RefPtr<Cursor> Cursor::create(Gfx::StandardCursor standard_cursor)
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
    case Gfx::StandardCursor::Move:
        return WindowManager::the().move_cursor();
    case Gfx::StandardCursor::Wait:
        return WindowManager::the().wait_cursor();
    default:
        ASSERT_NOT_REACHED();
    }
}

}
