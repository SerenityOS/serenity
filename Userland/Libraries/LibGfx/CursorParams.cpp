/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/LexicalPath.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/CursorParams.h>

namespace Gfx {

CursorParams CursorParams::parse_from_filename(StringView cursor_path, Gfx::IntPoint default_hotspot)
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
            auto parsed_number = params_str.substring_view(i, k - i).to_number<unsigned>();
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

CursorParams CursorParams::constrained(Gfx::Bitmap const& bitmap) const
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

}
