/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibGUI/Painter.h>
#include <LibGfx/Font.h>
#include <LibGfx/StylePainter.h>
#include <LibWeb/Layout/LayoutCanvas.h>

namespace Web {

LayoutCanvas::LayoutCanvas(DOM::Document& document, HTML::HTMLCanvasElement& element, NonnullRefPtr<CSS::StyleProperties> style)
    : LayoutReplaced(document, element, move(style))
{
}

LayoutCanvas::~LayoutCanvas()
{
}

void LayoutCanvas::layout(LayoutMode layout_mode)
{
    set_has_intrinsic_width(true);
    set_has_intrinsic_height(true);
    set_intrinsic_width(node().width());
    set_intrinsic_height(node().height());
    LayoutReplaced::layout(layout_mode);
}

void LayoutCanvas::paint(PaintContext& context, PaintPhase phase)
{
    if (!is_visible())
        return;

    LayoutReplaced::paint(context, phase);

    if (phase == PaintPhase::Foreground) {
        // FIXME: This should be done at a different level. Also rect() does not include padding etc!
        if (!context.viewport_rect().intersects(enclosing_int_rect(absolute_rect())))
            return;

        if (node().bitmap())
            context.painter().draw_scaled_bitmap(enclosing_int_rect(absolute_rect()), *node().bitmap(), node().bitmap()->rect());
    }
}

}
