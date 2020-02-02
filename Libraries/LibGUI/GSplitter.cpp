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

#include <LibDraw/Palette.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GSplitter.h>
#include <LibGUI/GWindow.h>

GSplitter::GSplitter(Orientation orientation, GWidget* parent)
    : GFrame(parent)
    , m_orientation(orientation)
{
    set_background_role(ColorRole::Button);
    set_layout(make<GBoxLayout>(orientation));
    set_fill_with_background_color(true);
    layout()->set_spacing(3);
}

GSplitter::~GSplitter()
{
}

void GSplitter::enter_event(Core::Event&)
{
    set_background_role(ColorRole::HoverHighlight);
    window()->set_override_cursor(m_orientation == Orientation::Horizontal ? GStandardCursor::ResizeHorizontal : GStandardCursor::ResizeVertical);
    update();
}

void GSplitter::leave_event(Core::Event&)
{
    set_background_role(ColorRole::Button);
    if (!m_resizing)
        window()->set_override_cursor(GStandardCursor::None);
    update();
}

void GSplitter::mousedown_event(GMouseEvent& event)
{
    if (event.button() != GMouseButton::Left)
        return;
    m_resizing = true;
    int x_or_y = event.position().primary_offset_for_orientation(m_orientation);
    GWidget* first_resizee { nullptr };
    GWidget* second_resizee { nullptr };
    int fudge = layout()->spacing();
    for_each_child_widget([&](auto& child) {
        int child_start = child.relative_rect().first_edge_for_orientation(m_orientation);
        int child_end = child.relative_rect().last_edge_for_orientation(m_orientation);
        if (x_or_y > child_end && (x_or_y - fudge) <= child_end)
            first_resizee = &child;
        if (x_or_y < child_start && (x_or_y + fudge) >= child_start)
            second_resizee = &child;
        return IterationDecision::Continue;
    });
    ASSERT(first_resizee && second_resizee);
    m_first_resizee = first_resizee->make_weak_ptr();
    m_second_resizee = second_resizee->make_weak_ptr();
    m_first_resizee_start_size = first_resizee->size();
    m_second_resizee_start_size = second_resizee->size();
    m_resize_origin = event.position();
}

void GSplitter::mousemove_event(GMouseEvent& event)
{
    if (!m_resizing)
        return;
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

void GSplitter::mouseup_event(GMouseEvent& event)
{
    if (event.button() != GMouseButton::Left)
        return;
    m_resizing = false;
    if (!rect().contains(event.position()))
        window()->set_override_cursor(GStandardCursor::None);
}
