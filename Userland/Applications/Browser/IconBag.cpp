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

    icon_bag.filetype_html = TRY(Gfx::Bitmap::try_request_resource("filetype-html"));
    icon_bag.filetype_text = TRY(Gfx::Bitmap::try_request_resource("filetype-text"));
    icon_bag.filetype_javascript = TRY(Gfx::Bitmap::try_request_resource("filetype-javascript"));
    icon_bag.bookmark_contour = TRY(Gfx::Bitmap::try_request_resource("bookmark-contour"));
    icon_bag.bookmark_filled = TRY(Gfx::Bitmap::try_request_resource("bookmark-filled"));
    icon_bag.inspector_object = TRY(Gfx::Bitmap::try_request_resource("inspector-object"));
    icon_bag.go_home = TRY(Gfx::Bitmap::try_request_resource("go-home"));
    icon_bag.find = TRY(Gfx::Bitmap::try_request_resource("find"));
    icon_bag.color_chooser = TRY(Gfx::Bitmap::try_request_resource("color-chooser"));
    icon_bag.delete_icon = TRY(Gfx::Bitmap::try_request_resource("delete"));
    icon_bag.new_tab = TRY(Gfx::Bitmap::try_request_resource("new-tab"));
    icon_bag.duplicate_tab = TRY(Gfx::Bitmap::try_request_resource("duplicate-tab"));
    icon_bag.code = TRY(Gfx::Bitmap::try_request_resource("code"));
    icon_bag.dom_tree = TRY(Gfx::Bitmap::try_request_resource("dom-tree"));
    icon_bag.layout = TRY(Gfx::Bitmap::try_request_resource("layout"));
    icon_bag.layers = TRY(Gfx::Bitmap::try_request_resource("layers"));
    icon_bag.filetype_css = TRY(Gfx::Bitmap::try_request_resource("filetype-css"));
    icon_bag.inspect = TRY(Gfx::Bitmap::try_request_resource("inspect"));
    icon_bag.history = TRY(Gfx::Bitmap::try_request_resource("history"));
    icon_bag.cookie = TRY(Gfx::Bitmap::try_request_resource("cookie"));
    icon_bag.local_storage = TRY(Gfx::Bitmap::try_request_resource("local-storage"));
    icon_bag.trash_can = TRY(Gfx::Bitmap::try_request_resource("trash-can"));
    icon_bag.clear_cache = TRY(Gfx::Bitmap::try_request_resource("clear-cache"));
    icon_bag.spoof = TRY(Gfx::Bitmap::try_request_resource("spoof"));

    return icon_bag;
}
}
