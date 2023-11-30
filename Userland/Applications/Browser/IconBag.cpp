/*
 * Copyright (c) 2022, Dylan Katz <dykatz@uw.edu>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Applications/Browser/IconBag.h>
#include <LibMedia/ImageFormats/ImageDecoder.h>

namespace Browser {
ErrorOr<IconBag> IconBag::try_create()
{
    IconBag icon_bag;

    icon_bag.filetype_html = TRY(Media::ImageDecoder::load_from_file("/res/icons/16x16/filetype-html.png"sv));
    icon_bag.filetype_text = TRY(Media::ImageDecoder::load_from_file("/res/icons/16x16/filetype-text.png"sv));
    icon_bag.filetype_javascript = TRY(Media::ImageDecoder::load_from_file("/res/icons/16x16/filetype-javascript.png"sv));
    icon_bag.filetype_audio = TRY(Media::ImageDecoder::load_from_file("/res/icons/16x16/filetype-sound.png"sv));
    icon_bag.filetype_image = TRY(Media::ImageDecoder::load_from_file("/res/icons/16x16/filetype-image.png"sv));
    icon_bag.filetype_video = TRY(Media::ImageDecoder::load_from_file("/res/icons/16x16/filetype-video.png"sv));
    icon_bag.bookmark_contour = TRY(Media::ImageDecoder::load_from_file("/res/icons/16x16/bookmark-contour.png"sv));
    icon_bag.bookmark_filled = TRY(Media::ImageDecoder::load_from_file("/res/icons/16x16/bookmark-filled.png"sv));
    icon_bag.inspector_object = TRY(Media::ImageDecoder::load_from_file("/res/icons/16x16/inspector-object.png"sv));
    icon_bag.go_home = TRY(Media::ImageDecoder::load_from_file("/res/icons/16x16/go-home.png"sv));
    icon_bag.find = TRY(Media::ImageDecoder::load_from_file("/res/icons/16x16/find.png"sv));
    icon_bag.color_chooser = TRY(Media::ImageDecoder::load_from_file("/res/icons/16x16/color-chooser.png"sv));
    icon_bag.delete_icon = TRY(Media::ImageDecoder::load_from_file("/res/icons/16x16/delete.png"sv));
    icon_bag.new_tab = TRY(Media::ImageDecoder::load_from_file("/res/icons/16x16/new-tab.png"sv));
    icon_bag.duplicate_tab = TRY(Media::ImageDecoder::load_from_file("/res/icons/16x16/duplicate-tab.png"sv));
    icon_bag.close_other_tabs = TRY(Media::ImageDecoder::load_from_file("/res/icons/16x16/close-other-tabs.png"sv));
    icon_bag.new_window = TRY(Media::ImageDecoder::load_from_file("/res/icons/16x16/new-window.png"sv));
    icon_bag.code = TRY(Media::ImageDecoder::load_from_file("/res/icons/16x16/code.png"sv));
    icon_bag.dom_tree = TRY(Media::ImageDecoder::load_from_file("/res/icons/browser/dom-tree.png"sv));
    icon_bag.layout = TRY(Media::ImageDecoder::load_from_file("/res/icons/16x16/layout.png"sv));
    icon_bag.layers = TRY(Media::ImageDecoder::load_from_file("/res/icons/16x16/layers.png"sv));
    icon_bag.filetype_css = TRY(Media::ImageDecoder::load_from_file("/res/icons/16x16/filetype-css.png"sv));
    icon_bag.inspect = TRY(Media::ImageDecoder::load_from_file("/res/icons/16x16/inspect.png"sv));
    icon_bag.history = TRY(Media::ImageDecoder::load_from_file("/res/icons/16x16/history.png"sv));
    icon_bag.cookie = TRY(Media::ImageDecoder::load_from_file("/res/icons/browser/cookie.png"sv));
    icon_bag.local_storage = TRY(Media::ImageDecoder::load_from_file("/res/icons/browser/local-storage.png"sv));
    icon_bag.trash_can = TRY(Media::ImageDecoder::load_from_file("/res/icons/16x16/trash-can.png"sv));
    icon_bag.clear_cache = TRY(Media::ImageDecoder::load_from_file("/res/icons/browser/clear-cache.png"sv));
    icon_bag.spoof = TRY(Media::ImageDecoder::load_from_file("/res/icons/16x16/spoof.png"sv));
    icon_bag.go_to = TRY(Media::ImageDecoder::load_from_file("/res/icons/16x16/go-forward.png"sv));
    icon_bag.download = TRY(Media::ImageDecoder::load_from_file("/res/icons/16x16/download.png"sv));
    icon_bag.copy = TRY(Media::ImageDecoder::load_from_file("/res/icons/16x16/edit-copy.png"sv));
    icon_bag.rename = TRY(Media::ImageDecoder::load_from_file("/res/icons/16x16/rename.png"sv));
    icon_bag.play = TRY(Media::ImageDecoder::load_from_file("/res/icons/16x16/play.png"sv));
    icon_bag.pause = TRY(Media::ImageDecoder::load_from_file("/res/icons/16x16/pause.png"sv));
    icon_bag.mute = TRY(Media::ImageDecoder::load_from_file("/res/icons/16x16/audio-volume-muted.png"sv));
    icon_bag.unmute = TRY(Media::ImageDecoder::load_from_file("/res/icons/16x16/audio-volume-high.png"sv));
    icon_bag.search = TRY(Media::ImageDecoder::load_from_file("/res/icons/16x16/find.png"sv));

    return icon_bag;
}
}
