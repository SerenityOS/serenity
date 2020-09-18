/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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
#include <LibGUI/ScrollBar.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Font.h>
#include <LibGfx/StylePainter.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/InProcessWebView.h>
#include <LibWeb/Layout/LayoutDocument.h>
#include <LibWeb/Layout/LayoutFrame.h>
#include <LibWeb/Page/Frame.h>

//#define DEBUG_HIGHLIGHT_FOCUSED_FRAME

namespace Web {

LayoutFrame::LayoutFrame(DOM::Document& document, DOM::Element& element, NonnullRefPtr<CSS::StyleProperties> style)
    : LayoutReplaced(document, element, move(style))
{
}

LayoutFrame::~LayoutFrame()
{
}

void LayoutFrame::layout(LayoutMode layout_mode)
{
    ASSERT(node().content_frame());

    set_has_intrinsic_width(true);
    set_has_intrinsic_height(true);
    // FIXME: Do proper error checking, etc.
    set_intrinsic_width(node().attribute(HTML::AttributeNames::width).to_int().value_or(300));
    set_intrinsic_height(node().attribute(HTML::AttributeNames::height).to_int().value_or(150));

    LayoutReplaced::layout(layout_mode);
}

void LayoutFrame::paint(PaintContext& context, PaintPhase phase)
{
    LayoutReplaced::paint(context, phase);

    if (phase == PaintPhase::Foreground) {
        auto* hosted_document = node().content_document();
        if (!hosted_document)
            return;
        auto* hosted_layout_tree = hosted_document->layout_node();
        if (!hosted_layout_tree)
            return;

        context.painter().save();
        auto old_viewport_rect = context.viewport_rect();

        context.painter().add_clip_rect(enclosing_int_rect(absolute_rect()));
        context.painter().translate(absolute_x(), absolute_y());

        context.set_viewport_rect({ {}, node().content_frame()->size() });
        const_cast<LayoutDocument*>(hosted_layout_tree)->paint_all_phases(context);

        context.set_viewport_rect(old_viewport_rect);
        context.painter().restore();

#ifdef DEBUG_HIGHLIGHT_FOCUSED_FRAME
        if (node().content_frame()->is_focused_frame()) {
            context.painter().draw_rect(absolute_rect().to<int>(), Color::Cyan);
        }
#endif
    }
}

void LayoutFrame::did_set_rect()
{
    LayoutReplaced::did_set_rect();

    ASSERT(node().content_frame());
    node().content_frame()->set_size(size().to_type<int>());
}

}
