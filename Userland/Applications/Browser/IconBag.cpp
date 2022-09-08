/*
 * Copyright (c) 2022, Dylan Katz <dykatz@uw.edu>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <Applications/Browser/IconBag.h>

namespace Browser {
ErrorOr<IconBag> IconBag::try_create()
{
    IconBag icon_bag;

    icon_bag.filetype_html = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/filetype-html.png"sv));
    icon_bag.filetype_text = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/filetype-text.png"sv));
    icon_bag.filetype_javascript = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/filetype-javascript.png"sv));
    icon_bag.filetype_image = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/filetype-image.png"sv));
    icon_bag.bookmark_contour = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/bookmark-contour.png"sv));
    icon_bag.bookmark_filled = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/bookmark-filled.png"sv));
    icon_bag.inspector_object = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/inspector-object.png"sv));
    icon_bag.go_home = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/go-home.png"sv));
    icon_bag.find = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/find.png"sv));
    icon_bag.color_chooser = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/color-chooser.png"sv));
    icon_bag.delete_icon = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/delete.png"sv));
    icon_bag.new_tab = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/new-tab.png"sv));
    icon_bag.duplicate_tab = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/duplicate-tab.png"sv));
    icon_bag.close_other_tabs = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/close-other-tabs.png"sv));
    icon_bag.code = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/code.png"sv));
    icon_bag.dom_tree = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/browser/dom-tree.png"sv));
    icon_bag.layout = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/layout.png"sv));
    icon_bag.layers = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/layers.png"sv));
    icon_bag.filetype_css = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/filetype-css.png"sv));
    icon_bag.inspect = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/inspect.png"sv));
    icon_bag.history = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/history.png"sv));
    icon_bag.cookie = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/browser/cookie.png"sv));
    icon_bag.local_storage = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/browser/local-storage.png"sv));
    icon_bag.trash_can = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/trash-can.png"sv));
    icon_bag.clear_cache = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/browser/clear-cache.png"sv));
    icon_bag.spoof = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/spoof.png"sv));
    icon_bag.go_to = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/go-forward.png"sv));
    icon_bag.download = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/download.png"sv));
    icon_bag.copy = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/edit-copy.png"sv));
    icon_bag.rename = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/rename.png"sv));

    return icon_bag;
}
}
