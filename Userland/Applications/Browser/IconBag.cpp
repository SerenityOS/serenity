/*
 * Copyright (c) 2022, Dylan Katz <dykatz@uw.edu>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Applications/Browser/IconBag.h>

namespace Browser {
ErrorOr<IconBag> IconBag::try_create()
{
    IconBag icon_bag;

    icon_bag.default_favicon = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-browser.png"sv));
    icon_bag.filetype_html = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-html.png"sv));
    icon_bag.filetype_text = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-text.png"sv));
    icon_bag.filetype_javascript = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-javascript.png"sv));
    icon_bag.filetype_audio = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-sound.png"sv));
    icon_bag.filetype_image = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-image.png"sv));
    icon_bag.filetype_video = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-video.png"sv));
    icon_bag.bookmark_contour = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/bookmark-contour.png"sv));
    icon_bag.bookmark_filled = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/bookmark-filled.png"sv));
    icon_bag.inspector_object = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/inspector-object.png"sv));
    icon_bag.go_home = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/go-home.png"sv));
    icon_bag.find = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/find.png"sv));
    icon_bag.color_chooser = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/color-chooser.png"sv));
    icon_bag.delete_icon = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/delete.png"sv));
    icon_bag.new_tab = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/new-tab.png"sv));
    icon_bag.duplicate_tab = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/duplicate-tab.png"sv));
    icon_bag.close_other_tabs = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/close-other-tabs.png"sv));
    icon_bag.new_window = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/new-window.png"sv));
    icon_bag.code = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/code.png"sv));
    icon_bag.dom_tree = TRY(Gfx::Bitmap::load_from_file("/res/icons/browser/dom-tree.png"sv));
    icon_bag.layout = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/layout.png"sv));
    icon_bag.layers = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/layers.png"sv));
    icon_bag.filetype_css = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/filetype-css.png"sv));
    icon_bag.inspect = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/inspect.png"sv));
    icon_bag.history = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/history.png"sv));
    icon_bag.cookie = TRY(Gfx::Bitmap::load_from_file("/res/icons/browser/cookie.png"sv));
    icon_bag.local_storage = TRY(Gfx::Bitmap::load_from_file("/res/icons/browser/local-storage.png"sv));
    icon_bag.trash_can = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/trash-can.png"sv));
    icon_bag.clear_cache = TRY(Gfx::Bitmap::load_from_file("/res/icons/browser/clear-cache.png"sv));
    icon_bag.spoof = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/spoof.png"sv));
    icon_bag.go_to = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/go-forward.png"sv));
    icon_bag.download = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/download.png"sv));
    icon_bag.copy = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/edit-copy.png"sv));
    icon_bag.rename = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/rename.png"sv));
    icon_bag.play = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/play.png"sv));
    icon_bag.pause = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/pause.png"sv));
    icon_bag.mute = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/audio-volume-muted.png"sv));
    icon_bag.unmute = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/audio-volume-high.png"sv));
    icon_bag.search = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/find.png"sv));
    icon_bag.task_manager = TRY(Gfx::Bitmap::load_from_file("/res/icons/16x16/app-system-monitor.png"sv));

    return icon_bag;
}
}
