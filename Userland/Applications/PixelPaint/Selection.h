/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Timer.h>
#include <LibGfx/Rect.h>

#include "Mask.h"

namespace PixelPaint {

class ImageEditor;

// Coordinates are image-relative.
class Selection {
public:
    enum class MergeMode {
        Set,
        Add,
        Subtract,
        Intersect,
        __Count,
    };

    explicit Selection(ImageEditor&);

    bool is_empty() const { return m_mask.is_null(); }
    void clear();
    void merge(Mask const&, MergeMode);
    void merge(Gfx::IntRect const& rect, MergeMode mode) { merge(Mask::full(rect), mode); }
    Gfx::IntRect bounding_rect() const { return m_mask.bounding_rect(); }

    [[nodiscard]] bool is_selected(int x, int y) const { return m_mask.get(x, y) > 0; }
    [[nodiscard]] bool is_selected(Gfx::IntPoint const& point) const { return is_selected(point.x(), point.y()); }

    [[nodiscard]] u8 get_selection_alpha(int x, int y) const { return m_mask.get(x, y); }
    [[nodiscard]] u8 get_selection_alpha(Gfx::IntPoint const& point) const { return get_selection_alpha(point.x(), point.y()); }

    void paint(Gfx::Painter&);

    void draw_marching_ants(Gfx::Painter&, Gfx::IntRect const&) const;
    void draw_marching_ants(Gfx::Painter&, Mask const&) const;

    void begin_interactive_selection() { m_in_interactive_selection = true; }
    void end_interactive_selection() { m_in_interactive_selection = false; }

private:
    ImageEditor& m_editor;
    Mask m_mask;
    RefPtr<Core::Timer> m_marching_ants_timer;
    int m_marching_ants_offset { 0 };
    bool m_in_interactive_selection { false };

    void draw_marching_ants_pixel(Gfx::Painter&, int x, int y) const;
};

}
