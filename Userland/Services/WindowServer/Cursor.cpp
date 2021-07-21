/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <WindowServer/Cursor.h>
#include <WindowServer/Screen.h>
#include <WindowServer/WindowManager.h>

namespace WindowServer {

CursorParams CursorParams::parse_from_filename(const StringView& cursor_path, const Gfx::IntPoint& default_hotspot)
{
    LexicalPath path(cursor_path);
    auto file_title = path.title();
    auto last_dot_in_title = file_title.find_last('.');
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
            dbgln("Failed to parse value for property '{}' from parsed cursor path: {}", property, cursor_path);
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
            dbgln("Ignore unknown property '{}' with value {} parsed from cursor path: {}", property, value.value(), cursor_path);
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
            dbgln("Cannot divide cursor dimensions {} into {} frames", rect, params.m_frames);
            params.m_frames = 1;
        }
    }
    if (params.m_have_hotspot)
        params.m_hotspot.constrain(rect);
    else
        params.m_hotspot = rect.center();
    return params;
}

Cursor::Cursor(NonnullRefPtr<Gfx::Bitmap>&& bitmap, int scale_factor, const CursorParams& cursor_params)
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

NonnullRefPtr<Cursor> Cursor::create(NonnullRefPtr<Gfx::Bitmap>&& bitmap, int scale_factor)
{
    auto hotspot = bitmap->rect().center();
    return adopt_ref(*new Cursor(move(bitmap), scale_factor, CursorParams(hotspot)));
}

RefPtr<Cursor> Cursor::create(const StringView& filename, const StringView& default_filename)
{
    auto cursor = adopt_ref(*new Cursor());
    if (cursor->load(filename, default_filename))
        return cursor;
    return {};
}

bool Cursor::load(const StringView& filename, const StringView& default_filename)
{
    bool did_load_any = false;

    auto load_bitmap = [&](const StringView& path, int scale_factor) {
        auto bitmap = Gfx::Bitmap::try_load_from_file(path, scale_factor);
        if (bitmap) {
            did_load_any = true;
            m_bitmaps.set(scale_factor, bitmap.release_nonnull());
        }
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
        m_params = CursorParams::parse_from_filename(filename, m_rect.center()).constrained(bitmap);
        update_rect_if_animated();
    }
    return did_load_any;
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
    case Gfx::StandardCursor::Disallowed:
        return WindowManager::the().disallowed_cursor();
    default:
        VERIFY_NOT_REACHED();
    }
}

}
