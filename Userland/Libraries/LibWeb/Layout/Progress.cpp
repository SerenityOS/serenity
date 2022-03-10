/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Painter.h>
#include <LibGfx/StylePainter.h>
#include <LibWeb/Layout/Progress.h>
#include <LibWeb/Painting/Box.h>

namespace Web::Layout {

Progress::Progress(DOM::Document& document, HTML::HTMLProgressElement& element, NonnullRefPtr<CSS::StyleProperties> style)
    : LabelableNode(document, element, move(style))
{
    set_intrinsic_height(12);
}

Progress::~Progress()
{
}

void Progress::paint(PaintContext& context, Painting::PaintPhase phase)
{
    if (!is_visible())
        return;

    if (phase == Painting::PaintPhase::Foreground) {
        // FIXME: This does not support floating point value() and max()
        Gfx::StylePainter::paint_progressbar(context.painter(), enclosing_int_rect(m_paint_box->absolute_rect()), context.palette(), 0, dom_node().max(), dom_node().value(), "");
    }
}

}
