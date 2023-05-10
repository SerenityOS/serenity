/*
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Bitmap.h>

namespace FontEditor {

struct Resources final {
    static Resources create()
    {
        Resources resources;

        auto load_bitmap = [](StringView path) -> RefPtr<Gfx::Bitmap> {
            auto bitmap = Gfx::Bitmap::load_from_file(path);
            if (!bitmap.is_error())
                return bitmap.release_value();
            warnln("Loading Gfx::Bitmap \"{}\" failed: {}", path, bitmap.release_error());
            return nullptr;
        };

        resources.copy_as_text = load_bitmap("/res/icons/16x16/edit-copy.png"sv);
        resources.flip_horizontally = load_bitmap("/res/icons/16x16/edit-flip-horizontal.png"sv);
        resources.flip_vertically = load_bitmap("/res/icons/16x16/edit-flip-vertical.png"sv);
        resources.go_to_glyph = load_bitmap("/res/icons/16x16/go-to.png"sv);
        resources.move_glyph = load_bitmap("/res/icons/16x16/selection-move.png"sv);
        resources.new_font = load_bitmap("/res/icons/16x16/filetype-font.png"sv);
        resources.next_glyph = load_bitmap("/res/icons/16x16/go-forward.png"sv);
        resources.paint_glyph = load_bitmap("/res/icons/pixelpaint/pen.png"sv);
        resources.preview_font = load_bitmap("/res/icons/16x16/find.png"sv);
        resources.previous_glyph = load_bitmap("/res/icons/16x16/go-back.png"sv);
        resources.scale_editor = load_bitmap("/res/icons/16x16/scale.png"sv);

        return resources;
    }

    RefPtr<Gfx::Bitmap> copy_as_text;
    RefPtr<Gfx::Bitmap> flip_horizontally;
    RefPtr<Gfx::Bitmap> flip_vertically;
    RefPtr<Gfx::Bitmap> go_to_glyph;
    RefPtr<Gfx::Bitmap> move_glyph;
    RefPtr<Gfx::Bitmap> new_font;
    RefPtr<Gfx::Bitmap> next_glyph;
    RefPtr<Gfx::Bitmap> paint_glyph;
    RefPtr<Gfx::Bitmap> preview_font;
    RefPtr<Gfx::Bitmap> previous_glyph;
    RefPtr<Gfx::Bitmap> scale_editor;
};

}
