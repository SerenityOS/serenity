/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Rect.h>

namespace PixelPaint {

class ImageEditor;

// Coordinates are image-relative.
class Selection {
public:
    Selection() { }

    bool is_empty() const { return m_rect.is_empty(); }
    void clear() { m_rect = {}; }
    void set(Gfx::IntRect const& rect) { m_rect = rect; }

    void paint(Gfx::Painter&, ImageEditor const&);

private:
    Gfx::IntRect m_rect;
};

}
