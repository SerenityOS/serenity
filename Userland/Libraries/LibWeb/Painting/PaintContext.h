/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Palette.h>
#include <LibGfx/Rect.h>
#include <LibWeb/SVG/SVGContext.h>

namespace Web {

class PaintContext {
public:
    PaintContext(Gfx::Painter& painter, Palette const& palette, Gfx::IntPoint const& scroll_offset);

    Gfx::Painter& painter() const { return m_painter; }
    Palette const& palette() const { return m_palette; }

    bool has_svg_context() const { return m_svg_context.has_value(); }
    SVGContext& svg_context();
    void set_svg_context(SVGContext);
    void clear_svg_context();

    bool should_show_line_box_borders() const { return m_should_show_line_box_borders; }
    void set_should_show_line_box_borders(bool value) { m_should_show_line_box_borders = value; }

    Gfx::IntRect viewport_rect() const { return m_viewport_rect; }
    void set_viewport_rect(Gfx::IntRect const& rect) { m_viewport_rect = rect; }

    Gfx::IntPoint scroll_offset() const { return m_scroll_offset; }

    bool has_focus() const { return m_focus; }
    void set_has_focus(bool focus) { m_focus = focus; }

    PaintContext clone(Gfx::Painter& painter) const
    {
        auto clone = PaintContext(painter, m_palette, m_scroll_offset);
        clone.m_viewport_rect = m_viewport_rect;
        clone.m_should_show_line_box_borders = m_should_show_line_box_borders;
        clone.m_focus = m_focus;
        clone.m_svg_context = m_svg_context;
        return clone;
    }

private:
    Gfx::Painter& m_painter;
    Palette m_palette;
    Optional<SVGContext> m_svg_context;
    Gfx::IntRect m_viewport_rect;
    Gfx::IntPoint m_scroll_offset;
    bool m_should_show_line_box_borders { false };
    bool m_focus { false };
};

}
