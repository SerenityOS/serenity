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

#include <LibGUI/BoxLayout.h>
#include <LibGUI/HorizontalStackView.h>
#include <LibGUI/ScrollBar.h>

namespace GUI {

HorizontalStackView::HorizontalStackView()
    : AbstractStackView()
{
    set_layout<GUI::HorizontalBoxLayout>();
}

HorizontalStackView::~HorizontalStackView() {}

void HorizontalStackView::did_scroll()
{
    int horzontal_offset = -horizontal_scrollbar().value();
    int spacing = layout()->spacing();
    for_each_child_widget([&](auto& child) {
        if (child.is_visible()) {
            child.set_relative_rect(Gfx::Rect(horzontal_offset, child.rect().y(), child.rect().width(), child.rect().height()));
            horzontal_offset += child.rect().width() + spacing;
        }
        return IterationDecision::Continue;
    });
}

void HorizontalStackView::add_to_scrollbar_range(const Gfx::Size& add)
{
    int spacing = layout()->spacing();
    set_content_size(Gfx::Size(content_width() + add.width() + spacing, add.height()));
}

void HorizontalStackView::rem_from_scrollbar_range(const Gfx::Size& add)
{
    int spacing = layout()->spacing();
    set_content_size(Gfx::Size(content_width() - add.width() - spacing, add.height()));
}

}
