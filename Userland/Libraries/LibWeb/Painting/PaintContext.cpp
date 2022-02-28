/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Painting/PaintContext.h>

namespace Web {

PaintContext::PaintContext(Gfx::Painter& painter, Palette const& palette, Gfx::IntPoint const& scroll_offset)
    : m_painter(painter)
    , m_palette(palette)
    , m_scroll_offset(scroll_offset)
{
}

SVGContext& PaintContext::svg_context()
{
    return m_svg_context.value();
}

void PaintContext::set_svg_context(SVGContext context)
{
    m_svg_context = move(context);
}

void PaintContext::clear_svg_context()
{
    m_svg_context.clear();
}

}
