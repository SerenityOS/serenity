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

    icon_bag.filetype_html = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/filetype-html.png"));
    icon_bag.filetype_text = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/filetype-text.png"));
    icon_bag.filetype_javascript = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/filetype-javascript.png"));
    icon_bag.bookmark_contour = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/bookmark-contour.png"));
    icon_bag.bookmark_filled = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/bookmark-filled.png"));
    icon_bag.inspector_object = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/inspector-object.png"));
    icon_bag.go_home = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/go-home.png"));
    icon_bag.find = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/find.png"));
    icon_bag.color_chooser = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/color-chooser.png"));
    icon_bag.delete_icon = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/delete.png"));
    icon_bag.new_tab = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/new-tab.png"));
    icon_bag.duplicate_tab = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/duplicate-tab.png"));
    icon_bag.code = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/code.png"));
    icon_bag.dom_tree = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/browser/dom-tree.png"));
    icon_bag.layout = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/layout.png"));
    icon_bag.layers = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/layers.png"));
    icon_bag.filetype_css = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/filetype-css.png"));
    icon_bag.inspect = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/inspect.png"));
    icon_bag.history = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/history.png"));
    icon_bag.cookie = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/browser/cookie.png"));
    icon_bag.local_storage = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/browser/local-storage.png"));
    icon_bag.trash_can = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/trash-can.png"));
    icon_bag.clear_cache = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/browser/clear-cache.png"));
    icon_bag.spoof = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/16x16/spoof.png"));

    return icon_bag;
}
}
