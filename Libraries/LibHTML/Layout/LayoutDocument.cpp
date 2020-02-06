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

#include <LibHTML/Dump.h>
#include <LibHTML/Frame.h>
#include <LibHTML/Layout/LayoutDocument.h>
#include <LibHTML/Layout/LayoutImage.h>

LayoutDocument::LayoutDocument(const Document& document, NonnullRefPtr<StyleProperties> style)
    : LayoutBlock(&document, move(style))
{
}

LayoutDocument::~LayoutDocument()
{
}

void LayoutDocument::layout()
{
    ASSERT(document().frame());
    rect().set_width(document().frame()->size().width());

    LayoutNode::layout();

    ASSERT(!children_are_inline());

    int lowest_bottom = 0;
    for_each_child([&](auto& child) {
        ASSERT(is<LayoutBlock>(child));
        auto& child_block = to<LayoutBlock>(child);
        if (child_block.rect().bottom() > lowest_bottom)
            lowest_bottom = child_block.rect().bottom();
    });
    rect().set_bottom(lowest_bottom);
}

void LayoutDocument::did_set_viewport_rect(Badge<Frame>, const Gfx::Rect& a_viewport_rect)
{
    Gfx::FloatRect viewport_rect(a_viewport_rect.x(), a_viewport_rect.y(), a_viewport_rect.width(), a_viewport_rect.height());
    for_each_in_subtree_of_type<LayoutImage>([&](auto& layout_image) {
        const_cast<HTMLImageElement&>(layout_image.node()).set_volatile({}, !viewport_rect.intersects(layout_image.rect()));
        return IterationDecision::Continue;
    });
}
