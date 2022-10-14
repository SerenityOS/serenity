/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TimelineContainer.h"
#include "TimelineView.h"
#include <LibGUI/Layout.h>

namespace Profiler {

TimelineContainer::TimelineContainer(GUI::Widget& header_container, TimelineView& timeline_view)
{
    set_should_hide_unnecessary_scrollbars(true);
    m_header_container = header_container;
    m_timeline_view = timeline_view;
    add_child(header_container);
    add_child(timeline_view);
    header_container.move_to_back();
    timeline_view.move_to_back();
    update_widget_sizes();
    update_widget_positions();

    int initial_height = min(300, timeline_view.height() + 16 + frame_thickness() * 2);
    set_preferred_height(initial_height);

    m_timeline_view->on_scale_change = [this] {
        update_widget_sizes();
        update_widget_positions();
    };
}

void TimelineContainer::did_scroll()
{
    AbstractScrollableWidget::did_scroll();
    update_widget_positions();
}

void TimelineContainer::update_widget_positions()
{
    m_header_container->move_to(0, -vertical_scrollbar().value());
    m_timeline_view->move_to(m_header_container->width() + -horizontal_scrollbar().value(), -vertical_scrollbar().value());
}

void TimelineContainer::update_widget_sizes()
{
    {
        m_timeline_view->do_layout();
        auto preferred_size = m_timeline_view->effective_preferred_size();
        m_timeline_view->resize(Gfx::IntSize(preferred_size));
        set_content_size(Gfx::IntSize(preferred_size));
    }

    {
        m_header_container->do_layout();
        auto preferred_size = m_header_container->effective_preferred_size();
        m_header_container->resize(Gfx::IntSize(preferred_size));
        set_size_occupied_by_fixed_elements({ preferred_size.width().as_int(), 0 });
    }
}

void TimelineContainer::resize_event(GUI::ResizeEvent& event)
{
    AbstractScrollableWidget::resize_event(event);
    update_widget_sizes();
    update_widget_positions();
}

}
