/*
 * Copyright (c) 2022, Brandon Jordan <brandonjordan124@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <Applications/PixelPaint/IconBag.h>

namespace PixelPaint {
ErrorOr<IconBag> IconBag::try_create()
{
    IconBag icon_bag;

    icon_bag.filetype_pixelpaint = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/filetype-pixelpaint.png"));
    icon_bag.new_clipboard = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/pixelpaint/new-clipboard.png"));
    icon_bag.file_export = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/file-export.png"));
    icon_bag.close_image = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/close-tab.png"));
    icon_bag.edit_copy = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/edit-copy.png"));
    icon_bag.clear_selection = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/clear-selection.png"));
    icon_bag.swap_colors = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/pixelpaint/swap-colors.png"));
    icon_bag.default_colors = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/pixelpaint/default-colors.png"));
    icon_bag.load_color_palette = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/pixelpaint/load-color-palette.png"));
    icon_bag.save_color_palette = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/pixelpaint/save-color-palette.png"));
    icon_bag.add_guide = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/pixelpaint/add-guide.png"));
    icon_bag.clear_guides = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/pixelpaint/clear-guides.png"));
    icon_bag.edit_flip_vertical = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/edit-flip-vertical.png"));
    icon_bag.edit_flip_horizontal = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/edit-flip-horizontal.png"));
    icon_bag.crop = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/pixelpaint/crop.png"));
    icon_bag.new_layer = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/new-layer.png"));
    icon_bag.previous_layer = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/pixelpaint/previous-layer.png"));
    icon_bag.next_layer = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/pixelpaint/next-layer.png"));
    icon_bag.top_layer = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/top-layer.png"));
    icon_bag.bottom_layer = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/bottom-layer.png"));
    icon_bag.active_layer_up = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/pixelpaint/active-layer-up.png"));
    icon_bag.active_layer_down = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/pixelpaint/active-layer-down.png"));
    icon_bag.delete_layer = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/delete.png"));
    icon_bag.merge_visible = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/pixelpaint/merge-visible.png"));
    icon_bag.merge_active_layer_up = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/pixelpaint/merge-active-layer-up.png"));
    icon_bag.merge_active_layer_down = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/pixelpaint/merge-active-layer-down.png"));
    icon_bag.filter = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/pixelpaint/filter.png"));

    return icon_bag;
}
}
