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
#include <LibGfx/Font.h>
#include <LibGfx/Painter.h>
#include <LibGfx/StylePainter.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/Layout/ButtonBox.h>
#include <LibWeb/Page/Frame.h>

namespace Web::Layout {

ButtonBox::ButtonBox(DOM::Document& document, HTML::HTMLInputElement& element, NonnullRefPtr<CSS::StyleProperties> style)
    : ReplacedBox(document, element, move(style))
{
}

ButtonBox::~ButtonBox()
{
}

void ButtonBox::prepare_for_replaced_layout()
{
    set_intrinsic_width(font().width(dom_node().value()) + 20);
    set_has_intrinsic_width(true);

    set_intrinsic_height(20);
    set_has_intrinsic_height(true);
}

void ButtonBox::paint(PaintContext& context, PaintPhase phase)
{
    if (!is_visible())
        return;

    ReplacedBox::paint(context, phase);

    if (phase == PaintPhase::Foreground) {
        bool hovered = document().hovered_node() == &dom_node();
        Gfx::StylePainter::paint_button(context.painter(), enclosing_int_rect(absolute_rect()), context.palette(), Gfx::ButtonStyle::Normal, m_being_pressed, hovered, dom_node().checked(), dom_node().enabled());

        auto text_rect = enclosing_int_rect(absolute_rect());
        if (m_being_pressed)
            text_rect.move_by(1, 1);
        context.painter().draw_text(text_rect, dom_node().value(), font(), Gfx::TextAlignment::Center, context.palette().button_text());
    }
}

void ButtonBox::handle_mousedown(Badge<EventHandler>, const Gfx::IntPoint&, unsigned button, unsigned)
{
    if (button != GUI::MouseButton::Left || !dom_node().enabled())
        return;

    m_being_pressed = true;
    set_needs_display();

    m_tracking_mouse = true;
    frame().event_handler().set_mouse_event_tracking_layout_node(this);
}

void ButtonBox::handle_mouseup(Badge<EventHandler>, const Gfx::IntPoint& position, unsigned button, unsigned)
{
    if (!m_tracking_mouse || button != GUI::MouseButton::Left || !dom_node().enabled())
        return;

    // NOTE: Handling the click may run arbitrary JS, which could disappear this node.
    NonnullRefPtr protected_this = *this;
    NonnullRefPtr protected_frame = frame();

    bool is_inside = enclosing_int_rect(absolute_rect()).contains(position);
    if (is_inside)
        dom_node().did_click_button({});

    m_being_pressed = false;
    m_tracking_mouse = false;

    protected_frame->event_handler().set_mouse_event_tracking_layout_node(nullptr);
}

void ButtonBox::handle_mousemove(Badge<EventHandler>, const Gfx::IntPoint& position, unsigned, unsigned)
{
    if (!m_tracking_mouse || !dom_node().enabled())
        return;

    bool is_inside = enclosing_int_rect(absolute_rect()).contains(position);
    if (m_being_pressed == is_inside)
        return;

    m_being_pressed = is_inside;
    set_needs_display();
}

}
