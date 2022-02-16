/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Painter.h>
#include <LibGfx/StylePainter.h>
#include <LibWeb/Layout/Progress.h>

namespace Web::Layout {

Progress::Progress(DOM::Document& document, HTML::HTMLProgressElement& element, NonnullRefPtr<CSS::StyleProperties> style)
    : LabelableNode(document, element, move(style))
{
    set_intrinsic_height(12);
}

Progress::~Progress()
{
}

void Progress::paint(PaintContext& context, PaintPhase phase)
{
    if (!is_visible())
        return;

    if (phase == PaintPhase::Foreground) {
        // FIXME: This does not support floating point value() and max()
        Gfx::StylePainter::paint_progressbar(context.painter(), enclosing_int_rect(absolute_rect()), context.palette(), 0, dom_node().max(), dom_node().value(), "");
    }
}

}
