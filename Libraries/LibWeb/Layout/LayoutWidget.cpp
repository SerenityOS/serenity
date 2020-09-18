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
#include <LibWeb/Layout/LayoutWidget.h>
#include <LibWeb/Page/Frame.h>

namespace Web {

LayoutWidget::LayoutWidget(DOM::Document& document, DOM::Element& element, GUI::Widget& widget)
    : LayoutReplaced(document, element, CSS::StyleProperties::create())
    , m_widget(widget)
{
    set_has_intrinsic_width(true);
    set_has_intrinsic_height(true);
    set_intrinsic_width(widget.width());
    set_intrinsic_height(widget.height());
}

LayoutWidget::~LayoutWidget()
{
    widget().remove_from_parent();
}

void LayoutWidget::did_set_rect()
{
    LayoutReplaced::did_set_rect();
    update_widget();
}

void LayoutWidget::update_widget()
{
    auto adjusted_widget_position = absolute_rect().location().to_type<int>();
    auto& page_view = static_cast<const InProcessWebView&>(frame().page().client());
    adjusted_widget_position.move_by(-page_view.horizontal_scrollbar().value(), -page_view.vertical_scrollbar().value());
    widget().move_to(adjusted_widget_position);
}

}
