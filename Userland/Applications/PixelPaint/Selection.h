/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Timer.h>
#include <LibGfx/Rect.h>

namespace PixelPaint {

class ImageEditor;

// Coordinates are image-relative.
class Selection {
public:
    explicit Selection(ImageEditor&);

    bool is_empty() const { return m_rect.is_empty(); }
    void clear() { m_rect = {}; }
    void set(Gfx::IntRect const& rect) { m_rect = rect; }

    void paint(Gfx::Painter&, ImageEditor const&);

    void draw_marching_ants(Gfx::Painter&, Gfx::IntRect const&) const;

private:
    ImageEditor& m_editor;
    Gfx::IntRect m_rect;
    RefPtr<Core::Timer> m_marching_ants_timer;
    int m_marching_ants_offset { 0 };
};

}
