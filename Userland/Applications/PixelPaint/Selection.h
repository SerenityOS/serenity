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
    void clear();
    void set(Gfx::IntRect const& rect) { m_rect = rect; }
    Gfx::IntRect bounding_rect() const { return m_rect; }

    void paint(Gfx::Painter&);

    void draw_marching_ants(Gfx::Painter&, Gfx::IntRect const&) const;

    void begin_interactive_selection() { m_in_interactive_selection = true; }
    void end_interactive_selection() { m_in_interactive_selection = false; }

private:
    ImageEditor& m_editor;
    Gfx::IntRect m_rect;
    RefPtr<Core::Timer> m_marching_ants_timer;
    int m_marching_ants_offset { 0 };
    bool m_in_interactive_selection { false };
};

}
