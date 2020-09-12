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

#include <LibGUI/Event.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Font.h>
#include <LibGfx/StylePainter.h>
#include <LibWeb/Layout/LayoutCheckBox.h>
#include <LibWeb/Page/Frame.h>

namespace Web {

LayoutCheckBox::LayoutCheckBox(DOM::Document& document, HTML::HTMLInputElement& element, NonnullRefPtr<CSS::StyleProperties> style)
    : LayoutReplaced(document, element, move(style))
{
}

LayoutCheckBox::~LayoutCheckBox()
{
}

void LayoutCheckBox::layout(LayoutMode layout_mode)
{
    set_has_intrinsic_width(true);
    set_has_intrinsic_height(true);
    set_intrinsic_width(13);
    set_intrinsic_height(13);
    LayoutReplaced::layout(layout_mode);
}

void LayoutCheckBox::paint(PaintContext& context, PaintPhase phase)
{
    if (!is_visible())
        return;

    LayoutReplaced::paint(context, phase);

    if (phase == PaintPhase::Foreground) {
        Gfx::StylePainter::paint_check_box(context.painter(), enclosing_int_rect(absolute_rect()), context.palette(), node().enabled(), node().checked(), m_being_pressed);
    }
}

void LayoutCheckBox::handle_mousedown(Badge<EventHandler>, const Gfx::IntPoint&, unsigned button, unsigned)
{
    if (button != GUI::MouseButton::Left || !node().enabled())
        return;

    m_being_pressed = true;
    set_needs_display();

    m_tracking_mouse = true;
    frame().event_handler().set_mouse_event_tracking_layout_node(this);
}

void LayoutCheckBox::handle_mouseup(Badge<EventHandler>, const Gfx::IntPoint& position, unsigned button, unsigned)
{
    if (!m_tracking_mouse || button != GUI::MouseButton::Left || !node().enabled())
        return;

    // NOTE: Changing the checked state of the DOM node may run arbitrary JS, which could disappear this node.
    NonnullRefPtr protect = *this;

    bool is_inside = enclosing_int_rect(absolute_rect()).contains(position);
    if (is_inside)
        node().set_checked(!node().checked());

    m_being_pressed = false;
    m_tracking_mouse = false;
    frame().event_handler().set_mouse_event_tracking_layout_node(nullptr);
}

void LayoutCheckBox::handle_mousemove(Badge<EventHandler>, const Gfx::IntPoint& position, unsigned, unsigned)
{
    if (!m_tracking_mouse || !node().enabled())
        return;

    bool is_inside = enclosing_int_rect(absolute_rect()).contains(position);
    if (m_being_pressed == is_inside)
        return;

    m_being_pressed = is_inside;
    set_needs_display();
}

}
