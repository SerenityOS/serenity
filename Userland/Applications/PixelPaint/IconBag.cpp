/*
 * Copyright (c) 2022, Brandon Jordan <brandonjordan124@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Applications/PixelPaint/IconBag.h>

namespace PixelPaint {
ErrorOr<IconBag> IconBag::create()
{
    IconBag icon_bag;

    icon_bag.filetype_pixelpaint = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-pixelpaint.png"sv));
    icon_bag.new_clipboard = TRY(Gfx::Bitmap::load_from_file("/res/icons/pixelpaint/new-clipboard.png"sv));
    icon_bag.file_export = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/file-export.png"sv));
    icon_bag.close_image = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/close-tab.png"sv));
    icon_bag.edit_copy = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/edit-copy.png"sv));
    icon_bag.clear_selection = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/clear-selection.png"sv));
    icon_bag.invert_selection = TRY(Gfx::Bitmap::load_from_file("/res/icons/pixelpaint/invert-selection.png"sv));
    icon_bag.swap_colors = TRY(Gfx::Bitmap::load_from_file("/res/icons/pixelpaint/swap-colors.png"sv));
    icon_bag.default_colors = TRY(Gfx::Bitmap::load_from_file("/res/icons/pixelpaint/default-colors.png"sv));
    icon_bag.load_color_palette = TRY(Gfx::Bitmap::load_from_file("/res/icons/pixelpaint/load-color-palette.png"sv));
    icon_bag.save_color_palette = TRY(Gfx::Bitmap::load_from_file("/res/icons/pixelpaint/save-color-palette.png"sv));
    icon_bag.fit_image_to_view = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/fit-image-to-view.png"sv));
    icon_bag.add_guide = TRY(Gfx::Bitmap::load_from_file("/res/icons/pixelpaint/add-guide.png"sv));
    icon_bag.clear_guides = TRY(Gfx::Bitmap::load_from_file("/res/icons/pixelpaint/clear-guides.png"sv));
    icon_bag.edit_flip_vertical = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/edit-flip-vertical.png"sv));
    icon_bag.edit_flip_horizontal = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/edit-flip-horizontal.png"sv));
    icon_bag.resize_image = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/selection-move.png"sv));
    icon_bag.crop = TRY(Gfx::Bitmap::load_from_file("/res/icons/pixelpaint/crop.png"sv));
    icon_bag.new_layer = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/new-layer.png"sv));
    icon_bag.previous_layer = TRY(Gfx::Bitmap::load_from_file("/res/icons/pixelpaint/previous-layer.png"sv));
    icon_bag.next_layer = TRY(Gfx::Bitmap::load_from_file("/res/icons/pixelpaint/next-layer.png"sv));
    icon_bag.top_layer = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/top-layer.png"sv));
    icon_bag.bottom_layer = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/bottom-layer.png"sv));
    icon_bag.active_layer_up = TRY(Gfx::Bitmap::load_from_file("/res/icons/pixelpaint/active-layer-up.png"sv));
    icon_bag.active_layer_down = TRY(Gfx::Bitmap::load_from_file("/res/icons/pixelpaint/active-layer-down.png"sv));
    icon_bag.delete_layer = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/delete.png"sv));
    icon_bag.flatten_image = TRY(Gfx::Bitmap::load_from_file("/res/icons/pixelpaint/flatten-image.png"sv));
    icon_bag.merge_visible = TRY(Gfx::Bitmap::load_from_file("/res/icons/pixelpaint/merge-visible.png"sv));
    icon_bag.merge_active_layer_up = TRY(Gfx::Bitmap::load_from_file("/res/icons/pixelpaint/merge-active-layer-up.png"sv));
    icon_bag.merge_active_layer_down = TRY(Gfx::Bitmap::load_from_file("/res/icons/pixelpaint/merge-active-layer-down.png"sv));
    icon_bag.filter = TRY(Gfx::Bitmap::load_from_file("/res/icons/pixelpaint/filter.png"sv));
    icon_bag.generic_5x5_convolution = TRY(Gfx::Bitmap::load_from_file("/res/icons/pixelpaint/generic-5x5-convolution.png"sv));
    icon_bag.levels = TRY(Gfx::Bitmap::load_from_file("/res/icons/pixelpaint/levels.png"sv));
    icon_bag.add_mask = TRY(Gfx::Bitmap::load_from_file("/res/icons/pixelpaint/add-mask.png"sv));

    return icon_bag;
}
}
