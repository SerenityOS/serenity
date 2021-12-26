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
#include <LibGUI/Painter.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/Window.h>
#include <LibGfx/Palette.h>

namespace GUI {

Splitter::Splitter(Orientation orientation)
    : m_orientation(orientation)
{
    set_background_role(ColorRole::Button);
    set_layout<BoxLayout>(orientation);
    set_fill_with_background_color(true);
    layout()->set_spacing(3);
}

Splitter::~Splitter()
{
}

void Splitter::paint_event(PaintEvent& event)
{
    Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.fill_rect(m_grabbable_rect, palette().hover_highlight());
}

void Splitter::resize_event(ResizeEvent& event)
{
    Widget::resize_event(event);
    m_grabbable_rect = {};
}

void Splitter::override_cursor(bool do_override)
{
    if (do_override) {
        if (!m_overriding_cursor) {
            set_override_cursor(m_orientation == Orientation::Horizontal ? Gfx::StandardCursor::ResizeColumn : Gfx::StandardCursor::ResizeRow);
            m_overriding_cursor = true;
        }
    } else {
        if (m_overriding_cursor) {
            set_override_cursor(Gfx::StandardCursor::None);
            m_overriding_cursor = false;
        }
    }
}

void Splitter::leave_event(Core::Event&)
{
    if (!m_resizing)
        override_cursor(false);
    if (!m_grabbable_rect.is_empty()) {
        m_grabbable_rect = {};
        update();
    }
}

bool Splitter::get_resize_candidates_at(const Gfx::IntPoint& position, Widget*& first, Widget*& second)
{
    int x_or_y = position.primary_offset_for_orientation(m_orientation);

    auto child_widgets = this->child_widgets();
    if (child_widgets.size() < 2)
        return false;

    for (size_t i = 0; i < child_widgets.size() - 1; ++i) {
        auto* first_candidate = child_widgets[i];
        auto* second_candidate = child_widgets[i + 1];

        if (x_or_y > first_candidate->content_rect().last_edge_for_orientation(m_orientation)
            && x_or_y <= second_candidate->content_rect().first_edge_for_orientation(m_orientation)) {
            first = first_candidate;
            second = second_candidate;
            return true;
        }
    }
    return false;
}

void Splitter::mousedown_event(MouseEvent& event)
{
    if (event.button() != MouseButton::Left)
        return;
    m_resizing = true;

    Widget* first { nullptr };
    Widget* second { nullptr };
    if (!get_resize_candidates_at(event.position(), first, second))
        return;

    m_first_resizee = first->make_weak_ptr();
    m_second_resizee = second->make_weak_ptr();
    m_first_resizee_start_size = first->size();
    m_second_resizee_start_size = second->size();
    m_resize_origin = event.position();
}

void Splitter::recompute_grabbable_rect(const Widget& first, const Widget& second)
{
    auto first_edge = first.content_rect().primary_offset_for_orientation(m_orientation) + first.content_rect().primary_size_for_orientation(m_orientation);
    auto second_edge = second.content_rect().primary_offset_for_orientation(m_orientation);
    Gfx::IntRect rect;
    rect.set_primary_offset_for_orientation(m_orientation, first_edge);
    rect.set_primary_size_for_orientation(m_orientation, second_edge - first_edge);
    rect.set_secondary_offset_for_orientation(m_orientation, first.content_rect().secondary_offset_for_orientation(m_orientation));
    rect.set_secondary_size_for_orientation(m_orientation, first.content_rect().secondary_size_for_orientation(m_orientation));

    if (m_grabbable_rect != rect) {
        m_grabbable_rect = rect;
        update();
    }
}

void Splitter::mousemove_event(MouseEvent& event)
{
    if (!m_resizing) {
        Widget* first { nullptr };
        Widget* second { nullptr };
        if (!get_resize_candidates_at(event.position(), first, second)) {
            override_cursor(false);
            return;
        }
        recompute_grabbable_rect(*first, *second);
        override_cursor(m_grabbable_rect.contains(event.position()));
        return;
    }
    auto delta = event.position() - m_resize_origin;
    if (!m_first_resizee || !m_second_resizee) {
        // One or both of the resizees were deleted during an ongoing resize, screw this.
        m_resizing = false;
        return;
    }
    int minimum_size = 0;
    auto new_first_resizee_size = m_first_resizee_start_size;
    auto new_second_resizee_size = m_second_resizee_start_size;

    new_first_resizee_size.set_primary_size_for_orientation(m_orientation, new_first_resizee_size.primary_size_for_orientation(m_orientation) + delta.primary_offset_for_orientation(m_orientation));
    new_second_resizee_size.set_primary_size_for_orientation(m_orientation, new_second_resizee_size.primary_size_for_orientation(m_orientation) - delta.primary_offset_for_orientation(m_orientation));

    if (new_first_resizee_size.primary_size_for_orientation(m_orientation) < minimum_size) {
        int correction = minimum_size - new_first_resizee_size.primary_size_for_orientation(m_orientation);
        new_first_resizee_size.set_primary_size_for_orientation(m_orientation, new_first_resizee_size.primary_size_for_orientation(m_orientation) + correction);
        new_second_resizee_size.set_primary_size_for_orientation(m_orientation, new_second_resizee_size.primary_size_for_orientation(m_orientation) - correction);
    }
    if (new_second_resizee_size.primary_size_for_orientation(m_orientation) < minimum_size) {
        int correction = minimum_size - new_second_resizee_size.primary_size_for_orientation(m_orientation);
        new_second_resizee_size.set_primary_size_for_orientation(m_orientation, new_second_resizee_size.primary_size_for_orientation(m_orientation) + correction);
        new_first_resizee_size.set_primary_size_for_orientation(m_orientation, new_first_resizee_size.primary_size_for_orientation(m_orientation) - correction);
    }
    m_first_resizee->set_preferred_size(new_first_resizee_size);
    m_second_resizee->set_preferred_size(new_second_resizee_size);

    m_first_resizee->set_size_policy(m_orientation, SizePolicy::Fixed);
    m_second_resizee->set_size_policy(m_orientation, SizePolicy::Fill);

    invalidate_layout();
}

void Splitter::did_layout()
{
    if (m_first_resizee && m_second_resizee)
        recompute_grabbable_rect(*m_first_resizee, *m_second_resizee);
}

void Splitter::mouseup_event(MouseEvent& event)
{
    if (event.button() != MouseButton::Left)
        return;
    m_resizing = false;
    m_first_resizee = nullptr;
    m_second_resizee = nullptr;
    if (!rect().contains(event.position()))
        set_override_cursor(Gfx::StandardCursor::None);
}

}
