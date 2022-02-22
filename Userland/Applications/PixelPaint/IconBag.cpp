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

    icon_bag.filetype_pixelpaint = TRY(Gfx::Bitmap::try_request_resource("filetype-pixelpaint"));
    icon_bag.new_clipboard = TRY(Gfx::Bitmap::try_request_resource("new-clipboard"));
    icon_bag.file_export = TRY(Gfx::Bitmap::try_request_resource("file-export"));
    icon_bag.close_image = TRY(Gfx::Bitmap::try_request_resource("close-tab"));
    icon_bag.edit_copy = TRY(Gfx::Bitmap::try_request_resource("edit-copy"));
    icon_bag.clear_selection = TRY(Gfx::Bitmap::try_request_resource("clear-selection"));
    icon_bag.swap_colors = TRY(Gfx::Bitmap::try_request_resource("swap-colors"));
    icon_bag.default_colors = TRY(Gfx::Bitmap::try_request_resource("default-colors"));
    icon_bag.load_color_palette = TRY(Gfx::Bitmap::try_request_resource("load-color-palette"));
    icon_bag.save_color_palette = TRY(Gfx::Bitmap::try_request_resource("save-color-palette"));
    icon_bag.add_guide = TRY(Gfx::Bitmap::try_request_resource("add-guide"));
    icon_bag.clear_guides = TRY(Gfx::Bitmap::try_request_resource("clear-guides"));
    icon_bag.edit_flip_vertical = TRY(Gfx::Bitmap::try_request_resource("edit-flip-vertical"));
    icon_bag.edit_flip_horizontal = TRY(Gfx::Bitmap::try_request_resource("edit-flip-horizontal"));
    icon_bag.crop = TRY(Gfx::Bitmap::try_request_resource("crop"));
    icon_bag.new_layer = TRY(Gfx::Bitmap::try_request_resource("new-layer"));
    icon_bag.previous_layer = TRY(Gfx::Bitmap::try_request_resource("previous-layer"));
    icon_bag.next_layer = TRY(Gfx::Bitmap::try_request_resource("next-layer"));
    icon_bag.top_layer = TRY(Gfx::Bitmap::try_request_resource("top-layer"));
    icon_bag.bottom_layer = TRY(Gfx::Bitmap::try_request_resource("bottom-layer"));
    icon_bag.active_layer_up = TRY(Gfx::Bitmap::try_request_resource("active-layer-up"));
    icon_bag.active_layer_down = TRY(Gfx::Bitmap::try_request_resource("active-layer-down"));
    icon_bag.delete_layer = TRY(Gfx::Bitmap::try_request_resource("delete"));
    icon_bag.merge_visible = TRY(Gfx::Bitmap::try_request_resource("merge-visible"));
    icon_bag.merge_active_layer_up = TRY(Gfx::Bitmap::try_request_resource("merge-active-layer-up"));
    icon_bag.merge_active_layer_down = TRY(Gfx::Bitmap::try_request_resource("merge-active-layer-down"));
    icon_bag.filter = TRY(Gfx::Bitmap::try_request_resource("filter"));

    return icon_bag;
}
}
