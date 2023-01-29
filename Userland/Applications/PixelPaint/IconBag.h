/*
 * Copyright (c) 2022, Brandon Jordan <brandonjordan124@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <LibGfx/Bitmap.h>

namespace PixelPaint {
struct IconBag final {
    static ErrorOr<IconBag> create();

    RefPtr<Gfx::Bitmap> filetype_pixelpaint { nullptr };
    RefPtr<Gfx::Bitmap> new_clipboard { nullptr };
    RefPtr<Gfx::Bitmap> file_export { nullptr };
    RefPtr<Gfx::Bitmap> close_image { nullptr };
    RefPtr<Gfx::Bitmap> edit_copy { nullptr };
    RefPtr<Gfx::Bitmap> clear_selection { nullptr };
    RefPtr<Gfx::Bitmap> invert_selection { nullptr };
    RefPtr<Gfx::Bitmap> swap_colors { nullptr };
    RefPtr<Gfx::Bitmap> default_colors { nullptr };
    RefPtr<Gfx::Bitmap> load_color_palette { nullptr };
    RefPtr<Gfx::Bitmap> save_color_palette { nullptr };
    RefPtr<Gfx::Bitmap> fit_image_to_view { nullptr };
    RefPtr<Gfx::Bitmap> add_guide { nullptr };
    RefPtr<Gfx::Bitmap> clear_guides { nullptr };
    RefPtr<Gfx::Bitmap> edit_flip_vertical { nullptr };
    RefPtr<Gfx::Bitmap> edit_flip_horizontal { nullptr };
    RefPtr<Gfx::Bitmap> resize_image { nullptr };
    RefPtr<Gfx::Bitmap> crop { nullptr };
    RefPtr<Gfx::Bitmap> new_layer { nullptr };
    RefPtr<Gfx::Bitmap> previous_layer { nullptr };
    RefPtr<Gfx::Bitmap> next_layer { nullptr };
    RefPtr<Gfx::Bitmap> top_layer { nullptr };
    RefPtr<Gfx::Bitmap> bottom_layer { nullptr };
    RefPtr<Gfx::Bitmap> active_layer_up { nullptr };
    RefPtr<Gfx::Bitmap> active_layer_down { nullptr };
    RefPtr<Gfx::Bitmap> delete_layer { nullptr };
    RefPtr<Gfx::Bitmap> flatten_image { nullptr };
    RefPtr<Gfx::Bitmap> merge_visible { nullptr };
    RefPtr<Gfx::Bitmap> merge_active_layer_up { nullptr };
    RefPtr<Gfx::Bitmap> merge_active_layer_down { nullptr };
    RefPtr<Gfx::Bitmap> filter { nullptr };
    RefPtr<Gfx::Bitmap> generic_5x5_convolution { nullptr };
    RefPtr<Gfx::Bitmap> levels { nullptr };
    RefPtr<Gfx::Bitmap> add_mask { nullptr };
};
}
