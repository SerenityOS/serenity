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

#include <AK/Debug.h>
#include <LibGfx/Painter.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/InProcessWebView.h>
#include <LibWeb/Layout/FrameBox.h>
#include <LibWeb/Layout/InitialContainingBlockBox.h>
#include <LibWeb/Page/Frame.h>

namespace Web::Layout {

FrameBox::FrameBox(DOM::Document& document, DOM::Element& element, NonnullRefPtr<CSS::StyleProperties> style)
    : ReplacedBox(document, element, move(style))
{
}

FrameBox::~FrameBox()
{
}

void FrameBox::prepare_for_replaced_layout()
{
    VERIFY(dom_node().content_frame());

    set_has_intrinsic_width(true);
    set_has_intrinsic_height(true);
    // FIXME: Do proper error checking, etc.
    set_intrinsic_width(dom_node().attribute(HTML::AttributeNames::width).to_int().value_or(300));
    set_intrinsic_height(dom_node().attribute(HTML::AttributeNames::height).to_int().value_or(150));
}

void FrameBox::paint(PaintContext& context, PaintPhase phase)
{
    ReplacedBox::paint(context, phase);

    if (phase == PaintPhase::Foreground) {
        auto* hosted_document = dom_node().content_document();
        if (!hosted_document)
            return;
        auto* hosted_layout_tree = hosted_document->layout_node();
        if (!hosted_layout_tree)
            return;

        context.painter().save();
        auto old_viewport_rect = context.viewport_rect();

        context.painter().add_clip_rect(enclosing_int_rect(absolute_rect()));
        context.painter().translate(absolute_x(), absolute_y());

        context.set_viewport_rect({ {}, dom_node().content_frame()->size() });
        const_cast<Layout::InitialContainingBlockBox*>(hosted_layout_tree)->paint_all_phases(context);

        context.set_viewport_rect(old_viewport_rect);
        context.painter().restore();

#if HIGHLIGHT_FOCUSED_FRAME_DEBUG
        if (dom_node().content_frame()->is_focused_frame()) {
            context.painter().draw_rect(absolute_rect().to<int>(), Color::Cyan);
        }
#endif
    }
}

void FrameBox::did_set_rect()
{
    ReplacedBox::did_set_rect();

    VERIFY(dom_node().content_frame());
    dom_node().content_frame()->set_size(size().to_type<int>());
}

}
