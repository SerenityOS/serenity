/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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
    explicit PaintContext(Gfx::Painter& painter, const Palette& palette, const Gfx::IntPoint& scroll_offset)
        : m_painter(painter)
        , m_palette(palette)
        , m_scroll_offset(scroll_offset)
    {
    }

    Gfx::Painter& painter() const { return m_painter; }
    const Palette& palette() const { return m_palette; }

    bool has_svg_context() const { return m_svg_context.has_value(); }
    SVGContext& svg_context() { return m_svg_context.value(); }
    void set_svg_context(SVGContext context) { m_svg_context = context; }
    void clear_svg_context() { m_svg_context.clear(); }

    bool should_show_line_box_borders() const { return m_should_show_line_box_borders; }
    void set_should_show_line_box_borders(bool value) { m_should_show_line_box_borders = value; }

    Gfx::IntRect viewport_rect() const { return m_viewport_rect; }
    void set_viewport_rect(const Gfx::IntRect& rect) { m_viewport_rect = rect; }

    const Gfx::IntPoint& scroll_offset() const { return m_scroll_offset; }

    bool has_focus() const { return m_focus; }
    void set_has_focus(bool focus) { m_focus = focus; }

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
