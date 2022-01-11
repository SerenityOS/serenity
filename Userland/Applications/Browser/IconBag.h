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

    RefPtr<Gfx::Bitmap> filetype_html { nullptr };
    RefPtr<Gfx::Bitmap> filetype_text { nullptr };
    RefPtr<Gfx::Bitmap> filetype_javascript { nullptr };
    RefPtr<Gfx::Bitmap> bookmark_contour { nullptr };
    RefPtr<Gfx::Bitmap> bookmark_filled { nullptr };
    RefPtr<Gfx::Bitmap> inspector_object { nullptr };
    RefPtr<Gfx::Bitmap> find { nullptr };
    RefPtr<Gfx::Bitmap> color_chooser { nullptr };
    RefPtr<Gfx::Bitmap> delete_icon { nullptr };
    RefPtr<Gfx::Bitmap> new_tab { nullptr };
    RefPtr<Gfx::Bitmap> duplicate_tab { nullptr };
    RefPtr<Gfx::Bitmap> code { nullptr };
    RefPtr<Gfx::Bitmap> tree { nullptr };
    RefPtr<Gfx::Bitmap> inspect { nullptr };
    RefPtr<Gfx::Bitmap> cookie { nullptr };
};
}
