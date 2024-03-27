/*
 * Copyright (c) 2022, Dylan Katz <dykatz@uw.edu>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <LibGfx/Bitmap.h>

namespace Browser {
struct IconBag final {
    static ErrorOr<IconBag> try_create();

    RefPtr<Gfx::Bitmap> default_favicon { nullptr };
    RefPtr<Gfx::Bitmap> filetype_html { nullptr };
    RefPtr<Gfx::Bitmap> filetype_text { nullptr };
    RefPtr<Gfx::Bitmap> filetype_javascript { nullptr };
    RefPtr<Gfx::Bitmap> filetype_audio { nullptr };
    RefPtr<Gfx::Bitmap> filetype_image { nullptr };
    RefPtr<Gfx::Bitmap> filetype_video { nullptr };
    RefPtr<Gfx::Bitmap> bookmark_contour { nullptr };
    RefPtr<Gfx::Bitmap> bookmark_filled { nullptr };
    RefPtr<Gfx::Bitmap> inspector_object { nullptr };
    RefPtr<Gfx::Bitmap> go_home { nullptr };
    RefPtr<Gfx::Bitmap> find { nullptr };
    RefPtr<Gfx::Bitmap> color_chooser { nullptr };
    RefPtr<Gfx::Bitmap> delete_icon { nullptr };
    RefPtr<Gfx::Bitmap> new_tab { nullptr };
    RefPtr<Gfx::Bitmap> duplicate_tab { nullptr };
    RefPtr<Gfx::Bitmap> close_other_tabs { nullptr };
    RefPtr<Gfx::Bitmap> new_window { nullptr };
    RefPtr<Gfx::Bitmap> code { nullptr };
    RefPtr<Gfx::Bitmap> dom_tree { nullptr };
    RefPtr<Gfx::Bitmap> layout { nullptr };
    RefPtr<Gfx::Bitmap> layers { nullptr };
    RefPtr<Gfx::Bitmap> filetype_css { nullptr };
    RefPtr<Gfx::Bitmap> inspect { nullptr };
    RefPtr<Gfx::Bitmap> history { nullptr };
    RefPtr<Gfx::Bitmap> cookie { nullptr };
    RefPtr<Gfx::Bitmap> local_storage { nullptr };
    RefPtr<Gfx::Bitmap> trash_can { nullptr };
    RefPtr<Gfx::Bitmap> clear_cache { nullptr };
    RefPtr<Gfx::Bitmap> spoof { nullptr };
    RefPtr<Gfx::Bitmap> go_to { nullptr };
    RefPtr<Gfx::Bitmap> download { nullptr };
    RefPtr<Gfx::Bitmap> copy { nullptr };
    RefPtr<Gfx::Bitmap> rename { nullptr };
    RefPtr<Gfx::Bitmap> play { nullptr };
    RefPtr<Gfx::Bitmap> pause { nullptr };
    RefPtr<Gfx::Bitmap> mute { nullptr };
    RefPtr<Gfx::Bitmap> unmute { nullptr };
    RefPtr<Gfx::Bitmap> search { nullptr };
    RefPtr<Gfx::Bitmap> task_manager { nullptr };
};
}
