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

#include <LibWeb/Dump.h>
#include <LibWeb/Frame/Frame.h>
#include <LibWeb/Layout/LayoutDocument.h>
#include <LibWeb/Layout/LayoutImage.h>
#include <LibWeb/Layout/LayoutWidget.h>

namespace Web {

LayoutDocument::LayoutDocument(const Document& document, NonnullRefPtr<StyleProperties> style)
    : LayoutBlock(&document, move(style))
{
}

LayoutDocument::~LayoutDocument()
{
}

void LayoutDocument::layout(LayoutMode layout_mode)
{
    ASSERT(document().frame());
    set_width(document().frame()->size().width());

    LayoutNode::layout(layout_mode);

    ASSERT(!children_are_inline());

    float lowest_bottom = 0;
    for_each_child([&](auto& child) {
        ASSERT(is<LayoutBlock>(child));
        auto& child_block = to<LayoutBlock>(child);
        lowest_bottom = max(lowest_bottom, child_block.absolute_rect().bottom());
    });
    set_height(lowest_bottom);

    layout_absolute_descendants();

    // FIXME: This is a total hack. Make sure any GUI::Widgets are moved into place after layout.
    //        We should stop embedding GUI::Widgets entirely, since that won't work out-of-process.
    for_each_in_subtree_of_type<LayoutWidget>([&](auto& widget) {
        widget.update_widget();
        return IterationDecision::Continue;
    });
}

void LayoutDocument::did_set_viewport_rect(Badge<Frame>, const Gfx::IntRect& a_viewport_rect)
{
    Gfx::FloatRect viewport_rect(a_viewport_rect.x(), a_viewport_rect.y(), a_viewport_rect.width(), a_viewport_rect.height());
    for_each_in_subtree_of_type<LayoutImage>([&](auto& layout_image) {
        const_cast<HTMLImageElement&>(layout_image.node()).set_visible_in_viewport({}, viewport_rect.intersects(layout_image.absolute_rect()));
        return IterationDecision::Continue;
    });
}

}
