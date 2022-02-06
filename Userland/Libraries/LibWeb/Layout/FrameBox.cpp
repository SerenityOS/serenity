/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <LibGfx/Painter.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/Layout/FrameBox.h>
#include <LibWeb/Layout/InitialContainingBlock.h>

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
    VERIFY(dom_node().nested_browsing_context());

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

        context.set_viewport_rect({ {}, dom_node().nested_browsing_context()->size() });
        const_cast<Layout::InitialContainingBlock*>(hosted_layout_tree)->paint_all_phases(context);

        context.set_viewport_rect(old_viewport_rect);
        context.painter().restore();

        if constexpr (HIGHLIGHT_FOCUSED_FRAME_DEBUG) {
            if (dom_node().nested_browsing_context()->is_focused_context()) {
                context.painter().draw_rect(absolute_rect().to_type<int>(), Color::Cyan);
            }
        }
    }
}

void FrameBox::did_set_rect()
{
    ReplacedBox::did_set_rect();

    VERIFY(dom_node().nested_browsing_context());
    dom_node().nested_browsing_context()->set_size(content_size().to_type<int>());
}

}
