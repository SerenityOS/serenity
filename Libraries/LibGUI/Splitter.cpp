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

Splitter::Splitter(Orientation orientation, Widget* parent)
    : Frame(parent)
    , m_orientation(orientation)
{
    set_background_role(ColorRole::Button);
    set_layout(make<BoxLayout>(orientation));
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
    Frame::resize_event(event);
    m_grabbable_rect = {};
}

void Splitter::enter_event(Core::Event&)
{
    window()->set_override_cursor(m_orientation == Orientation::Horizontal ? StandardCursor::ResizeHorizontal : StandardCursor::ResizeVertical);
}

void Splitter::leave_event(Core::Event&)
{
    if (!m_resizing)
        window()->set_override_cursor(StandardCursor::None);
    if (!m_grabbable_rect.is_empty()) {
        m_grabbable_rect = {};
        update();
    }
}

void Splitter::get_resize_candidates_at(const Gfx::Point& position, Widget*& first, Widget*& second)
{
    int x_or_y = position.primary_offset_for_orientation(m_orientation);
    int fudge = layout()->spacing();
    for_each_child_widget([&](auto& child) {
        int child_start = child.relative_rect().first_edge_for_orientation(m_orientation);
        int child_end = child.relative_rect().last_edge_for_orientation(m_orientation);
        if (x_or_y > child_end && (x_or_y - fudge) <= child_end)
            first = &child;
        if (x_or_y < child_start && (x_or_y + fudge) >= child_start)
            second = &child;
        return IterationDecision::Continue;
    });
    ASSERT(first);
    ASSERT(second);
}

void Splitter::mousedown_event(MouseEvent& event)
{
    if (event.button() != MouseButton::Left)
        return;
    m_resizing = true;

    Widget* first { nullptr };
    Widget* second { nullptr };
    get_resize_candidates_at(event.position(), first, second);

    m_first_resizee = first->make_weak_ptr();
    m_second_resizee = second->make_weak_ptr();
    m_first_resizee_start_size = first->size();
    m_second_resizee_start_size = second->size();
    m_resize_origin = event.position();
}

void Splitter::recompute_grabbable_rect(const Widget& first, const Widget& second)
{
    auto first_edge = first.relative_rect().primary_offset_for_orientation(m_orientation) + first.relative_rect().primary_size_for_orientation(m_orientation);
    auto second_edge = second.relative_rect().primary_offset_for_orientation(m_orientation);
    Gfx::Rect rect;
    rect.set_primary_offset_for_orientation(m_orientation, first_edge);
    rect.set_primary_size_for_orientation(m_orientation, second_edge - first_edge);
    rect.set_secondary_offset_for_orientation(m_orientation, first.relative_rect().secondary_offset_for_orientation(m_orientation));
    rect.set_secondary_size_for_orientation(m_orientation, first.relative_rect().secondary_size_for_orientation(m_orientation));
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
        get_resize_candidates_at(event.position(), first, second);
        recompute_grabbable_rect(*first, *second);
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
    if (!rect().contains(event.position()))
        window()->set_override_cursor(StandardCursor::None);
}

}
