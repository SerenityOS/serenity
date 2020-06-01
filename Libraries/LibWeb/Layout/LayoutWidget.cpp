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
#include <LibWeb/Frame.h>
#include <LibWeb/Layout/LayoutWidget.h>
#include <LibWeb/PageView.h>

namespace Web {

LayoutWidget::LayoutWidget(const Element& element, GUI::Widget& widget)
    : LayoutReplaced(element, StyleProperties::create())
    , m_widget(widget)
{
}

LayoutWidget::~LayoutWidget()
{
    widget().remove_from_parent();
}

void LayoutWidget::layout(LayoutMode layout_mode)
{
    rect().set_size(widget().width(), widget().height());
    LayoutReplaced::layout(layout_mode);

    auto adjusted_widget_position = rect().location().to_int_point();
    if (auto* page_view = document().frame()->page_view())
        adjusted_widget_position.move_by(-page_view->horizontal_scrollbar().value(), -page_view->vertical_scrollbar().value());
    widget().move_to(adjusted_widget_position);
}

void LayoutWidget::render(RenderingContext& context)
{
    LayoutReplaced::render(context);
}

}
