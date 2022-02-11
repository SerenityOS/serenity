/*
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Rect.h>

namespace GUI {

class WindowServerConnection {
public:
    static WindowServerConnection& the();

    void async_create_window(i32 window_id, Gfx::IntRect const& rect,
        bool auto_position, bool has_alpha_channel, bool modal, bool minimizable, bool closeable, bool resizable,
        bool fullscreen, bool frameless, bool forced_shadow, bool accessory, float opacity,
        float alpha_hit_threshold, Gfx::IntSize const& base_size, Gfx::IntSize const& size_increment,
        Gfx::IntSize const& minimum_size, Optional<Gfx::IntSize> const& resize_aspect_ratio, i32 type,
        String const& title, i32 parent_window_id, Gfx::IntRect const& launch_origin_rect);
};

}
